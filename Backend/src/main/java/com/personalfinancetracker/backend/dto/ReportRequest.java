package com.personalfinancetracker.backend.dto;

public class ReportRequest {
    private String email;
    private String startDate;
    private String endDate;
    private String reportTitle;
    private boolean includeExpenses;
    private boolean includeIncomes;
    private boolean includeBudgets;

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public String getReportTitle() { return reportTitle; }
    public void setReportTitle(String reportTitle) { this.reportTitle = reportTitle; }
    public boolean getIncludeExpenses() { return includeExpenses; }
    public void setIncludeExpenses(boolean includeExpenses) { this.includeExpenses = includeExpenses; }
    public boolean getIncludeIncomes() { return includeIncomes; }
    public void setIncludeIncomes(boolean includeIncomes) { this.includeIncomes = includeIncomes; }
    public boolean getIncludeBudgets() { return includeBudgets; }
    public void setIncludeBudgets(boolean includeBudgets) { this.includeBudgets = includeBudgets; }
}