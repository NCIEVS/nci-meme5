import { MemeLocation, memeAppRouteUrl } from './meme-deployment-paths';

const CONCEPT_LINK_PATTERN = /\/content\/(?:report|content)\/CONCEPT\/[^/]+\/(\d+)/i;

export function memeConceptReportUrl(
  conceptId: number,
  projectId?: number | null,
  tab = 'Report',
  location?: MemeLocation
): string {
  const params = new URLSearchParams();

  if (projectId) {
    params.set('projectId', String(projectId));
  }

  if (tab) {
    params.set('tab', tab);
  }

  params.set('id', String(conceptId));

  return memeAppRouteUrl('/concept-report', params, location);
}

export function rewriteMemeConceptReportLinks(
  html: string,
  projectId?: number | null,
  location?: MemeLocation
): string {
  const doc = new DOMParser().parseFromString(html, 'text/html');

  doc.querySelectorAll<HTMLAnchorElement>('a[href]').forEach((anchor) => {
    const conceptId = extractConceptIdFromReportHref(
      anchor.getAttribute('href') ?? ''
    );
    if (conceptId === null) return;

    anchor.removeAttribute('onclick');
    anchor.removeAttribute('target');
    anchor.setAttribute(
      'href',
      memeConceptReportUrl(conceptId, projectId, 'Report', location)
    );
    anchor.setAttribute('data-concept-id', String(conceptId));
    anchor.classList.add('crp-report-link');
  });

  return doc.body.innerHTML;
}

function extractConceptIdFromReportHref(href: string): number | null {
  const legacyMatch = href.match(CONCEPT_LINK_PATTERN);
  if (legacyMatch) {
    return parseFiniteNumber(legacyMatch[1]);
  }

  try {
    const url = new URL(href, 'http://meme.local');
    const isConceptReport =
      url.pathname.endsWith('/concept-report') ||
      url.hash.includes('/concept-report');

    if (!isConceptReport) {
      return null;
    }

    const hashQuery = url.hash.includes('?') ? url.hash.split('?')[1] : '';

    return parseFiniteNumber(
      url.searchParams.get('id') ?? new URLSearchParams(hashQuery).get('id')
    );
  } catch {
    return null;
  }
}

function parseFiniteNumber(value: string | null): number | null {
  if (!value) {
    return null;
  }

  const parsed = Number(value);

  return Number.isFinite(parsed) ? parsed : null;
}
