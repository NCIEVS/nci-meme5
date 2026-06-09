import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { finalize, Observable } from 'rxjs';

import { UserPreferences } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { DialogComponent } from '../../shared/dialog/dialog.component';
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

type UserSortField = 'userName' | 'name' | 'email' | 'applicationRole';
type ProjectSortField = 'lastModified' | 'id' | 'name' | 'terminology';
type AdminOperation = 'exception' | 'localException' | 'reloadConfig';
type ProjectAssignmentFormField = 'projectId' | 'role';
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

interface ProjectAssignmentForm {
  projectId: string;
  role: string;
  user: AdminUser;
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
  imports: [DialogComponent, FormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly applicationRoles = signal<string[]>([]);
  protected readonly projectRoles = signal<string[]>([]);
  protected readonly projects = signal<AdminProject[]>([]);
  protected readonly projectFilter = signal('');
  protected readonly projectPage = signal(1);
  protected readonly projectPageSize = signal(10);
  protected readonly projectSortAscending = signal(true);
  protected readonly projectSortField = signal<ProjectSortField>('lastModified');
  protected readonly projectTotalCount = signal(0);
  protected readonly loadingProjects = signal(false);
  protected readonly loadingPrecedenceList = signal(false);
  protected readonly loadingRoles = signal(false);
  protected readonly loadingUsers = signal(false);
  protected readonly selectedProject = signal<AdminProject | null>(null);
  protected readonly selectedUser = signal<AdminUser | null>(null);
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
  protected readonly projectForm = signal<ProjectForm | null>(null);
  protected readonly projectFormErrors = signal<string[]>([]);
  protected readonly projectAssignmentForm = signal<ProjectAssignmentForm | null>(null);
  protected readonly projectAssignmentErrors = signal<string[]>([]);
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
    this.loadRoles();
    this.loadProjectConfiguration();
    this.loadProjects();
    this.loadUsers();
  }

  protected loadProjects(
    preferredProjectId: number | null | undefined = this.selectedProject()?.id
  ): void {
    this.loadingProjects.set(true);
    this.api
      .findProjects(
        buildPfs(
          this.projectPage(),
          this.projectPageSize(),
          this.projectSortField(),
          this.projectSortAscending(),
          this.projectFilter()
        )
      )
      .pipe(finalize(() => this.loadingProjects.set(false)))
      .subscribe({
        next: (state) => {
          this.projects.set(state.items);
          this.projectTotalCount.set(state.totalCount);
          this.selectProject(
            state.items.find((project) => project.id === preferredProjectId) ??
              state.items[0] ??
              null
          );
        },
        error: () => {
          this.notifications.error('Projects could not be loaded.');
        }
      });
  }

