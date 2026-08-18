import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { shouldBlockUiForRequestMethod } from './loading.helpers';
import { LoadingService } from './loading.service';

export const loadingInterceptor: HttpInterceptorFn = (request, next) => {
  const loading = inject(LoadingService);
  const blockUi = shouldBlockUiForRequestMethod(request.method);
  loading.increment({ blockUi });

  return next(request).pipe(finalize(() => loading.decrement({ blockUi })));
};
