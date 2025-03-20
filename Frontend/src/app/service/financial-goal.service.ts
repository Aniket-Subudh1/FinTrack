import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { retry, catchError } from 'rxjs/operators';

const BASE_URL = 'http://localhost:8080';

export interface Milestone {
  id?: number;
  title: string;
  description?: string;
  targetAmount: number;
  targetDate: Date | string;
  completed?: boolean;
  completedDate?: Date | string;
  progressPercentage?: number;
  onTrack?: boolean;
}

export interface FinancialGoal {
  id?: number;
  title: string;
  description?: string;
  targetAmount: number;
  currentAmount: number;
  startDate: Date | string;
  targetDate: Date | string;
  category?: string;
  status?: string;
  priority?: string;
  color?: string;
  icon?: string;
  milestones?: Milestone[];
  achievements?: string[];
  progressPercentage?: number;
  daysRemaining?: number;
  daysElapsed?: number;
  totalDays?: number;
  onTrack?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class FinancialGoalService {

  constructor(private http: HttpClient) { }

  getAllGoals(): Observable<FinancialGoal[]> {
    return this.http.get<FinancialGoal[]>(`${BASE_URL}/api/financial-goals`, { withCredentials: true });
  }

  getGoalById(id: number): Observable<FinancialGoal> {
    return this.http.get<FinancialGoal>(`${BASE_URL}/api/financial-goals/${id}`, { withCredentials: true });
  }

  createGoal(goal: FinancialGoal): Observable<FinancialGoal> {
    return this.http.post<FinancialGoal>(`${BASE_URL}/api/financial-goals`, goal, { withCredentials: true });
  }

  updateGoal(id: number, goal: FinancialGoal): Observable<FinancialGoal> {
    return this.http.put<FinancialGoal>(`${BASE_URL}/api/financial-goals/${id}`, goal, { withCredentials: true });
  }

  deleteGoal(id: number): Observable<any> {
    return this.http.delete(`${BASE_URL}/api/financial-goals/${id}`, { withCredentials: true });
  }

 

  addMilestone(goalId: number, milestone: Milestone): Observable<FinancialGoal> {
    return this.http.post<FinancialGoal>(
      `${BASE_URL}/api/financial-goals/${goalId}/milestones`, 
      milestone, 
      { withCredentials: true }
    );
  }

  getActiveGoals(): Observable<FinancialGoal[]> {
    return this.http.get<FinancialGoal[]>(`${BASE_URL}/api/financial-goals/active`, { withCredentials: true });
  }

  getCompletedGoals(): Observable<FinancialGoal[]> {
    return this.http.get<FinancialGoal[]>(`${BASE_URL}/api/financial-goals/completed`, { withCredentials: true });
  }

  getGoalsByCategory(category: string): Observable<FinancialGoal[]> {
    return this.http.get<FinancialGoal[]>(
      `${BASE_URL}/api/financial-goals/category/${category}`, 
      { withCredentials: true }
    );
  }

  getUpcomingGoals(months: number = 3): Observable<FinancialGoal[]> {
    return this.http.get<FinancialGoal[]>(
      `${BASE_URL}/api/financial-goals/upcoming?months=${months}`, 
      { withCredentials: true }
    );
  }

  getNearlyCompletedGoals(): Observable<FinancialGoal[]> {
    return this.http.get<FinancialGoal[]>(
      `${BASE_URL}/api/financial-goals/nearly-completed`, 
      { withCredentials: true }
    );
  }

  getUpcomingMilestones(days: number = 30): Observable<Milestone[]> {
    return this.http.get<Milestone[]>(
      `${BASE_URL}/api/financial-goals/upcoming-milestones?days=${days}`, 
      { withCredentials: true }
    );
  }

  // Helper methods for UI
  getGoalStatusColor(status?: string): string {
    switch (status) {
      case 'COMPLETED': return '#4CAF50'; // Green
      case 'ACTIVE': return '#2196F3';    // Blue
      case 'ABANDONED': return '#F44336'; // Red
      default: return '#9E9E9E';          // Grey
    }
  }

  getGoalPriorityColor(priority?: string): string {
    switch (priority) {
      case 'HIGH': return '#F44336';    // Red
      case 'MEDIUM': return '#FF9800';  // Orange
      case 'LOW': return '#4CAF50';     // Green
      default: return '#9E9E9E';        // Grey
    }
  }

  getProgressColor(percentage: number, onTrack: boolean): string {
    if (!onTrack) {
      return '#F44336'; // Red for off-track
    }
    
    if (percentage >= 90) {
      return '#4CAF50'; // Green for near completion
    } else if (percentage >= 60) {
      return '#8BC34A'; // Light green for good progress
    } else if (percentage >= 30) {
      return '#FFEB3B'; // Yellow for moderate progress
    } else {
      return '#FF9800'; // Orange for early stages
    }
  }

  getDefaultGoalColor(category?: string): string {
    const colors: Record<string, string> = {
      'Emergency Fund': '#E57373', 
      'Retirement': '#81C784',     
      'Vacation': '#64B5F6',      
      'Education': '#FFD54F',     
      'Home': '#9575CD',           
      'Car': '#4DB6AC',           
      'Debt Payment': '#F06292',  
      'Wedding': '#BA68C8',        
      'Business': '#4FC3F7',      
      'Investment': '#AED581',     
      'Other': '#A1887F'           
    };
    
    return category && colors[category] ? colors[category] : '#7986CB'; 
  }
  
  updateGoalProgress(id: number, currentAmount: number): Observable<FinancialGoal> {
  return this.http.put<FinancialGoal>(
    `${BASE_URL}/api/financial-goals/${id}/progress`, 
    { currentAmount }, 
    { withCredentials: true }
  ).pipe(
    retry(2), 
    catchError(error => {
      console.error('Error updating progress:', error);
      return throwError(() => error);
    })
  );
}
  getDefaultGoalIcon(category?: string): string {
    const icons: Record<string, string> = {
      'Emergency Fund': 'health_and_safety',
      'Retirement': 'beach_access',
      'Vacation': 'flight',
      'Education': 'school',
      'Home': 'home',
      'Car': 'directions_car',
      'Debt Payment': 'money_off',
      'Wedding': 'favorite',
      'Business': 'business',
      'Investment': 'trending_up',
      'Other': 'stars'
    };
    
    return category && icons[category] ? icons[category] : 'flag';
  }
}

