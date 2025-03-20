package com.personalfinancetracker.backend.dto;

public class SaveReportRequest {
    private String reportTitle;
    private String reportType;
    private String configuration;

    // Constructors
    public SaveReportRequest() {}

    // Getters and Setters
    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}
