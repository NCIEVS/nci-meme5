import {
  buildOperationalPfs,
  normalizeOperationalListResponse
} from './operational-api.helpers';

describe('operational API helpers', () => {
  it('builds legacy PFS payloads for operational searches', () => {
    expect(buildOperationalPfs(2, 20, 'lastModified', false, '  name:foo  ')).toEqual({
      ascending: false,
      maxResults: 20,
      queryRestriction: 'name:foo',
      sortField: 'lastModified',
      startIndex: 20
    });
  });

  it('normalizes list responses by the first available legacy key', () => {
    expect(
      normalizeOperationalListResponse(
        {
          objects: [{ name: 'fallback' }],
          totalCount: 2,
          worklists: [{ name: 'wrk26a_one' }, { name: 'wrk26a_two' }]
        },
        ['worklists', 'objects']
      )
    ).toEqual({
      items: [{ name: 'wrk26a_one' }, { name: 'wrk26a_two' }],
      totalCount: 2
    });
  });
});
