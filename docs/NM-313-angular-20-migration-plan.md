# NM-313 Angular 20 Migration Plan

## Summary

Migrate MEME's AngularJS frontend in `src/main/webapp` to a new Angular 20
application through a staged, parallel-ui approach.

The recommended path is to create a new Angular 20 frontend that can run beside
the current AngularJS UI for an extended transition period. The new app should
use standalone bootstrap, relative API calls, a development proxy, and a
production packaging model inspired by `NCIEVS/evs-explore`.

This is not an in-place AngularJS upgrade. The current frontend is a
script-loaded AngularJS 1.x application with broad `$scope` usage, global module
state, dynamic route registration, legacy third-party browser libraries, and
many feature-specific controllers/directives. A parallel Angular 20 app gives
the team a safer route-by-route replacement path without destabilizing current
editing and workflow operations.

## Key Decisions

- Build a new Angular 20 app beside the current AngularJS app.
- Use standalone Angular bootstrap for new code.
- Run the new app on a separate local port during migration.
- Use relative API URLs and proxying instead of browser CORS as the default.
- Prioritize read-only screens and shell infrastructure before high-risk
  mutation-heavy workflows.
- Use `NCIEVS/evs-explore` as the closest build/deployment reference, while
  preserving MEME-specific auth, config, tab, project, and session behavior.
- Avoid `ngUpgrade` unless a future spike proves that a specific shared widget
  is worth temporarily embedding.

## Current MEME Frontend State

The legacy UI lives under:

```text
src/main/webapp
```

Important entry points:

- `src/main/webapp/index.html`
- `src/main/webapp/app/app.js`
- `src/main/webapp/app/routes.js`
- `src/main/webapp/app/appConfig.js`
- `src/main/webapp/app/permissions.js`
- `src/main/webapp/app/util/security/securityService.js`
- `src/main/webapp/app/util/configure/configureService.js`
- `src/main/webapp/app/util/general/tabService.js`
- `src/main/webapp/app/util/websocket/websocketService.js`

Current inventory:

- 184 AngularJS `.js` and `.html` files under `src/main/webapp/app`
- about 24,240 JavaScript lines under `src/main/webapp/app`
- about 7,376 HTML template lines under `src/main/webapp/app`
- 284 references matching high-risk migration mechanisms such as `$http`,
  `$watch`, `$broadcast`, `$on`, `$timeout`, `$interval`, `uibModal`,
  `ngTable`, `tinymce`, `hotkeys`, and websocket usage

The current app is AngularJS 1.x and loads dependencies from static scripts and
CDNs. Notable dependencies include:

- AngularJS core modules such as `ngRoute`, `ngCookies`, and `ngAnimate`
- UI Bootstrap
- `ng-file-upload` and `angular-file-upload`
- `ngTable`
- `ui-tree`
- TinyMCE
- angular-hotkeys
- SQL formatter
- bundled static libraries under `src/main/webapp/ui/components` and
  `src/main/webapp/lib`

The current route table is driven by `app/routes.js` and gated by
`deploy.enabled.tabs` from `configure/properties`. Enabled tabs include source,
content, terminology, metadata, workflow, edit, process, inversion, and admin.

Authentication/session behavior is centralized in `securityService.js` and uses
both browser storage and cookies:

- `Authorization` request header
- `user` cookie
- local storage
- `window.name` handoff behavior
- guest-user and license handling

This needs to be preserved during coexistence.

## Reference Project: EVS Explore

Use `NCIEVS/evs-explore` as the primary Angular/Spring packaging reference:

- Repository: `https://github.com/NCIEVS/evs-explore`
- Frontend package: `frontend/package.json`
- Angular workspace: `frontend/angular.json`
- Local proxy config: `frontend/proxy.config.json`
- Dev proxy config: `frontend/proxy.dev.config.json`
- Frontend Gradle build: `frontend/build.gradle`
- Java web wrapper: `web/`
- Java proxy controller: `web/src/main/java/gov/nih/nci/evsexplore/web/controllers/EVSController.java`
- Java proxy service: `web/src/main/java/gov/nih/nci/evsexplore/web/controllers/ProxyService.java`

