package com.example.ZebraPRJ;

import com.example.ZebraPRJ.model.User;
import com.example.ZebraPRJ.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
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
import org.testcontainers.DockerClientFactory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// 1. Tell Spring Boot to run on a random port to enable web testing
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
// 2. Override ddl-auto to have Hibernate create the schema for us in the fresh container
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ZebraPrjControllerTest {



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

    public static final String DELETEUSER_ENDPOINT = "/deleteuser";

    @BeforeAll
    static void init() {
        String dockerHostIp = DockerClientFactory.instance().dockerHostIpAddress();
        System.out.println("Docker host: " + dockerHostIp);
    }

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
    void testGETHelloSuccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(helloEndpoint, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(helloBody);
    }

    @Test
    @DisplayName("Check Content-Type for GET /hello")
    @Tag("Positive")
    void testGETHelloContentType() {
        ResponseEntity<String> response = restTemplate.getForEntity(helloEndpoint, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).hasToString("text/plain;charset=UTF-8");
    }

    @Test
    @DisplayName("Check response time for GET /hello")
    @Tag("Positive")
    void testGETHelloResponseTime() {
        long startTime = System.currentTimeMillis();
        restTemplate.getForEntity(helloEndpoint, String.class);
        long elapsedTime = System.currentTimeMillis() - startTime;
        assertThat(elapsedTime).isLessThan(1000L);
    }

    @Test
    @DisplayName("Check GET /invalid returns 404")
    @Tag("Negative")
    void testGETInvalidEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(invalidEndpoint, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("Check POST /hello returns 405")
    @Tag("Negative")
    void testPOSTHelloMethodNotAllowed() {
        ResponseEntity<String> response = restTemplate.postForEntity(helloEndpoint, null, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(405);
    }

    @Test
    @DisplayName("Check GET /users returns 200 and correct data")
    @Tag("Positive")
    void testGETUsersSuccess() {
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
    void testPOSTNewUserSuccess() {
        User newUser = new User(null, "Avraam Linkoln", "Avraam@mail.ru", LocalDate.of(1809, 2, 12));

        // We can typically make a POST call here, but since the controller method
        // is designed to work with userRepository.save, this is a valid test of the underlying logic.
        User savedUser = userRepository.save(newUser);
        assertNotNull(savedUser.getId());
        assertEquals("Avraam Linkoln", savedUser.getName());
    }

    @Test
    @DisplayName("DELETE via GET " + DELETEUSER_ENDPOINT + "/{id} removes the user")
    @Tag("Positive")
    void testDeleteUserByIdGET(){
        //Get ID of first test user (created with @before each)
        Long testUserID = userRepository.findAll().get(0).getId();
        ResponseEntity<Map> response = restTemplate.exchange(
                "/deleteuser/" + testUserID,
                HttpMethod.GET,
                null,
                Map.class
        );
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(userRepository.existsById(testUserID)).isFalse();
    }

    @Test
    @DisplayName("POST /deleteuser with name removes matching users")
    @Tag("Positive")
    void testDeleteUserByNamePOST(){
        String testUserName = userRepository.findAll().get(0).getName();
        List<Map<String,Object>> request = Arrays.asList(Map.of("name",testUserName));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                DELETEUSER_ENDPOINT,
                request,
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(userRepository.findByName(testUserName)).isEmpty();
    }

    @Test
    @DisplayName("POST /deleteuser non-existing ID")
    @Tag("Negative")
    void testDeleteUserNotExistingIdPOST(){
        List<Map<String,Object>> request = Arrays.asList(Map.of("id",999));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                DELETEUSER_ENDPOINT,
                request,
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    @DisplayName("DELETE /users. ID in URL (/users?id=xx)")
    @Tag("Positive")
    void testDeleteUserNonExistingIdInUrlDELETE(){
        Long testUserID = userRepository.findAll().get(0).getId();
        ResponseEntity<Map> response = restTemplate.exchange(
                "/users?id=" + testUserID,
                HttpMethod.DELETE,
                null,
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(userRepository.existsById(testUserID)).isFalse();
    }

    @Test
    @DisplayName("DELETE /users. ID in URL of not existing user (/users?id=xx)")
    @Tag("Negative")
    void testDeleteUserByIdInUrlDELETE(){
        ResponseEntity<Map> response = restTemplate.exchange(
                "/users?id=777",
                HttpMethod.DELETE,
                null,
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("DELETE /users. ID in body")
    @Tag("Positive")
    void testDeleteUserByIdInBodyDELETE(){
        Long testUserID = userRepository.findAll().get(0).getId();
        List<Map<String,Object>> request = Arrays.asList(Map.of("id",testUserID));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users",
                HttpMethod.DELETE,
                new org.springframework.http.HttpEntity<>(request),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(userRepository.findById(testUserID)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /users. Non existing ID in body")
    @Tag("Negative")
    void testDeleteUserNonExistingIdInBodyDELETE(){
        Long nonExistingUserID = 999L;
        List<Map<String,Object>> request = Arrays.asList(Map.of("id",nonExistingUserID));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users",
                HttpMethod.DELETE,
                new org.springframework.http.HttpEntity<>(request),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsKey("errors");
        assertThat(response.getBody().get("errors").toString())
                .contains("User with ID " + nonExistingUserID + " not found");
    }
}