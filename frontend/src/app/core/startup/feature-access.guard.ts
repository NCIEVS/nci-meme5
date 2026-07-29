import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { TAB_DEFINITIONS } from '../config/runtime-config.models';
import { RuntimeConfigService } from '../config/runtime-config.service';
import { NavigationService } from '../navigation/navigation.service';
import { PermissionService } from '../navigation/permission.service';

export const featureAccessGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const config = inject(RuntimeConfigService);
  const navigation = inject(NavigationService);
  const permissions = inject(PermissionService);
  const router = inject(Router);
  const tabKey = String(route.data['tabKey'] ?? '');
  const tab = TAB_DEFINITIONS[tabKey];

  if (!tab || !config.enabledTabKeys().includes(tabKey)) {
    return router.parseUrl('/');
  }

  if (config.isTrue('deploy.login.enabled') && !auth.isLoggedIn()) {
    return router.parseUrl('/login');
  }

  if (!permissions.canAccessTab(tab)) {
    return router.parseUrl(navigation.routeForUnavailableTab());
  }

  return true;
};
