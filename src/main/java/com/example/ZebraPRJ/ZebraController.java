package com.example.ZebraPRJ;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "ZebraAPI", description = "API for Zebra application")
public class ZebraController {
    @GetMapping("/hello")
    @Operation(summary = "Get Hello message", description = "Return simple hardcoded message 'Hello'")
    @ApiResponses(value = {
            @ApiResponse (responseCode = "200", description = "Successfully retrieved greeting"),
            @ApiResponse (responseCode = "500", description = "=( Internal server error =(")
    })
    public String sayHello(){
        return "Hello";
    }
}
