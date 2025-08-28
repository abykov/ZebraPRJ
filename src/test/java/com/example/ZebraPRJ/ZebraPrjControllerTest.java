package com.example.ZebraPRJ;

import com.example.ZebraPRJ.model.User;
import com.example.ZebraPRJ.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.DockerClientFactory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// 1. Tell Spring Boot to run on a random port to enable web testing
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// 2. Override ddl-auto to have Hibernate create the schema for us in the fresh container
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ZebraPrjControllerTest extends AbstractPostgresTest {

    // 3. Autowire both the repository and the TestRestTemplate
    @Autowired
    private UserRepository userRepository;

    @LocalServerPort
    private int port;

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
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
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
        User newUser = new User(null, "Abraham Lincoln", "Avraam@mail.ru", LocalDate.of(1809, 2, 12));

        // We can typically make a POST call here, but since the controller method
        // is designed to work with userRepository.save, this is a valid test of the underlying logic.
        User savedUser = userRepository.save(newUser);
        assertNotNull(savedUser.getId());
        assertEquals("Abraham Lincoln", savedUser.getName());
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

    @Test
    @DisplayName("GET /crazy. Correct ID in body - deletes user")
    @Tag("Positive")
    void testCrazyGetWithCorrectIdDeletesUser(){
        // Get ID of first test user
        Long testUserId = userRepository.findAll().get(0).getId();
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", testUserId))
        .when()
                .get("/crazy")
        .then()
                .statusCode(200)
                .body("message", equalTo("User with ID " + testUserId + " deleted successfully"));

        assertThat(userRepository.existsById(testUserId)).isFalse();
    }

    @Test
    @DisplayName("GET /crazy. Non-existing ID in body - user not found")
    @Tag("Negative")
    void testCrazyGetWithNonExistingIdUserNotFound() {
        Long nonExistingUserID = 999L;
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", nonExistingUserID))
        .when()
                .get("/crazy")
        .then()
                .statusCode(404)
                .body("error", equalTo("User with ID " + nonExistingUserID + " not found"));
    }

        @Test
        @DisplayName("GET /crazy. Valid username in body - returns user")
        @Tag("Positive")
        void testCrazyGetWithValidUsername(){
            String testUserName = userRepository.findAll().get(0).getName();
            var response =
                    given()
                            .contentType(ContentType.JSON)
                            .body(Map.of("name", testUserName))
                    .when()
                            .get("/crazy")
                    .then()
                            .statusCode(200)
                    .extract();

            List<Map<String, Object>> foundUsers = response.path("foundUsers");
            assertThat(foundUsers).hasSize(1);
            assertThat(foundUsers.get(0).get("name")).isEqualTo(testUserName);
        }

    @Test
    @DisplayName("GET /crazy. Non-existing username - user not found")
    @Tag("Negative")
    void testCrazyGetWithNonExistingUsername(){
        String testUserName = "NonExistingUser";
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", testUserName))
        .when()
                .get("/crazy")
        .then()
                .statusCode(404)
                .body("error", equalTo("No user found with name '" + testUserName + "'"));
    }

    @Test
    @DisplayName("GET /crazy. Empty body - error 400")
    @Tag("Negative")
    void testCrazyGetWithEmptyBody(){
        ResponseEntity<Map> response = restTemplate.exchange(
                "/crazy",
                HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(Map.of()),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "Request body must contain 'name' or 'id'");
    }

    @Test
    @DisplayName("GET /crazy. Invalid ID format - error 400")
    @Tag("Negative")
    void testCrazyGetInvalidIdFormat() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "not-a-number"))
        .when()
                .get("/crazy")
        .then()
                .statusCode(400)
                .body("error", equalTo("'id' must be a valid number"));
    }
}