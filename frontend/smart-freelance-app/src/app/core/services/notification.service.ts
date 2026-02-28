import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const NOTIFICATION_API = `${environment.apiGatewayUrl}/notification/api/notifications`;

/** Single notification from the Notification microservice (Firestore). */
export interface NotificationItem {
  id: string;
  userId: string;
  title: string;
  body: string;
  type: string;
  read: boolean;
  createdAt: string; // ISO-8601
  data?: Record<string, string>;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private http: HttpClient) {}

  /** List all notifications for a user (newest first). */
  getByUserId(userId: string | number): Observable<NotificationItem[]> {
    const id = typeof userId === 'number' ? String(userId) : userId;
    return this.http.get<NotificationItem[]>(`${NOTIFICATION_API}/user/${id}`).pipe(
      map((list) => Array.isArray(list) ? list : []),
      catchError(() => of([]))
    );
  }

  /** Mark a notification as read. */
  markRead(id: string): Observable<NotificationItem | null> {
    return this.http.patch<NotificationItem>(`${NOTIFICATION_API}/${id}/read`, {}).pipe(
      catchError(() => of(null))
    );
  }

  /** Delete a notification. */
  delete(id: string): Observable<boolean> {
    return this.http.delete(`${NOTIFICATION_API}/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  /** Unread count for a user (derived from list). */
  getUnreadCount(userId: string | number): Observable<number> {
    return this.getByUserId(userId).pipe(
      map((list) => list.filter((n) => !n.read).length)
    );
  }

  /**
   * Returns the dashboard route + query params for a notification (progress/comment).
   * Client → track-progress?projectId=X; Freelancer → progress-updates?projectId=X.
   */
  getNotificationRoute(n: NotificationItem, isClient: boolean): { route: string; queryParams: { projectId?: string } } {
    const projectId = n.data?.['projectId'];
    const base = isClient ? '/dashboard/track-progress' : '/dashboard/progress-updates';
    if (projectId) {
      return { route: base, queryParams: { projectId } };
    }
    return { route: base, queryParams: {} };
  }
}
