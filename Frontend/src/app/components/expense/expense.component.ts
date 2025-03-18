import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { 
  ExpenseService, 
  Expense, 
  ExpenseCategorySummary, 
  BudgetStatus 
} from '../../service/expense.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';

interface ExpenseWithUI extends Expense {
  isEditing?: boolean;
  showDetails?: boolean;
  editAmount?: number;
  editCategory?: string;
  editTags?: string[];
  editNote?: string;
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
  imports: [
    FormsModule, 
    CommonModule, 
    SidebarComponent, 
    MatIconModule,
    MatTooltipModule,
    MatChipsModule,
    MatMenuModule,
    MatProgressBarModule,
    MatBadgeModule,
    MatDividerModule
  ],
})
export class ExpenseComponent implements OnInit {
  isSidebarOpen: boolean = true;
  amount: number = 0;
  category: string = '';
  tags: string[] = [];
  note: string = '';
  newTag: string = '';
  categories: string[] = [];
  expenses: ExpenseWithUI[] = [];
  successBubble: { show: boolean; message: string } = { show: false, message: '' };
  filterToast: { show: boolean; message: string } = { show: false, message: '' };
  isLoading: boolean = false;
  totalExpenses: number = 0;
  
  // Actual filter values (applied)
  filterCategory: string = '';
  searchDate: string = '';
  tagFilter: string = '';
  amountMin: number | null = null;
  amountMax: number | null = null;
  
  // Temporary filter values (not yet applied)
  tempFilterCategory: string = '';
  tempSearchDate: string = '';
  tempTagFilter: string = '';
  tempAmountMin: number | null = null;
  tempAmountMax: number | null = null;
  
  // Sort field and direction
  sortField: string = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Multiple sort configuration
  sortConfigs: SortConfig[] = [{ field: 'date', direction: 'desc' }];
  
  // Track modal state
  showTrackerModal: boolean = false;
  showAdvancedFilters: boolean = false;
  
  // Budget status for the current month
  budgetStatus: BudgetStatus[] = [];
  
  // Alert thresholds for budget warnings
  warningThreshold: number = 80; // 80% of budget
  criticalThreshold: number = 95; // 95% of budget

  // For recurring expenses
  isRecurring: boolean = false;
  recurringFrequency: string = 'MONTHLY';
  recurringOptions: string[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'];
  
  // Popular tags
  popularTags: string[] = ['groceries', 'utilities', 'rent', 'dining', 'travel', 'entertainment'];
  
  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }
  
  toggleTrackerModal(): void {
    this.showTrackerModal = !this.showTrackerModal;
    
    if (this.showTrackerModal) {
      this.fetchExpenses();
      this.fetchBudgetStatus();
      
      // Initialize temporary filter values when opening modal
      this.tempFilterCategory = this.filterCategory;
      this.tempSearchDate = this.searchDate;
      this.tempTagFilter = this.tagFilter;
      this.tempAmountMin = this.amountMin;
      this.tempAmountMax = this.amountMax;
    }
  }
  
  constructor(private expenseService: ExpenseService, private router: Router) {}

  // Function to open calendar by triggering a click on the date input
  openCalendar(dateInput: HTMLInputElement): void {
    dateInput.showPicker();
  }
  
  // Add a tag to the expense being created
  addTag(): void {
    if (this.newTag.trim() && !this.tags.includes(this.newTag.trim())) {
      this.tags.push(this.newTag.trim());
      this.newTag = '';
    }
  }
  
  // Remove a tag from the expense being created
  removeTag(tag: string): void {
    this.tags = this.tags.filter(t => t !== tag);
  }
  
  // Add a popular tag
  addPopularTag(tag: string): void {
    if (!this.tags.includes(tag)) {
      this.tags.push(tag);
    }
  }
 
