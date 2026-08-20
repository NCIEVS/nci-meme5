import {
  ContentComponentType,
  ContentListResponse,
  ContentListState,
  ContentPfsParameter
} from './content-edit.models';

export type ContentListKey =
  | 'atoms'
  | 'codes'
  | 'concepts'
  | 'descriptors'
  | 'mappings'
  | 'mapsets'
  | 'objects'
  | 'relationships'
  | 'results'
  | 'strings'
  | 'trees'
  | 'treePositions';

export function buildContentPfs(
  page: number,
  pageSize: number,
  sortField: string | null | undefined,
  ascending: boolean,
  filter: string
): ContentPfsParameter {
  const trimmedFilter = filter.trim();
  const trimmedSortField = sortField?.trim();

  return {
    ascending,
    maxResults: pageSize,
    queryRestriction: trimmedFilter || undefined,
    sortField: trimmedSortField || undefined,
    startIndex: Math.max(0, page - 1) * pageSize
  };
}

export function contentTypePath(type: ContentComponentType | string): string {
  return type.trim().toLowerCase();
}

export function buildContentSearchPfs(
  page: number,
  pageSize: number,
  sortField: string | null | undefined,
  ascending: boolean,
  type: ContentComponentType | string
): ContentPfsParameter {
  const trimmedSortField = sortField?.trim();
  const restrictions = [
    '(suppressible:false^20.0 OR suppressible:true)',
    '(atoms.suppressible:false^20.0 OR atoms.suppressible:true)'
  ];

  if (contentTypePath(type) === 'concept') {
    restrictions.push('anonymous:false');
  }

  return {
    ascending,
    maxResults: pageSize,
    queryRestriction: restrictions.join(' AND '),
    sortField: trimmedSortField || undefined,
    startIndex: Math.max(0, page - 1) * pageSize
  };
}

export function buildWorkflowListFilterQuery(
  filter: string | null | undefined
): string | undefined {
  const trimmedFilter = filter?.trim() ?? '';

  if (!trimmedFilter) {
    return undefined;
  }

  if (isLuceneWorkflowListFilter(trimmedFilter)) {
    return trimmedFilter;
  }

  const clauses = [
    buildWorkflowListNameSortClause(trimmedFilter),
    buildWorkflowListTokenClause(trimmedFilter, /[\s_]+/),
    buildWorkflowListTokenClause(trimmedFilter, /[\s_-]+/)
  ].filter((clause, index, list): clause is string =>
    Boolean(clause) && list.indexOf(clause) === index
  );

  if (!clauses.length) {
    return undefined;
  }

  return clauses.length === 1
    ? clauses[0]
    : `(${clauses.map(groupWorkflowListClause).join(' OR ')})`;
}

function isLuceneWorkflowListFilter(filter: string): boolean {
  return /[:()[\]{}"~*?^]|\b(?:AND|OR|NOT)\b|&&|\|\|/i.test(filter);
}

function buildWorkflowListNameSortClause(filter: string): string | null {
  if (!/[_-]/.test(filter) || /\s/.test(filter)) {
    return null;
  }

  return `nameSort:${escapeLuceneTerm(filter)}*`;
}

function buildWorkflowListTokenClause(
  filter: string,
  splitPattern: RegExp
): string | null {
  const terms = filter
    .split(splitPattern)
    .map((term) => escapeLuceneTerm(term.toLowerCase()))
    .filter(Boolean);

  if (!terms.length) {
    return null;
  }

  const clause = terms.map((term) => `name:${term}*`).join(' AND ');
  return clause;
}

function groupWorkflowListClause(clause: string): string {
  return clause.includes(' AND ') ? `(${clause})` : clause;
}

function escapeLuceneTerm(term: string): string {
  return term.replace(/([+\-!(){}\[\]^"~*?:\\/])/g, '\\$1');
}

export function normalizeContentListResponse<T>(
  response: ContentListResponse<T> | null | undefined,
  keys: ContentListKey[]
): ContentListState<T> {
  const contentResponse = response ?? {};
  const items =
    keys
      .map((key) => contentResponse[key])
      .find((candidate) => Array.isArray(candidate)) ??
    [];

  return {
    items,
    totalCount: contentResponse.totalCount ?? items.length
  };
}
