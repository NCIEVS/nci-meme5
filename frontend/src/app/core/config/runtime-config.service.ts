import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { MEME_API_BASE_URL } from '../meme-api.tokens';
import {
  EnabledTab,
  RuntimeConfig,
  RuntimeConfigProperties,
  TAB_DEFINITIONS
} from './runtime-config.models';

@Injectable({
  providedIn: 'root'
})
export class RuntimeConfigService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(MEME_API_BASE_URL);
  private readonly configState = signal<RuntimeConfig | null>(null);
  private readonly errorState = signal<string | null>(null);

  readonly config = this.configState.asReadonly();
  readonly error = this.errorState.asReadonly();
  readonly properties = computed(() => this.configState()?.properties ?? {});
  readonly title = computed(
    () => this.get('deploy.title') || 'NCI-META Terminology Maintenance'
  );
  readonly enabledTabs = computed(() => this.buildEnabledTabs());

  async load(): Promise<void> {
    try {
      const properties = await firstValueFrom(
        this.http.get<RuntimeConfigProperties>(`${this.baseUrl}/configure/properties`)
      );

      this.configState.set({ properties });
      this.errorState.set(null);
    } catch {
      this.configState.set({ properties: {} });
      this.errorState.set('Application configuration could not be loaded.');
    }
  }

  get(key: string): string | undefined {
    return this.properties()[key];
  }

  isTrue(key: string): boolean {
    return this.get(key) === 'true';
  }

  enabledTabKeys(): string[] {
    return (this.get('deploy.enabled.tabs') ?? '')
      .split(',')
      .map((tab) => tab.trim())
      .filter(Boolean);
  }

  firstTab(): EnabledTab | null {
    return this.enabledTabs()[0] ?? null;
  }

  private buildEnabledTabs(): EnabledTab[] {
    return this.enabledTabKeys()
      .map((key) => TAB_DEFINITIONS[key])
      .filter((tab): tab is EnabledTab => Boolean(tab));
  }
}
