import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { GoalsComponent } from './goals.component';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { ExpenseService } from '../../service/expense.service';
import { IncomeService } from '../../service/income.service';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { of, throwError } from 'rxjs';

describe('GoalsComponent', () => {
  let component: GoalsComponent;
  let fixture: ComponentFixture<GoalsComponent>;
  let expenseServiceSpy: jasmine.SpyObj<ExpenseService>;
  let incomeServiceSpy: jasmine.SpyObj<IncomeService>;

  beforeEach(async () => {
    // Create spies for the services
    const expenseSpy = jasmine.createSpyObj('ExpenseService', ['getExpenseCategories', 'getExpenses']);
    const incomeSpy = jasmine.createSpyObj('IncomeService', ['getIncomes']);

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        FormsModule,
        NgxChartsModule,
        GoalsComponent
      ],
      providers: [
        { provide: ExpenseService, useValue: expenseSpy },
        { provide: IncomeService, useValue: incomeSpy }
      ]
    }).compileComponents();

    expenseServiceSpy = TestBed.inject(ExpenseService) as jasmine.SpyObj<ExpenseService>;
    incomeServiceSpy = TestBed.inject(IncomeService) as jasmine.SpyObj<IncomeService>;

    // Set up default return values for the spies
    expenseServiceSpy.getExpenseCategories.and.returnValue(of([
      'Housing', 'Food', 'Transportation', 'Utilities',
      'Entertainment', 'Healthcare', 'Debt Payments', 'Personal Care'
    ]));
    expenseServiceSpy.getExpenses.and.returnValue(of([]));
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GoalsComponent);
    component = fixture.componentInstance;
    
    // Mock localStorage
    spyOn(localStorage, 'getItem').and.returnValue(null);
    spyOn(localStorage, 'setItem').and.callThrough();
    
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with at least one budget item', () => {
    expect(component.categoryBudgets.length).toBeGreaterThan(0);
  });

  it('should add a budget item when addBudgetItem is called', () => {
    const initialLength = component.categoryBudgets.length;
    component.addBudgetItem();
    expect(component.categoryBudgets.length).toBe(initialLength + 1);
  });

  it('should remove a budget item when removeBudgetItem is called', () => {
    // Add a budget item first
    component.addBudgetItem();
    const initialLength = component.categoryBudgets.length;
    component.removeBudgetItem(0);
    expect(component.categoryBudgets.length).toBe(initialLength - 1);
  });

  it('should calculate available amount correctly', () => {
    component.monthlyIncome = 5000;
    component.savingsGoal = 1000;
    component.categoryBudgets = [
      { category: 'Housing', amount: 1500 },
      { category: 'Food', amount: 800 }
    ];
    
    const availableAmount = component.calculateAvailableAmount();
    expect(availableAmount).toBe(1700); // 5000 - 1000 - 1500 - 800 = 1700
  });

  it('should apply recommended amounts when applyRecommendations is called', () => {
    component.monthlyIncome = 5000;
    component.categoryBudgets = [
      { category: 'Housing', amount: 0 },
      { category: 'Food', amount: 0 }
    ];
    
    component.applyRecommendations();
    
    // Housing should be 30% of income
    expect(component.categoryBudgets[0].amount).toBe(1500);
    // Food should be 15% of income
    expect(component.categoryBudgets[1].amount).toBe(750);
  });

  it('should save goals to localStorage when saveGoals is called', () => {
    component.monthlyIncome = 5000;
    component.savingsGoal = 1000;
    component.categoryBudgets = [
      { category: 'Housing', amount: 1500 },
      { category: 'Food', amount: 800 }
    ];
    
    component.saveGoals();
    
    // Fast-forward the timer to simulate the timeout
    jasmine.clock().install();
    jasmine.clock().tick(1000);
    jasmine.clock().uninstall();
    
    expect(localStorage.setItem).toHaveBeenCalled();
    const savedGoals = JSON.parse(
      (localStorage.setItem as jasmine.Spy).calls.mostRecent().args[1]
    );
    expect(savedGoals.monthlyIncome).toBe(5000);
    expect(savedGoals.savingsGoal).toBe(1000);
    expect(savedGoals.categoryBudgets.length).toBe(2);
  });

  it('should handle errors when fetching expense categories', () => {
    expenseServiceSpy.getExpenseCategories.and.returnValue(throwError('Error'));
    
    spyOn(console, 'error');
    spyOn(component as any, 'showWarningBubble');
    
    component.fetchExpenseCategories();
    
    expect(console.error).toHaveBeenCalled();
    expect((component as any).showWarningBubble).toHaveBeenCalled();
  });

  it('should calculate days remaining in the month correctly', () => {
    // Mock the current date
    const mockDate = new Date(2023, 11, 15); // December 15, 2023
    jasmine.clock().mockDate(mockDate);
    
    component.calculateDaysRemaining();
    
    // December has 31 days, so days remaining should be 31 - 15 + 1 = 17
    expect(component.daysRemaining).toBe(17);
    
    jasmine.clock().uninstall();
  });

  it('should calculate category spent percentage correctly', () => {
    component.categoryBudgets = [
      { category: 'Housing', amount: 1000, spent: 500 }
    ];
    
    const percentage = component.getCategoryPercentage('Housing');
    expect(percentage).toBe(50); // 500/1000 * 100 = 50%
  });

  it('should calculate savings percentage correctly', () => {
    component.savingsGoal = 1000;
    component.currentSavings = 750;
    
    const percentage = component.getSavingsPercentage();
    expect(percentage).toBe(75); // 750/1000 * 100 = 75%
  });

  it('should generate spending insights correctly', () => {
    component.savingsGoal = 1000;
    component.currentSavings = 600;
    component.categoryBudgets = [
      { category: 'Housing', amount: 1000, spent: 1200 },
      { category: 'Food', amount: 500, spent: 400 }
    ];
    component.daysRemaining = 5;
    
    component.generateInsights();
    
    expect(component.spendingInsights.length).toBeGreaterThan(0);
    expect(component.spendingInsights.some(insight => 
      insight.includes('falling behind on your savings goal')
    )).toBeTruthy();
    expect(component.spendingInsights.some(insight => 
      insight.includes('exceeded your budget')
    )).toBeTruthy();
  });

  it('should toggle sidebar correctly', () => {
    const initialState = component.isSidebarOpen;
    component.toggleSidebar();
    expect(component.isSidebarOpen).toBe(!initialState);
  });

  it('should set active tab and load appropriate data', () => {
    spyOn(component, 'fetchExpenses');
    spyOn(component, 'generateCharts');
    spyOn(component, 'generateInsights');
    
    component.setActiveTab('progress');
    expect(component.activeTab).toBe('progress');
    expect(component.fetchExpenses).toHaveBeenCalled();
    
    component.setActiveTab('analysis');
    expect(component.activeTab).toBe('analysis');
    expect(component.generateCharts).toHaveBeenCalled();
    expect(component.generateInsights).toHaveBeenCalled();
  });

  it('should load saved goals from localStorage', () => {
    const savedGoals = {
      monthlyIncome: 6000,
      savingsGoal: 1200,
      categoryBudgets: [
        { category: 'Housing', amount: 1800 },
        { category: 'Food', amount: 900 }
      ]
    };
    
    (localStorage.getItem as jasmine.Spy).and.returnValue(JSON.stringify(savedGoals));
    
    component.loadSavedGoals();
    
    expect(component.monthlyIncome).toBe(6000);
    expect(component.savingsGoal).toBe(1200);
    expect(component.categoryBudgets.length).toBe(2);
    expect(component.categoryBudgets[0].category).toBe('Housing');
    expect(component.categoryBudgets[0].amount).toBe(1800);
  });
});