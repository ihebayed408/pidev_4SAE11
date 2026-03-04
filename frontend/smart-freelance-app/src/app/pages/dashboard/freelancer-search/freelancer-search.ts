import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  FreelancerService,
  FreelancerCard,
  DUMMY_FREELANCERS,
} from '../../../core/services/freelancer.service';
import { AuthService } from '../../../core/services/auth.service';

const SKILL_FILTERS = [
  'All', 'Angular', 'React', 'Spring Boot', 'Java', 'Python',
  'DevOps', 'UI/UX Design', 'Mobile', 'Data Science',
];

@Component({
  selector: 'app-freelancer-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './freelancer-search.html',
  styleUrl: './freelancer-search.scss',
})
export class FreelancerSearch implements OnInit {
  allFreelancers: FreelancerCard[] = [];
  filtered: FreelancerCard[] = [];
  isLoading = true;

  search = '';
  activeSkill = 'All';
  sortBy: 'rating' | 'reviews' | 'rate' = 'rating';

  readonly skillFilters = SKILL_FILTERS;

  constructor(
    public auth: AuthService,
    private freelancerSvc: FreelancerService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.freelancerSvc.getAllFreelancers().subscribe({
      next: cards => {
        // If API returned empty list, use dummy data
        this.allFreelancers = cards.length
          ? cards
          : DUMMY_FREELANCERS.map(p => ({
              userId: p.userId, firstName: p.firstName, lastName: p.lastName,
              title: p.title, avatarUrl: p.avatarUrl, rating: p.rating,
              totalReviews: p.totalReviews, skills: p.skills.slice(0, 4),
            }));
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.allFreelancers = DUMMY_FREELANCERS.map(p => ({
          userId: p.userId, firstName: p.firstName, lastName: p.lastName,
          title: p.title, avatarUrl: p.avatarUrl, rating: p.rating,
          totalReviews: p.totalReviews, skills: p.skills.slice(0, 4),
        }));
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  applyFilters(): void {
    const q = this.search.toLowerCase().trim();
    let list = this.allFreelancers.filter(f => {
      const matchSearch = !q
        || `${f.firstName} ${f.lastName}`.toLowerCase().includes(q)
        || f.title.toLowerCase().includes(q)
        || f.skills.some(s => s.name.toLowerCase().includes(q));
      const matchSkill = this.activeSkill === 'All'
        || f.skills.some(s => s.name.toLowerCase().includes(this.activeSkill.toLowerCase()));
      return matchSearch && matchSkill;
    });

    list = [...list].sort((a, b) =>
      this.sortBy === 'rating' ? b.rating - a.rating :
      this.sortBy === 'reviews' ? b.totalReviews - a.totalReviews :
      0
    );
    this.filtered = list;
  }

  setSkill(skill: string): void {
    this.activeSkill = skill;
    this.applyFilters();
  }

  viewPortfolio(f: FreelancerCard): void {
    this.router.navigate(['/dashboard/freelancer-portfolio', f.userId]);
  }

  initials(f: FreelancerCard): string {
    return (f.firstName[0] + f.lastName[0]).toUpperCase();
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

  stars(n: number): number[] { return [1, 2, 3, 4, 5]; }
  isFilled(s: number, r: number): boolean { return s <= Math.round(r); }
}
