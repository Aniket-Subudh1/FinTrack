package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.BudgetItemDTO;
import com.personalfinancetracker.backend.dto.SavingsGoalDTO;
import com.personalfinancetracker.backend.entities.Budget;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.SavingsGoal;
import com.personalfinancetracker.backend.repository.BudgetRepository;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.SavingsGoalRepository;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {
    private static final Logger logger = LoggerFactory.getLogger(BudgetController.class);

    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    @Autowired
    public BudgetController(BudgetRepository budgetRepository,
                            SavingsGoalRepository savingsGoalRepository,
                            CustomerRepository customerRepository,
                            JwtUtil jwtUtil,
                            HttpServletRequest request) {
        this.budgetRepository = budgetRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.customerRepository = customerRepository;
        this.jwtUtil = jwtUtil;
        this.request = request;
    }

    @GetMapping
    public ResponseEntity<List<BudgetItemDTO>> getBudgetItems() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getBudgetItems");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching budget items for email: {}", email);

        try {
            List<Budget> budgets = budgetRepository.findByCustomerEmail(email);
            List<BudgetItemDTO> budgetItems = budgets.stream()
                    .map(budget -> new BudgetItemDTO(budget.getCategory(), budget.getAmount()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(budgetItems);
        } catch (Exception e) {
            logger.error("Error fetching budget items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addOrUpdateBudgetItem(@RequestBody BudgetItemDTO budgetItemDTO) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        logger.info("Adding/updating budget item for email: {}", email);

        try {
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found for email: {}", email);
                        return new RuntimeException("User not found");
                    });

            // Check if the budget item already exists
            Optional<Budget> existingBudget = budgetRepository.findByCustomerEmailAndCategory(email, budgetItemDTO.getCategory());

            Budget budget;
            if (existingBudget.isPresent()) {
                budget = existingBudget.get();
                budget.setAmount(budgetItemDTO.getAmount());
            } else {
                budget = new Budget();
                budget.setCategory(budgetItemDTO.getCategory());
                budget.setAmount(budgetItemDTO.getAmount());
                budget.setCustomer(customer);
            }

            budgetRepository.save(budget);
            logger.info("Budget item saved successfully for email: {}", email);

            return ResponseEntity.ok(Collections.singletonMap("message", "Budget item saved successfully"));
        } catch (Exception e) {
            logger.error("Error saving budget item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to save budget item"));
        }
    }

    @DeleteMapping("/{category}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteBudgetItem(@PathVariable String category) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for deleteBudgetItem");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        try {
            // Delete the budget item
            budgetRepository.deleteByCustomerEmailAndCategory(email, category);
            logger.info("Budget item deleted successfully for email: {} and category: {}", email, category);

            return ResponseEntity.ok(Collections.singletonMap("message", "Budget item deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting budget item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to delete budget item"));
        }
    }

    @GetMapping("/savings-goal")
    public ResponseEntity<SavingsGoalDTO> getSavingsGoal() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getSavingsGoal");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching savings goal for email: {}", email);

        try {
            Optional<SavingsGoal> savingsGoal = savingsGoalRepository.findByCustomerEmail(email);
            Double amount = savingsGoal.map(SavingsGoal::getAmount).orElse(0.0);

            return ResponseEntity.ok(new SavingsGoalDTO(amount));
        } catch (Exception e) {
            logger.error("Error fetching savings goal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/savings-goal")
    public ResponseEntity<Map<String, String>> setSavingsGoal(@RequestBody SavingsGoalDTO savingsGoalDTO) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        if (savingsGoalDTO.getAmount() == null) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", "Amount is required"));
        }

        logger.info("Setting savings goal for email: {}", email);

        try {
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found for email: {}", email);
                        return new RuntimeException("User not found");
                    });

            // Check if the savings goal already exists
            Optional<SavingsGoal> existingSavingsGoal = savingsGoalRepository.findByCustomerEmail(email);

            SavingsGoal savingsGoal;
            if (existingSavingsGoal.isPresent()) {
                savingsGoal = existingSavingsGoal.get();
                savingsGoal.setAmount(savingsGoalDTO.getAmount());
            } else {
                savingsGoal = new SavingsGoal();
                savingsGoal.setAmount(savingsGoalDTO.getAmount());
                savingsGoal.setCustomer(customer);
            }

            savingsGoalRepository.save(savingsGoal);
            logger.info("Savings goal saved successfully for email: {}", email);

            return ResponseEntity.ok(Collections.singletonMap("message", "Savings goal saved successfully"));
        } catch (Exception e) {
            logger.error("Error saving savings goal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to save savings goal"));
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