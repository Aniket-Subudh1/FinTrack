import { Component } from '@angular/core';
import { ExpenseService, Expense } from '../../service/expense.service';
import { NgIf } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-add-expense',
  standalone: true, // Declares that this is a standalone component
  imports: [NgIf, FormsModule, HttpClientModule], // Import dependencies directly
  templateUrl: './add-expense.component.html',
  styleUrls: ['./add-expense.component.css']
})
export class AddExpenseComponent {
  expense: Expense = {
    userName: '',
    amount: 0,
    category: ''
  };


  successMessage: string = '';

  constructor(private expenseService: ExpenseService) {}

  onSubmit(form: NgForm) {
    if (form.valid) {
      this.expenseService.createExpense(this.expense).subscribe(
        (response) => {
          console.log('Expense added successfully', response);
          this.successMessage = 'Expense added successfully!'; // Set the success message
          setTimeout(() => this.successMessage = '', 3000); // Remove the message after 3 seconds
          form.reset(); // Reset the form after successful submission
        },
        (error) => {
          console.error('Error adding expense', error);
        }
      );
    }
  }
}
