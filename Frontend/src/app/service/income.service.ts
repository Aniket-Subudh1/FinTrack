import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE_URL = 'http://localhost:8080';

interface AddIncomeResponse {
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class IncomeService {
  constructor(private http: HttpClient) {}

  addIncome(incomeRequest: any): Observable<any> {
    return this.http.post(`${BASE_URL}/api/incomes`, incomeRequest, { withCredentials: true });
  }

  getIncomes() {
    return this.http.get(`${BASE_URL}/api/incomes`, { withCredentials: true });
  }
}