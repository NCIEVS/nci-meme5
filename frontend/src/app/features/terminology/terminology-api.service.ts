import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { MEME_API_BASE_URL } from '../../core/meme-api.tokens';
import {
  RootTerminology,
  Terminology,
  TerminologyListResponse
} from './terminology.models';

@Injectable({
  providedIn: 'root'
})
export class TerminologyApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(MEME_API_BASE_URL);

  getCurrentTerminologies(): Observable<Terminology[]> {
    return this.http
      .get<TerminologyListResponse>(`${this.baseUrl}/metadata/terminology/current`)
      .pipe(
        map((response) => response.terminologies ?? response.objects ?? [])
      );
  }

  getRootTerminology(terminology: string): Observable<RootTerminology> {
    return this.http.get<RootTerminology>(
      `${this.baseUrl}/metadata/rootTerminology/${encodeURIComponent(terminology)}`
    );
  }
}
