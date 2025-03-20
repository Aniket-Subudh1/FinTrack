package com.personalfinancetracker.backend.dto;

public class MonthlySummary {
    private String month;
    private double income;
    private double expense;
    private double net;
    private double savingsRate;

    // Constructors
    public MonthlySummary() {}

    public MonthlySummary(String month, double income, double expense, double net, double savingsRate) {
        this.month = month;
        this.income = income;
        this.expense = expense;
        this.net = net;
        this.savingsRate = savingsRate;
    }

    // Getters and Setters
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = income;
    }

    public double getExpense() {
        return expense;
    }

    public void setExpense(double expense) {
        this.expense = expense;
    }

    public double getNet() {
        return net;
    }

    public void setNet(double net) {
        this.net = net;
    }

    public double getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(double savingsRate) {
        this.savingsRate = savingsRate;
    }
}