-- RESEARCH / READ-ONLY DIAGNOSTICS (2026-10-22).
-- Historical-concept repair and prevention have NOT been implemented.
-- See README.md, "Release QA Research Status". Findings below preserve reported
-- observations and hypotheses, not a completed fix or passing release QA.
--
-- Investigation of missing historical NCIMTH concepts.
-- Reported scope: ncimdb, project 39751, NCIMTH/latest, release process 39856
-- targeting 202608. The earlier results did not record a hostname, so release-
-- versus editing-server provenance must be verified before reusing the findings.
-- The editing host was later identified as ip-172-17-5-22, also using ncimdb.
-- Matching schema names or internal IDs do not establish the server/snapshot.
-- These queries need a MEME application database, not an RRF-only import.
-- All statements are SELECT or SHOW; no data or server settings are changed.
--
-- Observations reported October 21, 2026:
--   All five CUIs are absent from concepts under every terminology/version.
--   No NCIMTH atom conceptTerminologyIds mappings retain these old CUIs.
--   mrcui still contains DEL: C1551311/202202 and the other four/202308.
-- A DEL release version is NOT a database deletion timestamp. These results
-- do not prove that placeholder concepts were ever created in this database.
-- A manual MRCUI repair never fed back into concept history is another cause.
--
-- Follow-up from the direct CUI log query (39 rows, no LIMIT truncation):
--   All five have "CUI in historySet, skipped" messages in MTH insertions
--   during May 2024, November 2024, May 2025, November 2025, and June 2026.
--   The August 17 and October 21, 2026 messages are release QA reports.
--   No per-CUI deletion message appears in those returned results.
-- AtomLoaderAlgorithm.retrieveHistoryIds() fills historySet directly from
-- component_histories without restricting terminology or requiring a concept
-- association. The skip message prevents assignment of the old project CUI;
-- that branch does not remove a concept. It shows a history row was read by
-- that insertion, not that an NCIMTH concept existed or was deleted then.
-- Query 4 is therefore the next targeted check. Do not restrict h.version to
-- 'latest': history.version can be a release version while its concept uses
-- NCIMTH/latest. Check both terminology and association before inferring scope.
--
-- Further follow-up: all five NCIMTH component_histories rows still exist.
--   C1551311 history 1398225 -> concept 947239 / CL1957200
--   C2107943 history 1583774 -> concept 13169811 / CL1957354
--   C3495370 history 1622250 -> no concepts_component_histories association
--   C3854048 history 2243443 -> concept 13183395 / CL1957355
--   C4723748 history 2393710 -> concept 13193076 / CL1957356
-- This establishes four CUI/owner mismatches and one unattached history row,
-- not five proven concept deletions. A concept ID change or a moved history
-- association can cause a mismatch. The history row's lastModified is not the
-- time its owning concept's CUI changed. Queries 9-10 use known numeric IDs.
--
-- Owner/action follow-up:
--   All four owners are publishable, have two atoms, and were last modified
--   by NCIMTH_202608 on August 12, 2026, between 01:34:27 and 01:34:42.
--   Each has an UPDATE_CONCEPT action changing publishable false -> true on
--   June 22, 2026, between 07:35:17 and 07:45:34, recorded by MTH_2026AA.
--   All four revival actions have activityId:
--     5d7f85e0-3e22-4eab-800a-e3e63d991e00
--   No terminologyId action was returned by the targeted audit query.
-- Revival is therefore established. Release-time CUI reassignment is a code-
-- supported hypothesis, not a recorded field-change event: ASSIGNRELEASEIDS
-- disables molecular-action auditing and assigns a new ID to a publishable
-- concept that did not win an atom-based prior-CUI assignment. MatrixInitializer
-- can revive concepts with publishable atoms through UPDATE_CONCEPT actions.
-- Queries 11-12 identify the actual runs before attributing either operation.
-- The unattached C3495370 history still requires a separate owner investigation.
-- Query results identify revival as MATRIXINIT execution 16456727 in process
-- 16456327 (MTH_2026AA), June 22, 2026, 07:31:59-07:46:10. The August owner
-- modification times overlap ASSIGNRELEASEIDS execution 16459583 in process
-- 16459527 (Pre-Production Process / 202608), August 11 22:52:07 through
-- August 12 14:15:51. This corroborates release-time reassignment; it is not a
-- per-field CUI-change audit. MATRIXINIT revival itself is directly audited.
-- Query 13 finds C3495370 in mapping keys MTH2025AB and MTH2026AA for atom
-- 10525063, MTH/2026AA, "facial nerve repair (treatment)", publishable=false.
-- The atom currently belongs to NCIMTH concept 205952 / C0394300, which is
-- publishable. An incoming MTH CUI mapping is not a former NCIMTH history
-- association and does not prove a C3495370 -> C0394300 merge. Query 14 checks
-- recorded atom moves and actions on the unattached history by numeric ID.
-- Query 14 returns no rows for atom concept/publishable/id actions or actions
-- on history 1622250. This does not exclude parent-collection changes: those
-- are stored with the parent concept's objectId and the child ID in oldValue
-- or newValue. Query 15 performs the reverse lookup. Without a retained match,
-- an older snapshot, legacy audit rows, or retained binary logs may be needed
-- to identify the missing owner; absence alone cannot establish a deletion.
--
-- Relevant code paths in the current checkout:
-- * ReloadConceptHistoryAlgorithm (RELOADHISTORY) clears history and removes
--   atomless NCIMTH concepts whose CUIs are absent from its input MRCUI.RRF.
--   It disables molecular-action recording and logs aggregate counts, not
--   each deleted CUI. A matching run/count is a lead, not per-CUI proof.
-- * AdHocAlgorithm also disables molecular-action recording. Its duplicate
--   repair logs terminologyId groups, conceptToDelete, and concept deleted.
-- * ProdMidCleanupAlgorithm disables molecular-action recording and removes
--   NCIMTH conceptTerminologyIds from unpublishable atoms. This can explain
--   lost atom mappings; by itself that message does not prove concept deletion.
-- * Audited concept removal uses atomic_actions field='id', oldValue=id,
--   newValue=NULL. Those records contain internal numeric IDs, not CUIs.
-- * ConceptJpa currently has @Audited disabled. Existing legacy audit tables,
--   if any, must be discovered rather than assumed.
-- Compare the application revision deployed at the time of a candidate run.
--
-- Start with queries 1-3. They intentionally search retained history across
-- release versions, because the disappearance may predate the 202608 cycle.
-- Queries 1 and 3 inspect log message text and can scan a large log table.
-- Their LIMITs cap returned rows, not the amount scanned. If a limit is reached,
-- investigate the returned activity IDs, then examine earlier time windows.
-- Empty results mean no matching retained evidence, not proof of no deletion.

