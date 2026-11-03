import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Functional HTTP Interceptor for FocusFlow 2.0.
 * - Automatically attaches Bearer token to API requests.
 * - Ensures withCredentials: true is set on API requests for HttpOnly cookie support.
 * - Performs seamless, automatic access-token refresh on 401 Unauthorized responses.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.accessToken();

  // Public authentication endpoints that do not require Authorization header
  const isAuthEndpoint = req.url.includes('/api/auth/login') ||
                         req.url.includes('/api/auth/register') ||
                         req.url.includes('/api/auth/forgot-password') ||
                         req.url.includes('/api/auth/reset-password');

  const isApiRequest = req.url.startsWith('/api') || req.url.startsWith('http://localhost:8080/api');

  let modifiedReq = req;

  // Set withCredentials for cross-origin cookie delivery (refresh token)
  if (isApiRequest) {
    if (token && !isAuthEndpoint) {
      modifiedReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        },
        withCredentials: true
      });
    } else {
      modifiedReq = req.clone({
        withCredentials: true
      });
    }
  }

  return next(modifiedReq).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        // If the 401 was from an auth endpoint itself, pass error along
        if (req.url.includes('/api/auth/')) {
          return throwError(() => error);
        }

        // Attempt silent refresh
        return authService.refreshToken().pipe(
          switchMap((authRes) => {
            const retriedReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${authRes.accessToken}`
              },
              withCredentials: true
            });
            return next(retriedReq);
          }),
          catchError((refreshErr) => {
            authService.logout();
            return throwError(() => refreshErr);
          })
        );
      }

      return throwError(() => error);
    })
  );
};

