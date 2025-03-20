import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { ExpenseService } from '../../service/expense.service';
import { IncomeService } from '../../service/income.service';
import { FinancialGoalService } from '../../service/financial-goal.service';
import { BudgetService } from '../../service/budget.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
interface Transaction {
  id?: number;
  type: 'income' | 'expense';
  amount: number;
  category: string;
  date: Date | string;
  description?: string;
  tags?: string[];
}

@Component({
  selector: 'app-transaction',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    SidebarComponent,
    DatePipe,
    FormsModule,
    RouterModule,
    NgxChartsModule
  ],
  templateUrl: './transaction.component.html',
  styleUrls: ['./transaction.component.css']
})
export class TransactionComponent implements OnInit {
  // UI state
  isSidebarOpen: boolean = true;
  isLoading: boolean = false;
  activeTab: string = 'all'; // 'all', 'income', 'expense', 'recent', 'insights'
  successMessage: string = '';
  errorMessage: string = '';
  showDeleteConfirmModal: boolean = false;
  showTransactionModal: boolean = false;
  selectedTransaction: Transaction | null = null;
  isSubmitting: boolean = false;
  searchQuery: string = '';
  filterCategory: string = '';
  dateRange: { start: Date | null, end: Date | null } = { start: null, end: null };
  selectedChartType: string = 'income';
  // Form
  transactionForm!: FormGroup;
  
  // Data
  transactions: Transaction[] = [];
  incomeCategories: string[] = [];
  expenseCategories: string[] = [];
  filteredTransactions: Transaction[] = [];
  goals: any[] = [];
  budgets: any[] = [];
  
  // Charts data
  cashFlowData: any[] = [];
  categoryDistributionData: any[] = [];
  dailyTrendsData: any[] = [];
  
  // Chart options
  colorScheme = {
    domain: ['#4CAF50', '#F44336', '#2196F3', '#FF9800', '#9C27B0', '#607D8B', '#E91E63', '#FFEB3B']
  };
  
  // Formatters
  currencyFormatter = (val: number) => `₹${Math.round(val).toLocaleString()}`;
  dateFormatter = (date: Date) => new DatePipe('en-US').transform(date, 'MMM d') || '';
  
  constructor(
    private fb: FormBuilder,
    private expenseService: ExpenseService,
    private incomeService: IncomeService,
    private financialGoalService: FinancialGoalService,
    private budgetService: BudgetService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadData();
  }

  initForm(): void {
    this.transactionForm = this.fb.group({
      id: [null],
      type: ['expense', Validators.required],
      amount: [null, [Validators.required, Validators.min(1)]],
      category: ['', Validators.required],
      date: [new Date().toISOString().slice(0, 10), Validators.required],
      description: [''],
      tags: [''],
      isRecurring: [false],
      recurringFrequency: [''],
      allocateToGoal: [false],
      goalId: [null]
    });
    
    // Listen for changes to transaction type
    this.transactionForm.get('type')?.valueChanges.subscribe(type => {
      this.updateCategoryValidators(type);
    });
    
    // Listen for changes to isRecurring
    this.transactionForm.get('isRecurring')?.valueChanges.subscribe(isRecurring => {
      const recurringFrequencyControl = this.transactionForm.get('recurringFrequency');
      if (isRecurring) {
        recurringFrequencyControl?.setValidators(Validators.required);
      } else {
        recurringFrequencyControl?.clearValidators();
      }
      recurringFrequencyControl?.updateValueAndValidity();
    });
    
    // Listen for changes to allocateToGoal
    this.transactionForm.get('allocateToGoal')?.valueChanges.subscribe(allocateToGoal => {
      const goalIdControl = this.transactionForm.get('goalId');
      if (allocateToGoal) {
        goalIdControl?.setValidators(Validators.required);
      } else {
        goalIdControl?.clearValidators();
      }
      goalIdControl?.updateValueAndValidity();
    });
  }
  
