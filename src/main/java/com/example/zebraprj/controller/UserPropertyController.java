package com.example.zebraprj.controller;

import com.example.zebraprj.model.UserProperty;
import com.example.zebraprj.repository.UserPropertyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "UserProperty", description = "API for managing user properties stored in MongoDB")
public class UserPropertyController {

    private final UserPropertyRepository userPropertyRepository;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(UserPropertyController.class);

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
        log.info("Received request to add user property: {}", body);
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
            log.info("Saved {} user properties", saved.size());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request body", e);
            return ResponseEntity.badRequest().body("Invalid request body");
        }
    }

    @GetMapping(value = "/userproperty", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user properties", description = "Return all user properties or one by userId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User property(ies) retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User property not found")
    })
    public ResponseEntity<?> getUserProperty(@RequestParam(required = false) String userId){
        log.info("Received request to get user property: {}", userId);
        if(userId != null){
            return userPropertyRepository.findById(userId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User property with ID " + userId + " not found")));
        }
        List<UserProperty> all = userPropertyRepository.findAll();
        log.info("Found {} user properties", all.size());
        return ResponseEntity.ok(all);
    }

    @DeleteMapping(value = "/userproperty", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete user property", description = "Delete user property by userId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User property deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User property not found")
    })
    public ResponseEntity<Map<String, String>> deleteUserProperty(@RequestParam String userId) {
        log.info("Received request to delete user property with userId: {}", userId);
        if (!userPropertyRepository.existsById(userId)) {
            log.warn("UserProperty with userId {} does not exist", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).
                    body(Map.of("error", "UserProperty with id: " + userId + " not found"));
        }
        userPropertyRepository.deleteById(userId);
        log.info("UserProperty with userId {} deleted", userId);
        return ResponseEntity.ok(Map.of("message", "UserProperty with id: " + userId + " deleted successfully"));
    }
}
