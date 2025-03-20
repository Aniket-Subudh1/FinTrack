package com.personalfinancetracker.backend.dto;

public class FinancialInsight {
    private String title;
    private String description;
    private String type;  // "success", "info", "warning"
    private String icon;

    // Constructors
    public FinancialInsight() {}

    public FinancialInsight(String title, String description, String type, String icon) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.icon = icon;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
