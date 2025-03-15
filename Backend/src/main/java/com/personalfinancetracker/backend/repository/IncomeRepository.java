package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.Income;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByCustomerEmail(String email);
}