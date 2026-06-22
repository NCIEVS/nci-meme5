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

import { ProjectContextService } from '../../core/navigation/project-context.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { DialogComponent } from '../../shared/dialog/dialog.component';
import { buildOperationalPfs } from './operational-api.helpers';
import { OperationalApiService } from './operational-api.service';
import {
  AlgorithmParameter,
  KeyValuePair,
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

@Component({
  selector: 'meme-process',
  imports: [DialogComponent, FormsModule],
  templateUrl: './process.component.html',
  styleUrl: './operations.component.css'
})
export class ProcessComponent implements OnInit, OnDestroy {
  private readonly api = inject(OperationalApiService);
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
  protected readonly deletingConfigId = signal<number | null>(null);
  protected readonly deletingStepId = signal<number | null>(null);
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
  protected readonly loadingExecutionDetail = signal(false);
  protected readonly queryPreviewMap = signal<Record<string, boolean>>({});
  protected readonly testingQueryKey = signal<string | null>(null);
  protected readonly activeStepId = signal<number | null>(null);
  protected readonly processLog = signal('');
  protected readonly processProgress = signal<number | null>(null);
  protected readonly processConfigForm = signal<ProcessConfigForm | null>(null);
  protected readonly processConfigFormErrors = signal<string[]>([]);
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
  protected readonly selectedConfig = signal<ProcessConfig | null>(null);
  protected readonly selectedExecution = signal<ProcessExecution | null>(null);
  protected readonly selectedProcessType = signal<ProcessType>('Insertion');
  protected readonly stepLog = signal('');
  protected readonly stepProgress = signal<number | null>(null);
  protected readonly feedbackUpdatedAt = signal<number | null>(null);
  protected readonly terminologies = signal<OperationalTerminology[]>([]);
  protected readonly updatingStepId = signal<number | null>(null);

  protected readonly configTotalPages = computed(() =>
    this.pageCount(this.configTotalCount(), this.configPageSize())
  );
  protected readonly executionTotalPages = computed(() =>
    this.pageCount(this.executionTotalCount(), this.executionPageSize())
  );
  protected readonly projectId = computed(() => this.projectContext.projectId());
  protected readonly projectRole = computed(
    () => this.projectContext.projectRole() || 'n/a'
  );
  protected readonly canManageProcesses = computed(
    () => this.projectContext.projectRole() === 'ADMINISTRATOR'
  );

  ngOnInit(): void {
    this.loadTerminologies();
    this.load();
    this.startRunningStateRefresh();
  }

  ngOnDestroy(): void {
    this.stopExecutionFeedbackPolling();
    this.runningStateSubscription.unsubscribe();
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
      'lastModified',
      false,
      this.processQueryRestriction()
    );
    const executionPfs = buildOperationalPfs(
      this.executionPage(),
      this.executionPageSize(),
      'lastModified',
      false,
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
          this.selectConfig(
            configs.items.find((config) => config.id === selectedConfigId) ??
              configs.items[0] ??
              null
          );
          this.selectExecution(
            running.items.find((execution) => execution.id === selectedExecutionId) ??
              executions.items.find((execution) => execution.id === selectedExecutionId) ??
              running.items[0] ??
              executions.items[0] ??
              null
          );
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

  protected clearFilter(): void {
    if (!this.filter()) {
      return;
    }

    this.filter.set('');
    this.load();
  }

  protected setProcessType(value: string): void {
    const processType = this.matchingProcessType(value) ?? 'Insertion';

    this.selectedProcessType.set(processType);
    this.configPage.set(1);
    this.executionPage.set(1);
    this.load();
  }

  protected setConfigPageSize(value: string): void {
    this.configPageSize.set(Number(value));
    this.configPage.set(1);
    this.load();
  }

  protected setExecutionPageSize(value: string): void {
    this.executionPageSize.set(Number(value));
    this.executionPage.set(1);
    this.load();
  }

  protected previousConfigPage(): void {
    if (this.configPage() === 1) {
      return;
    }

    this.configPage.update((page) => page - 1);
    this.load();
  }

  protected nextConfigPage(): void {
    if (this.configPage() === this.configTotalPages()) {
      return;
    }

    this.configPage.update((page) => page + 1);
    this.load();
  }

  protected previousExecutionPage(): void {
    if (this.executionPage() === 1) {
      return;
    }

    this.executionPage.update((page) => page - 1);
    this.load();
  }

  protected nextExecutionPage(): void {
    if (this.executionPage() === this.executionTotalPages()) {
      return;
    }

    this.executionPage.update((page) => page + 1);
    this.load();
  }

  protected startAddProcessConfig(): void {
    this.processConfigFormErrors.set([]);
    this.processConfigForm.set({
      description: '',
      feedbackEmail: '',
      inputPath: '',
      logPath: '',
      mode: 'add',
      name: '',
      processConfig: null,
      terminology: this.terminologies()[0]?.terminology ?? '',
      type: this.defaultProcessType(),
      version: this.terminologies()[0]?.version ?? ''
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
    const terminology = this.terminologies().find(
      (entry) => entry.terminology === value
    );

    this.processConfigForm.update((form) =>
      form
        ? {
            ...form,
            terminology: value,
            version: terminology?.version ?? form.version
          }
        : form
    );
  }

  protected processConfigTypeOptions(): string[] {
    const formType = this.processConfigForm()?.type;
    const options: string[] = this.processTypes;

    if (formType && !options.includes(formType)) {
      return [formType, ...options];
    }

    return options;
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
          this.notifications.success('Process config added.');
          this.load(addedConfig.id, this.selectedExecution()?.id);
          return;
        }

        this.notifications.success('Process config saved.');
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
          this.notifications.success('Process config removed.');
          this.selectConfig(null);
          this.load(null, this.selectedExecution()?.id);
        },
        error: () => {
          this.notifications.error('Process config could not be removed.');
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
          this.notifications.success('Process config imported.');
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
          this.notifications.success('Process config exported.');
        },
        error: () => {
          this.notifications.error('Process config could not be exported.');
        }
      });
  }

  protected startAddAlgorithmStep(config: ProcessConfig): void {
    const projectId = this.projectId();
    const processId = config.id;
    const algorithmType = this.algorithmTypes()[0];
    const algorithmKey = algorithmType?.key;

    if (!projectId || !processId || !algorithmKey) {
      return;
    }

    this.algorithmStepFormErrors.set([]);
    this.queryPreviewMap.set({});
    this.testingQueryKey.set(null);
    this.loadingAlgorithmStep.set(true);
    this.api
      .newAlgorithmConfig(projectId, processId, algorithmKey)
      .pipe(finalize(() => this.loadingAlgorithmStep.set(false)))
      .subscribe({
        next: (algorithm) => {
          this.algorithmStepForm.set({
            algorithm: this.prepareAlgorithmForForm(
              {
                ...algorithm,
                algorithmKey,
                enabled: true,
                name: algorithm.name ?? algorithmType.value ?? algorithmKey
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
          this.notifications.error('Algorithm step template could not be loaded.');
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
        error: () => {
          this.algorithmStepFormErrors.set([
            'Algorithm configuration could not be validated.'
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
        this.notifications.success(
          form.mode === 'add' ? 'Algorithm step added.' : 'Algorithm step saved.'
        );
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
          this.notifications.success(
            this.isStepEnabled(step)
              ? 'Algorithm step disabled.'
              : 'Algorithm step enabled.'
          );
          this.refreshProcessConfig(config.id as number);
        },
        error: () => {
          this.notifications.error('Algorithm step could not be updated.');
        }
      });
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
          this.notifications.success('Algorithm step removed.');
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
    const projectId = this.projectId();
    const processId = config.id;
    const steps = this.processSteps(config);
    const index = steps.findIndex((entry) => entry.id === step.id);
    const nextIndex = index + direction;

    if (
      !projectId ||
      !processId ||
      index === -1 ||
      nextIndex < 0 ||
      nextIndex >= steps.length
    ) {
      return;
    }

    const nextSteps = steps.map((entry) =>
      this.buildAlgorithmPayload(entry, processId)
    );
    const [movedStep] = nextSteps.splice(index, 1);
    nextSteps.splice(nextIndex, 0, movedStep);

    this.updatingStepId.set(step.id ?? null);
    this.api
      .updateProcessConfig(projectId, {
        ...config,
        steps: nextSteps
      })
      .pipe(finalize(() => this.updatingStepId.set(null)))
      .subscribe({
        next: () => {
          this.notifications.success('Algorithm step moved.');
          this.refreshProcessConfig(processId);
        },
        error: () => {
          this.notifications.error('Algorithm step could not be moved.');
        }
      });
  }

  protected selectConfig(config: ProcessConfig | null): void {
    this.selectedConfig.set(config);

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

    const projectId = this.projectId();
    if (!projectId || !execution?.id) {
      this.loadingExecutionDetail.set(false);
      return;
    }

    this.refreshSelectedExecutionFeedback(false);
  }

  protected runProcessOperation(
    operation: ProcessOperation,
    process: ProcessConfig | ProcessExecution
  ): void {
    const projectId = this.projectId();

    if (!projectId || !process.id || !this.canManageProcesses()) {
      return;
    }

    if (!this.confirmProcessOperation(operation, process)) {
      return;
    }

    const request = this.processOperationRequest(projectId, operation, process.id);

    this.runningOperation.set(operation);
    request.pipe(finalize(() => this.runningOperation.set(null))).subscribe({
      next: (processExecutionId) => {
        this.notifications.success(this.processOperationMessage(operation));
        this.load(this.selectedConfig()?.id, processExecutionId);
      },
      error: () => {
        this.notifications.error('Process operation could not be completed.');
      }
    });
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

    if (execution.failDate) {
      return 'FAILED';
    }

    if (execution.finishDate) {
      return 'FINISHED';
    }

    if (execution.stopDate) {
      return 'STOPPED';
    }

    if (execution.startDate) {
      return 'RUNNING';
    }

    return 'NEW';
  }

  protected statusClass(execution: ProcessExecution | null): string {
    const status = this.executionStatus(execution);

    if (status === 'FAILED') {
      return 'failed';
    }

    if (status === 'RUNNING') {
      return 'running';
    }

    return '';
  }

  protected processSteps(process: ProcessConfig | ProcessExecution | null): ProcessStep[] {
    return process?.steps ?? [];
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

    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
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

    if (typeof response.error === 'string') {
      return response.error;
    }

    if (typeof response.message === 'string') {
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
    this.processLog.set('');
    this.processProgress.set(null);
    this.stepLog.set('');
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
            processLog: this.api
              .getProcessLog(projectId, executionId)
              .pipe(catchError(() => of(''))),
            processProgress: this.api
              .getProcessProgress(projectId, executionId)
              .pipe(catchError(() => of(null))),
            stepLog: currentStepId
              ? this.api
                  .getAlgorithmLog(projectId, currentStepId)
                  .pipe(catchError(() => of('')))
              : of(''),
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
        next: ({ detail, processLog, processProgress, stepLog, stepProgress }) => {
          if (this.selectedExecution()?.id !== detail.id) {
            return;
          }

          const currentStep = this.activeOrLastExecutionStep(detail);

          this.selectedExecution.set(detail);
          this.replaceExecution(detail);
          this.activeStepId.set(currentStep?.id ?? null);
          this.processLog.set(processLog);
          this.processProgress.set(this.normalizedProgress(processProgress, detail));
          this.stepLog.set(stepLog);
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
        properties[parameter.fieldName] = parameter.values.join(';');
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
          this.algorithmTypes.set(
            [...algorithmTypes].sort((a, b) =>
              (a.value ?? a.key ?? '').localeCompare(b.value ?? b.key ?? '')
            )
          ),
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

  private processOperationMessage(operation: ProcessOperation): string {
    if (operation === 'prepare') {
      return 'Process prepared for execution.';
    }

    if (operation === 'execute') {
      return 'Process execution started.';
    }

    if (operation === 'restart') {
      return 'Process restart started.';
    }

    if (operation === 'step') {
      return 'Process step started.';
    }

    if (operation === 'unstep') {
      return 'Process unstep started.';
    }

    return 'Process cancellation requested.';
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
    return [this.filter().trim(), this.processTypeQueryRestriction()]
      .filter(Boolean)
      .join(' AND ');
  }

  private processTypeQueryRestriction(): string {
    return this.selectedProcessType();
  }
}
