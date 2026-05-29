# NM-306 Integration Test Inventory

## Purpose

Inventory of the current MEME integration test surface after the NM-306
modernization pass. This document classifies the existing `*IT.java` classes by
the environment they require and records the current smoke-baseline status from
the locally verified profile commands in
`docs/database-load-and-test-instructions.md`.

## Counts

Current `*IT.java` count: 111.

| Profile | Count | Primary prerequisites |
| --- | ---: | --- |
| `flyway` | 1 | disposable empty MySQL schemas |
| `sample-jpa` | 40 | sample DB loaded from `SAMPLE_UMLS`, indexes, Tomcat stopped |
| `nci-meta-jpa` | 22 | NCI-META sample DB loaded from `SAMPLE_NCI`, indexes, Tomcat stopped |
| `insertion` | 15 | insertion database, insertion source data, indexes, Tomcat stopped |
| `admin-loader` | 2 active, 4 ignored | disposable loader DBs/source data; mutates/loads/unloads data |
| `rest-sample` | 23 | running app using same env, sample or compatible DB, `BASE_URL` |
| `rest-nci-meta-disabled` | 4 | running app with NCI-META DB; legacy classes currently disabled or base-only |

Known ignored REST files from a first static scan:

- `com.wci.umls.server.test.rest.SecurityServiceRestIT`
- `com.wci.umls.server.test.rest.meta.MetadataServiceRestIT`
- `com.wci.umls.server.test.rest.meta.ProcessServiceRestIT`
- `com.wci.umls.server.test.rest.meta.ProjectServiceRestIT`
- `com.wci.umls.server.test.rest.ncimeta.MetaEditingServiceRestDegenerateUseIT`
- `com.wci.umls.server.test.rest.ncimeta.MetaEditingServiceRestEdgeCasesIT`
- `com.wci.umls.server.test.rest.ncimeta.MetaEditingServiceRestNormalUseIT`

`ContentServiceRestNormalUseIT` also contains an `@Ignore` marker, but it needs a
method-level review before treating the whole class as disabled.

## Profiles

### Flyway

| Class | Dataset | Server | Mutates DB | Status |
| --- | --- | --- | --- | --- |
| `com.wci.umls.server.test.jpa.FlywayMigrationIT` | disposable empty schemas | no | yes | passed 2026-05-29 |

Notes:

- Already guarded by `-Dflyway.it.enabled=true`.
- Requires empty target schemas and explicit JDBC URLs.
- Passed locally with `make integration-flyway` using
  `ncimdb_nm306_flyway_it` and `ncimdb_nm306_flyway_base_it`.
- Those schemas are no longer empty after the successful smoke run; recreate or
  choose fresh disposable schemas before rerunning the same profile.
- Preferred repeatable command is now `make integration-flyway-ephemeral`,
  which creates and drops generated disposable schemas for each run.
- The ephemeral command passed locally on 2026-05-29 and dropped its generated
  schemas afterward.

### Sample JPA

