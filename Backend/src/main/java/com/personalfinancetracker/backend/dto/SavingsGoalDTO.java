package com.personalfinancetracker.backend.dto;


public class SavingsGoalDTO {
    private Double amount;

    // Constructors
    public SavingsGoalDTO() {}

    public SavingsGoalDTO(Double amount) {
        this.amount = amount;
    }

    // Getters and Setters
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}