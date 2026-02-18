import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const REVIEW_API = `${environment.apiGatewayUrl}/review/api/reviews`;

export interface Review {
  id?: number;
  reviewerId: number;
  revieweeId: number;
  projectId: number;
  rating: number;
  comment?: string | null;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class ReviewService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<Review[]> {
    return this.http.get<Review[]>(REVIEW_API).pipe(
      catchError(() => of([]))
    );
  }

  getById(id: number): Observable<Review | null> {
    return this.http.get<Review>(`${REVIEW_API}/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  getByReviewerId(reviewerId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${REVIEW_API}/reviewer/${reviewerId}`).pipe(
      catchError(() => of([]))
    );
  }

  getByRevieweeId(revieweeId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${REVIEW_API}/reviewee/${revieweeId}`).pipe(
      catchError(() => of([]))
    );
  }

  getByProjectId(projectId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${REVIEW_API}/project/${projectId}`).pipe(
      catchError(() => of([]))
    );
  }

  getByRevieweeAndProject(revieweeId: number, projectId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${REVIEW_API}/reviewee/${revieweeId}/project/${projectId}`).pipe(
      catchError(() => of([]))
    );
  }

  create(review: Review): Observable<Review | null> {
    return this.http.post<Review>(REVIEW_API, review).pipe(
      catchError(() => of(null))
    );
  }

  update(id: number, review: Partial<Review>): Observable<Review | null> {
    return this.http.put<Review>(`${REVIEW_API}/${id}`, { ...review, id }).pipe(
      catchError(() => of(null))
    );
  }

  delete(id: number): Observable<boolean> {
    return this.http.delete(`${REVIEW_API}/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }
}
