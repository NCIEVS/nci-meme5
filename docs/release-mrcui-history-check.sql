-- RESEARCH / READ-ONLY DIAGNOSTICS (2026-10-22).
-- Historical-concept repair and prevention have NOT been implemented.
-- See README.md, "Release QA Research Status". This file preserves investigation
-- queries, not a completed fix or a passing QA result. Confirm hostname, schema,
-- snapshot, and deployed revision before reusing findings on the editing server.
--
-- Diagnostics for "ERROR: Old CUIs not in current version".
-- Run queries 1-3 against the MEME database snapshot used to BUILD the affected
-- release, preferably before manual DEL repairs or release feedback. A database
-- containing only imported RRF tables cannot explain the generator's decisions.
-- Assumes MySQL 8+, project NCIMTH/latest; adjust those literals if necessary.
-- Every statement is SELECT/WITH ... SELECT. No tables or rows are changed.
-- The five CUIs below were reported by the QA rerun on October 21, 2026,
-- while investigating the 202608 release. They replace the earlier 202602 list.
-- qa_checks.csh displays at most ten missing CUIs. Rerunning QA checks existing
-- release files; it does not regenerate MRCUI.RRF from database history.
--
-- Interpretation:
-- 1. A NULL conceptId means no NCIMTH/latest concept was found. A non-current
--    CUI with hasPriorAtomMapping=0 and no unpublishable concept can be absent
--    from the generator's database-derived previousCuis set.
-- 2. Stored history takes precedence over editor bequeathals when nonempty.
--    The writer handles DEL, SY, and R* history; inspect other relationship
--    types, missing targets, and targets without history. A history row not
--    linked through concepts_component_histories is not read from the concept.
-- 3. BRO/BRN/BRB targets determine bequeathals for concepts without history.
--    This checkout adds DEL when all bequeathal targets are dead. A dead-only
--    cycle therefore needs comparison with the actual deployed generator.
--    Relationship publishable/obsolete flags are displayed, not filtered:
--    getCurrentBequeathalRels() does not filter them either.
-- 4. The mrcui table is a separately loaded copy, not the file read by QA.
--    Confirm which release file populated it before interpreting these rows.
--
-- Send the results of 1-3, the release identifier, and the deployed application
-- revision. Include 4 if the mrcui table's provenance is known. Current database
-- state alone cannot prove what was present when an older release was built.

-- 1. Concept presence, publication state, and inputs used by the generator.
-- LEFT JOIN preserves the requested CUIs even when their concepts are missing.
WITH requested(cui) AS (
  SELECT 'C1551311' UNION ALL SELECT 'C2107943'
  UNION ALL SELECT 'C3495370' UNION ALL SELECT 'C3854048'
  UNION ALL SELECT 'C4723748'
)
SELECT q.cui, c.id AS conceptId, c.terminology, c.version, c.branch,
       c.name, c.publishable + 0 AS publishable,
       c.published + 0 AS published,
       c.lastModified, c.lastModifiedBy,
       EXISTS (
         SELECT 1 FROM atomjpa_conceptterminologyids a
         WHERE a.conceptTerminologyIds_KEY = 'NCIMTH'
           AND a.conceptTerminologyIds = q.cui
       ) AS hasPriorAtomMapping,
       (SELECT COUNT(*) FROM concepts_component_histories j
        WHERE j.concepts_id = c.id) AS attachedHistoryCount,
       (SELECT COUNT(*) FROM concept_relationships r
        WHERE r.from_id = c.id
          AND r.relationshipType IN ('BRO', 'BRN', 'BRB')) AS bequeathalCount
FROM requested q
LEFT JOIN concepts c ON c.terminologyId = q.cui
  AND c.terminology = 'NCIMTH' AND c.version = 'latest'
ORDER BY q.cui, c.id;

