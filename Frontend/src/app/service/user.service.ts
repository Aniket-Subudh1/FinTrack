import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Router } from '@angular/router';

export interface UserDetailsResponse {
  name: string;
  email: string;
  address: string;
  gender: string;
  age: number;
  profilePhoto?: string; 
}

export interface UserDetailsUpdateResponse {
  message: string;
}

const BASE_URL = 'http://localhost:8080';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  constructor(private http: HttpClient) {}
 
  getUserDetails(): Observable<any> {
    return this.http.get(`${BASE_URL}/api/user/details`, { withCredentials: true });
  }

  updateUserDetails(updateRequest: any): Observable<any> {
    return this.http.put(`${BASE_URL}/api/user/details`, updateRequest, { withCredentials: true });
  }

  private handleError(error: HttpErrorResponse) {
    console.error('API Error:', error);
    
    // Return a user-friendly error message
    let errorMessage = 'An error occurred. Please try again later.';
    
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Server-side error
      if (error.status === 401) {
        errorMessage = 'Your session has expired. Please log in again.';
      } else if (error.error && error.error.message) {
        errorMessage = error.error.message;
      }
    }
    
    return throwError(() => new Error(errorMessage));
  }
}