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
  return !/\/security\/authenticate(?:\/|$)/.test(stripQuery(url));
}

function stripQuery(url: string): string {
  return url.split(/[?#]/)[0] ?? url;
}
