import {
  Component, OnInit, OnDestroy, AfterViewInit,
  ViewChild, ElementRef, ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { AuthService } from '../../../core/services/auth.service';
import {
  FreelancerService,
  FreelancerProfile,
  ProfileSkill,
} from '../../../core/services/freelancer.service';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard-freelancer-portfolio',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './freelancer-portfolio.html',
  styleUrl: './freelancer-portfolio.scss',
})
export class DashboardFreelancerPortfolio implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('visitChartCanvas') chartCanvasRef!: ElementRef<HTMLCanvasElement>;

  profile: FreelancerProfile | null = null;
  isLoading = true;
  notFound = false;

  /** True when the viewer is the profile owner — shows Engagement Metrics panel */
  get isOwner(): boolean {
    return this.auth.getUserId() === this.profile?.userId;
  }

  private chart: Chart | null = null;
  private chartPending = false;

  constructor(
    public auth: AuthService,
    private freelancerSvc: FreelancerService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  // ── Lifecycle ───────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) { this.notFound = true; this.isLoading = false; return; }

    this.freelancerSvc.getFreelancerProfile(id).subscribe({
      next: p => {
        if (!p) { this.notFound = true; this.isLoading = false; this.cdr.detectChanges(); return; }
        this.profile = p;
        this.isLoading = false;
        this.cdr.detectChanges();
        if (this.chartPending && this.isOwner) this.drawChart();
      },
      error: () => { this.notFound = true; this.isLoading = false; this.cdr.detectChanges(); },
    });
  }

  ngAfterViewInit(): void {
    this.chartPending = true;
    if (this.isOwner && this.profile) this.drawChart();
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  // ── Chart ───────────────────────────────────────────────────────────────────

  private drawChart(): void {
    const canvas = this.chartCanvasRef?.nativeElement;
    if (!canvas || !this.profile) return;
    this.chart?.destroy();
    const visits = this.profile.metrics.dailyVisits;
    this.chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels: visits.map(v => v.date),
        datasets: [{
          label: 'Daily Visits',
          data: visits.map(v => v.visits),
          borderColor: '#E37E33',
          backgroundColor: 'rgba(227,126,51,.1)',
          borderWidth: 2.5,
          pointRadius: 0,
          pointHoverRadius: 5,
          tension: 0.4,
          fill: true,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#fff',
            titleColor: '#1e293b',
            bodyColor: '#E37E33',
            borderColor: '#e2e8f0',
            borderWidth: 1,
            padding: 10,
          },
        },
        scales: {
          x: { grid: { display: false }, ticks: { color: '#94a3b8', font: { size: 11 }, maxTicksLimit: 8 } },
          y: { grid: { color: '#f1f5f9' }, ticks: { color: '#94a3b8', font: { size: 11 } }, beginAtZero: false },
        },
      },
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  goBack(): void { this.router.navigate(['/dashboard/freelancer-search']); }

  initials(p: FreelancerProfile): string {
    return (p.firstName[0] + p.lastName[0]).toUpperCase();
  }

  avatarGradient(userId: number): string {
    const grads = [
      'linear-gradient(135deg,#667eea,#764ba2)',
      'linear-gradient(135deg,#E37E33,#E23D59)',
      'linear-gradient(135deg,#4facfe,#00f2fe)',
      'linear-gradient(135deg,#43e97b,#38f9d7)',
      'linear-gradient(135deg,#fa709a,#fee140)',
      'linear-gradient(135deg,#a18cd1,#fbc2eb)',
      'linear-gradient(135deg,#fda085,#f6d365)',
      'linear-gradient(135deg,#89f7fe,#66a6ff)',
    ];
    return grads[userId % grads.length];
  }

  stars(): number[] { return [1, 2, 3, 4, 5]; }
  isFilled(s: number, r: number): boolean { return s <= Math.round(r); }

  skillsByCategory(skills: ProfileSkill[]): { cat: string; items: ProfileSkill[] }[] {
    const map = new Map<string, ProfileSkill[]>();
    skills.forEach(sk => {
      if (!map.has(sk.category)) map.set(sk.category, []);
      map.get(sk.category)!.push(sk);
    });
    return Array.from(map.entries()).map(([cat, items]) => ({ cat, items }));
  }

  levelWidth(level: string): string {
    return level === 'Expert' ? '92%' : level === 'Intermediate' ? '60%' : '28%';
  }

  levelColor(level: string): string {
    return level === 'Expert' ? '#E37E33' : level === 'Intermediate' ? '#3b82f6' : '#94a3b8';
  }

  weekChange(p: FreelancerProfile): { pct: string; up: boolean } {
    const { thisWeekVisits: tw, lastWeekVisits: lw } = p.metrics;
    const pct = lw > 0 ? (((tw - lw) / lw) * 100).toFixed(1) : '0.0';
    return { pct: Math.abs(Number(pct)).toFixed(1), up: tw >= lw };
  }

  formatNum(n: number): string { return n.toLocaleString(); }

  hireFreelancer(p: FreelancerProfile): void {
    this.router.navigate(['/dashboard/my-projects/add']);
  }
}
