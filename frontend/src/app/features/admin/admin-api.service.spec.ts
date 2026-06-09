import { buildPfs, normalizeListResponse } from './admin-api.helpers';

describe('admin API helpers', () => {
  it('builds legacy PFS paging payloads', () => {
    expect(buildPfs(3, 25, 'userName', false, '  admin  ')).toEqual({
      ascending: false,
      maxResults: 25,
      queryRestriction: 'admin',
      sortField: 'userName',
      startIndex: 50
    });
  });

  it('omits empty query restrictions from PFS payloads', () => {
    expect(buildPfs(1, 10, 'lastModified', true, '   ')).toEqual({
      ascending: true,
      maxResults: 10,
      queryRestriction: undefined,
      sortField: 'lastModified',
      startIndex: 0
    });
  });

  it('normalizes typed list responses by preferred key', () => {
    const state = normalizeListResponse(
      {
        objects: [{ userName: 'fallback' }],
        totalCount: 2,
        users: [{ userName: 'admin' }, { userName: 'DSS' }]
      },
      ['users', 'objects']
    );

    expect(state).toEqual({
      items: [{ userName: 'admin' }, { userName: 'DSS' }],
      totalCount: 2
    });
  });
});
