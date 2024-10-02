import { Component } from '@angular/core';
import { NavbarComponent } from '../../homeContainer/navbar/navbar.component';
import { Section1Component } from '../../homeContainer/section1/section1.component';
import { Section2Component } from '../../homeContainer/section2/section2.component';



@Component({
  selector: 'app-home',
  standalone: true,
  imports: [NavbarComponent,Section1Component,Section2Component],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

}
