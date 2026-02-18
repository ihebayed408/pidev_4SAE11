import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { OfferService, Offer, OfferRequest, OfferStatus } from '../../../core/services/offer.service';
import { Card } from '../../../shared/components/card/card';

const DURATION_TYPES = ['hourly', 'fixed', 'monthly'] as const;
const OFFER_STATUSES: OfferStatus[] = ['DRAFT', 'AVAILABLE', 'IN_PROGRESS', 'ACCEPTED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'CLOSED'];

@Component({
  selector: 'app-offer-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './offer-management.html',
  styleUrl: './offer-management.scss',
})
export class OfferManagement implements OnInit {
  offers: Offer[] = [];
  loading = true;
  errorMessage = '';
  offerToDelete: Offer | null = null;
  deleting = false;
  addForm: FormGroup;
  editForm: FormGroup;
  addModalOpen = false;
  editingOffer: Offer | null = null;
  saving = false;
  adding = false;
  durationTypes = DURATION_TYPES;
  offerStatuses = OFFER_STATUSES;

  constructor(
    private offerService: OfferService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.addForm = this.fb.group({
      freelancerId: [null as number | null, [Validators.required, Validators.min(1)]],
      title: ['', [Validators.required, Validators.minLength(5)]],
      domain: ['', Validators.required],
      description: ['', [Validators.required, Validators.minLength(20)]],
      price: [1, [Validators.required, Validators.min(0.01)]],
      durationType: ['fixed' as const, Validators.required],
      deadline: [''],
      category: [''],
      tags: [''],
      imageUrl: [''],
      isFeatured: [false],
    });
    this.editForm = this.fb.group({
      freelancerId: [null as number | null, [Validators.required, Validators.min(1)]],
      title: ['', [Validators.required, Validators.minLength(5)]],
      domain: ['', Validators.required],
      description: ['', [Validators.required, Validators.minLength(20)]],
      price: [1, [Validators.required, Validators.min(0.01)]],
      durationType: ['fixed' as const, Validators.required],
      deadline: [''],
      category: [''],
      tags: [''],
      imageUrl: [''],
      isFeatured: [false],
      offerStatus: ['DRAFT' as OfferStatus],
    });
  }

  ngOnInit(): void {
    this.loadOffers();
  }

  loadOffers(): void {
    this.loading = true;
    this.errorMessage = '';
    this.offerService.searchOffers({ page: 0, size: 500 }).subscribe({
      next: (page) => {
        this.offers = page?.content ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load offers.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openAdd(): void {
    this.errorMessage = '';
    this.addForm.reset({
      freelancerId: null,
      title: '',
      domain: '',
      description: '',
      price: 0,
      durationType: 'fixed',
      deadline: '',
      category: '',
      tags: '',
      imageUrl: '',
      isFeatured: false,
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
    const price = Number(v.price);
    const request: OfferRequest = {
      freelancerId: Number(v.freelancerId),
      title: v.title.trim(),
      domain: v.domain.trim(),
      description: v.description.trim(),
      price: isNaN(price) || price < 0.01 ? 0.01 : price,
      durationType: v.durationType,
      deadline: v.deadline?.trim() || undefined,
      category: v.category?.trim() || undefined,
      tags: v.tags?.trim() || undefined,
      imageUrl: v.imageUrl?.trim() || undefined,
      isFeatured: !!v.isFeatured,
    };
    this.adding = true;
    this.offerService.createOffer(request).subscribe({
      next: (created) => {
        this.adding = false;
        if (created) {
          this.addModalOpen = false;
          this.loadOffers();
        } else this.errorMessage = 'Failed to create offer.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.adding = false;
        this.errorMessage = 'Failed to create offer.';
        this.cdr.detectChanges();
      },
    });
  }

  openEdit(offer: Offer): void {
    this.editingOffer = offer;
    this.offerToDelete = null;
    this.editForm.patchValue({
      freelancerId: offer.freelancerId,
      title: offer.title,
      domain: offer.domain,
      description: offer.description,
      price: offer.price,
      durationType: offer.durationType,
      deadline: offer.deadline ?? '',
      category: offer.category ?? '',
      tags: offer.tags ?? '',
      imageUrl: offer.imageUrl ?? '',
      isFeatured: offer.isFeatured ?? false,
      offerStatus: offer.offerStatus,
    });
  }

  closeEdit(): void {
    if (!this.saving) this.editingOffer = null;
  }

  saveEdit(): void {
    if (!this.editingOffer || this.editForm.invalid) return;
    const v = this.editForm.getRawValue();
    const freelancerId = v.freelancerId != null && v.freelancerId !== '' ? Number(v.freelancerId) : this.editingOffer.freelancerId;
    const newStatus = v.offerStatus as OfferStatus;
    const statusChanged = this.editingOffer.offerStatus !== newStatus;
    const offerId = this.editingOffer.id;
    const price = Number(v.price);
    const request: OfferRequest = {
      freelancerId,
      title: v.title.trim(),
      domain: v.domain.trim(),
      description: v.description.trim(),
      price: isNaN(price) || price < 0.01 ? 0.01 : price,
      durationType: v.durationType,
      deadline: v.deadline?.trim() || undefined,
      category: v.category?.trim() || undefined,
      tags: v.tags?.trim() || undefined,
      imageUrl: v.imageUrl?.trim() || undefined,
      isFeatured: !!v.isFeatured,
    };
    this.saving = true;
    this.errorMessage = '';
    this.offerService.updateOffer(this.editingOffer.id, request).subscribe({
      next: (updated) => {
        if (!updated) {
          this.saving = false;
          this.errorMessage = 'Failed to update offer.';
          this.cdr.detectChanges();
          return;
        }

        const idx = this.offers.findIndex((o) => o.id === offerId);
        if (idx !== -1) {
          this.offers[idx] = { ...this.offers[idx], ...updated };
        }

        if (!statusChanged) {
          this.saving = false;
          this.editingOffer = null;
          this.cdr.detectChanges();
          return;
        }

        // If status changed, call backend status endpoint after main update
        this.offerService.changeOfferStatus(offerId, newStatus, freelancerId).subscribe({
          next: (statusUpdated) => {
            this.saving = false;
            if (statusUpdated && idx !== -1) {
              this.offers[idx] = { ...this.offers[idx], ...statusUpdated };
            }
            this.editingOffer = null;
            this.cdr.detectChanges();
          },
          error: (err2) => {
            this.saving = false;
            const msg2 =
              err2?.error?.message ??
              err2?.error?.error ??
              (Array.isArray(err2?.error?.errors)
                ? err2.error.errors.map((e: { defaultMessage?: string }) => e.defaultMessage).join(', ')
                : null);
            this.errorMessage = msg2 || 'Offer updated but failed to change status.';
            this.editingOffer = null;
            this.cdr.detectChanges();
          },
        });
      },
      error: (err) => {
        this.saving = false;
        const msg = err?.error?.message ?? err?.error?.error ?? (Array.isArray(err?.error?.errors) ? err.error.errors.map((e: { defaultMessage?: string }) => e.defaultMessage).join(', ') : null);
        this.errorMessage = msg || 'Failed to update offer.';
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(offer: Offer): void {
    this.offerToDelete = offer;
    this.closeEdit();
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.offerToDelete = null;
  }

  doDelete(): void {
    if (!this.offerToDelete?.id || this.offerToDelete.freelancerId == null) return;
    this.deleting = true;
    this.offerService.deleteOffer(this.offerToDelete.id, this.offerToDelete.freelancerId).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.offerToDelete = null;
        if (ok) this.loadOffers();
        else this.errorMessage = 'Failed to delete offer.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.deleting = false;
        this.offerToDelete = null;
        this.errorMessage = err?.error?.message || 'Failed to delete offer.';
        this.cdr.detectChanges();
      },
    });
  }
}
