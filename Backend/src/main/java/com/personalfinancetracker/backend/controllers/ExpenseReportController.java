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

    // 2. Get Expense Breakdown by Category
    @GetMapping("/summary/{email}")
    public ResponseEntity<List<ExpenseCategorySummary>> getExpenseSummary(@PathVariable String email) {
        try {
            List<ExpenseCategorySummary> summary = expenseRepository.getExpenseSummaryByCategory(email);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 3. Export Expense Report as PDF
    @PostMapping(value = "/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportExpensesAsPDF(@RequestBody ExpenseFilterRequest filterRequest) {
        try {
            List<Expense> expenses = expenseRepository.findByCustomerEmailAndDateBetween(
                    filterRequest.getEmail(),
                    filterRequest.getStartDate(),
                    filterRequest.getEndDate()
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("Expense Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Add Report Details
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
            Paragraph details = new Paragraph(
                    "Email: " + filterRequest.getEmail() + "\n" +
                            "Period: " + filterRequest.getStartDate() + " to " + filterRequest.getEndDate(),
                    normalFont
            );
            details.setSpacingAfter(20f);
            document.add(details);

            // Create Table
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1.5f, 2f}); // Set column widths

            // Table Headers
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(BaseColor.DARK_GRAY);
            headerCell.setPadding(5);

            headerCell.setPhrase(new Phrase("Category", headerFont));
            table.addCell(headerCell);
            headerCell.setPhrase(new Phrase("Amount", headerFont));
            table.addCell(headerCell);
            headerCell.setPhrase(new Phrase("Date", headerFont));
            table.addCell(headerCell);

            // Table Data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
            for (Expense expense : expenses) {
                PdfPCell dataCell = new PdfPCell();
                dataCell.setPadding(5);

                dataCell.setPhrase(new Phrase(expense.getCategory(), dataFont));
                table.addCell(dataCell);
                dataCell.setPhrase(new Phrase(String.format("%.2f", expense.getAmount()), dataFont));
                table.addCell(dataCell);
                dataCell.setPhrase(new Phrase(expense.getDate().format(formatter), dataFont));
                table.addCell(dataCell);
            }

            document.add(table);
            document.close();

            // Prepare Response
            byte[] pdfBytes = outputStream.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expense_report_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf");
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (DocumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}