import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { LoadingIndicatorComponent } from './shared/loading/loading-indicator.component';
import { NotificationsComponent } from './shared/notifications/notifications.component';
import { FooterComponent } from './shell/footer/footer.component';
import { HeaderComponent } from './shell/header/header.component';
import { LoadingService } from './core/http/loading.service';

@Component({
  selector: 'meme-root',
  imports: [
    FooterComponent,
    HeaderComponent,
    LoadingIndicatorComponent,
    NotificationsComponent,
    RouterOutlet
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  private readonly router = inject(Router);
  protected readonly loading = inject(LoadingService);
  protected readonly shellHidden = signal(false);

  constructor() {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe((e) => {
        const url = (e as NavigationEnd).urlAfterRedirects;
        this.shellHidden.set(
          url.startsWith('/concept-report') ||
          url.startsWith('/edit/') ||
          url.startsWith('/contexts')
        );
      });
  }
}