  updateCategoryValidators(type: string): void {
    const categoryControl = this.transactionForm.get('category');
    if (categoryControl) {
      categoryControl.setValue('');
    }
  }
  getExpenseTransactionsCount(): number {
    return this.filteredTransactions.filter(transaction => transaction.type === 'expense').length;
  }
  getIncomeTransactionsCount(): number {
    return this.transactions.filter(transaction => transaction.type === 'income').length;
  }
  loadData(): void {
    this.isLoading = true;
    
    // Load all necessary data in parallel
    forkJoin({
      expenses: this.expenseService.getExpenses().pipe(catchError(err => {
        console.error('Error loading expenses:', err);
        return of([]);
      })),
      incomes: this.incomeService.getIncomes().pipe(catchError(err => {
        console.error('Error loading incomes:', err);
        return of([]);
      })),
      expenseCategories: this.expenseService.getExpenseCategories().pipe(catchError(err => {
        console.error('Error loading expense categories:', err);
        return of([]);
      })),
      incomeCategories: this.incomeService.getIncomeSources().pipe(catchError(err => {
        console.error('Error loading income categories:', err);
        return of([]);
      })),
      goals: this.financialGoalService.getActiveGoals().pipe(catchError(err => {
        console.error('Error loading goals:', err);
        return of([]);
      })),
      budgets: this.budgetService.getBudgetItems().pipe(catchError(err => {
        console.error('Error loading budgets:', err);
        return of([]);
      }))
    }).pipe(
      finalize(() => {
        this.isLoading = false;
      })
    ).subscribe({
      next: (data) => {
        // Store the category data
        this.expenseCategories = data.expenseCategories;
        this.incomeCategories = data.incomeCategories;
        this.goals = data.goals;
        this.budgets = data.budgets;
        
        // Process expenses and incomes into unified transaction list
        const expenses = data.expenses.map(expense => ({
          id: expense.id,
          type: 'expense' as const,
          amount: expense.amount,
          category: expense.category,
          date: new Date(expense.date),
          description: expense.note,
          tags: expense.tags || []
        }));
        
        const incomes = data.incomes.map(income => ({
          id: income.id,
          type: 'income' as const,
          amount: income.amount,
          category: income.source,
          date: new Date(income.date),
          description: income.description,
          tags: income.tags || []
        }));
        
        // Combine all transactions and sort by date (newest first)
        this.transactions = [...expenses, ...incomes].sort((a, b) => {
          return new Date(b.date).getTime() - new Date(a.date).getTime();
        });
        
        this.filteredTransactions = [...this.transactions];
        
        // Generate chart data
        this.generateChartData();
      },
      error: (error) => {
        console.error('Error loading data:', error);
        this.showError('Failed to load transaction data. Please try again.');
      }
    });
  }

  generateChartData(): void {
    this.generateCashFlowData();
    this.generateCategoryDistributionData();
    this.generateDailyTrendsData();
  }

  generateCashFlowData(): void {
    // Get last 6 months data
    const now = new Date();
    const months: { [key: string]: { income: number, expense: number } } = {};
    
    // Initialize the last 6 months
    for (let i = 5; i >= 0; i--) {
      const month = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const monthKey = month.toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
      months[monthKey] = { income: 0, expense: 0 };
    }
    
    // Populate the data
    this.transactions.forEach(transaction => {
      const date = new Date(transaction.date);
      // Only consider last 6 months
      if (date.getTime() >= new Date(now.getFullYear(), now.getMonth() - 5, 1).getTime() && 
          date.getTime() <= now.getTime()) {
        const monthKey = date.toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
        if (transaction.type === 'income') {
          months[monthKey].income += transaction.amount;
        } else {
          months[monthKey].expense += transaction.amount;
        }
      }
    });
    
    // Convert to chart data format
    this.cashFlowData = Object.entries(months).map(([name, data]) => ({
      name,
      series: [
        { name: 'Income', value: data.income },
        { name: 'Expense', value: data.expense }
      ]
    }));
  }

