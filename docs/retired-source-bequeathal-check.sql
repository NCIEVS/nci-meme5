SELECT DISTINCT c.id AS clusterId, c.id AS conceptId
FROM atoms a
JOIN concepts_atoms ca ON ca.atoms_id = a.id
JOIN concepts c ON c.id = ca.concepts_id
JOIN terminologies t ON a.codeId = CONCAT('V-', t.terminology, '_', t.version)
  AND t.current = false
WHERE a.terminology = 'SRC' AND a.termType = 'VAB' AND a.publishable = false
  AND c.terminology = 'NCIMTH' AND c.version = 'latest'
  AND EXISTS (
    SELECT 1 FROM terminologies current_source
    WHERE current_source.terminology = t.terminology AND current_source.current = true
  )
  AND NOT EXISTS (
    SELECT 1 FROM concepts_atoms same_ca
    JOIN atoms same_root ON same_root.id = same_ca.atoms_id
    WHERE same_ca.concepts_id = c.id AND same_root.terminology = 'SRC'
      AND same_root.termType = 'RAB' AND same_root.publishable = true
      AND (same_root.codeId = CONCAT('V-', t.terminology)
        OR (t.terminology = 'CDCREC' AND same_root.codeId = 'V-CDC')
        OR (t.terminology IN ('MTHSPL', 'MTHCMSFRF') AND same_root.codeId = 'V-MTH'))
      AND c.publishable = true
  )
  AND NOT EXISTS (
    SELECT 1 FROM concept_relationships r
    JOIN concepts root ON root.id = r.to_id AND root.publishable = true
      AND root.terminology = c.terminology AND root.version = c.version
    JOIN concepts_atoms root_ca ON root_ca.concepts_id = root.id
    JOIN atoms root_atom ON root_atom.id = root_ca.atoms_id
    WHERE r.from_id = c.id AND r.relationshipType IN ('BRO', 'BRN', 'BRB')
      AND root_atom.terminology = 'SRC' AND root_atom.termType = 'RAB'
      AND root_atom.publishable = true
      AND (root_atom.codeId = CONCAT('V-', t.terminology)
        OR (t.terminology = 'CDCREC' AND root_atom.codeId = 'V-CDC')
        OR (t.terminology IN ('MTHSPL', 'MTHCMSFRF') AND root_atom.codeId = 'V-MTH'))
  )
