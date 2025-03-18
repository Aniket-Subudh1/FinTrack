import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE_URL = 'http://localhost:8080';

interface AddIncomeResponse {
  message: string;
}

export interface Income {
  id?: number;
  source: string;
  amount: number;
  date: Date | string;
}

@Injectable({
  providedIn: 'root',
})
export class IncomeService {
  constructor(private http: HttpClient) {}

  addIncome(incomeRequest: any): Observable<AddIncomeResponse> {
    return this.http.post<AddIncomeResponse>(`${BASE_URL}/api/incomes`, incomeRequest, { withCredentials: true });
  }

  getIncomes(): Observable<Income[]> { // Updated to return Observable<Income[]>
    return this.http.get<Income[]>(`${BASE_URL}/api/incomes`, { withCredentials: true });
  }
}