Useful EVS Explore patterns:

- Keep Angular source in a dedicated `frontend/` module.
- Run local Angular with `ng serve --proxy-config`.
- Use relative API calls from Angular, such as `/api/v1/...`.
- Proxy API calls in development instead of relying on browser CORS.
- Build production Angular assets with a deployment base href.
- Copy Angular build output into the Java web module for packaged deployment.
- Keep frontend test commands explicit: Jest for unit tests and Cypress for e2e.
- Use a Java web module to serve static Angular assets and proxy API calls.
- Keep generated Angular build output separate from source changes unless a
  release/build task explicitly updates it.

Differences from MEME:

- EVS Explore is already an Angular app. MEME must support old and new UIs
  concurrently during migration.
- EVS Explore proxies `/api/v1/**` to a companion EVSRESTAPI service. MEME's
  existing REST API is served by the same application context as the AngularJS
  webapp.
- EVS Explore is mostly public read-only terminology browsing. MEME has
  authentication, role-specific project behavior, admin mutation workflows,
  workflow management, editing, websocket events, popout windows, and
  production data-operation screens.
- EVS Explore currently uses a traditional `AppModule` setup. MEME can use
  standalone bootstrap for the new Angular 20 app.

## Target Outcome

After the migration is complete:

- The Angular 20 UI replaces the AngularJS UI for supported production paths.
- Developers can run old and new UIs concurrently during the transition.
- Existing REST endpoints remain compatible.
- Existing authentication and authorization behavior remains compatible.
- Existing project, role, tab, and deploy-property behavior remains compatible.
- The new frontend has typed API services for high-use REST contracts.
- The new frontend has repeatable local, CI, and packaged build commands.
- High-risk workflows are migrated only after shell/session/config and
  lower-risk read-only screens are stable.
- The old AngularJS assets can eventually be removed from `src/main/webapp`
  after route parity and production confidence are achieved.

## Recommended Repository Shape

Add a dedicated Angular workspace at the repository root:

```text
frontend/
  angular.json
  package.json
  package-lock.json
  proxy.config.json
  proxy.dev.config.json
  src/
    main.ts
    app/
    assets/
    environments/
```

Preferred production output target:

```text
build/generated-ui20
```

or, if packaged into the Spring Boot WAR:

```text
build/generated-resources/ui20
```

Avoid committing generated Angular build output unless a release policy
explicitly requires checked-in static artifacts.

Do not place new Angular source under `src/main/webapp/app`. That tree should
remain the legacy AngularJS ownership boundary until it can be removed.

## Local Runtime Model

During development:

```text
http://localhost:8080/umls-server-rest
```

continues to serve the current MEME backend and AngularJS UI.

The Angular 20 app should run separately, for example:

```text
http://localhost:4200
```

with a proxy configuration that forwards MEME API calls to the local Spring Boot
server.

Candidate `frontend/proxy.config.json` shape:

