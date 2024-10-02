// app.routes.ts
import { Routes } from '@angular/router';
import { SignUpComponent } from './pages/signup/signup.component'; 
import { HomeComponent} from './pages/home/home.component'; 



export const routes: Routes = [
    { path: 'signup', component: SignUpComponent },
    { path: '', component:  HomeComponent },
   
  
];
