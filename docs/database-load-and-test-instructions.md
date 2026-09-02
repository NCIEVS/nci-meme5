# Database Load And Test Instructions

## NM-278 Configuration Model

The normal path is now Spring-style application properties plus environment
variables:

- `src/main/resources/application.properties`
- `config/local/setenv.sh`

Legacy `-Drun.config.*=/path/to/config.properties` files no longer override
the Spring-backed configuration.

For each flow below, set any local overrides first, then source the bootstrap:

```sh
export DB_NAME=<target-db>
source config/local/setenv.sh
```

Use a different `DB_NAME`, `APP_DIR`, `DATA_DIR`, `INDEX_DIR`, or
`SOURCE_DATA_DIR` before sourcing the script when a flow needs a separate
database or filesystem layout. If `DATA_DIR` is changed, set `INDEX_DIR` too.
Set `APP_DIR` before deriving any other paths from it. Preflight expects
`INDEX_DIR` to live under the profile-specific `DATA_DIR`; override only
deliberately with `-Dintegration.it.allowExternalIndexDir=true`.

## Build The Code

```sh
source config/local/setenv.sh
./gradlew explodeWar
```

## Known Good NM-306 Smoke Baseline

These commands were verified locally on May 29, 2026. Run them from the
repository root. The JPA/admin profiles expect Tomcat or Spring Boot to be
stopped. The REST profile expects the app to be running from the same
environment.

Run the repeatable Flyway smoke first. It creates generated disposable schemas,
runs the Flyway integration test, and drops those same schemas afterward:

```sh
make integration-flyway-ephemeral
```

For the sample JPA smoke:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

make prepare-sample
make preflight-sample
make integration-sample
```

For the NCI-META JPA smoke:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbncimeta
export DATA_DIR="$APP_DIR/data_ncimeta"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

make prepare-ncimeta
make preflight-ncimeta
make integration-ncimeta
```

For the REST smoke, start the app in one terminal:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
export SERVER_PORT=18080
export BASE_URL=http://localhost:18080/umls-server-rest
source config/local/setenv.sh

./gradlew bootRun
```

Then run the REST checks from a second terminal with the same database, data,
and `BASE_URL` values:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
export BASE_URL=http://localhost:18080/umls-server-rest
source config/local/setenv.sh

make preflight-rest
make integration-rest
```

If `BASE_URL` uses a host other than `localhost`, `127.0.0.1`, or `::1`, set
`REST_CLIENT_ALLOWED_HOSTS` to that host before sourcing `config/local/setenv.sh`.
If `DB_HOST` points at a host that should not be inferred from the database URL,
set `DB_ALLOWED_HOSTS` explicitly.

For the insertion smoke:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbinsert
export DATA_DIR="$APP_DIR/data_insert"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$DATA_DIR"
source config/local/setenv.sh

make prepare-insertion
make preflight-insertion
make integration-insertion
```

For the bounded admin/load smoke:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbadminload
export DATA_DIR="$APP_DIR/data_adminload"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
export LVG_DIR="$APP_DIR/data_sample/lvg2020"
source config/local/setenv.sh

make prepare-admin
make preflight-admin
make integration-admin
```

## Run The Application With Spring Boot

The normal local startup path no longer requires SmartTomcat or a separately
started Tomcat instance.

```sh
source config/local/setenv.sh
./gradlew bootRun
```

The local bootstrap sets `CATALINA_BASE` to `APP_DIR` by default, and
`bootRun` passes that value into the JVM as `catalina.base` for the Log4j user
activity appender.

Then open:

```text
http://localhost:8080/umls-server-rest
```

To package the executable web artifact:

```sh
source config/local/setenv.sh
./gradlew bootWar
```

When running the packaged executable WAR directly, pass the same base directory
as a JVM system property:

```sh
source config/local/setenv.sh
java -Dcatalina.base="$CATALINA_BASE" -jar build/libs/ROOT-2.0.0-SNAPSHOT-webapp.war
```

## Load The Sample DB

Run these with Tomcat stopped.

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

make prepare-sample
```

Direct Gradle equivalent:

```sh
./gradlew prepareSampleIntegrationData
```

The parent target runs the full sample preparation sequence below. Use the
individual commands only when troubleshooting one step:

```sh
./gradlew adminCreateDb

./gradlew adminLoadSampleRrfUmls

./gradlew adminReindexSample

