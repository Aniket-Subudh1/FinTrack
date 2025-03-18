package com.personalfinancetracker.backend.controllers;

import com.personalfinancetracker.backend.dto.ExpenseFilterRequest;
import com.personalfinancetracker.backend.dto.ExpenseCategorySummary;
import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.repository.ExpenseRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")  // Allow frontend to access
public class ExpenseReportController {

    @Autowired
    private ExpenseRepository expenseRepository;

    // 1. Get Expenses by Date Range
    @PostMapping("/filter")
    public ResponseEntity<List<Expense>> getFilteredExpenses(@RequestBody ExpenseFilterRequest filterRequest) {
        try {
            List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(
                    filterRequest.getEmail(),
                    filterRequest.getStartDate(),
                    filterRequest.getEndDate()
            );
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


}