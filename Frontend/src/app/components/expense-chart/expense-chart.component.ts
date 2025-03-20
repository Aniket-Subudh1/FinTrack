import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { ExpenseService, ExpenseCategorySummary, BudgetStatus } from '../../service/expense.service';
import { IncomeService } from '../../service/income.service';
import { BudgetService } from '../../service/budget.service';
import { TransactionService, Transaction, TransactionInsight } from '../../service/transaction.service';
import { HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { Color, ScaleType } from '@swimlane/ngx-charts';
import { text } from 'd3';

interface ChartData {
  name: string;
  value: number;
}

interface MultiSeriesData {
  name: string;
  series: { name: string; value: number }[];
}

@Component({
  selector: 'app-expense-chart',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    NgxChartsModule,
    MatIconModule,
    HttpClientModule,
    SidebarComponent,
    DatePipe
  ],
  templateUrl: './expense-chart.component.html',
  styleUrls: ['./expense-chart.component.css']
})
export class ExpenseChartComponent implements OnInit {
  @ViewChild('chartContainer') chartContainer!: ElementRef;
 
    
  

  
  // UI state
  isSidebarOpen: boolean = true;
  isLoading: boolean = false;
  activeTab: string = 'overview';
  selectedTimeframe: string = 'month';
  showInsightDetails: boolean = false;
  selectedInsight: TransactionInsight | null = null;
  
  // Data
  transactions: Transaction[] = [];
  expenseSummary: ExpenseCategorySummary[] = [];
  incomeSummary: any[] = [];
  budgetStatus: BudgetStatus[] = [];
  insights: TransactionInsight[] = [];
  forecastData: any = {};
  
  // Charts data
  expenseByCategoryData: ChartData[] = [];
  incomeByCategoryData: ChartData[] = [];
  monthlyComparisonData: MultiSeriesData[] = [];
  dailySpendingData: MultiSeriesData[] = [];
  savingsRateData: MultiSeriesData[] = [];
  spendingTrendsData: MultiSeriesData[] = [];
  categoryComparisonData: any[] = [];
  recurringVsOneTimeData: ChartData[] = [];
  view: [number, number] = [500, 500];

  colorScheme = {
    domain: ['#5AA454', '#A10A28', '#C7B42C', '#AAAAAA', '#4285F4', '#DB4437', '#F4B400', '#0F9D58', '#9C27B0', '#3F51B5']
  };
  colorSchemeIncome = {
    name: 'incomeScheme',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#AED581', '#DCE775', '#FFF176', '#FFD54F', '#FFB74D', '#FF8A65', '#A1887F', '#90A4AE', '#78909C', '#546E7A']
  };
 
  gradient: boolean = true;
  showXAxis: boolean = true;
  showYAxis: boolean = true;
  showLegend: boolean = true;
  legendPosition: string = 'below';
  showXAxisLabel: boolean = true;
  showYAxisLabel: boolean = true;
  showLabels: boolean = true;
  explodeSlices: boolean = false;
  doughnut: boolean = false;
  
  // Filters & forms
  filterForm: FormGroup;
  forecastForm: FormGroup;
  
  // Calculations
  totalIncome: number = 0;
  totalExpenses: number = 0;
  netSavings: number = 0;
  savingsRate: number = 0;
  
  constructor(
    private fb: FormBuilder,
    private expenseService: ExpenseService,
    private incomeService: IncomeService,
    private budgetService: BudgetService,
    private transactionService: TransactionService,
    private router: Router
  ) {
    this.filterForm = this.fb.group({
      startDate: [this.getDefaultStartDate()],
      endDate: [this.formatDateForInput(new Date())],
      categories: [[]],
      expenseType: ['all'],
      minAmount: [''],
      maxAmount: ['']
    });
    
    this.forecastForm = this.fb.group({
      months: [3],
      savingsGoal: [''],
      includeRecurring: [true]
    });
  }

  ngOnInit(): void {
    this.loadAllData();
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    
    // Load data specific to tabs if needed
    if (tab === 'forecast') {
      this.generateForecast();
    } else if (tab === 'budget') {
      this.loadBudgetData();
    } else if (tab === 'trends') {
      this.loadTrendsData();
    }
  }

  changeTimeframe(timeframe: string): void {
    this.selectedTimeframe = timeframe;
    this.updateDateRange(timeframe);
    this.applyFilters();
  }

