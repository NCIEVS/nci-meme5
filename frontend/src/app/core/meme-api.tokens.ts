import { InjectionToken } from '@angular/core';

import { resolveMemeApiBaseUrl } from './meme-deployment-paths';

export const MEME_API_BASE_URL = new InjectionToken<string>(
  'MEME API base URL',
  {
    factory: () => resolveMemeApiBaseUrl()
  }
);
