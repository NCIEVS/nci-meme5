import { HttpContext, HttpContextToken } from '@angular/common/http';

export const UI_BLOCKING_REQUEST = new HttpContextToken<boolean | null>(
  () => null
);

export function nonBlockingLoadingContext(): HttpContext {
  return new HttpContext().set(UI_BLOCKING_REQUEST, false);
}
