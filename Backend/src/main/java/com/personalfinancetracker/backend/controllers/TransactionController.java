package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.*;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.entities.Income;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.ExpenseRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for combined transaction operations that relate to both expenses and incomes.
 * Provides endpoints for retrieving and analyzing aggregated financial data.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    @Autowired
    public TransactionController(
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            CustomerRepository customerRepository,
            JwtUtil jwtUtil,
            HttpServletRequest request) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.customerRepository = customerRepository;
        this.jwtUtil = jwtUtil;
        this.request = request;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching all transactions for email: {}", email);

        try {
            // Get expenses
            List<Expense> expenses;
            if (startDate != null && endDate != null) {
                expenses = expenseRepository.findByCustomerEmailAndDateBetween(email, startDate, endDate);
            } else {
                expenses = expenseRepository.findByCustomerEmail(email);
            }

            // Get incomes
            List<Income> incomes;
            if (startDate != null && endDate != null) {
                incomes = incomeRepository.findByCustomerEmailAndDateBetween(email, startDate, endDate);
            } else {
                incomes = incomeRepository.findByCustomerEmail(email);
            }

            // Convert to a unified transaction format
            List<Map<String, Object>> transactions = new ArrayList<>();

            // Process expenses
            for (Expense expense : expenses) {
                Map<String, Object> transaction = new HashMap<>();
                transaction.put("id", expense.getId());
                transaction.put("type", "expense");
                transaction.put("amount", expense.getAmount());
                transaction.put("category", expense.getCategory());
                transaction.put("date", expense.getDate());
                transaction.put("description", expense.getNote());
                transaction.put("isRecurring", expense.getIsRecurring());
                transaction.put("recurringFrequency", expense.getRecurringFrequency());

                // Process tags if available
                if (expense.getTags() != null && !expense.getTags().isEmpty()) {
                    transaction.put("tags", Arrays.asList(expense.getTags().split(",")));
                } else {
                    transaction.put("tags", Collections.emptyList());
                }

                transactions.add(transaction);
            }

            // Process incomes
            for (Income income : incomes) {
                Map<String, Object> transaction = new HashMap<>();
                transaction.put("id", income.getId());
                transaction.put("type", "income");
                transaction.put("amount", income.getAmount());
                transaction.put("category", income.getSource());
                transaction.put("date", income.getDate());
                transaction.put("description", income.getDescription());
                transaction.put("isRecurring", income.isRecurring());
                transaction.put("recurringFrequency", income.getRecurringFrequency());

                // Process tags if available
                if (income.getTags() != null && !income.getTags().isEmpty()) {
                    transaction.put("tags", Arrays.asList(income.getTags().split(",")));
                } else {
                    transaction.put("tags", Collections.emptyList());
                }

                transactions.add(transaction);
            }

            // Sort by date (most recent first)
            transactions.sort((t1, t2) -> {
                LocalDate date1 = (LocalDate) t1.get("date");
                LocalDate date2 = (LocalDate) t2.get("date");
                return date2.compareTo(date1);
            });

            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            logger.error("Error fetching transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<List<Map<String, Object>>> getMonthlySummary(
            @RequestParam(defaultValue = "6") int months) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching monthly summary for email: {}, months: {}", email, months);

        try {
            LocalDate now = LocalDate.now();
            List<Map<String, Object>> monthlySummary = new ArrayList<>();

            // Generate data for each month
            for (int i = months - 1; i >= 0; i--) {
                YearMonth yearMonth = YearMonth.from(now.minusMonths(i));
                LocalDate startOfMonth = yearMonth.atDay(1);
                LocalDate endOfMonth = yearMonth.atEndOfMonth();

                // Get expense total for the month
                Double expenseTotal = expenseRepository.getTotalAmountByCategory(
                        email, null, startOfMonth.atStartOfDay(), endOfMonth.atStartOfDay());
                if (expenseTotal == null) expenseTotal = 0.0;

                // Get income total for the month
                Double incomeTotal = incomeRepository.getTotalIncomeForPeriod(
                        email, startOfMonth, endOfMonth);
                if (incomeTotal == null) incomeTotal = 0.0;

                // Calculate net amount
                double netAmount = incomeTotal - expenseTotal;

                // Format month name
                String monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"));

                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", monthName);
                monthData.put("income", incomeTotal);
                monthData.put("expense", expenseTotal);
                monthData.put("net", netAmount);
                monthData.put("savingsRate", incomeTotal > 0 ? (netAmount / incomeTotal) * 100 : 0);

                monthlySummary.add(monthData);
            }

            return ResponseEntity.ok(monthlySummary);
        } catch (Exception e) {
            logger.error("Error generating monthly summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<List<Map<String, Object>>> getCategoryBreakdown(
            @RequestParam(defaultValue = "expense") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching category breakdown for email: {}, type: {}", email, type);

        try {
            List<Map<String, Object>> result = new ArrayList<>();

            // Set default date range to current month if not specified
            if (startDate == null) {
                startDate = YearMonth.now().atDay(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            // Get expense categories breakdown
            if ("expense".equals(type) || "all".equals(type)) {
                List<ExpenseCategorySummary> expenseSummary = expenseRepository.getExpenseSummaryByCategory(email);

                for (ExpenseCategorySummary summary : expenseSummary) {
                    Map<String, Object> category = new HashMap<>();
                    category.put("category", summary.getCategory());
                    category.put("amount", summary.getTotalAmount());
                    category.put("type", "expense");
                    result.add(category);
                }
            }

            // Get income sources breakdown
            if ("income".equals(type) || "all".equals(type)) {
                List<IncomeSummary> incomeSummary = incomeRepository.getIncomeSummaryBySource(email);

                for (IncomeSummary summary : incomeSummary) {
                    Map<String, Object> category = new HashMap<>();
                    category.put("category", summary.getSource());
                    category.put("amount", summary.getTotalAmount());
                    category.put("type", "income");
                    result.add(category);
                }
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error generating category breakdown: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/insights")
    public ResponseEntity<List<Map<String, Object>>> getFinancialInsights() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Generating financial insights for email: {}", email);

        try {
            List<Map<String, Object>> insights = new ArrayList<>();

            // Get current month data
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = YearMonth.from(today).atDay(1);

            // Get previous month data
            LocalDate startOfPrevMonth = YearMonth.from(today).minusMonths(1).atDay(1);
            LocalDate endOfPrevMonth = YearMonth.from(today).minusMonths(1).atEndOfMonth();

            // Calculate current month totals
            Double currentMonthExpenses = expenseRepository.getTotalAmountByCategory(
                    email, null, startOfMonth.atStartOfDay(), today.atStartOfDay());
            if (currentMonthExpenses == null) currentMonthExpenses = 0.0;

            Double currentMonthIncome = incomeRepository.getTotalIncomeForPeriod(
                    email, startOfMonth, today);
            if (currentMonthIncome == null) currentMonthIncome = 0.0;

            // Calculate previous month totals
            Double prevMonthExpenses = expenseRepository.getTotalAmountByCategory(
                    email, null, startOfPrevMonth.atStartOfDay(), endOfPrevMonth.atStartOfDay());
            if (prevMonthExpenses == null) prevMonthExpenses = 0.0;

            Double prevMonthIncome = incomeRepository.getTotalIncomeForPeriod(
                    email, startOfPrevMonth, endOfPrevMonth);
            if (prevMonthIncome == null) prevMonthIncome = 0.0;

            // Insight 1: Savings Rate
            double savingsRate = currentMonthIncome > 0
                    ? ((currentMonthIncome - currentMonthExpenses) / currentMonthIncome) * 100
                    : 0;

            Map<String, Object> savingsInsight = new HashMap<>();
            savingsInsight.put("title", "Monthly Savings Rate");

            if (savingsRate >= 20) {
                savingsInsight.put("description",
                        String.format("Great job! You're saving %.1f%% of your income this month.", savingsRate));
                savingsInsight.put("type", "success");
            } else if (savingsRate > 0) {
                savingsInsight.put("description",
                        String.format("You're saving %.1f%% of your income. The recommended target is at least 20%%.", savingsRate));
                savingsInsight.put("type", "info");
            } else {
                savingsInsight.put("description",
                        "You're spending more than you earn this month. Consider reviewing your expenses.");
                savingsInsight.put("type", "warning");
            }

            insights.add(savingsInsight);

            // Insight 2: Income Change
            if (prevMonthIncome > 0) {
                double incomeChange = ((currentMonthIncome - prevMonthIncome) / prevMonthIncome) * 100;

                Map<String, Object> incomeInsight = new HashMap<>();
                incomeInsight.put("title", "Income Trend");

                if (incomeChange >= 10) {
                    incomeInsight.put("description",
                            String.format("Your income has increased by %.1f%% compared to last month.", incomeChange));
                    incomeInsight.put("type", "success");
                } else if (incomeChange <= -10) {
                    incomeInsight.put("description",
                            String.format("Your income has decreased by %.1f%% compared to last month.", Math.abs(incomeChange)));
                    incomeInsight.put("type", "warning");
                } else {
                    incomeInsight.put("description",
                            "Your income has remained relatively stable compared to last month.");
                    incomeInsight.put("type", "info");
                }

                insights.add(incomeInsight);
            }

            // Insight 3: Expense Change
            if (prevMonthExpenses > 0) {
                double expenseChange = ((currentMonthExpenses - prevMonthExpenses) / prevMonthExpenses) * 100;

                Map<String, Object> expenseInsight = new HashMap<>();
                expenseInsight.put("title", "Expense Trend");

                if (expenseChange >= 20) {
                    expenseInsight.put("description",
                            String.format("Your expenses have increased by %.1f%% compared to last month.", expenseChange));
                    expenseInsight.put("type", "warning");
                } else if (expenseChange <= -10) {
                    expenseInsight.put("description",
                            String.format("Your expenses have decreased by %.1f%% compared to last month. Good job!",
                                    Math.abs(expenseChange)));
                    expenseInsight.put("type", "success");
                } else {
                    expenseInsight.put("description",
                            "Your expenses have remained relatively stable compared to last month.");
                    expenseInsight.put("type", "info");
                }

                insights.add(expenseInsight);
            }

            // Insight 4: Top Expense Category
            List<ExpenseCategorySummary> expenseSummary = expenseRepository.getExpenseSummaryByCategory(email);
            if (!expenseSummary.isEmpty()) {
                // Sort by amount (highest first)
                expenseSummary.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

                ExpenseCategorySummary topCategory = expenseSummary.get(0);
                double percentOfTotal = currentMonthExpenses > 0
                        ? (topCategory.getTotalAmount() / currentMonthExpenses) * 100
                        : 0;

                Map<String, Object> categoryInsight = new HashMap<>();
                categoryInsight.put("title", "Top Spending Category");
                categoryInsight.put("description",
                        String.format("Your highest spending category is %s (%.1f%% of total expenses).",
                                topCategory.getCategory(), percentOfTotal));

                if (percentOfTotal > 50) {
                    categoryInsight.put("type", "warning");
                } else {
                    categoryInsight.put("type", "info");
                }

                insights.add(categoryInsight);
            }

            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            logger.error("Error generating financial insights: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getEmailFromJwtCookie() {
        String jwt = jwtUtil.getJwtFromCookies(request);
        if (jwt != null) {
            try {
                String email = jwtUtil.extractUsername(jwt);
                if (email != null && !email.isEmpty()) {
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