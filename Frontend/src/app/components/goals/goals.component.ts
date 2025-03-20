import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { FinancialGoalService, FinancialGoal, Milestone } from '../../service/financial-goal.service';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatIconModule,
    SidebarComponent,
    DatePipe
  ],
  templateUrl: './goals.component.html',
  styleUrls: ['./goals.component.css']
})
export class GoalsComponent implements OnInit {
  // UI State
  isSidebarOpen: boolean = true;
  isLoading: boolean = false;
  isSubmitting: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  currentDate: Date = new Date();
  
  // Modals
  showGoalModal: boolean = false;
  showMilestoneModal: boolean = false;
  showProgressModal: boolean = false;
  
  // Data
  goals: FinancialGoal[] = [];
  filteredGoals: FinancialGoal[] = [];
  activeGoals: FinancialGoal[] = [];
  completedGoals: FinancialGoal[] = [];
  nextMilestone?: Milestone;
  editingGoal: boolean = false;
  editingMilestone: boolean = false;
  selectedGoal?: FinancialGoal;
  selectedMilestone?: Milestone;
  
  // Filters and Sorting
  currentFilter: string = 'all';
  categoryFilter: string = '';
  sortBy: string = 'targetDate';
  
  // Forms
  goalForm!: FormGroup;
  milestoneForm!: FormGroup;
  progressForm!: FormGroup;
  
  // Options
  availableCategories: string[] = [
    'Emergency Fund', 'Retirement', 'Vacation', 'Education', 'Home',
    'Car', 'Debt Payment', 'Wedding', 'Business', 'Investment', 'Other'
  ];
  
  availableColors: string[] = [
    '#E57373', '#81C784', '#64B5F6', '#FFD54F', '#9575CD',
    '#4DB6AC', '#F06292', '#BA68C8', '#4FC3F7', '#AED581', '#A1887F'
  ];

  constructor(
    private fb: FormBuilder,
    private financialGoalService: FinancialGoalService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadGoals();
  }

  initForms(): void {
    this.goalForm = this.fb.group({
      id: [null],
      title: ['', Validators.required],
      description: [''],
      targetAmount: [null, [Validators.required, Validators.min(1)]],
      currentAmount: [0, [Validators.required, Validators.min(0)]],
      startDate: [this.formatDateForInput(new Date()), Validators.required],
      targetDate: [null, Validators.required],
      category: [''],
      priority: ['MEDIUM'],
      color: [''],
      icon: ['flag'],
      status: ['ACTIVE']
    }, { validators: this.dateValidator });

    this.milestoneForm = this.fb.group({
      id: [null],
      title: ['', Validators.required],
      description: [''],
      targetAmount: [null, [Validators.required, Validators.min(1)]],
      targetDate: [null, Validators.required]
    }, { validators: this.milestoneDateValidator });

    this.progressForm = this.fb.group({
      currentAmount: [0, [Validators.required, Validators.min(0)]]
    });
  }

  dateValidator(form: FormGroup) {
    const startDate = new Date(form.get('startDate')?.value);
    const targetDate = new Date(form.get('targetDate')?.value);
    return targetDate > startDate ? null : { invalidDateRange: true };
  }

  milestoneDateValidator(form: FormGroup) {
    if (!this.selectedGoal) return null;
    const goalStartDate = new Date(this.selectedGoal.startDate);
    const goalTargetDate = new Date(this.selectedGoal.targetDate);
    const milestoneTargetDate = new Date(form.get('targetDate')?.value);
    return (milestoneTargetDate >= goalStartDate && milestoneTargetDate <= goalTargetDate) ? null : { invalidMilestoneDate: true };
  }

  loadGoals(): void {
    this.isLoading = true;
    this.financialGoalService.getAllGoals().subscribe({
      next: (goals) => {
        this.goals = goals;
        this.activeGoals = this.goals.filter(g => g.status === 'ACTIVE');
        this.completedGoals = this.goals.filter(g => g.status === 'COMPLETED');
        this.applyFilters();
        this.findNextMilestone();
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.handleError(error, 'Failed to load financial goals');
        this.isLoading = false;
      }
    });
  }

  findNextMilestone(): void {
    const allMilestones: { milestone: Milestone, goal: FinancialGoal }[] = [];
    this.activeGoals.forEach(goal => {
      if (!goal.milestones) return;
      goal.milestones
        .filter(m => !m.completed)
        .forEach(milestone => {
          allMilestones.push({ milestone, goal });
        });
    });
    allMilestones.sort((a, b) => new Date(a.milestone.targetDate).getTime() - new Date(b.milestone.targetDate).getTime());
    this.nextMilestone = allMilestones.length > 0 ? allMilestones[0].milestone : undefined;
  }

