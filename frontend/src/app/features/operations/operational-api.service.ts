import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { MEME_API_BASE_URL } from '../../core/meme-api.tokens';
import {
  buildOperationalPfs,
  normalizeOperationalListResponse
} from './operational-api.helpers';
import {
  Checklist,
  KeyValuePair,
  OperationalListResponse,
  OperationalListState,
  OperationalTerminology,
  OperationalTerminologyListResponse,
  OperationalProject,
  OperationalUser,
  PfsParameter,
  ProcessConfig,
  ProcessExecution,
  ProcessStep,
  SearchResultListResponse,
  WorkflowBin,
  WorkflowBinDefinition,
  WorkflowConfig,
  WorkflowEpoch,
  WorkflowAction,
  WorkflowNote,
  Worklist
} from './operational.models';

@Injectable({
  providedIn: 'root'
})
export class OperationalApiService {
  private readonly baseUrl = inject(MEME_API_BASE_URL);
  private readonly http = inject(HttpClient);

  getCurrentTerminologies(): Observable<OperationalTerminology[]> {
    return this.http
      .get<OperationalTerminologyListResponse>(
        `${this.baseUrl}/metadata/terminology/current`
      )
      .pipe(map((response) => response.terminologies ?? response.objects ?? []));
  }

  getProject(projectId: number): Observable<OperationalProject> {
    return this.http.get<OperationalProject>(`${this.baseUrl}/project/${projectId}`);
  }

  findAssignedProjectUsers(
    projectId: number,
    query = '',
    pfs: PfsParameter = buildOperationalPfs(1, 500, 'userName', true, query)
  ): Observable<OperationalListState<OperationalUser>> {
    return this.http
      .post<OperationalListResponse<OperationalUser>>(
        `${this.baseUrl}/project/${projectId}/users`,
        pfs,
        {
          params: this.queryParams(query)
        }
      )
      .pipe(
        map((response) =>
          normalizeOperationalListResponse(response, ['users', 'objects'])
        )
      );
  }

  findProcessConfigs(
    projectId: number,
    query = '',
    pfs: PfsParameter = buildOperationalPfs(1, 20, 'lastModified', false, query)
  ): Observable<OperationalListState<ProcessConfig>> {
    return this.http
      .post<OperationalListResponse<ProcessConfig>>(
        `${this.baseUrl}/process/config/find`,
        pfs,
        {
          params: this.projectQueryParams(projectId, query)
        }
      )
      .pipe(
        map((response) =>
          normalizeOperationalListResponse(response, ['processes', 'objects'])
        )
      );
  }

