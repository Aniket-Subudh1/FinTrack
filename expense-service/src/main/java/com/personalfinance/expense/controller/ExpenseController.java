package com.personalfinance.expense.controller;

import com.personalfinance.expense.dto.ExpenseRequest;
import com.personalfinance.expense.dto.ExpenseResponse;
import com.personalfinance.expense.dto.ExpenseSummary;
import com.personalfinance.expense.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * Create a new expense
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @RequestHeader("X-User-Id") String userEmail,
            @Valid @RequestBody ExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.createExpense(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all expenses for a user
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(
            @RequestHeader("X-User-Id") String userEmail
    ) {
        List<ExpenseResponse> expenses = expenseService.getAllExpenses(userEmail);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get paginated expenses for a user
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @GetMapping("/paginated")
    public ResponseEntity<Map<String, Object>> getPaginatedExpenses(
            @RequestHeader("X-User-Id") String userEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<ExpenseResponse> expensePage = expenseService.getExpenses(userEmail, pageRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("expenses", expensePage.getContent());
        response.put("currentPage", expensePage.getNumber());
        response.put("totalItems", expensePage.getTotalElements());
        response.put("totalPages", expensePage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Get expense by ID
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpenseById(
            @RequestHeader("X-User-Id") String userEmail,
            @PathVariable Long id
    ) {
        ExpenseResponse response = expenseService.getExpenseById(userEmail, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update expense
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @RequestHeader("X-User-Id") String userEmail,
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.updateExpense(userEmail, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete expense
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @RequestHeader("X-User-Id") String userEmail,
            @PathVariable Long id
    ) {
        expenseService.deleteExpense(userEmail, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Filter expenses by category
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @GetMapping("/filter/category/{category}")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByCategory(
            @RequestHeader("X-User-Id") String userEmail,
            @PathVariable String category
    ) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByCategory(userEmail, category);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Filter expenses by date range
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @GetMapping("/filter/date")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByDateRange(
            @RequestHeader("X-User-Id") String userEmail,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByDateRange(userEmail, startDate, endDate);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Filter expenses by category and date range
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @GetMapping("/filter/category-date")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByCategoryAndDateRange(
            @RequestHeader("X-User-Id") String userEmail,
            @RequestParam String category,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByCategoryAndDateRange(
                userEmail, category, startDate, endDate);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get expense summary by category for a given date range
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @GetMapping("/summary/category")
    public ResponseEntity<List<ExpenseSummary>> getExpenseSummaryByCategory(
            @RequestHeader("X-User-Id") String userEmail,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<ExpenseSummary> summary = expenseService.getExpenseSummaryByCategory(userEmail, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get expense summary by month for a given date range
     * The user email is extracted from the X-User-Id header set by the API Gateway
     */
    @GetMapping("/summary/month")
    public ResponseEntity<List<ExpenseSummary>> getExpenseSummaryByMonth(
            @RequestHeader("X-User-Id") String userEmail,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<ExpenseSummary> summary = expenseService.getExpenseSummaryByMonth(userEmail, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get all expense categories
     * This endpoint is publicly accessible (no authentication required)
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllExpenseCategories() {
        List<String> categories = expenseService.getAllExpenseCategories();
        return ResponseEntity.ok(categories);
    }
}