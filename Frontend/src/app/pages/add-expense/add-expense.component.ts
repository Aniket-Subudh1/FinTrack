import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-add-expense',
  standalone: true,
  templateUrl: './add-expense.component.html',
  imports: [CommonModule, FormsModule],
})
export class AddExpenseComponent {
  expenseAmount: number = 0;
  username: string = '';
  expenseType: string = '';

  constructor(private router: Router) {}

  onSubmit(): void {
    console.log('Expense Submitted', {
      amount: this.expenseAmount,
      username: this.username,
      type: this.expenseType
    });
    // Redirect back to dashboard after submission
    this.router.navigate(['/dashboard']);
  }
}
