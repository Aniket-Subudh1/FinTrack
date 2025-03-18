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

  addExpense(expenseRequest: any): Observable<any> {
    const headers = this.createAuthorizationHeader();
    return this.http.post(`${BASE_URL}/api/expenses`, expenseRequest, { headers });
  }

  getExpenses(): Observable<Expense[]> { // Changed from Observable<any[]> to Observable<Expense[]>
    const headers = this.createAuthorizationHeader();
    return this.http.get<Expense[]>(`${BASE_URL}/api/expenses`, { headers });
  }

  getExpenseCategories(): Observable<string[]> {
    const headers = this.createAuthorizationHeader();
    return this.http.get<string[]>(`${BASE_URL}/api/expenses/categories`, { headers });
  }

  getExpenseSummary(): Observable<ExpenseCategorySummary[]> {
    const headers = this.createAuthorizationHeader();
    return this.http.get<ExpenseCategorySummary[]>(`${BASE_URL}/api/expenses/summary`, { headers });
  }

  getExpensesByDateRange(startDate: string, endDate: string): Observable<any[]> {
    const headers = this.createAuthorizationHeader();
    return this.http.get<any[]>(`${BASE_URL}/api/expenses/filter`, {
      headers,
      params: { startDate, endDate }
    });
  }

  filterExpenses(filters: ExpenseFilterParams): Observable<Expense[]> {
    const headers = this.createAuthorizationHeader();
    let params = new HttpParams();
    
    if (filters.startDate) params = params.set('startDate', filters.startDate);
    if (filters.endDate) params = params.set('endDate', filters.endDate);
    if (filters.category) params = params.set('category', filters.category);
    if (filters.minAmount) params = params.set('minAmount', filters.minAmount.toString());
    if (filters.maxAmount) params = params.set('maxAmount', filters.maxAmount.toString());
    if (filters.tags) params = params.set('tags', filters.tags);
    
    return this.http.get<Expense[]>(`${BASE_URL}/api/expenses/filter`, {
      headers,
      params
    });
  }
  
  getExpenseTrends(): Observable<ExpenseTrend[]> {
    const headers = this.createAuthorizationHeader();
    return this.http.get<ExpenseTrend[]>(`${BASE_URL}/api/expenses/trends`, { headers });
  }
  
  getBudgetStatus(): Observable<BudgetStatus[]> {
    const headers = this.createAuthorizationHeader();
    return this.http.get<BudgetStatus[]>(`${BASE_URL}/api/expenses/budget-status`, { headers });
  }
  
  updateExpense(id: number, expense: any): Observable<any> {
    const headers = this.createAuthorizationHeader();
    return this.http.put(`${BASE_URL}/api/expenses/${id}`, expense, { headers });
  }
  
  deleteExpense(id: number): Observable<any> {
    const headers = this.createAuthorizationHeader();
    return this.http.delete(`${BASE_URL}/api/expenses/${id}`, { headers });
  }
  
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
  
  getCategoryColor(category: string): string {
    const colors: { [key: string]: string } = {
      'GROCERY': '#4CAF50',
      'UTILITIES': '#2196F3',
      'INSURANCE': '#9C27B0',
      'ENTERTAINMENT': '#FF9800',
      'HOUSING': '#F44336',
      'DEBT_PAYMENTS': '#607D8B',
      'HEALTH_CARE': '#E91E63',
      'MEMBERSHIPS_AND_SUBSCRIPTIONS': '#3F51B5',
      'HOME_MAINTENANCE': '#795548',
      'TAXES': '#FF5722',
      'CLOTHING': '#009688',
      'PERSONAL_CARE': '#673AB7',
      'OTHERS': '#757575'
    };
    return colors[category] || '#FFD700';
  }

  private createAuthorizationHeader(): HttpHeaders {
    const jwtToken = localStorage.getItem('jwt');
    return jwtToken ? new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`) : new HttpHeaders();
  }
}