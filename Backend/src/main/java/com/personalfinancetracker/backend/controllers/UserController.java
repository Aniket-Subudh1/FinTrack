package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.UserDetailsResponse;
import com.personalfinancetracker.backend.dto.UserDetailsUpdateRequest;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @GetMapping("/details")
    public ResponseEntity<UserDetailsResponse> getUserDetails(Authentication authentication) {
        logger.info("Received request for /api/user/details");
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching user details for email: {}", email);

        try {
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found for email: {}", email);
                        return new RuntimeException("User not found");
                    });

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
        } catch (Exception e) {
            logger.error("Error fetching user details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Boolean>> checkAuth(Authentication authentication) {
        logger.info("Received request for /api/user/check-auth");
        Map<String, Boolean> response = new HashMap<>();
        String email = getEmailFromJwtCookie();
        boolean isAuthenticated = (email != null);

        if (isAuthenticated) {
            logger.debug("User authenticated with email: {}", email);
        } else {
            logger.warn("User not authenticated - no valid JWT token found");
        }

        response.put("authenticated", isAuthenticated);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/details")
    public ResponseEntity<UserDetailsResponse> updateUserDetails(
            @RequestBody UserDetailsUpdateRequest updateRequest, Authentication authentication) {
        logger.info("Received request for /api/user/details (PUT)");
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for update request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new UserDetailsResponse());
        }

        logger.info("Updating user details for email: {}", email);

        try {
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found for email (update): {}", email);
                        return new RuntimeException("User not found");
                    });

            if (updateRequest.getAddress() != null) customer.setAddress(updateRequest.getAddress());
            if (updateRequest.getGender() != null) customer.setGender(updateRequest.getGender());
            if (updateRequest.getAge() != null) customer.setAge(updateRequest.getAge());
            if (updateRequest.getProfilePhoto() != null && !updateRequest.getProfilePhoto().isEmpty()) {
                try {
                    byte[] photoBytes = Base64.getDecoder().decode(updateRequest.getProfilePhoto());
                    customer.setProfilePhoto(photoBytes);
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid Base64 format for profile photo: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UserDetailsResponse());
                }
            }

            customerRepository.save(customer);
            logger.info("User details updated successfully for: {}", email);

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
        } catch (Exception e) {
            logger.error("Error updating user details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new UserDetailsResponse());
        }
    }

    private String getEmailFromJwtCookie() {
        String jwt = jwtUtil.getJwtFromCookies(request);
        if (jwt != null) {
            try {
                String email = jwtUtil.extractUsername(jwt);
                if (email != null && !email.isEmpty()) {
                    logger.debug("Extracted email from JWT: {}", email);
                    return email;
                } else {
                    logger.warn("JWT token contains no valid username");
                }
            } catch (Exception e) {
                logger.error("Error extracting email from JWT: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("No JWT token found in cookies");
        }
        return null;
    }
}