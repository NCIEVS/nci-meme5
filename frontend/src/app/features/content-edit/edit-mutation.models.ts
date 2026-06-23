import {
  ContentAtom,
  ContentAttribute,
  ContentComponent as ContentComponentDetail,
  ContentRelationship
} from './content-edit.models';

export interface EditValidationResult {
  comments?: string[] | null;
  errors?: string[] | null;
  valid?: boolean | null;
  warnings?: string[] | null;
}

export interface EditApproveConceptRequest {
  activityId: string;
  conceptId: number;
  lastModified: number;
  overrideWarnings: boolean;
  projectId: number;
}

export interface EditAddSemanticTypeRequest extends EditApproveConceptRequest {
  semanticType: string;
}

export interface EditAddAtomRequest extends EditApproveConceptRequest {
  atom: ContentAtom;
}

export interface EditAddAttributeRequest extends EditApproveConceptRequest {
  attribute: ContentAttribute;
}

export interface EditAddRelationshipRequest extends EditApproveConceptRequest {
  relationship: ContentRelationship;
}

export interface EditAddRelationshipsRequest extends EditApproveConceptRequest {
  relationships: ContentRelationship[];
}

export interface EditMergeConceptRequest extends EditApproveConceptRequest {
  conceptId2: number;
}

export interface EditMoveAtomsRequest extends EditApproveConceptRequest {
  atomIds: number[];
  conceptId2: number;
}

export interface EditSplitConceptRequest extends EditApproveConceptRequest {
  atomIds: number[];
  copyRelationships: boolean;
  copySemanticTypes: boolean;
  relationshipType: string;
}

export interface EditRemoveAtomRequest extends EditApproveConceptRequest {
  atomId: number;
}

export interface EditUpdateAtomRequest extends EditApproveConceptRequest {
  atom: ContentAtom;
}

export interface EditRemoveAttributeRequest extends EditApproveConceptRequest {
  attributeId: number;
}

export interface EditRemoveRelationshipRequest extends EditApproveConceptRequest {
  relationshipId: number;
}

export interface EditRemoveSemanticTypeRequest extends EditApproveConceptRequest {
  semanticTypeId: number;
}

export interface EditUndoRedoRequest {
  activityId: string;
  force: boolean;
  molecularActionId: number;
  projectId: number;
}

export interface EditMutationReadiness {
  canExecute: boolean;
  reasons: string[];
}

export type EditableConceptPayload = ContentComponentDetail;

export type EditableAtomPayload = ContentAtom;
