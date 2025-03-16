package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.dto.ForgotPasswordRequest;
import com.personalfinancetracker.backend.dto.ResetPasswordRequest;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.PasswordResetToken;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ForgotPasswordService {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordService.class);

    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ForgotPasswordService(CustomerRepository customerRepository,
                                 PasswordResetTokenRepository passwordResetTokenRepository,
                                 OtpService otpService,
                                 PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    public void processForgotPassword(ForgotPasswordRequest request) {
        logger.info("Processing forgot password for email: {}", request.getEmail());

        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("No customer found with email: {}", request.getEmail());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer found with that email");
                });

        // Check if the customer is verified
        if (!customer.isVerified()) {
            logger.warn("Customer not verified: {}", request.getEmail());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not verified. Please verify your account first.");
        }

        // Generate OTP
        String otp = otpService.generateOtp();
        logger.info("Generated OTP for password reset: {}", request.getEmail());

        // Check if there's an existing token and delete it
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByEmail(customer.getEmail());
        existingToken.ifPresent(passwordResetTokenRepository::delete);

        // Save new OTP to database
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(customer.getEmail());
        resetToken.setOtp(otp);
        resetToken.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        passwordResetTokenRepository.save(resetToken);
        logger.info("Saved password reset token for: {}", request.getEmail());

        // Send OTP via email
        otpService.sendOtpEmail(customer.getEmail(), otp);
        logger.info("Sent password reset OTP email to: {}", request.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        logger.info("Resetting password for email: {}", request.getEmail());

        PasswordResetToken tokenEntity = passwordResetTokenRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Invalid or expired OTP for: {}", request.getEmail());
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
                });

        // Check if OTP is correct and within the expiration time
        if (tokenEntity.getOtp().equals(request.getOtp()) && tokenEntity.getExpirationTime().isAfter(LocalDateTime.now())) {
            Customer customer = customerRepository.findByEmail(tokenEntity.getEmail())
                    .orElseThrow(() -> {
                        logger.warn("Customer not found: {}", request.getEmail());
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
                    });

            // Update customer's password
            customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
            customerRepository.save(customer);
            logger.info("Updated password for: {}", request.getEmail());

            // Remove OTP entry after successful password reset
            passwordResetTokenRepository.deleteByEmail(customer.getEmail());
            logger.info("Deleted password reset token after successful reset for: {}", request.getEmail());
        } else {
            logger.warn("Invalid or expired OTP for: {}", request.getEmail());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }
    }

    public void resendOtp(String email) {
        logger.info("Resending OTP for password reset for email: {}", email);

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("No customer found with email: {}", email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer found with that email");
                });

        // Check if there's an existing token
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByEmail(email);
        if (existingToken.isEmpty()) {
            logger.warn("No password reset request found for: {}", email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No password reset request found. Please initiate password reset first.");
        }

        // Generate new OTP
        String otp = otpService.generateOtp();
        logger.info("Generated new OTP for password reset: {}", email);

        // Update token with new OTP
        PasswordResetToken tokenEntity = existingToken.get();
        tokenEntity.setOtp(otp);
        tokenEntity.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        passwordResetTokenRepository.save(tokenEntity);
        logger.info("Updated password reset token with new OTP for: {}", email);

        // Send new OTP via email
        otpService.sendOtpEmail(customer.getEmail(), otp);
        logger.info("Sent new password reset OTP email to: {}", email);
    }
}