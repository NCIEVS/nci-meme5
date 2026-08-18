import { computed, Injectable, signal } from '@angular/core';

export interface LoadingRequestOptions {
  blockUi?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private readonly pendingRequests = signal(0);
  private readonly pendingBlockingRequests = signal(0);

  readonly isLoading = computed(() => this.pendingRequests() > 0);
  readonly isUiBlocked = computed(() => this.pendingBlockingRequests() > 0);

  increment(options: LoadingRequestOptions = {}): void {
    this.pendingRequests.update((count) => count + 1);
    if (options.blockUi) {
      this.pendingBlockingRequests.update((count) => count + 1);
    }
  }

  decrement(options: LoadingRequestOptions = {}): void {
    this.pendingRequests.update((count) => Math.max(0, count - 1));
    if (options.blockUi) {
      this.pendingBlockingRequests.update((count) => Math.max(0, count - 1));
    }
  }
}
