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
- Treat route behavior, interaction parity, and visual parity as separate
  migration checkpoints. Implement risky API/permission behavior first, then
  converge on legacy interaction patterns and visual consistency before a route
  is considered production-ready.

## Interaction And Visual Parity Strategy

The Angular 20 app may temporarily use simpler controls while a route is being
made functional, but the end state should feel familiar to existing MEME users.

Recommended sequence:

1. Prove the route's API contracts, auth behavior, permissions, routing,
   loading, and error handling.
2. Replace temporary controls with reusable Angular interaction patterns that
   match legacy workflows, such as dialogs for add/edit forms, confirmation
   prompts for destructive actions, accordions or equivalent grouped sections,
   and table paging/sort/filter controls.
3. Consolidate repeated styling into shared component styles or small wrapper
   components instead of allowing each migrated screen to accumulate one-off
   CSS.
4. Complete a visual parity pass against the legacy screen before routing
   production users to the Angular 20 version by default.

For Phase 5 specifically, the inline user form is an intentional early
functional slice. Before admin parity is complete, add/edit user should move to
the shared dialog pattern chosen for admin mutation workflows.

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

The current AngularJS route table is driven by `app/routes.js` and gated by
`deploy.enabled.tabs` from `configure/properties`. Angular 20 intentionally
does not support the legacy Sources, Terminology, or Metadata tabs; if those
keys are present in deploy config, the Angular 20 shell ignores them.

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
    "ws": true,
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
- Jest for unit-test coverage
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
- `ContentApi`
- `WorkflowApi`
- `ProcessApi`
- `InversionApi`
- `ReportApi`

Start with the services needed by shell/config/auth and supported screens. Add
shared metadata endpoint wrappers only inside features that still require them,
such as Admin project setup or Process config defaults.

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
- current terminology selection where supported feature forms require it
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

- The original inventory identified terminology and metadata as low-risk
  read-only candidates, but the later NM-313 scope decision removed the
  Sources, Terminology, and Metadata tabs from Angular 20 support.
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

### Phase 3: Removed Read-Only Tab Slice

Status: removed from NM-313 scope on 2026-06-10.

The earlier Angular 20 Terminology feature slice was removed after the team
decided not to support the Sources, Terminology, or Metadata tabs in the new UI.
The shell still tolerates those keys in backend deploy configuration by ignoring
them, but Angular 20 no longer registers routes, navigation entries, feature
components, or Cypress coverage for those tabs.

### Phase 4: Read-Only Admin Foundation

Status: complete on 2026-06-09.

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
- admin route guard that blocks viewers while allowing application users and
  administrators to see the read-only foundation

Acceptance:

- Viewers cannot access admin routes.
- Application users and administrators can view users and projects.
- Data shown in Angular 20 matches AngularJS for selected fixtures.
- No create/update/delete admin actions are enabled yet.

Phase 4 implementation notes:

- Migrated `/admin` from a placeholder route to an Angular 20 read-only admin
  foundation.
- Kept the Angular 20 admin tab visibility aligned with the legacy AngularJS
  admin page: application `USER` and `ADMINISTRATOR` can view the read-only
  foundation, while `VIEWER` cannot.
- Added typed admin models and an `AdminApiService` for the legacy read
  endpoints:
  - `POST /project/find`
  - `POST /security/user/find`
  - `GET /security/roles`
  - `GET /project/roles`
- Added projects and users tables with server-backed filter, sort, paging,
  refresh controls, and selected read-only detail panels.
- Added role/permission display through application role lists, project role
  lists, project role assignments, and user project-role maps.
- Omitted all admin mutations, including add/edit/delete, validation changes,
  reload/config actions, precedence editing, and user-preference writes.
- Added Jest coverage for admin API helper behavior, with Cypress retained as
  the required e2e framework.

### Phase 5: Admin Mutations In Small Slices

Status: complete on 2026-06-09 for the planned Angular 20 admin mutation
slices, with later visual/interaction parity refinements tracked separately.

Goals:

- Migrate admin writes after read-only parity is established.
- Keep each mutation path independently testable and reversible.

Suggested order:

1. edit user basics
2. add user
3. user role/project assignments
4. edit project basics
5. add project
6. delete project
7. project terminology configuration
8. validation checks
9. precedence editing
10. reload/cache/exception operations

Acceptance:

- Each mutation has confirmation/error/success behavior.
- Each mutation has a browser smoke path against a safe local database.
- Admin write tests do not run against shared production-like databases.
- AngularJS remains available for admin operations not yet migrated.

Phase 5 initial implementation notes:

- Added Angular 20 admin user mutations for the first two suggested slices:
  - add user
  - edit user role/editor level
- Added a conservative user-project role assignment slice from the selected
  user's detail panel:
  - assign a loaded project to the selected user with a selected project role
  - remove an existing project role assignment after confirmation
- Added project edit basics from the selected project detail panel:
  - name
  - description
  - feedback email
  - workflow path
  - editing and automation flags
  Terminology/version/language edits, validation-check selection, and
  validation-data add/remove and precedence ordering were added in follow-on
  Phase 5 slices.
- Added project add basics from the projects panel:
  - name
  - description
  - feedback email
  - terminology
  - version
  - language
  - workflow path
  - editing and automation flags
- Added client-side validation for required project basics, including workflow
  path, so null/blank required fields are reported before a save request is
  sent.
- Preserved the legacy project permissions split: application
  `ADMINISTRATOR` users can manage users and project assignments; application
  `USER` users can add projects; project-level `ADMINISTRATOR` users can edit
  projects where their project role allows it.
- When an application `USER` creates a project, Angular 20 follows the legacy
  side effect of assigning that creator to the new project as project
  `ADMINISTRATOR`.
