# NM-306 Integration Test Restoration and Modernization Plan

## Summary

Restore the MEME integration test suite to a reproducible baseline, then
modernize the test harness enough that behavior-sensitive SpotBugs cleanup can
continue safely.

NM-304 made static analysis strict for new findings and documented the remaining
legacy SpotBugs backlog. The next high-value step is to make integration tests
trustworthy again before changing risky areas such as default encoding, JPA model
encapsulation, constructor lifecycle, dynamic SQL, and framework-populated
fields.

## Current State

The project already has the basic structure needed for integration testing:

- `build.gradle` defines a separate `integrationTest` task for `*IT.java`
  classes.
- `Makefile` has reliable unit-test and quality targets, but no dedicated
  integration-test targets yet.
- `docs/database-load-and-test-instructions.md` documents sample, NCI-META, REST,
  insertion, and Flyway test setup recipes.
- `config/local/setenv.sh` provides the local database, filesystem, and REST
  environment model.
- `src/test/java` currently contains 111 `*IT.java` classes.
- `FlywayMigrationIT` is already opt-in and requires disposable schemas before it
  mutates a database.

The existing operational runbook is
`docs/database-load-and-test-instructions.md`. NM-306 should treat that file as
the source of truth for concrete database load and test commands. This plan
should guide the cleanup and modernization work, while the runbook should be
updated as commands become reliable.

The current integration suite is not yet a dependable gate because the tests
span several different runtime assumptions:

- JPA/service tests that need a loaded local database and indexes
- REST tests that need the application running with the same environment
- admin/load/unload tests that can mutate or reset databases
- insertion tests that need source-data fixtures and a specific database shape
- NCI-META tests that need a different loaded dataset from the sample tests
- ignored legacy REST classes whose status needs to be decided deliberately

## Goals

- Produce a clear, repeatable local command sequence for each supported
  integration-test profile.
- Separate test profiles by required environment instead of treating all `*IT`
  classes as one undifferentiated suite.
- Get a small smoke subset green first, then expand to sample JPA, NCI-META JPA,
  REST, insertion, and admin/load tests.
- Make failures diagnosable by documenting prerequisites, generated artifacts,
  database requirements, and known skips.
- Add Make/Gradle entry points that make the intended test profile obvious.
- Preserve local developer speed by keeping `make quality` focused on static
  checks and unit tests.
- Establish the test baseline needed before deeper SpotBugs cleanup resumes.

## Non-Goals

- Do not make every integration test part of the default `make quality` gate.
- Do not require REST tests to start a server implicitly until the server
  lifecycle is reliable and observable.
- Do not rewrite the entire JUnit 4 suite at once.
- Do not replace the current local MySQL/data-directory model with containers in
  the first pass.
- Do not make admin/load tests run against shared developer databases.

## Proposed Test Profiles

### Unit And Static Quality

Existing fast gate:

```bash
make quality
```

This remains the default local/PR confidence gate for code that does not need a
database.

### Flyway Smoke

Opt-in disposable-schema test:

```bash
./gradlew integrationTest --tests "*.FlywayMigrationIT" \
  -Dflyway.it.enabled=true \
  -Dflyway.it.jdbcUrl=jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_it \
  -Dflyway.it.baselineJdbcUrl=jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_base_it \
  -Dflyway.it.user=root
```

This should remain isolated from loaded sample and NCI-META databases.

### Sample JPA Smoke

Runs against the sample database loaded from `SAMPLE_UMLS` and generated sample
data. This should become the first dependable integration baseline because it is
smaller than the NCI-META suite and does not require a running REST server.

Target areas:

- integrity checks
- search tests
- selected `com.wci.umls.server.test.jpa.*` service tests
- safe algorithm smoke tests such as `WaitAlgorithmIT`,
  `MatrixInitializerAlgorithmIT`, and `QueryActionAlgorithmIT`

### NCI-META JPA Smoke

Runs against the NCI-META sample database loaded from `SAMPLE_NCI`.

Target areas:

- graph resolution
- identifier assignment
- reports
- semantic categories
- NCI-META-oriented algorithm tests
- helper integration tests