```json
{
  "/umls-server-rest/**": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

If the Angular app is served under a local base path such as `/ui20`, use the
same relative API style and keep the proxy rules outside source code.

## Production Runtime Model

There are two viable deployment models. Start with the model that minimizes
production change, then converge later.

### Option A: Separate Angular Static App Plus Proxy

Host Angular 20 separately from the MEME backend and proxy API calls to the
existing MEME application.

Advantages:

- Strong old/new UI isolation
- Easy concurrent operation
- Clean rollback to old UI
- Similar local mental model to `ng serve`

Risks:

- Requires deployment/proxy configuration
- Requires careful cookie/header/session behavior across host, port, context,
  and path boundaries

### Option B: Package Angular 20 Into The MEME Spring Boot App

Build Angular 20 assets and serve them from the existing MEME Spring Boot app
under a path such as:

```text
/umls-server-rest/ui20
```

Advantages:

- Same origin as existing REST and cookies
- Avoids browser CORS concerns
- Easier auth/session compatibility
- Matches the EVS Explore pattern of Java serving static Angular assets

Risks:

- More coupling to the existing WAR/static-resource behavior
- Requires static fallback routing for Angular paths
- Requires careful separation from legacy `src/main/webapp` assets

Recommendation:

- Use Option A for early local and test environments if it is operationally
  simple.
- Design the Angular app so Option B remains easy later.
- Prefer same-origin packaged deployment before broad production rollout if
  session/cookie behavior becomes awkward across ports or hosts.

## Angular 20 Technical Baseline

Use Angular 20 with the current Angular-supported runtime constraints.

As of 2026-06-08, the official Angular version compatibility table lists
Angular `20.2.x || 20.3.x` as actively supported with:

- Node.js `^20.19.0 || ^22.12.0 || ^24.0.0`
- TypeScript `>=5.8.0 <6.0.0`
- RxJS `^6.5.3 || ^7.4.0`

Reference:

- `https://angular.dev/reference/versions`

Recheck the official table before implementation begins and pin exact versions
in the first scaffold change.

Pin these through `package-lock.json` and a documented Node/npm version. EVS
Explore uses a Gradle Node build that downloads a specific Node/npm version;
MEME can follow that pattern if reproducibility is more important than using a
developer-managed local Node installation.

Recommended frontend stack:

- Angular 20
- standalone bootstrap through `bootstrapApplication`
- Angular Router
- Angular HttpClient
- reactive forms for new forms
- Jest or the Angular CLI's chosen unit-test runner
- Cypress for smoke/e2e coverage
- ESLint/Prettier or Angular CLI equivalents

Recommended UI stack:

- Start with `ng-bootstrap` and/or PrimeNG if consistency with EVS Explore is
  valuable.
- Avoid adding Angular Material unless the team intentionally wants a new design
  system.
- Avoid carrying forward jQuery, Bootstrap JS, or legacy TinyMCE wrappers unless
  a migrated screen truly requires them.
- Prefer modern Angular-compatible libraries for tables, dialogs, trees, file
  upload, and rich text editing.

## Application Architecture

### Shell

The Angular 20 shell should own:

- app bootstrap
- route table
- header/navigation
- footer
- global notification display
- global loading indicator
- global error handling
- auth/session state
- deploy/config state
- enabled tab state
- current project/user context

The shell should be implemented before feature migrations so every migrated
screen has the same runtime assumptions.

### Runtime Config

MEME already exposes UI-relevant deploy configuration through
`configure/properties`. The new Angular app should consume that endpoint at
startup.

Create a typed config service that loads:

- deploy title
- enabled tabs
- base URL/context behavior
- authentication/security mode properties
- feature flags containing `enabled`
- environment-dependent display values

Use an Angular app initializer or equivalent bootstrap hook so route registration
and navigation state do not race config loading.

### Auth And Session

Build a new Angular auth service around the existing REST/session behavior
instead of changing the backend authentication model in NM-313.

The service should preserve:

- login request semantics
- logout request semantics
- `Authorization` header behavior
- current user model
- guest-user behavior
- `user` cookie compatibility
- license cookie/flow compatibility
- role and permission checks

Early coexistence should use the cookie/header path as the primary bridge
between old and new UIs. Local storage is not enough because it is origin-bound
and will not naturally bridge separate ports.

### API Layer

Do not call `HttpClient` ad hoc from components. Create typed feature services
that mirror the existing AngularJS service boundaries:

- `ConfigureApi`
- `SecurityApi`
- `ProjectApi`
- `MetadataApi`
- `TerminologyApi`
- `ContentApi`
- `WorkflowApi`
- `ProcessApi`
- `SourceDataApi`
- `InversionApi`
- `ReportApi`