- After assigning a non-admin creator to a newly added project, Angular 20 also
  updates the current user's `lastProjectId` preference through
  `POST /security/user/preferences/update` and refreshes the stored session
  with the returned preferences.
- Added project delete from the selected project detail panel:
  - uses the legacy `DELETE /project/{id}` endpoint
  - shows the legacy assigned-user warning when the project has user role
    assignments
  - refreshes both projects and users after removal because project deletion can
    unassign users
- Fixed the backend project delete path so assigned user project-role rows in
  `user_project_role_map` are removed before the project row is deleted.
- Added project terminology configuration basics:
  - loads current terminology suggestions from `GET /metadata/terminology/current`
  - auto-populates the current version when a known terminology is selected
  - loads language options from `GET /metadata/all/{terminology}/{version}`
  - loads workflow path options from `GET /workflow/paths`
  - keeps precedence in a later dedicated slice
- Added project validation-check selection:
  - loads available checks from `GET /project/checks`
  - shows available and selected check lists in Edit Project
  - saves selected check keys through the existing project update payload
  - preserves the legacy Add Project behavior of selecting checks whose label
    starts with `Default`
- Added project validation-data add/remove in Edit Project:
  - shows existing validation data rows with validation check, value 1, and
    value 2 columns
  - opens an Add Validation Data dialog using the available validation checks
  - removes rows after confirmation
  - preserves existing row ids and sends new rows without ids so the backend
    project update path creates/removes `TypeKeyValue` rows
- Added project precedence-list ordering in Edit Project:
  - loads the project precedence list from `GET /metadata/precedence/{id}`
  - shows terminology/term type ordering in a scrollable table
  - supports moving rows up and down while highlighting touched rows
  - saves reordered precedence independently through `POST /metadata/precedence`,
    matching the legacy separation between project saves and precedence saves
  - treats up/down buttons as the conservative first slice; during the admin
    interaction-parity pass, replace or supplement them with Angular CDK
    drag-and-drop ordering so long precedence lists can be rearranged more
    naturally
- Added user deletion:
  - blocks deletion when the user still has project-role assignments, matching
    the legacy rule
  - confirms deletion before calling `DELETE /security/user/remove/{id}`
  - reloads users and projects after removal
- Added current-user preference operations:
  - feedback email save
  - reset preferences to the legacy default state
  - stored-session refresh after preference saves
- Added admin-only operational actions:
  - reload configuration through `POST /project/reload`
  - force generic exception through `POST /project/exception`
  - force local/test exception through `POST /project/exception?local=true`
- Matched the legacy edit-user modal's conservative behavior: existing user
  username, name, and email are displayed read-only; editor level and
  application role are editable.
- Moved add/edit user into a reusable Angular dialog foundation so subsequent
  admin writes can converge on the legacy modal interaction pattern instead of
  accumulating one-off inline forms.
- Added `AdminApiService` write calls for:
  - `POST /project/`
  - `PUT /project/`
  - `DELETE /project/{id}`
  - `DELETE /security/user/remove/{id}`
  - `PUT /security/user/add`
  - `POST /security/user/update`
  - `POST /security/user/preferences/update`
  - `POST /project/reload`
  - `POST /project/exception`
  - `GET /project/assign?projectId=...&userName=...&role=...`
  - `GET /project/unassign?projectId=...&userName=...`
- Added form validation, saving state, success/error notifications, cancel
  behavior, and user-list reload after save.
- Added stubbed browser smoke paths for add/edit project, add/edit user,
  project-role assignment, project-role removal, project-admin edit access,
  project creator assignment, project delete, validation data, precedence
  ordering, user deletion, user preference updates, reload config, forced
  exceptions, and dialog rendering so the test does not write to a real shared
  database.
- Deferred richer visual/interaction parity work, including drag-and-drop
  precedence ordering and tighter legacy modal/table styling, to the admin
  parity pass rather than Phase 5 functional completion.

### Phase 6: Process And Workflow

Status: Process config, algorithm-step, import/export, and execution-control
slices, and Workflow config add/edit/delete/import/export, bin definition
add/edit/delete/query test, bin regeneration, and checklist/worklist
detail/log/export/delete complete on 2026-06-10. Workflow bin display now
includes the legacy created/modified date, run-time, and cluster-stat columns.
The unsupported Sources, Terminology, and Metadata tabs were removed from
Angular 20 scope. Current-user workflow assignment actions are now wired for
worklists, selected-user assignment is wired for available worklists, and
manager reassign controls are wired for assigned worklist authors/reviewers,
checklist/worklist creation from workflow-bin cluster rows is implemented, and
worklist concept report generate/download/remove is wired in the inline
worklist detail panel. Checklist/worklist note add/remove is wired in the inline
detail panel. Reviewer/editor/administrator stamp/unapprove list actions,
review-assigned worklist finish, concept-status initialize/update, and
checklist compute are also wired. Epoch add/remove and checklist import are now
wired in the Workflow route.
Process execution progress/log polling, automatic running-state refresh, and
richer execution/step progress details are wired in the Process route.
Process algorithm-parameter query formatting and query testing are wired in the
algorithm step editor.
The Workflow route now wraps the legacy `/websocket` endpoint for live
bin/checklist/worklist updates and uses bin-regeneration progress polling as a
fallback/visibility layer during long-running regeneration calls.
Workflow bin definition query formatting and order editing are wired, including
drag-and-drop reordering and Up/Down fallback controls backed by a focused
definition-order REST endpoint.
No legacy worklist import endpoint or UI workflow was found, so worklist import
is out of NM-313 parity scope unless a future backend contract is added.
Phase 6 is functionally complete for the currently supported Process and
Workflow Angular 20 scope.

