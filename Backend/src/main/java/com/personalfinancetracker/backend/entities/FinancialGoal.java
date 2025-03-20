package com.personalfinancetracker.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class FinancialGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Double targetAmount;
    private Double currentAmount;
    private LocalDate startDate;
    private LocalDate targetDate;
    private String category;
    private String status; // ACTIVE, COMPLETED, ABANDONED
    private String priority; // HIGH, MEDIUM, LOW
    private String color;
    private String icon;

    @ManyToOne
    @JoinColumn(name = "customer_email", referencedColumnName = "email")
    private Customer customer;

    @OneToMany(mappedBy = "financialGoal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalMilestone> milestones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "goal_achievements", joinColumns = @JoinColumn(name = "goal_id"))
    @Column(name = "achievement")
    private List<String> achievements = new ArrayList<>();

    // Constructors, getters, and setters
    public FinancialGoal() {}

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

    public Double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(Double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<GoalMilestone> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<GoalMilestone> milestones) {
        this.milestones = milestones;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<String> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<String> achievements) {
        this.achievements = achievements;
    }

    public void addMilestone(GoalMilestone milestone) {
        milestones.add(milestone);
        milestone.setFinancialGoal(this);
    }

    public void removeMilestone(GoalMilestone milestone) {
        milestones.remove(milestone);
        milestone.setFinancialGoal(null);
    }

    public void addAchievement(String achievement) {
        achievements.add(achievement);
    }

    public double getProgressPercentage() {
        if (targetAmount == 0) return 0;
        return Math.min(100.0, (currentAmount / targetAmount) * 100.0);
    }

    public long getTotalDays() {
        if (startDate == null || targetDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, targetDate);
    }

    public long getDaysRemaining() {
        if (targetDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
    }

    public long getDaysElapsed() {
        if (startDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now());
    }

    public boolean isOnTrack() {
        if (startDate == null || targetDate == null || targetAmount == 0 || currentAmount == 0) return true;

        long totalDays = getTotalDays();
        if (totalDays == 0) return true;

        long daysElapsed = getDaysElapsed();
        double expectedProgress = (double) daysElapsed / totalDays;
        double actualProgress = currentAmount / targetAmount;

        return actualProgress >= expectedProgress;
    }
}