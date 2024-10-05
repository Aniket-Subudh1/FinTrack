package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.dto.SignupRequest;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.OtpVerification;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.OtpVerificationRepository;
import com.personalfinancetracker.backend.utils.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final CustomerRepository customerRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtUtil jwtUtil; // Add JwtUtil to generate token

    @Autowired
    public AuthServiceImpl(CustomerRepository customerRepository,
                           OtpVerificationRepository otpVerificationRepository,
                           PasswordEncoder passwordEncoder,
                           OtpService otpService,
                           JwtUtil jwtUtil) {  // Inject JwtUtil
        this.customerRepository = customerRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean createCustomer(SignupRequest signupRequest) {
        // Check if user already exists
        if (customerRepository.existsByEmail(signupRequest.getEmail())) {
            return false;
        }

        // Generate OTP
        String otp = otpService.generateOtp();

        // Send OTP email
        otpService.sendOtpEmail(signupRequest.getEmail(), otp);

        // Save to temporary OTP table (OtpVerification)
        OtpVerification otpVerification = new OtpVerification();
        BeanUtils.copyProperties(signupRequest, otpVerification);  // Copy email, name, password
        otpVerification.setOtp(otp);
        otpVerification.setPassword(passwordEncoder.encode(signupRequest.getPassword()));  // Store the hashed password
        otpVerification.setExpirationTime(LocalDateTime.now().plusMinutes(2));  // Set 2 min expiration
        otpVerificationRepository.save(otpVerification);

        return true;
    }

    @Override
    @Transactional
    public String verifyOtp(String email, String otp) {
        // Fetch OTP verification data by email
        OtpVerification otpVerification = otpVerificationRepository.findFirstByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No OTP found for email: " + email));

        // Check if OTP is correct and within the expiration time
        if (otpVerification.getOtp().equals(otp) && otpVerification.getExpirationTime().isAfter(LocalDateTime.now())) {
            // Move user details from OtpVerification to Customer table
            Customer customer = new Customer();
            customer.setEmail(otpVerification.getEmail());
            customer.setName(otpVerification.getName());
            customer.setPassword(otpVerification.getPassword());
            customer.setVerified(true);  // Mark as verified
            customerRepository.save(customer);

            // Remove OTP entry after successful verification
            otpVerificationRepository.deleteByEmail(email);

            // Generate JWT token
            String token = jwtUtil.generateToken(customer.getEmail());

            return token;
        }

        return null;
    }




}
