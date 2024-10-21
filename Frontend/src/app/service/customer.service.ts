// customer.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class CustomerService {
  private apiUrl = 'http://localhost:8080/api/customers/names'; // Backend endpoint URL

  constructor(private http: HttpClient) {}

  getAllCustomerNames(): Observable<string[]> {
    return this.http.get<string[]>(this.apiUrl); // HTTP call to fetch customer names
  }
}
