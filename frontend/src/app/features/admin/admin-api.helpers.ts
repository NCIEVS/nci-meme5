import {
  AdminListResponse,
  AdminListState,
  PfsParameter
} from './admin.models';

type ListKey = 'objects' | 'projects' | 'strings' | 'users';

export function buildPfs(
  page: number,
  pageSize: number,
  sortField: string,
  ascending: boolean,
  filter: string
): PfsParameter {
  const trimmedFilter = filter.trim();

  return {
    ascending,
    maxResults: pageSize,
    queryRestriction: trimmedFilter || undefined,
    sortField,
    startIndex: Math.max(0, page - 1) * pageSize
  };
}

export function normalizeListResponse<T>(
  response: AdminListResponse<T>,
  keys: ListKey[]
): AdminListState<T> {
  const items =
    keys.map((key) => response[key]).find((candidate) => Array.isArray(candidate)) ??
    [];

  return {
    items,
    totalCount: response.totalCount ?? items.length
  };
}