Goals:

- Migrate supported operational screens that are important but less
  editor-intensive than content editing.
- Build reusable table, modal, file-import, and confirmation patterns.

Suggested order:

1. process read-only views
2. process edit/import/export dialogs
3. workflow read-only views
4. workflow bins, epochs, checklists, worklists
5. workflow assignment and remaining import/export operations

Special care:

- process/workflow import behavior
- long-running operation feedback
- websocket or polling notifications
- permission-gated buttons
- large tables and paging/sort/filter behavior

Acceptance:

- Users can complete common operational workflows in Angular 20.
- Long-running operations provide feedback equivalent to AngularJS.
- Errors are visible and actionable.
- REST requests match old UI semantics.

Phase 6 initial implementation notes:

- Added a shared project-context service for Angular 20 routes that need the
  legacy selected project:
  - reads `lastProjectId` from stored user preferences
  - reads `lastProjectRole` from preferences, falling back to `projectRoleMap`
    for the selected project when present
  - keeps project-role tabs unavailable when no selected project exists, so the
    existing fallback behavior still protects `/edit`, `/process`, `/workflow`,
    and `/inversion`
- Updated project-tab permission handling so application `USER` and
  `ADMINISTRATOR` users with a selected project can enter project-backed
  operational routes, matching the server-side `authorizeProject` behavior.
- Replaced the placeholders for `/process` and `/workflow` with Angular 20
  route foundations.
- Added a shared `OperationalApiService`, typed models, and list/PFS helpers
  for the Phase 6 REST calls.
- Process route:
  - uses `POST /process/config/find?projectId=...&query=`
  - uses `POST /process/execution/find?projectId=...&query=`
  - uses `GET /process/executing?projectId=...`
  - shows process configs, recent executions, running executions, and selected
    config/execution detail
  - hydrates selected configs and executions through `GET /process/config/{id}`
    and `GET /process/execution/{id}` so the detail panels can show configured
    and executed algorithm steps
  - supports process type filtering with the legacy process-type set:
    `Insertion`, `Inversion`, `Maintenance`, `Release`, `Report`, and `Autofix`
  - supports independent paging for configs and executions so large process
    lists are reachable
  - supports project-administrator prepare, execute, cancel, restart, step, and
    unstep actions through the legacy process REST endpoints
  - supports process execution feedback through `GET /process/executing`,
    `GET /process/{id}/progress`, `GET /process/algo/{id}/progress`,
    `GET /process/{id}/log`, and `GET /process/algo/{id}/log`, including
    automatic running-state refresh, selected execution polling, process log,
    active step log, and execution-info display
  - supports project-administrator process config add/edit/delete through
    `PUT /process/config`, `POST /process/config`, and
    `DELETE /process/config/{id}?cascade=true`
  - supports project-administrator process config import/export through
    multipart `POST /process/config/import?projectId=...` and
    octet-stream `POST /process/config/export?projectId=...&processId=...`
  - supports process-config algorithm step add/edit/delete, enable/disable,
    validation, and order updates through the legacy algorithm config endpoints
    and ordered process-config updates
  - supports algorithm-parameter query formatting and `QUERY_ID` /
    `QUERY_ID_PAIR` testing through
    `GET /process/testquery?projectId=...&processId=...&queryType=...&queryStyle=...&query=...`
  - keeps process operation controls hidden for non-administrator project roles,
    matching the legacy `EditProcessOrStep` permission
