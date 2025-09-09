package com.example.zebraprj.grpc;

import com.example.zebraprj.model.User;
import com.example.zebraprj.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;

// gRPC service implementation responsible for user creation
@GrpcService // Registers this class as a gRPC service bean
public class UserGrpcServiceImpl extends  UserGrpcServiceGrpc.UserGrpcServiceImplBase {
    private final UserRepository userRepository; // Repository for DB operations
    private static final Logger log = LoggerFactory.getLogger(UserGrpcServiceImpl.class);

    public UserGrpcServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository; // Inject repository through constructor
    }

    @Override
    public void addUser(AddUserRequest request, StreamObserver<AddUserResponse> responseObserver) {
        log.info("Received request to add user with name:{}", request.getUser().getName());
        List<String> errors = new ArrayList<>();  // Collect validation errors

        // Validate unique name
        if (userRepository.existsByName(request.getUser().getName())) {
            errors.add("User with name " + request.getUser().getName() + "already exists");
            log.warn("Attempt to add existing user name: {}", request.getUser().getName());
        }

        // Validate unique email
        if (userRepository.existsByEmail(request.getUser().getEmail())) {
            errors.add("User with email " + request.getUser().getEmail() + " already exists");
            log.warn("Attempt to add existing user email: {}", request.getUser().getEmail());
        }

        AddUserResponse.Builder responseBuilder = AddUserResponse.newBuilder(); // Prepare response

        if (!errors.isEmpty()) {
            log.warn("User creation failed with validation errors: {}", errors);
            responseBuilder.addAllError(errors); // Return validation errors
            responseObserver.onNext(responseBuilder.build()); // Return validation errors
            responseObserver.onCompleted(); // Complete stream
            return;
        }

        // Map request to entity
        User user = new User();
        user.setName(request.getUser().getName());
        user.setEmail(request.getUser().getEmail());
        user.setBirthdate(LocalDate.parse(request.getUser().getBirthdate())); // Parse birthdate

        User saved = userRepository.save(user); // Persist new
        log.info("User saved with id: {}", saved.getId());

        // Map entity back to protobuf message
        UserMessage responseUser =  UserMessage.newBuilder()
                .setId(saved.getId())
                .setName(saved.getName())
                .setEmail(saved.getEmail())
                .setBirthdate(saved.getBirthdate().toString())
                .build();

        responseBuilder.setUser(responseUser); // Attach user to response
        responseObserver.onNext(responseBuilder.build()); // Send successful response
        responseObserver.onCompleted(); // Finish call
        log.info("AddUser request processing completed for id: {}", saved.getId());
    }

    @Override
    public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
        log.info("getUsers call. Fetching all users");
        List<UserMessage> users = userRepository.findAll().stream()
                .map(u -> UserMessage.newBuilder()
                        .setId(u.getId())
                        .setName(u.getName())
                        .setEmail(u.getEmail())
                        .setBirthdate(u.getBirthdate().toString())
                        .build())
                .toList();

        GetUsersResponse response = GetUsersResponse.newBuilder()
                .addAllUsers(users)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        log.info("Retrieved {} users", users.size());
    }

    @Override
    public void deleteUserByNameId (DeleteUserByNameIDRequest request,
                                    StreamObserver<DeleteUserByNameIDResponse> responseObserver){
        log.info("deleteUserByNameId call. Deleting users: {}", request.getRequestList());
        List<String> errors = new ArrayList<>();
        List<String> deleted =  new ArrayList<>();

        for(DeleteUserRequest entity : request.getRequestList()){
            if(entity.getId() != 0) {
                long id = entity.getId();
                if (userRepository.existsById(id)) {
                    userRepository.deleteById(id);
                    deleted.add("Deleted user with ID: " + id);
                } else {
                    errors.add("User with ID " + id + " does not exist");
                    log.warn("User with ID {} does not exist", id);
                }
            } else if (!entity.getName().isEmpty()) {
                String name = entity.getName();
                List<User> usersByName = userRepository.findByName(name);
                if(!usersByName.isEmpty()){
                    userRepository.deleteAll(usersByName);
                    deleted.add("Deleted user with name: " + name);
                } else  {
                    errors.add("User(s) with name '" + name + "' do(es) not exist");
                    log.warn("User(s) with name '{}' do(es) not exist", name);
                }
            } else {
                errors.add("Invalid request object " + entity);
                log.warn("Invalid request object {}", entity);
            }
        }
        DeleteUserByNameIDResponse.Builder responseBuilder = DeleteUserByNameIDResponse.newBuilder()
                .addAllDelete(deleted);
        if (!errors.isEmpty()) {
            responseBuilder.addAllError(errors);
            log.warn("DeleteUser encountered errors: {}", errors);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
        log.info("DeleteUser processed: deleted={}, errors={}", deleted.size(), errors.size());
    }
}
