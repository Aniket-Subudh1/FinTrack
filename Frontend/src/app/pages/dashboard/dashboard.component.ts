import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';
import { SidebarComponent } from './sidebar/sidebar.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  imports: [CommonModule, CurrencyPipe, SidebarComponent]
})
export class DashboardComponent {
  totalBalance: number = 25000;
  monthlyIncome: number = 4500;
  monthlyExpenses: number = 2800;
  savingsGoal: number = 10000;

  recentTransactions = [
    { description: 'Groceries', amount: -150 },
    { description: 'Salary', amount: 4500 },
    { description: 'Utility Bills', amount: -120 },
    { description: 'Investment', amount: -300 }
  ];

  isSidebarOpen: boolean = true;

  constructor(private router: Router) {}

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  navigateToGoals(): void {
    this.router.navigate(['/goals']);
  }

  logout(): void {
    console.log('User logged out');
    this.router.navigate(['/login']);
  }

  redirectToAddExpense(): void {
    this.router.navigate(['/add-expense']);
  }
}
