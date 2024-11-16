package com.personalfinancetracker.backend.dto;

public class ExpenseRequest {
    private Double amount;
    private String category;


    public ExpenseRequest() {}

    public ExpenseRequest(Double amount, String category) {
        this.amount = amount;
        this.category = category;
    }



    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
