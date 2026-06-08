import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { LoadingIndicatorComponent } from './shared/loading/loading-indicator.component';
import { NotificationsComponent } from './shared/notifications/notifications.component';
import { FooterComponent } from './shell/footer/footer.component';
import { HeaderComponent } from './shell/header/header.component';

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
export class AppComponent {}
