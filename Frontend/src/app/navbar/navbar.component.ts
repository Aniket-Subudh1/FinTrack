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
<<<<<<< HEAD
  standalone: true,  
  imports: [
    MatToolbarModule,  
    MatButtonModule,  
    MatIconModule,     
    RouterModule,
    CommonModule       
=======
  standalone: true,
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    CommonModule
>>>>>>> 263306760c323a22cc9c5464a424c2effb2d8875
  ]
})
export class NavbarComponent {
  isMobile: boolean = false;
  scrollPosition: number = 0;

  constructor(private breakpointObserver: BreakpointObserver, private router: Router) {
    this.observeScreenSize();
  }

<<<<<<< HEAD
  
=======
>>>>>>> 263306760c323a22cc9c5464a424c2effb2d8875
  observeScreenSize() {
    this.breakpointObserver.observe([Breakpoints.Small, Breakpoints.Handset])
      .subscribe(result => {
        this.isMobile = result.matches;
        console.log('Is mobile:', this.isMobile); 
      });
  }

<<<<<<< HEAD
 
=======
>>>>>>> 263306760c323a22cc9c5464a424c2effb2d8875
  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.scrollPosition = window.pageYOffset;
    console.log('Current scroll position:', this.scrollPosition); 
  }

<<<<<<< HEAD
 
  navigateTo(route: string) {
    window.location.href = route; 
  }


=======
  navigateTo(route: string) {
    this.router.navigate([route]);
  }

>>>>>>> 263306760c323a22cc9c5464a424c2effb2d8875
  toggleMenu() {
    console.log("Menu toggled");
  }
}
