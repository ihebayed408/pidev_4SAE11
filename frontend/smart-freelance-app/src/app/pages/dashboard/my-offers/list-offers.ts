import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, Offer } from '../../../core/services/offer.service';

@Component({
  selector: 'app-list-offers',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './list-offers.html',
  styleUrl: './list-offers.scss',
})
export class ListOffers implements OnInit {
  offers: Offer[] = [];
  loading = true;
  errorMessage = '';
  currentUser: User | null = null;
  offerToDelete: Offer | null = null;
  deleting = false;
  offerToPublish: Offer | null = null;
  publishing = false;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
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
      if (this.currentUser) this.loadOffers();
      else {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
      }
    });
  }

  loadOffers(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.offerService.getOffersByFreelancer(this.currentUser.id).subscribe({
      next: (list) => {
        this.offers = list ?? [];
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

  openDeleteModal(offer: Offer): void {
    this.offerToDelete = offer;
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.offerToDelete = null;
  }

  doDelete(): void {
    if (!this.offerToDelete || !this.currentUser?.id) return;
    this.deleting = true;
    this.offerService.deleteOffer(this.offerToDelete.id, this.currentUser.id).subscribe({
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

  openPublishModal(offer: Offer): void {
    this.offerToPublish = offer;
  }

  closePublishModal(): void {
    if (!this.publishing) this.offerToPublish = null;
  }

  doPublish(): void {
    if (!this.offerToPublish || !this.currentUser?.id) return;
    this.publishing = true;
    this.offerService.publishOffer(this.offerToPublish.id, this.currentUser.id).subscribe((updated) => {
      this.publishing = false;
      this.offerToPublish = null;
      if (updated) this.loadOffers();
      else this.errorMessage = 'Failed to publish offer.';
      this.cdr.detectChanges();
    });
  }
}
