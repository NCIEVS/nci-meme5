import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  catchError,
  finalize,
  forkJoin,
  Observable,
  of,
  Subscription,
  switchMap,
  timer
} from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { MaintenanceWindow } from '../../core/maintenance-window.models';
import {
  dateFromLegacyValue,
  formatEasternDateTime,
  formatMaintenanceWindowRange
} from '../../core/maintenance-window-time';
import { ProjectContextService } from '../../core/navigation/project-context.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { DialogComponent } from '../../shared/dialog/dialog.component';
import { IconComponent } from '../../shared/icon/icon.component';
import { PagerComponent } from '../../shared/pager/pager.component';
import { buildOperationalPfs } from './operational-api.helpers';
import { OperationalApiService } from './operational-api.service';
import {
  AlgorithmParameter,
  KeyValuePair,
  OperationalProject,
  OperationalTerminology,
  ProcessConfig,
  ProcessExecution,
  ProcessStep
} from './operational.models';

type ProcessOperation =
  | 'cancel'
  | 'execute'
  | 'prepare'
  | 'restart'
  | 'step'
  | 'unstep';
type ProcessMode = 'Config' | 'Execution';
type ProcessSortField = 'name' | 'lastModified';
type ProcessType =
  | 'Insertion'
  | 'Inversion'
  | 'Maintenance'
  | 'Release'
  | 'Report'
  | 'Autofix';

interface ProcessConfigForm {
  description: string;
  feedbackEmail: string;
  inputPath: string;
  logPath: string;
  mode: 'add' | 'edit';
  name: string;
  processConfig: ProcessConfig | null;
  terminology: string;
  type: string;
  version: string;
}

interface AlgorithmStepForm {
  algorithm: ProcessStep;
  algorithmKey: string;
  mode: 'add' | 'edit';
  processConfig: ProcessConfig;
  validationMessages: string[];
}

interface QueryTestContext {
  errors: string[];
  objectTypeName: string | null;
  queryStyle: 'ID' | 'ID_PAIR';
  queryType: string;
}

interface ProcessLogState {
  filter: string;
  loading: boolean;
  log: string;
  execution: ProcessExecution;
}

interface StepLogState {
  filter: string;
  loading: boolean;
  log: string;
  progress: number | null;
  step: ProcessStep;
}

interface MaintenanceWindowWarningState {
  operation: ProcessOperation;
  process: ProcessConfig | ProcessExecution;
  window: MaintenanceWindow;
}

