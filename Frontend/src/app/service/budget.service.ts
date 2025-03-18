import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE_URL = 'http://localhost:8080';

export interface BudgetItem {
  category: string;
  amount: number;
}

export interface SavingsGoalResponse {
  amount: number;
}

@Injectable({
  providedIn: 'root',
})
export class BudgetService {
  constructor(private http: HttpClient) {}

  // Get all budget items for the logged in user
  getBudgetItems(): Observable<BudgetItem[]> {
    return this.http.get<BudgetItem[]>(`${BASE_URL}/api/budget`, { withCredentials: true });
  }

  // Add or update a budget item
  saveOrUpdateBudgetItem(budgetItem: BudgetItem): Observable<any> {
    return this.http.post(`${BASE_URL}/api/budget`, budgetItem, { withCredentials: true });
  }

  // Delete a budget item
  deleteBudgetItem(category: string): Observable<any> {
    return this.http.delete(`${BASE_URL}/api/budget/${category}`, { withCredentials: true });
  }

  // Get savings goal
  getSavingsGoal(): Observable<SavingsGoalResponse> {
    return this.http.get<SavingsGoalResponse>(`${BASE_URL}/api/budget/savings-goal`, { withCredentials: true });
  }

  // Set savings goal
  setSavingsGoal(amount: number): Observable<any> {
    return this.http.post(`${BASE_URL}/api/budget/savings-goal`, { amount }, { withCredentials: true });
  }
}