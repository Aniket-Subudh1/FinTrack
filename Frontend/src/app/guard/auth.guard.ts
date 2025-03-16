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
    return this.jwtService.checkAuth().pipe(
      tap(isValid => {
        if (!isValid) {
          localStorage.removeItem('jwt');
          this.router.navigate(['/login'], { replaceUrl: true });
        }
      }),
      map(isValid => isValid),
      catchError(error => {
        console.error('Auth check failed:', error);
        localStorage.removeItem('jwt');
        this.router.navigate(['/login'], { replaceUrl: true });
        return of(false);
      })
    );
  }
}