  addExpense(): void {
    if (this.amount > 0 && this.category.trim()) {
      const expenseRequest = {
        amount: this.amount,
        category: this.category,
        tags: this.tags,
        note: this.note,
        isRecurring: this.isRecurring,
        recurringFrequency: this.isRecurring ? this.recurringFrequency : null
      };

      this.isLoading = true;
      this.expenseService.addExpense(expenseRequest).subscribe(
        (response) => {
          console.log('Expense added successfully:', response);
          this.showSuccessBubble(`Added ${this.category}: ₹ ${this.amount}`);
          this.fetchExpenses();
          this.fetchBudgetStatus();
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
          isEditing: false,
          showDetails: false
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
  
  fetchBudgetStatus(): void {
    this.expenseService.getBudgetStatus().subscribe(
      (data: BudgetStatus[]) => {
        this.budgetStatus = data;
      },
      (error) => {
        console.error('Error fetching budget status:', error);
      }
    );
  }

  private resetFormFields(): void {
    this.amount = 0;
    this.category = '';
    this.tags = [];
    this.note = '';
    this.newTag = '';
    this.isRecurring = false;
    this.recurringFrequency = 'MONTHLY';
  }

  applyFilters(): void {
    this.filterCategory = this.tempFilterCategory;
    this.searchDate = this.tempSearchDate;
    this.tagFilter = this.tempTagFilter;
    this.amountMin = this.tempAmountMin;
    this.amountMax = this.tempAmountMax;
    
    this.calculateTotal();
    this.showFilterToast('Filters applied successfully');
    this.showAdvancedFilters = false;
  }

  calculateTotal(): void {
    this.totalExpenses = this.getFilteredExpenses().reduce((sum, expense) => sum + expense.amount, 0);
  }

  getFilteredExpenses(): ExpenseWithUI[] {
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
      
      // Filter by tags if specified
      let tagMatch = true;
      if (this.tagFilter && expense.tags) {
        const tagFilterLower = this.tagFilter.toLowerCase();
        tagMatch = expense.tags.some(tag => tag.toLowerCase().includes(tagFilterLower));
      }
      
      // Filter by amount range if specified
      let amountMatch = true;
      if (this.amountMin !== null) {
        amountMatch = amountMatch && expense.amount >= this.amountMin;
      }
      if (this.amountMax !== null) {
        amountMatch = amountMatch && expense.amount <= this.amountMax;
      }
      
      return categoryMatch && dateMatch && tagMatch && amountMatch;
    });
  }

  clearFilters(): void {
    this.filterCategory = '';
    this.searchDate = '';
    this.tagFilter = '';
    this.amountMin = null;
    this.amountMax = null;
    
    this.tempFilterCategory = '';
    this.tempSearchDate = '';
    this.tempTagFilter = '';
    this.tempAmountMin = null;
    this.tempAmountMax = null;
    
    this.calculateTotal();
    this.showFilterToast('Filters cleared');
    this.showAdvancedFilters = false;
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
  
  // Get icon for expense category
  getCategoryIcon(category: string): string {
    return this.expenseService.getCategoryIcon(category);
  }
  
  // Get color for expense category
  getCategoryColor(category: string): string {
    return this.expenseService.getCategoryColor(category);
  }
  
  // Toggle expense details
  toggleExpenseDetails(expense: ExpenseWithUI): void {
    expense.showDetails = !expense.showDetails;
  }
  
  // Enter edit mode for an expense
  editExpense(expense: ExpenseWithUI): void {
    // Reset edit state for all expenses
    this.expenses.forEach(e => e.isEditing = false);
    
    // Set edit state for this expense
    expense.isEditing = true;
    expense.editAmount = expense.amount;
    expense.editCategory = expense.category;
    expense.editTags = expense.tags ? [...expense.tags] : [];
    expense.editNote = expense.note;
  }
  
  // Cancel edit mode
  cancelEdit(expense: ExpenseWithUI): void {
    expense.isEditing = false;
  }
  
  // Save edited expense
  saveExpense(expense: ExpenseWithUI): void {
    if (!expense.editAmount || expense.editAmount <= 0) {
      return;
    }
    
    const updatedExpense = {
      amount: expense.editAmount,
      category: expense.editCategory,
      tags: expense.editTags,
      note: expense.editNote
    };
    
    this.isLoading = true;
    
    this.expenseService.updateExpense(expense.id!, updatedExpense).subscribe(
      (response) => {
        console.log('Expense updated successfully:', response);
        
        // Update local expense object
        expense.amount = expense.editAmount!;
        expense.category = expense.editCategory!;
        expense.tags = expense.editTags;
        expense.note = expense.editNote;
        expense.isEditing = false;
        
        this.showSuccessBubble('Expense updated successfully');
        this.fetchBudgetStatus();
        this.calculateTotal();
        this.isLoading = false;
      },
      (error) => {
        console.error('Error updating expense:', error);
        this.isLoading = false;
      }
    );
  }
  
  // Delete an expense
  deleteExpense(id: number): void {
    if (confirm('Are you sure you want to delete this expense?')) {
      this.isLoading = true;
      
      this.expenseService.deleteExpense(id).subscribe(
        (response) => {
          console.log('Expense deleted successfully:', response);
          this.expenses = this.expenses.filter(e => e.id !== id);
          this.calculateTotal();
          this.showSuccessBubble('Expense deleted successfully');
          this.fetchBudgetStatus();
          this.isLoading = false;
        },
        (error) => {
          console.error('Error deleting expense:', error);
          this.isLoading = false;
        }
      );
    }
  }
  
  // Add tag to expense being edited
  addEditTag(expense: ExpenseWithUI): void {
    if (this.newTag && !expense.editTags?.includes(this.newTag)) {
      if (!expense.editTags) {
        expense.editTags = [];
      }
      expense.editTags.push(this.newTag);
      this.newTag = '';
    }
  }
  
  // Remove tag from expense being edited
  removeEditTag(expense: ExpenseWithUI, tag: string): void {
    if (expense.editTags) {
      expense.editTags = expense.editTags.filter(t => t !== tag);
    }
  }
  
  // Get budget status for a category
  getBudgetStatusForCategory(category: string): BudgetStatus | undefined {
    return this.budgetStatus.find(status => status.category === category);
  }
  
  // Get progress bar color based on percent used
  getProgressBarColor(percentUsed: number): string {
    if (percentUsed >= this.criticalThreshold) {
      return 'bg-red-500';
    } else if (percentUsed >= this.warningThreshold) {
      return 'bg-orange-500';
    } else {
      return 'bg-green-500';
    }
  }
  
  // Get text class based on budget status
  getBudgetStatusTextClass(percentUsed: number): string {
    if (percentUsed >= this.criticalThreshold) {
      return 'text-red-500';
    } else if (percentUsed >= this.warningThreshold) {
      return 'text-orange-500';
    } else {
      return 'text-green-500';
    }
  }
  
  // Toggle advanced filters visibility
  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  ngOnInit(): void {
    this.fetchCategories();
    this.fetchExpenses();
    this.fetchBudgetStatus();
    
    // Initialize temporary filter values
    this.tempFilterCategory = this.filterCategory;
    this.tempSearchDate = this.searchDate;
    this.tempTagFilter = this.tagFilter;
    this.tempAmountMin = this.amountMin;
    this.tempAmountMax = this.amountMax;
  }
}
