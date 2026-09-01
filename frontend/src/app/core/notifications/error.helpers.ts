export function isCurrentSessionAuthFailure(
  failedAuthToken: string | null,
  currentAuthToken: string | null
): boolean {
  if (!currentAuthToken) {
    return true;
  }

  return !failedAuthToken || failedAuthToken === currentAuthToken;
}

export function shouldReportGlobalHttpError(url: string): boolean {
  const path = stripQuery(url);

  return ![
    /\/security\/authenticate(?:\/|$)/,
    /\/meta\/concept\/approve(?:\/|$)/,
    /\/meta\/relationship\/add(?:\/|$)/,
    /\/meta\/relationships\/add(?:\/|$)/
  ].some((ignoredPath) => ignoredPath.test(path));
}

function stripQuery(url: string): string {
  return url.split(/[?#]/)[0] ?? url;
}
