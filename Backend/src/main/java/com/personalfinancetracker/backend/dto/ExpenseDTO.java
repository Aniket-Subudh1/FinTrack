package com.personalfinancetracker.backend.dto;

import com.personalfinancetracker.backend.entities.ExpenseCategory;

public class ExpenseDTO {
    private double amount;
    private ExpenseCategory category;
    private String customCategory;

    // Default constructor
    public ExpenseDTO() { }

    // Constructor with all fields
    public ExpenseDTO(double amount, ExpenseCategory category, String customCategory) {
        this.amount = amount;
        this.category = category;
        this.customCategory = customCategory;
    }

    // Getters and setters
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public String getCustomCategory() {
        return customCategory;
    }

    public void setCustomCategory(String customCategory) {
        this.customCategory = customCategory;
    }
}


