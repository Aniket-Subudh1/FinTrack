import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { Router } from '@angular/router';

interface Transaction {
  id?: string;
  date: Date;
  amount: number;
  category: string;
  type: 'income' | 'expense';
  description?: string;
}

interface SortConfig {
  field: string;
  direction: 'asc' | 'desc';
}

@Component({
  selector: 'app-transaction',
  templateUrl: './transaction.component.html',
  styleUrls: ['./transaction.component.css'],
  standalone: true,
  imports: [FormsModule, CommonModule, SidebarComponent],
})
export class TransactionComponent implements OnInit {
  isSidebarOpen: boolean = true;
  amount: number = 0;
  category: string = '';
  type: 'income' | 'expense' = 'expense';
  description: string = '';
  categories: string[] = [
    'Salary', 'Food', 'Transport', 'Entertainment', 'Utilities', 
    'Rent', 'Shopping', 'Investment', 'Freelance', 'Medical'
  ];
  transactions: Transaction[] = [
    {
      id: '1',
      date: new Date(2025, 2, 10),
      amount: 50000,
      category: 'Salary',
      type: 'income',
      description: 'Monthly salary'
    },
    {
      id: '2',
      date: new Date(2025, 2, 12),
      amount: 2500,
      category: 'Food',
      type: 'expense',
      description: 'Grocery shopping'
    },
    {
      id: '3',
      date: new Date(2025, 2, 14),
      amount: 1000,
      category: 'Transport',
      type: 'expense',
      description: 'Fuel'
    },
    {
      id: '4',
      date: new Date(2025, 2, 15),
      amount: 10000,
      category: 'Freelance',
      type: 'income',
      description: 'Website project'
    }
  ];
  successBubble: { show: boolean; message: string } = { show: false, message: '' };
  filterToast: { show: boolean; message: string } = { show: false, message: '' };
  isLoading: boolean = false;
  totalIncome: number = 0;
  totalExpense: number = 0;
  netBalance: number = 0;
  
  // Actual filter values (applied)
  filterCategory: string = '';
  filterType: 'all' | 'income' | 'expense' = 'all';
  searchDate: string = '';
  
  // Temporary filter values (not yet applied)
  tempFilterCategory: string = '';
  tempFilterType: 'all' | 'income' | 'expense' = 'all';
  tempSearchDate: string = '';
  
  // Sort field and direction
  sortField: string = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Multiple sort configuration
  sortConfigs: SortConfig[] = [{ field: 'date', direction: 'desc' }];
  