  generateCategoryDistributionData(): void {
    const expenseCategories: { [key: string]: number } = {};
    const incomeCategories: { [key: string]: number } = {};
    
    // Current month only
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    
    this.transactions.forEach(transaction => {
      const date = new Date(transaction.date);
      if (date >= startOfMonth && date <= now) {
        if (transaction.type === 'expense') {
          if (!expenseCategories[transaction.category]) {
            expenseCategories[transaction.category] = 0;
          }
          expenseCategories[transaction.category] += transaction.amount;
        } else {
          if (!incomeCategories[transaction.category]) {
            incomeCategories[transaction.category] = 0;
          }
          incomeCategories[transaction.category] += transaction.amount;
        }
      }
    });
    
    // Convert to chart data format based on active tab
    if (this.activeTab === 'expense' || this.activeTab === 'all' || this.activeTab === 'insights') {
      this.categoryDistributionData = Object.entries(expenseCategories)
        .map(([name, value]) => ({ name, value }))
        .sort((a, b) => b.value - a.value)
        .slice(0, 7); // Top 7 categories only
    } else {
      this.categoryDistributionData = Object.entries(incomeCategories)
        .map(([name, value]) => ({ name, value }))
        .sort((a, b) => b.value - a.value)
        .slice(0, 7); // Top 7 categories only
    }
  }

  generateDailyTrendsData(): void {
    const last30Days: { [key: string]: { income: number, expense: number } } = {};
    const now = new Date();
    
    // Initialize last 30 days
    for (let i = 29; i >= 0; i--) {
      const day = new Date(now);
      day.setDate(now.getDate() - i);
      const dayKey = day.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
      last30Days[dayKey] = { income: 0, expense: 0 };
    }
    
    // Populate the data
    this.transactions.forEach(transaction => {
      const date = new Date(transaction.date);
      // Only consider last 30 days
      if (date.getTime() >= new Date(now.getFullYear(), now.getMonth(), now.getDate() - 29).getTime() && 
          date.getTime() <= now.getTime()) {
        const dayKey = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
        if (transaction.type === 'income') {
          last30Days[dayKey].income += transaction.amount;
        } else {
          last30Days[dayKey].expense += transaction.amount;
        }
      }
    });
    
    // Convert to chart data format
    this.dailyTrendsData = [
      {
        name: 'Income',
        series: Object.entries(last30Days).map(([name, data]) => ({
          name,
          value: data.income
        }))
      },
      {
        name: 'Expense',
        series: Object.entries(last30Days).map(([name, data]) => ({
          name,
          value: data.expense
        }))
      }
    ];
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }
  
  switchTab(tab: string): void {
    this.activeTab = tab;
    
    // Apply tab-specific filters
    switch (tab) {
      case 'all':
        this.resetFilters();
        break;
      case 'income':
        this.filteredTransactions = this.transactions.filter(t => t.type === 'income');
        this.generateCategoryDistributionData();
        break;
      case 'expense':
        this.filteredTransactions = this.transactions.filter(t => t.type === 'expense');
        this.generateCategoryDistributionData();
        break;
      case 'recent':
        // Show last 7 days transactions
        const last7Days = new Date();
        last7Days.setDate(last7Days.getDate() - 7);
        this.filteredTransactions = this.transactions.filter(t => 
          new Date(t.date) >= last7Days
        );
        break;
      case 'insights':
        this.filteredTransactions = this.transactions;
        break;
    }
  }

