import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { JwtService } from '../service/jwt.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  
  constructor(private jwtService: JwtService, private router: Router) {}
  
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    console.log('AuthGuard: Checking route access to', state.url);
    
    // Always check auth status with server
    return this.jwtService.checkAuth().pipe(
      tap(isValid => {
        console.log('Authentication check result:', isValid);
        if (!isValid) {
          console.log('Authentication check failed, redirecting to login');
          // Always clear localStorage
          localStorage.removeItem('jwt');
          console.log('JWT token removed from localStorage');
          
          this.router.navigate(['/login'], { 
            queryParams: { returnUrl: state.url },
            replaceUrl: true 
          });
        }
      }),
      map(isValid => {
        const result = !!isValid;
        console.log('Final auth guard result:', result);
        return result;
      }), 
      catchError(error => {
        console.error('Auth check failed with error:', error);
        localStorage.removeItem('jwt');
        console.log('JWT token removed from localStorage due to error');
        
        this.router.navigate(['/login'], { 
          queryParams: { returnUrl: state.url },
          replaceUrl: true 
        });
        return of(false);
      })
    );
  }
}