Start with the services needed by shell/config/auth and the first read-only
screens. Add types as endpoints are migrated.

### Routing

Prefer path routing under a dedicated base path if the server can support SPA
fallback:

```text
/ui20/...
```

or:

```text
/umls-server-rest/ui20/...
```

Hash routing is an acceptable temporary fallback if server fallback routing
creates risk, but the preferred long-term shape is path routing.

During coexistence, maintain a clear route ownership map:

- routes still owned by AngularJS
- routes implemented in Angular 20
- routes intentionally disabled
- routes that redirect between old and new UI

### State Management

Start with Angular services and RxJS state. Do not add NgRx or another global
state framework in the first slice unless a migrated feature proves it is
needed.

Use small, typed state services for:

- current authenticated user
- deploy configuration
- enabled tabs
- current project
- current terminology/source selection
- global notifications
- global loader state

### Websockets

Do not migrate websocket-dependent workflows first.

When needed, build a focused websocket service that preserves:

- URL derivation under local and deployed contexts
- `Authorization` session matching
- reconnect/error behavior
- event-to-notification behavior
- workflow/process/edit event semantics

## Migration Strategy

The safest strategy is vertical slices, but in risk order rather than navigation
order.

Each slice should include:

- route ownership decision
- typed API methods
- component/template implementation
- loading/error behavior
- auth/permission behavior
- unit tests for service/component logic
- one browser smoke or Cypress test for the route
- comparison against AngularJS behavior

## Phased Plan

### Phase 0: Baseline And Inventory

Status: complete on 2026-06-08. See
`docs/NM-313-phase-0-frontend-inventory.md`.

Goals:

- Document current AngularJS route ownership.
- Document current service-to-endpoint usage.
- Capture screenshots or browser smoke notes for high-use routes.
- Identify role/project combinations needed for validation.
- Decide local URLs and deployment paths for old/new coexistence.

Deliverables:

- route inventory
- service endpoint inventory
- role/permission inventory
- screen-risk matrix
- agreed local URL/proxy model
- first Cypress smoke skeleton against the legacy UI, if practical

Suggested commands:

```bash
rg -n "\\$routeProvider|when\\(" src/main/webapp/app
rg -n "\\$http\\.|Upload\\.|FileUploader|WebSocket" src/main/webapp/app
rg -n "hasPermission|applicationRole|projectRole|deploy.enabled.tabs" src/main/webapp/app
```

Acceptance:

- The team can answer which routes are safest to migrate first.
- The team can answer which endpoints the first migrated screens need.
- The team has an agreed old/new local server and proxy model. Actually running
  both servers together is deferred to NM-313A, after the Angular 20 workspace
  exists.

Phase 0 findings:

- `terminology` is the cleaner first independent read-only feature because it
  loads its own terminology list and details.
- `metadata` is still a strong early feature, but the legacy route assumes a
  selected terminology/model and redirects to `/content` if none exists.
- Phase 1 added the first Cypress smoke skeleton after the `frontend/`
  workspace existed.
- Phase 1 validated concurrent old/new local servers with the Angular 20 dev
  server on `localhost:4200` and the existing MEME backend on `localhost:8080`.

### Phase 1: Angular 20 Workspace And Build Skeleton

Status: complete on 2026-06-08.

Goals:

- Add the new Angular workspace.
- Add repeatable npm and Gradle/Makefile commands.
- Run the new app locally on a separate port.
- Proxy API calls to the existing MEME backend.
- Keep generated Angular artifacts out of normal source diffs.

Deliverables:

- `frontend/package.json`
- `frontend/angular.json`
- `frontend/src/main.ts`
- `frontend/src/app/app.config.ts`
- `frontend/src/app/app.routes.ts`
- `frontend/proxy.config.json`
- `frontend/proxy.dev.config.json`
- Makefile targets such as `make frontend-run`, `make frontend-build`,
  `make frontend-test`
- optional Gradle Node task modeled after EVS Explore

