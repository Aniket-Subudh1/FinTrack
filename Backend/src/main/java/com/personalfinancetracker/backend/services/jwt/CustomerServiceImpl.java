package com.personalfinancetracker.backend.services.jwt;

import com.personalfinancetracker.backend.dto.LoginRequest;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomerServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));


        return new User(customer.getEmail(), customer.getPassword(), Collections.emptyList());
    }


    public void saveOAuth2User(String name, String email, String password, String provider) {
        Customer existingCustomer = customerRepository.findByEmail(email)
                .orElse(null);

        if (existingCustomer == null) {

            Customer customer = new Customer();
            customer.setName(name);
            customer.setEmail(email);
            customer.setPassword(password);  // Save a dummy password for OAuth2 users
            customer.setProvider(provider);
            customer.setVerified(true);  // OAuth2 users are automatically verified

            customerRepository.save(customer);
        }
    }
}

