import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

const BASE_URL = 'http://localhost:8080';

export interface SpendingPattern {
  categoryTotals: { key: string, value: number }[];
  monthlyTrends: { [month: string]: { [category: string]: number } };
  dailyPatterns: { [day: string]: number };
}

export interface FinancialInsight {
  type: string;
  title: string;
  description: string;
  insight_type: 'success' | 'warning' | 'info';
  icon: string;
  [key: string]: any; // Additional properties
}

export interface ForecastParams {
  months: number;
  includeRecurring: boolean;
  savingsGoal?: number;
}

export interface ForecastResult {
  forecastMonths: {
    month: string;
    projectedIncome: number;
    projectedExpense: number;
    monthlySavings: number;
    cumulativeSavings: number;
  }[];
  avgMonthlyIncome: number;
  avgMonthlyExpense: number;
  recurringExpenses: number;
  monthlySavings: number;
  monthsToGoal?: number;
}

export interface BudgetAnalysis {
  daysInMonth: number;
  daysElapsed: number;
  daysRemaining: number;
  categorySpending: { [category: string]: number };
  dailySpendingRate: number;
  monthToDateSpending: number;
  projectedMonthTotal: number;
  percentOfMonthElapsed: number;
}

export interface PeriodicComparison {
  period: string;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  previousPeriodStart: string;
  previousPeriodEnd: string;
  currentTotalExpenses: number;
  previousTotalExpenses: number;
  expenseChangePercent: number;
  currentTotalIncome: number;
  previousTotalIncome: number;
  incomeChangePercent: number;
  currentSavings: number;
  previousSavings: number;
  savingsChangePercent: number;
  categoryComparison: {
    category: string;
    currentAmount: number;
    previousAmount: number;
    changePercent: number;
  }[];
}

export interface DailySpendingPattern {
  startDate: string;
  endDate: string;
  dayOfWeekTotals: { [day: string]: number };
  avgDayOfWeekTotals: { [day: string]: number };
  dayOfMonthTotals: { [day: number]: number };
  hourlyTotals: { [hour: number]: number };
  highestSpendingDay: string;
  lowestSpendingDay: string;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  constructor(private http: HttpClient) {}

