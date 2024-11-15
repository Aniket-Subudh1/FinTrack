package com.personalfinancetracker.backend.entities;

public enum ExpenseCategory {
    FOOD_AND_DINING("Food & Dining"),
    TRANSPORTATION("Transportation"),
    SHOPPING("Shopping"),
    UTILITIES("Utilities"),
    HEALTHCARE("Healthcare"),
    ENTERTAINMENT("Entertainment"),
    EDUCATION("Education"),
    HOUSING("Housing"),
    OTHERS("Others");

    private final String displayName;

    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}