- Workflow route:
  - uses `GET /workflow/config/all?projectId=...`
  - uses `GET /workflow/epoch/all?projectId=...`
  - uses `POST /workflow/checklist/find?projectId=...&query=`
  - uses `POST /workflow/worklist/find?projectId=...&query=`
  - uses `GET /workflow/bin/all?projectId=...&type=...`
  - shows workflow configs, bins for the selected config type, worklists,
    checklists, epochs, and selected bin detail
  - shows workflow-bin created date, modified date, run time, cluster type,
    all, assigned, and unassigned values to match the legacy bin table
  - supports project-administrator workflow config add/edit/delete through
    `PUT /workflow/config`, `POST /workflow/config`, and
    `DELETE /workflow/config/{id}?projectId=...`
  - supports project-administrator workflow config import/export through
    multipart `POST /workflow/config/import?projectId=...` and octet-stream
    `POST /workflow/config/export?projectId=...&workflowId=...`
  - supports project-administrator workflow bin definition add/edit/delete
    through `PUT /workflow/definition`, `POST /workflow/definition`, and
    `DELETE /workflow/definition/{id}?projectId=...`
  - supports workflow-bin definition query testing through
    `GET /workflow/query/test?projectId=...&query=...&queryType=...&queryStyle=...`
  - supports workflow-bin definition query formatting in the add/edit dialog
  - adds the legacy add-bin "position bin after" control using workflow-bin
    definition IDs for `positionAfterId`
  - loads autofix algorithm choices from `GET /process/algo/autofix?projectId=...`
    and shows a picklist for MID validation workflow configs
  - supports workflow-bin definition order edits through
    `POST /workflow/definition/order?projectId=...&workflowConfigId=...`,
    with drag-and-drop rows, Up/Down fallback controls, exact submitted-ID
    validation, existing workflow-bin rank updates, and BINS websocket
    notification
  - supports project-administrator workflow-bin regeneration:
    - all bins for the selected config use the legacy clear-then-regenerate
      sequence: `POST /workflow/bin/clear/all` followed by
      `POST /workflow/bin/regenerate/all`
    - single-bin regeneration uses `POST /workflow/bin/{id}/regenerate` for
      non-mutually-exclusive configs
    - the Angular UI blocks overlapping regenerations, preserves the selected
      bin during refreshes, and polls bins every 5 seconds while regeneration
      is running
  - supports live workflow updates by wrapping the existing
    `/umls-server-rest/websocket?{userName}` endpoint in a typed Angular
    service with reconnect, legacy ping, same-session filtering by
    `Authorization`, and `BINS`/`CHECKLIST`/`WORKLIST` refresh handling for the
    current project
  - supports checklist/worklist detail and log viewing in an inline selected
    detail panel through `GET /workflow/checklist/{id}`,
    `GET /workflow/worklist/{id}`, and `GET /workflow/log?projectId=...`
  - shows worklist author/reviewer assignment state in the inline detail panel
    and supports current-user assign, unassign, and legacy-state reassign
    through `GET /workflow/worklist/action?projectId=...&worklistId=...`
  - supports reviewer/editor/administrator selected-user assignment for
    available worklists by loading the selected project and assigned project
    users from `GET /project/{projectId}` and
    `POST /project/{projectId}/users`, filtering by worklist team and target
    role, then calling
    `GET /workflow/worklist/action?projectId=...&worklistId=...&action=ASSIGN`
    with an optional worklist note
  - supports project-administrator removal of assigned worklist authors and
    reviewers from the inline detail panel through
    `GET /workflow/worklist/action?projectId=...&worklistId=...&action=UNASSIGN`
  - supports reviewer/editor/administrator reassign controls for an assigned
    worklist author or reviewer when the legacy workflow handler allows
    `action=REASSIGN` for the current worklist state
  - supports reviewer/editor/administrator concept-status initialize/update
    through `POST /workflow/status/compute?projectId=...`
  - supports checklist compute through
    `POST /workflow/checklist/compute?projectId=...&name=...&query=...&queryType=...`,
    including duplicate-name validation, cluster count, skip count, query
    formatting, and query testing
  - supports checklist/worklist stamp and unapprove actions through
    `POST /workflow/checklist/{id}/stamp?projectId=...&approve=...` and
    `POST /workflow/worklist/{id}/stamp?projectId=...&approve=...`
  - supports review-assigned worklist finish actions through
    `GET /workflow/worklist/action?projectId=...&worklistId=...&action=FINISH`
  - supports worklist concept report status, generation, download, and removal
    in the inline worklist detail panel through
    `POST /workflow/report?projectId=...&query=...`,
    `GET /workflow/worklist/{id}/report/generate?projectId=...&sendEmail=true`,
    `GET /workflow/report/{fileName}?projectId=...`, and
    `DELETE /workflow/report/{fileName}?projectId=...`
  - supports checklist/worklist notes in the inline detail panel through
    `PUT /workflow/checklist/{id}/note?projectId=...`,
    `DELETE /workflow/checklist/note/{noteId}?projectId=...`,
    `PUT /workflow/worklist/{id}/note?projectId=...`, and
    `DELETE /workflow/worklist/note/{noteId}?projectId=...`
  - supports checklist creation from non-`all` workflow-bin cluster rows through
    `POST /workflow/checklist?projectId=...&workflowBinId=...`
  - supports worklist creation from non-`all` workflow-bin cluster rows through
    `PUT /workflow/worklist?projectId=...&workflowBinId=...`, including the
    legacy cluster count, skip count, sort order, and number-of-worklists
    controls
  - supports checklist/worklist export and delete through
    `GET /workflow/checklist/{id}/export`,
    `GET /workflow/worklist/{id}/export`, `DELETE /workflow/checklist/{id}`,
    and `DELETE /workflow/worklist/{id}`
  - supports workflow epoch add/remove through `PUT /workflow/epoch` and
    `DELETE /workflow/epoch/{id}?projectId=...`
  - supports checklist import through multipart
    `POST /workflow/checklist/import?projectId=...&name=...`, including
    duplicate-name validation before import
- Added Jest coverage for the operational API helper behavior.
- Added Cypress smoke coverage for Process and Workflow route load,
  representative data rendering, process import/export, process config
  mutations, algorithm-step mutations, process execution controls, workflow
  config add/edit/delete/import/export, and workflow bin definition
  add/edit/delete, checklist creation, worklist creation, and current-user
  plus selected-user worklist assignment and unassignment, reviewer
  stamp/finish, worklist concept reports, and checklist/worklist notes.
- Deferred behavior:
  - worklist import is intentionally not listed for parity because no legacy
    worklist import endpoint or UI workflow exists

### Phase 7: Content And Edit Workflows

