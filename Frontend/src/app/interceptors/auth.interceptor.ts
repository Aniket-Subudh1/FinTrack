import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError,switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private router: Router) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    console.log('Intercepting request to:', request.url);
    request = request.clone({
      withCredentials: true
    });

    const token = localStorage.getItem('jwt');
    if (token) {
      console.log('Adding JWT token from localStorage to request');
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    } else {
      console.log('No JWT token in localStorage');
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('HTTP error intercepted:', {
          url: request.url,
          status: error.status,
          message: error.message
        });

        // Handle authentication errors
        if (error.status === 401) {
          console.log('401 Unauthorized response - redirecting to login');
          localStorage.removeItem('jwt');
          this.router.navigate(['/login']);
        }

        return throwError(() => error);
      })
    );
  }
}
