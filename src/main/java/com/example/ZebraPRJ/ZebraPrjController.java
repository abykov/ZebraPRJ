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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
}