@Component({
  selector: 'meme-process',
  imports: [DialogComponent, FormsModule, IconComponent, PagerComponent],
  templateUrl: './process.component.html',
  styleUrl: './operations.component.css'
})
export class ProcessComponent implements OnInit, OnDestroy {
  private readonly api = inject(OperationalApiService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly projectContext = inject(ProjectContextService);
  private executionFeedbackSubscription: Subscription | null = null;
  private readonly runningStateSubscription = new Subscription();

  protected readonly configPage = signal(1);
  protected readonly configPageSize = signal(10);
  protected readonly configTotalCount = signal(0);
  protected readonly configs = signal<ProcessConfig[]>([]);
  protected readonly algorithmStepForm = signal<AlgorithmStepForm | null>(null);
  protected readonly algorithmStepFormErrors = signal<string[]>([]);
  protected readonly algorithmTypes = signal<KeyValuePair[]>([]);
  protected readonly cloningConfigId = signal<number | null>(null);
  protected readonly deletingConfigId = signal<number | null>(null);
  protected readonly deletingExecutionId = signal<number | null>(null);
  protected readonly deletingStepId = signal<number | null>(null);
  protected readonly draggingAlgorithmStepIndex = signal<number | null>(null);
  protected readonly executionPage = signal(1);
  protected readonly executionPageSize = signal(10);
  protected readonly executionTotalCount = signal(0);
  protected readonly executions = signal<ProcessExecution[]>([]);
  protected readonly exportingConfigId = signal<number | null>(null);
  protected readonly filter = signal('');
  protected readonly importProcessDialogOpen = signal(false);
  protected readonly importProcessFile = signal<File | null>(null);
  protected readonly importProcessFormErrors = signal<string[]>([]);
  protected readonly importingProcessConfig = signal(false);
  protected readonly loading = signal(false);
  protected readonly loadingAlgorithmStep = signal(false);
  protected readonly loadingAlgorithmTypes = signal(false);
  protected readonly loadingConfigDetail = signal(false);
  protected readonly loadingExecutionFeedback = signal(false);
  protected readonly checkingMaintenanceWindowOperation = signal<ProcessOperation | null>(null);
  protected readonly loadingExecutionDetail = signal(false);
  protected readonly maintenanceWindowWarning = signal<MaintenanceWindowWarningState | null>(null);
  protected readonly queryPreviewMap = signal<Record<string, boolean>>({});
  protected readonly testingQueryKey = signal<string | null>(null);
  protected readonly activeStepId = signal<number | null>(null);
  protected readonly processProgress = signal<number | null>(null);
  protected readonly processLogState = signal<ProcessLogState | null>(null);
  protected readonly stepLogState = signal<StepLogState | null>(null);
  protected readonly processConfigForm = signal<ProcessConfigForm | null>(null);
  protected readonly processConfigFormErrors = signal<string[]>([]);
  protected readonly processModes: ProcessMode[] = ['Config', 'Execution'];
  protected readonly processSortAscending = signal(false);
  protected readonly processSortField = signal<ProcessSortField>('lastModified');
  protected readonly processTypes: ProcessType[] = [
    'Insertion',
    'Inversion',
    'Maintenance',
    'Release',
    'Report',
    'Autofix'
  ];
  protected readonly runningExecutions = signal<ProcessExecution[]>([]);
  protected readonly runningOperation = signal<ProcessOperation | null>(null);
  protected readonly savingAlgorithmStep = signal(false);
  protected readonly savingProcessConfig = signal(false);
  protected readonly selectedAlgorithmKey = signal('');
  protected readonly selectedConfig = signal<ProcessConfig | null>(null);
  protected readonly selectedConfigForExecution = signal<ProcessConfig | null>(null);
  protected readonly selectedConfigForExecutionStepId = signal<number | null>(null);
  protected readonly selectedExecution = signal<ProcessExecution | null>(null);
  protected readonly selectedMode = signal<ProcessMode>('Config');
  protected readonly selectedProcessType = signal<ProcessType>('Insertion');
  protected readonly selectedStepId = signal<number | null>(null);
  protected readonly stepProgress = signal<number | null>(null);
  protected readonly feedbackUpdatedAt = signal<number | null>(null);
  protected readonly terminologies = signal<OperationalTerminology[]>([]);
  protected readonly updatingStepId = signal<number | null>(null);
  protected readonly currentTerminologies = computed(() =>
    this.terminologies()
      .filter((terminology) => terminology.current !== false)
      .slice()
      .sort((a, b) =>
        (a.terminology ?? '').localeCompare(b.terminology ?? '')
      )
  );

  protected readonly displayedExecutions = computed(() => {
    const seen = new Set<string>();

    return [...this.runningExecutions(), ...this.executions()].filter((execution) => {
      const key = String(execution.id ?? execution.name ?? JSON.stringify(execution));

      if (seen.has(key)) {
        return false;
      }

      seen.add(key);
      return true;
    });
  });
  protected readonly currentPage = computed(() =>
    this.selectedMode() === 'Config' ? this.configPage() : this.executionPage()
  );
  protected readonly currentPageSize = computed(() =>
    this.selectedMode() === 'Config'
      ? this.configPageSize()
      : this.executionPageSize()
  );
  protected readonly currentProcesses = computed<
    Array<ProcessConfig | ProcessExecution>
  >(() =>
    this.selectedMode() === 'Config'
      ? this.configs()
      : this.displayedExecutions()
  );
  protected readonly currentTotalCount = computed(() =>
    this.selectedMode() === 'Config'
      ? this.configTotalCount()
      : this.executionTotalCount()
  );
  protected readonly currentTotalPages = computed(() =>
    this.pageCount(this.currentTotalCount(), this.currentPageSize())
  );
  protected readonly projectId = computed(() => this.projectContext.projectId());
  protected readonly projectRole = computed(
    () => this.projectContext.projectRole() || 'n/a'
  );
  protected readonly canManageProcesses = computed(
    () => this.projectContext.projectRole() === 'ADMINISTRATOR'
  );
  protected readonly availableProjects = signal<OperationalProject[]>([]);
  protected readonly currentProject = computed(() =>
    this.availableProjects().find((project) => project.id === this.projectId()) ??
    null
  );
  protected readonly availableRoles = computed<string[]>(() => {
    const user = this.auth.currentUser();
    const projectId = this.projectId();
    if (!projectId) return [];
    const assigned = user.projectRoleMap?.[String(projectId)];
    if (assigned === 'ADMINISTRATOR') return ['ADMINISTRATOR', 'REVIEWER', 'AUTHOR'];
    if (assigned === 'REVIEWER') return ['REVIEWER', 'AUTHOR'];
    if (assigned === 'AUTHOR') return ['AUTHOR'];
    return assigned ? [assigned] : [];
  });

  ngOnInit(): void {
    this.initializeProcessPreferences();
    this.loadTerminologies();
    this.loadProjects();
    this.load();
    this.startRunningStateRefresh();
  }

  ngOnDestroy(): void {
    this.stopExecutionFeedbackPolling();
    this.runningStateSubscription.unsubscribe();
  }

  protected loadProjects(): void {
    const user = this.auth.currentUser();
    const projectIds = Object.keys(user.projectRoleMap ?? {}).map(Number).filter(Boolean);
    this.api.findProjectsByIds(projectIds).subscribe({
      next: (projects) => this.availableProjects.set(projects),
      error: () => {}
    });
  }

  protected selectProject(idStr: string): void {
    const user = this.auth.currentUser();
    const newPrefs = { ...user.userPreferences, lastProjectId: Number(idStr), lastProjectRole: null };
    this.api.updateUserPreferences(newPrefs).subscribe({
      next: (saved) => {
        this.auth.updateCurrentUserPreferences(saved ?? newPrefs);
        this.load();
      },
      error: () => this.notifications.error('Could not switch project.')
    });
  }

  protected selectRole(role: string): void {
    const user = this.auth.currentUser();
    const newPrefs = { ...user.userPreferences, lastProjectRole: role };
    this.api.updateUserPreferences(newPrefs).subscribe({
      next: (saved) => {
        this.auth.updateCurrentUserPreferences(saved ?? newPrefs);
        this.load();
      },
      error: () => this.notifications.error('Could not switch role.')
    });
  }

  protected load(
    selectedConfigId: number | null | undefined = this.selectedConfig()?.id,
    selectedExecutionId: number | null | undefined = this.selectedExecution()?.id
  ): void {
    const projectId = this.projectId();

    if (!projectId) {
      return;
    }

    const configPfs = buildOperationalPfs(
      this.configPage(),
      this.configPageSize(),
      this.processSortField(),
      this.processSortAscending(),
      this.processQueryRestriction()
    );
    const executionPfs = buildOperationalPfs(
      this.executionPage(),
      this.executionPageSize(),
      this.processSortField(),
      this.processSortAscending(),
      this.processQueryRestriction()
    );
    this.loading.set(true);

    forkJoin({
      configs: this.api.findProcessConfigs(projectId, '', configPfs),
      executions: this.api.findProcessExecutions(projectId, '', executionPfs),
      running: this.api.getExecutingProcesses(projectId)
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ configs, executions, running }) => {
          this.configs.set(configs.items);
          this.configTotalCount.set(configs.totalCount);
          this.executions.set(executions.items);
          this.executionTotalCount.set(executions.totalCount);
          this.runningExecutions.set(running.items);
          const nextConfig =
            configs.items.find((config) => config.id === selectedConfigId) ??
            configs.items[0] ??
            null;
          const nextExecution =
            running.items.find((execution) => execution.id === selectedExecutionId) ??
            executions.items.find((execution) => execution.id === selectedExecutionId) ??
            running.items[0] ??
            executions.items[0] ??
            null;

          if (this.selectedMode() === 'Config') {
            this.selectConfig(nextConfig);
            this.selectedExecution.set(nextExecution);
          } else {
            this.selectedConfig.set(nextConfig);
            this.selectExecution(nextExecution);
          }
        },
        error: () => {
          this.notifications.error('Process information could not be loaded.');
        }
      });
  }

  protected setFilter(value: string): void {
    const wasFiltered = Boolean(this.filter().trim());

    this.filter.set(value);
    this.configPage.set(1);
    this.executionPage.set(1);

    if (wasFiltered && !value.trim()) {
      this.load();
    }
  }

  protected setMode(mode: ProcessMode): void {
    if (this.selectedMode() === mode) {
      return;
    }

    this.selectedMode.set(mode);
    this.selectedStepId.set(null);
    this.selectedConfigForExecutionStepId.set(null);
    this.saveProcessPreference('processMode', mode);
    this.load();
  }

  protected selectProcess(process: ProcessConfig | ProcessExecution): void {
    if (this.selectedMode() === 'Config') {
      this.selectConfig(process as ProcessConfig);
      return;
    }

    this.selectExecution(process as ProcessExecution);
  }

  protected isProcessSelected(process: ProcessConfig | ProcessExecution): boolean {
    const selected =
      this.selectedMode() === 'Config'
        ? this.selectedConfig()
        : this.selectedExecution();

    return Boolean(selected?.id && process.id && selected.id === process.id);
  }

  protected setProcessPage(page: number): void {
    if (this.selectedMode() === 'Config') {
      this.configPage.set(page);
    } else {
      this.executionPage.set(page);
    }

    this.load();
  }

  protected setProcessPageSize(value: number | string): void {
    if (this.selectedMode() === 'Config') {
      this.setConfigPageSize(value);
      return;
    }

    this.setExecutionPageSize(value);
  }

  protected setProcessSortField(field: ProcessSortField): void {
    if (this.processSortField() === field) {
      this.processSortAscending.update((ascending) => !ascending);
    } else {
      this.processSortField.set(field);
      this.processSortAscending.set(true);
    }

    this.configPage.set(1);
    this.executionPage.set(1);
    this.load();
  }

  protected setProcessType(value: string): void {
    const processType = this.matchingProcessType(value) ?? 'Insertion';

    this.selectedProcessType.set(processType);
    this.configPage.set(1);
    this.executionPage.set(1);
    this.saveProcessPreference('processType', processType);
    this.load();
  }

  protected setConfigPageSize(value: number | string): void {
    this.configPageSize.set(Number(value));
    this.configPage.set(1);
    this.load();
  }

  protected setExecutionPageSize(value: number | string): void {
    this.executionPageSize.set(Number(value));
    this.executionPage.set(1);
    this.load();
  }

  protected startAddProcessConfig(): void {
    const terminology =
      this.currentProject()?.terminology ??
      this.currentTerminologies()[0]?.terminology ??
      '';

    this.processConfigFormErrors.set([]);
    this.processConfigForm.set({
      description: '',
      feedbackEmail: '',
      inputPath: '',
      logPath: '',
      mode: 'add',
      name: '',
      processConfig: null,
      terminology,
      type: this.defaultProcessType(),
      version:
        this.processFormVersionForTerminology(terminology) ??
        this.currentProject()?.version ??
        ''
    });
  }

  protected startEditProcessConfig(config: ProcessConfig): void {
    this.processConfigFormErrors.set([]);
    this.processConfigForm.set({
      description: config.description ?? '',
      feedbackEmail: config.feedbackEmail ?? '',
      inputPath: config.inputPath ?? '',
      logPath: config.logPath ?? '',
      mode: 'edit',
      name: config.name ?? '',
      processConfig: config,
      terminology: config.terminology ?? '',
      type: config.type ?? this.defaultProcessType(),
      version: config.version ?? ''
    });
  }

  protected closeProcessConfigForm(): void {
    if (this.savingProcessConfig()) {
      return;
    }

    this.processConfigForm.set(null);
    this.processConfigFormErrors.set([]);
  }

  protected updateProcessConfigForm(
    field: keyof Omit<ProcessConfigForm, 'mode' | 'processConfig'>,
    value: string
  ): void {
    this.processConfigForm.update((form) =>
      form
        ? {
            ...form,
            [field]: value
          }
        : form
    );
  }

  protected setProcessFormTerminology(value: string): void {
    this.processConfigForm.update((form) =>
      form
        ? {
            ...form,
            terminology: value,
            version: this.processFormVersionForTerminology(value) ?? form.version
          }
        : form
    );
  }

  protected clearProcessFormTerminology(): void {
    this.processConfigForm.update((form) =>
      form
        ? {
            ...form,
            terminology: '',
            version: ''
          }
        : form
    );
  }

  private processFormVersionForTerminology(
    terminology: string
  ): string | null | undefined {
    return this.currentTerminologies().find(
      (entry) => entry.terminology === terminology
    )?.version;
  }

  protected saveProcessConfig(): void {
    const form = this.processConfigForm();
    const projectId = this.projectId();

    if (!form || !projectId) {
      return;
    }

    const errors = this.validateProcessConfigForm(form);
    this.processConfigFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    const payload = this.buildProcessConfigPayload(form);
    const request: Observable<ProcessConfig | void> =
      form.mode === 'add'
        ? this.api.addProcessConfig(projectId, payload)
        : this.api.updateProcessConfig(projectId, payload);

    this.savingProcessConfig.set(true);
    request.pipe(finalize(() => this.savingProcessConfig.set(false))).subscribe({
      next: (processConfig) => {
        this.processConfigForm.set(null);
        this.processConfigFormErrors.set([]);

        if (form.mode === 'add') {
          const addedConfig = processConfig as ProcessConfig;
          this.load(addedConfig.id, this.selectedExecution()?.id);
          return;
        }

        this.load(payload.id, this.selectedExecution()?.id);
      },
      error: () => {
        this.notifications.error('Process config could not be saved.');
      }
    });
  }

  protected removeProcessConfig(config: ProcessConfig): void {
    const projectId = this.projectId();

    if (!projectId || !config.id) {
      return;
    }

    if (
      !window.confirm(
        `Remove process config "${config.name || config.id}" and its steps?`
      )
    ) {
      return;
    }

    this.deletingConfigId.set(config.id);
    this.api
      .removeProcessConfig(projectId, config.id, true)
      .pipe(finalize(() => this.deletingConfigId.set(null)))
      .subscribe({
        next: () => {
          this.selectConfig(null);
          this.load(null, this.selectedExecution()?.id);
        },
        error: () => {
          this.notifications.error('Process config could not be removed.');
        }
      });
  }

  protected cloneProcessConfig(config: ProcessConfig): void {
    const projectId = this.projectId();

    if (!projectId || !config.id) {
      return;
    }

    this.cloningConfigId.set(config.id);
    this.api
      .cloneProcessConfig(projectId, config)
      .pipe(finalize(() => this.cloningConfigId.set(null)))
      .subscribe({
        next: (clonedConfig) => {
          this.selectedMode.set('Config');
          this.load(clonedConfig.id ?? config.id, this.selectedExecution()?.id);
        },
        error: () => {
          this.notifications.error('Process config could not be cloned.');
        }
      });
  }

  protected removeProcessExecution(execution: ProcessConfig | ProcessExecution): void {
    const projectId = this.projectId();

    if (!projectId || !execution.id) {
      return;
    }

    if (
      !window.confirm(
        `Remove process execution "${execution.name || execution.id}"?`
      )
    ) {
      return;
    }

    this.deletingExecutionId.set(execution.id);
    this.api
      .removeProcessExecution(projectId, execution.id, true)
      .pipe(finalize(() => this.deletingExecutionId.set(null)))
      .subscribe({
        next: () => {
          this.selectExecution(null);
          this.load(this.selectedConfig()?.id, null);
        },
        error: () => {
          this.notifications.error('Process execution could not be removed.');
        }
      });
  }

  protected startImportProcessConfig(): void {
    this.importProcessFile.set(null);
    this.importProcessFormErrors.set([]);
    this.importProcessDialogOpen.set(true);
  }

  protected closeImportProcessConfig(): void {
    if (this.importingProcessConfig()) {
      return;
    }

    this.importProcessDialogOpen.set(false);
    this.importProcessFile.set(null);
    this.importProcessFormErrors.set([]);
  }

  protected setImportProcessFile(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.importProcessFile.set(input.files?.item(0) ?? null);
    this.importProcessFormErrors.set([]);
  }

  protected importProcessConfig(): void {
    const projectId = this.projectId();
    const file = this.importProcessFile();

    if (!projectId) {
      return;
    }

    if (!file) {
      this.importProcessFormErrors.set(['Choose a process config file to import.']);
      return;
    }

    this.importingProcessConfig.set(true);
    this.api
      .importProcessConfig(projectId, file)
      .pipe(finalize(() => this.importingProcessConfig.set(false)))
      .subscribe({
        next: (processConfig) => {
          const importedType = this.matchingProcessType(processConfig.type);

          if (importedType) {
            this.selectedProcessType.set(importedType);
          }

          this.configPage.set(1);
          this.importProcessDialogOpen.set(false);
          this.importProcessFile.set(null);
          this.importProcessFormErrors.set([]);
          this.load(processConfig.id, this.selectedExecution()?.id);
        },
        error: () => {
          this.importProcessFormErrors.set([
            'Process config could not be imported.'
          ]);
        }
      });
  }

  protected exportProcessConfig(config: ProcessConfig): void {
    const projectId = this.projectId();

    if (!projectId || !config.id) {
      return;
    }

    this.exportingConfigId.set(config.id);
    this.api
      .exportProcessConfig(projectId, config.id)
      .pipe(finalize(() => this.exportingConfigId.set(null)))
      .subscribe({
        next: (blob) => {
          this.downloadBlob(blob, this.processExportFileName(config));
        },
        error: () => {
          this.notifications.error('Process config could not be exported.');
        }
      });
  }

  protected startAddAlgorithmStep(config: ProcessConfig, algorithmKey?: string): void {
    const projectId = this.projectId();
    const processId = config.id;
    const selectedAlgorithmKey =
      algorithmKey || this.selectedAlgorithmKey() || this.algorithmTypes()[0]?.key;
    const algorithmType = this.algorithmTypes().find(
      (entry) => entry.key === selectedAlgorithmKey
    );

    if (!projectId || !processId || !selectedAlgorithmKey) {
      return;
    }

    this.algorithmStepFormErrors.set([]);
    this.queryPreviewMap.set({});
    this.testingQueryKey.set(null);
    this.loadingAlgorithmStep.set(true);
    this.api
      .newAlgorithmConfig(projectId, processId, selectedAlgorithmKey)
      .pipe(finalize(() => this.loadingAlgorithmStep.set(false)))
      .subscribe({
        next: (algorithm) => {
          const defaultName =
            algorithm.name ?? algorithmType?.value ?? selectedAlgorithmKey;
          const defaultDescription = algorithm.description ?? defaultName;

          this.algorithmStepForm.set({
            algorithm: this.prepareAlgorithmForForm(
              {
                ...algorithm,
                algorithmKey: selectedAlgorithmKey,
                enabled: true,
                description: this.uniqueAlgorithmStepText(
                  config,
                  'description',
                  defaultDescription
                ),
                name: this.uniqueAlgorithmStepText(config, 'name', defaultName)
              },
              processId
            ),
            algorithmKey: selectedAlgorithmKey,
            mode: 'add',
            processConfig: config,
            validationMessages: []
          });
        },
        error: () => {
          this.notifications.error('Algorithm step template could not be loaded.');
        }
      });
  }

  protected startAddSelectedAlgorithmStep(config: ProcessConfig): void {
    this.startAddAlgorithmStep(config, this.selectedAlgorithmKey());
  }

  protected setSelectedAlgorithmKey(algorithmKey: string): void {
    this.selectedAlgorithmKey.set(algorithmKey);
  }

  protected startCloneAlgorithmStep(
    config: ProcessConfig,
    step: ProcessStep
  ): void {
    const projectId = this.projectId();
    const processId = config.id;

    if (!projectId || !processId || !step.id) {
      return;
    }

    this.algorithmStepFormErrors.set([]);
    this.queryPreviewMap.set({});
    this.testingQueryKey.set(null);
    this.loadingAlgorithmStep.set(true);
    this.api
      .getAlgorithmConfig(projectId, step.id)
      .pipe(finalize(() => this.loadingAlgorithmStep.set(false)))
      .subscribe({
        next: (algorithm) => {
          const algorithmKey = algorithm.algorithmKey ?? step.algorithmKey ?? '';
          const defaultName = algorithm.name ?? step.name ?? algorithmKey;
          const defaultDescription =
            algorithm.description ?? step.description ?? defaultName;

          this.algorithmStepForm.set({
            algorithm: this.prepareAlgorithmForForm(
              {
                ...algorithm,
                id: null,
                algorithmConfigId: null,
                algorithmKey,
                description: this.uniqueAlgorithmStepText(
                  config,
                  'description',
                  defaultDescription
                ),
                lastModified: null,
                name: this.uniqueAlgorithmStepText(config, 'name', defaultName)
              },
              processId
            ),
            algorithmKey,
            mode: 'add',
            processConfig: config,
            validationMessages: []
          });
        },
        error: () => {
          this.notifications.error('Algorithm step detail could not be loaded.');
        }
      });
  }

  protected startEditAlgorithmStep(
    config: ProcessConfig,
    step: ProcessStep
  ): void {
    const projectId = this.projectId();

    if (!projectId || !config.id || !step.id) {
      return;
    }

    this.algorithmStepFormErrors.set([]);
    this.queryPreviewMap.set({});
    this.testingQueryKey.set(null);
    this.loadingAlgorithmStep.set(true);
    this.api
      .getAlgorithmConfig(projectId, step.id)
      .pipe(finalize(() => this.loadingAlgorithmStep.set(false)))
      .subscribe({
        next: (algorithm) => {
          this.algorithmStepForm.set({
            algorithm: this.prepareAlgorithmForForm(algorithm, config.id as number),
            algorithmKey: algorithm.algorithmKey ?? '',
            mode: 'edit',
            processConfig: config,
            validationMessages: []
          });
        },
        error: () => {
          this.notifications.error('Algorithm step detail could not be loaded.');
        }
      });
  }

  protected closeAlgorithmStepForm(): void {
    if (this.savingAlgorithmStep()) {
      return;
    }

    this.algorithmStepForm.set(null);
    this.algorithmStepFormErrors.set([]);
    this.queryPreviewMap.set({});
    this.testingQueryKey.set(null);
  }

  protected setAlgorithmStepType(algorithmKey: string): void {
    const form = this.algorithmStepForm();
    const projectId = this.projectId();
    const processId = form?.processConfig.id;

    if (!form || form.mode !== 'add' || !projectId || !processId || !algorithmKey) {
      return;
    }

    const algorithmType = this.algorithmTypes().find(
      (entry) => entry.key === algorithmKey
    );

    this.loadingAlgorithmStep.set(true);
    this.api
      .newAlgorithmConfig(projectId, processId, algorithmKey)
      .pipe(finalize(() => this.loadingAlgorithmStep.set(false)))
      .subscribe({
        next: (algorithm) => {
          this.algorithmStepForm.set({
            ...form,
            algorithm: this.prepareAlgorithmForForm(
              {
                ...algorithm,
                algorithmKey,
                enabled: true,
                name: algorithm.name ?? algorithmType?.value ?? algorithmKey
              },
              processId
            ),
            algorithmKey,
            validationMessages: []
          });
          this.algorithmStepFormErrors.set([]);
          this.queryPreviewMap.set({});
          this.testingQueryKey.set(null);
        },
        error: () => {
          this.notifications.error('Algorithm step template could not be loaded.');
        }
      });
  }

  protected updateAlgorithmStepForm(
    field: keyof Pick<ProcessStep, 'description' | 'name'>,
    value: string
  ): void {
    this.algorithmStepForm.update((form) =>
      form
        ? {
            ...form,
            algorithm: {
              ...form.algorithm,
              [field]: value
            },
            validationMessages: []
          }
        : form
    );
  }

  protected updateAlgorithmStepEnabled(value: boolean): void {
    this.algorithmStepForm.update((form) =>
      form
        ? {
            ...form,
            algorithm: {
              ...form.algorithm,
              enabled: value
            },
            validationMessages: []
          }
        : form
    );
  }

  protected updateAlgorithmParameterValue(index: number, value: string): void {
    this.updateAlgorithmParameter(index, {
      value,
      values: []
    });
  }

  protected updateAlgorithmParameterBoolean(index: number, value: boolean): void {
    this.updateAlgorithmParameterValue(index, String(value));
  }

  protected updateAlgorithmParameterValues(index: number, values: string[]): void {
    this.updateAlgorithmParameter(index, {
      value: null,
      values
    });
  }

  protected selectAllAlgorithmParameterValues(
    index: number,
    parameter: AlgorithmParameter
  ): void {
    this.updateAlgorithmParameterValues(index, this.parameterOptions(parameter));
  }

  protected clearAlgorithmParameterValues(index: number): void {
    this.updateAlgorithmParameterValues(index, []);
  }

  protected toggleQueryPreview(index: number, parameter: AlgorithmParameter): void {
    const key = this.queryPreviewKey(parameter, index);

    this.queryPreviewMap.update((previewMap) => ({
      ...previewMap,
      [key]: !previewMap[key]
    }));
  }

  protected isQueryPreviewVisible(
    index: number,
    parameter: AlgorithmParameter
  ): boolean {
    return Boolean(this.queryPreviewMap()[this.queryPreviewKey(parameter, index)]);
  }

  protected isQueryParameter(parameter: AlgorithmParameter): boolean {
    const type = parameter.type ?? '';
    const name = parameter.name ?? '';

    return (
      type === 'QUERY_ID' ||
      type === 'QUERY_ID_PAIR' ||
      (type === 'TEXT' && name.includes('Query'))
    );
  }

  protected isQueryIdParameter(parameter: AlgorithmParameter): boolean {
    return parameter.type === 'QUERY_ID' || parameter.type === 'QUERY_ID_PAIR';
  }

  protected canTestQueryParameter(parameter: AlgorithmParameter): boolean {
    return parameter.type === 'QUERY_ID' || parameter.type === 'QUERY_ID_PAIR';
  }

  protected isTestingQueryParameter(
    index: number,
    parameter: AlgorithmParameter
  ): boolean {
    return this.testingQueryKey() === this.queryPreviewKey(parameter, index);
  }

  protected formattedQuery(value: boolean | string | null | undefined): string {
    const query = String(value ?? '').trim();

    if (!query) {
      return '';
    }

    return query
      .replace(/\s+/g, ' ')
      .replace(
        /\b(select|from|where|and|or|group by|order by|having|union)\b/gi,
        (keyword) => `\n${keyword.toUpperCase()}`
      )
      .replace(/^\n/, '')
      .trim();
  }

  protected testAlgorithmParameterQuery(
    index: number,
    parameter: AlgorithmParameter
  ): void {
    const form = this.algorithmStepForm();
    const projectId = this.projectId();
    const processId = form?.processConfig.id;
    const query = String(parameter.value ?? '').trim();

    if (!form || !projectId || !processId) {
      return;
    }

    if (!query) {
      this.algorithmStepFormErrors.set(['Query cannot be blank.']);
      return;
    }

    const queryContext = this.resolveQueryContext(form.algorithm, parameter, query);

    if (queryContext.errors.length) {
      this.algorithmStepFormErrors.set(queryContext.errors);
      return;
    }

    const key = this.queryPreviewKey(parameter, index);

    this.testingQueryKey.set(key);
    this.algorithmStepFormErrors.set([]);
    this.api
      .testProcessQuery(
        projectId,
        processId,
        queryContext.queryType,
        queryContext.queryStyle,
        query,
        queryContext.objectTypeName
      )
      .pipe(finalize(() => this.testingQueryKey.set(null)))
      .subscribe({
        next: (count) => {
          this.algorithmStepForm.update((current) =>
            current
              ? {
                  ...current,
                  validationMessages: [
                    `Query is properly formed. Returned ${count} result${count === 1 ? '' : 's'}.`
                  ]
                }
              : current
          );
        },
        error: (error: unknown) => {
          this.algorithmStepFormErrors.set([
            `Query is improperly formed: ${this.errorMessage(error)}`
          ]);
        }
      });
  }

  protected validateAlgorithmStep(): void {
    const form = this.algorithmStepForm();
    const projectId = this.projectId();
    const processId = form?.processConfig.id;

    if (!form || !projectId || !processId) {
      return;
    }

    const errors = this.validateAlgorithmStepForm(form);
    this.algorithmStepFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    this.loadingAlgorithmStep.set(true);
    this.api
      .validateAlgorithmConfig(
        projectId,
        processId,
        this.buildAlgorithmPayload(form.algorithm, processId)
      )
      .pipe(finalize(() => this.loadingAlgorithmStep.set(false)))
      .subscribe({
        next: () => {
          this.algorithmStepForm.update((current) =>
            current
              ? {
                  ...current,
                  validationMessages: [
                    'Algorithm configuration successfully validated.'
                  ]
                }
              : current
          );
          this.algorithmStepFormErrors.set([]);
        },
        error: (error: unknown) => {
          const message = this.errorMessage(error);

          this.algorithmStepFormErrors.set([
            message === 'Unknown error'
              ? 'Algorithm configuration could not be validated.'
              : `Algorithm configuration could not be validated: ${message}`
          ]);
        }
      });
  }

  protected saveAlgorithmStep(): void {
    const form = this.algorithmStepForm();
    const projectId = this.projectId();
    const processId = form?.processConfig.id;

    if (!form || !projectId || !processId) {
      return;
    }

    const errors = this.validateAlgorithmStepForm(form);
    this.algorithmStepFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    if (!this.algorithmStepValidated(form)) {
      this.algorithmStepFormErrors.set([
        'Validate the algorithm configuration before saving.'
      ]);
      return;
    }

    const payload = this.buildAlgorithmPayload(form.algorithm, processId);
    const request: Observable<ProcessStep | void> =
      form.mode === 'add'
        ? this.api.addAlgorithmConfig(projectId, processId, payload)
        : this.api.updateAlgorithmConfig(projectId, processId, payload);

    this.savingAlgorithmStep.set(true);
    request.pipe(finalize(() => this.savingAlgorithmStep.set(false))).subscribe({
      next: () => {
        this.algorithmStepForm.set(null);
        this.algorithmStepFormErrors.set([]);
        this.refreshProcessConfig(processId);
      },
      error: () => {
        this.notifications.error('Algorithm step could not be saved.');
      }
    });
  }

  protected toggleAlgorithmStep(config: ProcessConfig, step: ProcessStep): void {
    const projectId = this.projectId();

    if (!projectId || !config.id || !step.id) {
      return;
    }

    this.updatingStepId.set(step.id);
    this.api
      .getAlgorithmConfig(projectId, step.id)
      .pipe(
        switchMap((algorithm) =>
          this.api.updateAlgorithmConfig(
            projectId,
            config.id as number,
            this.buildAlgorithmPayload(
              {
                ...algorithm,
                enabled: !this.isStepEnabled(algorithm)
              },
              config.id as number
            )
          )
        ),
        finalize(() => this.updatingStepId.set(null))
      )
      .subscribe({
        next: () => {
          this.refreshProcessConfig(config.id as number);
        },
        error: () => {
          this.notifications.error('Algorithm step could not be updated.');
        }
      });
  }

  protected algorithmStepValidated(form: AlgorithmStepForm | null): boolean {
    return Boolean(
      form?.validationMessages.some((message) =>
        message.includes('successfully validated')
      )
    );
  }

  protected removeAlgorithmStep(config: ProcessConfig, step: ProcessStep): void {
    const projectId = this.projectId();

    if (!projectId || !config.id || !step.id) {
      return;
    }

    if (!window.confirm(`Remove algorithm step "${step.name || step.id}"?`)) {
      return;
    }

    this.deletingStepId.set(step.id);
    this.api
      .removeAlgorithmConfig(projectId, step.id)
      .pipe(finalize(() => this.deletingStepId.set(null)))
      .subscribe({
        next: () => {
          this.refreshProcessConfig(config.id as number);
        },
        error: () => {
          this.notifications.error('Algorithm step could not be removed.');
        }
      });
  }

  protected moveAlgorithmStep(
    config: ProcessConfig,
    step: ProcessStep,
    direction: -1 | 1
  ): void {
    const steps = this.processSteps(config);
    const index = steps.findIndex((entry) => entry.id === step.id);
    const nextIndex = index + direction;

    if (index === -1) {
      return;
    }

    this.reorderAlgorithmStep(config, index, nextIndex);
  }

  protected startAlgorithmStepDrag(
    event: DragEvent,
    step: ProcessStep,
    index: number
  ): void {
    if (
      !this.canManageProcesses() ||
      this.updatingStepId() !== null ||
      this.deletingStepId() === step.id
    ) {
      event.preventDefault();
      return;
    }

    this.draggingAlgorithmStepIndex.set(index);
    event.dataTransfer?.setData('text/plain', String(index));
    event.dataTransfer?.setDragImage(event.currentTarget as Element, 0, 0);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  protected dragOverAlgorithmStep(event: DragEvent, index: number): void {
    const sourceIndex = this.draggingAlgorithmStepIndex();

    if (
      sourceIndex !== null &&
      sourceIndex !== index &&
      this.canManageProcesses() &&
      this.updatingStepId() === null
    ) {
      event.preventDefault();
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move';
      }
    }
  }

  protected dropAlgorithmStep(
    event: DragEvent,
    config: ProcessConfig,
    targetIndex: number
  ): void {
    const transferredIndex = event.dataTransfer?.getData('text/plain');
    const rawSourceIndex =
      this.draggingAlgorithmStepIndex() ??
      (transferredIndex ? Number(transferredIndex) : Number.NaN);
    const sourceIndex = Number(rawSourceIndex);
    const viewportLeft = window.scrollX;
    const viewportTop = window.scrollY;

    event.preventDefault();
    event.stopPropagation();
    this.draggingAlgorithmStepIndex.set(null);

    if (
      !this.canManageProcesses() ||
      this.updatingStepId() !== null ||
      !Number.isInteger(sourceIndex) ||
      sourceIndex === targetIndex
    ) {
      return;
    }

    this.reorderAlgorithmStep(config, sourceIndex, targetIndex);
    this.restoreAlgorithmStepDropViewport(viewportLeft, viewportTop);
  }

  protected clearAlgorithmStepDrag(): void {
    this.draggingAlgorithmStepIndex.set(null);
  }

  private reorderAlgorithmStep(
    config: ProcessConfig,
    index: number,
    targetIndex: number
  ): void {
    const projectId = this.projectId();
    const processId = config.id;
    const steps = this.processSteps(config);

    if (
      !projectId ||
      !processId ||
      index < 0 ||
      targetIndex < 0 ||
      index >= steps.length ||
      targetIndex >= steps.length
    ) {
      return;
    }

    const reorderedDisplaySteps = [...steps];
    const [displayStep] = reorderedDisplaySteps.splice(index, 1);
    reorderedDisplaySteps.splice(targetIndex, 0, displayStep);
    const nextSteps = steps.map((entry) =>
      this.buildAlgorithmPayload(entry, processId)
    );
    const [movedStep] = nextSteps.splice(index, 1);
    nextSteps.splice(targetIndex, 0, movedStep);
    const optimisticConfig = {
      ...config,
      steps: reorderedDisplaySteps
    };

    this.selectedConfig.set(optimisticConfig);
    this.replaceConfig(optimisticConfig);
    this.updatingStepId.set(displayStep.id ?? null);
    this.api
      .updateProcessConfig(projectId, {
        ...config,
        steps: nextSteps
      })
      .pipe(finalize(() => this.updatingStepId.set(null)))
      .subscribe({
        next: () => {
          this.refreshProcessConfig(processId);
        },
        error: () => {
          this.notifications.error('Algorithm step could not be moved.');
          this.refreshProcessConfig(processId);
        }
      });
  }

  private restoreAlgorithmStepDropViewport(left: number, top: number): void {
    requestAnimationFrame(() => window.scrollTo(left, top));
  }

  protected selectConfig(config: ProcessConfig | null): void {
    this.selectedConfig.set(config);
    this.selectedStepId.set(null);

    const projectId = this.projectId();
    if (!projectId || !config?.id) {
      this.loadingConfigDetail.set(false);
      return;
    }

    this.loadingConfigDetail.set(true);
    this.api
      .getProcessConfig(projectId, config.id)
      .pipe(finalize(() => this.loadingConfigDetail.set(false)))
      .subscribe({
        next: (detail) => {
          if (this.selectedConfig()?.id !== detail?.id) {
            return;
          }

          this.selectedConfig.set(detail);
          this.replaceConfig(detail);
          this.loadAlgorithmTypesForConfig(detail);
        },
        error: () => {
          this.notifications.error('Process config detail could not be loaded.');
        }
      });
  }

  protected selectExecution(execution: ProcessExecution | null): void {
    this.stopExecutionFeedbackPolling();
    this.resetExecutionFeedback();
    this.selectedExecution.set(execution);
    this.selectedStepId.set(null);
    this.selectedConfigForExecutionStepId.set(null);
    this.selectedConfigForExecution.set(null);

    const projectId = this.projectId();
    if (!projectId || !execution?.id) {
      this.loadingExecutionDetail.set(false);
      return;
    }

    this.loadConfigForExecution(execution);
    this.refreshSelectedExecutionFeedback(false);
  }

  protected runProcessOperation(
    operation: ProcessOperation,
    process: ProcessConfig | ProcessExecution
  ): void {
    const projectId = this.projectId();

    if (
      !projectId ||
      !process.id ||
      !this.canManageProcesses() ||
      this.processOperationInFlight()
    ) {
      return;
    }

    if (this.processOperationNeedsMaintenanceWarning(operation)) {
      this.checkMaintenanceWindowBeforeOperation(operation, process);
      return;
    }

    this.submitProcessOperationWithConfirmation(operation, process);
  }

  protected processOperationInFlight(): boolean {
    return (
      this.runningOperation() !== null ||
      this.checkingMaintenanceWindowOperation() !== null
    );
  }

  protected closeMaintenanceWindowWarning(): void {
    if (this.runningOperation() !== null) {
      return;
    }

    this.maintenanceWindowWarning.set(null);
  }

  protected continueMaintenanceWindowOperation(): void {
    const warning = this.maintenanceWindowWarning();

    if (!warning || this.runningOperation() !== null) {
      return;
    }

    this.maintenanceWindowWarning.set(null);
    this.submitProcessOperation(warning.operation, warning.process);
  }

  protected maintenanceWindowRange(window: MaintenanceWindow): string {
    return formatMaintenanceWindowRange(window);
  }

  protected maintenanceWarningProcessName(
    process: ProcessConfig | ProcessExecution
  ): string | number {
    return process.name || process.id || 'process';
  }

  protected maintenanceWarningOperationLabel(
    operation: ProcessOperation
  ): string {
    return this.processOperationLabel(operation).toLocaleLowerCase();
  }

  private submitProcessOperationWithConfirmation(
    operation: ProcessOperation,
    process: ProcessConfig | ProcessExecution
  ): void {
    if (!this.confirmProcessOperation(operation, process)) {
      return;
    }

    this.submitProcessOperation(operation, process);
  }

  private submitProcessOperation(
    operation: ProcessOperation,
    process: ProcessConfig | ProcessExecution
  ): void {
    const projectId = this.projectId();

    if (!projectId || !process.id || this.runningOperation() !== null) {
      return;
    }

    const request = this.processOperationRequest(projectId, operation, process.id);

    this.runningOperation.set(operation);
    request.pipe(finalize(() => this.runningOperation.set(null))).subscribe({
      next: (processExecutionId) => {
        if (operation === 'prepare') {
          this.selectedMode.set('Execution');
          this.saveProcessPreference('processMode', 'Execution');
        }
        this.load(this.selectedConfig()?.id, processExecutionId);
      },
      error: () => {
        this.notifications.error('Process operation could not be completed.');
      }
    });
  }

  private checkMaintenanceWindowBeforeOperation(
    operation: ProcessOperation,
    process: ProcessConfig | ProcessExecution
  ): void {
    this.checkingMaintenanceWindowOperation.set(operation);
    this.api
      .getNextMaintenanceWindow()
      .pipe(finalize(() => this.checkingMaintenanceWindowOperation.set(null)))
      .subscribe({
        next: (window) => {
          if (this.shouldWarnForMaintenanceWindow(window)) {
            this.maintenanceWindowWarning.set({ operation, process, window });
            return;
          }

          this.submitProcessOperationWithConfirmation(operation, process);
        },
        error: () => {
          this.notifications.error('Maintenance windows could not be checked.');
          this.submitProcessOperationWithConfirmation(operation, process);
        }
      });
  }

  private shouldWarnForMaintenanceWindow(
    window: MaintenanceWindow | null
  ): window is MaintenanceWindow {
    const startDate = dateFromLegacyValue(window?.startDate);
    const endDate = dateFromLegacyValue(window?.endDate);

    if (!startDate || !endDate) {
      return false;
    }

    const now = Date.now();
    const fourDaysFromNow = now + 4 * 24 * 60 * 60 * 1000;

    return endDate.getTime() >= now && startDate.getTime() <= fourDaysFromNow;
  }

  protected canPrepareProcess(config: ProcessConfig): boolean {
    return this.canManageProcesses() && Boolean(config.id);
  }

  protected canExecuteProcess(execution: ProcessExecution): boolean {
    return this.canManageProcesses() && Boolean(execution.id) && !execution.startDate;
  }

  protected canCancelProcess(execution: ProcessExecution): boolean {
    return (
      this.canManageProcesses() &&
      Boolean(execution.id) &&
      this.isProcessRunning(execution)
    );
  }

  protected canRestartProcess(execution: ProcessExecution): boolean {
    return (
      this.canManageProcesses() &&
      Boolean(execution.id) &&
      Boolean(execution.startDate) &&
      Boolean(execution.stopDate || execution.failDate)
    );
  }

  protected canStepProcess(execution: ProcessExecution): boolean {
    return (
      this.canManageProcesses() &&
      Boolean(execution.id) &&
      !execution.finishDate &&
      !this.isProcessRunning(execution)
    );
  }

  protected canUnstepProcess(execution: ProcessExecution): boolean {
    return (
      this.canManageProcesses() &&
      Boolean(execution.id) &&
      this.stepCount(execution) > 0 &&
      !this.isProcessRunning(execution)
    );
  }

  protected executionStatus(execution: ProcessExecution | null): string {
    if (!execution) {
      return 'n/a';
    }

    let status = 'READY';

    if (
      execution.startDate &&
      !execution.stopDate &&
      !execution.failDate &&
      !execution.finishDate
    ) {
      status = 'RUNNING';
    } else if (execution.stopDate) {
      status = 'STOPPED';
    } else if (execution.failDate && execution.finishDate) {
      status = 'CANCELLED';
    } else if (!execution.failDate && execution.finishDate) {
      status = 'COMPLETE';
    } else if (execution.failDate && !execution.finishDate) {
      status = 'FAILED';
    }

    return execution.warning ? `${status}, WARNING` : status;
  }

  protected statusClass(execution: ProcessConfig | ProcessExecution | null): string {
    const status = this.executionStatus(execution as ProcessExecution | null);

    if (status.includes('FAILED') || status.includes('CANCELLED')) {
      return 'failed';
    }

    if (status.includes('RUNNING')) {
      return 'running';
    }

    return '';
  }

  protected processState(process: ProcessConfig | ProcessExecution): string {
    if (this.selectedMode() === 'Config') {
      return 'CONFIG';
    }

    return this.executionStatus(process as ProcessExecution);
  }

  protected selectedProcess(): ProcessConfig | ProcessExecution | null {
    return this.selectedMode() === 'Config'
      ? this.selectedConfig()
      : this.selectedExecution();
  }

  protected selectStep(step: ProcessStep): void {
    this.selectedStepId.set(step.id ?? null);
  }

  protected selectConfigForExecutionStep(step: ProcessStep): void {
    this.selectedConfigForExecutionStepId.set(step.id ?? null);
  }

  protected isStepSelected(step: ProcessStep): boolean {
    return Boolean(step.id && this.selectedStepId() === step.id);
  }

  protected isConfigForExecutionStepSelected(step: ProcessStep): boolean {
    return Boolean(
      step.id && this.selectedConfigForExecutionStepId() === step.id
    );
  }

  protected unexecutedSteps(): ProcessStep[] {
    const config = this.selectedConfigForExecution();
    const execution = this.selectedExecution();
    const executionSteps = this.processSteps(execution);

    return this.enabledProcessSteps(config).filter(
      (configStep) =>
        !executionSteps.some(
          (executionStep) =>
            configStep.name === executionStep.name &&
            configStep.description === executionStep.description
        )
    );
  }

  protected unexecutedStepStartIndex(): number {
    return this.processSteps(this.selectedExecution()).length;
  }

  protected processSteps(process: ProcessConfig | ProcessExecution | null): ProcessStep[] {
    return process?.steps ?? [];
  }

  protected enabledProcessSteps(
    process: ProcessConfig | ProcessExecution | null
  ): ProcessStep[] {
    return this.processSteps(process).filter((step) => this.isStepEnabled(step));
  }

  protected stepParameters(step: ProcessStep): AlgorithmParameter[] {
    return step.parameters ?? [];
  }

  protected isStepEnabled(step: ProcessStep): boolean {
    return step.enabled !== false && step.enabled !== 0;
  }

  protected isFirstStep(config: ProcessConfig, step: ProcessStep): boolean {
    return this.processSteps(config)[0]?.id === step.id;
  }

  protected isLastStep(config: ProcessConfig, step: ProcessStep): boolean {
    const steps = this.processSteps(config);
    return steps[steps.length - 1]?.id === step.id;
  }

  protected uniqueAlgorithmStepText(
    config: ProcessConfig,
    field: 'description' | 'name',
    value: string | null | undefined
  ): string {
    const base = String(value ?? '').trim() || 'Algorithm step';
    const existingValues = new Set(
      this.processSteps(config)
        .map((step) => String(step[field] ?? '').trim())
        .filter(Boolean)
    );

    if (!existingValues.has(base)) {
      return base;
    }

    let suffix = 2;
    let candidate = `${base} ${suffix}`;

    while (existingValues.has(candidate)) {
      suffix += 1;
      candidate = `${base} ${suffix}`;
    }

    return candidate;
  }

  protected algorithmTypeLabel(algorithmKey: string | null | undefined): string {
    if (!algorithmKey) {
      return 'n/a';
    }

    return (
      this.algorithmTypes().find((entry) => entry.key === algorithmKey)?.value ??
      algorithmKey
    );
  }

  protected parameterOptions(parameter: AlgorithmParameter): string[] {
    return parameter.possibleValues ?? [];
  }

  protected isTextParameter(parameter: AlgorithmParameter): boolean {
    return ['QUERY_ID', 'QUERY_ID_PAIR', 'TEXT'].includes(parameter.type ?? '');
  }

  protected isBooleanParameter(parameter: AlgorithmParameter): boolean {
    return parameter.type === 'BOOLEAN';
  }

  protected isEnumParameter(parameter: AlgorithmParameter): boolean {
    return parameter.type === 'ENUM';
  }

  protected isIntegerParameter(parameter: AlgorithmParameter): boolean {
    return parameter.type === 'INTEGER';
  }

  protected isMultiParameter(parameter: AlgorithmParameter): boolean {
    return parameter.type === 'MULTI';
  }

  protected parameterBooleanValue(parameter: AlgorithmParameter): boolean {
    return parameter.value === true || parameter.value === 'true';
  }

  protected stepStatus(step: ProcessStep): string {
    let status = 'READY';

    if (step.failDate && step.finishDate) {
      status = 'CANCELLED';
    } else if (step.failDate) {
      status = 'FAILED';
    } else if (step.finishDate) {
      status = 'COMPLETE';
    } else if (step.startDate) {
      status = 'RUNNING';
    }

    return step.warning ? `${status}, WARNING` : status;
  }

  protected stepStatusClass(step: ProcessStep): string {
    const status = this.stepStatus(step);

    if (status.includes('FAILED') || status.includes('CANCELLED')) {
      return 'failed';
    }

    if (status.includes('RUNNING')) {
      return 'running';
    }

    return '';
  }

  protected parameterValue(parameter: AlgorithmParameter): string {
    if (parameter.value) {
      return String(parameter.value);
    }

    if (parameter.values?.length) {
      return parameter.values.join(', ');
    }

    return 'n/a';
  }

  protected executionInfoEntries(
    execution: ProcessExecution
  ): Array<{ key: string; value: string }> {
    return Object.entries(execution.executionInfo ?? {})
      .map(([key, value]) => ({
        key,
        value
      }))
      .sort((first, second) => first.key.localeCompare(second.key));
  }

  protected currentExecutionStep(): ProcessStep | null {
    return this.activeOrLastExecutionStep(this.selectedExecution());
  }

  protected progressPercent(progress: number | null): number {
    if (progress === null || Number.isNaN(progress)) {
      return 0;
    }

    return Math.min(100, Math.max(0, progress));
  }

  protected progressLabel(progress: number | null): string {
    if (progress === null || Number.isNaN(progress)) {
      return 'n/a';
    }

    return `${this.progressPercent(progress)} / 100`;
  }

  protected stepProgressValue(step: ProcessStep): number | null {
    if (step.id && step.id === this.activeStepId()) {
      return this.stepProgress();
    }

    if (step.finishDate && !step.failDate) {
      return 100;
    }

    return null;
  }

  protected displayDate(value: string | number | null | undefined): string {
    if (!value) {
      return 'n/a';
    }
    return dateFromLegacyValue(value)
      ? formatEasternDateTime(value)
      : String(value);
  }

  protected stepCount(process: ProcessConfig | ProcessExecution | null): number {
    return process?.steps?.length ?? 0;
  }

  private updateAlgorithmParameter(
    index: number,
    patch: Partial<AlgorithmParameter>
  ): void {
    this.algorithmStepForm.update((form) => {
      if (!form) {
        return form;
      }

      return {
        ...form,
        algorithm: {
          ...form.algorithm,
          parameters: this.stepParameters(form.algorithm).map((parameter, i) =>
            i === index
              ? {
                  ...parameter,
                  ...patch
                }
              : parameter
          )
        },
        validationMessages: []
      };
    });
  }

  private pageCount(totalCount: number, pageSize: number): number {
    return Math.max(1, Math.ceil(totalCount / pageSize));
  }

  private queryPreviewKey(parameter: AlgorithmParameter, index: number): string {
    return parameter.fieldName ?? parameter.name ?? String(index);
  }

  private resolveQueryContext(
    algorithm: ProcessStep,
    parameter: AlgorithmParameter,
    query: string
  ): QueryTestContext {
    const errors: string[] = [];
    const fieldName = parameter.fieldName ?? '';
    const queryTypeParameter = this.stepParameters(algorithm).find(
      (entry) => entry.fieldName === `${fieldName}Type`
    );
    const objectTypeParameter = this.stepParameters(algorithm).find(
      (entry) => entry.fieldName === 'objectType'
    );
    const queryType = queryTypeParameter
      ? String(queryTypeParameter.value ?? '').trim()
      : this.inferQueryType(query);
    const objectTypeName = objectTypeParameter
      ? String(objectTypeParameter.value ?? '').trim()
      : '';

    if (!queryType) {
      errors.push(
        `${queryTypeParameter?.name || queryTypeParameter?.fieldName || 'Query type'} needs to be set.`
      );
    }

    if (objectTypeParameter && !objectTypeName) {
      errors.push(`${objectTypeParameter.name || objectTypeParameter.fieldName} needs to be set.`);
    }

    return {
      errors,
      objectTypeName: objectTypeName || null,
      queryStyle: parameter.type === 'QUERY_ID_PAIR' ? 'ID_PAIR' : 'ID',
      queryType
    };
  }

  private inferQueryType(query: string): string {
    if (/select.*from +[^ ]+jpa/i.test(query)) {
      return 'JPQL';
    }

    if (/select/i.test(query)) {
      return 'SQL';
    }

    return 'LUCENE';
  }

  private errorMessage(error: unknown): string {
    const response = error as { error?: unknown; message?: unknown };

    if (typeof response.error === 'string' && response.error.trim()) {
      return response.error;
    }

    if (response.error && typeof response.error === 'object') {
      const body = response.error as {
        detail?: unknown;
        error?: unknown;
        message?: unknown;
      };

      for (const value of [body.message, body.detail, body.error]) {
        if (typeof value === 'string' && value.trim()) {
          return value;
        }
      }
    }

    if (typeof response.message === 'string' && response.message.trim()) {
      return response.message;
    }

    return 'Unknown error';
  }

  private startRunningStateRefresh(): void {
    this.runningStateSubscription.add(
      timer(5000, 5000).subscribe(() => this.refreshRunningExecutions())
    );
  }

  private refreshRunningExecutions(): void {
    const projectId = this.projectId();

    if (!projectId) {
      return;
    }

    const hadRunningExecutions = this.runningExecutions().length > 0;

    this.api.getExecutingProcesses(projectId).subscribe({
      next: (running) => {
        this.runningExecutions.set(running.items);

        if (hadRunningExecutions && !running.items.length) {
          this.load(this.selectedConfig()?.id, this.selectedExecution()?.id);
        }
      }
    });
  }

  private startExecutionFeedbackPolling(execution: ProcessExecution): void {
    if (!this.isProcessRunning(execution) || this.executionFeedbackSubscription) {
      return;
    }

    this.executionFeedbackSubscription = timer(2000, 2000).subscribe(() =>
      this.refreshSelectedExecutionFeedback(true)
    );
  }

  private stopExecutionFeedbackPolling(): void {
    this.executionFeedbackSubscription?.unsubscribe();
    this.executionFeedbackSubscription = null;
  }

  private resetExecutionFeedback(): void {
    this.activeStepId.set(null);
    this.feedbackUpdatedAt.set(null);
    this.processProgress.set(null);
    this.stepProgress.set(null);
  }

  protected refreshSelectedExecutionFeedback(polling: boolean): void {
    const projectId = this.projectId();
    const executionId = this.selectedExecution()?.id;

    if (!projectId || !executionId) {
      return;
    }

    if (polling && this.loadingExecutionFeedback()) {
      return;
    }

    if (!polling) {
      this.loadingExecutionDetail.set(true);
    }

    this.loadingExecutionFeedback.set(true);
    this.api
      .getProcessExecution(projectId, executionId)
      .pipe(
        switchMap((detail) => {
          const currentStep = this.activeOrLastExecutionStep(detail);
          const currentStepId = currentStep?.id ?? null;

          return forkJoin({
            detail: of(detail),
            processProgress: this.api
              .getProcessProgress(projectId, executionId)
              .pipe(catchError(() => of(null))),
            stepProgress: currentStepId
              ? this.api
                  .getAlgorithmProgress(projectId, currentStepId)
                  .pipe(catchError(() => of(null)))
              : of(null)
          });
        }),
        finalize(() => {
          this.loadingExecutionFeedback.set(false);
          if (!polling) {
            this.loadingExecutionDetail.set(false);
          }
        })
      )
      .subscribe({
        next: ({ detail, processProgress, stepProgress }) => {
          if (this.selectedExecution()?.id !== detail.id) {
            return;
          }

          const currentStep = this.activeOrLastExecutionStep(detail);

          this.selectedExecution.set(detail);
          this.replaceExecution(detail);
          this.loadConfigForExecution(detail);
          this.activeStepId.set(currentStep?.id ?? null);
          this.processProgress.set(this.normalizedProgress(processProgress, detail));
          this.stepProgress.set(
            currentStep ? this.normalizedProgress(stepProgress, currentStep) : null
          );
          this.feedbackUpdatedAt.set(Date.now());

          if (this.isProcessRunning(detail)) {
            this.startExecutionFeedbackPolling(detail);
          } else {
            this.stopExecutionFeedbackPolling();
          }
        },
        error: () => {
          if (!polling) {
            this.notifications.error('Process execution detail could not be loaded.');
          }
        }
      });
  }

  protected openProcessLog(process: ProcessConfig | ProcessExecution): void {
    const execution = process as ProcessExecution;

    if (!execution.id) {
      return;
    }

    this.processLogState.set({ filter: '', loading: true, log: '', execution });
    this.fetchProcessLog(execution, '');
  }

  protected closeProcessLog(): void {
    this.processLogState.set(null);
  }

  protected updateProcessLogFilter(value: string): void {
    this.processLogState.update((state) => (state ? { ...state, filter: value } : state));
  }

  protected searchProcessLog(): void {
    const state = this.processLogState();

    if (state) {
      this.fetchProcessLog(state.execution, state.filter);
    }
  }

  protected clearProcessLogFilter(): void {
    const state = this.processLogState();

    if (!state) {
      return;
    }

    this.processLogState.set({ ...state, filter: '' });
    this.fetchProcessLog(state.execution, '');
  }

  private fetchProcessLog(execution: ProcessExecution, filter: string): void {
    const projectId = this.projectId();
    const executionId = execution.id;

    if (!projectId || !executionId) {
      return;
    }

    this.processLogState.update((state) => (state ? { ...state, loading: true } : state));
    this.api.getProcessLog(projectId, executionId, filter).subscribe({
      next: (log) => {
        this.processLogState.update((state) =>
          state?.execution.id === executionId ? { ...state, loading: false, log } : state
        );
      },
      error: () => {
        this.notifications.error('Process log could not be loaded.');
        this.processLogState.update((state) =>
          state?.execution.id === executionId ? { ...state, loading: false } : state
        );
      }
    });
  }

  protected openStepLog(step: ProcessStep): void {
    const stepId = step.id;

    if (!stepId) {
      return;
    }

    this.selectedStepId.set(stepId);
    this.stepLogState.set({
      filter: '',
      loading: true,
      log: '',
      progress: null,
      step
    });
    this.fetchStepLog(step, '');
  }

  protected closeStepLog(): void {
    this.stepLogState.set(null);
  }

  protected updateStepLogFilter(value: string): void {
    this.stepLogState.update((state) => (state ? { ...state, filter: value } : state));
  }

  protected searchStepLog(): void {
    const state = this.stepLogState();

    if (state) {
      this.fetchStepLog(state.step, state.filter);
    }
  }

  protected clearStepLogFilter(): void {
    const state = this.stepLogState();

    if (!state) {
      return;
    }

    this.stepLogState.set({ ...state, filter: '' });
    this.fetchStepLog(state.step, '');
  }

  private fetchStepLog(step: ProcessStep, filter: string): void {
    const projectId = this.projectId();
    const stepId = step.id;

    if (!projectId || !stepId) {
      return;
    }

    this.stepLogState.update((state) => (state ? { ...state, loading: true } : state));
    forkJoin({
      stepLog: this.api.getAlgorithmLog(projectId, stepId, filter),
      stepProgress: this.api
        .getAlgorithmProgress(projectId, stepId)
        .pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ stepLog, stepProgress }) => {
        const progress = this.normalizedProgress(stepProgress, step);

        this.stepLogState.update((state) =>
          state?.step.id === stepId
            ? { ...state, loading: false, log: stepLog, progress }
            : state
        );

        if (this.activeStepId() === stepId) {
          this.stepProgress.set(progress);
        }
      },
      error: () => {
        this.notifications.error('Step log could not be loaded.');
        this.stepLogState.update((state) =>
          state?.step.id === stepId ? { ...state, loading: false } : state
        );
      }
    });
  }

  private loadConfigForExecution(execution: ProcessExecution): void {
    const projectId = this.projectId();
    const processConfigId = execution.processConfigId;

    if (!projectId || !processConfigId) {
      this.selectedConfigForExecution.set(null);
      return;
    }

    if (this.selectedConfigForExecution()?.id === processConfigId) {
      return;
    }

    this.api.getProcessConfig(projectId, processConfigId).subscribe({
      next: (config) => {
        if (this.selectedExecution()?.processConfigId !== processConfigId) {
          return;
        }

        this.selectedConfigForExecution.set(config);
      },
      error: () => this.selectedConfigForExecution.set(null)
    });
  }

  private validateAlgorithmStepForm(form: AlgorithmStepForm): string[] {
    const errors: string[] = [];

    if (!form.algorithmKey.trim()) {
      errors.push('Algorithm type cannot be blank.');
    }

    if (!form.algorithm.name?.trim()) {
      errors.push('Name cannot be blank.');
    }

    return errors;
  }

  private buildAlgorithmPayload(
    algorithm: ProcessStep,
    processId: number
  ): ProcessStep {
    const parameters = this.stepParameters(algorithm).map((parameter) => ({
      ...parameter,
      values: parameter.values ? [...parameter.values] : []
    }));
    const properties: Record<string, string> = {};

    for (const parameter of parameters) {
      if (!parameter.fieldName) {
        continue;
      }

      if (parameter.values?.length) {
        properties[parameter.fieldName] = parameter.values.join(',');
      } else if (
        parameter.value !== null &&
        parameter.value !== undefined &&
        String(parameter.value) !== ''
      ) {
        properties[parameter.fieldName] = String(parameter.value);
      }
    }

    return {
      ...algorithm,
      algorithmKey: algorithm.algorithmKey ?? '',
      description: algorithm.description ?? '',
      enabled: this.isStepEnabled(algorithm),
      name: algorithm.name ?? '',
      parameters,
      process: {
        id: processId
      },
      processId,
      properties
    };
  }

  private prepareAlgorithmForForm(
    algorithm: ProcessStep,
    processId: number
  ): ProcessStep {
    return {
      ...algorithm,
      enabled: algorithm.enabled ?? true,
      parameters: this.stepParameters(algorithm).map((parameter) => ({
        ...parameter,
        values: parameter.values ? [...parameter.values] : []
      })),
      process: {
        id: processId
      },
      processId,
      properties: {
        ...(algorithm.properties ?? {})
      }
    };
  }

  private refreshProcessConfig(id: number): void {
    const projectId = this.projectId();

    if (!projectId) {
      return;
    }

    this.loadingConfigDetail.set(true);
    this.api
      .getProcessConfig(projectId, id)
      .pipe(finalize(() => this.loadingConfigDetail.set(false)))
      .subscribe({
        next: (detail) => {
          this.selectedConfig.set(detail);
          this.replaceConfig(detail);
          this.loadAlgorithmTypesForConfig(detail);
        },
        error: () => this.load(id, this.selectedExecution()?.id)
      });
  }

  private loadAlgorithmTypesForConfig(config: ProcessConfig): void {
    const projectId = this.projectId();
    const type = config.type;

    if (!projectId || !type) {
      this.algorithmTypes.set([]);
      return;
    }

    this.loadingAlgorithmTypes.set(true);
    this.api
      .getAlgorithmsForType(projectId, type)
      .pipe(finalize(() => this.loadingAlgorithmTypes.set(false)))
      .subscribe({
        next: (algorithmTypes) =>
          {
            const sortedTypes = [...algorithmTypes].sort((a, b) =>
              (a.value ?? a.key ?? '').localeCompare(b.value ?? b.key ?? '')
            );
            const currentKey = this.selectedAlgorithmKey();

            this.algorithmTypes.set(sortedTypes);
            this.selectedAlgorithmKey.set(
              sortedTypes.some((entry) => entry.key === currentKey)
                ? currentKey
                : sortedTypes[0]?.key ?? ''
            );
          },
        error: () => {
          this.algorithmTypes.set([]);
          this.notifications.error('Algorithm types could not be loaded.');
        }
      });
  }

  private validateProcessConfigForm(form: ProcessConfigForm): string[] {
    const errors: string[] = [];

    if (!form.name.trim()) {
      errors.push('Name cannot be blank.');
    }

    if (!form.type.trim()) {
      errors.push('Type cannot be blank.');
    }

    if (form.feedbackEmail.trim() && !this.isEmailList(form.feedbackEmail)) {
      errors.push(`Invalid feedback email: ${form.feedbackEmail}`);
    }

    return errors;
  }

  private buildProcessConfigPayload(form: ProcessConfigForm): ProcessConfig {
    return {
      ...(form.processConfig ?? {}),
      description: form.description.trim() || null,
      feedbackEmail: form.feedbackEmail.trim() || null,
      inputPath: form.inputPath.trim() || null,
      logPath: form.logPath.trim() || null,
      name: form.name.trim(),
      terminology: form.terminology.trim() || null,
      type: form.type.trim(),
      version: form.version.trim() || null
    };
  }

  private isEmailList(value: string): boolean {
    return value
      .split(';')
      .map((email) => email.trim())
      .every((email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email));
  }

  private defaultProcessType(): string {
    return this.selectedProcessType();
  }

  private matchingProcessType(value: string | null | undefined): ProcessType | null {
    const normalizedValue = value?.trim().toLocaleLowerCase();

    return (
      this.processTypes.find(
        (processType) => processType.toLocaleLowerCase() === normalizedValue
      ) ?? null
    );
  }

  private matchingProcessMode(value: string | null | undefined): ProcessMode | null {
    const normalizedValue = value?.trim().toLocaleLowerCase();

    return (
      this.processModes.find(
        (processMode) => processMode.toLocaleLowerCase() === normalizedValue
      ) ?? null
    );
  }

  private processExportFileName(config: ProcessConfig): string {
    const id = config.id ?? 'config';
    const baseName =
      config.name
        ?.trim()
        .toLocaleLowerCase()
        .replace(/[^a-z0-9._-]+/g, '-')
        .replace(/^-+|-+$/g, '') || `process-${id}`;

    return `${baseName}.${id}.txt`;
  }

  private downloadBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = fileName;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  private loadTerminologies(): void {
    this.api.getCurrentTerminologies().subscribe({
      next: (terminologies) =>
        this.terminologies.set(
          terminologies.filter((terminology) => terminology.current !== false)
        ),
      error: () => {
        this.notifications.error('Current terminologies could not be loaded.');
      }
    });
  }

  private initializeProcessPreferences(): void {
    const preferences = this.auth.currentUser().userPreferences;
    const properties = preferences?.properties ?? {};
    const savedMode = this.matchingProcessMode(
      String(properties['processMode'] ?? '')
    );
    const savedType = this.matchingProcessType(
      String(properties['processType'] ?? '')
    );

    if (savedMode) {
      this.selectedMode.set(savedMode);
    }

    if (savedType) {
      this.selectedProcessType.set(savedType);
    }
  }

  private saveProcessPreference(
    key: 'processMode' | 'processType',
    value: string
  ): void {
    const user = this.auth.currentUser();
    const preferences = user.userPreferences ?? { properties: {} };
    const nextPreferences = {
      ...preferences,
      properties: {
        ...(preferences.properties ?? {}),
        [key]: value
      }
    };

    this.api.updateUserPreferences(nextPreferences).subscribe({
      next: (saved) =>
        this.auth.updateCurrentUserPreferences(saved ?? nextPreferences),
      error: () => {}
    });
  }

  private confirmProcessOperation(
    operation: ProcessOperation,
    process: ProcessConfig | ProcessExecution
  ): boolean {
    const processName = process.name || process.id;

    if (operation === 'execute') {
      return window.confirm(`Execute process "${processName}"?`);
    }

    if (operation === 'restart') {
      return window.confirm(`Restart process "${processName}"?`);
    }

    if (operation === 'step') {
      return window.confirm(`Step process "${processName}" forward?`);
    }

    if (operation === 'unstep') {
      return window.confirm(`Step process "${processName}" back?`);
    }

    return true;
  }

  private processOperationNeedsMaintenanceWarning(
    operation: ProcessOperation
  ): boolean {
    return operation !== 'cancel' && operation !== 'unstep';
  }

  private processOperationLabel(operation: ProcessOperation): string {
    if (operation === 'execute') {
      return 'Execute';
    }

    if (operation === 'prepare') {
      return 'Prepare';
    }

    if (operation === 'restart') {
      return 'Restart';
    }

    if (operation === 'step') {
      return 'Step forward';
    }

    if (operation === 'unstep') {
      return 'Step back';
    }

    return 'Cancel';
  }

  private processOperationRequest(
    projectId: number,
    operation: ProcessOperation,
    processId: number
  ): Observable<number> {
    if (operation === 'prepare') {
      return this.api.prepareProcess(projectId, processId);
    }

    if (operation === 'execute') {
      return this.api.executeProcess(projectId, processId);
    }

    if (operation === 'restart') {
      return this.api.restartProcess(projectId, processId);
    }

    if (operation === 'step') {
      return this.api.stepProcess(projectId, processId, 1);
    }

    if (operation === 'unstep') {
      return this.api.stepProcess(projectId, processId, -1);
    }

    return this.api.cancelProcess(projectId, processId);
  }

  private replaceConfig(detail: ProcessConfig): void {
    this.configs.update((configs) =>
      configs.map((config) => (config.id === detail.id ? detail : config))
    );
  }

  private replaceExecution(detail: ProcessExecution): void {
    this.executions.update((executions) =>
      executions.map((execution) =>
        execution.id === detail.id ? detail : execution
      )
    );
    this.runningExecutions.update((executions) =>
      executions.map((execution) =>
        execution.id === detail.id ? detail : execution
      )
    );
  }

  private isProcessRunning(execution: ProcessExecution): boolean {
    return Boolean(
      execution.startDate &&
        !execution.stopDate &&
        !execution.failDate &&
        !execution.finishDate
    );
  }

  private activeOrLastExecutionStep(
    execution: ProcessExecution | null
  ): ProcessStep | null {
    const steps = this.processSteps(execution);

    return (
      [...steps]
        .reverse()
        .find(
          (step) =>
            step.startDate && !step.stopDate && !step.failDate && !step.finishDate
        ) ??
      [...steps].reverse().find((step) => step.startDate) ??
      null
    );
  }

  private normalizedProgress(
    progress: number | null,
    process: ProcessExecution | ProcessStep
  ): number | null {
    if (process.finishDate && !process.failDate) {
      return 100;
    }

    if (progress === null || Number.isNaN(progress) || progress < 0) {
      return null;
    }

    return this.progressPercent(progress);
  }

  private processQueryRestriction(): string {
    const queryRestriction = [
      this.processFilterQueryRestriction(),
      `type:${this.escapeLuceneTerm(this.selectedProcessType())}`
    ]
      .filter(Boolean)
      .join(' AND ');

    return queryRestriction;
  }

  private processFilterQueryRestriction(): string {
    const filter = this.filter().trim();

    if (!filter) {
      return '';
    }

    if (this.isExplicitProcessQuery(filter)) {
      return `(${filter})`;
    }

    return filter
      .split(/\s+/)
      .filter(Boolean)
      .map((token) => this.processFilterTokenQuery(token))
      .join(' AND ');
  }

  private processFilterTokenQuery(token: string): string {
    const escapedToken = this.escapeLuceneTerm(token);
    const fields = ['name', 'description', 'terminology', 'version'];

    return `(${fields
      .map((field) => `${field}:${escapedToken}*`)
      .join(' OR ')})`;
  }

  private isExplicitProcessQuery(value: string): boolean {
    return /[:"()]/.test(value);
  }

  private escapeLuceneTerm(value: string): string {
    return value
      .replace(/&&/g, '\\&&')
      .replace(/\|\|/g, '\\||')
      .replace(/([+\-!(){}\[\]^"~?:\\/])/g, '\\$1')
      .replace(/\*/g, '');
  }
}
