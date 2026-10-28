-- RESEARCH / READ-ONLY DIAGNOSTICS (2026-10-22).
-- Operational implementation of the proposed corrections is DEFERRED.
-- See README.md, "Release QA Research Status". The RADLEX-only repair trial was
-- rolled back; these queries do not apply a repair or demonstrate deployment.
-- Candidate loader code exists in the checkout; running-server behavior and a
-- passing regenerated release have not been established by these notes.
--
-- Diagnostics for recurring CVX|AB|N and VANDF|AB|N release QA errors.
-- Queries 1-5 concern the affected release-build snapshot; queries 6-7 inventory
-- the active editing database. Record hostname and schema for each result set:
-- ncimdb and matching IDs alone do not distinguish editing from release copies.
-- These queries assume NCIMTH/latest; adjust if the release uses another project.
-- No release files or database rows are changed by this file.
--
-- NCIMTH's established release policy overrides incoming AB suppression to Y,
-- and makes OP/IS atoms suppressible and obsolete. The candidate atom-insertion
-- changes apply this policy before saving new or updated atoms; deployment is
-- deferred. Release generation does not repair legacy flags. These diagnostics
-- identify candidates for a possible future one-time correction and compare
-- current flags. They do not implement that correction. Replaying insertions
-- over later edits is not the proposed repair. Published files are outside scope.
--
-- The QA comparison uses:
--   comm -13 MRRANK.sts MRCONSO.sts
-- It reports MRCONSO triples missing from MRRANK. Older scripts reverse the
-- direction in the displayed error message.
-- Deleting MRRANK rows cannot reduce that difference for unchanged MRCONSO.
-- Compare the deployed script if deleting rows appears to resolve the error.

-- 1. MRRANK takes SUPPRESS from this project-wide TTY setting, independent of SAB.
-- A value of 1 makes every emitted AB rank row Y; a value of 0 makes it N.
SELECT id, terminology, version, abbreviation,
       suppressible + 0 AS suppressible,
       lastModified, lastModifiedBy
FROM term_types
WHERE terminology = 'NCIMTH' AND version = 'latest' AND abbreviation = 'AB';

-- 2. Check whether the release precedence list contains both term groups.
-- A missing pair is another reason for MRRANK to omit a group present in MRCONSO.
-- The two collection tables must be joined at the same list position.
SELECT p.id AS precedenceListId, p.name,
       s.terminologies AS source, t.termTypes AS tty,
       s.terminologies_ORDER AS precedencePosition
FROM precedence_lists p
JOIN precedence_list_terminologies s ON s.PrecedenceListJpa_id = p.id
JOIN precedence_list_term_types t ON t.PrecedenceListJpa_id = p.id
  AND t.termTypes_ORDER = s.terminologies_ORDER
WHERE p.terminology = 'NCIMTH' AND p.version = 'latest'
  AND s.terminologies IN ('CVX', 'VANDF') AND t.termTypes = 'AB'
ORDER BY p.id, s.terminologies_ORDER;

-- 3. Reproduce the two writers' SUPPRESS decisions on release-eligible atoms.
-- With AB suppressible=1, non-obsolete atoms with suppressible=0 produce
-- MRCONSO=N but MRRANK=Y. With AB suppressible=0, suppressed atoms produce E,
-- which this QA comparison excludes, together with obsolete atoms (O).
-- Multiple atom versions can be legitimate: the writers filter the concept's
-- project/version, not the atom's source version.
SELECT a.terminology AS source, a.version AS sourceVersion,
       a.termType AS tty,
       a.obsolete + 0 AS obsolete, a.suppressible + 0 AS atomSuppressible,
       CASE WHEN t.suppressible = 1 THEN 'Y' ELSE 'N' END AS mrrankSuppress,
       CASE WHEN a.obsolete = 1 THEN 'O'
            WHEN a.suppressible = 0 THEN 'N'
            WHEN t.suppressible = 1 THEN 'Y'
            ELSE 'E' END AS mrconsoSuppress,
       COUNT(DISTINCT a.id) AS atomCount
FROM atoms a
JOIN concepts_atoms ca ON ca.atoms_id = a.id
JOIN concepts c ON c.id = ca.concepts_id
JOIN term_types t ON t.abbreviation = a.termType
  AND t.terminology = 'NCIMTH' AND t.version = 'latest'
WHERE c.terminology = 'NCIMTH' AND c.version = 'latest'
  AND c.publishable = 1 AND a.publishable = 1
  AND a.terminology IN ('CVX', 'VANDF') AND a.termType = 'AB'
GROUP BY a.terminology, a.version, a.termType, a.obsolete,
         a.suppressible, t.suppressible
ORDER BY source, sourceVersion, mrconsoSuppress;

-- 4. Identify incoming/edited atoms responsible for unsuppressed release rows.
-- Under the established NCIMTH policy, these atoms require suppressible=true.
SELECT DISTINCT a.id AS atomId, a.terminology AS source,
       a.version AS sourceVersion, a.codeId, a.name,
       a.lastModified, a.lastModifiedBy
