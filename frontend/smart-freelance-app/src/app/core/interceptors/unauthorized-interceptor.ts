import { inject, Injector } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Auth endpoints where 401 means "wrong credentials" — we should NOT logout/redirect. */
const AUTH_ENDPOINTS = ['/token', '/refresh'];

/**
 * When any API returns 401 (e.g. expired JWT), clear the token and redirect to login.
 * Skips logout for auth endpoints (login, refresh) so the login form can show the error.
 *
 * Uses lazy injection via Injector to avoid a circular DI dependency:
 * AuthService constructor → HttpClient → interceptor → inject(AuthService) → NG0200.
 */
export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
  const injector = inject(Injector);
  const isAuthRequest = AUTH_ENDPOINTS.some((ep) => req.url.includes(ep));

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err?.status === 401 && !isAuthRequest) {
        injector.get(AuthService).logout();
      }
      return throwError(() => err);
    })
  );
};
