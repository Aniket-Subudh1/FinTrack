package com.personalfinancetracker.backend.dto;

import java.time.LocalDateTime;

public class SavedReportDTO {
    private Long id;
    private String reportTitle;
    private String reportType;
    private String configuration;
    private LocalDateTime createdDate;

    // Constructors
    public SavedReportDTO() {}

    public SavedReportDTO(Long id, String reportTitle, String reportType, String configuration, LocalDateTime createdDate) {
        this.id = id;
        this.reportTitle = reportTitle;
        this.reportType = reportType;
        this.configuration = configuration;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}