import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, Offer, OfferRequest } from '../../../core/services/offer.service';
import { timeout, catchError, of, finalize, switchMap } from 'rxjs';

@Component({
  selector: 'app-edit-offer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './edit-offer.html',
  styleUrl: './add-offer.scss',
})
export class EditOffer implements OnInit {
  id!: number;
  form: FormGroup;
  loading = true;
  isSubmitting = false;
  submitError: string | null = null;
  currentUser: User | null = null;
  minDate: string;

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    const t = new Date();
    this.minDate = t.toISOString().slice(0, 10);
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]],
      domain: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(5000)]],
      price: [null, [Validators.required, Validators.min(0.01)]],
      durationType: ['fixed', [Validators.required]],
      deadline: [''],
      category: [''],
      tags: [''],
    });
  }

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.submitError = 'You must be logged in.';
      return;
    }
    if (!Number.isFinite(this.id) || this.id < 1) {
      this.loading = false;
      this.submitError = 'Invalid offer ID.';
      return;
    }
    this.userService.getByEmail(email).pipe(
      timeout(10000),
      catchError(() => of(null)),
      switchMap((u) => {
        this.currentUser = u ?? null;
        if (!this.currentUser) {
          this.submitError = 'Could not load your profile.';
          return of(null);
        }
        return this.offerService.getOfferById(this.id).pipe(
          timeout(10000),
          catchError(() => of(null))
        );
      }),
      finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (offer) => {
        if (this.submitError) return;
        if (!offer || offer.freelancerId !== this.currentUser?.id) {
          this.submitError = 'Offer not found or you cannot edit it.';
          return;
        }
        if (offer.offerStatus !== 'DRAFT') {
          this.submitError = 'Only draft offers can be edited.';
          return;
        }
        const deadline = offer.deadline ? String(offer.deadline).slice(0, 10) : '';
        this.form.patchValue({
          title: offer.title,
          domain: offer.domain,
          description: offer.description,
          price: offer.price,
          durationType: offer.durationType,
          deadline: deadline || '',
          category: offer.category || '',
          tags: offer.tags || '',
        });
      },
      error: () => {
        if (!this.submitError) this.submitError = 'Request failed or timed out. Check your connection and that services are running.';
      }
    });
  }

  submit(): void {
    if (this.form.invalid || !this.currentUser?.id) return;
    this.form.markAllAsTouched();
    this.isSubmitting = true;
    this.submitError = null;
    const v = this.form.value;
    const req: OfferRequest = {
      freelancerId: this.currentUser.id,
      title: (v.title as string).trim(),
      domain: (v.domain as string).trim(),
      description: (v.description as string).trim(),
      price: Number(v.price),
      durationType: v.durationType,
      deadline: v.deadline ? (v.deadline as string) : undefined,
      category: (v.category as string)?.trim() || undefined,
      tags: (v.tags as string)?.trim() || undefined,
    };
    this.offerService.updateOffer(this.id, req).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.router.navigate(['/dashboard/my-offers']);
      },
      error: (err: unknown) => {
        this.isSubmitting = false;
        const e = err as { error?: { message?: string }; message?: string };
        this.submitError = e?.error?.message || e?.message || 'Failed to update offer.';
      },
    });
  }
}
