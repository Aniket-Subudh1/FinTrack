package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    // Save a new expense
    public Expense saveExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    // Retrieve expenses for a specific user
    public List<Expense> getExpensesByUserName(String userName) {
        return expenseRepository.findByUserName(userName);
    }
}
