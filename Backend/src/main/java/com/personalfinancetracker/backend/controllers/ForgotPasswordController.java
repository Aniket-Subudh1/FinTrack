package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.ForgotPasswordRequest;
import com.personalfinancetracker.backend.dto.ResetPasswordRequest;
import com.personalfinancetracker.backend.services.ForgotPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/forgot-password")
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @Autowired
    public ForgotPasswordController(ForgotPasswordService forgotPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> processForgotPassword(@RequestBody ForgotPasswordRequest request) {
        forgotPasswordService.processForgotPassword(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset OTP sent to your email.");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        forgotPasswordService.resetPassword(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password successfully reset.");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
