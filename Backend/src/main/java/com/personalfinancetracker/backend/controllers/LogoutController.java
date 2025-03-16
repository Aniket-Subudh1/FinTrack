package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.services.auth.AuthenticationService;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/logout")
public class LogoutController {

    private final AuthenticationService authenticationService;

    @Autowired
    public LogoutController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        // Clear JWT cookie
        authenticationService.clearTokenCookie(response);

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "Logout successful");

        return ResponseEntity.ok(responseBody);
    }
}