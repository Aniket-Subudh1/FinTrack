package com.personalfinance.user.controller;

import com.personalfinance.user.dto.UserDetailsRequest;
import com.personalfinance.user.dto.UserDetailsResponse;
import com.personalfinance.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/details")
    public ResponseEntity<UserDetailsResponse> getUserDetails(
            @RequestHeader("X-User-Id") String email
    ) {
        UserDetailsResponse response = userService.getUserDetails(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/details")
    public ResponseEntity<UserDetailsResponse> updateUserDetails(
            @RequestHeader("X-User-Id") String email,
            @Valid @RequestBody UserDetailsRequest request
    ) {
        UserDetailsResponse response = userService.updateUserDetails(email, request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/internal/create")
    public ResponseEntity<Void> createUser(
            @RequestParam String name,
            @RequestParam String email
    ) {
        userService.createUser(name, email);
        return ResponseEntity.ok().build();
    }
}