import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { JwtService } from '../../service/jwt.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex h-screen w-screen items-center justify-center bg-gray-900">
      <div class="text-center">
        <div class="w-16 h-16 border-4 border-yellow-400 border-t-transparent rounded-full animate-spin mx-auto"></div>
        <p class="mt-4 text-yellow-400 text-xl">Completing authentication...</p>
      </div>
    </div>
  `,
  styles: []
})
export class OAuthCallbackComponent implements OnInit {
  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private jwtService: JwtService
  ) {}

  ngOnInit(): void {
    console.log('OAuth callback component initialized');
    
    // Check for token in URL or success parameter
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const success = params['login'] === 'success';
      
      console.log('OAuth callback params:', { token, success });
      
      if (token) {
        // If we have a token in the URL, save it and validate
        console.log('JWT token found in URL, saving to localStorage');
        localStorage.setItem('jwt', token);
        this.validateAndNavigate(token);
      } else if (success) {
        // If we have success flag but no token in URL, check if token is in cookies
        // and try to extract it for localStorage consistency
        this.handleSuccessfulLogin();
      } else {
        // If no success or token, try to check authentication status
        this.checkAuthStatus();
      }
    });
  }

  private validateAndNavigate(token: string): void {
    console.log('Validating OAuth token');
    this.jwtService.validateOAuthToken(token).subscribe({
      next: (response) => {
        console.log('Token validation response:', response);
        if (response && response.valid) {
          // Ensure username from token is available
          const username = response.username;
          if (username) {
            console.log('Valid token for user:', username);
          }
          this.router.navigate(['/dashboard']);
        } else {
          console.error('Token validation failed');
          this.router.navigate(['/login']);
        }
      },
      error: (error) => {
        console.error('Error validating token:', error);
        this.router.navigate(['/login']);
      }
    });
  }

  private handleSuccessfulLogin(): void {
    console.log('Handling successful OAuth login');
    
    // Try to get user info which might include the token
    this.jwtService.getUserInfo().subscribe({
      next: (response) => {
        console.log('User info response:', response);
        
        if (response.token) {
          // If token is in the response, save it to localStorage
          console.log('JWT token found in user info, saving to localStorage');
          localStorage.setItem('jwt', response.token);
        }
        
        // Continue with auth validation
        this.jwtService.syncTokenState().subscribe(valid => {
          if (valid) {
            this.router.navigate(['/dashboard']);
          } else {
            this.router.navigate(['/login']);
          }
        });
      },
      error: () => {
        // Failed to get user info, but might still be authenticated via cookies
        this.checkAuthStatus();
      }
    });
  }

  private checkAuthStatus(): void {
    console.log('Checking auth status in OAuth callback');
    this.jwtService.checkAuth().subscribe(isAuthenticated => {
      if (isAuthenticated) {
        this.router.navigate(['/dashboard']);
      } else {
        this.router.navigate(['/login']);
      }
    });
  }
}