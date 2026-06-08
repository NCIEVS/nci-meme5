import { Component, inject } from '@angular/core';

import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'meme-notifications',
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.css'
})
export class NotificationsComponent {
  protected readonly notifications = inject(NotificationService);
}
