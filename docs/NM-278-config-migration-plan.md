# NM-278 Config Migration Plan

## Scope

This plan is for `NM-278`, which standardizes configuration using Spring and Spring Boot conventions without doing a full Spring rewrite.

This effort is **not** modeled after `evsrestapi`.

This effort **is** modeled after:

- `/Users/deborahshapiro/Code/wci-common-java`

This effort is specifically intended to:

- move configuration into `application.properties`
- wire environment-specific and sensitive values through environment variables or injected config
- defer Spring profile files until there is a concrete need for profile-specific overrides
- add a `setenv.sh` file for local environment setup
- preserve the current manual Hibernate wiring
- preserve the current manual Flyway wiring

This effort is **not** intended, at this stage, to:

- move Hibernate management to Spring Data / Spring ORM
- move Flyway management to Spring Boot autoconfiguration
- fully rewrite the REST stack
- fully rewrite application bootstrapping and internals in one pass

## Story Summary

This story standardizes configuration using Spring and Spring Boot conventions, moving configuration into `application.properties` and wiring environment-specific values to environment variables.

## Acceptance Criteria

- Configuration values currently in property files are mapped into Spring Boot-style configuration (`application.properties` plus environment overrides).
- Sensitive or environment-specific settings (DB URL, user/password, pools, feature flags, etc.) are externalized via environment variables or config injection.
- Application starts successfully using only externalized configuration (no hard-coded environment-specific values).
- Clear documentation exists on required env vars and how to override them per environment (dev/test/prod).
- Legacy, unused configuration files are removed or marked deprecated.

## Current State In This Repo

The current configuration model is centered on `ConfigUtility`.

Relevant file:

- [ConfigUtility.java](/Users/deborahshapiro/Code/workspace-meme/nci-meme5/src/main/java/com/wci/umls/server/helpers/ConfigUtility.java:221)

Today, configuration resolution works roughly like this:

1. determine a config label using `label.prop`
2. look for a JVM property like `-Drun.config.<label>=...`
3. if not found, fall back to classpath `/config.properties`
4. if not found, fall back to `~/.term-server/<label>/config.properties`

Important methods in `ConfigUtility`:

- `getConfigLabel()`
- `getConfigProperties()`
- `getLocalConfigFile()`
- `getHomeDirs()`

The build and admin/test workflows currently reinforce that model.

Relevant file:

- [build.gradle](/Users/deborahshapiro/Code/workspace-meme/nci-meme5/build.gradle:183)

Current examples include:

- `-Drun.config.umls=...`
- `-Drun.config.label=...`

Legacy `config.properties` and `config-demo.properties` files have been
removed from the tracked repo as part of the cleanup.

Current operational assumption:

- `config/prod-nci-meta` appears to be the one remaining packaged config
  family that still matters operationally at this stage of the migration.
- Other packaged config families have now been removed as part of the
  `NM-278` cleanup, leaving `prod-nci-meta` as the remaining packaged
  legacy config path.

Representative config packaging still exists under assembly descriptors such as:

- [config/prod-nci-meta/src/main/assembly/config.xml](/Users/deborahshapiro/Code/workspace-meme/nci-meme5/config/prod-nci-meta/src/main/assembly/config.xml:1)

## Reference Project Pattern

The reference project for this story is:

- `/Users/deborahshapiro/Code/wci-common-java`

The important thing about `wci-common-java` is not “copy everything,” but “follow its configuration model.”

Relevant reference files:

- `/Users/deborahshapiro/Code/wci-common-java/src/main/resources/application.properties`
- `/Users/deborahshapiro/Code/wci-common-java/src/main/resources/application-deploy.properties`
- `/Users/deborahshapiro/Code/wci-common-java/src/main/java/com/wci/config/PropertiesConfiguration.java`
- `/Users/deborahshapiro/Code/wci-common-java/src/main/java/com/wci/Application.java`

Key patterns from `wci-common-java`:

