package com.personalfinancetracker.backend.entities;

import jakarta.persistence.*;

@Entity
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private Double amount;

    @ManyToOne
    @JoinColumn(name = "customer_email", referencedColumnName = "email")
    private Customer customer;

    // Constructors
    public Budget() {}

    public Budget(String category, Double amount, Customer customer) {
        this.category = category;
        this.amount = amount;
        this.customer = customer;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}