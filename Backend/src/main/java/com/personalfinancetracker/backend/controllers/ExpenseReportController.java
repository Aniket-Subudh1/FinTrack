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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
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
        List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(
                filterRequest.getEmail(),
                filterRequest.getStartDate(),
                filterRequest.getEndDate()
        );
        return ResponseEntity.ok(expenses);
    }

    // 2. Get Expense Breakdown by Category
    @GetMapping("/summary/{email}")
    public ResponseEntity<List<ExpenseCategorySummary>> getExpenseSummary(@PathVariable String email) {
        List<ExpenseCategorySummary> summary = expenseRepository.getExpenseSummaryByCategory(email);
        return ResponseEntity.ok(summary);
    }

    // 3. Export Expense Report as PDF
    @PostMapping("/export-pdf")
    public ResponseEntity<byte[]> exportExpensesAsPDF(@RequestBody ExpenseFilterRequest filterRequest) {
        List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(
                filterRequest.getEmail(),
                filterRequest.getStartDate(),
                filterRequest.getEndDate()
        );

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("Expense Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Create Table
            PdfPTable table = new PdfPTable(3); // Columns: Category, Amount, Date
            table.setWidthPercentage(100);

            PdfPCell cell1 = new PdfPCell(new Phrase("Category"));
            PdfPCell cell2 = new PdfPCell(new Phrase("Amount"));
            PdfPCell cell3 = new PdfPCell(new Phrase("Date"));
            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);

            // Add Expense Data
            for (Expense expense : expenses) {
                table.addCell(expense.getCategory());
                table.addCell(String.valueOf(expense.getAmount()));
                table.addCell(expense.getDate().toString());
            }

            document.add(table);
            document.close();

            // Prepare Response
            byte[] pdfBytes = outputStream.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=expense_report.pdf");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
