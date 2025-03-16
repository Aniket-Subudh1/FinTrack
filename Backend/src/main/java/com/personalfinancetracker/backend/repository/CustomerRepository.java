package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByProviderId(String providerId);

}