  getSpendingPatterns(startDate?: string, endDate?: string): Observable<SpendingPattern> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);

    return this.http.get<SpendingPattern>(`${BASE_URL}/api/analytics/spending-patterns`, {
      params,
      withCredentials: true
    }).pipe(
      catchError(this.handleError)
    );
  }

  getFinancialInsights(): Observable<FinancialInsight[]> {
    return this.http.get<FinancialInsight[]>(`${BASE_URL}/api/analytics/insights`, {
      withCredentials: true
    }).pipe(
      catchError(this.handleError)
    );
  }

  generateForecast(params: ForecastParams): Observable<ForecastResult> {
    return this.http.post<ForecastResult>(`${BASE_URL}/api/analytics/forecast`, params, {
      withCredentials: true
    }).pipe(
      catchError(this.handleError)
    );
  }

  getBudgetAnalysis(): Observable<BudgetAnalysis> {
    return this.http.get<BudgetAnalysis>(`${BASE_URL}/api/analytics/budget-analysis`, {
      withCredentials: true
    }).pipe(
      catchError(this.handleError)
    );
  }

  getPeriodicComparison(period: 'week' | 'month' | 'year' = 'month'): Observable<PeriodicComparison> {
    let params = new HttpParams().set('period', period);

    return this.http.get<PeriodicComparison>(`${BASE_URL}/api/analytics/periodic-comparison`, {
      params,
      withCredentials: true
    }).pipe(
      catchError(this.handleError)
    );
  }

  getDailySpendingPattern(startDate?: string, endDate?: string): Observable<DailySpendingPattern> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);

    return this.http.get<DailySpendingPattern>(`${BASE_URL}/api/analytics/daily-spending`, {
      params,
      withCredentials: true
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Convert raw spending pattern data to chart-friendly format
  formatCategoryDataForChart(data: SpendingPattern): { name: string, value: number }[] {
    if (!data || !data.categoryTotals) return [];
    
    return data.categoryTotals.map(entry => ({
      name: entry.key,
      value: entry.value
    })).sort((a, b) => b.value - a.value);
  }

  // Convert monthly trends to time series data
  formatMonthlyTrendsForChart(data: SpendingPattern): any[] {
    if (!data || !data.monthlyTrends) return [];
    
    const result: any[] = [];
    const months = Object.keys(data.monthlyTrends).sort();
    
    // Get all categories
    const allCategories = new Set<string>();
    months.forEach(month => {
      Object.keys(data.monthlyTrends[month]).forEach(category => {
        allCategories.add(category);
      });
    });
    
    // Get top 5 categories by total spending
    const topCategories = Array.from(allCategories)
      .map(category => {
        const total = months.reduce((sum, month) => {
          return sum + (data.monthlyTrends[month][category] || 0);
        }, 0);
        return { category, total };
      })
      .sort((a, b) => b.total - a.total)
      .slice(0, 5)
      .map(item => item.category);
    
    // Create series for each month
    months.forEach(month => {
      const series = topCategories.map(category => ({
        name: category,
        value: data.monthlyTrends[month][category] || 0
      }));
      
      result.push({
        name: month,
        series
      });
    });
    
    return result;
  }

  // Format daily spending for chart
  formatDailyPatternForChart(data: DailySpendingPattern): any[] {
    if (!data) return [];
    
    // Format day of week data
    const dayOfWeekData = Object.entries(data.dayOfWeekTotals).map(([day, value]) => ({
      name: this.formatDayOfWeek(day),
      value
    }));
    
    // Sort by day of week
    const dayOrder = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
    dayOfWeekData.sort((a, b) => {
      return dayOrder.indexOf(this.getDayEnum(a.name)) - dayOrder.indexOf(this.getDayEnum(b.name));
    });
    
    return dayOfWeekData;
  }

  // Format forecast data for chart
  formatForecastForChart(data: ForecastResult): any[] {
    if (!data || !data.forecastMonths) return [];
    
    return [
      {
        name: 'Income',
        series: data.forecastMonths.map(month => ({
          name: month.month,
          value: month.projectedIncome
        }))
      },
      {
        name: 'Expenses',
        series: data.forecastMonths.map(month => ({
          name: month.month,
          value: month.projectedExpense
        }))
      },
      {
        name: 'Savings',
        series: data.forecastMonths.map(month => ({
          name: month.month,
          value: month.monthlySavings
        }))
      }
    ];
  }

  // Format budget analysis for chart
  formatBudgetAnalysisForChart(data: BudgetAnalysis): any[] {
    if (!data || !data.categorySpending) return [];
    
    return Object.entries(data.categorySpending)
      .map(([category, amount]) => ({
        name: category,
        value: amount
      }))
      .sort((a, b) => b.value - a.value);
  }

  // Format periodic comparison for chart
  formatPeriodicComparisonForChart(data: PeriodicComparison): any[] {
    if (!data) return [];
    
    // Format for bar chart comparison
    return [
      {
        name: 'Current Period',
        series: [
          { name: 'Income', value: data.currentTotalIncome },
          { name: 'Expenses', value: data.currentTotalExpenses },
          { name: 'Savings', value: data.currentSavings }
        ]
      },
      {
        name: 'Previous Period',
        series: [
          { name: 'Income', value: data.previousTotalIncome },
          { name: 'Expenses', value: data.previousTotalExpenses },
          { name: 'Savings', value: data.previousSavings }
        ]
      }
    ];
  }

  // Helper methods
  private formatDayOfWeek(day: string): string {
    return day.charAt(0) + day.slice(1).toLowerCase();
  }
  
  private getDayEnum(formattedDay: string): string {
    return formattedDay.toUpperCase();
  }

  private handleError(error: any) {
    console.error('API Error in AnalyticsService:', error);
    
    let errorMessage = 'An error occurred while processing your request';
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else if (error.status) {
      // Server-side error
      switch (error.status) {
        case 401:
          errorMessage = 'Unauthorized. Please log in again.';
          break;
        case 403:
          errorMessage = 'You do not have permission to access this resource.';
          break;
        case 404:
          errorMessage = 'Resource not found.';
          break;
        case 500:
          errorMessage = 'Internal server error. Please try again later.';
          break;
        default:
          errorMessage = `Error Code: ${error.status}. Message: ${error.message}`;
      }
    }
    
    return throwError(() => new Error(errorMessage));
  }
}