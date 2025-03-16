package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.dto.SignupRequest;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.OtpVerification;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.OtpVerificationRepository;
import com.personalfinancetracker.backend.services.auth.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuthenticationService authenticationService;

    @Autowired
    public AuthServiceImpl(CustomerRepository customerRepository,
                           OtpVerificationRepository otpVerificationRepository,
                           PasswordEncoder passwordEncoder,
                           OtpService otpService,
                           AuthenticationService authenticationService) {
        this.customerRepository = customerRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.authenticationService = authenticationService;
    }

    @Override
    public boolean createCustomer(SignupRequest signupRequest) {
        logger.info("Creating customer with email: {}", signupRequest.getEmail());

        // Check if user already exists and is verified
        Optional<Customer> existingCustomer = customerRepository.findByEmail(signupRequest.getEmail());
        if (existingCustomer.isPresent() && existingCustomer.get().isVerified()) {
            logger.warn("Customer already exists and is verified: {}", signupRequest.getEmail());
            return false;
        }

        // Check if there's a pending OTP verification
        Optional<OtpVerification> existingOtpVerification = otpVerificationRepository.findFirstByEmail(signupRequest.getEmail());
        if (existingOtpVerification.isPresent()) {
            // Delete the existing OTP verification
            otpVerificationRepository.delete(existingOtpVerification.get());
            logger.info("Deleted existing OTP verification for: {}", signupRequest.getEmail());
        }

        // Generate OTP
        String otp = otpService.generateOtp();
        logger.info("Generated OTP for: {}", signupRequest.getEmail());

        try {
            // Send OTP email
            otpService.sendOtpEmail(signupRequest.getEmail(), otp);
            logger.info("Sent OTP email to: {}", signupRequest.getEmail());

            // Save to temporary OTP table (OtpVerification)
            OtpVerification otpVerification = new OtpVerification();
            BeanUtils.copyProperties(signupRequest, otpVerification);  // Copy email, name, password
            otpVerification.setOtp(otp);
            otpVerification.setPassword(passwordEncoder.encode(signupRequest.getPassword()));  // Store hashed password
            otpVerification.setExpirationTime(LocalDateTime.now().plusMinutes(5));  // Set 5 min expiration
            otpVerificationRepository.save(otpVerification);
            logger.info("Saved OTP verification for: {}", signupRequest.getEmail());

            return true;
        } catch (Exception e) {
            logger.error("Error during customer creation for {}: {}", signupRequest.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public String verifyOtp(String email, String otp, HttpServletResponse response) {
        logger.info("Verifying OTP for email: {}", email);

        // Fetch OTP verification data by email
        OtpVerification otpVerification = otpVerificationRepository.findFirstByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("No OTP found for email: {}", email);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "No OTP found for email: " + email);
                });

        // Check if OTP is correct and within the expiration time
        if (otpVerification.getOtp().equals(otp) && otpVerification.getExpirationTime().isAfter(LocalDateTime.now())) {
            logger.info("OTP verified successfully for: {}", email);

            // Move user details from OtpVerification to Customer table
            Customer customer = new Customer();
            customer.setEmail(otpVerification.getEmail());
            customer.setName(otpVerification.getName());
            customer.setPassword(otpVerification.getPassword());
            customer.setVerified(true);  // Mark as verified
            customer.setProvider("Normal");  // Set provider for regular signup
            customerRepository.save(customer);
            logger.info("Created verified customer account for: {}", email);

            // Remove OTP entry after successful verification
            otpVerificationRepository.deleteByEmail(email);
            logger.info("Deleted OTP verification after successful verification for: {}", email);

            // Generate JWT token and add to cookie
            authenticationService.addTokenCookie(response, customer.getEmail());
            logger.info("Added JWT token cookie for: {}", email);

            // Generate token for backward compatibility
            String token = authenticationService.generateToken(customer.getEmail());
            logger.info("Generated JWT token for backward compatibility for: {}", email);

            return token;
        } else {
            logger.warn("Invalid OTP or OTP expired for: {}", email);
            return null;
        }
    }

    @Override
    public boolean resendOtp(String email) {
        logger.info("Resending OTP for email: {}", email);

        // First check if user already exists and is verified
        Optional<Customer> existingCustomer = customerRepository.findByEmail(email);
        if (existingCustomer.isPresent() && existingCustomer.get().isVerified()) {
            logger.warn("User already verified, no need to resend OTP: {}", email);
            return false; // User already verified, no need to resend OTP
        }

        // Check if there's a pending OTP verification
        Optional<OtpVerification> existingOtpVerification = otpVerificationRepository.findFirstByEmail(email);
        if (!existingOtpVerification.isPresent()) {
            logger.warn("No pending registration found for: {}", email);
            return false; // No pending registration found
        }

        OtpVerification otpVerification = existingOtpVerification.get();

        // Generate new OTP
        String otp = otpService.generateOtp();
        logger.info("Generated new OTP for: {}", email);

        try {
            // Send OTP email
            otpService.sendOtpEmail(email, otp);
            logger.info("Sent new OTP email to: {}", email);

            // Update OTP and expiration time
            otpVerification.setOtp(otp);
            otpVerification.setExpirationTime(LocalDateTime.now().plusMinutes(5)); // Reset 5 min expiration
            otpVerificationRepository.save(otpVerification);
            logger.info("Updated OTP verification with new OTP for: {}", email);

            return true;
        } catch (Exception e) {
            logger.error("Error resending OTP for {}: {}", email, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public void completeOAuth2Registration(String name, String email, String provider, HttpServletResponse response) {
        logger.info("Completing OAuth2 registration for: {} with provider: {}", email, provider);

        try {
            Customer existingCustomer = customerRepository.findByEmail(email).orElse(null);

            if (existingCustomer == null) {
                // Create new customer
                Customer customer = new Customer();
                customer.setName(name);
                customer.setEmail(email);
                customer.setPassword(passwordEncoder.encode("OAuth2User" + System.currentTimeMillis()));
                customer.setVerified(true);  // OAuth2 users are automatically verified
                customer.setProvider(provider);

                customerRepository.save(customer);
                logger.info("Created new customer account for OAuth2 user: {}", email);
            } else if (!existingCustomer.isVerified()) {
                // Update existing unverified customer
                existingCustomer.setVerified(true);
                existingCustomer.setProvider(provider);
                customerRepository.save(existingCustomer);
                logger.info("Updated existing customer account for OAuth2 user: {}", email);
            }

            // Generate JWT token and add to cookie - ALWAYS use EMAIL as the subject
            authenticationService.addTokenCookie(response, email);
            logger.info("Added JWT token cookie for OAuth2 user: {}", email);
        } catch (Exception e) {
            logger.error("Error during OAuth2 registration for {}: {}", email, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to complete OAuth2 registration");
        }
    }
}