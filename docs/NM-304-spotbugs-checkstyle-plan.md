# NM-304 Static Analysis and Vulnerability Scan Plan

## Summary

Add Gradle-backed SpotBugs, Checkstyle, and Trivy verification to MEME, using
`workspace-evsrestapi/evsrestapi` as the local reference for the quality and
vulnerability-scan wiring.

The initial implementation makes the quality gate useful without turning NM-304
into a broad legacy refactor. Checkstyle enforces low-risk hygiene now.
SpotBugs fails for new unsuppressed findings while the existing legacy backlog
is cleaned up incrementally.

Current status:

- the NM-304 quality and Trivy wiring has been merged into
  `dss/NM-291-migration-phase2`
- Checkstyle, SpotBugs, `make scan`, and the GitHub Trivy workflow are in place
- the first SpotBugs cleanup passes are complete
- the remaining SpotBugs baseline has been audited and documented
- the default-encoding migration is complete for insertion, validation, loader,
  file-utility source-data, release, statistics, process-log, and selected
  property-file paths

## Reference Project

Use this project as the local reference:

- `/Users/deborahshapiro/Code/workspace-evsrestapi/evsrestapi`

Relevant reference files:

- `/Users/deborahshapiro/Code/workspace-evsrestapi/evsrestapi/build.gradle`
- `/Users/deborahshapiro/Code/workspace-evsrestapi/evsrestapi/config/spotbugs/excludeFilter.xml`
- `/Users/deborahshapiro/Code/workspace-evsrestapi/evsrestapi/config/trivy/html.tpl`
- `/Users/deborahshapiro/Code/workspace-evsrestapi/evsrestapi/Makefile`
- `/Users/deborahshapiro/Code/workspace-evsrestapi/evsrestapi/.github/workflows/trivy-scan.yml`

Important reference patterns:

- Apply the `com.github.spotbugs` Gradle plugin.
- Configure HTML reports for `spotbugsMain` and `spotbugsTest`.
- Keep `ignoreFailures = false`.
- Use `config/spotbugs/excludeFilter.xml` for accepted baseline exclusions.
- Add a `make scan` target for a Trivy dependency vulnerability scan.
- Generate a temporary Gradle lockfile for the Trivy scan and clean it up
  afterward so dependency-lock artifacts are not accidentally committed.
- Use `config/trivy/html.tpl` for local HTML report output.
- Add a GitHub Actions workflow that runs the Trivy scan on pull requests and
  manual dispatch, failing on HIGH or CRITICAL vulnerabilities.

## Current Implementation

Implemented in NM-304 and merged into `dss/NM-291-migration-phase2`:

- `build.gradle`
  - applies `checkstyle`
  - applies `com.github.spotbugs`
  - adds SpotBugs annotations as compile-only dependencies
  - wires SpotBugs into Gradle `check`
  - enables HTML reports for Checkstyle and SpotBugs
  - enables Gradle dependency locking so Trivy can scan a generated lockfile
- `config/checkstyle/checkstyle.xml`
  - enables conservative import and modifier-order checks
- `config/checkstyle/suppressions.xml`
  - suppresses import cleanup for a few CRLF legacy release files to avoid
    noisy line-ending diffs
- `config/spotbugs/excludeFilter.xml`
  - baselines legacy findings from the first project-wide scan
  - leaves SpotBugs strict for new unsuppressed patterns
  - documents why the remaining broad suppression families are deferred
- `Makefile`
  - includes a strict `make scan` target aligned with the reference Trivy
    lockfile/report workflow
  - creates a temporary Gradle lockfile, scans it for HIGH and CRITICAL
    vulnerabilities, writes `report.html`, and removes temporary lock output
- `config/trivy/html.tpl`
  - adds the local Trivy HTML report template from the reference project
- `.github/workflows/trivy-scan.yml`
  - adds the CI Trivy vulnerability scan for pull requests and manual runs

Initial cleanup completed:

- removed unused and redundant imports surfaced by Checkstyle
- fixed modifier-order findings such as `static public` to `public static`
- preserved existing line endings in legacy CRLF files

Phase 1 cleanup completed:

- fixed the targeted reference-comparison, null-on-some-path, unrelated-type,
  redundant-null-check, and adjacent string-comparison findings
- kept the remaining non-phase-1 legacy SpotBugs backlog baselined by pattern
- left the Phase 1 bug families unsuppressed so regressions fail the build

