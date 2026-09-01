import {
  isCurrentSessionAuthFailure,
  shouldReportGlobalHttpError
} from './error.helpers';

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

  it('suppresses global banners for login authentication requests', () => {
    expect(
      shouldReportGlobalHttpError(
        '/umls-server-rest/security/authenticate/DSS?attempt=1'
      )
    ).toBe(false);
  });

  it('reports global banners for non-login requests', () => {
    expect(
      shouldReportGlobalHttpError('/umls-server-rest/project/current')
    ).toBe(true);
  });

  it('suppresses global banners for concept approval requests', () => {
    expect(
      shouldReportGlobalHttpError(
        '/umls-server-rest/meta/concept/approve?projectId=1&conceptId=2'
      )
    ).toBe(false);
  });

  it('suppresses global banners for relationship add requests', () => {
    expect(
      shouldReportGlobalHttpError(
        '/umls-server-rest/meta/relationship/add?projectId=1&conceptId=2'
      )
    ).toBe(false);
    expect(
      shouldReportGlobalHttpError(
        '/umls-server-rest/meta/relationships/add?projectId=1&conceptId=2'
      )
    ).toBe(false);
  });
});
