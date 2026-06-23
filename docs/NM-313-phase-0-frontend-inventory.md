# NM-313 Phase 0 Frontend Inventory

## Summary

This document completes the Phase 0 inventory for the AngularJS-to-Angular 20
migration plan.

Phase 0 focused on:

- current AngularJS route ownership
- REST/service usage by the frontend
- application and project role behavior
- screen risk and migration order
- local old/new UI runtime model
- legacy smoke-test notes

No Angular 20 source was added in this phase. The output is an implementation
orientation document for the first scaffold and feature-slice tickets.

Scope update, 2026-06-10: Sources, Terminology, and Metadata are no longer
supported Angular 20 tabs. References to those routes below are retained as
historical AngularJS inventory, not as active migration targets.

## Inventory Date

2026-06-08

## Source Areas Reviewed

Primary AngularJS files:

- `src/main/webapp/index.html`
- `src/main/webapp/app/app.js`
- `src/main/webapp/app/routes.js`
- `src/main/webapp/app/appConfig.js`
- `src/main/webapp/app/permissions.js`
- `src/main/webapp/app/page/**`
- `src/main/webapp/app/component/**`
- `src/main/webapp/app/util/**`

Backend/config files relevant to the UI:

- `src/main/resources/application.properties`
- `config/local/setenv.sh`
- `src/main/java/com/wci/umls/server/helpers/PropertyUtility.java`
- `src/main/java/com/wci/umls/server/rest/impl/ConfigureServiceRestImpl.java`
- `src/main/java/com/wci/umls/server/model/algo/UserRole.java`
- `src/main/java/com/wci/umls/server/rest/impl/RootServiceRestImpl.java`

## Current Frontend Size

AngularJS app inventory:

| Area | Count |
| --- | ---: |
| `.js` and `.html` files under `src/main/webapp/app` | 184 |
| JavaScript lines under `src/main/webapp/app` | 24,240 |
| HTML template lines under `src/main/webapp/app` | 7,376 |
| `$http` call sites | 218 |

Page-area size:

| Page Area | Files | Lines | Notes |
| --- | ---: | ---: | --- |
| `edit` | 28 | 6,226 | Highest-risk editor/worklist area |
| `workflow` | 18 | 2,915 | Many workflow endpoints and websocket usage |
| `process` | 8 | 1,964 | Long-running process execution and upload |
| `admin` | 11 | 1,832 | Users, projects, assignments, validation, reload |
| `content` | 8 | 1,589 | Search, tree/list modes, notes, favorites, popouts |
| `source` | 3 | 739 | File upload and background load/remove operations |
| `inversion` | 4 | 420 | Source ID range operations |
| `terminology` | 2 | 342 | Read-only list/details plus export and nav |
| `metadata` | 2 | 129 | Read-only display; depends on selected terminology model |
| `login` | 2 | 110 | Small but session-critical |
| `configure` | 2 | 89 | Setup/destructive config area |
| `header` | 2 | 79 | Shell/navigation support |
| `license` | 2 | 73 | License cookie flow |
| `landing` | 2 | 55 | Startup/entry route |
| `general` | 7 | 46 | Shared general templates |
| `footer` | 2 | 146 | Shell/footer support |

Service size:

| Service | Lines |
| --- | ---: |
| `workflowService.js` | 1,605 |
| `contentService.js` | 1,221 |
| `metadataService.js` | 918 |
| `metaEditingService.js` | 831 |
| `securityService.js` | 805 |
| `processService.js` | 728 |
| `projectService.js` | 610 |
| `sourceDataService.js` | 332 |
| `editService.js` | 257 |
| `websocketService.js` | 243 |
| `reportService.js` | 158 |
| `tabService.js` | 156 |
| `configureService.js` | 100 |
| `inversionService.js` | 99 |

## Runtime Configuration Inventory

The AngularJS app does not use static route configuration alone. On first route
change, `routes.js` calls `configure/properties`, merges the returned values
into `appConfig`, and then registers routes dynamically.

