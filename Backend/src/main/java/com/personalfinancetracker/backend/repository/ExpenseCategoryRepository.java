package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.ExpenseCategory;
import com.personalfinancetracker.backend.entities.ExpenseCategoryEnum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    boolean existsByExpenseCategory(ExpenseCategoryEnum expenseCategory);
}
