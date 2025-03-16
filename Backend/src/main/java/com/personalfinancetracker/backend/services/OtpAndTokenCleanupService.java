package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.repository.OtpVerificationRepository;
import com.personalfinancetracker.backend.repository.PasswordResetTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OtpAndTokenCleanupService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public OtpAndTokenCleanupService(OtpVerificationRepository otpVerificationRepository,
                                     PasswordResetTokenRepository passwordResetTokenRepository) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }


    @Scheduled(fixedRate = 60000)
    public void deleteExpiredOtpsAndTokens() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Running cleanup task at: " + now);

        // Delete expired OTP verifications
        otpVerificationRepository.deleteByExpirationTimeBefore(now);
        System.out.println("Deleted expired OTP verifications up to: " + now);

        // Delete expired password reset tokens
        passwordResetTokenRepository.deleteByExpirationTimeBefore(now);
        System.out.println("Deleted expired password reset tokens up to: " + now);
    }
}
