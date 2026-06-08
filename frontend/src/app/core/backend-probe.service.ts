import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, startWith } from 'rxjs';

import { MEME_API_BASE_URL } from './meme-api.tokens';

export type BackendProbeStatus = 'checking' | 'available' | 'unavailable';

export interface BackendProbeState {
  endpoint: string;
  message: string;
  status: BackendProbeStatus;
}

@Injectable({
  providedIn: 'root'
})
export class BackendProbeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(MEME_API_BASE_URL);
  private readonly endpoint = `${this.baseUrl}/configure/properties`;

  probe(): Observable<BackendProbeState> {
    return this.http.get<Record<string, unknown>>(this.endpoint).pipe(
      map((properties) => ({
        endpoint: this.endpoint,
        message: this.describeProperties(properties),
        status: 'available' as const
      })),
      startWith({
        endpoint: this.endpoint,
        message: 'Checking backend',
        status: 'checking' as const
      }),
      catchError((error: unknown) =>
        of({
          endpoint: this.endpoint,
          message: this.describeError(error),
          status: 'unavailable' as const
        })
      )
    );
  }

  private describeProperties(properties: Record<string, unknown>): string {
    const title = properties['deploy.title'] ?? properties['deployTitle'];

    if (title) {
      return `Backend responded: ${String(title)}`;
    }

    return 'Backend responded';
  }

  private describeError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'Backend unavailable';
      }

      return `Backend returned HTTP ${error.status}`;
    }

    return 'Backend probe failed';
  }
}
