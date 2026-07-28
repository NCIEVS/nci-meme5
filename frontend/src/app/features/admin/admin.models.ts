import { UserPreferences } from '../../core/auth/auth.models';

export interface PfsParameter {
  ascending: boolean;
  maxResults: number;
  queryRestriction?: string;
  sortField: string;
  startIndex: number;
}

export interface AdminListState<T> {
  items: T[];
  totalCount: number;
}

export interface AdminListResponse<T> {
  maintenanceWindows?: T[];
  objects?: T[];
  projects?: T[];
  strings?: T[];
  totalCount?: number;
  users?: T[];
}

export interface AdminKeyValuePair {
  key: string;
  value: string;
}

export interface AdminKeyValuePairList {
  keyValuePairs?: AdminKeyValuePair[];
  name?: string | null;
}

export interface AdminPrecedenceList {
  branch?: string | null;
  id?: number | null;
  lastModified?: number | null;
  lastModifiedBy?: string | null;
  name?: string | null;
  precedence?: AdminKeyValuePairList | null;
  terminology?: string | null;
  version?: string | null;
}

export interface AdminKeyValuePairListsResponse {
  keyValuePairLists?: AdminKeyValuePairList[];
}

export interface AdminStringListResponse {
  objects?: string[];
  strings?: string[];
  totalCount?: number;
}

export interface AdminTerminology {
  current?: boolean | null;
  terminology?: string | null;
  version?: string | null;
}

export interface AdminTerminologyListResponse {
  objects?: AdminTerminology[];
  terminologies?: AdminTerminology[];
  totalCount?: number;
}

export interface AdminValidationData {
  id?: number | null;
  key?: string | null;
  type?: string | null;
  value?: string | null;
}

export interface AdminUser {
  applicationRole?: string | null;
  editorLevel?: number | null;
  email?: string | null;
  id?: number | null;
  name?: string | null;
  projectRoleMap?: Record<string, string> | null;
  team?: string | null;
  userName?: string | null;
  userPreferences?: UserPreferences | null;
}

export interface AdminProject {
  automationsEnabled?: boolean | null;
  branch?: string | null;
  description?: string | null;
  editingEnabled?: boolean | null;
  feedbackEmail?: string | null;
  id?: number | null;
  language?: string | null;
  lastModified?: number | null;
  lastModifiedBy?: string | null;
  name?: string | null;
  newAtomTermgroups?: string[] | null;
  organization?: string | null;
  precedenceListId?: number | null;
  public?: boolean | null;
  semanticTypeCategoryMap?: Record<string, string> | null;
  teamBased?: boolean | null;
  teams?: Array<string | null> | null;
  terminology?: string | null;
  userRoleMap?: Record<string, string> | null;
  validCategories?: string[] | null;
  validationData?: AdminValidationData[] | null;
  validationChecks?: string[] | null;
  version?: string | null;
  workflowPath?: string | null;
}
