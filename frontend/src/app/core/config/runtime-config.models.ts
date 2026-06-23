export type RuntimeConfigProperties = Record<string, string>;

export interface RuntimeConfig {
  properties: RuntimeConfigProperties;
}

export interface EnabledTab {
  key: string;
  label: string;
  link: string;
  projectRole?: boolean;
  role?: 'ADMINISTRATOR' | 'USER' | 'VIEWER';
}

export const TAB_DEFINITIONS: Record<string, EnabledTab> = {
  workflow: {
    key: 'workflow',
    label: 'Workflow',
    link: 'workflow',
    projectRole: true
  },
  edit: {
    key: 'edit',
    label: 'Edit',
    link: 'edit',
    projectRole: true
  },
  process: {
    key: 'process',
    label: 'Process',
    link: 'process',
    projectRole: true
  },
  inversion: {
    key: 'inversion',
    label: 'Inversion',
    link: 'inversion',
    projectRole: true
  },
  admin: {
    key: 'admin',
    label: 'Admin',
    link: 'admin',
    role: 'USER'
  }
};
