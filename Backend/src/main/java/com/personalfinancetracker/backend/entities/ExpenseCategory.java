package com.personalfinancetracker.backend.entities;

import jakarta.persistence.*;
import com.personalfinancetracker.backend.entities.ExpenseCategoryEnum;

@Entity
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private ExpenseCategoryEnum expenseCategory;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExpenseCategoryEnum getExpenseCategory() {
        return expenseCategory;
    }

    public void setExpenseCategory(ExpenseCategoryEnum expenseCategory) {
        this.expenseCategory = expenseCategory;
    }
}
