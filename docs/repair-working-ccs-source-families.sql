-- COMPLETED REPAIR REFERENCE (2026-10-22).
-- The operator confirmed that CCS source families are implemented and complete.
-- See README.md, "NCI CCS Source Families (Completed)".
-- Atom-flag and MRCUI research remains deferred; CCS work is complete.
--
-- Archived one-time MySQL metadata repair for the ACTIVE EDITING database.
-- This file contains UPDATE statements and retains the schema placeholder and
-- final ROLLBACK as preview defaults. Completion is recorded above; the defaults
-- preserve the script for reference and do not indicate outstanding CCS work.
-- Operational context: SQL bypasses metadata caches, requiring a stopped
-- application and restart. This root metadata field needs no content-index rebuild.
--
-- This guard does not identify frozen databases. The operator must choose the
-- active editing database for the next release, even if a frozen copy also has
-- an NCIMTH/latest project.
SET @working_schema = 'REPLACE_WITH_ACTIVE_EDITING_SCHEMA';

SELECT DATABASE() AS selectedSchema, @@hostname AS serverHost,
       @working_schema AS requiredWorkingSchema;

-- Inspect the current roots/families before any update. The two CCSR sources
-- should share the current CCSR_ICD10PCS family; the old CCS_10 name was renamed
-- in 2021 and need not be an emitted RSAB in current release files.
SELECT rt.id, rt.terminology AS source, t.version AS sourceVersion,
       rt.family AS currentFamily, 'CCSR_ICD10PCS' AS proposedFamily
FROM root_terminologies rt
JOIN terminologies t ON t.rootTerminology_id = rt.id
WHERE t.`current` = 1
  AND rt.terminology IN ('CCSR_ICD10CM', 'CCSR_ICD10PCS')
ORDER BY source, sourceVersion;

-- An absent/non-current anchor deliberately prevents the update below.
SELECT terminology AS familyAnchor, version, `current` + 0 AS isCurrent
FROM terminologies
WHERE terminology = 'CCSR_ICD10PCS' AND `current` = 1;

START TRANSACTION;

UPDATE root_terminologies rt
JOIN terminologies t ON t.rootTerminology_id = rt.id AND t.`current` = 1
JOIN terminologies anchor ON anchor.terminology = 'CCSR_ICD10PCS'
                         AND anchor.`current` = 1
SET rt.family = anchor.terminology,
    rt.lastModified = NOW(),
    rt.lastModifiedBy = 'NCI_CCS_FAMILY_FIX',
    rt.timestamp = NOW()
WHERE DATABASE() = @working_schema
  AND EXISTS (SELECT 1 FROM projects p
              WHERE p.terminology = 'NCIMTH' AND p.version = 'latest')
  AND rt.terminology IN ('CCSR_ICD10CM', 'CCSR_ICD10PCS')
  AND rt.family IN ('CCS', 'CCS_10', 'CCSR_10');

SELECT ROW_COUNT() AS changedRoots;

SELECT rt.terminology AS source, rt.family AS resultingFamily
FROM root_terminologies rt
WHERE rt.terminology IN ('CCSR_ICD10CM', 'CCSR_ICD10PCS')
ORDER BY source;

-- Archived preview default; the completed repair is recorded in the header.
-- The checked-in root_terminologies table uses InnoDB.
ROLLBACK;

-- Release-format reference: MRSAB field 4 contains CCSR_ICD10PCS, and both
-- roots use that value in field 6 for the SF/RSAB QA comparison. This correction
-- changes metadata only, not atoms, content publication flags, or application
-- history. The next editing-to-release database clone carries it forward.
