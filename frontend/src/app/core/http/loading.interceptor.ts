import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { UI_BLOCKING_REQUEST } from './loading.context';
import { shouldBlockUiForRequestMethod } from './loading.helpers';
import { LoadingService } from './loading.service';

export const loadingInterceptor: HttpInterceptorFn = (request, next) => {
  const loading = inject(LoadingService);
  const blockUi = shouldBlockUiForRequestMethod(
    request.method,
    request.context.get(UI_BLOCKING_REQUEST)
  );
  loading.increment({ blockUi });

  return next(request).pipe(finalize(() => loading.decrement({ blockUi })));
};
