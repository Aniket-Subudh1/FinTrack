// app.routes.ts
import { Routes } from '@angular/router';
 

import { HomeComponent} from './pages/home/home.component'; 
import { Page2Component } from './pages/page2/page2.component';
import { Page3Component } from './pages/page3/page3.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AddExpenseComponent } from './pages/add-expense/add-expense.component';



export const routes: Routes = [
    { path: 'signup', component: Page2Component },
    { path: 'login', component:Page3Component  },
    { path: '', component:  HomeComponent },
    { path: 'dashboard', component: DashboardComponent  },
    { path: 'add-expense', component: AddExpenseComponent }
   
  
];
