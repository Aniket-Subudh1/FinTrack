package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.dto.CategoryDTO;
import com.personalfinancetracker.backend.dto.ExpenseDTO;
import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.entities.ExpenseCategory;
import com.personalfinancetracker.backend.exceptions.InvalidExpenseException;
import com.personalfinancetracker.backend.repositories.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public List<CategoryDTO> getAllCategories() {
        return Arrays.stream(ExpenseCategory.values())
                .map(category -> new CategoryDTO(
                        category.name(),
                        category.getDisplayName()
                ))
                .collect(Collectors.toList());
    }

    public Expense addExpense(String userName, ExpenseDTO expenseDTO) {
        validateExpense(expenseDTO);

        Expense expense = new Expense();
        expense.setUserName(userName);
        expense.setAmount(expenseDTO.getAmount());
        expense.setCategory(expenseDTO.getCategory());

        if (expenseDTO.getCategory() == ExpenseCategory.OTHERS) {
            expense.setCustomCategory(expenseDTO.getCustomCategory().trim());
        }

        return expenseRepository.save(expense);
    }

    private void validateExpense(ExpenseDTO expenseDTO) {
        if (expenseDTO.getAmount() <= 0) {
            throw new InvalidExpenseException("Amount must be greater than 0");
        }

        if (expenseDTO.getCategory() == null) {
            throw new InvalidExpenseException("Category is required");
        }

        if (expenseDTO.getCategory() == ExpenseCategory.OTHERS) {
            if (expenseDTO.getCustomCategory() == null || expenseDTO.getCustomCategory().trim().isEmpty()) {
                throw new InvalidExpenseException("Custom category is required when 'Others' is selected");
            }
        }
    }
}