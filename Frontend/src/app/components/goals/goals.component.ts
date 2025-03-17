import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { ExpenseService } from '../../service/expense.service';
import { IncomeService } from '../../service/income.service';
import { NgxChartsModule } from '@swimlane/ngx-charts';

interface CategoryBudget {
  category: string;
  amount: number;
  recommended?: number;
  spent?: number;
}

interface ChartData {
  name: string;
  value: number;
}

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent, NgxChartsModule],
  templateUrl: './goals.component.html',
  styleUrls: ['./goals.component.css']
})
export class GoalsComponent implements OnInit {
  isSidebarOpen = true;
  activeTab = 'setup';
  
  // Goal setup properties
  monthlyIncome: number = 0;
  savingsGoal: number = 0;
  categoryBudgets: CategoryBudget[] = [];
  availableCategories: string[] = [];
  isLoading: boolean = false;
  
  // Progress tracking properties
  currentSavings: number = 0;
  daysRemaining: number = 0;
  
  // Success and warning notifications
  successBubble: { show: boolean; message: string } = { show: false, message: '' };
  warningBubble: { show: boolean; message: string } = { show: false, message: '' };
  
  // Chart data properties
  incomeVsExpenseData: ChartData[] = [];
  categoryBreakdownData: ChartData[] = [];
  spendingInsights: string[] = [];
  
  // Recommended percentages for budget categories
  categoryRecommendations: { [key: string]: number } = {
    'Housing': 30,
    'Food': 15,
    'Transportation': 10,
    'Utilities': 10,
    'Entertainment': 5,
    'Healthcare': 10,
    'Debt Payments': 10,
    'Personal Care': 5,
    'Education': 5,
    'Miscellaneous': 5
  };
  
  constructor(
    private expenseService: ExpenseService,
    private incomeService: IncomeService
  ) {}

