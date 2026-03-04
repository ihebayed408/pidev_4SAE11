import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContractService, ContractStats as ContractStatsData, ConflictStats as ConflictStatsData } from '../../../core/services/contract.service';
import { Chart, ArcElement, Tooltip, Legend, DoughnutController } from 'chart.js';

Chart.register(ArcElement, Tooltip, Legend, DoughnutController);

@Component({
  selector: 'app-contract-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './contract-stats.html',
  styleUrl: './contract-stats.scss',
})
export class ContractStats implements OnInit, AfterViewInit {
  @ViewChild('contractChart') contractChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('conflictChart') conflictChartRef!: ElementRef<HTMLCanvasElement>;

  contractStats: ContractStatsData | null = null;
  conflictStats: ConflictStatsData | null = null;
  isLoadingContracts = true;
  isLoadingConflicts = true;
  contractError: string | null = null;
  conflictError: string | null = null;

  private contractChartInstance: Chart | null = null;
  private conflictChartInstance: Chart | null = null;

  constructor(private contractSvc: ContractService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.loadContractStats();
    this.loadConflictStats();
  }

  ngAfterViewInit() {
    // Charts are initialized after data loads (in the load methods)
  }

  private loadContractStats() {
    this.contractSvc.getContractStats().subscribe({
      next: (stats) => {
        this.contractStats = stats;
        this.isLoadingContracts = false;
        this.cdr.detectChanges();
        setTimeout(() => this.initContractChart(), 0);
      },
      error: () => {
        this.contractError = 'Could not load contract statistics.';
        this.isLoadingContracts = false;
        this.cdr.detectChanges();
      },
    });
  }

  private loadConflictStats() {
    this.contractSvc.getConflictStats().subscribe({
      next: (stats) => {
        this.conflictStats = stats;
        this.isLoadingConflicts = false;
        this.cdr.detectChanges();
        setTimeout(() => this.initConflictChart(), 0);
      },
      error: () => {
        this.conflictError = 'Could not load conflict statistics.';
        this.isLoadingConflicts = false;
        this.cdr.detectChanges();
      },
    });
  }

  private initContractChart() {
    if (!this.contractChartRef || !this.contractStats) return;
    if (this.contractChartInstance) this.contractChartInstance.destroy();

    const s = this.contractStats;
    this.contractChartInstance = new Chart(this.contractChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Completed', 'Active', 'In Conflict', 'Cancelled', 'Pending Signature', 'Draft'],
        datasets: [{
          data: [s.completed, s.active, s.inConflict, s.cancelled, s.pendingSignature, s.draft],
          backgroundColor: ['#10B981', '#6366F1', '#EF4444', '#9CA3AF', '#F59E0B', '#D1D5DB'],
          borderWidth: 2,
          borderColor: '#ffffff',
        }],
      },
      options: {
        responsive: true,
        cutout: '65%',
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const total = (ctx.dataset.data as number[]).reduce((a, b) => a + b, 0);
                const pct = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : '0';
                return ` ${ctx.label}: ${ctx.parsed} (${pct}%)`;
              },
            },
          },
        },
      },
    });
  }

  private initConflictChart() {
    if (!this.conflictChartRef || !this.conflictStats) return;
    if (this.conflictChartInstance) this.conflictChartInstance.destroy();

    const s = this.conflictStats;
    this.conflictChartInstance = new Chart(this.conflictChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Resolved', 'Open', 'In Review'],
        datasets: [{
          data: [s.resolved, s.open, s.inReview],
          backgroundColor: ['#10B981', '#EF4444', '#F59E0B'],
          borderWidth: 2,
          borderColor: '#ffffff',
        }],
      },
      options: {
        responsive: true,
        cutout: '65%',
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const total = (ctx.dataset.data as number[]).reduce((a, b) => a + b, 0);
                const pct = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : '0';
                return ` ${ctx.label}: ${ctx.parsed} (${pct}%)`;
              },
            },
          },
        },
      },
    });
  }

  get completedPct(): number {
    if (!this.contractStats || this.contractStats.total === 0) return 0;
    return Math.round((this.contractStats.completed / this.contractStats.total) * 100);
  }

  get resolvedPct(): number {
    if (!this.conflictStats || this.conflictStats.total === 0) return 0;
    return Math.round((this.conflictStats.resolved / this.conflictStats.total) * 100);
  }
}
