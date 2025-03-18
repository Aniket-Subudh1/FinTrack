import { Component, OnInit } from '@angular/core';
import { ExpenseService } from '../../service/expense.service';
import { ExpenseCategorySummary } from '../../service/expense.service';
import { saveAs } from 'file-saver';
import { NgIf, NgFor, DatePipe , CurrencyPipe  } from '@angular/common';
import { FormsModule } from '@angular/forms';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Component({
  selector: 'app-expense-report',
  standalone: true, // Ensure it's a standalone component
  imports: [NgIf, NgFor, FormsModule, DatePipe, CurrencyPipe ], // Import FormsModule here
  templateUrl: './expense-report.component.html',
  styleUrls: ['./expense-report.component.css'],
})
export class ExpenseReportComponent implements OnInit {
  expenses: any[] = [];
  expenseSummary: ExpenseCategorySummary[] = [];
  startDate: string = '';
  endDate: string = '';
  errorMessage: string = '';

  constructor(private expenseService: ExpenseService) {}

  ngOnInit(): void {
    this.getExpenseSummary();
  }

  fetchExpensesByDateRange() {
    if (!this.startDate || !this.endDate) {
      this.errorMessage = 'Please select a start and end date.';
      return;
    }

    this.expenseService.getExpensesByDateRange(this.startDate, this.endDate).subscribe(
      (data) => {
        this.expenses = data;
        this.errorMessage = '';
      },
      (error) => {
        console.error('Error fetching expenses:', error);
        this.errorMessage = 'Failed to load expenses.';
      }
    );
  }

  getExpenseSummary() {
    this.expenseService.getExpenseSummary().subscribe(
      (data) => {
        this.expenseSummary = data;
      },
      (error) => {
        console.error('Error fetching expense summary:', error);
      }
    );
  }
  exportAsPDF() {
    const doc = new jsPDF();

    // Title
    doc.text('Expense Report', 10, 10);

    let finalY = 20; // Initial Y position

    // Expense Table
    if (this.expenses.length > 0) {
      const expenseData = this.expenses.map(expense => [
        expense.category,
        expense.amount,
        new Date(expense.date).toLocaleDateString(),
      ]);

      doc.text('Filtered Expenses', 10, finalY);
      autoTable(doc, {
        head: [['Category', 'Amount', 'Date']],
        body: expenseData,
        startY: finalY + 5,
      });

      // Get the last printed position after the table
      finalY = (doc as any).lastAutoTable.finalY + 10;
    }

    // Expense Summary Table
    if (this.expenseSummary.length > 0) {
      const summaryData = this.expenseSummary.map(summary => [
        summary.category,
        summary.totalAmount
      ]);

      doc.text('Expense Breakdown', 10, finalY);
      autoTable(doc, {
        head: [['Category', 'Total Amount']],
        body: summaryData,
        startY: finalY + 5,
      });
    }

    // Save the PDF
    doc.save('Expense_Report.pdf');
  }

}
