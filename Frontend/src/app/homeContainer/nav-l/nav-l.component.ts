import { Component, HostListener } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-nav-l',
  standalone: true,
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    CommonModule
  ],
  templateUrl: './nav-l.component.html',
  styleUrl: './nav-l.component.css'
})
export class NavLComponent {
  isMobile: boolean = false;
  scrollPosition: number = 0;

  constructor(private breakpointObserver: BreakpointObserver, private router: Router) {
    this.observeScreenSize();
}
observeScreenSize() {
  this.breakpointObserver.observe([Breakpoints.Small, Breakpoints.Handset])
    .subscribe(result => {
      this.isMobile = result.matches;
      console.log('Is mobile:', this.isMobile); 
    });
}

@HostListener('window:scroll', [])
onWindowScroll() {
  this.scrollPosition = window.pageYOffset;
  console.log('Current scroll position:', this.scrollPosition); 
}

navigateTo(route: string) {
  this.router.navigate([route]);
}

toggleMenu() {
  console.log("Menu toggled");
}
}