| Class | Dataset | Server | Mutates DB | Status |
| --- | --- | --- | --- | --- |
| `com.wci.umls.server.test.jpa.AddDemotionIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.ComponentStatsIT` | sample | no | likely | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.ContentDeepRelsIT` | sample | no | likely | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.ContentServiceAutocompleteIT` | sample | no | likely | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.ContentServiceFindRelationshipsIT` | sample | no | likely | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.ProjectJpaIT` | sample | no | likely | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.UpdateConceptStatusIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.FailOnceAlgorithmIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.MatrixInitializerAlgorithmIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.QueryActionAlgorithmIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.SemanticTypeResolverAlgorithmIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.WaitAlgorithmIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.DT_I2IT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.DT_I3BIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.DT_I3IT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.DT_M1IT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.DT_PN2IT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_A4IT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_BIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_CIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_E2IT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_EIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_FIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_GIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_H1IT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_H2IT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_IIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_MIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_NCIPNIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_SCUIIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.integrity.MGV_SDUIIT` | sample | no | no | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.search.AtomRelationshipSearchIntegrationIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.search.AtomSearchIntegrationIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.search.ChecklistSearchIntegrationIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.search.ConceptRelationshipSearchIntegrationIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.search.ConceptSearchIntegrationIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.search.ProjectSearchIntegrationIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.search.TrackingRecordSearchIntegrationIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.search.UserSearchIntegrationIT` | sample | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.jpa.search.WorklistSearchIntegrationIT` | sample | no | yes | passed 2026-05-29 |

Notes:

- The sample command block in `docs/database-load-and-test-instructions.md`
  selects this profile.
- Use `make prepare-sample` to create, load, reindex, and generate the
  `ncimdbmeta` sample integration fixture before capturing a fresh baseline.
- The search tests create indexed entities and are likely sensitive to index
  directory reuse.
- An accidental run against the default `ncimdb` schema on 2026-05-28 completed
  163 tests with 43 failures. Treat that as a wrong-environment result, not the
  expected sample baseline.
- The sample preflight now expects the documented `ncimdbmeta` schema by default
  and checks for core MTH/latest fixture rows before the full profile runs.
- After refreshing `ncimdbmeta`, `make integration-sample` passed locally on
  2026-05-29 with all 163 tests green.

### NCI-META JPA

| Class | Dataset | Server | Mutates DB | Status |
| --- | --- | --- | --- | --- |
| `com.wci.umls.server.test.algo.ComputePreferredNamesAlgorithmIT` | NCI-META | no | yes | passed 2026-05-28 |
| `com.wci.umls.server.test.helpers.SearchHandlerIT` | NCI-META | no | likely | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.CloseReopenFactoryIT` | NCI-META | no | no | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.ComputePreferredNameHandlerIT` | NCI-META | no | likely | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.ConfigUtilityIT` | NCI-META | no | no | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.ContentServiceGeneralQueryTimeoutIT` | NCI-META | no | no | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.ContentServiceTreePositionFromTreeIT` | NCI-META | no | no | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.DefaultValidationCheckIT` | NCI-META | no | likely | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.GraphResolutionHandlerIT` | NCI-META | no | likely | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.IdentifierAssignmentHandlerIT` | NCI-META | no | yes | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.MappingIT` | NCI-META | no | likely | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.ProgressEventIT` | NCI-META | no | yes | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.ReportHelperIT` | NCI-META | no | likely | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.SemanticCategorySearchIT` | NCI-META | no | no | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.UmlsIdentifierAssignmentHandlerIT` | NCI-META | no | yes | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.algorithm.AddRemoveIntegrityCheckAlgorithmIT` | NCI-META | no | yes | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.algorithm.ComponentInfoRelRemapperAlgorithmIT` | NCI-META | no | yes | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.algorithm.LexicalClassAssignmentAlgorithmIT` | NCI-META | no | yes | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.algorithm.MapSetLoaderAlgorithmIT` | NCI-META | no | yes | passed 2026-05-28 |
| `com.wci.umls.server.test.jpa.algorithm.SubsetLoaderAlgorithmIT` | NCI-META | no | yes | passed 2026-05-28 |

Notes:

- The NCI-META command block in `docs/database-load-and-test-instructions.md`
  selects this profile.
- Use `make prepare-ncimeta` to create, load, generate, and reindex the
  `ncimdbncimeta` NCI-META integration fixture before capturing a fresh
  baseline.
- `make prepare-ncimeta`, `make preflight-ncimeta`, and
  `make integration-ncimeta` passed locally on 2026-05-28. The JPA profile
  emitted 20 runnable suites and 37 tests.
- `PfsParameterForComponentTestSupport` and
  `PfsParameterForConceptTestSupport` remain helper classes in the test tree.
  They were renamed away from the `*IT` convention and are no longer selected
  by the NCI-META integration profile because they have no runnable `@Test`
  methods.
- Several tests exercise algorithm execution or identifier assignment and should
  be run against disposable or resettable NCI-META data.

### Insertion

