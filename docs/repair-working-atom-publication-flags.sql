-- RESEARCH DRAFT (2026-10-22): operational implementation is DEFERRED.
-- Retained for reference, not a record of a completed repair or deployment.
-- See README.md, "Release QA Research Status".
-- The operator reported ROLLBACK after a RADLEX-only trial matched 74 IS and
-- 940 OP atoms (1,014 total). CVX/VANDF were excluded from that trial. The trial
-- counts do not establish a committed correction or passing release QA.
-- Candidate atom-loader code exists in the checkout; deployment is not verified.
--
-- Proposed one-time MySQL repair for the ACTIVE EDITING database.
-- This draft contains UPDATE statements: it is NOT read-only. Keep the schema
-- placeholder and final ROLLBACK while implementation is deferred. Do not run it
-- as an operational repair merely because it is committed for reference.
-- Future implementation would need a fresh candidate audit and a stopped
-- application, because SQL bypasses application caches and the search index.
--
-- Set the exact schema name shown by SELECT DATABASE(). A mismatched or unchanged
-- placeholder deliberately selects no rows. This does not identify frozen DBs;
-- the operator must select the correct working database for the next release.
SET @working_schema = 'REPLACE_WITH_ACTIVE_EDITING_SCHEMA';

-- Default: include all three source/TTY groups from the original QA findings.
-- 1: RADLEX 4_3 OP/IS plus the carried-forward CVX/VANDF AB exceptions.
-- 0: RADLEX only, for a deliberately partial repair.
-- The earlier audit found 1,014 RADLEX and 21,552 CVX/VANDF candidates (22,566
-- total). Those findings may come from different snapshots; any future repair
-- must preview the active editing database rather than assume those counts.
-- Older AB exceptions still violate the policy and need correction for the next
-- release. lastModified does not establish when a flag became incorrect.
SET @include_carried_ab = 1;

SELECT DATABASE() AS selectedSchema, @@hostname AS serverHost,
       @working_schema AS requiredWorkingSchema,
       @include_carried_ab AS includeCarriedAb;

START TRANSACTION;

-- Research preview of the proposed scope; not proof of a completed correction.
SELECT a.terminology AS source, a.version AS sourceVersion, a.termType AS tty,
       COUNT(DISTINCT a.id) AS candidates
FROM atoms a
JOIN concepts_atoms ca ON ca.atoms_id = a.id
JOIN concepts c ON c.id = ca.concepts_id
WHERE DATABASE() = @working_schema
  AND c.terminology = 'NCIMTH' AND c.version = 'latest'
  AND c.publishable = 1 AND a.publishable = 1
  AND ((a.terminology = 'RADLEX' AND a.version = '4_3'
      AND a.termType IN ('OP', 'IS'))
    OR (@include_carried_ab = 1 AND a.termType = 'AB'
      AND ((a.terminology = 'CVX' AND a.version = '2026_02_12')
        OR (a.terminology = 'VANDF' AND a.version = '2026_01_30'))))
  AND ((a.termType = 'AB' AND a.suppressible = 0)
    OR (a.termType IN ('OP', 'IS')
      AND (a.suppressible = 0 OR a.obsolete = 0)))
GROUP BY a.terminology, a.version, a.termType
ORDER BY source, sourceVersion, tty;

UPDATE atoms a
JOIN concepts_atoms ca ON ca.atoms_id = a.id
JOIN concepts c ON c.id = ca.concepts_id
SET a.suppressible = b'1',
    a.obsolete = CASE WHEN a.termType IN ('OP', 'IS') THEN b'1'
                      ELSE a.obsolete END,
    a.lastModified = NOW(),
    a.lastModifiedBy = 'NCI_ATOM_POLICY_FIX'
WHERE DATABASE() = @working_schema
  AND c.terminology = 'NCIMTH' AND c.version = 'latest'
  AND c.publishable = 1 AND a.publishable = 1
  AND ((a.terminology = 'RADLEX' AND a.version = '4_3'
      AND a.termType IN ('OP', 'IS'))
    OR (@include_carried_ab = 1 AND a.termType = 'AB'
      AND ((a.terminology = 'CVX' AND a.version = '2026_02_12')
        OR (a.terminology = 'VANDF' AND a.version = '2026_01_30'))))
  AND ((a.termType = 'AB' AND a.suppressible = 0)
    OR (a.termType IN ('OP', 'IS')
      AND (a.suppressible = 0 OR a.obsolete = 0)));

SELECT ROW_COUNT() AS changedAtoms;

-- Research preview only: leave ROLLBACK while implementation is deferred.
-- All tables involved are InnoDB in the checked-in schema.
ROLLBACK;

-- Deferred validation proposal: a future committed repair would need a repeat
-- audit, a working-index rebuild with the application stopped, preferred-name
-- recomputation, regenerated release files, and QA. A full rebuild also refreshes
-- atom flags embedded in concept/code/descriptor documents. SQL does not create
-- application history records. None of those follow-up operations is implemented
-- or performed by this research script; published files are outside its scope.
