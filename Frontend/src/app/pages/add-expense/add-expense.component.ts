// src/app/components/add-expense/add-expense.component.ts
import { Component } from '@angular/core';
import { ExpenseService } from '../../service/expense.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-add-expense',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-expense.component.html',
  styleUrls: ['./add-expense.component.css']
})
export class AddExpenseComponent {
  expense = {
    username: '',
    amount: 0,
    category: ''
  };
  submitted = false;

  constructor(private expenseService: ExpenseService) {}

  addExpense(): void {
    this.expenseService.addExpense(this.expense)
      .subscribe({
        next: (response) => {
          console.log(response);
          this.submitted = true;
        },
        error: (e) => console.error(e)
      });
  }

  newExpense(): void {
    this.submitted = false;
    this.expense = {
      username: '',
      amount: 0,
      category: ''
    };
  }
}
