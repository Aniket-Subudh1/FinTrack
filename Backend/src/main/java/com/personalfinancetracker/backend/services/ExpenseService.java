package com.personalfinancetracker.backend.services;



import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public Expense addExpense(Expense expense) {
        return expenseRepository.save(expense); // Save expense to the database
    }
}
