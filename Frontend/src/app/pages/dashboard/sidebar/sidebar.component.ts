import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon'; 
import { Router } from '@angular/router';

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

  constructor(private router: Router) {}

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
    console.log('Logging out...');
    this.router.navigate(['/login']);
  }
}
