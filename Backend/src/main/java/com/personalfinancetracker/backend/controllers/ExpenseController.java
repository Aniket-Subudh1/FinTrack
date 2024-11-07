package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.entities.Expense;
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
    public ResponseEntity<Expense> createExpense(@RequestBody Expense expense, Authentication authentication) {
        String userName = authentication.getName();  // retrieve the username
        expense.setUserName(userName);  // associate the expense with the logged-in user

        Expense newExpense = expenseService.saveExpense(expense);
        return new ResponseEntity<>(newExpense, HttpStatus.CREATED);
    }

}
