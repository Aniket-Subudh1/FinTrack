import { Component, Input } from '@angular/core';
import { CountUpModule } from 'ngx-countup';  // Make sure to import CountUpModule

@Component({
  selector: 'app-custom-counter',
  standalone: true,
  imports: [CountUpModule],  
  templateUrl: './custom-counter.component.html',
  styleUrls: ['./custom-counter.component.css'],
})
export class CustomCounterComponent {
  @Input() before: string = '';
  @Input() after: string = '';
  @Input() counter: number = 0;
  @Input() subtitle: string = '';
  @Input() decimals: number = 0;
}
