package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.ExpenseCategorySummary;
import com.personalfinancetracker.backend.dto.ExpenseRequest;
import com.personalfinancetracker.backend.dto.ExpenseResponse;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.entities.ExpenseCategoryEnum;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.ExpenseRepository;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    private final ExpenseRepository expenseRepository;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    @Autowired
    public ExpenseController(ExpenseRepository expenseRepository, CustomerRepository customerRepository,
                             JwtUtil jwtUtil, HttpServletRequest request) {
        this.expenseRepository = expenseRepository;
        this.customerRepository = customerRepository;
        this.jwtUtil = jwtUtil;
        this.request = request;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addExpense(@RequestBody ExpenseRequest expenseRequest) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        logger.info("Adding expense for email: {}", email);

        try {
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found for email: {}", email);
                        return new RuntimeException("User not found");
                    });

            ExpenseCategoryEnum category;
            try {
                category = ExpenseCategoryEnum.valueOf(expenseRequest.getCategory().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid expense category: {}", expenseRequest.getCategory());
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("message", "Invalid expense category"));
            }

            Expense expense = new Expense();
            expense.setAmount(expenseRequest.getAmount());
            expense.setCategory(category.name());
            expense.setDate(LocalDateTime.now());
            expense.setCustomer(customer);

            expenseRepository.save(expense);
            logger.info("Expense added successfully for email: {}", email);

            return ResponseEntity.ok(Collections.singletonMap("message", "Expense added successfully"));
        } catch (Exception e) {
            logger.error("Error adding expense: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to add expense"));
        }
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenses() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getExpenses");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching expenses for email: {}", email);

        try {
            List<Expense> expenses = expenseRepository.findByCustomerEmail(email);
            List<ExpenseResponse> expenseResponses = expenses.stream()
                    .map(expense -> new ExpenseResponse(
                            expense.getId(),
                            expense.getAmount(),
                            expense.getCategory(),
                            expense.getDate(),
                            expense.getCustomer().getEmail()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(expenseResponses);
        } catch (Exception e) {
            logger.error("Error fetching expenses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getExpenseCategories() {
        List<String> categoryNames = Arrays.stream(ExpenseCategoryEnum.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categoryNames);
    }
    @GetMapping("/summary")
    public ResponseEntity<List<ExpenseCategorySummary>> getExpenseSummaryByCategory(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        List<ExpenseCategorySummary> summary = expenseRepository.getExpenseSummaryByCategory(email);
        return ResponseEntity.ok(summary);
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