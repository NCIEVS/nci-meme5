import { Injectable, signal } from '@angular/core';

export type NotificationLevel = 'error' | 'success';

export interface NotificationMessage {
  id: number;
  level: NotificationLevel;
  message: string;
}

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

  success(message: string): void {
    this.add('success', message);
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
    this.messagesState.update((messages) => [
      ...messages,
      {
        id: this.nextId++,
        level,
        message
      }
    ]);
  }
}