- Spring Boot property files are the primary configuration source
- environment variables are injected with `${ENV_VAR:default}` placeholders
- Spring profiles separate local/test/deploy behavior
- Spring Boot is used for configuration and application bootstrap conventions
- Spring auto-configuration for JDBC/JPA can be excluded when manual Hibernate wiring is desired
- a helper configuration layer can flatten Spring environment properties into an application-specific structure

This is the model to follow for `NM-278`.

## Target Architecture For This Story

The target state for this story is:

- `application.properties` contains shared defaults and common settings
- profile-specific property files are deferred until they represent real configuration profiles
- secrets and environment-specific settings are injected via environment variables
- Hibernate remains wired manually
- Flyway remains wired manually
- code that currently depends on `ConfigUtility.getConfigProperties()` keeps working through a compatibility bridge during migration

The key idea is:

- Spring Boot becomes the configuration system
- existing application code continues to receive a `Properties` object until later refactors are desired

## What This Story Should Not Try To Solve

To keep `NM-278` focused, this story should not expand into:

- converting JAX-RS resources into Spring MVC controllers
- replacing manual service wiring everywhere
- introducing Spring Data repositories
- changing persistence strategy
- changing Flyway invocation strategy
- full deployment modernization

Those may happen later, but they should not be coupled to this configuration story.

## Deferred Profiles

Possible future profiles:

- `sample`
- `ncimeta`
- `insert`
- `local`
- `test`
- `prod`

However, those should only become Spring profiles if they truly represent application configuration shapes.

If they are really dataset or workflow modes rather than application environment profiles, it may be better to keep them as runtime inputs or task arguments rather than Spring profiles.

## Proposed File Layout

Planned configuration files:

- `src/main/resources/application.properties`

Possibly later:

- `src/main/resources/application-local.properties`
- `src/main/resources/application-test.properties`
- `src/main/resources/application-prod.properties`
- `src/main/resources/application-sample.properties`
- `src/main/resources/application-ncimeta.properties`
- `src/main/resources/application-insert.properties`

Planned local environment helper:

- `config/local/setenv.sh`

Optional companion:

- `config/local/setenv.sh.example`

## Why `config/local/setenv.sh`

This is a good place for the local environment bootstrap file because:

- it is clearly configuration-related
- it avoids cluttering the repo root
- it separates local setup from production packaging
- it makes it easier to document “source this file before local startup”

The purpose of `setenv.sh` is to define the environment variables needed for local development and assign them local values.

It should document:

- which vars are required
- which vars are optional
- which values are safe defaults for local use
- how those values map into `application.properties`

## Local Startup Notes

Current local bootstrap files:

- [config/local/setenv.sh](/Users/deborahshapiro/Code/workspace-meme/nci-meme5/config/local/setenv.sh:1)
- [config/local/setenv.sh.example](/Users/deborahshapiro/Code/workspace-meme/nci-meme5/config/local/setenv.sh.example:1)

Recommended local startup flow:

1. source `config/local/setenv.sh`
2. optionally override machine-specific values with `config/local/setenv.sh.example` or shell exports
3. run the desired Gradle or app startup command in that same shell

The highest-priority variables to verify locally are:

- `APP_DIR`
- `DATA_DIR`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `BASE_URL`

The most common optional overrides are:

- `INDEX_DIR`
- `LVG_DIR`
- `SOURCE_DATA_DIR`
- `MAIL_*`
- `DEPLOY_*`
- `SECURITY_*`

The intended local defaults are:

- mail disabled
- local filesystem paths under the repo root unless overridden
- local DB host/port defaults with credentials provided externally as needed

## Categories Of Configuration To Migrate

The first implementation step should group existing keys into categories.

### Shared application settings

Examples:

- `base.url`
- `deploy.*`
- UI or site-related settings
- mail behavior flags
- security URL settings
- feature toggles

### Hibernate and persistence settings

Examples:

