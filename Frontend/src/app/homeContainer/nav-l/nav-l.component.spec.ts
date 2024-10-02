import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NavLComponent } from './nav-l.component';

describe('NavLComponent', () => {
  let component: NavLComponent;
  let fixture: ComponentFixture<NavLComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavLComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NavLComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
