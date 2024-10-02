import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Section2Component } from './section2.component';

describe('Section2Component', () => {
  let component: Section2Component;
  let fixture: ComponentFixture<Section2Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Section2Component]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Section2Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