  getMilestoneGoal(milestone: Milestone): FinancialGoal | undefined {
    return this.goals.find(goal => goal.milestones?.some(m => m.id === milestone.id));
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  filterGoals(filter: string): void {
    this.currentFilter = filter;
    this.applyFilters();
  }

  applyFilters(): void {
    let filteredGoals: FinancialGoal[] = [];
    switch (this.currentFilter) {
      case 'active':
        filteredGoals = this.goals.filter(g => g.status === 'ACTIVE');
        break;
      case 'completed':
        filteredGoals = this.goals.filter(g => g.status === 'COMPLETED');
        break;
      case 'nearComplete':
        filteredGoals = this.goals.filter(g => g.status === 'ACTIVE' && (g.progressPercentage || 0) >= 75);
        break;
      case 'all':
      default:
        filteredGoals = [...this.goals];
        break;
    }
    if (this.categoryFilter) {
      filteredGoals = filteredGoals.filter(g => g.category === this.categoryFilter);
    }
    filteredGoals = this.sortGoals(filteredGoals);
    this.filteredGoals = filteredGoals;
  }

  sortGoals(goals: FinancialGoal[]): FinancialGoal[] {
    return [...goals].sort((a, b) => {
      switch (this.sortBy) {
        case 'progress':
          return (b.progressPercentage || 0) - (a.progressPercentage || 0);
        case 'priority': {
          const priorityOrder = { 'HIGH': 0, 'MEDIUM': 1, 'LOW': 2 };
          return (priorityOrder[a.priority as keyof typeof priorityOrder] || 1) - 
                 (priorityOrder[b.priority as keyof typeof priorityOrder] || 1);
        }
        case 'amount':
          return (b.targetAmount || 0) - (a.targetAmount || 0);
        case 'targetDate':
        default:
          return new Date(a.targetDate).getTime() - new Date(b.targetDate).getTime();
      }
    });
  }

  sortMilestones(milestones: Milestone[]): Milestone[] {
    return [...milestones].sort((a, b) => 
      new Date(a.targetDate).getTime() - new Date(b.targetDate).getTime()
    );
  }

  openAddGoalModal(): void {
    this.editingGoal = false;
    this.goalForm.reset({
      currentAmount: 0,
      startDate: this.formatDateForInput(new Date()),
      priority: 'MEDIUM',
      icon: 'flag',
      status: 'ACTIVE'
    });
    this.showGoalModal = true;
  }

  editGoal(goal: FinancialGoal): void {
    this.editingGoal = true;
    this.selectedGoal = goal;
    this.goalForm.patchValue({
      id: goal.id,
      title: goal.title,
      description: goal.description || '',
      targetAmount: goal.targetAmount,
      currentAmount: goal.currentAmount,
      startDate: this.formatDateForInput(goal.startDate),
      targetDate: this.formatDateForInput(goal.targetDate),
      category: goal.category || '',
      priority: goal.priority || 'MEDIUM',
      color: goal.color || '',
      icon: goal.icon || 'flag',
      status: goal.status || 'ACTIVE'
    });
    this.showGoalModal = true;
  }

  saveGoal(): void {
    if (this.goalForm.invalid) return;
    this.isSubmitting = true;
    const formValue = this.goalForm.value;
    if (!formValue.color && formValue.category) {
      formValue.color = this.financialGoalService.getDefaultGoalColor(formValue.category);
    }
    if (!formValue.icon && formValue.category) {
      formValue.icon = this.financialGoalService.getDefaultGoalIcon(formValue.category);
    }
    const goal: FinancialGoal = {
      ...formValue,
      startDate: new Date(formValue.startDate),
      targetDate: new Date(formValue.targetDate),
      currentAmount: Number(formValue.currentAmount),
      targetAmount: Number(formValue.targetAmount)
    };
    if (this.editingGoal && goal.id) {
      this.financialGoalService.updateGoal(goal.id, goal).subscribe({
        next: (updatedGoal) => {
          const index = this.goals.findIndex(g => g.id === updatedGoal.id);
          if (index !== -1) {
            this.goals[index] = updatedGoal;
          }
          this.showSuccess('Goal updated successfully');
          this.closeModal();
          this.applyFilters();
          this.findNextMilestone();
          this.isSubmitting = false;
        },
        error: (error: HttpErrorResponse) => {
          this.handleError(error, 'Failed to update goal');
          this.isSubmitting = false;
        }
      });
    } else {
      this.financialGoalService.createGoal(goal).subscribe({
        next: (newGoal) => {
          this.goals.push(newGoal);
          this.showSuccess('Goal created successfully');
          this.closeModal();
          this.applyFilters();
          this.findNextMilestone();
          this.isSubmitting = false;
        },
        error: (error: HttpErrorResponse) => {
          this.handleError(error, 'Failed to create goal');
          this.isSubmitting = false;
        }
      });
    }
  }

  deleteGoal(goal: FinancialGoal): void {
    if (!goal.id || !confirm('Are you sure you want to delete this goal?')) return;
    this.financialGoalService.deleteGoal(goal.id).subscribe({
      next: () => {
        this.goals = this.goals.filter(g => g.id !== goal.id);
        this.applyFilters();
        this.findNextMilestone();
        this.showSuccess('Goal deleted successfully');
      },
      error: (error: HttpErrorResponse) => {
        this.handleError(error, 'Failed to delete goal');
      }
    });
  }

  addMilestone(goal: FinancialGoal): void {
    this.editingMilestone = false;
    this.selectedGoal = goal;
    this.milestoneForm.reset();
    this.showMilestoneModal = true;
  }

  editMilestone(goal: FinancialGoal, milestone: Milestone): void {
    this.editingMilestone = true;
    this.selectedGoal = goal;
    this.selectedMilestone = milestone;
    this.milestoneForm.patchValue({
      id: milestone.id,
      title: milestone.title,
      description: milestone.description || '',
      targetAmount: milestone.targetAmount,
      targetDate: this.formatDateForInput(milestone.targetDate)
    });
    this.showMilestoneModal = true;
  }

  saveMilestone(): void {
    if (this.milestoneForm.invalid || !this.selectedGoal || !this.selectedGoal.id) return;
    this.isSubmitting = true;
    const formValue = this.milestoneForm.value;
    const milestone: Milestone = {
      ...formValue,
      targetAmount: Number(formValue.targetAmount),
      targetDate: new Date(formValue.targetDate)
    };
    const updatedGoal: FinancialGoal = { ...this.selectedGoal };
    if (!updatedGoal.milestones) updatedGoal.milestones = [];
    if (this.editingMilestone && milestone.id) {
      const milestoneIndex = updatedGoal.milestones.findIndex(m => m.id === milestone.id);
      if (milestoneIndex !== -1) {
        updatedGoal.milestones[milestoneIndex] = milestone;
      }
    } else {
      updatedGoal.milestones.push(milestone);
    }
    this.financialGoalService.updateGoal(this.selectedGoal.id, updatedGoal).subscribe({
      next: (updatedGoal) => {
        const index = this.goals.findIndex(g => g.id === updatedGoal.id);
        if (index !== -1) {
          this.goals[index] = updatedGoal;
        }
        this.showSuccess(this.editingMilestone ? 'Milestone updated successfully' : 'Milestone added successfully');
        this.closeMilestoneModal();
        this.applyFilters();
        this.findNextMilestone();
        this.isSubmitting = false;
      },
      error: (error: HttpErrorResponse) => {
        this.handleError(error, this.editingMilestone ? 'Failed to update milestone' : 'Failed to add milestone');
        this.isSubmitting = false;
      }
    });
  }

  deleteMilestone(goal: FinancialGoal, milestone: Milestone): void {
    if (!goal.id || !milestone.id || !confirm('Are you sure you want to delete this milestone?')) return;
    const updatedGoal: FinancialGoal = { ...goal };
    updatedGoal.milestones = updatedGoal.milestones?.filter(m => m.id !== milestone.id) || [];
    this.financialGoalService.updateGoal(goal.id, updatedGoal).subscribe({
      next: (updatedGoal) => {
        const index = this.goals.findIndex(g => g.id === updatedGoal.id);
        if (index !== -1) {
          this.goals[index] = updatedGoal;
        }
        this.showSuccess('Milestone deleted successfully');
        this.applyFilters();
        this.findNextMilestone();
      },
      error: (error: HttpErrorResponse) => {
        this.handleError(error, 'Failed to delete milestone');
      }
    });
  }

  updateProgress(goal: FinancialGoal): void {
    this.selectedGoal = goal;
    this.progressForm.patchValue({
      currentAmount: goal.currentAmount
    });
    this.showProgressModal = true;
  }

  saveProgress(): void {
    if (this.progressForm.invalid || !this.selectedGoal || !this.selectedGoal.id) return;
    this.isSubmitting = true;
    const newAmount = Number(this.progressForm.get('currentAmount')?.value);
    this.financialGoalService.updateGoalProgress(this.selectedGoal.id, newAmount).subscribe({
      next: (updatedGoal) => {
        const index = this.goals.findIndex(g => g.id === updatedGoal.id);
        if (index !== -1) {
          this.goals[index] = updatedGoal;
        }
        this.showSuccess('Progress updated successfully');
        this.closeProgressModal();
        this.applyFilters();
        this.findNextMilestone();
        this.isSubmitting = false;
      },
      error: (error: HttpErrorResponse) => {
        this.handleError(error, 'Failed to update progress');
        this.isSubmitting = false;
      }
    });
  }

  getNextMilestones(): Milestone[] {
    if (!this.selectedGoal || !this.selectedGoal.milestones) return [];
    const newAmount = Number(this.progressForm.get('currentAmount')?.value);
    return this.selectedGoal.milestones
      .filter(m => !m.completed && newAmount >= m.targetAmount)
      .sort((a, b) => a.targetAmount - b.targetAmount);
  }

  closeModal(): void {
    this.showGoalModal = false;
    this.editingGoal = false;
    this.selectedGoal = undefined;
    this.goalForm.reset({
      currentAmount: 0,
      startDate: this.formatDateForInput(new Date()),
      priority: 'MEDIUM',
      icon: 'flag',
      status: 'ACTIVE'
    });
  }

  closeMilestoneModal(): void {
    this.showMilestoneModal = false;
    this.editingMilestone = false;
    this.selectedGoal = undefined;
    this.selectedMilestone = undefined;
    this.milestoneForm.reset();
  }

  closeProgressModal(): void {
    this.showProgressModal = false;
    this.selectedGoal = undefined;
    this.progressForm.reset();
  }

  formatDate(date?: Date | string): string {
    if (!date) return 'N/A';
    return new DatePipe('en-US').transform(date, 'MMM d, yyyy') || 'N/A';
  }

  formatDateForInput(date?: Date | string): string {
    if (!date) return '';
    const d = new Date(date);
    return d.toISOString().split('T')[0];
  }

  getFilterButtonClass(filter: string): string {
    const baseClass = 'px-4 py-2 rounded-lg text-sm transition-all duration-300';
    return this.currentFilter === filter
      ? `${baseClass} bg-teal-500 text-white`
      : `${baseClass} bg-gray-800 text-gray-300 hover:bg-gray-700`;
  }

  getStatusBackgroundColor(status?: string): string {
    return this.financialGoalService.getGoalStatusColor(status) + '33';
  }

  getPriorityBackgroundColor(priority?: string): string {
    return this.financialGoalService.getGoalPriorityColor(priority) + '33';
  }

  getProgressGradient(goal: FinancialGoal): string {
    const color = this.financialGoalService.getProgressColor(goal.progressPercentage || 0, goal.onTrack || false);
    return `linear-gradient(to right, ${color}, ${color}66)`;
  }

  getMilestoneMarkerClass(milestone: Milestone): string {
    return `milestone-marker`;
  }

  getGoalStatusMessage(goal: FinancialGoal): string {
    return goal.onTrack
      ? 'On Track - Keep up the great work!'
      : 'Off Track - Needs attention to meet the target!';
  }

  getAITip(goal: FinancialGoal): string {
    const remainingAmount = goal.targetAmount - goal.currentAmount;
    const remainingDays = goal.daysRemaining || 1;
    const dailySavingsNeeded = remainingAmount / remainingDays;
    return `To get back on track, try saving ₹${Math.ceil(dailySavingsNeeded)} per day for the next ${remainingDays} days.`;
  }

  getAverageProgress(): number {
    if (this.activeGoals.length === 0) return 0;
    const totalProgress = this.activeGoals.reduce((sum, goal) => sum + (goal.progressPercentage || 0), 0);
    return totalProgress / this.activeGoals.length;
  }

  getTotalTargetAmount(): number {
    return this.goals.reduce((sum, goal) => sum + (goal.targetAmount || 0), 0);
  }

  getTotalCurrentAmount(): number {
    return this.goals.reduce((sum, goal) => sum + (goal.currentAmount || 0), 0);
  }

  getTotalRemainingAmount(): number {
    return this.getTotalTargetAmount() - this.getTotalCurrentAmount();
  }

  showSuccess(message: string): void {
    this.successMessage = message;
    setTimeout(() => this.successMessage = '', 3000);
  }

  showError(message: string): void {
    this.errorMessage = message;
    setTimeout(() => this.errorMessage = '', 3000);
  }

  isDateBefore(date1: string | undefined, date2: string | undefined): boolean {
    if (!date1 || !date2) return false;
    return new Date(date1) < new Date(date2);
  }

  handleError(error: HttpErrorResponse, defaultMessage: string): void {
    let errorMessage = defaultMessage;
    if (error.status === 401) {
      errorMessage = 'Unauthorized access. Please log in again.';
      this.router.navigate(['/login']);
    } else if (error.status === 404) {
      errorMessage = 'Resource not found. Please try again.';
    } else if (error.status === 400) {
      errorMessage = 'Invalid request. Please check your input.';
    } else if (error.status === 500) {
      errorMessage = 'Server error. Please try again later.';
    }
    console.error(`${defaultMessage}:`, error);
    this.showError(errorMessage);
  }
}