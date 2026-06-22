export interface PfsParameter {
  ascending?: boolean;
  maxResults?: number;
  queryRestriction?: string;
  sortField?: string;
  startIndex?: number;
}

export interface OperationalListState<T> {
  items: T[];
  totalCount: number;
}

export interface OperationalListResponse<T> {
  bins?: T[];
  checklists?: T[];
  configs?: T[];
  epochs?: T[];
  keyValuePairs?: T[];
  objects?: T[];
  processes?: T[];
  projects?: T[];
  strings?: T[];
  totalCount?: number;
  users?: T[];
  worklists?: T[];
}

export interface OperationalTerminology {
  current?: boolean | null;
  terminology?: string | null;
  version?: string | null;
}

export interface OperationalTerminologyListResponse {
  objects?: OperationalTerminology[];
  terminologies?: OperationalTerminology[];
  totalCount?: number;
}

export interface KeyValuePair {
  key?: string | null;
  value?: string | null;
}

export interface SearchResult {
  id?: string | number | null;
  terminologyId?: string | null;
  value?: string | null;
}

export interface SearchResultListResponse {
  objects?: SearchResult[];
  results?: SearchResult[];
  totalCount?: number;
}

export interface WorkflowNote {
  id?: number | null;
  lastModified?: string | number | null;
  lastModifiedBy?: string | null;
  note?: string | null;
  timestamp?: string | number | null;
}

export interface OperationalProject {
  id?: number | null;
  name?: string | null;
  teamBased?: boolean | null;
  userRoleMap?: Record<string, string> | null;
}

export interface OperationalUser {
  id?: number | null;
  name?: string | null;
  projectRoleMap?: Record<string, string> | null;
  team?: string | null;
  userName?: string | null;
}

export interface AlgorithmParameter {
  description?: string | null;
  fieldName?: string | null;
  length?: number | null;
  name?: string | null;
  placeholder?: string | null;
  possibleValues?: string[] | null;
  type?: string | null;
  value?: boolean | string | null;
  values?: string[] | null;
}

export interface ProcessStep {
  algorithmConfigId?: number | null;
  algorithmKey?: string | null;
  description?: string | null;
  enabled?: boolean | number | null;
  failDate?: string | number | null;
  finishDate?: string | number | null;
  id?: number | null;
  lastModified?: string | number | null;
  lastModifiedBy?: string | null;
  name?: string | null;
  parameters?: AlgorithmParameter[] | null;
  process?: {
    id?: number | null;
  } | null;
  processId?: number | null;
  properties?: Record<string, string> | null;
  startDate?: string | number | null;
  stopDate?: string | number | null;
  warning?: boolean | null;
}

export interface ProcessConfig {
  description?: string | null;
  feedbackEmail?: string | null;
  id?: number | null;
  inputPath?: string | null;
  lastModified?: string | number | null;
  lastModifiedBy?: string | null;
  logPath?: string | null;
  name?: string | null;
  steps?: ProcessStep[] | null;
  terminology?: string | null;
  type?: string | null;
  version?: string | null;
}

export interface ProcessExecution extends ProcessConfig {
  executionInfo?: Record<string, string> | null;
  failDate?: string | number | null;
  finishDate?: string | number | null;
  processConfigId?: number | null;
  startDate?: string | number | null;
  stopDate?: string | number | null;
  warning?: boolean | null;
  workId?: string | null;
}

export interface WorkflowBinDefinition {
  autofix?: string | null;
  description?: string | null;
  editable?: boolean | null;
  enabled?: boolean | null;
  id?: number | null;
  lastModified?: string | number | null;
  lastModifiedBy?: string | null;
  name?: string | null;
  query?: string | null;
  queryType?: string | null;
  rank?: number | null;
  required?: boolean | null;
  workflowConfig?: {
    id?: number | null;
  } | null;
  workflowConfigId?: number | null;
}

export interface WorkflowConfig {
  adminConfig?: boolean | null;
  id?: number | null;
  lastModified?: string | number | null;
  lastModifiedBy?: string | null;
  lastPartitionTime?: number | null;
  mutuallyExclusive?: boolean | null;
  queryStyle?: string | null;
  type?: string | null;
  workflowBinDefinitions?: WorkflowBinDefinition[] | null;
}

export interface ClusterTypeStats {
  clusterType?: string | null;
  stats?: Record<string, number> | null;
}

export interface WorkflowBin {
  autofix?: string | null;
  clusterCt?: number | null;
  creationTime?: number | null;
  description?: string | null;
  editable?: boolean | null;
  enabled?: boolean | number | null;
  id?: number | null;
  lastModified?: string | number | null;
  name?: string | null;
  rank?: number | null;
  required?: boolean | null;
  stats?: ClusterTypeStats[] | null;
  terminology?: string | null;
  timestamp?: string | number | null;
  type?: string | null;
  version?: string | null;
}

export interface Checklist {
  description?: string | null;
  id?: number | null;
  lastModified?: string | number | null;
  name?: string | null;
  notes?: WorkflowNote[] | null;
  stats?: Record<string, number> | null;
  trackingRecords?: unknown[] | null;
}

export interface Worklist extends Checklist {
  authorAvailable?: boolean | null;
  authorTime?: number | null;
  authors?: string[] | null;
  epoch?: string | null;
  number?: number | null;
  reviewerAvailable?: boolean | null;
  reviewerTime?: number | null;
  reviewers?: string[] | null;
  team?: string | null;
  workflowBinName?: string | null;
  workflowStateHistory?: Record<string, string | number> | null;
  workflowStatus?: string | null;
}

export type WorkflowAction = 'ASSIGN' | 'UNASSIGN' | 'REASSIGN' | 'FINISH';

export interface WorkflowEpoch {
  active?: boolean | null;
  id?: number | null;
  lastModified?: string | number | null;
  name?: string | null;
}