### REST Smoke

Runs only when the application is already running from the same sourced
environment.

Target areas:

- security REST smoke
- metadata/content/project/process/report REST smoke
- workflow REST smoke

The first pass should add an explicit preflight check for `BASE_URL` so failures
are reported as "server is not reachable" instead of a long REST stack trace.

### Insertion And Admin/Loader Suites

Keep these separate from the normal sample/NCI-META smoke profiles because they
load, mutate, unload, or reset data.

Target areas:

- insertion loader tests
- merge/update/releasibility tests
- admin load/unload tests
- reset database helpers

These should use named disposable databases or documented manual approval before
running.

## Implementation Plan

### Phase 1: Inventory And Classification

- Generate a machine-readable inventory of all `*IT.java` classes.
- Classify each test by profile:
  `flyway`, `sample-jpa`, `nci-meta-jpa`, `rest`, `insertion`, `admin-loader`,
  `ignored`, or `unknown`.
- Record prerequisites for each class:
  database, source data, indexes, running server, mutates data, expected users,
  and known skip/ignore status.
- Identify tests that are misnamed, accidentally picked up by the wrong task, or
  require manual cleanup.

Deliverables:

- `docs/NM-306-integration-test-inventory.md`
- optional CSV/TSV under `docs/` if useful for sorting and triage
- updates to `docs/database-load-and-test-instructions.md` when a profile's
  command sequence is confirmed

Status:

- initial inventory created in `docs/NM-306-integration-test-inventory.md`
- all 111 current `*IT.java` classes are grouped by apparent profile
- actual pass/fail/skip status still needs to be populated from real profile
  runs

### Phase 2: Local Environment Preflight

- Add a lightweight preflight helper for integration tests.
- Check database connectivity before JPA suites run.
- Check `BASE_URL` before REST suites run.
- Check required directories such as `DATA_DIR`, `INDEX_DIR`, `LVG_DIR`, and
  `SOURCE_DATA_DIR`.
- Make failures explicit and actionable.
- Avoid creating or deleting databases in preflight checks.

Deliverables:

- shared test utility or Gradle task for environment checks
- README/doc updates showing the exact preflight command

Status:

- added `IntegrationTestPreflight` test utility
- added Gradle preflight tasks:
  `sampleJpaIntegrationPreflight`, `nciMetaJpaIntegrationPreflight`,
  `restIntegrationPreflight`, `insertionIntegrationPreflight`,
  `adminLoaderIntegrationPreflight`, and `flywayIntegrationPreflight`
- added Make targets:
  `preflight-sample`, `preflight-ncimeta`, `preflight-rest`,
  `preflight-insertion`, `preflight-admin`, and `preflight-flyway`
- updated `docs/database-load-and-test-instructions.md` with preflight commands
- expanded Flyway preflight guardrails:
  verifies migration resources are on the test runtime classpath, reports parsed
  schema names, rejects the configured app schema, and rejects using the same
  schema for both Flyway smoke-test paths
- added `prepareFlywayIntegrationSchemas` / `make prepare-flyway` so new
  environments can create missing disposable Flyway smoke schemas without
  dropping or cleaning existing non-empty schemas
- ran an initial local preflight baseline on 2026-05-28 and found that the
  generic loaded-data check was too permissive because `sample-jpa` could pass
  against the default `ncimdb` environment
- tightened JPA profile preflight guardrails so `sample-jpa`, `nci-meta-jpa`,
  and `insertion` require the documented fixture schemas by default
  (`ncimdbmeta`, `ncimdbncimeta`, and `ncimdbinsert`) with an explicit
  `-Dintegration.it.expectedSchema.<profile>=...` override for intentionally
  renamed fixture databases
- added sample fixture probes for the MTH/latest project and concepts used by
  multiple sample JPA smoke tests so wrong or partially loaded sample databases
  fail before the full profile runs
- confirmed `rest` passes when the app is running and `BASE_URL` points to that
  server; when the app is down, it fails early with a clear connection message
- confirmed the documented Flyway smoke schemas are reachable but not empty;
  `ncimdb_flyway_it` currently has application tables and
  `ncimdb_flyway_base_it` currently has baseline probe/history tables
