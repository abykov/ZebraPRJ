package com.example.ZebraPRJ;

import com.example.ZebraPRJ.model.User;
import com.example.ZebraPRJ.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
