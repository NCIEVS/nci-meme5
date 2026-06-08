import { inject } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import { RuntimeConfigService } from '../config/runtime-config.service';

export function initializeApplication(): () => Promise<void> {
  const config = inject(RuntimeConfigService);
  const auth = inject(AuthService);

  return async () => {
    await config.load();
    auth.initializeFromStoredSession();
  };
}