Acceptance:

- `cd frontend && npm start` serves the Angular 20 app.
- Angular 20 can call a harmless MEME backend endpoint through the proxy.
- `cd frontend && npm run build` succeeds.
- No legacy AngularJS assets are modified.

Phase 1 implementation notes:

- Added the Angular 20 workspace under `frontend/` using standalone bootstrap.
- Added proxy configs for `localhost:8080` and `localhost:18080`.
- Added a minimal shell and backend probe for
  `/umls-server-rest/configure/properties`.
- Added root Makefile wrappers for install, run, build, test, and e2e.
- Added the first Cypress smoke spec for the Angular 20 shell.
- Added a local Node `24.16.0` dev dependency plus `.node-version`/`.nvmrc`
  because Angular 20 supports Node `^20.19.0 || ^22.12.0 || ^24.0.0`, while
  the current shell default was Node `25.2.1`.
- Verified `npm start`, the Angular proxy, `npm run build`,
  `make frontend-build`, and `make frontend-test`.

### Phase 2: Shell, Config, Auth, And Navigation

Status: complete on 2026-06-08.

Goals:

- Build the Angular 20 shell.
- Load `configure/properties` at startup.
- Recreate enabled-tab navigation from deploy properties.
- Recreate login/logout/session behavior.
- Preserve old/new UI coexistence.

Deliverables:

- app shell component
- header/navigation component
- footer component
- config service
- auth service
- auth interceptor
- permission service
- global loader service
- global notification/error service
- basic login page
- landing/license route compatibility

Acceptance:

- New UI can show deploy title and enabled tabs from backend config.
- New UI can authenticate and persist the current user/session in a way that
  remains compatible with AngularJS.
- Logout works from the new UI and leaves old UI behavior sane.
- Direct refresh of the new UI does not lose required config/auth state.
- Browser smoke tests cover login, logout, config load, and tab rendering.

Phase 2 implementation notes:

- Added an Angular startup initializer that loads
  `/umls-server-rest/configure/properties` before the shell routes render.
- Added a runtime config service with deploy title, enabled-tab parsing, and
  deploy flag helpers.
- Added auth/session support that preserves the AngularJS-compatible
  `Authorization` header, `localStorage.user`, `user` cookie, and `window.name`
  handoff shape.
- Added login/logout support for
  `/security/authenticate/{userName}` and `/security/logout/{authToken}`.
- Added license acceptance with the same `WCI <deploy.title>` cookie contract.
- Added config-driven header navigation, footer, global loading indicator, and
  notification/error display.
- Added route compatibility for landing, login, license, and enabled-tab
  placeholder routes. Actual feature screens remain Phase 3+.
- Added Cypress smoke coverage for config/tab rendering, login persistence,
  logout cleanup, and license cookie behavior.
- Updated Cypress scripts to unset `ELECTRON_RUN_AS_NODE`, which is required
  when running the Electron-based Cypress runner from this Codex/VS Code shell.

### Phase 3: First Read-Only Feature Slice

Status: complete on 2026-06-08.

Goals:

- Migrate a useful but lower-risk read-only screen before admin mutations.
- Validate the API/service/component pattern.
- Validate routing, permissions, loading, and error behavior on real data.

Preferred candidates:

- metadata tab
- terminology tab
- read-only project/current-user summary
- selected source-data read-only views

Recommendation:

- Start with metadata or terminology before admin.
- Choose the route with the fewest modal/editor/websocket dependencies and the
  clearest read-only REST contract.

Acceptance:

- The screen reaches visual and behavioral parity for common read-only use.
- Empty/error/loading states are handled deliberately.
- API service tests cover query parameter construction and error handling.
- A Cypress smoke test verifies route load and one representative data table or
  detail panel.

Phase 3 implementation notes:

- Migrated the read-only Terminology route into Angular 20 as the first feature
  slice.
- Added typed frontend models for terminology, root terminology, citation, and
  contact information.