  applyFilters(): void {
    let filtered = [...this.transactions];
    
    // Apply active tab filter
    if (this.activeTab === 'income') {
      filtered = filtered.filter(t => t.type === 'income');
    } else if (this.activeTab === 'expense') {
      filtered = filtered.filter(t => t.type === 'expense');
    }
    
    // Apply search query
    if (this.searchQuery.trim() !== '') {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(t => 
        t.category.toLowerCase().includes(query) ||
        (t.description && t.description.toLowerCase().includes(query)) ||
        (t.tags && t.tags.some(tag => tag.toLowerCase().includes(query)))
      );
    }
    
    // Apply category filter
    if (this.filterCategory !== '') {
      filtered = filtered.filter(t => t.category === this.filterCategory);
    }
    
    // Apply date range filter
    if (this.dateRange.start) {
      filtered = filtered.filter(t => new Date(t.date) >= (this.dateRange.start as Date));
    }
    if (this.dateRange.end) {
      const endDate = new Date(this.dateRange.end as Date);
      endDate.setHours(23, 59, 59, 999); // End of day
      filtered = filtered.filter(t => new Date(t.date) <= endDate);
    }
    
    this.filteredTransactions = filtered;
  }

  resetFilters(): void {
    this.searchQuery = '';
    this.filterCategory = '';
    this.dateRange = { start: null, end: null };
    this.filteredTransactions = [...this.transactions];
    
    // Apply active tab filter
    this.switchTab(this.activeTab);
  }

  openTransactionModal(type: 'income' | 'expense'): void {
    this.transactionForm.reset({
      id: null,
      type: type,
      amount: null,
      category: '',
      date: new Date().toISOString().slice(0, 10),
      description: '',
      tags: '',
      isRecurring: false,
      recurringFrequency: '',
      allocateToGoal: false,
      goalId: null
    });
    
    this.selectedTransaction = null;
    this.showTransactionModal = true;
  }

  editTransaction(transaction: Transaction): void {
    this.selectedTransaction = transaction;
    
    const tags = Array.isArray(transaction.tags) ? transaction.tags.join(', ') : '';
    const date = transaction.date instanceof Date 
      ? transaction.date.toISOString().slice(0, 10) 
      : new Date(transaction.date).toISOString().slice(0, 10);
    
    this.transactionForm.patchValue({
      id: transaction.id,
      type: transaction.type,
      amount: transaction.amount,
      category: transaction.category,
      date: date,
      description: transaction.description || '',
      tags: tags,
      // Additional properties would be patched here based on the transaction type
      isRecurring: false, // Default value, would need to be set based on actual data
      recurringFrequency: '',
      allocateToGoal: false,
      goalId: null
    });
    
    this.showTransactionModal = true;
  }

  saveTransaction(): void {
    if (this.transactionForm.invalid) {
      return;
    }
    
    this.isSubmitting = true;
    const formValue = this.transactionForm.value;
    
    // Process tags
    const tags = formValue.tags ? formValue.tags.split(',').map((tag: string) => tag.trim()) : [];
    
    if (formValue.type === 'income') {
      const incomeData = {
        id: formValue.id,
        amount: formValue.amount,
        source: formValue.category,
        description: formValue.description,
        isRecurring: formValue.isRecurring,
        recurringFrequency: formValue.isRecurring ? formValue.recurringFrequency : '',
        tags: tags
      };
      
      if (formValue.id) {
        // Update existing income
        this.incomeService.updateIncome(formValue.id, incomeData).subscribe({
          next: () => this.handleTransactionSuccess('Income updated successfully'),
          error: (error) => this.handleTransactionError(error, 'Failed to update income')
        });
      } else {
        // Add new income
        this.incomeService.addIncome(incomeData).subscribe({
          next: () => this.handleTransactionSuccess('Income added successfully'),
          error: (error) => this.handleTransactionError(error, 'Failed to add income')
        });
      }
    } else {
      // Process expense
      const expenseData = {
        id: formValue.id,
        amount: formValue.amount,
        category: formValue.category,
        note: formValue.description,
        isRecurring: formValue.isRecurring,
        recurringFrequency: formValue.isRecurring ? formValue.recurringFrequency : '',
        tags: tags
      };
      
      if (formValue.id) {
        // Update existing expense
        this.expenseService.updateExpense(formValue.id, expenseData).subscribe({
          next: () => this.handleTransactionSuccess('Expense updated successfully'),
          error: (error) => this.handleTransactionError(error, 'Failed to update expense')
        });
      } else {
        // Add new expense
        this.expenseService.addExpense(expenseData).subscribe({
          next: () => this.handleTransactionSuccess('Expense added successfully'),
          error: (error) => this.handleTransactionError(error, 'Failed to add expense')
        });
      }
    }
    
    // Handle goal allocation if needed
    if (formValue.allocateToGoal && formValue.goalId) {
      this.updateGoalProgress(formValue.goalId, formValue.amount, formValue.type);
    }
  }

