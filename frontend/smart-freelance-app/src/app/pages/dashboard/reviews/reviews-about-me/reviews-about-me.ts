import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService, User } from '../../../../core/services/user.service';
import { ReviewService, Review } from '../../../../core/services/review.service';
import { ProjectService } from '../../../../core/services/project.service';

@Component({
  selector: 'app-reviews-about-me',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reviews-about-me.html',
  styleUrl: './reviews-about-me.scss',
})
export class ReviewsAboutMe implements OnInit {
  reviews: Review[] = [];
  loading = true;
  errorMessage = '';
  currentUser: User | null = null;
  reviewerNames: Record<number, string> = {};
  projectTitles: Record<number, string> = {};

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private reviewService: ReviewService,
    private projectService: ProjectService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.errorMessage = 'You must be logged in.';
      this.cdr.detectChanges();
      return;
    }
    this.userService.getByEmail(email).subscribe((user) => {
      this.currentUser = user ?? null;
      if (this.currentUser) this.load();
      else {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
      }
    });
  }

  load(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.reviewService.getByRevieweeId(this.currentUser.id).subscribe({
      next: (list) => {
        this.reviews = list ?? [];
        this.loadReviewerNames();
        this.loadProjectTitles();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load reviews.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private loadReviewerNames(): void {
    const ids = [...new Set(this.reviews.map((r) => r.reviewerId))];
    ids.forEach((id) => {
      if (this.reviewerNames[id]) return;
      this.userService.getById(id).subscribe((u) => {
        if (u) this.reviewerNames[id] = (u.firstName + ' ' + u.lastName).trim() || u.email;
        else this.reviewerNames[id] = 'User #' + id;
        this.cdr.detectChanges();
      });
    });
  }

  getReviewerName(r: Review): string {
    return this.reviewerNames[r.reviewerId] ?? 'User #' + r.reviewerId;
  }

  private loadProjectTitles(): void {
    const ids = [...new Set(this.reviews.map((r) => r.projectId))];
    ids.forEach((id) => {
      if (this.projectTitles[id]) return;
      this.projectService.getById(id).subscribe((p) => {
        this.projectTitles[id] = p?.title ?? 'Project #' + id;
        this.cdr.detectChanges();
      });
    });
  }

  getProjectTitle(r: Review): string {
    return this.projectTitles[r.projectId] ?? 'Project #' + r.projectId;
  }
}