- real profile runs still need to populate pass/fail results in the inventory

### Phase 3: Gradle And Make Entry Points

Add named commands for the major profiles while leaving the existing
`integrationTest` task available for ad hoc selection.

Candidate Gradle tasks:

- `flywayIntegrationTest`
- `sampleJpaIntegrationTest`
- `nciMetaJpaIntegrationTest`
- `restIntegrationTest`
- `insertionIntegrationTest`
- `adminLoaderIntegrationTest`

Candidate Make targets:

- `make prepare-sample`
- `make prepare-ncimeta`
- `make prepare-insertion`
- `make prepare-admin`
- `make integration-flyway`
- `make integration-sample`
- `make integration-ncimeta`
- `make integration-rest`
- `make integration-insertion`
- `make integration-admin`

Each target should source `config/local/setenv.sh` and run only the profile it
names.

After each target is added, update `docs/database-load-and-test-instructions.md`
so the runbook shows the preferred Make target first and the direct Gradle
command second.

Status:

- added named Gradle `Test` tasks:
  `flywayIntegrationTest`, `sampleJpaIntegrationTest`,
  `nciMetaJpaIntegrationTest`, `restIntegrationTest`,
  `insertionIntegrationTest`, and `adminLoaderIntegrationTest`
- each named Gradle task depends on its matching preflight task
- added Make targets:
  `integration-flyway`, `integration-sample`, `integration-ncimeta`,
  `integration-rest`, `integration-insertion`, and `integration-admin`
- added `prepareSampleIntegrationData` / `make prepare-sample` to create,
  load, reindex, and generate the documented `ncimdbmeta` sample integration
  fixture with one command; the parent task runs `adminCreateDb`,
  `adminLoadSampleRrfUmls`, `adminReindexSample`, and
  `adminGenerateSampleIntegrationData` in order
- added `prepareNciMetaIntegrationData` / `make prepare-ncimeta` to create,
  load, generate, and reindex the documented `ncimdbncimeta` NCI-META
  integration fixture with one command; the parent task runs `adminCreateDb`,
  `adminLoadNciMetaRrfUmls`, `adminGenerateNciMetaIntegrationData`, and
  `adminReindexNciMeta` in order
- added `prepareInsertionIntegrationData` / `make prepare-insertion` to create,
  load, generate, and reindex the documented `ncimdbinsert` insertion
  integration fixture with one command; the parent task also creates the local
  `terminologies/NCI_INSERT/src` source-data directory expected by the insertion
  loader tests
- added `prepareAdminLoaderIntegrationData` / `make prepare-admin` to create an
  empty disposable `ncimdbadminload` admin/load fixture and copy required loader
  support files into the selected `DATA_DIR`
- updated `docs/database-load-and-test-instructions.md` to show preferred Make
  targets first and direct Gradle task equivalents second
- updated `README.md` with the common named integration profile commands
- actual smoke profile pass/fail results are tracked in Phase 4

### Phase 4: Restore The Smoke Baselines

Bring profiles back in this order:

1. Flyway smoke on disposable empty schemas.
2. Sample JPA smoke with Tomcat stopped.
3. NCI-META JPA smoke with Tomcat stopped.
4. REST smoke with the app already running.
5. Insertion tests with explicit source-data prerequisites.
6. Admin/load/unload suites against disposable databases only.

For each profile:

- run the documented command
- capture failing classes and failure categories
- fix configuration drift first
- fix test assumptions second
- fix product behavior only when the failure represents a real regression
- document known skips instead of leaving them surprising

Status:

- Added `ephemeralFlywayIntegrationTest` / `make integration-flyway-ephemeral`
  so normal Flyway smoke runs create generated disposable schemas and drop those
  same generated schemas afterward.
- `make integration-flyway-ephemeral` passed locally on 2026-05-28 and the
  generated schemas were dropped by the finalizer.
- `make integration-flyway-ephemeral` was rerun on 2026-05-29 with generated
  schemas `ncimdb_nm306_flyway_it_20260529155155` and
  `ncimdb_nm306_flyway_base_it_20260529155155`; the smoke passed and both
  schemas were dropped by the finalizer.
