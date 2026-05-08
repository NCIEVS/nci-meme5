# NM-279 HikariCP Migration Plan

## Summary

Replace the legacy c3p0 connection pool with HikariCP for the MEME Hibernate
JPA persistence layer.

The implementation should follow the Hibernate-managed HikariCP pattern already
used in `/Users/deborahshapiro/Code/wci-common-java`, adapted for this
application's MySQL configuration and existing Spring-style property bridge.

## Background

Connection pooling keeps a reusable set of database connections open so the
application does not create and tear down a new JDBC connection for every
operation. Hibernate requests a connection from the configured pool when an
`EntityManager` needs to execute SQL, then returns it when the work is complete.

Pooling is needed here because MEME serves concurrent REST and admin workflows,
and each workflow may run many Hibernate queries. A bounded pool improves
performance, limits database connection pressure, validates stale connections,
and can detect code paths that fail to return connections.

HikariCP is the modern replacement for c3p0. It is faster, simpler to tune,
actively maintained, and is the preferred/default pool in current Spring Boot
applications.

## Reference Implementation

Use `wci-common-java` as the house-style reference:

- `/Users/deborahshapiro/Code/wci-common-java/build.gradle`
  - uses `org.hibernate.orm:hibernate-hikaricp:6.6.3.Final`
  - excludes the transitive `HikariCP` dependency
  - pins `com.zaxxer:HikariCP:6.2.1`
- `/Users/deborahshapiro/Code/wci-common-java/src/main/resources/application.properties`
  - sets `hibernate.connection.provider_class=org.hibernate.hikaricp.internal.HikariCPConnectionProvider`
  - configures `hibernate.hikari.jdbcUrl`, `username`, `password`,
    `connectionTestQuery`, `maximumPoolSize`, `minimumIdle`, `idleTimeout`,
    `connectionTimeout`, and `poolName`
- `/Users/deborahshapiro/Code/wci-common-java/src/main/java/com/wci/util/MigrationUtility.java`
  - treats `hibernate.hikari.*` as the canonical database connection config

## Current MEME Touchpoints

- `build.gradle`
  - currently depends on `org.hibernate.orm:hibernate-c3p0:${hibernateVersion}`
- `src/main/resources/application.properties`
  - currently has c3p0 settings under `hibernate.c3p0.*`
  - currently uses `hibernate.connection.provider_class=org.hibernate.c3p0.internal.C3P0ConnectionProvider`
- `config/local/setenv.sh`
  - exports c3p0-era pool tuning variables
- `src/main/java/com/wci/umls/server/jpa/services/RootServiceJpa.java`
  - creates the static `EntityManagerFactory`
  - passes resolved config properties into Hibernate
- `src/main/resources/META-INF/persistence.xml`
  - defines the `TermServiceDS` persistence unit
- `src/main/java/com/wci/umls/server/rest/impl/SessionFactoryShutdownListener.java`
  - closes the `EntityManagerFactory` during application shutdown
- `src/main/java/com/wci/umls/server/jpa/algo/release/ReloadMrcuiTableAlgorithm.java`
  - directly uses `DriverManager.getConnection(...)`, bypassing Hibernate and
    any configured pool

## Implementation Plan

### 1. Update dependencies

In `build.gradle`:

- remove `org.hibernate.orm:hibernate-c3p0:${hibernateVersion}`
- add `org.hibernate.orm:hibernate-hikaricp:${hibernateVersion}`
- follow the `wci-common-java` dependency pattern if compatible:

```gradle
implementation ("org.hibernate.orm:hibernate-hikaricp:${hibernateVersion}") {
    exclude group: "com.zaxxer", module: "HikariCP"
}
implementation "com.zaxxer:HikariCP:6.2.1"
```

Before finalizing the exact HikariCP version, verify dependency compatibility
with MEME's current logging stack. MEME currently uses
`org.slf4j:slf4j-log4j12:1.7.36`, while HikariCP versions may vary in their
SLF4J expectations.

### 2. Replace c3p0 properties with HikariCP properties

In `src/main/resources/application.properties`, replace the c3p0 block with:

```properties
#
# HikariCP config
#
hibernate.connection.provider_class=org.hibernate.hikaricp.internal.HikariCPConnectionProvider
hibernate.hikari.jdbcUrl=${jakarta.persistence.jdbc.url}
hibernate.hikari.username=${jakarta.persistence.jdbc.user}
hibernate.hikari.password=${jakarta.persistence.jdbc.password}
hibernate.hikari.connectionTestQuery=SELECT 1
hibernate.hikari.maximumPoolSize=${DB_MAX_POOL_SIZE:64}
hibernate.hikari.minimumIdle=${DB_MIN_POOL_SIZE:5}
hibernate.hikari.idleTimeout=${DB_IDLE_TIMEOUT_MS:3600000}
hibernate.hikari.connectionTimeout=${DB_CONNECTION_TIMEOUT_MS:30000}
hibernate.hikari.maxLifetime=${DB_MAX_LIFETIME_MS:3600000}
hibernate.hikari.validationTimeout=${DB_VALIDATION_TIMEOUT_MS:5000}
hibernate.hikari.leakDetectionThreshold=${DB_LEAK_DETECTION_THRESHOLD_MS:0}
hibernate.hikari.poolName=${DB_POOL_NAME:NciMemeHikariCPPool}
```

