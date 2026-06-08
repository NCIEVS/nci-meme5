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
  source: {
    key: 'source',
    label: 'Sources',
    link: 'source',
    role: 'USER'
  },
  content: {
    key: 'content',
    label: 'Content',
    link: 'content'
  },
  terminology: {
    key: 'terminology',
    label: 'Terminology',
    link: 'terminology'
  },
  metadata: {
    key: 'metadata',
    label: 'Metadata',
    link: 'metadata'
  },
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
