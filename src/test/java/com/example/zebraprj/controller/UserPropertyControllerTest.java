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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
public class UserPropertyControllerTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.5");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
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
