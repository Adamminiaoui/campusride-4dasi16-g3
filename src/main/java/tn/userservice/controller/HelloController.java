package tn.userservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "user-service OK";
    }
}