  // Track modal state
  showTrackerModal: boolean = false;
  
  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }
  
  toggleTrackerModal(): void {
    this.showTrackerModal = !this.showTrackerModal;
    
    if (this.showTrackerModal) {
      // Simulate loading
      this.isLoading = true;
      setTimeout(() => {
        this.calculateTotals();
        this.sortTransactions();
        this.isLoading = false;
      }, 500);
      
      this.tempFilterCategory = this.filterCategory;
      this.tempFilterType = this.filterType;
      this.tempSearchDate = this.searchDate;
    }
  }
  
  constructor() {}

  openCalendar(dateInput: HTMLInputElement): void {
    dateInput.showPicker();
  }
 
  addTransaction(): void {
    if (this.amount > 0 && this.category.trim()) {
      // Simulate loading
      this.isLoading = true;
      
      setTimeout(() => {
        const newTransaction: Transaction = {
          id: (this.transactions.length + 1).toString(),
          date: new Date(),
          amount: this.amount,
          category: this.category,
          type: this.type,
          description: this.description
        };
        
        this.transactions.push(newTransaction);
        this.showSuccessBubble(`Added ${this.type === 'income' ? 'Income' : 'Expense'} - ${this.category}: ₹ ${this.amount}`);
        this.calculateTotals();
        this.sortTransactions();
        this.resetFormFields();
        this.isLoading = false;
      }, 800);
    }
  }

  private showSuccessBubble(message: string): void {
    this.successBubble = { show: true, message };
    setTimeout(() => {
      this.successBubble.show = false;
    }, 3000);
  }
  
  private showFilterToast(message: string): void {
    this.filterToast = { show: true, message };
    setTimeout(() => {
      this.filterToast.show = false;
    }, 3000);
  }

  private resetFormFields(): void {
    this.amount = 0;
    this.category = '';
    this.description = '';
  }

  applyFilters(): void {
    this.filterCategory = this.tempFilterCategory;
    this.filterType = this.tempFilterType;
    this.searchDate = this.tempSearchDate;
    this.calculateTotals();
    this.showFilterToast('Filters applied successfully');
  }

  calculateTotals(): void {
    const filteredTransactions = this.getFilteredTransactions();
    this.totalIncome = filteredTransactions
      .filter(transaction => transaction.type === 'income')
      .reduce((sum, transaction) => sum + transaction.amount, 0);
    
    this.totalExpense = filteredTransactions
      .filter(transaction => transaction.type === 'expense')
      .reduce((sum, transaction) => sum + transaction.amount, 0);
    
    this.netBalance = this.totalIncome - this.totalExpense;
  }

  getFilteredTransactions(): Transaction[] {
    return this.transactions.filter(transaction => {
      // Filter by category if one is selected
      const categoryMatch = !this.filterCategory || transaction.category === this.filterCategory;
      
      // Filter by type if one is selected
      const typeMatch = this.filterType === 'all' || transaction.type === this.filterType;
      
      // Filter by date if one is entered
      let dateMatch = true;
      if (this.searchDate) {
        const searchDate = new Date(this.searchDate);
        const transactionDate = new Date(transaction.date);
        dateMatch = transactionDate.toDateString() === searchDate.toDateString();
      }
      
      return categoryMatch && typeMatch && dateMatch;
    });
  }

  clearFilters(): void {
    this.filterCategory = '';
    this.filterType = 'all';
    this.searchDate = '';
    this.tempFilterCategory = '';
    this.tempFilterType = 'all';
    this.tempSearchDate = '';
    this.calculateTotals();
    this.showFilterToast('Filters cleared');
  }

  formatDate(date: Date): string {
    return date.toLocaleDateString();
  }

  sortTransactions(): void {
    if (this.sortConfigs.length > 0) {
      this.transactions.sort((a, b) => {
        // Apply multiple sort fields in order
        for (const config of this.sortConfigs) {
          let comparison = 0;
          
          if (config.field === 'date') {
            comparison = new Date(a.date).getTime() - new Date(b.date).getTime();
          } else if (config.field === 'amount') {
            comparison = a.amount - b.amount;
          } else if (config.field === 'category') {
            comparison = a.category.localeCompare(b.category);
          } else if (config.field === 'type') {
            comparison = a.type.localeCompare(b.type);
          }
          
          if (comparison !== 0) {
            return config.direction === 'asc' ? comparison : -comparison;
          }
        }
        
        return 0; // If all comparisons are equal
      });
    }
  }

  toggleSort(field: string): void {
    // Update the legacy sort field and direction
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    
    // Also update the sortConfigs array
    this.sortConfigs = this.sortConfigs.filter(config => config.field !== field);
    
    // Then add it as the primary sort
    this.sortConfigs.unshift({
      field: field, 
      direction: this.sortDirection
    });
    
    // Keep only up to 3 sort fields
    if (this.sortConfigs.length > 3) {
      this.sortConfigs.pop();
    }
    
    this.sortTransactions();
  }

  getSortIcon(field: string): string {
    const config = this.sortConfigs.find(config => config.field === field);
    if (!config) return '↕️';
    return config.direction === 'asc' ? `↑` : `↓`;
  }

  getAmountClass(type: string): string {
    return type === 'income' ? 'text-green-400' : 'text-red-400';
  }

  ngOnInit(): void {
    // Calculate initial totals
    this.calculateTotals();
    
    // Initialize temporary filter values
    this.tempFilterCategory = this.filterCategory;
    this.tempFilterType = this.filterType;
    this.tempSearchDate = this.searchDate;
  }
}