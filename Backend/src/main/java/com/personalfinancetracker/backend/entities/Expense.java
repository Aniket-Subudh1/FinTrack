package com.personalfinancetracker.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)package com.personalfinancetracker.backend.entities;

import jakarta.persistence.*;

    @Entity
    @Table(name = "expenses")
    public class Expense {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String userName;

        @Column(nullable = false)
        private Double amount;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private ExpenseCategory category;

        private String customCategory; // New field for custom category input

        // Constructors
        public Expense() {}

        public Expense(String userName, Double amount, ExpenseCategory category, String customCategory) {
            this.userName = userName;
            this.amount = amount;
            this.category = category;
            this.customCategory = customCategory;
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

        public ExpenseCategory getCategory() {
            return category;
        }

        public void setCategory(ExpenseCategory category) {
            this.category = category;
        }

        public String getCustomCategory() {
            return customCategory;
        }

        public void setCustomCategory(String customCategory) {
            this.customCategory = customCategory;
        }
    }

    private String category;

    // Constructors
    public Expense() {}

    public Expense(String userName, Double amount, String category) {
        this.userName = userName;
        this.amount = amount;
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
