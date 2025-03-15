import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { IncomeService } from '../../service/income.service';

interface Income {
  id?: string;
  date: Date;
  amount: number;
  source: string;
}

interface SortConfig {
  field: string;
  direction: 'asc' | 'desc';
}

interface AddIncomeResponse {
  message: string;
}

@Component({
  selector: 'app-income',
  templateUrl: './income.component.html',
  styleUrls: ['./income.component.css'],
  standalone: true,
  imports: [FormsModule, CommonModule, SidebarComponent],
})
export class IncomeComponent implements OnInit {
  isSidebarOpen: boolean = true;
  amount: number = 0;
  source: string = '';
  categories: string[] = ['Salary', 'Business', 'Investment', 'Other'];
  incomes: Income[] = [];
  successBubble: { show: boolean; message: string } = { show: false, message: '' };
  isLoading: boolean = false;

  // Sort field and direction (for backward compatibility)
  sortField: string = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';

  // Multiple sort configuration
  sortConfigs: SortConfig[] = [{ field: 'date', direction: 'desc' }];

  // Track modal state
  showTrackerModal: boolean = false;

  constructor(private incomeService: IncomeService) {}

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  // Toggle income tracker modal
  toggleTrackerModal(): void {
    this.showTrackerModal = !this.showTrackerModal;
  }

  // Function to open calendar by triggering a click on the date input
  openCalendar(dateInput: HTMLInputElement): void {
    dateInput.showPicker();
  }

  addIncome(): void {
    if (this.amount > 0 && this.source.trim()) {
      const incomeRequest = {
        amount: this.amount,
        source: this.source
      };

      this.isLoading = true;
      this.incomeService.addIncome(incomeRequest).subscribe(
        (response: AddIncomeResponse) => {
          this.incomes.push({ ...incomeRequest, date: new Date() });
          this.showSuccessBubble(response.message);
          this.resetFormFields();
          this.isLoading = false;
        },
        error => {
          console.error('Error adding income:', error);
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

  private resetFormFields(): void {
    this.amount = 0;
    this.source = '';
  }

  formatDate(date: Date): string {
    return date.toLocaleDateString();
  }

  sortIncomes(): void {
    // Support both single and multiple sort methods
    if (this.sortConfigs.length > 0) {
      this.incomes.sort((a, b) => {
        // Apply multiple sort fields in order
        for (const config of this.sortConfigs) {
          let comparison = 0;

          if (config.field === 'date') {
            comparison = new Date(a.date).getTime() - new Date(b.date).getTime();
          } else if (config.field === 'amount') {
            comparison = a.amount - b.amount;
          } else if (config.field === 'source') {
            comparison = a.source.localeCompare(b.source);
          }

          if (comparison !== 0) {
            return config.direction === 'asc' ? comparison : -comparison;
          }
        }

        return 0; // If all comparisons are equal
      });
    } else {
      // Fallback to single sort field method
      this.incomes.sort((a, b) => {
        let comparison = 0;

        if (this.sortField === 'date') {
          comparison = new Date(a.date).getTime() - new Date(b.date).getTime();
        } else if (this.sortField === 'amount') {
          comparison = a.amount - b.amount;
        } else if (this.sortField === 'source') {
          comparison = a.source.localeCompare(b.source);
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

    this.sortIncomes();
  }

  getSortIcon(field: string): string {
    const config = this.sortConfigs.find(config => config.field === field);
    if (!config) return '↕️';
    return config.direction === 'asc' ? `↑` : `↓`;
  }

  ngOnInit(): void {}
}
