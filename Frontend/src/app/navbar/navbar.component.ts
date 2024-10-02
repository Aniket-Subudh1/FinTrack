import { Component, HostListener } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
  standalone: true,
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    CommonModule
  ]
})
export class NavbarComponent {
  isMobile: boolean = false;
  scrollPosition: number = 0;

  constructor(private breakpointObserver: BreakpointObserver, private router: Router) {
    this.observeScreenSize();
  }

  observeScreenSize() {
    this.breakpointObserver.observe([Breakpoints.Small, Breakpoints.Handset])
      .subscribe(result => {
        this.isMobile = result.matches;
      });
  }

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.scrollPosition = window.pageYOffset;
  }

  navigateTo(route: string) {
    this.router.navigate([route]);
  }

  toggleMenu() {
    console.log("Menu toggled");
  }
}
