import { Component, OnInit } from '@angular/core';
import { ExpenseService, Expense } from '../../../service/expense.service';
import { CustomerService } from '../../../service/customer.service';// Import the customer service
import { NgIf, NgFor } from '@angular/common'; // Import NgFor
import { FormsModule, NgForm } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-add-expense',
  standalone: true,
  templateUrl: './add-expense.component.html',
  styleUrls: ['./add-expense.component.css'],
  imports: [NgIf, NgFor, FormsModule, HttpClientModule, SidebarComponent] // Import NgFor here
 // Import NgFor here
})
export class AddExpenseComponent implements OnInit {
  expense: Expense = {
    userName: '',
    amount: 0,
    category: ''
  };

  successMessage: string = '';
  customers: string[] = []; // Store the fetched customer names

  constructor(
    private expenseService: ExpenseService,
    private customerService: CustomerService // Inject the CustomerService
  ) {}

  ngOnInit() {
    this.customerService.getAllCustomerNames().subscribe(
      (customerNames) => {
        this.customers = customerNames; // Populate the customers array with fetched names
      },
      (error) => {
        console.error('Error fetching customer names', error);
      }
    );
  }

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
