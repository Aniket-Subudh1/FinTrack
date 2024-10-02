// app.routes.ts
import { Routes } from '@angular/router';
import { SignUpComponent } from './pages/signup/signup.component'; 
import { LoginComponent  } from './pages/login/login.component'; 
import { HomeComponent} from './pages/home/home.component'; 




export const routes: Routes = [
    { path: 'signup', component: SignUpComponent },
    { path: 'login', component:LoginComponent  },
    { path: '', component:  HomeComponent },
   
  
];
