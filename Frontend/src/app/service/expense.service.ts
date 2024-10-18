import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Expense {
  userName: string;
  amount: number;
  category: string;
}

@Injectable({
  providedIn: 'root' // Standalone service
})
export class ExpenseService {
  private apiUrl = 'http://localhost:8080/api/expenses'; // Replace with your backend API URL

  constructor(private http: HttpClient) {}

  createExpense(expense: Expense): Observable<Expense> {
    return this.http.post<Expense>(this.apiUrl, expense);
  }
}
