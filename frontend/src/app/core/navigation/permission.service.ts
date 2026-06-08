import { inject, Injectable } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import { EnabledTab } from '../config/runtime-config.models';

@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  private readonly auth = inject(AuthService);

  canAccessTab(tab: EnabledTab): boolean {
    if (tab.projectRole) {
      return false;
    }

    return this.auth.hasPrivilegesOf(tab.role);
  }
}
