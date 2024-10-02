import { Component, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { Application } from '@splinetool/runtime'; // Import the Spline runtime
import { MatIconModule } from '@angular/material/icon';
import { NavbarComponent } from '../navbar/navbar.component';

@Component({
  selector: 'app-signup',
  standalone: true,
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css'],
  imports: [
    MatIconModule,
    NavbarComponent
  ]
})
export class SignUpComponent implements AfterViewInit {
  showPassword: boolean = false;
  showConfirmPassword: boolean = false;

  @ViewChild('canvas3d', { static: true }) canvas3d!: ElementRef<HTMLCanvasElement>; // Get canvas reference

  ngAfterViewInit(): void {
    const app = new Application(this.canvas3d.nativeElement); // Initialize Spline application
    app.load('https://prod.spline.design/mEfZs9zaxqVlMcyO/scene.splinecode'); // Load Spline scene
  }

  togglePasswordVisibility(field: 'password' | 'confirm-password') {
    if (field === 'password') {
      this.showPassword = !this.showPassword;
    } else {
      this.showConfirmPassword = !this.showConfirmPassword;
    }
  }
}
