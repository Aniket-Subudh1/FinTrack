import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, tap } from 'rxjs/operators';
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
  private clearLocalStorage(): void {
    localStorage.removeItem('jwt');
    console.log('JWT removed from localStorage');
  }
  logout(): Observable<any> {
    console.log('Starting logout process');
    console.log('Current cookies:', document.cookie);

    // Don't send CSRF tokens in headers - simplify the request
    // Your backend should recognize the authenticated cookie without additional headers

    return this.http.post<any>(`${BASE_URL}/logout`, {}, {
      withCredentials: true, // This ensures cookies are sent
      observe: 'response'  // Get full response including headers
    }).pipe(
      tap(response => {
        console.log('Logout API response:', response);
        console.log('Response headers:', response.headers);
        console.log('Response status:', response.status);
        localStorage.removeItem('jwt');
        console.log('JWT removed from localStorage');
        console.log('Cookies after API call:', document.cookie);
      }),
      catchError(error => {
        console.error('Logout API error details:', {
          status: error.status,
          statusText: error.statusText,
          error: error.error,
          message: error.message,
          url: error.url
        });

        // Still clear local storage token
        localStorage.removeItem('jwt');
        console.log('JWT removed from localStorage despite API error');
        console.log('Cookies after error:', document.cookie);

        return of({ message: 'Logged out locally only' });
      }),
      finalize(() => {
        // Force redirect regardless of success/failure
        this.router.navigate(['/login'], { replaceUrl: true });
      })
    );
  }

  private getCsrfToken(): string {

    console.warn('getCsrfToken() called but not used in current logout implementation');
    return '';
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

  getUserInfo(): Observable<any> {
    console.log('Getting user info for potential token extraction');
    return this.http.get<any>(`${BASE_URL}/oauth2/user-info`, { 
      withCredentials: true 
    }).pipe(
      tap(response => {
        console.log('User info response:', response);
        if (response && response.token) {
          localStorage.setItem('jwt', response.token);
          console.log('JWT token stored from user info response');
        }
      }),
      catchError(error => {
        console.error('Error getting user info:', error);
        return of({ authenticated: false });
      })
    );
  }
  checkAuth(): Observable<boolean> {
    console.log('Checking authentication status...');
    return this.http.get<any>(`${BASE_URL}/api/user/check-auth`, { withCredentials: true })
      .pipe(
        map(response => {
          console.log('Auth check response:', response);
          
          // If we get a token in the response, store it
          if (response && response.token) {
            localStorage.setItem('jwt', response.token);
            console.log('JWT token stored from auth check response');
          }
          
          return response.authenticated || false;
        }),
        catchError(error => {
          console.error('Auth check error:', error);
          return of(false);
        })
      );
  }
  syncTokenState(): Observable<boolean> {
    const token = localStorage.getItem('jwt');
    if (!token) {
      return of(false);
    }

    return this.http.post(`${BASE_URL}/oauth2/validate`, { token }, { withCredentials: true })
      .pipe(
        map((response: any) => {
          return response && response.valid;
        }),
        catchError(() => {
          this.clearLocalStorage();
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