  updateGoalProgress(goalId: number, amount: number, type: string): void {
    const goal = this.goals.find(g => g.id === goalId);
    if (!goal) return;
    
    let newAmount = goal.currentAmount;
    if (type === 'income') {
      newAmount += amount;
    } else {
      newAmount -= amount;
    }
    
    // Ensure we don't go below 0
    if (newAmount < 0) newAmount = 0;
    
    this.financialGoalService.updateGoalProgress(goalId, newAmount).subscribe({
      next: () => {
        console.log('Goal progress updated successfully');
      },
      error: (error) => {
        console.error('Failed to update goal progress:', error);
        this.showError('Goal allocation recorded, but failed to update goal progress.');
      }
    });
  }

  handleTransactionSuccess(message: string): void {
    this.showTransactionModal = false;
    this.isSubmitting = false;
    this.showSuccess(message);
    this.loadData();
  }

  handleTransactionError(error: HttpErrorResponse, fallbackMessage: string): void {
    this.isSubmitting = false;
    console.error('Transaction error:', error);
    
    let errorMessage = fallbackMessage;
    if (error.status === 401) {
      errorMessage = 'Your session has expired. Please log in again.';
      this.router.navigate(['/login']);
    } else if (error.status === 403) {
      errorMessage = 'You don\'t have permission to perform this action.';
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    }
    
    this.showError(errorMessage);
  }

  confirmDeleteTransaction(transaction: Transaction): void {
    this.selectedTransaction = transaction;
    this.showDeleteConfirmModal = true;
  }

  deleteTransaction(): void {
    if (!this.selectedTransaction) return;
    
    this.isSubmitting = true;
    
    if (this.selectedTransaction.type === 'income') {
      this.incomeService.deleteIncome(this.selectedTransaction.id as number).subscribe({
        next: () => {
          this.showSuccess('Income deleted successfully');
          this.closeDeleteModal();
          this.loadData();
        },
        error: (error) => {
          this.isSubmitting = false;
          this.handleTransactionError(error, 'Failed to delete income');
        }
      });
    } else {
      this.expenseService.deleteExpense(this.selectedTransaction.id as number).subscribe({
        next: () => {
          this.showSuccess('Expense deleted successfully');
          this.closeDeleteModal();
          this.loadData();
        },
        error: (error) => {
          this.isSubmitting = false;
          this.handleTransactionError(error, 'Failed to delete expense');
        }
      });
    }
  }

  closeTransactionModal(): void {
    this.showTransactionModal = false;
    this.selectedTransaction = null;
  }

  closeDeleteModal(): void {
    this.showDeleteConfirmModal = false;
    this.selectedTransaction = null;
    this.isSubmitting = false;
  }

  showSuccess(message: string): void {
    this.successMessage = message;
    setTimeout(() => this.successMessage = '', 3000);
  }

  showError(message: string): void {
    this.errorMessage = message;
    setTimeout(() => this.errorMessage = '', 3000);
  }
  
  getCategoryIcon(category: string, type: string): string {
    if (type === 'income') {
      return this.incomeService.getSourceIcon(category);
    } else {
      return this.expenseService.getCategoryIcon(category);
    }
  }

