# NM-280 Flyway Migration Plan

## Summary

Introduce Flyway for managing MEME database schema and data migrations.

The implementation should use `/Users/deborahshapiro/Code/wci-common-java` as
the reference project. In particular, follow its pattern of managing Flyway and
Hibernate manually from loaded application properties, rather than relying on
Spring Boot persistence auto-configuration.

## Background

MEME currently relies on Hibernate schema generation and admin utilities to
create or update the database schema. Flyway adds a versioned migration history
so schema and data changes can be reviewed, applied consistently, validated,
and tracked across local, test, and deployed environments.

For legacy databases, Flyway should not blindly replay a baseline schema
creation script over an existing populated database. The migration strategy must
distinguish between fresh databases and already-existing databases.

## Reference Implementation

Use `wci-common-java` as the house-style reference:

- `/Users/deborahshapiro/Code/wci-common-java/build.gradle`
  - adds `org.flywaydb:flyway-core`
  - adds a database-specific Flyway module
  - comments that Flyway and Hibernate are managed outside Spring
- `/Users/deborahshapiro/Code/wci-common-java/src/main/java/com/wci/util/MigrationUtility.java`
  - loads database connection properties from application configuration
  - configures Flyway manually
  - uses migration locations `classpath:db.migration` and
    `classpath:db/migration`
  - calls `flyway.migrate()`
- `/Users/deborahshapiro/Code/wci-common-java/src/test/java/com/wci/test/AbstractHibernateTest.java`
  - runs Flyway in test setup when Hibernate is not creating the schema
- `/Users/deborahshapiro/Code/wci-common-java/src/main/resources/db.migration/V1.0__wci_common_java.sql`
  - stores versioned SQL migrations under `src/main/resources/db.migration`

## Current MEME Touchpoints

- `build.gradle`
  - no Flyway dependencies yet
  - has `adminCreateDb` and `adminUpdateDb` tasks that call `UpdateDb`
- `src/main/java/com/wci/umls/server/admin/UpdateDb.java`
  - sets `hibernate.hbm2ddl.auto` to `create` or `update`
  - triggers JPA initialization to apply schema changes
- `src/main/java/com/wci/umls/server/admin/AbstractLoader.java`
  - recreates the database schema through JPA before some load flows
- `src/main/java/com/wci/umls/server/rest/impl/ConfigureServiceRestImpl.java`
  - initializes/reset databases by setting `hibernate.hbm2ddl.auto=create`
- `src/main/resources/application.properties`
  - central Spring-style property bridge for database connection settings
- `src/main/resources/META-INF/persistence.xml`
  - defines the `TermServiceDS` persistence unit
- `docs/NM-278-config-migration-plan.md`
  - states that Hibernate and Flyway should stay manually wired, matching the
    `wci-common-java` approach

## Recommended Approach

Start with manual Flyway integration and documented Gradle/admin tasks.

Do not enable Spring Boot Flyway auto-configuration in this ticket. MEME is a
Tomcat WAR with existing manual JPA factory creation, and NM-278 deliberately
kept persistence wiring outside Spring-managed JDBC/JPA auto-configuration.

Automatic migration at application startup can be added behind an explicit
configuration flag if needed, but the safer initial path is a documented admin
task/script.

## Implementation Plan

### 1. Add Flyway dependencies

In `build.gradle`, add:

```gradle
implementation "org.flywaydb:flyway-core:<version>"
implementation "org.flywaydb:flyway-mysql:<version>"
```

Use the `wci-common-java` Flyway version as the first candidate, then verify
compatibility with this project's Java 17, MySQL connector, and existing
dependency graph.

If the selected Flyway version does not require a separate MySQL module, document
that explicitly in the PR.

Initial implementation note: the branch started from the `wci-common-java`
version candidate, then upgraded to Flyway `12.6.1` after confirming both
`flyway-core` and `flyway-mysql` are available at that release and the admin
info command runs cleanly against local MySQL 8.4.

### 2. Add migration resources

Create:

```text
src/main/resources/db.migration/
```

Add:

```text
V1.0__baseline_current_schema.sql
V1.1__example.sql
```

The baseline migration should represent the current schema for a fresh MEME
database. The sample migration should be intentionally tiny and low-risk, such
as creating and dropping a harmless verification object, or adding a small
Flyway-only metadata table used only in test verification if appropriate.

Avoid sample changes that alter application behavior.

