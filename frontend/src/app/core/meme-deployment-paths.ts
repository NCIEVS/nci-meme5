const DEFAULT_API_BASE_URL = '/umls-server-rest';
const UI20_PATH_SEGMENT = '/ui20';

interface MemeLocation {
  origin: string;
  pathname: string;
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
