package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.SignupRequest;
import com.personalfinancetracker.backend.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/signup")
public class SignupController {
    private final AuthService authService;
   @Autowired
    public SignupController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping
    public ResponseEntity<String> signupCustomer(@RequestBody SignupRequest signupRequest) {
        boolean isUserCreated = authService.createCustomer(signupRequest);
        if (isUserCreated) {
            return  ResponseEntity.status( HttpStatus.CREATED).body("Customer created successfully");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Customer creation failed");
        }
}
}