### 3. Define baseline strategy

Document two paths:

- fresh database
  - run Flyway migrate from version `1.0`
  - `V1.0__baseline_current_schema.sql` creates the current schema
- existing legacy database
  - do not run the baseline schema creation script
  - use a Flyway baseline operation to mark the DB as already being at version
    `1.0`
  - later migrations apply from `V1.1` onward

The legacy baseline flow should be explicit and operationally hard to run by
accident.

Initial implementation note: `adminFlywayBaseline` requires an explicit
confirmation flag:

```bash
./gradlew adminFlywayBaseline -Pflyway.baseline.confirm=true
```

Use this only for an existing populated legacy database that already represents
the `1.0` schema.

### 4. Add a MEME MigrationUtility

Create a utility modeled after `wci-common-java`:

```text
src/main/java/com/wci/umls/server/helpers/MigrationUtility.java
```

Responsibilities:

- load properties from `ConfigUtility.getConfigProperties()`
- read database connection settings
- configure Flyway locations:
  - `classpath:db.migration`
  - `classpath:db/migration`
- expose operations:
  - `migrate`
  - `info`
  - `validate`
  - `baseline`

Connection property priority:

- after NM-279, prefer:
  - `hibernate.hikari.jdbcUrl`
  - `hibernate.hikari.username`
  - `hibernate.hikari.password`
- during transition, support existing:
  - `jakarta.persistence.jdbc.url`
  - `jakarta.persistence.jdbc.user`
  - `jakarta.persistence.jdbc.password`

### 5. Add admin entry point and Gradle tasks

Add a small admin class, for example:

```text
src/main/java/com/wci/umls/server/admin/FlywayDb.java
```

Support a system property such as:

```text
-Dflyway.command=migrate|info|validate|baseline
```

Add Gradle tasks:

```text
adminFlywayMigrate
adminFlywayInfo
adminFlywayValidate
adminFlywayBaseline
```

These should use the same config/env conventions as existing admin tasks.
`adminFlywayBaseline` should remain guarded by
`flyway.baseline.confirm=true`; fresh databases should use
`adminFlywayMigrate` instead.

### 6. Add configuration flags

In `src/main/resources/application.properties`, add:

```properties
spring.flyway.enabled=false
flyway.locations=classpath:db.migration,classpath:db/migration
flyway.baseline.version=${FLYWAY_BASELINE_VERSION:1.0}
flyway.baseline.description=${FLYWAY_BASELINE_DESCRIPTION:Baseline current MEME schema}
flyway.migrate.on.startup=${FLYWAY_MIGRATE_ON_STARTUP:false}
```

`spring.flyway.enabled=false` documents that Spring Boot is not managing Flyway.

`flyway.migrate.on.startup` should default to false unless the team decides the
acceptance criteria require application startup migration. A documented Gradle
task/script satisfies the ticket as written.

### 7. Decide startup behavior

Chosen initial behavior:

- migrations are run by explicit Gradle/admin task
- application startup does not mutate schema by default

Rationale:

- MEME does not currently define a normal runtime default for
  `hibernate.hbm2ddl.auto`; it is set imperatively only by transitional admin,
  loader, and configure/reset flows.
- Running Flyway automatically on startup would add schema mutation to the
  Tomcat WAR startup path before the baseline strategy has been fully exercised
  across fresh and existing legacy databases.
- Explicit Gradle/admin execution is operationally clearer for the first Flyway
  rollout and satisfies the initial acceptance path.

Future optional guarded startup behavior:

- if `flyway.migrate.on.startup=true`, run Flyway before the first
  `EntityManagerFactory` is created
- this likely belongs in or immediately before `RootServiceJpa.init()`
- startup migration must happen once per JVM and must fail fast on migration
  errors

If startup migration is implemented in a future ticket, it should not coexist with
`hibernate.hbm2ddl.auto=update` in normal runtime configuration.

### 8. Transition existing Hibernate schema tools

Keep these flows initially:

- `adminCreateDb`
- `adminUpdateDb`
- configure/reset flows that use `hibernate.hbm2ddl.auto`

But document them as legacy schema-management paths.

Update docs to say:

- use Flyway for schema evolution
- use Hibernate create/update only for transitional local/test bootstrap until
  the baseline migration is proven
- do not run Hibernate update and Flyway migrate as competing production schema
  management mechanisms

Initial implementation note:

- `adminCreateDb` and `adminUpdateDb` remain available but their Gradle task
  descriptions and `UpdateDb` runtime log now identify them as legacy
  transitional Hibernate schema tools.
- Loader and configure/reset database recreation paths remain unchanged, with
  comments marking them as transitional local/test bootstrap flows.
- New schema changes should be added as Flyway migrations under
  `src/main/resources/db.migration` and applied with `adminFlywayMigrate`.

### 9. Tests and verification

Add focused coverage:

- unit test Flyway config resolution from `application.properties`
- unit test database connection property fallback:
  - Hikari keys when present
  - `jakarta.persistence.jdbc.*` keys during transition
- integration smoke test against a local/test MySQL database:
  - start with empty DB
  - run `adminFlywayMigrate`
  - confirm `flyway_schema_history` exists
  - create a JPA service and run a simple query
- legacy baseline smoke test:
  - create schema using existing `adminCreateDb`
  - run `adminFlywayBaseline`
  - run `adminFlywayValidate`

Initial implementation note:

- `MigrationUtilityUnitTest` covers application Flyway defaults, Hikari-first
  connection resolution, Jakarta JDBC fallback, location parsing, and Flyway
  safety defaults such as `baselineOnMigrate=false` and `cleanDisabled=true`.
- `FlywayMigrationIT` adds opt-in MySQL smoke coverage for:
  - migrating a fresh empty schema, confirming `flyway_schema_history`, and
    bootstrapping a JPA service
  - marking a legacy schema as baseline, applying later migrations, and
    validating the Flyway state
- `FlywayMigrationIT` is skipped unless `-Dflyway.it.enabled=true` and a
  disposable empty schema JDBC URL are provided. It intentionally fails if the
  target schema is not empty before it mutates anything.
- Local verification ran the opt-in MySQL smoke tests against disposable
  `ncimdb_flyway_it` and `ncimdb_flyway_base_it` schemas. The Gradle XML report
  showed `tests="2" skipped="0" failures="0" errors="0"` for
  `FlywayMigrationIT`.

Useful commands:

```bash
./gradlew test
./gradlew dependencies --configuration runtimeClasspath
./gradlew adminFlywayInfo
./gradlew adminFlywayMigrate
./gradlew adminFlywayValidate
```

With local environment:

```bash
source config/local/setenv.sh
./gradlew adminFlywayMigrate
./gradlew integrationTest --tests "com.wci.umls.server.test.jpa.CloseReopenFactoryIT"
```

Opt-in disposable MySQL smoke tests:

```bash
./gradlew integrationTest --tests "*.FlywayMigrationIT" \
  -Dflyway.it.enabled=true \
  -Dflyway.it.jdbcUrl=jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_it \
  -Dflyway.it.baselineJdbcUrl=jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_base_it \
  -Dflyway.it.user=root
```

Both schemas must exist and be empty before running that command.

## Acceptance Criteria Mapping

- Flyway dependencies are added and integrated with the application.
  - add `flyway-core`
  - add MySQL Flyway support if required
  - add MEME `MigrationUtility`
  - add admin/Gradle commands
- Baseline migration is created to represent current schema.
  - add `V1.0__baseline_current_schema.sql`
  - document fresh DB vs legacy DB strategy
- A sample versioned migration is created and verified in a test environment.
  - add `V1.1__example.sql`
  - verify with `adminFlywayMigrate` and `adminFlywayValidate`
- Application startup runs Flyway migrations automatically, or via documented
  Gradle task/script.
  - chosen initial behavior is documented `adminFlywayMigrate`
  - startup does not mutate schema by default
  - future optional behavior can add guarded startup migration through
    `flyway.migrate.on.startup=true`

## Watchpoints

- The baseline SQL is the riskiest part. Generate it from the current Hibernate
  schema, review it, and test it on an empty MySQL database.
- Avoid running destructive baseline SQL against legacy populated databases.
- Do not let Hibernate `hbm2ddl.auto=update` and Flyway both manage schema in
  the same deployed path.
- Coordinate with NM-279. Once HikariCP is in place, Flyway should use
  `hibernate.hikari.jdbcUrl`, `hibernate.hikari.username`, and
  `hibernate.hikari.password`, matching `wci-common-java`.
- Some existing admin/configure flows recreate the schema through Hibernate.
  Those flows need either transitional documentation or future refactoring to
  call Flyway instead.
