import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

const OFFER_API = `${environment.apiGatewayUrl}/offer/api/offers`;
const APPLICATION_API = `${environment.apiGatewayUrl}/offer/api/applications`;

export type OfferStatus =
  | 'DRAFT'
  | 'AVAILABLE'
  | 'IN_PROGRESS'
  | 'ACCEPTED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'CLOSED';

export type ApplicationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN' | 'SHORTLISTED';

export interface Offer {
  id: number;
  freelancerId: number;
  title: string;
  domain: string;
  description: string;
  price: number;
  durationType: 'hourly' | 'fixed' | 'monthly';
  offerStatus: OfferStatus;
  deadline?: string;
  category?: string;
  rating?: number;
  communicationScore?: number;
  tags?: string;
  imageUrl?: string;
  viewsCount?: number;
  isFeatured?: boolean;
  isActive?: boolean;
  createdAt: string;
  updatedAt?: string;
  publishedAt?: string;
  expiredAt?: string;
  applicationsCount?: number;
  pendingApplicationsCount?: number;
  canReceiveApplications?: boolean;
  isValid?: boolean;
}

export interface OfferRequest {
  freelancerId: number;
  title: string;
  domain: string;
  description: string;
  price: number;
  durationType: 'hourly' | 'fixed' | 'monthly';
  deadline?: string;
  category?: string;
  tags?: string;
  imageUrl?: string;
  isFeatured?: boolean;
}

export interface OfferFilterRequest {
  keyword?: string;
  domain?: string;
  category?: string;
  offerStatus?: OfferStatus;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  durationType?: string;
  isFeatured?: boolean;
  isActive?: boolean;
  freelancerId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface OfferApplication {
  id: number;
  offerId: number;
  offerTitle?: string;
  clientId: number;
  message: string;
  proposedBudget: number;
  portfolioUrl?: string;
  attachmentUrl?: string;
  estimatedDuration?: number;
  status: ApplicationStatus;
  rejectionReason?: string;
  isRead?: boolean;
  appliedAt: string;
  respondedAt?: string;
  acceptedAt?: string;
  canBeModified?: boolean;
}

export interface OfferApplicationRequest {
  offerId: number;
  clientId: number;
  message: string;
  proposedBudget: number;
  portfolioUrl?: string;
  attachmentUrl?: string;
  estimatedDuration?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

@Injectable({ providedIn: 'root' })
export class OfferService {
  constructor(private http: HttpClient) {}

  // ---------- Offers ----------
  getActiveOffers(page = 0, size = 10): Observable<PageResponse<Offer>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<Offer>>(OFFER_API, { params }).pipe(
      catchError(() => of({ content: [], totalElements: 0, totalPages: 0, size, number: page, first: true, last: true }))
    );
  }

  getOfferById(id: number): Observable<Offer | null> {
    return this.http.get<Offer>(`${OFFER_API}/${id}`).pipe(catchError(() => of(null)));
  }

  getOffersByFreelancer(freelancerId: number): Observable<Offer[]> {
    return this.http.get<Offer[]>(`${OFFER_API}/freelancer/${freelancerId}`).pipe(catchError(() => of([])));
  }

  getFeaturedOffers(): Observable<Offer[]> {
    return this.http.get<Offer[]>(`${OFFER_API}/featured`).pipe(catchError(() => of([])));
  }

  searchOffers(filter: OfferFilterRequest): Observable<PageResponse<Offer>> {
    return this.http.post<PageResponse<Offer>>(`${OFFER_API}/search`, filter).pipe(
      catchError(() =>
        of({
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: filter.size ?? 10,
          number: filter.page ?? 0,
          first: true,
          last: true,
        })
      )
    );
  }

  createOffer(request: OfferRequest): Observable<Offer | null> {
    return this.http.post<Offer>(OFFER_API, request).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  updateOffer(id: number, request: OfferRequest): Observable<Offer | null> {
    return this.http.put<Offer>(`${OFFER_API}/${id}`, request).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  publishOffer(id: number, freelancerId: number): Observable<Offer | null> {
    return this.http
      .patch<Offer>(`${OFFER_API}/${id}/publish`, null, {
        params: { freelancerId: String(freelancerId) },
      })
      .pipe(catchError(() => of(null)));
  }

  changeOfferStatus(id: number, status: OfferStatus, freelancerId: number): Observable<Offer | null> {
    return this.http
      .patch<Offer>(`${OFFER_API}/${id}/status`, null, {
        params: { status, freelancerId: String(freelancerId) },
      })
      .pipe(catchError(() => of(null)));
  }

  deleteOffer(id: number, freelancerId: number): Observable<boolean> {
    return this.http
      .delete(`${OFFER_API}/${id}`, { params: { freelancerId: String(freelancerId) }, observe: 'response' })
      .pipe(
        map((res) => res.status >= 200 && res.status < 300),
        catchError((err) => throwError(() => err))
      );
  }

  // ---------- Applications ----------
  applyToOffer(request: OfferApplicationRequest): Observable<OfferApplication | null> {
    return this.http.post<OfferApplication>(APPLICATION_API, request).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  getApplicationById(id: number): Observable<OfferApplication | null> {
    return this.http.get<OfferApplication>(`${APPLICATION_API}/${id}`).pipe(catchError(() => of(null)));
  }

  getApplicationsByOffer(offerId: number, page = 0, size = 20): Observable<PageResponse<OfferApplication>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<OfferApplication>>(`${APPLICATION_API}/offer/${offerId}`, { params }).pipe(
      catchError(() =>
        of({ content: [], totalElements: 0, totalPages: 0, size, number: page, first: true, last: true })
      )
    );
  }

  getApplicationsByClient(clientId: number, page = 0, size = 20): Observable<PageResponse<OfferApplication>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<OfferApplication>>(`${APPLICATION_API}/client/${clientId}`, { params }).pipe(
      catchError(() =>
        of({ content: [], totalElements: 0, totalPages: 0, size, number: page, first: true, last: true })
      )
    );
  }

  acceptApplication(id: number, freelancerId: number): Observable<OfferApplication | null> {
    return this.http
      .patch<OfferApplication>(`${APPLICATION_API}/${id}/accept`, null, {
        params: { freelancerId: String(freelancerId) },
      })
      .pipe(catchError(() => of(null)));
  }

  rejectApplication(id: number, freelancerId: number, reason?: string): Observable<OfferApplication | null> {
    let params: HttpParams = new HttpParams().set('freelancerId', String(freelancerId));
    if (reason != null && reason !== '') params = params.set('reason', reason);
    return this.http.patch<OfferApplication>(`${APPLICATION_API}/${id}/reject`, null, { params }).pipe(catchError(() => of(null)));
  }

  markApplicationAsRead(id: number, freelancerId: number): Observable<OfferApplication | null> {
    return this.http
      .patch<OfferApplication>(`${APPLICATION_API}/${id}/mark-read`, null, {
        params: { freelancerId: String(freelancerId) },
      })
      .pipe(catchError(() => of(null)));
  }

  withdrawApplication(id: number, clientId: number): Observable<OfferApplication | null> {
    return this.http
      .patch<OfferApplication>(`${APPLICATION_API}/${id}/withdraw`, null, {
        params: { clientId: String(clientId) },
      })
      .pipe(catchError(() => of(null)));
  }
}