Phase 2 cleanup completed:

- closed the targeted file, process, ontology, XML, and properties resources
  with try-with-resources or explicit closes
- removed the resource-handling families from the SpotBugs baseline:
  `OS_OPEN_STREAM`, `OBL_UNSATISFIED_OBLIGATION`, and
  `OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE`
- left the Phase 2 bug families unsuppressed so regressions fail the build

Phase 3 cleanup completed:

- replaced shared string lock monitors with private lock objects
- synchronized lazy static initialization and factory refresh paths that remain
  process-wide state
- converted per-test fixtures from static fields to instance fields where setup
  was already per-test
- removed the static-state/threading families from the SpotBugs baseline:
  `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD`,
  `DL_SYNCHRONIZATION_ON_SHARED_CONSTANT`, `LI_LAZY_INIT_STATIC`, and
  `LI_LAZY_INIT_UPDATE_STATIC`
- left the Phase 3 bug families unsuppressed so regressions fail the build

Step 5 cleanup completed:

- removed dead local stores in validation checks, algorithm setup, release
  helpers, and affected tests
- replaced intentional lazy-loading getter/collection-size calls with
  `ConfigUtility.initializeLazy(...)`
- checked filesystem operation return values instead of discarding
  `mkdir`, `delete`, `createNewFile`, and `renameTo` results
- removed the ignored-return/dead-store families from the SpotBugs baseline:
  `RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT`, `DLS_DEAD_LOCAL_STORE`,
  `RV_RETURN_VALUE_IGNORED_BAD_PRACTICE`, and `UC_USELESS_OBJECT`

Default encoding cleanup completed:

- changed insertion and inversion validation source-data readers/writers to use
  `StandardCharsets.UTF_8`
- covered common insertion helpers, semantic type file generation, bequeathal
  output writers, report/checklist output, and validation `.src`/`.RRF` readers
- changed the next loader/file-utility batch to use `StandardCharsets.UTF_8`:
  `ClamlLoaderAlgorithm`, `FileSorter`, `Rf2FileCopier`,
  `Rf2FullLoaderAlgorithm`, `SimpleLoaderAlgorithm`,
  `UmlsIdentityLoaderAlgorithm`, and the touched `AdHocAlgorithm` file paths
- changed process-log export and the atom-class search-handler acronym file
  reader to use `StandardCharsets.UTF_8`
- changed release/statistics writers and readers to use
  `StandardCharsets.UTF_8`, including RRF mappings, RRF statistics, RRF
  content/history/index/metadata writers, and MetamorphoSys release files
- changed loader integration-test fixture writers to use
  `StandardCharsets.UTF_8`
- verified the batch with `compileJava`, `checkstyleMain`, normal
  `spotbugsMain`, `spotbugsTest`, and the refreshed `integration-insertion`
  suite
- reran the temporary no-`DM_DEFAULT_ENCODING` SpotBugs diagnostic and
  confirmed the main-code default-encoding backlog is now clean
- removed `DM_DEFAULT_ENCODING` from the SpotBugs baseline

Baseline audit completed:

- reran SpotBugs with an empty temporary exclude filter
- confirmed the remaining suppressions still correspond to active findings,
  except one stale entry
- removed the stale `EI_EXPOSE_STATIC_REP` suppression
- removed the completed `DM_DEFAULT_ENCODING` suppression
- left 39 active legacy suppression patterns in
  `config/spotbugs/excludeFilter.xml`

## Trivy Implementation

Trivy is an NM-304 deliverable and is implemented as a strict dependency
vulnerability scan.

Implemented files:

- `config/trivy/html.tpl`
  - contains the Trivy HTML report template from the reference project
- `Makefile`
  - `make scan` creates a temporary Gradle lockfile with
    `./gradlew dependencies --write-locks`
  - scans `gradle.lockfile` with Trivy's vulnerability scanner instead of
    scanning the whole repository
  - emits `report.html` using `config/trivy/html.tpl`
  - fails the target when Trivy is missing or HIGH or CRITICAL vulnerabilities
    are present
  - removes temporary `gradle/dependency-locks` and `gradle.lockfile` output
