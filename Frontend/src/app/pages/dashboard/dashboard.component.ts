import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  imports: [CommonModule, CurrencyPipe]
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

  constructor(private router: Router) {} // Inject Router

  logout(): void {
    // Perform any logout logic here, like clearing authentication tokens
    console.log('User logged out');
    
    // Navigate to the login page
    this.router.navigate(['/login']);
  }
}