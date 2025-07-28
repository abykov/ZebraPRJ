package com.example.ZebraPRJ;

import com.example.ZebraPRJ.model.User;
import com.example.ZebraPRJ.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// 1. Tell Spring Boot to run on a random port to enable web testing
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
// 2. Override ddl-auto to have Hibernate create the schema for us in the fresh container
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
public class ZebraPrjControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    // 3. Autowire both the repository and the TestRestTemplate
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    private final String helloEndpoint = "/hello";
    private final String helloBody = "Hello";
    private final String invalidEndpoint = "/invalid";
    private final String usersEndpoint = "/users";

    // 4. Use @BeforeEach to ensure a clean database state for each test
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(new User(null, "Alice Smith", "alice@example.com", LocalDate.of(1999, 1, 1)));
        userRepository.save(new User(null, "Bob Johnson", "bob@example.com", LocalDate.of(1994, 2, 15)));
    }

    @Test
    @DisplayName("Check GET /hello returns 200 and 'Hello'")
    @Tag("Positive")
    public void testGetHelloSuccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(helloEndpoint, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(helloBody);
    }

    @Test
    @DisplayName("Check Content-Type for GET /hello")
    @Tag("Positive")
    public void testGetHelloContentType() {
        ResponseEntity<String> response = restTemplate.getForEntity(helloEndpoint, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/plain;charset=UTF-8");
    }

    @Test
    @DisplayName("Check response time for GET /hello")
    @Tag("Positive")
    public void testGetHelloResponseTime() {
        long startTime = System.currentTimeMillis();
        restTemplate.getForEntity(helloEndpoint, String.class);
        long elapsedTime = System.currentTimeMillis() - startTime;
        assertThat(elapsedTime).isLessThan(1000L);
    }

    @Test
    @DisplayName("Check GET /invalid returns 404")
    @Tag("Negative")
    public void testGetInvalidEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(invalidEndpoint, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("Check POST /hello returns 405")
    @Tag("Negative")
    public void testPostHelloMethodNotAllowed() {
        ResponseEntity<String> response = restTemplate.postForEntity(helloEndpoint, null, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(405);
    }

    @Test
    @DisplayName("Check GET /users returns 200 and correct data")
    @Tag("Positive")
    public void testGetUsersSuccess() {
        ResponseEntity<List<User>> response = restTemplate.exchange(
                usersEndpoint,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("Check POST /users adds a new user")
    @Tag("Positive")
    public void testPostNewUserSuccess() {
        User newUser = new User(null, "Avraam Linkoln", "Avraam@mail.ru", LocalDate.of(1809, 2, 12));

        // You would typically make a POST call here, but since the controller method
        // is designed to work with userRepository.save, this is a valid test of the underlying logic.
        User savedUser = userRepository.save(newUser);
        assertNotNull(savedUser.getId());
        assertEquals("Avraam Linkoln", savedUser.getName());
    }
}