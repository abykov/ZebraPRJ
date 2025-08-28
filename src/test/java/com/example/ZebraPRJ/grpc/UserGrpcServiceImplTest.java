package com.example.ZebraPRJ.grpc;

import com.example.ZebraPRJ.model.User;
import com.example.ZebraPRJ.AbstractPostgresTest;
import com.example.ZebraPRJ.repository.UserRepository;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class UserGrpcServiceImplTest extends AbstractPostgresTest {

    @Autowired
    private UserRepository userRepository;

    private ManagedChannel channel;
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        stub = UserGrpcServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
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
                .setEmail("u2@example.com")
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

}
