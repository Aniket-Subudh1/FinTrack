package com.personalfinancetracker.backend.dto;

public class IncomeSummary {
    private String source;
    private Double totalAmount;

    public IncomeSummary() {}

    public IncomeSummary(String source, Double totalAmount) {
        this.source = source;
        this.totalAmount = totalAmount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}