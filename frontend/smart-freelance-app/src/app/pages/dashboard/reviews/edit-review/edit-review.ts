import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService, User } from '../../../../core/services/user.service';
import { ReviewService, Review } from '../../../../core/services/review.service';

@Component({
  selector: 'app-edit-review',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './edit-review.html',
  styleUrl: './edit-review.scss',
})
export class EditReview implements OnInit {
  id!: number;
  form!: FormGroup;
  loading = true;
  isSubmitting = false;
  submitError: string | null = null;
  submitSuccess = false;
  currentUser: User | null = null;
  revieweeLabel = '';
  revieweeId: number | null = null;
  projectId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private auth: AuthService,
    private userService: UserService,
    private reviewService: ReviewService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.form = this.fb.group({
      rating: [null as number | null, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: ['', [Validators.maxLength(1000)]],
    });
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.submitError = 'You must be logged in.';
      return;
    }
    this.userService.getByEmail(email).subscribe((user) => {
      this.currentUser = user ?? null;
      if (!this.currentUser) {
        this.loading = false;
        this.submitError = 'Could not load your profile.';
        return;
      }
      this.loadReview();
    });
  }

  loadReview(): void {
    this.reviewService.getById(this.id).subscribe({
      next: (review) => {
        if (!review) {
          this.submitError = 'Review not found.';
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }
        if (review.reviewerId !== this.currentUser!.id) {
          this.submitError = 'You can only edit your own review.';
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }
        this.revieweeId = review.revieweeId;
        this.projectId = review.projectId ?? null;
        this.form.patchValue({
          rating: review.rating,
          comment: review.comment ?? '',
        });
        this.userService.getById(review.revieweeId).subscribe((u) => {
          if (u) this.revieweeLabel = (u.firstName + ' ' + u.lastName).trim() || u.email;
          else this.revieweeLabel = 'User #' + review.revieweeId;
          this.cdr.detectChanges();
        });
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.submitError = 'Failed to load review.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  submit(): void {
    if (this.form.invalid || !this.currentUser?.id) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.submitError = null;
    const payload: Partial<Review> = {
      reviewerId: this.currentUser.id,
      revieweeId: this.revieweeId ?? undefined,
      projectId: this.projectId ?? undefined,
      rating: Number(this.form.value.rating),
      comment: (this.form.value.comment || '').trim() || undefined,
    };
    this.reviewService.update(this.id, payload).subscribe({
      next: () => {
        this.submitSuccess = true;
        this.isSubmitting = false;
        setTimeout(() => this.router.navigateByUrl('/dashboard/reviews'), 1500);
      },
      error: () => {
        this.submitError = 'Failed to update review. Please try again.';
        this.isSubmitting = false;
      },
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/dashboard/reviews');
  }
}