  ngOnInit(): void {
    this.fetchExpenseCategories();
    this.loadSavedGoals();
    this.calculateDaysRemaining();
    
    // Initialize with at least one budget item
    if (this.categoryBudgets.length === 0) {
      this.addBudgetItem();
    }
  }
  
  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }
  
  setActiveTab(tab: string): void {
    this.activeTab = tab;
    
    if (tab === 'progress') {
      this.fetchExpenses();
    } else if (tab === 'analysis') {
      this.generateCharts();
      this.generateInsights();
    }
  }
  
  fetchExpenseCategories(): void {
    this.expenseService.getExpenseCategories().subscribe(
      (categories: string[]) => {
        this.availableCategories = categories;
      },
      (error) => {
        console.error('Error fetching expense categories:', error);
        this.showWarningBubble('Failed to load expense categories');
      }
    );
  }
  
  loadSavedGoals(): void {
    // Get goals from local storage or API
    const savedGoals = localStorage.getItem('financialGoals');
    
    if (savedGoals) {
      const goals = JSON.parse(savedGoals);
      this.monthlyIncome = goals.monthlyIncome;
      this.savingsGoal = goals.savingsGoal;
      this.categoryBudgets = goals.categoryBudgets;
    }
  }
  
  calculateDaysRemaining(): void {
    const now = new Date();
    const lastDayOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    this.daysRemaining = lastDayOfMonth.getDate() - now.getDate() + 1;
  }
  
  addBudgetItem(): void {
    this.categoryBudgets.push({
      category: '',
      amount: 0
    });
  }
  
  removeBudgetItem(index: number): void {
    this.categoryBudgets.splice(index, 1);
  }
  
  calculateAvailableAmount(): number {
    const totalBudgeted = this.categoryBudgets.reduce((total, budget) => total + (budget.amount || 0), 0);
    return this.monthlyIncome - this.savingsGoal - totalBudgeted;
  }
  
  getRecommendedPercentage(index: number): number {
    const category = this.categoryBudgets[index].category;
    return category ? (this.categoryRecommendations[category] || 5) : 0;
  }
  
  getRecommendedAmount(index: number): number {
    const percentage = this.getRecommendedPercentage(index);
    return (percentage / 100) * this.monthlyIncome;
  }
  
  applyRecommendations(): void {
    this.categoryBudgets.forEach((budget, index) => {
      if (budget.category) {
        const recommendedAmount = this.getRecommendedAmount(index);
        budget.amount = Math.round(recommendedAmount);
      }
    });
  }
  
  saveGoals(): void {
    this.isLoading = true;
    
    const goals = {
      monthlyIncome: this.monthlyIncome,
      savingsGoal: this.savingsGoal,
      categoryBudgets: this.categoryBudgets
    };
    
    // Save to local storage for now, you can later add API call
    localStorage.setItem('financialGoals', JSON.stringify(goals));
    
    setTimeout(() => {
      this.isLoading = false;
      this.showSuccessBubble('Financial goals saved successfully!');
      this.setActiveTab('progress');
    }, 1000);
  }
  
  fetchExpenses(): void {
    this.expenseService.getExpenses().subscribe(
      (expenses: any) => {
        // Calculate current savings and spending by category
        this.calculateProgress(expenses);
      },
      (error) => {
        console.error('Error fetching expenses:', error);
      }
    );
  }
  
  calculateProgress(expenses: any[]): void {
    // Calculate current month's expenses
    const now = new Date();
    const currentMonthExpenses = expenses.filter(expense => {
      const expenseDate = new Date(expense.date);
      return expenseDate.getMonth() === now.getMonth() && 
             expenseDate.getFullYear() === now.getFullYear();
    });
    
    // Sum expenses by category
    const expensesByCategory: { [key: string]: number } = {};
    currentMonthExpenses.forEach(expense => {
      if (!expensesByCategory[expense.category]) {
        expensesByCategory[expense.category] = 0;
      }
      expensesByCategory[expense.category] += expense.amount;
    });
    
    // Update budget objects with spent amounts
    this.categoryBudgets.forEach(budget => {
      budget.spent = expensesByCategory[budget.category] || 0;
    });
    
    // Calculate total expenses
    const totalExpenses = currentMonthExpenses.reduce((sum, expense) => sum + expense.amount, 0);
    
    // Calculate current savings (assuming all remaining money is saved)
    this.currentSavings = Math.max(0, this.monthlyIncome - totalExpenses);
  }
  
  getSavingsPercentage(): number {
    return this.savingsGoal > 0 ? Math.min(100, Math.round((this.currentSavings / this.savingsGoal) * 100)) : 0;
  }
  
  getCategorySpent(category: string): number {
    const budget = this.categoryBudgets.find(b => b.category === category);
    return budget?.spent || 0;
  }
  
  getCategoryPercentage(category: string): number {
    const budget = this.categoryBudgets.find(b => b.category === category);
    if (!budget || budget.amount === 0) return 0;
    return Math.round((this.getCategorySpent(category) / budget.amount) * 100);
  }
  
  getCategoryOverage(category: string): number {
    const budget = this.categoryBudgets.find(b => b.category === category);
    if (!budget) return 0;
    const spent = this.getCategorySpent(category);
    return spent > budget.amount ? spent - budget.amount : 0;
  }
  
  getDailyBudget(): number {
    // Get total budget excluding savings
    const totalBudget = this.categoryBudgets.reduce((sum, budget) => sum + budget.amount, 0);
    // Divide by days remaining in month
    return this.daysRemaining > 0 ? totalBudget / this.daysRemaining : 0;
  }
  
  generateCharts(): void {
    // Generate Income vs Expenses chart data
    this.incomeVsExpenseData = [
      { name: 'Income', value: this.monthlyIncome },
      { name: 'Expenses', value: this.categoryBudgets.reduce((sum, budget) => sum + (budget.spent || 0), 0) },
      { name: 'Savings', value: this.currentSavings }
    ];
    
    // Generate Category Breakdown chart data
    this.categoryBreakdownData = this.categoryBudgets
      .filter(budget => budget.category && budget.spent)
      .map(budget => ({
        name: budget.category,
        value: budget.spent || 0
      }));
  }
  
  generateInsights(): void {
    this.spendingInsights = [];
    
    // Check if user is on track with savings
    if (this.currentSavings < this.savingsGoal * 0.75) {
      this.spendingInsights.push("You're falling behind on your savings goal. Consider reducing spending in non-essential categories.");
    } else if (this.currentSavings >= this.savingsGoal) {
      this.spendingInsights.push("Great job! You've met or exceeded your savings goal for this month.");
    }
    
    // Check for overspending categories
    const overspentCategories = this.categoryBudgets.filter(budget => 
      budget.category && budget.spent && budget.amount && budget.spent > budget.amount);
    
    if (overspentCategories.length > 0) {
      this.spendingInsights.push(`You've exceeded your budget in ${overspentCategories.length} ${overspentCategories.length === 1 ? 'category' : 'categories'}.`);
      
      overspentCategories.forEach(budget => {
        const percentage = Math.round((budget.spent || 0) / budget.amount * 100) - 100;
        this.spendingInsights.push(`${budget.category}: ${percentage}% over budget. Consider adjusting your spending habits.`);
      });
    }
    
    // Add some general insights
    if (this.daysRemaining < 10) {
      this.spendingInsights.push(`Only ${this.daysRemaining} days left in the month. Your daily budget is ₹${Math.round(this.getDailyBudget())}.`);
    }
    
    // If any category has very low spending
    const lowSpendCategories = this.categoryBudgets.filter(budget => 
      budget.category && budget.spent !== undefined && budget.amount && 
      (budget.spent / budget.amount) < 0.3 && this.daysRemaining < 15);
    
    if (lowSpendCategories.length > 0) {
      this.spendingInsights.push(`You're underspending in ${lowSpendCategories.length} ${lowSpendCategories.length === 1 ? 'category' : 'categories'}. You might be able to reallocate some funds.`);
    }
  }
  
  private showSuccessBubble(message: string): void {
    this.successBubble = { show: true, message };
    setTimeout(() => {
      this.successBubble.show = false;
    }, 3000);
  }
  
  private showWarningBubble(message: string): void {
    this.warningBubble = { show: true, message };
    setTimeout(() => {
      this.warningBubble.show = false;
    }, 3000);
  }
}