- `hibernate.*`
- connection pool settings
- Lucene / Hibernate Search settings
- dialect and schema settings

### Environment-specific infrastructure settings

Examples:

- DB host / URL / username / password
- search index root
- source data directory
- mail credentials
- any filesystem paths that should not be committed as environment-specific values

### Profile-specific behavior settings

Examples:

- local vs test vs prod behavior
- deploy mode
- logging
- server URL and port defaults

## Compatibility Bridge Strategy

This is the most important technical design point for `NM-278`.

The plan is **not** to rewrite all callers immediately.

Instead, refactor `ConfigUtility.getConfigProperties()` so that it returns a `Properties` object backed by Spring-loaded property sources.

That allows existing code to continue to call:

- `ConfigUtility.getConfigProperties()`

while the underlying source of truth changes from:

- `run.config.*`
- `config.properties`

to:

- Spring Boot environment and profile-loaded application property files

### Proposed transitional behavior

Runtime configuration should load using this priority:

1. Spring environment / `application*.properties`
2. environment variables and JVM property overrides

The temporary legacy `run.config.*` file fallback has been removed.

### Benefits of the bridge

- existing code keeps working during transition
- startup can be validated earlier
- Hibernate and Flyway manual wiring can stay mostly untouched
- refactor risk is reduced

## Manual Hibernate And Flyway Constraint

This story deliberately keeps Hibernate and Flyway outside Spring-managed persistence wiring.

That means:

- Spring Boot should be used for property loading and application conventions
- Spring JDBC / JPA auto-configuration should be excluded if necessary
- the application should continue to construct Hibernate configuration manually from loaded properties
- Flyway should continue to be configured and invoked manually from loaded properties

This mirrors the `wci-common-java` approach, where Spring property conventions are used without requiring full Spring persistence management.

## Phased Implementation Plan

### Phase 1: Inventory and key mapping

Goal:

- create a complete map of current configuration keys and their future home

Tasks:

- inventory all current config keys from:
  - `config/**/config.properties`
  - any other property-bearing files
- classify keys as:
  - shared/common
  - profile-specific
  - secret or environment-specific
  - obsolete / legacy / unused
- create a mapping table:
  - old key
  - new property location
  - env var name if applicable
  - profile file if applicable
  - notes on whether the key remains needed

Deliverable:

- a config key mapping reference

### Phase 2: Introduce Spring Boot configuration skeleton

Goal:

- establish Spring Boot as the configuration loading mechanism

Tasks:

- add minimal Spring Boot bootstrap support to the project
- add:
  - `application.properties`
- exclude Spring auto-configuration for JDBC/JPA/transaction manager if needed to preserve manual Hibernate and Flyway wiring
- add a minimal application bootstrap class if required

Deliverable:

- property files exist and can be resolved by Spring profiles

### Phase 3: Build the `ConfigUtility` compatibility bridge

Goal:

- preserve current callers while switching the underlying config system

Tasks:

- refactor `ConfigUtility.getConfigProperties()` to read from Spring environment-backed properties
- return a flattened `Properties` object
- log active profile information for clarity
- keep temporary legacy fallback behavior while migration is in progress
- avoid changing application behavior yet

Deliverable:

- application code still calling `ConfigUtility` works with Spring-style configuration

### Phase 4: Externalize secrets and environment-specific settings

Goal:

- remove hard-coded environment-specific values from committed property files

Tasks:

- move DB URL/user/password to env-var backed placeholders
- move mail credentials to env-var backed placeholders
- move filesystem and search index roots to env-var backed placeholders
- move any feature flags or deploy-mode toggles that vary by environment to env-backed settings

Examples of the desired pattern:

- `hibernate.hikari.jdbcUrl=${DB_URL}`
- `hibernate.hikari.username=${DB_USER}`
- `hibernate.hikari.password=${DB_PASSWORD}`
- `hibernate.search.backend.directory.root=${INDEX_ROOT}`

