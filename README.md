# NCI MEME 5

NCI MEME 5 is the UMLS Terminology Server codebase used for MEME-style
terminology editing, metadata loading, release support, validation, and REST UI
workflows. The application is a Java 17 Gradle project that packages a WAR and
can also run locally through Spring Boot.

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

Check integration-test prerequisites before running a profile:

```sh
make preflight-sample
make preflight-ncimeta
make preflight-rest
make preflight-insertion
make preflight-admin
```

The sample, REST, NCI-META, insertion, and admin-loader preflights verify that
the selected database matches the documented fixture schema. For example,
`sample-jpa` and `rest` expect `ncimdbmeta`, not the default `ncimdb`; the
admin-loader profile expects disposable `ncimdbadminload`. REST preflight also
probes the running server for sample fixture data so an app started against the
wrong database fails before the REST test classes run.

Prepare the sample integration-test database fixture:

```sh
make prepare-sample
```

Prepare the NCI-META integration-test database fixture:

```sh
make prepare-ncimeta
```

Prepare the insertion integration-test database fixture:

```sh
make prepare-insertion
```

Prepare the disposable admin/load integration-test database fixture:

```sh
make prepare-admin
```

Run named integration-test profiles from the same sourced environment:

```sh
make integration-sample
make integration-ncimeta
make integration-rest
make integration-insertion
make integration-admin
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

## Database And Migrations

Flyway migrations live under `src/main/resources/db.migration`. The normal
administrative tasks are:

- `make migrate` or `./gradlew adminFlywayMigrate`
- `make migrate-info` or `./gradlew adminFlywayInfo`
- `make migrate-validate` or `./gradlew adminFlywayValidate`

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
