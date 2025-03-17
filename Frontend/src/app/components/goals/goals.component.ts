import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { ExpenseService } from '../../service/expense.service';
import { IncomeService } from '../../service/income.service';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { Color, ScaleType } from '@swimlane/ngx-charts';
import * as d3 from 'd3';

interface CategoryBudget {
  category: string;
  amount: number;
  recommended?: number;
  spent?: number;
}

interface ColorScheme extends Color {
  domain: string[];
}

interface ChartData {
  name: string;
  value: number;
}

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent, NgxChartsModule],
  templateUrl: './goals.component.html',
  styleUrls: ['./goals.component.css']
})
export class GoalsComponent implements OnInit {
  isSidebarOpen = true;
  activeTab = 'setup';
  @ViewChild('incomeVsExpenseChart') incomeVsExpenseChartElement!: ElementRef;
  @ViewChild('categoryBreakdownChart') categoryBreakdownChartElement!: ElementRef;
  
  // Goal setup properties
  monthlyIncome: number = 0;
  savingsGoal: number = 0;
  categoryBudgets: CategoryBudget[] = [];
  availableCategories: string[] = [];
  isLoading: boolean = false;
  
  // Progress tracking properties
  currentSavings: number = 0;
  daysRemaining: number = 0;
  
  // Success and warning notifications
  successBubble: { show: boolean; message: string } = { show: false, message: '' };
  warningBubble: { show: boolean; message: string } = { show: false, message: '' };
  
  // Chart data properties
  incomeVsExpenseData: ChartData[] = [];
  categoryBreakdownData: ChartData[] = [];
  spendingInsights: string[] = [];
  
  // Recommended percentages for budget categories
  categoryRecommendations: { [key: string]: number } = {
    'Housing': 30,
    'Food': 15,
    'Transportation': 10,
    'Utilities': 10,
    'Entertainment': 5,
    'Healthcare': 10,
    'Debt Payments': 10,
    'Personal Care': 5,
    'Education': 5,
    'Miscellaneous': 5
  };
  
  constructor(
    private expenseService: ExpenseService,
    private incomeService: IncomeService
  ) {}

  ngOnInit(): void {
    this.fetchExpenseCategories();
    this.loadSavedGoals();
    this.calculateDaysRemaining();
    this.fetchExpenses();
    
    // Initialize with at least one budget item
    if (this.categoryBudgets.length === 0) {
      this.addBudgetItem();
    }
  }
  
  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  /*colorScheme = {
    domain: ['#FFD700', '#1E88E5', '#13D8AA', '#FF5252', '#8A2BE2', '#FF7F50', '#8BC34A']
  };*/

  colorScheme: ColorScheme = {
    name: 'customScheme',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#5AA454', '#A10A28', '#C7B42C', '#AAAAAA']
  };
  
  setActiveTab(tab: string): void {
    this.activeTab = tab;
    
    if (tab === 'progress') {
      this.fetchExpenses();
    } else if (tab === 'analysis') {
      this.fetchExpenses();
      setTimeout(() => {
        this.generateCharts();
        this.generateInsights();
      }, 100);
    }
  }
  
  ngAfterViewInit(): void {
    if (this.activeTab === 'analysis') {
      this.generateCharts();
    }
  }
  
  fetchExpenseCategories(): void {
    this.expenseService.getExpenseCategories().subscribe(
      (categories: string[]) => {
        this.availableCategories = categories;
      },
      (error) => {
        console.error('Error fetching expense categories:', error);
        this.showWarningBubble('Failed to load expense categories');
      }
    );
  }
  
  loadSavedGoals(): void {
    // Get goals from local storage or API
    const savedGoals = localStorage.getItem('financialGoals');
    
    if (savedGoals) {
      const goals = JSON.parse(savedGoals);
      this.monthlyIncome = goals.monthlyIncome;
      this.savingsGoal = goals.savingsGoal;
      this.categoryBudgets = goals.categoryBudgets;
    }
  }
  
  calculateDaysRemaining(): void {
    const now = new Date();
    const lastDayOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    this.daysRemaining = lastDayOfMonth.getDate() - now.getDate() + 1;
  }
  
  addBudgetItem(): void {
    this.categoryBudgets.push({
      category: '',
      amount: 0
    });
  }
  
