package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.entities.Income;
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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    @Autowired
    public AnalyticsController(
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            JwtUtil jwtUtil,
            HttpServletRequest request) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.jwtUtil = jwtUtil;
        this.request = request;
    }

    @GetMapping("/spending-patterns")
    public ResponseEntity<Map<String, Object>> getSpendingPatterns(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching spending patterns for email: {}", email);

        try {
            // Set default date range to last 6 months if not specified
            if (startDate == null) {
                startDate = LocalDate.now().minusMonths(6).withDayOfMonth(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(email, startDate, endDate);

            // Calculate spending by category
            Map<String, Double> categoryTotals = new HashMap<>();
            for (Expense expense : expenses) {
                String category = expense.getCategory();
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + expense.getAmount());
            }

            // Sort categories by total amount (descending)
            List<Map.Entry<String, Double>> sortedCategories = categoryTotals.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

            // Monthly spending trends
            Map<String, Map<String, Double>> monthlyTrends = new HashMap<>();

            // Initialize months
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                String monthKey = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                monthlyTrends.put(monthKey, new HashMap<>());
                current = current.plusMonths(1);
            }

            // Fill in monthly trends data
            for (Expense expense : expenses) {
                String monthKey = expense.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                String category = expense.getCategory();

                Map<String, Double> monthData = monthlyTrends.get(monthKey);
                if (monthData != null) {
                    monthData.put(category, monthData.getOrDefault(category, 0.0) + expense.getAmount());
                }
            }

            // Daily spending patterns
            Map<String, Double> dailyPatterns = new HashMap<>();
            for (Expense expense : expenses) {
                String dayOfWeek = expense.getDate().getDayOfWeek().toString();
                dailyPatterns.put(dayOfWeek, dailyPatterns.getOrDefault(dayOfWeek, 0.0) + expense.getAmount());
            }

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("categoryTotals", sortedCategories);
            response.put("monthlyTrends", monthlyTrends);
            response.put("dailyPatterns", dailyPatterns);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching spending patterns: {}", e.getMessage(), e);
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

            // Calculate date ranges
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = YearMonth.from(today).atDay(1);
            LocalDate endOfMonth = YearMonth.from(today).atEndOfMonth();

            // Previous month
            LocalDate startOfPrevMonth = YearMonth.from(today).minusMonths(1).atDay(1);
            LocalDate endOfPrevMonth = YearMonth.from(today).minusMonths(1).atEndOfMonth();

            // Get expenses
            List<Expense> currentMonthExpenses = expenseRepository.findByCustomerEmailAndDateBetween(
                    email, startOfMonth, today);
            List<Expense> prevMonthExpenses = expenseRepository.findByCustomerEmailAndDateBetween(
                    email, startOfPrevMonth, endOfPrevMonth);

            // Get incomes
            List<Income> currentMonthIncomes = incomeRepository.findByCustomerEmailAndDateBetween(
                    email, startOfMonth, today);
            List<Income> prevMonthIncomes = incomeRepository.findByCustomerEmailAndDateBetween(
                    email, startOfPrevMonth, endOfPrevMonth);

            // Calculate totals
            double currentExpenseTotal = currentMonthExpenses.stream()
                    .mapToDouble(Expense::getAmount)
                    .sum();
            double prevExpenseTotal = prevMonthExpenses.stream()
                    .mapToDouble(Expense::getAmount)
                    .sum();

            double currentIncomeTotal = currentMonthIncomes.stream()
                    .mapToDouble(Income::getAmount)
                    .sum();
            double prevIncomeTotal = prevMonthIncomes.stream()
                    .mapToDouble(Income::getAmount)
                    .sum();

            // Insight 1: Spending trend
            if (prevExpenseTotal > 0) {
                double changePercentage = ((currentExpenseTotal - prevExpenseTotal) / prevExpenseTotal) * 100;
                Map<String, Object> spendingTrend = new HashMap<>();
                spendingTrend.put("type", "spending_trend");

                if (changePercentage > 10) {
                    spendingTrend.put("title", "Spending Has Increased");
                    spendingTrend.put("description", String.format("Your spending has increased by %.1f%% compared to last month.", changePercentage));
                    spendingTrend.put("insight_type", "warning");
                    spendingTrend.put("icon", "trending_up");
                } else if (changePercentage < -10) {
                    spendingTrend.put("title", "Spending Has Decreased");
                    spendingTrend.put("description", String.format("Your spending has decreased by %.1f%% compared to last month.", Math.abs(changePercentage)));
                    spendingTrend.put("insight_type", "success");
                    spendingTrend.put("icon", "trending_down");
                } else {
                    spendingTrend.put("title", "Spending Is Stable");
                    spendingTrend.put("description", "Your spending is stable compared to last month.");
                    spendingTrend.put("insight_type", "info");
                    spendingTrend.put("icon", "balance");
                }

                insights.add(spendingTrend);
            }

            // Insight 2: Savings rate
            if (currentIncomeTotal > 0) {
                double savingsRate = ((currentIncomeTotal - currentExpenseTotal) / currentIncomeTotal) * 100;
                Map<String, Object> savingsInsight = new HashMap<>();
                savingsInsight.put("type", "savings_rate");

                if (savingsRate >= 20) {
                    savingsInsight.put("title", "Excellent Savings Rate");
                    savingsInsight.put("description", String.format("You're saving %.1f%% of your income this month. Great job!", savingsRate));
                    savingsInsight.put("insight_type", "success");
                    savingsInsight.put("icon", "savings");
                } else if (savingsRate > 0) {
                    savingsInsight.put("title", "Positive Savings Rate");
                    savingsInsight.put("description", String.format("You're saving %.1f%% of your income. Try to save 20%% or more.", savingsRate));
                    savingsInsight.put("insight_type", "info");
                    savingsInsight.put("icon", "account_balance_wallet");
                } else {
                    savingsInsight.put("title", "Negative Savings Rate");
                    savingsInsight.put("description", "You're spending more than you earn this month. Review your expenses.");
                    savingsInsight.put("insight_type", "warning");
                    savingsInsight.put("icon", "warning");
                }

                insights.add(savingsInsight);
            }

            // Insight 3: Top spending category
            if (!currentMonthExpenses.isEmpty()) {
                Map<String, Double> categoryTotals = new HashMap<>();
                for (Expense expense : currentMonthExpenses) {
                    String category = expense.getCategory();
                    categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + expense.getAmount());
                }

                String topCategory = categoryTotals.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("");

                double topCategoryAmount = categoryTotals.getOrDefault(topCategory, 0.0);
                double topCategoryPercentage = (topCategoryAmount / currentExpenseTotal) * 100;

                Map<String, Object> categoryInsight = new HashMap<>();
                categoryInsight.put("type", "top_category");
                categoryInsight.put("title", "Top Spending Category");
                categoryInsight.put("category", topCategory);
                categoryInsight.put("percentage", topCategoryPercentage);

                categoryInsight.put("description", String.format("Your highest spending is on %s (%.1f%% of total)",
                        topCategory, topCategoryPercentage));

                if (topCategoryPercentage > 40) {
                    categoryInsight.put("insight_type", "warning");
                    categoryInsight.put("icon", "pie_chart");
                } else {
                    categoryInsight.put("insight_type", "info");
                    categoryInsight.put("icon", "category");
                }

                insights.add(categoryInsight);
            }

            // Insight 4: Recurring expenses percentage
            long recurringExpensesCount = currentMonthExpenses.stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsRecurring()))
                    .count();

            double recurringTotal = currentMonthExpenses.stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsRecurring()))
                    .mapToDouble(Expense::getAmount)
                    .sum();

            if (currentExpenseTotal > 0) {
                double recurringPercentage = (recurringTotal / currentExpenseTotal) * 100;

                Map<String, Object> recurringInsight = new HashMap<>();
                recurringInsight.put("type", "recurring_expenses");
                recurringInsight.put("percentage", recurringPercentage);

                if (recurringPercentage > 70) {
                    recurringInsight.put("title", "High Fixed Expenses");
                    recurringInsight.put("description", String.format("%.1f%% of your spending is on recurring expenses.", recurringPercentage));
                    recurringInsight.put("insight_type", "warning");
                } else {
                    recurringInsight.put("title", "Recurring Expenses");
                    recurringInsight.put("description", String.format("%.1f%% of your spending is on recurring expenses.", recurringPercentage));
                    recurringInsight.put("insight_type", "info");
                }

                recurringInsight.put("icon", "repeat");
                insights.add(recurringInsight);
            }

            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            logger.error("Error generating financial insights: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/forecast")
    public ResponseEntity<Map<String, Object>> generateForecast(@RequestBody Map<String, Object> request) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Generating financial forecast for email: {}", email);

        try {
            // Get parameters from request
            int months = (int) request.getOrDefault("months", 3);
            boolean includeRecurring = (boolean) request.getOrDefault("includeRecurring", true);
            Double savingsGoal = request.get("savingsGoal") != null ?
                    Double.parseDouble(request.get("savingsGoal").toString()) : null;

            // Set date range for historical data (last 6 months)
            LocalDate today = LocalDate.now();
            LocalDate sixMonthsAgo = today.minusMonths(6);

            // Get historical expenses and incomes
            List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(
                    email, sixMonthsAgo, today);
            List<Income> incomes = incomeRepository.findByCustomerEmailAndDateBetween(
                    email, sixMonthsAgo, today);

            // Calculate monthly averages
            Map<YearMonth, Double> monthlyExpenses = new HashMap<>();
            Map<YearMonth, Double> monthlyIncomes = new HashMap<>();

            // Group expenses by month
            for (Expense expense : expenses) {
                YearMonth yearMonth = YearMonth.from(expense.getDate());
                monthlyExpenses.put(yearMonth, monthlyExpenses.getOrDefault(yearMonth, 0.0) + expense.getAmount());
            }

            // Group incomes by month
            for (Income income : incomes) {
                YearMonth yearMonth = YearMonth.from(income.getDate());
                monthlyIncomes.put(yearMonth, monthlyIncomes.getOrDefault(yearMonth, 0.0) + income.getAmount());
            }

            // Calculate averages
            double avgMonthlyIncome = monthlyIncomes.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            double avgMonthlyExpense = monthlyExpenses.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            // Calculate recurring expenses if needed
            double recurringExpenses = 0.0;
            if (includeRecurring) {
                // Only count most recent month's recurring expenses to avoid duplicates
                LocalDate startOfCurrentMonth = YearMonth.from(today).atDay(1);
                recurringExpenses = expenses.stream()
                        .filter(e -> e.getDate().isAfter(startOfCurrentMonth.atStartOfDay())
                                && Boolean.TRUE.equals(e.getIsRecurring()))
                        .mapToDouble(Expense::getAmount)
                        .sum();
            }

            // Generate forecast
            List<Map<String, Object>> forecastMonths = new ArrayList<>();
            double cumulativeSavings = monthlyIncomes.getOrDefault(YearMonth.from(today), 0.0) -
                    monthlyExpenses.getOrDefault(YearMonth.from(today), 0.0);

            double monthlySavings = 0;
            for (int i = 1; i <= months; i++) {
                YearMonth forecastMonth = YearMonth.from(today).plusMonths(i);
                double projectedIncome = avgMonthlyIncome;
                double projectedExpense = includeRecurring ? recurringExpenses : avgMonthlyExpense;
                monthlySavings = projectedIncome - projectedExpense;

                cumulativeSavings += monthlySavings;

                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", forecastMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")));
                monthData.put("projectedIncome", projectedIncome);
                monthData.put("projectedExpense", projectedExpense);
                monthData.put("monthlySavings", monthlySavings);
                monthData.put("cumulativeSavings", cumulativeSavings);

                forecastMonths.add(monthData);
            }

            // Calculate time to reach savings goal
            Integer monthsToGoal = null;
            if (savingsGoal != null && monthlySavings > 0) {
                double remainingToGoal = savingsGoal - cumulativeSavings;
                if (remainingToGoal > 0) {
                    monthsToGoal = (int) Math.ceil(remainingToGoal / monthlySavings);
                } else {
                    monthsToGoal = 0; // Already reached goal
                }
            }

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("forecastMonths", forecastMonths);
            response.put("avgMonthlyIncome", avgMonthlyIncome);
            response.put("avgMonthlyExpense", avgMonthlyExpense);
            response.put("recurringExpenses", recurringExpenses);
            response.put("monthlySavings", avgMonthlyIncome - (includeRecurring ? recurringExpenses : avgMonthlyExpense));
            response.put("monthsToGoal", monthsToGoal);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating financial forecast: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/budget-analysis")
    public ResponseEntity<Map<String, Object>> getBudgetAnalysis() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching budget analysis for email: {}", email);

        try {
            // Get current month's data
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = YearMonth.from(today).atDay(1);
            LocalDate endOfMonth = YearMonth.from(today).atEndOfMonth();

            // Get current month expenses
            List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(
                    email, startOfMonth, today);

            // Group expenses by category
            Map<String, Double> categoryExpenses = new HashMap<>();
            for (Expense expense : expenses) {
                String category = expense.getCategory();
                categoryExpenses.put(category, categoryExpenses.getOrDefault(category, 0.0) + expense.getAmount());
            }

            // Calculate days elapsed and remaining in month
            long daysInMonth = startOfMonth.until(endOfMonth.plusDays(1), ChronoUnit.DAYS);
            long daysElapsed = startOfMonth.until(today.plusDays(1), ChronoUnit.DAYS);
            long daysRemaining = daysInMonth - daysElapsed;

            // Calculate daily spending rate
            double totalSpent = expenses.stream()
                    .mapToDouble(Expense::getAmount)
                    .sum();

            double dailySpendingRate = daysElapsed > 0 ? totalSpent / daysElapsed : 0;
            double projectedMonthTotal = dailySpendingRate * daysInMonth;

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("daysInMonth", daysInMonth);
            response.put("daysElapsed", daysElapsed);
            response.put("daysRemaining", daysRemaining);
            response.put("categorySpending", categoryExpenses);
            response.put("dailySpendingRate", dailySpendingRate);
            response.put("monthToDateSpending", totalSpent);
            response.put("projectedMonthTotal", projectedMonthTotal);
            response.put("percentOfMonthElapsed", (double) daysElapsed / daysInMonth * 100);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching budget analysis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/periodic-comparison")
    public ResponseEntity<Map<String, Object>> getPeriodicComparison(
            @RequestParam(defaultValue = "month") String period) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching {} over {} comparison for email: {}", period, period, email);

        try {
            LocalDate today = LocalDate.now();
            LocalDate startDate1, endDate1, startDate2, endDate2;

            // Set date ranges based on period
            switch (period) {
                case "week":
                    // Current week vs previous week
                    startDate1 = today.minusDays(today.getDayOfWeek().getValue() - 1);
                    endDate1 = today;
                    startDate2 = startDate1.minusWeeks(1);
                    endDate2 = startDate1.minusDays(1);
                    break;
                case "year":
                    // Current year vs previous year
                    startDate1 = LocalDate.of(today.getYear(), 1, 1);
                    endDate1 = today;
                    startDate2 = LocalDate.of(today.getYear() - 1, 1, 1);
                    endDate2 = LocalDate.of(today.getYear() - 1, today.getMonthValue(), today.getDayOfMonth());
                    break;
                case "month":
                default:
                    // Current month vs previous month
                    startDate1 = YearMonth.from(today).atDay(1);
                    endDate1 = today;
                    startDate2 = YearMonth.from(today).minusMonths(1).atDay(1);
                    endDate2 = YearMonth.from(today).minusMonths(1).atEndOfMonth();
                    break;
            }

            // Get expenses for both periods
            List<Expense> expenses1 = expenseRepository.findByCustomerEmailAndDateBetween(
                    email, startDate1, endDate1);
            List<Expense> expenses2 = expenseRepository.findByCustomerEmailAndDateBetween(
                    email, startDate2, endDate2);

            // Get incomes for both periods
            List<Income> incomes1 = incomeRepository.findByCustomerEmailAndDateBetween(
                    email, startDate1, endDate1);
            List<Income> incomes2 = incomeRepository.findByCustomerEmailAndDateBetween(
                    email, startDate2, endDate2);

            // Calculate totals
            double totalExpenses1 = expenses1.stream().mapToDouble(Expense::getAmount).sum();
            double totalExpenses2 = expenses2.stream().mapToDouble(Expense::getAmount).sum();
            double totalIncomes1 = incomes1.stream().mapToDouble(Income::getAmount).sum();
            double totalIncomes2 = incomes2.stream().mapToDouble(Income::getAmount).sum();
            double savings1 = totalIncomes1 - totalExpenses1;
            double savings2 = totalIncomes2 - totalExpenses2;

            // Group expenses by category for both periods
            Map<String, Double> categoryExpenses1 = new HashMap<>();
            Map<String, Double> categoryExpenses2 = new HashMap<>();

            for (Expense expense : expenses1) {
                String category = expense.getCategory();
                categoryExpenses1.put(category, categoryExpenses1.getOrDefault(category, 0.0) + expense.getAmount());
            }

            for (Expense expense : expenses2) {
                String category = expense.getCategory();
                categoryExpenses2.put(category, categoryExpenses2.getOrDefault(category, 0.0) + expense.getAmount());
            }

            // Calculate changes
            double expenseChange = totalExpenses2 > 0
                    ? ((totalExpenses1 - totalExpenses2) / totalExpenses2) * 100
                    : 0;
            double incomeChange = totalIncomes2 > 0
                    ? ((totalIncomes1 - totalIncomes2) / totalIncomes2) * 100
                    : 0;
            double savingsChange = savings2 != 0
                    ? ((savings1 - savings2) / Math.abs(savings2)) * 100
                    : (savings1 > 0 ? 100 : -100);

            // Prepare category comparison
            List<Map<String, Object>> categoryComparison = new ArrayList<>();
            Set<String> allCategories = new HashSet<>();
            allCategories.addAll(categoryExpenses1.keySet());
            allCategories.addAll(categoryExpenses2.keySet());

            for (String category : allCategories) {
                double amount1 = categoryExpenses1.getOrDefault(category, 0.0);
                double amount2 = categoryExpenses2.getOrDefault(category, 0.0);
                double change = amount2 > 0 ? ((amount1 - amount2) / amount2) * 100 : 0;

                Map<String, Object> categoryData = new HashMap<>();
                categoryData.put("category", category);
                categoryData.put("currentAmount", amount1);
                categoryData.put("previousAmount", amount2);
                categoryData.put("changePercent", change);

                categoryComparison.add(categoryData);
            }

            // Sort by absolute change
            categoryComparison.sort((a, b) -> {
                double changeA = Math.abs((double) a.get("changePercent"));
                double changeB = Math.abs((double) b.get("changePercent"));
                return Double.compare(changeB, changeA);
            });

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("period", period);
            response.put("currentPeriodStart", startDate1);
            response.put("currentPeriodEnd", endDate1);
            response.put("previousPeriodStart", startDate2);
            response.put("previousPeriodEnd", endDate2);

            response.put("currentTotalExpenses", totalExpenses1);
            response.put("previousTotalExpenses", totalExpenses2);
            response.put("expenseChangePercent", expenseChange);

            response.put("currentTotalIncome", totalIncomes1);
            response.put("previousTotalIncome", totalIncomes2);
            response.put("incomeChangePercent", incomeChange);

            response.put("currentSavings", savings1);
            response.put("previousSavings", savings2);
            response.put("savingsChangePercent", savingsChange);

            response.put("categoryComparison", categoryComparison);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching periodic comparison: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/daily-spending")
    public ResponseEntity<Map<String, Object>> getDailySpendingPattern(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Fetching daily spending patterns for email: {}", email);

        try {
            // Set default date range to current month if not specified
            if (startDate == null) {
                startDate = YearMonth.from(LocalDate.now()).atDay(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(
                    email, startDate, endDate);

            // Group by day of week
            Map<String, Double> dayOfWeekTotals = new HashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                dayOfWeekTotals.put(day.toString(), 0.0);
            }

            Map<String, Integer> dayOfWeekCounts = new HashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                dayOfWeekCounts.put(day.toString(), 0);
            }

            // Group by day of month (1-31)
            Map<Integer, Double> dayOfMonthTotals = new HashMap<>();
            for (int day = 1; day <= 31; day++) {
                dayOfMonthTotals.put(day, 0.0);
            }

            // Group by hour of day (0-23)
            Map<Integer, Double> hourlyTotals = new HashMap<>();
            for (int hour = 0; hour < 24; hour++) {
                hourlyTotals.put(hour, 0.0);
            }

            // Calculate totals
            for (Expense expense : expenses) {
                String dayOfWeek = expense.getDate().getDayOfWeek().toString();
                dayOfWeekTotals.put(dayOfWeek, dayOfWeekTotals.get(dayOfWeek) + expense.getAmount());
                dayOfWeekCounts.put(dayOfWeek, dayOfWeekCounts.get(dayOfWeek) + 1);

                int dayOfMonth = expense.getDate().getDayOfMonth();
                dayOfMonthTotals.put(dayOfMonth, dayOfMonthTotals.getOrDefault(dayOfMonth, 0.0) + expense.getAmount());

                LocalDateTime dateTime = expense.getDate().toLocalDate().atStartOfDay();
                int hour = dateTime.getHour();
                hourlyTotals.put(hour, hourlyTotals.getOrDefault(hour, 0.0) + expense.getAmount());
            }

            // Calculate average daily spending by day of week
            Map<String, Double> avgDayOfWeekTotals = new HashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                String dayName = day.toString();
                int count = dayOfWeekCounts.get(dayName);
                double total = dayOfWeekTotals.get(dayName);
                avgDayOfWeekTotals.put(dayName, count > 0 ? total / count : 0.0);
            }

            // Find highest and lowest spending days
            String highestSpendingDay = dayOfWeekTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");

            String lowestSpendingDay = dayOfWeekTotals.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("dayOfWeekTotals", dayOfWeekTotals);
            response.put("avgDayOfWeekTotals", avgDayOfWeekTotals);
            response.put("dayOfMonthTotals", dayOfMonthTotals);
            response.put("hourlyTotals", hourlyTotals);
            response.put("highestSpendingDay", highestSpendingDay);
            response.put("lowestSpendingDay", lowestSpendingDay);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching daily spending patterns: {}", e.getMessage(), e);
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