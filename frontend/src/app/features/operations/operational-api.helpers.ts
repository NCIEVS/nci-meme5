import {
  OperationalListResponse,
  OperationalListState,
  PfsParameter
} from './operational.models';

export type OperationalListKey =
  | 'atoms'
  | 'bins'
  | 'checklists'
  | 'configs'
  | 'epochs'
  | 'keyValuePairs'
  | 'objects'
  | 'processes'
  | 'projects'
  | 'strings'
  | 'users'
  | 'worklists';

export function buildOperationalPfs(
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

export function workflowBinRecordQueryRestriction(
  clusterType: string | null | undefined
): string | undefined {
  const trimmedClusterType = clusterType?.trim();

  if (!trimmedClusterType || trimmedClusterType.toLocaleLowerCase() === 'all') {
    return undefined;
  }

  if (trimmedClusterType.toLocaleLowerCase() === 'default') {
    return 'clusterType:""';
  }

  return `clusterType:${quoteLuceneValue(trimmedClusterType)}`;
}

function quoteLuceneValue(value: string): string {
  return `"${value.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`;
}

export function normalizeOperationalListResponse<T>(
  response: OperationalListResponse<T>,
  keys: OperationalListKey[]
): OperationalListState<T> {
  const items =
    keys.map((key) => response[key]).find((candidate) => Array.isArray(candidate)) ??
    [];

  return {
    items,
    totalCount: response.totalCount ?? items.length
  };
}