Deliverable:

- sensitive and machine-specific settings are externalized

### Phase 5: Add local environment bootstrap

Goal:

- provide a consistent local way to define required variables

Tasks:

- add `config/local/setenv.sh`
- document all required and optional vars
- assign local-development values
- optionally add `setenv.sh.example`

Deliverable:

- a developer can source a single file to populate required local env vars

### Phase 6: Update startup, build, and test flows

Goal:

- stop relying on `run.config.*` startup assumptions

Tasks:

- update `build.gradle` tasks that currently assume `run.config.*`
- update `.vscode/tasks.json` and startup conventions
- update Tomcat launch conventions if they remain in use
- update integration test conventions to use environment-backed config
- update admin tasks to read from the new property system

Deliverable:

- app, admin tasks, and test flows work using Spring config plus environment overrides

### Phase 7: Remove or deprecate legacy config files

Goal:

- complete the migration and reduce duplicate config sources

Tasks:

- remove `config/**/src/main/resources/config.properties`
- remove `config-demo.properties`
- remove `src/main/resources/config.properties.start`
- remove or retire `label.prop` / `run.config.label` if no longer needed
- remove or simplify assembly logic that only exists to package legacy config

If immediate deletion is too risky, mark legacy files deprecated first and remove in a follow-up cleanup.

Deliverable:

- old config files are gone or clearly deprecated

### Phase 8: Documentation

Goal:

- make the new model obvious and reproducible

Tasks:

- document required env vars
- document profile usage
- document local startup
- document test overrides
- document Tomcat startup if still supported
- document admin task usage under the new config model
- document what replaced `run.config.*`

Deliverable:

- a developer can start and configure the app without needing the legacy config model

## Risks And Watchouts

### `ConfigUtility` is deeply embedded

This is the main reason for using a compatibility bridge first.

Trying to rewrite all configuration consumers at once would create unnecessary risk.

### Not every current difference deserves a Spring profile

Some current variants may represent:

- dataset mode
- workflow mode
- admin task inputs

rather than true application environment profiles.

These should remain runtime parameters where that is the cleaner model.

### Build and admin flows are tightly coupled to JVM properties

Many tasks currently pass a mix of:

- `-Drun.config.*`
- other `-D...`
- `-P...`

Not all of these should become Spring properties.

Some should remain task arguments or runtime parameters.

### Legacy config removal should happen late

Removing config files too early will break:

- tests
- admin loaders
- Tomcat startup
- packaging or deployment scripts

Legacy config should only be removed once the compatibility bridge and updated workflows are proven.

## Proposed Deliverables For NM-278

The story should aim to produce the following:

1. `application.properties` exists for shared defaults.
2. Spring Boot property loading is active for the application.
3. `ConfigUtility.getConfigProperties()` returns Spring-backed properties.
4. Sensitive values are externalized through env vars.
5. `config/local/setenv.sh` exists and documents required local variables.
6. Startup and test docs are updated to the new config model.
7. Legacy `config*.properties` files are removed or marked deprecated.

## Recommended Implementation Order

This is the recommended sequence once implementation begins.

1. Add Spring Boot configuration skeleton.
2. Add `application.properties`.
3. Refactor `ConfigUtility` to read Spring-loaded properties first.
4. Migrate common and local settings into Spring property files.
5. Add `config/local/setenv.sh`.
6. Update startup, build, admin, and test flows.
7. Prove app startup and at least one admin/test flow under the new model.
8. Remove or deprecate old `config*.properties` files.

## Detailed Future Implementation Checklist

Use this checklist when work on `NM-278` actually begins.

Current status note:

- The Spring-style configuration bridge is now in place.
- The items checked below reflect work completed in the repo so far.
- Remaining unchecked items are the best candidates for the next implementation steps.

### Discovery and design

