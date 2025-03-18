package com.personalfinancetracker.backend.dto;

public class BudgetStatus {
    private String category;
    private Double budgetAmount;
    private Double spentAmount;
    private Double remainingAmount;
    private Double percentUsed;

    public BudgetStatus() {}

    public BudgetStatus(String category, Double budgetAmount, Double spentAmount, Double remainingAmount, Double percentUsed) {
        this.category = category;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.remainingAmount = remainingAmount;
        this.percentUsed = percentUsed;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(Double budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public Double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(Double spentAmount) {
        this.spentAmount = spentAmount;
    }

    public Double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(Double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public Double getPercentUsed() {
        return percentUsed;
    }

    public void setPercentUsed(Double percentUsed) {
        this.percentUsed = percentUsed;
    }
}