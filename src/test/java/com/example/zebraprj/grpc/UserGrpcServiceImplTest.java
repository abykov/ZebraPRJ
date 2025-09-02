package com.example.zebraprj.grpc;

import com.example.zebraprj.model.User;
import com.example.zebraprj.AbstractPostgresTest;
import com.example.zebraprj.repository.UserRepository;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "grpc.server.in-process-name=test",
        "grpc.server.port=-1",
        "grpc.client.test.address=in-process:test"
})
public class UserGrpcServiceImplTest extends AbstractPostgresTest {

    @Autowired
    private UserRepository userRepository;

    @GrpcClient("test")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("gRPC addUser saves and returns user")
    @Tag("GRPc")
    @Tag("Positive")
    public void testAddUserSuccess() {
        UserMessage userMessage = UserMessage.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .setBirthdate("2000-01-01")
                .build();

        AddUserRequest request = AddUserRequest.newBuilder().setUser(userMessage).build();
        AddUserResponse response = stub.addUser(request);

        assertTrue(response.hasUser());
        assertNotNull(response.getUser().getId());
        assertEquals("John Doe", response.getUser().getName());
        assertTrue(response.getErrorList().isEmpty());
    }

    @Test
    @DisplayName("gRPC addUser assigns unique ids for multiple users")
    @Tag("GRPc")
    @Tag("Positive")
    public void testAddMultipleUserSuccess() {
        AddUserResponse first = stub.addUser(AddUserRequest.newBuilder().setUser(
                UserMessage.newBuilder()
                        .setName("User1")
                        .setEmail("u1@example.com")
                        .setBirthdate("1990-01-01")
                        .build()
        ).build());

        AddUserResponse second = stub.addUser(AddUserRequest.newBuilder().setUser(
                UserMessage.newBuilder()
                        .setName("User2")
                        .setEmail("u2@example.com")
                        .setBirthdate("1992-02-02")
                        .build()
        ).build());

        assertNotEquals(first.getUser().getId(), second.getUser().getId());
        assertEquals(2,userRepository.count());
    }

    @Test
    @DisplayName("gRPC addUser returns error when email exists")
    @Tag("GRPc")
    @Tag("Negative")
    public void testAddUserDuplicateEmail() {
        userRepository.save(new User(null, "Existing", "exists@example.com", LocalDate.of(1990, 1, 1)));

        UserMessage userMessage = UserMessage.newBuilder()
                .setName("User2")
                .setEmail("exists@example.com")
                .setBirthdate("1992-02-02")
                .build();

        AddUserRequest request = AddUserRequest.newBuilder().setUser(userMessage).build();
        AddUserResponse response = stub.addUser(request);

        assertFalse(response.getErrorList().isEmpty());
        assertFalse(response.hasUser());
        assertEquals(1,userRepository.count());
    }

    @Test
    @DisplayName("gRPC addUser returns error when name exists")
    @Tag("GRPc")
    @Tag("Negative")
    public void testAddUserDuplicateName() {
        userRepository.save(new User(null, "ExistingUser", "exists@example.com", LocalDate.of(1990, 1, 1)));

        UserMessage userMessage = UserMessage.newBuilder()
                .setName("ExistingUser")
                .setEmail("email@example.com")
                .setBirthdate("1993-02-02")
                .build();

        AddUserRequest request = AddUserRequest.newBuilder().setUser(userMessage).build();
        AddUserResponse response = stub.addUser(request);

        assertFalse(response.getErrorList().isEmpty());
        assertFalse(response.hasUser());
        assertEquals(1,userRepository.count());
    }

    @Test
    @DisplayName("gRPC addUser with invalid birthdate throws exception")
    @Tag("GRPc")
    @Tag("Negative")
    public void testAddUserInvalidBirthdate() {
        UserMessage userMessage = UserMessage.newBuilder()
                .setName("User2")
                .setEmail("u2@example.com")
                .setBirthdate("invalid")
                .build();

        AddUserRequest request = AddUserRequest.newBuilder().setUser(userMessage).build();
        assertThrows(StatusRuntimeException.class, () -> stub.addUser(request));
        assertEquals(0,userRepository.count());
    }

    @Test
    @DisplayName("gRPC getUsers returns all saved users")
    @Tag("GRPc")
    @Tag("Positive")
    public void testGetUsers() {
        userRepository.save(new User(null, "User1", "u1@example.com", LocalDate.of(1990, 1, 1)));
        userRepository.save(new User(null, "User2", "u2@example.com", LocalDate.of(1991, 2, 2)));

        GetUsersResponse response = stub.getUsers(GetUsersRequest.newBuilder().build());
        assertEquals(2,userRepository.count());
    }

    @Test
    @DisplayName("gRPC postToDeleteUserByNameId removes users by id and name")
    @Tag("GRPc")
    @Tag("Positive")
    public void testDeleteUserByNameId() {
        User u1 = userRepository.save(new User(null, "User1", "u1@example.com", LocalDate.of(1990, 1, 1)));
        userRepository.save(new User(null, "User2", "u2@example.com", LocalDate.of(1991, 2, 2)));

        DeleteUserByNameIDRequest request = DeleteUserByNameIDRequest.newBuilder()
                .addRequest(DeleteUserRequest.newBuilder().setId(u1.getId()).build())
                .addRequest(DeleteUserRequest.newBuilder().setName("User2").build())
                .build();

        DeleteUserByNameIDResponse response = stub.deleteUserByNameId(request);
        assertEquals(2, response.getDeleteCount());
        assertEquals(0,userRepository.count());
    }

}
