// app.routes.ts
import { Routes } from '@angular/router';
import { SignUpComponent } from './signup/signup.component'; // Import the SignUpComponent
import { LandingPageComponent } from './landing-page/landing-page.component';

export const routes: Routes = [
  { path: 'signup', component: SignUpComponent }, // Add route for signup
  { path: '', component: LandingPageComponent }
];
