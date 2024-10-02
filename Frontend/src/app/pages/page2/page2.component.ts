import { Component } from '@angular/core';
import { SignUpComponent } from '../../homeContainer/signup/signup.component';
import { NavSComponent } from '../../homeContainer/nav-s/nav-s.component';


@Component({
  selector: 'app-page2',
  standalone: true,
  imports: [SignUpComponent,NavSComponent],
  templateUrl: './page2.component.html',
  styleUrl: './page2.component.css'
})
export class Page2Component {

}
