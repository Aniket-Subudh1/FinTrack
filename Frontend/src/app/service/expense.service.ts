import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

const BASE_URL = 'http://localhost:8080';

@Injectable({
  providedIn: 'root',
})
export class ExpenseService {
  constructor(private http: HttpClient) {}

  addExpense(expenseRequest: { amount: number; category: string }) {
    return this.http.post(`${BASE_URL}/api/expenses`, expenseRequest, { withCredentials: true });
  }

  getExpenses() {
    return this.http.get(`${BASE_URL}/api/expenses`, { withCredentials: true });
  }

  getExpenseCategories() {
    return this.http.get<string[]>(`${BASE_URL}/api/expenses/categories`, { withCredentials: true });
  }
}