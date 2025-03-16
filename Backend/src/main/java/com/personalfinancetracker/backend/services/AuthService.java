package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.dto.SignupRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    boolean createCustomer(SignupRequest signupRequest);
    String verifyOtp(String email, String otp, HttpServletResponse response);
    boolean resendOtp(String email);
    void completeOAuth2Registration(String name, String email, String provider, HttpServletResponse response);
}