  removeBudgetItem(index: number): void {
    this.categoryBudgets.splice(index, 1);
  }
  
  calculateAvailableAmount(): number {
    const totalBudgeted = this.categoryBudgets.reduce((total, budget) => total + (budget.amount || 0), 0);
    return this.monthlyIncome - this.savingsGoal - totalBudgeted;
  }
  
  getRecommendedPercentage(index: number): number {
    const category = this.categoryBudgets[index].category;
    return category ? (this.categoryRecommendations[category] || 5) : 0;
  }
  
  getRecommendedAmount(index: number): number {
    const percentage = this.getRecommendedPercentage(index);
    return (percentage / 100) * this.monthlyIncome;
  }
  
  applyRecommendations(): void {
    this.categoryBudgets.forEach((budget, index) => {
      if (budget.category) {
        const recommendedAmount = this.getRecommendedAmount(index);
        budget.amount = Math.round(recommendedAmount);
      }
    });
  }
  
  saveGoals(): void {
    this.isLoading = true;
    
    const goals = {
      monthlyIncome: this.monthlyIncome,
      savingsGoal: this.savingsGoal,
      categoryBudgets: this.categoryBudgets
    };
    
    // Save to local storage for now, you can later add API call
    localStorage.setItem('financialGoals', JSON.stringify(goals));
    
    setTimeout(() => {
      this.isLoading = false;
      this.showSuccessBubble('Financial goals saved successfully!');
      this.setActiveTab('progress');
      this.renderIncomeVsExpenseChart();
      this.renderCategoryBreakdownChart();
    }, 1000);
  }
  
  fetchExpenses(): void {
    this.expenseService.getExpenses().subscribe(
      (expenses: any) => {
        // Calculate current savings and spending by category
        this.calculateProgress(expenses);
      },
      (error) => {
        console.error('Error fetching expenses:', error);
      }
    );
  }
  
  calculateProgress(expenses: any[]): void {
    // Calculate current month's expenses
    const now = new Date();
    const currentMonthExpenses = expenses.filter(expense => {
      const expenseDate = new Date(expense.date);
      return expenseDate.getMonth() === now.getMonth() && 
             expenseDate.getFullYear() === now.getFullYear();
    });
    
    // Sum expenses by category
    const expensesByCategory: { [key: string]: number } = {};
    currentMonthExpenses.forEach(expense => {
      if (!expensesByCategory[expense.category]) {
        expensesByCategory[expense.category] = 0;
      }
      expensesByCategory[expense.category] += expense.amount;
    });
    
    // Update budget objects with spent amounts
    this.categoryBudgets.forEach(budget => {
      budget.spent = expensesByCategory[budget.category] || 0;
    });
    
    // Calculate total expenses
    const totalExpenses = currentMonthExpenses.reduce((sum, expense) => sum + expense.amount, 0);
    
    // Calculate current savings (assuming all remaining money is saved)
    this.currentSavings = Math.max(0, this.monthlyIncome - totalExpenses);
  }
  
  getSavingsPercentage(): number {
    return this.savingsGoal > 0 ? Math.min(100, Math.round((this.currentSavings / this.savingsGoal) * 100)) : 0;
  }
  
  getCategorySpent(category: string): number {
    const budget = this.categoryBudgets.find(b => b.category === category);
    return budget?.spent || 0;
  }
  
  getCategoryPercentage(category: string): number {
    const budget = this.categoryBudgets.find(b => b.category === category);
    if (!budget || budget.amount === 0) return 0;
    return Math.round((this.getCategorySpent(category) / budget.amount) * 100);
  }
  
  getCategoryOverage(category: string): number {
    const budget = this.categoryBudgets.find(b => b.category === category);
    if (!budget) return 0;
    const spent = this.getCategorySpent(category);
    return spent > budget.amount ? spent - budget.amount : 0;
  }
  
  getDailyBudget(): number {
    // Get total budget excluding savings
    const totalBudget = this.categoryBudgets.reduce((sum, budget) => sum + budget.amount, 0);
    // Divide by days remaining in month
    return this.daysRemaining > 0 ? totalBudget / this.daysRemaining : 0;
  }
  
