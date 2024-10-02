import { Component } from '@angular/core';
import { NavLComponent } from '../../homeContainer/nav-l/nav-l.component';
import { LoginComponent } from '../../homeContainer/login/login.component';


@Component({
  selector: 'app-page3',
  standalone: true,
  imports: [NavLComponent,LoginComponent],
  templateUrl: './page3.component.html',
  styleUrl: './page3.component.css'
})
export class Page3Component {

}
