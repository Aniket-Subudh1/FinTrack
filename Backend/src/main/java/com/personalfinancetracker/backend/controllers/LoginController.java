package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.LoginRequest;
import com.personalfinancetracker.backend.dto.LoginResponse;
import com.personalfinancetracker.backend.services.auth.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final AuthenticationManager authenticationManager;
    private final AuthenticationService authenticationService;

    @Autowired
    public LoginController(AuthenticationManager authenticationManager,
                           AuthenticationService authenticationService) {
        this.authenticationManager = authenticationManager;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,
                                   HttpServletResponse response) {
        logger.info("Login attempt for email: {}", loginRequest.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            if (authentication.isAuthenticated()) {
                UserDetails userDetails = authenticationService.loadUserByUsername(loginRequest.getEmail());

                // Generate JWT token and add to cookie - use EMAIL, not ID
                String email = userDetails.getUsername();
                authenticationService.addTokenCookie(response, email);

                // Generate token for backward compatibility
                String jwt = authenticationService.generateToken(email);

                logger.info("Login successful for: {}", loginRequest.getEmail());
                return ResponseEntity.ok(new LoginResponse(jwt));
            } else {
                logger.warn("Authentication failed for: {}", loginRequest.getEmail());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (BadCredentialsException e) {
            logger.warn("Bad credentials for: {}", loginRequest.getEmail());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (UsernameNotFoundException e) {
            logger.warn("User not found: {}", loginRequest.getEmail());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (AuthenticationException e) {
            logger.error("Authentication error for {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during login for {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}