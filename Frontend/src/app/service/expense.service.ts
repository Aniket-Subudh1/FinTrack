import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE_URL = 'http://localhost:8080';

export interface ExpenseCategorySummary {
  category: string;
  totalAmount: number;
}

export interface ExpenseTrend {
  month: string;
  category: string;
  amount: number;
}

export interface BudgetStatus {
  category: string;
  budgetAmount: number;
  spentAmount: number;
  remainingAmount: number;
  percentUsed: number;
}

export interface Expense {
  id?: number;
  amount: number;
  category: string;
  date: Date;
  customerEmail?: string;
  tags?: string[];
  note?: string;
  isRecurring?: boolean;
  recurringFrequency?: string;
}

export interface ExpenseFilterParams {
  startDate?: string;
  endDate?: string;
  category?: string;
  minAmount?: number;
  maxAmount?: number;
  tags?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ExpenseService {
  constructor(private http: HttpClient) {}

  addExpense(expenseRequest: any) {
    const headers = this.createAuthorizationHeader();
    return this.http.post(`${BASE_URL}/api/expenses`, expenseRequest, { headers });
  }

  getExpenses() {
    const headers = this.createAuthorizationHeader();
    return this.http.get(`${BASE_URL}/api/expenses`, { headers });
  }

  getExpenseCategories() {
    const headers = this.createAuthorizationHeader();
    return this.http.get<string[]>(`${BASE_URL}/api/expenses/categories`, { headers });
  }

  getExpenseSummary(): Observable<ExpenseCategorySummary[]> {
    const headers = this.createAuthorizationHeader();
    return this.http.get<ExpenseCategorySummary[]>(`${BASE_URL}/api/expenses/summary`, { headers });
  }

  // Existing: Fetch expenses by date range
  getExpensesByDateRange(startDate: string, endDate: string): Observable<any[]> {
    const headers = this.createAuthorizationHeader();
    return this.http.get<any[]>(`${BASE_URL}/api/expenses/filter`, {
      headers,
      params: { startDate, endDate }
    });
  }

  // New: Filter expenses with advanced params
  filterExpenses(filters: ExpenseFilterParams): Observable<Expense[]> {
    const headers = this.createAuthorizationHeader();
    let params = new HttpParams();
    
    if (filters.startDate) {
      params = params.set('startDate', filters.startDate);
    }
    
    if (filters.endDate) {
      params = params.set('endDate', filters.endDate);
    }
    
    if (filters.category) {
      params = params.set('category', filters.category);
    }
    
    if (filters.minAmount) {
      params = params.set('minAmount', filters.minAmount.toString());
    }
    
    if (filters.maxAmount) {
      params = params.set('maxAmount', filters.maxAmount.toString());
    }
    
    if (filters.tags) {
      params = params.set('tags', filters.tags);
    }
    
    return this.http.get<Expense[]>(`${BASE_URL}/api/expenses/filter`, {
      headers,
      params
    });
  }
  
  // New: Get expense trends for charts
  getExpenseTrends(): Observable<ExpenseTrend[]> {
    const headers = this.createAuthorizationHeader();
    return this.http.get<ExpenseTrend[]>(`${BASE_URL}/api/expenses/trends`, { headers });
  }
  
  // New: Get budget status
  getBudgetStatus(): Observable<BudgetStatus[]> {
    const headers = this.createAuthorizationHeader();
    return this.http.get<BudgetStatus[]>(`${BASE_URL}/api/expenses/budget-status`, { headers });
  }
  
  // New: Update an expense
  updateExpense(id: number, expense: any): Observable<any> {
    const headers = this.createAuthorizationHeader();
    return this.http.put(`${BASE_URL}/api/expenses/${id}`, expense, { headers });
  }
  
  // New: Delete an expense
  deleteExpense(id: number): Observable<any> {
    const headers = this.createAuthorizationHeader();
    return this.http.delete(`${BASE_URL}/api/expenses/${id}`, { headers });
  }
  
  // Utility method to get category icons
  getCategoryIcon(category: string): string {
    const icons: { [key: string]: string } = {
      'GROCERY': 'shopping_cart',
      'UTILITIES': 'electrical_services',
      'INSURANCE': 'health_and_safety',
      'ENTERTAINMENT': 'movie',
      'HOUSING': 'home',
      'DEBT_PAYMENTS': 'payments',
      'HEALTH_CARE': 'medical_services',
      'MEMBERSHIPS_AND_SUBSCRIPTIONS': 'subscriptions',
      'HOME_MAINTENANCE': 'home_repair_service',
      'TAXES': 'receipt_long',
      'CLOTHING': 'checkroom',
      'PERSONAL_CARE': 'spa',
      'OTHERS': 'more_horiz'
    };
    
    return icons[category] || 'receipt_long';
  }
  
  // Utility method to get category colors
  getCategoryColor(category: string): string {
    const colors: { [key: string]: string } = {
      'GROCERY': '#4CAF50', // Green
      'UTILITIES': '#2196F3', // Blue
      'INSURANCE': '#9C27B0', // Purple
      'ENTERTAINMENT': '#FF9800', // Orange
      'HOUSING': '#F44336', // Red
      'DEBT_PAYMENTS': '#607D8B', // Blue Grey
      'HEALTH_CARE': '#E91E63', // Pink
      'MEMBERSHIPS_AND_SUBSCRIPTIONS': '#3F51B5', // Indigo
      'HOME_MAINTENANCE': '#795548', // Brown
      'TAXES': '#FF5722', // Deep Orange
      'CLOTHING': '#009688', // Teal
      'PERSONAL_CARE': '#673AB7', // Deep Purple
      'OTHERS': '#757575' // Grey
    };
    
    return colors[category] || '#FFD700'; // Default to gold
  }

  private createAuthorizationHeader(): HttpHeaders {
    const jwtToken = localStorage.getItem('jwt');
    if (jwtToken) {
      return new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`);
    } else {
      return new HttpHeaders();
    }
  }
}