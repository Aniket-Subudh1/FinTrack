import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { JwtService } from '../service/jwt.service';

export const authGuard: CanActivateFn = (route, state) => {
  const jwtService = inject(JwtService);
  const router = inject(Router);

  if (jwtService.isLoggedIn()) {
    return true;
  }

  // Redirect to login page
  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};