- `.github/workflows/trivy-scan.yml`
  - runs on pull requests to `dss/NM-291-migration-phase2`
  - allows manual `workflow_dispatch` runs
  - installs Java 17 and Trivy on the runner
  - generates the temporary Gradle lockfile
  - runs Trivy's vulnerability scanner with HIGH and CRITICAL severity filtering
  - publishes a readable failure summary of vulnerable packages

The Trivy workflow is intentionally scoped to
`dss/NM-291-migration-phase2` for now. It should not target `develop`,
`develop-*`, or `master` until the Trivy config and template exist on those
branches.

Dependency cleanup completed:

- `commons-io` upgraded from `2.8.0` to `2.16.1`
- `commons-vfs2` upgraded from `2.0` to `2.10.0`
- `plexus-utils` made explicit and forced to `3.6.1`
- `tomcat.version` temporarily overridden to `10.1.55` until Spring Boot
  3.5.x manages Tomcat `10.1.55` or newer
- MySQL Connector/J moved from legacy `mysql:mysql-connector-java:8.0.17`
  to `com.mysql:mysql-connector-j:9.7.0`
- default MySQL JDBC driver class updated to `com.mysql.cj.jdbc.Driver`

## First SpotBugs Baseline

The first project-wide SpotBugs scan found a legacy backlog:

- `spotbugsMain`: 1,267 findings
- `spotbugsTest`: 196 findings

The largest initial main-code families were:

- `EI_EXPOSE_REP` / `EI_EXPOSE_REP2`: exposed mutable model state
- `CT_CONSTRUCTOR_THROW`: constructors that can throw
- `RV_RETURN_VALUE_IGNORED_*`: ignored return values
- `DM_DEFAULT_ENCODING`: file IO using the platform default encoding
- `WMI_WRONG_MAP_ITERATOR`: inefficient map iteration
- `DLS_DEAD_LOCAL_STORE`: values assigned and never used
- `NP_*`: possible null dereferences
- `RC_REF_COMPARISON`: reference comparison where value comparison is likely
- `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD`: instance methods writing static
  fields

Because those findings span hundreds of legacy classes, the initial gate should
not attempt a wholesale fix. The baseline exists so new work can ratchet quality
forward while legacy cleanup is planned in smaller, reviewable slices.

## Current SpotBugs Baseline Status

The current `config/spotbugs/excludeFilter.xml` contains 39 active legacy
suppression patterns. These were confirmed by running SpotBugs with an empty
temporary exclude filter and comparing the reported bug patterns back to the
checked-in filter.

Completed and no longer suppressed:

- Phase 1 targeted correctness/null families:
  `RC_REF_COMPARISON`, `NP_NULL_ON_SOME_PATH`,
  `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE`,
  `NP_NULL_ON_SOME_PATH_EXCEPTION`, `EC_UNRELATED_TYPES`, and
  `RCN_REDUNDANT_NULLCHECK_*`
- Phase 2 resource-handling families:
  `OS_OPEN_STREAM`, `OBL_UNSATISFIED_OBLIGATION`, and
  `OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE`
- Phase 3 static/threading families:
  `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD`,
  `DL_SYNCHRONIZATION_ON_SHARED_CONSTANT`, `LI_LAZY_INIT_STATIC`, and
  `LI_LAZY_INIT_UPDATE_STATIC`
- Step 5 ignored-return/dead-store families:
  `RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT`, `DLS_DEAD_LOCAL_STORE`,
  `RV_RETURN_VALUE_IGNORED_BAD_PRACTICE`, and `UC_USELESS_OBJECT`
- Default encoding family:
  `DM_DEFAULT_ENCODING`

Remaining backlog:

- broad model/JPA encapsulation and mutable representation exposure
- constructor/lifecycle cleanup for reflection-loaded algorithms
- dynamic SQL/query execution review
- remaining null/correctness edge cases that need focused behavior tests
- legacy control-flow and exception-handling decisions
- unused/unwritten fields that may be populated by frameworks or legacy
  serialization paths
- lower-priority modernization/noise such as boxing/string construction and map
  iteration

## Cleanup Priorities

### 1. Correctness and Null Handling (Completed Initial Cleanup)

Start with findings most likely to represent real behavior bugs:

- `RC_REF_COMPARISON`
- `NP_NULL_ON_SOME_PATH`
- `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE`
- `NP_NULL_ON_SOME_PATH_EXCEPTION`
- `EC_UNRELATED_TYPES`
- `RCN_REDUNDANT_NULLCHECK_*`

