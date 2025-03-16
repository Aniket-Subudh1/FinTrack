package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.IncomeRequest;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.Income;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class IncomeController {

    private final IncomeRepository incomeRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public IncomeController(IncomeRepository incomeRepository, CustomerRepository customerRepository) {
        this.incomeRepository = incomeRepository;
        this.customerRepository = customerRepository;
    }

    @PostMapping("/api/incomes")
    public ResponseEntity<Map<String, String>> addIncome(@RequestBody IncomeRequest incomeRequest, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        String email = authentication.getName();

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Income income = new Income();
        income.setSource(incomeRequest.getSource());
        income.setAmount(incomeRequest.getAmount());
        income.setDate(LocalDate.now());
        income.setCustomerEmail(email);

        incomeRepository.save(income);

        return ResponseEntity.ok(Collections.singletonMap("message", "Income added successfully"));
    }

    @GetMapping("/api/incomes")
    public ResponseEntity<List<Income>> getIncomes(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();

        List<Income> incomes = incomeRepository.findByCustomerEmail(email);

        return ResponseEntity.ok(incomes);
    }
}