- Flyway smoke passed locally on 2026-05-28 using the fixed-schema path:
  `make integration-flyway
  FLYWAY_IT_JDBC_URL=jdbc:mysql://127.0.0.1:3306/ncimdb_nm306_flyway_it
  FLYWAY_IT_BASELINE_JDBC_URL=jdbc:mysql://127.0.0.1:3306/ncimdb_nm306_flyway_base_it
  FLYWAY_IT_USER=root`
- The successful Flyway run intentionally populated those disposable schemas,
  so repeat fixed-schema runs need fresh or manually recreated empty schemas.
- Sample JPA is next; an accidental early run against the default `ncimdb`
  schema produced 43 failures across 163 tests, so those results are treated as
  configuration-drift signal rather than the real sample baseline.
- The documented `ncimdbmeta` sample fixture was reloaded from `SAMPLE_UMLS` on
  2026-05-28, followed by `adminReindex` and `adminGenerateSampleData`.
- Future refreshes should use `make prepare-sample`, which wraps the same
  create/load/reindex/generate sequence.
- `make preflight-sample` passed against the refreshed `ncimdbmeta` sample
  fixture on 2026-05-28.
- The first real Sample JPA baseline run completed 163 tests with 2 stale
  baseline expectation failures:
  `ComponentStatsIT.testSnomedComponentStats` expected `Total SemanticTypeJpa`
  196 but the refreshed fixture produced 193, and
  `ContentServiceAutocompleteIT.testConceptAutocompleteEdgeCases` expected 2
  SNOMED one-character concept completions but the refreshed fixture produced 3.
- After updating those two fixture expectations, `make integration-sample`
  passed locally on 2026-05-28.
- The documented `ncimdbncimeta` NCI-META fixture was reloaded from
  `SAMPLE_NCI` on 2026-05-28 with `make prepare-ncimeta`, which wraps
  database creation, RRF load, NCI-META fixture generation, and Lucene reindex.
- `make preflight-ncimeta` passed against the refreshed `ncimdbncimeta`
  fixture on 2026-05-28.
- `make integration-ncimeta` passed locally on 2026-05-28 with 20 runnable
  suites and 37 tests.
- The PFS helper classes that were previously named
  `PfsParameterForComponentIT` and `PfsParameterForConceptIT` contained helper
  methods only and emitted no runnable test cases.
- REST preflight now guards against accidentally running the REST smoke against
  the default `ncimdb` schema: `rest` expects `ncimdbmeta` by default, reuses the
  sample fixture probes, and verifies the running server can return MTH/latest
  sample concept `C0000097`.
- The REST schema guard was verified on 2026-05-28: with default `DB_NAME=ncimdb`
  the preflight failed before JUnit ran, and with `DB_NAME=ncimdbmeta` plus the
  app running on `BASE_URL=http://localhost:18080/umls-server-rest` the preflight
  passed.
- The REST smoke was restored on 2026-05-28 and `./gradlew restIntegrationTest`
  passed locally with 111 tests selected, 0 failures, and 2 explicit skips.
- The remaining REST failures were fixed rather than dropped:
  descriptor routes were made tolerant of Spring Boot 3 trailing-slash matching,
  the REST client was aligned with descriptor endpoints, and refreshed
  `SAMPLE_UMLS` count expectations were updated for content query smoke tests.
- `ProcessServiceRestNormalUseIT.testFailOnceAndEmailProcess` is now explicitly
  skipped because it requires a reachable SMTP server and is not suitable for the
  local REST smoke profile.
- `ContentServiceRestNormalUseIT.testGetDeepRelationships` remains an explicit
  legacy skip because deep relationships are calculated weekly rather than
  loaded by the sample fixture.
- The documented `ncimdbinsert` insertion fixture was rebuilt from
  `SAMPLE_NCI` on 2026-05-29 with `make prepare-insertion`, which wraps
  database creation, RRF load, insertion fixture generation, and Lucene reindex.
