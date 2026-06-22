import { inject, Injectable, NgZone, signal } from '@angular/core';
import { Subject } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { MEME_API_BASE_URL } from '../../core/meme-api.tokens';

export interface WorkflowChangeContainer {
  id?: number | null;
  name?: string | null;
  terminology?: string | null;
  terminologyId?: string | null;
  type?: string | null;
  version?: string | null;
}

export interface WorkflowChangeEvent {
  container?: WorkflowChangeContainer | null;
  events?: WorkflowChangeEvent[] | null;
  id?: number | null;
  lastModified?: string | number | null;
  lastModifiedBy?: string | null;
  name?: string | null;
  objectId?: number | null;
  sessionId?: string | null;
  timestamp?: string | number | null;
  type?: string | null;
}

interface WorkflowChangeEventList {
  events?: unknown;
  objects?: unknown;
}

@Injectable({
  providedIn: 'root'
})
export class WorkflowLiveUpdateService {
  private readonly auth = inject(AuthService);
  private readonly baseUrl = inject(MEME_API_BASE_URL);
  private readonly zone = inject(NgZone);
  private readonly connectedState = signal(false);
  private readonly eventsSubject = new Subject<WorkflowChangeEvent>();

  private pingTimer: ReturnType<typeof setInterval> | null = null;
  private reconnectDelayMs = 2000;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private socket: WebSocket | null = null;
  private shouldReconnect = false;

  readonly connected = this.connectedState.asReadonly();
  readonly events$ = this.eventsSubject.asObservable();

  connect(): void {
    const userName = this.auth.currentUser().userName;

    this.shouldReconnect = true;

    if (!userName) {
      this.connectedState.set(false);
      return;
    }

    if (
      this.socket?.readyState === WebSocket.OPEN ||
      this.socket?.readyState === WebSocket.CONNECTING
    ) {
      return;
    }

    this.clearReconnectTimer();
    this.openSocket(userName);
  }

  disconnect(): void {
    this.shouldReconnect = false;
    this.clearReconnectTimer();
    this.stopPing();

    const socket = this.socket;
    this.socket = null;

    if (
      socket?.readyState === WebSocket.OPEN ||
      socket?.readyState === WebSocket.CONNECTING
    ) {
      socket.close();
    }

    this.connectedState.set(false);
  }

  private openSocket(userName: string): void {
    const socket = new WebSocket(this.websocketUrl(userName));
    this.socket = socket;

    socket.onopen = () => {
      this.zone.run(() => {
        this.reconnectDelayMs = 2000;
        this.connectedState.set(true);
        this.startPing();
      });
    };

    socket.onmessage = (message) => {
      this.zone.run(() => {
        this.parseEvents(message.data)
          .filter((event) => this.shouldEmitEvent(event))
          .forEach((event) => this.eventsSubject.next(event));
      });
    };

    socket.onerror = () => {
      this.zone.run(() => {
        this.connectedState.set(false);
      });
    };

    socket.onclose = () => {
      this.zone.run(() => {
        if (this.socket === socket) {
          this.socket = null;
        }
        this.connectedState.set(false);
        this.stopPing();
        this.scheduleReconnect();
      });
    };
  }

  private websocketUrl(userName: string): string {
    const restBase = this.baseUrl.replace(/\/+$/, '');
    const url = new URL(`${restBase}/websocket`, window.location.origin);

    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    url.search = encodeURIComponent(userName);

    return url.toString();
  }

  private parseEvents(data: unknown): WorkflowChangeEvent[] {
    if (typeof data !== 'string') {
      return [];
    }

    try {
      return this.normalizePayload(JSON.parse(data) as unknown);
    } catch {
      return [];
    }
  }

  private normalizePayload(payload: unknown): WorkflowChangeEvent[] {
    if (Array.isArray(payload)) {
      return payload.filter((item) => this.isWorkflowChangeEvent(item));
    }

    if (!this.isRecord(payload)) {
      return [];
    }

    const listPayload = payload as WorkflowChangeEventList;
    const events =
      Array.isArray(listPayload.events)
        ? listPayload.events
        : Array.isArray(listPayload.objects)
          ? listPayload.objects
          : null;

    if (events) {
      return events.filter((item) => this.isWorkflowChangeEvent(item));
    }

    return this.isWorkflowChangeEvent(payload) ? [payload] : [];
  }

  private isWorkflowChangeEvent(value: unknown): value is WorkflowChangeEvent {
    if (!this.isRecord(value)) {
      return false;
    }

    const type = value['type'];
    const name = value['name'];

    return (
      (typeof type === 'string' || typeof name === 'string') &&
      (type === null || type === undefined || typeof type === 'string') &&
      (name === null || name === undefined || typeof name === 'string')
    );
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
  }

  private shouldEmitEvent(event: WorkflowChangeEvent): boolean {
    const authToken = this.auth.authToken();

    return !authToken || !event.sessionId || event.sessionId !== authToken;
  }

  private scheduleReconnect(): void {
    if (!this.shouldReconnect || this.reconnectTimer) {
      return;
    }

    const delayMs = this.reconnectDelayMs;
    this.reconnectDelayMs = Math.min(this.reconnectDelayMs * 2, 30000);

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, delayMs);
  }

  private clearReconnectTimer(): void {
    if (!this.reconnectTimer) {
      return;
    }

    clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
  }

  private startPing(): void {
    this.stopPing();
    this.pingTimer = setInterval(() => {
      if (this.socket?.readyState === WebSocket.OPEN) {
        this.socket.send('ping');
      }
    }, 5000);
  }

  private stopPing(): void {
    if (!this.pingTimer) {
      return;
    }

    clearInterval(this.pingTimer);
    this.pingTimer = null;
  }
}
