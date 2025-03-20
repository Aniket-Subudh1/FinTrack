package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.dto.FinancialGoalDTO;
import com.personalfinancetracker.backend.dto.MilestoneDTO;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.FinancialGoal;
import com.personalfinancetracker.backend.entities.GoalMilestone;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.FinancialGoalRepository;
import com.personalfinancetracker.backend.repository.GoalMilestoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinancialGoalService {

    private final FinancialGoalRepository financialGoalRepository;
    private final GoalMilestoneRepository milestoneRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public FinancialGoalService(FinancialGoalRepository financialGoalRepository,
                                GoalMilestoneRepository milestoneRepository,
                                CustomerRepository customerRepository) {
        this.financialGoalRepository = financialGoalRepository;
        this.milestoneRepository = milestoneRepository;
        this.customerRepository = customerRepository;
    }

    public List<FinancialGoalDTO> getAllGoalsForUser(String email) {
        List<FinancialGoal> goals = financialGoalRepository.findByCustomerEmail(email);
        return goals.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public FinancialGoalDTO getGoalById(Long id, String email) {
        FinancialGoal goal = financialGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Check if the goal belongs to the user
        if (!goal.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access to goal");
        }

        return convertToDTO(goal);
    }

    @Transactional
    public FinancialGoalDTO createGoal(FinancialGoalDTO goalDTO, String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FinancialGoal goal = new FinancialGoal();
        updateGoalFromDTO(goal, goalDTO);
        goal.setCustomer(customer);
        goal.setStatus("ACTIVE");

        // Set default start date to today if not provided
        if (goal.getStartDate() == null) {
            goal.setStartDate(LocalDate.now());
        }

        // Save the goal first to get an ID
        FinancialGoal savedGoal = financialGoalRepository.save(goal);

        // Create milestones if any
        if (goalDTO.getMilestones() != null && !goalDTO.getMilestones().isEmpty()) {
            for (MilestoneDTO milestoneDTO : goalDTO.getMilestones()) {
                GoalMilestone milestone = new GoalMilestone();
                updateMilestoneFromDTO(milestone, milestoneDTO);
                milestone.setFinancialGoal(savedGoal);
                milestone.setCompleted(false);

                milestoneRepository.save(milestone);
                savedGoal.getMilestones().add(milestone);
            }
        } else {
            // Create default milestones (25%, 50%, 75%, 100%)
            createDefaultMilestones(savedGoal);
        }

        // Generate starter achievement
        savedGoal.addAchievement("Goal Created");

        // Save again with milestones and achievements
        savedGoal = financialGoalRepository.save(savedGoal);

        return convertToDTO(savedGoal);
    }

    @Transactional
    public FinancialGoalDTO updateGoal(Long id, FinancialGoalDTO goalDTO, String email) {
        FinancialGoal goal = financialGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Check if the goal belongs to the user
        if (!goal.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access to goal");
        }

        // Check if the goal was completed with this update
        boolean wasCompleted = "COMPLETED".equals(goal.getStatus());
        boolean willBeCompleted = goalDTO.getCurrentAmount() >= goalDTO.getTargetAmount();

        updateGoalFromDTO(goal, goalDTO);

        // If goal is newly completed, add achievement
        if (!wasCompleted && willBeCompleted) {
            goal.setStatus("COMPLETED");
            goal.addAchievement("Goal Completed");
        }

        // Handle milestones
        if (goalDTO.getMilestones() != null) {
            // Process existing and new milestones
            List<GoalMilestone> existingMilestones = new ArrayList<>(goal.getMilestones());
            List<GoalMilestone> updatedMilestones = new ArrayList<>();

            for (MilestoneDTO milestoneDTO : goalDTO.getMilestones()) {
                GoalMilestone milestone;

                if (milestoneDTO.getId() != null) {
                    // Update existing milestone
                    milestone = milestoneRepository.findById(milestoneDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Milestone not found"));
                    updateMilestoneFromDTO(milestone, milestoneDTO);
                } else {
                    // Create new milestone
                    milestone = new GoalMilestone();
                    updateMilestoneFromDTO(milestone, milestoneDTO);
                    milestone.setFinancialGoal(goal);
                }

                // Check if milestone is completed
                if (goalDTO.getCurrentAmount() >= milestone.getTargetAmount() && Boolean.FALSE.equals(milestone.getCompleted())) {
                    milestone.setCompleted(true);
                    milestone.setCompletedDate(LocalDate.now());
                    goal.addAchievement("Milestone Completed: " + milestone.getTitle());
                }

                milestoneRepository.save(milestone);
                updatedMilestones.add(milestone);
            }

            // Remove milestones that weren't in the update
            for (GoalMilestone existingMilestone : existingMilestones) {
                if (updatedMilestones.stream().noneMatch(m -> m.getId().equals(existingMilestone.getId()))) {
                    goal.removeMilestone(existingMilestone);
                    milestoneRepository.delete(existingMilestone);
                }
            }
        }

        // Save the updated goal
        FinancialGoal updatedGoal = financialGoalRepository.save(goal);
        return convertToDTO(updatedGoal);
    }

    @Transactional
    public void deleteGoal(Long id, String email) {
        FinancialGoal goal = financialGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Check if the goal belongs to the user
        if (!goal.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access to goal");
        }

        financialGoalRepository.delete(goal);
    }

    public List<FinancialGoalDTO> getActiveGoals(String email) {
        List<FinancialGoal> goals = financialGoalRepository.findByCustomerEmailAndStatus(email, "ACTIVE");
        return goals.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<FinancialGoalDTO> getCompletedGoals(String email) {
        List<FinancialGoal> goals = financialGoalRepository.findByCustomerEmailAndStatus(email, "COMPLETED");
        return goals.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<FinancialGoalDTO> getGoalsByCategory(String email, String category) {
        List<FinancialGoal> goals = financialGoalRepository.findByCustomerEmailAndCategory(email, category);
        return goals.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<FinancialGoalDTO> getUpcomingGoals(String email, int months) {
        LocalDate futureDate = LocalDate.now().plusMonths(months);
        List<FinancialGoal> goals = financialGoalRepository.findUpcomingGoals(email, futureDate);
        return goals.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<FinancialGoalDTO> getNearlyCompletedGoals(String email) {
        List<FinancialGoal> goals = financialGoalRepository.findNearlyCompletedGoals(email);
        return goals.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MilestoneDTO> getUpcomingMilestones(String email, int days) {
        LocalDate now = LocalDate.now();
        LocalDate futureDate = now.plusDays(days);

        List<GoalMilestone> milestones = milestoneRepository.findUpcomingMilestones(
                email, now, futureDate);

        return milestones.stream()
                .map(this::convertMilestoneToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public FinancialGoalDTO updateGoalProgress(Long id, Double newAmount, String email) {
        FinancialGoal goal = financialGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Check if the goal belongs to the user
        if (!goal.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access to goal");
        }

        // Update current amount
        goal.setCurrentAmount(newAmount);

        // Check if goal is completed
        if (newAmount >= goal.getTargetAmount() && !"COMPLETED".equals(goal.getStatus())) {
            goal.setStatus("COMPLETED");
            goal.addAchievement("Goal Completed");
        }

        // Check for milestone completions
        for (GoalMilestone milestone : goal.getMilestones()) {
            if (Boolean.FALSE.equals(milestone.getCompleted()) && newAmount >= milestone.getTargetAmount()) {
                milestone.setCompleted(true);
                milestone.setCompletedDate(LocalDate.now());
                goal.addAchievement("Milestone Completed: " + milestone.getTitle());
            }
        }

        FinancialGoal updatedGoal = financialGoalRepository.save(goal);
        return convertToDTO(updatedGoal);
    }

    @Transactional
    public FinancialGoalDTO addGoalMilestone(Long goalId, MilestoneDTO milestoneDTO, String email) {
        FinancialGoal goal = financialGoalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Check if the goal belongs to the user
        if (!goal.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access to goal");
        }

        GoalMilestone milestone = new GoalMilestone();
        updateMilestoneFromDTO(milestone, milestoneDTO);
        milestone.setFinancialGoal(goal);
        milestone.setCompleted(false);

        milestoneRepository.save(milestone);
        goal.getMilestones().add(milestone);

        // Check if milestone is immediately completed based on current progress
        if (goal.getCurrentAmount() >= milestone.getTargetAmount()) {
            milestone.setCompleted(true);
            milestone.setCompletedDate(LocalDate.now());
            goal.addAchievement("Milestone Completed: " + milestone.getTitle());
        }

        FinancialGoal updatedGoal = financialGoalRepository.save(goal);
        return convertToDTO(updatedGoal);
    }

    // Helper methods
    private FinancialGoalDTO convertToDTO(FinancialGoal goal) {
        FinancialGoalDTO dto = new FinancialGoalDTO();
        dto.setId(goal.getId());
        dto.setTitle(goal.getTitle());
        dto.setDescription(goal.getDescription());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setCurrentAmount(goal.getCurrentAmount());
        dto.setStartDate(goal.getStartDate());
        dto.setTargetDate(goal.getTargetDate());
        dto.setCategory(goal.getCategory());
        dto.setStatus(goal.getStatus());
        dto.setPriority(goal.getPriority());
        dto.setColor(goal.getColor());
        dto.setIcon(goal.getIcon());

        // Set computed fields
        dto.setProgressPercentage(goal.getProgressPercentage());
        dto.setDaysRemaining(goal.getDaysRemaining());
        dto.setDaysElapsed(goal.getDaysElapsed());
        dto.setTotalDays(goal.getTotalDays());
        dto.setOnTrack(goal.isOnTrack());

        // Convert milestones
        if (goal.getMilestones() != null) {
            dto.setMilestones(goal.getMilestones().stream()
                    .map(this::convertMilestoneToDTO)
                    .collect(Collectors.toList()));
        }

        // Set achievements
        if (goal.getAchievements() != null) {
            dto.setAchievements(new ArrayList<>(goal.getAchievements()));
        }

        return dto;
    }

    private MilestoneDTO convertMilestoneToDTO(GoalMilestone milestone) {
        MilestoneDTO dto = new MilestoneDTO();
        dto.setId(milestone.getId());
        dto.setTitle(milestone.getTitle());
        dto.setDescription(milestone.getDescription());
        dto.setTargetAmount(milestone.getTargetAmount());
        dto.setTargetDate(milestone.getTargetDate());
        dto.setCompleted(milestone.getCompleted());
        dto.setCompletedDate(milestone.getCompletedDate());
        dto.setProgressPercentage(milestone.getProgressPercentage());
        dto.setOnTrack(milestone.isOnTrack());
        return dto;
    }

    private void updateGoalFromDTO(FinancialGoal goal, FinancialGoalDTO dto) {
        goal.setTitle(dto.getTitle());
        goal.setDescription(dto.getDescription());
        goal.setTargetAmount(dto.getTargetAmount());
        goal.setCurrentAmount(dto.getCurrentAmount());
        goal.setStartDate(dto.getStartDate());
        goal.setTargetDate(dto.getTargetDate());
        goal.setCategory(dto.getCategory());
        goal.setStatus(dto.getStatus());
        goal.setPriority(dto.getPriority());
        goal.setColor(dto.getColor());
        goal.setIcon(dto.getIcon());
    }

    private void updateMilestoneFromDTO(GoalMilestone milestone, MilestoneDTO dto) {
        milestone.setTitle(dto.getTitle());
        milestone.setDescription(dto.getDescription());
        milestone.setTargetAmount(dto.getTargetAmount());
        milestone.setTargetDate(dto.getTargetDate());
        if (dto.getCompleted() != null) {
            milestone.setCompleted(dto.getCompleted());
        }
        if (dto.getCompletedDate() != null) {
            milestone.setCompletedDate(dto.getCompletedDate());
        }
    }

    private void createDefaultMilestones(FinancialGoal goal) {
        double targetAmount = goal.getTargetAmount();
        LocalDate startDate = goal.getStartDate();
        LocalDate targetDate = goal.getTargetDate();
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, targetDate);

        // Create 25% milestone
        GoalMilestone milestone25 = new GoalMilestone();
        milestone25.setTitle("25% Milestone");
        milestone25.setDescription("Reached 25% of goal");
        milestone25.setTargetAmount(targetAmount * 0.25);
        milestone25.setTargetDate(startDate.plusDays(totalDays / 4));
        milestone25.setCompleted(false);
        milestone25.setFinancialGoal(goal);
        milestoneRepository.save(milestone25);
        goal.getMilestones().add(milestone25);

        // Create 50% milestone
        GoalMilestone milestone50 = new GoalMilestone();
        milestone50.setTitle("Halfway There!");
        milestone50.setDescription("Reached 50% of goal");
        milestone50.setTargetAmount(targetAmount * 0.5);
        milestone50.setTargetDate(startDate.plusDays(totalDays / 2));
        milestone50.setCompleted(false);
        milestone50.setFinancialGoal(goal);
        milestoneRepository.save(milestone50);
        goal.getMilestones().add(milestone50);

        // Create 75% milestone
        GoalMilestone milestone75 = new GoalMilestone();
        milestone75.setTitle("75% Milestone");
        milestone75.setDescription("Reached 75% of goal");
        milestone75.setTargetAmount(targetAmount * 0.75);
        milestone75.setTargetDate(startDate.plusDays(totalDays * 3 / 4));
        milestone75.setCompleted(false);
        milestone75.setFinancialGoal(goal);
        milestoneRepository.save(milestone75);
        goal.getMilestones().add(milestone75);

        // Create 100% milestone
        GoalMilestone milestone100 = new GoalMilestone();
        milestone100.setTitle("Goal Complete!");
        milestone100.setDescription("Reached 100% of goal");
        milestone100.setTargetAmount(targetAmount);
        milestone100.setTargetDate(targetDate);
        milestone100.setCompleted(false);
        milestone100.setFinancialGoal(goal);
        milestoneRepository.save(milestone100);
        goal.getMilestones().add(milestone100);
    }
}