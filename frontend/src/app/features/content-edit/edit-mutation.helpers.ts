import {
  EditMutationReadiness,
  EditValidationResult
} from './edit-mutation.models';

export function validationErrors(result: EditValidationResult | null | undefined): string[] {
  return Array.from(result?.errors ?? []);
}

export function validationWarnings(result: EditValidationResult | null | undefined): string[] {
  return Array.from(result?.warnings ?? []);
}

export function validationNeedsWarningOverride(
  result: EditValidationResult | null | undefined
): boolean {
  return validationErrors(result).length === 0 && validationWarnings(result).length > 0;
}

export function validationBlocksCommit(
  result: EditValidationResult | null | undefined
): boolean {
  return result?.valid === false || validationErrors(result).length > 0;
}

export function buildConceptMutationReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = [];

  if (!projectId) {
    reasons.push('Project context is required.');
  }
  if (!conceptId) {
    reasons.push('A persisted concept is required.');
  }
  if (!hasProjectPrivilegesOf(projectRole, 'AUTHOR')) {
    reasons.push('Author-level project role is required.');
  }
  if (!editingEnabled) {
    reasons.push('Project editing must be enabled.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

function hasProjectPrivilegesOf(
  projectRole: string | null | undefined,
  requiredRole: string
): boolean {
  const normalizedRole = projectRole?.toUpperCase();
  const normalizedRequiredRole = requiredRole.toUpperCase();

  if (!normalizedRole) {
    return normalizedRequiredRole === 'VIEWER';
  }

  if (normalizedRole === 'ADMINISTRATOR') {
    return true;
  }

  if (normalizedRole === 'REVIEWER') {
    return ['VIEWER', 'USER', 'AUTHOR', 'REVIEWER'].includes(
      normalizedRequiredRole
    );
  }

  if (normalizedRole === 'USER') {
    return ['VIEWER', 'USER', 'AUTHOR'].includes(normalizedRequiredRole);
  }

  if (normalizedRole === 'AUTHOR') {
    return ['VIEWER', 'AUTHOR'].includes(normalizedRequiredRole);
  }

  return normalizedRole === 'VIEWER' && normalizedRequiredRole === 'VIEWER';
}

export function buildAtomMutationReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  atomId: number | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;

  if (!atomId) {
    reasons.push('Atom id is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildAtomAddReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  atomName: string | null | undefined,
  termgroup: string | null | undefined,
  language: string | null | undefined,
  codeId: string | null | undefined,
  atomConceptId: string | null | undefined,
  descriptorId: string | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;

  if (!atomName?.trim()) {
    reasons.push('Atom name is required.');
  }
  if (!termgroup?.trim()) {
    reasons.push('Atom termgroup is required.');
  }
  if (!language?.trim()) {
    reasons.push('Atom language is required.');
  }
  if (!codeId?.trim() && !atomConceptId?.trim() && !descriptorId?.trim()) {
    reasons.push('At least one atom source id is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildSemanticTypeMutationReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  semanticTypeId: number | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;

  if (!semanticTypeId) {
    reasons.push('Semantic type id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildSemanticTypeAddReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  semanticType: string | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;

  if (!semanticType?.trim()) {
    reasons.push('Semantic type is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildAttributeMutationReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  attributeId: number | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;

  if (!attributeId) {
    reasons.push('Attribute id is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildAttributeAddReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  attributeName: string | null | undefined,
  attributeValue: string | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;

  if (!attributeName?.trim()) {
    reasons.push('Attribute name is required.');
  }
  if (!attributeValue?.trim()) {
    reasons.push('Attribute value is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildRelationshipMutationReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  relationshipId: number | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;

  if (!relationshipId) {
    reasons.push('Relationship id is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildRelationshipAddReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  targetConceptId: number | null | undefined,
  relationshipType: string | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;

  if (!targetConceptId) {
    reasons.push('Target concept id is required.');
  }
  if (conceptId && targetConceptId && conceptId === targetConceptId) {
    reasons.push('Target concept must be different from the current concept.');
  }
  if (!relationshipType?.trim()) {
    reasons.push('Relationship type is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildRelationshipsAddReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  targetConceptIds: readonly (number | null | undefined)[] | null | undefined,
  relationshipType: string | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;
  const selectedTargetIds = (targetConceptIds ?? []).filter(
    (targetConceptId): targetConceptId is number => Boolean(targetConceptId)
  );

  if (!selectedTargetIds.length) {
    reasons.push('At least one target concept is required.');
  }
  if (conceptId && selectedTargetIds.includes(conceptId)) {
    reasons.push('Target concept must be different from the current concept.');
  }
  if (!relationshipType?.trim()) {
    reasons.push('Relationship type is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildMergeConceptReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  targetConceptId: number | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;

  if (!targetConceptId) {
    reasons.push('Target concept id is required.');
  }
  if (conceptId && targetConceptId && conceptId === targetConceptId) {
    reasons.push('Target concept must be different from the current concept.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildMoveAtomsReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  targetConceptId: number | null | undefined,
  atomIds: readonly (number | null | undefined)[] | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;
  const selectedAtomIds = (atomIds ?? []).filter(
    (atomId): atomId is number => Boolean(atomId)
  );

  if (!targetConceptId) {
    reasons.push('Target concept id is required.');
  }
  if (conceptId && targetConceptId && conceptId === targetConceptId) {
    reasons.push('Target concept must be different from the current concept.');
  }
  if (!selectedAtomIds.length) {
    reasons.push('At least one atom is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildSplitConceptReadiness(
  projectId: number | null | undefined,
  conceptId: number | null | undefined,
  atomIds: readonly (number | null | undefined)[] | null | undefined,
  relationshipType: string | null | undefined,
  activityId: string | null | undefined,
  lastModified: number | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = buildConceptMutationReadiness(
    projectId,
    conceptId,
    projectRole,
    editingEnabled
  ).reasons;
  const selectedAtomIds = (atomIds ?? []).filter(
    (atomId): atomId is number => Boolean(atomId)
  );

  if (!selectedAtomIds.length) {
    reasons.push('At least one atom is required.');
  }
  if (!relationshipType?.trim()) {
    reasons.push('Relationship type is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!lastModified) {
    reasons.push('Concept lastModified timestamp is required.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}

export function buildActionMutationReadiness(
  projectId: number | null | undefined,
  molecularActionId: number | null | undefined,
  activityId: string | null | undefined,
  projectRole: string | null | undefined,
  editingEnabled: boolean
): EditMutationReadiness {
  const reasons = [];
  const normalizedProjectRole = projectRole?.toUpperCase();

  if (!projectId) {
    reasons.push('Project context is required.');
  }
  if (!molecularActionId) {
    reasons.push('Molecular action id is required.');
  }
  if (!activityId?.trim()) {
    reasons.push('Activity id is required.');
  }
  if (!['AUTHOR', 'ADMINISTRATOR'].includes(normalizedProjectRole ?? '')) {
    reasons.push('Author-level project role is required.');
  }
  if (!editingEnabled) {
    reasons.push('Project editing must be enabled.');
  }

  return {
    canExecute: reasons.length === 0,
    reasons
  };
}
