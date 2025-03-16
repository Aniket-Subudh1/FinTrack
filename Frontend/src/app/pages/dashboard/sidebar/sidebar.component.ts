import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon'; 
import { Router } from '@angular/router';
import { JwtService } from '../../../service/jwt.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css'] 
})
export class SidebarComponent {
  @Input() isSidebarOpen: boolean = true; 
  @Output() toggleSidebar = new EventEmitter<void>(); 

  constructor(private router: Router, private jwtService: JwtService) {}

  toggle(): void {
    this.toggleSidebar.emit(); 
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }

  isActive(route: string): boolean {
    return this.router.url === route;
  }

  logout(): void {
    console.log('Logout button clicked');
    this.jwtService.logout().subscribe({
      next: (response) => {
        console.log('Logout API call successful:', response);
        console.log('Token after logout:', localStorage.getItem('jwt'));
        this.router.navigate(['/login'], { replaceUrl: true });
      },
      error: (error) => {
        console.error('Logout API call failed:', error);
        console.error('Status:', error.status);
        console.error('Message:', error.message);
        console.error('Error object:', error);
        console.log('Token after logout error:', localStorage.getItem('jwt'));
        this.router.navigate(['/login'], { replaceUrl: true });
      }
    });
  }
  
}