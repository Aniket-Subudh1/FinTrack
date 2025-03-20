package com.personalfinancetracker.backend.services;

import com.personalfinancetracker.backend.dto.FinancialInsight;
import com.personalfinancetracker.backend.dto.MonthlySummary;
import com.personalfinancetracker.backend.entities.Budget;
import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.entities.Income;
import com.personalfinancetracker.backend.repository.BudgetRepository;
import com.personalfinancetracker.backend.repository.ExpenseRepository;
import com.personalfinancetracker.backend.repository.IncomeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationService.class);

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;

    @Autowired
    public ReportGenerationService(
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.budgetRepository = budgetRepository;
    }


    public byte[] generateExcelReport(
            String email,
            LocalDate startDate,
            LocalDate endDate,
            String reportTitle,
            Boolean includeExpenses,
            Boolean includeIncomes,
            Boolean includeBudgets) throws IOException {
        logger.info("Generating Excel report for email: {}, period: {} to {}", email, startDate, endDate);

        // Create workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle subHeaderStyle = createSubHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle summaryStyle = createSummaryStyle(workbook);

            // Create Summary Sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(
                    summarySheet,
                    email,
                    startDate,
                    endDate,
                    reportTitle,
                    titleStyle,
                    headerStyle,
                    summaryStyle,
                    currencyStyle
            );

            // Create Expenses Sheet if included
            if (includeExpenses != null && includeExpenses) {
                Sheet expenseSheet = workbook.createSheet("Expenses");
                createExpenseSheet(
                        expenseSheet,
                        email,
                        startDate,
                        endDate,
                        titleStyle,
                        headerStyle,
                        dataStyle,
                        currencyStyle,
                        dateStyle
                );
            }

            // Create Incomes Sheet if included
            if (includeIncomes != null && includeIncomes) {
                Sheet incomeSheet = workbook.createSheet("Incomes");
                createIncomeSheet(
                        incomeSheet,
                        email,
                        startDate,
                        endDate,
                        titleStyle,
                        headerStyle,
                        dataStyle,
                        currencyStyle,
                        dateStyle
                );
            }

            // Create Budget Analysis Sheet if included
            if (includeBudgets != null && includeBudgets) {
                Sheet budgetSheet = workbook.createSheet("Budget Analysis");
                createBudgetAnalysisSheet(
                        budgetSheet,
                        email,
                        startDate,
                        endDate,
                        titleStyle,
                        headerStyle,
                        dataStyle,
                        currencyStyle
                );
            }

            // Auto-size columns in all sheets
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int j = 0; j < 10; j++) {  // Adjust column count based on your needs
                    sheet.autoSizeColumn(j);
                }
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Create the summary sheet with overall financial metrics
     */
    private void createSummarySheet(
            Sheet sheet,
            String email,
            LocalDate startDate,
            LocalDate endDate,
            String reportTitle,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle summaryStyle,
            CellStyle currencyStyle) {

        // Set column widths
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 4000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(reportTitle);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Date range
        rowNum++;
        Row dateRow = sheet.createRow(rowNum++);
        Cell dateLabel = dateRow.createCell(0);
        dateLabel.setCellValue("Period:");
        dateLabel.setCellStyle(headerStyle);

        Cell dateValue = dateRow.createCell(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        dateValue.setCellValue(startDate.format(formatter) + " - " + endDate.format(formatter));
        dateValue.setCellStyle(summaryStyle);

        rowNum++;

        // Get financial data for the period
        List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(email, startDate, endDate);
        List<Income> incomes = incomeRepository.findByCustomerEmailAndDateBetween(email, startDate, endDate);

        double totalExpense = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double totalIncome = incomes.stream().mapToDouble(Income::getAmount).sum();
        double netSavings = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0;

        // Create financial summary section
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Financial Summary");
        headerCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        // Total Income
        Row incomeRow = sheet.createRow(rowNum++);
        Cell incomeLabel = incomeRow.createCell(0);
        incomeLabel.setCellValue("Total Income");
        incomeLabel.setCellStyle(summaryStyle);

        Cell incomeValue = incomeRow.createCell(1);
        incomeValue.setCellValue(totalIncome);
        incomeValue.setCellStyle(currencyStyle);

        // Total Expenses
        Row expenseRow = sheet.createRow(rowNum++);
        Cell expenseLabel = expenseRow.createCell(0);
        expenseLabel.setCellValue("Total Expenses");
        expenseLabel.setCellStyle(summaryStyle);

        Cell expenseValue = expenseRow.createCell(1);
        expenseValue.setCellValue(totalExpense);
        expenseValue.setCellStyle(currencyStyle);

        // Net Savings
        Row savingsRow = sheet.createRow(rowNum++);
        Cell savingsLabel = savingsRow.createCell(0);
        savingsLabel.setCellValue("Net Savings");
        savingsLabel.setCellStyle(summaryStyle);

        Cell savingsValue = savingsRow.createCell(1);
        savingsValue.setCellValue(netSavings);
        savingsValue.setCellStyle(currencyStyle);

        // Savings Rate
        Row rateRow = sheet.createRow(rowNum++);
        Cell rateLabel = rateRow.createCell(0);
        rateLabel.setCellValue("Savings Rate");
        rateLabel.setCellStyle(summaryStyle);

        Cell rateValue = rateRow.createCell(1);
        rateValue.setCellValue(savingsRate + "%");
        rateValue.setCellStyle(summaryStyle);

        rowNum += 2;

        // Top Expense Categories
        if (!expenses.isEmpty()) {
            Row topExpensesHeader = sheet.createRow(rowNum++);
            Cell topExpensesHeaderCell = topExpensesHeader.createCell(0);
            topExpensesHeaderCell.setCellValue("Top Expense Categories");
            topExpensesHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

            // Group expenses by category
            Map<String, Double> expensesByCategory = expenses.stream()
                    .collect(Collectors.groupingBy(
                            Expense::getCategory,
                            Collectors.summingDouble(Expense::getAmount)
                    ));

            // Sort by amount (descending)
            List<Map.Entry<String, Double>> sortedExpenses = expensesByCategory.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(5)  // Top 5 categories
                    .collect(Collectors.toList());

            for (Map.Entry<String, Double> entry : sortedExpenses) {
                Row categoryRow = sheet.createRow(rowNum++);
                Cell categoryLabel = categoryRow.createCell(0);
                categoryLabel.setCellValue(entry.getKey());
                categoryLabel.setCellStyle(summaryStyle);

                Cell categoryValue = categoryRow.createCell(1);
                categoryValue.setCellValue(entry.getValue());
                categoryValue.setCellStyle(currencyStyle);
            }
        }

        rowNum += 2;

        // Income Sources
        if (!incomes.isEmpty()) {
            Row incomeSourcesHeader = sheet.createRow(rowNum++);
            Cell incomeSourcesHeaderCell = incomeSourcesHeader.createCell(0);
            incomeSourcesHeaderCell.setCellValue("Income Sources");
            incomeSourcesHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

            // Group incomes by source
            Map<String, Double> incomesBySource = incomes.stream()
                    .collect(Collectors.groupingBy(
                            Income::getSource,
                            Collectors.summingDouble(Income::getAmount)
                    ));

            // Sort by amount (descending)
            List<Map.Entry<String, Double>> sortedIncomes = incomesBySource.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

            for (Map.Entry<String, Double> entry : sortedIncomes) {
                Row sourceRow = sheet.createRow(rowNum++);
                Cell sourceLabel = sourceRow.createCell(0);
                sourceLabel.setCellValue(entry.getKey());
                sourceLabel.setCellStyle(summaryStyle);

                Cell sourceValue = sourceRow.createCell(1);
                sourceValue.setCellValue(entry.getValue());
                sourceValue.setCellStyle(currencyStyle);
            }
        }
    }


    private void createExpenseSheet(
            Sheet sheet,
            String email,
            LocalDate startDate,
            LocalDate endDate,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle currencyStyle,
            CellStyle dateStyle) {

        // Set column widths
        sheet.setColumnWidth(0, 3000);  // Date
        sheet.setColumnWidth(1, 5000);  // Category
        sheet.setColumnWidth(2, 3000);  // Amount
        sheet.setColumnWidth(3, 5000);  // Description/Notes
        sheet.setColumnWidth(4, 3000);  // Tags

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Expense Transactions");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Date range subtitle
        rowNum++;
        Row dateRow = sheet.createRow(rowNum++);
        Cell dateRangeCell = dateRow.createCell(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        dateRangeCell.setCellValue("Period: " + startDate.format(formatter) + " - " + endDate.format(formatter));
        dateRangeCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 4));

        rowNum++;

        // Table header
        Row headerRow = sheet.createRow(rowNum++);
        String[] columns = {"Date", "Category", "Amount", "Description", "Tags"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Get expense data
        List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(email, startDate, endDate);

        // Sort by date (newest first)
        expenses.sort(Comparator.comparing(Expense::getDate).reversed());

        // Add expense rows
        for (Expense expense : expenses) {
            Row row = sheet.createRow(rowNum++);

            // Date
            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(Date.from(expense.getDate().toLocalDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
            dateCell.setCellStyle(dateStyle);

            // Category
            Cell categoryCell = row.createCell(1);
            categoryCell.setCellValue(expense.getCategory());
            categoryCell.setCellStyle(dataStyle);

            // Amount
            Cell amountCell = row.createCell(2);
            amountCell.setCellValue(expense.getAmount());
            amountCell.setCellStyle(currencyStyle);

            // Description/Notes
            Cell descriptionCell = row.createCell(3);
            descriptionCell.setCellValue(expense.getNote() != null ? expense.getNote() : "");
            descriptionCell.setCellStyle(dataStyle);

            // Tags
            Cell tagsCell = row.createCell(4);
            tagsCell.setCellValue(expense.getTags() != null ? expense.getTags() : "");
            tagsCell.setCellStyle(dataStyle);
        }

        rowNum += 2;

        // Add category summary
        Row categorySummaryHeader = sheet.createRow(rowNum++);
        Cell categorySummaryHeaderCell = categorySummaryHeader.createCell(0);
        categorySummaryHeaderCell.setCellValue("Expense Summary by Category");
        categorySummaryHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row categoryHeaderRow = sheet.createRow(rowNum++);
        Cell categoryHeaderCell = categoryHeaderRow.createCell(0);
        categoryHeaderCell.setCellValue("Category");
        categoryHeaderCell.setCellStyle(headerStyle);

        Cell amountHeaderCell = categoryHeaderRow.createCell(1);
        amountHeaderCell.setCellValue("Total Amount");
        amountHeaderCell.setCellStyle(headerStyle);

        // Group expenses by category
        Map<String, Double> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));

        // Sort by amount (descending)
        List<Map.Entry<String, Double>> sortedExpenses = expensesByCategory.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Add category summary rows
        for (Map.Entry<String, Double> entry : sortedExpenses) {
            Row row = sheet.createRow(rowNum++);

            Cell categoryCell = row.createCell(0);
            categoryCell.setCellValue(entry.getKey());
            categoryCell.setCellStyle(dataStyle);

            Cell totalCell = row.createCell(1);
            totalCell.setCellValue(entry.getValue());
            totalCell.setCellStyle(currencyStyle);
        }

        // Add total row
        Row totalRow = sheet.createRow(rowNum++);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("Total Expenses");
        totalLabel.setCellStyle(headerStyle);

        Cell totalValue = totalRow.createCell(1);
        double total = sortedExpenses.stream().mapToDouble(Map.Entry::getValue).sum();
        totalValue.setCellValue(total);
        totalValue.setCellStyle(currencyStyle);
    }


    private void createIncomeSheet(
            Sheet sheet,
            String email,
            LocalDate startDate,
            LocalDate endDate,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle currencyStyle,
            CellStyle dateStyle) {

        // Set column widths
        sheet.setColumnWidth(0, 3000);  // Date
        sheet.setColumnWidth(1, 5000);  // Source
        sheet.setColumnWidth(2, 3000);  // Amount
        sheet.setColumnWidth(3, 5000);  // Description
        sheet.setColumnWidth(4, 3000);  // Tags

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Income Transactions");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Date range subtitle
        rowNum++;
        Row dateRow = sheet.createRow(rowNum++);
        Cell dateRangeCell = dateRow.createCell(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        dateRangeCell.setCellValue("Period: " + startDate.format(formatter) + " - " + endDate.format(formatter));
        dateRangeCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 4));

        rowNum++;

        // Table header
        Row headerRow = sheet.createRow(rowNum++);
        String[] columns = {"Date", "Source", "Amount", "Description", "Tags"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Get income data
        List<Income> incomes = incomeRepository.findByCustomerEmailAndDateBetween(email, startDate, endDate);

        // Sort by date (newest first)
        incomes.sort(Comparator.comparing(Income::getDate).reversed());

        // Add income rows
        for (Income income : incomes) {
            Row row = sheet.createRow(rowNum++);

            // Date
            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(Date.from(income.getDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
            dateCell.setCellStyle(dateStyle);

            // Source
            Cell sourceCell = row.createCell(1);
            sourceCell.setCellValue(income.getSource());
            sourceCell.setCellStyle(dataStyle);

            // Amount
            Cell amountCell = row.createCell(2);
            amountCell.setCellValue(income.getAmount());
            amountCell.setCellStyle(currencyStyle);

            // Description
            Cell descriptionCell = row.createCell(3);
            descriptionCell.setCellValue(income.getDescription() != null ? income.getDescription() : "");
            descriptionCell.setCellStyle(dataStyle);

            // Tags
            Cell tagsCell = row.createCell(4);
            tagsCell.setCellValue(income.getTags() != null ? income.getTags() : "");
            tagsCell.setCellStyle(dataStyle);
        }

        rowNum += 2;

        // Add source summary
        Row sourceSummaryHeader = sheet.createRow(rowNum++);
        Cell sourceSummaryHeaderCell = sourceSummaryHeader.createCell(0);
        sourceSummaryHeaderCell.setCellValue("Income Summary by Source");
        sourceSummaryHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row sourceHeaderRow = sheet.createRow(rowNum++);
        Cell sourceHeaderCell = sourceHeaderRow.createCell(0);
        sourceHeaderCell.setCellValue("Source");
        sourceHeaderCell.setCellStyle(headerStyle);

        Cell amountHeaderCell = sourceHeaderRow.createCell(1);
        amountHeaderCell.setCellValue("Total Amount");
        amountHeaderCell.setCellStyle(headerStyle);

        // Group incomes by source
        Map<String, Double> incomesBySource = incomes.stream()
                .collect(Collectors.groupingBy(
                        Income::getSource,
                        Collectors.summingDouble(Income::getAmount)
                ));

        // Sort by amount (descending)
        List<Map.Entry<String, Double>> sortedIncomes = incomesBySource.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Add source summary rows
        for (Map.Entry<String, Double> entry : sortedIncomes) {
            Row row = sheet.createRow(rowNum++);

            Cell sourceCell = row.createCell(0);
            sourceCell.setCellValue(entry.getKey());
            sourceCell.setCellStyle(dataStyle);

            Cell totalCell = row.createCell(1);
            totalCell.setCellValue(entry.getValue());
            totalCell.setCellStyle(currencyStyle);
        }

        // Add total row
        Row totalRow = sheet.createRow(rowNum++);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("Total Income");
        totalLabel.setCellStyle(headerStyle);

        Cell totalValue = totalRow.createCell(1);
        double total = sortedIncomes.stream().mapToDouble(Map.Entry::getValue).sum();
        totalValue.setCellValue(total);
        totalValue.setCellStyle(currencyStyle);
    }

    private void createBudgetAnalysisSheet(
            Sheet sheet,
            String email,
            LocalDate startDate,
            LocalDate endDate,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle currencyStyle) {

        // Set column widths
        sheet.setColumnWidth(0, 5000);  // Category
        sheet.setColumnWidth(1, 3000);  // Budget Amount
        sheet.setColumnWidth(2, 3000);  // Actual Amount
        sheet.setColumnWidth(3, 3000);  // Difference
        sheet.setColumnWidth(4, 3000);  // % Used

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Budget Analysis");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Date range subtitle
        rowNum++;
        Row dateRow = sheet.createRow(rowNum++);
        Cell dateRangeCell = dateRow.createCell(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        dateRangeCell.setCellValue("Period: " + startDate.format(formatter) + " - " + endDate.format(formatter));
        dateRangeCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 4));

        rowNum++;

        // Table header
        Row headerRow = sheet.createRow(rowNum++);
        String[] columns = {"Category", "Budget Amount", "Actual Amount", "Difference", "% Used"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Get budget data
        List<Budget> budgets = budgetRepository.findByCustomerEmail(email);

        // Get actual expenses
        List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(email, startDate, endDate);

        // Calculate actual expenses by category
        Map<String, Double> actualExpensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));

        // Create comparison rows
        for (Budget budget : budgets) {
            Row row = sheet.createRow(rowNum++);

            // Category
            Cell categoryCell = row.createCell(0);
            categoryCell.setCellValue(budget.getCategory());
            categoryCell.setCellStyle(dataStyle);

            // Budget Amount
            Cell budgetCell = row.createCell(1);
            budgetCell.setCellValue(budget.getAmount());
            budgetCell.setCellStyle(currencyStyle);

            // Actual Amount
            double actual = actualExpensesByCategory.getOrDefault(budget.getCategory(), 0.0);
            Cell actualCell = row.createCell(2);
            actualCell.setCellValue(actual);
            actualCell.setCellStyle(currencyStyle);

            // Difference
            double difference = budget.getAmount() - actual;
            Cell differenceCell = row.createCell(3);
            differenceCell.setCellValue(difference);
            differenceCell.setCellStyle(currencyStyle);

            // % Used
            double percentUsed = budget.getAmount() > 0 ? (actual / budget.getAmount()) * 100 : 0;
            Cell percentCell = row.createCell(4);
            percentCell.setCellValue(percentUsed + "%");
            percentCell.setCellStyle(dataStyle);
        }
    }

    /**
     * Generate financial insights based on transaction data
     */
    public List<FinancialInsight> generateFinancialInsights(
            String email,
            List<Expense> expenses,
            List<Income> incomes,
            LocalDate startDate,
            LocalDate endDate) {

        List<FinancialInsight> insights = new ArrayList<>();

        // If no data, return empty list
        if (expenses.isEmpty() && incomes.isEmpty()) {
            return insights;
        }

        // Calculate totals
        double totalIncome = incomes.stream().mapToDouble(Income::getAmount).sum();
        double totalExpense = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double netSavings = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0;

        // 1. Savings Rate Insight
        FinancialInsight savingsInsight = new FinancialInsight();
        savingsInsight.setTitle("Monthly Savings Rate");

        if (savingsRate >= 20) {
            savingsInsight.setDescription(
                    String.format("Great job! You're saving %.1f%% of your income this month.", savingsRate));
            savingsInsight.setType("success");
            savingsInsight.setIcon("savings");
        } else if (savingsRate > 0) {
            savingsInsight.setDescription(
                    String.format("You're saving %.1f%% of your income. The recommended target is at least 20%%.", savingsRate));
            savingsInsight.setType("info");
            savingsInsight.setIcon("account_balance");
        } else {
            savingsInsight.setDescription(
                    "You're spending more than you earn this month. Consider reviewing your expenses.");
            savingsInsight.setType("warning");
            savingsInsight.setIcon("warning");
        }

        insights.add(savingsInsight);

        // 2. Top Spending Category Insight
        if (!expenses.isEmpty()) {
            Map<String, Double> expensesByCategory = expenses.stream()
                    .collect(Collectors.groupingBy(
                            Expense::getCategory,
                            Collectors.summingDouble(Expense::getAmount)
                    ));

            // Find the top category
            Map.Entry<String, Double> topCategory = expensesByCategory.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (topCategory != null) {
                double percentOfTotal = totalExpense > 0 ? (topCategory.getValue() / totalExpense) * 100 : 0;

                FinancialInsight categoryInsight = new FinancialInsight();
                categoryInsight.setTitle("Top Spending Category");
                categoryInsight.setDescription(
                        String.format("Your highest spending category is %s (%.1f%% of total expenses).",
                                topCategory.getKey(), percentOfTotal));

                if (percentOfTotal > 50) {
                    categoryInsight.setType("warning");
                    categoryInsight.setIcon("pie_chart");
                    categoryInsight.setDescription(
                            String.format("Your highest spending category is %s (%.1f%% of total expenses). " +
                                            "Consider diversifying your spending for better financial balance.",
                                    topCategory.getKey(), percentOfTotal));
                } else {
                    categoryInsight.setType("info");
                    categoryInsight.setIcon("pie_chart");
                }

                insights.add(categoryInsight);
            }
        }

        // 3. Budget Adherence Insight
        List<Budget> budgets = budgetRepository.findByCustomerEmail(email);
        if (!budgets.isEmpty() && !expenses.isEmpty()) {
            Map<String, Double> actualExpensesByCategory = expenses.stream()
                    .collect(Collectors.groupingBy(
                            Expense::getCategory,
                            Collectors.summingDouble(Expense::getAmount)
                    ));

            int overBudgetCount = 0;
            String overBudgetCategory = null;
            double highestOverage = 0;

            for (Budget budget : budgets) {
                double actual = actualExpensesByCategory.getOrDefault(budget.getCategory(), 0.0);
                if (actual > budget.getAmount()) {
                    overBudgetCount++;
                    double overage = actual - budget.getAmount();
                    if (overage > highestOverage) {
                        highestOverage = overage;
                        overBudgetCategory = budget.getCategory();
                    }
                }
            }

            if (overBudgetCount > 0) {
                FinancialInsight budgetInsight = new FinancialInsight();
                budgetInsight.setTitle("Budget Alert");

                if (overBudgetCount > 1) {
                    budgetInsight.setDescription(
                            String.format("You have exceeded your budget in %d categories. %s has the highest overage of %s.",
                                    overBudgetCount, overBudgetCategory, formatCurrency(highestOverage)));
                } else {
                    budgetInsight.setDescription(
                            String.format("You have exceeded your budget for %s by %s.",
                                    overBudgetCategory, formatCurrency(highestOverage)));
                }

                budgetInsight.setType("warning");
                budgetInsight.setIcon("trending_up");
                insights.add(budgetInsight);
            } else if (!budgets.isEmpty()) {
                FinancialInsight budgetInsight = new FinancialInsight();
                budgetInsight.setTitle("Budget Success");
                budgetInsight.setDescription("You're staying within your budget in all categories. Great financial discipline!");
                budgetInsight.setType("success");
                budgetInsight.setIcon("check_circle");
                insights.add(budgetInsight);
            }
        }

        // 4. Day-to-day spending insights
        if (!expenses.isEmpty()) {
            // Sort expenses by date
            List<Expense> sortedExpenses = new ArrayList<>(expenses);
            sortedExpenses.sort(Comparator.comparing(expense -> expense.getDate()));

            // Calculate average daily spending
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            double avgDailySpending = totalExpense / daysBetween;

            // Group expenses by day to find peak spending days
            Map<LocalDate, Double> expensesByDay = expenses.stream()
                    .collect(Collectors.groupingBy(
                            expense -> expense.getDate().toLocalDate(),
                            Collectors.summingDouble(Expense::getAmount)
                    ));

            // Find the day with highest spending
            Map.Entry<LocalDate, Double> peakDay = expensesByDay.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (peakDay != null && peakDay.getValue() > 2 * avgDailySpending) {
                FinancialInsight spendingInsight = new FinancialInsight();
                spendingInsight.setTitle("Spending Pattern");
                spendingInsight.setDescription(
                        String.format("You had a high spending day on %s with total expenses of %s, " +
                                        "which is %.1fx your daily average.",
                                peakDay.getKey().format(DateTimeFormatter.ofPattern("MMM d")),
                                formatCurrency(peakDay.getValue()),
                                peakDay.getValue() / avgDailySpending));
                spendingInsight.setType("info");
                spendingInsight.setIcon("calendar_today");
                insights.add(spendingInsight);
            }
        }

        return insights;
    }

    public List<MonthlySummary> generateMonthlySummary(String email, int months) {
        List<MonthlySummary> summary = new ArrayList<>();

        // Calculate date range
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusMonths(months - 1).withDayOfMonth(1);

        // Get all expenses and incomes in the range
        List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateAfter(
                email, startDate.atStartOfDay());

        List<Income> incomes = incomeRepository.findByCustomerEmailAndDateAfter(
                email, startDate);

        // Generate data for each month
        for (int i = 0; i < months; i++) {
            YearMonth yearMonth = YearMonth.from(now.minusMonths(i));
            LocalDate monthStart = yearMonth.atDay(1);
            LocalDate monthEnd = yearMonth.atEndOfMonth();

            // Filter expenses for this month
            double monthlyExpense = expenses.stream()
                    .filter(e -> isDateInRange(e.getDate().toLocalDate(), monthStart, monthEnd))
                    .mapToDouble(Expense::getAmount)
                    .sum();

            // Filter incomes for this month
            double monthlyIncome = incomes.stream()
                    .filter(inc -> isDateInRange(inc.getDate(), monthStart, monthEnd))
                    .mapToDouble(Income::getAmount)
                    .sum();

            // Calculate net amount and savings rate
            double netAmount = monthlyIncome - monthlyExpense;
            double savingsRate = monthlyIncome > 0 ? (netAmount / monthlyIncome) * 100 : 0;

            // Create summary object
            MonthlySummary monthlySummary = new MonthlySummary();
            monthlySummary.setMonth(yearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            monthlySummary.setIncome(monthlyIncome);
            monthlySummary.setExpense(monthlyExpense);
            monthlySummary.setNet(netAmount);
            monthlySummary.setSavingsRate(savingsRate);

            summary.add(monthlySummary);
        }

        // Sort by month (oldest first)
        summary.sort(Comparator.comparing(s -> YearMonth.parse(s.getMonth(),
                DateTimeFormatter.ofPattern("MMM yyyy"))));

        return summary;
    }

    private boolean isDateInRange(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private String formatCurrency(double amount) {
        return String.format("₹%.2f", amount);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }


    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }


    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setDataFormat(workbook.createDataFormat().getFormat("₹#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }


    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setDataFormat(workbook.createDataFormat().getFormat("dd-mmm-yyyy"));
        return style;
    }


    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }


    private CellStyle createSummaryStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}