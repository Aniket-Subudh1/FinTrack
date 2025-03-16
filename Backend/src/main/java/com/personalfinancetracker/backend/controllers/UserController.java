package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.UserDetailsResponse;
import com.personalfinancetracker.backend.dto.UserDetailsUpdateRequest;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/details")
    public ResponseEntity<UserDetailsResponse> getUserDetails(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetailsResponse response = new UserDetailsResponse();
        response.setName(customer.getName());
        response.setEmail(customer.getEmail());
        response.setAddress(customer.getAddress());
        response.setGender(customer.getGender());
        response.setAge(customer.getAge());
        if (customer.getProfilePhoto() != null) {
            response.setProfilePhoto(Base64.getEncoder().encodeToString(customer.getProfilePhoto()));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Boolean>> checkAuth(Authentication authentication) {
        Map<String, Boolean> response = new HashMap<>();
        boolean isAuthenticated = (authentication != null && authentication.isAuthenticated());
        response.put("authenticated", isAuthenticated);

        return ResponseEntity.ok(response);
    }
    @PutMapping("/details")
    public ResponseEntity<UserDetailsResponse> updateUserDetails(
            @RequestBody UserDetailsUpdateRequest updateRequest, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new UserDetailsResponse());
        }

        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        customer.setAddress(updateRequest.getAddress());
        customer.setGender(updateRequest.getGender());
        customer.setAge(updateRequest.getAge());
        if (updateRequest.getProfilePhoto() != null) {
            byte[] photoBytes = Base64.getDecoder().decode(updateRequest.getProfilePhoto());
            customer.setProfilePhoto(photoBytes);
        }

        customerRepository.save(customer);

        return ResponseEntity.ok(new UserDetailsResponse());
    }
}