| Class | Dataset | Server | Mutates DB | Status |
| --- | --- | --- | --- | --- |
| `com.wci.umls.server.test.jpa.algorithm.AtomLoaderAlgorithmIT` | insertion | no | yes | passed via `InsertionLoaderAlgorithmsIT` on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.AttributeLoaderAlgorithmIT` | insertion | no | yes | passed via `InsertionLoaderAlgorithmsIT` on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.BequeathAlgorithmIT` | insertion | no | yes | passed on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.ContextLoaderAlgorithmIT` | insertion | no | yes | passed via `InsertionLoaderAlgorithmsIT` on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.GeneratedMergeAlgorithmIT` | insertion | no | yes | passed on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.InsertionLoaderAlgorithmsIT` | insertion | no | yes | passed on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.MetadataLoaderAlgorithmIT` | insertion | no | yes | passed via `InsertionLoaderAlgorithmsIT` on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.PreInsertionAlgorithmIT` | insertion | no | yes | passed on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.PrecomputedMergeAlgorithmIT` | insertion | no | yes | passed on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.ProdMidCleanupAlgorithmIT` | insertion | no | yes | passed on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.RelationshipLoaderAlgorithmIT` | insertion | no | yes | passed via `InsertionLoaderAlgorithmsIT` on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.SafeReplaceAlgorithmIT` | insertion | no | yes | passed on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.SemanticTypeLoaderAlgorithmIT` | insertion | no | yes | passed via `InsertionLoaderAlgorithmsIT` on 2026-05-29 |
| `com.wci.umls.server.test.jpa.algorithm.UpdatePublishedAlgorithmIT` | insertion | no | yes | dormant; `@Test` remains commented out |
| `com.wci.umls.server.test.jpa.algorithm.UpdateReleasibilityAlgorithmIT` | insertion | no | yes | passed on 2026-05-29 |

Notes:

- The insertion command block in `docs/database-load-and-test-instructions.md`
  selects this profile.
- Use `make prepare-insertion` to create, load, generate, and reindex the
  `ncimdbinsert` insertion integration fixture before running the profile.
- `make preflight-insertion` and `make integration-insertion` passed locally on
  2026-05-29 against a freshly prepared `ncimdbinsert` fixture. The profile ran
  13 runnable tests.
- `InsertionLoaderAlgorithmsIT` invokes several loader IT classes directly.
- `UpdatePublishedAlgorithmIT` is not in the current runnable baseline because
  its JUnit `@Test` annotation is still commented out.
- This profile should not run against shared sample or NCI-META databases.

### Admin Loader

| Class | Dataset | Server | Mutates DB | Status |
| --- | --- | --- | --- | --- |
| `com.wci.umls.server.test.admin.ClaMLLoadAndUnloadIT` | loader source data | no | yes | ignored; ClaML fixture exceeded local smoke memory/runtime budget |
| `com.wci.umls.server.test.admin.CompareRf2FullRf2SnapshotLoadersIT` | loader source data | no | yes | ignored; RF2 full-vs-snapshot comparison exceeded local smoke runtime budget |
| `com.wci.umls.server.test.admin.OwlLoadAndUnloadIT` | loader source data | no | yes | ignored; OWL fixture deferred until runtime is bounded |
| `com.wci.umls.server.test.admin.Rf2SnapshotLoadAndUnloadIT` | loader source data | no | yes | ignored; RF2 snapshot fixture exceeded local smoke runtime budget |
| `com.wci.umls.server.test.admin.RrfSingleLoadAndUnloadIT` | loader source data | no | yes | passed 2026-05-29 |
| `com.wci.umls.server.test.admin.RrfUmlsLoadAndUnloadIT` | loader source data | no | yes | passed primary load/unload path 2026-05-29 |

Notes:

- These are the highest-risk tests from a database-safety perspective.
- `make prepare-admin`, `make preflight-admin`, and `make integration-admin`
  now require the disposable `ncimdbadminload` schema by default.
- The active smoke profile is RRF-only. The deferred RF2/OWL/ClaML classes have
  Gradle-era test bodies but remain ignored until their runtime/memory behavior
  is suitable for a smoke baseline.
- `RrfUmlsLoadAndUnloadIT` verifies the primary MTH unload path. Removing
  secondary source `SNOMEDCT_US/2014_09_01` after MTH removal currently exposes
  a `root_terminologies` foreign-key constraint failure and remains follow-up
  work.

### REST Sample

