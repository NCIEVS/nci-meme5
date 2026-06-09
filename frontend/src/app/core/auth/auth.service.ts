import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { RuntimeConfigService } from '../config/runtime-config.service';
import { MEME_API_BASE_URL } from '../meme-api.tokens';
import { NotificationService } from '../notifications/notification.service';
import { EMPTY_USER, MemeUser, UserPreferences } from './auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(MEME_API_BASE_URL);
  private readonly config = inject(RuntimeConfigService);
  private readonly notifications = inject(NotificationService);
  private readonly userState = signal<MemeUser>({ ...EMPTY_USER });

  readonly currentUser = this.userState.asReadonly();
  readonly isLoggedIn = computed(() => Boolean(this.userState().authToken));
  readonly isGuest = computed(() => this.userState().authToken === 'guest');

  initializeFromStoredSession(): void {
    if (!this.config.isTrue('deploy.login.enabled')) {
      this.setGuestUser();
      return;
    }

    const storedUser = this.getStoredUser() ?? this.getWindowNameUser();

    if (storedUser) {
      if (storedUser.authToken === 'guest') {
        this.clearUser();
        return;
      }

      this.setUser(storedUser);
    }
  }

  authToken(): string | null {
    return this.userState().authToken;
  }

  async authenticate(userName: string, password: string): Promise<MemeUser> {
    const user = await firstValueFrom(
      this.http.post<MemeUser>(
        `${this.baseUrl}/security/authenticate/${encodeURIComponent(userName)}`,
        password,
        {
          headers: {
            'Content-Type': 'text/plain'
          }
        }
      )
    );

    this.setUser(user);
    this.notifications.success(`Logged in as ${user.name || user.userName}`);
    return this.currentUser();
  }

  async logout(): Promise<void> {
    const authToken = this.userState().authToken;

    if (!authToken) {
      this.clearUser();
      return;
    }

    if (authToken !== 'guest') {
      await firstValueFrom(
        this.http.get(
          `${this.baseUrl}/security/logout/${encodeURIComponent(authToken)}`
        )
      );
    }

    this.clearUser();
    this.notifications.success('Logged out');
  }

  setUser(data: MemeUser): void {
    const user = this.normalizeUser(data);
    this.userState.set(user);
    this.saveStoredUser(user);
  }

  setGuestUser(): void {
    this.setUser({
      applicationRole: 'VIEWER',
      authToken: 'guest',
      name: 'Guest',
      password: 'guest',
      userName: 'guest',
      userPreferences: {
        properties: {}
      }
    });
  }

  clearUser(): void {
    this.userState.set({ ...EMPTY_USER });
    window.localStorage?.removeItem('user');
    this.removeCookie('user');
  }

  hasPrivilegesOf(role: string | false | undefined): boolean {
    if (!role) {
      return true;
    }

    switch (role) {
      case 'ADMINISTRATOR':
        return this.userState().applicationRole === 'ADMINISTRATOR';
      case 'USER':
        return (
          this.userState().applicationRole === 'USER' ||
          this.userState().applicationRole === 'ADMINISTRATOR'
        );
      case 'VIEWER':
        return Boolean(this.userState().applicationRole);
      default:
        return true;
    }
  }

  acceptsLicense(): boolean {
    if (!this.config.isTrue('deploy.license.enabled')) {
      return true;
    }

    const cookieName = this.licenseCookieName();
    const cookie = this.getCookie(cookieName);

    if (cookie === 'license_accepted') {
      this.acceptLicense();
      return true;
    }

    return false;
  }

  acceptLicense(): void {
    this.setCookie(this.licenseCookieName(), 'license_accepted', 30);
  }

  private normalizeUser(data: MemeUser): MemeUser {
    const preferences: UserPreferences = {
      ...(data.userPreferences ?? {}),
      properties: {
        ...(data.userPreferences?.properties ?? {})
      }
    };

    return {
      ...EMPTY_USER,
      ...data,
      password: '',
      userPreferences: preferences
    };
  }

  private compactUser(user: MemeUser): MemeUser {
    const preferences = user.userPreferences ?? {};

    return {
      applicationRole: user.applicationRole,
      authToken: user.authToken,
      editorLevel: user.editorLevel,
      email: user.email,
      name: user.name,
      userName: user.userName,
      userPreferences: {
        lastProjectId: preferences.lastProjectId,
        lastProjectRole: preferences.lastProjectRole,
        lastTerminology: preferences.lastTerminology,
        lastTab: preferences.lastTab,
        properties: {
          reportModeTab: preferences.properties?.['reportModeTab']
        }
      }
    };
  }

  private getStoredUser(): MemeUser | null {
    const localStorageUser = this.parseStoredUser(
      window.localStorage?.getItem('user')
    );

    return localStorageUser ?? this.parseStoredUser(this.getCookie('user'));
  }

  private getWindowNameUser(): MemeUser | null {
    try {
      if (!window.name) {
        return null;
      }

      const session = JSON.parse(window.name) as { user?: MemeUser };

      if (!session.user?.authToken) {
        return null;
      }

      window.name = '';
      return session.user;
    } catch {
      return null;
    }
  }

  private parseStoredUser(storedUser: string | null | undefined): MemeUser | null {
    if (!storedUser) {
      return null;
    }

    try {
      const parsedUser = JSON.parse(storedUser) as MemeUser;
      return parsedUser?.authToken ? parsedUser : null;
    } catch {
      return null;
    }
  }

  private saveStoredUser(user: MemeUser): void {
    if (!user.authToken) {
      return;
    }

    window.localStorage?.setItem('user', JSON.stringify(user));
    this.setCookie('user', JSON.stringify(this.compactUser(user)), 30);
  }

  private licenseCookieName(): string {
    return `WCI ${this.config.title()}`;
  }

  private getCookie(name: string): string | null {
    const encodedName = encodeURIComponent(name);
    const cookie = document.cookie
      .split('; ')
      .find(
        (entry) =>
          entry.startsWith(`${encodedName}=`) || entry.startsWith(`${name}=`)
      );

    if (!cookie) {
      return null;
    }

    const value = cookie.substring(cookie.indexOf('=') + 1);

    try {
      return decodeURIComponent(value);
    } catch {
      return value;
    }
  }

  private setCookie(name: string, value: string, days: number): void {
    const expires = new Date();
    expires.setDate(expires.getDate() + days);
    document.cookie = [
      `${encodeURIComponent(name)}=${encodeURIComponent(value)}`,
      `expires=${expires.toUTCString()}`,
      'path=/',
      'SameSite=Lax'
    ].join('; ');
  }

  private removeCookie(name: string): void {
    document.cookie = `${encodeURIComponent(name)}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  }
}
