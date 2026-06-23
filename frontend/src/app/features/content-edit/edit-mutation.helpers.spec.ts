import {
  buildActionMutationReadiness,
  buildAtomMutationReadiness,
  buildAttributeAddReadiness,
  buildAttributeMutationReadiness,
  buildConceptMutationReadiness,
  buildMergeConceptReadiness,
  buildMoveAtomsReadiness,
  buildRelationshipAddReadiness,
  buildRelationshipsAddReadiness,
  buildRelationshipMutationReadiness,
  buildSemanticTypeAddReadiness,
  buildSemanticTypeMutationReadiness,
  buildSplitConceptReadiness,
  validationBlocksCommit,
  validationNeedsWarningOverride
} from './edit-mutation.helpers';

describe('edit mutation helpers', () => {
  it('treats warning-only validation results as override candidates', () => {
    expect(
      validationNeedsWarningOverride({
        valid: true,
        errors: [],
        warnings: ['Review preferred term change.']
      })
    ).toBe(true);
  });

  it('treats validation errors as blocking', () => {
    expect(
      validationBlocksCommit({
        valid: false,
        errors: ['Concept was modified by another user.'],
        warnings: []
      })
    ).toBe(true);
  });

  it('reports missing edit prerequisites', () => {
    expect(buildConceptMutationReadiness(null, null, 'REVIEWER', false)).toEqual({
      canExecute: false,
      reasons: [
        'Project context is required.',
        'A persisted concept is required.',
        'Author-level project role is required.',
        'Project editing must be enabled.'
      ]
    });
  });

  it('allows author edits with a persisted concept and editing enabled', () => {
    expect(buildConceptMutationReadiness(3, 123, 'AUTHOR', true)).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('normalizes project role casing for readiness checks', () => {
    expect(buildConceptMutationReadiness(3, 123, 'author', true)).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing undo/redo action prerequisites', () => {
    expect(buildActionMutationReadiness(3, null, '', 'AUTHOR', false)).toEqual({
      canExecute: false,
      reasons: [
        'Molecular action id is required.',
        'Activity id is required.',
        'Project editing must be enabled.'
      ]
    });
  });

  it('allows author undo/redo with an activity id and editing enabled project', () => {
    expect(buildActionMutationReadiness(3, 9001, 'ACT-123', 'author', true)).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing atom mutation prerequisites', () => {
    expect(
      buildAtomMutationReadiness(3, 123, null, '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'Atom id is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('allows author atom edits with concept and atom context', () => {
    expect(
      buildAtomMutationReadiness(3, 123, 1001, 'ACT-123', 1770000000000, 'AUTHOR', true)
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing semantic type mutation prerequisites', () => {
    expect(
      buildSemanticTypeMutationReadiness(3, 123, null, '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'Semantic type id is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('allows author semantic type edits with concept and semantic type context', () => {
    expect(
      buildSemanticTypeMutationReadiness(
        3,
        123,
        3001,
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing semantic type add prerequisites', () => {
    expect(
      buildSemanticTypeAddReadiness(3, 123, '', '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'Semantic type is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('allows author semantic type adds with concept and semantic type context', () => {
    expect(
      buildSemanticTypeAddReadiness(
        3,
        123,
        'Finding',
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing attribute mutation prerequisites', () => {
    expect(
      buildAttributeMutationReadiness(3, 123, null, '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'Attribute id is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('allows author attribute edits with concept and attribute context', () => {
    expect(
      buildAttributeMutationReadiness(
        3,
        123,
        4001,
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing attribute add prerequisites', () => {
    expect(
      buildAttributeAddReadiness(3, 123, '', '', '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'Attribute name is required.',
        'Attribute value is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('allows author attribute adds with concept and attribute context', () => {
    expect(
      buildAttributeAddReadiness(
        3,
        123,
        'Concept_Status',
        'Reviewed',
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing relationship mutation prerequisites', () => {
    expect(
      buildRelationshipMutationReadiness(3, 123, null, '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'Relationship id is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('allows author relationship edits with concept and relationship context', () => {
    expect(
      buildRelationshipMutationReadiness(
        3,
        123,
        5001,
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing relationship add prerequisites', () => {
    expect(
      buildRelationshipAddReadiness(3, 123, null, '', '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'Target concept id is required.',
        'Relationship type is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('prevents self-referential relationship adds', () => {
    expect(
      buildRelationshipAddReadiness(
        3,
        123,
        123,
        'RO',
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: false,
      reasons: ['Target concept must be different from the current concept.']
    });
  });

  it('allows author relationship adds with target and type context', () => {
    expect(
      buildRelationshipAddReadiness(
        3,
        123,
        456,
        'RO',
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing relationship batch add prerequisites', () => {
    expect(
      buildRelationshipsAddReadiness(3, 123, [], '', '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'At least one target concept is required.',
        'Relationship type is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('prevents self-referential relationship batch adds', () => {
    expect(
      buildRelationshipsAddReadiness(
        3,
        123,
        [456, 123],
        'RO',
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: false,
      reasons: ['Target concept must be different from the current concept.']
    });
  });

  it('allows author relationship batch adds with selected targets', () => {
    expect(
      buildRelationshipsAddReadiness(
        3,
        123,
        [456, 789],
        'RO',
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing merge prerequisites', () => {
    expect(
      buildMergeConceptReadiness(3, 123, null, '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'Target concept id is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('prevents self-referential merges', () => {
    expect(
      buildMergeConceptReadiness(
        3,
        123,
        123,
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: false,
      reasons: ['Target concept must be different from the current concept.']
    });
  });

  it('allows author merges with target concept context', () => {
    expect(
      buildMergeConceptReadiness(
        3,
        123,
        456,
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing move atom prerequisites', () => {
    expect(
      buildMoveAtomsReadiness(3, 123, null, [], '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'Target concept id is required.',
        'At least one atom is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('prevents moving atoms onto the same concept', () => {
    expect(
      buildMoveAtomsReadiness(
        3,
        123,
        123,
        [1001],
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: false,
      reasons: ['Target concept must be different from the current concept.']
    });
  });

  it('allows author atom moves with target and atom context', () => {
    expect(
      buildMoveAtomsReadiness(
        3,
        123,
        456,
        [1001, 1002],
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });

  it('reports missing split prerequisites', () => {
    expect(
      buildSplitConceptReadiness(3, 123, [], '', '', null, 'AUTHOR', true)
    ).toEqual({
      canExecute: false,
      reasons: [
        'At least one atom is required.',
        'Relationship type is required.',
        'Activity id is required.',
        'Concept lastModified timestamp is required.'
      ]
    });
  });

  it('allows author concept splits with atom and relationship context', () => {
    expect(
      buildSplitConceptReadiness(
        3,
        123,
        [1001, 1002],
        'RN',
        'ACT-123',
        1770000000000,
        'AUTHOR',
        true
      )
    ).toEqual({
      canExecute: true,
      reasons: []
    });
  });
});
