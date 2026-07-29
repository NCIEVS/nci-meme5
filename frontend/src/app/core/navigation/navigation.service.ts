import { computed, inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { EnabledTab } from '../config/runtime-config.models';
import { RuntimeConfigService } from '../config/runtime-config.service';
import { legacyMemeUrl } from '../meme-deployment-paths';
import { MEME_API_BASE_URL } from '../meme-api.tokens';
import { PermissionService } from './permission.service';

@Injectable({
  providedIn: 'root'
})
export class NavigationService {
  private readonly auth = inject(AuthService);
  private readonly apiBaseUrl = inject(MEME_API_BASE_URL);
  private readonly config = inject(RuntimeConfigService);
  private readonly permissions = inject(PermissionService);
  private readonly router = inject(Router);

  readonly tabs = computed(() =>
    this.config.enabledTabs().map((tab) => ({
      ...tab,
      accessible: this.permissions.canAccessTab(tab)
    }))
  );

  startupRoute(): string {
    if (this.config.isTrue('deploy.login.enabled') && !this.auth.isLoggedIn()) {
      return '/login';
    }

    return this.routeForStartingTab();
  }

  routeForStartingTab(): string {
    const lastTab = this.auth.currentUser().userPreferences?.lastTab;
    const normalizedLastTab = lastTab?.replace(/^\//, '');
    const preferredTab = this.config
      .enabledTabs()
      .find((enabledTab) => enabledTab.link === normalizedLastTab);
    const tab =
      preferredTab && this.permissions.canAccessTab(preferredTab)
        ? preferredTab
        : this.firstAccessibleTab() ?? this.config.firstTab();

    return tab ? `/${tab.link}` : '/login';
  }

  routeForUnavailableTab(): string {
    const tab = this.firstAccessibleTab();
    return tab ? `/${tab.link}` : '/login';
  }

  legacyUrl(tab: EnabledTab): string {
    return legacyMemeUrl(tab.link, this.apiBaseUrl);
  }

  legacyRootUrl(): string {
    return legacyMemeUrl('', this.apiBaseUrl);
  }

  async goHome(): Promise<void> {
    await this.router.navigateByUrl(this.startupRoute());
  }

  private firstAccessibleTab(): EnabledTab | null {
    return (
      this.config.enabledTabs().find((tab) => this.permissions.canAccessTab(tab)) ??
      null
    );
  }
}
