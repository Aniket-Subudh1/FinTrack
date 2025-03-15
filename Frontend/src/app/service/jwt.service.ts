import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';

@Injectable({
  providedIn: 'root'
})
export class JwtService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  // Method to register a new user
  register(signRequest: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, signRequest);
  }

  // Method to login the user and return JWT token
  login(loginRequest: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/login`, loginRequest);
  }

  // Method to verify OTP (Signup)
  verifyOtp(otpRequest: { email: string, otp: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/verify-otp`, otpRequest);
  }

  // Method to resend OTP (Signup)
  resendOtp(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/resend-otp`, { email });
  }

  // Method to send OTP for Forgot Password
  sendForgotPasswordOtp(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/forgot-password`, { email });
  }

  // Method to verify OTP and reset password
  verifyForgotPasswordOtp(otpRequest: { email: string, otp: string, newPassword: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/reset-password`, otpRequest);
  }

  // Method to refresh access token using refresh token
  refreshToken(): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/refresh-token`, {}, { withCredentials: true });
  }

  // Method to logout
  logout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/logout`, {}, { withCredentials: true });
  }
  
  resendForgotPasswordOtp(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/forgot-password/resend`, { email });
  }
  // Google Auth URL
  getGoogleAuthUrl(): string {
    return `${this.apiUrl}/auth/oauth2/google`;
  }

  // Check if user is logged in
  isLoggedIn(): boolean {
    return !!localStorage.getItem('jwt');
  }

  // Get token from local storage
  getToken(): string | null {
    return localStorage.getItem('jwt');
  }

  // Save token to local storage
  saveToken(token: string): void {
    localStorage.setItem('jwt', token);
  }

  // Remove token from local storage
  removeToken(): void {
    localStorage.removeItem('jwt');
  }
}