-- 1. Direct log references: includes unscoped administrative entries.
-- Look for conceptToDelete, concept deleted, merge, and duplicate-repair
-- messages as well as QA reports. Preserve any numeric concept IDs found.
SELECT id AS logId, timestamp, lastModifiedBy, projectId, objectId,
       activityId, workId, message
FROM log_entries
WHERE (projectId = 39751 OR projectId IS NULL)
  AND message REGEXP 'C1551311|C2107943|C3495370|C3854048|C4723748'
ORDER BY timestamp DESC, id DESC
LIMIT 500;

-- 2. Candidate process runs, including unsuccessful and partially committed
-- runs. startDate/finishDate bracket the run, not an individual deletion.
-- A deleted process execution or a standalone admin invocation may be absent.
SELECT p.id AS processExecutionId, p.name AS processName,
       p.terminology AS processTerminology, p.version AS processVersion,
       p.inputPath, p.logPath,
       a.id AS algorithmExecutionId, a.algorithmKey, a.activityId,
       a.startDate, a.finishDate, a.failDate,
       a.lastModifiedBy AS recordedUser
FROM process_executions p
JOIN algorithm_execs a ON a.process_id = p.id
WHERE p.project_id = 39751
  AND a.algorithmKey IN (
    'RELOADHISTORY', 'RELOADMRCUITABLE', 'FEEDBACKRELEASE',
    'PRODMIDCLEANUP', 'ADHOC', 'RRFHISTORY',
    'CONSOLIDATEBYCUI', 'TRUNCATETABLES'
  )
ORDER BY a.startDate DESC, a.id DESC;

-- 3. History-reload and cleanup summaries, correlated with process metadata
-- when an execution with that activityId survives. Aggregate removal counts
-- do not identify which concepts were removed. Counts of zero are informative.
-- Logs from standalone admin invocations can have no matching execution.
SELECT l.id AS logId, l.timestamp, l.lastModifiedBy,
       l.activityId, l.workId,
       a.id AS algorithmExecutionId, a.algorithmKey,
       p.id AS processExecutionId, p.name AS processName,
       p.version AS processVersion, p.inputPath,
       l.message
