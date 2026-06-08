import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { NavigationService } from '../../core/navigation/navigation.service';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'meme-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  protected readonly auth = inject(AuthService);
  protected readonly config = inject(RuntimeConfigService);
  protected readonly navigation = inject(NavigationService);
  private readonly notifications = inject(NotificationService);

  protected async logout(): Promise<void> {
    try {
      await this.auth.logout();
      await this.navigation.goHome();
    } catch {
      this.notifications.error('Logout failed.');
    }
  }
}
