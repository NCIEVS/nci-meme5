import { Routes } from '@angular/router';

import { TAB_DEFINITIONS } from './core/config/runtime-config.models';
import { featureAccessGuard } from './core/startup/feature-access.guard';
import { startupGuard } from './core/startup/startup.guard';
import { FeaturePlaceholderComponent } from './pages/feature-placeholder/feature-placeholder.component';
import { LandingComponent } from './pages/landing/landing.component';
import { LicenseComponent } from './pages/license/license.component';
import { LoginComponent } from './pages/login/login.component';
import { StartupComponent } from './pages/startup/startup.component';
import { TerminologyComponent } from './features/terminology/terminology.component';

const tabRoutes: Routes = Object.keys(TAB_DEFINITIONS)
  .filter((tabKey) => tabKey !== 'terminology')
  .map((tabKey) => ({
    path: TAB_DEFINITIONS[tabKey].link,
    component: FeaturePlaceholderComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey
    },
    title: `NCI-META ${TAB_DEFINITIONS[tabKey].label}`
  }));

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
    path: 'terminology',
    component: TerminologyComponent,
    canActivate: [featureAccessGuard],
    data: {
      tabKey: 'terminology'
    },
    title: 'NCI-META Terminology'
  },
  ...tabRoutes,
  {
    path: '**',
    redirectTo: ''
  }
];
