import { shouldBlockUiForRequestMethod } from './loading.helpers';

describe('loading helpers', () => {
  it('blocks the UI for mutating request methods', () => {
    expect(shouldBlockUiForRequestMethod('POST')).toBe(true);
    expect(shouldBlockUiForRequestMethod('PUT')).toBe(true);
    expect(shouldBlockUiForRequestMethod('PATCH')).toBe(true);
    expect(shouldBlockUiForRequestMethod('DELETE')).toBe(true);
  });

  it('keeps read-only request methods non-blocking', () => {
    expect(shouldBlockUiForRequestMethod('GET')).toBe(false);
    expect(shouldBlockUiForRequestMethod('HEAD')).toBe(false);
    expect(shouldBlockUiForRequestMethod('OPTIONS')).toBe(false);
  });

  it('handles lowercase method names defensively', () => {
    expect(shouldBlockUiForRequestMethod('post')).toBe(true);
    expect(shouldBlockUiForRequestMethod('get')).toBe(false);
  });
});