- Added a `TerminologyApiService` for:
  - `GET /metadata/terminology/current`
  - `GET /metadata/rootTerminology/{terminology}`
- Added a read-only terminology table with client-side filtering, sorting, page
  size controls, paging, selection, and a detail panel.
- Kept Phase 3 read-only: legacy export, content navigation, metadata
  navigation, and user-preference updates remain later slices.
- Added Cypress smoke coverage for route load, table rendering, selected detail
  display, and filtering.

### Phase 4: Read-Only Admin Foundation

Goals:

- Introduce admin navigation and read-only admin views.
- Validate admin permissions before enabling mutations.
- Preserve current project/user list behavior.

Deliverables:

- admin route shell
- users list view
- projects list view
- selected user details view
- selected project details view
- role/permission display
- admin-only route guard

Acceptance:

- Non-admin users cannot access admin routes.
- Admin users can view users and projects.
- Data shown in Angular 20 matches AngularJS for selected fixtures.
- No create/update/delete admin actions are enabled yet.

### Phase 5: Admin Mutations In Small Slices

Goals:

- Migrate admin writes after read-only parity is established.
- Keep each mutation path independently testable and reversible.

Suggested order:

1. edit user basics
2. add user
3. user role/project assignments
4. edit project basics
5. add project
6. project terminology/source configuration
7. validation checks
8. precedence editing
9. reload/cache/exception operations

Acceptance:

- Each mutation has confirmation/error/success behavior.
- Each mutation has a browser smoke path against a safe local database.
- Admin write tests do not run against shared production-like databases.
- AngularJS remains available for admin operations not yet migrated.

### Phase 6: Source, Process, And Workflow

Goals:

- Migrate operational screens that are important but less editor-intensive than
  content editing.
- Build reusable table, modal, file-upload, and confirmation patterns.

Suggested order:

1. source read-only views
2. source data import/upload paths
3. process read-only views
4. process edit/import dialogs
5. workflow read-only views
6. workflow bins, epochs, checklists, worklists
7. workflow assignment and import operations

Special care:

- file upload behavior
- long-running operation feedback
- websocket or polling notifications
- permission-gated buttons
- large tables and paging/sort/filter behavior

Acceptance:

- Users can complete common operational workflows in Angular 20.
- Long-running operations provide feedback equivalent to AngularJS.
- Errors are visible and actionable.
- REST requests match old UI semantics.

### Phase 7: Content And Edit Workflows

Goals:

- Migrate the highest-risk workflows last.
- Preserve editor productivity and data safety.

High-risk areas:

- content search/detail/edit
- atoms
- relationships
- definitions
- attributes
- semantic types
- contexts
- merge/move/split
- finish workflow
- code concepts
- TinyMCE/rich text behavior
- hotkeys
- popout windows
- websocket-driven notifications

Approach:

- Break edit workflows into narrow workbench slices.
- Prefer read-only detail parity before write operations.
- Build a keyboard shortcut strategy deliberately.
- Replace TinyMCE and legacy popout patterns only after a focused spike.
- Use production-like local data for smoke testing before user acceptance.

Acceptance:

- Critical edit operations match legacy behavior.
- Hotkeys and focus behavior are documented and tested where important.
- Unsaved-change and destructive-action guards are explicit.
- Content/edit routes have the strongest e2e coverage in the migration.

### Phase 8: Cutover And AngularJS Retirement

Goals:

- Make Angular 20 the default UI.
- Remove AngularJS only after route parity and production confidence.

Deliverables:

- final route ownership map
- deployment switch or redirect plan
- rollback plan
- user acceptance checklist
- cleanup ticket list

Cleanup candidates:

- remove AngularJS app scripts/templates from `src/main/webapp/app`
- remove legacy AngularJS libraries under `src/main/webapp/ui/components`
- remove unused CSS and bundled static browser libraries
- remove compatibility redirects that are no longer needed
- update docs and screenshots

