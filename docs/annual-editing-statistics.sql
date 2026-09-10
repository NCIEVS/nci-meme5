-- Annual editing statistics queries.
--
-- Update these dates for the reporting window. The end date is exclusive.
-- Example below covers 2025-10-01 through 2026-09-30.
SET @report_start = '2025-10-01';
SET @report_end = '2026-10-01';
SET @mth_terminology = 'MTH';
SET @mth_version_1 = '2026AA';
SET @mth_version_2 = '2025AB';

-- Programmatic actions completed during insertion processes, and distinct
-- concepts touched by those actions.
SELECT
  COUNT(DISTINCT ma.id) AS programmatic_actions_completed,
  COUNT(DISTINCT ma.componentId) AS concepts_programmatically_modified
FROM molecular_actions ma
JOIN algorithm_execs ae
  ON ae.activityId = ma.activityId
JOIN process_executions pe
  ON pe.id = ae.process_id
WHERE ma.undoneFlag = b'0'
  AND ma.lastModified >= @report_start
  AND ma.lastModified < @report_end
  AND ae.finishDate IS NOT NULL
  AND ae.failDate IS NULL
  AND pe.finishDate IS NOT NULL
  AND pe.failDate IS NULL
  AND pe.type = 'Insertion';

-- Concepts approved and approval actions grouped by responsible user.
-- E- and S- prefixes are removed so editor and stamping authorities roll up
-- under the same initials.
SELECT
  CASE
    WHEN ma.lastModifiedBy LIKE 'E-%' OR ma.lastModifiedBy LIKE 'S-%'
      THEN SUBSTRING(ma.lastModifiedBy, 3)
    ELSE ma.lastModifiedBy
  END AS responsible_user,
  COUNT(DISTINCT ma.componentId) AS concepts_approved,
  COUNT(*) AS approval_actions
FROM molecular_actions ma
WHERE ma.name = 'APPROVE'
  AND ma.undoneFlag = b'0'
  AND ma.lastModified >= @report_start
  AND ma.lastModified < @report_end
GROUP BY responsible_user
ORDER BY concepts_approved DESC, responsible_user;

-- Current versioned terminology rows included in source/release counts.
SELECT
  rt.family,
  rt.terminology AS rsab,
  t.terminology,
  t.version,
  t.preferredName
FROM terminologies t
JOIN root_terminologies rt
  ON rt.id = t.rootTerminology_id
WHERE t.current = b'1'
ORDER BY rt.family, rt.terminology, t.version;

-- Count subsources updated during selected MTH insertion metadata-loading runs.
-- This uses completed Insertion process executions whose name/inputPath contains
-- the MTH insertion names. LOW_SOURCE from sources.src is not persisted, so this
-- counts terminology rows touched by METADATALOADING during those runs.
SELECT
  updated.mth_insertion,
  COUNT(DISTINCT updated.rsab) AS subsources_updated,
  COUNT(DISTINCT updated.terminology_id) AS versioned_terminology_rows_touched
FROM (
  SELECT
    CASE
      WHEN pe.terminology = @mth_terminology
        AND pe.version = @mth_version_1
        THEN CONCAT(@mth_terminology, '_', @mth_version_1)
      WHEN pe.terminology = @mth_terminology
        AND pe.version = @mth_version_2
        THEN CONCAT(@mth_terminology, '_', @mth_version_2)
      WHEN pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
          @mth_version_1, '%')
        OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_1, '%')
        THEN CONCAT(@mth_terminology, '_', @mth_version_1)
      WHEN pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
          @mth_version_2, '%')
        OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_2, '%')
        THEN CONCAT(@mth_terminology, '_', @mth_version_2)
      ELSE '(unknown)'
    END AS mth_insertion,
    t.id AS terminology_id,
    t.terminology AS rsab
  FROM terminologies t
  JOIN algorithm_execs ae
    ON t.lastModified >= ae.startDate
    AND t.lastModified <= ae.finishDate
  JOIN process_executions pe
    ON pe.id = ae.process_id
  WHERE t.lastModified >= @report_start
    AND t.lastModified < @report_end
    AND ae.algorithmKey = 'METADATALOADING'
    AND ae.finishDate IS NOT NULL
    AND ae.failDate IS NULL
    AND pe.type = 'Insertion'
    AND pe.finishDate IS NOT NULL
    AND pe.failDate IS NULL
    AND (
      (
        pe.terminology = @mth_terminology
        AND pe.version IN (@mth_version_1, @mth_version_2)
      )
      OR pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
        @mth_version_1, '%')
      OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_1, '%')
      OR pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
        @mth_version_2, '%')
      OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_2, '%')
    )
    -- Uncomment to exclude the MTH parent source itself from the subsource count.
    -- AND t.terminology <> 'MTH'
) updated
GROUP BY updated.mth_insertion
ORDER BY updated.mth_insertion;

