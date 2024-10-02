import { Component } from '@angular/core';
import { NavbarComponent } from '../navbar/navbar.component';
import { Section1Component } from '../section1/section1.component';


@Component({
  selector: 'app-home',
  standalone: true,
  imports: [NavbarComponent,Section1Component],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

}
