import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private router: Router) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Always add withCredentials to send cookies with every request
    request = request.clone({
      withCredentials: true
    });
    
    // Check for JWT token in localStorage (backward compatibility)
    const token = localStorage.getItem('jwt');
    if (token) {
      console.log('Adding JWT token from localStorage to request');
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Handle authentication errors
        if (error.status === 401) {
          console.log('401 Unauthorized response - redirecting to login');
          localStorage.removeItem('jwt'); // Clean up any local storage
          this.router.navigate(['/login']);
        }
        
        // For debugging - log all errors
        console.error('HTTP request error:', error);
        
        return throwError(() => error);
      })
    );
  }
}