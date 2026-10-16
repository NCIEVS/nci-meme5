-- Read-only diagnostics for recurring CVX|AB|N and VANDF|AB|N release QA errors.
-- Run against the confirmed release-build database. These queries assume the
-- NCIMTH/latest project; adjust those literals if the release uses another one.
-- No release files or database rows are changed.
--
-- NCIMTH's established release policy overrides incoming AB suppression to Y,
-- and makes OP/IS atoms suppressible and obsolete. Atom insertion now applies
-- this policy before saving new or updated atoms. Release generation does not
-- repair legacy flags. For unpublished builds, these diagnostics identify data
-- that may need a one-time correction or insertion reprocessing and verify flags
-- after loading. Published releases remain unchanged; no retroactive correction
-- is required for them.
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
