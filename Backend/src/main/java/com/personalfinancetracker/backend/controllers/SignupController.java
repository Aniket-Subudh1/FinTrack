package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.SignupRequest;
import com.personalfinancetracker.backend.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Import Map and HashMap
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/signup")
public class SignupController {
    private final AuthService authService;

    @Autowired
    public SignupController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> signupCustomer(@RequestBody SignupRequest signupRequest) {
        boolean isUserCreated = authService.createCustomer(signupRequest);
        Map<String, String> response = new HashMap<>();
        if (isUserCreated) {
            response.put("message", "Customer created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("message", "Customer creation failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
