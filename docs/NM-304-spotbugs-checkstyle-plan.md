# NM-304 Static Analysis and Vulnerability Scan Plan

## Summary

Add Gradle-backed SpotBugs, Checkstyle, and Trivy verification to MEME, using
`workspace-evsrestapi/evsrestapi` as the local reference for the quality and
vulnerability-scan wiring.

The initial implementation should make the quality gate useful without turning
NM-304 into a broad legacy refactor. Checkstyle can enforce low-risk hygiene now.
SpotBugs should fail for new unsuppressed findings while the existing legacy
backlog is cleaned up incrementally.

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

Implemented in the NM-304 branch:

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
  - runs on pull requests to `develop`, `develop-*`, and `master`
  - allows manual `workflow_dispatch` runs
  - installs Java 17 and Trivy on the runner
  - generates the temporary Gradle lockfile
  - runs Trivy's vulnerability scanner with HIGH and CRITICAL severity filtering
  - publishes a readable failure summary of vulnerable packages

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

The largest current main-code families are:

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

## Cleanup Priorities

### 1. Correctness and Null Handling

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

### 2. Resource Handling

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

Recommended approach:

- decide the intended encoding for each file family
- use `UTF-8` for application/config/generated text unless legacy data proves a
  different requirement
- treat RRF/RF2/source-data file IO carefully because external data formats may
  have release-specific expectations
- convert in small batches by subsystem

Likely first areas:

- `ConfigUtility`
- `PropertyUtility`
- `FileSorter`
- `Rf2FileCopier`
- insertion and validation algorithms
- release file writers

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

Break the backlog into focused tickets:

- NM-304 follow-up: correctness/null SpotBugs cleanup
- NM-304 follow-up: resource handling cleanup
- NM-304 follow-up: static state and synchronization review
- NM-304 follow-up: default encoding migration plan
- NM-304 follow-up: ignored return values and dead-store cleanup
- NM-304 follow-up: JPA model encapsulation strategy
- NM-304 follow-up: constructor initialization strategy

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
