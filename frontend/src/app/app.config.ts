import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideZoneChangeDetection
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  provideRouter,
  withComponentInputBinding,
  withHashLocation,
  withInMemoryScrolling
} from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { loadingInterceptor } from './core/http/loading.interceptor';
import { errorInterceptor } from './core/notifications/error.interceptor';
import { initializeApplication } from './core/startup/app-initializer';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withHashLocation(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })
    ),
    provideHttpClient(
      withInterceptors([loadingInterceptor, authInterceptor, errorInterceptor])
    ),
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: initializeApplication
    }
  ]
};
