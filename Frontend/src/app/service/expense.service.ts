import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, map,tap } from 'rxjs/operators';

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
    return this.http.post(`${BASE_URL}/api/expenses`, expenseRequest, {
      withCredentials: true
    }).pipe(
      catchError(this.handleError)
    );
  }

  getExpenses(): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${BASE_URL}/api/expenses`, {
      withCredentials: true
    }).pipe(
      map(expenses => {
        console.log('Raw expenses data:', expenses);
        return expenses || [];
      }),
      catchError(this.handleError)
    );
  }

  getExpenseCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${BASE_URL}/api/expenses/categories`, {
      withCredentials: true
    }).pipe(
      map(categories => categories || []),
      catchError(this.handleError)
    );
  }

  getExpenseSummary(): Observable<ExpenseCategorySummary[]> {
    this.logRequestStart('api/expenses/summary');
    
    return this.http.get<ExpenseCategorySummary[]>(`${BASE_URL}/api/expenses/summary`, { 
      withCredentials: true 
    }).pipe(
      tap(rawData => {
        console.log('📊 Raw expense summary data:', rawData);
        
        // Check authentication - if auth is good but data is empty, this is suspicious
        const token = localStorage.getItem('jwt');
        console.log('🔑 JWT token in localStorage when getting summary:', 
                    token ? 'Present' : 'Missing');
      }),
      map(summary => {
        if (!summary || !Array.isArray(summary)) {
          console.warn('⚠️ Expense summary is not an array or is empty, returning empty array');
          return [];
        }
       
        if (summary.length > 0) {
          console.log('📊 First expense summary item:', summary[0]);
        }
        
        const processedSummary = summary.map(item => ({
          category: item.category || 'Unknown',
          totalAmount: typeof item.totalAmount === 'number' && !isNaN(item.totalAmount) 
            ? item.totalAmount : 0
        })).filter(item => item.totalAmount > 0);
        
        console.log('📊 Processed expense summary data:', processedSummary);
        return processedSummary;
      }),
      catchError(error => {
        console.error('❌ Error fetching expense summary:', error);
        console.log('🔑 JWT token when error occurred:', localStorage.getItem('jwt'));
        return throwError(() => error);
      })
    );
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
    return this.http.get<ExpenseTrend[]>(`${BASE_URL}/api/expenses/trends`, {
      withCredentials: true
    }).pipe(
      map(trends => {
        console.log('Raw expense trends data:', trends);
        if (!trends || !Array.isArray(trends)) {
          console.warn('Expense trends is not an array or is empty, returning empty array');
          return [];
        }

        return trends.map(trend => ({
          month: trend.month || 'Unknown',
          category: trend.category || 'Unknown',
          amount: typeof trend.amount === 'number' ? trend.amount : 0
        }));
      }),
      catchError(this.handleError)
    );
  }

  getBudgetStatus(): Observable<BudgetStatus[]> {
    return this.http.get<BudgetStatus[]>(`${BASE_URL}/api/expenses/budget-status`, {
      withCredentials: true
    }).pipe(
      map(status => {
        console.log('Raw budget status data:', status);
        if (!status || !Array.isArray(status)) {
          console.warn('Budget status is not an array or is empty, returning empty array');
          return [];
        }

        return status.map(item => ({
          category: item.category || 'Unknown',
          budgetAmount: typeof item.budgetAmount === 'number' ? item.budgetAmount : 0,
          spentAmount: typeof item.spentAmount === 'number' ? item.spentAmount : 0,
          remainingAmount: typeof item.remainingAmount === 'number' ? item.remainingAmount : 0,
          percentUsed: typeof item.percentUsed === 'number' ? item.percentUsed : 0
        }));
      }),
      catchError(this.handleError)
    );
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
  private logRequestStart(endpoint: string): void {
    console.log(`📤 Requesting data from: ${endpoint}`);
  }
  
  private logRequestComplete(endpoint: string, data: any): void {
    console.log(`📥 Data received from ${endpoint}:`, data);
    // Additional data validation checks for debugging
    if (!data) {
      console.warn(`⚠️ Received null or undefined data from ${endpoint}`);
    } else if (Array.isArray(data) && data.length === 0) {
      console.warn(`⚠️ Received empty array from ${endpoint}`);
    }
  }

  private handleError(error: any) {
    console.error('API Error in ExpenseService:', error);
    let errorMessage = 'An error occurred while processing your request';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else if (error.status) {
      // Server-side error
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }

    console.error(errorMessage);
    return throwError(() => error);
  }
}
