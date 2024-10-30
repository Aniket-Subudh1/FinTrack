package com.personalfinancetracker.backend.entities;

import com.personalfinancetracker.backend.dto.IncomeDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jdk.jfr.DataAmount;

import java.time.LocalDate;

@Entity
@DataAmount
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String userName;

    private Double amount;

    private String source;

    private LocalDate date;

    private String category;

    private String description;

    // Default constructor
    public Income() {}

    // Parameterized constructor
    public Income(String userName, Double amount, String source) {
        this.userName = userName;
        this.amount = amount;
        this.source = source;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getdate() {
        return date .toString();
    }

    public void setdate(String date) {
        this.date = LocalDate . parse(date);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this. category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    }


