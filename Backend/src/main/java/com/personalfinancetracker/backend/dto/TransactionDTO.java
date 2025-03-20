package com.personalfinancetracker.backend.dto;

import java.time.LocalDate;
import java.util.List;

public class TransactionDTO {
    private Long id;
    private String type; // "expense" or "income"
    private Double amount;
    private String category; // Expense category or income source
    private LocalDate date;
    private String description;
    private Boolean isRecurring;
    private String recurringFrequency;
    private List<String> tags;

    // Constructors
    public TransactionDTO() {}

    public TransactionDTO(
            Long id,
            String type,
            Double amount,
            String category,
            LocalDate date,
            String description,
            Boolean isRecurring,
            String recurringFrequency,
            List<String> tags) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
        this.isRecurring = isRecurring;
        this.recurringFrequency = recurringFrequency;
        this.tags = tags;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}