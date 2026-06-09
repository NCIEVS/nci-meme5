import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { RuntimeConfigService } from '../config/runtime-config.service';
import { NotificationService } from './notification.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const config = inject(RuntimeConfigService);
  const notifications = inject(NotificationService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (isAuthTokenFailure(error)) {
          auth.clearUser();
          notifications.error('Your session has expired. Please log in again.');

          if (config.isTrue('deploy.login.enabled')) {
            void router.navigateByUrl('/login');
          }

          return throwError(() => error);
        }

        notifications.error(describeHttpError(error));
      }

      return throwError(() => error);
    })
  );
};

function describeHttpError(error: HttpErrorResponse): string {
  if (typeof error.error === 'string' && error.error.trim()) {
    return error.error;
  }

  if (error.status === 0) {
    return 'The MEME backend is not reachable.';
  }

  return `Request failed with HTTP ${error.status}.`;
}

function isAuthTokenFailure(error: HttpErrorResponse): boolean {
  const message = typeof error.error === 'string' ? error.error : '';

  return [
    'AuthToken does not have a valid username',
    'AuthToken has expired',
    'Attempt to access a service without an AuthToken'
  ].some((failureText) => message.includes(failureText));
}
