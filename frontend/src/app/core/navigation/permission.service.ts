import { inject, Injectable } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import { EnabledTab } from '../config/runtime-config.models';
import { ProjectContextService } from './project-context.service';

@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  private readonly auth = inject(AuthService);
  private readonly projectContext = inject(ProjectContextService);

  canAccessTab(tab: EnabledTab): boolean {
    if (tab.projectRole) {
      return this.projectContext.hasPrivilegesOf('AUTHOR');
    }

    return this.auth.hasPrivilegesOf(tab.role);
  }
}
