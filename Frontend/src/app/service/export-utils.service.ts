import { Injectable } from '@angular/core';
import * as FileSaver from 'file-saver';

@Injectable({
  providedIn: 'root'
})
export class ExportUtilsService {
  constructor() {}

  exportToCSV(data: any[], filename: string): void {
    if (!data || !data.length) {
      console.warn('No data to export');
      return;
    }

    const headers = Object.keys(data[0]);
    
    // Create CSV rows
    const csvRows = [];
    
    // Add headers
    csvRows.push(headers.join(','));
    
    // Add data rows
    for (const row of data) {
      const values = headers.map(header => {
        const value = row[header];
        // Handle string values with commas
        if (typeof value === 'string' && value.includes(',')) {
          return `"${value}"`;
        }
        // Handle null or undefined
        if (value === null || value === undefined) {
          return '';
        }
        return value;
      });
      csvRows.push(values.join(','));
    }
    

    const csvString = csvRows.join('\\n');
  
    const blob = new Blob([csvString], { type: 'text/csv;charset=utf-8' });
    FileSaver.saveAs(blob, `${filename}.csv`);
  }

  
  exportToPDF(element: HTMLElement, filename: string): void {
   
    console.log('Exporting element to PDF:', element);
    alert(`PDF export functionality would convert ${element.id} to a PDF named ${filename}.pdf`);
    
    
  }

  formatDataForExport(data: any, type: string): any[] {
    if (!data) return [];
    
 
    switch (type) {
      case 'expenses':
        return this.formatExpensesForExport(data);
      case 'income':
        return this.formatIncomeForExport(data);
      case 'budget':
        return this.formatBudgetForExport(data);
      case 'forecast':
        return this.formatForecastForExport(data);
      case 'transactions':
        return this.formatTransactionsForExport(data);
      default:
       
        return Array.isArray(data) ? data : [data];
    }
  }


  private formatExpensesForExport(expenses: any[]): any[] {
    return expenses.map(expense => ({
      Date: this.formatDate(expense.date),
      Category: expense.category,
      Amount: expense.amount,
      Description: expense.description || '',
      Tags: Array.isArray(expense.tags) ? expense.tags.join(', ') : expense.tags || '',
      Recurring: expense.isRecurring ? 'Yes' : 'No',
      Frequency: expense.recurringFrequency || ''
    }));
  }

  private formatIncomeForExport(incomes: any[]): any[] {
    return incomes.map(income => ({
      Date: this.formatDate(income.date),
      Source: income.source || income.category,
      Amount: income.amount,
      Description: income.description || '',
      Tags: Array.isArray(income.tags) ? income.tags.join(', ') : income.tags || '',
      Recurring: income.isRecurring ? 'Yes' : 'No',
      Frequency: income.recurringFrequency || ''
    }));
  }

 
  private formatBudgetForExport(budgetItems: any[]): any[] {
    return budgetItems.map(item => ({
      Category: item.category,
      BudgetAmount: item.budgetAmount || item.amount,
      SpentAmount: item.spentAmount || 0,
      RemainingAmount: item.remainingAmount || (item.budgetAmount - item.spentAmount) || 0,
      PercentUsed: item.percentUsed ? `${item.percentUsed.toFixed(2)}%` : '0%'
    }));
  }

 
  private formatForecastForExport(forecast: any): any[] {
    if (!forecast || !forecast.forecastMonths) return [];
    
    return forecast.forecastMonths.map((month: any) => ({
      Month: month.month,
      ProjectedIncome: month.projectedIncome,
      ProjectedExpenses: month.projectedExpense,
      MonthlySavings: month.monthlySavings,
      CumulativeSavings: month.cumulativeSavings
    }));
  }

  private formatTransactionsForExport(transactions: any[]): any[] {
    return transactions.map(transaction => ({
      Date: this.formatDate(transaction.date),
      Type: transaction.type.charAt(0).toUpperCase() + transaction.type.slice(1),
      Category: transaction.category,
      Amount: transaction.amount,
      Description: transaction.description || '',
      Tags: Array.isArray(transaction.tags) ? transaction.tags.join(', ') : transaction.tags || '',
      Recurring: transaction.isRecurring ? 'Yes' : 'No',
      Frequency: transaction.recurringFrequency || ''
    }));
  }

  private formatDate(date: string | Date): string {
    if (!date) return '';
    
    const d = new Date(date);
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }


  exportChartAsImage(chartElement: HTMLElement, filename: string): void {

    console.log('Exporting chart as image:', chartElement);
    alert(`Image export functionality would convert the chart to a PNG named ${filename}.png`);
    
   
  }

  
  generateReport(data: any, title: string, type: string): void {
    // Create a new window for the report
    const reportWindow = window.open('', '_blank');
    if (!reportWindow) {
      alert('Please allow popups to view the report');
      return;
    }


    let formattedData = this.formatDataForExport(data, type);
    
 
    let htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>${title}</title>
        <style>
          body {
            font-family: Arial, sans-serif;
            margin: 20px;
            color: #333;
          }
          h1 {
            color: #2c3e50;
            border-bottom: 2px solid #3498db;
            padding-bottom: 10px;
          }
          .report-meta {
            margin-bottom: 20px;
            color: #7f8c8d;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
          }
          th {
            background-color: #3498db;
            color: white;
            padding: 10px;
            text-align: left;
          }
          td {
            padding: 8px;
            border-bottom: 1px solid #ddd;
          }
          tr:nth-child(even) {
            background-color: #f2f2f2;
          }
          .summary {
            margin-top: 30px;
            padding: 15px;
            background-color: #eef5f9;
            border-radius: 5px;
          }
          .footer {
            margin-top: 30px;
            text-align: center;
            font-size: 12px;
            color: #7f8c8d;
          }
          @media print {
            .no-print {
              display: none;
            }
            body {
              margin: 0;
              padding: 15px;
            }
            .page-break {
              page-break-before: always;
            }
          }
        </style>
      </head>
      <body>
        <h1>${title}</h1>
        <div class="report-meta">
          <p>Generated on: ${new Date().toLocaleDateString('en-US', { 
            year: 'numeric', month: 'long', day: 'numeric', 
            hour: '2-digit', minute: '2-digit'
          })}</p>
        </div>
        <div class="no-print">
          <button onclick="window.print()">Print Report</button>
          <button onclick="window.close()">Close</button>
        </div>
        <div class="report-content">
    `;

    if (formattedData && formattedData.length > 0) {
      htmlContent += `<table>
        <thead>
          <tr>
            ${Object.keys(formattedData[0]).map(key => `<th>${key}</th>`).join('')}
          </tr>
        </thead>
        <tbody>
          ${formattedData.map(item => `
            <tr>
              ${Object.values(item).map(value => `<td>${value}</td>`).join('')}
            </tr>
          `).join('')}
        </tbody>
      </table>`;
    } else {
      htmlContent += '<p>No data available for this report.</p>';
    }

    if (type === 'expenses' || type === 'income' || type === 'transactions') {
      const total = formattedData.reduce((sum, item) => sum + (parseFloat(item.Amount) || 0), 0);
      htmlContent += `
        <div class="summary">
          <h3>Summary</h3>
          <p>Total: ${total.toLocaleString('en-US', { style: 'currency', currency: 'INR' })}</p>
          <p>Number of entries: ${formattedData.length}</p>
        </div>
      `;
    }

    htmlContent += `
        </div>
        <div class="footer">
          <p>FinTrack - Personal Finance Tracker</p>
        </div>
      </body>
      </html>
    `;

    // Write to the report window
    reportWindow.document.write(htmlContent);
    reportWindow.document.close();
  }


  createShareableLink(data: any, type: string): string {
   
    let shareData: any = { type };
    
    switch (type) {
      case 'expense_summary':
        shareData.categories = Object.keys(data).slice(0, 5); // Limit to top 5 categories
        shareData.total = Object.values(data).reduce((sum: any, val: any) => sum + val, 0);
        break;
      case 'savings_rate':
        shareData.rate = data;
        break;
      case 'monthly_comparison':
        if (data.length >= 2) {
          shareData.current = data[0];
          shareData.previous = data[1];
        }
        break;
      default:
        shareData.summary = 'Financial data';
    }
    

    const encodedData = btoa(JSON.stringify(shareData));
    
    return `${window.location.origin}/share?data=${encodedData}`;
  }

  downloadSVG(svgElement: SVGElement, filename: string): void {
 
    const svgClone = svgElement.cloneNode(true) as SVGElement;
    

    if (!svgClone.getAttribute('xmlns')) {
      svgClone.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
    }
    

    const svgString = new XMLSerializer().serializeToString(svgClone);
    

    const blob = new Blob([svgString], { type: 'image/svg+xml' });
    
   
    FileSaver.saveAs(blob, `${filename}.svg`);
  }
}