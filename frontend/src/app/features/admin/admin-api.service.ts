import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { MEME_API_BASE_URL } from '../../core/meme-api.tokens';
import {
  AdminListResponse,
  AdminListState,
  AdminProject,
  AdminUser,
  PfsParameter
} from './admin.models';
import { normalizeListResponse } from './admin-api.helpers';

@Injectable({
  providedIn: 'root'
})
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(MEME_API_BASE_URL);

  findProjects(pfs: PfsParameter, query = ''): Observable<AdminListState<AdminProject>> {
    return this.http
      .post<AdminListResponse<AdminProject>>(
        `${this.baseUrl}/project/find?query=${encodeURIComponent(query)}`,
        pfs
      )
      .pipe(map((response) => normalizeListResponse(response, ['projects', 'objects'])));
  }

  findUsers(pfs: PfsParameter, query = ''): Observable<AdminListState<AdminUser>> {
    return this.http
      .post<AdminListResponse<AdminUser>>(
        `${this.baseUrl}/security/user/find?query=${encodeURIComponent(query)}`,
        pfs
      )
      .pipe(map((response) => normalizeListResponse(response, ['users', 'objects'])));
  }

  getApplicationRoles(): Observable<string[]> {
    return this.http
      .get<AdminListResponse<string>>(`${this.baseUrl}/security/roles`)
      .pipe(map((response) => normalizeListResponse(response, ['objects', 'strings']).items));
  }

  getProjectRoles(): Observable<string[]> {
    return this.http
      .get<AdminListResponse<string>>(`${this.baseUrl}/project/roles`)
      .pipe(map((response) => normalizeListResponse(response, ['objects', 'strings']).items));
  }
}
