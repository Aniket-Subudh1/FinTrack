package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.OtpVerificationRequest;
import com.personalfinancetracker.backend.dto.SignupRequest;
import com.personalfinancetracker.backend.services.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/signup")
public class SignupController {
    private final AuthService authService;

    @Autowired
    public SignupController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> signupCustomer(@RequestBody SignupRequest signupRequest) {
        boolean isOtpSent = authService.createCustomer(signupRequest);
        Map<String, String> response = new HashMap<>();
        if (isOtpSent) {
            response.put("message", "OTP sent to email. Please verify.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("message", "Customer creation failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @RequestBody OtpVerificationRequest otpRequest,
            HttpServletResponse response) {
        String token = authService.verifyOtp(otpRequest.getEmail(), otpRequest.getOtp(), response);
        Map<String, String> responseMap = new HashMap<>();
        if (token != null) {
            responseMap.put("message", "Customer verified successfully.");
            responseMap.put("token", token);  // Return token for backward compatibility
            return ResponseEntity.status(HttpStatus.OK).body(responseMap);
        } else {
            responseMap.put("message", "Invalid OTP or OTP expired.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseMap);
        }
    }
}