Acceptance:

- All production-supported routes are owned by Angular 20.
- Legacy UI can be disabled without blocking supported workflows.
- Build and deployment docs no longer depend on AngularJS assets.

## Screen Prioritization

Use this order as the default unless stakeholder testing suggests otherwise:

1. shell/config/auth/navigation
2. login, landing, license compatibility
3. metadata or terminology read-only route
4. admin read-only users/projects
5. small admin mutations
6. source read-only and source import
7. process read-only and process operations
8. workflow read-only and workflow operations
9. content read-only detail/search
10. edit workbench and mutation-heavy content workflows
11. websocket-heavy and popout-heavy workflows
12. AngularJS removal

Rationale:

- Login is small, but it is foundational and touches session compatibility.
- Admin appears peripheral, but full admin is not low risk because it mutates
  users, projects, roles, validation, precedence, and reload state.
- Metadata/terminology read-only routes are better first feature slices because
  they validate real API/data rendering without write-risk.
- Content/edit should come late because productivity, data safety, hotkeys,
  websocket behavior, and popouts all matter.

## Testing Strategy

### Unit Tests

Add focused tests for:

- config service
- auth service
- auth interceptor
- permission service
- route guards
- API URL/query construction
- component empty/loading/error states

### Cypress Smoke Tests

Add Cypress early, even if the first tests are small.

Initial smoke tests:

- app loads
- config loads
- login succeeds against a local/default environment
- logout succeeds
- enabled tabs render according to config
- first read-only migrated route displays data
- non-admin user is blocked from admin route
- admin user can view users/projects

### Legacy Parity Checks

For each migrated route, keep a lightweight parity checklist:

- route URL
- required role/project state
- API calls made
- visible table/detail fields
- empty state
- error state
- loading state
- permission-gated actions
- old UI comparison notes

### Manual Test Environments

Document which local database/profile is required for each migrated feature.
Use the existing database runbook as the source of truth:

```text
docs/database-load-and-test-instructions.md
```

Do not run admin or edit mutation tests against shared databases.

## Build And CI Strategy

Add explicit frontend targets instead of folding Angular 20 into every Gradle
build immediately.

Suggested Make targets:

```make
frontend-install
frontend-run
frontend-build
frontend-test
frontend-e2e
frontend-package
```

Suggested Gradle integration:

- Add a frontend Gradle build only after the npm scripts are stable.
- Use Gradle Node if reproducible Node/npm downloads are needed.
- Copy Angular production build output into a generated resource directory or
  static deployment directory.
- Keep `make quality` focused unless the team decides frontend tests should
  become part of the default gate.

CI should eventually run:

- Java unit/static checks
- Angular unit tests
- Angular production build
- selected Cypress smoke tests against a started local app

## Rollout Strategy

Use route-level rollout rather than one large UI switch.

Early rollout options:

- expose Angular 20 at `/ui20`
- link selected read-only tabs to Angular 20 for selected testers
- keep AngularJS links available for unfinished workflows
- use deploy properties or environment flags to hide incomplete Angular 20
  routes

Cutover options:

- redirect old route to new route after parity
- keep old route accessible by fallback URL during a grace period
- disable old route once users confirm parity

Rollback:

- old AngularJS UI remains available until final retirement
- new route links can be hidden or redirected back to AngularJS
- API/backend changes should be avoided in early slices to keep rollback simple

## Risks And Mitigations

### Session Compatibility

Risk:

- Old and new UIs may not share local storage across ports or hosts.

Mitigation:

- Use compatible cookies and `Authorization` header behavior as the primary
  bridge.
- Prefer same-origin packaged deployment before broad rollout if separate-port
  session behavior is painful.

### API Shape Drift

Risk:

- New typed services may accidentally normalize or reinterpret legacy REST
  contracts.

Mitigation:

- Compare requests against AngularJS behavior.
- Keep service tests around URL/query/body construction.
- Avoid backend behavior changes in early frontend migration slices.