Likely first areas:

- `AbstractMergeAlgorithm`
- `AbstractMolecularAction`
- `ContextLoaderAlgorithm`
- `Create*BequeathalAlgorithm`
- `QuerySearchHandler`
- `MGV_B`
- `MGV_E2`

Goal:

- replace suspicious `==` comparisons with value comparisons where appropriate
- make null assumptions explicit
- remove redundant null checks only after verifying behavior
- add focused tests around corrected branches when practical

Status:

- the targeted Phase 1 families are no longer suppressed
- a smaller set of null/correctness remnants remains baselined because each
  path needs focused behavior verification before changing legacy edge cases

### 2. Resource Handling (Completed Initial Cleanup)

Next address findings that can leak files, streams, readers, or writers:

- `OS_OPEN_STREAM`
- `OBL_UNSATISFIED_OBLIGATION`
- `OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE`

Likely first areas:

- loaders
- release writers
- file sorters
- config and source-data utilities

Goal:

- convert eligible file and stream handling to try-with-resources
- preserve current close/flush behavior where output file formats depend on it
- verify with compile, quality checks, and focused loader/release tests when
  available

Status:

- the Phase 2 resource-handling families are no longer suppressed
- future resource changes should be handled opportunistically when touching the
  owning loader, release writer, or utility

### 3. Static State and Threading (Completed)

Phase 3 addressed static mutable state and synchronization warnings:

- `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD`
- `DL_SYNCHRONIZATION_ON_SHARED_CONSTANT`
- `LI_LAZY_INIT_STATIC`
- `LI_LAZY_INIT_UPDATE_STATIC`

Covered areas:

- `RootServiceJpa`
- `UmlsIdentityServiceJpa`
- `NotificationWebsocketConfigurator`
- `TreePositionAlgorithm`

Outcome:

- intentional process-wide state remains static and is protected behind
  synchronized access where needed
- unsafe shared-constant synchronization targets were replaced with private
  lock objects
- per-test fixture state now uses instance fields
- transaction and identity-assignment semantics were preserved

### 4. Default Encoding

Handle `DM_DEFAULT_ENCODING` as a controlled migration.

Why this matters:

- APIs such as `FileReader`, `FileWriter`, `InputStreamReader(InputStream)`,
  `OutputStreamWriter(OutputStream)`, `String.getBytes()`, and
  `new String(byte[])` use the JVM default charset.
- The default charset can differ between developer machines, GitHub runners,
  servers, and JVM launch settings.
- MEME reads and writes user-facing terminology files, release artifacts, and
  config files, so default-charset behavior can create machine-specific output.

Batching approach:

- decide the intended encoding for each file family
- use `UTF-8` for application/config/generated text unless legacy data proves a
  different requirement
- treat RRF/RF2/source-data file IO carefully because external data formats may
  have release-specific expectations
- convert in small batches by subsystem
- remove the broad `DM_DEFAULT_ENCODING` baseline once all active findings are
  converted and the diagnostic run is clean

Batch 1, internal/config text IO:

- `ConfigUtility`
- `PropertyUtility`
- `ConfigureServiceRestImpl`
- selected REST report/import paths
- selected security handler request/response streams

Batch 1 testing:

- add or extend unit tests with non-ASCII content
- read and write test fixtures with `StandardCharsets.UTF_8`
- run targeted tests under the normal JVM charset
- run the same tests with `-Dfile.encoding=ISO-8859-1` to catch accidental
  default-charset use

Batch 2, source-data readers:

- `ClamlLoaderAlgorithm`
- `FileSorter`
- `Rf2FileCopier`
- `Rf2FullLoaderAlgorithm`
- `SimpleLoaderAlgorithm`
- `UmlsIdentityLoaderAlgorithm`
- insertion and validation algorithms
- touched `AdHocAlgorithm` file paths

Batch 2 status:

- completed for insertion, validation, loader, and file-utility algorithms
- the remaining release/statistics findings were handled in Batch 3

Batch 2 testing:

- create minimal `.src`, `.RRF`, and RF2 fixtures containing UTF-8 characters
- assert parsed values, sorted output, and copied output bytes
- keep the fixtures small and focused on the touched reader/writer path
- for the completed insertion/validation slice, use
  `make prepare-insertion && make integration-insertion` against
  `ncimdbinsert`

Batch 3, release/statistics writers and readers:

