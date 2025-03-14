import { Component, OnInit, ElementRef } from '@angular/core';
import { ExpenseService } from '../../service/expense.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { Router } from '@angular/router';

interface Expense {
  id?: string;
  date: Date;
  amount: number;
  category: string;
}

interface SortConfig {
  field: string;
  direction: 'asc' | 'desc';
}

@Component({
  selector: 'app-expense',
  templateUrl: './expense.component.html',
  styleUrls: ['./expense.component.css'],
  standalone: true,
  imports: [FormsModule, CommonModule, SidebarComponent],
})
export class ExpenseComponent implements OnInit {
  isSidebarOpen: boolean = true;
  amount: number = 0;
  category: string = '';
  categories: string[] = [];
  expenses: Expense[] = [];
  successBubble: { show: boolean; message: string } = { show: false, message: '' };
  filterToast: { show: boolean; message: string } = { show: false, message: '' };
  isLoading: boolean = false;
  totalExpenses: number = 0;
  
  // Actual filter values (applied)
  filterCategory: string = '';
  searchDate: string = '';
  
  // Temporary filter values (not yet applied)
  tempFilterCategory: string = '';
  tempSearchDate: string = '';
  
  // Sort field and direction (for backward compatibility)
  sortField: string = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Multiple sort configuration
  sortConfigs: SortConfig[] = [{ field: 'date', direction: 'desc' }];
  
  // Track modal state
  showTrackerModal: boolean = false;
  
  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }
  
  // Toggle expense tracker modal
  toggleTrackerModal(): void {
    this.showTrackerModal = !this.showTrackerModal;
    
    // If opening the modal, refresh the expense data
    if (this.showTrackerModal) {
      this.fetchExpenses();
      
      // Initialize temporary filter values when opening modal
      this.tempFilterCategory = this.filterCategory;
      this.tempSearchDate = this.searchDate;
    }
  }
  
  constructor(private expenseService: ExpenseService, private router: Router) {}

  // Function to open calendar by triggering a click on the date input
  openCalendar(dateInput: HTMLInputElement): void {
    dateInput.showPicker();
  }
 
  addExpense(): void {
    if (this.amount > 0 && this.category.trim()) {
      const expenseRequest = {
        amount: this.amount,
        category: this.category,
      };

      this.isLoading = true;
      this.expenseService.addExpense(expenseRequest).subscribe(
        (response) => {
          console.log('Expense added successfully:', response);
          this.showSuccessBubble(`Added ${this.category}: ₹ ${this.amount}`);
          this.fetchExpenses();
          this.resetFormFields();
          this.isLoading = false;
        },
        (error) => {
          console.error('Error adding expense:', error);
          this.isLoading = false;
        }
      );
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

  fetchExpenses(): void {
    this.isLoading = true;
    this.expenseService.getExpenses().subscribe(
      (data: any) => {
        this.expenses = data.map((expense: any) => ({
          ...expense,
          date: new Date(expense.date),
        }));
        this.calculateTotal();
        this.sortExpenses();
        this.isLoading = false;
      },
      (error) => {
        console.error('Error fetching expenses:', error);
        this.isLoading = false;
      }
    );
  }

  fetchCategories(): void {
    this.expenseService.getExpenseCategories().subscribe(
      (data: string[]) => {
        this.categories = data;
      },
      (error) => {
        console.error('Error fetching categories:', error);
      }
    );
  }

  private resetFormFields(): void {
    this.amount = 0;
    this.category = '';
  }

  applyFilters(): void {
    this.filterCategory = this.tempFilterCategory;
    this.searchDate = this.tempSearchDate;
    this.calculateTotal();
    this.showFilterToast('Filters applied successfully');
  }

  calculateTotal(): void {
    this.totalExpenses = this.getFilteredExpenses().reduce((sum, expense) => sum + expense.amount, 0);
  }

  getFilteredExpenses(): Expense[] {
    return this.expenses.filter(expense => {
      // Filter by category if one is selected
      const categoryMatch = !this.filterCategory || expense.category === this.filterCategory;
      
      // Filter by date if one is entered
      let dateMatch = true;
      if (this.searchDate) {
        const searchDate = new Date(this.searchDate);
        const expenseDate = new Date(expense.date);
        dateMatch = expenseDate.toDateString() === searchDate.toDateString();
      }
      
      return categoryMatch && dateMatch;
    });
  }

  clearFilters(): void {
    this.filterCategory = '';
    this.searchDate = '';
    this.tempFilterCategory = '';
    this.tempSearchDate = '';
    this.calculateTotal();
    this.showFilterToast('Filters cleared');
  }

  formatDate(date: Date): string {
    return date.toLocaleDateString();
  }

  sortExpenses(): void {
    // Support both single and multiple sort methods
    if (this.sortConfigs.length > 0) {
      this.expenses.sort((a, b) => {
        // Apply multiple sort fields in order
        for (const config of this.sortConfigs) {
          let comparison = 0;
          
          if (config.field === 'date') {
            comparison = new Date(a.date).getTime() - new Date(b.date).getTime();
          } else if (config.field === 'amount') {
            comparison = a.amount - b.amount;
          } else if (config.field === 'category') {
            comparison = a.category.localeCompare(b.category);
          }
          
          if (comparison !== 0) {
            return config.direction === 'asc' ? comparison : -comparison;
          }
        }
        
        return 0; // If all comparisons are equal
      });
    } else {
      // Fallback to single sort field method
      this.expenses.sort((a, b) => {
        let comparison = 0;
        
        if (this.sortField === 'date') {
          comparison = new Date(a.date).getTime() - new Date(b.date).getTime();
        } else if (this.sortField === 'amount') {
          comparison = a.amount - b.amount;
        } else if (this.sortField === 'category') {
          comparison = a.category.localeCompare(b.category);
        }
        
        return this.sortDirection === 'asc' ? comparison : -comparison;
      });
    }
  }

  // Original toggleSort function to maintain backward compatibility
  toggleSort(field: string): void {
    // Update the legacy sort field and direction
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    
    // Also update the sortConfigs array
    // First remove any existing configuration for this field
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
    
    this.sortExpenses();
  }

  getSortIcon(field: string): string {
    const config = this.sortConfigs.find(config => config.field === field);
    if (!config) return '↕️';
    return config.direction === 'asc' ? `↑` : `↓`;
  }

  ngOnInit(): void {
    this.fetchCategories();
    
    // Initialize temporary filter values
    this.tempFilterCategory = this.filterCategory;
    this.tempSearchDate = this.searchDate;
  }
}