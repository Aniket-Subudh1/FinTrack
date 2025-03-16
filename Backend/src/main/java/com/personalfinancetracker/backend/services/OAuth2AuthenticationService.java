package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuth2AuthenticationService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public OAuth2AuthenticationService(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public String processOAuth2Login(String name, String email, String provider) {
        saveOAuth2User(name, email, provider);
        return jwtUtil.generateToken(email);
    }

    @Transactional
    public void saveOAuth2User(String name, String email, String provider) {
        Customer existingCustomer = customerRepository.findByEmail(email)
                .orElse(null);

        if (existingCustomer == null) {
            Customer customer = new Customer();
            customer.setName(name);
            customer.setEmail(email);
            customer.setPassword(passwordEncoder.encode("dummyPassword"));  // Encode even the dummy password
            customer.setProvider(provider);
            customer.setVerified(true);  // OAuth2 users are automatically verified

            customerRepository.save(customer);
        } else if (!existingCustomer.isVerified()) {
            // If user exists but not verified, mark as verified for OAuth users
            existingCustomer.setVerified(true);
            existingCustomer.setProvider(provider);
            customerRepository.save(existingCustomer);
        }
    }
}