-- 2. Stored history, including one level of bequeathal/history targets.
-- This also exposes histories for the other endpoint of a two-concept cycle.
-- Missing history or a missing concept remains visible as NULL columns.
WITH requested(cui) AS (
  SELECT 'C1551311' UNION ALL SELECT 'C2107943'
  UNION ALL SELECT 'C3495370' UNION ALL SELECT 'C3854048'
  UNION ALL SELECT 'C4723748'
), seeds AS (
  SELECT c.id FROM concepts c JOIN requested q ON q.cui = c.terminologyId
  WHERE c.terminology = 'NCIMTH' AND c.version = 'latest'
), focus(cui) AS (
  SELECT cui FROM requested
  UNION
  SELECT t.terminologyId FROM seeds s
  JOIN concept_relationships r ON r.from_id = s.id
  JOIN concepts t ON t.id = r.to_id
  WHERE r.relationshipType IN ('BRO', 'BRN', 'BRB')
    AND t.terminology = 'NCIMTH' AND t.version = 'latest'
  UNION
  SELECT h.referencedTerminologyId FROM seeds s
  JOIN concepts_component_histories j ON j.concepts_id = s.id
  JOIN component_histories h ON h.id = j.componentHistories_id
  WHERE h.referencedTerminologyId IS NOT NULL
    AND h.referencedTerminologyId <> ''
)
SELECT f.cui, c.id AS conceptId, c.branch,
       c.publishable + 0 AS conceptPublishable,
       h.id AS historyId, h.associatedRelease,
       h.relationshipType, h.referencedTerminologyId AS targetCui,
       h.terminology AS historyTerminology, h.version AS historyVersion,
       h.lastModified AS historyModified, h.lastModifiedBy AS historyModifiedBy,
       EXISTS (
         SELECT 1 FROM concepts t
         WHERE t.terminologyId = h.referencedTerminologyId
           AND t.terminology = 'NCIMTH' AND t.version = 'latest'
           AND t.publishable = 1
       ) AS targetInCurrentCuis
FROM focus f
LEFT JOIN concepts c ON c.terminologyId = f.cui
  AND c.terminology = 'NCIMTH' AND c.version = 'latest'
LEFT JOIN concepts_component_histories j ON j.concepts_id = c.id
LEFT JOIN component_histories h ON h.id = j.componentHistories_id
ORDER BY f.cui, c.id, h.associatedRelease, h.id;

-- 3. Editor bequeathals and the bequeathals of their direct targets.
WITH requested(cui) AS (
  SELECT 'C1551311' UNION ALL SELECT 'C2107943'
  UNION ALL SELECT 'C3495370' UNION ALL SELECT 'C3854048'
  UNION ALL SELECT 'C4723748'
), seeds AS (
  SELECT c.id FROM concepts c JOIN requested q ON q.cui = c.terminologyId
  WHERE c.terminology = 'NCIMTH' AND c.version = 'latest'
), focus(id) AS (
  SELECT id FROM seeds
  UNION
  SELECT r.to_id FROM seeds s
  JOIN concept_relationships r ON r.from_id = s.id
  WHERE r.relationshipType IN ('BRO', 'BRN', 'BRB')
)
SELECT c.terminologyId AS fromCui, c.id AS fromId,
       c.publishable + 0 AS fromPublishable,
       r.id AS relationshipId, r.relationshipType,
       r.terminology AS relationshipTerminology,
       r.version AS relationshipVersion,
       r.publishable + 0 AS relationshipPublishable,
       r.obsolete + 0 AS relationshipObsolete,
       t.terminologyId AS toCui, t.id AS toId,
       t.terminology AS toTerminology, t.version AS toVersion,
       t.publishable + 0 AS toPublishable,
       EXISTS (
         SELECT 1 FROM concepts live
         WHERE live.terminologyId = t.terminologyId
           AND live.terminology = 'NCIMTH' AND live.version = 'latest'
           AND live.publishable = 1
       ) AS targetInCurrentCuis,
       r.lastModified, r.lastModifiedBy
FROM focus f
JOIN concepts c ON c.id = f.id
JOIN concept_relationships r ON r.from_id = c.id
JOIN concepts t ON t.id = r.to_id
WHERE r.relationshipType IN ('BRO', 'BRN', 'BRB')
ORDER BY fromCui, fromId, relationshipId;

-- 4. Optional: MRCUI rows where each sampled CUI is source or destination.
-- No rows here establishes absence from THIS TABLE, not from MRCUI.RRF.
WITH requested(cui) AS (
  SELECT 'C1551311' UNION ALL SELECT 'C2107943'
  UNION ALL SELECT 'C3495370' UNION ALL SELECT 'C3854048'
  UNION ALL SELECT 'C4723748'
)
SELECT q.cui AS requestedCui, m.cui1, m.version, m.rel, m.rela,
       m.cui2, m.mapin + 0 AS mapin
FROM requested q
LEFT JOIN mrcui m ON m.cui1 = q.cui OR m.cui2 = q.cui
ORDER BY q.cui, m.cui1, m.version, m.rel, m.cui2;