  generateCharts(): void {
    // Generate Income vs Expenses chart data
    this.incomeVsExpenseData = [
      { name: 'Income', value: this.monthlyIncome },
      { name: 'Expenses', value: this.categoryBudgets.reduce((sum, budget) => sum + (budget.spent || 0), 0) },
      { name: 'Savings', value: this.currentSavings }
    ];
    
    // Generate Category Breakdown chart data
    this.categoryBreakdownData = this.categoryBudgets
      .filter(budget => budget.category && budget.spent)
      .map(budget => ({
        name: budget.category,
        value: budget.spent || 0
      }));
  }
  
  generateInsights(): void {
    this.spendingInsights = [];
    
    // Check if user is on track with savings
    if (this.currentSavings < this.savingsGoal * 0.75) {
      this.spendingInsights.push("You're falling behind on your savings goal. Consider reducing spending in non-essential categories.");
    } else if (this.currentSavings >= this.savingsGoal) {
      this.spendingInsights.push("Great job! You've met or exceeded your savings goal for this month.");
    }
    
    // Check for overspending categories
    const overspentCategories = this.categoryBudgets.filter(budget => 
      budget.category && budget.spent && budget.amount && budget.spent > budget.amount);
    
    if (overspentCategories.length > 0) {
      this.spendingInsights.push(`You've exceeded your budget in ${overspentCategories.length} ${overspentCategories.length === 1 ? 'category' : 'categories'}.`);
      
      overspentCategories.forEach(budget => {
        const percentage = Math.round((budget.spent || 0) / budget.amount * 100) - 100;
        this.spendingInsights.push(`${budget.category}: ${percentage}% over budget. Consider adjusting your spending habits.`);
      });
    }
    
    // Add some general insights
    if (this.daysRemaining < 10) {
      this.spendingInsights.push(`Only ${this.daysRemaining} days left in the month. Your daily budget is ₹${Math.round(this.getDailyBudget())}.`);
    }
    
    // If any category has very low spending
    const lowSpendCategories = this.categoryBudgets.filter(budget => 
      budget.category && budget.spent !== undefined && budget.amount && 
      (budget.spent / budget.amount) < 0.3 && this.daysRemaining < 15);
    
    if (lowSpendCategories.length > 0) {
      this.spendingInsights.push(`You're underspending in ${lowSpendCategories.length} ${lowSpendCategories.length === 1 ? 'category' : 'categories'}. You might be able to reallocate some funds.`);
    }
  }
  
  private showSuccessBubble(message: string): void {
    this.successBubble = { show: true, message };
    setTimeout(() => {
      this.successBubble.show = false;
    }, 3000);
  }
  
  private showWarningBubble(message: string): void {
    this.warningBubble = { show: true, message };
    setTimeout(() => {
      this.warningBubble.show = false;
    }, 3000);
  }

  renderIncomeVsExpenseChart(): void {
    const element = document.getElementById('incomeVsExpenseChart');
    if (!element) return;
    
    // Clear existing chart
    d3.select(element).selectAll('*').remove();
    
    // Set up dimensions
    const width = element.clientWidth;
    const height = element.clientHeight;
    const margin = {top: 20, right: 20, bottom: 30, left: 70};
    const chartWidth = width - margin.left - margin.right;
    const chartHeight = height - margin.top - margin.bottom;
    
    // Create SVG
    const svg = d3.select(element)
      .append('svg')
      .attr('width', width)
      .attr('height', height)
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);
    
    // Set up scales
    const x = d3.scaleBand()
      .range([0, chartWidth])
      .padding(0.4)
      .domain(this.incomeVsExpenseData.map(d => d.name));
    
    const maxValue = Math.max(...this.incomeVsExpenseData.map(d => d.value));
    const y = d3.scaleLinear()
      .range([chartHeight, 0])
      .domain([0, maxValue * 1.1]);
    
    // Add X axis
    svg.append('g')
      .attr('transform', `translate(0,${chartHeight})`)
      .call(d3.axisBottom(x))
      .selectAll('text')
      .style('fill', '#ffffff');
    
    // Add Y axis
    svg.append('g')
      .call(d3.axisLeft(y).ticks(5).tickFormat(d => `₹${d3.format(',')(d)}`))
      .selectAll('text')
      .style('fill', '#ffffff');
    
