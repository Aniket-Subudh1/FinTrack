import { Routes } from '@angular/router';
import { HomeComponent} from './pages/home/home.component';
import { Page2Component } from './pages/page2/page2.component';
import { Page3Component } from './pages/page3/page3.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { ExpenseComponent } from './components/expense/expense.component';
import { ProfileComponent } from './components/profile/profile.component';
import { TransactionComponent } from './components/transaction/transaction.component';
import { IncomeComponent } from './components/income/income.component';
import { AuthGuard } from './guard/auth.guard';
import {ExpenseChartComponent} from './components/expense-chart/expense-chart.component'
import { GoalsComponent } from './components/goals/goals.component';
import { ExpenseReportComponent } from './components/expense-report/expense-report.component';
import { OAuthCallbackComponent } from './components/oauth-callback/oauth-callback.component';

export const routes: Routes = [
    { path: 'signup', component: Page2Component },
    { path: 'login', component: Page3Component },
    { path: '', component: HomeComponent },
    { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'oauth-callback', component: OAuthCallbackComponent },
    {
      path: 'expense',
      component: ExpenseComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'profile',
      component: ProfileComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'transaction',
      component: TransactionComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'income',
      component: IncomeComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'analytics',
      component: ExpenseChartComponent,
      canActivate: [AuthGuard]

    },
    {
      path: 'report',
      component: ExpenseReportComponent,
      canActivate: [AuthGuard]

    },

    { path: 'goals', component: GoalsComponent, canActivate: [AuthGuard] }
];
