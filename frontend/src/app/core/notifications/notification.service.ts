import { Injectable, signal } from '@angular/core';

import { appendNotificationMessage } from './notification.helpers';
import type {
  NotificationLevel,
  NotificationMessage
} from './notification.helpers';

export type { NotificationLevel, NotificationMessage } from './notification.helpers';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private nextId = 1;
  private readonly messagesState = signal<NotificationMessage[]>([]);

  readonly messages = this.messagesState.asReadonly();

  error(message: string): void {
    this.add('error', message);
  }

  success(_message: string): void {
    // Global banners are reserved for errors and warnings.
  }

  dismiss(id: number): void {
    this.messagesState.update((messages) =>
      messages.filter((message) => message.id !== id)
    );
  }

  clear(): void {
    this.messagesState.set([]);
  }

  private add(level: NotificationLevel, message: string): void {
    this.messagesState.update((messages) => {
      const nextMessage = {
        id: this.nextId,
        level,
        message
      };
      const updatedMessages = appendNotificationMessage(messages, nextMessage);

      if (updatedMessages !== messages) {
        this.nextId++;
      }

      return updatedMessages;
    });
  }
}