- [ ] Inventory all existing configuration keys from legacy property files
- [ ] Separate keys into common, profile-specific, secret/env-specific, and obsolete
- [ ] Produce an old-key to new-property mapping table
- [ ] Decide which differences belong to profiles and which remain runtime args
- [x] Confirm exact local environment variable names to use

### Spring configuration foundation

- [x] Add minimal Spring Boot configuration/bootstrap support to the project
- [x] Add `src/main/resources/application.properties`
- [x] Defer `application-local.properties`, `application-test.properties`, and `application-prod.properties` until they are needed
- [x] Exclude Spring JDBC/JPA auto-config if needed to preserve manual Hibernate/Flyway wiring

### Compatibility bridge

- [x] Refactor `ConfigUtility.getConfigProperties()` to load from Spring environment
- [x] Preserve return type as `Properties`
- [x] Remove temporary fallback for legacy `run.config.*`
- [x] Add clear logging of active config source/profile
- [x] Keep current callers working without broad application rewrites

### Property migration

- [x] Move shared app properties into `application.properties`
- [x] Keep local/test/prod overrides as environment variables for now
- [x] Convert sensitive values to `${ENV_VAR}` placeholders
- [x] Convert machine-specific filesystem settings to `${ENV_VAR}` placeholders

### Local bootstrap

- [x] Add `config/local/setenv.sh`
- [x] Document required local variables in the script
- [x] Add local-safe default values where appropriate
- [x] Optionally add `config/local/setenv.sh.example`

### Build and runtime integration

- [x] Update `build.gradle` tasks that currently depend on `run.config.*`
- [x] Update `.vscode/tasks.json`
- [x] Update Tomcat launch/startup approach if still applicable
- [x] Update local run instructions to use env vars
- [x] Update admin task entrypoints to read new configuration

### Testing

- [x] Update integration tests to use `application.properties` plus env/system-property overrides
- [ ] Confirm tests can run without legacy `config*.properties`
- [ ] Validate sample / ncimeta / insert flows under the new model
- [x] Verify at least one REST test flow and one admin flow

Current proof points:

- `./gradlew adminReindex -Pserver=false` was successfully run under the new
  local env-based configuration path on April 27, 2026, without a legacy
  `-Drun.config.*=/path/to/config.properties` override.
- `./gradlew adminReindex -Pindexed.objects=ProjectJpa -Pserver=false` was
  successfully run with Tomcat down on April 30, 2026, using
  `config/local/setenv.sh` and no legacy `-Drun.config.*` override.
- `./gradlew integrationTest --tests com.wci.umls.server.test.jpa.GraphResolutionHandlerIT`
  was also successfully run under the same new env-based configuration path on
  April 27, 2026.

- `src/main/resources/config.properties.start` was removed on April 27, 2026
  after `/configure` was updated to require Spring-style application property
  resources for its starting configuration.
- `config/prod-nci-meta` is currently treated as the one remaining operational
  legacy packaged config family. The non-`prod-nci-meta` packaged config
  families were removed on April 27, 2026 as part of the cleanup phase.
- The VS Code Tomcat debug startup path was updated to source
  `config/local/setenv.sh` and start without `-Drun.config.*` on April 30,
  2026.
- Local Tomcat startup was manually smoke-tested on April 30, 2026 using the
  Spring/env-backed configuration path. The application started and basic UI
  behavior appeared healthy.
- Web resource packaging now filters `app/appConfig.js` so
  `${project.version}` resolves during the Gradle build, and overlays
  deploy-specific web assets from `config/prod-nci-meta/src/main/resources`.
  This restored the expected footer version and NCI header logo in the local
  Tomcat smoke test.
- `config/dev-windows` was audited during cleanup. Useful remaining local/demo
  configuration was folded into `application.properties`, including MTH graph
  and identifier aliases, SNOMEDCT preferred-name handlers, legacy autofix
  handler registration, UI tracking/cookie/simple-edit flags, and optional
  Cygwin support.
