import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-section1',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './section1.component.html',
  styleUrl: './section1.component.css'
})

export class Section1Component {
  constructor (private router: Router) {
    
  }
  navigateTo(route: string) {
    this.router.navigate([route]);
  }
  

}
