import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { NavigationService } from '../navigation/navigation.service';

export const startupGuard: CanActivateFn = () => {
  const navigation = inject(NavigationService);
  const router = inject(Router);

  return router.parseUrl(navigation.startupRoute());
};
