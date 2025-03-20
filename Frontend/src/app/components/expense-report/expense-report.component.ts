import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { ExpenseService, Expense, ExpenseCategorySummary } from '../../service/expense.service';
import { IncomeService, Income, IncomeSummary } from '../../service/income.service';
import { TransactionService, Transaction, TransactionInsight } from '../../service/transaction.service';
import { BudgetService, BudgetItem } from '../../service/budget.service';
import { saveAs } from 'file-saver';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin } from 'rxjs';
import { Router } from '@angular/router';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

interface ReportData {
  expenses: Expense[];
  incomes: Income[];
  transactions: Transaction[];
  expenseSummary: ExpenseCategorySummary[];
  incomeSummary: IncomeSummary[];
  budgetItems: BudgetItem[];
  insights: TransactionInsight[];
}

@Component({
  selector: 'app-expense-report',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatIconModule,
    SidebarComponent,
    DatePipe
  ],
  templateUrl: './expense-report.component.html',
  styleUrls: ['./expense-report.component.css']
})
export class ExpenseReportComponent implements OnInit {
  isSidebarOpen: boolean = true;
  isLoading: boolean = false;
  activeTab: string = 'generator';
  reportPeriod: string = 'month';
  reportFormat: string = 'pdf';
  exportingReport: boolean = false;
  showPreview: boolean = false;
  showCustomDateRange: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  reportForm!: FormGroup;

  reportData: ReportData = {
    expenses: [],
    incomes: [],
    transactions: [],
    expenseSummary: [],
    incomeSummary: [],
    budgetItems: [],
    insights: []
  };

  expenseChartData: any[] = [];
  incomeChartData: any[] = [];
  savingsChartData: any[] = [];

  today = new Date();
  defaultStartDate = new Date(this.today.getFullYear(), this.today.getMonth(), 1);
  defaultEndDate = new Date();

  reportTemplates = [
    { id: 'expense-summary', name: 'Expense Summary Report', description: 'Overview of all expenses by category' },
    { id: 'income-summary', name: 'Income Summary Report', description: 'Summary of all income sources' },
    { id: 'budget-analysis', name: 'Budget Analysis Report', description: 'Analysis of budgets vs. actual spending' },
    { id: 'savings-report', name: 'Savings Report', description: 'Comprehensive view of your saving patterns' },
    { id: 'comprehensive', name: 'Comprehensive Financial Report', description: 'Complete financial overview and analysis' }
  ];

  selectedTemplate: string = 'comprehensive';

  constructor(
    private fb: FormBuilder,
    private expenseService: ExpenseService,
    private incomeService: IncomeService,
    private transactionService: TransactionService,
    private budgetService: BudgetService,
    private http: HttpClient,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.initForm();
    this.updateDateRangeBasedOnPeriod('month');
    this.loadReportData();
  }

  initForm(): void {
    this.reportForm = this.fb.group({
      reportTitle: ['Financial Report', [Validators.required]],
      startDate: [this.formatDateForInput(this.defaultStartDate), [Validators.required]],
      endDate: [this.formatDateForInput(this.defaultEndDate), [Validators.required]],
      includeExpenses: [true],
      includeIncomes: [true],
      includeBudgets: [true],
      includeInsights: [true],
      includeCharts: [true]
    });
  }

  formatDateForInput(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    if (tab === 'generator') {
      this.loadReportData();
    }
  }

  updateReportPeriod(period: string): void {
    this.reportPeriod = period;
    this.showCustomDateRange = period === 'custom';
    if (period !== 'custom') {
      this.updateDateRangeBasedOnPeriod(period);
      this.loadReportData();
    }
  }

  updateDateRangeBasedOnPeriod(period: string): void {
    const today = new Date();
    let startDate: Date;
    const endDate = today;

    switch (period) {
      case 'week':
        startDate = new Date(today);
        startDate.setDate(today.getDate() - 7);
        break;
      case 'month':
        startDate = new Date(today.getFullYear(), today.getMonth(), 1);
        break;
      case 'quarter':
        startDate = new Date(today.getFullYear(), Math.floor(today.getMonth() / 3) * 3, 1);
        break;
      case 'year':
        startDate = new Date(today.getFullYear(), 0, 1);
        break;
      default:
        startDate = new Date(today.getFullYear(), today.getMonth(), 1);
    }

    this.reportForm.patchValue({
      startDate: this.formatDateForInput(startDate),
      endDate: this.formatDateForInput(endDate)
    });
  }