Status: inventory and Angular 20 route foundation started on 2026-06-22.
The new UI treats Edit as the owner for content display and modification. The
`/edit` tab opens the content workbench, while `/content` and mode-based
content report URLs remain as Edit-authorized deep links rather than a
separate Content tab. A typed read-only content API scaffold is in place for
the first search/detail slice, and read-only content list search is wired for
concept, code, and descriptor search-result rows. Selecting a search result now
loads the full component detail by terminology/version/terminologyId and renders
read-only atoms, definitions, semantic types, attributes, and relationship
summaries. Mode-based report routes now load component detail directly from the
route context and render a first read-only report view without requiring a
search, including the legacy preformatted report payload from `/report`.
The report view also loads first-page read-only expansions for hierarchies,
deep relationships, mappings, subset memberships, and notes when those legacy
content endpoints return data.
A typed mutation API scaffold now covers the first safety-critical `/edit` and
`/meta` calls. The selected concept detail panel now exposes the first live
guarded edit action: concept approval through `POST /meta/concept/approve`,
including project `editingEnabled` checks, author-level role gating,
`lastModified` stale-update visibility, activity id input, browser confirmation,
validation error display, and the legacy-style warning-only override retry.
The selected concept workbench also has a shared current Activity ID field so
approval/add/remove panels can reuse the assigned workflow/worklist activity
while still allowing per-action overrides.
The selected concept panel also exposes the legacy simple concept update path
through `POST /edit/concept`, currently scoped to workflow status and
publishable edits with project editing state, author-level role gating, browser
confirmation, inline failures, and post-save component refresh.
The same selected concept panel also exposes read-only concept validation
through `POST /content/validate/concept`, with optional project validation-check
selection and inline errors, warnings, and comments.
Selected atom rows now expose read-only atom validation through
`POST /content/validate/atom`, with inline validation errors, warnings, and
comments before fuller atom add/update parity.
Selected atom rows also expose a read-only code-concepts lookup for atoms with
a code id, using the legacy `atoms.codeId:<code>` concept query and rendering
the matching concepts in the Edit workbench before fuller popout parity.
Selected atom rows also expose the legacy simple atom update path through
`POST /edit/atom`, scoped to atom name, termgroup, language, publishable, and
suppressible fields with project editing state, author-level role gating,
browser confirmation, inline failures, and component refresh.
Selected atom rows now expose a fuller guarded atom edit path through
`POST /meta/atom/update`, preserving the legacy immutable update fields and
allowing PN atom publishable edits with warning override and component refresh.
The selected concept panel now includes a live contexts browser backed by
`POST /content/concept/{terminology}/{version}/{terminologyId}/treePositions/deep`,
with text filtering, context row display, and guarded Open actions for
supported component types.
The edit workbench also exposes guarded undo/redo controls for molecular
actions through `POST /meta/action/undo` and `POST /meta/action/redo`, requiring
project editing state, author-level role, molecular action id, activity id, and
browser confirmation before execution.
Workflow finish parity now preserves the legacy time-entry sequence: the
Angular workflow worklist Finish action collects hours/minutes, updates
author/reviewer time through `POST /workflow/worklist`, then performs
`FINISH` through `/workflow/worklist/action`.
The selected concept panel now also exposes a limited merge path through
`POST /meta/concept/merge`, with target concept search/select, manual target
concept id input, default or reverse merge order, activity id, the correct
from-concept `lastModified`, warning override, and refresh safeguards.
The selected concept atom list now exposes the first guarded atom mutation:
atom removal through `POST /meta/atom/remove/{id}`, with activity id,
`lastModified`, project editing state, browser confirmation, validation result
display, and warning-only override retry.
The selected concept panel also exposes guarded atom add through
`POST /meta/atom/add`, using project `newAtomTermgroups`, project/default
language, required source identifiers, workflow status, activity id,
`lastModified`, validation result display, and warning-only override retry.
It also exposes the first guarded atom update path through
`POST /meta/atom/update` by updating a selected atom's workflow status to the
legacy-supported `NEEDS_REVIEW` or `READY_FOR_PUBLICATION` values, keeping the
request limited to backend-allowed atom status fields and preserving the same
activity id, `lastModified`, warning override, and refresh safeguards.
Selected concept atoms now also have limited move coverage through
`POST /meta/atom/move`, with row-level atom selection, target concept
search/select, manual target concept id, activity id, `lastModified`, warning
override, and refresh safeguards. Fuller legacy merge/move/split modal parity
remains a later Phase 7 refinement.
Selected concept atoms now also have limited split coverage through
`POST /meta/concept/split`, with row-level atom selection, copy
relationships/semantic-types control, legacy inverse relationship lookup,
activity id, `lastModified`, warning override, and refresh safeguards.
The selected concept semantic type list now exposes the same guarded removal
pattern through `POST /meta/sty/remove/{id}`.
It also loads semantic type metadata from `/metadata/sty/{terminology}/{version}`
and exposes guarded semantic type add through `POST /meta/sty/add`.
The selected concept attribute and relationship sections now expose matching
guarded removal paths through `POST /meta/attribute/remove/{id}` and
`POST /meta/relationship/remove/{id}`.
The relationship section also exposes guarded explicit add paths through
`POST /meta/relationship/add` and `POST /meta/relationships/add`, using the
legacy accepted relationship type list, the legacy inverse-relationship lookup,
target concept search/select, selected multi-target batching, manual target
concept id input, activity id, `lastModified`, warning override, and refresh
safeguards. Broader legacy relationship workbench modal parity remains a later
visual/interaction refinement.
The selected concept attribute section also exposes guarded attribute add
through `POST /meta/attribute/add`, with name/value inputs and warning override.
Selected component notes are also surfaced in the workbench, with add/remove
coverage through `POST /content/{type}/{id}/note` and
`DELETE /content/{type}/note/{noteId}`. These remain legacy content-note
operations rather than activity-bound `/meta` edits, and refresh the selected
component detail after each mutation.

Goals:

- Migrate the highest-risk workflows last.
- Preserve editor productivity and data safety.

High-risk areas:

- content search/detail/edit
- atoms
- relationships
- attributes
- semantic types
- contexts
- merge/move/split
- finish workflow
- code concepts
- popout windows
- websocket-driven notifications

Approach:

- Break edit workflows into narrow workbench slices.
- Prefer read-only detail parity before write operations.
- Use production-like local data for smoke testing before user acceptance.

Acceptance:

- Critical edit operations match legacy behavior.
- Unsaved-change and destructive-action guards are explicit.
- Content/edit routes have the strongest e2e coverage in the migration.

Phase 7 initial inventory notes:

