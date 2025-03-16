package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.dto.ForgotPasswordRequest;
import com.personalfinancetracker.backend.dto.ResetPasswordRequest;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.PasswordResetToken;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class ForgotPasswordService {

    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ForgotPasswordService(CustomerRepository customerRepository,
                                 PasswordResetTokenRepository passwordResetTokenRepository,
                                 EmailService emailService,
                                 PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // Generate a random 6-digit OTP
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public void processForgotPassword(ForgotPasswordRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer found with that email"));

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Save OTP to database
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(customer.getEmail());
        resetToken.setOtp(otp);  // Store the generated OTP
        resetToken.setExpirationTime(LocalDateTime.now().plusMinutes(2));
        passwordResetTokenRepository.save(resetToken);

        // Send OTP via email
        emailService.sendOtpEmail(customer.getEmail(), "Your password reset OTP is: " + otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken tokenEntity = passwordResetTokenRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP"));

        // Check if OTP is correct and within the expiration time
        if (tokenEntity.getOtp().equals(request.getOtp()) && tokenEntity.getExpirationTime().isAfter(LocalDateTime.now())) {
            Customer customer = customerRepository.findByEmail(tokenEntity.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

            // Update customer's password
            customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
            customerRepository.save(customer);

            // Remove OTP entry after successful password reset
            passwordResetTokenRepository.deleteByEmail(customer.getEmail());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }
    }
}
