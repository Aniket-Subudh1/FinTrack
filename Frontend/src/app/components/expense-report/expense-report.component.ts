import { Component, OnInit } from '@angular/core';
import { ExpenseService, ExpenseCategorySummary, ExpenseFilterParams } from '../../service/expense.service';
import { saveAs } from 'file-saver';
import { NgIf, NgFor, DatePipe, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';

@Component({
  selector: 'app-expense-report',
  standalone: true, // Ensure it's a standalone component
  imports: [NgIf, NgFor, FormsModule, DatePipe, SidebarComponent], // Import FormsModule here
  templateUrl: './expense-report.component.html',
  styleUrls: ['./expense-report.component.css'],
})
export class ExpenseReportComponent {
  expenses: any[] = [];
  expenseSummary: ExpenseCategorySummary[] = [];
  startDate: string = '';
  email: string = '';
  endDate: string = '';
  category: string = '';
  minAmount: string = '';
  maxAmount: string = '';
  tags: string = '';
  errorMessage: string = '';
  isSidebarOpen = true;

  constructor(private expenseService: ExpenseService) {}

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }
  fetchFilteredExpenses() {
    if (!this.startDate || !this.endDate) {
      alert('Please enter start date and end date.');
      return;
    }

    const filters: ExpenseFilterParams = {
      startDate: this.startDate,
      endDate: this.endDate,
      category: this.category || undefined,
      minAmount: this.minAmount ? parseFloat(this.minAmount) : undefined,
      maxAmount: this.maxAmount ? parseFloat(this.maxAmount) : undefined,
      tags: this.tags || undefined,
    };

    this.expenseService.filterExpenses(filters).subscribe(
      (data) => {
        this.expenses = data;
        console.log('Filtered Expenses:', data);
      },
      (error) => {
        console.error('Error fetching expenses:', error);
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
      const expenseData = this.expenses.map((expense) => [
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
      const summaryData = this.expenseSummary.map((summary) => [
        summary.category,
        summary.totalAmount,
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
