package com.personalfinance.auth.service;

import com.personalfinance.auth.dto.*;
import com.personalfinance.auth.entity.OtpVerification;
import com.personalfinance.auth.entity.PasswordResetToken;
import com.personalfinance.auth.entity.Role;
import com.personalfinance.auth.entity.User;
import com.personalfinance.auth.exception.AuthException;
import com.personalfinance.auth.repository.OtpVerificationRepository;
import com.personalfinance.auth.repository.PasswordResetRepository;
import com.personalfinance.auth.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthService(
            UserRepository userRepository,
            OtpVerificationRepository otpVerificationRepository,
            PasswordResetRepository passwordResetRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService,
            ObjectProvider<AuthenticationManager> authenticationManagerProvider,
            UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.authenticationManagerProvider = authenticationManagerProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Register a new user and send verification OTP
     */
    @Transactional
    public void register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered");
        }

        // Generate OTP
        String otp = otpService.generateOtp();

        // Create OTP verification record
        OtpVerification otpVerification = OtpVerification.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .otp(otp)
                .expirationTime(LocalDateTime.now().plusMinutes(10)) // 10 minutes
                .build();

        otpVerificationRepository.save(otpVerification);

        // Send OTP email
        otpService.sendRegistrationOtp(request.getEmail(), otp);
    }

    /**
     * Verify OTP and complete user registration
     */
    @Transactional
    public AuthResponse verifyRegistrationOtp(OtpVerificationRequest request) {
        // Find OTP verification record
        OtpVerification otpVerification = otpVerificationRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("No OTP verification request found for this email"));

        // Validate OTP
        if (!otpVerification.getOtp().equals(request.getOtp())) {
            throw new AuthException("Invalid OTP");
        }

        // Check if OTP is expired
        if (otpVerification.isExpired()) {
            throw new AuthException("OTP has expired");
        }

        // Create new user
        User user = User.builder()
                .email(otpVerification.getEmail())
                .name(otpVerification.getName())
                .password(otpVerification.getPassword())
                .verified(true)
                .provider("local")
                .roles(Set.of(Role.USER))
                .build();

        User savedUser = userRepository.save(user);

        // Delete OTP verification record
        otpVerificationRepository.delete(otpVerification);

        // Generate tokens
        String accessToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(savedUser.getId().toString())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .build();
    }

    /**
     * Authenticate a user and return JWT tokens
     */
    public AuthResponse authenticate(AuthRequest request) {
        try {
            // Get the AuthenticationManager when needed
            AuthenticationManager authenticationManager = authenticationManagerProvider.getObject();

            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Get authenticated user
            User user = (User) userDetailsService.loadUserByUsername(request.getEmail());

            // Check if user is verified
            if (!user.isVerified()) {
                throw new AuthException("Account not verified. Please verify your email.");
            }

            // Generate tokens
            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            return AuthResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId().toString())
                    .name(user.getName())
                    .email(user.getEmail())
                    .build();
        } catch (AuthenticationException e) {
            throw new AuthException("Invalid email or password");
        }
    }

    /**
     * Process forgot password request by sending OTP
     */
    @Transactional
    public void processForgotPassword(ForgotPasswordRequest request) {
        // Check if user exists
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("No account found with this email"));

        // Generate OTP
        String otp = otpService.generateOtp();

        // Create or update password reset token
        passwordResetRepository.findByEmail(request.getEmail())
                .ifPresentOrElse(
                        // Update existing token
                        token -> {
                            token.setOtp(otp);
                            token.setExpirationTime(LocalDateTime.now().plusMinutes(10)); // 10 minutes
                            passwordResetRepository.save(token);
                        },
                        // Create new token
                        () -> {
                            PasswordResetToken token = PasswordResetToken.builder()
                                    .email(request.getEmail())
                                    .otp(otp)
                                    .expirationTime(LocalDateTime.now().plusMinutes(10)) // 10 minutes
                                    .build();
                            passwordResetRepository.save(token);
                        }
                );

        // Send OTP email
        otpService.sendPasswordResetOtp(request.getEmail(), otp);
    }

    /**
     * Reset password with OTP verification
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Find password reset token
        PasswordResetToken token = passwordResetRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("No password reset request found for this email"));

        // Validate OTP
        if (!token.getOtp().equals(request.getOtp())) {
            throw new AuthException("Invalid OTP");
        }

        // Check if OTP is expired
        if (token.isExpired()) {
            throw new AuthException("OTP has expired");
        }

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete password reset token
        passwordResetRepository.delete(token);
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        // Extract email from refresh token
        String email = jwtService.extractUsername(refreshToken);

        if (email == null) {
            throw new AuthException("Invalid refresh token");
        }

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found"));

        // Validate refresh token
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new AuthException("Invalid or expired refresh token");
        }

        // Generate new access token
        String accessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken) // Return same refresh token
                .userId(user.getId().toString())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}