- `make preflight-insertion` passed against the refreshed `ncimdbinsert`
  fixture on 2026-05-29. The insertion preflight now checks the
  `terminologies/NCI_INSERT/src` payload files, the `NCIMTH/latest` project,
  and baseline `NCIMTH/latest` plus `NCI/2016_04D` concept rows.
- `make integration-insertion` passed locally on 2026-05-29 with 13 runnable
  tests. `GeneratedMergeAlgorithmIT` now uses the in-process REST service
  implementation instead of requiring a running webapp, and
  `PreInsertionAlgorithmIT` now turns editing/automation off for its
  normal-use precondition check and restores the original project flags.
- Clarified the insertion runbook so `make prepare-insertion` comes before
  `make preflight-insertion` for a fresh `ncimdbinsert` schema. The preflight
  is intentionally strict and reports missing `NCI/2016_04D` data until the
  disposable fixture has been rebuilt.
- `UpdatePublishedAlgorithmIT` is listed in the source tree but still has its
  JUnit `@Test` annotation commented out, so it is not part of the current
  13-test insertion baseline. Phase 5 should either restore it deliberately or
  keep it excluded with a clear reason.
- Added `prepareAdminLoaderIntegrationData` / `make prepare-admin` for the
  disposable `ncimdbadminload` admin-loader fixture. The target creates the
  schema if needed, initializes an empty schema, copies loader support files
  into `DATA_DIR`, and rebuilds empty indexes.
- `adminLoaderIntegrationPreflight` now rejects shared schemas and expects
  `ncimdbadminload` by default unless explicitly overridden. It also checks
  bundled loader source files plus copied `acronyms.txt` and `spelling.txt`.
- The legacy admin/load IT classes were ported away from active
  `fail("Fix this...")` placeholders. `make integration-admin` now runs the
  bounded RRF smoke baseline only:
  `RrfSingleLoadAndUnloadIT` and `RrfUmlsLoadAndUnloadIT`.
- `make integration-admin` passed locally on 2026-05-29 against
  `ncimdbadminload` with 2 runnable tests. The RRF loader needed one product
  fix: its final component-stats logging now passes `Branch.ROOT` instead of
  null so assertion-enabled test JVMs do not fail after a successful load.
- The RF2 comparison, RF2 snapshot, OWL, and ClaML admin-loader classes have
  Gradle-era test bodies but are annotated with `@Ignore` because they exceeded
  the local smoke runtime or memory budget. They remain follow-up candidates for
  a heavier/manual profile.
- `RrfUmlsLoadAndUnloadIT` currently verifies the primary MTH unload path.
  Removing secondary `SNOMEDCT_US/2014_09_01` after MTH removal exposes an
  existing `root_terminologies` foreign-key constraint failure and remains
  follow-up work.
- Phase 5 cleanup is now implemented for the active smoke harnesses. REST base
  fixtures use `RestIntegrationSupport` for common property loading,
  `base.url` validation, viewer/admin credential validation, and guarded
  logout.
- REST subclasses that create short-lived cleanup/helper clients now reuse the
  inherited REST fixture `properties` instead of rereading the property file.
- NCI-META REST editing subclasses now use base helpers for copied-concept
  cleanup and guarded logout instead of repeating ad hoc teardown client
  construction.
- REST and JPA search tests now use shared unique test-data helpers instead of
  bare timestamp suffixes for names and identifiers.
- Integration preflight now verifies that `index.dir` is isolated under the
  profile's `data.dir`, unless deliberately overridden with
  `-Dintegration.it.allowExternalIndexDir=true`.
- Ignored REST fixtures/tests now carry explicit reasons.
- The PFS helper classes have been renamed to
  `PfsParameterForComponentTestSupport` and
  `PfsParameterForConceptTestSupport`, and `nciMetaJpaIntegrationTest` no
  longer selects either helper-only class.
- The Phase 5 cleanup pass was verified on 2026-05-29 with
  `./gradlew compileTestJava`, `./gradlew checkstyleTest`, Gradle dry-runs for
  sample JPA, NCI-META JPA, REST, insertion, admin/load, and ephemeral Flyway
  profiles, and `git diff --check`.
