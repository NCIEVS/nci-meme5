const DEFAULT_API_BASE_URL = '/umls-server-rest';
const UI20_PATH_SEGMENT = '/ui20';

export interface MemeLocation {
  hash?: string;
  origin: string;
  pathname: string;
  search?: string;
}

export function resolveMemeApiBaseUrl(pathname = currentPathname()): string {
  const ui20Index = pathname
    .toLowerCase()
    .search(new RegExp(`${UI20_PATH_SEGMENT}(?:/|$)`));

  if (ui20Index >= 0) {
    return trimTrailingSlash(pathname.slice(0, ui20Index));
  }

  return DEFAULT_API_BASE_URL;
}

export function memeAppRouteUrl(
  route: string,
  queryParams?: URLSearchParams | string,
  location = currentLocation()
): string {
  const normalizedRoute = route.startsWith('/') ? route : `/${route}`;
  const query =
    typeof queryParams === 'string'
      ? queryParams.replace(/^\?/, '')
      : queryParams?.toString() ?? '';
  const queryPrefix = normalizedRoute.includes('?') ? '&' : '?';

  return `${memeAppEntryUrl(location)}#${normalizedRoute}${
    query ? queryPrefix + query : ''
  }`;
}

export function legacyMemeUrl(path: string, apiBaseUrl: string): string {
  const baseUrl = trimTrailingSlash(apiBaseUrl);
  const route = path.replace(/^\/+/, '');

  return `${baseUrl}/#/${route}`;
}

export function canonicalMemeAppEntryUrl(
  location = currentLocation()
): string | null {
  const pathname = location.pathname || '/';
  const ui20Match = pathname.match(/^(.*\/ui20)(?:\/(.*))?$/i);

  if (!ui20Match) {
    return null;
  }

  const ui20Base = ui20Match[1];
  const ui20Path = ui20Match[2] ?? '';

  if (!ui20Path) {
    return `${location.origin}${ui20Base}/index.html${location.search ?? ''}${location.hash ?? ''}`;
  }

  if (ui20Path.toLowerCase() === 'index.html') {
    return null;
  }

  if (ui20Path.includes('.')) {
    return null;
  }

  if (location.hash) {
    return `${location.origin}${ui20Base}/index.html${location.search ?? ''}${location.hash}`;
  }

  const route = ui20Path.replace(/^\/+/, '');

  return `${location.origin}${ui20Base}/index.html#/${route}${location.search ?? ''}`;
}

export function canonicalizeMemeAppEntryUrl(): void {
  const url = canonicalMemeAppEntryUrl();

  if (!url) {
    return;
  }

  globalThis.history.replaceState(globalThis.history.state, '', url);
}

function memeAppEntryUrl(location: MemeLocation): string {
  const pathname = location.pathname || '/';
  const ui20Match = pathname.match(/^(.*\/ui20)(?:\/.*)?$/i);

  if (ui20Match) {
    return `${location.origin}${ui20Match[1]}/index.html`;
  }

  if (pathname.endsWith('/index.html')) {
    return `${location.origin}${pathname}`;
  }

  return `${location.origin}/`;
}

function currentLocation(): MemeLocation {
  return globalThis.location ?? { origin: '', pathname: '/' };
}

function currentPathname(): string {
  return globalThis.location?.pathname ?? '';
}

function trimTrailingSlash(value: string): string {
  return value.replace(/\/+$/, '');
}