FROM atoms a
JOIN concepts_atoms ca ON ca.atoms_id = a.id
JOIN concepts c ON c.id = ca.concepts_id
WHERE c.terminology = 'NCIMTH' AND c.version = 'latest'
  AND c.publishable = 1 AND a.publishable = 1
  AND a.terminology IN ('CVX', 'VANDF') AND a.termType = 'AB'
  AND a.obsolete = 0 AND a.suppressible = 0
ORDER BY a.terminology, a.version, a.id
LIMIT 50;

-- 5. Check the independent eligibility mismatch between the writers.
-- MRRANK currently omits the concept version and concept publishable filters.
-- metadataCandidateAtoms>0 and consoCandidateAtoms=0 can produce an orphan rank
-- row when the pair is in the precedence list. This explains extra MRRANK rows,
-- but the checked-in comm -13 comparison does not report those extra rows.
SELECT a.terminology AS source, a.termType AS tty,
       COUNT(DISTINCT a.id) AS metadataCandidateAtoms,
       COUNT(DISTINCT CASE WHEN c.version = 'latest' AND c.publishable = 1
                           THEN a.id END) AS consoCandidateAtoms
FROM atoms a
JOIN concepts_atoms ca ON ca.atoms_id = a.id
JOIN concepts c ON c.id = ca.concepts_id
WHERE c.terminology = 'NCIMTH' AND a.publishable = 1
  AND a.terminology IN ('CVX', 'VANDF') AND a.termType = 'AB'
GROUP BY a.terminology, a.termType
ORDER BY source;

-- 6. Transition audit for the ACTIVE EDITING database after the August 2026
-- publication. This covers every source with AB/OP/IS policy violations, whether
-- introduced by insertion or editing, including unpublishable working content.
-- Run on the working database for the NEXT release, not a frozen release copy.
-- Do not filter by lastModified or published=false: existing published atoms can
-- be updated in the working database, and modification dates are not an audit of
-- individual flag changes. The NCIMTH/latest membership defines working scope.
-- The output is an inventory, not authorization to update any database.
SELECT a.terminology AS source, a.version AS sourceVersion, a.termType AS tty,
       c.publishable + 0 AS conceptPublishable,
       a.publishable + 0 AS atomPublishable,
       a.published + 0 AS previouslyPublished,
       a.suppressible + 0 AS currentSuppressible,
       a.obsolete + 0 AS currentObsolete,
       COUNT(DISTINCT a.id) AS atomCount,
       MIN(a.lastModified) AS earliestModification,
       MAX(a.lastModified) AS latestModification
FROM atoms a
JOIN concepts_atoms ca ON ca.atoms_id = a.id
JOIN concepts c ON c.id = ca.concepts_id
WHERE c.terminology = 'NCIMTH' AND c.version = 'latest'
  AND ((a.termType = 'AB' AND a.suppressible = 0)
    OR (a.termType IN ('OP', 'IS')
      AND (a.suppressible = 0 OR a.obsolete = 0)))
GROUP BY a.terminology, a.version, a.termType, c.publishable,
         a.publishable, a.published, a.suppressible, a.obsolete
ORDER BY source, sourceVersion, tty, conceptPublishable, atomPublishable,
         previouslyPublished, currentSuppressible, currentObsolete;

-- 7. Complete candidate inventory for review/export in that same working DB.
-- The deferred catch-up proposal would apply NciAtomPublicationPolicy through
-- application persistence, preserving indexing and change tracking. It has not
-- been implemented as a catch-up operation by this research work.
-- Change only suppressible for AB, and suppressible/obsolete for OP/IS; preserve
-- AB obsolescence, identity, names, source versions, concept memberships,
-- relationships, workflow status, publishable, and published flags.
-- Do not replay source insertions over subsequent editorial work for this repair.
-- After a complete future catch-up, queries 6 and 7 should return no rows; that
-- outcome has not been established. The narrower repair SQL draft does not
-- cover this entire inventory. These queries never mutate data.
SELECT DISTINCT a.id AS atomId, a.terminology AS source,
       a.version AS sourceVersion, a.termType AS tty, a.codeId, a.name,
       a.suppressible + 0 AS currentSuppressible,
       a.obsolete + 0 AS currentObsolete,
       a.publishable + 0 AS atomPublishable,
       a.published + 0 AS previouslyPublished,
       a.lastModified, a.lastModifiedBy
FROM atoms a
JOIN concepts_atoms ca ON ca.atoms_id = a.id
JOIN concepts c ON c.id = ca.concepts_id
WHERE c.terminology = 'NCIMTH' AND c.version = 'latest'
  AND ((a.termType = 'AB' AND a.suppressible = 0)
    OR (a.termType IN ('OP', 'IS')
      AND (a.suppressible = 0 OR a.obsolete = 0)))
ORDER BY a.terminology, a.version, a.termType, a.id;
