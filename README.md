# NCI MEME 5

NCI MEME 5 is the UMLS Terminology Server codebase used for MEME-style
terminology editing, metadata loading, release support, validation, and REST UI
workflows. The application is a Java 17 Gradle project that packages a WAR and
can also run locally through Spring Boot.

> **Release QA status (2026-10-22): CCS source families are implemented and
> complete.** Atom-flag and MRCUI work remains research with implementation
> deferred. The RADLEX trial was rolled back. See
> [research status](#release-qa-research-status) for the status of each item.

## Quick Deploy

Use this path for a normal redeploy when the target database, data directories,
indexes, and server-owned environment file are already in place. The detailed
database, migration, and test setup notes below are still available for
first-time environments and validation work.

Build the Spring Boot executable WAR from the checked-out branch:

```sh
./gradlew clean bootWar
```

Install the built artifact on the server:

```sh
mkdir -p /local/content/MEME/MEME5/ncim/deploy
cp build/libs/ROOT-2.0.0-SNAPSHOT-webapp.war \
  /local/content/MEME/MEME5/ncim/deploy/nci-meme5-webapp.war
```

Confirm the server environment file exists and points at the intended database,
data, index, source-data, and port settings:

```sh
test -r /local/content/MEME/MEME5/ncim/setenv.sh
```

Restart the Spring Boot service:

```sh
sudo systemctl restart nci-meme5
sudo systemctl status nci-meme5
```

For a foreground smoke run instead of systemd:

```sh
source /local/content/MEME/MEME5/ncim/setenv.sh
java -Dcatalina.base=/local/content/MEME/MEME5/ncim \
  -jar /local/content/MEME/MEME5/ncim/deploy/nci-meme5-webapp.war
```

Open the configured context path, for example:

```text
http://<server>:<SERVER_PORT>/<SERVER_SERVLET_CONTEXT_PATH>
```

For local defaults this is usually `http://localhost:8080/umls-server-rest`;
NCI-META production normally uses `/ncim-server-rest`.

Do not deploy this WAR under an external Tomcat `webapps` directory. Production
uses the executable WAR with embedded Tomcat; see
`config/prod-nci-meta/src/main/resources/README.txt` for service and production
overlay details.

The Angular 20 UI is packaged into the executable WAR under `ui20/` when
building the backend artifact:

```sh
./gradlew clean bootWar
```

The legacy UI remains at `/index.html#/login`. The Angular 20 UI is available
under the same servlet context at `/ui20/index.html#/login`; for example,
`http://localhost:8080/umls-server-rest/ui20/index.html#/login`.

## Architecture

At a high level the project is organized around:

- `src/main/java` - application entry point, REST services, admin utilities,
  algorithms, helpers, and service implementations.
- `src/main/resources` - Spring-style runtime configuration, Log4j settings,
  JPA persistence metadata, Flyway migrations, and parser resources.
- `src/main/webapp` - legacy web UI assets packaged into the WAR.
- `src/test/java` and `src/test/resources` - unit tests and integration test
  support.
- `config` - local and deployment configuration defaults, sample terminology
  data, and environment bootstrap files.
- `docs` - migration plans and detailed local database/test procedures.

Runtime configuration is driven by `src/main/resources/application.properties`
with environment variables supplied by `config/local/setenv.sh`. The project
keeps Hibernate/JPA and Flyway wiring explicit in the application/admin code
instead of relying on Spring Boot JDBC or JPA auto-configuration.

User-facing timestamps are formatted with `APP_DISPLAY_TIMEZONE`, which
defaults to `America/New_York`. Database/JDBC timestamp interpretation remains
controlled separately by `DB_SERVER_TIMEZONE`, which defaults to `UTC`.
Insertion progress and report-checklist notification recipients are configured
with `INSERTION_NOTIFICATION_RECIPIENTS`; failed process runs continue to use the
process feedback email.

## Prerequisites

- Java 17.
- The checked-in Gradle wrapper, `./gradlew`.
- GNU Make or a compatible `make`.
- Bash for sourcing `config/local/setenv.sh`.
- MySQL reachable from the values in `config/local/setenv.sh`; local defaults
  use `127.0.0.1:3306`, database `ncimdb`, and user `root`.
- Local data directories for `APP_DIR`, `DATA_DIR`, `INDEX_DIR`, and `LVG_DIR`.
  The bootstrap script creates the expected values but not the full data
  payloads.
- Trivy only if you plan to use `make scan`.

For local overrides, export variables before sourcing the bootstrap:

```sh
export DB_NAME=ncimdbmeta
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
source config/local/setenv.sh
```

The Makefile targets source `config/local/setenv.sh` automatically before
running Gradle.

REST clients and test JDBC helpers validate configured network targets before
opening connections. `DB_ALLOWED_HOSTS` defaults to `DB_HOST`, and
`REST_CLIENT_ALLOWED_HOSTS` defaults to `localhost,127.0.0.1,::1`; set the
matching allowlist when `DB_HOST` or `BASE_URL` points to a different host.

## Common Workflows

Build and package the project artifacts:

```sh
make build
```

Run unit tests:

```sh
make test
```

The default unit test filter is `*UnitTest`. Override it when you need a
different focused test selection:

```sh
make test UNIT_TEST_PATTERN='*MigrationUtilityUnitTest'
```

Run Gradle verification checks:

```sh
make quality
```

Check integration-test prerequisites before running a profile. These targets are
read-only, but each profile expects its own fixture database; the default
`DB_NAME=ncimdb` is for normal local app startup and is intentionally rejected
by these preflights.

```sh
DB_NAME=ncimdbmeta make preflight-sample
DB_NAME=ncimdbncimeta make preflight-ncimeta
DB_NAME=ncimdbmeta make preflight-rest
DB_NAME=ncimdbinsert make preflight-insertion
DB_NAME=ncimdbadminload make preflight-admin
```

REST preflight also probes the running server for sample fixture data, so the
app must already be running from the same environment. For example, if the REST
server is on port `18080`, run:

```sh
DB_NAME=ncimdbmeta SERVER_PORT=18080 \
BASE_URL=http://localhost:18080/umls-server-rest make preflight-rest
```

For a non-local REST endpoint, also set `REST_CLIENT_ALLOWED_HOSTS` to the host
from `BASE_URL`.

If a preflight reports missing fixture rows, prepare or refresh that fixture
before running the matching integration profile.

Stop any local Spring Boot or Tomcat app that is using the same `INDEX_DIR`
before running the sample, NCI-META, insertion, or admin-loader integration
profiles. These tests open Lucene/Hibernate Search indexes directly and will
fail with `LockObtainFailedException` if the application already holds the
index writer locks. REST integration is the exception: it requires the app to
be running from the same database and index environment being tested.

Prepare the sample integration-test database fixture:

```sh
DB_NAME=ncimdbmeta make prepare-sample
```

Prepare the NCI-META integration-test database fixture:

```sh
DB_NAME=ncimdbncimeta make prepare-ncimeta
```

Prepare the insertion integration-test database fixture:

```sh
DB_NAME=ncimdbinsert make prepare-insertion
```

Prepare the disposable admin/load integration-test database fixture:

```sh
DB_NAME=ncimdbadminload make prepare-admin
```

Run named integration-test profiles with the same fixture database:

```sh
DB_NAME=ncimdbmeta make integration-sample
DB_NAME=ncimdbncimeta make integration-ncimeta
DB_NAME=ncimdbmeta make integration-rest
DB_NAME=ncimdbinsert make integration-insertion
DB_NAME=ncimdbadminload make integration-admin
```

Or export the fixture database once before running multiple commands:

```sh
export DB_NAME=ncimdbmeta
make preflight-sample
make integration-sample
```

Run Flyway integration tests with generated disposable schemas:

```sh
make integration-flyway-ephemeral
```

Start the application locally with Spring Boot:

```sh
make run
```

Then open:

```text
http://localhost:8080/umls-server-rest
```

Run Flyway migrations against the configured database:

```sh
make migrate
```

Inspect or validate Flyway state:

```sh
make migrate-info
make migrate-validate
```

Run the future Trivy filesystem scan target after Trivy is installed:

```sh
make scan
```

Equivalent Gradle commands can be run directly after sourcing the environment:

```sh
source config/local/setenv.sh
./gradlew clean assemble
./gradlew bootRun
./gradlew adminFlywayMigrate
```

For deterministic unit test and quality runs, clear the local path overrides
after sourcing the environment:

```sh
source config/local/setenv.sh
unset APP_DIR CATALINA_BASE DATA_DIR INDEX_DIR LVG_DIR SOURCE_DATA_DIR
./gradlew check -x test
./gradlew test --tests '*UnitTest'
```

## Preferred-Name Handlers

`COMPUTE_PREFERRED_NAME_HANDLERS` defaults to `DEFAULT`, which selects
`RrfComputePreferredNameHandler` for NCI MEME source insertions, including
SNOMEDCT_US. This preserves the old NCI production configuration and uses the
project's term-type precedence for preferred-name computation.

During insertion, `PREFNAMES` computes names first for project concepts and then
for the inserted source's concepts. Removing the SNOMED handler overrides
restores RRF for the SNOMED source pass; the `NCIMTH/latest` project pass continues
to use `DEFAULT`. The setting also applies to operations that compute names or
sort atoms directly for SNOMED source concepts.

The native RF2 handler expects RF2 description types and an `acceptabilityId`
language-refset attribute. MEME insertion files use source term types such as
`PT`/`SY` and preserve the attribute name `ACCEPTABILITYID`. Enabling the RF2
handler for this data can fail during the source preferred-name pass. Renaming
the stored attributes or ignoring the missing attribute does not restore the
historical NCI ranking policy.

For deployments that actually load native RF2 data, explicitly set
`COMPUTE_PREFERRED_NAME_HANDLERS=DEFAULT,SNOMEDCT,SNOMEDCT_US`. Restart the
application after changing this setting; handler instances are cached at
initialization. An existing build can restore the NCI setting with the JVM
argument `-Dcompute.preferred.name.handler=DEFAULT` before `-jar`. After restarting,
retry the failed preferred-name process step using the corrected configuration.

## Release QA Research Status

**Status as of October 22, 2026:**

| Work item | Status |
| --- | --- |
| CCS source families | Implemented and complete, confirmed by the operator. |
| Atom publication flags | Research; operational implementation deferred. RADLEX trial rolled back. |
| MRCUI historical concepts | Research; repair and prevention not implemented. |

The atom-flag and MRCUI findings, candidate changes, diagnostic queries, and draft
repair SQL are being preserved for reference. Committing that research does not
mean its fixes have been deployed, database repairs committed, or release QA
passed. Candidate atom-loader code already exists and is invoked by insertion in
this checkout, so a build includes that behavior. Its presence is not evidence
of the application revision running on either server. For atom flags, operational
deployment, database catch-up, and release validation remain deferred.

The operator reported `ROLLBACK` for the RADLEX atom-flag trial after it matched
74 `IS` and 940 `OP` atoms (1,014 total). That trial is not a completed repair.
CVX/VANDF `AB` atoms were excluded from that trial; their earlier audit count was
21,552. They were part of the original QA findings and remain in the proposed
repair scope. These counts are observations, not guaranteed future update counts.
The MRCUI findings likewise do not establish a completed repair or prevention
implementation.

The editing database was identified as `ncimdb` on `ip-172-17-5-22`, with project
39751 (`NCIMTH/latest`). Earlier release troubleshooting also used a schema named
`ncimdb`, without a recorded hostname. Matching schema names or internal IDs do
not establish the server or snapshot. Reconfirm the host, schema, application
revision, and current data before interpreting older results as editing-server
state. The intended future flow is to repair the active editing database and
clone it to the release server before the next release.

The diagnostic files contain read-only queries. The atom-flag repair SQL is a
**research draft, not an implementation runbook**. Keep its schema placeholder and
final `ROLLBACK` while that work is deferred. Future implementation of the deferred
work requires a fresh audit, review of the intended scope, application deployment,
any selected repair, and validation of regenerated release files. The CCS repair
SQL is retained as a reference for the completed work; its preview defaults do
not change the completion status recorded above.

## NCI Release Atom Flags (Research)

**Status: proposed behavior and draft repair; implementation deferred.**

The candidate source-insertion code in this checkout applies the established NCI
publication policy for `NCIMTH` projects: `AB` atoms are suppressible, and `OP`/`IS`
atoms are both suppressible and obsolete. Its intended behavior overrides incoming
source suppression flags for those three term types while preserving existing
`AB` obsolescence and behavior for other term types and projects. This describes
the candidate code, not verified behavior of the running servers.

The candidate atom-loader changes apply the policy before saving new atoms or
updating existing ones, including atoms encountered when resuming an insertion.
The proposed prevention is to retain these overrides across later source loads.
Release generation does not modify atom flags to compensate for loading errors.

Deployment for future insertion cycles is deferred. Published release data and
files are outside the proposed repair scope.

Editing and source insertions completed since the last publication still need a
transition audit in the active working database for the next release. Queries 6
and 7 in the diagnostic SQL inventory all current `NCIMTH/latest` atoms that
violate this policy, including unpublishable content and atoms whose `published`
flag is still true. A modification-date cutoff is not a reliable substitute for
checking the current flags.

If implementation resumes, the proposed transition is to review those candidates,
deploy the insertion fix, and apply a one-time flag-only correction through
application persistence. That catch-up is not implemented by the insertion fix
or the diagnostic SQL. The proposal preserves editorial content, relationships,
workflow status, and publication flags. Replaying source insertions can affect
later edits and is not the proposed transition repair.

The deferred manual SQL proposal is retained in
[`docs/repair-working-atom-publication-flags.sql`](docs/repair-working-atom-publication-flags.sql).
It scopes updates to release-eligible atoms in the audited source versions and
requires an explicit working-schema name. It does not implement the full
transition inventory, which also includes unpublishable content and other source
versions. Its proposed default includes RADLEX and the carried-forward
CVX/VANDF exceptions from the original QA findings; those older exceptions also
need review for the next release. Setting `@include_carried_ab = 0` limits the
proposal to RADLEX. The script ends in `ROLLBACK`; leave that unchanged while
implementation is deferred. A future SQL repair would require stopping the
application and rebuilding the working search indexes afterward, because SQL
bypasses Hibernate Search indexing and application caches. The draft SQL records
modification metadata but does not generate application history records.

Future validation would repeat the audit, recompute preferred names, regenerate
release files, and rerun QA. The proposed fix retains the shared term-type
suppression settings and precedence entries. Read-only diagnostics are available in
[`docs/release-mrrank-suppression-check.sql`](docs/release-mrrank-suppression-check.sql).

## NCI CCS Source Families (Completed)

**Status: implemented and complete, confirmed by the operator on October 22,
2026.**

For `NCIMTH` projects, metadata insertion groups `CCSR_ICD10CM` and
`CCSR_ICD10PCS` in the shared family `CCSR_ICD10PCS`. This carries forward the
historical NCI grouping of `CCSR_10` and `CCS_10` under `CCS_10`, using the
[source names introduced in UMLS 2021AA](https://www.nlm.nih.gov/pubs/techbull/mj21/mj21_umls_2021aa_release.html).
Insertions with the old names retain the historical `CCS_10` family; other
sources and projects keep the incoming family unchanged.

The metadata loader applies the override when creating or updating a root
terminology and logs changes from the supplied family. Incoming `CCS` values in
`sources.src` cannot undo the grouping on a later insertion.

`sources.src` field 7 becomes `root_terminologies.family`, which the release
writer copies directly into MRSAB field 6 (`SF`). The NCI release check requires
each SF to occur in MRSAB field 4 (`RSAB`). Both August 2026 CCSR rows had `CCS`
as SF with no `CCS` RSAB. The February 2026 rows instead had `CCS_10` as SF,
also without a matching RSAB. That historical manual correction would also fail
the current check. The implemented shared family uses the emitted
`CCSR_ICD10PCS` root. The legacy **Fix Snomed Family** AdHoc covers only the
pre-2021 CCS source names.

The SQL for the completed editing-database metadata repair is retained in
[`docs/repair-working-ccs-source-families.sql`](docs/repair-working-ccs-source-families.sql).
It records the update for the two current roots, with an explicit working-schema
guard and a current `CCSR_ICD10PCS` anchor. The archived script retains its schema
placeholder and `ROLLBACK` preview default for reference; the CCS work is complete.
This root metadata change does not require a content-index rebuild. The active
editing database is cloned to the release server before the next release, carrying
the correction forward. Published release files were outside the repair scope.

## NCI MRCUI History (Research)

**Status: investigation only; historical-concept repair and prevention have not
been implemented.** The five-CUI sample belongs to the October 21, 2026 QA rerun
for release `202608`, not the earlier `202602` sample.

Read-only queries and the investigation chronology are retained in
[`docs/release-mrcui-history-check.sql`](docs/release-mrcui-history-check.sql) and
[`docs/release-mrcui-disappearance-check.sql`](docs/release-mrcui-disappearance-check.sql).
Reported results establish four old-CUI histories attached to publishable
`CL...` owners, whose revival is audited to `MATRIXINIT`. An overlapping
`ASSIGNRELEASEIDS` run corroborates later identifier reassignment, but no
per-field identifier-change audit was found. The fifth CUI, `C3495370`, has an
unattached history row; its former owner and cause remain unresolved. These are
findings from the queried database, not verified current editing-server state.

The proposed repair for the four revived owners would preserve their live
concepts and atoms, while attaching the original `DEL` histories to separate
unpublishable historical concepts with the original CUIs and release dates.
Proposed prevention would preserve historical identity during release-ID
reassignment and validate missing or mismatched history owners before export.
No repair routine or prevention change has been implemented for these MRCUI
findings. No regenerated MRCUI or passing QA result is recorded.

## Database And Migrations

Flyway migrations live under `src/main/resources/db.migration`. The normal
administrative tasks are:

- `make migrate` or `./gradlew adminFlywayMigrate`
- `make migrate-info` or `./gradlew adminFlywayInfo`
- `make migrate-validate` or `./gradlew adminFlywayValidate`

Migration `V1.2__maintenance_windows.sql` adds the `maintenance_windows` table
used by the Admin UI to warn process operators about upcoming maintenance.

For an existing populated legacy database, baseline explicitly before applying
later migrations:

```sh
source config/local/setenv.sh
./gradlew adminFlywayBaseline -Pflyway.baseline.confirm=true
```

Fresh database loading, sample data, NCI-META data, and integration test
recipes are documented in
[docs/database-load-and-test-instructions.md](docs/database-load-and-test-instructions.md).
That runbook also contains the known-good NM-306 smoke-test command sequence.
The Flyway rollout details are in
[docs/NM-280-flyway-migration-plan.md](docs/NM-280-flyway-migration-plan.md).

## Additional Docs

- [Configuration migration plan](docs/NM-278-config-migration-plan.md)
- [HikariCP migration plan](docs/NM-279-hikaricp-migration-plan.md)
- [Flyway migration plan](docs/NM-280-flyway-migration-plan.md)
- [Spring Boot integration plan](docs/NM-300-spring-boot-integration-plan.md)
- [Swagger upgrade plan](docs/NM-303-swagger-upgrade-plan.md)
- [Integration test modernization plan](docs/NM-306-integration-test-modernization-plan.md)
- [Database load and test instructions](docs/database-load-and-test-instructions.md)

## License

See [LICENSE.txt](LICENSE.txt).
