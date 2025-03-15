package com.personalfinance.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuth2Controller {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    /**
     * Get OAuth2 client configuration
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getOAuth2Config() {
        return ResponseEntity.ok(Map.of(
                "google", Map.of(
                        "clientId", googleClientId,
                        "redirectUri", redirectUri
                )
        ));
    }

    /**
     * Redirect endpoint after successful Google OAuth login
     * This is just a fallback in case the OAuth2 success handler doesn't work
     */
    @GetMapping("/success")
    public RedirectView oauthSuccess() {
        // Should not normally get here as the OAuth2 success handler should handle the redirect
        // This is just a fallback
        return new RedirectView("http://localhost:4200/dashboard");
    }

    /**
     * Error endpoint for OAuth2 errors
     */
    @GetMapping("/error")
    public ResponseEntity<Map<String, String>> oauthError() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "OAuth2 authentication failed"));
    }
}