-- List the subsource rows behind the MTH insertion tally above.
SELECT DISTINCT
  detail.mth_insertion,
  detail.rsab,
  detail.subsource_version,
  detail.preferred_name,
  detail.current,
  detail.last_modified
FROM (
  SELECT
    CASE
      WHEN pe.terminology = @mth_terminology
        AND pe.version = @mth_version_1
        THEN CONCAT(@mth_terminology, '_', @mth_version_1)
      WHEN pe.terminology = @mth_terminology
        AND pe.version = @mth_version_2
        THEN CONCAT(@mth_terminology, '_', @mth_version_2)
      WHEN pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
          @mth_version_1, '%')
        OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_1, '%')
        THEN CONCAT(@mth_terminology, '_', @mth_version_1)
      WHEN pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
          @mth_version_2, '%')
        OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_2, '%')
        THEN CONCAT(@mth_terminology, '_', @mth_version_2)
      ELSE '(unknown)'
    END AS mth_insertion,
    t.terminology AS rsab,
    t.version AS subsource_version,
    t.preferredName AS preferred_name,
    t.current AS current,
    t.lastModified AS last_modified
  FROM terminologies t
  JOIN algorithm_execs ae
    ON t.lastModified >= ae.startDate
    AND t.lastModified <= ae.finishDate
  JOIN process_executions pe
    ON pe.id = ae.process_id
  WHERE t.lastModified >= @report_start
    AND t.lastModified < @report_end
    AND ae.algorithmKey = 'METADATALOADING'
    AND ae.finishDate IS NOT NULL
    AND ae.failDate IS NULL
    AND pe.type = 'Insertion'
    AND pe.finishDate IS NOT NULL
    AND pe.failDate IS NULL
    AND (
      (
        pe.terminology = @mth_terminology
        AND pe.version IN (@mth_version_1, @mth_version_2)
      )
      OR pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
        @mth_version_1, '%')
      OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_1, '%')
      OR pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
        @mth_version_2, '%')
      OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_2, '%')
    )
    -- Uncomment to exclude the MTH parent source itself from the subsource list.
    -- AND t.terminology <> 'MTH'
) detail
ORDER BY detail.mth_insertion, detail.rsab, detail.subsource_version;

-- Compare the selected MTH insertion subsource lists side-by-side.
SELECT
  compared.rsab,
  GROUP_CONCAT(DISTINCT CASE
    WHEN compared.mth_insertion = CONCAT(@mth_terminology, '_',
      @mth_version_1)
      THEN compared.subsource_version
    ELSE NULL
  END ORDER BY compared.subsource_version SEPARATOR ', ') AS versions_in_first_mth,
  GROUP_CONCAT(DISTINCT CASE
    WHEN compared.mth_insertion = CONCAT(@mth_terminology, '_',
      @mth_version_2)
      THEN compared.subsource_version
    ELSE NULL
  END ORDER BY compared.subsource_version SEPARATOR ', ') AS versions_in_second_mth,
  COUNT(DISTINCT compared.mth_insertion) AS mth_insertions_touched
FROM (
  SELECT
    CASE
      WHEN pe.terminology = @mth_terminology
        AND pe.version = @mth_version_1
        THEN CONCAT(@mth_terminology, '_', @mth_version_1)
      WHEN pe.terminology = @mth_terminology
        AND pe.version = @mth_version_2
        THEN CONCAT(@mth_terminology, '_', @mth_version_2)
      WHEN pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
          @mth_version_1, '%')
        OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_1, '%')
        THEN CONCAT(@mth_terminology, '_', @mth_version_1)
      WHEN pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
          @mth_version_2, '%')
        OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_2, '%')
        THEN CONCAT(@mth_terminology, '_', @mth_version_2)
      ELSE '(unknown)'
    END AS mth_insertion,
    t.terminology AS rsab,
    t.version AS subsource_version
  FROM terminologies t
  JOIN algorithm_execs ae
    ON t.lastModified >= ae.startDate
    AND t.lastModified <= ae.finishDate
  JOIN process_executions pe
    ON pe.id = ae.process_id
  WHERE t.lastModified >= @report_start
    AND t.lastModified < @report_end
    AND ae.algorithmKey = 'METADATALOADING'
    AND ae.finishDate IS NOT NULL
    AND ae.failDate IS NULL
    AND pe.type = 'Insertion'
    AND pe.finishDate IS NOT NULL
    AND pe.failDate IS NULL
    AND (
      (
        pe.terminology = @mth_terminology
        AND pe.version IN (@mth_version_1, @mth_version_2)
      )
      OR pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
        @mth_version_1, '%')
      OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_1, '%')
      OR pe.inputPath LIKE CONCAT('%', @mth_terminology, '_',
        @mth_version_2, '%')
      OR pe.name LIKE CONCAT('%', @mth_terminology, '_', @mth_version_2, '%')
    )
    -- Uncomment to exclude the MTH parent source itself from this comparison.
    -- AND t.terminology <> 'MTH'
) compared
GROUP BY compared.rsab
ORDER BY compared.rsab;