./gradlew adminGenerateSampleIntegrationData
```

## Sample Tests

Before running the sample JPA profile, verify that the selected environment has
the expected database, data directories, source data directory, LVG directory,
and index directory. The sample preflight expects the documented sample schema
`ncimdbmeta`; running this profile against the default `ncimdb` schema is a
configuration error because it produces cascading fixture-data failures instead
of a meaningful baseline:

```sh
make preflight-sample
```

Run JPA tests with Tomcat stopped:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

make integration-sample
```

Direct Gradle equivalent:

```sh
./gradlew sampleJpaIntegrationTest
```

If a local or CI environment intentionally uses a different name for the loaded
sample fixture database, override the schema guard explicitly:

```sh
./gradlew sampleJpaIntegrationTest \
  -Dintegration.it.expectedSchema.sample-jpa=<loaded-sample-schema>
```

Run REST tests with Tomcat running from the same sourced environment. The REST
preflight expects the sample fixture schema `ncimdbmeta`, and it also probes the
running server for MTH/latest sample content so a server started against the
default `ncimdb` fails before tests run:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

make integration-rest
```

Direct Gradle equivalent:

```sh
./gradlew restIntegrationTest
```

The local REST smoke currently has two explicit skips: the deep-relationships
content test, because those relationships are calculated weekly rather than
loaded by the sample fixture, and the fail-once-and-email process test, because
it requires a reachable SMTP server.

## Load The NCI-META Database

Run these with Tomcat stopped.

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbncimeta
export DATA_DIR="$APP_DIR/data_ncimeta"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

make prepare-ncimeta
```

Direct Gradle equivalent:

```sh
./gradlew prepareNciMetaIntegrationData
```

This target was verified locally against `ncimdbncimeta` on 2026-05-28.

The parent target runs the full NCI-META preparation sequence below. Use the
individual commands only when troubleshooting one step:

```sh
./gradlew adminCreateDb

./gradlew adminLoadNciMetaRrfUmls

./gradlew adminGenerateNciMetaIntegrationData

./gradlew adminReindexNciMeta
```

## NCI-META Tests

Before running the NCI-META JPA profile, verify the selected environment. The
NCI-META preflight expects the documented `ncimdbncimeta` schema unless it is
explicitly overridden:

```sh
make preflight-ncimeta
```

Run JPA tests with Tomcat stopped:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbncimeta
export DATA_DIR="$APP_DIR/data_ncimeta"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

make integration-ncimeta
```

Direct Gradle equivalent:

```sh
./gradlew nciMetaJpaIntegrationTest
```

The refreshed `ncimdbncimeta` baseline passed locally on 2026-05-28:
`make preflight-ncimeta` passed, and `make integration-ncimeta` completed 20
runnable suites and 37 tests.

Before running REST tests, start the application from the same sourced
environment, then verify `BASE_URL`, local directories, loaded project data,
the `ncimdbmeta` schema guard, REST test credentials, and a server-side
MTH/latest sample-content probe:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

make preflight-rest
```

Run a REST smoke test with the application running from the same sourced
environment:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

make integration-rest
```

Direct Gradle equivalent:

```sh
./gradlew restIntegrationTest
```

The `com.wci.umls.server.test.rest.ncimeta.*` meta-editing classes are
currently annotated with JUnit `@Ignore`, so this command selects them but
reports them as skipped unless those classes are deliberately re-enabled:

```sh
./gradlew integrationTest \
  --tests 'com.wci.umls.server.test.rest.ncimeta.*'
```

## Insert Tests

Use the same env-backed model as the NCI-META flow, but choose the insertion
database and data directories that match the insertion test data.

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbinsert
export DATA_DIR="$APP_DIR/data_insert"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$DATA_DIR"
source config/local/setenv.sh
```

Verify the insertion profile prerequisites before running insertion tests. The
insertion preflight expects the documented `ncimdbinsert` schema unless it is
explicitly overridden. If this schema has not already been prepared, this
preflight will fail because the required `NCIMTH/latest` and baseline
`NCI/2016_04D` data are not present yet.

Create, load, generate, and reindex the insertion fixture with one target:

```sh
make prepare-insertion
```

Then verify the prepared fixture:

```sh
make preflight-insertion
```

Direct Gradle equivalent:

```sh
./gradlew prepareInsertionIntegrationData
```

After the fixture is prepared, run:

```sh
make integration-insertion
```

Direct Gradle equivalent:

```sh
./gradlew insertionIntegrationTest
```

This target was verified locally against a freshly prepared `ncimdbinsert`
fixture on 2026-05-29. The profile completed 13 runnable tests.

