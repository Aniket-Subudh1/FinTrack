import { ApplicationConfig, inject } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient, withInterceptors, HttpRequest } from '@angular/common/http';
import { routes } from './app.routes';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { provideAnimations } from '@angular/platform-browser/animations';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        (request: HttpRequest<unknown>, next) => {
          const router = inject(Router);
          const authInterceptor = new AuthInterceptor(router);
          return authInterceptor.intercept(request, {
            handle: next
          });
        }
      ])
    ),
    provideAnimations()
  ]
};