Keep the existing `jakarta.persistence.jdbc.*` keys because they are still read
by application code and tests.

### 3. Update local environment variables

In `config/local/setenv.sh`:

- remove c3p0-specific variables:
  - `DB_ACQUIRE_INCREMENT`
  - `DB_IDLE_TEST_PERIOD`
  - `DB_MAX_CONNECTION_AGE`
- keep common pool size variables:
  - `DB_MAX_POOL_SIZE`
  - `DB_MIN_POOL_SIZE`
- add HikariCP variables:
  - `DB_IDLE_TIMEOUT_MS`
  - `DB_CONNECTION_TIMEOUT_MS`
  - `DB_MAX_LIFETIME_MS`
  - `DB_VALIDATION_TIMEOUT_MS`
  - `DB_LEAK_DETECTION_THRESHOLD_MS`
  - `DB_POOL_NAME`

Use millisecond-based names for timeout variables to make the unit explicit.
c3p0 settings were effectively second-based; HikariCP settings are
millisecond-based.

### 4. Keep current JPA wiring

Do not introduce Spring Boot datasource auto-configuration as part of NM-279.

MEME currently creates the `EntityManagerFactory` manually in `RootServiceJpa`
and passes resolved config properties directly into Hibernate. This is
compatible with Hibernate-managed HikariCP and keeps the ticket scoped to the
connection pool replacement.

Leave `persistence.xml` unchanged unless runtime testing shows Hibernate needs
additional provider hints.

### 5. Audit direct JDBC usage

`ReloadMrcuiTableAlgorithm` currently calls `DriverManager.getConnection(...)`
directly. This bypasses the connection pool.

For NM-279, decide whether to:

- leave it as an explicit exception because it is a specialized release loader,
  and document it as a follow-up cleanup, or
- refactor it to use the existing Hibernate/EntityManager connection path so it
  participates in pooling.

This should be called out in the PR because the acceptance criteria say HikariCP
is configured for all data sources.

### 6. Add regression coverage

Add or update focused tests:

- unit test config resolution:
  - HikariCP keys resolve from `application.properties`
  - c3p0 keys are absent
- dependency/classpath verification:
  - `hibernate-hikaricp` is present
  - `hibernate-c3p0` and c3p0 jars are absent
- integration smoke test:
  - create a JPA service
  - run a simple DB query/workflow
  - close the service
  - close and reopen the factory
- optional provider assertion:
  - inspect the Hibernate service registry/session metadata if there is a clean
    way to confirm the active connection provider is HikariCP

Existing useful smoke coverage:

- `src/test/java/com/wci/umls/server/test/jpa/CloseReopenFactoryIT.java`

### 7. Verify locally

Run:

```bash
./gradlew test
./gradlew dependencies --configuration runtimeClasspath
./gradlew dependencyInsight --configuration runtimeClasspath --dependency HikariCP
```

Confirm:

- `hibernate-hikaricp` is present
- `hibernate-c3p0` is absent
- c3p0 jars are absent
- HikariCP resolves to the intended version
- no unexpected logging dependency changes are introduced

With local database environment loaded:

```bash
source config/local/setenv.sh
./gradlew integrationTest --tests "com.wci.umls.server.test.jpa.CloseReopenFactoryIT"
```

If a local Tomcat/server smoke test is available, also verify startup and a
basic REST workflow.

## Acceptance Criteria Mapping

- HikariCP is added and configured as the connection pool for all data sources.
  - Add `hibernate-hikaricp`
  - configure `hibernate.connection.provider_class`
  - configure `hibernate.hikari.*`
  - audit direct JDBC paths
- c3p0 dependencies are removed from the build.
  - remove `hibernate-c3p0`
  - verify runtime classpath
- Connection pool configuration is externalized via Spring config/env vars.
  - keep settings in `application.properties`
  - use env-backed placeholders
  - update `config/local/setenv.sh`
- Basic performance and regression tests confirm no connection leaks or regressions.
  - run unit tests
  - run JPA integration smoke test
  - use optional Hikari leak detection threshold for local/test smoke runs

## Watchpoints

- Remove every `hibernate.c3p0.*` key. Leaving c3p0 keys behind can confuse
  provider selection and hides dead configuration.
- HikariCP timeout values are milliseconds.
- If HikariCP 6.2.1 causes logging dependency friction, either align exclusions
  with `wci-common-java` or choose the newest HikariCP version compatible with
  MEME's SLF4J 1.7 logging stack.
- `ReloadMrcuiTableAlgorithm` bypasses Hibernate pooling today and may need a
  follow-up or in-ticket refactor depending on the final interpretation of "all
  data sources."
