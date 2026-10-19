# NCI MEME 5

NCI MEME 5 is the UMLS Terminology Server codebase used for MEME-style
terminology editing, metadata loading, release support, validation, and REST UI
workflows. The application is a Java 17 Gradle project that packages a WAR and
can also run locally through Spring Boot.

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

## NCI Release Atom Flags

For `NCIMTH` projects, source insertion automatically applies the established
NCI publication policy: `AB` atoms are suppressible, and `OP`/`IS` atoms are both
suppressible and obsolete. This overrides incoming source suppression flags for
those three term types. Existing `AB` obsolescence, other term types, and projects
with other terminologies retain their existing behavior.

The atom loader applies this policy before saving new atoms or updating existing
ones, including atoms encountered when resuming an insertion. Subsequent source
loads cannot undo these NCI overrides. Release generation does not modify atom
flags to compensate for loading errors.

Deploy this fix for future insertion cycles. Published release data and files
remain unchanged and do not require retroactive correction.

Editing and source insertions completed since the last publication still need a
transition audit in the active working database for the next release. Queries 6
and 7 in the diagnostic SQL inventory all current `NCIMTH/latest` atoms that
violate this policy, including unpublishable content and atoms whose `published`
flag is still true. A modification-date cutoff is not a reliable substitute for
checking the current flags.

The transition plan is to review those candidates, deploy the insertion fix, and
apply a one-time flag-only correction through application persistence. That
catch-up is not implemented by the insertion fix or the diagnostic SQL. Preserve
all editorial content, relationships, workflow status, and publication flags;
keep the frozen published database and release files untouched. Replaying source
insertions is not the transition repair because it can affect later edits.

For a manual SQL correction, see
[`docs/repair-working-atom-publication-flags.sql`](docs/repair-working-atom-publication-flags.sql).
It scopes the update to the audited source versions and requires an explicit
working-schema name. Choose RADLEX only or also include the carried-forward
CVX/VANDF exceptions. The script ends in `ROLLBACK` for preview; change that to
`COMMIT` only for the intended repair. Stop the application for the SQL update
and rebuild the working search indexes afterward, because SQL bypasses Hibernate
Search indexing and application caches. The SQL records modification metadata
but does not generate application history records.

After correction, repeat the audit, recompute preferred names, and generate the
next release normally. Keep the existing shared term-type suppression settings
and precedence entries. Read-only diagnostics are available in
[`docs/release-mrrank-suppression-check.sql`](docs/release-mrrank-suppression-check.sql).

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
