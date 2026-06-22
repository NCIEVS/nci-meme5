export type ApplicationRole = 'ADMINISTRATOR' | 'USER' | 'VIEWER' | string;

export interface UserPreferences {
  feedbackEmail?: string | null;
  favorites?: string[] | null;
  id?: number | null;
  lastProjectId?: number | string | null;
  lastProjectRole?: string | null;
  lastTerminology?: string | null;
  lastTab?: string | null;
  precedenceListId?: number | null;
  properties?: Record<string, unknown>;
  user?: Partial<MemeUser> | null;
  userId?: number | null;
  userName?: string | null;
}

export interface MemeUser {
  applicationRole: ApplicationRole | null;
  authToken: string | null;
  editorLevel?: number | null;
  email?: string | null;
  id?: number | null;
  name?: string | null;
  password?: string | null;
  projectRoleMap?: Record<string, string> | null;
  userName: string | null;
  userPreferences: UserPreferences | null;
}

export const EMPTY_USER: MemeUser = {
  applicationRole: null,
  authToken: null,
  editorLevel: null,
  email: null,
  name: null,
  password: null,
  userName: null,
  userPreferences: null
};
