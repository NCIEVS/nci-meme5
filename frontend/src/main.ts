import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { canonicalizeMemeAppEntryUrl } from './app/core/meme-deployment-paths';

canonicalizeMemeAppEntryUrl();
bootstrapApplication(AppComponent, appConfig).catch((error: unknown) => {
  console.error(error);
});
