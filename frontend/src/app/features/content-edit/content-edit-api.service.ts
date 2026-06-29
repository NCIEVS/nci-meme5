import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { MEME_API_BASE_URL } from '../../core/meme-api.tokens';
import {
  contentTypePath,
  normalizeContentListResponse
} from './content-edit-api.helpers';
import {
  ContentAtom,
  ContentComponent,
  ContentComponentType,
  ContentKeyValuePairLists,
  ContentListResponse,
  ContentListState,
  ContentMapping,
  ContentMetadata,
  ContentPfsParameter,
  ContentRelationship,
  ContentRelationshipTypeDetail,
  ContentSearchResult,
  ContentSemanticTypeListResponse,
  ContentSemanticTypeMetadata,
  ContentStringListResponse,
  ContentTerminology,
  ContentTerminologyListResponse,
  ContentTermTypeDetail,
  ContentTree,
  ContentTreePosition,
  MolecularActionListResponse
} from './content-edit.models';
import { EditValidationResult } from './edit-mutation.models';

@Injectable({
  providedIn: 'root'
})
export class ContentEditApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(MEME_API_BASE_URL);

  autocomplete(
    autocompletePath: string,
    searchTerm: string
  ): Observable<string[]> {
    return this.http
      .get<ContentStringListResponse>(
        `${this.baseUrl}/content/${autocompletePath}${encodeURIComponent(searchTerm)}`
      )
      .pipe(map((response) => response.strings ?? response.objects ?? []));
  }

  findComponents(
    type: ContentComponentType | string,
    terminology: string,
    version: string,
    query: string,
    pfs: ContentPfsParameter
  ): Observable<ContentListState<ContentSearchResult>> {
    const componentPath = contentTypePath(type);

    return this.http
      .post<ContentListResponse<ContentSearchResult>>(
        `${this.baseUrl}/content/${componentPath}/${encodeURIComponent(
          terminology
        )}/${encodeURIComponent(version)}`,
        pfs,
        {
          params: new HttpParams().set('query', query)
        }
      )
      .pipe(
        map((response) =>
          normalizeContentListResponse(response, [
            'results',
            'objects'
          ])
        )
      );
  }

  getCurrentTerminologies(): Observable<ContentTerminology[]> {
    return this.http
      .get<ContentTerminologyListResponse>(
        `${this.baseUrl}/metadata/terminology/current`
      )
      .pipe(map((response) => response.terminologies ?? response.objects ?? []));
  }

  getSemanticTypes(
    terminology: string,
    version: string
  ): Observable<ContentSemanticTypeMetadata[]> {
    return this.http
      .get<ContentSemanticTypeListResponse>(
        `${this.baseUrl}/metadata/sty/${encodeURIComponent(
          terminology
        )}/${encodeURIComponent(version)}`
      )
      .pipe(map((response) => response.types ?? response.objects ?? []));
  }

  getComponentById(
    type: ContentComponentType | string,
    id: number,
    projectId?: number | null
  ): Observable<ContentComponent | null> {
    return this.http.get<ContentComponent | null>(
      `${this.baseUrl}/content/${contentTypePath(type)}/${id}`,
      {
        params: this.projectParams(projectId)
      }
    );
  }

  getComponentByTerminologyId(
    type: ContentComponentType | string,
    terminology: string,
    version: string,
    terminologyId: string,
    projectId?: number | null
  ): Observable<ContentComponent | null> {
    return this.http.get<ContentComponent | null>(
      `${this.baseUrl}/content/${contentTypePath(type)}/${encodeURIComponent(
        terminology
      )}/${encodeURIComponent(version)}/${encodeURIComponent(terminologyId)}`,
      {
        params: this.projectParams(projectId)
      }
    );
  }

  findMolecularActions(
    componentId: number,
    terminology: string,
    version: string,
    query: string,
    pfs: ContentPfsParameter
  ): Observable<MolecularActionListResponse> {
    return this.http.post<MolecularActionListResponse>(
      `${this.baseUrl}/project/actions/molecular`,
      pfs,
      {
        params: new HttpParams()
          .set('componentId', componentId)
          .set('terminology', terminology)
          .set('version', version)
          .set('query', query)
      }
    );
  }

  getActionLog(projectId: number, actionId: number): Observable<string> {
    return this.http.get(
      `${this.baseUrl}/project/log`,
      {
        params: new HttpParams()
          .set('projectId', projectId)
          .set('objectId', actionId)
          .set('message', 'ACTION')
          .set('lines', '1000'),
        responseType: 'text'
      }
    );
  }

  getComponentReport(
    type: ContentComponentType | string,
    id: number,
    projectId?: number | null
  ): Observable<string> {
    return this.http.get(
      `${this.baseUrl}/report/${contentTypePath(type)}/${id}`,
      {
        params: this.projectParams(projectId),
        responseType: 'text'
      }
    );
  }

  addComponentNote(
    type: ContentComponentType | string,
    id: number,
    noteText: string
  ): Observable<string> {
    return this.http.post(
      `${this.baseUrl}/content/${contentTypePath(type)}/${id}/note`,
      noteText,
      {
        responseType: 'text'
      }
    );
  }

  removeComponentNote(
    type: ContentComponentType | string,
    noteId: number
  ): Observable<string> {
    return this.http.delete(
      `${this.baseUrl}/content/${contentTypePath(type)}/note/${noteId}`,
      {
        responseType: 'text'
      }
    );
  }

  findTrees(
    type: ContentComponentType | string,
    terminology: string,
    version: string,
    terminologyId: string,
    pfs: ContentPfsParameter
  ): Observable<ContentListState<ContentTree>> {
    return this.http
      .post<ContentListResponse<ContentTree>>(
        `${this.baseUrl}/content/${contentTypePath(type)}/${encodeURIComponent(
          terminology
        )}/${encodeURIComponent(version)}/${encodeURIComponent(terminologyId)}/trees`,
        pfs
      )
      .pipe(map((response) => normalizeContentListResponse(response, ['trees', 'objects'])));
  }

  findDeepTreePositions(
    terminology: string,
    version: string,
    terminologyId: string,
    query: string,
    pfs: ContentPfsParameter
  ): Observable<ContentListState<ContentTreePosition>> {
    return this.http
      .post<ContentListResponse<ContentTreePosition>>(
        `${this.baseUrl}/content/concept/${encodeURIComponent(
          terminology
        )}/${encodeURIComponent(version)}/${encodeURIComponent(
          terminologyId
        )}/treePositions/deep`,
        pfs,
        {
          params: new HttpParams().set('query', query)
        }
      )
      .pipe(
        map((response) =>
          normalizeContentListResponse(response, ['treePositions', 'objects'])
        )
      );
  }

  findRelationships(
    type: string,
    terminology: string,
    version: string,
    terminologyId: string,
    pfs: ContentPfsParameter
  ): Observable<ContentListState<ContentRelationship>> {
    return this.http
      .post<ContentListResponse<ContentRelationship>>(
        `${this.baseUrl}/content/${type.toLowerCase()}/${encodeURIComponent(
          terminology
        )}/${encodeURIComponent(version)}/${encodeURIComponent(
          terminologyId
        )}/relationships`,
        pfs,
        { params: new HttpParams().set('query', '') }
      )
      .pipe(
        map((response) =>
          normalizeContentListResponse(response, ['relationships', 'objects'])
        )
      );
  }

  findDeepRelationships(
    terminology: string,
    version: string,
    terminologyId: string,
    pfs: ContentPfsParameter
  ): Observable<ContentListState<ContentRelationship>> {
    return this.http
      .post<ContentListResponse<ContentRelationship>>(
        `${this.baseUrl}/content/concept/${encodeURIComponent(
          terminology
        )}/${encodeURIComponent(version)}/${encodeURIComponent(
          terminologyId
        )}/relationships/deep`,
        pfs,
        {
          params: new HttpParams()
            .set('query', '')
            .set('inverseFlag', 'false')
            .set('includeConceptRels', 'false')
            .set('preferredOnly', 'false')
            .set('includeSelfReferential', 'false')
        }
      )
      .pipe(
        map((response) =>
          normalizeContentListResponse(response, ['relationships', 'objects'])
        )
      );
  }

  findMappings(
    type: ContentComponentType | string,
    terminology: string,
    version: string,
    terminologyId: string,
    pfs: ContentPfsParameter
  ): Observable<ContentListState<ContentMapping>> {
    return this.http
      .post<ContentListResponse<ContentMapping>>(
        `${this.baseUrl}/content/${contentTypePath(type)}/${encodeURIComponent(
          terminologyId
        )}/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}/mappings`,
        pfs,
        {
          params: new HttpParams().set('query', '')
        }
      )
      .pipe(
        map((response) => normalizeContentListResponse(response, ['mappings', 'objects']))
      );
  }

  getInverseRelationshipType(
    terminology: string,
    version: string,
    relationshipType: string
  ): Observable<string> {
    return this.http.get(
      `${this.baseUrl}/content/inverseRelationshipType/${encodeURIComponent(
        terminology
      )}/${encodeURIComponent(version)}/${encodeURIComponent(relationshipType)}`,
      {
        responseType: 'text'
      }
    );
  }

  validateConcept(
    projectId: number,
    concept: ContentComponent,
    checkId?: string | null
  ): Observable<EditValidationResult> {
    let params = new HttpParams().set('projectId', projectId);

    if (checkId) {
      params = params.set('checkId', checkId);
    }

    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/content/validate/concept`,
      concept,
      {
        params
      }
    );
  }

  validateAtom(
    projectId: number,
    atom: ContentAtom
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/content/validate/atom`,
      atom,
      {
        params: new HttpParams().set('projectId', projectId)
      }
    );
  }

  getAllMetadata(terminology: string, version: string): Observable<ContentMetadata> {
    return this.http
      .get<ContentKeyValuePairLists>(
        `${this.baseUrl}/metadata/all/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
      )
      .pipe(
        map((response) => {
          const lists = response.keyValuePairLists ?? [];
          const find = (name: string) =>
            lists.find((l) => l.name === name)?.keyValuePairs ?? [];
          return {
            additionalRelationshipTypes: find('Additional_Relationship_Types'),
            attributeNames: find('Attribute_Names'),
            relationshipTypes: find('Relationship_Types'),
            termTypes: find('Term_Types')
          };
        })
      );
  }

  removeTermType(type: string, terminology: string, version: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/metadata/termType/${encodeURIComponent(type)}/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
    );
  }

  removeAttributeName(type: string, terminology: string, version: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/metadata/attributeName/${encodeURIComponent(type)}/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
    );
  }

  removeRelationshipType(type: string, terminology: string, version: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/metadata/relationshipType/${encodeURIComponent(type)}/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
    );
  }

  removeAdditionalRelationshipType(
    type: string,
    terminology: string,
    version: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/metadata/additionalRelationshipType/${encodeURIComponent(type)}/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
    );
  }

  getTermType(key: string, terminology: string, version: string): Observable<ContentTermTypeDetail> {
    return this.http.get<ContentTermTypeDetail>(
      `${this.baseUrl}/metadata/termType/${encodeURIComponent(key)}/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
    );
  }

  addTermType(obj: ContentTermTypeDetail): Observable<ContentTermTypeDetail> {
    return this.http.put<ContentTermTypeDetail>(`${this.baseUrl}/metadata/termType`, obj);
  }

  updateTermType(obj: ContentTermTypeDetail): Observable<ContentTermTypeDetail> {
    return this.http.post<ContentTermTypeDetail>(`${this.baseUrl}/metadata/termType`, obj);
  }

  getAttributeName(key: string, terminology: string, version: string): Observable<ContentTermTypeDetail> {
    return this.http.get<ContentTermTypeDetail>(
      `${this.baseUrl}/metadata/attributeName/${encodeURIComponent(key)}/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
    );
  }

  addAttributeName(obj: ContentTermTypeDetail): Observable<ContentTermTypeDetail> {
    return this.http.put<ContentTermTypeDetail>(`${this.baseUrl}/metadata/attributeName`, obj);
  }

  updateAttributeName(obj: ContentTermTypeDetail): Observable<ContentTermTypeDetail> {
    return this.http.post<ContentTermTypeDetail>(`${this.baseUrl}/metadata/attributeName`, obj);
  }

  getRelationshipType(key: string, terminology: string, version: string): Observable<ContentRelationshipTypeDetail> {
    return this.http.get<ContentRelationshipTypeDetail>(
      `${this.baseUrl}/metadata/relationshipType/${encodeURIComponent(key)}/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
    );
  }

  addRelationshipType(list: { types: ContentRelationshipTypeDetail[] }): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/metadata/relationshipType`, list);
  }

  updateRelationshipType(obj: ContentRelationshipTypeDetail): Observable<ContentRelationshipTypeDetail> {
    return this.http.post<ContentRelationshipTypeDetail>(`${this.baseUrl}/metadata/relationshipType`, obj);
  }

  getAdditionalRelationshipType(key: string, terminology: string, version: string): Observable<ContentRelationshipTypeDetail> {
    return this.http.get<ContentRelationshipTypeDetail>(
      `${this.baseUrl}/metadata/additionalRelationshipType/${encodeURIComponent(key)}/${encodeURIComponent(terminology)}/${encodeURIComponent(version)}`
    );
  }

  addAdditionalRelationshipType(list: { types: ContentRelationshipTypeDetail[] }): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/metadata/additionalRelationshipType`, list);
  }

  updateAdditionalRelationshipType(obj: ContentRelationshipTypeDetail): Observable<ContentRelationshipTypeDetail> {
    return this.http.post<ContentRelationshipTypeDetail>(`${this.baseUrl}/metadata/additionalRelationshipType`, obj);
  }

  private projectParams(projectId?: number | null): HttpParams {
    return projectId ? new HttpParams().set('projectId', projectId) : new HttpParams();
  }
}
