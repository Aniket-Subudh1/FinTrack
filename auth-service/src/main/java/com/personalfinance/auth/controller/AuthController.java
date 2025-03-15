package com.personalfinance.auth.controller;

import com.personalfinance.auth.dto.*;
import com.personalfinance.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("OTP sent to email. Please verify."));
    }

    /**
     * Verify registration OTP
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.verifyRegistrationOtp(request);

        // Set refresh token as an HTTP-only secure cookie
        setRefreshTokenCookie(response, authResponse.getRefreshToken());

        return ResponseEntity.ok(authResponse);
    }

    /**
     * Login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.authenticate(request);

        // Set refresh token as an HTTP-only secure cookie
        setRefreshTokenCookie(response, authResponse.getRefreshToken());

        return ResponseEntity.ok(authResponse);
    }

    /**
     * Forgot password (send OTP)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.processForgotPassword(request);
        return ResponseEntity.ok(
                new ApiResponse("Password reset OTP sent to your email.")
        );
    }

    /**
     * Reset password with OTP
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                new ApiResponse("Password successfully reset.")
        );
    }

    /**
     * Refresh access token using refresh token from cookie
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        AuthResponse authResponse = authService.refreshToken(refreshToken);

        // Set new refresh token as an HTTP-only secure cookie
        setRefreshTokenCookie(response, authResponse.getRefreshToken());

        return ResponseEntity.ok(authResponse);
    }

    /**
     * Logout (clear refresh token cookie)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletResponse response) {
        // Clear refresh token cookie
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new ApiResponse("Logged out successfully"));
    }

    /**
     * Helper method to set refresh token cookie
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // For HTTPS
        response.addCookie(cookie);
    }
}