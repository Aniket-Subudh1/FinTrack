import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NavSComponent } from './nav-s.component';

describe('NavSComponent', () => {
  let component: NavSComponent;
  let fixture: ComponentFixture<NavSComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavSComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NavSComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
