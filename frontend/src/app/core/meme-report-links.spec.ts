import {
  memeConceptReportUrl,
  rewriteMemeConceptReportLinks
} from './meme-report-links';

const { JSDOM } = require('jsdom') as {
  JSDOM: new (html?: string) => { window: { DOMParser: typeof DOMParser } };
};

const packagedLocation = {
  origin: 'http://ncias-q3794-c.nci.nih.gov:8080',
  pathname: '/ncim-server-rest/ui20/index.html'
};

describe('meme report links', () => {
  beforeAll(() => {
    (globalThis as typeof globalThis & { DOMParser: typeof DOMParser }).DOMParser =
      new JSDOM('').window.DOMParser;
  });

  it('builds packaged Angular concept report URLs', () => {
    expect(memeConceptReportUrl(323696, 39751, 'Report', packagedLocation)).toBe(
      'http://ncias-q3794-c.nci.nih.gov:8080/ncim-server-rest/ui20/index.html#/concept-report?projectId=39751&tab=Report&id=323696'
    );
  });

  it('rewrites legacy report concept anchors to packaged Angular URLs', () => {
    const html =
      '<a href="/ncim-server-rest/content/report/CONCEPT/NCIMTH/323696" target="_blank" onclick="return false">C123</a>';
    const rewritten = rewriteMemeConceptReportLinks(
      html,
      39751,
      packagedLocation
    );
    const doc = new DOMParser().parseFromString(rewritten, 'text/html');
    const anchor = doc.querySelector('a');

    expect(anchor?.getAttribute('href')).toBe(
      'http://ncias-q3794-c.nci.nih.gov:8080/ncim-server-rest/ui20/index.html#/concept-report?projectId=39751&tab=Report&id=323696'
    );
    expect(anchor?.getAttribute('data-concept-id')).toBe('323696');
    expect(anchor?.hasAttribute('onclick')).toBe(false);
    expect(anchor?.hasAttribute('target')).toBe(false);
  });

  it('rewrites host-root concept report anchors to packaged Angular URLs', () => {
    const html =
      '<a href="/concept-report?projectId=39751&tab=Report&id=323696">C123</a>';
    const rewritten = rewriteMemeConceptReportLinks(
      html,
      39751,
      packagedLocation
    );
    const doc = new DOMParser().parseFromString(rewritten, 'text/html');

    expect(doc.querySelector('a')?.getAttribute('href')).toBe(
      'http://ncias-q3794-c.nci.nih.gov:8080/ncim-server-rest/ui20/index.html#/concept-report?projectId=39751&tab=Report&id=323696'
    );
  });

  it('leaves non-concept links unchanged', () => {
    expect(
      rewriteMemeConceptReportLinks(
        '<a href="https://example.test">Example</a>',
        39751,
        packagedLocation
      )
    ).toBe('<a href="https://example.test">Example</a>');
  });
});
