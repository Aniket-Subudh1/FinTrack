import { HttpClient, HttpHeaders } from '@angular/common/http';
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

  addIncome(incomeRequest: { amount: number; source: string }): Observable<AddIncomeResponse> {
    const headers = this.createAuthorizationHeader();
    return this.http.post<AddIncomeResponse>(`${BASE_URL}/api/incomes`, incomeRequest, { headers });
  }

  getIncomes() {
    const headers = this.createAuthorizationHeader();
    return this.http.get(`${BASE_URL}/api/incomes`, { headers });
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
