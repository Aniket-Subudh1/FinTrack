package com.personalfinancetracker.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.Random;

@Service
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final JavaMailSender javaMailSender;

    @Autowired
    public OtpService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    // Generate a random 6-digit OTP
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);  // Generates a random 6-digit OTP
        return String.valueOf(otp);
    }

    // Send OTP email
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Your FinTrack Verification Code");

            // HTML content with some basic styling
            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h1 style="color: #3b82f6;">FinTrack</h1>
                        <p style="font-size: 18px; color: #333;">Your Personal Finance Tracker</p>
                    </div>
                    <div style="background-color: #f9fafb; padding: 20px; border-radius: 5px; margin-bottom: 20px;">
                        <p style="margin: 0; font-size: 16px;">Hello,</p>
                        <p style="margin-top: 10px; font-size: 16px;">Thank you for registering with FinTrack. Your verification code is:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; padding: 10px 20px; background-color: #3b82f6; color: white; border-radius: 5px;">%s</span>
                        </div>
                        <p style="margin: 0; font-size: 16px;">This code will expire in 5 minutes.</p>
                    </div>
                    <div style="font-size: 14px; color: #6b7280; text-align: center;">
                        <p>If you didn't request this code, please ignore this email.</p>
                        <p>&copy; %d FinTrack. All rights reserved.</p>
                    </div>
                </div>
            """.formatted(otp, java.time.Year.now().getValue());

            helper.setText(htmlContent, true);
            javaMailSender.send(message);
            logger.info("OTP email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}