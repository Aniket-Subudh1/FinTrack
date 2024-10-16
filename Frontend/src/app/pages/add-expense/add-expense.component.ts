import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExpenseService } from '../../service/expense.service';

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

  expense = {
    username: '',
    type: '',
    amount: 0
  };
 

  constructor(private expenseService: ExpenseService) { }

  onSubmit() {
    // Call the service to add the expense
    this.expenseService.addExpense(this.expense).subscribe(
      (response) => {
        console.log('Expense added:', response);
        // Handle success (e.g., reset form or show confirmation)
      },
      (error) => {
        console.error('Error adding expense:', error);
        // Handle error (e.g., show error message)
      }
    );
    // Redirect back to dashboard after submission
  //   this.router.navigate(['/dashboard']);
  // 
  }
}
