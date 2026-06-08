TRUNCATE TABLE deep_atom_relationships;

INSERT INTO deep_atom_relationships
SELECT id AS relationship_id, 'ATOM' as component_type,
     terminologyId, terminology, version,
     relationshipType, additionalRelationshipType,
     obsolete, suppressible, published, publishable,
     workflowStatus, lastModifiedby, lastModified,
     from_id AS from_atoms_id, to_id AS to_atoms_id
FROM atom_relationships WHERE publishable=true
AND workflowStatus in ('PUBLISHED','READY_FOR_PUBLICATION')
AND from_id < to_id;

INSERT INTO deep_atom_relationships
SELECT id AS relationship_id, 'CONCEPT' as component_type,
      terminologyId,terminology,version,
      relationshipType, additionalRelationshipType,
      obsolete, suppressible, published, publishable,
      workflowStatus, lastModifiedby, lastModified,
      ca1.atoms_id AS from_atoms_id, ca2.atoms_id AS to_atoms_id
FROM concept_relationships cr, concepts_atoms ca1, concepts_atoms ca2
WHERE publishable=true
AND cr.workflowStatus in ('PUBLISHED','READY_FOR_PUBLICATION')
AND cr.from_id = ca1.concepts_id
AND cr.to_id = ca2.concepts_id
AND cr.from_id < cr.to_id;

INSERT INTO deep_atom_relationships
SELECT id AS relationship_id, 'CODE' as component_type,
    terminologyId,terminology,version,
    relationshipType, additionalRelationshipType,
    obsolete, suppressible, published, publishable,
    workflowStatus, lastModifiedby, lastModified,
    ca1.atoms_id AS from_atoms_id, ca2.atoms_id AS to_atoms_id
FROM code_relationships cr, codes_atoms ca1, codes_atoms ca2
WHERE publishable=true
AND workflowStatus in ('PUBLISHED','READY_FOR_PUBLICATION')
AND cr.from_id = ca1.codes_id AND cr.to_id = ca2.codes_id
AND from_id < to_id;

INSERT INTO deep_atom_relationships
SELECT id AS relationship_id, 'DESCRIPTOR' as component_type,
     terminologyId,terminology,version,
     relationshipType, additionalRelationshipType,
     obsolete, suppressible, published, publishable,
     workflowStatus, lastModifiedby, lastModified,
     from_id AS from_atoms_id, to_id AS to_atoms_id
 FROM descriptor_relationships WHERE publishable=true
 AND workflowStatus in ('PUBLISHED','READY_FOR_PUBLICATION')
 AND from_id < to_id;

COMMIT;

TRUNCATE TABLE deep_concept_relationships;

INSERT INTO deep_concept_relationships
SELECT id AS relationship_id,'CONCEPT' as component_type,
     terminologyId,terminology,version,
     relationshipType, additionalRelationshipType,
     obsolete,suppressible,published,publishable,
     workflowStatus,lastModifiedby,lastModified,
     from_id AS from_concepts_id, to_id AS to_concepts_id
FROM concept_relationships
WHERE publishable=true
AND workflowStatus in ('PUBLISHED','READY_FOR_PUBLICATION')
AND from_id < to_id;

INSERT INTO deep_concept_relationships
SELECT DISTINCT dar.relationship_id, dar.component_type,
    dar.terminologyId, dar.terminology, dar.version,
    dar.relationshipType, dar.additionalRelationshipType,
    dar.obsolete, dar.suppressible, dar.published, dar.publishable,
    dar.workflowStatus, dar.lastModifiedby, dar.lastModified,
    ca1.concepts_id AS from_concepts_id, ca2.concepts_id AS to_concepts_id
FROM deep_atom_relationships dar, concepts_atoms ca1, concepts_atoms ca2
WHERE dar.from_atoms_id = ca1.atoms_id AND dar.to_atoms_id = ca2.atoms_id;
