package com.personalfinancetracker.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class GoalMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Double targetAmount;
    private LocalDate targetDate;
    private Boolean completed;
    private LocalDate completedDate;

    @ManyToOne
    @JoinColumn(name = "goal_id")
    private FinancialGoal financialGoal;

    // Constructors, getters, and setters
    public GoalMilestone() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(Double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    public FinancialGoal getFinancialGoal() {
        return financialGoal;
    }

    public void setFinancialGoal(FinancialGoal financialGoal) {
        this.financialGoal = financialGoal;
    }

    public double getProgressPercentage() {
        if (financialGoal == null || financialGoal.getCurrentAmount() == 0 || targetAmount == 0) {
            return 0;
        }
        return Math.min(100.0, (financialGoal.getCurrentAmount() / targetAmount) * 100.0);
    }

    public boolean isOnTrack() {
        if (targetDate == null || financialGoal == null ||
                financialGoal.getStartDate() == null || targetAmount == 0) {
            return true;
        }

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(
                financialGoal.getStartDate(), targetDate);
        if (totalDays == 0) return true;

        long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(
                financialGoal.getStartDate(), LocalDate.now());

        double expectedProgress = (double) daysElapsed / totalDays;
        double actualProgress = financialGoal.getCurrentAmount() / targetAmount;

        return actualProgress >= expectedProgress;
    }
}