FROM log_entries l
LEFT JOIN algorithm_execs a ON a.activityId = l.activityId
LEFT JOIN process_executions p ON p.id = a.process_id
WHERE (l.projectId = 39751 OR l.projectId IS NULL)
  AND (
    l.message LIKE '%Concepts removed:%'
    OR l.message LIKE '%placeholder concepts created%'
    OR l.message LIKE '%component histories created%'
    OR l.message LIKE '%component histories deleted%'
    OR l.message LIKE '%dbConceptsWithHistories not in MRCUI:%'
    OR l.message LIKE '%[FixDuplicateConcepts]%'
    OR l.message LIKE '%conceptToDelete:%'
    OR l.message LIKE '%concept deleted:%'
    OR l.message LIKE '%Removed conceptTerminologyId where Key=%'
  )
ORDER BY l.timestamp DESC, l.id DESC
LIMIT 500;

-- 4. Surviving component histories, even without an attached matching concept.
-- A history may be orphaned or attached to a concept whose CUI changed.
-- lastModified is a surviving row's modification time, not a deletion time.
SELECT h.id AS historyId, h.terminologyId AS historyCui,
       h.terminology, h.version, h.associatedRelease,
       h.relationshipType, h.referencedTerminologyId,
       h.timestamp AS historyTimestamp, h.lastModified, h.lastModifiedBy,
       j.concepts_id AS attachedConceptId,
       c.terminologyId AS attachedConceptCui,
       c.terminology AS attachedConceptTerminology,
       c.version AS attachedConceptVersion,
       c.publishable + 0 AS attachedConceptPublishable
FROM component_histories h
LEFT JOIN concepts_component_histories j ON j.componentHistories_id = h.id
LEFT JOIN concepts c ON c.id = j.concepts_id
WHERE h.terminologyId IN (
  'C1551311', 'C2107943', 'C3495370', 'C3854048', 'C4723748'
)
ORDER BY h.terminologyId, h.timestamp, h.id;

-- 5. Recover candidate internal concept IDs from recorded CUI changes, then
-- show all retained concept actions for those IDs, including audited deletion.
-- Some concept creations only record the numeric ID and never record a CUI
-- change, so this query cannot find every historical concept. Empty output
-- does not exclude RELOADHISTORY/ADHOC cleanup, which disables these actions.
-- undoneFlag identifies an undone molecular action; inspect later undo/redo
-- events before interpreting an action as the final state.
WITH candidate_ids AS (
  SELECT DISTINCT a.objectId
  FROM atomic_actions a
  JOIN molecular_actions m ON m.id = a.molecularAction_id
  WHERE a.idType = 'CONCEPT' AND a.field = 'terminologyId'
    AND m.terminology = 'NCIMTH'
    AND (
      a.oldValue IN (
        'C1551311', 'C2107943', 'C3495370', 'C3854048', 'C4723748'
      )
      OR a.newValue IN (
        'C1551311', 'C2107943', 'C3495370', 'C3854048', 'C4723748'
      )
    )
)
SELECT a.objectId AS conceptId, a.id AS atomicActionId,
       a.field, a.oldValue, a.newValue,
       CASE WHEN a.field = 'id' AND a.oldValue IS NOT NULL
                  AND a.newValue IS NULL THEN 1 ELSE 0 END AS deletionAction,
       m.id AS molecularActionId, m.name AS actionName,
       m.timestamp AS actionTimestamp, m.lastModified, m.lastModifiedBy,
       m.undoneFlag + 0 AS undone, m.activityId, m.workId
FROM candidate_ids c
JOIN atomic_actions a ON a.objectId = c.objectId AND a.idType = 'CONCEPT'
JOIN molecular_actions m ON m.id = a.molecularAction_id
WHERE m.terminology = 'NCIMTH'
ORDER BY a.objectId, m.timestamp, a.id;

-- 6. Drill into a candidate run from 1-3. Replace the placeholder with an
-- actual activityId. This returns all lines, including the underlying input
-- path and any individual-concept diagnostics the implementation logged.
SELECT id AS logId, timestamp, lastModifiedBy, projectId,
       objectId, activityId, workId, message
FROM log_entries
WHERE activityId = 'PASTE_ACTIVITY_ID_HERE'
ORDER BY timestamp, id;

