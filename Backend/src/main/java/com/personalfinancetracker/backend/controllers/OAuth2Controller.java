package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.TokenValidationRequest;
import com.personalfinancetracker.backend.dto.TokenValidationResponse;
import com.personalfinancetracker.backend.services.AuthService;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Autowired
    public OAuth2Controller(JwtUtil jwtUtil, AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestBody TokenValidationRequest request,
            HttpServletResponse response) {
        logger.info("Validating token");

        String token = request.getToken();
        try {
            String username = jwtUtil.extractUsername(token);
            boolean isValid = !jwtUtil.isTokenExpired(token);

            logger.info("Token validation result for {}: {}", username, isValid);

            // If token is valid, set it as a cookie for future requests
            if (isValid) {
                jwtUtil.createJwtCookie(response, token);
                logger.info("Created JWT cookie for: {}", username);
            }

            return ResponseEntity.ok(new TokenValidationResponse(isValid, username));
        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage(), e);
            return ResponseEntity.ok(new TokenValidationResponse(false, null));
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        logger.info("Getting OAuth2 user info");

        String token = jwtUtil.getJwtFromCookies(request);
        Map<String, Object> userInfo = new HashMap<>();

        if (token != null) {
            try {
                String username = jwtUtil.extractUsername(token);
                boolean isValid = !jwtUtil.isTokenExpired(token);

                if (isValid) {
                    userInfo.put("email", username);
                    userInfo.put("authenticated", true);
                    logger.info("Returning user info for: {}", username);
                    return ResponseEntity.ok(userInfo);
                }
            } catch (Exception e) {
                logger.error("Error processing user info: {}", e.getMessage(), e);
            }
        }

        userInfo.put("authenticated", false);
        return ResponseEntity.ok(userInfo);
    }
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handleOAuth2Callback(
            @RequestBody Map<String, String> payload,
            HttpServletResponse response) {
        logger.info("Handling OAuth2 callback with payload: {}", payload);

        String name = payload.get("name");
        String email = payload.get("email");
        String provider = payload.get("provider");

        if (email == null || provider == null) {
            logger.warn("Missing required OAuth2 data: email={}, provider={}", email, provider);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Missing required OAuth2 data");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            authService.completeOAuth2Registration(name, email, provider, response);

            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", "OAuth2 authentication successful");
            successResponse.put("email", email); // Include email in response
            logger.info("OAuth2 authentication successful for: {}", email);
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            logger.error("Error handling OAuth2 callback: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "OAuth2 authentication failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}