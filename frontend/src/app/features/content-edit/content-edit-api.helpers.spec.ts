import {
  buildContentPfs,
  buildContentSearchPfs,
  buildWorkflowListFilterQuery,
  contentTypePath,
  normalizeContentListResponse
} from './content-edit-api.helpers';

describe('content edit API helpers', () => {
  it('builds legacy PFS paging payloads', () => {
    expect(buildContentPfs(2, 25, 'name', true, '  heart  ')).toEqual({
      ascending: true,
      maxResults: 25,
      queryRestriction: 'heart',
      sortField: 'name',
      startIndex: 25
    });
  });

  it('omits blank PFS query restrictions', () => {
    expect(buildContentPfs(1, 10, 'terminologyId', false, '   ')).toEqual({
      ascending: false,
      maxResults: 10,
      queryRestriction: undefined,
      sortField: 'terminologyId',
      startIndex: 0
    });
  });

  it('omits blank PFS sort fields for Lucene-backed report facets', () => {
    expect(buildContentPfs(1, 10, '   ', true, '')).toEqual({
      ascending: true,
      maxResults: 10,
      queryRestriction: undefined,
      sortField: undefined,
      startIndex: 0
    });
  });

  it('normalizes component type paths for legacy content URLs', () => {
    expect(contentTypePath('CONCEPT')).toBe('concept');
    expect(contentTypePath(' Code ')).toBe('code');
  });

  it('normalizes typed list responses by preferred key', () => {
    expect(
      normalizeContentListResponse(
        {
          results: [{ terminologyId: 'C1' }, { terminologyId: 'C2' }],
          objects: [{ terminologyId: 'fallback' }],
          totalCount: 6
        },
        ['results', 'objects']
      )
    ).toEqual({
      items: [{ terminologyId: 'C1' }, { terminologyId: 'C2' }],
      totalCount: 6
    });
  });

  it('normalizes empty legacy list responses', () => {
    expect(normalizeContentListResponse(null, ['relationships', 'objects'])).toEqual({
      items: [],
      totalCount: 0
    });
  });

  it('builds legacy content search restrictions without forcing score sort', () => {
    expect(buildContentSearchPfs(1, 10, '', false, 'CONCEPT')).toEqual({
      ascending: false,
      maxResults: 10,
      queryRestriction:
        '(suppressible:false^20.0 OR suppressible:true) AND (atoms.suppressible:false^20.0 OR atoms.suppressible:true) AND anonymous:false',
      sortField: undefined,
      startIndex: 0
    });
  });

  it('preserves explicit legacy content search sort fields', () => {
    expect(buildContentSearchPfs(1, 10, 'name', true, 'CODE')).toEqual({
      ascending: true,
      maxResults: 10,
      queryRestriction:
        '(suppressible:false^20.0 OR suppressible:true) AND (atoms.suppressible:false^20.0 OR atoms.suppressible:true)',
      sortField: 'name',
      startIndex: 0
    });
  });

  it('builds workflow list prefix filters against name', () => {
    expect(buildWorkflowListFilterQuery(' wrk25 ')).toBe('name:wrk25*');
  });

  it('builds workflow list filters for multiple plain terms', () => {
    expect(buildWorkflowListFilterQuery('wrk25 snomedct')).toBe(
      'name:wrk25* AND name:snomedct*'
    );
  });

  it('treats underscores like spaces for workflow list filters', () => {
    expect(buildWorkflowListFilterQuery('wrk25b_snomedct')).toBe(
      '(nameSort:wrk25b_snomedct* OR (name:wrk25b* AND name:snomedct*))'
    );
  });

  it('keeps a keyword prefix and both hyphen token strategies for workflow list filters', () => {
    expect(buildWorkflowListFilterQuery('chk_ACC-AHA')).toBe(
      '(nameSort:chk_ACC\\-AHA* OR (name:chk* AND name:acc\\-aha*) OR (name:chk* AND name:acc* AND name:aha*))'
    );
  });

  it('builds pasted checklist names as tokenized prefix filters', () => {
    expect(buildWorkflowListFilterQuery('chk_ACC-AHA_2025_07D_DEMOTION')).toBe(
      '(nameSort:chk_ACC\\-AHA_2025_07D_DEMOTION* OR (name:chk* AND name:acc\\-aha* AND name:2025* AND name:07d* AND name:demotion*) OR (name:chk* AND name:acc* AND name:aha* AND name:2025* AND name:07d* AND name:demotion*))'
    );
  });

  it('passes through advanced workflow list filters', () => {
    expect(buildWorkflowListFilterQuery('workflowStatus:NEW')).toBe(
      'workflowStatus:NEW'
    );
  });
});
