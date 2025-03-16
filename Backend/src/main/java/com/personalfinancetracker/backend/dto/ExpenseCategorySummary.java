package com.personalfinancetracker.backend.dto;

public class ExpenseCategorySummary {
    private String category;
    private Double totalAmount;

    public ExpenseCategorySummary(String category, Double totalAmount) {
        this.category = category;
        this.totalAmount = totalAmount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
