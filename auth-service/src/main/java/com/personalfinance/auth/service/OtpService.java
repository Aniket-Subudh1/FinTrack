package com.personalfinance.auth.service;

import com.personalfinance.auth.entity.OtpVerification;
import com.personalfinance.auth.entity.PasswordResetToken;
import com.personalfinance.auth.repository.OtpVerificationRepository;
import com.personalfinance.auth.repository.PasswordResetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender javaMailSender;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;

    /**
     * Generate a random 6-digit OTP code
     */
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    /**
     * Send OTP email for registration verification
     */
    public void sendRegistrationOtp(String toEmail, String otp) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("FinTrack: Account Verification OTP");

            String emailContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #4a4a4a;">Welcome to FinTrack!</h2>
                    <p>Thank you for registering. To complete your account verification, please use the following OTP code:</p>
                    <div style="background-color: #f4f4f4; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;">
                        %s
                    </div>
                    <p>This code will expire in 10 minutes.</p>
                    <p>If you didn't request this verification, please ignore this email.</p>
                    <p>Best regards,<br>The FinTrack Team</p>
                </div>
                """.formatted(otp);

            helper.setText(emailContent, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    /**
     * Send OTP email for password reset
     */
    public void sendPasswordResetOtp(String toEmail, String otp) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("FinTrack: Password Reset OTP");

            String emailContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #4a4a4a;">Password Reset Request</h2>
                    <p>We received a request to reset your password. Please use the following OTP code to proceed with your password reset:</p>
                    <div style="background-color: #f4f4f4; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;">
                        %s
                    </div>
                    <p>This code will expire in 10 minutes.</p>
                    <p>If you didn't request a password reset, please ignore this email or contact support if you're concerned about your account security.</p>
                    <p>Best regards,<br>The FinTrack Team</p>
                </div>
                """.formatted(otp);

            helper.setText(emailContent, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset OTP email", e);
        }
    }

    /**
     * Scheduled task to clean up expired OTP codes (runs every 10 minutes)
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpVerificationRepository.deleteByExpirationTimeBefore(now);
        passwordResetRepository.deleteByExpirationTimeBefore(now);
    }
}