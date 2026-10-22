-- One-time MySQL metadata repair for the ACTIVE EDITING database only.
-- Do not run against the frozen 202608 database or edit its published files.
-- Deploy the metadata-loader fix so future insertions retain this grouping.
-- Stop the application before committing; restart it to clear metadata caches.
-- No content-index rebuild is needed for this root metadata field.
-- This script defaults to ROLLBACK. Review the preview, then replace the final
-- ROLLBACK with COMMIT and rerun only against the intended working database.
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

-- Preview only. The checked-in root_terminologies table uses InnoDB.
ROLLBACK;

-- After an actual COMMIT, restart the app and generate MRSAB for the NEXT
-- release. Confirm CCSR_ICD10PCS occurs in field 4 and both roots have that
-- value in field 6; rerun the existing SF/RSAB QA check. This SQL updates only
-- metadata, not atoms, content publication flags, or application history.