The backend returns UI config through:

- `ConfigureServiceRestImpl.getConfigProperties()`
- `PropertyUtility.getUiProperties()`

`PropertyUtility.getUiProperties()` exposes:

- all properties beginning with `deploy.`
- `base.url`
- security properties that begin with `security` and contain `url`
- any property whose key contains `enabled`

Important properties:

| Property | Default In `application.properties` | Local `setenv.sh` Default | Migration Note |
| --- | --- | --- | --- |
| `server.servlet.context-path` | `/umls-server-rest` | `/umls-server-rest` | Existing backend context path |
| `base.url` | `http://localhost:${server.port}${server.servlet.context-path}` | `http://localhost:${SERVER_PORT}${SERVER_CONTEXT_PATH}` | Existing REST test base |
| `deploy.enabled.tabs` | `metadata,workflow,edit,admin,process,inversion` | `workflow,edit,admin,process,inversion` | Local default enables the migrated Angular 20 feature tabs; content display/edit is owned by `edit` |
| `deploy.landing.enabled` | `true` | `true` | Controls `/landing` and root route |
| `deploy.license.enabled` | `true` | `true` | Controls `/license` and license cookie check |
| `deploy.login.enabled` | `true` | `true` | Controls `/login`; source tab also requires login |
| `deploy.simpleedit.enabled` | `false` | `false` | Any Angular 20 content/edit split should preserve this flag |
| `security.guest.disabled` | `true` | inherited | Guest behavior still exists in frontend |
| `security.handler` | `DEFAULT` | inherited | Default local auth users are property-driven |

Phase 1/2 implication:

- The Angular 20 shell needs a startup config service before route/nav parity.
- The first read-only feature slice may need local test overrides such as:

```bash
export DEPLOY_ENABLED_TABS=workflow,edit,admin,process,inversion
```

or the Angular 20 feature can be tested with a smaller tab list that still
includes the route under active development.

## Route Ownership Inventory

All routes are currently owned by AngularJS.

Routes are registered in `src/main/webapp/app/routes.js`.

| Route | Template | Controller | Enabled By | Migration Notes |
| --- | --- | --- | --- | --- |
| `/configure` | `app/page/configure/configure.html` | `ConfigureCtrl` | Always registered | Admin/setup flow; can remain legacy until late |
| `/source` | `app/page/source/source.html` | `SourceCtrl` | `source` tab and login enabled | File upload/background operations; not first slice |
| `/content` | `app/page/content/content.html` | `ContentCtrl` | `content` tab | Large search/content surface; later |
| `/terminology` | `app/page/terminology/terminology.html` | `TerminologyCtrl` | `terminology` tab | Best independent read-only first feature candidate |
| `/metadata` | `app/page/metadata/metadata.html` | `MetadataCtrl` | `metadata` tab | Small, but assumes selected terminology metadata model |
| `/workflow` | `app/page/workflow/workflow.html` | `WorkflowCtrl` | `workflow` tab | Large workflow surface; later |
| `/edit` | `app/page/edit/edit.html` | `EditCtrl` | `edit` tab | Highest risk; migrate late |
| `/edit/semantic-types` | `app/page/edit/semantic-types/semanticTypesWindow.html` | `SemanticTypesCtrl` | `edit` tab | Popout/editor companion route |
| `/edit/codeConcepts` | `app/page/edit/codeConcepts/codeConcepts.html` | `CodeConceptsCtrl` | `edit` tab | Popout/editor companion route |
| `/edit/atoms` | `app/page/edit/atoms/atomsWindow.html` | `AtomsCtrl` | `edit` tab | Popout/editor companion route |
| `/edit/relationships` | `app/page/edit/relationships/relationshipsWindow.html` | `RelationshipsCtrl` | `edit` tab | Popout/editor companion route |
| `/contexts` | `app/page/edit/contexts/contextsWindow.html` | `ContextsCtrl` | `edit` tab | Popout/editor companion route |
| `/process` | `app/page/process/process.html` | `ProcessCtrl` | `process` tab | Long-running process management |
| `/admin` | `app/page/admin/admin.html` | `AdminCtrl` | `admin` tab | Start read-only, then mutate in slices |
| `/inversion` | `app/page/inversion/inversion.html` | `InversionCtrl` | `inversion` tab | Project-role operational workflow |
| `/content/:mode/:type/:terminology/:version/:terminologyId` | `app/page/content/{mode}.html` | `ContentCtrl` | Always registered | Dynamic report/simple mode |
| `/content/:mode/:type/:terminology/:id` | `app/page/content/{mode}.html` | `ContentCtrl` | Always registered | Dynamic report/simple mode |
| `/landing` | `app/page/landing/landing.html` | `LandingCtrl` | `deploy.landing.enabled=true` | Startup route |
| `/login` | `app/page/login/login.html` | `LoginCtrl` | `deploy.login.enabled=true` | Session-critical |
| `/license` | `app/page/license/license.html` | `LicenseCtrl` | `deploy.license.enabled=true` | License cookie flow |
| `/` | landing, login, license, or content | varies | Derived from deploy flags | Root priority is landing, then login, then license, then content |