-- 7. Check for retained legacy audit tables before attempting audit SQL.
-- revinfo alone is not concept-deletion evidence. If an audit table exists,
-- inspect its actual columns and follow its schema before querying revisions.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND LOWER(table_name) IN ('concepts_aud', 'component_histories_aud', 'revinfo')
ORDER BY table_name;

-- 8. Optional: determine whether binary logs are enabled and which files are
-- retained. SHOW BINARY LOGS requires the relevant database privilege.
-- Current configuration does not prove logging was enabled at the time.
-- Expired logs cannot establish who/when; a retained earlier snapshot can
-- establish whether a concept existed and provide its internal numeric ID.
SHOW VARIABLES
WHERE Variable_name IN ('log_bin', 'binlog_format', 'binlog_expire_logs_seconds');

SHOW BINARY LOGS;

-- 9. Inspect the existing owners by primary key. Atom/history counts and names
-- distinguish empty historical placeholders from active/reused concepts.
-- timestamp and lastModified are stored row times, not per-field audit times.
SELECT c.id AS conceptId, c.terminologyId AS currentCui,
       c.terminology, c.version, c.branch, c.name,
       c.publishable + 0 AS publishable,
       c.published + 0 AS published,
       c.obsolete + 0 AS obsolete,
       c.timestamp AS storedTimestamp, c.lastModified, c.lastModifiedBy,
       (SELECT COUNT(*) FROM concepts_atoms j
        WHERE j.concepts_id = c.id) AS atomCount,
       (SELECT COUNT(*) FROM concepts_component_histories j
        WHERE j.concepts_id = c.id) AS historyCount
FROM concepts c
WHERE c.id IN (947239, 13169811, 13183395, 13193076)
ORDER BY c.id;

-- 10. Direct concept action lookup using the recovered internal IDs.
-- A terminologyId oldValue/newValue pair can establish CUI reassignment.
-- Collection changes can establish movement of a component history instead.
-- Empty output does not exclude an unaudited algorithm or direct SQL change.
SELECT a.objectId AS conceptId, a.field, a.collectionClassName,
       a.oldValue, a.newValue,
       m.id AS molecularActionId, m.name AS actionName,
       m.timestamp AS actionTimestamp, m.lastModifiedBy,
       m.undoneFlag + 0 AS undone, m.activityId, m.workId
FROM atomic_actions a
JOIN molecular_actions m ON m.id = a.molecularAction_id
WHERE a.idType = 'CONCEPT'
  AND a.objectId IN (947239, 13169811, 13183395, 13193076)
  AND (
    a.field IN ('id', 'terminologyId', 'publishable', 'componentHistory')
    OR a.collectionClassName LIKE
       'com.wci.umls.server.jpa.model.content.ComponentHistoryJpa%'
  )
ORDER BY a.objectId, m.timestamp, a.id;

-- 11. Identify the algorithm and process behind the four June revival actions.
-- Children can share a parent's activityId; correlate the actual algorithm
-- with its source implementation and logs rather than relying on username.
SELECT p.id AS processExecutionId, p.name AS processName,
       p.terminology AS processTerminology, p.version AS processVersion,
       a.id AS algorithmExecutionId, a.algorithmKey, a.activityId,
       a.startDate, a.finishDate, a.failDate, p.inputPath
FROM algorithm_execs a
JOIN process_executions p ON p.id = a.process_id
WHERE a.activityId = '5d7f85e0-3e22-4eab-800a-e3e63d991e00'
ORDER BY a.startDate, a.id;

-- 12. Identify release-ID assignment runs overlapping the owners' August
-- modification times. Failed/incomplete runs may still have committed changes.
-- An overlap supports attribution; it is not a per-concept CUI-change audit.
SELECT p.id AS processExecutionId, p.name AS processName,
       p.version AS processVersion,
       a.id AS algorithmExecutionId, a.algorithmKey, a.activityId,
       a.startDate, a.finishDate, a.failDate,
       a.lastModifiedBy AS recordedUser
FROM algorithm_execs a
JOIN process_executions p ON p.id = a.process_id
WHERE p.project_id = 39751
  AND a.algorithmKey = 'ASSIGNRELEASEIDS'
  AND a.startDate <= '2026-08-12 01:34:42'
  AND (COALESCE(a.finishDate, a.failDate) IS NULL
       OR COALESCE(a.finishDate, a.failDate) >= '2026-08-12 01:34:27')
ORDER BY a.startDate, a.id;

