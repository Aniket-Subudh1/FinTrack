package com.personalfinancetracker.backend.services.jwt;

import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class CustomerServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CustomerServiceImpl(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new User(customer.getEmail(), customer.getPassword(), Collections.emptyList());
    }

    @Transactional
    public void saveOAuth2User(String name, String email, String password, String provider) {
        Customer existingCustomer = customerRepository.findByEmail(email)
                .orElse(null);

        if (existingCustomer == null) {
            Customer customer = new Customer();
            customer.setName(name);
            customer.setEmail(email);
            customer.setPassword(passwordEncoder.encode(password));  // Encode even the dummy password
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