package com.personalfinancetracker.backend.services.auth;

import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AuthenticationService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthenticationService(
            CustomerRepository customerRepository,
            @Lazy PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by email: {}", email);

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        // Only allow verified users to login
        if (!customer.isVerified()) {
            logger.warn("User is not verified: {}", email);
            throw new UsernameNotFoundException("User is not verified: " + email);
        }

        return new User(customer.getEmail(), customer.getPassword(), Collections.emptyList());
    }

    public String generateToken(String username) {
        logger.debug("Generating token for: {}", username);
        return jwtUtil.generateToken(username);
    }

    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        return jwtUtil.validateToken(token, userDetails);
    }

    public void addTokenCookie(HttpServletResponse response, String username) {
        String token = jwtUtil.generateToken(username);
        jwtUtil.addJwtCookie(response, token);
        logger.info("Added token cookie for: {}", username);
    }

    public void clearTokenCookie(HttpServletResponse response) {
        logger.debug("Clearing token cookie");
        jwtUtil.clearJwtCookie(response);
    }
}