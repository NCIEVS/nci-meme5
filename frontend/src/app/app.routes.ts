import { Routes } from '@angular/router';

import { TAB_DEFINITIONS } from './core/config/runtime-config.models';
import { featureAccessGuard } from './core/startup/feature-access.guard';
import { startupGuard } from './core/startup/startup.guard';
import { AdminComponent } from './features/admin/admin.component';
import { ProcessComponent } from './features/operations/process.component';
import { WorkflowComponent } from './features/operations/workflow.component';
import { FeaturePlaceholderComponent } from './pages/feature-placeholder/feature-placeholder.component';
import { LandingComponent } from './pages/landing/landing.component';
import { LicenseComponent } from './pages/license/license.component';
import { LoginComponent } from './pages/login/login.component';
import { StartupComponent } from './pages/startup/startup.component';

const tabRoutes: Routes = Object.keys(TAB_DEFINITIONS)
  .filter(
    (tabKey) =>
      !['admin', 'edit', 'process', 'workflow'].includes(tabKey)
  )
  .map((tabKey) => ({
    path: TAB_DEFINITIONS[tabKey].link,
    component: FeaturePlaceholderComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey
    },
    title: `NCI-META ${TAB_DEFINITIONS[tabKey].label}`
  }));

const loadContentComponent = () =>
  import('./features/content-edit/content.component').then(
    (module) => module.ContentComponent
  );

const loadEditWorkbenchComponent = () =>
  import('./features/content-edit/edit-workbench.component').then(
    (module) => module.EditWorkbenchComponent
  );

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    component: StartupComponent,
    canActivate: [startupGuard],
    title: 'NCI-META'
  },
  {
    path: 'landing',
    component: LandingComponent,
    title: 'NCI-META Landing'
  },
  {
    path: 'login',
    component: LoginComponent,
    title: 'NCI-META Login'
  },
  {
    path: 'license',
    component: LicenseComponent,
    title: 'NCI-META License'
  },
  {
    path: 'content',
    loadComponent: loadContentComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'edit'
    },
    title: 'NCI-META Content'
  },
  {
    path: 'content/:mode/:type/:terminology/:version/:terminologyId',
    loadComponent: loadContentComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'edit'
    },
    title: 'NCI-META Content'
  },
  {
    path: 'content/:mode/:type/:terminology/:id',
    loadComponent: loadContentComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'edit'
    },
    title: 'NCI-META Content'
  },
  {
    path: 'edit',
    loadComponent: loadContentComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'edit'
    },
    title: 'NCI-META Edit'
  },
  {
    path: 'edit/semantic-types',
    loadComponent: loadEditWorkbenchComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'edit',
      workbench: 'semantic-types'
    },
    title: 'NCI-META Semantic Type Editor'
  },
  {
    path: 'edit/codeConcepts',
    loadComponent: loadEditWorkbenchComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'edit',
      workbench: 'code-concepts'
    },
    title: 'NCI-META Code Concept Editor'
  },
  {
    path: 'edit/atoms',
    loadComponent: loadEditWorkbenchComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'edit',
      workbench: 'atoms'
    },
    title: 'NCI-META Atom Editor'
  },
  {
    path: 'edit/relationships',
    loadComponent: loadEditWorkbenchComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'edit',
      workbench: 'relationships'
    },
    title: 'NCI-META Relationship Editor'
  },
  {
    path: 'contexts',
    loadComponent: loadEditWorkbenchComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'edit',
      workbench: 'contexts'
    },
    title: 'NCI-META Context Editor'
  },
  {
    path: 'process',
    component: ProcessComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'process'
    },
    title: 'NCI-META Process'
  },
  {
    path: 'workflow',
    component: WorkflowComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'workflow'
    },
    title: 'NCI-META Workflow'
  },
  {
    path: 'admin',
    component: AdminComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'admin'
    },
    title: 'NCI-META Admin'
  },
  ...tabRoutes,
  {
    path: '**',
    redirectTo: ''
  }
];
