import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon'; 
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
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

  constructor(private router: Router, private jwtService: JwtService, private dialog: MatDialog) {}

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
    const dialogRef = this.dialog.open(LogoutConfirmDialog, {
      width: '300px'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.jwtService.logout().subscribe({
          next: () => {
            this.router.navigate(['/login'], { replaceUrl: true });
          },
          error: (error) => {
            console.error('Logout error:', error);
            this.router.navigate(['/login'], { replaceUrl: true });
          }
        });
      }
    });
  }
}

@Component({
  selector: 'app-logout-confirm-dialog',
  standalone: true,
  imports: [CommonModule], // Add CommonModule here
  template: `
    <div class="p-4 text-center">
      <p class="text-lg font-semibold">Are you sure you want to logout?</p>
      <div class="text-4xl mt-4" [ngClass]="{ 'text-sad': isHovered, 'text-smile': !isHovered }">
        {{ isHovered ? '😞' : '😊' }}
      </div>
      <div class="flex justify-around mt-6">
        <button (mouseover)="isHovered = true" (mouseleave)="isHovered = false" (click)="confirmLogout(true)" class="px-4 py-2 bg-red-500 text-white rounded-md">Yes</button>
        <button (click)="confirmLogout(false)" class="px-4 py-2 bg-gray-300 rounded-md">No</button>
      </div>
    </div>
  `,
  styles: [
    `.text-smile { color: green; }`,
    `.text-sad { color: red; }`
  ]
})
export class LogoutConfirmDialog {
  isHovered = false;

  constructor(private dialogRef: MatDialogRef<LogoutConfirmDialog>) {}

  confirmLogout(choice: boolean): void {
    this.dialogRef.close(choice);
  }
}
