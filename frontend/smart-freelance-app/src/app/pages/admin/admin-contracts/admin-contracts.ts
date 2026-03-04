import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ContractService, Contract, ContractStatus, ContractStats, ConflictStats } from '../../../core/services/contract.service';
import { Card } from '../../../shared/components/card/card';
import { Chart, ArcElement, Tooltip, Legend, DoughnutController } from 'chart.js';

Chart.register(ArcElement, Tooltip, Legend, DoughnutController);

type FilterTab = 'ALL' | ContractStatus;

@Component({
  selector: 'app-admin-contracts',
  standalone: true,
  imports: [CommonModule, FormsModule, Card],
  templateUrl: './admin-contracts.html',
  styleUrl: './admin-contracts.scss',
})
export class AdminContracts implements OnInit, AfterViewInit {
  @ViewChild('contractChart') contractChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('conflictChart') conflictChartRef!: ElementRef<HTMLCanvasElement>;

  contractStats: ContractStats | null = null;
  conflictStats: ConflictStats | null = null;
  isLoadingContractStats = true;
  isLoadingConflictStats = true;
  contractStatsError: string | null = null;
  conflictStatsError: string | null = null;

  private contractChartInstance: Chart | null = null;
  private conflictChartInstance: Chart | null = null;
  contracts: Contract[] = [];
  isLoading = true;
  errorMsg: string | null = null;
  actionMsg: string | null = null;

  activeFilter: FilterTab = 'ALL';
  readonly filterTabs: FilterTab[] = [
    'ALL', 'DRAFT', 'PENDING_SIGNATURE', 'ACTIVE', 'IN_CONFLICT', 'COMPLETED', 'CANCELLED',
  ];

  // Per-row status change
  changingStatusId: number | null = null;

  // Delete confirmation
  contractToDelete: Contract | null = null;
  isDeleting = false;

  // All possible statuses for the inline select
  readonly allStatuses: ContractStatus[] = [
    'DRAFT', 'PENDING_SIGNATURE', 'ACTIVE', 'IN_CONFLICT', 'COMPLETED', 'CANCELLED',
  ];

  constructor(private contractSvc: ContractService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.load();
    this.loadContractStats();
    this.loadConflictStats();
  }

  ngAfterViewInit() {}

  private loadContractStats() {
    this.contractSvc.getContractStats().subscribe({
      next: (stats) => {
        this.contractStats = stats;
        this.isLoadingContractStats = false;
        this.cdr.detectChanges();
        setTimeout(() => this.initContractChart(), 0);
      },
      error: () => {
        this.contractStatsError = 'Could not load contract statistics.';
        this.isLoadingContractStats = false;
        this.cdr.detectChanges();
      },
    });
  }

  private loadConflictStats() {
    this.contractSvc.getConflictStats().subscribe({
      next: (stats) => {
        this.conflictStats = stats;
        this.isLoadingConflictStats = false;
        this.cdr.detectChanges();
        setTimeout(() => this.initConflictChart(), 0);
      },
      error: () => {
        this.conflictStatsError = 'Could not load conflict statistics.';
        this.isLoadingConflictStats = false;
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

  load() {
    this.isLoading = true;
    this.errorMsg = null;
    this.contractSvc.getAll().subscribe({
      next: (list) => {
        this.contracts = Array.isArray(list) ? list : [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Could not load contracts. Make sure the Contract service is running on port 8083.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  get filtered(): Contract[] {
    if (this.activeFilter === 'ALL') return this.contracts;
    return this.contracts.filter(c => c.status === this.activeFilter);
  }

  countByStatus(s: FilterTab): number {
    if (s === 'ALL') return this.contracts.length;
    return this.contracts.filter(c => c.status === s).length;
  }

  setFilter(f: FilterTab) { this.activeFilter = f; }

  // ── Status change ──────────────────────────────────────────────

  changeStatus(contract: Contract, newStatus: ContractStatus) {
    if (!contract.id || contract.status === newStatus) return;
    this.changingStatusId = contract.id;
    this.actionMsg = null;
    this.contractSvc.updateStatus(contract.id, newStatus).subscribe({
      next: (updated) => {
        const idx = this.contracts.findIndex(c => c.id === contract.id);
        if (idx !== -1) this.contracts[idx] = updated;
        this.changingStatusId = null;
        this.actionMsg = `Contract "${updated.title}" status changed to ${this.getStatusLabel(updated.status)}.`;
        this.cdr.detectChanges();
      },
      error: () => {
        this.changingStatusId = null;
        this.actionMsg = 'Failed to update status. Please try again.';
        this.cdr.detectChanges();
      },
    });
  }

  // ── Delete ────────────────────────────────────────────────────

  openDelete(c: Contract) { this.contractToDelete = c; }
  closeDelete() { if (!this.isDeleting) this.contractToDelete = null; }

  confirmDelete() {
    if (!this.contractToDelete?.id) return;
    this.isDeleting = true;
    this.contractSvc.delete(this.contractToDelete.id).subscribe({
      next: () => {
        this.contracts = this.contracts.filter(c => c.id !== this.contractToDelete!.id);
        this.isDeleting = false;
        this.actionMsg = `Contract "${this.contractToDelete!.title}" has been deleted.`;
        this.contractToDelete = null;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isDeleting = false;
        this.actionMsg = 'Failed to delete contract.';
        this.contractToDelete = null;
        this.cdr.detectChanges();
      },
    });
  }

  // ── Helpers ───────────────────────────────────────────────────

  getStatusLabel(s?: ContractStatus): string {
    const map: Record<ContractStatus, string> = {
      DRAFT: 'Draft',
      PENDING_SIGNATURE: 'Pending Signature',
      ACTIVE: 'Active',
      IN_CONFLICT: 'In Conflict',
      COMPLETED: 'Completed',
      CANCELLED: 'Cancelled',
    };
    return s ? (map[s] ?? s) : '—';
  }

  getTabLabel(f: FilterTab): string {
    return f === 'ALL' ? 'All' : this.getStatusLabel(f as ContractStatus);
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  // Stat helpers
  get activeCount()    { return this.contracts.filter(c => c.status === 'ACTIVE').length; }
  get conflictCount()  { return this.contracts.filter(c => c.status === 'IN_CONFLICT').length; }
  get completedCount() { return this.contracts.filter(c => c.status === 'COMPLETED').length; }
  get totalValue()     { return this.contracts.reduce((sum, c) => sum + (c.amount ?? 0), 0); }
}
