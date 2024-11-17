// src/main/java/com/personalfinancetracker/backend/repository/ExpenseCategoryRepository.java
package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
}