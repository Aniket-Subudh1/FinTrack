// transaction.component.ts
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { Router } from '@angular/router';

interface Transaction {
  id: string;
  amount: number;
  category: string;
  type: 'Income' | 'Expense';
  date: string;
}

@Component({
  selector: 'app-transaction',
  templateUrl: './transaction.component.html',
  styleUrls: ['./transaction.component.css'],
  standalone: true,
  imports: [SidebarComponent,CommonModule,FormsModule],
})
export class TransactionComponent implements OnInit {
  isSidebarOpen: boolean = true;
  transactions: Transaction[] = [];
  totalIncome = 0;
  totalExpense = 0;
  remainingBalance = 0;
  
  constructor(private router: Router) {}

  ngOnInit() {
    // Initial data loading
    this.loadTransactions();
    this.calculateTotals();
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  private loadTransactions() {
    // Mock data - replace with your actual data service
    this.transactions = [
      {
        id: '1',
        amount: 1000,
        category: 'Salary',
        type: 'Income',
        date: new Date().toLocaleDateString()
      },
      {
        id: '2',
        amount: 500,
        category: 'Groceries',
        type: 'Expense',
        date: new Date().toLocaleDateString()
      }
    ];
  }

  private calculateTotals() {
    this.totalIncome = this.transactions
      .filter(t => t.type === 'Income')
      .reduce((sum, t) => sum + t.amount, 0);
    
    this.totalExpense = this.transactions
      .filter(t => t.type === 'Expense')
      .reduce((sum, t) => sum + t.amount, 0);
    
    this.remainingBalance = this.totalIncome - this.totalExpense;
  }

  // Format currency with Indian Rupee symbol
  formatCurrency(amount: number): string {
    return `₹${amount.toLocaleString('en-IN')}`;
  }
}