  loadAllData(): void {
    this.isLoading = true;
    
    // Load transactions first as they're used by other data
    this.transactionService.getTransactionHistory(this.selectedTimeframe as 'week' | 'month' | 'year' | 'all').subscribe(
      transactions => {
        this.transactions = transactions;
        this.calculateTotals();
        this.prepareChartData();
        this.generateInsights();
        
        // Load other data
        this.loadExpenseSummary();
        this.loadIncomeSummary();
        this.loadBudgetData();
        this.generateDailySpendingData();
        
        this.isLoading = false;
      },
      error => {
        console.error('Error loading transactions:', error);
        this.isLoading = false;
      }
    );
  }
  colorSchemeExpense: Color = {
    name: 'customScheme',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#FF5733', '#33FF57', '#3357FF']
  };
  loadExpenseSummary(): void {
    this.expenseService.getExpenseSummary().subscribe(
      summary => {
        this.expenseSummary = summary;
        this.prepareExpenseCategoryData();
      },
      error => console.error('Error loading expense summary:', error)
    );
  }

  loadIncomeSummary(): void {
    this.incomeService.getIncomeSummary().subscribe(
      summary => {
        this.incomeSummary = summary;
        this.prepareIncomeCategoryData();
      },
      error => console.error('Error loading income summary:', error)
    );
  }

  loadBudgetData(): void {
    this.expenseService.getBudgetStatus().subscribe(
      status => {
        this.budgetStatus = status;
        this.prepareBudgetComparisonData();
      },
      error => console.error('Error loading budget status:', error)
    );
  }

  loadTrendsData(): void {
    // Get expense trends over time
    this.expenseService.getExpenseTrends().subscribe(
      trends => {
        this.prepareSpendingTrendsData(trends);
      },
      error => console.error('Error loading expense trends:', error)
    );
  }

  calculateTotals(): void {
    // Calculate totals from transactions
    this.totalIncome = this.transactions
      .filter(t => t.type === 'income')
      .reduce((sum, t) => sum + t.amount, 0);
      
    this.totalExpenses = this.transactions
      .filter(t => t.type === 'expense')
      .reduce((sum, t) => sum + t.amount, 0);
      
    this.netSavings = this.totalIncome - this.totalExpenses;
    this.savingsRate = this.totalIncome > 0 ? (this.netSavings / this.totalIncome) * 100 : 0;
  }

  prepareChartData(): void {
    // Prepare chart data from transactions
    this.prepareExpenseCategoryData();
    this.prepareIncomeCategoryData();
    this.prepareMonthlyComparisonData();
    this.prepareRecurringVsOneTimeData();
    
    // Calculate savings rate trend
    this.prepareSavingsRateData();
  }

  prepareExpenseCategoryData(): void {
    if (!this.expenseSummary || !this.expenseSummary.length) return;
    
    this.expenseByCategoryData = this.expenseSummary
      .filter(item => item.totalAmount > 0)
      .map(item => ({
        name: item.category,
        value: item.totalAmount
      }))
      .sort((a, b) => b.value - a.value);
  }

  prepareIncomeCategoryData(): void {
    if (!this.incomeSummary || !this.incomeSummary.length) return;
    
    this.incomeByCategoryData = this.incomeSummary
      .map(item => ({
        name: item.source,
        value: item.totalAmount
      }))
      .sort((a, b) => b.value - a.value);
  }

  prepareMonthlyComparisonData(): void {
    // Generate monthly comparison data for income vs. expense
    this.monthlyComparisonData = this.transactionService.getMonthlyFinancialData(
      this.transactions, 
      this.selectedTimeframe === 'year' ? 12 : 6
    );
  }

  prepareRecurringVsOneTimeData(): void {
    // Calculate recurring vs. one-time expenses
    const recurringExpenses = this.transactions
      .filter(t => t.type === 'expense' && t.isRecurring)
      .reduce((sum, t) => sum + t.amount, 0);
      
    const oneTimeExpenses = this.transactions
      .filter(t => t.type === 'expense' && !t.isRecurring)
      .reduce((sum, t) => sum + t.amount, 0);
      
    this.recurringVsOneTimeData = [
      { name: 'Recurring', value: recurringExpenses },
      { name: 'One-time', value: oneTimeExpenses }
    ];
  }

  prepareSavingsRateData(): void {
    // Create monthly savings rate data
    const months = this.generateLastNMonths(6);
    const savingsData: MultiSeriesData[] = [];
    
    months.forEach(monthDate => {
      const monthKey = monthDate.toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
      const startOfMonth = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
      const endOfMonth = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0);
      
      // Filter transactions for this month
      const monthTransactions = this.transactions.filter(t => {
        const date = new Date(t.date);
        return date >= startOfMonth && date <= endOfMonth;
      });
      
      // Calculate totals
      const income = monthTransactions
        .filter(t => t.type === 'income')
        .reduce((sum, t) => sum + t.amount, 0);
        
      const expenses = monthTransactions
        .filter(t => t.type === 'expense')
        .reduce((sum, t) => sum + t.amount, 0);
        
      const savingsRate = income > 0 ? ((income - expenses) / income) * 100 : 0;
      
      savingsData.push({
        name: monthKey,
        series: [
          { name: 'Savings Rate', value: savingsRate }
        ]
      });
    });
    