- The remaining `config/prod-nci-meta` Maven assembly descriptor and generated
  config zip artifacts were removed on April 30, 2026. Its retained resources
  are now explicitly documented as web overlays, operational SQL/META support,
  or scripts pending owner review.
- The normal `adminReindex` validation target remains the direct admin path:
  run with Tomcat down and `-Pserver=false`. That path has now been confirmed
  with the focused `ProjectJpa` run above. An April 30, 2026 one-entity
  `ProjectJpa` attempt against the running server (`-Pserver=true`) reached the
  Spring/env-backed config path and authenticated successfully, but the REST
  endpoint returned HTTP 500. Treat that as a separate server-up reindex issue,
  not evidence against the usual server-down admin flow.
- Runtime configuration now loads Spring-style `application*.properties` first.
  Direct legacy `run.config.*` file loading has been removed.
- Focused unit coverage for `ConfigUtilityUnitTest` and
  `ConfigureServiceRestImplUnitTest` passed after the compatibility bridge was
  tightened.
- Local Tomcat smoke testing after the compatibility bridge tightening looked
  healthy on April 30, 2026.
- Decision: remove the gated legacy `run.config.*` fallback after the normal
  sample / ncimeta / insert flows were translated to the env-backed model.
- `docs/database-load-and-test-instructions.md` was translated to the
  env-backed model on April 30, 2026. Normal sample / ncimeta / insert examples
  now source `config/local/setenv.sh` and no longer use `-Drun.config.*`.
- The translated load/test examples now set flow-specific `DB_NAME`,
  `DATA_DIR`, `INDEX_DIR`, and `SOURCE_DATA_DIR` before sourcing
  `config/local/setenv.sh`, matching the legacy sample, ncimeta, and insert
  filesystem split.
- The `com.wci.umls.server.test.rest.ncimeta.*` suite was selected under the
  env-backed setup on April 30, 2026, but all selected tests were skipped
  because those meta-editing classes are currently annotated with JUnit
  `@Ignore`. The load/test instructions now call this out and recommend a
  non-ignored REST smoke test for routine Tomcat validation.
- `com.wci.umls.server.test.rest.SecurityServiceRestNormalUseIT` passed under
  the same env-backed Tomcat/NCI-META setup on April 30, 2026.

### Cleanup

- [x] Remove `src/main/resources/config.properties.start`
- [x] Remove non-`prod-nci-meta` packaged `config/**/src/main/resources/config.properties`
- [x] Remove any unused `config-demo.properties`
- [x] Remove or retire `label.prop`
- [x] Retire direct `run.config.*` override precedence
- [x] Remove gated legacy `run.config.*` fallback
- [x] Remove or simplify assembly/package artifacts that only support legacy config packaging

### Documentation

- [x] Document required env vars
- [x] Document profile names and when to use them
- [x] Document local startup instructions
- [x] Document test startup instructions
- [x] Document admin task usage
- [x] Document migration away from `run.config.*`

## Suggested Next Step

The highest-value next step is:

- review the full worktree, confirm the broad legacy config deletions are
  intended for this story, and package the change for review

## Open Design Questions For Later

These do not need to be answered before the planning document is saved, but they should be resolved before implementation starts.

- Should `sample`, `ncimeta`, and `insert` become Spring profiles, or remain runtime modes?
- Which legacy assembly/package artifacts still matter operationally?
- Should Tomcat deployment remain a supported primary local run path during this story?
- How much of `label.prop` and the current config label mechanism should survive, if any?
- Should local overrides prefer env vars only, or allow `.properties` overrides as a secondary mechanism?

## Final Guidance

The safest path for `NM-278` is:

- standardize configuration first
- preserve `ConfigUtility` as a compatibility surface
- keep Hibernate and Flyway manual
- migrate startup and tests next
- remove legacy config only after the new model is proven

This keeps the story aligned with the acceptance criteria while avoiding a larger architectural rewrite than is necessary right now.