  protected loadUsers(
    preferredUserName: string | null | undefined = this.selectedUser()?.userName
  ): void {
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
          this.selectUser(
            state.items.find((user) => user.userName === preferredUserName) ??
              state.items[0] ??
              null
          );
        },
        error: () => {
          this.notifications.error('Users could not be loaded.');
        }
      });
  }

  protected selectProject(project: AdminProject | null): void {
    this.selectedProject.set(project);
  }

  protected selectUser(user: AdminUser | null): void {
    this.selectedUser.set(user);
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
    this.selectProject(project);
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

  protected movePrecedenceEntry(index: number, direction: -1 | 1): void {
    const list = this.precedenceList();
    const entries = [...this.precedenceEntries()];
    const targetIndex = index + direction;

    if (!list || targetIndex < 0 || targetIndex >= entries.length) {
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
          this.completeProjectSave(project, 'Project updated.');
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
    this.selectUser(user);
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
      next: (savedUser) => {
        this.notifications.success(
          form.mode === 'add' ? 'User added.' : 'User updated.'
        );
        this.userForm.set(null);
        this.userFormErrors.set([]);
        this.loadUsers(savedUser?.userName ?? user.userName);

        if (savedUser) {
          this.selectUser(savedUser);
        }
      },
      error: () => {
        this.userFormErrors.set(['User could not be saved.']);
      }
    });
  }

  protected startProjectAssignment(user: AdminUser): void {
    this.selectUser(user);

    const selectedProject = this.selectedProject();
    const defaultProject =
      selectedProject?.id === null || selectedProject?.id === undefined
        ? this.projects().find((project) => project.id !== null && project.id !== undefined)
        : selectedProject;

    this.projectAssignmentForm.set({
      projectId:
        defaultProject?.id === null || defaultProject?.id === undefined
          ? ''
          : String(defaultProject.id),
      role: this.projectRoles()[0] ?? 'AUTHOR',
      user
    });
    this.projectAssignmentErrors.set([]);
  }

  protected cancelProjectAssignment(): void {
    this.projectAssignmentForm.set(null);
    this.projectAssignmentErrors.set([]);
  }

  protected updateProjectAssignmentForm(
    field: ProjectAssignmentFormField,
    value: number | string
  ): void {
    this.projectAssignmentForm.update((form) =>
      form ? { ...form, [field]: String(value ?? '') } : form
    );
  }

  protected saveProjectAssignment(): void {
    const form = this.projectAssignmentForm();

    if (!form || this.savingProjectAssignment()) {
      return;
    }

    const errors = this.validateProjectAssignmentForm(form);
    this.projectAssignmentErrors.set(errors);

    if (errors.length) {
      return;
    }

    const projectId = Number(form.projectId);
    const userName = form.user.userName?.trim() ?? '';
    const role = form.role.trim();

    this.savingProjectAssignment.set(true);
    this.api
      .assignUserToProject(projectId, userName, role)
      .pipe(finalize(() => this.savingProjectAssignment.set(false)))
      .subscribe({
        next: (project) => {
          this.notifications.success('Project role assigned.');
          this.projectAssignmentForm.set(null);
          this.projectAssignmentErrors.set([]);
          this.selectProject(project);
          this.loadProjects(project.id ?? projectId);
          this.loadUsers(userName);
        },
        error: () => {
          this.projectAssignmentErrors.set(['Project role could not be assigned.']);
        }
      });
  }

  protected removeProjectAssignment(user: AdminUser, role: RoleEntry): void {
    const projectId = Number(role.key);
    const userName = user.userName?.trim() ?? '';

    if (!Number.isFinite(projectId) || !userName || this.savingProjectAssignment()) {
      this.notifications.error('Project role could not be removed.');
      return;
    }

    if (!window.confirm(`Remove ${userName} from ${this.projectLabel(role.key)}?`)) {
      return;
    }

    this.savingProjectAssignment.set(true);
    this.api
      .unassignUserFromProject(projectId, userName)
      .pipe(finalize(() => this.savingProjectAssignment.set(false)))
      .subscribe({
        next: (project) => {
          this.notifications.success('Project role removed.');
          this.selectProject(project);
          this.loadProjects(project.id ?? projectId);
          this.loadUsers(userName);
        },
        error: () => {
          this.notifications.error('Project role could not be removed.');
        }
      });
  }

  protected setProjectFilter(value: string): void {
    const wasFiltered = Boolean(this.projectFilter().trim());

    this.projectFilter.set(value);
    this.projectPage.set(1);

    if (wasFiltered && !value.trim()) {
      this.loadProjects();
    }
  }

  protected setUserFilter(value: string): void {
    const wasFiltered = Boolean(this.userFilter().trim());

    this.userFilter.set(value);
    this.userPage.set(1);

    if (wasFiltered && !value.trim()) {
      this.loadUsers();
    }
  }

  protected setProjectPageSize(value: string): void {
    this.projectPageSize.set(Number(value));
    this.projectPage.set(1);
    this.loadProjects();
  }

  protected setUserPageSize(value: string): void {
    this.userPageSize.set(Number(value));
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

  protected previousProjectPage(): void {
    if (this.projectPage() === 1) {
      return;
    }

    this.projectPage.update((page) => page - 1);
    this.loadProjects();
  }

  protected nextProjectPage(): void {
    if (this.projectPage() === this.projectTotalPages()) {
      return;
    }

    this.projectPage.update((page) => page + 1);
    this.loadProjects();
  }

  protected previousUserPage(): void {
    if (this.userPage() === 1) {
      return;
    }

    this.userPage.update((page) => page - 1);
    this.loadUsers();
  }

  protected nextUserPage(): void {
    if (this.userPage() === this.userTotalPages()) {
      return;
    }

    this.userPage.update((page) => page + 1);
    this.loadUsers();
  }

  protected roleEntries(roleMap: Record<string, string> | null | undefined): RoleEntry[] {
    return Object.entries(roleMap ?? {})
      .map(([key, value]) => ({ key, value }))
      .sort((left, right) => left.key.localeCompare(right.key, undefined, {
        numeric: true,
        sensitivity: 'base'
      }));
  }

  protected projectLabel(projectId: number | string | null | undefined): string {
    const id = String(projectId ?? '');
    const project = this.projects().find((candidate) => String(candidate.id ?? '') === id);

    if (!id) {
      return 'n/a';
    }

    return project?.name ? `${project.name} (${id})` : `Project ${id}`;
  }

  protected listValue(values: Array<string | null> | null | undefined): string {
    const usableValues = values?.filter((value): value is string => Boolean(value));
    return usableValues?.length ? usableValues.join(', ') : 'n/a';
  }

  protected validationCheckLabels(checkKeys: Array<string | null> | null | undefined): string {
    const checksByKey = new Map(
      this.validationCheckDefinitions().map((check) => [check.key, check])
    );
    const labels = checkKeys
      ?.filter((key): key is string => Boolean(key))
      .map((key) => this.validationCheckLabel(checksByKey.get(key) ?? { key, value: key }));

    return labels?.length ? labels.join(', ') : 'n/a';
  }

  protected validationDataLabel(data: AdminValidationData): string {
    const type = data.type || 'n/a';
    const key = data.key || 'n/a';
    const value = data.value || '';

    return value ? `${type}: ${key}, ${value}` : `${type}: ${key}`;
  }

  protected precedenceTrackKey(entry: AdminKeyValuePair, index: number): string {
    return `${index}:${this.precedenceEntryKey(entry)}`;
  }

  protected formatDate(timestamp: number | null | undefined): string {
    if (!timestamp) {
      return 'n/a';
    }

    return new Date(timestamp).toLocaleString();
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
      this.completeProjectSave(project, 'Project added.');
      return;
    }

    this.savingProject.set(true);
    this.api
      .assignUserToProject(projectId, userName, 'ADMINISTRATOR')
      .pipe(finalize(() => this.savingProject.set(false)))
      .subscribe({
        next: (assignedProject) => {
          this.updateLastProjectPreference(projectId, () => {
            this.completeProjectSave(assignedProject, 'Project added.');
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

  private completeProjectSave(project: AdminProject, message: string): void {
    this.notifications.success(message);
    this.projectForm.set(null);
    this.projectFormErrors.set([]);
    this.clearPrecedenceList();
    this.selectProject(project);
    this.loadProjects(project.id);
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

  private validateProjectAssignmentForm(form: ProjectAssignmentForm): string[] {
    const errors: string[] = [];

    if (!form.user.userName?.trim()) {
      errors.push('User is required.');
    }

    if (!form.projectId.trim() || !Number.isFinite(Number(form.projectId))) {
      errors.push('Project is required.');
    }

    if (!form.role.trim()) {
      errors.push('Project role is required.');
    }

    return errors;
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