  loadReportData(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const startDate = this.reportForm.get('startDate')?.value;
    const endDate = this.reportForm.get('endDate')?.value;

    if (!startDate || !endDate) {
      this.errorMessage = 'Please select a valid date range';
      this.isLoading = false;
      return;
    }

    const expenseFilters = { startDate, endDate };

    forkJoin({
      expenses: this.expenseService.getExpenses().pipe(catchError(err => { console.error('Error loading expenses:', err); this.errorMessage = 'Failed to load expense data'; return []; })),
      incomes: this.incomeService.getIncomes().pipe(catchError(err => { console.error('Error loading incomes:', err); this.errorMessage = 'Failed to load income data'; return []; })),
      expenseSummary: this.expenseService.getExpenseSummary().pipe(catchError(err => { console.error('Error loading expense summary:', err); return []; })),
      incomeSummary: this.incomeService.getIncomeSummary().pipe(catchError(err => { console.error('Error loading income summary:', err); return []; })),
      budgetItems: this.budgetService.getBudgetItems().pipe(catchError(err => { console.error('Error loading budget items:', err); return []; }))
    }).subscribe({
      next: (data) => {
        const startDateObj = new Date(startDate);
        const endDateObj = new Date(endDate);
        endDateObj.setHours(23, 59, 59, 999);

        const filteredExpenses = data.expenses.filter(expense => {
          const expenseDate = new Date(expense.date);
          return expenseDate >= startDateObj && expenseDate <= endDateObj;
        });

        const filteredIncomes = data.incomes.filter(income => {
          const incomeDate = new Date(income.date);
          return incomeDate >= startDateObj && incomeDate <= endDateObj;
        });

        const expenseTransactions = filteredExpenses.map(expense => ({
          id: expense.id,
          type: 'expense' as const,
          amount: expense.amount,
          category: expense.category,
          date: new Date(expense.date),
          description: expense.note,
          tags: expense.tags || [],
          isRecurring: expense.isRecurring,
          recurringFrequency: expense.recurringFrequency
        }));

        const incomeTransactions = filteredIncomes.map(income => ({
          id: income.id,
          type: 'income' as const,
          amount: income.amount,
          category: income.source,
          date: new Date(income.date),
          description: income.description,
          tags: income.tags || [],
          isRecurring: income.isRecurring,
          recurringFrequency: income.recurringFrequency
        }));

        const allTransactions = [...expenseTransactions, ...incomeTransactions].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
        const insights = this.transactionService.generateInsights(allTransactions);

        this.prepareChartData(filteredExpenses, filteredIncomes, data.budgetItems);

        this.reportData = {
          expenses: filteredExpenses,
          incomes: filteredIncomes,
          transactions: allTransactions,
          expenseSummary: data.expenseSummary,
          incomeSummary: data.incomeSummary,
          budgetItems: data.budgetItems,
          insights
        };

        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading report data:', error);
        this.errorMessage = 'Failed to load report data. Please try again.';
        this.isLoading = false;
      }
    });
  }

  prepareChartData(expenses: Expense[], incomes: Income[], budgetItems: BudgetItem[]): void {
    const expenseByCategory: { [key: string]: number } = {};
    expenses.forEach(expense => {
      if (!expenseByCategory[expense.category]) expenseByCategory[expense.category] = 0;
      expenseByCategory[expense.category] += expense.amount;
    });

    this.expenseChartData = Object.entries(expenseByCategory).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value);

    const incomeBySource: { [key: string]: number } = {};
    incomes.forEach(income => {
      if (!incomeBySource[income.source]) incomeBySource[income.source] = 0;
      incomeBySource[income.source] += income.amount;
    });

