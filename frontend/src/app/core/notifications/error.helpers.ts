export function isCurrentSessionAuthFailure(
  failedAuthToken: string | null,
  currentAuthToken: string | null
): boolean {
  if (!currentAuthToken) {
    return true;
  }

  return !failedAuthToken || failedAuthToken === currentAuthToken;
}
