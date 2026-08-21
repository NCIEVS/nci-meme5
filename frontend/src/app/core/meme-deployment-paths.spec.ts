import {
  canonicalMemeAppEntryUrl,
  legacyMemeUrl,
  memeAppRouteUrl,
  resolveMemeApiBaseUrl
} from './meme-deployment-paths';

describe('meme deployment paths', () => {
  it('uses the default local API base outside a packaged ui20 path', () => {
    expect(resolveMemeApiBaseUrl('/')).toBe('/umls-server-rest');
    expect(resolveMemeApiBaseUrl('/admin')).toBe('/umls-server-rest');
    expect(resolveMemeApiBaseUrl('/ncim-server-rest/ui20beta/')).toBe(
      '/umls-server-rest'
    );
  });

  it('derives the API base from the deployed ui20 context path', () => {
    expect(resolveMemeApiBaseUrl('/ncim-server-rest/ui20/')).toBe(
      '/ncim-server-rest'
    );
    expect(resolveMemeApiBaseUrl('/umls-server-rest/ui20/')).toBe(
      '/umls-server-rest'
    );
  });

  it('builds legacy hash URLs from the resolved API base', () => {
    expect(legacyMemeUrl('admin', '/ncim-server-rest')).toBe(
      '/ncim-server-rest/#/admin'
    );
    expect(legacyMemeUrl('/process', '/ncim-server-rest/')).toBe(
      '/ncim-server-rest/#/process'
    );
  });

  it('builds Angular hash-route URLs under the packaged ui20 index', () => {
    expect(
      memeAppRouteUrl(
        '/concept-report',
        'projectId=39751&tab=Report&id=323696',
        {
          origin: 'http://ncias-q3794-c.nci.nih.gov:8080',
          pathname: '/ncim-server-rest/ui20/index.html'
        }
      )
    ).toBe(
      'http://ncias-q3794-c.nci.nih.gov:8080/ncim-server-rest/ui20/index.html#/concept-report?projectId=39751&tab=Report&id=323696'
    );
  });

  it('normalizes packaged ui20 directory URLs to the explicit Angular index', () => {
    expect(
      memeAppRouteUrl('/login', undefined, {
        origin: 'http://localhost:8080',
        pathname: '/umls-server-rest/ui20/'
      })
    ).toBe('http://localhost:8080/umls-server-rest/ui20/index.html#/login');
  });

  it('builds Angular hash-route URLs for local frontend serving', () => {
    expect(
      memeAppRouteUrl('/concept-report', new URLSearchParams('id=1'), {
        origin: 'http://localhost:4200',
        pathname: '/'
      })
    ).toBe('http://localhost:4200/#/concept-report?id=1');
  });

  it('canonicalizes packaged ui20 hash routes to the explicit index page', () => {
    expect(
      canonicalMemeAppEntryUrl({
        hash: '#/process',
        origin: 'http://localhost:8080',
        pathname: '/umls-server-rest/ui20/',
        search: ''
      })
    ).toBe('http://localhost:8080/umls-server-rest/ui20/index.html#/process');

    expect(
      canonicalMemeAppEntryUrl({
        hash: '#/workflow',
        origin: 'http://localhost:8080',
        pathname: '/umls-server-rest/ui20',
        search: ''
      })
    ).toBe('http://localhost:8080/umls-server-rest/ui20/index.html#/workflow');
  });

  it('canonicalizes packaged ui20 path routes to hash routes under the index page', () => {
    expect(
      canonicalMemeAppEntryUrl({
        hash: '',
        origin: 'http://localhost:8080',
        pathname: '/umls-server-rest/ui20/workflow',
        search: '?projectId=5'
      })
    ).toBe('http://localhost:8080/umls-server-rest/ui20/index.html#/workflow?projectId=5');
  });

  it('does not canonicalize already explicit index, ui20 assets, or local dev URLs', () => {
    expect(
      canonicalMemeAppEntryUrl({
        hash: '#/process',
        origin: 'http://localhost:8080',
        pathname: '/umls-server-rest/ui20/index.html',
        search: ''
      })
    ).toBeNull();

    expect(
      canonicalMemeAppEntryUrl({
        hash: '',
        origin: 'http://localhost:8080',
        pathname: '/umls-server-rest/ui20/main-ABC123.js',
        search: ''
      })
    ).toBeNull();

    expect(
      canonicalMemeAppEntryUrl({
        hash: '#/process',
        origin: 'http://localhost:4200',
        pathname: '/',
        search: ''
      })
    ).toBeNull();
  });
});
