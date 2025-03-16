package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.ForgotPasswordRequest;
import com.personalfinancetracker.backend.dto.ResetPasswordRequest;
import com.personalfinancetracker.backend.services.ForgotPasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/forgot-password")
public class ForgotPasswordController {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    private final ForgotPasswordService forgotPasswordService;

    @Autowired
    public ForgotPasswordController(ForgotPasswordService forgotPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> processForgotPassword(@RequestBody ForgotPasswordRequest request) {
        logger.info("Processing forgot password request for email: {}", request.getEmail());

        try {
            forgotPasswordService.processForgotPassword(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset OTP sent to your email.");

            logger.info("Forgot password OTP sent to: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            logger.error("Error processing forgot password request for {}: {}", request.getEmail(), e.getMessage(), e);

            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        logger.info("Processing password reset for email: {}", request.getEmail());

        try {
            forgotPasswordService.resetPassword(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password successfully reset.");

            logger.info("Password reset successful for: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            logger.error("Error resetting password for {}: {}", request.getEmail(), e.getMessage(), e);

            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        logger.info("Resending password reset OTP for: {}", email);

        try {
            forgotPasswordService.resendOtp(email);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset OTP resent to your email.");

            logger.info("Password reset OTP resent to: {}", email);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            logger.error("Error resending password reset OTP for {}: {}", email, e.getMessage(), e);

            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}