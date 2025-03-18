package com.personalfinancetracker.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ExpenseResponse {
    private Long id;
    private Double amount;
    private String category;
    private LocalDateTime date;
    private String customerEmail;
    private List<String> tags;
    private String note;
    private Boolean isRecurring;
    private String recurringFrequency;

    public ExpenseResponse() {}

    public ExpenseResponse(Long id, Double amount, String category, LocalDateTime date, String customerEmail) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.customerEmail = customerEmail;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public Double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public String getRecurringFrequency() {
        return recurringFrequency;
    }

    public void setRecurringFrequency(String recurringFrequency) {
        this.recurringFrequency = recurringFrequency;
    }
}