Default route behavior:

- If landing is enabled, `/` routes to `/landing`.
- If landing is disabled and login is enabled, `/` routes to `/login`.
- If landing and login are disabled and license is enabled, `/` routes to
  `/license`.
- If landing, login, and license are all disabled, `/` routes to `/content`.
- `otherwise` redirects to `/`.

## Tab And Navigation Inventory

Tabs are initialized by `tabService.initEnabledTabs()` after config is loaded.

| Tab Key | Label | Link | Visibility Requirement |
| --- | --- | --- | --- |
| `source` | Sources | `source` | Application `USER` or `ADMINISTRATOR`; route also requires login enabled |
| `content` | Content | `content` | No application/project role requirement |
| `terminology` | Terminology | `terminology` | No application/project role requirement |
| `metadata` | Metadata | `metadata` | No application/project role requirement |
| `workflow` | Workflow | `workflow` | User has any project role |
| `edit` | Edit | `edit` | User has any project role |
| `process` | Process | `process` | User has any project role |
| `inversion` | Inversion | `inversion` | User has any project role |
| `admin` | Admin | `admin` | Application `USER` or `ADMINISTRATOR` |

Tab visibility is checked in `tabController.js`:

- Tabs with no role/project requirement always show.
- Application role tabs use `securityService.hasPrivilegesOf(tab.role)`.
- Project role tabs show when `projectService.getUserProjectsInfo().anyrole`
  is true.

## Auth And Session Inventory

The legacy session model is in `securityService.js`.

Session storage mechanisms:

- `$http.defaults.headers.common.Authorization`
- `localStorage.user`
- `user` cookie with path `/`
- `window.name` handoff for popout/new-window sessions

Login/logout endpoints:

- `POST security/authenticate/{userName}` with `text/plain` password body
- `GET security/logout/{authToken}`

License behavior:

- Cookie name is `WCI ` plus `deploy.title`.
- Cookie value is `license_accepted`.
- Expiration is refreshed to 30 days when license is checked.

Guest behavior:

- If login is disabled, the frontend can set a guest user.
- Guest user has:
  - `userName = guest`
  - `authToken = guest`
  - `applicationRole = VIEWER`

Phase 2 implications:

- Angular 20 should preserve the `Authorization` header and `user` cookie
  compatibility first.
- Local storage alone is not a reliable old/new bridge because it is origin
  scoped.
- `window.name` behavior matters for migrated popout/editor routes, but can wait
  until content/edit slices.
- `clearUser()` removes the `user` cookie and then iterates all cookies. Confirm
  exact intended behavior before rewriting logout.

## Role And Permission Inventory

Backend role enum:

