import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { EnabledTab } from '../../core/config/runtime-config.models';
import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { NavigationService } from '../../core/navigation/navigation.service';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'meme-header',
  imports: [RouterLink],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  protected readonly auth = inject(AuthService);
  protected readonly config = inject(RuntimeConfigService);
  protected readonly navigation = inject(NavigationService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected currentUrl = this.router.url;

  constructor() {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.currentUrl = (event as NavigationEnd).urlAfterRedirects;
      });
  }

  protected async logout(): Promise<void> {
    try {
      await this.auth.logout();
      await this.navigation.goHome();
    } catch {
      this.notifications.error('Logout failed.');
    }
  }

  protected isTabActive(tab: EnabledTab): boolean {
    const path = this.currentUrl.split(/[?#]/)[0] || '/';

    if (tab.key === 'edit') {
      return path === '/edit' || path.startsWith('/edit/') || path.startsWith('/content') || path.startsWith('/contexts');
    }

    return path === `/${tab.link}` || path.startsWith(`/${tab.link}/`);
  }

  protected showTabs(): boolean {
    const path = this.currentUrl.split(/[?#]/)[0] || '/';
    return path !== '/landing' && path !== '/login';
  }
}
