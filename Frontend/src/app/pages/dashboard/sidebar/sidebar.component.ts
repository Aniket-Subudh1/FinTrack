import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
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
  @Output() toggleSidebar = new EventEmitter<void>();
  isSidebarOpen: boolean = true;

  constructor(private router: Router) {}

  toggle(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
    this.toggleSidebar.emit(); 
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }

  logout(): void {
    console.log('Logging out...');
    this.router.navigate(['/login']);
  }
}
