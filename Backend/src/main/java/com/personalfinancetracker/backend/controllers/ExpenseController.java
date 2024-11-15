package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.CategoryDTO;
import com.personalfinancetracker.backend.dto.ExpenseDTO;
import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.exceptions.InvalidExpenseException;
import com.personalfinancetracker.backend.services.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "http://localhost:4200") // For Angular frontend
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> getCategories() {
        return ResponseEntity.ok(expenseService.getAllCategories());
    }

    @PostMapping
    public ResponseEntity<?> addExpense(
            @RequestHeader("X-User-Name") String userName,
            @RequestBody ExpenseDTO expenseDTO) {
        try {
            Expense savedExpense = expenseService.addExpense(userName, expenseDTO);
            return ResponseEntity.ok(savedExpense);
        } catch (InvalidExpenseException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}