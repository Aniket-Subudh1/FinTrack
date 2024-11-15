package com.personalfinancetracker.backend.dto;

import com.personalfinancetracker.backend.entities.ExpenseCategory;

public class ExpenseDTO {
    private double amount;
    private ExpenseCategory category;
    private String customCategory;

    // Default constructor
    public ExpenseDTO() {}

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

// CategoryDTO.java
package com.personalfinancetracker.backend.dto;

public class CategoryDTO {
    private String value;
    private String label;

    public CategoryDTO(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}