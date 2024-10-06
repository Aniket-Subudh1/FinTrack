import { Component, AfterViewInit, ElementRef, ViewChild, OnInit, ChangeDetectorRef } from '@angular/core';
import { Application } from '@splinetool/runtime'; 
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon'; 
import { JwtService } from '../../service/jwt.service';  
import { CommonModule } from '@angular/common'; 
import { Router } from '@angular/router';  

@Component({
  selector: 'app-signup',
  standalone: true,
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css'],
  imports: [
    MatIconModule,  
    ReactiveFormsModule,  
    CommonModule,
    FormsModule
  ]
})
export class SignUpComponent implements AfterViewInit, OnInit {

  showPassword: boolean = false;
  showConfirmPassword: boolean = false;
  registerForm!: FormGroup;  
  showModal: boolean = false;  
  otp: string = '';  
  otpVerified: boolean = false;  
  timer: number = 120;  
  isLoading: boolean = false;

  @ViewChild('canvas3d', { static: true }) canvas3d!: ElementRef<HTMLCanvasElement>;  

  constructor(
    private fb: FormBuilder,
    private service: JwtService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}  

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      confirmPassword: ['', Validators.required]
    }, {
      validators: this.passwordMatchValidator  
    });
  }

  passwordMatchValidator(formGroup: AbstractControl): ValidationErrors | null {
    const password = formGroup.get('password')?.value;
    const confirmPassword = formGroup.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { mismatch: true };
  }

  togglePasswordVisibility(field: 'password' | 'confirmPassword'): void {
    if (field === 'password') {
      this.showPassword = !this.showPassword;
    } else if (field === 'confirmPassword') {
      this.showConfirmPassword = !this.showConfirmPassword;
    }
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.isLoading = true;
      this.submitForm();  
    } else {
      console.log('Form is invalid');
    }
  }

  submitForm(): void {
    this.service.register(this.registerForm.value).subscribe(
      (response: any) => {
        console.log('Registration successful, awaiting OTP verification:', response);
        this.showModal = true;
        this.startTimer();  
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      (error: any) => {
        console.error('Error during registration:', error);
        this.isLoading = false;
      }
    );
  }

  verifyOtp(): void {
    this.isLoading = true;
    const otpRequest = {
      email: this.registerForm.value.email,
      otp: this.otp
    };

    this.service.verifyOtp(otpRequest).subscribe(
      (response: any) => {
        console.log('OTP verification successful:', response);
        this.otpVerified = true;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      (error: any) => {
        console.error('OTP verification failed:', error);
        this.isLoading = false;
      }
    );
  }

  resendOtp(): void {
    this.isLoading = true;
    this.service.resendOtp(this.registerForm.value.email).subscribe(
      (response: any) => {
        console.log('OTP resent successfully:', response);
        this.timer = 120;
        this.startTimer();
        this.isLoading = false;
      },
      (error: any) => {
        console.error('Error resending OTP:', error);
        this.isLoading = false;
      }
    );
  }

  redirectToLogin(): void {
    console.log('Redirecting to login page');
    this.showModal = false;
    this.router.navigateByUrl('/login').then(
      success => console.log('Navigation Success:', success),
      error => console.error('Navigation Error:', error)
    );
  }

  ngAfterViewInit(): void {
    const app = new Application(this.canvas3d.nativeElement);
    app.load('https://prod.spline.design/mEfZs9zaxqVlMcyO/scene.splinecode');
  }

  startTimer(): void {
    const intervalId = setInterval(() => {
      this.timer--;
      if (this.timer === 0) {
        clearInterval(intervalId);
      }
    }, 1000);
  }
}
