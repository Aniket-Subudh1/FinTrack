import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Router } from '@angular/router';

const BASE_URL = 'http://localhost:8080';  

@Injectable({
  providedIn: 'root'
})
export class JwtService {

  constructor(private http: HttpClient, private router: Router) { }

  // Method to register a new user
  register(signRequest: any): Observable<any> {
    return this.http.post(`${BASE_URL}/signup`, signRequest);
  }

  // Method to login the user and return JWT token
  login(loginRequest: any): Observable<any> {
    return this.http.post(`${BASE_URL}/login`, loginRequest, { withCredentials: true });
  }

  logout(): Observable<any> { // Add type parameter
    return this.http.post<any>(`${BASE_URL}/logout`, {}, { withCredentials: true })
      .pipe(
        tap(() => {
          localStorage.removeItem('jwt'); // Remove token once at the service level
        }),
        catchError(error => {
          console.error('Logout error:', error);
          localStorage.removeItem('jwt');
          return of({ message: 'Logged out locally' });
        })
      );
  }

  // Method to verify OTP (Signup)
  verifyOtp(otpRequest: { email: string, otp: string }): Observable<any> {
    return this.http.post(`${BASE_URL}/signup/verify-otp`, otpRequest, { withCredentials: true });
  }

  // Method to resend OTP (Signup)
  resendOtp(email: string): Observable<any> {
    return this.http.post(`${BASE_URL}/signup/resend-otp`, { email });
  }

  // Method to send OTP for Forgot Password
  sendForgotPasswordOtp(email: string): Observable<any> {
    return this.http.post(`${BASE_URL}/forgot-password`, { email });
  }

  // Method to verify OTP and reset password
  verifyForgotPasswordOtp(otpRequest: { email: string, otp: string, newPassword: string }): Observable<any> {
    return this.http.post(`${BASE_URL}/forgot-password/reset`, otpRequest);
  }

  // Method to resend OTP for Forgot Password
  resendForgotPasswordOtp(email: string): Observable<any> {
    return this.http.post(`${BASE_URL}/forgot-password/resend-otp`, { email });
  }

  // Validate session - Check if user is authenticated
  checkAuth(): Observable<boolean> {
    return this.http.get<any>(`${BASE_URL}/api/user/check-auth`, { withCredentials: true })
      .pipe(
        map(() => true),
        catchError(() => of(false))
      );
  }

  // Handle Google OAuth token validation
  validateOAuthToken(token: string): Observable<any> {
    return this.http.post(`${BASE_URL}/oauth2/validate`, { token }, { withCredentials: true });
  }

  // Initialize OAuth login - redirect to Google
  initiateGoogleLogin(): void {
    window.location.href = `${BASE_URL}/oauth2/authorization/google`;
  }
}

function tap(arg0: () => void): import("rxjs").OperatorFunction<Object, any> {
  throw new Error('Function not implemented.');
}
