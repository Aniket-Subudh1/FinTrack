package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.repository.ExpenseRepository;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class ExpenseService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);

    @Autowired
    private final ExpenseRepository expenseRepository;

    @Autowired
    private final CustomerRepository customerRepository;

    @Autowired
    private final JwtUtil jwtUtil;

    public ExpenseService(ExpenseRepository expenseRepository, CustomerRepository customerRepository, JwtUtil jwtUtil) {
        this.expenseRepository = expenseRepository;
        this.customerRepository = customerRepository;
        this.jwtUtil = jwtUtil;
    }

    public Expense saveExpense(Expense expense) {
        // Get the logged-in user's email from the JWT token
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        logger.debug("Retrieved email from security context: {}", email);

        // Retrieve the Customer entity using the email
        Optional<Customer> customerOptional = customerRepository.findByEmail(email);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            // Set the username in the expense entity
            expense.setUserName(customer.getName());
            logger.debug("Retrieved user name from database: {}", customer.getName());
        } else {
            logger.warn("No customer found with email: {}", email);
        }

        return expenseRepository.save(expense);
    }
}