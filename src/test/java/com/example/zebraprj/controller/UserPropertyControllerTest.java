package com.example.zebraprj.controller;

import com.example.zebraprj.model.UserProperty;
import com.example.zebraprj.repository.UserPropertyRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "grpc.server.port=0",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class UserPropertyControllerTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.5");

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")


    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserPropertyRepository repository;

    @Test
    @Tag("Positive")
    @Tag("Mongo")
    void postUserPropertyPersistInMongoDB() throws Exception {
        String body = "{\"userId\":\"42\",\"address\":\"Main\",\"organisation\":\"Org\",\"favouriteColour\":\"green\"}";

        mockMvc.perform(post("/userproperty")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("42"));

        UserProperty saved = repository.findById("42").orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getAddress()).isEqualTo("Main");
        assertThat(saved.getFavouriteColour()).isEqualTo("green");
        assertThat(saved.getOrganisation()).isEqualTo("Org");

    }
}
