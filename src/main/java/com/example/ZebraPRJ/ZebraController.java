package com.example.ZebraPRJ;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ZebraController {
    @GetMapping("/hello")
    public String sayHello(){
        return "Hello";
    }
}
