export type NotificationLevel = 'error' | 'success';

export interface NotificationMessage {
  id: number;
  level: NotificationLevel;
  message: string;
}

export function appendNotificationMessage(
  messages: NotificationMessage[],
  nextMessage: NotificationMessage
): NotificationMessage[] {
  if (
    messages.some(
      (existing) =>
        existing.level === nextMessage.level &&
        existing.message === nextMessage.message
    )
  ) {
    return messages;
  }

  return [...messages, nextMessage];
}