  getCategoryColor(category: string, type: string): string {
    if (type === 'income') {
      return this.incomeService.getSourceColor(category);
    } else {
      return this.expenseService.getCategoryColor(category);
    }
  }
  
  getTransactionStatusClass(transaction: Transaction): string {
    return transaction.type === 'income' ? 'bg-green-500' : 'bg-red-500';
  }
  
  formatDate(date: Date | string): string {
    return new DatePipe('en-US').transform(date, 'MMM d, y') || '';
  }
  
  getTotalIncome(): number {
    return this.filteredTransactions
      .filter(t => t.type === 'income')
      .reduce((sum, t) => sum + t.amount, 0);
  }
  
  getTotalExpense(): number {
    return this.filteredTransactions
      .filter(t => t.type === 'expense')
      .reduce((sum, t) => sum + t.amount, 0);
  }
  
  getNetAmount(): number {
    return this.getTotalIncome() - this.getTotalExpense();
  }
  
  getSpendingInsights(): string[] {
    const insights: string[] = [];
    
    // Calculate some basic insights
    const totalIncome = this.transactions
      .filter(t => t.type === 'income')
      .reduce((sum, t) => sum + t.amount, 0);
      
    const totalExpense = this.transactions
      .filter(t => t.type === 'expense')
      .reduce((sum, t) => sum + t.amount, 0);
    
    // Savings rate
    const savingsRate = totalIncome > 0 ? (totalIncome - totalExpense) / totalIncome * 100 : 0;
    
    if (savingsRate >= 20) {
      insights.push(`Great job saving ${savingsRate.toFixed(1)}% of your income!`);
    } else if (savingsRate > 0) {
      insights.push(`You're saving ${savingsRate.toFixed(1)}% of your income. Try to reach 20%!`);
    } else if (savingsRate < 0) {
      insights.push(`You're spending more than you earn. Consider reducing expenses.`);
    }
    
    // Highest expense category
    const expenseByCategory: Record<string, number> = {};
    this.transactions.filter(t => t.type === 'expense').forEach(expense => {
      if (!expenseByCategory[expense.category]) {
        expenseByCategory[expense.category] = 0;
      }
      expenseByCategory[expense.category] += expense.amount;
    });
    
    const highestCategory = Object.entries(expenseByCategory)
      .sort((a, b) => b[1] - a[1])[0];
    
    if (highestCategory) {
      const percentOfExpense = (highestCategory[1] / totalExpense) * 100;
      insights.push(`Your highest spending category is ${highestCategory[0]} (${percentOfExpense.toFixed(1)}% of expenses).`);
    }
    
    // Month-over-month spending
    const currentMonth = new Date().getMonth();
    const lastMonth = currentMonth === 0 ? 11 : currentMonth - 1;
    const currentYear = new Date().getFullYear();
    const lastMonthYear = currentMonth === 0 ? currentYear - 1 : currentYear;
    
    const currentMonthExpenses = this.transactions
      .filter(t => t.type === 'expense' && new Date(t.date).getMonth() === currentMonth && new Date(t.date).getFullYear() === currentYear)
      .reduce((sum, t) => sum + t.amount, 0);
      
    const lastMonthExpenses = this.transactions
      .filter(t => t.type === 'expense' && new Date(t.date).getMonth() === lastMonth && new Date(t.date).getFullYear() === lastMonthYear)
      .reduce((sum, t) => sum + t.amount, 0);
    
    if (lastMonthExpenses > 0) {
      const changePercent = ((currentMonthExpenses - lastMonthExpenses) / lastMonthExpenses) * 100;
      if (changePercent > 10) {
        insights.push(`Your spending is up ${changePercent.toFixed(1)}% compared to last month.`);
      } else if (changePercent < -10) {
        insights.push(`You've reduced spending by ${Math.abs(changePercent).toFixed(1)}% from last month. Good job!`);
      } else {
        insights.push(`Your spending is stable compared to last month (${changePercent.toFixed(1)}% change).`);
      }
    }
    
    return insights;
  }
}