export type ApplicationRole = 'ADMINISTRATOR' | 'USER' | 'VIEWER' | string;

export interface UserPreferences {
  lastProjectId?: number | string | null;
  lastProjectRole?: string | null;
  lastTerminology?: string | null;
  lastTab?: string | null;
  properties?: Record<string, unknown>;
}

export interface MemeUser {
  applicationRole: ApplicationRole | null;
  authToken: string | null;
  editorLevel?: number | null;
  email?: string | null;
  name?: string | null;
  password?: string | null;
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
