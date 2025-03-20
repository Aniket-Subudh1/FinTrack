package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.FinancialGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {
    List<FinancialGoal> findByCustomerEmail(String email);

    @Query("SELECT g FROM FinancialGoal g WHERE g.customer.email = :email AND g.status = :status")
    List<FinancialGoal> findByCustomerEmailAndStatus(@Param("email") String email, @Param("status") String status);

    @Query("SELECT g FROM FinancialGoal g WHERE g.customer.email = :email AND g.category = :category")
    List<FinancialGoal> findByCustomerEmailAndCategory(@Param("email") String email, @Param("category") String category);

    @Query("SELECT g FROM FinancialGoal g WHERE g.customer.email = :email AND g.targetDate <= :date")
    List<FinancialGoal> findUpcomingGoals(@Param("email") String email, @Param("date") LocalDate date);

    @Query("SELECT g FROM FinancialGoal g WHERE g.customer.email = :email AND g.currentAmount / g.targetAmount >= 0.9 AND g.status = 'ACTIVE'")
    List<FinancialGoal> findNearlyCompletedGoals(@Param("email") String email);
}