    this.savingsRateData = savingsData;
  }

  prepareBudgetComparisonData(): void {
    if (!this.budgetStatus || !this.budgetStatus.length) return;
    
    // Prepare data for budget vs. actual spending chart
    this.categoryComparisonData = this.budgetStatus
      .filter(status => status.budgetAmount > 0) // Only include categories with a budget
      .map(status => ({
        name: status.category,
        series: [
          { name: 'Budget', value: status.budgetAmount },
          { name: 'Spent', value: status.spentAmount }
        ]
      }));
  }

  prepareSpendingTrendsData(trends: any[]): void {
    if (!trends || !trends.length) return;
    
    // Group by month and category
    const trendsByMonth: { [key: string]: { [category: string]: number } } = {};
    
    trends.forEach(trend => {
      if (!trendsByMonth[trend.month]) {
        trendsByMonth[trend.month] = {};
      }
      
      if (!trendsByMonth[trend.month][trend.category]) {
        trendsByMonth[trend.month][trend.category] = 0;
      }
      
      trendsByMonth[trend.month][trend.category] += trend.amount;
    });
    
    // Find top categories
    const allCategories = new Set<string>();
    Object.values(trendsByMonth).forEach(monthData => {
      Object.keys(monthData).forEach(category => allCategories.add(category));
    });
    
    // Limit to top 5 categories by total amount
    const topCategories = Array.from(allCategories)
      .map(category => {
        const total = Object.values(trendsByMonth).reduce((sum, month) => {
          return sum + (month[category] || 0);
        }, 0);
        return { category, total };
      })
      .sort((a, b) => b.total - a.total)
      .slice(0, 5)
      .map(item => item.category);
    
    // Prepare the chart data for top categories
    this.spendingTrendsData = Object.keys(trendsByMonth)
      .sort() // Sort months chronologically
      .map(month => {
        const series = topCategories.map(category => ({
          name: category,
          value: trendsByMonth[month][category] || 0
        }));
        
        return {
          name: month,
          series
        };
      });
  }

  generateDailySpendingData(): void {
    // Generate daily spending data for the current month
    const today = new Date();
    const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    const daysInMonth = endOfMonth.getDate();
    
    const dailyData: { [key: string]: number } = {};
    
    // Initialize all days of the month
    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(today.getFullYear(), today.getMonth(), day);
      const dayKey = date.toLocaleDateString('en-US', { day: '2-digit' });
      dailyData[dayKey] = 0;
    }
    
    // Sum up expenses for each day
    this.transactions
      .filter(t => {
        const date = new Date(t.date);
        return t.type === 'expense' && date >= startOfMonth && date <= endOfMonth;
      })
      .forEach(t => {
        const date = new Date(t.date);
        const dayKey = date.toLocaleDateString('en-US', { day: '2-digit' });
        dailyData[dayKey] += t.amount;
      });
    
    // Format for chart
    this.dailySpendingData = [{
      name: 'Daily Spending',
      series: Object.entries(dailyData).map(([day, amount]) => ({
        name: day,
        value: amount
      }))
    }];
  }

  generateInsights(): void {
    // Generate financial insights
    this.insights = this.transactionService.generateInsights(this.transactions);
  }

  generateForecast(): void {
    const months = this.forecastForm.get('months')?.value || 3;
    const savingsGoal = this.forecastForm.get('savingsGoal')?.value;
    const includeRecurring = this.forecastForm.get('includeRecurring')?.value;
    
    // Calculate average income and expenses from past data
    const pastMonths = this.generateLastNMonths(6);
    const monthlyStats = pastMonths.map(monthDate => {
      const startOfMonth = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
      const endOfMonth = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0);
      
      const monthTransactions = this.transactions.filter(t => {
        const date = new Date(t.date);
        return date >= startOfMonth && date <= endOfMonth;
      });
      
      const income = monthTransactions
        .filter(t => t.type === 'income')
        .reduce((sum, t) => sum + t.amount, 0);
        
      const expenses = monthTransactions
        .filter(t => t.type === 'expense')
        .reduce((sum, t) => sum + t.amount, 0);
        
      return { income, expenses };
    });
    
    // Calculate averages
    const avgIncome = monthlyStats.reduce((sum, month) => sum + month.income, 0) / monthlyStats.length || 0;
    const avgExpenses = monthlyStats.reduce((sum, month) => sum + month.expenses, 0) / monthlyStats.length || 0;
    
    // Calculate recurring expenses if needed
    let recurringExpenses = 0;
    if (includeRecurring) {
      recurringExpenses = this.transactions
        .filter(t => t.type === 'expense' && t.isRecurring)
        .reduce((sum, t) => sum + t.amount, 0);
    }
    
    // Generate forecast for next N months
    const forecastMonths = [];
    let cumulativeSavings = this.netSavings; // Start with current savings
    
    for (let i = 1; i <= months; i++) {
      const forecastDate = new Date();
      forecastDate.setMonth(forecastDate.getMonth() + i);
      const monthName = forecastDate.toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
      
      // Calculate projected income and expenses
      const projectedIncome = avgIncome;
      const projectedExpenses = includeRecurring ? recurringExpenses : avgExpenses;
      const monthlySavings = projectedIncome - projectedExpenses;
      cumulativeSavings += monthlySavings;
      
      forecastMonths.push({
        month: monthName,
        projectedIncome,
        projectedExpenses,
        monthlySavings,
        cumulativeSavings
      });
    }
    
    // Calculate time to reach savings goal if provided
    let timeToGoal = null;
    const forecastAvgIncome = this.forecastData.avgIncome || 0;
    const forecastAvgExpenses = this.forecastData.avgExpenses || 0;
    const monthlySavings = forecastAvgIncome - avgExpenses;

    if (savingsGoal && monthlySavings > 0) {
      const remainingToGoal = savingsGoal - this.netSavings;
      if (remainingToGoal > 0) {
        timeToGoal = Math.ceil(remainingToGoal / monthlySavings);
      }
    }
    
    this.forecastData = {
      months: forecastMonths,
      avgIncome,
      avgExpenses,
      timeToGoal
    };
  }

  applyFilters(): void {
    // Apply filters and reload data
    this.loadAllData();
  }

  onInsightClick(insight: TransactionInsight): void {
    this.selectedInsight = insight;
    this.showInsightDetails = true;
  }

  closeInsightDetails(): void {
    this.showInsightDetails = false;
    this.selectedInsight = null;
  }

  exportToPDF(): void {
    alert('Exporting to PDF...');
  }

  exportToCSV(): void {
    alert('Exporting to CSV...');
   
  }

  getDefaultStartDate(): string {
    const date = new Date();
    date.setMonth(date.getMonth() - 1);
    return this.formatDateForInput(date);
  }
  colorSchemeBudget = {
    name: 'budgetScheme',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#FF5733', '#33FF57', '#3357FF']
  };
  formatDate(date: string | null): string {
    if (!date) return 'N/A';
    const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
    return new Date(date).toLocaleDateString(undefined, options);
  }
  formatDateForInput(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  updateDateRange(timeframe: string): void {
    const today = new Date();
    let startDate = new Date();
    
    switch (timeframe) {
      case 'week':
        startDate.setDate(today.getDate() - 7);
        break;
      case 'month':
        startDate.setMonth(today.getMonth() - 1);
        break;
      case 'quarter':
        startDate.setMonth(today.getMonth() - 3);
        break;
      case 'year':
        startDate.setFullYear(today.getFullYear() - 1);
        break;
      case 'all':
        startDate = new Date(2000, 0, 1); // Far back in the past
        break;
    }
    
    this.filterForm.patchValue({
      startDate: this.formatDateForInput(startDate),
      endDate: this.formatDateForInput(today)
    });
  }

  generateLastNMonths(n: number): Date[] {
    const result = [];
    const today = new Date();
    
    for (let i = n - 1; i >= 0; i--) {
      const month = new Date(today.getFullYear(), today.getMonth() - i, 1);
      result.push(month);
    }
    
    return result;
  }

  formatCurrency(amount: number): string {
    return amount.toLocaleString('en-IN', { 
      style: 'currency', 
      currency: 'INR',
      maximumFractionDigits: 0
    });
  }

  formatPercentage(value: number): string {
    return `${value.toFixed(1)}%`;
  }

  getPercentageClass(value: number): string {
    if (value > 0) return 'text-green-500';
    if (value < 0) return 'text-red-500';
    return 'text-gray-500';
  }

  getCategoryIcon(category: string): string {
    return this.expenseService.getCategoryIcon(category);
  }

  getCategoryColor(category: string): string {
    return this.expenseService.getCategoryColor(category);
  }

  getStatusClass(percentage: number): string {
    if (percentage >= 100) return 'bg-red-500';
    if (percentage >= 75) return 'bg-orange-500';
    if (percentage >= 50) return 'bg-yellow-500';
    return 'bg-green-500';
  }

  getInsightIconClass(type: string): string {
    switch (type) {
      case 'success': return 'text-green-500';
      case 'warning': return 'text-orange-500';
      case 'info': return 'text-blue-500';
      default: return 'text-gray-500';
    }
  }
}