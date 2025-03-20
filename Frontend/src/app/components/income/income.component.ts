import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { IncomeService, Income, IncomeSummary } from '../../service/income.service';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { Router } from '@angular/router';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { Color, ScaleType } from '@swimlane/ngx-charts';

interface IncomeWithUI extends Income {
  isEditing?: boolean;
  showDetails?: boolean;
  editAmount?: number;
  editSource?: string;
  editDescription?: string;
  editIsRecurring?: boolean;
  editRecurringFrequency?: string;
  editTags?: string[];
}

interface SortConfig {
  field: string;
  direction: 'asc' | 'desc';
}

@Component({
  selector: 'app-income',
  templateUrl: './income.component.html',
  styleUrls: ['./income.component.css'],
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
    MatDividerModule,
    MatBadgeModule,
    NgxChartsModule
  ],
})
export class IncomeComponent implements OnInit {
  // Income form fields
  amount: number = 0;
  source: string = '';
  description: string = '';
  isRecurring: boolean = false;
  recurringFrequency: string = 'MONTHLY';
  tags: string[] = [];
  newTag: string = '';

  // UI state 
  isSidebarOpen: boolean = true;
  isLoading: boolean = false;
  successBubble: { show: boolean; message: string } = { show: false, message: '' };
  incomes: IncomeWithUI[] = [];
  sources: string[] = [];
  recurringOptions: string[] = [];
  suggestedTags: string[] = [];
  totalMonthlyIncome: number = 0;
  monthlyRecurringIncome: number = 0;
  showAnalytics: boolean = false;
  filterApplied: boolean = false;

  // Filters
  startDate: string = '';
  endDate: string = '';
  sourceFilter: string = '';
  minAmount: number | null = null;
  maxAmount: number | null = null;
  tagFilter: string = '';
  recurringFilter: boolean | null = null;

  // Sort configuration
  sortConfig: SortConfig = { field: 'date', direction: 'desc' };

  // Charts data
  incomeSummary: IncomeSummary[] = [];
  pieChartData: { name: string; value: number }[] = [];
  barChartData: { name: string; value: number }[] = [];
  monthlyTrends: { name: string; series: { name: string; value: number }[] }[] = [];

  // Charts configuration
  colorScheme: Color = {
    name: 'incomeColors',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#4CAF50', '#2196F3', '#9C27B0', '#FF9800', '#3F51B5', '#795548', '#E91E63', '#607D8B']
  };

  // For monthly trends chart
  trendsData: { name: string; series: { name: string; value: number }[] }[] = [];

  constructor(private incomeService: IncomeService, private router: Router) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.fetchIncomes();
    this.fetchSources();
    this.recurringOptions = this.incomeService.getRecurringFrequencyOptions();
    this.suggestedTags = this.incomeService.getSuggestedTags();
    
    // Set default date filters to current month
    const now = new Date();
    const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDayOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    
    this.startDate = firstDayOfMonth.toISOString().split('T')[0];
    this.endDate = lastDayOfMonth.toISOString().split('T')[0];
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  toggleAnalytics(): void {
    this.showAnalytics = !this.showAnalytics;
    if (this.showAnalytics) {
      this.fetchIncomeSummary();
      this.fetchMonthlyTrends();
    }
  }

  fetchIncomes(): void {
    this.isLoading = true;
    this.incomeService.getIncomes().subscribe(
      (data) => {
        this.incomes = data.map(income => ({
          ...income,
          isEditing: false,
          showDetails: false
        }));
        this.sortIncomes();
        this.calculateTotals();
        this.isLoading = false;
      },
      (error) => {
        console.error('Error fetching incomes:', error);
        this.isLoading = false;
        this.showMessage('Error loading incomes. Please try again.', false);
      }
    );
  }

  fetchSources(): void {
    this.incomeService.getIncomeSources().subscribe(
      (data) => {
        this.sources = data;
        // Add default sources if none are available
        if (this.sources.length === 0) {
          this.sources = this.incomeService.getSuggestedSources();
        }
      },
      (error) => {
        console.error('Error fetching income sources:', error);
        this.sources = this.incomeService.getSuggestedSources();
      }
    );
  }

  fetchIncomeSummary(): void {
    this.incomeService.getIncomeSummary().subscribe(
      (data) => {
        this.incomeSummary = data;
        this.pieChartData = data.map(item => ({
          name: item.source,
          value: item.totalAmount
        }));
      },
      (error) => {
        console.error('Error fetching income summary:', error);
      }
    );
  }

