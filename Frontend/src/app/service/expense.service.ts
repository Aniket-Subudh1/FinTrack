// src/app/service/expense.service.ts
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

const BASE_URL = 'http://localhost:8080';

@Injectable({
  providedIn: 'root',
})
export class ExpenseService {
  constructor(private http: HttpClient) {}

  addExpense(expenseRequest: { amount: number; category: string }) {
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

  private createAuthorizationHeader(): HttpHeaders {
    const jwtToken = localStorage.getItem('jwt');
    if (jwtToken) {
      return new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`);
    } else {
      return new HttpHeaders();
    }
  }
}
