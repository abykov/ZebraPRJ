package com.example.zebraprj.controller;

import com.example.zebraprj.model.UserProperty;
import com.example.zebraprj.repository.UserPropertyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Tag(name = "UserProperty", description = "API for managing user properties stored in MongoDB")
public class UserPropertyController {

    private final UserPropertyRepository userPropertyRepository;
    private final ObjectMapper objectMapper;

    public UserPropertyController(UserPropertyRepository userPropertyRepository, ObjectMapper objectMapper) {
        this.userPropertyRepository = userPropertyRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/userproperty", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add user properties", description = "Add one or more user properties and persist them in MongoDB")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User property(ies) added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> addUserProperty(@RequestBody Object body) {
        try{
            List<UserProperty> toSave = new ArrayList<>();
            if (body instanceof List) {
                CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, UserProperty.class);
                toSave.addAll(objectMapper.convertValue(body, listType));
            } else {
                toSave.add(objectMapper.convertValue(body, UserProperty.class));
            }
            List<UserProperty> saved = new ArrayList<>();
            for (UserProperty property : toSave) {
                saved.add(userPropertyRepository.save(property));
            }
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }
    }

}
