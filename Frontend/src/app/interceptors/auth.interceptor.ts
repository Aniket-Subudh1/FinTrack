import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { JwtService } from '../service/jwt.service';

// Singleton state for the refresh token process
const refreshingState = {
  isRefreshing: false,
  refreshTokenSubject: new BehaviorSubject<string | null>(null)
};

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const jwtService = inject(JwtService);
  const router = inject(Router);

  // Skip interceptor for refresh token or auth requests
  if (isRefreshTokenRequest(req) || isAuthRequest(req)) {
    return next(req);
  }

  // Add token if available
  const token = jwtService.getToken();
  if (token) {
    req = addToken(req, token);
  }

  // Handle the request and catch any errors
  return next(req).pipe(
    catchError(error => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        return handle401Error(req, next, jwtService, router);
      }
      return throwError(() => error);
    })
  );
};

// Helper function to add the token to a request
function addToken(request: any, token: string): any {
  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
}

// Handle 401 Unauthorized errors
function handle401Error(request: any, next: any, jwtService: JwtService, router: Router): Observable<any> {
  if (!refreshingState.isRefreshing) {
    refreshingState.isRefreshing = true;
    refreshingState.refreshTokenSubject.next(null);

    return jwtService.refreshToken().pipe(
      switchMap((token: any) => {
        refreshingState.isRefreshing = false;
        jwtService.saveToken(token.token);
        refreshingState.refreshTokenSubject.next(token.token);
        return next(addToken(request, token.token));
      }),
      catchError((err) => {
        refreshingState.isRefreshing = false;
        jwtService.removeToken();
        router.navigate(['/login']);
        return throwError(() => err);
      })
    );
  }

  return refreshingState.refreshTokenSubject.pipe(
    filter(token => token !== null),
    take(1),
    switchMap(token => next(addToken(request, token as string)))
  );
}

// Check if the request is a refresh token request
function isRefreshTokenRequest(request: any): boolean {
  return request.url.includes('refresh-token');
}

// Check if the request is an auth request (login, register, etc.)
function isAuthRequest(request: any): boolean {
  return request.url.includes('login') || 
         request.url.includes('register') || 
         request.url.includes('verify-otp') || 
         request.url.includes('forgot-password');
}