import { Component, AfterViewInit, ElementRef, ViewChild, OnInit } from '@angular/core';
import { Application } from '@splinetool/runtime';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { JwtService } from './../../service/jwt.service';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  imports: [
    MatIconModule,
    ReactiveFormsModule,
    CommonModule
  ]
})
export class LoginComponent implements AfterViewInit, OnInit {

  @ViewChild('canvas3d', { static: true }) canvas3d!: ElementRef<HTMLCanvasElement>;
  loginForm!: FormGroup;
  isLoading: boolean = false;  // New flag to control loader visibility

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

  submitForm() {
    if (this.loginForm.valid) {
      this.isLoading = true;  // Show loader when the login request starts
      console.log(this.loginForm.value);  // Debugging - Check form data before submission
      this.service.login(this.loginForm.value).subscribe(
        (response: any) => {
          console.log('Login response:', response);  // Debugging - Check response from backend
          if (response && response.jwtToken) {  // Check if JWT is present
            localStorage.setItem('jwt', response.jwtToken);  // Store JWT token
            this.router.navigateByUrl('/dashboard').then(() => {
              this.isLoading = false;  // Hide loader after navigation is successful
            });
          } else {
            console.error('JWT not found in response:', response);
            this.isLoading = false;  // Hide loader if JWT is missing
          }
        },
        (error: any) => {
          console.error('Login failed:', error);  // Handle error case
          this.isLoading = false;  // Hide loader on error
        }
      );
    }
  }

}
