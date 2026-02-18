import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService, User } from '../../../../core/services/user.service';
import { ReviewService, Review } from '../../../../core/services/review.service';
import { ProjectService } from '../../../../core/services/project.service';

@Component({
  selector: 'app-my-reviews',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-reviews.html',
  styleUrl: './my-reviews.scss',
})
export class MyReviews implements OnInit {
  reviews: Review[] = [];
  loading = true;
  errorMessage = '';
  currentUser: User | null = null;
  reviewToDelete: Review | null = null;
  deleting = false;
  revieweeNames: Record<number, string> = {};
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
    this.reviewService.getByReviewerId(this.currentUser.id).subscribe({
      next: (list) => {
        this.reviews = list ?? [];
        this.loadRevieweeNames();
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

  private loadRevieweeNames(): void {
    const ids = [...new Set(this.reviews.map((r) => r.revieweeId))];
    ids.forEach((id) => {
      if (this.revieweeNames[id]) return;
      this.userService.getById(id).subscribe((u) => {
        if (u) this.revieweeNames[id] = `${u.firstName} ${u.lastName}`.trim() || u.email;
        else this.revieweeNames[id] = `User #${id}`;
        this.cdr.detectChanges();
      });
    });
  }

  getRevieweeName(r: Review): string {
    return this.revieweeNames[r.revieweeId] ?? `User #${r.revieweeId}`;
  }

  private loadProjectTitles(): void {
    const ids = [...new Set(this.reviews.map((r) => r.projectId))];
    ids.forEach((id) => {
      if (this.projectTitles[id]) return;
      this.projectService.getById(id).subscribe((p) => {
        this.projectTitles[id] = p?.title ?? `Project #${id}`;
        this.cdr.detectChanges();
      });
    });
  }

  getProjectTitle(r: Review): string {
    return this.projectTitles[r.projectId] ?? `Project #${r.projectId}`;
  }

  openDeleteModal(review: Review): void {
    this.reviewToDelete = review;
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.reviewToDelete = null;
  }

  doDelete(): void {
    if (!this.reviewToDelete?.id) return;
    this.deleting = true;
    this.reviewService.delete(this.reviewToDelete.id).subscribe((ok) => {
      this.deleting = false;
      this.reviewToDelete = null;
      if (ok) this.load();
      else this.errorMessage = 'Failed to delete review.';
      this.cdr.detectChanges();
    });
  }
}
