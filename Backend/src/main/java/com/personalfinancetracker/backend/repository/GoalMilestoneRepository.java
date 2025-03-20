package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.GoalMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GoalMilestoneRepository extends JpaRepository<GoalMilestone, Long> {
    List<GoalMilestone> findByFinancialGoalId(Long financialGoalId);

    @Query("SELECT m FROM GoalMilestone m WHERE m.financialGoal.id = :goalId AND m.completed = false ORDER BY m.targetDate ASC")
    List<GoalMilestone> findNextMilestonesByGoalId(@Param("goalId") Long goalId);

    @Query("SELECT m FROM GoalMilestone m WHERE m.financialGoal.customer.email = :email AND m.targetDate BETWEEN :startDate AND :endDate")
    List<GoalMilestone> findUpcomingMilestones(@Param("email") String email,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
}