| Class | Dataset | Server | Mutates DB | Status |
| --- | --- | --- | --- | --- |
| `com.wci.umls.server.test.rest.SecurityServiceRestDegenerateUseIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.SecurityServiceRestEdgeCasesIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.SecurityServiceRestIT` | sample-compatible | yes | no | ignored/base |
| `com.wci.umls.server.test.rest.SecurityServiceRestNormalUseIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.SecurityServiceRestRoleCheckIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.WorkflowServiceRestIT` | sample-compatible | yes | likely | base/unverified |
| `com.wci.umls.server.test.rest.WorkflowServiceRestNormalUseIT` | sample-compatible | yes | yes | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.meta.ContentServiceRestDegenerateUseIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.meta.ContentServiceRestEdgeCasesIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.meta.ContentServiceRestIT` | sample-compatible | yes | no | base/unverified |
| `com.wci.umls.server.test.rest.meta.ContentServiceRestNormalUseIT` | sample-compatible | yes | likely | passed 2026-05-28; one explicit skip |
| `com.wci.umls.server.test.rest.meta.MetadataServiceRestDegenerateUseIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.meta.MetadataServiceRestIT` | sample-compatible | yes | no | ignored/base |
| `com.wci.umls.server.test.rest.meta.MetadataServiceRestNormalUseIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.meta.ProcessServiceRestIT` | sample-compatible | yes | yes | ignored/base |
| `com.wci.umls.server.test.rest.meta.ProcessServiceRestNormalUseIT` | sample-compatible | yes | yes | passed 2026-05-28; SMTP test skipped |
| `com.wci.umls.server.test.rest.meta.ProjectServiceRestDegenerateUseIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.meta.ProjectServiceRestEdgeCasesIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.meta.ProjectServiceRestIT` | sample-compatible | yes | likely | ignored/base |
| `com.wci.umls.server.test.rest.meta.ProjectServiceRestNormalUseIT` | sample-compatible | yes | likely | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.meta.ProjectServiceRestRoleCheckIT` | sample-compatible | yes | no | passed 2026-05-28 |
| `com.wci.umls.server.test.rest.meta.ReportServiceRestIT` | sample-compatible | yes | likely | base/unverified |
| `com.wci.umls.server.test.rest.meta.ReportServiceRestNormalUseIT` | sample-compatible | yes | likely | passed 2026-05-28 |

Notes:

- REST tests require the app to be running from the same sourced environment.
- REST preflight expects the sample fixture schema `ncimdbmeta`, not the
  default `ncimdb`.
- REST preflight checks `BASE_URL`, test credentials, local sample fixture data,
  and a server-side MTH/latest sample concept probe so wrong-server/wrong-DB
  failures are clear before JUnit runs.
- `./gradlew restIntegrationTest` passed locally on 2026-05-28 against the app
  running at `http://localhost:18080/umls-server-rest` with `DB_NAME=ncimdbmeta`.
  The run selected 111 tests with 0 failures and 2 explicit skips.
- The explicit skips are `ContentServiceRestNormalUseIT.testGetDeepRelationships`
  because deep relationships are calculated weekly rather than loaded, and
  `ProcessServiceRestNormalUseIT.testFailOnceAndEmailProcess` because it
  requires a reachable SMTP server.

### REST NCI-META Disabled

| Class | Dataset | Server | Mutates DB | Status |
| --- | --- | --- | --- | --- |
| `com.wci.umls.server.test.rest.ncimeta.MetaEditingServiceRestDegenerateUseIT` | NCI-META | yes | yes | ignored |
| `com.wci.umls.server.test.rest.ncimeta.MetaEditingServiceRestEdgeCasesIT` | NCI-META | yes | yes | ignored |
| `com.wci.umls.server.test.rest.ncimeta.MetaEditingServiceRestIT` | NCI-META | yes | yes | base/unverified |
| `com.wci.umls.server.test.rest.ncimeta.MetaEditingServiceRestNormalUseIT` | NCI-META | yes | yes | ignored |

Notes:

- `docs/database-load-and-test-instructions.md` already notes the NCI-META REST
  editing classes are currently skipped.
- The ignored editing subclasses now share base teardown helpers for copied
  concept cleanup and guarded logout, but their behavior is still unverified
  because they remain outside the runnable REST smoke profile.
- The ignore annotations now document that these classes need a separate
  NCI-META REST editing fixture/profile decision.
- NM-306 should decide whether to repair, split, or keep these explicitly
  ignored.

## Remaining Follow-Up

- Decide whether `UpdatePublishedAlgorithmIT` should be restored to the
  insertion profile or kept dormant with a stronger source-level reason.
- Keep the NCI-META REST editing classes explicitly ignored until there is a
  separate fixture/profile decision for REST editing workflows.
- Split the RF2, OWL, and ClaML admin-loader tests into heavier/manual profiles
  before trying to make them part of an automated smoke baseline.
- Investigate the secondary `SNOMEDCT_US/2014_09_01` unload path in
  `RrfUmlsLoadAndUnloadIT`; it currently exposes a `root_terminologies`
  foreign-key constraint issue after the primary MTH unload succeeds.
- Treat the known-good command sequence in
  `docs/database-load-and-test-instructions.md` as the NM-306 local baseline
  before resuming behavior-sensitive NM-304 SpotBugs cleanup.