  fetchMonthlyTrends(): void {
    this.incomeService.getMonthlyTrends().subscribe(
      (data) => {
        // Convert the object to array format required by ngx-charts
        const monthsArray = Object.entries(data).map(([month, value]) => ({
          name: this.formatMonthLabel(month),
          value: value as number
        }));
        
        // Sort by month
        monthsArray.sort((a, b) => {
          const aMonth = a.name.split(' ');
          const bMonth = b.name.split(' ');
          const aYear = parseInt(aMonth[1]);
          const bYear = parseInt(bMonth[1]);
          
          if (aYear !== bYear) {
            return aYear - bYear;
          }
          
          const monthOrder = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
          return monthOrder.indexOf(aMonth[0]) - monthOrder.indexOf(bMonth[0]);
        });
        
        this.barChartData = monthsArray;
        
        // Create line chart data
        this.trendsData = [{
          name: 'Monthly Income',
          series: monthsArray
        }];
      },
      (error) => {
        console.error('Error fetching monthly trends:', error);
      }
    );
  }

  formatMonthLabel(monthStr: string): string {
    const [year, month] = monthStr.split('-');
    const date = new Date(parseInt(year), parseInt(month) - 1, 1);
    return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
  }

  addIncome(): void {
    if (this.amount <= 0 || !this.source) {
      this.showMessage('Please enter a valid amount and source', false);
      return;
    }

    this.isLoading = true;
    
    const incomeRequest = {
      amount: this.amount,
      source: this.source,
      description: this.description,
      isRecurring: this.isRecurring,
      recurringFrequency: this.isRecurring ? this.recurringFrequency : null,
      tags: this.tags
    };

    this.incomeService.addIncome(incomeRequest).subscribe(
      (response) => {
        this.fetchIncomes(); // Refresh the incomes list
        this.resetForm();
        this.showMessage('Income added successfully!', true);
        if (this.showAnalytics) {
          this.fetchIncomeSummary();
          this.fetchMonthlyTrends();
        }
      },
      (error) => {
        console.error('Error adding income:', error);
        this.isLoading = false;
        this.showMessage('Failed to add income. Please try again.', false);
      }
    );
  }

  resetForm(): void {
    this.amount = 0;
    this.source = '';
    this.description = '';
    this.isRecurring = false;
    this.recurringFrequency = 'MONTHLY';
    this.tags = [];
    this.newTag = '';
  }

  showMessage(message: string, isSuccess: boolean): void {
    this.successBubble = { 
      show: true, 
      message: message 
    };
    setTimeout(() => {
      this.successBubble.show = false;
    }, 3000);
  }

  addTag(): void {
    if (this.newTag && !this.tags.includes(this.newTag)) {
      this.tags.push(this.newTag.trim());
      this.newTag = '';
    }
  }

  removeTag(tag: string): void {
    this.tags = this.tags.filter(t => t !== tag);
  }

  addSuggestedTag(tag: string): void {
    if (!this.tags.includes(tag)) {
      this.tags.push(tag);
    }
  }

  toggleIncomeDetails(income: IncomeWithUI): void {
    income.showDetails = !income.showDetails;
  }

  editIncome(income: IncomeWithUI): void {
    // Reset edit state for all incomes
    this.incomes.forEach(i => i.isEditing = false);
    
    income.isEditing = true;
    income.editAmount = income.amount;
    income.editSource = income.source;
    income.editDescription = income.description;
    income.editIsRecurring = income.isRecurring;
    income.editRecurringFrequency = income.recurringFrequency;
    income.editTags = income.tags ? [...income.tags] : [];
  }

  cancelEdit(income: IncomeWithUI): void {
    income.isEditing = false;
  }

  saveIncome(income: IncomeWithUI): void {
    if (!income.editAmount || income.editAmount <= 0 || !income.editSource) {
      this.showMessage('Please enter a valid amount and source', false);
      return;
    }

    this.isLoading = true;
    
    const incomeRequest = {
      amount: income.editAmount,
      source: income.editSource,
      description: income.editDescription,
      isRecurring: income.editIsRecurring,
      recurringFrequency: income.editIsRecurring ? income.editRecurringFrequency : null,
      tags: income.editTags
    };

    this.incomeService.updateIncome(income.id!, incomeRequest).subscribe(
      (response) => {
        // Update local object
        income.amount = income.editAmount!;
        income.source = income.editSource!;
        income.description = income.editDescription;
        income.isRecurring = income.editIsRecurring;
        income.recurringFrequency = income.editRecurringFrequency;
        income.tags = income.editTags;
        income.isEditing = false;
        
        this.calculateTotals();
        this.isLoading = false;
        this.showMessage('Income updated successfully!', true);
        
        if (this.showAnalytics) {
          this.fetchIncomeSummary();
          this.fetchMonthlyTrends();
        }
      },
      (error) => {
        console.error('Error updating income:', error);
        this.isLoading = false;
        this.showMessage('Failed to update income. Please try again.', false);
      }
    );
  }

