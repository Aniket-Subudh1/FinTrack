package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.FinancialGoalDTO;
import com.personalfinancetracker.backend.dto.MilestoneDTO;
import com.personalfinancetracker.backend.services.FinancialGoalService;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/financial-goals")
public class FinancialGoalController {
    private static final Logger logger = LoggerFactory.getLogger(FinancialGoalController.class);

    private final FinancialGoalService financialGoalService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    @Autowired
    public FinancialGoalController(FinancialGoalService financialGoalService, JwtUtil jwtUtil, HttpServletRequest request) {
        this.financialGoalService = financialGoalService;
        this.jwtUtil = jwtUtil;
        this.request = request;
    }

    @GetMapping
    public ResponseEntity<List<FinancialGoalDTO>> getAllGoals() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<FinancialGoalDTO> goals = financialGoalService.getAllGoalsForUser(email);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            logger.error("Error fetching goals: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinancialGoalDTO> getGoalById(@PathVariable Long id) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            FinancialGoalDTO goal = financialGoalService.getGoalById(id, email);
            return ResponseEntity.ok(goal);
        } catch (RuntimeException e) {
            logger.error("Error fetching goal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<FinancialGoalDTO> createGoal(@RequestBody FinancialGoalDTO goalDTO) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            FinancialGoalDTO createdGoal = financialGoalService.createGoal(goalDTO, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdGoal);
        } catch (Exception e) {
            logger.error("Error creating goal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinancialGoalDTO> updateGoal(@PathVariable Long id, @RequestBody FinancialGoalDTO goalDTO) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            FinancialGoalDTO updatedGoal = financialGoalService.updateGoal(id, goalDTO, email);
            return ResponseEntity.ok(updatedGoal);
        } catch (RuntimeException e) {
            logger.error("Error updating goal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGoal(@PathVariable Long id) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, String> response = new HashMap<>();
        try {
            financialGoalService.deleteGoal(id, email);
            response.put("message", "Goal deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error deleting goal: {}", e.getMessage(), e);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<FinancialGoalDTO> updateGoalProgress(
            @PathVariable Long id,
            @RequestBody Map<String, Double> progressUpdate) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Double newAmount = progressUpdate.get("currentAmount");
        if (newAmount == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            FinancialGoalDTO updatedGoal = financialGoalService.updateGoalProgress(id, newAmount, email);
            return ResponseEntity.ok(updatedGoal);
        } catch (RuntimeException e) {
            logger.error("Error updating goal progress: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/milestones")
    public ResponseEntity<FinancialGoalDTO> addMilestone(
            @PathVariable Long id,
            @RequestBody MilestoneDTO milestoneDTO) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            FinancialGoalDTO updatedGoal = financialGoalService.addGoalMilestone(id, milestoneDTO, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedGoal);
        } catch (RuntimeException e) {
            logger.error("Error adding milestone: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<FinancialGoalDTO>> getActiveGoals() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<FinancialGoalDTO> goals = financialGoalService.getActiveGoals(email);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            logger.error("Error fetching active goals: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/completed")
    public ResponseEntity<List<FinancialGoalDTO>> getCompletedGoals() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<FinancialGoalDTO> goals = financialGoalService.getCompletedGoals(email);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            logger.error("Error fetching completed goals: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FinancialGoalDTO>> getGoalsByCategory(@PathVariable String category) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<FinancialGoalDTO> goals = financialGoalService.getGoalsByCategory(email, category);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            logger.error("Error fetching goals by category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<FinancialGoalDTO>> getUpcomingGoals(
            @RequestParam(defaultValue = "3") int months) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<FinancialGoalDTO> goals = financialGoalService.getUpcomingGoals(email, months);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            logger.error("Error fetching upcoming goals: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/nearly-completed")
    public ResponseEntity<List<FinancialGoalDTO>> getNearlyCompletedGoals() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<FinancialGoalDTO> goals = financialGoalService.getNearlyCompletedGoals(email);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            logger.error("Error fetching nearly completed goals: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/upcoming-milestones")
    public ResponseEntity<List<MilestoneDTO>> getUpcomingMilestones(
            @RequestParam(defaultValue = "30") int days) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<MilestoneDTO> milestones = financialGoalService.getUpcomingMilestones(email, days);
            return ResponseEntity.ok(milestones);
        } catch (Exception e) {
            logger.error("Error fetching upcoming milestones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getEmailFromJwtCookie() {
        String jwt = jwtUtil.getJwtFromCookies(request);
        if (jwt != null) {
            try {
                String email = jwtUtil.extractUsername(jwt);
                if (email != null && !email.isEmpty()) {
                    logger.debug("Extracted email from JWT: {}", email);
                    return email;
                } else {
                    logger.warn("JWT token contains no valid username");
                }
            } catch (Exception e) {
                logger.error("Error extracting email from JWT: {}", e.getMessage());
            }
        } else {
            logger.warn("No JWT token found in cookies");
        }
        return null;
    }
}