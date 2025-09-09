package com.example.zebraprj;

import com.example.zebraprj.model.User;
import com.example.zebraprj.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Tag(name = "ZebraAPI", description = "API for Zebra application")
public class ZebraPrjController {

    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(ZebraPrjController.class);

    public ZebraPrjController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get Hello message", description = "Return simple hardcoded message 'Hello'")
    @ApiResponses(value = {
            @ApiResponse (responseCode = "200", description = "Successfully retrieved greeting"),
            @ApiResponse (responseCode = "500", description = "=( Internal server error =(")
    })
    public String sayHello(){
        log.info("sayHello called");
        return "Hello";
    }

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get list of users",
            description = "Return list of users with their parameters (id, name, email, birthdate)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users"),
            @ApiResponse(responseCode = "500", description = "=( Internal server error =(")
    })
    public List<User> getUsers(){
        log.info("getUsers call. Fetching all users");
        return userRepository.findAll();
    }

    @PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add new users", description = "Add one or more users to the database, checking for unique name and email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User(s) added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "User with this name or email already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> addUsers(@Valid @RequestBody List<User> users){
        log.info("addUsers call. Request to add {} users", users.size());
        List<String> errors = new ArrayList<>();
        List<User> addedUsers = new ArrayList<>();

        for (User user:users){
            if(userRepository.existsByName(user.getName())){
                String msg = "User with name " + user.getName() + " already exists";
                log.warn(msg);
                errors.add(msg);
                continue;
            }
            if(userRepository.existsByEmail(user.getEmail())){
                String msg = "User with email " + user.getEmail() + " already exists";
                log.warn(msg);
                errors.add(msg);
                continue;
            }
            User saved = userRepository.save(user);
            log.info("User with id {} saved successfully", saved);
            addedUsers.add(saved);
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("addedUsers", addedUsers);
        if(!errors.isEmpty()){
            response.put("errors", errors);
            log.warn("User creation finished with errors: {}", errors);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        log.info("Successfully added {} users", addedUsers.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/deleteuser/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete user by ID", description = "Deletes a single user based on their ID")
    @ApiResponses(value ={
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, Object>> getToDeleteUserById(@PathVariable Long id){
        log.info("getToDeleteUserById call. Deleting user with id {} via GET", id);
        if(!userRepository.existsById(id)){
            log.warn("User with id {} not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error","User with ID " + id + " not found"));
        }
        userRepository.deleteById(id);
        log.info("User with id {} deleted", id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("message", "User with ID " + id + " deleted successfully"));
    }

    @PostMapping(value = "/deleteuser", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete user(s) by ID or Name", description = "Deletes one or more users by ID or Name")
    @ApiResponses(value ={
            @ApiResponse(responseCode = "200", description = "User(s) deleted successfully"),
            @ApiResponse(responseCode = "404", description = "One or more users not found")
    })
    public ResponseEntity<Map<String, Object>> postToDeleteUserByNameId(@RequestBody List<Map<String, Object>> requestList) {
        log.info("postToDeleteUserByNameId call. Deleting users via POST request: {}", requestList);
        List<String> deleted = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for(Map<String, Object> entity : requestList) {
            if (entity.containsKey("id")) {
                Long id = Long.valueOf(entity.get("id").toString());
                if(userRepository.existsById(id)){
                    userRepository.deleteById(id);
                    deleted.add("Deleted user with ID: " + id);
                    log.info("Deleted user with id {}", id);
                } else {
                    String message = "User with ID " + id + " not found";
                    log.warn(message);
                    errors.add(message);
                }

            } else if(entity.containsKey("name")){
                String name = entity.get("name").toString();
                List<User> userByName = userRepository.findByName(name);
                if(!userByName.isEmpty()){
                    userRepository.deleteAll(userByName);
                    deleted.add("Deleted user(s) with name '" + name + "'");
                    log.info("Deleted user(s) with name {}", name);
                } else {
                    String message = "User(s) '" + name + "' not found";
                    log.warn(message);
                    errors.add(message);
                }
            } else {
                String message = "Invalid request object: " + entity;
                log.warn(message);
                errors.add(message);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("deleted", deleted);
        if(!errors.isEmpty()){
            response.put("errors", errors);
            log.warn("Deletion via POST encountered errors: {}", errors);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        log.info("Deletion via POST succeeded for {} entries", deleted.size());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete user by ID", description = "Delete a user using query param ?id=xx or JSON body with {id:xx}")
    @ApiResponses(value ={
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Param ID is not present in request"),
            @ApiResponse(responseCode = "404", description = "Users not found")
    })
    public ResponseEntity<Map<String, Object>> deleteUser (
            @RequestParam(required = false) Long id,
            @RequestBody(required = false) List<Map<String, Object>> requestList) {
        log.info("deleteUser call. DELETE /users called with id={} and body={} ", id, requestList);
        List<String> deleted = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // If query param used
        if(id != null){
            if(userRepository.existsById(id)){
                userRepository.deleteById(id);
                deleted.add("User with ID " + id + " deleted successfully (query)");
                log.info("Deleted user with id {} via query param", id);
            } else {
                String message = "User with ID " + id + " not found (query)";
                log.warn(message);
                errors.add(message);
            }
        }

        // If ID not given as query param, try extracting from body list
         if (id == null && requestList != null && !requestList.isEmpty()) {
            for(Map<String, Object> entity : requestList) {
                if (entity.containsKey("id")) {
                     Long userId = Long.valueOf(entity.get("id").toString());
                    if(userRepository.existsById(userId)){
                        userRepository.deleteById(userId);
                        deleted.add("User with ID " + userId + " deleted successfully (body)");
                        log.info("Deleted user with id {} via body", userId);
                    } else {
                        String message = "User with ID " + userId + " not found (body)";
                        log.warn(message);
                        errors.add(message);
                    }
                }
            }
        }

        // If ID not found in query nor body
        if(errors.isEmpty() && deleted.isEmpty()){
            log.warn("DELETE /users missing 'id' in query param or body");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(Map.of("incorrectRequest","Missing 'id' in query param or request body"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("deleted", deleted);
        if(!errors.isEmpty()){
            response.put("errors", errors);
            log.warn("DELETE /users completed with errors: {}", errors);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        log.info("DELETE /users removed {} users", deleted.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/crazy", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Crazy GET endpoint with body parameters",
            description = "Accepts JSON body on a GET request. If 'name' is provided → returns user(s) with that name. " +
                    "If 'id' is provided → deletes the user with that ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, Object>> crazyGet(@RequestBody(required = false) Map<String, Object> params) {
        log.info("crazyGet called with params {}", params);
        if (params == null || params.isEmpty()) {
            log.warn("crazyGet missing required parameters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Request body must contain 'name' or 'id'"));
        }

        Map<String, Object> response = new HashMap<>();

        // Case 1: Find by name
        if (params.containsKey("name")) {
            String name = params.get("name").toString();
            List<User> users = userRepository.findByName(name);
            if (users.isEmpty()) {
                log.warn("No user found with name {}", name);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No user found with name '" + name + "'"));
            }
            response.put("foundUsers", users);
            log.info("Found {} users with name {}", users.size(), name);
            return ResponseEntity.ok(response);
        }

        // Case 2: Delete by ID
        if (params.containsKey("id")) {
            try {
                Long id = Long.valueOf(params.get("id").toString());
                if (!userRepository.existsById(id)) {
                    log.warn("User with id {} not found", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "User with ID " + id + " not found"));
                }
                userRepository.deleteById(id);
                response.put("message", "User with ID " + id + " deleted successfully");
                log.info("Deleted user with id {} via crazyGet", id);
                return ResponseEntity.ok(response);
            } catch (NumberFormatException e) {
                log.warn("Invalid id format in crazyGet: {}", params.get("id"));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "'id' must be a valid number"));
            }
        }

        log.warn("crazyGet request missing both 'name' and 'id'");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Request body must contain either 'name' or 'id'"));
    }
}