    this.incomeChartData = Object.entries(incomeBySource).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value);

    const budgetVsActual: any[] = [];
    const expenseTotalByCategory: { [key: string]: number } = {};
    expenses.forEach(expense => {
      if (!expenseTotalByCategory[expense.category]) expenseTotalByCategory[expense.category] = 0;
      expenseTotalByCategory[expense.category] += expense.amount;
    });

    budgetItems.forEach(budget => {
      const actual = expenseTotalByCategory[budget.category] || 0;
      budgetVsActual.push({
        name: budget.category,
        series: [
          { name: 'Budget', value: budget.amount },
          { name: 'Actual', value: actual }
        ]
      });
    });

    budgetVsActual.sort((a, b) => {
      const aActual = a.series.find((s: any) => s.name === 'Actual')?.value || 0;
      const bActual = b.series.find((s: any) => s.name === 'Actual')?.value || 0;
      return bActual - aActual;
    });
  }

  getTotalExpenses(): number {
    return this.reportData.expenses.reduce((sum, expense) => sum + expense.amount, 0);
  }

  getTotalIncome(): number {
    return this.reportData.incomes.reduce((sum, income) => sum + income.amount, 0);
  }

  getNetSavings(): number {
    return this.getTotalIncome() - this.getTotalExpenses();
  }

  getSavingsRate(): number {
    const totalIncome = this.getTotalIncome();
    return totalIncome > 0 ? (this.getNetSavings() / totalIncome) * 100 : 0;
  }

  formatCurrency(amount: number): string {
    return `Rs.${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
  

  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    return new DatePipe('en-US').transform(date, 'MMM d, yyyy') || 'N/A';
  }

  generateReport(): void {
    if (this.reportForm.invalid) {
      this.errorMessage = 'Please fill all required fields';
      return;
    }

    this.exportingReport = true;

    const formValue = this.reportForm.value;
    const reportTitle = formValue.reportTitle;
    const startDate = new Date(formValue.startDate);
    const endDate = new Date(formValue.endDate);

    try {
      switch (this.reportFormat) {
        case 'pdf':
          this.exportToPdf(reportTitle, startDate, endDate);
          break;
        case 'csv':
          this.exportToCsv(reportTitle, startDate, endDate);
          break;
        case 'excel':
          this.exportToExcel(reportTitle, startDate, endDate);
          break;
      }
      this.showSuccess('Report generated successfully');
    } catch (error) {
      console.error('Error generating report:', error);
      this.errorMessage = 'Failed to generate report';
    } finally {
      this.exportingReport = false;
    }
  }

  exportToPdf(title: string, startDate: Date, endDate: Date): void {
    const doc = new jsPDF();

    doc.setFontSize(18);
    doc.text(title, 105, 15, { align: 'center' });

    doc.setFontSize(12);
    doc.text(`Period: ${this.formatDate(startDate)} to ${this.formatDate(endDate)}`, 105, 25, { align: 'center' });

    let yPos = 35;

    doc.setFontSize(14);
    doc.text('Financial Summary', 14, yPos);
    yPos += 10;

    doc.setFontSize(10);
    doc.text(`Total Income: ${this.formatCurrency(this.getTotalIncome())}`, 14, yPos);
    yPos += 7;
    doc.text(`Total Expenses: ${this.formatCurrency(this.getTotalExpenses())}`, 14, yPos);
    yPos += 7;
    doc.text(`Net Savings: ${this.formatCurrency(this.getNetSavings())}`, 14, yPos);
    yPos += 7;
    doc.text(`Savings Rate: ${this.getSavingsRate().toFixed(2)}%`, 14, yPos);
    yPos += 15;

    if (this.reportForm.get('includeExpenses')?.value) {
      doc.setFontSize(14);
      doc.text('Expense Breakdown', 14, yPos);
      yPos += 10;

      const expenseTableData = this.reportData.expenseSummary.map(item => [
        item.category,
        this.formatCurrency(item.totalAmount)
      ]);

      autoTable(doc, {
        head: [['Category', 'Amount']],
        body: expenseTableData,
        startY: yPos,
        margin: { left: 14 },
        theme: 'grid',
        headStyles: { fillColor: [244, 254, 0] }
      });

      const expenseRows = expenseTableData.length + 1;
      yPos += expenseRows * 10 + 5;
    }

    if (this.reportForm.get('includeIncomes')?.value) {
      doc.setFontSize(14);
      doc.text('Income Sources', 14, yPos);
      yPos += 10;

      const incomeTableData = this.reportData.incomeSummary.map(item => [
        item.source,
        this.formatCurrency(item.totalAmount)
      ]);

      autoTable(doc, {
        head: [['Source', 'Amount']],
        body: incomeTableData,
        startY: yPos,
        margin: { left: 14 },
        theme: 'grid',
        headStyles: { fillColor: [244, 254, 0] }
      });

      const incomeRows = incomeTableData.length + 1;
      yPos += incomeRows * 10 + 5;
    }

    if (this.reportForm.get('includeBudgets')?.value && this.reportData.budgetItems.length > 0) {
      if (yPos > 230) {
        doc.addPage();
        yPos = 20;
      }

      doc.setFontSize(14);
      doc.text('Budget Analysis', 14, yPos);
      yPos += 10;

      const expenseTotalByCategory: { [key: string]: number } = {};
      this.reportData.expenses.forEach(expense => {
        if (!expenseTotalByCategory[expense.category]) expenseTotalByCategory[expense.category] = 0;
        expenseTotalByCategory[expense.category] += expense.amount;
      });

      const budgetTableData = this.reportData.budgetItems.map(item => {
        const actual = expenseTotalByCategory[item.category] || 0;
        const difference = item.amount - actual;
        const percentUsed = item.amount > 0 ? (actual / item.amount) * 100 : 0;

        return [
          item.category,
          this.formatCurrency(item.amount),
          this.formatCurrency(actual),
          this.formatCurrency(difference),
          `${percentUsed.toFixed(1)}%`
        ];
      });

      autoTable(doc, {
        head: [['Category', 'Budget', 'Actual', 'Difference', 'Used %']],
        body: budgetTableData,
        startY: yPos,
        margin: { left: 14 },
        theme: 'grid',
        headStyles: { fillColor: [244, 254, 0] }
      });

      const budgetRows = budgetTableData.length + 1;
      yPos += budgetRows * 10 + 5;
    }

    if (this.reportForm.get('includeInsights')?.value && this.reportData.insights.length > 0) {
      if (yPos > 230) {
        doc.addPage();
        yPos = 20;
      }

      doc.setFontSize(14);
      doc.text('Financial Insights', 14, yPos);
      yPos += 10;

      this.reportData.insights.forEach(insight => {
        doc.setFontSize(11);
        doc.text(insight.title, 14, yPos);
        yPos += 5;

        doc.setFontSize(10);
        doc.text(insight.description, 14, yPos);
        yPos += 10;
      });
    }

    const pageCount = doc.getNumberOfPages();
    for (let i = 1; i <= pageCount; i++) {
      doc.setPage(i);
      doc.setFontSize(8);
      doc.text(`Generated on ${new Date().toLocaleDateString()} by FinTrack`, 105, doc.internal.pageSize.height - 10, { align: 'center' });
    }

    doc.save(`${title.replace(/\s+/g, '_')}_${this.formatDateForFilename(new Date())}.pdf`);
  }

  exportToCsv(title: string, startDate: Date, endDate: Date): void {
    const transactions = this.reportData.transactions;

    if (transactions.length === 0) {
      this.errorMessage = 'No transactions to export';
      return;
    }

    const headers = ['Date', 'Type', 'Category', 'Amount', 'Description', 'Tags'];

    const csvRows = [
      headers.join(','),
      ...transactions.map(t => {
        const formattedDate = this.formatDate(t.date);
        const type = t.type.charAt(0).toUpperCase() + t.type.slice(1);
        const category = t.category;
        const amount = t.amount.toString();
        const description = t.description ? `"${t.description.replace(/"/g, '""')}"` : '';
        const tags = t.tags && t.tags.length > 0 ? `"${t.tags.join(', ')}"` : '';
        return [formattedDate, type, category, amount, description, tags].join(',');
      })
    ];

    const csvContent = csvRows.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' });
    saveAs(blob, `${title.replace(/\s+/g, '_')}_${this.formatDateForFilename(new Date())}.csv`);
  }

  exportToExcel(title: string, startDate: Date, endDate: Date): void {
    this.isLoading = true;
    this.errorMessage = '';
  
    const BASE_URL = 'http://localhost:8080';
    const endpoint = `${BASE_URL}/api/reports/export-excel`; // Changed from /api/expenses to /api/reports
  
    const requestBody = {
      email: localStorage.getItem('user_email') || 'current_user',
      startDate: this.reportForm.get('startDate')?.value,
      endDate: this.reportForm.get('endDate')?.value,
      reportTitle: title, // Added to match ReportRequest
      includeExpenses: this.reportForm.get('includeExpenses')?.value,
      includeIncomes: this.reportForm.get('includeIncomes')?.value,
      includeBudgets: this.reportForm.get('includeBudgets')?.value
    };
  
    console.log('Exporting to Excel with request:', { endpoint, requestBody });
  
    this.http.post(endpoint, requestBody, {
      responseType: 'blob',
      withCredentials: true
    }).subscribe({
      next: (response: Blob) => {
        console.log('Excel response received:', response);
        if (response.size === 0) {
          this.errorMessage = 'Received an empty Excel file from the server';
          this.isLoading = false;
          return;
        }
  
        const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
        saveAs(blob, `${title.replace(/\s+/g, '_')}_${this.formatDateForFilename(new Date())}.xlsx`);
        this.showSuccess('Excel report downloaded successfully');
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error exporting to Excel:', error);
        if (error.status === 0) {
          this.errorMessage = 'Could not connect to the server. Check if the backend is running at ' + BASE_URL;
        } else if (error.status === 405) {
          this.errorMessage = 'Server does not support POST for this endpoint. Verify endpoint configuration';
        } else if (error.status === 404) {
          this.errorMessage = 'Excel export endpoint not found. Verify the URL: ' + endpoint;
        } else if (error.status === 401 || error.status === 403) {
          this.errorMessage = 'Authentication failed. Check credentials or JWT token';
        } else {
          this.errorMessage = `Failed to generate Excel report: ${error.message || 'Unknown error'}`;
        }
        this.isLoading = false;
      }
    });
  }

  togglePreview(): void {
    this.showPreview = !this.showPreview;
  }

  selectTemplate(templateId: string): void {
    this.selectedTemplate = templateId;

    switch (templateId) {
      case 'expense-summary':
        this.reportForm.patchValue({
          reportTitle: 'Expense Summary Report',
          includeExpenses: true,
          includeIncomes: false,
          includeBudgets: false,
          includeInsights: true,
          includeCharts: true
        });
        break;
      case 'income-summary':
        this.reportForm.patchValue({
          reportTitle: 'Income Summary Report',
          includeExpenses: false,
          includeIncomes: true,
          includeBudgets: false,
          includeInsights: true,
          includeCharts: true
        });
        break;
      case 'budget-analysis':
        this.reportForm.patchValue({
          reportTitle: 'Budget Analysis Report',
          includeExpenses: true,
          includeIncomes: false,
          includeBudgets: true,
          includeInsights: true,
          includeCharts: true
        });
        break;
      case 'savings-report':
        this.reportForm.patchValue({
          reportTitle: 'Savings Report',
          includeExpenses: true,
          includeIncomes: true,
          includeBudgets: false,
          includeInsights: true,
          includeCharts: true
        });
        break;
      case 'comprehensive':
        this.reportForm.patchValue({
          reportTitle: 'Comprehensive Financial Report',
          includeExpenses: true,
          includeIncomes: true,
          includeBudgets: true,
          includeInsights: true,
          includeCharts: true
        });
        break;
    }
  }

  formatDateForFilename(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}${month}${day}`;
  }

  showSuccess(message: string): void {
    this.successMessage = message;
    this.errorMessage = '';
    setTimeout(() => this.successMessage = '', 3000);
  }
}