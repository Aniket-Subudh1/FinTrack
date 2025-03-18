package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByCustomerEmail(String email);

    @Query("SELECT b FROM Budget b WHERE b.customer.email = :email AND b.category = :category")
    Optional<Budget> findByCustomerEmailAndCategory(@Param("email") String email, @Param("category") String category);

    void deleteByCustomerEmailAndCategory(String email, String category);
}