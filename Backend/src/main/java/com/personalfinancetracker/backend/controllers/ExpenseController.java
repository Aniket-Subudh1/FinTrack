package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.ExpenseDTO;
import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.entities.ExpenseCategory;
import com.personalfinancetracker.backend.services.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Autowired
    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<Expense> createExpense(@RequestBody ExpenseDTO expenseDTO, Authentication authentication) {
        String userName = authentication.getName();  // retrieve the username

        // Create a new Expense object and set its fields based on the DTO and user's input
        Expense expense = new Expense();
        expense.setUserName(userName);  // associate the expense with the logged-in user
        expense.setAmount(expenseDTO.getAmount());

        // Set category and customCategory based on the DTO
        if (expenseDTO.getCategory() == ExpenseCategory.OTHERS && expenseDTO.getCustomCategory() != null) {
            expense.setCategory(ExpenseCategory.OTHERS);
            expense.setCustomCategory(expenseDTO.getCustomCategory());
        } else {
            expense.setCategory(expenseDTO.getCategory());
            expense.setCustomCategory(null);  // clear custom category if not 'OTHERS'
        }

        // Save the expense and return the response
        Expense newExpense = expenseService.saveExpense(expense);
        return new ResponseEntity<>(newExpense, HttpStatus.CREATED);
    }
}
