import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const authGuard: CanActivateFn = () => {
  if (sessionStorage.getItem('auth') === '1') {
    return true;
  }
  const router = inject(Router);
  return router.createUrlTree(['/']);
};
