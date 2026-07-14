import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { finalize, Observable } from 'rxjs';

import { UserPreferences } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { DialogComponent } from '../../shared/dialog/dialog.component';
import { IconComponent } from '../../shared/icon/icon.component';
import { PagerComponent } from '../../shared/pager/pager.component';
import { buildPfs } from './admin-api.helpers';
import { AdminApiService } from './admin-api.service';
import {
  AdminKeyValuePair,
  AdminPrecedenceList,
  AdminProject,
  AdminTerminology,
  AdminValidationData,
  AdminUser
} from './admin.models';

type UserSortField = 'userName' | 'name';
type ProjectSortField = 'lastModified' | 'name';
type AdminOperation = 'exception' | 'localException' | 'reloadConfig';
type ProjectFormField =
  | 'automationsEnabled'
  | 'description'
  | 'editingEnabled'
  | 'feedbackEmail'
  | 'language'
  | 'name'
  | 'terminology'
  | 'version'
  | 'workflowPath';
type ValidationDataFormField = 'key' | 'type' | 'value';
type UserFormField = 'applicationRole' | 'editorLevel' | 'email' | 'name' | 'userName';

interface RoleEntry {
  key: string;
  value: string;
}

interface ProjectLogState {
  filter: string;
  loading: boolean;
  log: string;
  project: AdminProject;
}

interface ProjectForm {
  automationsEnabled: boolean;
  description: string;
  editingEnabled: boolean;
  feedbackEmail: string;
  language: string;
  mode: 'add' | 'edit';
  name: string;
  sourceProject: AdminProject | null;
  terminology: string;
  validationChecks: string[];
  validationData: AdminValidationData[];
  version: string;
  workflowPath: string;
}

interface ValidationDataForm {
  key: string;
  type: string;
  value: string;
}

interface UserForm {
  applicationRole: string;
  editorLevel: string;
  email: string;
  mode: 'add' | 'edit';
  name: string;
  sourceUser: AdminUser | null;
  userName: string;
}

