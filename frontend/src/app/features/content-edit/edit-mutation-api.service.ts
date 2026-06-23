import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { MEME_API_BASE_URL } from '../../core/meme-api.tokens';
import {
  EditAddAtomRequest,
  EditAddAttributeRequest,
  EditAddRelationshipRequest,
  EditAddRelationshipsRequest,
  EditAddSemanticTypeRequest,
  EditApproveConceptRequest,
  EditMergeConceptRequest,
  EditMoveAtomsRequest,
  EditRemoveAtomRequest,
  EditRemoveAttributeRequest,
  EditRemoveRelationshipRequest,
  EditRemoveSemanticTypeRequest,
  EditSplitConceptRequest,
  EditUpdateAtomRequest,
  EditableAtomPayload,
  EditableConceptPayload,
  EditUndoRedoRequest,
  EditValidationResult
} from './edit-mutation.models';

@Injectable({
  providedIn: 'root'
})
export class EditMutationApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(MEME_API_BASE_URL);

  addAtom(
    projectId: number,
    conceptId: number,
    atom: EditableAtomPayload
  ): Observable<EditableAtomPayload> {
    return this.http.put<EditableAtomPayload>(`${this.baseUrl}/edit/atom`, atom, {
      params: this.projectConceptParams(projectId, conceptId)
    });
  }

  updateAtom(
    projectId: number,
    conceptId: number,
    atom: EditableAtomPayload
  ): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/edit/atom`, atom, {
      params: this.projectConceptParams(projectId, conceptId)
    });
  }

  removeAtom(projectId: number, conceptId: number, atomId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/edit/atom/${encodeURIComponent(String(atomId))}`,
      {
        params: this.projectConceptParams(projectId, conceptId)
      }
    );
  }

  removeAtomFromConcept(
    request: EditRemoveAtomRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/atom/remove/${encodeURIComponent(String(request.atomId))}`,
      '',
      {
        params: new HttpParams()
          .set('projectId', request.projectId)
          .set('conceptId', request.conceptId)
          .set('activityId', request.activityId)
          .set('lastModified', request.lastModified)
          .set('overrideWarnings', request.overrideWarnings)
      }
    );
  }

  addAtomToConcept(request: EditAddAtomRequest): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/atom/add`,
      request.atom,
      {
        params: this.metaConceptParams(request)
      }
    );
  }

  updateAtomOnConcept(
    request: EditUpdateAtomRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/atom/update`,
      request.atom,
      {
        params: this.metaConceptParams(request)
      }
    );
  }

  removeAttributeFromConcept(
    request: EditRemoveAttributeRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/attribute/remove/${encodeURIComponent(String(request.attributeId))}`,
      '',
      {
        params: this.metaConceptParams(request)
      }
    );
  }

  addAttributeToConcept(
    request: EditAddAttributeRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/attribute/add`,
      request.attribute,
      {
        params: this.metaConceptParams(request)
      }
    );
  }

  addRelationshipToConcept(
    request: EditAddRelationshipRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/relationship/add`,
      request.relationship,
      {
        params: this.metaConceptParams(request)
      }
    );
  }

  addRelationshipsToConcept(
    request: EditAddRelationshipsRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/relationships/add`,
      request.relationships,
      {
        params: this.metaConceptParams(request)
      }
    );
  }

  removeRelationshipFromConcept(
    request: EditRemoveRelationshipRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/relationship/remove/${encodeURIComponent(String(request.relationshipId))}`,
      '',
      {
        params: this.metaConceptParams(request)
      }
    );
  }

  addSemanticTypeToConcept(
    request: EditAddSemanticTypeRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/sty/add`,
      '',
      {
        params: this.metaConceptParams(request).set('semanticType', request.semanticType)
      }
    );
  }

  removeSemanticTypeFromConcept(
    request: EditRemoveSemanticTypeRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/sty/remove/${encodeURIComponent(String(request.semanticTypeId))}`,
      '',
      {
        params: this.metaConceptParams(request)
      }
    );
  }

  updateConcept(projectId: number, concept: EditableConceptPayload): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/edit/concept`, concept, {
      params: this.projectParams(projectId)
    });
  }

  approveConcept(
    request: EditApproveConceptRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/concept/approve`,
      '',
      {
        params: this.metaConceptParams(request)
      }
    );
  }

  mergeConcepts(request: EditMergeConceptRequest): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/concept/merge`,
      '',
      {
        params: this.metaConceptParams(request).set('conceptId2', request.conceptId2)
      }
    );
  }

  moveAtoms(request: EditMoveAtomsRequest): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/atom/move`,
      request.atomIds,
      {
        params: this.metaConceptParams(request).set('conceptId2', request.conceptId2)
      }
    );
  }

  splitConcept(request: EditSplitConceptRequest): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/concept/split`,
      request.atomIds,
      {
        params: this.metaConceptParams(request)
          .set('copyRelationships', request.copyRelationships)
          .set('copySemanticTypes', request.copySemanticTypes)
          .set('relationshipType', request.relationshipType)
      }
    );
  }

  undoAction(request: EditUndoRedoRequest): Observable<EditValidationResult> {
    return this.actionRequest('undo', request);
  }

  redoAction(request: EditUndoRedoRequest): Observable<EditValidationResult> {
    return this.actionRequest('redo', request);
  }

  private actionRequest(
    action: 'redo' | 'undo',
    request: EditUndoRedoRequest
  ): Observable<EditValidationResult> {
    return this.http.post<EditValidationResult>(
      `${this.baseUrl}/meta/action/${action}`,
      '',
      {
        params: new HttpParams()
          .set('projectId', request.projectId)
          .set('molecularActionId', request.molecularActionId)
          .set('activityId', request.activityId)
          .set('force', request.force)
      }
    );
  }

  private projectParams(projectId: number): HttpParams {
    return new HttpParams().set('projectId', projectId);
  }

  private projectConceptParams(projectId: number, conceptId: number): HttpParams {
    return this.projectParams(projectId).set('conceptId', conceptId);
  }

  private metaConceptParams(request: EditApproveConceptRequest): HttpParams {
    return new HttpParams()
      .set('projectId', request.projectId)
      .set('conceptId', request.conceptId)
      .set('activityId', request.activityId)
      .set('lastModified', request.lastModified)
      .set('overrideWarnings', request.overrideWarnings);
  }
}
