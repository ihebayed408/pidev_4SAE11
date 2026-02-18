import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OfferService, Offer } from '../../../core/services/offer.service';

@Component({
  selector: 'app-browse-offers',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './browse-offers.html',
  styleUrl: './browse-offers.scss',
})
export class BrowseOffers implements OnInit {
  offers: Offer[] = [];
  loading = false;
  page = 0;
  size = 12;
  totalElements = 0;
  totalPages = 0;

  constructor(private offerService: OfferService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.offerService.getActiveOffers(this.page, this.size).subscribe((p) => {
      this.offers = p.content ?? [];
      this.totalElements = p.totalElements ?? 0;
      this.totalPages = p.totalPages ?? 0;
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  prev(): void {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }

  next(): void {
    if (this.page < this.totalPages - 1) {
      this.page++;
      this.load();
    }
  }
}
