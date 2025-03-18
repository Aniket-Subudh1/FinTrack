package com.personalfinancetracker.backend.dto;

import java.time.LocalDate;

public class ExpenseFilterRequest {
    private String email;
    private LocalDate startDate;
    private LocalDate endDate;

    // Constructors
    public ExpenseFilterRequest() {}

    public ExpenseFilterRequest(String email, LocalDate startDate, LocalDate endDate) {
        this.email = email;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}