package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.*;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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

            // Add tags if provided
            if (expenseRequest.getTags() != null && !expenseRequest.getTags().isEmpty()) {
                expense.setTags(String.join(",", expenseRequest.getTags()));
            }

            // Add notes if provided
            if (expenseRequest.getNote() != null && !expenseRequest.getNote().isEmpty()) {
                expense.setNote(expenseRequest.getNote());
            }

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
                    .map(expense -> {
                        ExpenseResponse response = new ExpenseResponse(
                                expense.getId(),
                                expense.getAmount(),
                                expense.getCategory(),
                                expense.getDate(),
                                expense.getCustomer().getEmail()
                        );
                        // Set additional fields if they exist
                        if (expense.getTags() != null && !expense.getTags().isEmpty()) {
                            response.setTags(Arrays.asList(expense.getTags().split(",")));
                        }
                        if (expense.getNote() != null) {
                            response.setNote(expense.getNote());
                        }
                        return response;
                    })
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

    @GetMapping("/filter")
    public ResponseEntity<List<ExpenseResponse>> filterExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String tags) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for filterExpenses");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Expense> expenses = expenseRepository.findByCustomerEmail(email);

            // Apply filters
            if (startDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                expenses = expenses.stream()
                        .filter(expense -> expense.getDate().isAfter(startDateTime) || expense.getDate().isEqual(startDateTime))
                        .collect(Collectors.toList());
            }

            if (endDate != null) {
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
                expenses = expenses.stream()
                        .filter(expense -> expense.getDate().isBefore(endDateTime))
                        .collect(Collectors.toList());
            }

            if (category != null && !category.isEmpty()) {
                expenses = expenses.stream()
                        .filter(expense -> expense.getCategory().equalsIgnoreCase(category))
                        .collect(Collectors.toList());
            }

            if (minAmount != null) {
                expenses = expenses.stream()
                        .filter(expense -> expense.getAmount() >= minAmount)
                        .collect(Collectors.toList());
            }

            if (maxAmount != null) {
                expenses = expenses.stream()
                        .filter(expense -> expense.getAmount() <= maxAmount)
                        .collect(Collectors.toList());
            }

            if (tags != null && !tags.isEmpty()) {
                String[] tagArray = tags.split(",");
                expenses = expenses.stream()
                        .filter(expense -> {
                            if (expense.getTags() == null) return false;
                            for (String tag : tagArray) {
                                if (expense.getTags().contains(tag.trim())) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
            }

            // Convert to response objects
            List<ExpenseResponse> expenseResponses = expenses.stream()
                    .map(expense -> {
                        ExpenseResponse response = new ExpenseResponse(
                                expense.getId(),
                                expense.getAmount(),
                                expense.getCategory(),
                                expense.getDate(),
                                expense.getCustomer().getEmail()
                        );
                        // Set additional fields if they exist
                        if (expense.getTags() != null && !expense.getTags().isEmpty()) {
                            response.setTags(Arrays.asList(expense.getTags().split(",")));
                        }
                        if (expense.getNote() != null) {
                            response.setNote(expense.getNote());
                        }
                        return response;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(expenseResponses);
        } catch (Exception e) {
            logger.error("Error filtering expenses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/trends")
    public ResponseEntity<List<ExpenseTrend>> getExpenseTrends() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getExpenseTrends");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get past 6 months of data
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateAfter(email, sixMonthsAgo);

            // Group by month and category
            Map<String, Map<String, Double>> monthCategoryTotals = new HashMap<>();

            for (Expense expense : expenses) {
                String monthKey = expense.getDate().getYear() + "-" + String.format("%02d", expense.getDate().getMonthValue());
                String category = expense.getCategory();

                monthCategoryTotals.putIfAbsent(monthKey, new HashMap<>());
                Map<String, Double> categoryTotals = monthCategoryTotals.get(monthKey);

                double currentTotal = categoryTotals.getOrDefault(category, 0.0);
                categoryTotals.put(category, currentTotal + expense.getAmount());
            }

            // Convert to response objects
            List<ExpenseTrend> trends = new ArrayList<>();
            for (Map.Entry<String, Map<String, Double>> entry : monthCategoryTotals.entrySet()) {
                String month = entry.getKey();
                Map<String, Double> categoryTotals = entry.getValue();

                for (Map.Entry<String, Double> categoryEntry : categoryTotals.entrySet()) {
                    trends.add(new ExpenseTrend(month, categoryEntry.getKey(), categoryEntry.getValue()));
                }
            }

            // Sort by month
            trends.sort(Comparator.comparing(ExpenseTrend::getMonth));

            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            logger.error("Error getting expense trends: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteExpense(@PathVariable Long id) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for deleteExpense");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        try {
            // Verify the expense belongs to the authenticated user
            Expense expense = expenseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Expense not found"));

            if (!expense.getCustomer().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "Not authorized to delete this expense"));
            }

            expenseRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Expense deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting expense: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to delete expense"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateExpense(@PathVariable Long id, @RequestBody ExpenseRequest expenseRequest) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for updateExpense");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        try {
            // Verify the expense belongs to the authenticated user
            Expense expense = expenseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Expense not found"));

            if (!expense.getCustomer().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "Not authorized to update this expense"));
            }

            // Update the expense
            if (expenseRequest.getAmount() != null) {
                expense.setAmount(expenseRequest.getAmount());
            }

            if (expenseRequest.getCategory() != null && !expenseRequest.getCategory().isEmpty()) {
                try {
                    ExpenseCategoryEnum category = ExpenseCategoryEnum.valueOf(expenseRequest.getCategory().toUpperCase());
                    expense.setCategory(category.name());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(Collections.singletonMap("message", "Invalid expense category"));
                }
            }

            if (expenseRequest.getTags() != null) {
                expense.setTags(String.join(",", expenseRequest.getTags()));
            }

            if (expenseRequest.getNote() != null) {
                expense.setNote(expenseRequest.getNote());
            }

            expenseRepository.save(expense);
            return ResponseEntity.ok(Collections.singletonMap("message", "Expense updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating expense: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to update expense"));
        }
    }

    @GetMapping("/budget-status")
    public ResponseEntity<List<BudgetStatus>> getBudgetStatus() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getBudgetStatus");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get current month expenses
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateAfter(email, startOfMonth);

            // Get budget data from local storage (normally this would be stored in the database)
            // For demonstration purposes, we'll create dummy budget data for each category
            Map<String, Double> categoryBudgets = new HashMap<>();
            for (ExpenseCategoryEnum category : ExpenseCategoryEnum.values()) {
                switch (category) {
                    case GROCERY:
                        categoryBudgets.put(category.name(), 500.0);
                        break;
                    case UTILITIES:
                        categoryBudgets.put(category.name(), 300.0);
                        break;
                    case ENTERTAINMENT:
                        categoryBudgets.put(category.name(), 200.0);
                        break;
                    default:
                        categoryBudgets.put(category.name(), 100.0);
                        break;
                }
            }

            // Calculate current spending by category
            Map<String, Double> categorySpending = new HashMap<>();
            for (Expense expense : expenses) {
                String category = expense.getCategory();
                double currentAmount = categorySpending.getOrDefault(category, 0.0);
                categorySpending.put(category, currentAmount + expense.getAmount());
            }

            // Create budget status response
            List<BudgetStatus> budgetStatusList = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categoryBudgets.entrySet()) {
                String category = entry.getKey();
                double budgetAmount = entry.getValue();
                double spentAmount = categorySpending.getOrDefault(category, 0.0);
                double remainingAmount = budgetAmount - spentAmount;
                double percentUsed = (spentAmount / budgetAmount) * 100;

                BudgetStatus status = new BudgetStatus(
                        category,
                        budgetAmount,
                        spentAmount,
                        remainingAmount,
                        percentUsed
                );

                budgetStatusList.add(status);
            }

            return ResponseEntity.ok(budgetStatusList);
        } catch (Exception e) {
            logger.error("Error getting budget status: {}", e.getMessage(), e);
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