- `docs/database-load-and-test-instructions.md` now contains the known-good
  NM-306 command sequence for Flyway, sample JPA, NCI-META JPA, REST,
  insertion, and bounded admin/load smoke profiles.

### Phase 5: Test Harness Modernization

Once the smoke profiles are reproducible:

- centralize common setup currently repeated across REST/JPA tests
  - done for REST with `RestIntegrationSupport` and inherited `properties`
    reuse in REST subclasses; JPA search now shares unique-name helpers
- reduce static mutable fixture state where it causes order dependence
  - addressed where the smoke harness was carrying avoidable static-style
    setup or timestamp-only identifiers
- prefer unique test data names/ids to avoid cross-test collisions
  - done for REST-created names and JPA search-test suffixes
- add deterministic cleanup helpers for rows created by tests
  - done for copied concepts in the ignored NCI-META REST editing tests and
    guarded REST logouts
- isolate Hibernate Search index directories per profile or per disposable data
  directory
  - enforced by preflight unless intentionally overridden
- make REST client setup consistently use the env-backed `BASE_URL`
  - done by validating `base.url` in the shared REST fixture loader
- decide whether ignored REST/NCI-META classes should be repaired, split, or
  kept explicitly ignored with comments
  - documented as explicit skips/fixtures; behavioral repair remains a later
    profile decision
- keep helper-only classes out of runnable integration profiles
  - done for the two renamed PFS helper classes in the NCI-META profile

JUnit modernization should be incremental. Keep JUnit 4 until the suite is
stable, then consider a later migration to JUnit 5 tags for profile selection.

### Phase 6: CI Strategy

Start with manual or scheduled CI only after local profiles are stable.

Recommended progression:

1. PR gate: keep `make quality` and Trivy only.
2. Manual GitHub Action: run Flyway smoke against disposable MySQL service.
3. Manual GitHub Action: run sample JPA smoke after loading small sample data.
4. Scheduled/nightly Action: run broader NCI-META and REST profiles if data load
   time is acceptable.
5. Consider selected smoke profiles as required PR checks only after they are
   fast and reliable.

## Failure Categories To Track

Use consistent labels while triaging:

- `config`: missing env var, wrong property name, stale test setup
- `db-schema`: Flyway/migration/schema mismatch
- `db-data`: missing expected terminology/project/workflow data
- `index`: missing or stale Hibernate Search/Lucene index
- `rest-server`: server not running or wrong `BASE_URL`
- `auth`: expected local users or roles missing
- `filesystem`: missing data/source/LVG/acronym/spelling files
- `order-dependent`: test passes alone but fails in a suite
- `cleanup`: test leaves data that breaks later tests
- `product-regression`: application behavior changed unexpectedly
- `obsolete-test`: test no longer matches supported product behavior

## Definition Of Done

NM-306 is complete when:

- integration-test profiles are documented and runnable by name
- smoke baselines for Flyway, sample JPA, NCI-META JPA, REST, insertion, and
  bounded admin-loader profiles have known commands and known expected outcomes
- failures are either fixed or documented as explicit skips/follow-ups
- database-mutating suites cannot accidentally run against an unstated shared
  database
- README and `docs/database-load-and-test-instructions.md` point to the current
  profile commands
- `docs/database-load-and-test-instructions.md` remains the concrete runbook for
  setup and execution, with this NM-306 plan linked as the modernization roadmap
- the team has a clear baseline to use before behavior-sensitive SpotBugs
  cleanup resumes

## Relationship To NM-304

NM-304 left 40 active legacy SpotBugs suppression patterns after auditing the
baseline. NM-306 should be treated as a prerequisite for the riskiest remaining
cleanup families:

- `DM_DEFAULT_ENCODING`
- `EI_EXPOSE_REP` / `EI_EXPOSE_REP2`
- `CT_CONSTRUCTOR_THROW`
- `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE`
- remaining null/correctness edge cases
- unused/unwritten fields that may be framework-populated

The practical rule: if a SpotBugs fix can change persisted data, serialized
shape, generated file bytes, REST behavior, or algorithm lifecycle, make sure the
relevant NM-306 profile is green first.
