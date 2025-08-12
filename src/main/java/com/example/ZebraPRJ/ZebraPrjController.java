package com.example.ZebraPRJ;

import com.example.ZebraPRJ.model.User;
import com.example.ZebraPRJ.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Tag(name = "ZebraAPI", description = "API for Zebra application")
public class ZebraPrjController {

    private final UserRepository userRepository;

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
        List<String> errors = new ArrayList<>();
        List<User> addedUsers = new ArrayList<>();

        for (User user:users){
            if(userRepository.existsByName(user.getName())){
                errors.add("User with name '" + user.getName() + "' is already registered");
                continue;
            }
            if(userRepository.existsByEmail(user.getEmail())){
                errors.add("User with email '" + user.getEmail() + "' is already registered");
                continue;
            }
            addedUsers.add(userRepository.save(user));
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("addedUsers", addedUsers);
        if(!errors.isEmpty()){
            response.put("errors", errors);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/deleteuser/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete user by ID", description = "Deletes a single user based on their ID")
    @ApiResponses(value ={
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, Object>> getToDeleteUserById(@PathVariable Long id){
        if(!userRepository.existsById(id)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error","User with ID " + id + " not found"));
        }
        userRepository.deleteById(id);
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
        List<String> deleted = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for(Map<String, Object> entity : requestList) {
            if (entity.containsKey("id")) {
                Long id = Long.valueOf(entity.get("id").toString());
                if(userRepository.existsById(id)){
                    userRepository.deleteById(id);
                    deleted.add("Deleted user with ID: " + id);
                } else {
                    errors.add("User with ID " + id + " not found");
                }

            } else if(entity.containsKey("name")){
                String name = entity.get("name").toString();
                List<User> userByName = userRepository.findByName(name);
                if(!userByName.isEmpty()){
                    userRepository.deleteAll(userByName);
                    deleted.add("Deleted user(s) with name '" + name + "'");
                } else {
                    errors.add("User(s) '" + name + "' not found");
                }
            } else {
                errors.add("Invalid request object: " + entity);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("deleted", deleted);
        if(!errors.isEmpty()){
            response.put("errors", errors);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete user by ID", description = "Delete a user using query param ?id=xx or JSON body with {id:xx}")
    @ApiResponses(value ={
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Param ID is not present in request"),
            @ApiResponse(responseCode = "404", description = "Users not found")
    })
    public ResponseEntity<Map<String, Object>> deleteUser (
            @RequestParam(required = false) Long id,
            @RequestBody(required = false) List<Map<String, Object>> requestList) {
        List<String> deleted = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // If query param used
        if(id != null){
            if(userRepository.existsById(id)){
                userRepository.deleteById(id);
                deleted.add("User with ID " + id + " deleted successfully (query)");
            } else {
                errors.add("User with ID " + id + " not found (query)");
            }
        }

        // If ID not given as query param, try extracting from body list
         if (id == null && requestList != null && !requestList.isEmpty()) {
            for(Map<String, Object> entity : requestList) {
                if (entity.containsKey("id")) {
                     Long UserId = Long.valueOf(entity.get("id").toString());
                    if(userRepository.existsById(UserId)){
                        userRepository.deleteById(UserId);
                        deleted.add("User with ID " + UserId + " deleted successfully (body)");
                    } else {
                        errors.add("User with ID " + UserId + " not found (body)");
                    }
                }
            }
        }

        // If ID not found in query nor body
        if(errors.isEmpty() && deleted.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(Map.of("incorrectRequest","Missing 'id' in query param or request body"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("deleted", deleted);
        if(!errors.isEmpty()){
            response.put("errors", errors);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
