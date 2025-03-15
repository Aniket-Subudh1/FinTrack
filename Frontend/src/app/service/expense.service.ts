import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';

export interface ExpenseRequest {
  amount: number;
  category: string;
  date?: Date;
  description?: string;
}

export interface ExpenseResponse {
  id: number;
  amount: number;
  category: string;
  date: Date;
  description?: string;
  userEmail: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface ExpenseSummary {
  category?: string;
  period: string;
  amount: number;
  percentage?: number;
}

@Injectable({
  providedIn: 'root',
})
export class ExpenseService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Create a new expense
  addExpense(expenseRequest: ExpenseRequest): Observable<ExpenseResponse> {
    return this.http.post<ExpenseResponse>(`${this.apiUrl}/expenses`, expenseRequest);
  }

  // Get all expenses
  getExpenses(): Observable<ExpenseResponse[]> {
    return this.http.get<ExpenseResponse[]>(`${this.apiUrl}/expenses`);
  }

  // Get paginated expenses
  getPaginatedExpenses(page: number = 0, size: number = 10, sortBy: string = 'date', direction: string = 'desc'): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/expenses/paginated?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`
    );
  }

  // Get expense by ID
  getExpenseById(id: number): Observable<ExpenseResponse> {
    return this.http.get<ExpenseResponse>(`${this.apiUrl}/expenses/${id}`);
  }

  // Update expense
  updateExpense(id: number, expenseRequest: ExpenseRequest): Observable<ExpenseResponse> {
    return this.http.put<ExpenseResponse>(`${this.apiUrl}/expenses/${id}`, expenseRequest);
  }

  // Delete expense
  deleteExpense(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/expenses/${id}`);
  }

  // Filter expenses by category
  getExpensesByCategory(category: string): Observable<ExpenseResponse[]> {
    return this.http.get<ExpenseResponse[]>(`${this.apiUrl}/expenses/filter/category/${category}`);
  }

  // Filter expenses by date range
  getExpensesByDateRange(startDate: Date, endDate: Date): Observable<ExpenseResponse[]> {
    const start = startDate.toISOString().split('T')[0];
    const end = endDate.toISOString().split('T')[0];
    return this.http.get<ExpenseResponse[]>(
      `${this.apiUrl}/expenses/filter/date?startDate=${start}&endDate=${end}`
    );
  }

  // Filter expenses by category and date range
  getExpensesByCategoryAndDateRange(category: string, startDate: Date, endDate: Date): Observable<ExpenseResponse[]> {
    const start = startDate.toISOString().split('T')[0];
    const end = endDate.toISOString().split('T')[0];
    return this.http.get<ExpenseResponse[]>(
      `${this.apiUrl}/expenses/filter/category-date?category=${category}&startDate=${start}&endDate=${end}`
    );
  }

  // Get expense summary by category
  getExpenseSummaryByCategory(startDate: Date, endDate: Date): Observable<ExpenseSummary[]> {
    const start = startDate.toISOString().split('T')[0];
    const end = endDate.toISOString().split('T')[0];
    return this.http.get<ExpenseSummary[]>(
      `${this.apiUrl}/expenses/summary/category?startDate=${start}&endDate=${end}`
    );
  }

  // Get expense summary by month
  getExpenseSummaryByMonth(startDate: Date, endDate: Date): Observable<ExpenseSummary[]> {
    const start = startDate.toISOString().split('T')[0];
    const end = endDate.toISOString().split('T')[0];
    return this.http.get<ExpenseSummary[]>(
      `${this.apiUrl}/expenses/summary/month?startDate=${start}&endDate=${end}`
    );
  }

  // Get all expense categories
  getExpenseCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/expenses/categories`);
  }
}