package com.example.ZebraPRJ.grpc;

import com.example.ZebraPRJ.model.User;
import com.example.ZebraPRJ.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;

// gRPC service implementation responsible for user creation
@GrpcService // Registers this class as a gRPC service bean
public class UserGrpcServiceImpl extends  UserGrpcServiceGrpc.UserGrpcServiceImplBase {
    private final UserRepository userRepository; // Repository for DB operations

    public UserGrpcServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository; // Inject repository through constructor
    }

    @Override
    public void addUser(AddUserRequest request, StreamObserver<AddUserResponse> responseObserver) {
        List<String> errors = new ArrayList<>();  // Collect validation errors

        // Validate unique name
        if (userRepository.existsByEmail(request.getUser().getName())) {
            errors.add("User with name " + request.getUser().getName() + "already exists");
        }

        // Validate unique email
        if (userRepository.existsByEmail(request.getUser().getEmail())) {
            errors.add("User with email " + request.getUser().getEmail() + " already exists");
        }

        AddUserResponse.Builder responseBuilder = AddUserResponse.newBuilder(); // Prepare response

        if (!errors.isEmpty()) {
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
    }
}