-- 13. Trace the unattached C3495370 history through incoming/source CUI maps.
-- Earlier checks restricted mappings to key NCIMTH. AtomLoaderAlgorithm also
-- stores an incoming CUI under processTerminology+processVersion (for example
-- MTH2026AA) BEFORE checking historySet. Those mappings may locate surviving
-- atoms and their current project concepts even though no NCIMTH mapping
-- retains this old CUI. They identify candidates, not a former history owner.
WITH project_memberships AS (
  SELECT j.atoms_id, c.id AS conceptId, c.terminologyId AS currentCui,
         c.publishable AS conceptPublishable
  FROM concepts_atoms j
  JOIN concepts c ON c.id = j.concepts_id
  WHERE c.terminology = 'NCIMTH' AND c.version = 'latest'
)
SELECT m.conceptTerminologyIds_KEY AS mappingKey,
       a.id AS atomId, a.terminology AS source, a.version AS sourceVersion,
       a.termType, a.codeId, a.name, a.publishable + 0 AS atomPublishable,
       p.conceptId, p.currentCui, p.conceptPublishable + 0 AS conceptPublishable
FROM atomjpa_conceptterminologyids m
JOIN atoms a ON a.id = m.AtomJpa_id
LEFT JOIN project_memberships p ON p.atoms_id = a.id
WHERE m.conceptTerminologyIds = 'C3495370'
ORDER BY m.conceptTerminologyIds_KEY, a.id, p.conceptId;

-- 14. Targeted action lookup for the surviving atom and unattached history.
-- For ATOM/field='concept', oldValue and newValue are numeric from/to concept
-- IDs. Molecular-action componentId/componentId2 provide additional context.
-- A move may lead to an earlier owner to investigate; the MTH CUI map alone
-- does not establish that this owner ever used C3495370 as its NCIMTH CUI.
-- History creation/update actions may identify an original owner, but bulk
-- RELOADHISTORY disables actions. Empty output is not proof of no movement.
SELECT a.id AS atomicActionId, a.idType, a.objectId, a.field,
       a.oldValue, a.newValue,
       m.id AS molecularActionId, m.name AS actionName,
       m.componentId, m.componentId2, m.terminology, m.version,
       m.timestamp AS actionTimestamp, m.lastModifiedBy,
       m.undoneFlag + 0 AS undone, m.activityId, m.workId
FROM atomic_actions a
JOIN molecular_actions m ON m.id = a.molecularAction_id
WHERE (
    a.idType = 'ATOM' AND a.objectId = 10525063
    AND a.field IN ('concept', 'publishable', 'id')
  ) OR (
    a.idType = 'COMPONENT_HISTORY' AND a.objectId = 1622250
  )
ORDER BY m.timestamp, a.id;

-- 15. Reverse lookup of parent concept collection changes for the orphaned
-- history and surviving atom, plus recorded changes of the original CUI.
-- Unlike objectId lookups, this searches
-- oldValue/newValue and may scan a large action table. Exact child IDs and
-- collection types avoid confusing unrelated objects with the same number.
-- For a history collection row, a.objectId is a candidate former/added owner.
-- For an atom collection row, establish the owner's historical CUI separately.
-- Inspect undoneFlag and later actions before interpreting final ownership.
SELECT a.objectId AS parentConceptId, a.field, a.collectionClassName,
       a.oldValue, a.newValue,
       m.id AS molecularActionId, m.name AS actionName,
       m.componentId, m.componentId2, m.terminology, m.version,
       m.timestamp AS actionTimestamp, m.lastModifiedBy,
       m.undoneFlag + 0 AS undone, m.activityId, m.workId
FROM atomic_actions a
JOIN molecular_actions m ON m.id = a.molecularAction_id
WHERE a.idType = 'CONCEPT'
  AND (
    (
      (a.field IN ('componentHistory', 'componentHistories')
       OR a.collectionClassName LIKE
          'com.wci.umls.server.jpa.model.content.ComponentHistoryJpa%')
      AND (a.oldValue = '1622250' OR a.newValue = '1622250')
    ) OR (
      (a.field = 'atoms'
       OR a.collectionClassName LIKE
          'com.wci.umls.server.jpa.model.content.AtomJpa%')
      AND (a.oldValue = '10525063' OR a.newValue = '10525063')
    ) OR (
      a.field = 'terminologyId'
      AND (a.oldValue = 'C3495370' OR a.newValue = 'C3495370')
    )
  )
ORDER BY m.timestamp, a.id;
