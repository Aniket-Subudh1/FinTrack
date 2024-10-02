import { Component } from '@angular/core';
import { NavbarComponent } from './../navbar/navbar.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NavbarComponent], // Import the NavbarComponent
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'] // Corrected the property name to styleUrls
})
export class DashboardComponent {
  // Logic for the dashboard can be added here if needed
}