- Legacy AngularJS routes:
  - `/content` uses `ContentCtrl` and `app/page/content/content.html`
  - `/content/:mode/:type/:terminology/:version/:terminologyId` uses
    mode-specific content templates such as `simple.html`
  - `/content/:mode/:type/:terminology/:id` supports shorter report links
  - `/edit` uses `EditCtrl` and `app/page/edit/edit.html`
  - `/edit/semantic-types` uses `SemanticTypesCtrl`
  - `/edit/codeConcepts` uses `CodeConceptsCtrl`
  - `/edit/atoms` uses `AtomsCtrl`
  - `/edit/relationships` uses `RelationshipsCtrl`
  - `/contexts` uses `ContextsCtrl`; it is not nested under `/edit`, but is
    part of the edit popout family
- Legacy read-only content REST groups:
  - component detail: `GET /content/{type}/{id}` and
    `GET /content/{type}/{terminology}/{version}/{terminologyId}`
  - component search/list: `POST /content/{type}/{terminology}/{version}`
    with `query` plus PFS payload
  - exact concept query lookup:
    `POST /content/concept/{terminology}/{version}/get`
  - autocomplete:
    `GET /content/{type}/{terminology}/{version}/autocomplete/{searchTerm}`
  - relationship/facet expansion: relationships, deep relationships, trees,
    tree children, tree roots, subsets, mappings, members, notes, and
    validation endpoints under `/content`
- Legacy mutation REST groups:
  - simple edit service uses `/edit` for simple atom, semantic type, concept,
    and bulk concept remove calls
  - meta editing service uses `/meta` for atom, attribute, relationship,
    semantic type, merge, move, split, approve, undo, and redo calls
  - edit mutations rely on `lastModified`, `activityId`, warning override
    prompts, and action error/warning modals; Angular 20 must preserve these
    safety semantics before write actions are enabled
- Angular 20 foundation added:
  - `/edit` backed by `ContentComponent` so the Edit tab displays the content
    workbench
  - explicit `/content` and mode-based content routes backed by
    `ContentComponent` as Edit-authorized deep links
  - explicit edit popout and `/contexts` routes backed by
    `EditWorkbenchComponent`
  - selected concept popout launchers for semantic types, code concepts, atoms,
    relationships, and contexts; the Angular routes now preserve selected
    concept, project, and activity context through query params and offer a
    return link into the full Edit detail
  - `ContentEditApiService` and typed content models for read-only search and
    detail endpoint construction
  - read-only content list search through
    `POST /content/{type}/{terminology}/{version}?query=...`, including current
    terminology defaults, legacy suppressible/anonymous PFS restrictions,
    paging, sorting, result selection, and Cypress smoke coverage
  - selected-result detail loading through
    `GET /content/{type}/{terminology}/{version}/{terminologyId}` with project
    context, rendering read-only atoms, definitions, semantic types,
    attributes, and relationship summaries for report-view preparation
  - definition rendering now preserves safe rich text markup, plain-text line
    breaks, atom-source indicators, and suppressible/obsolete indicators in
    both Edit detail and report sections without carrying forward TinyMCE
  - mode-based report rendering for `/content/:mode/:type/:terminology/:version/:terminologyId`
    so simple report links load detail directly and show read-only report
    sections plus the legacy `/report/{type}/{id}` preformatted payload without
    exposing a separate Content tab
  - report expansion calls for first-page hierarchies, deep relationships,
    mappings, subset memberships, and notes, with independent error handling so
    one report-adjacent endpoint does not blank the component report
  - typed mutation API scaffold for concept update, atom add/update/remove,
    concept approval, and undo/redo, plus validation-result helpers for
    hard errors versus warning-only override paths
  - selected concept approval in the content detail panel, including activity id
    input, project `editingEnabled` lookup, stale-update timestamp visibility,
    browser confirmation, validation errors, warning-only override retry, and
    Cypress smoke coverage with mocked `/meta/concept/approve` calls
  - shared selected-concept Activity ID fallback for guarded mutation panels,
    with per-action override fields retained for cases that need different
    activities
  - selected concept simple update through `/edit/concept`, scoped to workflow
    status and publishable updates, with project editing checks, author-level
    role gating, confirmation, and component refresh
  - selected concept validation through `/content/validate/concept`, including
    project validation-check options and inline errors, warnings, and comments
  - selected atom validation through `/content/validate/atom`, including inline
    errors, warnings, and comments ahead of fuller atom add/update parity
  - selected atom code-concepts lookup through the existing concept search
    endpoint with `atoms.codeId:<code>` query semantics, rendering matching
    concepts in the Edit workbench with Cypress smoke coverage
  - selected atom simple update through `/edit/atom`, scoped to name,
    termgroup, language, publishable, and suppressible fields, with project
    editing checks, author-level role gating, confirmation, and component
    refresh
  - selected atom edit through `/meta/atom/update`, currently scoped to the
    legacy PN publishable edit field while preserving read-only atom identity
    fields, warning override, and post-action component refresh
  - live selected concept contexts through
    `/content/concept/{terminology}/{version}/{terminologyId}/treePositions/deep`,
    including text filter, tree-position table display, and Cypress coverage
  - edit workbench undo/redo controls for `/meta/action/undo|redo`, including
    activity id, molecular action id, force confirmation, validation result
    display, and mocked Cypress coverage
  - worklist finish workflow time entry through `/workflow/worklist` followed
    by `/workflow/worklist/action?action=FINISH`, matching the legacy
    hours/minutes author/reviewer time sequence with Cypress coverage
  - limited concept merge through `/meta/concept/merge`, including target
    concept search/select, manual target concept id input, default/reverse
    merge order, warning override, and post-action component refresh
  - selected concept atom removal through `/meta/atom/remove/{id}`, including
    activity id, `lastModified`, validation errors/warnings, explicit warning
    override retry, and post-action component refresh
  - selected concept atom add through `/meta/atom/add`, using project
    `newAtomTermgroups`, project/default language, source identifiers, workflow
    status, warning override, and post-action component refresh
  - selected concept atom status update through `/meta/atom/update`, currently
    scoped to the legacy atom workflow statuses with the same warning override
    and refresh safeguards
  - limited selected concept atom move through `/meta/atom/move`, including
    row-level atom selection, target concept search/select, manual target
    concept id input, warning override, and post-action component refresh
  - limited selected concept split through `/meta/concept/split`, including
    row-level atom selection, copy related-data flag, legacy inverse
    relationship lookup, warning override, and post-action component refresh
  - selected concept semantic type removal through `/meta/sty/remove/{id}`,
    with the same activity id, `lastModified`, warning override, and refresh
    safeguards
  - selected concept semantic type add through `/meta/sty/add`, populated from
    `/metadata/sty/{terminology}/{version}` and guarded by the same activity id,
    `lastModified`, warning override, and refresh safeguards
  - selected concept attribute and relationship removal through
    `/meta/attribute/remove/{id}` and `/meta/relationship/remove/{id}`, with
    the same activity id, `lastModified`, warning override, and refresh
    safeguards
  - limited selected concept relationship add through
    `/meta/relationship/add`, including target concept search/select, manual
    target concept id input, legacy inverse-relationship lookup, warning
    override, and post-action component refresh
  - selected concept relationship batch add through `/meta/relationships/add`,
    including selected target concept list management, legacy inverse
    relationship lookup, warning override, and post-action component refresh
  - selected concept attribute add through `/meta/attribute/add`, with name and
    value inputs plus the same activity id, `lastModified`, warning override,
    and refresh safeguards
  - selected component note add/remove through
    `/content/{type}/{id}/note` and `/content/{type}/note/{noteId}`, including
    inline error display and post-action component refresh
  - edit workbench readiness panel documenting staged mutation actions and
    safety gates before any live write buttons are exposed
  - helper coverage for PFS payloads, content type path normalization, and
    list-response normalization

