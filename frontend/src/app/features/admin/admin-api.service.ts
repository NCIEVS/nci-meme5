import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { MEME_API_BASE_URL } from '../../core/meme-api.tokens';
import { UserPreferences } from '../../core/auth/auth.models';
import {
  AdminKeyValuePair,
  AdminKeyValuePairList,
  AdminKeyValuePairListsResponse,
  AdminListResponse,
  AdminListState,
  AdminPrecedenceList,
  AdminProject,
  AdminStringListResponse,
  AdminTerminology,
  AdminTerminologyListResponse,
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

  assignUserToProject(
    projectId: number,
    userName: string,
    role: string
  ): Observable<AdminProject> {
    const params = new URLSearchParams({
      projectId: String(projectId),
      userName,
      role
    });

    return this.http.get<AdminProject>(`${this.baseUrl}/project/assign?${params}`);
  }

  addProject(project: AdminProject): Observable<AdminProject> {
    return this.http.put<AdminProject>(`${this.baseUrl}/project/`, project);
  }

  addUser(user: AdminUser): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.baseUrl}/security/user/add`, user);
  }

  removeProject(projectId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/project/${projectId}`);
  }

  removeUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/security/user/remove/${userId}`);
  }

  reloadConfigProperties(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/project/reload`, '');
  }

  updateUser(user: AdminUser): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/security/user/update`, user);
  }

  updateProject(project: AdminProject): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/project/`, project);
  }

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

  getCurrentTerminologies(): Observable<AdminTerminology[]> {
    return this.http
      .get<AdminTerminologyListResponse>(`${this.baseUrl}/metadata/terminology/current`)
      .pipe(map((response) => response.terminologies ?? response.objects ?? []));
  }

  getMetadataLanguages(terminology: string, version: string): Observable<AdminKeyValuePair[]> {
    return this.http
      .get<AdminKeyValuePairListsResponse>(
        `${this.baseUrl}/metadata/all/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
      )
      .pipe(
        map(
          (response) =>
            response.keyValuePairLists?.find((list) => list.name === 'Languages')
              ?.keyValuePairs ?? []
        )
      );
  }

  getProjectRoles(): Observable<string[]> {
    return this.http
      .get<AdminListResponse<string>>(`${this.baseUrl}/project/roles`)
      .pipe(map((response) => normalizeListResponse(response, ['objects', 'strings']).items));
  }

  getPrecedenceList(precedenceListId: number): Observable<AdminPrecedenceList> {
    return this.http.get<AdminPrecedenceList>(
      `${this.baseUrl}/metadata/precedence/${precedenceListId}`
    );
  }

  getValidationChecks(): Observable<AdminKeyValuePair[]> {
    return this.http
      .get<AdminKeyValuePairList>(`${this.baseUrl}/project/checks`)
      .pipe(map((response) => response.keyValuePairs ?? []));
  }

  getWorkflowPaths(): Observable<string[]> {
    return this.http
      .get<AdminStringListResponse>(`${this.baseUrl}/workflow/paths`)
      .pipe(map((response) => response.strings ?? response.objects ?? []));
  }

  unassignUserFromProject(projectId: number, userName: string): Observable<AdminProject> {
    const params = new URLSearchParams({
      projectId: String(projectId),
      userName
    });

    return this.http.get<AdminProject>(`${this.baseUrl}/project/unassign?${params}`);
  }

  updatePrecedenceList(precedenceList: AdminPrecedenceList): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/metadata/precedence`, precedenceList);
  }

  updateUserPreferences(userPreferences: UserPreferences): Observable<UserPreferences> {
    return this.http.post<UserPreferences>(
      `${this.baseUrl}/security/user/preferences/update`,
      userPreferences
    );
  }

  forceException(local = false): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/project/exception${local ? '?local=true' : ''}`,
      ''
    );
  }
}
