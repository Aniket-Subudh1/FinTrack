package com.personalfinancetracker.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.personalfinancetracker.backend.services.auth.AuthenticationService;

@RestController
@RequestMapping("/logout")
public class LogoutController {
    private static final Logger logger = LoggerFactory.getLogger(LogoutController.class);

    private final AuthenticationService authenticationService;

    public LogoutController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        try {
            logger.info("Processing logout request");
            authenticationService.clearTokenCookie(response);
            SecurityContextHolder.clearContext();

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Logout successful");
            logger.info("Logout successful, cookie cleared");
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            logger.error("Error during logout", e);
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", STR."Logout failed: \{e.getMessage()}");
            return ResponseEntity.status(500).body(errorBody);
        }
    }
}