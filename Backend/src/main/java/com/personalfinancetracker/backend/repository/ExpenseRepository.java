package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Custom method to find expenses by userId
    List<Expense> findByUserId(String userId);
}
