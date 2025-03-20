package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.*;
import com.personalfinancetracker.backend.entities.Customer;
import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.entities.Income;
import com.personalfinancetracker.backend.entities.SavedReport;
import com.personalfinancetracker.backend.repository.CustomerRepository;
import com.personalfinancetracker.backend.repository.ExpenseRepository;
import com.personalfinancetracker.backend.repository.IncomeRepository;
import com.personalfinancetracker.backend.repository.SavedReportRepository;
import com.personalfinancetracker.backend.services.ReportGenerationService;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ExpenseReportController {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseReportController.class);

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CustomerRepository customerRepository;
    private final SavedReportRepository savedReportRepository;
    private final ReportGenerationService reportGenerationService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    @Autowired
    public ExpenseReportController(
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            CustomerRepository customerRepository,
            SavedReportRepository savedReportRepository,
            ReportGenerationService reportGenerationService,
            JwtUtil jwtUtil,
            HttpServletRequest request) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.customerRepository = customerRepository;
        this.savedReportRepository = savedReportRepository;
        this.reportGenerationService = reportGenerationService;
        this.jwtUtil = jwtUtil;
        this.request = request;
    }

    @PostMapping(value = "/export-excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExpensesToExcel(@RequestBody ReportRequest reportRequest) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for Excel export");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        try {
            // Parse dates as LocalDate and convert to LocalDateTime
            LocalDate startDateLocal = LocalDate.parse(reportRequest.getStartDate());
            LocalDate endDateLocal = LocalDate.parse(reportRequest.getEndDate());
            LocalDate startDate = LocalDate.from(startDateLocal.atStartOfDay());
            LocalDateTime endDate = endDateLocal.atTime(23, 59, 59, 999_999_999);

            // Generate the Excel file
            byte[] excelBytes = reportGenerationService.generateExcelReport(
                    email,
                    startDate,
                    LocalDate.from(endDate),
                    reportRequest.getReportTitle(),
                    reportRequest.getIncludeExpenses(),
                    reportRequest.getIncludeIncomes(),
                    reportRequest.getIncludeBudgets()
            );

            // Prepare response
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financial_report_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error generating Excel report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/save-configuration")
    public ResponseEntity<Map<String, Object>> saveReportConfiguration(@RequestBody SaveReportRequest saveRequest) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for saving report");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            SavedReport savedReport = new SavedReport();
            savedReport.setCustomer(customer);
            savedReport.setReportTitle(saveRequest.getReportTitle());
            savedReport.setReportType(saveRequest.getReportType());
            savedReport.setConfiguration(saveRequest.getConfiguration());
            savedReport.setCreatedDate(LocalDateTime.now());

            SavedReport result = savedReportRepository.save(savedReport);

            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("message", "Report configuration saved successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error saving report configuration: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to save report configuration");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/saved-configurations")
    public ResponseEntity<List<SavedReportDTO>> getSavedReportConfigurations() {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getting saved reports");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<SavedReport> savedReports = savedReportRepository.findByCustomerEmailOrderByCreatedDateDesc(email);

            List<SavedReportDTO> result = savedReports.stream()
                    .map(report -> new SavedReportDTO(
                            report.getId(),
                            report.getReportTitle(),
                            report.getReportType(),
                            report.getConfiguration(),
                            report.getCreatedDate()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching saved report configurations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/saved-configurations/{id}")
    public ResponseEntity<Map<String, String>> deleteSavedReportConfiguration(@PathVariable Long id) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for deleting saved report");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            SavedReport savedReport = savedReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Saved report not found"));

            if (!savedReport.getCustomer().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "You don't have permission to delete this report"));
            }

            savedReportRepository.delete(savedReport);

            return ResponseEntity.ok(Collections.singletonMap("message", "Report configuration deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting saved report configuration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to delete report configuration"));
        }
    }

    @GetMapping("/insights")
    public ResponseEntity<List<FinancialInsight>> getFinancialInsights(
            @RequestParam(required = false) String period) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getting insights");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            LocalDate startDateLocal;
            LocalDate endDateLocal = LocalDate.now();

            if ("week".equals(period)) {
                startDateLocal = endDateLocal.minusDays(7);
            } else if ("month".equals(period)) {
                startDateLocal = endDateLocal.withDayOfMonth(1);
            } else if ("year".equals(period)) {
                startDateLocal = endDateLocal.withDayOfYear(1);
            } else {
                startDateLocal = endDateLocal.minusDays(30);
            }

            // Convert to LocalDateTime for repository
            LocalDate startDate = LocalDate.from(startDateLocal.atStartOfDay());
            LocalDateTime endDate = endDateLocal.atTime(23, 59, 59, 999_999_999);

            List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(email, startDate, LocalDate.from(endDate));
            List<Income> incomes = incomeRepository.findByCustomerEmailAndDateBetween(email, startDate, LocalDate.from(endDate));

            List<FinancialInsight> insights = reportGenerationService.generateFinancialInsights(
                    email, expenses, incomes, startDate, LocalDate.from(endDate));

            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            logger.error("Error generating financial insights: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<List<MonthlySummary>> getMonthlySummary(
            @RequestParam(defaultValue = "6") int months) {
        String email = getEmailFromJwtCookie();
        if (email == null) {
            logger.warn("No valid JWT token found in cookie for getting monthly summary");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<MonthlySummary> summary = reportGenerationService.generateMonthlySummary(email, months);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error generating monthly summary: {}", e.getMessage(), e);
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