package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.IncomeRequest;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.Income;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.IncomeRepository;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incomes") // Consistent mapping
public class IncomeController {
    private static final Logger logger = LoggerFactory.getLogger(IncomeController.class);

    private final IncomeRepository incomeRepository;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    @Autowired
    public IncomeController(IncomeRepository incomeRepository, CustomerRepository customerRepository,
                            JwtUtil jwtUtil, HttpServletRequest request) {
        this.incomeRepository = incomeRepository;
        this.customerRepository = customerRepository;
        this.jwtUtil = jwtUtil;
        this.request = request;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addIncome(@RequestBody IncomeRequest incomeRequest) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        logger.info("Adding income for email: {}", email);

        try {
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found for email: {}", email);
                        return new RuntimeException("User not found");
                    });

            Income income = new Income();
            income.setSource(incomeRequest.getSource());
            income.setAmount(incomeRequest.getAmount());
            income.setDate(LocalDate.now());
            income.setCustomerEmail(email); // Assuming this sets the email directly; adjust if entity uses Customer

            incomeRepository.save(income);
            logger.info("Income added successfully for email: {}", email);

            return ResponseEntity.ok(Collections.singletonMap("message", "Income added successfully"));
        } catch (Exception e) {
            logger.error("Error adding income: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to add income"));
        }
    }

    @GetMapping
    public ResponseEntity<List<Income>> getIncomes() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getIncomes");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching incomes for email: {}", email);

        try {
            List<Income> incomes = incomeRepository.findByCustomerEmail(email);
            return ResponseEntity.ok(incomes);
        } catch (Exception e) {
            logger.error("Error fetching incomes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getEmailFromJwtCookie() {
        String jwt = jwtUtil.getJwtFromCookies(request);
        if (jwt != null) {
            try {
                String email = jwtUtil.extractUsername(jwt);
                if (email != null && !email.isEmpty()) {
                    logger.debug("Extracted email from JWT: {}", email);
                    return email;
                } else {
                    logger.warn("JWT token contains no valid username");
                }
            } catch (Exception e) {
                logger.error("Error extracting email from JWT: {}", e.getMessage());
            }
        } else {
            logger.warn("No JWT token found in cookies");
        }
        return null;
    }
}