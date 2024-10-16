package com.personalfinancetracker.backend.controllers;




import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.services.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping("/add")
    public ResponseEntity<Expense> addExpense(@RequestBody Expense expense) {
        Expense savedExpense = expenseService.addExpense(expense);
        return ResponseEntity.ok(savedExpense); // Return the saved expense
    }
}
