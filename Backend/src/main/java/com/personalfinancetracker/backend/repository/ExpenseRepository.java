package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCustomerEmail(String email);

    @Query("SELECT DISTINCT e.category FROM Expense e")
    List<String> findDistinctCategories();
}