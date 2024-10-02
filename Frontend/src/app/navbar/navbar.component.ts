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
  standalone: true,  
  imports: [
    MatToolbarModule,  
    MatButtonModule,  
    MatIconModule,     
    RouterModule,
    CommonModule       
  ]
})
export class NavbarComponent {
  isMobile: boolean = false;
  scrollPosition: number = 0;

  constructor(private breakpointObserver: BreakpointObserver) {
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
    window.location.href = route; 
  }


  toggleMenu() {
    console.log("Menu toggled");
  }
}
