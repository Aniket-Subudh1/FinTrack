import { Component, OnInit } from '@angular/core';
import { ExpenseService } from '../../service/expense.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { Router } from '@angular/router';
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
  expenses: { date: Date; amount: number; category: string }[] = [];
  successBubble: { show: boolean; message: string } = { show: false, message: '' };
  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }
  
  constructor(private expenseService: ExpenseService,private router: Router) {
    
  }
 
  addExpense(): void {
    if (this.amount > 0 && this.category.trim()) {
      const expenseRequest = {
        amount: this.amount,
        category: this.category,
      };

      this.expenseService.addExpense(expenseRequest).subscribe(
        (response) => {
          console.log('Expense added successfully:', response);
          this.showSuccessBubble(`Added ${this.category}: ₹ ${this.amount}`);
          this.fetchExpenses();
          this.resetFormFields();
        },
        (error) => {
          console.error('Error adding expense:', error);
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

  fetchExpenses(): void {
    this.expenseService.getExpenses().subscribe(
      (data: any) => {
        this.expenses = data.map((expense: any) => ({
          ...expense,
          date: new Date(expense.date),
        }));
      },
      (error) => {
        console.error('Error fetching expenses:', error);
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

  ngOnInit(): void {
    this.fetchExpenses();
    this.fetchCategories();
  }
}