### Permission Drift

Risk:

- Buttons/routes may appear or disappear differently than AngularJS.

Mitigation:

- Port `permissions.js` behavior into a focused permission service.
- Add route-guard tests.
- Add admin/non-admin smoke tests.

### UI Library Churn

Risk:

- Replacing all UI widgets at once can create distracting visual and behavioral
  drift.

Mitigation:

- Pick a small UI stack early.
- Build wrappers for table, modal, confirmation, notification, loader, and file
  upload patterns.
- Avoid one-off component library decisions per screen.

### Edit Workflow Safety

Risk:

- Editor workflows have high data-safety and productivity risk.

Mitigation:

- Migrate edit workflows last.
- Require parity checklists and strong e2e coverage.
- Preserve old UI fallback until edit users sign off.

### Build Complexity

Risk:

- Adding Node/npm/Angular to a Java-centric repo can make local setup brittle.

Mitigation:

- Keep frontend commands explicit.
- Pin Node/npm through docs or Gradle Node.
- Follow EVS Explore's reproducible build pattern where useful.

## Open Questions

- Should the new app live at `frontend/` or a more explicit name such as
  `ui20/`?
- Should production initially host Angular 20 separately, or package it under
  the existing Spring Boot app from the start?
- What URL should identify the new UI: `/ui20`, `/angular`, or another path?
- Which read-only route should be the first migrated feature: metadata,
  terminology, or an admin read-only view?
- Which UI library should be the default: EVS Explore-style Bootstrap/PrimeNG,
  Bootstrap-only, or another approved stack?
- Should frontend CI be added in the first scaffold ticket or after the first
  feature slice?
- How should route-level rollout be controlled: deploy properties, app config,
  server routing, or a combination?

## Initial Story Breakdown

### NM-313A: Inventory And Scaffold

- Create route/service inventory.
- Create `frontend/` Angular 20 workspace.
- Add local proxy config.
- Add build/test/run scripts.
- Add empty shell route.
- Verify concurrent old/new local operation.

### NM-313B: Config/Auth Shell

- Load `configure/properties`.
- Render deploy title and enabled tabs.
- Implement login/logout/session bridge.
- Implement auth interceptor and permission service.
- Add smoke tests for login/logout/config/navigation.

### NM-313C: First Read-Only Route

- Migrate metadata or terminology read-only route.
- Add typed API methods.
- Add empty/loading/error states.
- Add Cypress smoke coverage.
- Record parity notes against AngularJS.

### NM-313D: Admin Read-Only Foundation

- Add admin route shell.
- Add admin route guard.
- Add users/projects read-only views.
- Add admin/non-admin smoke tests.

### NM-313E: Admin Mutation Slices

- Migrate admin writes in small, reversible increments.
- Start with edit user basics, then add user and assignments.
- Keep project validation/precedence/reload operations for later admin slices.

### NM-313F: Operational Tabs

- Migrate source, process, and workflow routes in staged read-only then mutation
  slices.
- Establish shared table/modal/file-upload/long-running-operation patterns.

### NM-313G: Content/Edit Workbench

- Migrate content and edit workflows after the foundation is mature.
- Add focused e2e coverage for critical editing operations.
- Preserve AngularJS fallback until user acceptance is complete.

### NM-313H: Cutover And Cleanup

- Make Angular 20 the default UI.
- Remove or archive AngularJS assets after parity.
- Update build/deployment docs.
- Remove compatibility routes and unused legacy libraries.

## Acceptance Criteria For NM-313 Planning

- The team agrees to a parallel Angular 20 migration.
- The team agrees to standalone bootstrap for the new app.
- The team agrees to prioritize read-only screens before admin mutations.
- The team agrees to use EVS Explore as a build/proxy/deployment reference.
- The team agrees not to use `ngUpgrade` as the primary migration strategy.
- First implementation tickets can be created from the story breakdown above.