@Component({
  selector: 'meme-admin',
  imports: [DialogComponent, FormsModule, IconComponent, PagerComponent],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly applicationRoles = signal<string[]>([]);
  protected readonly projectRoles = signal<string[]>([]);
  protected readonly projectsGroupOpen = signal(true);
  protected readonly usersGroupOpen = signal(true);
  protected readonly preferencesGroupOpen = signal(true);
  protected readonly projects = signal<AdminProject[]>([]);
  protected readonly projectPage = signal(1);
  protected readonly projectPageSize = signal(10);
  protected readonly projectSortAscending = signal(true);
  protected readonly projectSortField = signal<ProjectSortField>('lastModified');
  protected readonly projectTotalCount = signal(0);
  protected readonly loadingProjects = signal(false);
  protected readonly loadingPrecedenceList = signal(false);
  protected readonly loadingRoles = signal(false);
  protected readonly loadingUsers = signal(false);
  protected readonly users = signal<AdminUser[]>([]);
  protected readonly deletingProjectId = signal<number | null>(null);
  protected readonly deletingUserId = signal<number | null>(null);
  protected readonly userPreferenceFeedbackEmail = signal('');
  protected readonly savingProject = signal(false);
  protected readonly savingProjectAssignment = signal(false);
  protected readonly savingPrecedenceList = signal(false);
  protected readonly savingUser = signal(false);
  protected readonly savingUserPreferences = signal(false);
  protected readonly runningAdminOperation = signal<AdminOperation | null>(null);
  protected readonly precedenceList = signal<AdminPrecedenceList | null>(null);
  protected readonly precedenceTouchedKeys = signal<string[]>([]);
  protected readonly draggingPrecedenceIndex = signal<number | null>(null);
  protected readonly projectLogState = signal<ProjectLogState | null>(null);
  protected readonly projectForm = signal<ProjectForm | null>(null);
  protected readonly projectFormErrors = signal<string[]>([]);
  protected readonly projectLanguageOptions = signal<AdminKeyValuePair[]>([]);
  protected readonly projectTerminologies = signal<AdminTerminology[]>([]);
  protected readonly validationCheckDefinitions = signal<AdminKeyValuePair[]>([]);
  protected readonly userForm = signal<UserForm | null>(null);
  protected readonly userFormErrors = signal<string[]>([]);
  protected readonly userFilter = signal('');
  protected readonly userPage = signal(1);
  protected readonly userPageSize = signal(10);
  protected readonly userSortAscending = signal(true);
  protected readonly userSortField = signal<UserSortField>('userName');
  protected readonly userTotalCount = signal(0);
  protected readonly validationDataForm = signal<ValidationDataForm | null>(null);
  protected readonly validationDataFormErrors = signal<string[]>([]);
  protected readonly workflowPaths = signal<string[]>([]);

  // User & Project Management section
  protected readonly candidateProjects = signal<AdminProject[]>([]);
  protected readonly candidateProjectFilter = signal('');
  protected readonly candidateProjectPage = signal(1);
  protected readonly candidateProjectPageSize = signal(10);
  protected readonly candidateProjectTotalCount = signal(0);
  protected readonly loadingCandidateProjects = signal(false);
  protected readonly selectedCandidateProject = signal<AdminProject | null>(null);
  protected readonly unassignedUsers = signal<AdminUser[]>([]);
  protected readonly unassignedUserFilter = signal('');
  protected readonly unassignedUserPage = signal(1);
  protected readonly unassignedUserPageSize = signal(10);
  protected readonly unassignedUserTotalCount = signal(0);
  protected readonly assignedUsers = signal<AdminUser[]>([]);
  protected readonly assignedUserFilter = signal('');
  protected readonly assignedUserPage = signal(1);
  protected readonly assignedUserPageSize = signal(10);
  protected readonly assignedUserTotalCount = signal(0);
  protected readonly upmRoles = signal<Record<string, string>>({});
  protected readonly workflowPathOptions = computed(() => {
    const currentWorkflowPath = this.projectForm()?.workflowPath;
    const paths = this.workflowPaths();

    if (currentWorkflowPath && !paths.includes(currentWorkflowPath)) {
      return [currentWorkflowPath, ...paths];
    }

    return paths;
  });
  protected readonly availableValidationChecks = computed(() => {
    const selectedKeys = new Set(this.projectForm()?.validationChecks ?? []);

    return this.validationCheckDefinitions()
      .filter((check) => !selectedKeys.has(check.key))
      .sort((left, right) => this.validationCheckLabel(left).localeCompare(
        this.validationCheckLabel(right),
        undefined,
        { sensitivity: 'base' }
      ));
  });
  protected readonly selectedValidationChecks = computed(() => {
    const checksByKey = new Map(
      this.validationCheckDefinitions().map((check) => [check.key, check])
    );

    return (this.projectForm()?.validationChecks ?? [])
      .map((key) => checksByKey.get(key) ?? { key, value: key })
      .sort((left, right) => this.validationCheckLabel(left).localeCompare(
        this.validationCheckLabel(right),
        undefined,
        { sensitivity: 'base' }
      ));
  });
  protected readonly precedenceEntries = computed(
    () => this.precedenceList()?.precedence?.keyValuePairs ?? []
  );

  protected readonly projectTotalPages = computed(() =>
    this.pageCount(this.projectTotalCount(), this.projectPageSize())
  );
  protected readonly userTotalPages = computed(() =>
    this.pageCount(this.userTotalCount(), this.userPageSize())
  );
  protected readonly candidateProjectTotalPages = computed(() =>
    this.pageCount(this.candidateProjectTotalCount(), this.candidateProjectPageSize())
  );
  protected readonly unassignedUserTotalPages = computed(() =>
    this.pageCount(this.unassignedUserTotalCount(), this.unassignedUserPageSize())
  );
  protected readonly assignedUserTotalPages = computed(() =>
    this.pageCount(this.assignedUserTotalCount(), this.assignedUserPageSize())
  );
  protected readonly canAddProjects = computed(() =>
    ['ADMINISTRATOR', 'USER'].includes(this.auth.currentUser().applicationRole ?? '')
  );
  protected readonly canMutateUsers = computed(
    () => this.auth.currentUser().applicationRole === 'ADMINISTRATOR'
  );
  protected readonly currentUser = this.auth.currentUser;
  protected readonly userPreferenceFeedbackEmailChanged = computed(
    () =>
      this.userPreferenceFeedbackEmail() !==
      (this.currentUser().userPreferences?.feedbackEmail ?? '')
  );

  ngOnInit(): void {
    this.userPreferenceFeedbackEmail.set(
      this.currentUser().userPreferences?.feedbackEmail ?? ''
    );
    this.restoreAccordionState();
    this.loadRoles();
    this.loadProjectConfiguration();
    this.loadProjects();
    this.loadUsers();
    this.loadCandidateProjects();
  }

  protected onAccordionToggle(group: 'preferences' | 'projects' | 'users', event: Event): void {
    const isOpen = (event.target as HTMLDetailsElement).open;

    if (group === 'projects') {
      this.projectsGroupOpen.set(isOpen);
    } else if (group === 'users') {
      this.usersGroupOpen.set(isOpen);
    } else {
      this.preferencesGroupOpen.set(isOpen);
    }

    this.saveAccordionState();
  }

  private restoreAccordionState(): void {
    const raw = this.currentUser().userPreferences?.properties?.['adminGroups'];

    if (typeof raw !== 'string' || !raw) {
      return;
    }

    try {
      const groups = JSON.parse(raw) as Array<{ open: boolean; title: string }>;
      const openFor = (title: string) => groups.find((group) => group.title === title)?.open;

      const projectsOpen = openFor('Projects');
      const usersOpen = openFor('Users');
      const preferencesOpen = openFor('User Preferences');

      if (projectsOpen !== undefined) {
        this.projectsGroupOpen.set(projectsOpen);
      }
      if (usersOpen !== undefined) {
        this.usersGroupOpen.set(usersOpen);
      }
      if (preferencesOpen !== undefined) {
        this.preferencesGroupOpen.set(preferencesOpen);
      }
    } catch {
      // ignore malformed stored accordion state
    }
  }

  private saveAccordionState(): void {
    const groups = [
      { open: this.projectsGroupOpen(), title: 'Projects' },
      { open: this.usersGroupOpen(), title: 'Users' },
      { open: this.preferencesGroupOpen(), title: 'User Preferences' }
    ];

    const preferences = this.currentUserPreferencesForUpdate({
      properties: {
        ...(this.currentUser().userPreferences?.properties ?? {}),
        adminGroups: JSON.stringify(groups)
      }
    });

    if (!preferences) {
      return;
    }

    this.api.updateUserPreferences(preferences).subscribe({
      next: (savedPreferences) => {
        this.auth.updateCurrentUserPreferences(savedPreferences ?? preferences);
      }
    });
  }

  protected loadProjects(): void {
    this.loadingProjects.set(true);
    this.api
      .findProjects(
        buildPfs(
          this.projectPage(),
          this.projectPageSize(),
          this.projectSortField(),
          this.projectSortAscending(),
          ''
        )
      )
      .pipe(finalize(() => this.loadingProjects.set(false)))
      .subscribe({
        next: (state) => {
          this.projects.set(state.items);
          this.projectTotalCount.set(state.totalCount);
        },
        error: () => {
          this.notifications.error('Projects could not be loaded.');
        }
      });
  }

  protected loadUsers(): void {
    this.loadingUsers.set(true);
    this.api
      .findUsers(
        buildPfs(
          this.userPage(),
          this.userPageSize(),
          this.userSortField(),
          this.userSortAscending(),
          this.userFilter()
        )
      )
      .pipe(finalize(() => this.loadingUsers.set(false)))
      .subscribe({
        next: (state) => {
          this.users.set(state.items);
          this.userTotalCount.set(state.totalCount);
        },
        error: () => {
          this.notifications.error('Users could not be loaded.');
        }
      });
  }

  protected canEditProject(project: AdminProject): boolean {
    return (
      this.auth.currentUser().applicationRole === 'ADMINISTRATOR' ||
      this.currentUserProjectRole(project) === 'ADMINISTRATOR'
    );
  }

  protected startAddProject(): void {
    const preferences = this.auth.currentUser().userPreferences;

    this.projectForm.set({
      automationsEnabled: false,
      description: '',
      editingEnabled: true,
      feedbackEmail: preferences?.feedbackEmail ?? '',
      language: 'ENG',
      mode: 'add',
      name: '',
      sourceProject: null,
      terminology: '',
      validationChecks: this.defaultValidationCheckKeys(),
      validationData: [],
      version: '',
      workflowPath: this.defaultWorkflowPath('')
    });
    this.projectFormErrors.set([]);
    this.projectLanguageOptions.set([]);
    this.clearPrecedenceList();
  }

  protected startEditProject(project: AdminProject): void {
    this.projectForm.set({
      automationsEnabled: Boolean(project.automationsEnabled),
      description: project.description ?? '',
      editingEnabled: Boolean(project.editingEnabled),
      feedbackEmail: project.feedbackEmail ?? '',
      language: project.language ?? '',
      mode: 'edit',
      name: project.name ?? '',
      sourceProject: project,
      terminology: project.terminology ?? '',
      validationChecks: [...(project.validationChecks ?? [])],
      validationData: [...(project.validationData ?? [])],
      version: project.version ?? '',
      workflowPath: this.defaultWorkflowPath(project.workflowPath ?? '')
    });
    this.projectFormErrors.set([]);
    this.loadProjectLanguages(project.terminology ?? '', project.version ?? '');
    this.loadProjectPrecedenceList(project.precedenceListId);
  }

  protected cancelProjectForm(): void {
    this.projectForm.set(null);
    this.projectFormErrors.set([]);
    this.projectLanguageOptions.set([]);
    this.clearPrecedenceList();
    this.cancelValidationDataForm();
  }

  protected updateProjectForm(field: ProjectFormField, value: boolean | string): void {
    const form = this.projectForm();

    if (!form) {
      return;
    }

    const updatedForm: ProjectForm = {
      ...form,
      [field]: value
    };

    if (field === 'terminology' && typeof value === 'string') {
      const matchingTerminology = this.projectTerminologies().find(
        (terminology) => terminology.terminology === value
      );

      if (matchingTerminology?.version) {
        updatedForm.version = matchingTerminology.version;
      }

      if (!updatedForm.language) {
        updatedForm.language = 'ENG';
      }
    }

    this.projectForm.set(updatedForm);

    if (field === 'terminology') {
      this.loadProjectLanguages(updatedForm.terminology, updatedForm.version);
    }
  }

  protected refreshProjectLanguages(): void {
    const form = this.projectForm();

    if (form) {
      this.loadProjectLanguages(form.terminology, form.version);
    }
  }

  protected selectValidationCheck(check: AdminKeyValuePair): void {
    this.projectForm.update((form) => {
      if (!form || form.validationChecks.includes(check.key)) {
        return form;
      }

      return {
        ...form,
        validationChecks: [...form.validationChecks, check.key]
      };
    });
  }

  protected removeValidationCheck(check: AdminKeyValuePair): void {
    this.projectForm.update((form) =>
      form
        ? {
            ...form,
            validationChecks: form.validationChecks.filter((key) => key !== check.key)
          }
        : form
    );
  }

  protected startAddValidationData(): void {
    this.validationDataForm.set({
      key: '',
      type: this.validationCheckDefinitions()[0]?.key ?? '',
      value: ''
    });
    this.validationDataFormErrors.set([]);
  }

  protected cancelValidationDataForm(): void {
    this.validationDataForm.set(null);
    this.validationDataFormErrors.set([]);
  }

  protected updateValidationDataForm(
    field: ValidationDataFormField,
    value: string
  ): void {
    this.validationDataForm.update((form) =>
      form ? { ...form, [field]: value } : form
    );
  }

  protected saveValidationData(): void {
    const form = this.validationDataForm();

    if (!form) {
      return;
    }

    const errors = this.validateValidationDataForm(form);
    this.validationDataFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    this.projectForm.update((projectForm) =>
      projectForm
        ? {
            ...projectForm,
            validationData: [
              ...projectForm.validationData,
              {
                key: form.key.trim(),
                type: form.type.trim(),
                value: form.value.trim()
              }
            ]
          }
        : projectForm
    );
    this.cancelValidationDataForm();
  }

  protected removeValidationData(_data: AdminValidationData, index: number): void {
    if (!window.confirm('Are you sure you want to remove this?')) {
      return;
    }

    this.projectForm.update((form) =>
      form
        ? {
            ...form,
            validationData: form.validationData.filter((_, itemIndex) => itemIndex !== index)
          }
        : form
    );
  }

  protected isPrecedenceEntryTouched(entry: AdminKeyValuePair): boolean {
    return this.precedenceTouchedKeys().includes(this.precedenceEntryKey(entry));
  }

  protected startPrecedenceDrag(event: DragEvent, index: number): void {
    if (this.savingPrecedenceList()) {
      event.preventDefault();
      return;
    }

    this.draggingPrecedenceIndex.set(index);
    event.dataTransfer?.setData('text/plain', String(index));
    event.dataTransfer?.setDragImage(event.currentTarget as Element, 0, 0);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  protected dragOverPrecedenceEntry(event: DragEvent, index: number): void {
    const sourceIndex = this.draggingPrecedenceIndex();

    if (sourceIndex !== null && sourceIndex !== index && !this.savingPrecedenceList()) {
      event.preventDefault();
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move';
      }
    }
  }

  protected dropPrecedenceEntry(event: DragEvent, targetIndex: number): void {
    const rawSourceIndex =
      this.draggingPrecedenceIndex() ??
      Number(event.dataTransfer?.getData('text/plain'));
    const sourceIndex = Number(rawSourceIndex);
    const viewportLeft = window.scrollX;
    const viewportTop = window.scrollY;

    event.preventDefault();
    this.draggingPrecedenceIndex.set(null);

    if (
      this.savingPrecedenceList() ||
      !Number.isInteger(sourceIndex) ||
      sourceIndex === targetIndex
    ) {
      return;
    }

    this.reorderPrecedenceEntry(sourceIndex, targetIndex);
    this.restorePrecedenceDropViewport(viewportLeft, viewportTop);
  }

  protected clearPrecedenceDrag(): void {
    this.draggingPrecedenceIndex.set(null);
  }

  private reorderPrecedenceEntry(index: number, targetIndex: number): void {
    const list = this.precedenceList();
    const entries = [...this.precedenceEntries()];

    if (
      !list ||
      index < 0 ||
      targetIndex < 0 ||
      index >= entries.length ||
      targetIndex >= entries.length
    ) {
      return;
    }

    const [entry] = entries.splice(index, 1);
    entries.splice(targetIndex, 0, entry);
    this.precedenceList.set({
      ...list,
      precedence: {
        ...(list.precedence ?? {}),
        keyValuePairs: entries
      }
    });
    this.markPrecedenceEntryTouched(entry);
  }

  private restorePrecedenceDropViewport(left: number, top: number): void {
    requestAnimationFrame(() => window.scrollTo(left, top));
  }

  protected savePrecedenceList(): void {
    const list = this.precedenceList();

    if (!list || this.savingPrecedenceList()) {
      return;
    }

    const precedenceList = {
      ...list,
      precedence: {
        ...(list.precedence ?? {}),
        keyValuePairs: this.precedenceEntries()
      }
    };

    this.savingPrecedenceList.set(true);
    this.api
      .updatePrecedenceList(precedenceList)
      .pipe(finalize(() => this.savingPrecedenceList.set(false)))
      .subscribe({
        next: () => {
          this.precedenceTouchedKeys.set([]);
          this.notifications.success('Precedence list saved.');
        },
        error: () => {
          this.notifications.error('Precedence list could not be saved.');
        }
      });
  }

  protected saveProject(): void {
    const form = this.projectForm();

    if (!form || this.savingProject()) {
      return;
    }

    const errors = this.validateProjectForm(form);
    this.projectFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    const project = this.buildProjectPayload(form);
    const request: Observable<AdminProject | void> =
      form.mode === 'add' ? this.api.addProject(project) : this.api.updateProject(project);

    this.savingProject.set(true);
    request.pipe(finalize(() => this.savingProject.set(false))).subscribe({
      next: (savedProject) => {
        if (form.mode === 'add' && savedProject) {
          this.completeAddedProject(savedProject);
        } else {
          this.completeProjectSave('Project updated.');
        }
      },
      error: (error: unknown) => {
        this.projectFormErrors.set([this.describeProjectSaveError(error)]);
      }
    });
  }

  protected removeProject(project: AdminProject): void {
    const projectId = project.id;

    if (projectId === null || projectId === undefined || this.deletingProjectId() !== null) {
      this.notifications.error('Project could not be removed.');
      return;
    }

    if (!window.confirm(this.removeProjectConfirmation(project))) {
      return;
    }

    this.deletingProjectId.set(projectId);
    this.api
      .removeProject(projectId)
      .pipe(finalize(() => this.deletingProjectId.set(null)))
      .subscribe({
        next: () => {
          this.notifications.success('Project removed.');
          this.projectForm.update((form) =>
            form?.sourceProject?.id === projectId ? null : form
          );
          this.loadProjects();
          this.loadUsers();
        },
        error: (error: unknown) => {
          this.notifications.error(this.describeProjectError(error, 'Project could not be removed.'));
        }
      });
  }

  protected openProjectLog(project: AdminProject): void {
    this.projectLogState.set({ filter: '', loading: true, log: '', project });
    this.fetchProjectLog(project, '');
  }

  protected closeProjectLog(): void {
    this.projectLogState.set(null);
  }

  protected updateProjectLogFilter(value: string): void {
    this.projectLogState.update((state) => (state ? { ...state, filter: value } : state));
  }

  protected searchProjectLog(): void {
    const state = this.projectLogState();

    if (state) {
      this.fetchProjectLog(state.project, state.filter);
    }
  }

  protected clearProjectLogFilter(): void {
    const state = this.projectLogState();

    if (!state) {
      return;
    }

    this.projectLogState.set({ ...state, filter: '' });
    this.fetchProjectLog(state.project, '');
  }

  private fetchProjectLog(project: AdminProject, filter: string): void {
    const projectId = project.id;

    if (projectId === null || projectId === undefined) {
      return;
    }

    this.projectLogState.update((state) => (state ? { ...state, loading: true } : state));
    this.api.getProjectLog(projectId, filter).subscribe({
      next: (log) => {
        this.projectLogState.update((state) => (state ? { ...state, loading: false, log } : state));
      },
      error: () => {
        this.notifications.error('Project log could not be loaded.');
        this.projectLogState.update((state) => (state ? { ...state, loading: false } : state));
      }
    });
  }

  protected removeUser(user: AdminUser): void {
    const userId = user.id;
    const userName = user.userName ?? 'user';

    if (Object.keys(user.projectRoleMap ?? {}).length > 0) {
      window.alert(
        'You can not remove a user that is assigned to a project - ' +
          'Remove this user from all projects before deleting it'
      );
      return;
    }

    if (userId === null || userId === undefined || this.deletingUserId() !== null) {
      this.notifications.error('User could not be removed.');
      return;
    }

    if (!window.confirm(`Are you sure you want to remove the user (${userName})?`)) {
      return;
    }

    this.deletingUserId.set(userId);
    this.api
      .removeUser(userId)
      .pipe(finalize(() => this.deletingUserId.set(null)))
      .subscribe({
        next: () => {
          this.notifications.success('User removed.');
          this.userForm.update((form) => (form?.sourceUser?.id === userId ? null : form));
          this.loadUsers();
          this.loadProjects();
        },
        error: () => {
          this.notifications.error('User could not be removed.');
        }
      });
  }

  protected updateUserPreferenceFeedbackEmail(value: string): void {
    this.userPreferenceFeedbackEmail.set(value);
  }

  protected saveFeedbackEmailPreference(): void {
    const preferences = this.currentUserPreferencesForUpdate({
      feedbackEmail: this.userPreferenceFeedbackEmail().trim() || null
    });

    if (!preferences) {
      this.notifications.error('User preferences could not be saved.');
      return;
    }

    this.saveUserPreferences(preferences, 'User preferences saved.');
  }

  protected resetUserPreferences(): void {
    if (!window.confirm('Reset user preferences to defaults?')) {
      return;
    }

    const preferences = this.currentUserPreferencesForUpdate({
      lastProjectId: null,
      lastProjectRole: null,
      lastTab: null,
      lastTerminology: null,
      properties: {}
    });

    if (!preferences) {
      this.notifications.error('User preferences could not be reset.');
      return;
    }

    this.saveUserPreferences(preferences, 'User preferences reset.');
  }

  protected preferencePropertyEntries(): RoleEntry[] {
    return Object.entries(this.currentUser().userPreferences?.properties ?? {})
      .map(([key, value]) => ({ key, value: String(value ?? '') }))
      .sort((left, right) => left.key.localeCompare(right.key, undefined, {
        numeric: true,
        sensitivity: 'base'
      }));
  }

  protected runAdminOperation(operation: AdminOperation): void {
    const confirmation = this.adminOperationConfirmation(operation);

    if (!window.confirm(confirmation)) {
      return;
    }

    const request =
      operation === 'reloadConfig'
        ? this.api.reloadConfigProperties()
        : this.api.forceException(operation === 'localException');

    this.runningAdminOperation.set(operation);
    request.pipe(finalize(() => this.runningAdminOperation.set(null))).subscribe({
      next: () => {
        if (operation === 'reloadConfig') {
          this.notifications.success('Configuration reloaded.');
          this.loadProjectConfiguration();
          this.loadProjects();
          this.loadUsers();
        }
      },
      error: () => {
        if (operation === 'reloadConfig') {
          this.notifications.error('Configuration could not be reloaded.');
        }
      }
    });
  }

  protected startAddUser(): void {
    const defaultRole = this.applicationRoles()[0] ?? 'VIEWER';

    this.userForm.set({
      applicationRole: defaultRole,
      editorLevel: '0',
      email: '',
      mode: 'add',
      name: '',
      sourceUser: null,
      userName: ''
    });
    this.userFormErrors.set([]);
  }

  protected startEditUser(user: AdminUser): void {
    this.userForm.set({
      applicationRole: user.applicationRole ?? this.applicationRoles()[0] ?? 'VIEWER',
      editorLevel:
        user.editorLevel === null || user.editorLevel === undefined
          ? ''
          : String(user.editorLevel),
      email: user.email ?? '',
      mode: 'edit',
      name: user.name ?? '',
      sourceUser: user,
      userName: user.userName ?? ''
    });
    this.userFormErrors.set([]);
  }

  protected cancelUserForm(): void {
    this.userForm.set(null);
    this.userFormErrors.set([]);
  }

  protected updateUserForm(field: UserFormField, value: number | string): void {
    this.userForm.update((form) =>
      form ? { ...form, [field]: String(value ?? '') } : form
    );
  }

  protected saveUser(): void {
    const form = this.userForm();

    if (!form || this.savingUser()) {
      return;
    }

    const errors = this.validateUserForm(form);
    this.userFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    const user = this.buildUserPayload(form);
    const request: Observable<AdminUser | void> =
      form.mode === 'add' ? this.api.addUser(user) : this.api.updateUser(user);

    this.savingUser.set(true);
    request.pipe(finalize(() => this.savingUser.set(false))).subscribe({
      next: () => {
        this.notifications.success(
          form.mode === 'add' ? 'User added.' : 'User updated.'
        );
        this.userForm.set(null);
        this.userFormErrors.set([]);
        this.loadUsers();
      },
      error: () => {
        this.userFormErrors.set(['User could not be saved.']);
      }
    });
  }

  // ---------- User & Project Management methods ----------

  protected loadCandidateProjects(): void {
    this.loadingCandidateProjects.set(true);
    const filter = this.candidateProjectFilter().trim();
    this.api
      .findProjects(
        buildPfs(
          this.candidateProjectPage(),
          this.candidateProjectPageSize(),
          'lastModified',
          false,
          filter
        )
      )
      .pipe(finalize(() => this.loadingCandidateProjects.set(false)))
      .subscribe({
        next: (state) => {
          // Application administrators can manage any project; everyone else
          // only sees projects where they hold the project-level ADMINISTRATOR role
          if (this.currentUser().applicationRole === 'ADMINISTRATOR') {
            this.candidateProjects.set(state.items);
            this.candidateProjectTotalCount.set(state.totalCount);
            return;
          }

          const currentUserNames = [
            this.currentUser().userName,
            this.currentUser().authToken
          ].filter((v): v is string => Boolean(v)).map((v) => v.toLocaleLowerCase());
          const adminProjects = state.items.filter((project) =>
            Object.entries(project.userRoleMap ?? {}).some(
              ([userName, role]) =>
                currentUserNames.includes(userName.toLocaleLowerCase()) &&
                role === 'ADMINISTRATOR'
            )
          );
          this.candidateProjects.set(adminProjects);
          this.candidateProjectTotalCount.set(adminProjects.length);
        },
        error: () => {
          this.notifications.error('Candidate projects could not be loaded.');
        }
      });
  }

  protected selectCandidateProject(project: AdminProject): void {
    this.selectedCandidateProject.set(project);
    this.unassignedUserPage.set(1);
    this.assignedUserPage.set(1);
    this.loadUnassignedUsers();
    this.loadAssignedUsers();
  }

  protected loadUnassignedUsers(): void {
    const project = this.selectedCandidateProject();

    if (project?.id === null || project?.id === undefined) {
      this.unassignedUsers.set([]);
      this.unassignedUserTotalCount.set(0);
      return;
    }

    this.api
      .findUnassignedUsersForProject(
        project.id,
        buildPfs(
          this.unassignedUserPage(),
          this.unassignedUserPageSize(),
          'userName',
          true,
          this.unassignedUserFilter().trim()
        ),
        '(applicationRole:USER OR applicationRole:ADMINISTRATOR)'
      )
      .subscribe({
        next: (state) => {
          this.unassignedUsers.set(state.items);
          this.unassignedUserTotalCount.set(state.totalCount);
          // Seed default role for each unassigned user
          const defaultRole = this.projectRoles()[0] ?? 'AUTHOR';
          const roles = { ...this.upmRoles() };
          state.items.forEach((user) => {
            if (user.userName && !roles[user.userName]) {
              roles[user.userName] = defaultRole;
            }
          });
          this.upmRoles.set(roles);
        },
        error: () => {
          this.notifications.error('Unassigned users could not be loaded.');
        }
      });
  }

  protected loadAssignedUsers(): void {
    const project = this.selectedCandidateProject();

    if (project?.id === null || project?.id === undefined) {
      this.assignedUsers.set([]);
      this.assignedUserTotalCount.set(0);
      return;
    }

    this.api
      .findAssignedUsersForProject(
        project.id,
        buildPfs(
          this.assignedUserPage(),
          this.assignedUserPageSize(),
          'userName',
          true,
          this.assignedUserFilter().trim()
        )
      )
      .subscribe({
        next: (state) => {
          this.assignedUsers.set(state.items);
          this.assignedUserTotalCount.set(state.totalCount);
        },
        error: () => {
          this.notifications.error('Assigned users could not be loaded.');
        }
      });
  }

  protected upmRoleForUser(userName: string | null | undefined): string {
    return this.upmRoles()[userName ?? ''] ?? this.projectRoles()[0] ?? 'AUTHOR';
  }

  protected setUpmRoleForUser(userName: string | null | undefined, role: string): void {
    if (!userName) {
      return;
    }
    this.upmRoles.update((roles) => ({ ...roles, [userName]: role }));
  }

  protected upmAssignedRole(user: AdminUser, project: AdminProject | null): string {
    if (!project || !user.userName) {
      return 'n/a';
    }
    const entry = Object.entries(project.userRoleMap ?? {}).find(
      ([name]) => name.toLocaleLowerCase() === (user.userName ?? '').toLocaleLowerCase()
    );
    return entry?.[1] ?? 'n/a';
  }

  protected upmAssignUser(user: AdminUser): void {
    const project = this.selectedCandidateProject();
    const userName = user.userName?.trim() ?? '';
    const role = this.upmRoleForUser(userName);
    const projectId = project?.id;

    if (!project || projectId === null || projectId === undefined || !userName) {
      this.notifications.error('Could not assign user to project.');
      return;
    }

    this.savingProjectAssignment.set(true);
    this.api
      .assignUserToProject(projectId, userName, role)
      .pipe(finalize(() => this.savingProjectAssignment.set(false)))
      .subscribe({
        next: (updatedProject) => {
          this.selectedCandidateProject.set(updatedProject);
          this.loadUnassignedUsers();
          this.loadAssignedUsers();
          this.loadProjects();
          this.loadUsers();
        },
        error: () => {
          this.notifications.error('User could not be assigned to project.');
        }
      });
  }

  protected upmUnassignUser(user: AdminUser): void {
    const project = this.selectedCandidateProject();
    const userName = user.userName?.trim() ?? '';
    const projectId = project?.id;

    if (!project || projectId === null || projectId === undefined || !userName) {
      this.notifications.error('Could not remove user from project.');
      return;
    }

    if (!window.confirm(`Remove ${userName} from ${project.name ?? 'project'}?`)) {
      return;
    }

    this.savingProjectAssignment.set(true);
    this.api
      .unassignUserFromProject(projectId, userName)
      .pipe(finalize(() => this.savingProjectAssignment.set(false)))
      .subscribe({
        next: (updatedProject) => {
          this.selectedCandidateProject.set(updatedProject);
          this.loadUnassignedUsers();
          this.loadAssignedUsers();
          this.loadProjects();
          this.loadUsers();
        },
        error: () => {
          this.notifications.error('User could not be removed from project.');
        }
      });
  }

  protected setCandidateProjectFilter(value: string): void {
    this.candidateProjectFilter.set(value);
    this.candidateProjectPage.set(1);
    this.loadCandidateProjects();
  }

  protected setCandidateProjectPageSize(value: number): void {
    this.candidateProjectPageSize.set(value);
    this.candidateProjectPage.set(1);
    this.loadCandidateProjects();
  }

  protected setCandidateProjectPage(page: number): void {
    this.candidateProjectPage.set(page);
    this.loadCandidateProjects();
  }

  protected setUnassignedUserFilter(value: string): void {
    this.unassignedUserFilter.set(value);
    this.unassignedUserPage.set(1);
    this.loadUnassignedUsers();
  }

  protected setUnassignedUserPageSize(value: number): void {
    this.unassignedUserPageSize.set(value);
    this.unassignedUserPage.set(1);
    this.loadUnassignedUsers();
  }

  protected setUnassignedUserPage(page: number): void {
    this.unassignedUserPage.set(page);
    this.loadUnassignedUsers();
  }

  protected setAssignedUserFilter(value: string): void {
    this.assignedUserFilter.set(value);
    this.assignedUserPage.set(1);
    this.loadAssignedUsers();
  }

  protected setAssignedUserPageSize(value: number): void {
    this.assignedUserPageSize.set(value);
    this.assignedUserPage.set(1);
    this.loadAssignedUsers();
  }

  protected setAssignedUserPage(page: number): void {
    this.assignedUserPage.set(page);
    this.loadAssignedUsers();
  }

  // ---------- end User & Project Management ----------

  protected setUserFilter(value: string): void {
    const wasFiltered = Boolean(this.userFilter().trim());

    this.userFilter.set(value);
    this.userPage.set(1);

    if (wasFiltered && !value.trim()) {
      this.loadUsers();
    }
  }

  protected setProjectPageSize(value: number): void {
    this.projectPageSize.set(value);
    this.projectPage.set(1);
    this.loadProjects();
  }

  protected setUserPageSize(value: number): void {
    this.userPageSize.set(value);
    this.userPage.set(1);
    this.loadUsers();
  }

  protected setProjectSortField(field: ProjectSortField): void {
    if (this.projectSortField() === field) {
      this.projectSortAscending.update((ascending) => !ascending);
    } else {
      this.projectSortField.set(field);
      this.projectSortAscending.set(true);
    }

    this.projectPage.set(1);
    this.loadProjects();
  }

  protected setUserSortField(field: UserSortField): void {
    if (this.userSortField() === field) {
      this.userSortAscending.update((ascending) => !ascending);
    } else {
      this.userSortField.set(field);
      this.userSortAscending.set(true);
    }

    this.userPage.set(1);
    this.loadUsers();
  }

  protected projectSortIndicator(field: ProjectSortField): string {
    if (this.projectSortField() !== field) {
      return '';
    }

    return this.projectSortAscending() ? 'ascending' : 'descending';
  }

  protected userSortIndicator(field: UserSortField): string {
    if (this.userSortField() !== field) {
      return '';
    }

    return this.userSortAscending() ? 'ascending' : 'descending';
  }

  protected setProjectPage(page: number): void {
    this.projectPage.set(page);
    this.loadProjects();
  }

  protected setUserPage(page: number): void {
    this.userPage.set(page);
    this.loadUsers();
  }

  protected precedenceTrackKey(entry: AdminKeyValuePair, _index: number): string {
    return this.precedenceEntryKey(entry);
  }

  private loadRoles(): void {
    this.loadingRoles.set(true);
    this.api
      .getApplicationRoles()
      .pipe(finalize(() => this.loadingRoles.set(false)))
      .subscribe({
        next: (roles) => this.applicationRoles.set(roles),
        error: () => {
          this.notifications.error('Application roles could not be loaded.');
        }
      });

    this.api.getProjectRoles().subscribe({
      next: (roles) => this.projectRoles.set(roles),
      error: () => {
        this.notifications.error('Project roles could not be loaded.');
      }
    });
  }

  private loadProjectConfiguration(): void {
    this.api.getCurrentTerminologies().subscribe({
      next: (terminologies) => this.projectTerminologies.set(terminologies),
      error: () => {
        this.notifications.error('Terminology options could not be loaded.');
      }
    });

    this.api.getWorkflowPaths().subscribe({
      next: (paths) => {
        this.workflowPaths.set(paths);
        this.applyDefaultWorkflowPath();
      },
      error: () => {
        this.notifications.error('Workflow paths could not be loaded.');
      }
    });

    this.api.getValidationChecks().subscribe({
      next: (checks) => {
        this.validationCheckDefinitions.set(checks);
        this.applyDefaultValidationChecks();
      },
      error: () => {
        this.notifications.error('Validation checks could not be loaded.');
      }
    });
  }

  private loadProjectPrecedenceList(precedenceListId: number | null | undefined): void {
    this.clearPrecedenceList();

    if (precedenceListId === null || precedenceListId === undefined) {
      return;
    }

    this.loadingPrecedenceList.set(true);
    this.api
      .getPrecedenceList(precedenceListId)
      .pipe(finalize(() => this.loadingPrecedenceList.set(false)))
      .subscribe({
        next: (list) => {
          this.precedenceList.set(list);
        },
        error: () => {
          this.notifications.error('Precedence list could not be loaded.');
        }
      });
  }

  private loadProjectLanguages(terminology: string, version: string): void {
    const cleanTerminology = terminology.trim();
    const cleanVersion = version.trim();

    if (!cleanTerminology || !cleanVersion) {
      this.projectLanguageOptions.set([]);
      return;
    }

    this.api.getMetadataLanguages(cleanTerminology, cleanVersion).subscribe({
      next: (languages) => {
        this.projectLanguageOptions.set(languages);
        this.applyDefaultProjectLanguage(languages);
      },
      error: () => {
        this.projectLanguageOptions.set([]);
        this.notifications.error('Language options could not be loaded.');
      }
    });
  }

  private applyDefaultProjectLanguage(languages: AdminKeyValuePair[]): void {
    const form = this.projectForm();

    if (!form || form.language) {
      return;
    }

    const defaultLanguage =
      languages.find((language) => language.key === 'ENG')?.key ??
      languages[0]?.key ??
      '';

    if (defaultLanguage) {
      this.projectForm.set({
        ...form,
        language: defaultLanguage
      });
    }
  }

  private applyDefaultWorkflowPath(): void {
    const form = this.projectForm();
    const workflowPath = this.defaultWorkflowPath(form?.workflowPath ?? '');

    if (form && !form.workflowPath && workflowPath) {
      this.projectForm.set({
        ...form,
        workflowPath
      });
    }
  }

  private applyDefaultValidationChecks(): void {
    const form = this.projectForm();

    if (!form || form.mode !== 'add' || form.validationChecks.length) {
      return;
    }

    this.projectForm.set({
      ...form,
      validationChecks: this.defaultValidationCheckKeys()
    });
  }

  private defaultWorkflowPath(workflowPath: string): string {
    if (workflowPath.trim()) {
      return workflowPath;
    }

    const paths = this.workflowPaths();
    return paths.length === 1 ? paths[0] : '';
  }

  private defaultValidationCheckKeys(): string[] {
    return this.validationCheckDefinitions()
      .filter((check) => this.validationCheckLabel(check).startsWith('Default'))
      .map((check) => check.key);
  }

  protected validationCheckLabel(check: AdminKeyValuePair): string {
    return check.value || check.key;
  }

  private clearPrecedenceList(): void {
    this.precedenceList.set(null);
    this.precedenceTouchedKeys.set([]);
    this.draggingPrecedenceIndex.set(null);
    this.loadingPrecedenceList.set(false);
  }

  private markPrecedenceEntryTouched(entry: AdminKeyValuePair): void {
    const key = this.precedenceEntryKey(entry);
    this.precedenceTouchedKeys.update((keys) => (keys.includes(key) ? keys : [...keys, key]));
  }

  private precedenceEntryKey(entry: AdminKeyValuePair): string {
    return `${entry.key ?? ''}|${entry.value ?? ''}`;
  }

  private pageCount(totalCount: number, pageSize: number): number {
    return Math.max(1, Math.ceil(totalCount / pageSize));
  }

  private completeAddedProject(project: AdminProject): void {
    const user = this.auth.currentUser();
    const projectId = project.id;
    const userName = user.userName?.trim() || user.authToken?.trim() || '';

    if (
      user.applicationRole === 'ADMINISTRATOR' ||
      projectId === null ||
      projectId === undefined ||
      !userName
    ) {
      this.completeProjectSave('Project added.');
      return;
    }

    this.savingProject.set(true);
    this.api
      .assignUserToProject(projectId, userName, 'ADMINISTRATOR')
      .pipe(finalize(() => this.savingProject.set(false)))
      .subscribe({
        next: () => {
          this.updateLastProjectPreference(projectId, () => {
            this.completeProjectSave('Project added.');
          });
        },
        error: () => {
          this.projectFormErrors.set([
            'Project was added, but the creator could not be assigned as project administrator.'
          ]);
        }
      });
  }

  private updateLastProjectPreference(projectId: number, complete: () => void): void {
    const preferences = this.currentUserPreferencesForUpdate({
      lastProjectId: projectId
    });

    if (!preferences) {
      complete();
      return;
    }

    this.api.updateUserPreferences(preferences).subscribe({
      next: (savedPreferences) => {
        this.auth.updateCurrentUserPreferences(savedPreferences ?? preferences);
        this.userPreferenceFeedbackEmail.set(
          (savedPreferences ?? preferences).feedbackEmail ?? ''
        );
        complete();
      },
      error: () => {
        this.notifications.error(
          'Project was added, but the last project preference could not be saved.'
        );
        complete();
      }
    });
  }

  private saveUserPreferences(
    preferences: UserPreferences,
    successMessage: string
  ): void {
    this.savingUserPreferences.set(true);
    this.api
      .updateUserPreferences(preferences)
      .pipe(finalize(() => this.savingUserPreferences.set(false)))
      .subscribe({
        next: (savedPreferences) => {
          const nextPreferences = savedPreferences ?? preferences;
          this.auth.updateCurrentUserPreferences(nextPreferences);
          this.userPreferenceFeedbackEmail.set(nextPreferences.feedbackEmail ?? '');
          this.notifications.success(successMessage);
        },
        error: () => {
          this.notifications.error('User preferences could not be saved.');
        }
      });
  }

  private currentUserPreferencesForUpdate(
    overrides: Partial<UserPreferences>
  ): UserPreferences | null {
    const user = this.currentUser();
    const preferences = user.userPreferences;
    const userName = user.userName ?? user.authToken;

    if (!preferences || this.auth.isGuest() || !userName) {
      return null;
    }

    return {
      ...preferences,
      properties: {
        ...(preferences.properties ?? {})
      },
      user: preferences.user ?? {
        id: user.id,
        userName
      },
      userId: preferences.userId ?? user.id,
      userName: preferences.userName ?? userName,
      ...overrides
    };
  }

  private adminOperationConfirmation(operation: AdminOperation): string {
    switch (operation) {
      case 'exception':
        return 'Are you sure you want to force an exception?';
      case 'localException':
        return 'Are you sure you want to force a test exception?';
      case 'reloadConfig':
        return 'Are you sure you want to reload the configuration?';
    }
  }

  private completeProjectSave(message: string): void {
    this.notifications.success(message);
    this.projectForm.set(null);
    this.projectFormErrors.set([]);
    this.clearPrecedenceList();
    this.loadProjects();
  }

  private validateProjectForm(form: ProjectForm): string[] {
    const errors: string[] = [];

    if (
      form.mode === 'edit' &&
      (form.sourceProject?.id === null || form.sourceProject?.id === undefined)
    ) {
      errors.push('Project id is required.');
    }

    if (
      !form.name.trim() ||
      !form.description.trim() ||
      !form.terminology.trim() ||
      !form.version.trim() ||
      !form.workflowPath.trim()
    ) {
      errors.push(
        'The name, description, terminology, version, and workflow path fields cannot be blank.'
      );
    }

    return errors;
  }

  private removeProjectConfirmation(project: AdminProject): string {
    const name = project.name || `project ${project.id ?? ''}`.trim();
    const hasAssignedUsers = Object.keys(project.userRoleMap ?? {}).length > 0;

    if (hasAssignedUsers) {
      return (
        'The project has users assigned to it. Are you sure you want to remove the project ' +
        `(${name}) and unassign all of its users?`
      );
    }

    return `Are you sure you want to remove the project (${name})?`;
  }

  private describeProjectSaveError(error: unknown): string {
    return this.describeProjectError(error, 'Project could not be saved.');
  }

  private describeProjectError(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    return fallback;
  }

  private validateValidationDataForm(form: ValidationDataForm): string[] {
    const errors: string[] = [];

    if (!form.type.trim() || !form.key.trim()) {
      errors.push('The validation check and value 1 fields cannot be blank.');
    }

    return errors;
  }

  private validateUserForm(form: UserForm): string[] {
    const errors: string[] = [];

    if (!form.userName.trim() || !form.name.trim() || !form.applicationRole.trim()) {
      errors.push('The name, user name, and application role fields cannot be blank.');
    }

    if (form.editorLevel.trim() && Number.isNaN(Number(form.editorLevel))) {
      errors.push('Editor level must be numeric.');
    }

    return errors;
  }

  private buildProjectPayload(form: ProjectForm): AdminProject {
    const sourceProject = form.sourceProject ?? {};

    return {
      ...sourceProject,
      automationsEnabled: form.automationsEnabled,
      description: form.description.trim(),
      editingEnabled: form.editingEnabled,
      feedbackEmail: form.feedbackEmail.trim() || null,
      language: form.language.trim() || null,
      name: form.name.trim(),
      terminology: form.terminology.trim(),
      validationChecks: form.validationChecks,
      validationData: form.validationData,
      version: form.version.trim(),
      workflowPath: form.workflowPath.trim() || null
    };
  }

  private currentUserProjectRole(project: AdminProject): string | null {
    const user = this.auth.currentUser();
    const candidateNames = [user.userName, user.authToken]
      .filter((value): value is string => Boolean(value))
      .map((value) => value.toLocaleLowerCase());

    const entry = Object.entries(project.userRoleMap ?? {}).find(([userName]) =>
      candidateNames.includes(userName.toLocaleLowerCase())
    );

    return entry?.[1] ?? null;
  }

  private buildUserPayload(form: UserForm): AdminUser {
    const sourceUser = form.sourceUser ?? {};
    const editorLevel = form.editorLevel.trim();

    return {
      ...sourceUser,
      applicationRole: form.applicationRole,
      editorLevel: editorLevel ? Number(editorLevel) : null,
      email: form.email.trim() || null,
      name: form.name.trim(),
      userName: form.userName.trim()
    };
  }
}
