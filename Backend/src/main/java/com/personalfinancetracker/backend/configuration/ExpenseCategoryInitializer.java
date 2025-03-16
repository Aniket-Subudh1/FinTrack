package com.personalfinancetracker.backend.configuration;

import com.personalfinancetracker.backend.entities.ExpenseCategory;
import com.personalfinancetracker.backend.entities.ExpenseCategoryEnum;
import com.personalfinancetracker.backend.repository.ExpenseCategoryRepository;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;

@Component
public class ExpenseCategoryInitializer {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    public ExpenseCategoryInitializer(ExpenseCategoryRepository expenseCategoryRepository) {
        this.expenseCategoryRepository = expenseCategoryRepository;
    }

    @PostConstruct
    public void initializeCategories() {

        Arrays.stream(ExpenseCategoryEnum.values()).forEach(categoryEnum -> {

            if (!expenseCategoryRepository.existsByExpenseCategory(categoryEnum)) {
                ExpenseCategory expenseCategory = new ExpenseCategory();
                expenseCategory.setExpenseCategory(categoryEnum);
                expenseCategoryRepository.save(expenseCategory);
            }
        });
    }
}
