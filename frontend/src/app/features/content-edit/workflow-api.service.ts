import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { MEME_API_BASE_URL } from '../../core/meme-api.tokens';
import { ContentPfsParameter } from './content-edit.models';
import {
  WorkflowChecklistResponse,
  WorkflowRecordResponse,
  WorkflowWorklist,
  WorkflowWorklistResponse
} from './workflow.models';

@Injectable({
  providedIn: 'root'
})
export class WorkflowApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(MEME_API_BASE_URL);

  findAvailableWorklists(
    projectId: number,
    userName: string,
    role: string,
    pfs: ContentPfsParameter
  ): Observable<WorkflowWorklistResponse> {
    return this.http.post<WorkflowWorklistResponse>(
      `${this.baseUrl}/workflow/worklist/available`,
      pfs,
      { params: new HttpParams().set('projectId', projectId).set('userName', userName).set('role', role) }
    );
  }

  findAssignedWorklists(
    projectId: number,
    userName: string,
    role: string,
    pfs: ContentPfsParameter
  ): Observable<WorkflowWorklistResponse> {
    return this.http.post<WorkflowWorklistResponse>(
      `${this.baseUrl}/workflow/worklist/assigned`,
      pfs,
      { params: new HttpParams().set('projectId', projectId).set('userName', userName).set('role', role) }
    );
  }

  findDoneWorklists(
    projectId: number,
    userName: string,
    role: string,
    pfs: ContentPfsParameter
  ): Observable<WorkflowWorklistResponse> {
    return this.http.post<WorkflowWorklistResponse>(
      `${this.baseUrl}/workflow/worklist/done`,
      pfs,
      { params: new HttpParams().set('projectId', projectId).set('userName', userName).set('role', role) }
    );
  }

  findChecklists(
    projectId: number,
    query: string,
    pfs: ContentPfsParameter
  ): Observable<WorkflowChecklistResponse> {
    let params = new HttpParams().set('projectId', projectId);
    if (query) {
      params = params.set('query', query);
    }
    return this.http.post<WorkflowChecklistResponse>(
      `${this.baseUrl}/workflow/checklist/find`,
      pfs,
      { params }
    );
  }

  findTrackingRecordsForWorklist(
    projectId: number,
    worklistId: number,
    pfs: ContentPfsParameter
  ): Observable<WorkflowRecordResponse> {
    return this.http.post<WorkflowRecordResponse>(
      `${this.baseUrl}/workflow/worklist/${worklistId}/records`,
      pfs,
      { params: new HttpParams().set('projectId', projectId) }
    );
  }

  findTrackingRecordsForChecklist(
    projectId: number,
    checklistId: number,
    pfs: ContentPfsParameter
  ): Observable<WorkflowRecordResponse> {
    return this.http.post<WorkflowRecordResponse>(
      `${this.baseUrl}/workflow/checklist/${checklistId}/records`,
      pfs,
      { params: new HttpParams().set('projectId', projectId) }
    );
  }

  performWorkflowAction(
    projectId: number,
    worklistId: number,
    userName: string,
    userRole: string,
    action: string
  ): Observable<WorkflowWorklist> {
    return this.http.get<WorkflowWorklist>(
      `${this.baseUrl}/workflow/worklist/action`,
      {
        params: new HttpParams()
          .set('projectId', projectId)
          .set('worklistId', worklistId)
          .set('userName', userName)
          .set('userRole', userRole)
          .set('action', action)
      }
    );
  }

  removeWorklist(projectId: number, worklistId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/workflow/worklist/${worklistId}`,
      { params: new HttpParams().set('projectId', projectId) }
    );
  }

  removeChecklist(projectId: number, checklistId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/workflow/checklist/${checklistId}`,
      { params: new HttpParams().set('projectId', projectId) }
    );
  }

  getWorklistCount(
    mode: 'available' | 'assigned' | 'done',
    projectId: number,
    userName: string,
    role: string
  ): Observable<number> {
    const minPfs: ContentPfsParameter = {
      ascending: false,
      maxResults: 1,
      startIndex: 0,
      sortField: 'lastModified'
    };
    const endpoint$ = mode === 'available'
      ? this.findAvailableWorklists(projectId, userName, role, minPfs)
      : mode === 'assigned'
        ? this.findAssignedWorklists(projectId, userName, role, minPfs)
        : this.findDoneWorklists(projectId, userName, role, minPfs);
    return endpoint$.pipe(map((r) => r.totalCount ?? 0));
  }

  getChecklistCount(projectId: number): Observable<number> {
    const minPfs: ContentPfsParameter = { ascending: false, maxResults: 1, startIndex: 0 };
    return this.findChecklists(projectId, '', minPfs).pipe(map((r) => r.totalCount ?? 0));
  }
}
