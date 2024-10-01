import { Component, HostListener } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
  standalone: true,  // Standalone component
  imports: [
    MatToolbarModule,  // Toolbar module from Angular Material
    MatButtonModule,   // Button module from Angular Material
    MatIconModule,     // Icon module from Angular Material
    RouterModule,
    CommonModule       // RouterModule for navigation
  ]
})
export class NavbarComponent {
  isMobile: boolean = false;
  scrollPosition: number = 0;

  constructor(private breakpointObserver: BreakpointObserver) {
    this.observeScreenSize();
  }

  // Observe screen size for responsive behavior
  observeScreenSize() {
    this.breakpointObserver.observe([Breakpoints.Small, Breakpoints.Handset])
      .subscribe(result => {
        this.isMobile = result.matches;
      });
  }

  // Listen to window scroll to apply dynamic background styling
  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.scrollPosition = window.pageYOffset;
  }

  // Navigation function
  navigateTo(route: string) {
    window.location.href = route;
  }

  // Placeholder for mobile menu toggle
  toggleMenu() {
    console.log("Menu toggled");
  }
}