### Phase 8: Visual Parity Pass

Status: not started as of 2026-06-25. All migrated tabs are functionally
complete but each was built as a minimum-viable functional slice without
attention to layout, spacing, density, or interaction patterns from the legacy
UI. This phase brings the Angular 20 UI into visual and interaction alignment
with what MEME editors already know before those tabs become the default.

Doing visual parity in one dedicated sweep rather than per-screen allows shared
CSS decisions — color palette, spacing scale, table density, panel geometry,
button placement, form-field sizing — to be made once and applied consistently.
Per-screen cleanup done earlier would force each screen to be revisited the
moment a shared pattern is changed.

Goals:

- Match the visual grammar of the legacy AngularJS UI closely enough that
  experienced editors feel at home without retraining.
- Centralize all visual decisions in shared CSS custom properties and a small
  number of shared class patterns so each screen inherits changes automatically.
- Eliminate the per-component CSS fragmentation that has accumulated: some
  components use `admin.component.css`, others use `operations.component.css`,
  and the content-edit component has no separate CSS file at all.
- Verify each screen side-by-side against the live legacy UI before the tab is
  considered parity-complete.

Approach:

The work proceeds in four ordered layers. Later layers benefit from earlier ones
and should not start until the earlier layer is stable.

**Layer 1: Design-token foundation**

Establish a shared set of CSS custom properties in `frontend/src/styles.css`
that capture the core visual decisions extracted from the legacy stylesheet
(`src/main/webapp/css/style.css` and `tsApp.css`):

- Color palette: background, surface, border, text primary/secondary/muted,
  brand accent, error/warning/success state colors.
- Spacing scale: a small set of named gap/padding sizes (--space-xs through
  --space-xl) used consistently rather than arbitrary px values in each
  component.
- Typography: font family, base size, line height, heading scale, monospace
  family for IDs/codes.
- Table density: row height, cell padding, header weight and background matching
  the legacy ng-table style.
- Form geometry: input height, label weight, field gap, disabled appearance.
- Panel geometry: section header height and weight, step-panel border and
  background, toolbar gap.
- Z-index scale: overlay, dialog, notification tiers.

These tokens replace the ad-hoc px values currently scattered across
`admin.component.css`, `operations.component.css`, `dialog.component.css`, and
the inline styles in content templates.

**Layer 2: Shared component pass**

Update the reusable shared components before touching any feature screen, since
these appear on every migrated tab. A fix here multiplies across all screens
for free:

- `meme-dialog` — visual match to the legacy `$uibModal` overlay: backdrop
  opacity, dialog width, header style, close-button placement, footer
  (form-actions) alignment.
- `meme-notifications` — match legacy toast position, color coding, and
  auto-dismiss timing.
- `meme-loading` — match legacy spinner style and placement.
- Global table styles — create a shared `.meme-table` class (or apply styles
  via the `:host` token in a shared stylesheet) that matches legacy ng-table
  density: font size, row height, hover highlight, header background and
  border.
- Global button styles — primary, secondary, danger, and disabled appearances
  consistent with legacy Bootstrap-derived button styles.
- Global form-field styles — `label + input`, `select`, `textarea`, and
  `form-errors` aligned to legacy form patterns.
- Global panel styles — `.step-panel`, `.section-header`, `.toolbar`,
  `.context-panel`, `.list-panel` classes aligned to legacy panel geometry and
  header weight.

**Layer 3: Per-screen parity pass**

Work through each tab in the same order as functional migration. For each
screen, open the legacy UI and the Angular 20 UI side-by-side and record
differences in layout, spacing, table density, button placement, and section
organization, then apply targeted fixes.

*Shell and navigation*
- Header: logo placement, project selector appearance, active-tab highlight,
  user/logout button placement.
- Footer: text and link alignment.
- Tab bar: selected-tab indicator, disabled-tab appearance, tab label casing.

