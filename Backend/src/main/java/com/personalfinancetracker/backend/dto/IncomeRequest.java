// IncomeRequest.java
package com.personalfinancetracker.backend.dto;

public class IncomeRequest {
    private double amount;
    private String source;

    // Getters and Setters
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}