- `VIEWER`
- `AUTHOR`
- `REVIEWER`
- `USER`
- `ADMINISTRATOR`

The same enum is used for application roles and project roles.

Backend privilege behavior from `UserRole.hasPrivilegesOf()`:

| Current Role | Has Privileges Of |
| --- | --- |
| `VIEWER` | `VIEWER` |
| `AUTHOR` | `VIEWER`, `AUTHOR` |
| `REVIEWER` | `VIEWER`, `USER`, `AUTHOR`, `REVIEWER` |
| `USER` | `VIEWER`, `USER`, `AUTHOR` |
| `ADMINISTRATOR` | all roles |

Authorization helpers:

- `RootServiceRestImpl.authorizeApp(...)` checks application role.
- `RootServiceRestImpl.authorizeProject(...)` allows application `USER` or
  `ADMINISTRATOR` to do anything, otherwise checks the user's project role.

Frontend application-role checks:

- `securityService.isAdmin()` means application role `ADMINISTRATOR`.
- `securityService.isUser()` means application role `USER`.
- `securityService.isViewer()` means application role `VIEWER`.
- `securityService.hasPrivilegesOf('USER')` is true for app `USER` and
  `ADMINISTRATOR`.

Frontend action permissions are registered in `permissions.js`.

| Permission | Project/App Roles Allowed |
| --- | --- |
| `CreateWorklist` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `CreateChecklist` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `RegenerateBins` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `EditEpoch` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `RecomputeConceptStatus` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `UndoRedo` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `GenerateReport` | `AUTHOR`, `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `ImportChecklist` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `RemoveChecklist` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `RemoveWorklist` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `Stamp` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `Unapprove` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `AssignWorklist` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `UnassignWorklist` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `EditProjectOrUser` | application `ADMINISTRATOR` only |
| `AddProjectOrUser` | application `USER` or `ADMINISTRATOR` |
| `EditProcessOrStep` | project `ADMINISTRATOR` |
| `OverrideEditDisabled` | `REVIEWER`, `EDITOR5`, `ADMINISTRATOR` |
| `AutofixMidValidation` | project `ADMINISTRATOR` |

Important frontend quirk:

- `EDITOR5` is not a backend `UserRole`.
- `securityService.hasPermissions()` converts project `AUTHOR` plus
  `editorLevel == 5` into `EDITOR5`.

Phase 2 implications:

- Implement a focused Angular permission service.
- Preserve `EDITOR5` synthesis.
- Test app-role and project-role guards separately.
- Test admin route behavior for app `VIEWER`, app `USER`, and app
  `ADMINISTRATOR`.

## Service And Endpoint Inventory

This is a frontend-facing inventory. It names the AngularJS service boundary,
REST root, approximate call count, and migration use.

| AngularJS Service | REST Root / Transport | HTTP/Transport Count | Main Uses | Migration Priority |
| --- | --- | ---: | --- | --- |
| `configureService` | `configure` | 4 | configured check, setup, destroy, UI properties | Phase 2 shell |
| `securityService` | `security` | 17 | auth, logout, users, roles, preferences, favorites | Phase 2 shell/admin |
| `projectService` | `project` | 20 | projects, assignments, roles, logs, validation names, reload, exception | Shell/admin/workflow/edit |
| `metadataService` | `metadata` | 26 | metadata model, terminologies, precedence, semantic types, term/attribute/relationship types | First read-only slice |
| `contentService` | `content` | 22 | search, component detail, trees, notes, favorites, mappings, validation, simple export | Content/edit later |
| `workflowService` | `workflow` | 60 plus 2 uploads | workflow configs, bins, checklists, worklists, reports, actions, imports/exports, progress | Workflow/edit later |
| `processService` | `process` | 29 plus 1 upload | process configs, algorithm configs, execution, progress, logs, import/export | Process later |
| `sourceDataService` | `file` | 15 | source-data files/data, load/remove/cancel/log | Source later |
| `sourceController` uploader | `file/upload` | 1 upload | source-data file upload | Source later |
| `editService` | `edit` | 9 | simple atom/concept/semantic type CRUD | Content/edit later |
| `metaEditingService` | `meta` | 16 | molecular edit operations, approve, merge, move, split, undo, redo | Edit last |
| `inversionService` | `inversion` | 4 | source ID range get/request/update/remove | Inversion later |
| `reportService` | `report` | 5 | component reports, report definitions, report generation/removal | Content/workflow later |
| `websocketService` | `/websocket?{userName}` | 1 socket plus event bus | favorite/note/workflow/checklist/worklist/concept events | Late foundation before workflow/edit |

Important endpoint examples by service:

### Configure

- `GET configure/configured`
- `POST configure/configure`
- `DELETE configure/destroy`
- `GET configure/properties`

### Security

- `POST security/authenticate/{userName}`
- `GET security/logout/{authToken}`
- `GET security/user/name/{userName}`
- `GET security/user/users`
- `GET security/user`
- `PUT security/user/add`
- `POST security/user/update`
- `DELETE security/user/remove/{id}`
- `GET security/roles`
- `POST security/user/find`
- `POST security/user/preferences/update`

### Project

- `GET project/{projectId}`
- `PUT project/`
- `POST project/`
- `DELETE project/{id}`
- `POST project/find`
- `POST project/{projectId}/users`
- `GET project/assign`
- `GET project/unassign`
- `GET project/roles`
- `GET project/queryTypes`
- `GET project/user/anyrole`
- `GET project/checks`
- `POST project/reload`
- `POST project/exception`

### Metadata

- `GET metadata/all/{terminology}/{version}`
- `GET metadata/precedence/{terminology}/{version}`
- `GET metadata/precedence/{precedenceListId}`
- `POST metadata/precedence`
- `GET metadata/terminology/current`
- `GET metadata/terminology/{terminology}/{version}`
- `GET metadata/rootTerminology/{terminology}`
- `GET metadata/sty/{terminology}/{version}`
- `GET/POST/PUT/DELETE metadata/termType...`
- `GET/POST/PUT/DELETE metadata/attributeName...`
- `GET/POST/PUT/DELETE metadata/relationshipType...`
- `GET/POST/PUT/DELETE metadata/additionalRelationshipType...`

### Content

- `GET content/{type}/{terminology}/{version}/{id}`
- `POST content/{type}/find`
- `POST content/{type}/tree`
- `POST content/{type}/childTrees`
- `POST content/{type}/roots`
- `POST content/relationships`
- `POST content/deepRelationships`
- `POST content/deepTreePositions`
- `POST content/{type}/note`
- `DELETE content/{type}/note/{noteId}`
- `POST content/favorites`
- `GET content/mapset/all/{terminology}/{version}`
- `POST content/mappings`
- `POST content/concept/validate`
- `POST content/concepts/validate`
- `GET content/export/simple/{terminology}/{version}`

### Workflow

- `GET workflow/paths`
- `GET/PUT/DELETE workflow/epoch...`
- `GET/POST/PUT/DELETE workflow/config...`
- `GET/POST/PUT/DELETE workflow/definition...`
- `POST workflow/assigned...`
- `POST workflow/available...`
- `POST workflow/done...`
- `GET workflow/bin/all`
- `GET workflow/worklist/{id}`
- `GET workflow/checklist/{id}`
- `POST workflow/worklist`
- `POST workflow/checklist`
- `DELETE workflow/worklist/{id}`
- `DELETE workflow/checklist/{id}`
- `POST workflow/action`
- `POST workflow/bin/{id}/records`
- `POST workflow/checklist/import`
- `GET workflow/checklist/{id}/export`
- `GET workflow/worklist/{id}/export`
- `POST workflow/recompute`
- progress/result endpoints for workflow-triggered processes

### Process

- `GET/POST/PUT/DELETE process/config...`
- `GET/POST/PUT/DELETE process/config/algo...`
- `GET process/execution/{id}`
- `GET process/execution/{id}/cancel`
- `GET process/executing`
- `GET process/{id}/progress`
- `GET process/algo/{id}/progress`
- `POST process/config/export`
- `POST process/config/import`
- `POST process/testQuery`

### Source Data

- `GET file/find`
- `POST file/update`
- `PUT file/add`
- `DELETE file/remove/{id}`
- `GET file/data/find`
- `GET file/data/all`
- `GET file/data/id/{id}`
- `GET file/data/sourceDataHandlers`
- `POST file/data/update`
- `PUT file/data/add`
- `DELETE file/data/remove/{id}`
- `POST file/data/load?background=true`
- `POST file/data/remove?background=true`
- `POST file/data/cancel`
- `GET file/data/log`
- `POST file/upload`

### Edit And Meta Editing

- `edit` service: simple atom, semantic type, and concept CRUD.
- `meta` service: molecular editing actions including atom, attribute,
  relationship, semantic type, approve, merge, move, remove, split, undo, redo.

These endpoints are high risk because they alter terminology content and often
interact with websocket event suppression.

## Page-To-Service Dependency Map

| Page Area | Primary Services | Migration Notes |
| --- | --- | --- |
| `login` | `securityService`, `configureService`, `tabService` | Small UI, high session importance |
| `landing` | `securityService`, `tabService` | Routes authorized users to preferred tab |
| `license` | `securityService`, `tabService` | Cookie flow before app access |
| `terminology` | `metadataService`, `contentService`, `projectService`, `securityService` | Best independent read-only candidate |
| `metadata` | `metadataService`, `projectService`, `securityService` | Depends on selected metadata model; redirects to `/content` if absent |
| `admin` | `projectService`, `securityService`, `metadataService`, `workflowService` | Split read-only from mutations |
| `source` | `sourceDataService`, `projectService`, `FileUploader` | File upload and background polling/logs |
| `process` | `processService`, `projectService`, `metadataService` | Execution/cancel/restart/progress |
| `workflow` | `workflowService`, `projectService`, `metadataService`, `processService`, `websocketService` | Large operational surface |
| `content` | `contentService`, `metadataService`, `projectService`, `securityService`, `websocketService` | Search/detail/tree/notes/favorites/popouts |
| `edit` | `workflowService`, `contentService`, `metadataService`, `projectService`, `metaEditingService`, `websocketService` | Highest risk; migrate late |
| `inversion` | `inversionService`, `projectService` | Project-scoped source ID ranges |

## Screen Risk Matrix

| Area | Risk | Why | Recommended Order |
| --- | --- | --- | --- |
| Shell/config/auth/navigation | High foundation risk | Every migrated route depends on config, tabs, auth headers, cookies, permissions | 1 |
| Terminology | Low to medium | Read-only table/detail, independent metadata load, small code surface | 2 |
| Metadata | Low to medium | Very small display route, but assumes selected terminology/model | 3 |
| Admin read-only | Medium | App roles, project/user tables, PFS queries, role lists | 4 |
| Admin mutations | Medium to high | Users/projects/assignments/validation/reload operations | 5 |
| Source | Medium to high | Upload, background load/remove/cancel, logs | 6 |
| Inversion | Medium | Project-scoped range creation/update/delete | 7 |
| Process | High | Long-running execution, progress polling, import/export, mutation-heavy configs | 8 |
| Workflow | High | Many endpoints, checklist/worklist lifecycle, websocket events, process progress | 9 |
| Content read-only | High | Complex search modes, trees, callbacks, history, favorites, notes, popouts | 10 |
| Edit/content mutations | Very high | Terminology edits, workflow actions, websocket suppression, unsaved-change risk | 11 |
| AngularJS removal | High cleanup risk | Requires route parity and production confidence | 12 |

Refinement from the planning doc:

- `terminology` is a better first independent read-only feature than
  `metadata`.
- `metadata` is still a good early feature, but Angular 20 should either migrate
  terminology first or make metadata load/select terminology independently
  instead of assuming a prior AngularJS shared model.

## Local Old/New Runtime Model

Current MEME local defaults:

- Backend and legacy AngularJS UI:

```text
http://localhost:8080/umls-server-rest/
```

- Common integration-test server override:

```text
http://localhost:18080/umls-server-rest/
```

Recommended Angular 20 development URL:

```text
http://localhost:4200/
```

Recommended first dev proxy:

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

Recommended frontend API base:

```text
/umls-server-rest
```

This lets Angular 20 call:

```text
/umls-server-rest/configure/properties
/umls-server-rest/security/authenticate/{userName}
/umls-server-rest/metadata/terminology/current
```

When packaged under the Spring Boot app, prefer:

```text
/umls-server-rest/ui20/
```

and keep API calls rooted at:

```text
/umls-server-rest
```

This avoids browser CORS and keeps same-origin cookie behavior available.

## Legacy Smoke-Test Notes

I checked whether a local MEME server was already running:

| URL | Result |
| --- | --- |
| `http://localhost:8080/umls-server-rest/` | no response |
| `http://localhost:18080/umls-server-rest/` | no response |

