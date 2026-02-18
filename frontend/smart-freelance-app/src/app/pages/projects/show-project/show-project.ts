import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService, Project } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { ReviewService } from '../../../core/services/review.service';

@Component({
  selector: 'app-show-project',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './show-project.html',
  styleUrl: './show-project.scss',
})
export class ShowProject implements OnInit {
  project: Project | null = null;
  isLoading = true;
  errorMessage: string | null = null;
  id!: number;
  currentUser: User | null = null;
  canLeaveReview = false;
  revieweeId: number | null = null;
  revieweeLabel = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private auth: AuthService,
    private userService: UserService,
    private reviewService: ReviewService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id || Number.isNaN(this.id)) {
      this.errorMessage = 'Invalid project.';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }
    this.loadProject();
  }

  loadProject(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.cdr.detectChanges();

    this.projectService.getProjectById(this.id).subscribe({
      next: (res: Project | null) => {
        this.project = res ?? null;
        if (!this.project) {
          this.errorMessage = 'Project not found.';
          this.isLoading = false;
          this.cdr.detectChanges();
          return;
        }
        const email = this.auth.getPreferredUsername();
        if (email) {
          this.userService.getByEmail(email).subscribe({
            next: (user) => {
              this.currentUser = user ?? null;
              if (this.currentUser && this.project) this.checkCanLeaveReview();
              this.isLoading = false;
              this.cdr.detectChanges();
            },
            error: () => {
              this.isLoading = false;
              this.cdr.detectChanges();
            },
          });
        } else {
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      },
      error: () => {
        this.errorMessage = 'Failed to load project details.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private checkCanLeaveReview(): void {
    if (!this.project || !this.currentUser?.id) return;
    const reviewee = this.currentUser.role === 'CLIENT' ? this.project.freelancerId : this.project.clientId;
    if (reviewee == null) return;
    const isMyProject = this.currentUser.role === 'CLIENT'
      ? Number(this.project.clientId) === this.currentUser.id
      : Number(this.project.freelancerId) === this.currentUser.id;
    if (!isMyProject) return;
    this.reviewService.getByReviewerId(this.currentUser.id).subscribe((reviews) => {
      const alreadyReviewed = (reviews ?? []).some((r) => Number(r.projectId) === Number(this.project!.id));
      this.canLeaveReview = !alreadyReviewed;
      this.revieweeId = reviewee;
      this.userService.getById(reviewee).subscribe((u) => {
        this.revieweeLabel = u ? (u.firstName + ' ' + u.lastName).trim() || u.email : 'User #' + reviewee;
        this.cdr.detectChanges();
      });
      this.cdr.detectChanges();
    });
  }

  leaveReview(): void {
    if (this.revieweeId == null || !this.project?.id) return;
    this.router.navigate(['/dashboard/reviews/add'], {
      state: { projectId: this.project.id, revieweeId: this.revieweeId, revieweeLabel: this.revieweeLabel },
    });
  }

  getSkills(): string[] {
    const skills = this.project?.skillsRequiered;
    if (!skills) return [];
    const str = Array.isArray(skills) ? (skills as string[]).join(',') : String(skills);
    return str
      .split(',')
      .map((s: string) => s.trim())
      .filter((s: string) => s.length > 0);
  }
}
