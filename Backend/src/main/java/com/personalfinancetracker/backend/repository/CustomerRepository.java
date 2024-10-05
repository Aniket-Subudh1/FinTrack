package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Check if customer exists by email
    boolean existsByEmail(String email);

    // Optional method to find customer by email if needed elsewhere
    Optional<Customer> findByEmail(String email);
}