- release file writers
- release statistics readers/writers
- RRF mappings, content, history, index, and metadata output
- MetamorphoSys release-file generation and replacement paths

Batch 3 testing:

- use golden-file tests where practical for generated `.RRF` and report output
- preserve append/truncate behavior exactly
- compare generated UTF-8 bytes against expected output for representative
  release files

Final cleanup:

- completed by removing `<Bug pattern="DM_DEFAULT_ENCODING"/>` from
  `config/spotbugs/excludeFilter.xml`
- the temporary no-`DM_DEFAULT_ENCODING` diagnostic now passes with no active
  main-code findings

### 5. Ignored Return Values and Dead Stores

Prioritize no-side-effect ignored return values before broad bad-practice
findings:

- `RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT`
- `DLS_DEAD_LOCAL_STORE`
- `UC_USELESS_OBJECT`

Goal:

- fix places where calls such as `trim()`, `replace()`, or immutable operations
  discard their result
- remove genuinely dead assignments
- avoid deleting assignments that exist for debugger visibility or old side
  effects until verified

Status:

- completed the initial cleanup of Step 5 main/test findings
- removed dead local stores in validation checks, algorithm setup, release
  helpers, and affected tests
- replaced intentional lazy-loading getter/collection-size calls with
  `ConfigUtility.initializeLazy(...)`
- checked filesystem operation return values instead of discarding
  `mkdir`, `delete`, `createNewFile`, and `renameTo` results
- removed the Step 5 SpotBugs suppressions for:
  `RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT`, `DLS_DEAD_LOCAL_STORE`,
  `RV_RETURN_VALUE_IGNORED_BAD_PRACTICE`, and `UC_USELESS_OBJECT`

Verification:

- `./gradlew compileJava compileTestJava`
- `./gradlew spotbugsMain spotbugsTest`
- `./gradlew check -x test`
- `./gradlew test --tests org.ihtsdo.otf.ts.helpers.KeyValuesMapUnitTest`

Note: the full `./gradlew test` task is not a clean local gate yet because it
includes environment-coupled integration/example tests that expect REST services,
database configuration, and populated data to be available.

### 6. Broad Model Encapsulation

Defer the bulk of:

- `EI_EXPOSE_REP`
- `EI_EXPOSE_REP2`
- `MS_EXPOSE_REP`

These are mostly in JPA/domain model classes. Defensive copying can interact
poorly with Hibernate, Jackson, XML binding, and legacy callers that expect live
collections.

Recommended approach:

- handle only when already changing a model contract
- prefer tests around serialization and persistence behavior before changing
  getters/setters
- avoid mechanical defensive-copy changes across the model layer

### 7. Constructor Throws

Defer broad cleanup of:

- `CT_CONSTRUCTOR_THROW`

This appears across many algorithm and service classes because constructors
perform setup that can throw. Cleaning it up is worthwhile, but it is a larger
design change.

Recommended approach:

- address opportunistically when working on a specific algorithm
- move heavy setup to explicit initialization methods where it clarifies
  lifecycle
- avoid changing constructor behavior for reflection-driven algorithm loading
  without tests

## Suggested Follow-Up Tickets

Completed in NM-304:

- correctness/null SpotBugs initial cleanup
- resource handling cleanup
- static state and synchronization review
- ignored return values and dead-store cleanup
- default encoding migration for source-data, loader, release, statistics, and
  related file IO
- SpotBugs baseline audit and suppression documentation

Remaining follow-up tickets:

- NM-304 follow-up: JPA model encapsulation strategy
- NM-304 follow-up: constructor initialization strategy
- NM-304 follow-up: dynamic SQL/query execution review
- NM-304 follow-up: remaining null/correctness edge-case review
- NM-304 follow-up: legacy exception and control-flow cleanup
- NM-304 follow-up: unused/unwritten field framework-population review
- NM-304 follow-up: remaining static/global state mutability review
- NM-304 follow-up: low-risk modernization/noise cleanup

## Verification

Use the existing quality target:

```bash
make quality
```

Equivalent direct Gradle commands:

```bash
source config/local/setenv.sh
unset APP_DIR CATALINA_BASE DATA_DIR INDEX_DIR LVG_DIR SOURCE_DATA_DIR
./gradlew check -x test
./gradlew test --tests '*UnitTest'
```

Before committing, also run:

```bash
git diff --check
```
