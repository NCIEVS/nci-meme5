const DEFAULT_API_BASE_URL = '/umls-server-rest';
const UI20_PATH_SEGMENT = '/ui20';

export function resolveMemeApiBaseUrl(pathname = currentPathname()): string {
  const ui20Index = pathname
    .toLowerCase()
    .search(new RegExp(`${UI20_PATH_SEGMENT}(?:/|$)`));

  if (ui20Index >= 0) {
    return trimTrailingSlash(pathname.slice(0, ui20Index));
  }

  return DEFAULT_API_BASE_URL;
}

export function legacyMemeUrl(path: string, apiBaseUrl: string): string {
  const baseUrl = trimTrailingSlash(apiBaseUrl);
  const route = path.replace(/^\/+/, '');

  return `${baseUrl}/#/${route}`;
}

function currentPathname(): string {
  return globalThis.location?.pathname ?? '';
}

function trimTrailingSlash(value: string): string {
  return value.replace(/\/+$/, '');
}
