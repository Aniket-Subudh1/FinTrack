import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { JwtService } from './../../service/jwt.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Application } from '@splinetool/runtime';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  imports: [
    ReactiveFormsModule,
    FormsModule,
    CommonModule,
    MatIconModule
  ]
})
export class LoginComponent implements OnInit, AfterViewInit {

  @ViewChild('canvas3d', { static: true }) canvas3d!: ElementRef<HTMLCanvasElement>;
  loginForm!: FormGroup;
  isLoading: boolean = false;
  isModalLoading: boolean = false;
  showForgotPasswordModal: boolean = false;
  forgotPasswordEmail: string = '';
  otp: string = '';
  newPassword: string = '';
  otpSent: boolean = false;
  otpVerified: boolean = false;
  timer: number = 120;
  showPassword: boolean = false;

  constructor(
    private fb: FormBuilder, 
    private service: JwtService, 
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });

    // Check for query params (from OAuth2 redirect)
    this.route.queryParams.subscribe(params => {
      // No need to extract token - cookies are handled automatically
      if (params['login'] === 'success') {
        this.router.navigate(['/dashboard']);
      }
    });
  }

  ngAfterViewInit(): void {
    const app = new Application(this.canvas3d.nativeElement);
    app.load('https://prod.spline.design/mEfZs9zaxqVlMcyO/scene.splinecode');
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  loginWithGoogle(): void {
    this.service.initiateGoogleLogin();
  }
  
  showForgotPassword(event: Event): void {
    event.preventDefault();
    this.showForgotPasswordModal = true;
    this.forgotPasswordEmail = '';
    this.otp = '';
    this.newPassword = '';
    this.otpSent = false;
    this.otpVerified = false;
    this.timer = 120;
  }

  sendOtp(): void {
    this.isModalLoading = true;
    this.service.sendForgotPasswordOtp(this.forgotPasswordEmail).subscribe(
      (response: any) => {
        console.log('OTP sent successfully:', response);
        this.otpSent = true;
        this.isModalLoading = false;
        this.startTimer();
      },
      (error: any) => {
        console.error('Error sending OTP:', error);
        alert('Error sending OTP: ' + (error.error?.message || 'Unknown error'));
        this.isModalLoading = false;
      }
    );
  }

  startTimer(): void {
    this.timer = 120;
    const intervalId = setInterval(() => {
      this.timer--;
      if (this.timer === 0) {
        clearInterval(intervalId);
      }
    }, 1000);
  }

  verifyOtp(): void {
    this.isModalLoading = true;
    const resetPasswordRequest = {
      email: this.forgotPasswordEmail,
      otp: this.otp,
      newPassword: this.newPassword
    };

    this.service.verifyForgotPasswordOtp(resetPasswordRequest).subscribe(
      (response: any) => {
        console.log('Password reset successful:', response);
        this.otpVerified = true;
        this.isModalLoading = false;
      },
      (error: any) => {
        console.error('OTP verification failed:', error);
        alert('OTP verification failed: ' + (error.error?.message || 'Unknown error'));
        this.isModalLoading = false;
      }
    );
  }

  resendOtp(): void {
    this.isModalLoading = true;
    this.service.resendForgotPasswordOtp(this.forgotPasswordEmail).subscribe(
      (response: any) => {
        console.log('OTP resent successfully:', response);
        this.timer = 120;
        this.startTimer();
        this.isModalLoading = false;
      },
      (error: any) => {
        console.error('Error resending OTP:', error);
        alert('Error resending OTP: ' + (error.error?.message || 'Unknown error'));
        this.isModalLoading = false;
      }
    );
  }

  closeForgotPasswordModal(): void {
    this.showForgotPasswordModal = false;
  }

  redirectToLogin(): void {
    this.showForgotPasswordModal = false;
    alert('Password reset successful! You can now log in with your new password.');
  }

  submitForm(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      const loginData = this.loginForm.value;

      this.service.login(loginData).subscribe(
        (response: any) => {
          // No need to manually store token - it's in the cookie
          this.router.navigateByUrl('/dashboard').then(() => {
            this.isLoading = false;
          });
        },
        (error: any) => {
          console.error('Login failed:', error);
          alert('Login failed: ' + (error.error?.message || 'Unknown error'));
          this.isLoading = false;
        }
      );
    } else {
      console.log('Form is invalid:', this.loginForm.errors);
      alert('Please fill out all required fields correctly.');
    }
  }
}