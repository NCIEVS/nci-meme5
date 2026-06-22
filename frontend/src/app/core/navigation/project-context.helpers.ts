export function resolveProjectContextId(
  rawProjectId: number | string | null | undefined,
  projectRoleMap: Record<string, string> | null | undefined
): number | null {
  const selectedProjectId = parseProjectId(rawProjectId);

  if (selectedProjectId !== null) {
    return selectedProjectId;
  }

  const assignedProjectIds = Object.keys(projectRoleMap ?? {})
    .map((key) => parseProjectId(key))
    .filter((projectId): projectId is number => projectId !== null);

  return assignedProjectIds.length === 1 ? assignedProjectIds[0] : null;
}

function parseProjectId(value: number | string | null | undefined): number | null {
  const projectId = typeof value === 'number' ? value : Number(value);

  return Number.isFinite(projectId) && projectId > 0 ? projectId : null;
}
