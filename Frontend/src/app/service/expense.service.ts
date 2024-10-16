import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ExpenseService {

  private apiUrl = 'http://localhost:4200/api/expenses/add';  // Spring Boot API endpoint

  constructor(private http: HttpClient) { }

  addExpense(expense: any): Observable<any> {
    // Make an HTTP POST request to add the expense
    return this.http.post(this.apiUrl, expense);
  }
}
