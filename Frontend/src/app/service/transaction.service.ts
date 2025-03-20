import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ExpenseService } from './expense.service';
import { IncomeService } from './income.service';

export interface Transaction {
  id?: number;
  type: 'income' | 'expense';
  amount: number;
  category: string;
  date: Date | string;
  description?: string;
  tags?: string[];
  isRecurring?: boolean;
  recurringFrequency?: string;
}

export interface TransactionFilterParams {
  startDate?: string;
  endDate?: string;
  category?: string;
  type?: 'income' | 'expense' | 'all';
  minAmount?: number;
  maxAmount?: number;
  tags?: string[];
  searchText?: string;
}

export interface TransactionInsight {
  title: string;
  description: string;
  type: 'info' | 'warning' | 'success';
  icon: string;
}

const BASE_URL = 'http://localhost:8080';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  constructor(
    private http: HttpClient,
    private expenseService: ExpenseService,
    private incomeService: IncomeService
  ) {}

  // Get all transactions (combining expenses and incomes)
  getAllTransactions(): Observable<Transaction[]> {
    return forkJoin({
      expenses: this.expenseService.getExpenses().pipe(
        catchError(err => {
          console.error('Error loading expenses:', err);
          return of([]);
        })
      ),
      incomes: this.incomeService.getIncomes().pipe(
        catchError(err => {
          console.error('Error loading incomes:', err);
          return of([]);
        })
      )
    }).pipe(
      map(({ expenses, incomes }) => {
        // Map expenses to Transaction interface
        const expenseTransactions = expenses.map(expense => ({
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
        
        // Map incomes to Transaction interface
        const incomeTransactions = incomes.map(income => ({
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
        
        // Combine and sort by date (most recent first)
        return [...expenseTransactions, ...incomeTransactions].sort((a, b) => {
          return new Date(b.date).getTime() - new Date(a.date).getTime();
        });
      })
    );
  }

  // Filter transactions based on various criteria
  filterTransactions(transactions: Transaction[], filters: TransactionFilterParams): Transaction[] {
    return transactions.filter(transaction => {
      // Filter by type
      if (filters.type && filters.type !== 'all' && transaction.type !== filters.type) {
        return false;
      }
      
      // Filter by date range
      if (filters.startDate && new Date(transaction.date) < new Date(filters.startDate)) {
        return false;
      }
      
      if (filters.endDate) {
        const endDate = new Date(filters.endDate);
        endDate.setHours(23, 59, 59, 999); // End of day
        if (new Date(transaction.date) > endDate) {
          return false;
        }
      }
      
      // Filter by category
      if (filters.category && transaction.category !== filters.category) {
        return false;
      }
      
      // Filter by amount range
      if (filters.minAmount !== undefined && transaction.amount < filters.minAmount) {
        return false;
      }
      
      if (filters.maxAmount !== undefined && transaction.amount > filters.maxAmount) {
        return false;
      }
      
      // Filter by tags
      if (filters.tags && filters.tags.length > 0) {
        const transactionTags = transaction.tags || [];
        if (!filters.tags.some(tag => transactionTags.includes(tag))) {
          return false;
        }
      }
      
      // Filter by search text
      if (filters.searchText) {
        const searchLower = filters.searchText.toLowerCase();
        const foundInCategory = transaction.category.toLowerCase().includes(searchLower);
        const foundInDescription = transaction.description?.toLowerCase().includes(searchLower) || false;
        const foundInTags = (transaction.tags || []).some(tag => tag.toLowerCase().includes(searchLower));
        
        if (!foundInCategory && !foundInDescription && !foundInTags) {
          return false;
        }
      }
      
      return true;
    });
  }

  // Get transaction history for a specific period
  getTransactionHistory(period: 'week' | 'month' | 'year' | 'all' = 'month'): Observable<Transaction[]> {
    return this.getAllTransactions().pipe(
      map(transactions => {
        const now = new Date();
        let startDate: Date;
        
        switch (period) {
          case 'week':
            startDate = new Date(now);
            startDate.setDate(now.getDate() - 7);
            break;
          case 'month':
            startDate = new Date(now.getFullYear(), now.getMonth(), 1);
            break;
          case 'year':
            startDate = new Date(now.getFullYear(), 0, 1);
            break;
          case 'all':
          default:
            return transactions;
        }
        
        return transactions.filter(t => new Date(t.date) >= startDate);
      })
    );
  }

  // Calculate spending distribution by categories
  getCategoryDistribution(transactions: Transaction[], type: 'income' | 'expense' | 'all' = 'all'): { name: string, value: number }[] {
    const filteredTransactions = type === 'all' 
      ? transactions 
      : transactions.filter(t => t.type === type);
      
    const categoryMap: Record<string, number> = {};
    
    filteredTransactions.forEach(transaction => {
      if (!categoryMap[transaction.category]) {
        categoryMap[transaction.category] = 0;
      }
      categoryMap[transaction.category] += transaction.amount;
    });
    
    return Object.entries(categoryMap)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value);
  }

  // Calculate monthly financial data for charts
  getMonthlyFinancialData(transactions: Transaction[], monthsCount: number = 6): any[] {
    const now = new Date();
    const months: { [key: string]: { income: number, expense: number } } = {};
    
    // Initialize the last N months
    for (let i = monthsCount - 1; i >= 0; i--) {
      const month = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const monthKey = month.toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
      months[monthKey] = { income: 0, expense: 0 };
    }
    
    // Populate data
    transactions.forEach(transaction => {
      const date = new Date(transaction.date);
      if (date.getTime() >= new Date(now.getFullYear(), now.getMonth() - (monthsCount - 1), 1).getTime()) {
        const monthKey = date.toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
        if (months[monthKey]) {
          if (transaction.type === 'income') {
            months[monthKey].income += transaction.amount;
          } else {
            months[monthKey].expense += transaction.amount;
          }
        }
      }
    });
    
    // Format for charts
    return Object.entries(months).map(([name, data]) => ({
      name,
      series: [
        { name: 'Income', value: data.income },
        { name: 'Expense', value: data.expense }
      ]
    }));
  }

  // Generate financial insights based on transaction data
  generateInsights(transactions: Transaction[]): TransactionInsight[] {
    const insights: TransactionInsight[] = [];
    const now = new Date();
    
    // Current month data
    const currentMonthStart = new Date(now.getFullYear(), now.getMonth(), 1);
    const currentMonthTransactions = transactions.filter(t => 
      new Date(t.date) >= currentMonthStart && new Date(t.date) <= now
    );
    
    // Previous month data
    const prevMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const prevMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0);
    const prevMonthTransactions = transactions.filter(t => 
      new Date(t.date) >= prevMonthStart && new Date(t.date) <= prevMonthEnd
    );
    
    // Calculate totals
    const currentIncomeTotal = currentMonthTransactions
      .filter(t => t.type === 'income')
      .reduce((sum, t) => sum + t.amount, 0);
      
    const currentExpenseTotal = currentMonthTransactions
      .filter(t => t.type === 'expense')
      .reduce((sum, t) => sum + t.amount, 0);
      
    const prevIncomeTotal = prevMonthTransactions
      .filter(t => t.type === 'income')
      .reduce((sum, t) => sum + t.amount, 0);
      
    const prevExpenseTotal = prevMonthTransactions
      .filter(t => t.type === 'expense')
      .reduce((sum, t) => sum + t.amount, 0);
    
    // Calculate savings rate
    const currentSavingsRate = currentIncomeTotal > 0 
      ? ((currentIncomeTotal - currentExpenseTotal) / currentIncomeTotal) * 100 
      : 0;
    
    // Insight: Savings Rate
    if (currentSavingsRate >= 20) {
      insights.push({
        title: 'Great Saving Habit!',
        description: `You're saving ${currentSavingsRate.toFixed(1)}% of your income this month.`,
        type: 'success',
        icon: 'savings'
      });
    } else if (currentSavingsRate > 0) {
      insights.push({
        title: 'Savings Below Target',
        description: `You're currently saving ${currentSavingsRate.toFixed(1)}% of income. Try to reach 20%.`,
        type: 'info',
        icon: 'account_balance'
      });
    } else if (currentSavingsRate < 0) {
      insights.push({
        title: 'Spending Alert',
        description: 'You\'re spending more than you earn this month.',
        type: 'warning',
        icon: 'warning'
      });
    }
    
    // Insight: Income Change
    if (prevIncomeTotal > 0) {
      const incomeChangePercent = ((currentIncomeTotal - prevIncomeTotal) / prevIncomeTotal) * 100;
      if (incomeChangePercent >= 10) {
        insights.push({
          title: 'Income Increase',
          description: `Your income increased by ${incomeChangePercent.toFixed(1)}% compared to last month.`,
          type: 'success',
          icon: 'trending_up'
        });
      } else if (incomeChangePercent <= -10) {
        insights.push({
          title: 'Income Decrease',
          description: `Your income decreased by ${Math.abs(incomeChangePercent).toFixed(1)}% from last month.`,
          type: 'warning',
          icon: 'trending_down'
        });
      }
    }
    
    // Insight: Expense Change
    if (prevExpenseTotal > 0) {
      const expenseChangePercent = ((currentExpenseTotal - prevExpenseTotal) / prevExpenseTotal) * 100;
      if (expenseChangePercent >= 20) {
        insights.push({
          title: 'Spending Increase',
          description: `Your spending increased by ${expenseChangePercent.toFixed(1)}% compared to last month.`,
          type: 'warning',
          icon: 'trending_up'
        });
      } else if (expenseChangePercent <= -10) {
        insights.push({
          title: 'Spending Decrease',
          description: `You reduced spending by ${Math.abs(expenseChangePercent).toFixed(1)}% from last month.`,
          type: 'success',
          icon: 'trending_down'
        });
      }
    }
    
    // Insight: Top Expense Category
    const expenseCategories = this.getCategoryDistribution(currentMonthTransactions, 'expense');
    if (expenseCategories.length > 0) {
      const topCategory = expenseCategories[0];
      const percentOfTotal = (topCategory.value / currentExpenseTotal) * 100;
      
      if (percentOfTotal > 40) {
        insights.push({
          title: 'High Category Concentration',
          description: `${topCategory.name} makes up ${percentOfTotal.toFixed(1)}% of your expenses.`,
          type: 'info',
          icon: 'pie_chart'
        });
      }
    }
    
    // Insight: Recurring Expenses
    const recurringExpenses = transactions.filter(t => t.type === 'expense' && t.isRecurring);
    const recurringTotal = recurringExpenses.reduce((sum, t) => sum + t.amount, 0);
    const recurringPercent = currentExpenseTotal > 0 ? (recurringTotal / currentExpenseTotal) * 100 : 0;
    
    if (recurringPercent > 70) {
      insights.push({
        title: 'High Fixed Expenses',
        description: `${recurringPercent.toFixed(1)}% of your spending is on recurring expenses.`,
        type: 'info',
        icon: 'repeat'
      });
    }
    
    return insights;
  }
}