# Agent Notes For NCI MEME 5

## Project Shape

NCI MEME 5 is a Java 17 Gradle project for MEME-style terminology editing,
metadata loading, validation, release support, and REST UI workflows.

Use the checked-in Gradle wrapper. Prefer Makefile targets when available.

Key paths:
- `src/main/java` - application, REST services, admin utilities, algorithms
- `src/main/resources` - application properties, Flyway migrations, JPA metadata
- `src/main/webapp` - legacy web UI assets
- `src/test/java` - unit and integration tests
- `config/local/setenv.sh` - local environment bootstrap
- `config/prod-nci-meta` - production overlays and operational scripts
- `docs/database-load-and-test-instructions.md` - integration test runbook

## Configuration Rules

Runtime config comes from:
- `src/main/resources/application.properties`
- environment variables, usually via `config/local/setenv.sh`

Do not add new legacy `config.properties` files. Add runtime defaults to
`application.properties` and environment-specific values to env setup scripts.

Hibernate/JPA and Flyway are intentionally wired manually. Do not enable Spring
Boot JDBC, JPA, transaction-manager, or Flyway auto-configuration unless the
ticket explicitly asks for that migration.

## Local Environment

Before direct Gradle commands, source:

```sh
source config/local/setenv.sh
```

The Makefile does this automatically for most targets.

Default local DB values are `127.0.0.1:3306`, database `ncimdb`, user `root`.
Many integration profiles require different DB names, data dirs, and index dirs.
Check the runbook before running integration or admin/load tasks.

## Common Commands

```sh
make build
make test
make quality
make run
```

Focused unit test:

```sh
make test UNIT_TEST_PATTERN='*MigrationUtilityUnitTest'
```

Direct Gradle equivalents are okay after sourcing `config/local/setenv.sh`.

For deterministic unit/quality runs, clear path overrides as documented in
`README.md`.

## Integration Tests

Always run the matching preflight before an integration profile.

```sh
make preflight-sample
make preflight-ncimeta
make preflight-rest
make preflight-insertion
make preflight-admin
make integration-flyway-ephemeral
```

Do not run `prepare-*`, `integration-*`, admin loader, Flyway migration, or
database mutation tasks unless the requested work needs them and the target DB is
confirmed.

REST integration tests require the app to be running from the same environment
as the test command.

## Database And Flyway

Flyway migrations live in:

```text
src/main/resources/db.migration
```

Use versioned migrations for schema evolution. Normal admin tasks:

```sh
make migrate
make migrate-info
make migrate-validate
```

Baseline existing populated legacy databases only with explicit intent:

```sh
./gradlew adminFlywayBaseline -Pflyway.baseline.confirm=true
```

## Production And Operational Scripts

Files under `config/prod-nci-meta/src/main/resources/bin` are operational
scripts. Treat them as production-sensitive.

Do not run scripts that stop services, call AWS, snapshot/restore RDS, modify
S3, or touch production-like databases unless explicitly instructed.

## Code Style

Java source is UTF-8 and targets Java 17.

Static checks are Checkstyle and SpotBugs:

```sh
make quality
```

Keep edits scoped. Prefer existing project patterns, especially around legacy
JAX-RS/Jersey services, admin utilities, manual persistence setup, and Spring
Boot compatibility bridges.

## Documentation

When changing config, database setup, migrations, integration tests, deployment,
or operational scripts, update the relevant README or runbook in the same
change.
