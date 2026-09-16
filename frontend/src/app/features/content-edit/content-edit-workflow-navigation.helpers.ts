export type NextWorkflowRecordNavigation =
  | {
      kind: 'record';
      recordIndex: number;
    }
  | {
      kind: 'page';
      page: number;
    }
  | {
      kind: 'end';
    };

export const NO_MORE_WORKLIST_CLUSTERS_MESSAGE = 'No more clusters in the worklist.';

export function nextWorkflowRecordNavigation<T extends { id?: number | null }>(
  selectedRecordId: number | null | undefined,
  records: readonly T[],
  currentPage: number,
  pageSize: number,
  totalCount: number
): NextWorkflowRecordNavigation | null {
  if (!selectedRecordId || !records.length || currentPage < 1 || pageSize < 1) {
    return null;
  }

  const selectedIndex = records.findIndex((record) => record.id === selectedRecordId);
  if (selectedIndex < 0) {
    return null;
  }

  if (selectedIndex < records.length - 1) {
    return {
      kind: 'record',
      recordIndex: selectedIndex + 1
    };
  }

  const totalPages = Math.ceil(totalCount / pageSize);
  if (currentPage < totalPages) {
    return {
      kind: 'page',
      page: currentPage + 1
    };
  }

  return {
    kind: 'end'
  };
}