No screenshots were captured because no local app server was running, and Phase
0 should not start a DB-backed server unexpectedly.

Phase 1 update: the first Cypress skeleton now exists under
`frontend/cypress/e2e/smoke.cy.ts` after the Angular 20 workspace was added.

Recommended first Cypress smoke cases once the Angular 20 workspace exists:

```typescript
describe('legacy MEME UI smoke', () => {
  it('loads the legacy landing or login route', () => {
    cy.visit('/umls-server-rest/');
    cy.contains(/NCI|META|Terminology|Login|License/);
  });

  it('loads deploy UI properties', () => {
    cy.request('/umls-server-rest/configure/properties')
      .its('body')
      .should('have.property', 'deploy.enabled.tabs');
  });
});
```

Recommended manual legacy smoke checklist:

| Route | Preconditions | Smoke Notes |
| --- | --- | --- |
| `/` | local server running | Confirms root route resolves to landing/login/license/content based on deploy flags |
| `/login` | login enabled | Authenticate with a local DEFAULT-security user |
| `/license` | license enabled | Accept license and verify cookie behavior |
| `/terminology` | `terminology` tab enabled | Load list, select terminology, view details, navigate to metadata |
| `/metadata` | terminology selected or metadata model loaded | View metadata entries and precedence list |
| `/admin` | app `USER` or `ADMINISTRATOR` | View users/projects without mutating |
| `/workflow` | project role | Load workflow bins/configs; do not mutate in smoke |
| `/process` | project role | Load process config/execution lists; do not execute in smoke |
| `/edit` | project role | Load assigned/available worklists; do not edit in smoke |

## Phase 0 Deliverables

Completed:

- route inventory
- tab/navigation inventory
- runtime config inventory
- role/permission inventory
- service endpoint inventory
- page-to-service dependency map
- screen-risk matrix
- local old/new runtime recommendation
- legacy smoke checklist

Deferred to NM-313A:

- actual screenshots, because no local app server was running

Completed in Phase 1:

- Cypress skeleton files
- concurrent old/new local server validation with Angular 20 on
  `localhost:4200` and the existing MEME backend on `localhost:8080`

## Remaining Recommendations After Phase 1

Next:

1. Promote the Phase 1 backend probe into a typed Angular startup config service
   for `/umls-server-rest/configure/properties`.
2. Add auth service shell for `/umls-server-rest/security/authenticate/{userName}`.
3. Replace the placeholder shell route with config-driven navigation.
4. Superseded on 2026-06-10: do not enable terminology/metadata in the Angular
   20 local test tab set.

Preferred first feature after shell/auth:

1. admin read-only users/projects
2. process read-only and operations
3. workflow read-only and operations
