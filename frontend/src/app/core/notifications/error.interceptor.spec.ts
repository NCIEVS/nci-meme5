import { isCurrentSessionAuthFailure } from './error.helpers';

describe('errorInterceptor auth failure handling', () => {
  it('ignores auth failures from stale requests after a new login token is active', () => {
    expect(isCurrentSessionAuthFailure('old-token', 'new-token')).toBe(false);
  });

  it('handles auth failures from the current request token', () => {
    expect(isCurrentSessionAuthFailure('active-token', 'active-token')).toBe(true);
  });

  it('handles missing-token auth failures when a session is active', () => {
    expect(isCurrentSessionAuthFailure(null, 'active-token')).toBe(true);
  });

  it('handles auth failures when no session is active', () => {
    expect(isCurrentSessionAuthFailure('expired-token', null)).toBe(true);
  });
});
