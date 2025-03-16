package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.dto.ExpenseCategorySummary;
import com.personalfinancetracker.backend.entities.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCustomerEmail(String email);

    @Query("SELECT DISTINCT e.category FROM Expense e")
    List<String> findDistinctCategories();
    @Query("SELECT new com.personalfinancetracker.backend.dto.ExpenseCategorySummary(e.category, SUM(e.amount)) " +
            "FROM Expense e WHERE e.customer.email = :email GROUP BY e.category")
    List<ExpenseCategorySummary> getExpenseSummaryByCategory(@Param("email") String email);
}