  getProcessConfig(projectId: number, id: number): Observable<ProcessConfig> {
    return this.http.get<ProcessConfig>(`${this.baseUrl}/process/config/${id}`, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  addProcessConfig(
    projectId: number,
    processConfig: ProcessConfig
  ): Observable<ProcessConfig> {
    return this.http.put<ProcessConfig>(
      `${this.baseUrl}/process/config`,
      processConfig,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  updateProcessConfig(
    projectId: number,
    processConfig: ProcessConfig
  ): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/process/config`, processConfig, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  removeProcessConfig(
    projectId: number,
    id: number,
    cascade = true
  ): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/process/config/${id}`, {
      params: new HttpParams().set('projectId', projectId).set('cascade', cascade)
    });
  }

  importProcessConfig(projectId: number, file: File): Observable<ProcessConfig> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ProcessConfig>(
      `${this.baseUrl}/process/config/import`,
      formData,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  exportProcessConfig(projectId: number, id: number): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/process/config/export`, '', {
      params: new HttpParams().set('projectId', projectId).set('processId', id),
      responseType: 'blob'
    });
  }

  getAlgorithmsForType(projectId: number, type: string): Observable<KeyValuePair[]> {
    return this.http
      .get<OperationalListResponse<KeyValuePair>>(
        `${this.baseUrl}/process/algo/${encodeURIComponent(type.toLocaleLowerCase())}`,
        {
          params: new HttpParams().set('projectId', projectId)
        }
      )
      .pipe(
        map(
          (response) =>
            normalizeOperationalListResponse(response, ['keyValuePairs', 'objects'])
              .items
        )
      );
  }

  getAlgorithmConfig(projectId: number, id: number): Observable<ProcessStep> {
    return this.http.get<ProcessStep>(
      `${this.baseUrl}/process/config/algo/${id}`,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  newAlgorithmConfig(
    projectId: number,
    processId: number,
    algorithmKey: string
  ): Observable<ProcessStep> {
    return this.http.get<ProcessStep>(
      `${this.baseUrl}/process/config/algo/${encodeURIComponent(algorithmKey)}/new`,
      {
        params: new HttpParams().set('projectId', projectId).set('processId', processId)
      }
    );
  }

  addAlgorithmConfig(
    projectId: number,
    processId: number,
    algorithmConfig: ProcessStep
  ): Observable<ProcessStep> {
    return this.http.put<ProcessStep>(
      `${this.baseUrl}/process/config/algo`,
      algorithmConfig,
      {
        params: new HttpParams().set('projectId', projectId).set('processId', processId)
      }
    );
  }

  updateAlgorithmConfig(
    projectId: number,
    processId: number,
    algorithmConfig: ProcessStep
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/process/config/algo`,
      algorithmConfig,
      {
        params: new HttpParams().set('projectId', projectId).set('processId', processId)
      }
    );
  }

  validateAlgorithmConfig(
    projectId: number,
    processId: number,
    algorithmConfig: ProcessStep
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/process/config/algo/validate`,
      algorithmConfig,
      {
        params: new HttpParams().set('projectId', projectId).set('processId', processId)
      }
    );
  }

  testProcessQuery(
    projectId: number,
    processId: number,
    queryType: string,
    queryStyle: string,
    query: string,
    objectTypeName: string | null = null
  ): Observable<number> {
    let params = new HttpParams()
      .set('projectId', projectId)
      .set('processId', processId)
      .set('queryType', queryType)
      .set('queryStyle', queryStyle)
      .set('query', query);

    if (objectTypeName) {
      params = params.set('objectTypeName', objectTypeName);
    }

    return this.http
      .get(`${this.baseUrl}/process/testquery`, {
        params,
        responseType: 'text'
      })
      .pipe(map((count) => Number(count)));
  }

  removeAlgorithmConfig(projectId: number, id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/process/config/algo/${id}`,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  prepareProcess(projectId: number, id: number): Observable<number> {
    return this.processOperationId(`${this.baseUrl}/process/config/${id}/prepare`, {
      projectId
    });
  }

  findProcessExecutions(
    projectId: number,
    query = '',
    pfs: PfsParameter = buildOperationalPfs(1, 20, 'lastModified', false, query)
  ): Observable<OperationalListState<ProcessExecution>> {
    return this.http
      .post<OperationalListResponse<ProcessExecution>>(
        `${this.baseUrl}/process/execution/find`,
        pfs,
        {
          params: this.projectQueryParams(projectId, query)
        }
      )
      .pipe(
        map((response) =>
          normalizeOperationalListResponse(response, ['processes', 'objects'])
        )
      );
  }

  getProcessExecution(projectId: number, id: number): Observable<ProcessExecution> {
    return this.http.get<ProcessExecution>(
      `${this.baseUrl}/process/execution/${id}`,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  getProcessProgress(projectId: number, id: number): Observable<number> {
    return this.http
      .get(`${this.baseUrl}/process/${id}/progress`, {
        params: new HttpParams().set('projectId', projectId),
        responseType: 'text'
      })
      .pipe(map((progress) => Number(progress)));
  }

  getAlgorithmProgress(projectId: number, id: number): Observable<number> {
    return this.http
      .get(`${this.baseUrl}/process/algo/${id}/progress`, {
        params: new HttpParams().set('projectId', projectId),
        responseType: 'text'
      })
      .pipe(map((progress) => Number(progress)));
  }

  getProcessLog(
    projectId: number,
    processExecutionId: number,
    query = ''
  ): Observable<string> {
    return this.http.get(`${this.baseUrl}/process/${processExecutionId}/log`, {
      params: this.projectQueryParams(projectId, query),
      responseType: 'text'
    });
  }

  getAlgorithmLog(
    projectId: number,
    algorithmExecutionId: number,
    query = ''
  ): Observable<string> {
    return this.http.get(
      `${this.baseUrl}/process/algo/${algorithmExecutionId}/log`,
      {
        params: this.projectQueryParams(projectId, query),
        responseType: 'text'
      }
    );
  }

  executeProcess(
    projectId: number,
    id: number,
    background = true
  ): Observable<number> {
    return this.processOperationId(
      `${this.baseUrl}/process/execution/${id}/execute`,
      {
        background,
        projectId
      }
    );
  }

  cancelProcess(projectId: number, id: number): Observable<number> {
    return this.processOperationId(
      `${this.baseUrl}/process/execution/${id}/cancel`,
      {
        projectId
      }
    );
  }

  restartProcess(
    projectId: number,
    id: number,
    background = true
  ): Observable<number> {
    return this.processOperationId(
      `${this.baseUrl}/process/execution/${id}/restart`,
      {
        background,
        projectId
      }
    );
  }

  stepProcess(
    projectId: number,
    id: number,
    step: number,
    background = true
  ): Observable<number> {
    return this.processOperationId(
      `${this.baseUrl}/process/execution/${id}/step`,
      {
        background,
        projectId,
        step
      }
    );
  }

  getExecutingProcesses(
    projectId: number
  ): Observable<OperationalListState<ProcessExecution>> {
    return this.http
      .get<OperationalListResponse<ProcessExecution>>(
        `${this.baseUrl}/process/executing`,
        {
          params: new HttpParams().set('projectId', projectId)
        }
      )
      .pipe(
        map((response) =>
          normalizeOperationalListResponse(response, ['processes', 'objects'])
        )
      );
  }

  getWorkflowConfigs(
    projectId: number
  ): Observable<OperationalListState<WorkflowConfig>> {
    return this.http
      .get<OperationalListResponse<WorkflowConfig>>(
        `${this.baseUrl}/workflow/config/all`,
        {
          params: new HttpParams().set('projectId', projectId)
        }
      )
      .pipe(
        map((response) =>
          normalizeOperationalListResponse(response, ['configs', 'objects'])
        )
      );
  }

  addWorkflowConfig(
    projectId: number,
    workflowConfig: WorkflowConfig
  ): Observable<WorkflowConfig> {
    return this.http.put<WorkflowConfig>(
      `${this.baseUrl}/workflow/config`,
      workflowConfig,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  updateWorkflowConfig(
    projectId: number,
    workflowConfig: WorkflowConfig
  ): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/workflow/config`, workflowConfig, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  removeWorkflowConfig(projectId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/workflow/config/${id}`, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  importWorkflowConfig(projectId: number, file: File): Observable<WorkflowConfig> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<WorkflowConfig>(
      `${this.baseUrl}/workflow/config/import`,
      formData,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  exportWorkflowConfig(projectId: number, id: number): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/workflow/config/export`, '', {
      params: new HttpParams().set('projectId', projectId).set('workflowId', id),
      responseType: 'blob'
    });
  }

  getWorkflowBins(
    projectId: number,
    type: string
  ): Observable<OperationalListState<WorkflowBin>> {
    return this.http
      .get<OperationalListResponse<WorkflowBin>>(`${this.baseUrl}/workflow/bin/all`, {
        params: new HttpParams().set('projectId', projectId).set('type', type)
      })
      .pipe(
        map((response) => normalizeOperationalListResponse(response, ['bins', 'objects']))
      );
  }

  getWorkflowBinDefinition(
    projectId: number,
    name: string,
    type: string
  ): Observable<WorkflowBinDefinition> {
    return this.http.get<WorkflowBinDefinition>(
      `${this.baseUrl}/workflow/definition`,
      {
        params: new HttpParams()
          .set('projectId', projectId)
          .set('name', name)
          .set('type', type)
      }
    );
  }

  addWorkflowBinDefinition(
    projectId: number,
    definition: WorkflowBinDefinition,
    positionAfterId: number | null = null
  ): Observable<WorkflowBinDefinition> {
    let params = new HttpParams().set('projectId', projectId);

    if (positionAfterId) {
      params = params.set('positionAfterId', positionAfterId);
    }

    return this.http.put<WorkflowBinDefinition>(
      `${this.baseUrl}/workflow/definition`,
      definition,
      {
        params
      }
    );
  }

  updateWorkflowBinDefinition(
    projectId: number,
    definition: WorkflowBinDefinition
  ): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/workflow/definition`, definition, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  reorderWorkflowBinDefinitions(
    projectId: number,
    workflowConfigId: number,
    definitionIds: number[]
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/workflow/definition/order`,
      definitionIds,
      {
        params: new HttpParams()
          .set('projectId', projectId)
          .set('workflowConfigId', workflowConfigId)
      }
    );
  }

  removeWorkflowBinDefinition(projectId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/workflow/definition/${id}`, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  clearWorkflowBins(projectId: number, type: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/workflow/bin/clear/all`, '', {
      params: new HttpParams().set('projectId', projectId).set('type', type)
    });
  }

  regenerateWorkflowBins(projectId: number, type: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/workflow/bin/regenerate/all`, '', {
      params: new HttpParams().set('projectId', projectId).set('type', type)
    });
  }

  regenerateWorkflowBin(
    projectId: number,
    id: number,
    type: string
  ): Observable<WorkflowBin> {
    return this.http.post<WorkflowBin>(
      `${this.baseUrl}/workflow/bin/${id}/regenerate`,
      '',
      {
        params: new HttpParams().set('projectId', projectId).set('type', type)
      }
    );
  }

  testWorkflowQuery(
    projectId: number,
    query: string,
    queryType: string,
    queryStyle: string
  ): Observable<SearchResultListResponse> {
    return this.http
      .get<SearchResultListResponse>(`${this.baseUrl}/workflow/query/test`, {
        params: new HttpParams()
          .set('projectId', projectId)
          .set('query', query)
          .set('queryType', queryType)
          .set('queryStyle', queryStyle)
      })
      .pipe(
        map((response) => ({
          ...response,
          results: response.results ?? response.objects ?? []
        }))
      );
  }

  findChecklists(
    projectId: number,
    query = '',
    pfs: PfsParameter = buildOperationalPfs(1, 20, 'lastModified', false, query)
  ): Observable<OperationalListState<Checklist>> {
    return this.http
      .post<OperationalListResponse<Checklist>>(
        `${this.baseUrl}/workflow/checklist/find`,
        pfs,
        {
          params: this.projectQueryParams(projectId, query)
        }
      )
      .pipe(
        map((response) =>
          normalizeOperationalListResponse(response, ['checklists', 'objects'])
        )
      );
  }

  getChecklist(projectId: number, id: number): Observable<Checklist> {
    return this.http.get<Checklist>(`${this.baseUrl}/workflow/checklist/${id}`, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  createChecklist(
    projectId: number,
    workflowBinId: number,
    clusterType: string,
    name: string,
    description: string,
    randomize: boolean,
    excludeOnWorklist: boolean,
    pfs: PfsParameter,
    query = ''
  ): Observable<Checklist> {
    let params = this.workflowBinParams(projectId, workflowBinId, clusterType)
      .set('name', name)
      .set('description', description)
      .set('randomize', randomize)
      .set('excludeOnWorklist', excludeOnWorklist);
    const trimmedQuery = query.trim();

    if (trimmedQuery) {
      params = params.set('query', trimmedQuery);
    }

    return this.http.post<Checklist>(`${this.baseUrl}/workflow/checklist`, pfs, {
      params
    });
  }

  removeChecklist(projectId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/workflow/checklist/${id}`, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  exportChecklist(projectId: number, id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/workflow/checklist/${id}/export`, {
      params: new HttpParams().set('projectId', projectId),
      responseType: 'blob'
    });
  }

  importChecklist(
    projectId: number,
    name: string,
    file: File
  ): Observable<Checklist> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<Checklist>(
      `${this.baseUrl}/workflow/checklist/import`,
      formData,
      {
        params: new HttpParams().set('projectId', projectId).set('name', name)
      }
    );
  }

  computeChecklist(
    projectId: number,
    name: string,
    query: string,
    queryType: string,
    pfs: PfsParameter
  ): Observable<Checklist> {
    return this.http.post<Checklist>(
      `${this.baseUrl}/workflow/checklist/compute`,
      pfs,
      {
        params: new HttpParams()
          .set('projectId', projectId)
          .set('name', name)
          .set('query', query)
          .set('queryType', queryType)
      }
    );
  }

  addChecklistNote(
    projectId: number,
    checklistId: number,
    note: string
  ): Observable<WorkflowNote> {
    return this.http.put<WorkflowNote>(
      `${this.baseUrl}/workflow/checklist/${checklistId}/note`,
      note,
      {
        headers: {
          'Content-Type': 'text/plain'
        },
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  removeChecklistNote(projectId: number, noteId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/workflow/checklist/note/${noteId}`,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  findWorklists(
    projectId: number,
    query = '',
    pfs: PfsParameter = buildOperationalPfs(1, 20, 'lastModified', false, query)
  ): Observable<OperationalListState<Worklist>> {
    return this.http
      .post<OperationalListResponse<Worklist>>(
        `${this.baseUrl}/workflow/worklist/find`,
        pfs,
        {
          params: this.projectQueryParams(projectId, query)
        }
      )
      .pipe(
        map((response) =>
          normalizeOperationalListResponse(response, ['worklists', 'objects'])
        )
      );
  }

  getWorklist(projectId: number, id: number): Observable<Worklist> {
    return this.http.get<Worklist>(`${this.baseUrl}/workflow/worklist/${id}`, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  createWorklist(
    projectId: number,
    workflowBinId: number,
    clusterType: string,
    pfs: PfsParameter
  ): Observable<Worklist> {
    return this.http.put<Worklist>(`${this.baseUrl}/workflow/worklist`, pfs, {
      params: this.workflowBinParams(projectId, workflowBinId, clusterType)
    });
  }

  performWorkflowAction(
    projectId: number,
    worklistId: number,
    userName: string,
    userRole: string,
    action: WorkflowAction
  ): Observable<Worklist> {
    return this.http.get<Worklist>(`${this.baseUrl}/workflow/worklist/action`, {
      params: new HttpParams()
        .set('projectId', projectId)
        .set('worklistId', worklistId)
        .set('userName', userName)
        .set('userRole', userRole)
        .set('action', action)
    });
  }

  removeWorklist(projectId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/workflow/worklist/${id}`, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  exportWorklist(projectId: number, id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/workflow/worklist/${id}/export`, {
      params: new HttpParams().set('projectId', projectId),
      responseType: 'blob'
    });
  }

  addWorklistNote(
    projectId: number,
    worklistId: number,
    note: string
  ): Observable<WorkflowNote> {
    return this.http.put<WorkflowNote>(
      `${this.baseUrl}/workflow/worklist/${worklistId}/note`,
      note,
      {
        headers: {
          'Content-Type': 'text/plain'
        },
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  removeWorklistNote(projectId: number, noteId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/workflow/worklist/note/${noteId}`,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  findGeneratedConceptReports(
    projectId: number,
    worklistName: string,
    pfs: PfsParameter = buildOperationalPfs(1, 1, '', true, worklistName)
  ): Observable<OperationalListState<string>> {
    return this.http
      .post<OperationalListResponse<string>>(`${this.baseUrl}/workflow/report`, pfs, {
        params: new HttpParams()
          .set('projectId', projectId)
          .set('query', worklistName)
      })
      .pipe(
        map((response) =>
          normalizeOperationalListResponse(response, ['strings', 'objects'])
        )
      );
  }

  generateConceptReport(projectId: number, worklistId: number): Observable<string> {
    return this.http.get(
      `${this.baseUrl}/workflow/worklist/${worklistId}/report/generate`,
      {
        params: new HttpParams().set('projectId', projectId).set('sendEmail', true),
        responseType: 'text'
      }
    );
  }

  getGeneratedConceptReport(projectId: number, fileName: string): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl}/workflow/report/${encodeURIComponent(fileName)}`,
      {
        params: new HttpParams().set('projectId', projectId),
        responseType: 'blob'
      }
    );
  }

  removeGeneratedConceptReport(
    projectId: number,
    fileName: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/workflow/report/${encodeURIComponent(fileName)}`,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  stampWorklist(
    projectId: number,
    id: number,
    activityId: string,
    approve = true
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/workflow/worklist/${id}/stamp`,
      null,
      {
        params: new HttpParams()
          .set('projectId', projectId)
          .set('activityId', activityId)
          .set('approve', approve)
      }
    );
  }

  stampChecklist(
    projectId: number,
    id: number,
    activityId: string,
    approve = true
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/workflow/checklist/${id}/stamp`,
      null,
      {
        params: new HttpParams()
          .set('projectId', projectId)
          .set('activityId', activityId)
          .set('approve', approve)
      }
    );
  }

  recomputeConceptStatus(projectId: number, update = false): Observable<void> {
    let params = new HttpParams().set('projectId', projectId);

    if (update) {
      params = params.set('update', true);
    }

    return this.http.post<void>(
      `${this.baseUrl}/workflow/status/compute`,
      null,
      {
        params
      }
    );
  }

  getWorkflowLog(
    projectId: number,
    checklistId: number | null,
    worklistId: number | null,
    lines = 1000
  ): Observable<string> {
    let params = new HttpParams().set('projectId', projectId).set('lines', lines);

    if (checklistId) {
      params = params.set('checklistId', checklistId);
    }

    if (worklistId) {
      params = params.set('worklistId', worklistId);
    }

    return this.http.get(`${this.baseUrl}/workflow/log`, {
      params,
      responseType: 'text'
    });
  }

  getWorkflowEpochs(
    projectId: number
  ): Observable<OperationalListState<WorkflowEpoch>> {
    return this.http
      .get<OperationalListResponse<WorkflowEpoch>>(
        `${this.baseUrl}/workflow/epoch/all`,
        {
          params: new HttpParams().set('projectId', projectId)
        }
      )
      .pipe(
        map((response) =>
          normalizeOperationalListResponse(response, ['epochs', 'objects'])
        )
      );
  }

  getCurrentWorkflowEpoch(projectId: number): Observable<WorkflowEpoch> {
    return this.http.get<WorkflowEpoch>(`${this.baseUrl}/workflow/epoch`, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  addWorkflowEpoch(
    projectId: number,
    epoch: WorkflowEpoch
  ): Observable<WorkflowEpoch> {
    return this.http.put<WorkflowEpoch>(`${this.baseUrl}/workflow/epoch`, epoch, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  removeWorkflowEpoch(projectId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/workflow/epoch/${id}`, {
      params: new HttpParams().set('projectId', projectId)
    });
  }

  private projectQueryParams(projectId: number, query: string): HttpParams {
    const params = new HttpParams().set('projectId', projectId);
    const trimmedQuery = query.trim();

    return trimmedQuery ? params.set('query', trimmedQuery) : params;
  }

  private queryParams(query: string): HttpParams {
    const trimmedQuery = query.trim();

    return trimmedQuery ? new HttpParams().set('query', trimmedQuery) : new HttpParams();
  }

  private workflowBinParams(
    projectId: number,
    workflowBinId: number,
    clusterType: string
  ): HttpParams {
    let params = new HttpParams()
      .set('projectId', projectId)
      .set('workflowBinId', workflowBinId);

    if (clusterType && clusterType !== 'default') {
      params = params.set('clusterType', clusterType);
    }

    return params;
  }

  private processOperationId(
    url: string,
    params: Record<string, boolean | number>
  ): Observable<number> {
    let httpParams = new HttpParams();

    for (const [key, value] of Object.entries(params)) {
      httpParams = httpParams.set(key, value);
    }

    return this.http
      .get(url, {
        params: httpParams,
        responseType: 'text'
      })
      .pipe(map((id) => Number(id)));
  }
}
