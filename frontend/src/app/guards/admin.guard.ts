import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Route guard restricting access to users with ROLE_ADMIN.
 * - Unauthenticated users are redirected to /login with returnUrl.
 * - Authenticated users without admin role are redirected to /access-denied.
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  if (authService.isAdmin()) {
    return true;
  }

  return router.createUrlTree(['/access-denied']);
};