    // Add bars
    svg.selectAll('.bar')
      .data(this.incomeVsExpenseData)
      .enter()
      .append('rect')
      .attr('class', 'bar')
      .attr('x', d => x(d.name) ?? 0)
      .attr('y', d => y(d.value))
      .attr('width', x.bandwidth())
      .attr('height', d => chartHeight - y(d.value))
      .attr('fill', (d, i) => this.colorScheme.domain[i % this.colorScheme.domain.length]);
    
    // Add labels
    svg.selectAll('.label')
      .data(this.incomeVsExpenseData)
      .enter()
      .append('text')
      .attr('class', 'label')
      .attr('x', d => {
        const xValue = x(d.name);
        return xValue !== undefined ? xValue + x.bandwidth() / 2 : 0;
      })
      .attr('y', d => y(d.value) - 5)
      .attr('text-anchor', 'middle')
      .text(d => `₹${d.value.toLocaleString()}`)
      .style('fill', '#ffffff')
      .style('font-size', '12px');
  }
  
  renderCategoryBreakdownChart(): void {
    const element = document.getElementById('categoryBreakdownChart');
    if (!element || this.categoryBreakdownData.length === 0) return;
    
    // Clear existing chart
    d3.select(element).selectAll('*').remove();
    
    // Set up dimensions
    const width = element.clientWidth;
    const height = element.clientHeight;
    const radius = Math.min(width, height) / 2 - 10;
    
    // Create SVG
    const svg = d3.select(element)
      .append('svg')
      .attr('width', width)
      .attr('height', height)
      .append('g')
      .attr('transform', `translate(${width / 2},${height / 2})`);
    
    // Set up color scale
    const color = d3.scaleOrdinal()
      .domain(this.categoryBreakdownData.map(d => d.name))
      .range(this.colorScheme.domain);
    
    // Compute the position of each group on the pie
    const pie = d3.pie()
      .value((d: any) => d.value)
      .sort(null);
    
    const data_ready = pie(this.categoryBreakdownData as any);
    
    // Build the pie chart
    const arcGenerator = d3.arc()
      .innerRadius(0)
      .outerRadius(radius);
    
    // Add the arcs
    svg.selectAll('slices')
      .data(data_ready)
      .enter()
      .append('path')
      .attr('d', arcGenerator as unknown as string)
      .attr('fill', d => color((d.data as unknown as ChartData).name) as string)
      .attr('stroke', 'white')
      .style('stroke-width', '2px');
    
    // Add the labels
    const labelArc = d3.arc()
      .innerRadius(radius * 0.6)
      .outerRadius(radius * 0.6);
    
    svg.selectAll('labels')
      .data(data_ready)
      .enter()
      .append('text')
      .text(d => {
        const percent = Math.round(((d.data as unknown as ChartData).value / this.categoryBreakdownData.reduce((sum, item) => sum + item.value, 0)) * 100);
        return percent > 5 ? `${(d.data as unknown as ChartData).name}: ${percent}%` : '';
      })
      .attr('transform', d => `translate(${labelArc.centroid(d as unknown as d3.DefaultArcObject)})`)
      .style('text-anchor', 'middle')
      .style('font-size', '12px')
      .style('fill', 'white');
    
    // Add a legend
    const legendRectSize = 15;
    const legendSpacing = 4;
    const legendHeight = legendRectSize + legendSpacing;
    
    const legend = svg.selectAll('.legend')
      .data(this.categoryBreakdownData)
      .enter()
      .append('g')
      .attr('class', 'legend')
      .attr('transform', (d, i) => {
        const height = legendHeight * this.categoryBreakdownData.length;
        const offset = height / 2;
        const horz = radius + 20;
        const vert = i * legendHeight - offset;
        return `translate(${horz},${vert})`;
      });
    
    legend.append('rect')
      .attr('width', legendRectSize)
      .attr('height', legendRectSize)
      .style('fill', (d: ChartData) => color(d.name) as string)
      .style('stroke', (d: ChartData) => color(d.name) as string);
    
    legend.append('text')
      .attr('x', legendRectSize + legendSpacing)
      .attr('y', legendRectSize - legendSpacing)
      .text(d => d.name)
      .style('fill', 'white')
      .style('font-size', '12px');
  }
}