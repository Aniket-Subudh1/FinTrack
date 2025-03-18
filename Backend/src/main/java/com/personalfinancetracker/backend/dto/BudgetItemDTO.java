package com.personalfinancetracker.backend.dto;

// DTO for budget items
public class BudgetItemDTO {
    private String category;
    private Double amount;

    // Constructors
    public BudgetItemDTO() {}

    public BudgetItemDTO(String category, Double amount) {
        this.category = category;
        this.amount = amount;
    }

    // Getters and Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}

