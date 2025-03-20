import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

const BASE_URL = 'http://localhost:8080';

export interface Income {
  id?: number;
  source: string;
  amount: number;
  date: Date | string;
  description?: string;
  isRecurring?: boolean;
  recurringFrequency?: string;
  tags?: string[];
}

export interface IncomeSummary {
  source: string;
  totalAmount: number;
}

export interface IncomeFilterParams {
  startDate?: string;
  endDate?: string;
  source?: string;
  minAmount?: number;
  maxAmount?: number;
  tags?: string;
  recurring?: boolean;
}

export interface AddIncomeResponse {
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class IncomeService {
  constructor(private http: HttpClient) {}

  addIncome(incomeRequest: any): Observable<AddIncomeResponse> {
    return this.http.post<AddIncomeResponse>(`${BASE_URL}/api/incomes`, incomeRequest, { withCredentials: true });
  }

  getIncomes(): Observable<Income[]> {
    return this.http.get<Income[]>(`${BASE_URL}/api/incomes`, { withCredentials: true })
      .pipe(
        map(incomes => {
          // Convert date strings to Date objects
          return incomes.map(income => ({
            ...income,
            date: new Date(income.date as string)
          }));
        })
      );
  }

  filterIncomes(filters: IncomeFilterParams): Observable<Income[]> {
    let params: any = {};
    
    if (filters.startDate) params.startDate = filters.startDate;
    if (filters.endDate) params.endDate = filters.endDate;
    if (filters.source) params.source = filters.source;
    if (filters.minAmount !== undefined) params.minAmount = filters.minAmount.toString();
    if (filters.maxAmount !== undefined) params.maxAmount = filters.maxAmount.toString();
    if (filters.tags) params.tags = filters.tags;
    if (filters.recurring !== undefined) params.recurring = filters.recurring.toString();
    
    return this.http.get<Income[]>(`${BASE_URL}/api/incomes/filter`, { 
      params,
      withCredentials: true 
    }).pipe(
      map(incomes => {
        return incomes.map(income => ({
          ...income,
          date: new Date(income.date as string)
        }));
      })
    );
  }

  getIncomeSummary(): Observable<IncomeSummary[]> {
    return this.http.get<IncomeSummary[]>(`${BASE_URL}/api/incomes/summary`, { withCredentials: true });
  }

  getMonthlyTrends(): Observable<{ [key: string]: number }> {
    return this.http.get<{ [key: string]: number }>(`${BASE_URL}/api/incomes/trends`, { withCredentials: true });
  }

  getIncomeSources(): Observable<string[]> {
    return this.http.get<string[]>(`${BASE_URL}/api/incomes/sources`, { withCredentials: true });
  }

  updateIncome(id: number, incomeRequest: any): Observable<any> {
    return this.http.put(`${BASE_URL}/api/incomes/${id}`, incomeRequest, { withCredentials: true });
  }

  deleteIncome(id: number): Observable<any> {
    return this.http.delete(`${BASE_URL}/api/incomes/${id}`, { withCredentials: true });
  }

  // Helper functions for UI
  getSourceIcon(source: string): string {
    const icons: { [key: string]: string } = {
      'Salary': 'payments',
      'Freelance': 'work',
      'Business': 'storefront',
      'Investment': 'trending_up',
      'Dividend': 'account_balance',
      'Rental': 'home',
      'Gift': 'card_giftcard',
      'Other': 'more_horiz'
    };
    return icons[source] || 'paid';
  }

  getSourceColor(source: string): string {
    const colors: { [key: string]: string } = {
      'Salary': '#4CAF50',
      'Freelance': '#2196F3',
      'Business': '#9C27B0',
      'Investment': '#FF9800',
      'Dividend': '#3F51B5',
      'Rental': '#795548',
      'Gift': '#E91E63',
      'Other': '#607D8B'
    };
    return colors[source] || '#4CAF50';
  }

  getRecurringFrequencyOptions(): string[] {
    return ['DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'];
  }

  getSuggestedSources(): string[] {
    return ['Salary', 'Freelance', 'Business', 'Investment', 'Dividend', 'Rental', 'Gift', 'Other'];
  }

  getSuggestedTags(): string[] {
    return ['personal', 'business', 'side-hustle', 'passive', 'active', 'bonus', 'tax-free', 'taxable'];
  }
}