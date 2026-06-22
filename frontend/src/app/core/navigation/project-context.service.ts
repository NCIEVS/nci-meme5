import { computed, inject, Injectable } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import { resolveProjectContextId } from './project-context.helpers';

@Injectable({
  providedIn: 'root'
})
export class ProjectContextService {
  private readonly auth = inject(AuthService);

  readonly projectId = computed(() => {
    const rawProjectId = this.auth.currentUser().userPreferences?.lastProjectId;
    return resolveProjectContextId(
      rawProjectId,
      this.auth.currentUser().projectRoleMap
    );
  });

  readonly projectRole = computed(() => {
    const user = this.auth.currentUser();
    const projectId = this.projectId();
    const roleFromPreferences = user.userPreferences?.lastProjectRole;

    if (roleFromPreferences) {
      return roleFromPreferences;
    }

    if (projectId) {
      return user.projectRoleMap?.[String(projectId)] ?? null;
    }

    return null;
  });

  readonly hasProjectContext = computed(() => this.projectId() !== null);

  hasPrivilegesOf(requiredRole: string): boolean {
    if (!this.hasProjectContext()) {
      return false;
    }

    const applicationRole = this.auth.currentUser().applicationRole;

    if (applicationRole === 'ADMINISTRATOR' || applicationRole === 'USER') {
      return true;
    }

    return this.projectRoleHasPrivilegesOf(this.projectRole(), requiredRole);
  }

  private projectRoleHasPrivilegesOf(
    role: string | null | undefined,
    requiredRole: string
  ): boolean {
    const normalizedRole = role?.toUpperCase();
    const normalizedRequiredRole = requiredRole.toUpperCase();

    if (!normalizedRole) {
      return normalizedRequiredRole === 'VIEWER';
    }

    if (normalizedRole === 'ADMINISTRATOR') {
      return true;
    }

    if (normalizedRole === 'REVIEWER') {
      return ['VIEWER', 'USER', 'AUTHOR', 'REVIEWER'].includes(
        normalizedRequiredRole
      );
    }

    if (normalizedRole === 'USER') {
      return ['VIEWER', 'USER', 'AUTHOR'].includes(normalizedRequiredRole);
    }

    if (normalizedRole === 'AUTHOR') {
      return ['VIEWER', 'AUTHOR'].includes(normalizedRequiredRole);
    }

    return normalizedRole === 'VIEWER' && normalizedRequiredRole === 'VIEWER';
  }
}
