import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from './sidebar/sidebar.component';
import { ExpenseService, ExpenseCategorySummary, BudgetStatus } from '../../service/expense.service';
import { IncomeService, Income as IncomeModel } from '../../service/income.service';
import { UserService } from '../../service/user.service';
import { NgxChartsModule, Color, ScaleType, LegendPosition } from '@swimlane/ngx-charts';
import { MatIconModule } from '@angular/material/icon';
import { curveCatmullRom } from 'd3-shape';
import { JwtService } from './../../service/jwt.service';
import { BudgetService, BudgetItem } from '../../service/budget.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { trigger, state, style, animate, transition } from '@angular/animations';
// Interfaces
interface Transaction {
  description: string;
  amount: number;
  date: Date;
  type: 'income' | 'expense';
}

interface Expense {
  id?: number;
  amount: number;
  category: string;
  date: Date | string;
  note?: string;
  tags?: string[];
  isRecurring?: boolean;
  recurringFrequency?: string;
}

interface Income {
  id?: number;
  source: string;
  amount: number;
  date: Date | string;
}

interface UserDetails {
  name?: string;
  email?: string;
  profilePhoto?: string;
}

interface ExpenseTrend {
  category: string;
  month?: string;
  date: string | Date;
  amount: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  imports: [

  CommonModule,
    CurrencyPipe,
    DatePipe,
    SidebarComponent,
    NgxChartsModule,
    MatIconModule,
    FormsModule,
  ], animations: [
    trigger('animationState', [
      state('start', style({ strokeDashoffset: '0' })),
      state('end', style({ strokeDashoffset: '100' })),
      transition('start => end', animate('1s')),
    ]),
  ],
})

export class DashboardComponent implements OnInit {
  // User profile
  userName: string = '';
  userEmail: string = '';
  profilePhotoUrl: string | null = null;

  // Financial stats
  totalBalance: number = 0;
  monthlyIncome: number = 0;
  monthlyExpenses: number = 0;
  savingsGoal: number = 0;

  // Transaction data
  recentTransactions: Transaction[] = [];
  expenseSummary: ExpenseCategorySummary[] = [];
  expenseTrends: ExpenseTrend[] = [];

  // Charts data
  pieChartData: { name: string; value: number }[] = [];
  lineChartData: { name: string; series: { name: string; value: number }[] }[] = [];

  // Chart configurations
  colorScheme: Color = {
    name: 'custom',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#FFD700', '#FF6B6B', '#4CAF50', '#2196F3', '#9C27B0', '#FF9800', '#795548'],
  };
  curve = curveCatmullRom;
  legendPosition = LegendPosition.Right;

  // UI state
  isSidebarOpen: boolean = true;
  isLoading: boolean = true;
  loadingError: string | null = null;

