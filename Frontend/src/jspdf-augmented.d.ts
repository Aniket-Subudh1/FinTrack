import 'jspdf';

declare module 'jspdf' {
  interface jsPDF {
    autoTable(options: any): void;
    lastAutoTable: {
      finalY: number;
    };
  }
}