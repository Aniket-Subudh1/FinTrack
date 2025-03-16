import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { Router } from '@angular/router';

const BASE_URL = 'http://localhost:8080';

@Injectable({
  providedIn: 'root'
})
export class JwtService {
  constructor(private http: HttpClient, private router: Router) {}

  register(signRequest: any): Observable<any> {
    return this.http.post(`${BASE_URL}/signup`, signRequest);
  }

  login(loginRequest: any): Observable<any> {
    return this.http.post(`${BASE_URL}/login`, loginRequest, { withCredentials: true })
      .pipe(
        tap((response: any) => {
          console.log('Login response:', response);
          if (response && response.jwtToken) {
            localStorage.setItem('jwt', response.jwtToken);
            console.log('JWT token stored in localStorage');
          }
        })
      );
  }

  logout(): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/logout`, {}, { withCredentials: true })
      .pipe(
        tap(response => {
          console.log('Logout response:', response);
          localStorage.removeItem('jwt');
          this.router.navigate(['/login']); // Redirect after logout
        }),
        catchError(error => {
          console.error('Logout error:', error.status, error.message, error);
          localStorage.removeItem('jwt'); 
          this.router.navigate(['/login']);
          return of({ message: 'Logged out locally' });
        })
      );
  }

  verifyOtp(otpRequest: { email: string, otp: string }): Observable<any> {
    return this.http.post(`${BASE_URL}/signup/verify-otp`, otpRequest, { withCredentials: true })
      .pipe(
        tap((response: any) => {
          if (response && response.token) {
            localStorage.setItem('jwt', response.token);
            console.log('JWT token stored after OTP verification');
          }
        })
      );
  }

  resendOtp(email: string): Observable<any> {
    return this.http.post(`${BASE_URL}/signup/resend-otp`, { email });
  }

  sendForgotPasswordOtp(email: string): Observable<any> {
    return this.http.post(`${BASE_URL}/forgot-password`, { email });
  }

  verifyForgotPasswordOtp(otpRequest: { email: string, otp: string, newPassword: string }): Observable<any> {
    return this.http.post(`${BASE_URL}/forgot-password/reset`, otpRequest);
  }

  resendForgotPasswordOtp(email: string): Observable<any> {
    return this.http.post(`${BASE_URL}/forgot-password/resend-otp`, { email });
  }

  checkAuth(): Observable<boolean> {
    console.log('Checking authentication status...');
    return this.http.get<any>(`${BASE_URL}/api/user/check-auth`, { withCredentials: true })
      .pipe(
        map(response => {
          console.log('Auth check response:', response);
          return response.authenticated || false;
        }),
        catchError(error => {
          console.error('Auth check error:', error);
          return of(false);
        })
      );
  }

  validateOAuthToken(token: string): Observable<any> {
    return this.http.post(`${BASE_URL}/oauth2/validate`, { token }, { withCredentials: true });
  }

  initiateGoogleLogin(): void {
    window.location.href = `${BASE_URL}/oauth2/authorization/google`;
  }
}