package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.ExpenseRequest;
import com.personalfinancetracker.backend.dto.ExpenseResponse;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.entities.ExpenseCategoryEnum;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final ExpenseRepository expenseRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public ExpenseController(ExpenseRepository expenseRepository, CustomerRepository customerRepository) {
        this.expenseRepository = expenseRepository;
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addExpense(@RequestBody ExpenseRequest expenseRequest, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        String email = authentication.getName();

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate that the category exists in the Enum
        ExpenseCategoryEnum category;
        try {
            category = ExpenseCategoryEnum.valueOf(expenseRequest.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", "Invalid expense category"));
        }

        Expense expense = new Expense();
        expense.setAmount(expenseRequest.getAmount());
        expense.setCategory(category.name()); // Save the category as a String
        expense.setDate(LocalDateTime.now());
        expense.setCustomer(customer);

        expenseRepository.save(expense);

        return ResponseEntity.ok(Collections.singletonMap("message", "Expense added successfully"));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenses(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();

        List<Expense> expenses = expenseRepository.findByCustomerEmail(email);

        // Map to ExpenseResponse DTOs
        List<ExpenseResponse> expenseResponses = expenses.stream()
                .map(expense -> new ExpenseResponse(
                        expense.getId(),
                        expense.getAmount(),
                        expense.getCategory(), // This will return the category name as a string
                        expense.getDate(),
                        expense.getCustomer().getEmail()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(expenseResponses);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getExpenseCategories() {
        // Fetch all categories from the Enum
        List<String> categoryNames = Arrays.stream(ExpenseCategoryEnum.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categoryNames);
    }
}
