import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { section2Content } from '../utils/section2-content';
import { CustomCounterComponent } from '../custom-counter/custom-counter.component';

@Component({
  selector: 'app-section2',
  standalone: true,
  imports: [CommonModule, CustomCounterComponent],
  templateUrl: './section2.component.html',
  styleUrls: ['./section2.component.css'],
})
export class Section2Component {
  items = section2Content.items;
}
