import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ReviewService, Review } from '../../../core/services/review.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-review-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './review-management.html',
  styleUrl: './review-management.scss',
})
export class ReviewManagement implements OnInit {
  reviews: Review[] = [];
  loading = true;
  errorMessage = '';
  reviewToDelete: Review | null = null;
  deleting = false;
  addForm: FormGroup;
  editForm: FormGroup;
  addModalOpen = false;
  editingReview: Review | null = null;
  saving = false;
  adding = false;

  constructor(
    private reviewService: ReviewService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.addForm = this.fb.group({
      reviewerId: [null as number | null, [Validators.required, Validators.min(1)]],
      revieweeId: [null as number | null, [Validators.required, Validators.min(1)]],
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: [''],
    });
    this.editForm = this.fb.group({
      reviewerId: [null as number | null, [Validators.required, Validators.min(1)]],
      revieweeId: [null as number | null, [Validators.required, Validators.min(1)]],
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: [''],
    });
  }

  ngOnInit(): void {
    this.loadReviews();
  }

  loadReviews(): void {
    this.loading = true;
    this.errorMessage = '';
    this.reviewService.getAll().subscribe({
      next: (list) => {
        this.reviews = list ?? [];
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

  openAdd(): void {
    this.errorMessage = '';
    this.addForm.reset({
      reviewerId: null,
      revieweeId: null,
      projectId: null,
      rating: 5,
      comment: '',
    });
    this.addModalOpen = true;
  }

  closeAdd(): void {
    if (!this.adding) this.addModalOpen = false;
  }

  saveAdd(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const v = this.addForm.getRawValue();
    const review: Review = {
      reviewerId: Number(v.reviewerId),
      revieweeId: Number(v.revieweeId),
      projectId: Number(v.projectId),
      rating: Number(v.rating),
      comment: v.comment?.trim() || null,
    };
    this.adding = true;
    this.reviewService.create(review).subscribe({
      next: (created) => {
        this.adding = false;
        if (created) {
          this.addModalOpen = false;
          this.loadReviews();
        } else this.errorMessage = 'Failed to create review.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.adding = false;
        this.errorMessage = 'Failed to create review.';
        this.cdr.detectChanges();
      },
    });
  }

  openEdit(review: Review): void {
    this.editingReview = review;
    this.reviewToDelete = null;
    this.editForm.patchValue({
      reviewerId: review.reviewerId,
      revieweeId: review.revieweeId,
      projectId: review.projectId,
      rating: review.rating,
      comment: review.comment ?? '',
    });
  }

  closeEdit(): void {
    if (!this.saving) this.editingReview = null;
  }

  saveEdit(): void {
    if (!this.editingReview?.id || this.editForm.invalid) return;
    const v = this.editForm.getRawValue();
    const payload: Partial<Review> = {
      reviewerId: Number(v.reviewerId),
      revieweeId: Number(v.revieweeId),
      projectId: Number(v.projectId),
      rating: Number(v.rating),
      comment: v.comment?.trim() || null,
    };
    this.saving = true;
    this.reviewService.update(this.editingReview.id, payload).subscribe({
      next: (updated) => {
        this.saving = false;
        if (updated) {
          const idx = this.reviews.findIndex((r) => r.id === this.editingReview!.id);
          if (idx !== -1) this.reviews[idx] = { ...this.reviews[idx], ...updated };
          this.editingReview = null;
        } else this.errorMessage = 'Failed to update review.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to update review.';
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(review: Review): void {
    this.reviewToDelete = review;
    this.closeEdit();
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.reviewToDelete = null;
  }

  doDelete(): void {
    if (!this.reviewToDelete?.id) return;
    this.deleting = true;
    this.reviewService.delete(this.reviewToDelete.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.reviewToDelete = null;
        if (ok) this.loadReviews();
        else this.errorMessage = 'Failed to delete review.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.errorMessage = 'Failed to delete review.';
        this.cdr.detectChanges();
      },
    });
  }
}
