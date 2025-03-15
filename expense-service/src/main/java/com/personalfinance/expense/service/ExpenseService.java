package com.personalfinance.expense.service;

import com.personalfinance.expense.dto.ExpenseRequest;
import com.personalfinance.expense.dto.ExpenseResponse;
import com.personalfinance.expense.dto.ExpenseSummary;
import com.personalfinance.expense.entity.Expense;
import com.personalfinance.expense.entity.ExpenseCategory;
import com.personalfinance.expense.exception.ExpenseException;
import com.personalfinance.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    /**
     * Create a new expense
     */
    public ExpenseResponse createExpense(String userEmail, ExpenseRequest request) {
        ExpenseCategory category;
        try {
            category = ExpenseCategory.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ExpenseException("Invalid expense category: " + request.getCategory());
        }

        LocalDateTime expenseDate = request.getDate() != null ? request.getDate() : LocalDateTime.now();

        Expense expense = Expense.builder()
                .amount(request.getAmount())
                .category(category)
                .date(expenseDate)
                .description(request.getDescription())
                .userEmail(userEmail)
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        return mapToExpenseResponse(savedExpense);
    }

    /**
     * Get all expenses for a user
     */
    public List<ExpenseResponse> getAllExpenses(String userEmail) {
        List<Expense> expenses = expenseRepository.findByUserEmail(userEmail);
        return expenses.stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated expenses for a user
     */
    public Page<ExpenseResponse> getExpenses(String userEmail, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByUserEmail(userEmail, pageable);
        return expenses.map(this::mapToExpenseResponse);
    }

    /**
     * Get expense by ID
     */
    public ExpenseResponse getExpenseById(String userEmail, Long id) {
        Expense expense = expenseRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ExpenseException("Expense not found with id: " + id));
        return mapToExpenseResponse(expense);
    }

    /**
     * Update expense
     */
    @Transactional
    public ExpenseResponse updateExpense(String userEmail, Long id, ExpenseRequest request) {
        Expense expense = expenseRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ExpenseException("Expense not found with id: " + id));

        ExpenseCategory category;
        try {
            category = ExpenseCategory.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ExpenseException("Invalid expense category: " + request.getCategory());
        }

        expense.setAmount(request.getAmount());
        expense.setCategory(category);
        if (request.getDate() != null) {
            expense.setDate(request.getDate());
        }
        expense.setDescription(request.getDescription());

        Expense updatedExpense = expenseRepository.save(expense);
        return mapToExpenseResponse(updatedExpense);
    }

    /**
     * Delete expense
     */
    @Transactional
    public void deleteExpense(String userEmail, Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new ExpenseException("Expense not found with id: " + id);
        }
        expenseRepository.deleteByIdAndUserEmail(id, userEmail);
    }

    /**
     * Filter expenses by category
     */
    public List<ExpenseResponse> getExpensesByCategory(String userEmail, String categoryName) {
        ExpenseCategory category;
        try {
            category = ExpenseCategory.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ExpenseException("Invalid expense category: " + categoryName);
        }

        List<Expense> expenses = expenseRepository.findByUserEmailAndCategoryOrderByDateDesc(userEmail, category);
        return expenses.stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    /**
     * Filter expenses by date range
     */
    public List<ExpenseResponse> getExpensesByDateRange(String userEmail, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Expense> expenses = expenseRepository.findByUserEmailAndDateBetweenOrderByDateDesc(
                userEmail, startDateTime, endDateTime);

        return expenses.stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    /**
     * Filter expenses by category and date range
     */
    public List<ExpenseResponse> getExpensesByCategoryAndDateRange(
            String userEmail, String categoryName, LocalDate startDate, LocalDate endDate) {

        ExpenseCategory category;
        try {
            category = ExpenseCategory.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ExpenseException("Invalid expense category: " + categoryName);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Expense> expenses = expenseRepository.findByUserEmailAndCategoryAndDateBetweenOrderByDateDesc(
                userEmail, category, startDateTime, endDateTime);

        return expenses.stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get expense summary by category for a given date range
     */
    public List<ExpenseSummary> getExpenseSummaryByCategory(String userEmail, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> results = expenseRepository.findExpenseSummaryByCategory(userEmail, startDateTime, endDateTime);

        // Calculate total for percentage
        double total = results.stream()
                .mapToDouble(result -> (Double) result[1])
                .sum();

        return results.stream()
                .map(result -> {
                    ExpenseCategory category = (ExpenseCategory) result[0];
                    Double amount = (Double) result[1];
                    Double percentage = (total > 0) ? (amount / total) * 100 : 0;

                    return ExpenseSummary.builder()
                            .category(category.name())
                            .period(startDate + " to " + endDate)
                            .amount(amount)
                            .percentage(Math.round(percentage * 100.0) / 100.0) // Round to 2 decimal places
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get expense summary by month for a given date range
     */
    public List<ExpenseSummary> getExpenseSummaryByMonth(String userEmail, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> results = expenseRepository.findExpenseSummaryByMonth(userEmail, startDateTime, endDateTime);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        return results.stream()
                .map(result -> {
                    String monthStr = (String) result[0];
                    YearMonth yearMonth = YearMonth.parse(monthStr, formatter);
                    Double amount = (Double) result[1];

                    return ExpenseSummary.builder()
                            .period(yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
                            .amount(amount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all expense categories
     */
    public List<String> getAllExpenseCategories() {
        return Arrays.stream(ExpenseCategory.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    /**
     * Map Expense entity to ExpenseResponse DTO
     */
    private ExpenseResponse mapToExpenseResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .category(expense.getCategory().name())
                .date(expense.getDate())
                .description(expense.getDescription())
                .userEmail(expense.getUserEmail())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}