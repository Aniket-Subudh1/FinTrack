import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomCounterComponent } from './custom-counter.component';

describe('CustomCounterComponent', () => {
  let component: CustomCounterComponent;
  let fixture: ComponentFixture<CustomCounterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomCounterComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CustomCounterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