`InsertionLoaderAlgorithmsIT` calls `MetadataLoaderAlgorithmIT`,
`AtomLoaderAlgorithmIT`, `RelationshipLoaderAlgorithmIT`,
`ContextLoaderAlgorithmIT`, `SemanticTypeLoaderAlgorithmIT`, and
`AttributeLoaderAlgorithmIT`.
`UpdatePublishedAlgorithmIT` exists in the source tree but is not part of the
current runnable baseline because its JUnit `@Test` annotation is still
commented out.

## Flyway Smoke Tests

The preferred Flyway smoke-test path creates run-specific disposable schemas,
runs the opt-in Flyway integration tests, and drops those same generated schemas
afterward:

```sh
make integration-flyway-ephemeral
```

The ephemeral target uses `FLYWAY_IT_HOST`, `FLYWAY_IT_PORT`,
`FLYWAY_IT_SCHEMA_PREFIX`, and `FLYWAY_IT_RUN_ID` to build generated schema
names. The default schema names look like
`ncimdb_nm306_flyway_it_YYYYMMDDHHMMSS` and
`ncimdb_nm306_flyway_base_it_YYYYMMDDHHMMSS`.

For debugging fixed disposable schemas, use the manual prepare/preflight/run
path:

```sh
make prepare-flyway \
  FLYWAY_IT_JDBC_URL='jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_it' \
  FLYWAY_IT_BASELINE_JDBC_URL='jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_base_it' \
  FLYWAY_IT_USER=root
make preflight-flyway \
  FLYWAY_IT_JDBC_URL='jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_it' \
  FLYWAY_IT_BASELINE_JDBC_URL='jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_base_it' \
  FLYWAY_IT_USER=root
make integration-flyway \
  FLYWAY_IT_JDBC_URL='jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_it' \
  FLYWAY_IT_BASELINE_JDBC_URL='jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_base_it' \
  FLYWAY_IT_USER=root
```

The prepare step creates missing disposable schemas but refuses to clean or
overwrite non-empty schemas. The preflight is read-only. It checks Flyway
migration resources, rejects the configured application schema such as `ncimdb`,
rejects using the same schema for both Flyway smoke-test paths, and checks
connectivity and emptiness. If either command reports existing tables, recreate
those schemas manually or point the command at different empty disposable
schemas.

Direct Gradle equivalent:

```sh
./gradlew ephemeralFlywayIntegrationTest \
  -Dflyway.it.enabled=true \
  -Dflyway.it.ephemeral=true \
  -Dflyway.it.runId=20260528123456 \
  -Dflyway.it.jdbcUrl='jdbc:mysql://127.0.0.1:3306/ncimdb_nm306_flyway_it_20260528123456' \
  -Dflyway.it.baselineJdbcUrl='jdbc:mysql://127.0.0.1:3306/ncimdb_nm306_flyway_base_it_20260528123456' \
  -Dflyway.it.user=root
```

## Admin/Loader Preflight

Admin/load/unload tests must use disposable databases. The documented local
target is `ncimdbadminload`; do not point this profile at `ncimdb`,
`ncimdbmeta`, `ncimdbncimeta`, or `ncimdbinsert`.

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbadminload
export DATA_DIR="$APP_DIR/data_adminload"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
export LVG_DIR="$APP_DIR/data_sample/lvg2020"
source config/local/setenv.sh
```

Create the disposable schema if needed, initialize an empty schema, copy loader
support files into `DATA_DIR`, and clear/rebuild indexes:

```sh
make prepare-admin
```

Direct Gradle equivalent:

```sh
./gradlew prepareAdminLoaderIntegrationData
```

After preparing the disposable fixture, run:

```sh
make preflight-admin
```

This preflight checks connectivity, local directories, the disposable schema
guard, bundled loader source files, and the copied `acronyms.txt` /
`spelling.txt` support files.

```sh
make integration-admin
```

Direct Gradle equivalent:

```sh
./gradlew adminLoaderIntegrationTest
```

The current admin-loader smoke profile is intentionally RRF-only:
`RrfSingleLoadAndUnloadIT` and `RrfUmlsLoadAndUnloadIT`. It passed locally
against `ncimdbadminload` on 2026-05-29. The RF2 comparison, RF2 snapshot, OWL,
and ClaML classes have Gradle-era test bodies but are annotated with
`@Ignore` because they currently exceed the smoke-test runtime or memory budget.
