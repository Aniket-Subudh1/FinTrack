package com.personalfinancetracker.backend.entities;

import jakarta.persistence.*;

@Entity
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @OneToOne
    @JoinColumn(name = "customer_email", referencedColumnName = "email", unique = true)
    private Customer customer;

    // Constructors
    public SavingsGoal() {}

    public SavingsGoal(Double amount, Customer customer) {
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