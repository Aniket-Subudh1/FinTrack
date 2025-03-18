package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.dto.ExpenseCategorySummary;
import com.personalfinancetracker.backend.entities.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCustomerEmail(String email);

    @Query("SELECT DISTINCT e.category FROM Expense e")
    List<String> findDistinctCategories();

    @Query("SELECT new com.personalfinancetracker.backend.dto.ExpenseCategorySummary(e.category, SUM(e.amount)) " +
            "FROM Expense e WHERE e.customer.email = :email GROUP BY e.category")
    List<ExpenseCategorySummary> getExpenseSummaryByCategory(@Param("email") String email);

    List<Expense> findByCustomerEmailAndDateBetween(String email, LocalDate startDate, LocalDate endDate);

    // New queries
    List<Expense> findByCustomerEmailAndDateAfter(String email, LocalDateTime date);

    @Query("SELECT e FROM Expense e WHERE e.customer.email = :email AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:startDate IS NULL OR e.date >= :startDate) AND " +
            "(:endDate IS NULL OR e.date <= :endDate)")
    List<Expense> findExpensesWithFilters(
            @Param("email") String email,
            @Param("category") String category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.customer.email = :email AND " +
            "e.category = :category AND e.date >= :startDate AND e.date <= :endDate")
    Double getTotalAmountByCategory(
            @Param("email") String email,
            @Param("category") String category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT MONTH(e.date) as month, YEAR(e.date) as year, " +
            "e.category, SUM(e.amount) as total FROM Expense e " +
            "WHERE e.customer.email = :email AND e.date >= :startDate " +
            "GROUP BY YEAR(e.date), MONTH(e.date), e.category " +
            "ORDER BY YEAR(e.date), MONTH(e.date)")
    List<Object[]> getMonthlyExpensesByCategory(
            @Param("email") String email,
            @Param("startDate") LocalDateTime startDate);
}