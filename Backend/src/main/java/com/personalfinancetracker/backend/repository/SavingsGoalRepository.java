package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    Optional<SavingsGoal> findByCustomerEmail(String email);
}