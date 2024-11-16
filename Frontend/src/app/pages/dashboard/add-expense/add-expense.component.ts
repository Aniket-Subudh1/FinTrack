
import { Component, OnInit } from '@angular/core';
import { ExpenseService, Expense } from '../../../service/expense.service';
import { NgIf, NgFor } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-add-expense',
  standalone: true,
  templateUrl: './add-expense.component.html',
  styleUrls: ['./add-expense.component.css'],
  imports: [NgIf, NgFor, FormsModule, HttpClientModule, SidebarComponent]
})
export class AddExpenseComponent implements OnInit {
  expense: Expense = {
    amount: 0,
    category: '',
    userName: ''
  };

  successMessage: string = '';

  constructor(private expenseService: ExpenseService) {}

  ngOnInit() {}

  onSubmit(form: NgForm) {
    if (form.valid) {
      this.expenseService.createExpense(this.expense).subscribe(
        (response) => {
          console.log('Expense added successfully', response);
          this.successMessage = 'Expense added successfully!';
          setTimeout(() => this.successMessage = '', 3000);
          form.reset();
        },
        (error) => {
          console.error('Error adding expense', error);
        }
      );
    }
  }
}