  // Budget setup
  budgetCategories: string[] = [];
  categoryBudgets: BudgetItem[] = [];
  newBudgetCategory: string = '';
  newBudgetAmount: number = 0;
  budgetStatus: BudgetStatus[] = [];
  showBudgetModal: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private expenseService: ExpenseService,
    private incomeService: IncomeService,
    private userService: UserService,
    private budgetService: BudgetService,
    private jwtService: JwtService
  ) {}

  ngOnInit(): void {

    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      if (token) {
        console.log('Found token in URL parameters, saving to localStorage');
        localStorage.setItem('jwt', token);


        this.router.navigate(['/dashboard']);
      }
    });

    this.checkAndSyncAuth();
    this.loadInitialData();
  }

  private checkAndSyncAuth(): void {
    this.jwtService.checkAuth().subscribe(isAuthenticated => {
      if (!isAuthenticated) {
        console.warn('Authentication check failed in dashboard. Trying to sync token state...');
        this.jwtService.syncTokenState().subscribe(synced => {
          if (!synced) {
            console.error('Failed to sync token state. Redirecting to login.');
            this.router.navigate(['/login']);
          } else {
            console.log('Token state synced successfully. Reloading dashboard data.');
            this.loadInitialData();
          }
        });
      }
    });
  }

  loadInitialData(): void {
    this.isLoading = true;
    this.loadingError = null;

    forkJoin({
      categories: this.expenseService.getExpenseCategories().pipe(catchError(() => of([]))),
      budgetStatus: this.expenseService.getBudgetStatus().pipe(catchError(() => of([]))),
      budgetItems: this.budgetService.getBudgetItems().pipe(catchError(() => of([]))),
      savingsGoal: this.budgetService.getSavingsGoal().pipe(catchError(() => of({ amount: 0 }))),
      expenses: this.expenseService.getExpenses().pipe(catchError(() => of([]))),
      incomes: this.incomeService.getIncomes().pipe(catchError(() => of([]))),
      trends: this.expenseService.getExpenseTrends().pipe(catchError(() => of([]))),
      summary: this.expenseService.getExpenseSummary().pipe(catchError(() => of([]))),
      user: this.userService.getUserDetails().pipe(catchError(() => of({ name: 'User', email: '', profilePhoto: null }))),
    }).subscribe({
      next: (results) => {
        this.budgetCategories = results.categories;
        this.budgetStatus = results.budgetStatus;
        this.categoryBudgets = results.budgetItems;
        this.savingsGoal = results.savingsGoal.amount;

        this.userName = results.user.name || 'User';
        this.userEmail = results.user.email || '';
        this.profilePhotoUrl = results.user.profilePhoto ? 'data:image/jpeg;base64,' + results.user.profilePhoto : null;

        // Load expenses
        const now = new Date();
        const currentMonth = now.getMonth();
        const currentYear = now.getFullYear();

        const currentMonthExpenses = results.expenses.filter((expense: Expense) => {
          const expenseDate = new Date(expense.date);
          return expenseDate.getMonth() === currentMonth && expenseDate.getFullYear() === currentYear;
        });

        this.monthlyExpenses = currentMonthExpenses.reduce((sum: number, expense: Expense) => sum + (expense.amount || 0), 0);

        const recentExpenses = results.expenses
          .sort((a: Expense, b: Expense) => new Date(b.date).getTime() - new Date(a.date).getTime())
          .slice(0, 5)
          .map((expense: Expense) => ({
            description: expense.category + (expense.note ? ` - ${expense.note}` : ''),
            amount: -expense.amount,
            date: new Date(expense.date),
            type: 'expense' as const,
          }));
        this.recentTransactions = [...recentExpenses];

        // Load incomes
        const currentMonthIncomes = results.incomes.filter((income: Income) => {
          const incomeDate = new Date(income.date);
          return incomeDate.getMonth() === currentMonth && incomeDate.getFullYear() === currentYear;
        });
        this.monthlyIncome = currentMonthIncomes.reduce((sum: number, income: Income) => sum + (income.amount || 0), 0);
        this.totalBalance = this.monthlyIncome - this.monthlyExpenses;
        const recentIncomes = results.incomes
          .sort((a: Income, b: Income) => new Date(b.date).getTime() - new Date(a.date).getTime())
          .slice(0, 5)
          .map((income: Income) => ({
            description: income.source || 'Income',
            amount: income.amount || 0,
            date: new Date(income.date),
            type: 'income' as const,
          }));
        this.recentTransactions = [...this.recentTransactions, ...recentIncomes]
          .sort((a, b) => b.date.getTime() - a.date.getTime())
          .slice(0, 5);

        // Load expense trends for line chart
        const categories = [...new Set(results.trends.map((trend) => trend.category))];
        this.lineChartData = categories.map((category) => ({
          name: category,
          series: results.trends
            .filter((trend) => trend.category === category)
            .map((trend) => ({
              name:
                trend.month ||
                ('date' in trend && trend.date && (typeof trend.date === 'string' || trend.date instanceof Date)
                  ? new Date(trend.date).toLocaleString('default', { month: 'short', year: 'numeric' })
                  : 'Unknown'),
              value: trend.amount || 0,
            })),
        }));

        // Filter expenses for the current month and calculate summary
        // This replaces the API's summary data with our own calculation based on current month only
        this.expenseSummary = this.calculateExpenseSummary(currentMonthExpenses);

        this.pieChartData = this.expenseSummary.map((item) => ({
          name: item.category,
          value: item.totalAmount,
        }));

        console.log('Current Month Expenses:', currentMonthExpenses);
        console.log('Processed Current Month Expense Summary:', this.expenseSummary);
        console.log('Pie Chart Data:', this.pieChartData);

        this.calculateSpendingTotals();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading dashboard data:', error);
        this.loadingError = 'Failed to load dashboard data. Please try again.';
        this.isLoading = false;
      },
    });
  }

  private calculateExpenseSummary(expenses: Expense[]): ExpenseCategorySummary[] {
    const summary: { [category: string]: number } = {};

    expenses.forEach(expense => {
      if (!summary[expense.category]) {
        summary[expense.category] = 0;
      }
      summary[expense.category] += expense.amount;
    });

    return Object.keys(summary).map(category => ({
      category,
      totalAmount: summary[category]
    }));
  }

  labelFormatter(value: number): string {
    const dataItem = this.pieChartData.find((item) => Math.abs(item.value - value) < 1); // Increased tolerance
    if (dataItem && Number.isFinite(dataItem.value) && dataItem.value > 0) {
      return `${dataItem.name}: ${this.currencyFormatter(dataItem.value)}`;
    }
    return ''; // Avoid displaying ₹0 for invalid/no-match cases
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  navigateToGoals(): void {
    this.router.navigate(['/goals']);
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  getCategoryIcon(category: string): string {
    return this.expenseService.getCategoryIcon(category) || 'category';
  }

  getCategoryColor(category: string): string {
    return this.expenseService.getCategoryColor(category) || '#FFD700';
  }

  currencyFormatter(value: number): string {
    if (value === undefined || value === null || isNaN(value)) {
      return '₹0';
    }
    try {
      return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(value);
    } catch (error) {
      console.error('Error formatting currency value:', value, error);
      return '₹0';
    }
  }

  formatAxisLabel(value: any): string {
    return typeof value === 'number' ? `₹${value.toLocaleString('en-IN')}` : value;
  }

  openBudgetModal(): void {
    this.showBudgetModal = true;
  }

  closeBudgetModal(): void {
    this.showBudgetModal = false;
  }

  addBudgetItem(): void {
    if (this.newBudgetCategory && this.newBudgetAmount > 0) {
      this.isLoading = true;

      const budgetItem: BudgetItem = {
        category: this.newBudgetCategory,
        amount: this.newBudgetAmount,
      };

      this.budgetService.saveOrUpdateBudgetItem(budgetItem).subscribe({
        next: () => {
          const existingIndex = this.categoryBudgets.findIndex((item) => item.category === budgetItem.category);
          if (existingIndex >= 0) {
            this.categoryBudgets[existingIndex].amount = budgetItem.amount;
          } else {
            this.categoryBudgets.push(budgetItem);
          }
          this.newBudgetCategory = '';
          this.newBudgetAmount = 0;
          this.isLoading = false;
          this.successMessage = 'Budget item saved successfully!';
          this.refreshBudgetStatus();
          setTimeout(() => (this.successMessage = ''), 3000);
        },
        error: (error) => {
          console.error('Error saving budget item:', error);
          this.isLoading = false;
          this.errorMessage = 'Failed to save budget item. Please try again.';
          setTimeout(() => (this.errorMessage = ''), 3000);
        },
      });
    }
  }

  refreshBudgetStatus(): void {
    this.expenseService.getBudgetStatus().subscribe({
      next: (budgetStatus) => {
        this.budgetStatus = budgetStatus;
        this.calculateSpendingTotals();
      },
      error: (error) => console.error('Error refreshing budget status:', error),
    });
  }

  removeBudgetItem(index: number): void {
    const budgetItem = this.categoryBudgets[index];
    if (!budgetItem) return;

    this.isLoading = true;

    this.budgetService.deleteBudgetItem(budgetItem.category).subscribe({
      next: () => {
        this.categoryBudgets.splice(index, 1);
        this.isLoading = false;
        this.successMessage = 'Budget item deleted successfully!';
        this.refreshBudgetStatus();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (error) => {
        console.error('Error deleting budget item:', error);
        this.isLoading = false;
        this.errorMessage = 'Failed to delete budget item. Please try again.';
        setTimeout(() => (this.errorMessage = ''), 3000);
      },
    });
  }

  saveSavingsGoal(): void {
    this.isLoading = true;

    this.budgetService.setSavingsGoal(this.savingsGoal).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Savings goal saved successfully!';
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (error) => {
        console.error('Error saving savings goal:', error);
        this.isLoading = false;
        this.errorMessage = 'Failed to save savings goal. Please try again.';
        setTimeout(() => (this.errorMessage = ''), 3000);
      },
    });
  }

  calculateSpendingTotals(): void {
    this.monthlyExpenses = this.budgetStatus.reduce((total, item) => total + item.spentAmount, 0);
  }

  getSpentPercentage(category: string): number {
    const statusItem = this.budgetStatus.find((item) => item.category === category);
    return statusItem ? Math.min(100, Math.round(statusItem.percentUsed)) : 0;
  }

  getAmountSpent(category: string): number {
    const statusItem = this.budgetStatus.find((item) => item.category === category);
    return statusItem ? statusItem.spentAmount : 0;
  }

  getRemainingAmount(category: string): number {
    const statusItem = this.budgetStatus.find((item) => item.category === category);
    return statusItem ? statusItem.remainingAmount : 0;
  }

  getProgressColor(category: string): string {
    const percentage = this.getSpentPercentage(category);
    if (percentage >= 90) return 'bg-red-500';
    else if (percentage >= 70) return 'bg-orange-500';
    else return 'bg-green-500';
  }

  getTotalBudgeted(): number {
    return this.categoryBudgets.reduce((sum, item) => sum + item.amount, 0);
  }

  getRemainingBudget(): number {
    return this.monthlyIncome - this.getTotalBudgeted() - this.savingsGoal;
  }
}
