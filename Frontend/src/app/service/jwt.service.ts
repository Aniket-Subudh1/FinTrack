import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE_URL = 'http://localhost:8080';  // Replace with your Backend API URL

@Injectable({
  providedIn: 'root'
})
export class JwtService {

  constructor(private http: HttpClient) { }

  // Method to register a new user
  register(signRequest: any): Observable<any> {
    return this.http.post(`${BASE_URL}/signup`, signRequest);
  }

  // Method to login the user and return JWT token
  login(loginRequest: any): Observable<any> {
    return this.http.post(`${BASE_URL}/login`, loginRequest);
  }

  // Method to verify OTP (Signup)
  verifyOtp(otpRequest: { email: string, otp: string }): Observable<any> {
    // Assuming this endpoint does not require Authorization header
    return this.http.post(`${BASE_URL}/signup/verify-otp`, otpRequest);
  }

  // Method to resend OTP (Signup)
  resendOtp(email: string): Observable<any> {
    // Assuming this endpoint does not require Authorization header
    return this.http.post(`${BASE_URL}/signup/resend-otp`, { email });
  }

  // Method to send OTP for Forgot Password
  sendForgotPasswordOtp(email: string): Observable<any> {
    // No Authorization header needed
    return this.http.post(`${BASE_URL}/forgot-password`, { email });
  }

  // Method to verify OTP and reset password
  verifyForgotPasswordOtp(otpRequest: { email: string, otp: string, newPassword: string }): Observable<any> {
    // No Authorization header needed
    return this.http.post(`${BASE_URL}/forgot-password/reset`, otpRequest);
  }

  // Method to resend OTP for Forgot Password
  resendForgotPasswordOtp(email: string): Observable<any> {
    // No Authorization header needed
    return this.http.post(`${BASE_URL}/forgot-password/resend-otp`, { email });
  }

  // Create Authorization Header with JWT token from localStorage
  private createAuthorizationHeader(): HttpHeaders {
    const jwtToken = localStorage.getItem('jwt');
    if (jwtToken) {
      console.log("JWT token found in local storage:", jwtToken);
      return new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`);
    } else {
      console.log("JWT token not found in local storage");
      return new HttpHeaders();  
    }
  }
}
