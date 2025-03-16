import { Component, OnInit } from '@angular/core';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { CommonModule } from '@angular/common';
import { ExpenseService } from '../../service/expense.service';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';

@Component({
  selector: 'app-expense-chart',
  standalone: true,
  imports: [CommonModule, NgxChartsModule, SidebarComponent], // Import NgxChartsModule
  templateUrl: './expense-chart.component.html',
  styleUrls: ['./expense-chart.component.css'],
})
export class ExpenseChartComponent implements OnInit {
  expenseData: { name: string; value: number }[] = [];
  view: [number, number] = [700, 400]; // Chart size
  isSidebarOpen = true; // Sidebar state

  // Chart options
  showXAxis = true;
  showYAxis = true;
  gradient = false;
  showLegend = true;
  showXAxisLabel = true;
  xAxisLabel = 'Expense Category';
  showYAxisLabel = true;
  yAxisLabel = 'Amount Spent';

  colorScheme = {
    domain: ['#5AA454', '#A10A28', '#C7B42C', '#AAAAAA']
  };

  constructor(private expenseService: ExpenseService) {}

  ngOnInit(): void {
    this.loadExpenseSummary();
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  loadExpenseSummary() {
    this.expenseService.getExpenseSummary().subscribe(
      (data) => {
        this.expenseData = data.map((item) => ({
          name: item.category,
          value: item.totalAmount
        }));
      },
      (error) => {
        console.error('Error fetching expense summary:', error);
      }
    );
  }
}
