import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { JwtService } from './../../service/jwt.service';
import { Router } from '@angular/router';
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
    FormsModule,  // Required for [(ngModel)]
    CommonModule,
    MatIconModule
  ]
})
export class LoginComponent implements OnInit, AfterViewInit {

  @ViewChild('canvas3d', { static: true }) canvas3d!: ElementRef<HTMLCanvasElement>;
  loginForm!: FormGroup;
  isLoading: boolean = false;  // Loader for login
  isModalLoading: boolean = false;  // Loader for modal actions
  showForgotPasswordModal: boolean = false;  // Controls modal visibility
  forgotPasswordEmail: string = '';  // Stores the email for password reset
  otp: string = '';  // OTP entered by the user
  newPassword: string = '';  // New password entered by the user
  otpSent: boolean = false;  // Tracks whether OTP has been sent
  otpVerified: boolean = false;  // Tracks whether OTP is verified
  timer: number = 120;  // Countdown timer for OTP validity
  showPassword: boolean = false;  // Password visibility toggle

  constructor(private fb: FormBuilder, private service: JwtService, private router: Router) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  ngAfterViewInit(): void {
    const app = new Application(this.canvas3d.nativeElement);
    app.load('https://prod.spline.design/mEfZs9zaxqVlMcyO/scene.splinecode');
  }

  // Toggle password visibility
  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  loginWithGoogle(): void {
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  }
  
  // After OAuth2 login success
  storeJwtToken(jwtToken: string): void {
    localStorage.setItem('jwt', jwtToken);
    this.router.navigateByUrl('/dashboard');  // Navigate to the dashboard or home page
  }
  
  // Show Forgot Password Modal
  showForgotPassword(event: Event): void {
    event.preventDefault();  // Prevent default action of the link
    this.showForgotPasswordModal = true;
    // Reset modal state
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
        this.startTimer();  // Start countdown timer
      },
      (error: any) => {
        console.error('Error sending OTP:', error);
        alert('Error sending OTP: ' + (error.error?.message || 'Unknown error'));
        this.isModalLoading = false;
      }
    );
  }

  // Start countdown timer for OTP
  startTimer(): void {
    this.timer = 120;  // Reset timer
    const intervalId = setInterval(() => {
      this.timer--;
      if (this.timer === 0) {
        clearInterval(intervalId);  // Stop timer when it reaches zero
      }
    }, 1000);
  }

  // Verify OTP and reset password
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

  // Resend OTP
  resendOtp(): void {
    this.isModalLoading = true;
    this.service.resendForgotPasswordOtp(this.forgotPasswordEmail).subscribe(
      (response: any) => {
        console.log('OTP resent successfully:', response);
        this.timer = 120;  // Reset timer after resending OTP
        this.startTimer();  // Start countdown again
        this.isModalLoading = false;
      },
      (error: any) => {
        console.error('Error resending OTP:', error);
        alert('Error resending OTP: ' + (error.error?.message || 'Unknown error'));
        this.isModalLoading = false;
      }
    );
  }

  // Close Forgot Password Modal
  closeForgotPasswordModal(): void {
    this.showForgotPasswordModal = false;
  }

  // Redirect to login after successful password reset
  redirectToLogin(): void {
    this.showForgotPasswordModal = false;
    // Optionally reset the form or navigate elsewhere
    alert('Password reset successful! You can now log in with your new password.');
  }

  // Submit Login Form
  submitForm(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      const loginData = this.loginForm.value;
      console.log('Form Values:', loginData);

      this.service.login(loginData).subscribe(
        (response: any) => {
          console.log('Login Response:', response);
          if (response && response.jwtToken) {
            localStorage.setItem('jwt', response.jwtToken);
            this.router.navigateByUrl('/dashboard').then(() => {
              this.isLoading = false;
            });
          } else {
            console.error('JWT not found in response:', response);
            alert('Login failed: JWT token not found in response.');
            this.isLoading = false;
          }
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
