import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, OfferApplication } from '../../../core/services/offer.service';

@Component({
  selector: 'app-my-offer-applications',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-offer-applications.html',
  styleUrl: './my-offer-applications.scss',
})
export class MyOfferApplications implements OnInit {
  applications: OfferApplication[] = [];
  loading = false;
  currentUser: User | null = null;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.cdr.detectChanges();
      return;
    }
    this.userService.getByEmail(email).subscribe((u) => {
      this.currentUser = u ?? null;
      if (this.currentUser) this.load();
      this.cdr.detectChanges();
    });
  }

  load(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.offerService.getApplicationsByClient(this.currentUser.id, 0, 100).subscribe((page) => {
      this.applications = page.content ?? [];
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  formatDate(s: string): string {
    if (!s) return '';
    return new Date(s).toLocaleDateString();
  }
}
