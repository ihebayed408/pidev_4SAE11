import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, Offer, OfferApplication } from '../../../core/services/offer.service';

@Component({
  selector: 'app-show-offer',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './show-offer.html',
  styleUrl: './show-offer.scss',
})
export class ShowOffer implements OnInit {
  offer: Offer | null = null;
  applications: OfferApplication[] = [];
  loading = true;
  loadingApps = false;
  errorMessage = '';
  actioning = false;
  currentUser: User | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.errorMessage = 'You must be logged in.';
      this.cdr.detectChanges();
      return;
    }
    this.userService.getByEmail(email).subscribe((u) => {
      this.currentUser = u ?? null;
      if (!this.currentUser) {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
        return;
      }
      this.offerService.getOfferById(id).subscribe((offer) => {
        this.offer = offer ?? null;
        this.loading = false;
        if (this.offer && this.currentUser != null && this.offer.freelancerId === this.currentUser.id) {
          this.loadApplications();
        } else if (this.offer) {
          this.errorMessage = 'You do not own this offer.';
        } else {
          this.errorMessage = 'Offer not found.';
        }
        this.cdr.detectChanges();
      });
    });
  }

  loadApplications(): void {
    if (!this.offer?.id || !this.currentUser?.id) return;
    this.loadingApps = true;
    this.offerService.getApplicationsByOffer(this.offer.id, 0, 50).subscribe((page) => {
      this.applications = page.content ?? [];
      this.loadingApps = false;
      this.cdr.detectChanges();
    });
  }

  formatDate(s: string): string {
    if (!s) return '';
    const d = new Date(s);
    return d.toLocaleDateString();
  }

  accept(app: OfferApplication): void {
    if (!this.currentUser?.id) return;
    this.actioning = true;
    this.offerService.acceptApplication(app.id, this.currentUser.id).subscribe(() => {
      this.actioning = false;
      this.loadApplications();
      this.cdr.detectChanges();
    });
  }

  reject(app: OfferApplication): void {
    if (!this.currentUser?.id) return;
    this.actioning = true;
    this.offerService.rejectApplication(app.id, this.currentUser.id).subscribe(() => {
      this.actioning = false;
      this.loadApplications();
      this.cdr.detectChanges();
    });
  }

  createProject(app: OfferApplication): void {
    if (!this.offer) return;
    this.router.navigate(['/dashboard/my-projects/add'], {
      state: { fromOffer: true, offer: this.offer, application: app },
    });
  }
}
