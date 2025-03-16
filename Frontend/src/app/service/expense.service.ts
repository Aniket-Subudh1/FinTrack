import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE_URL = 'http://localhost:8080';

@Injectable({
  providedIn: 'root',
})
export class ExpenseService {
  constructor(private http: HttpClient) {}

  addExpense(expenseRequest: any): Observable<any> {
    return this.http.post(`${BASE_URL}/api/expenses`, expenseRequest, { withCredentials: true });
  }
  getExpenses(): Observable<any> {
    return this.http.get(`${BASE_URL}/api/expenses`, { withCredentials: true });
  }

  getExpenseCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${BASE_URL}/api/expenses/categories`, { withCredentials: true });
  }
}