  deleteIncome(id: number): void {
    if (!confirm('Are you sure you want to delete this income?')) {
      return;
    }

    this.isLoading = true;
    
    this.incomeService.deleteIncome(id).subscribe(
      (response) => {
        this.incomes = this.incomes.filter(income => income.id !== id);
        this.calculateTotals();
        this.isLoading = false;
        this.showMessage('Income deleted successfully!', true);
        
        if (this.showAnalytics) {
          this.fetchIncomeSummary();
          this.fetchMonthlyTrends();
        }
      },
      (error) => {
        console.error('Error deleting income:', error);
        this.isLoading = false;
        this.showMessage('Failed to delete income. Please try again.', false);
      }
    );
  }

  addTagToIncome(income: IncomeWithUI): void {
    if (this.newTag && income.editTags && !income.editTags.includes(this.newTag)) {
      income.editTags.push(this.newTag.trim());
      this.newTag = '';
    }
  }

  removeTagFromIncome(income: IncomeWithUI, tag: string): void {
    if (income.editTags) {
      income.editTags = income.editTags.filter(t => t !== tag);
    }
  }

  applyFilters(): void {
    this.isLoading = true;
    this.filterApplied = true;
    
    const filters = {
      startDate: this.startDate,
      endDate: this.endDate,
      source: this.sourceFilter,
      minAmount: this.minAmount ?? undefined,
      maxAmount: this.maxAmount ?? undefined,
      tags: this.tagFilter,
      recurring: this.recurringFilter !== null ? this.recurringFilter : undefined
    };

    this.incomeService.filterIncomes(filters).subscribe(
      (data) => {
        this.incomes = data.map(income => ({
          ...income,
          isEditing: false,
          showDetails: false
        }));
        this.sortIncomes();
        this.calculateTotals();
        this.isLoading = false;
        this.showMessage('Filters applied successfully', true);
      },
      (error) => {
        console.error('Error filtering incomes:', error);
        this.isLoading = false;
        this.showMessage('Error applying filters. Please try again.', false);
      }
    );
  }

  clearFilters(): void {
    this.startDate = '';
    this.endDate = '';
    this.sourceFilter = '';
    this.minAmount = null;
    this.maxAmount = null;
    this.tagFilter = '';
    this.recurringFilter = null;
    this.filterApplied = false;
    
    this.fetchIncomes();
  }

  sortIncomes(): void {
    this.incomes.sort((a, b) => {
      let comparison = 0;
      
      switch (this.sortConfig.field) {
        case 'date':
          comparison = new Date(a.date).getTime() - new Date(b.date).getTime();
          break;
        case 'amount':
          comparison = a.amount - b.amount;
          break;
        case 'source':
          comparison = a.source.localeCompare(b.source);
          break;
        default:
          comparison = new Date(a.date).getTime() - new Date(b.date).getTime();
      }
      
      return this.sortConfig.direction === 'asc' ? comparison : -comparison;
    });
  }

  toggleSort(field: string): void {
    if (this.sortConfig.field === field) {
      // Toggle direction if already sorted by this field
      this.sortConfig.direction = this.sortConfig.direction === 'asc' ? 'desc' : 'asc';
    } else {
      // Set new field and default to ascending
      this.sortConfig.field = field;
      this.sortConfig.direction = 'asc';
    }
    
    this.sortIncomes();
  }

  getSortIcon(field: string): string {
    if (this.sortConfig.field !== field) {
      return '↕️';
    }
    return this.sortConfig.direction === 'asc' ? '↑' : '↓';
  }

  calculateTotals(): void {
    const now = new Date();
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();
    
    // Filter incomes for the current month
    const thisMonthIncomes = this.incomes.filter(income => {
      const incomeDate = new Date(income.date);
      return incomeDate.getMonth() === currentMonth && incomeDate.getFullYear() === currentYear;
    });
    
    // Calculate total monthly income
    this.totalMonthlyIncome = thisMonthIncomes.reduce((sum, income) => sum + income.amount, 0);
    
    // Calculate recurring income
    this.monthlyRecurringIncome = thisMonthIncomes
      .filter(income => income.isRecurring)
      .reduce((sum, income) => sum + income.amount, 0);
  }

  getSourceIcon(source: string): string {
    return this.incomeService.getSourceIcon(source);
  }

  getSourceColor(source: string): string {
    return this.incomeService.getSourceColor(source);
  }

  formatDate(date: Date | string): string {
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    return dateObj.toLocaleDateString();
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);
  }
}