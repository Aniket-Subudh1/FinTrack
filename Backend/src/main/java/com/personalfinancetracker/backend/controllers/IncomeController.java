package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.IncomeRequest;
import com.personalfinancetracker.backend.dto.IncomeResponse;
import com.personalfinancetracker.backend.dto.IncomeSummary;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.Income;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.IncomeRepository;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incomes")
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
            income.setAmount(incomeRequest.getAmount());
            income.setSource(incomeRequest.getSource());
            income.setDate(LocalDate.now());
            income.setCustomerEmail(email);

            // Add new fields
            income.setDescription(incomeRequest.getDescription());

            if (incomeRequest.getIsRecurring() != null) {
                income.setRecurring(incomeRequest.getIsRecurring());
                if (incomeRequest.getIsRecurring() && incomeRequest.getRecurringFrequency() != null) {
                    income.setRecurringFrequency(incomeRequest.getRecurringFrequency());
                }
            }

            if (incomeRequest.getTags() != null && !incomeRequest.getTags().isEmpty()) {
                income.setTags(String.join(",", incomeRequest.getTags()));
            }

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
    public ResponseEntity<List<IncomeResponse>> getIncomes() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getIncomes");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching incomes for email: {}", email);

        try {
            List<Income> incomes = incomeRepository.findByCustomerEmail(email);
            List<IncomeResponse> incomeResponses = mapToIncomeResponses(incomes);

            return ResponseEntity.ok(incomeResponses);
        } catch (Exception e) {
            logger.error("Error fetching incomes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<List<IncomeResponse>> filterIncomes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Boolean recurring) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for filterIncomes");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Income> incomes = incomeRepository.findByCustomerEmail(email);

            // Apply filters
            if (startDate != null) {
                incomes = incomes.stream()
                        .filter(income -> income.getDate().isAfter(startDate) || income.getDate().isEqual(startDate))
                        .collect(Collectors.toList());
            }

            if (endDate != null) {
                incomes = incomes.stream()
                        .filter(income -> income.getDate().isBefore(endDate) || income.getDate().isEqual(endDate))
                        .collect(Collectors.toList());
            }

            if (source != null && !source.isEmpty()) {
                incomes = incomes.stream()
                        .filter(income -> income.getSource().equalsIgnoreCase(source))
                        .collect(Collectors.toList());
            }

            if (minAmount != null) {
                incomes = incomes.stream()
                        .filter(income -> income.getAmount() >= minAmount)
                        .collect(Collectors.toList());
            }

            if (maxAmount != null) {
                incomes = incomes.stream()
                        .filter(income -> income.getAmount() <= maxAmount)
                        .collect(Collectors.toList());
            }

            if (tags != null && !tags.isEmpty()) {
                String[] tagArray = tags.split(",");
                incomes = incomes.stream()
                        .filter(income -> {
                            if (income.getTags() == null) return false;
                            for (String tag : tagArray) {
                                if (income.getTags().contains(tag.trim())) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
            }

            if (recurring != null) {
                incomes = incomes.stream()
                        .filter(income -> income.isRecurring() == recurring)
                        .collect(Collectors.toList());
            }

            // Convert to response objects
            List<IncomeResponse> incomeResponses = mapToIncomeResponses(incomes);

            return ResponseEntity.ok(incomeResponses);
        } catch (Exception e) {
            logger.error("Error filtering incomes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<List<IncomeSummary>> getIncomeSummary() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getIncomeSummary");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<IncomeSummary> summary = incomeRepository.getIncomeSummaryBySource(email);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting income summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/trends")
    public ResponseEntity<Map<String, Number>> getMonthlyTrends() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getMonthlyTrends");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            LocalDate startDate = LocalDate.now().minusMonths(11).withDayOfMonth(1);
            List<Object[]> trends = incomeRepository.getMonthlyIncomeTrend(email, startDate);

            Map<String, Number> monthlyTotals = new LinkedHashMap<>();

            LocalDate currentMonthStart = YearMonth.now().atDay(1);
            for (int i = 11; i >= 0; i--) {
                LocalDate monthStart = currentMonthStart.minusMonths(i);
                String monthKey = monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue());
                monthlyTotals.put(monthKey, 0.0);
            }

            for (Object[] trend : trends) {
                String month = (String) trend[0];
                Double amount = ((Number) trend[1]).doubleValue();
                monthlyTotals.put(month, amount);
            }

            return ResponseEntity.ok(monthlyTotals);
        } catch (Exception e) {
            logger.error("Error getting monthly trends: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/sources")
    public ResponseEntity<List<String>> getIncomeSources() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getIncomeSources");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<String> sources = incomeRepository.findByCustomerEmail(email).stream()
                    .map(Income::getSource)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            return ResponseEntity.ok(sources);
        } catch (Exception e) {
            logger.error("Error getting income sources: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteIncome(@PathVariable Long id) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for deleteIncome");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        try {
            Income income = incomeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Income not found"));

            if (!income.getCustomerEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "Not authorized to delete this income"));
            }

            incomeRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Income deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting income: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to delete income"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateIncome(@PathVariable Long id, @RequestBody IncomeRequest incomeRequest) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for updateIncome");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "User is not authenticated"));
        }

        try {
            // Verify the income belongs to the authenticated user
            Income income = incomeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Income not found"));

            if (!income.getCustomerEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "Not authorized to update this income"));
            }

            // Update the income
            if (incomeRequest.getAmount() > 0) {
                income.setAmount(incomeRequest.getAmount());
            }

            if (incomeRequest.getSource() != null && !incomeRequest.getSource().isEmpty()) {
                income.setSource(incomeRequest.getSource());
            }

            if (incomeRequest.getDescription() != null) {
                income.setDescription(incomeRequest.getDescription());
            }

            if (incomeRequest.getIsRecurring() != null) {
                income.setRecurring(incomeRequest.getIsRecurring());
                if (incomeRequest.getIsRecurring() && incomeRequest.getRecurringFrequency() != null) {
                    income.setRecurringFrequency(incomeRequest.getRecurringFrequency());
                }
            }

            if (incomeRequest.getTags() != null) {
                income.setTags(String.join(",", incomeRequest.getTags()));
            }

            incomeRepository.save(income);
            return ResponseEntity.ok(Collections.singletonMap("message", "Income updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating income: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to update income"));
        }
    }

    private List<IncomeResponse> mapToIncomeResponses(List<Income> incomes) {
        return incomes.stream()
                .map(income -> {
                    IncomeResponse response = new IncomeResponse();
                    response.setId(income.getId());
                    response.setAmount(income.getAmount());
                    response.setSource(income.getSource());
                    response.setDate(income.getDate());
                    response.setCustomerEmail(income.getCustomerEmail());
                    response.setDescription(income.getDescription());
                    response.setIsRecurring(income.isRecurring());
                    response.setRecurringFrequency(income.getRecurringFrequency());

                    if (income.getTags() != null && !income.getTags().isEmpty()) {
                        response.setTags(Arrays.asList(income.getTags().split(",")));
                    }

                    return response;
                })
                .collect(Collectors.toList());
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