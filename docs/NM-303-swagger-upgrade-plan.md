# NM-303 Swagger Upgrade Plan

## Summary

Upgrade MEME's Swagger/OpenAPI support from the legacy bundled Swagger UI and
Swagger 2/JAX-RS generation path to Springdoc OpenAPI 3, using the current
`wci-terminology-service` Swagger setup as the reference point.

The first implementation slice has already established Springdoc generation and
made the Swagger UI usable under the Spring Boot application. Remaining work is
mostly parity and polish: custom branded UI shell, better auth experience,
OpenAPI 3 annotation cleanup, curated examples, and endpoint smoke coverage.

## Reference Project

Use this project as the local reference:

- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service`

Relevant reference files:

- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/build.gradle`
- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/src/main/resources/application.properties`
- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/src/main/resources/swagger-ui/index.html`
- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/src/main/resources/swagger-ui/index.css`
- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/src/main/resources/swagger-ui/logo.png`
- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/src/main/resources/swagger-ui/favicon.png`
- `/Users/deborahshapiro/Code/workspace-terminology-service/wci-terminology-service/src/main/java/com/wci/rest/SwaggerTerminologyServiceRestImpl.java`

Important reference patterns:

- Springdoc supplies `/v3/api-docs` and Swagger UI.
- Swagger UI sorting and doc expansion are configured through
  `springdoc.swagger-ui.*` properties.
- The OpenAPI document declares global security metadata.
- The reference service uses a bearer/JWT token that is already valid when
  pasted into Swagger's Authorize dialog.
- The Swagger UI is a custom static/templated shell with branded header,
  favicon, logo, base URL/docs links, and custom CSS.

## Current MEME State

The branch currently has the core Springdoc migration in progress.

Implemented:

- `build.gradle`
  - adds `org.springdoc:springdoc-openapi-starter-webmvc-ui`
  - removes the legacy Swagger 2 annotations dependency
  - removes the old Swagger JAX-RS/OpenAPI generator dependency
- `src/main/resources/application*.properties`
  - configures `/v3/api-docs`
  - configures Swagger UI tag/operation sorting and collapsed operation display
  - updates deployment titles to NCI-META Terminology Maintenance branding
- `src/main/java/com/wci/umls/server/rest/impl/OpenApiConfiguration.java`
  - defines OpenAPI title, description, contact, server URL, and Authorization
    header security scheme
  - removes duplicate generated `Authorization` header parameters
  - makes `/security/authenticate/{username}` public in the OpenAPI document
  - applies safe examples for `PfsParameterJpa` request bodies
- `src/main/webapp/swagger.html`
  - replaces the legacy Swagger UI page with a branded compatibility shell
  - shows the base URL, OpenAPI docs link, and authentication note
  - embeds Springdoc's generated Swagger UI
- `src/main/webapp/WEB-INF/web.xml`
  - keeps `/swagger.html` mapped to static web resources for external Tomcat
    compatibility
- `src/main/java/com/wci/umls/server/rest/impl/SecurityServiceRestImpl.java`
  - makes authentication consume `text/plain`
  - documents the password body for OpenAPI 3
  - normalizes simple quoted text bodies from Swagger/curl

Validated so far:

- `/umls-server-rest/v3/api-docs` returns OpenAPI JSON.
- `/umls-server-rest/swagger-ui/index.html` loads.
- `/swagger.html` serves the branded shell and embeds the new UI.
- `POST /security/authenticate/{username}` works with `text/plain` body.
- Swagger-generated authenticated requests include `Authorization` after the
  user clicks Authorize.
- PFS examples no longer encourage unsafe `"string"` values.
- all REST implementation endpoints have OpenAPI 3 tags and operation
  summaries.
- `POST /project/find` works with:

```json
{
  "maxResults": 25,
  "startIndex": 0
}
```

Known local testing nuance:

- In DEFAULT security, `admin`, `guest`, and similar tokens are only valid
  after `/security/authenticate/{username}` runs in the same JVM.
- Swagger's stock Authorize dialog only stores a header value. It does not call
  the MEME authentication endpoint.

## Target Outcome

After NM-303:

- Swagger is available at:

```text
http://localhost:8080/umls-server-rest/swagger-ui/index.html
```

- Legacy `/swagger.html` continues to land on a working Swagger UI.
- `/v3/api-docs` is the source OpenAPI document.
- Branding says NCI-META Terminology Maintenance, not WCI Term Server.
- The OpenAPI server URL includes the `/umls-server-rest` context path.
- Authenticated endpoints use one global `Authorization` header security scheme.
- Authentication and common request-body workflows are understandable from
  Swagger without tribal knowledge.
- High-use endpoints have examples that can be executed successfully from
  Swagger.
- Legacy Swagger 2 annotation references have been removed from the REST layer
  and Gradle dependencies.

## Recommended Scope

In scope:

- Springdoc and Swagger UI parity with the reference project
- branded Swagger UI shell
- compatibility redirect from `/swagger.html`
- OpenAPI metadata and global security scheme
- endpoint grouping/tag cleanup
- request examples for common helper payloads
- manual Swagger smoke-test instructions
- OpenAPI 3 annotation updates for REST implementation endpoints

Out of scope:

- replacing MEME's legacy authentication model with JWT/OAuth
- changing authorization semantics for production endpoints
- broad REST API behavior changes
- FHIR-specific OpenAPI resource-page machinery from the reference service
- fixing legacy service methods that return HTTP 500 for auth failures, except
  where the ticket explicitly expands to response-code cleanup

## Remaining Work Plan

### 1. Finish and Verify the Core Springdoc Slice

Status: complete in the current branch.

Keep the current Springdoc work as the base slice.

Checklist:

- confirm `./gradlew compileJava compileTestJava`
- confirm `git diff --check`
- start local app with:

```bash
source config/local/setenv.sh
./gradlew bootRun
```

- verify:

```bash
curl -s http://localhost:8080/umls-server-rest/v3/api-docs
curl -I http://localhost:8080/umls-server-rest/swagger-ui/index.html
curl -I http://localhost:8080/umls-server-rest/swagger.html
```

Expected OpenAPI details:

- `info.title` uses deploy title plus `API`
- `servers[0].url` is `/umls-server-rest`
- `components.securitySchemes.authorizationHeader.name` is `Authorization`
- protected operations have `authorizationHeader` security
- `/security/authenticate/{username}` has no security requirement

### 2. Add a Branded Swagger UI Shell

Status: complete in the current branch.

MEME now uses the existing `src/main/webapp/swagger.html` compatibility path as
the branded shell. This keeps Springdoc's bundled `/swagger-ui/index.html`
available and avoids copying generated Swagger UI bundles into the repository.

The page shows:

- NCI-META Terminology Maintenance API
- current instance/base URL
- OpenAPI docs URL
- clear authentication note
- the standard Springdoc Swagger UI embedded below the header

Acceptance:

- `/swagger-ui/index.html` remains usable
- branding is visible before the operation list
- `/swagger.html` compatibility path serves the branded shell
- no duplicate or broken Swagger asset paths

### 3. Improve Swagger Authentication UX

Status: complete in the current branch.

Problem:

- The stock Authorize button stores a header value.
- It does not call `/security/authenticate/{username}`.
- DEFAULT security only registers `admin`, `guest`, etc. after authenticate runs
  in the same JVM.

Preferred approach:

- Added a small custom Swagger login helper outside the stock Authorize modal
  in `/swagger.html`.
- User enters username/password.
- Helper calls:

```text
POST /security/authenticate/{username}
Content-Type: text/plain
```

- Helper reads the returned `authToken`.
- Helper calls Swagger UI's preauthorization API for `authorizationHeader`.
- Helper can log out of the Swagger UI authorization state.

Benefits:

- keeps server auth semantics unchanged
- makes local Swagger testing less confusing
- avoids pretending the stock Authorize button is a login flow

### 4. Migrate REST Endpoints to OpenAPI 3 Annotations

Status: complete for all REST implementation endpoints.

Migrated endpoint areas:

- `SecurityServiceRestImpl.authenticate`
- `ContentServiceRestImpl.getConcept`
- `ContentServiceRestImpl.findConcepts`
- all `ProjectServiceRestImpl` endpoints, including:
  - project CRUD and find
  - project/user assignment and lookup
  - roles, query types, and validation checks
  - project and activity logs
  - molecular and atomic action history
  - reload/exception admin helpers
  - type-key-value CRUD and find
- all remaining `*ServiceRestImpl` endpoints now use OpenAPI 3 metadata
  directly instead of legacy Swagger 2 annotations

Use OpenAPI 3 annotations:

- `@Tag`
- `@Operation`
- `@Parameter`
- `@io.swagger.v3.oas.annotations.parameters.RequestBody`
- `@ApiResponse`

Acceptance:

- operation summaries and parameter names are useful
- request body examples are executable
- auth requirements are visible and accurate
- old `Authorization` parameters do not reappear as ordinary header fields
- generated fallback tags such as `workflow-service-rest-impl` do not appear
- no `io.swagger.annotations` imports or `io.swagger:swagger-annotations`
  dependency remain

### 5. Curate Common Request and Schema Examples

Status: complete for the current NM-303 Swagger upgrade scope.

The generated Swagger examples can create invalid API calls, especially when a
plain `"string"` value is treated as a Lucene field or sort field.

Addressed:

- `PfsParameterJpa` and `PfsParameter` request bodies now offer named examples.
  The `Sorted by name` variant is only shown for operations where `name` is a
  broadly safe sort field, such as project, user, report, and source-data
  searches.

  - **First page**

```json
{
  "maxResults": 25,
  "startIndex": 0
}
```

  - **Sorted by name**

```json
{
  "maxResults": 25,
  "startIndex": 0,
  "sortField": "name",
  "ascending": true
}
```

  - **Restricted to non-obsolete**

```json
{
  "maxResults": 25,
  "startIndex": 0,
  "queryRestriction": "obsolete:false"
}
```

- optional PFS properties use safer examples/descriptions, such as
  `queryRestriction: "obsolete:false"` and a `sortField` description, instead
  of placeholder `"string"` values.
- high-use parameters now get examples where endpoint annotations did not
  already define one:
  - `query`
  - `JPQL`, with general-query endpoints marking it optional and blank by
    default so Swagger does not send invalid `JPQL=JPQL`
  - `terminology`
  - `version`
  - `projectId`
  - `process`
- common non-PFS helper bodies now have realistic examples:
  - `/content/reindex` uses `ProjectJpa`
  - note body helpers use `Sample note`
  - workflow process progress body uses project id `39751`
  - bulk workflow progress uses `["BETA"]`
  - terminology load path bodies use `/path/to/input`
  - metadata relationship-type update bodies show realistic abbreviation,
    terminology, version, and hierarchy fields
  - metadata relationship-type add bodies show the required two-item inverse
    pair list wrapper

Future follow-up, if needed:

- any body where Swagger generates deeply nested or misleading examples

Acceptance:

- examples in Swagger's "Try it out" path should either work as-is or fail for
  understandable domain reasons, not because of placeholder `"string"` values

### 6. Improve Tags and Visibility

Status: complete for the current NM-303 Swagger upgrade scope.

Swagger grouping is now controlled by the OpenAPI customizer so the generated
docs stay navigable without changing endpoint behavior.

Visible groups:

- Security
- Project
- Content
- Metadata
- Workflow
- Process
- Source Data
- Report
- History
- Simple Edit
- Meta Editing
- Inversion
- Configure
- Admin / Use with care

Hidden from Swagger:

- `/test/**`
- `/project/exception`
- `/configure/destroy`
- `/configure/configure`
- `/process/testquery`

Grouped under **Admin / Use with care** with warning text:

- `/project/reload`
- `/content/reindex`
- `/content/expression/index/{terminology}/{version}`
- terminology load/remove endpoints
- `/file/data/load`
- `/file/data/remove`
- `/file/data/cancel`
- process prepare/execute/restart/step/cancel endpoints
- workflow checklist compute
- workflow worklist/checklist stamp
- workflow status recompute
- workflow autofix

Acceptance:

- endpoint list is scannable
- important endpoints are easy to find
- internal/test endpoints are hidden
- risky admin workflows are clearly labeled before a user can execute them

### 7. Document Auth and Smoke Testing

Add a short manual test section to the ticket or README.

Recommended smoke flow:

1. Start the app.
2. Open Swagger.
3. Authenticate `admin` with body `admin`.
4. Authorize with the returned token.
5. Run:

```text
GET /project/checks
POST /project/find
POST /project/{projectId}/users
```

Example PFS body:

```json
{
  "maxResults": 25,
  "startIndex": 0
}
```

Expected result:

- authentication returns a user with `authToken`
- project checks returns validation check names
- project find returns at least one project in local sample data
- project users returns assigned users when authorized with a project-authorized
  account such as `admin`

### 8. Optional Response-Code Cleanup

Swagger currently exposes a confusing behavior: auth failures often return HTTP
500 with a text body such as:

```text
AuthToken does not have a valid username.
```

This is not required for basic Swagger parity, but it is worth a follow-up
ticket.

Possible later work:

- map missing/invalid token to 401
- map insufficient role to 403
- add documented error response schemas
- preserve existing clients if they depend on legacy error bodies

## Risks and Decisions

### Authentication UX

Decision needed:

- Do we keep stock Authorize and document the separate authenticate step?
- Or do we add the custom login helper in this ticket?

Recommendation:

- Hold the custom login helper until after the current Springdoc slice is
  committed, then implement it as a focused follow-up within NM-303 if time
  allows.

### Swagger 2 Annotation Cleanup

Decision:

- Swagger 2 annotations and the `io.swagger:swagger-annotations` dependency
  have been removed as part of this ticket.

Risk:

- The diff is larger because documentation annotations were updated across the
  REST layer.

Mitigation:

- `/v3/api-docs` was verified to retain all 327 generated operations, operation
  summaries, service tags, and the global Authorization security behavior.

### Custom UI Assets

Risk:

- Copying Swagger UI bundles into the app can create version drift from
  Springdoc's packaged Swagger UI.

Recommendation:

- prefer a lightweight custom shell/CSS around Springdoc where possible
- only vendor Swagger UI assets if Springdoc customization is insufficient

### Generated Examples

Risk:

- Default examples are syntactically valid JSON but semantically bad API input.

Recommendation:

- continue central schema/example customization for shared helper classes
- add operation-specific examples for high-use endpoints

## Suggested Implementation Order

1. Commit the current core Springdoc migration.
2. Add documentation for the authenticate/authorize testing flow.
3. Curate examples for the top smoke-test endpoints.
4. Add OpenAPI 3 annotations to Security and Project endpoints.
5. Convert the remaining REST implementation annotations to OpenAPI 3.
6. Remove the Swagger 2 dependency.
7. Revisit the custom login helper.
8. Create a follow-up ticket for 401/403 error-code cleanup if desired.

## Acceptance Checklist

- `./gradlew compileJava compileTestJava` passes.
- `git diff --check` passes.
- `/umls-server-rest/v3/api-docs` returns OpenAPI JSON.
- `/umls-server-rest/swagger-ui/index.html` loads.
- `/umls-server-rest/swagger.html` serves the branded Swagger shell.
- Branding uses NCI-META Terminology Maintenance.
- OpenAPI server URL includes `/umls-server-rest`.
- Swagger Authorize sends `Authorization`.
- Authentication endpoint is callable without prior auth.
- PFS request examples are safe.
- Manual smoke flow succeeds locally.
- Remaining auth UX limitation is documented or addressed by the custom helper.
