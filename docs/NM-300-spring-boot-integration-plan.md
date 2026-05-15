# NM-300 Spring Boot Integration Plan

## Summary

Integrate Spring Boot so MEME can start as an embedded-Tomcat application with
`Application.java`, without requiring SmartTomcat or a separately managed local
Tomcat installation.

This story should build on the NM-278 configuration migration. The application
already reads Spring-style `application.properties` files through the
`ConfigUtility` bridge, so NM-300 should focus on application bootstrap,
embedded servlet container wiring, packaging, and developer startup workflow.

## Reference Project

Use this project as the local reference:

- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service`

Relevant reference files:

- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/src/main/java/com/wci/Application.java`
- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/build.gradle`
- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/src/main/resources/application.properties`
- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/run.sh`

Important reference patterns:

- `Application.java` is the process entry point.
- `Application.java` is annotated with `@SpringBootApplication`.
- `Application.java` lives one package level above the rest of the project code
  so Boot component scanning starts from the project root package.
- The Boot application can be packaged as an executable WAR.
- Boot startup uses embedded Tomcat.
- Manual Hibernate/Flyway/service wiring is preserved by excluding Spring Boot
  JDBC/JPA auto-configuration where needed.
- Tomcat connector behavior can be customized with a
  `WebServerFactoryCustomizer<TomcatServletWebServerFactory>`.
- Local/deploy runtime values are driven by environment variables and
  Spring-style property files.

## Current MEME State

MEME is currently a WAR-first Jakarta/Jersey application.

Relevant files:

- `build.gradle`
  - applies `java` and `war`
  - builds `ROOT-1.5.1-SNAPSHOT.war`
  - creates `build/exploded` for SmartTomcat/external Tomcat deployment
  - has no Spring Boot Gradle plugin yet
- `src/main/java/com/wci/umls/server/rest/impl/TermServerApplication.java`
  - extends Jersey `ResourceConfig`
  - registers all JAX-RS resource implementation classes manually
  - starts the periodic DB connection ping timer
- `src/main/webapp/WEB-INF/web.xml`
  - registers the Jersey servlet at `/*`
  - registers `SessionFactoryShutdownListener`
  - registers `ApiOriginFilter`
  - registers `UserActivityLoggingFilter`
  - maps selected static UI paths back to Tomcat's default servlet
- `.vscode/tasks.json`
  - sources `config/local/setenv.sh`
  - builds/explodes the WAR
  - deploys the exploded WAR into Homebrew Tomcat
  - starts external Tomcat in JPDA mode

The REST layer is still JAX-RS/Jersey, not Spring MVC. The reference
terminology-service project has many Spring MVC `@RestController` classes, but
converting MEME's REST endpoints should not be part of the first Spring Boot
startup story. That conversion belongs in NM-301.

## Target Outcome

After NM-300:

- A developer can start MEME with:

```bash
source config/local/setenv.sh
./gradlew bootRun
```

- The app can also be packaged and started with:

```bash
./gradlew bootWar
java -jar build/libs/<boot-war-name>.war
```

- SmartTomcat is no longer required for local application startup.
- Existing REST URLs continue to work under the same local base URL, preferably:

```text
http://localhost:8080/umls-server-rest
```

- The existing Angular/static web UI still loads from the embedded Boot server.
- Existing admin Gradle tasks continue to run directly outside the web server.
- Hibernate remains manually managed.
- Flyway remains manually managed.
- The NM-278 `ConfigUtility` bridge continues to be the compatibility layer for
  existing code.

## Recommended Scope

NM-300 should do the Boot container migration, not a REST rewrite.

In scope:

- add Spring Boot Gradle plugin/dependencies
- add `Application.java`
- make `bootRun` start the app locally
- make `bootWar` produce an executable deployable artifact
- register Jersey resources under embedded Tomcat
- register current servlet filters/listeners programmatically or through Boot
  compatible configuration
- preserve static web UI behavior
- update VS Code tasks/launch configuration for Boot startup
- update local startup docs
- add smoke validation steps

Out of scope:

- converting JAX-RS resources to Spring MVC controllers
- converting service/JPA classes to Spring-managed beans
- replacing manual Hibernate lifecycle with Spring Data/Spring ORM
- enabling Spring Boot Flyway auto-configuration
- updating Springdoc, Swagger UI, or OpenAPI generation
- changing authentication/authorization behavior
- changing URL paths unless a specific compatibility issue forces it

Future ticket:

- NM-301: convert REST implementation classes from JAX-RS/Jersey resources to
  Spring MVC controllers using `@RestController` and `@RequestMapping`
- update Springdoc / Swagger / OpenAPI support after the Boot startup path is
  stable
- decide whether to keep Swagger Core/JAX-RS generation, migrate to Springdoc
  generation, or do a staged bridge
- decide what replaces the legacy bundled `src/main/webapp/swagger.html`
- update the JUnit test suite for compatibility with the Spring Boot test stack
  chosen in that future ticket

## Implementation Plan

### 1. Add Spring Boot Build Support

Update `build.gradle` to apply the Spring Boot and dependency-management
plugins, following the reference project style.

Follow the same high-level ordering as the reference `build.gradle`:

1. `buildscript`
2. `plugins`
3. `repositories`
4. `apply plugin`
5. project metadata / Java settings / dependency constants
6. dependencies and tasks

Candidate setup:

```gradle
// Top level configuration
buildscript {
  ext {
    springBootVersion = '3.5.14'
  }

  repositories {
    mavenLocal()
    mavenCentral()
    maven { url "https://repo.spring.io/release" }
    maven { url "https://plugins.gradle.org/m2/" }
  }

  dependencies {
    classpath "org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}"
  }
}

// This must remain near the top of the file, immediately after buildscript.
plugins {
  id 'java'
  id 'war'
}

repositories {
  mavenLocal()
  mavenCentral()
  maven { url "https://repo.spring.io/release" }
  maven { url "https://plugins.gradle.org/m2/" }
}

apply plugin: 'io.spring.dependency-management'
apply plugin: 'org.springframework.boot'
```

Use `springBootVersion = '3.5.14'`, matching the reference project.

The `spring-boot-gradle-plugin` classpath dependency is required. Do not add an
`elasticsearch.version` or `elastisearch.version` constant for this story; MEME
is not adding Elasticsearch support as part of NM-300.

### 2. Add Boot Dependencies

Add Boot web support. The `dependencies` block must start with
`spring-boot-starter-web`, matching the reference project's ordering.

```gradle
dependencies {
  // NOTE: this must be the first dependency.
  implementation "org.springframework.boot:spring-boot-starter-web"
  implementation "org.springframework.boot:spring-boot-starter-jersey"
}
```

Do not add DevTools in this first Boot slice. Its restart classloader conflicts
with the current manual JPA/model loading path and can cause casts between model
classes loaded by different classloaders.

Consider actuator only if the team wants health/metrics endpoints in this story:

```gradle
implementation "org.springframework.boot:spring-boot-starter-actuator"
```

Do not enable Spring-managed JDBC/JPA. Either exclude auto-config in
`Application.java` or set `spring.autoconfigure.exclude` in
`application.properties`.

Expected exclusions:

```text
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

### 3. Resolve Logging Deliberately

MEME currently uses Log4j 1.x APIs plus `slf4j-log4j12:1.7.36`. Spring Boot 3
normally brings SLF4J 2.x and Logback through `spring-boot-starter-logging`.

Do not let this change accidentally.

Recommended first pass:

- exclude `spring-boot-starter-logging` from Boot starters
- preserve the existing logging behavior initially
- confirm startup logging, REST activity logging, and admin task logging
- plan a separate later cleanup if moving to Log4j 2 is desired

This is one of the riskiest dependency areas in NM-300, so it should be tested
early.

### 4. Add `Application.java`

Create:

```text
src/main/java/com/wci/umls/Application.java
```

This intentionally sits one package level above the existing
`com.wci.umls.server.*` code, matching the reference project's
`src/main/java/com/wci/Application.java` placement.

Recommended shape:

```java
package com.wci.umls;

import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
    return factory -> factory.addConnectorCustomizers(connector -> {
      connector.setAllowBackslash(true);
      connector.setEncodedSolidusHandling(EncodedSolidusHandling.DECODE.getValue());
    });
  }
}
```

The Tomcat customizer mirrors the reference project. Keep it if MEME endpoints
or query parameters need encoded slash/backslash behavior.

### 5. Wire Existing Jersey Application Into Boot

Keep `TermServerApplication` as the Jersey `ResourceConfig` for NM-300. This is
a temporary compatibility bridge until NM-301 converts the REST classes to
Spring MVC controllers.

Preferred first-pass approach:

- expose `TermServerApplication` as the Boot Jersey `ResourceConfig` bean
- keep its explicit resource registration list
- keep the existing dev/non-PROD conditional registration of
  `IntegrationTestServiceRestImpl`

Candidate configuration:

```java
@Bean
public ResourceConfig jerseyConfig() throws Exception {
  return new TermServerApplication();
}
```

This can live in `Application.java` or in a small configuration class such as:

```text
src/main/java/com/wci/umls/server/rest/impl/JerseyConfig.java
```

The existing `TermServerApplication` constructor starts a timer. That is
acceptable for the first Boot pass if it is instantiated exactly once. A later
cleanup can move the ping into Spring scheduling.

### 6. Replace `web.xml` Runtime Wiring

Embedded Boot startup should not depend on SmartTomcat or external `web.xml`
processing.

Programmatically register the pieces now declared in `web.xml`:

- `SessionFactoryShutdownListener`
- `ApiOriginFilter`
- `UserActivityLoggingFilter`
- Jersey servlet mapping
- static/default servlet behavior

Boot may still package `web.xml` in a WAR, but relying on programmatic Boot
registration makes the embedded startup path explicit and testable.

Suggested beans:

```java
@Bean
public ServletListenerRegistrationBean<SessionFactoryShutdownListener>
    sessionFactoryShutdownListener() {
  return new ServletListenerRegistrationBean<>(new SessionFactoryShutdownListener());
}

@Bean
public FilterRegistrationBean<ApiOriginFilter> apiOriginFilter() {
  FilterRegistrationBean<ApiOriginFilter> bean =
      new FilterRegistrationBean<>(new ApiOriginFilter());
  bean.addUrlPatterns("/*");
  return bean;
}

@Bean
public FilterRegistrationBean<UserActivityLoggingFilter> userActivityLoggingFilter() {
  FilterRegistrationBean<UserActivityLoggingFilter> bean =
      new FilterRegistrationBean<>(new UserActivityLoggingFilter());
  bean.addUrlPatterns("/*");
  return bean;
}
```

The static UI behavior needs special attention because the current Jersey
servlet is mapped to `/*`, and `web.xml` maps specific static paths back to the
default servlet.

Validate one of these approaches:

- keep Jersey at `/*` and recreate the required default/static servlet mappings
  programmatically
- or let Boot serve static resources first and register Jersey in a way that
  does not swallow `/app`, `/images`, `/css`, `/fonts`, `/lib`, `/ui`, and
  `/index.html`

Do not change public REST paths just to make the servlet mapping easier unless
that is explicitly approved.

### 7. Preserve Web UI Packaging

The current `prepareWebapp` task filters `app/appConfig.js` and overlays
deploy-specific static resources from `config/prod-nci-meta/src/main/resources`.

NM-300 should keep that behavior.

Verify that both `war` and `bootWar` use the generated webapp directory:

```gradle
bootWar {
  dependsOn prepareWebapp
  webAppDirectory = generatedWebappDir
  mainClass = 'com.wci.umls.Application'
}
```

Decide artifact naming:

- keep the regular WAR for compatibility if needed
- make the executable Boot artifact clearly named, for example with classifier
  `webapp`
- document which artifact should be used for local/deploy startup

### 8. Add Boot Runtime Properties

Add shared Boot properties to `src/main/resources/application.properties`.

Candidate additions:

```properties
spring.application.name=nci-meme5
server.port=${SERVER_PORT:8080}
server.servlet.context-path=${SERVER_SERVLET_CONTEXT_PATH:/umls-server-rest}
server.servlet.session.persistent=false
spring.servlet.multipart.max-file-size=${MULTIPART_MAX_FILE_SIZE:200MB}
spring.servlet.multipart.max-request-size=${MULTIPART_MAX_REQUEST_SIZE:200MB}
spring.jersey.type=servlet
```

Keep:

```properties
spring.flyway.enabled=false
```

if/when NM-280 Flyway work lands, because MEME is intentionally managing Flyway
outside Spring Boot auto-configuration.

### 9. Update Local Startup Workflow

Add or replace VS Code tasks so local startup no longer needs SmartTomcat.

Recommended tasks:

- `spring boot: run`
- `spring boot: run (debug)`
- `spring boot: stop` only if a background task/session needs cleanup
- `gradle: bootWar`

Example command shape:

```bash
source "${workspaceFolder}/config/local/setenv.sh" && \
  "${workspaceFolder}/gradlew" bootRun
```

For debugging, either use:

```bash
./gradlew bootRun --debug-jvm
```

or configure the `bootRun` task with a JDWP port matching
`.vscode/launch.json`.

The old Tomcat tasks can remain temporarily as a fallback, but the primary
documented path should be Boot.

### 10. Add Documentation

Update local docs with:

- how to start with `bootRun`
- how to start the packaged executable WAR
- default local URL
- how to override `SERVER_PORT` and `SERVER_SERVLET_CONTEXT_PATH`
- how to run REST integration tests against the Boot-started app
- when external Tomcat/SmartTomcat is still supported, if at all

Candidate quick start:

```bash
cd /Users/deborahshapiro/Code/workspace-meme/nci-meme5
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbncimeta
export DATA_DIR="$APP_DIR/data_ncimeta"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh
./gradlew bootRun
```

Then visit:

```text
http://localhost:8080/umls-server-rest
```

## Validation Plan

### Build and Unit Tests

```bash
./gradlew clean test
```

### Boot Startup

```bash
source config/local/setenv.sh
./gradlew bootRun
```

Expected:

- embedded Tomcat starts on `SERVER_PORT` or `8080`
- context path is `/umls-server-rest`
- `TermServerApplication` logs startup once
- no SmartTomcat or Homebrew Tomcat process is required

### UI Smoke

Open:

```text
http://localhost:8080/umls-server-rest
```

Verify:

- header logo is correct
- footer version is resolved
- static assets load
- basic navigation works

### REST Smoke

```bash
curl -s -H 'Authorization: DSS' \
  http://localhost:8080/umls-server-rest/project/user/anyrole
```

Expected:

- endpoint responds as it does under external Tomcat
- no context-path or servlet-mapping regression

### REST Integration Tests

With Boot running in another terminal:

```bash
source config/local/setenv.sh
./gradlew integrationTest \
  --tests 'com.wci.umls.server.test.rest.SecurityServiceRestNormalUseIT'
```

Also run whichever NCI-META smoke suite is active at the time. If
`com.wci.umls.server.test.rest.ncimeta.*` remains `@Ignore`, document that it is
selected but skipped.

### Executable WAR

```bash
source config/local/setenv.sh
./gradlew bootWar
java -jar build/libs/<boot-war-name>.war
```

Repeat the UI and REST smoke checks.

### Admin Task Regression

Confirm representative non-web admin tasks still run from the sourced
environment:

```bash
source config/local/setenv.sh
./gradlew adminReindex -Pindexed.objects=ProjectJpa -Pserver=false
```

This guards against Boot dependency changes breaking direct JavaExec flows.

## Main Risks

- Jersey mapped at `/*` can prevent Boot/static resource handling from serving
  the Angular UI unless the old default servlet behavior is recreated.
- Boot's dependency management may upgrade or alter Jersey, Jackson, SLF4J, or
  Tomcat dependencies in ways that change runtime behavior.
- Boot starter logging conflicts with the current Log4j 1.x / SLF4J 1.7 setup.
- `TermServerApplication` timer startup could run twice if Jersey config is
  accidentally instantiated twice.
- `SessionFactoryShutdownListener` must still run on embedded server shutdown,
  or Hibernate resources may leak.
- `src/main/webapp` behavior differs between plain WAR, executable Boot WAR,
  and executable JAR. Prefer `bootWar` first to reduce packaging churn.
- Existing tests and UI code assume `/umls-server-rest`; Boot's default context
  path is `/`, so the context path must be set deliberately.
- File upload endpoints need multipart limits configured through Boot.
- Encoded slash/backslash handling may differ between external Tomcat and Boot's
  embedded Tomcat.

## Suggested Commit Split

1. Build/dependency foundation
   - add Boot plugin/dependencies
   - add Boot runtime properties
   - keep existing WAR/explode tasks working

2. Boot application wiring
   - add `Application.java`
   - register Jersey, filters, listener, and Tomcat customizer
   - prove `bootRun` starts

3. Packaging and workflow
   - configure `bootWar`
   - update VS Code tasks/launch config
   - update docs

4. Stabilization
   - fix static asset routing if needed
   - resolve logging/dependency conflicts
   - add or update tests
   - run full smoke validation

## Acceptance Criteria

- `source config/local/setenv.sh && ./gradlew bootRun` starts the app.
- No SmartTomcat or separately started local Tomcat is required.
- The app serves the UI at `/umls-server-rest`.
- Existing REST endpoints work under `/umls-server-rest`.
- `bootWar` produces an executable WAR that starts with `java -jar`.
- Existing admin JavaExec tasks still work.
- Configuration continues to come from `application.properties` and active
  Spring profiles/env vars.
- Hibernate and Flyway remain manually managed.
- Local startup documentation and VS Code tasks point to the Boot workflow.
