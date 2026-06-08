import { InjectionToken } from '@angular/core';

export const MEME_API_BASE_URL = new InjectionToken<string>(
  'MEME API base URL',
  {
    factory: () => '/umls-server-rest'
  }
);