*Admin tab*
- User list: column widths, action-button alignment, role badge appearance.
- Project list: column widths, expand/collapse geometry.
- Add/edit user dialog: label alignment, field spacing, role checkboxes layout.
- Project detail view: validation table, precedence table, section headers.

*Operations tab (Inversion)*
- Toolbar: VSAB input width, button grouping.
- ID-ranges table: column widths and density.
- Request / Adjust dialogs: field layout, button placement.

*Workflow tab*
- Worklist and checklist tables: column widths, status badge appearance,
  action-button placement.
- Worklist detail / finish dialog: time-entry field layout, button placement.

*Process tab*
- Process config list and execution list: column widths, status indicator,
  action buttons.
- Algorithm config detail: step-panel organization, field layout.

*Edit / Content tab*
This is the largest and most visible screen. Priority order within the tab:

1. Overall layout — two-column split (search/list on left, detail on right)
   matching legacy edit.html panel geometry.
2. Search toolbar — input width, button grouping, paging controls.
3. Result list — row density, selected-row highlight, concept-id/name column
   widths.
4. Concept detail header — concept name size and weight, ID display, status
   badges (workflow status, publishable, approved).
5. Step-panel organization — section header height/weight/border, collapse
   behavior if applicable, spacing between panels.
6. Atom table — column widths, move/split checkbox column, action-button
   placement, suppressible/obsolete row indicators.
7. Merge / Move / Split dialogs — source-context block at top, selected-atoms
   preview, target-search field and results, form-actions alignment.
8. Semantic types, relationships, attributes, notes sections — each section
   header and table matching legacy panel appearance.
9. Undo/redo and approve controls — placement and visual weight matching
   legacy toolbar actions.
10. Contexts browser — tree-position row density, filter field placement.

**Layer 4: Acceptance and review**

After each tab's screen pass is complete, conduct a side-by-side review with
a user familiar with the legacy UI. The review should cover:

- No major layout surprises for an editor who knows the legacy screen.
- Key data (concept ID, atom name, status) visible without horizontal scrolling
  at the standard browser window width used by editors.
- Dialogs open and close with the expected animation and backdrop.
- Error and warning messages visually prominent (red/yellow band, not just
  small inline text).
- Empty states and loading states consistent across all tabs.

High-risk areas:

- Edit/Content tab: the largest screen; most at risk for accumulated one-off
  CSS decisions from functional migration phases.
- Table density: legacy ng-table uses a tighter row height than browser-default
  tables; too-tall rows on wide tables consume screen real estate editors
  depend on.
- Form-field sizing: legacy Bootstrap input height is specific; mismatched
  heights in dialogs look immediately wrong.
- Color fidelity: brand accent color, header background, and step-panel
  background are visible on every screen and should be exact.

Out of scope for this phase:

- Drag-and-drop or sortable widgets not present in the current Angular 20 UI.
- Pixel-perfect match; the goal is visual familiarity, not a screenshot diff.
- Legacy browser libraries (angular-ui-tree, TinyMCE visual skin) are not
  ported; their Angular 20 replacements should look appropriate but need not
  match exactly.
- Dark mode or accessibility contrast upgrades beyond what the legacy UI
  provided; those belong in a separate accessibility ticket.

Deliverables:

- Populated `--meme-*` CSS custom property set in `styles.css`.
- Shared table, button, form, and panel class patterns documented in a short
  `frontend/docs/visual-system.md` note so future screens can follow them
  without re-inventing.
- Per-tab parity checklist completed (layout, density, dialogs, states).
- No screen has more than cosmetic differences from its legacy counterpart.

Acceptance:

- An editor familiar with the legacy UI can navigate all migrated tabs without
  remarking on visual or layout surprises.
- Shared CSS custom properties account for all colors, spacing sizes, and
  typography decisions — no arbitrary px or hex values in per-component CSS
  that are not derived from a token.
- Every migrated screen passes side-by-side review by at least one user before
  Phase 9 begins.

### Phase 9: Cutover And AngularJS Retirement

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
3. admin read-only users/projects
4. small admin mutations
5. process read-only and process operations
6. workflow read-only and workflow operations
7. content read-only detail/search
8. edit workbench and mutation-heavy content workflows
9. websocket-heavy and popout-heavy workflows
10. visual parity pass (design tokens, shared components, per-tab review)
11. AngularJS removal

Rationale:

- Login is small, but it is foundational and touches session compatibility.
- Admin appears peripheral, but full admin is not low risk because it mutates
  users, projects, roles, validation, precedence, and reload state.
- Sources, Terminology, and Metadata are explicitly out of Angular 20 scope, so
  early parity work should stay on Admin before moving into Process/Workflow.
- Content/edit should come late because productivity, data safety, websocket
  behavior, and popouts all matter.
- Visual parity is deliberately deferred until all functional screens are
  complete so shared CSS decisions (tokens, panel classes, table density) can
  be made once and applied consistently across every tab rather than being
  re-litigated per screen.

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
- interaction pattern, including dialogs, accordions/sections, confirmation
  prompts, keyboard behavior, and button placement
- visual comparison notes for spacing, typography, table density, form layout,
  and action placement
- empty state
- error state
- loading state
- permission-gated actions
- shared style/component opportunities that would reduce one-off CSS

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

### NM-313C: Removed Read-Only Tab Slice

- Sources, Terminology, and Metadata are not supported Angular 20 tabs.
- Keep deploy-tab parsing tolerant of those legacy keys.
- Do not add routes, navigation entries, feature components, or Cypress smoke
  coverage for those tabs.

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

- Migrate process and workflow routes in staged read-only then mutation slices.
- Establish shared table/modal/file-import/long-running-operation patterns.

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
