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
  objects?: T[];
  projects?: T[];
  strings?: T[];
  totalCount?: number;
  users?: T[];
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
  validationChecks?: string[] | null;
  version?: string | null;
  workflowPath?: string | null;
}
