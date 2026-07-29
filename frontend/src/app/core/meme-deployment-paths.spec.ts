import { legacyMemeUrl, resolveMemeApiBaseUrl } from './meme-deployment-paths';

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
});
