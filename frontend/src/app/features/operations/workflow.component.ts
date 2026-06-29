import { DatePipe, NgClass, NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  concatMap,
  finalize,
  forkJoin,
  interval,
  map,
  Observable,
  of,
  range,
  Subscription,
  toArray
} from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { ProjectContextService } from '../../core/navigation/project-context.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { DialogComponent } from '../../shared/dialog/dialog.component';
import { buildOperationalPfs } from './operational-api.helpers';
import { OperationalApiService } from './operational-api.service';
import {
  WorkflowChangeEvent,
  WorkflowLiveUpdateService
} from './workflow-live-update.service';
import {
  Checklist,
  ClusterTypeStats,
  KeyValuePair,
  PfsParameter,
  TrackingRecord,
  TrackingRecordConcept,
  WorkflowBin,
  WorkflowBinDefinition,
  WorkflowConfig,
  WorkflowEpoch,
  WorkflowAction,
  WorkflowNote,
  WorkflowReport,
  OperationalProject,
  OperationalUser,
  Worklist
} from './operational.models';

interface WorkflowConfigForm {
  adminConfig: boolean;
  mode: 'add' | 'edit';
  mutuallyExclusive: boolean;
  queryStyle: string;
  type: string;
  workflowConfig: WorkflowConfig | null;
}

interface WorkflowBinDefinitionForm {
  definition: WorkflowBinDefinition;
  mode: 'add' | 'edit';
  originalName: string | null;
  positionAfterId: number | null;
  workflowConfig: WorkflowConfig;
}

interface WorkflowQueryTestResult {
  results: string[];
  totalCount: number;
}

type WorkflowListKind = 'checklist' | 'worklist';

interface WorkflowListDetail {
  item: Checklist & Partial<Worklist>;
  kind: WorkflowListKind;
  log: string;
}

interface WorkflowAssignmentAction {
  action: WorkflowAction;
  label: string;
}

type WorkflowAssignmentRole = 'AUTHOR' | 'REVIEWER';

type WorkflowLifecycleAction = 'STAMP' | 'UNAPPROVE' | 'FINISH';

interface WorkflowLifecycleButton {
  action: WorkflowLifecycleAction;
  label: string;
}

interface WorkflowFinishForm {
  errors: string[];
  hours: number | null;
  minutes: number | null;
  role: WorkflowAssignmentRole;
  worklist: Checklist & Partial<Worklist>;
}

type WorkflowListSortOrder = 'clusterId' | 'indexedData' | 'RANDOM';

interface WorkflowChecklistCreationForm {
  bin: WorkflowBin;
  clusterCount: number | null;
  clusterType: string;
  description: string;
  excludeOnWorklist: boolean;
  name: string;
  skipClusterCount: number | null;
  sortOrder: WorkflowListSortOrder;
}

interface WorkflowWorklistCreationForm {
  availableClusterCount: number;
  bin: WorkflowBin;
  clusterCount: number | null;
  clusterType: string;
  numberOfWorklists: number | null;
  skipClusterCount: number | null;
  sortOrder: WorkflowListSortOrder;
}

interface WorkflowChecklistComputeForm {
  clusterCount: number | null;
  name: string;
  query: string;
  queryPreview: boolean;
  queryType: string;
  skipClusterCount: number | null;
}

@Component({
  selector: 'meme-workflow',
  imports: [DatePipe, DialogComponent, FormsModule, NgClass, NgTemplateOutlet],
  templateUrl: './workflow.component.html',
  styleUrl: './operations.component.css'
})
export class WorkflowComponent implements OnInit, OnDestroy {
  private readonly api = inject(OperationalApiService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly projectContext = inject(ProjectContextService);
  private readonly workflowLiveUpdates = inject(WorkflowLiveUpdateService);

  private binRegenerationPollSubscription: Subscription | null = null;
  private workflowLiveSubscription: Subscription | null = null;

  protected readonly actingWorkflowItemKey = signal<string | null>(null);
  protected readonly autofixAlgorithms = signal<KeyValuePair[]>([]);
  protected readonly bins = signal<WorkflowBin[]>([]);
  protected readonly binFilter = signal('');
  protected readonly binPage = signal(1);
  protected readonly binPageSize = 10;
  protected readonly filteredBins = computed(() => {
    const q = this.binFilter().toLowerCase();
    if (!q) return this.bins();
    return this.bins().filter((b) => b.name?.toLowerCase().includes(q));
  });
  protected readonly binTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredBins().length / this.binPageSize))
  );
  protected readonly pagedBins = computed(() => {
    const start = (this.binPage() - 1) * this.binPageSize;
    return this.filteredBins().slice(start, start + this.binPageSize);
  });
  protected readonly checklists = signal<Checklist[]>([]);
  protected readonly checklistsTotal = signal(0);
  protected readonly checklistFilter = signal('');
  protected readonly checklistPage = signal(1);
  protected readonly checklistPageSize = 10;
  protected readonly checklistCreationForm =
    signal<WorkflowChecklistCreationForm | null>(null);
  protected readonly checklistCreationFormErrors = signal<string[]>([]);
  protected readonly clusterCountOptions = [20, 50, 100, 200, 500];
  protected readonly currentEpoch = signal<WorkflowEpoch | null>(null);
  protected readonly configs = signal<WorkflowConfig[]>([]);
  protected readonly deletingBinId = signal<number | null>(null);
  protected readonly deletingConfigId = signal<number | null>(null);
  protected readonly deletingWorkflowItemKey = signal<string | null>(null);
  protected readonly epochs = signal<WorkflowEpoch[]>([]);
  protected readonly exportingConfigId = signal<number | null>(null);
  protected readonly exportingWorkflowItemKey = signal<string | null>(null);
  protected readonly filter = signal('');
  protected readonly finishWorkflowForm = signal<WorkflowFinishForm | null>(null);
  protected readonly generatingWorkflowReportKey = signal<string | null>(null);
  protected readonly importWorkflowDialogOpen = signal(false);
  protected readonly importWorkflowFile = signal<File | null>(null);
  protected readonly importWorkflowFormErrors = signal<string[]>([]);
  protected readonly importingWorkflowConfig = signal(false);
  protected readonly importChecklistDialogOpen = signal(false);
  protected readonly importChecklistFile = signal<File | null>(null);
  protected readonly importChecklistFormErrors = signal<string[]>([]);
  protected readonly importChecklistName = signal('');
  protected readonly importingChecklist = signal(false);
  protected readonly checklistComputeForm =
    signal<WorkflowChecklistComputeForm | null>(null);
  protected readonly checklistComputeFormErrors = signal<string[]>([]);
  protected readonly checklistComputeQueryTestResult =
    signal<WorkflowQueryTestResult | null>(null);
  protected readonly computingChecklist = signal(false);
  protected readonly testingChecklistComputeQuery = signal(false);
  protected readonly loading = signal(false);
  protected readonly loadingAutofixAlgorithms = signal(false);
  protected readonly loadingBinDefinition = signal(false);
  protected readonly loadingBins = signal(false);
  protected readonly loadingWorkflowReport = signal(false);
  protected readonly loadingWorkflowItemKey = signal<string | null>(null);
  protected readonly loadingWorkflowProjectUsers = signal(false);
  protected readonly addingWorkflowNoteKey = signal<string | null>(null);
  protected readonly assignmentNoteText = signal('');
  protected readonly queryTypes = ['SQL', 'LUCENE', 'JPQL', 'PROGRAM'];
  protected readonly queryStyles = ['CLUSTER', 'REPORT', 'OTHER'];
  protected readonly recomputingConceptStatus =
    signal<'initialize' | 'update' | null>(null);
  protected readonly binRegenerationElapsedSeconds = signal(0);
  protected readonly binRegenerationPolling = signal(false);
  protected readonly draggingWorkflowBinDefinitionId = signal<number | null>(null);
  protected readonly regeneratingBinId = signal<number | null>(null);
  protected readonly regeneratingBins = signal(false);
  protected readonly reorderingWorkflowBins = signal(false);
  protected readonly removingWorkflowReportKey = signal<string | null>(null);
  protected readonly removingWorkflowNoteId = signal<number | null>(null);
  protected readonly savingBinDefinition = signal(false);
  protected readonly savingWorkflowConfig = signal(false);
  protected readonly deletingEpochId = signal<number | null>(null);
  protected readonly editingEpochs = signal(false);
  protected readonly epochFormErrors = signal<string[]>([]);
  protected readonly epochName = signal('');
  protected readonly savingEpoch = signal(false);
  protected readonly selectedBin = signal<WorkflowBin | null>(null);
  protected readonly selectedClusterType = signal<string>('all');
  protected readonly binRecords = signal<TrackingRecord[]>([]);
  protected readonly binRecordsTotal = signal(0);
  protected readonly loadingBinRecords = signal(false);
  protected readonly selectedConcept = signal<TrackingRecordConcept | null>(null);
  protected readonly reportMode = signal<'Report' | 'Interactive' | 'Actions'>('Report');
  protected readonly listDetailRecords = signal<TrackingRecord[]>([]);
  protected readonly listDetailRecordsTotal = signal(0);
  protected readonly listDetailRecordsPage = signal(1);
  protected readonly listDetailRecordsPageSize = 20;
  protected readonly listDetailRecordsTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.listDetailRecordsTotal() / this.listDetailRecordsPageSize))
  );
  protected readonly loadingListDetailRecords = signal(false);
  protected readonly selectedListDetailConcept = signal<TrackingRecordConcept | null>(null);
  protected readonly listDetailReportMode = signal<'Report' | 'Interactive' | 'Actions'>('Report');
  protected readonly assignmentDialogOpen = signal(false);
  protected readonly notesDialogOpen = signal(false);
  protected readonly logDialogOpen = signal(false);
  protected readonly conceptReportsDialogOpen = signal(false);
  protected readonly reportTypeSelection = signal<WorkflowBinDefinition | null>(null);
  protected readonly reports = signal<WorkflowReport[]>([]);
  protected readonly reportsTotal = signal(0);
  protected readonly reportsPage = signal(1);
  protected readonly reportsPageSize = signal(10);
  protected readonly reportsPageSizeOptions = [10, 20, 50, 100];
  protected readonly reportsTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.reportsTotal() / this.reportsPageSize()))
  );
  protected readonly resultsFilter = signal('');
  protected readonly resultsPage = signal(1);
  protected readonly resultsPageSize = signal(10);
  protected readonly filteredResults = computed(() => {
    const q = this.resultsFilter().toLowerCase();
    const results = this.selectedReport()?.results ?? [];
    const filtered = q ? results.filter((r) => r.value?.toLowerCase().includes(q)) : results;
    return [...filtered].sort((a, b) => (a.value ?? '').localeCompare(b.value ?? ''));
  });
  protected readonly resultsTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredResults().length / this.resultsPageSize()))
  );
  protected readonly pagedResults = computed(() => {
    const start = (this.resultsPage() - 1) * this.resultsPageSize();
    return this.filteredResults().slice(start, start + this.resultsPageSize());
  });
  protected readonly selectedReport = signal<WorkflowReport | null>(null);
  protected readonly loadingReports = signal(false);
  protected readonly generatingReport = signal(false);
  protected readonly removingReportId = signal<number | null>(null);
  protected readonly selectedConfigType = signal('');
  protected readonly selectedProject = signal<OperationalProject | null>(null);
  protected readonly selectedWorkflowAssignmentUserName = signal('');
  protected readonly savingChecklist = signal(false);
  protected readonly savingWorklist = signal(false);
  protected readonly downloadingWorkflowReportKey = signal<string | null>(null);
  protected readonly sortOrderOptions: Array<{
    label: string;
    value: WorkflowListSortOrder;
  }> = [
    { label: 'Cluster id', value: 'clusterId' },
    { label: 'Name', value: 'indexedData' },
    { label: 'Randomize', value: 'RANDOM' }
  ];
  protected readonly workflowBinDefinitionForm =
    signal<WorkflowBinDefinitionForm | null>(null);
  protected readonly workflowBinDefinitionFormErrors = signal<string[]>([]);
  protected readonly workflowBinQueryTestResult =
    signal<WorkflowQueryTestResult | null>(null);
  protected readonly testingWorkflowBinQuery = signal(false);
  protected readonly workflowConfigForm = signal<WorkflowConfigForm | null>(null);
  protected readonly workflowConfigFormErrors = signal<string[]>([]);
  protected readonly workflowListDetail = signal<WorkflowListDetail | null>(null);
  protected readonly workflowNoteText = signal('');
  protected readonly workflowProjectUsers = signal<OperationalUser[]>([]);
  protected readonly workflowReportFileName = signal<string | null>(null);
  protected readonly worklistCreationForm =
    signal<WorkflowWorklistCreationForm | null>(null);
  protected readonly worklistCreationFormErrors = signal<string[]>([]);
  protected readonly worklists = signal<Worklist[]>([]);
  protected readonly worklistsTotal = signal(0);
  protected readonly worklistFilter = signal('');
  protected readonly worklistPage = signal(1);
  protected readonly worklistPageSize = 20;
  protected readonly workflowLiveConnected = this.workflowLiveUpdates.connected;

  protected readonly worklistTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.worklistsTotal() / this.worklistPageSize))
  );
  protected readonly checklistTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.checklistsTotal() / this.checklistPageSize))
  );

  protected readonly availableProjects = signal<OperationalProject[]>([]);
  protected readonly projectId = computed(() => this.projectContext.projectId());
  protected readonly projectRole = computed(
    () => this.projectContext.projectRole() || 'n/a'
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
  protected readonly canManageWorkflow = computed(
    () => this.projectContext.projectRole() === 'ADMINISTRATOR'
  );
  protected readonly canCreateWorkflowLists = computed(() => {
    const role = this.projectContext.projectRole();

    return role === 'ADMINISTRATOR' || role === 'REVIEWER' || role === 'EDITOR5';
  });
  protected readonly canEditWorkflowEpochs = computed(() => {
    const role = this.projectContext.projectRole();

    return role === 'ADMINISTRATOR' || role === 'REVIEWER' || role === 'EDITOR5';
  });
  protected readonly canImportChecklists = computed(() => {
    const role = this.projectContext.projectRole();

    return role === 'ADMINISTRATOR' || role === 'REVIEWER' || role === 'EDITOR5';
  });
  protected readonly canManageWorkflowListActions = computed(() => {
    const role = this.projectContext.projectRole();

    return role === 'ADMINISTRATOR' || role === 'REVIEWER' || role === 'EDITOR5';
  });
  protected readonly canManageWorklistAssignments = computed(() => {
    const role = this.projectContext.projectRole();

    return role === 'ADMINISTRATOR' || role === 'REVIEWER' || role === 'EDITOR5';
  });
  protected readonly canGenerateWorkflowReports = computed(() => {
    const role = this.projectContext.projectRole();

    return (
      role === 'ADMINISTRATOR' ||
      role === 'AUTHOR' ||
      role === 'REVIEWER' ||
      role === 'EDITOR5'
    );
  });
  protected readonly canManageWorkflowNotes = computed(() => {
    const role = this.projectContext.projectRole();

    return (
      role === 'ADMINISTRATOR' ||
      role === 'AUTHOR' ||
      role === 'REVIEWER' ||
      role === 'EDITOR5'
    );
  });
  protected readonly selectedWorkflowConfig = computed(
    () =>
      this.configs().find((config) => config.type === this.selectedConfigType()) ??
      null
  );
  protected readonly reportTypeList = computed<WorkflowBinDefinition[]>(() =>
    this.configs()
      .filter((c) => c.queryStyle === 'REPORT')
      .flatMap((c) => c.workflowBinDefinitions ?? [])
  );
  protected readonly binRegenerationRunning = computed(
    () => this.regeneratingBins() || this.regeneratingBinId() !== null
  );
  protected readonly binRegenerationProgressText = computed(() => {
    const elapsedSeconds = this.binRegenerationElapsedSeconds();

    return elapsedSeconds
      ? `Refreshing bins every 5 seconds while regeneration runs (${elapsedSeconds} sec).`
      : 'Refreshing bins every 5 seconds while regeneration runs.';
  });

  ngOnInit(): void {
    this.workflowLiveSubscription = this.workflowLiveUpdates.events$.subscribe(
      (event) => this.handleWorkflowChangeEvent(event)
    );
    this.workflowLiveUpdates.connect();
    this.loadProjects();
    this.load();
  }

  ngOnDestroy(): void {
    this.workflowLiveSubscription?.unsubscribe();
    this.stopBinRegenerationProgressPolling();
    this.workflowLiveUpdates.disconnect();
  }

  protected load(): void {
    const projectId = this.projectId();

    if (!projectId) {
      return;
    }

    const wlPfs = buildOperationalPfs(this.worklistPage(), this.worklistPageSize, 'lastModified', false, this.worklistFilter());
    const clPfs = buildOperationalPfs(this.checklistPage(), this.checklistPageSize, 'lastModified', false, this.checklistFilter());
    this.loading.set(true);

    forkJoin({
      checklists: this.api.findChecklists(projectId, this.checklistFilter(), clPfs),
      configs: this.api.getWorkflowConfigs(projectId),
      currentEpoch: this.api.getCurrentWorkflowEpoch(projectId),
      epochs: this.api.getWorkflowEpochs(projectId),
      worklists: this.api.findWorklists(projectId, this.worklistFilter(), wlPfs)
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ checklists, configs, currentEpoch, epochs, worklists }) => {
          this.checklists.set(checklists.items);
          this.checklistsTotal.set(checklists.totalCount);
          this.configs.set([...configs.items].sort((a, b) => (a.type ?? '').localeCompare(b.type ?? '')));
          this.currentEpoch.set(currentEpoch);
          this.epochs.set(this.sortWorkflowEpochs(epochs.items));
          this.worklists.set(worklists.items);
          this.worklistsTotal.set(worklists.totalCount);
          this.ensureSelectedConfigType(configs.items);
          this.loadWorkflowAssignmentContext(projectId);
        },
        error: () => {
          this.notifications.error('Workflow information could not be loaded.');
        }
      });
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
    const prefs = user.userPreferences ?? {};
    const id = Number(idStr);
    const newPrefs = { ...prefs, lastProjectId: id, lastProjectRole: null };
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
    const prefs = user.userPreferences ?? {};
    const newPrefs = { ...prefs, lastProjectRole: role };
    this.api.updateUserPreferences(newPrefs).subscribe({
      next: (saved) => {
        this.auth.updateCurrentUserPreferences(saved ?? newPrefs);
        this.load();
      },
      error: () => this.notifications.error('Could not switch role.')
    });
  }

  protected reloadWorklists(): void {
    const projectId = this.projectId();

    if (!projectId) {
      return;
    }

    const query = this.prepListQuery(this.worklistFilter());
    const pfs = buildOperationalPfs(this.worklistPage(), this.worklistPageSize, 'lastModified', false, query);

    this.api.findWorklists(projectId, query, pfs).subscribe({
      next: (result) => {
        this.worklists.set(result.items);
        this.worklistsTotal.set(result.totalCount);
      },
      error: () => this.notifications.error('Worklists could not be reloaded.')
    });
  }

  protected reloadChecklists(): void {
    const projectId = this.projectId();

    if (!projectId) {
      return;
    }

    const query = this.prepListQuery(this.checklistFilter());
    const pfs = buildOperationalPfs(this.checklistPage(), this.checklistPageSize, 'lastModified', false, query);

    this.api.findChecklists(projectId, query, pfs).subscribe({
      next: (result) => {
        this.checklists.set(result.items);
        this.checklistsTotal.set(result.totalCount);
      },
      error: () => this.notifications.error('Checklists could not be reloaded.')
    });
  }

  protected setWorklistFilter(value: string): void {
    this.worklistFilter.set(value);
    this.worklistPage.set(1);
    this.reloadWorklists();
  }

  protected setChecklistFilter(value: string): void {
    this.checklistFilter.set(value);
    this.checklistPage.set(1);
    this.reloadChecklists();
  }

  protected setBinFilter(value: string): void {
    this.binFilter.set(value);
    this.binPage.set(1);
  }

  protected selectReportType(def: WorkflowBinDefinition): void {
    this.reportTypeSelection.set(def);
    this.selectedReport.set(null);
    this.resultsFilter.set('');
    this.resultsPage.set(1);
    this.reportsPage.set(1);
    this.loadReports(1);
  }

  protected setReportsPageSize(value: number): void {
    this.reportsPageSize.set(value);
    this.reportsPage.set(1);
    this.loadReports(1);
  }

  protected selectReport(report: WorkflowReport): void {
    this.selectedReport.set(report);
    this.resultsFilter.set('');
    this.resultsPage.set(1);
  }

  protected setResultsFilter(value: string): void {
    this.resultsFilter.set(value);
    this.resultsPage.set(1);
  }

  protected setResultsPageSize(value: number): void {
    this.resultsPageSize.set(value);
    this.resultsPage.set(1);
  }

  protected loadReports(page = this.reportsPage()): void {
    const projectId = this.projectId();
    const def = this.reportTypeSelection();
    if (!projectId || !def?.name) return;
    const pfs = buildOperationalPfs(page, this.reportsPageSize(), 'name', true, '');
    this.loadingReports.set(true);
    this.api.findReports(projectId, def.name, pfs).subscribe({
      next: (result) => {
        this.reports.set(result.items);
        this.reportsTotal.set(result.totalCount);
        this.reportsPage.set(page);
      },
      error: () => this.notifications.error('Reports could not be loaded.'),
      complete: () => this.loadingReports.set(false)
    });
  }

  protected generateTopLevelReport(): void {
    const projectId = this.projectId();
    const def = this.reportTypeSelection();
    if (!projectId || !def?.name || !def.query || !def.queryType) return;
    this.generatingReport.set(true);
    this.api.generateReport(projectId, def.name, def.query, def.queryType).subscribe({
      next: () => {
        this.notifications.success('Report generated.');
        this.loadReports(1);
      },
      error: () => this.notifications.error('Report could not be generated.'),
      complete: () => this.generatingReport.set(false)
    });
  }

  protected removeTopLevelReport(report: WorkflowReport): void {
    if (!report.id) return;
    if (!window.confirm(`Remove report "${report.name}"?`)) return;
    this.removingReportId.set(report.id);
    this.api.removeReport(report.id).subscribe({
      next: () => {
        if (this.selectedReport()?.id === report.id) this.selectedReport.set(null);
        this.loadReports(this.reportsPage());
      },
      error: () => this.notifications.error('Report could not be removed.'),
      complete: () => this.removingReportId.set(null)
    });
  }

  protected reportResultValues(result: { value?: string | null }): string[] {
    return result.value?.split('@').filter(Boolean) ?? [];
  }

  private prepListQuery(raw: string): string {
    const q = raw.trim();

    if (!q) {
      return '';
    }

    return q.includes('(') || q.includes(':') || q.includes('"') ? q : `${q}*`;
  }

  protected worklistPageTo(page: number): void {
    this.worklistPage.set(page);
    this.reloadWorklists();
  }

  protected checklistPageTo(page: number): void {
    this.checklistPage.set(page);
    this.reloadChecklists();
  }

  protected setFilter(value: string): void {
    this.filter.set(value);
  }

  private sortWorkflowEpochs(epochs: WorkflowEpoch[]): WorkflowEpoch[] {
    return [...epochs].sort((first, second) => {
      const firstName = first.name || '';
      const secondName = second.name || '';
      if (secondName > firstName) {
        return 1;
      }
      if (secondName < firstName) {
        return -1;
      }
      return 0;
    });
  }

  protected clearFilter(): void {
    if (!this.filter()) {
      return;
    }

    this.filter.set('');
    this.load();
  }

  protected startEditEpochs(): void {
    this.epochName.set('');
    this.epochFormErrors.set([]);
    this.editingEpochs.set(true);
  }

  protected closeEditEpochs(): void {
    if (this.savingEpoch() || this.deletingEpochId()) {
      return;
    }

    this.editingEpochs.set(false);
    this.epochName.set('');
    this.epochFormErrors.set([]);
  }

  protected setEpochName(value: string): void {
    this.epochName.set(value);
    this.epochFormErrors.set([]);
  }

  protected addWorkflowEpoch(): void {
    const projectId = this.projectId();
    const name = this.epochName().trim();

    if (!projectId) {
      return;
    }

    if (!name) {
      this.epochFormErrors.set(['Epoch name is required.']);
      return;
    }

    if (
      this.epochs().some(
        (epoch) => epoch.name?.toLocaleLowerCase() === name.toLocaleLowerCase()
      )
    ) {
      this.epochFormErrors.set(['Epoch with this name already exists.']);
      return;
    }

    this.savingEpoch.set(true);
    this.api
      .addWorkflowEpoch(projectId, { name })
      .pipe(finalize(() => this.savingEpoch.set(false)))
      .subscribe({
        next: () => {
          this.epochName.set('');
          this.epochFormErrors.set([]);
          this.notifications.success('Workflow epoch added.');
          this.load();
        },
        error: () => {
          this.epochFormErrors.set(['Workflow epoch could not be added.']);
        }
      });
  }

  protected removeWorkflowEpoch(epoch: WorkflowEpoch): void {
    const projectId = this.projectId();

    if (!projectId || !epoch.id) {
      return;
    }

    if (!window.confirm(`Remove workflow epoch "${epoch.name || epoch.id}"?`)) {
      return;
    }

    this.deletingEpochId.set(epoch.id);
    this.api
      .removeWorkflowEpoch(projectId, epoch.id)
      .pipe(finalize(() => this.deletingEpochId.set(null)))
      .subscribe({
        next: () => {
          this.notifications.success('Workflow epoch removed.');
          this.load();
        },
        error: () => {
          this.epochFormErrors.set(['Workflow epoch could not be removed.']);
        }
      });
  }

  protected selectConfigType(type: string): void {
    this.selectedConfigType.set(type);
    this.loadBins(type);
  }

  protected selectBin(bin: WorkflowBin, clusterType = 'all'): void {
    this.selectedBin.set(bin);
    this.selectedClusterType.set(clusterType);
    this.selectedConcept.set(null);
    this.reportMode.set('Report');
    this.loadBinRecords(bin, clusterType);
  }

  private loadBinRecords(bin: WorkflowBin, clusterType = 'all'): void {
    const projectId = this.projectId();

    if (!projectId || !bin.id) {
      return;
    }

    this.loadingBinRecords.set(true);
    this.binRecords.set([]);
    this.binRecordsTotal.set(0);

    const queryRestriction =
      clusterType === 'all' ? ''
      : clusterType === 'default' ? ' NOT clusterType:[* TO *]'
      : clusterType;

    this.api
      .findTrackingRecordsForBin(projectId, bin.id, {
        startIndex: 0,
        maxResults: 20,
        sortField: 'clusterId',
        ascending: true,
        queryRestriction
      })
      .pipe(finalize(() => this.loadingBinRecords.set(false)))
      .subscribe({
        next: ({ records, totalCount }) => {
          this.binRecords.set(records ?? []);
          this.binRecordsTotal.set(totalCount ?? 0);
        },
        error: () => {
          this.notifications.error('Workflow bin records could not be loaded.');
        }
      });
  }

  protected selectConcept(concept: TrackingRecordConcept): void {
    this.selectedConcept.set(concept);
    this.reportMode.set('Report');
  }

  protected setReportMode(mode: 'Report' | 'Interactive' | 'Actions'): void {
    this.reportMode.set(mode);
  }

  protected selectListDetailConcept(concept: TrackingRecordConcept): void {
    this.selectedListDetailConcept.set(concept);
    this.listDetailReportMode.set('Report');
  }

  protected clusterPageTo(page: number): void {
    const detail = this.workflowListDetail();
    if (!detail) return;
    this.loadListDetailRecords(detail.kind, detail.item, page);
  }

  protected setListDetailReportMode(mode: 'Report' | 'Interactive' | 'Actions'): void {
    this.listDetailReportMode.set(mode);
  }

  protected openAssignmentDialog(worklist: Worklist): void {
    this.selectedWorkflowAssignmentUserName.set('');
    this.assignmentNoteText.set('');
    this.viewWorkflowItem('worklist', worklist, () => this.assignmentDialogOpen.set(true));
  }
  protected closeAssignmentDialog(): void {
    this.assignmentDialogOpen.set(false);
    this.selectedWorkflowAssignmentUserName.set('');
    this.assignmentNoteText.set('');
  }
  protected openNotesDialog(): void { this.notesDialogOpen.set(true); }
  protected closeNotesDialog(): void { this.notesDialogOpen.set(false); }
  protected openLogDialog(): void { this.logDialogOpen.set(true); }
  protected closeLogDialog(): void { this.logDialogOpen.set(false); }
  protected openConceptReportsDialog(): void { this.conceptReportsDialogOpen.set(true); }
  protected closeConceptReportsDialog(): void { this.conceptReportsDialogOpen.set(false); }

  private loadListDetailRecords(kind: WorkflowListKind, item: Checklist | Worklist, page = 1): void {
    const projectId = this.projectId();

    if (!projectId || !item.id) {
      return;
    }

    this.listDetailRecordsPage.set(page);
    this.loadingListDetailRecords.set(true);
    this.listDetailRecords.set([]);
    this.listDetailRecordsTotal.set(0);
    this.selectedListDetailConcept.set(null);
    this.listDetailReportMode.set('Report');

    const startIndex = (page - 1) * this.listDetailRecordsPageSize;
    const request =
      kind === 'checklist'
        ? this.api.findTrackingRecordsForChecklist(projectId, item.id, {
            startIndex,
            maxResults: this.listDetailRecordsPageSize,
            sortField: 'clusterId',
            ascending: true
          })
        : this.api.findTrackingRecordsForWorklist(projectId, item.id, {
            startIndex,
            maxResults: this.listDetailRecordsPageSize,
            sortField: 'clusterId',
            ascending: true
          });

    request.pipe(finalize(() => this.loadingListDetailRecords.set(false))).subscribe({
      next: ({ records, totalCount }) => {
        this.listDetailRecords.set(records ?? []);
        this.listDetailRecordsTotal.set(totalCount ?? 0);
      },
      error: () => {
        this.notifications.error('Tracking records could not be loaded.');
      }
    });
  }

  protected startAddWorkflowConfig(): void {
    this.workflowConfigFormErrors.set([]);
    this.workflowConfigForm.set({
      adminConfig: false,
      mode: 'add',
      mutuallyExclusive: false,
      queryStyle: this.queryStyles[0],
      type: '',
      workflowConfig: null
    });
  }

  protected startEditWorkflowConfig(config: WorkflowConfig): void {
    this.workflowConfigFormErrors.set([]);
    this.workflowConfigForm.set({
      adminConfig: Boolean(config.adminConfig),
      mode: 'edit',
      mutuallyExclusive: Boolean(config.mutuallyExclusive),
      queryStyle: config.queryStyle ?? this.queryStyles[0],
      type: config.type ?? '',
      workflowConfig: config
    });
  }

  protected closeWorkflowConfigForm(): void {
    if (this.savingWorkflowConfig()) {
      return;
    }

    this.workflowConfigForm.set(null);
    this.workflowConfigFormErrors.set([]);
  }

  protected updateWorkflowConfigForm(
    field: keyof Pick<WorkflowConfigForm, 'queryStyle' | 'type'>,
    value: string
  ): void {
    this.workflowConfigForm.update((form) =>
      form
        ? {
            ...form,
            [field]: value
          }
        : form
    );
  }

  protected updateWorkflowConfigFormFlag(
    field: keyof Pick<WorkflowConfigForm, 'adminConfig' | 'mutuallyExclusive'>,
    value: boolean
  ): void {
    this.workflowConfigForm.update((form) =>
      form
        ? {
            ...form,
            [field]: value
          }
        : form
    );
  }

  protected saveWorkflowConfig(): void {
    const form = this.workflowConfigForm();
    const projectId = this.projectId();

    if (!form || !projectId) {
      return;
    }

    const errors = this.validateWorkflowConfigForm(form);
    this.workflowConfigFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    const payload = this.buildWorkflowConfigPayload(form);
    const request: Observable<WorkflowConfig | void> =
      form.mode === 'add'
        ? this.api.addWorkflowConfig(projectId, payload)
        : this.api.updateWorkflowConfig(projectId, payload);

    this.savingWorkflowConfig.set(true);
    request.pipe(finalize(() => this.savingWorkflowConfig.set(false))).subscribe({
      next: (workflowConfig) => {
        const selectedType =
          form.mode === 'add'
            ? (workflowConfig as WorkflowConfig).type
            : payload.type;

        this.workflowConfigForm.set(null);
        this.workflowConfigFormErrors.set([]);

        if (selectedType) {
          this.selectedConfigType.set(selectedType);
        }

        this.notifications.success(
          form.mode === 'add'
            ? 'Workflow config added.'
            : 'Workflow config saved.'
        );
        this.load();
      },
      error: () => {
        this.notifications.error('Workflow config could not be saved.');
      }
    });
  }

  protected removeWorkflowConfig(config: WorkflowConfig): void {
    const projectId = this.projectId();

    if (!projectId || !config.id) {
      return;
    }

    if (
      !window.confirm(
        `Remove workflow config "${config.type || config.id}" and its bins?`
      )
    ) {
      return;
    }

    this.deletingConfigId.set(config.id);
    this.api
      .removeWorkflowConfig(projectId, config.id)
      .pipe(finalize(() => this.deletingConfigId.set(null)))
      .subscribe({
        next: () => {
          if (this.selectedConfigType() === config.type) {
            this.selectedConfigType.set('');
          }

          this.notifications.success('Workflow config removed.');
          this.load();
        },
        error: () => {
          this.notifications.error('Workflow config could not be removed.');
        }
      });
  }

  protected startAddWorkflowBinDefinition(config: WorkflowConfig): void {
    if (!config.id) {
      return;
    }

    this.workflowBinQueryTestResult.set(null);
    this.workflowBinDefinitionFormErrors.set([]);
    this.workflowBinDefinitionForm.set({
      definition: {
        autofix: '',
        description: '',
        editable: true,
        enabled: true,
        name: '',
        query: '',
        queryType: this.queryTypes[0],
        required: false,
        workflowConfig: {
          id: config.id
        },
        workflowConfigId: config.id
      },
      mode: 'add',
      originalName: null,
      positionAfterId: null,
      workflowConfig: config
    });
    this.loadAutofixAlgorithms();
  }

  protected startEditWorkflowBinDefinition(bin: WorkflowBin): void {
    const projectId = this.projectId();
    const type = this.selectedConfigType();

    if (!projectId || !bin.name || !type) {
      return;
    }

    this.workflowBinQueryTestResult.set(null);
    this.workflowBinDefinitionFormErrors.set([]);
    this.loadingBinDefinition.set(true);
    this.api
      .getWorkflowBinDefinition(projectId, bin.name, type)
      .pipe(finalize(() => this.loadingBinDefinition.set(false)))
      .subscribe({
        next: (definition) => {
          const workflowConfig = this.selectedWorkflowConfig();

          if (!definition || !workflowConfig) {
            this.notifications.error('Workflow bin definition could not be loaded.');
            return;
          }

          this.workflowBinDefinitionForm.set({
            definition: this.prepareWorkflowBinDefinitionForForm(
              definition,
              workflowConfig
            ),
            mode: 'edit',
            originalName: definition.name ?? null,
            positionAfterId: null,
            workflowConfig
          });
          this.loadAutofixAlgorithms();
        },
        error: () => {
          this.notifications.error('Workflow bin definition could not be loaded.');
        }
      });
  }

  protected closeWorkflowBinDefinitionForm(): void {
    if (this.savingBinDefinition()) {
      return;
    }

    this.workflowBinDefinitionForm.set(null);
    this.workflowBinDefinitionFormErrors.set([]);
    this.workflowBinQueryTestResult.set(null);
  }

  protected updateWorkflowBinDefinitionForm(
    field: keyof Pick<
      WorkflowBinDefinition,
      'autofix' | 'description' | 'name' | 'query' | 'queryType'
    >,
    value: string
  ): void {
    if (field === 'query' || field === 'queryType') {
      this.workflowBinQueryTestResult.set(null);
    }

    this.workflowBinDefinitionForm.update((form) =>
      form
        ? {
            ...form,
            definition: {
              ...form.definition,
              [field]: value
            }
          }
        : form
    );
  }

  protected updateWorkflowBinDefinitionFlag(
    field: keyof Pick<WorkflowBinDefinition, 'editable' | 'enabled' | 'required'>,
    value: boolean
  ): void {
    this.workflowBinDefinitionForm.update((form) =>
      form
        ? {
            ...form,
            definition: {
              ...form.definition,
              [field]: value
            }
          }
        : form
    );
  }

  protected setWorkflowBinPositionAfter(value: string): void {
    const positionAfterId = value ? Number(value) : null;

    this.workflowBinDefinitionForm.update((form) =>
      form
        ? {
            ...form,
            positionAfterId: Number.isNaN(positionAfterId)
              ? null
              : positionAfterId
          }
        : form
    );
  }

  protected testWorkflowBinQuery(): void {
    const form = this.workflowBinDefinitionForm();
    const projectId = this.projectId();

    if (!form || !projectId) {
      return;
    }

    const query = form.definition.query?.trim() ?? '';
    const queryType = form.definition.queryType?.trim() ?? '';
    const queryStyle = form.workflowConfig.queryStyle?.trim() ?? '';
    const errors: string[] = [];

    if (!query) {
      errors.push('Bin query must be set before testing.');
    }

    if (!queryType) {
      errors.push('Bin query type must be set before testing.');
    }

    if (!queryStyle) {
      errors.push('Workflow config query style must be set before testing.');
    }

    this.workflowBinDefinitionFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    this.testingWorkflowBinQuery.set(true);
    this.workflowBinQueryTestResult.set(null);
    this.api
      .testWorkflowQuery(projectId, query, queryType, queryStyle)
      .pipe(finalize(() => this.testingWorkflowBinQuery.set(false)))
      .subscribe({
        next: (result) => {
          this.workflowBinDefinitionFormErrors.set([]);
          this.workflowBinQueryTestResult.set({
            results: (result.results ?? [])
              .map((searchResult) => searchResult.value ?? '')
              .filter((value) => value.length > 0),
            totalCount: result.totalCount ?? 0
          });
        },
        error: () => {
          this.workflowBinDefinitionFormErrors.set([
            'Bin query could not be validated.'
          ]);
        }
      });
  }

  protected formatWorkflowBinQuery(): void {
    this.workflowBinDefinitionForm.update((form) =>
      form
        ? {
            ...form,
            definition: {
              ...form.definition,
              query: this.formattedQuery(form.definition.query)
            }
          }
        : form
    );
    this.workflowBinQueryTestResult.set(null);
  }

  protected saveWorkflowBinDefinition(): void {
    const form = this.workflowBinDefinitionForm();
    const projectId = this.projectId();

    if (!form || !projectId) {
      return;
    }

    const errors = this.validateWorkflowBinDefinitionForm(form);
    this.workflowBinDefinitionFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    const payload = this.buildWorkflowBinDefinitionPayload(form);
    const request: Observable<WorkflowBinDefinition | void> =
      form.mode === 'add'
        ? this.api.addWorkflowBinDefinition(
            projectId,
            payload,
            form.positionAfterId
          )
        : this.api.updateWorkflowBinDefinition(projectId, payload);

    this.savingBinDefinition.set(true);
    request.pipe(finalize(() => this.savingBinDefinition.set(false))).subscribe({
      next: () => {
        this.workflowBinDefinitionForm.set(null);
        this.workflowBinDefinitionFormErrors.set([]);
        this.notifications.success(
          form.mode === 'add'
            ? 'Workflow bin added.'
            : 'Workflow bin saved.'
        );
        this.loadBins(form.workflowConfig.type ?? this.selectedConfigType());
      },
      error: () => {
        this.notifications.error('Workflow bin could not be saved.');
      }
    });
  }

  protected removeWorkflowBinDefinition(bin: WorkflowBin): void {
    const projectId = this.projectId();
    const type = this.selectedConfigType();

    if (!projectId || !bin.id || !bin.name || !type) {
      return;
    }

    if (!window.confirm(`Remove workflow bin "${bin.name}"?`)) {
      return;
    }

    this.deletingBinId.set(bin.id);
    this.api
      .getWorkflowBinDefinition(projectId, bin.name, type)
      .subscribe({
        next: (definition) => {
          if (!definition?.id) {
            this.deletingBinId.set(null);
            this.notifications.error('Workflow bin definition could not be loaded.');
            return;
          }

          this.api
            .removeWorkflowBinDefinition(projectId, definition.id)
            .pipe(finalize(() => this.deletingBinId.set(null)))
            .subscribe({
              next: () => {
                this.notifications.success('Workflow bin removed.');
                this.loadBins(type);
              },
              error: () => {
                this.notifications.error('Workflow bin could not be removed.');
              }
            });
        },
        error: () => {
          this.deletingBinId.set(null);
          this.notifications.error('Workflow bin definition could not be loaded.');
        }
      });
  }

  protected regenerateWorkflowBins(): void {
    const projectId = this.projectId();
    const type = this.selectedConfigType();

    if (!projectId || !type || this.binRegenerationRunning()) {
      return;
    }

    if (!window.confirm(`Regenerate all ${type} workflow bins?`)) {
      return;
    }

    this.regeneratingBins.set(true);
    this.startBinRegenerationProgressPolling();
    this.api
      .clearWorkflowBins(projectId, type)
      .pipe(
        concatMap(() => this.api.regenerateWorkflowBins(projectId, type)),
        finalize(() => {
          this.regeneratingBins.set(false);
          this.stopBinRegenerationProgressPolling();
        })
      )
      .subscribe({
        next: () => {
          this.notifications.success('Workflow bins regenerated.');
          this.loadBins(type);
        },
        error: () => {
          this.notifications.error('Workflow bins could not be regenerated.');
        }
      });
  }

  protected recomputeConceptStatus(update: boolean): void {
    const projectId = this.projectId();
    const mode = update ? 'update' : 'initialize';

    if (!projectId || this.recomputingConceptStatus()) {
      return;
    }

    if (
      !window.confirm(
        update
          ? 'Update concept status for concepts changed since the last matrix initialization?'
          : 'Initialize concept status for this project?'
      )
    ) {
      return;
    }

    this.recomputingConceptStatus.set(mode);
    this.api
      .recomputeConceptStatus(projectId, update)
      .pipe(finalize(() => this.recomputingConceptStatus.set(null)))
      .subscribe({
        next: () => {
          this.notifications.success(
            update ? 'Concept status update started.' : 'Concept status initialized.'
          );
          this.load();
        },
        error: () => {
          this.notifications.error(
            update
              ? 'Concept status could not be updated.'
              : 'Concept status could not be initialized.'
          );
        }
      });
  }

  protected regenerateWorkflowBin(bin: WorkflowBin): void {
    const projectId = this.projectId();
    const type = this.selectedConfigType();

    if (!projectId || !type || !bin.id || this.binRegenerationRunning()) {
      return;
    }

    if (!window.confirm(`Regenerate workflow bin "${bin.name || bin.id}"?`)) {
      return;
    }

    this.regeneratingBinId.set(bin.id);
    this.startBinRegenerationProgressPolling();
    this.api
      .regenerateWorkflowBin(projectId, bin.id, type)
      .pipe(
        finalize(() => {
          this.regeneratingBinId.set(null);
          this.stopBinRegenerationProgressPolling();
        })
      )
      .subscribe({
        next: () => {
          this.notifications.success('Workflow bin regenerated.');
          this.loadBins(type);
        },
        error: () => {
          this.notifications.error('Workflow bin could not be regenerated.');
        }
      });
  }

  protected toggleBinEnabled(bin: WorkflowBin): void {
    const projectId = this.projectId();
    const type = this.selectedConfigType();

    if (!projectId || !type || !bin.name) {
      return;
    }

    this.api
      .getWorkflowBinDefinition(projectId, bin.name, type)
      .subscribe({
        next: (definition) => {
          if (!definition?.id) {
            this.notifications.error('Workflow bin definition could not be loaded.');
            return;
          }

          this.api
            .updateWorkflowBinDefinition(projectId, { ...definition, enabled: !this.isBinEnabled(bin) })
            .subscribe({
              next: () => this.loadBins(type),
              error: () => this.notifications.error('Workflow bin could not be updated.')
            });
        },
        error: () => {
          this.notifications.error('Workflow bin definition could not be loaded.');
        }
      });
  }

  protected startCloneWorkflowBin(bin: WorkflowBin): void {
    this.notifications.success(`Clone bin "${bin.name || bin.id}" — not yet implemented.`);
  }

  protected startRunAutofix(bin: WorkflowBin): void {
    this.notifications.success(`Run autofix for "${bin.name || bin.id}" — not yet implemented.`);
  }

  protected startAddChecklist(bin: WorkflowBin, stats: ClusterTypeStats): void {
    if (!this.canCreateChecklistForStat(bin, stats)) {
      return;
    }

    this.checklistCreationFormErrors.set([]);
    this.checklistCreationForm.set({
      bin,
      clusterCount: 100,
      clusterType: stats.clusterType ?? 'default',
      description: '',
      excludeOnWorklist: false,
      name: '',
      skipClusterCount: 0,
      sortOrder: 'clusterId'
    });
  }

  protected closeChecklistCreationForm(): void {
    if (this.savingChecklist()) {
      return;
    }

    this.checklistCreationForm.set(null);
    this.checklistCreationFormErrors.set([]);
  }

  protected updateChecklistCreationForm(
    field: keyof Pick<
      WorkflowChecklistCreationForm,
      'description' | 'name' | 'sortOrder'
    >,
    value: string
  ): void {
    this.checklistCreationForm.update((form) =>
      form
        ? {
            ...form,
            [field]: value
          }
        : form
    );
  }

  protected updateChecklistCreationNumber(
    field: keyof Pick<
      WorkflowChecklistCreationForm,
      'clusterCount' | 'skipClusterCount'
    >,
    value: string
  ): void {
    const numericValue = value === '' ? null : Number(value);

    this.checklistCreationForm.update((form) =>
      form
        ? {
            ...form,
            [field]: Number.isNaN(numericValue) ? null : numericValue
          }
        : form
    );
  }

  protected updateChecklistCreationFlag(
    field: keyof Pick<WorkflowChecklistCreationForm, 'excludeOnWorklist'>,
    value: boolean
  ): void {
    this.checklistCreationForm.update((form) =>
      form
        ? {
            ...form,
            [field]: value
          }
        : form
    );
  }

  protected saveChecklist(): void {
    const form = this.checklistCreationForm();
    const projectId = this.projectId();

    if (!form || !projectId || !form.bin.id) {
      return;
    }

    const errors = this.validateChecklistCreationForm(form);
    this.checklistCreationFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    const clusterCount = form.clusterCount ?? 100;
    const pfs: PfsParameter = {
      maxResults: clusterCount,
      startIndex: form.skipClusterCount ?? 0
    };

    if (form.sortOrder !== 'RANDOM') {
      pfs.sortField = form.sortOrder;
    }

    this.savingChecklist.set(true);
    this.api
      .createChecklist(
        projectId,
        form.bin.id,
        form.clusterType,
        form.name.trim(),
        form.description.trim(),
        form.sortOrder === 'RANDOM',
        form.excludeOnWorklist,
        pfs
      )
      .pipe(finalize(() => this.savingChecklist.set(false)))
      .subscribe({
        next: () => {
          this.checklistCreationForm.set(null);
          this.checklistCreationFormErrors.set([]);
          this.notifications.success('Checklist created.');
          this.load();
        },
        error: () => {
          this.checklistCreationFormErrors.set(['Checklist could not be created.']);
        }
      });
  }

  protected startAddWorklist(bin: WorkflowBin, stats: ClusterTypeStats): void {
    if (!this.canCreateWorklistForStat(bin, stats)) {
      return;
    }

    this.worklistCreationFormErrors.set([]);
    this.worklistCreationForm.set({
      availableClusterCount: this.binStatValue(stats, 'unassigned'),
      bin,
      clusterCount: 100,
      clusterType: stats.clusterType ?? 'default',
      numberOfWorklists: 1,
      skipClusterCount: 0,
      sortOrder: 'clusterId'
    });
  }

  protected closeWorklistCreationForm(): void {
    if (this.savingWorklist()) {
      return;
    }

    this.worklistCreationForm.set(null);
    this.worklistCreationFormErrors.set([]);
  }

  protected updateWorklistCreationForm(
    field: keyof Pick<WorkflowWorklistCreationForm, 'sortOrder'>,
    value: string
  ): void {
    this.worklistCreationForm.update((form) =>
      form
        ? {
            ...form,
            [field]: value as WorkflowListSortOrder
          }
        : form
    );
  }

  protected updateWorklistCreationNumber(
    field: keyof Pick<
      WorkflowWorklistCreationForm,
      'clusterCount' | 'numberOfWorklists' | 'skipClusterCount'
    >,
    value: string
  ): void {
    const numericValue = value === '' ? null : Number(value);

    this.worklistCreationForm.update((form) =>
      form
        ? {
            ...form,
            [field]: Number.isNaN(numericValue) ? null : numericValue
          }
        : form
    );
  }

  protected saveWorklist(): void {
    const form = this.worklistCreationForm();
    const projectId = this.projectId();

    if (!form || !projectId || !form.bin.id) {
      return;
    }

    const errors = this.validateWorklistCreationForm(form);
    this.worklistCreationFormErrors.set(errors);

    if (errors.length) {
      return;
    }

    const clusterCount = form.clusterCount ?? 100;
    let numberOfWorklists = form.numberOfWorklists ?? 0;

    if (
      numberOfWorklists > 10 &&
      !window.confirm(`Create ${numberOfWorklists} worklists?`)
    ) {
      return;
    }

    if (numberOfWorklists * clusterCount > form.availableClusterCount) {
      numberOfWorklists = Math.ceil(
        Math.max(0, form.availableClusterCount - (form.skipClusterCount ?? 0)) /
          clusterCount
      );
    }

    if (numberOfWorklists < 1) {
      this.worklistCreationFormErrors.set([
        'No unassigned clusters are available for the requested worklist range.'
      ]);
      return;
    }

    const pfs: PfsParameter = {
      maxResults: clusterCount,
      sortField: form.sortOrder,
      startIndex: form.skipClusterCount ?? 0
    };

    this.savingWorklist.set(true);
    range(0, numberOfWorklists)
      .pipe(
        concatMap(() =>
          this.api.createWorklist(projectId, form.bin.id!, form.clusterType, pfs)
        ),
        toArray(),
        finalize(() => this.savingWorklist.set(false))
      )
      .subscribe({
        next: (worklists) => {
          this.worklistCreationForm.set(null);
          this.worklistCreationFormErrors.set([]);
          this.notifications.success(
            worklists.length === 1
              ? 'Worklist created.'
              : `${worklists.length} worklists created.`
          );
          this.load();
        },
        error: () => {
          this.worklistCreationFormErrors.set(['Worklist could not be created.']);
        }
      });
  }

  protected startImportWorkflowConfig(): void {
    this.importWorkflowFile.set(null);
    this.importWorkflowFormErrors.set([]);
    this.importWorkflowDialogOpen.set(true);
  }

  protected closeImportWorkflowConfig(): void {
    if (this.importingWorkflowConfig()) {
      return;
    }

    this.importWorkflowDialogOpen.set(false);
    this.importWorkflowFile.set(null);
    this.importWorkflowFormErrors.set([]);
  }

  protected setImportWorkflowFile(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.importWorkflowFile.set(input.files?.item(0) ?? null);
    this.importWorkflowFormErrors.set([]);
  }

  protected importWorkflowConfig(): void {
    const projectId = this.projectId();
    const file = this.importWorkflowFile();

    if (!projectId) {
      return;
    }

    if (!file) {
      this.importWorkflowFormErrors.set(['Choose a workflow config file to import.']);
      return;
    }

    this.importingWorkflowConfig.set(true);
    this.api
      .importWorkflowConfig(projectId, file)
      .pipe(finalize(() => this.importingWorkflowConfig.set(false)))
      .subscribe({
        next: (workflowConfig) => {
          if (workflowConfig.type) {
            this.selectedConfigType.set(workflowConfig.type);
          }

          this.importWorkflowDialogOpen.set(false);
          this.importWorkflowFile.set(null);
          this.importWorkflowFormErrors.set([]);
          this.notifications.success('Workflow config imported.');
          this.load();
        },
        error: () => {
          this.importWorkflowFormErrors.set([
            'Workflow config could not be imported.'
          ]);
        }
      });
  }

  protected startImportChecklist(): void {
    this.importChecklistName.set('');
    this.importChecklistFile.set(null);
    this.importChecklistFormErrors.set([]);
    this.importChecklistDialogOpen.set(true);
  }

  protected startComputeChecklist(): void {
    this.checklistComputeFormErrors.set([]);
    this.checklistComputeQueryTestResult.set(null);
    this.checklistComputeForm.set({
      clusterCount: 100,
      name: '',
      query: '',
      queryPreview: false,
      queryType: 'SQL',
      skipClusterCount: 0
    });
  }

  protected closeImportChecklist(): void {
    if (this.importingChecklist()) {
      return;
    }

    this.importChecklistDialogOpen.set(false);
    this.importChecklistName.set('');
    this.importChecklistFile.set(null);
    this.importChecklistFormErrors.set([]);
  }

  protected closeComputeChecklist(): void {
    if (this.computingChecklist() || this.testingChecklistComputeQuery()) {
      return;
    }

    this.checklistComputeForm.set(null);
    this.checklistComputeFormErrors.set([]);
    this.checklistComputeQueryTestResult.set(null);
  }

  protected setImportChecklistName(value: string): void {
    this.importChecklistName.set(value);
    this.importChecklistFormErrors.set([]);
  }

  protected setImportChecklistFile(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.importChecklistFile.set(input.files?.item(0) ?? null);
    this.importChecklistFormErrors.set([]);
  }

  protected updateChecklistComputeForm(
    field: keyof Pick<WorkflowChecklistComputeForm, 'name' | 'query' | 'queryType'>,
    value: string
  ): void {
    this.checklistComputeForm.update((form) =>
      form
        ? {
            ...form,
            [field]: value
          }
        : form
    );
    this.checklistComputeFormErrors.set([]);
    this.checklistComputeQueryTestResult.set(null);
  }

  protected updateChecklistComputeNumber(
    field: keyof Pick<
      WorkflowChecklistComputeForm,
      'clusterCount' | 'skipClusterCount'
    >,
    value: string
  ): void {
    const numericValue = value === '' ? null : Number(value);

    this.checklistComputeForm.update((form) =>
      form
        ? {
            ...form,
            [field]: Number.isNaN(numericValue) ? null : numericValue
          }
        : form
    );
    this.checklistComputeFormErrors.set([]);
  }

  protected toggleChecklistComputeQueryPreview(): void {
    this.checklistComputeForm.update((form) =>
      form
        ? {
            ...form,
            queryPreview: !form.queryPreview
          }
        : form
    );
  }

  protected formattedQuery(value: string | null | undefined): string {
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

  protected testChecklistComputeQuery(): void {
    const form = this.checklistComputeForm();
    const projectId = this.projectId();
    const queryStyle = this.selectedWorkflowConfig()?.queryStyle?.trim() ?? '';

    if (!form || !projectId) {
      return;
    }

    const errors = this.validateChecklistComputeForm(form, false);

    if (!queryStyle) {
      errors.push('Workflow config query style must be set before testing.');
    }

    this.checklistComputeFormErrors.set(errors);
    this.checklistComputeQueryTestResult.set(null);

    if (errors.length) {
      return;
    }

    this.testingChecklistComputeQuery.set(true);
    this.api
      .testWorkflowQuery(projectId, form.query.trim(), form.queryType, queryStyle)
      .pipe(finalize(() => this.testingChecklistComputeQuery.set(false)))
      .subscribe({
        next: (result) => {
          this.checklistComputeFormErrors.set([]);
          this.checklistComputeQueryTestResult.set({
            results: (result.results ?? [])
              .map((item) => String(item))
              .filter(Boolean)
              .slice(0, 5),
            totalCount: result.totalCount ?? result.results?.length ?? 0
          });
        },
        error: () => {
          this.checklistComputeFormErrors.set(['Checklist query could not be tested.']);
        }
      });
  }

  protected importChecklist(): void {
    const projectId = this.projectId();
    const name = this.importChecklistName().trim();
    const file = this.importChecklistFile();
    const errors: string[] = [];

    if (!projectId) {
      return;
    }

    if (!name) {
      errors.push('Checklist name is required.');
    }

    if (!file) {
      errors.push('Choose a checklist file to import.');
    }

    if (errors.length) {
      this.importChecklistFormErrors.set(errors);
      return;
    }

    const duplicateQuery = `nameSort:"${name.replace(/"/g, '\\"')}"`;
    const duplicatePfs = buildOperationalPfs(1, 1, 'name', true, duplicateQuery);

    this.importingChecklist.set(true);
    this.api
      .findChecklists(projectId, duplicateQuery, duplicatePfs)
      .pipe(
        concatMap((existing) => {
          if (existing.totalCount > 0) {
            this.importChecklistFormErrors.set([
              'Checklist with this name already exists.'
            ]);
            return of(null);
          }

          return this.api.importChecklist(projectId, name, file!);
        }),
        finalize(() => this.importingChecklist.set(false))
      )
      .subscribe({
        next: (checklist) => {
          if (!checklist) {
            return;
          }

          this.importChecklistDialogOpen.set(false);
          this.importChecklistName.set('');
          this.importChecklistFile.set(null);
          this.importChecklistFormErrors.set([]);
          this.notifications.success('Checklist imported.');
          this.load();
        },
        error: () => {
          this.importChecklistFormErrors.set(['Checklist could not be imported.']);
        }
      });
  }

  protected computeChecklist(): void {
    const form = this.checklistComputeForm();
    const projectId = this.projectId();

    if (!form || !projectId) {
      return;
    }

    const errors = this.validateChecklistComputeForm(form, true);
    this.checklistComputeFormErrors.set(errors);
    this.checklistComputeQueryTestResult.set(null);

    if (errors.length) {
      return;
    }

    const name = form.name.trim();
    const duplicateQuery = `nameSort:"${name.replace(/"/g, '\\"')}"`;
    const duplicatePfs = buildOperationalPfs(1, 1, 'name', true, duplicateQuery);
    const pfs: PfsParameter = {
      maxResults: form.clusterCount ?? 100,
      startIndex: form.skipClusterCount ?? 0
    };

    this.computingChecklist.set(true);
    this.api
      .findChecklists(projectId, duplicateQuery, duplicatePfs)
      .pipe(
        concatMap((existing) => {
          if (existing.totalCount > 0) {
            this.checklistComputeFormErrors.set([
              'Checklist with this name already exists.'
            ]);
            return of(null);
          }

          return this.api.computeChecklist(
            projectId,
            name,
            form.query.trim(),
            form.queryType,
            pfs
          );
        }),
        finalize(() => this.computingChecklist.set(false))
      )
      .subscribe({
        next: (checklist) => {
          if (!checklist) {
            return;
          }

          this.checklistComputeForm.set(null);
          this.checklistComputeFormErrors.set([]);
          this.checklistComputeQueryTestResult.set(null);
          this.notifications.success('Checklist computed.');
          this.load();
        },
        error: () => {
          this.checklistComputeFormErrors.set(['Checklist could not be computed.']);
        }
      });
  }

  protected exportWorkflowConfig(config: WorkflowConfig): void {
    const projectId = this.projectId();

    if (!projectId || !config.id) {
      return;
    }

    this.exportingConfigId.set(config.id);
    this.api
      .exportWorkflowConfig(projectId, config.id)
      .pipe(finalize(() => this.exportingConfigId.set(null)))
      .subscribe({
        next: (blob) => {
          this.downloadBlob(blob, this.workflowExportFileName(config));
          this.notifications.success('Workflow config exported.');
        },
        error: () => {
          this.notifications.error('Workflow config could not be exported.');
        }
      });
  }

  protected viewWorkflowItem(kind: WorkflowListKind, item: Checklist | Worklist, afterLoad?: () => void): void {
    const projectId = this.projectId();

    if (!projectId || !item.id) {
      return;
    }

    const itemKey = this.workflowItemKey(kind, item);
    const detailRequest: Observable<Checklist | Worklist> =
      kind === 'checklist'
        ? this.api.getChecklist(projectId, item.id)
        : this.api.getWorklist(projectId, item.id);

    this.loadingWorkflowItemKey.set(itemKey);
    forkJoin({
      item: detailRequest,
      log: this.api.getWorkflowLog(
        projectId,
        kind === 'checklist' ? item.id : null,
        kind === 'worklist' ? item.id : null
      )
    })
      .pipe(finalize(() => this.loadingWorkflowItemKey.set(null)))
      .subscribe({
        next: ({ item: detailItem, log }) => {
          this.selectedWorkflowAssignmentUserName.set('');
          this.assignmentNoteText.set('');
          this.workflowListDetail.set({
            item: detailItem,
            kind,
            log
          });
          this.loadListDetailRecords(kind, detailItem);

          if (kind === 'worklist') {
            this.loadGeneratedConceptReport(detailItem);
          } else {
            this.workflowReportFileName.set(null);
          }

          afterLoad?.();
        },
        error: () => {
          this.notifications.error(`${this.workflowItemLabel(kind)} details could not be loaded.`);
        }
      });
  }

  protected closeWorkflowListDetail(): void {
    this.workflowListDetail.set(null);
    this.selectedWorkflowAssignmentUserName.set('');
    this.assignmentNoteText.set('');
    this.workflowNoteText.set('');
    this.workflowReportFileName.set(null);
    this.listDetailRecords.set([]);
    this.listDetailRecordsTotal.set(0);
    this.listDetailRecordsPage.set(1);
    this.selectedListDetailConcept.set(null);
    this.listDetailReportMode.set('Report');
    this.assignmentDialogOpen.set(false);
    this.notesDialogOpen.set(false);
    this.logDialogOpen.set(false);
    this.conceptReportsDialogOpen.set(false);
  }

  protected setWorkflowNoteText(value: string): void {
    this.workflowNoteText.set(value);
  }

  protected setWorkflowAssignmentUserName(value: string): void {
    this.selectedWorkflowAssignmentUserName.set(value);
  }

  protected setAssignmentNoteText(value: string): void {
    this.assignmentNoteText.set(value);
  }

  protected workflowAssignmentTargetRole(
    worklist: Checklist & Partial<Worklist>
  ): WorkflowAssignmentRole | null {
    if (this.isAuthorAssignmentAvailable(worklist)) {
      return 'AUTHOR';
    }

    if (this.isReviewerAssignmentAvailable(worklist)) {
      return 'REVIEWER';
    }

    return null;
  }

  protected assignableWorkflowUsers(
    worklist: Checklist & Partial<Worklist>
  ): OperationalUser[] {
    const role = this.workflowAssignmentTargetRole(worklist);
    const project = this.selectedProject();

    if (!role || !project || !this.canManageWorklistAssignments()) {
      return [];
    }

    const assignedNames = new Set(
      [...(worklist.authors ?? []), ...(worklist.reviewers ?? [])].map((name) =>
        name.toLocaleLowerCase()
      )
    );

    return this.workflowProjectUsers()
      .filter((user) => {
        const userName = user.userName ?? '';

        if (!userName || assignedNames.has(userName.toLocaleLowerCase())) {
          return false;
        }

        if (
          project.teamBased &&
          worklist.team &&
          user.team !== worklist.team
        ) {
          return false;
        }

        if (role === 'REVIEWER') {
          return this.workflowProjectRoleForUser(user) !== 'AUTHOR';
        }

        return true;
      })
      .sort((left, right) =>
        (left.userName ?? '').localeCompare(right.userName ?? '')
      );
  }

  protected assignedWorkflowUsers(
    worklist: Checklist & Partial<Worklist>,
    role: WorkflowAssignmentRole
  ): string[] {
    return role === 'AUTHOR' ? worklist.authors ?? [] : worklist.reviewers ?? [];
  }

  protected canRemoveAssignedWorkflowUser(
    worklist: Checklist & Partial<Worklist>,
    userName: string,
    role: WorkflowAssignmentRole
  ): boolean {
    return (
      this.projectContext.projectRole() === 'ADMINISTRATOR' &&
      Boolean(worklist.id) &&
      Boolean(userName) &&
      !this.isWorkflowComplete(worklist) &&
      !this.isWorkflowAssignmentRunning(worklist) &&
      this.includesAssignmentName(
        this.assignedWorkflowUsers(worklist, role),
        userName.toLocaleLowerCase()
      )
    );
  }

  protected canReassignAssignedWorkflowUser(
    worklist: Checklist & Partial<Worklist>,
    userName: string,
    role: WorkflowAssignmentRole
  ): boolean {
    if (
      !this.canManageWorklistAssignments() ||
      !worklist.id ||
      !userName ||
      this.isWorkflowAssignmentRunning(worklist)
    ) {
      return false;
    }

    const assignedUsers = this.assignedWorkflowUsers(worklist, role);
    const currentUserName = this.currentUserName().toLocaleLowerCase();

    if (
      assignedUsers.length !== 1 ||
      !this.includesAssignmentName(assignedUsers, userName.toLocaleLowerCase())
    ) {
      return false;
    }

    if (
      userName.toLocaleLowerCase() === currentUserName &&
      this.canReassignWorklist(worklist)
    ) {
      return false;
    }

    return (
      (role === 'AUTHOR' && worklist.workflowStatus === 'EDITING_DONE') ||
      (role === 'REVIEWER' &&
        worklist.workflowStatus === 'READY_FOR_PUBLICATION')
    );
  }

  protected assignWorkflowToSelectedUser(
    worklist: Checklist & Partial<Worklist>
  ): void {
    const projectId = this.projectId();
    const userName = this.selectedWorkflowAssignmentUserName();
    const role = this.workflowAssignmentTargetRole(worklist);

    if (!projectId || !worklist.id || !userName || !role) {
      return;
    }

    const itemKey = this.workflowItemKey('worklist', worklist);
    const note = this.assignmentNoteText().trim();

    this.actingWorkflowItemKey.set(itemKey);
    this.api
      .performWorkflowAction(projectId, worklist.id, userName, role, 'ASSIGN')
      .pipe(
        concatMap((updatedWorklist) =>
          note
            ? this.api.addWorklistNote(projectId, worklist.id!, note).pipe(
                map((newNote) => ({
                  newNote,
                  updatedWorklist
                }))
              )
            : of({
                newNote: null,
                updatedWorklist
              })
        ),
        finalize(() => this.actingWorkflowItemKey.set(null))
      )
      .subscribe({
        next: ({ newNote, updatedWorklist }) => {
          const updatedWithNote = newNote
            ? {
                ...updatedWorklist,
                notes: [...(updatedWorklist.notes ?? worklist.notes ?? []), newNote]
              }
            : updatedWorklist;

          this.replaceWorklist(updatedWithNote);
          this.selectedWorkflowAssignmentUserName.set('');
          this.assignmentNoteText.set('');
          this.notifications.success(`Worklist assigned to ${userName}.`);
          this.load();
        },
        error: () => {
          this.notifications.error(`Worklist could not be assigned to ${userName}.`);
        }
      });
  }

  protected removeAssignedWorkflowUser(
    worklist: Checklist & Partial<Worklist>,
    userName: string,
    role: WorkflowAssignmentRole
  ): void {
    const projectId = this.projectId();

    if (!projectId || !worklist.id || !this.canRemoveAssignedWorkflowUser(worklist, userName, role)) {
      return;
    }

    if (!window.confirm(`Remove ${userName} from ${role.toLocaleLowerCase()} assignments for worklist "${worklist.name || worklist.id}"?`)) {
      return;
    }

    const itemKey = this.workflowItemKey('worklist', worklist);

    this.actingWorkflowItemKey.set(itemKey);
    this.api
      .performWorkflowAction(projectId, worklist.id, userName, 'ADMINISTRATOR', 'UNASSIGN')
      .pipe(finalize(() => this.actingWorkflowItemKey.set(null)))
      .subscribe({
        next: (updatedWorklist) => {
          this.replaceWorklist(updatedWorklist);
          this.notifications.success(`Worklist unassigned from ${userName}.`);
          this.load();
        },
        error: () => {
          this.notifications.error(`Worklist could not be unassigned from ${userName}.`);
        }
      });
  }

  protected reassignAssignedWorkflowUser(
    worklist: Checklist & Partial<Worklist>,
    userName: string,
    role: WorkflowAssignmentRole
  ): void {
    const projectId = this.projectId();

    if (
      !projectId ||
      !worklist.id ||
      !this.canReassignAssignedWorkflowUser(worklist, userName, role)
    ) {
      return;
    }

    if (
      !window.confirm(
        `Reassign worklist "${worklist.name || worklist.id}" back to ${userName}?`
      )
    ) {
      return;
    }

    const itemKey = this.workflowItemKey('worklist', worklist);

    this.actingWorkflowItemKey.set(itemKey);
    this.api
      .performWorkflowAction(projectId, worklist.id, userName, role, 'REASSIGN')
      .pipe(finalize(() => this.actingWorkflowItemKey.set(null)))
      .subscribe({
        next: (updatedWorklist) => {
          this.replaceWorklist(updatedWorklist);
          this.notifications.success(`Worklist reassigned to ${userName}.`);
          this.load();
        },
        error: () => {
          this.notifications.error(`Worklist could not be reassigned to ${userName}.`);
        }
      });
  }

  protected workflowNotes(item: Checklist & Partial<Worklist>): WorkflowNote[] {
    return [...(item.notes ?? [])].sort(
      (left, right) => this.workflowNoteTime(right) - this.workflowNoteTime(left)
    );
  }

  protected addWorkflowNote(detail: WorkflowListDetail): void {
    const projectId = this.projectId();
    const note = this.workflowNoteText().trim();

    if (!projectId || !detail.item.id || !note || !this.canManageWorkflowNotes()) {
      return;
    }

    const itemKey = this.workflowItemKey(detail.kind, detail.item);
    const request =
      detail.kind === 'checklist'
        ? this.api.addChecklistNote(projectId, detail.item.id, note)
        : this.api.addWorklistNote(projectId, detail.item.id, note);

    this.addingWorkflowNoteKey.set(itemKey);
    request.pipe(finalize(() => this.addingWorkflowNoteKey.set(null))).subscribe({
      next: (newNote) => {
        this.workflowNoteText.set('');
        this.updateWorkflowDetailNotes(detail, [
          ...(detail.item.notes ?? []),
          newNote
        ]);
        this.notifications.success(`${this.workflowItemLabel(detail.kind)} note added.`);
      },
      error: () => {
        this.notifications.error(`${this.workflowItemLabel(detail.kind)} note could not be added.`);
      }
    });
  }

  protected removeWorkflowNote(
    detail: WorkflowListDetail,
    note: WorkflowNote
  ): void {
    const projectId = this.projectId();

    if (!projectId || !note.id || !this.canManageWorkflowNotes()) {
      return;
    }

    if (!window.confirm('Remove this note?')) {
      return;
    }

    const request =
      detail.kind === 'checklist'
        ? this.api.removeChecklistNote(projectId, note.id)
        : this.api.removeWorklistNote(projectId, note.id);

    this.removingWorkflowNoteId.set(note.id);
    request.pipe(finalize(() => this.removingWorkflowNoteId.set(null))).subscribe({
      next: () => {
        this.updateWorkflowDetailNotes(
          detail,
          (detail.item.notes ?? []).filter((existingNote) => existingNote.id !== note.id)
        );
        this.notifications.success(`${this.workflowItemLabel(detail.kind)} note removed.`);
      },
      error: () => {
        this.notifications.error(`${this.workflowItemLabel(detail.kind)} note could not be removed.`);
      }
    });
  }

  protected generateWorkflowReport(worklist: Checklist & Partial<Worklist>): void {
    const projectId = this.projectId();

    if (!projectId || !worklist.id) {
      return;
    }

    const itemKey = this.workflowItemKey('worklist', worklist);
    this.generatingWorkflowReportKey.set(itemKey);
    this.api
      .generateConceptReport(projectId, worklist.id)
      .pipe(finalize(() => this.generatingWorkflowReportKey.set(null)))
      .subscribe({
        next: (fileName) => {
          this.workflowReportFileName.set(
            fileName?.trim() || this.expectedWorkflowReportFileName(worklist)
          );
          this.notifications.success('Concept report generated.');
        },
        error: () => {
          this.notifications.error('Concept report could not be generated.');
        }
      });
  }

  protected downloadWorkflowReport(worklist: Checklist & Partial<Worklist>): void {
    const projectId = this.projectId();
    const fileName =
      this.workflowReportFileName() ?? this.expectedWorkflowReportFileName(worklist);

    if (!projectId || !fileName) {
      return;
    }

    const itemKey = this.workflowItemKey('worklist', worklist);
    this.downloadingWorkflowReportKey.set(itemKey);
    this.api
      .getGeneratedConceptReport(projectId, fileName)
      .pipe(finalize(() => this.downloadingWorkflowReportKey.set(null)))
      .subscribe({
        next: (blob) => {
          this.downloadBlob(blob, fileName);
          this.notifications.success('Concept report downloaded.');
        },
        error: () => {
          this.notifications.error('Concept report could not be downloaded.');
        }
      });
  }

  protected removeWorkflowReport(worklist: Checklist & Partial<Worklist>): void {
    const projectId = this.projectId();
    const fileName =
      this.workflowReportFileName() ?? this.expectedWorkflowReportFileName(worklist);

    if (!projectId || !fileName) {
      return;
    }

    if (!window.confirm(`Remove concept report "${fileName}"?`)) {
      return;
    }

    const itemKey = this.workflowItemKey('worklist', worklist);
    this.removingWorkflowReportKey.set(itemKey);
    this.api
      .removeGeneratedConceptReport(projectId, fileName)
      .pipe(finalize(() => this.removingWorkflowReportKey.set(null)))
      .subscribe({
        next: () => {
          this.workflowReportFileName.set(null);
          this.notifications.success('Concept report removed.');
        },
        error: () => {
          this.notifications.error('Concept report could not be removed.');
        }
      });
  }

  protected exportWorkflowItem(kind: WorkflowListKind, item: Checklist | Worklist): void {
    const projectId = this.projectId();

    if (!projectId || !item.id) {
      return;
    }

    const itemKey = this.workflowItemKey(kind, item);
    const request =
      kind === 'checklist'
        ? this.api.exportChecklist(projectId, item.id)
        : this.api.exportWorklist(projectId, item.id);

    this.exportingWorkflowItemKey.set(itemKey);
    request.pipe(finalize(() => this.exportingWorkflowItemKey.set(null))).subscribe({
      next: (blob) => {
        this.downloadBlob(blob, this.workflowListExportFileName(kind, item));
        this.notifications.success(`${this.workflowItemLabel(kind)} exported.`);
      },
      error: () => {
        this.notifications.error(`${this.workflowItemLabel(kind)} could not be exported.`);
      }
    });
  }

  protected removeWorkflowItem(kind: WorkflowListKind, item: Checklist | Worklist): void {
    const projectId = this.projectId();

    if (!projectId || !item.id || !this.canManageWorkflowListActions()) {
      return;
    }

    if (!window.confirm(`Remove ${this.workflowItemLabel(kind).toLocaleLowerCase()} "${item.name || item.id}"?`)) {
      return;
    }

    const itemKey = this.workflowItemKey(kind, item);
    const request =
      kind === 'checklist'
        ? this.api.removeChecklist(projectId, item.id)
        : this.api.removeWorklist(projectId, item.id);

    this.deletingWorkflowItemKey.set(itemKey);
    request.pipe(finalize(() => this.deletingWorkflowItemKey.set(null))).subscribe({
      next: () => {
        const detail = this.workflowListDetail();

        if (detail?.kind === kind && detail.item.id === item.id) {
          this.workflowListDetail.set(null);
        }

        this.notifications.success(`${this.workflowItemLabel(kind)} removed.`);
        this.load();
      },
      error: () => {
        this.notifications.error(`${this.workflowItemLabel(kind)} could not be removed.`);
      }
      });
  }

  protected workflowListActions(
    kind: WorkflowListKind,
    item: Checklist & Partial<Worklist>
  ): WorkflowLifecycleButton[] {
    const actions: WorkflowLifecycleButton[] = [];

    if (this.canStampWorkflowItem(kind, item)) {
      actions.push({ action: 'STAMP', label: 'Stamp' });
    }

    if (kind === 'worklist' && this.canFinishWorklist(item)) {
      actions.push({ action: 'FINISH', label: 'Finish' });
    }

    if (this.canUnapproveWorkflowItem(kind, item)) {
      actions.push({ action: 'UNAPPROVE', label: 'Unapprove' });
    }

    return actions;
  }

  protected workflowAssignmentActions(
    worklist: Checklist & Partial<Worklist>
  ): WorkflowAssignmentAction[] {
    const actions: WorkflowAssignmentAction[] = [];

    if (this.canAssignWorklist(worklist)) {
      actions.push({ action: 'ASSIGN', label: 'Assign to me' });
    }

    if (this.canUnassignWorklist(worklist)) {
      actions.push({ action: 'UNASSIGN', label: 'Unassign me' });
    }

    if (this.canReassignWorklist(worklist)) {
      actions.push({ action: 'REASSIGN', label: 'Reassign' });
    }

    return actions;
  }
  protected performWorkflowAssignment(
    worklist: Checklist & Partial<Worklist>,
    action: WorkflowAction
  ): void {
    const projectId = this.projectId();
    const userName = this.currentUserName();

    if (!projectId || !worklist.id || !userName) {
      return;
    }

    const itemKey = this.workflowItemKey('worklist', worklist);
    const userRole = this.workflowActionRole(worklist, action);

    this.actingWorkflowItemKey.set(itemKey);
    this.api
      .performWorkflowAction(projectId, worklist.id, userName, userRole, action)
      .pipe(finalize(() => this.actingWorkflowItemKey.set(null)))
      .subscribe({
        next: (updatedWorklist) => {
          this.replaceWorklist(updatedWorklist);
          this.notifications.success(this.workflowAssignmentSuccessMessage(action));
          this.load();
        },
        error: () => {
          this.notifications.error(this.workflowAssignmentErrorMessage(action));
        }
      });
  }

  protected performWorkflowListAction(
    kind: WorkflowListKind,
    item: Checklist & Partial<Worklist>,
    action: WorkflowLifecycleAction
  ): void {
    if (action === 'FINISH' && kind === 'worklist') {
      this.openFinishWorkflowForm(item);
      return;
    }

    const projectId = this.projectId();
    const userName = this.currentUserName();

    if (!projectId || !item.id) {
      return;
    }

    if (action === 'FINISH' && (!userName || kind !== 'worklist')) {
      return;
    }

    const label = this.workflowItemActionLabel(action);
    const itemLabel = this.workflowItemLabel(kind).toLocaleLowerCase();

    if (!window.confirm(`${label} ${itemLabel} "${item.name || item.id}"?`)) {
      return;
    }

    const itemKey = this.workflowItemKey(kind, item);
    const request: Observable<void | Worklist> = this.workflowItemActionRequest(
      projectId,
      kind,
      item,
      action,
      userName
    );

    this.actingWorkflowItemKey.set(itemKey);
    request.pipe(finalize(() => this.actingWorkflowItemKey.set(null))).subscribe({
      next: (updatedWorklist) => {
        if (kind === 'worklist' && updatedWorklist) {
          this.replaceWorklist(updatedWorklist as Worklist);
        }

        this.notifications.success(
          `${this.workflowItemLabel(kind)} ${this.workflowItemActionPastTense(action)}.`
        );
        this.load();
      },
      error: () => {
        this.notifications.error(
          `${this.workflowItemLabel(kind)} could not be ${this.workflowItemActionPastTense(action)}.`
        );
      }
    });
  }

  protected openFinishWorkflowForm(worklist: Checklist & Partial<Worklist>): void {
    if (!worklist.id || !this.currentUserName()) {
      return;
    }

    this.finishWorkflowForm.set({
      errors: [],
      hours: null,
      minutes: null,
      role: this.finishWorkflowRole(worklist),
      worklist
    });
  }

  protected closeFinishWorkflowForm(): void {
    if (this.finishWorkflowFormRunning()) {
      return;
    }

    this.finishWorkflowForm.set(null);
  }

  protected updateFinishWorkflowForm(
    field: 'hours' | 'minutes',
    value: string | number | null
  ): void {
    const form = this.finishWorkflowForm();

    if (!form) {
      return;
    }

    const parsedValue = value === '' || value === null ? null : Number(value);
    const nextValue =
      typeof parsedValue === 'number' && Number.isFinite(parsedValue)
        ? parsedValue
        : null;
    this.finishWorkflowForm.set({
      ...form,
      errors: [],
      [field]: nextValue
    });
  }

  protected finishWorkflowFormRunning(): boolean {
    const form = this.finishWorkflowForm();

    return Boolean(
      form?.worklist && this.actingWorkflowItemKey() === this.workflowItemKey('worklist', form.worklist)
    );
  }

  protected submitFinishWorkflowForm(): void {
    const projectId = this.projectId();
    const userName = this.currentUserName();
    const form = this.finishWorkflowForm();

    if (!projectId || !userName || !form?.worklist.id) {
      return;
    }

    const hours = form.hours ?? 0;
    const minutes = form.minutes ?? 0;
    const errors: string[] = [];

    if (hours < 0) {
      errors.push('Invalid number of hours, < 0');
    }
    if (hours > 23) {
      errors.push('Invalid number of hours, > 24');
    }
    if (minutes < 0) {
      errors.push('Invalid number of minutes, < 0');
    }
    if (minutes > 59) {
      errors.push('Invalid number of minutes, > 59');
    }
    if (!hours && !minutes) {
      errors.push('Time spent is required.');
    }

    if (errors.length) {
      this.finishWorkflowForm.set({
        ...form,
        errors
      });
      return;
    }

    const seconds = hours * 60 * 60 + minutes * 60;
    const updatedWorklist: Worklist = {
      ...form.worklist,
      authorTime: form.role === 'AUTHOR' ? seconds : form.worklist.authorTime,
      reviewerTime: form.role === 'REVIEWER' ? seconds : form.worklist.reviewerTime
    };
    const itemKey = this.workflowItemKey('worklist', form.worklist);

    this.actingWorkflowItemKey.set(itemKey);
    this.api
      .updateWorklist(projectId, updatedWorklist)
      .pipe(
        concatMap(() =>
          this.api.performWorkflowAction(
            projectId,
            form.worklist.id!,
            userName,
            form.role,
            'FINISH'
          )
        ),
        finalize(() => this.actingWorkflowItemKey.set(null))
      )
      .subscribe({
        next: (finishedWorklist) => {
          this.finishWorkflowForm.set(null);
          this.replaceWorklist(finishedWorklist);
          this.notifications.success('Worklist finished.');
          this.load();
        },
        error: () => {
          this.finishWorkflowForm.set({
            ...form,
            errors: ['Worklist could not be finished.']
          });
          this.notifications.error('Worklist could not be finished.');
        }
      });
  }

  protected assignmentDescription(worklist: Checklist & Partial<Worklist>): string {
    const details: string[] = [];

    if (this.isAuthorAssignmentAvailable(worklist)) {
      details.push('Available for author assignment');
    }

    if (this.isReviewerAssignmentAvailable(worklist)) {
      details.push('Available for reviewer assignment');
    }

    return details.length ? details.join('; ') : 'n/a';
  }

  protected joinAssignmentNames(names: string[] | null | undefined): string {
    return names?.length ? names.join(', ') : 'n/a';
  }

  protected joinEditors(worklist: Partial<Worklist>): string {
    const authors = worklist.authors?.filter(Boolean) ?? [];
    const reviewers = worklist.reviewers?.filter(Boolean) ?? [];
    return [...authors, ...reviewers].join(', ');
  }

  protected latestWorkflowState(
    worklist: Checklist & Partial<Worklist>
  ): string {
    const entries = Object.entries(worklist.workflowStateHistory ?? {});

    if (!entries.length) {
      return 'n/a';
    }

    return entries.reduce(
      (latest, entry) =>
        this.workflowStateTime(entry[1]) > this.workflowStateTime(latest[1])
          ? entry
          : latest,
      entries[0]
    )[0];
  }

  protected isWorkflowAssignmentRunning(
    worklist: Checklist & Partial<Worklist>
  ): boolean {
    return this.actingWorkflowItemKey() === this.workflowItemKey('worklist', worklist);
  }

  protected isWorkflowItemActionRunning(
    kind: WorkflowListKind,
    item: Checklist & Partial<Worklist>
  ): boolean {
    return this.actingWorkflowItemKey() === this.workflowItemKey(kind, item);
  }

  protected workflowItemKey(
    kind: WorkflowListKind,
    item: { id?: number | null }
  ): string {
    return `${kind}:${item.id ?? 'new'}`;
  }

  protected workflowItemLabel(kind: WorkflowListKind): string {
    return kind === 'checklist' ? 'Checklist' : 'Worklist';
  }

  protected isWorkflowItemSelected(
    kind: WorkflowListKind,
    item: { id?: number | null }
  ): boolean {
    const detail = this.workflowListDetail();

    return detail?.kind === kind && detail.item.id === item.id;
  }

  protected workflowListDetailTitle(detail: WorkflowListDetail): string {
    return `${this.workflowItemLabel(detail.kind)}: ${detail.item.name || detail.item.id || 'Details'}`;
  }

  protected statEntries(item: Checklist | Worklist | null): Array<{
    key: string;
    value: number;
  }> {
    return Object.entries(item?.stats ?? {})
      .filter((entry): entry is [string, number] => typeof entry[1] === 'number')
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([key, value]) => ({ key, value }));
  }

  protected displayDate(value: string | number | null | undefined): string {
    if (!value) {
      return 'n/a';
    }

    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
  }

  protected recordCount(recordHolder: Checklist | Worklist | null): number {
    return recordHolder?.trackingRecords?.length ?? recordHolder?.stats?.['clusterCt'] ?? 0;
  }

  protected binCount(bin: WorkflowBin | null): number {
    return bin?.clusterCt ?? bin?.stats?.[0]?.stats?.['all'] ?? 0;
  }

  protected binStatRows(bin: WorkflowBin): ClusterTypeStats[] {
    const stats = bin.stats?.filter((stat) => stat) ?? [];

    if (stats.length) {
      return stats;
    }

    const count = bin.clusterCt ?? 0;
    return [
      {
        clusterType: 'all',
        stats: {
          all: count,
          assigned: 0,
          unassigned: count
        }
      }
    ];
  }

  protected binStatValue(
    stats: ClusterTypeStats,
    key: 'all' | 'assigned' | 'unassigned'
  ): number {
    return stats.stats?.[key] ?? 0;
  }

  protected displayRunTime(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return 'n/a';
    }

    const seconds = value / 1000;
    return `${Number.isInteger(seconds) ? seconds : seconds.toFixed(2)} sec`;
  }

  protected isBinEnabled(bin: WorkflowBin): boolean {
    return bin.enabled !== false && bin.enabled !== 0;
  }

  protected isAutofixConfig(config: WorkflowConfig): boolean {
    return config.type === 'MID_VALIDATION' || config.type === 'MID_VALIDATION_OTHER';
  }

  protected workflowBinDefinitionOptions(
    config: WorkflowConfig
  ): WorkflowBinDefinition[] {
    return [...(config.workflowBinDefinitions ?? [])];
  }

  protected workflowBinDefinitionForBin(
    bin: WorkflowBin
  ): WorkflowBinDefinition | null {
    const binName = bin.name?.trim().toLocaleLowerCase();

    if (!binName) {
      return null;
    }

    return (
      this.workflowBinDefinitionOptions(this.selectedWorkflowConfig() ?? {}).find(
        (definition) =>
          definition.name?.trim().toLocaleLowerCase() === binName
      ) ?? null
    );
  }

  protected canReorderWorkflowBinDefinition(bin: WorkflowBin): boolean {
    const config = this.selectedWorkflowConfig();

    return (
      this.canManageWorkflow() &&
      Boolean(config?.id) &&
      this.workflowBinDefinitionOptions(config ?? {}).length > 1 &&
      Boolean(this.workflowBinDefinitionForBin(bin)) &&
      !this.reorderingWorkflowBins()
    );
  }

  protected canMoveWorkflowBinDefinition(
    bin: WorkflowBin,
    direction: -1 | 1
  ): boolean {
    const definition = this.workflowBinDefinitionForBin(bin);
    const definitions = this.workflowBinDefinitionOptions(
      this.selectedWorkflowConfig() ?? {}
    );
    const index = definitions.findIndex((item) => item.id === definition?.id);

    return (
      this.canReorderWorkflowBinDefinition(bin) &&
      index >= 0 &&
      index + direction >= 0 &&
      index + direction < definitions.length
    );
  }

  protected moveWorkflowBinDefinition(
    bin: WorkflowBin,
    direction: -1 | 1
  ): void {
    const definition = this.workflowBinDefinitionForBin(bin);

    if (!definition?.id || !this.canMoveWorkflowBinDefinition(bin, direction)) {
      return;
    }

    const definitions = this.workflowBinDefinitionOptions(
      this.selectedWorkflowConfig() ?? {}
    );
    const fromIndex = definitions.findIndex((item) => item.id === definition.id);
    const toIndex = fromIndex + direction;
    const reorderedDefinitions = [...definitions];
    const [movedDefinition] = reorderedDefinitions.splice(fromIndex, 1);
    reorderedDefinitions.splice(toIndex, 0, movedDefinition);
    this.saveWorkflowBinDefinitionOrder(reorderedDefinitions);
  }

  protected startWorkflowBinDefinitionDrag(
    event: DragEvent,
    bin: WorkflowBin
  ): void {
    const definition = this.workflowBinDefinitionForBin(bin);

    if (!definition?.id || !this.canReorderWorkflowBinDefinition(bin)) {
      event.preventDefault();
      return;
    }

    this.draggingWorkflowBinDefinitionId.set(definition.id);
    event.dataTransfer?.setData('text/plain', String(definition.id));
    event.dataTransfer?.setDragImage(event.currentTarget as Element, 0, 0);
  }

  protected dragOverWorkflowBinDefinition(
    event: DragEvent,
    bin: WorkflowBin
  ): void {
    if (
      this.draggingWorkflowBinDefinitionId() &&
      this.workflowBinDefinitionForBin(bin)?.id !==
        this.draggingWorkflowBinDefinitionId()
    ) {
      event.preventDefault();
    }
  }

  protected dropWorkflowBinDefinition(event: DragEvent, bin: WorkflowBin): void {
    const sourceId =
      this.draggingWorkflowBinDefinitionId() ??
      Number(event.dataTransfer?.getData('text/plain'));
    const targetDefinition = this.workflowBinDefinitionForBin(bin);

    event.preventDefault();
    this.draggingWorkflowBinDefinitionId.set(null);

    if (!sourceId || !targetDefinition?.id || sourceId === targetDefinition.id) {
      return;
    }

    const definitions = this.workflowBinDefinitionOptions(
      this.selectedWorkflowConfig() ?? {}
    );
    const sourceIndex = definitions.findIndex(
      (definition) => definition.id === sourceId
    );
    const targetIndex = definitions.findIndex(
      (definition) => definition.id === targetDefinition.id
    );

    if (sourceIndex < 0 || targetIndex < 0) {
      return;
    }

    const reorderedDefinitions = [...definitions];
    const [movedDefinition] = reorderedDefinitions.splice(sourceIndex, 1);
    const insertIndex = sourceIndex < targetIndex ? targetIndex - 1 : targetIndex;
    reorderedDefinitions.splice(insertIndex, 0, movedDefinition);
    this.saveWorkflowBinDefinitionOrder(reorderedDefinitions);
  }

  protected clearWorkflowBinDefinitionDrag(): void {
    this.draggingWorkflowBinDefinitionId.set(null);
  }

  private saveWorkflowBinDefinitionOrder(
    definitions: WorkflowBinDefinition[]
  ): void {
    const projectId = this.projectId();
    const config = this.selectedWorkflowConfig();
    const definitionIds = definitions
      .map((definition) => definition.id)
      .filter((id): id is number => typeof id === 'number');

    if (!projectId || !config?.id || definitionIds.length !== definitions.length) {
      return;
    }

    this.reorderingWorkflowBins.set(true);
    this.api
      .reorderWorkflowBinDefinitions(projectId, config.id, definitionIds)
      .pipe(finalize(() => this.reorderingWorkflowBins.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success('Workflow bin order saved.');
          this.load();
        },
        error: () => {
          this.notifications.error('Workflow bin order could not be saved.');
        }
      });
  }

  protected isWorkflowReportActionRunning(
    worklist: Checklist & Partial<Worklist>
  ): boolean {
    const itemKey = this.workflowItemKey('worklist', worklist);

    return (
      this.generatingWorkflowReportKey() === itemKey ||
      this.downloadingWorkflowReportKey() === itemKey ||
      this.removingWorkflowReportKey() === itemKey
    );
  }

  protected isWorkflowNoteAddRunning(detail: WorkflowListDetail): boolean {
    return this.addingWorkflowNoteKey() === this.workflowItemKey(detail.kind, detail.item);
  }

  private handleWorkflowChangeEvent(event: WorkflowChangeEvent): void {
    if (!this.isCurrentProjectWorkflowEvent(event)) {
      return;
    }

    switch ((event.type ?? '').toLocaleUpperCase()) {
      case 'BINS':
        this.load();
        return;
      case 'CHECKLIST':
        this.load();
        this.refreshWorkflowListDetail('checklist', event.objectId ?? null);
        return;
      case 'WORKLIST':
        this.load();
        this.refreshWorkflowListDetail('worklist', event.objectId ?? null);
        return;
    }
  }

  private isCurrentProjectWorkflowEvent(event: WorkflowChangeEvent): boolean {
    const projectId = this.projectId();
    const eventProjectId = event.container?.id ?? null;

    return !projectId || !eventProjectId || eventProjectId === projectId;
  }

  private refreshWorkflowListDetail(
    kind: WorkflowListKind,
    objectId: number | null
  ): void {
    const detail = this.workflowListDetail();

    if (
      detail?.kind !== kind ||
      !detail.item.id ||
      (objectId !== null && detail.item.id !== objectId)
    ) {
      return;
    }

    this.viewWorkflowItem(kind, detail.item);
  }

  private startBinRegenerationProgressPolling(): void {
    this.stopBinRegenerationProgressPolling();
    this.binRegenerationElapsedSeconds.set(0);
    this.binRegenerationPolling.set(true);

    const startedAt = Date.now();
    this.binRegenerationPollSubscription = interval(1000).subscribe((tick) => {
      this.binRegenerationElapsedSeconds.set(
        Math.max(1, Math.round((Date.now() - startedAt) / 1000))
      );

      if ((tick + 1) % 5 !== 0) {
        return;
      }

      const type = this.selectedConfigType();

      if (type) {
        this.loadBins(type, false);
      }
    });
  }

  private stopBinRegenerationProgressPolling(): void {
    this.binRegenerationPollSubscription?.unsubscribe();
    this.binRegenerationPollSubscription = null;
    this.binRegenerationPolling.set(false);
    this.binRegenerationElapsedSeconds.set(0);
  }

  private loadGeneratedConceptReport(
    worklist: Checklist & Partial<Worklist>
  ): void {
    const projectId = this.projectId();
    const worklistName = worklist.name ?? '';

    this.workflowReportFileName.set(null);

    if (!projectId || !worklistName) {
      return;
    }

    this.loadingWorkflowReport.set(true);
    this.api
      .findGeneratedConceptReports(projectId, worklistName, {
        maxResults: 1,
        startIndex: 0
      })
      .pipe(finalize(() => this.loadingWorkflowReport.set(false)))
      .subscribe({
        next: (reports) => {
          this.workflowReportFileName.set(reports.items[0] ?? null);
        },
        error: () => {
          this.workflowReportFileName.set(null);
          this.notifications.error('Concept report status could not be loaded.');
        }
      });
  }

  private updateWorkflowDetailNotes(
    detail: WorkflowListDetail,
    notes: WorkflowNote[]
  ): void {
    const currentDetail = this.workflowListDetail();

    if (
      currentDetail?.kind !== detail.kind ||
      currentDetail.item.id !== detail.item.id
    ) {
      return;
    }

    this.workflowListDetail.set({
      ...currentDetail,
      item: {
        ...currentDetail.item,
        notes
      }
    });
  }

  private canAssignWorklist(worklist: Checklist & Partial<Worklist>): boolean {
    return (
      this.isWorkflowEditor() &&
      Boolean(worklist.id) &&
      Boolean(this.currentUserName()) &&
      !this.isWorkflowComplete(worklist) &&
      !this.isCurrentUserAssigned(worklist) &&
      (this.isAuthorAssignmentAvailable(worklist) ||
        this.isReviewerAssignmentAvailable(worklist))
    );
  }

  private canUnassignWorklist(worklist: Checklist & Partial<Worklist>): boolean {
    return (
      this.isWorkflowEditor() &&
      Boolean(worklist.id) &&
      Boolean(this.currentUserName()) &&
      !this.isWorkflowComplete(worklist) &&
      this.isCurrentUserAssigned(worklist)
    );
  }

  private canReassignWorklist(worklist: Checklist & Partial<Worklist>): boolean {
    const role = this.projectContext.projectRole();
    const status = worklist.workflowStatus;
    const userName = this.currentUserName().toLocaleLowerCase();

    return (
      this.isWorkflowEditor() &&
      Boolean(worklist.id) &&
      Boolean(userName) &&
      ((role === 'AUTHOR' &&
        status === 'EDITING_DONE' &&
        this.includesAssignmentName(worklist.authors, userName)) ||
        (role === 'REVIEWER' &&
          status === 'READY_FOR_PUBLICATION' &&
          this.includesAssignmentName(worklist.reviewers, userName)))
    );
  }

  private canFinishWorklist(
    worklist: Checklist & Partial<Worklist>
  ): boolean {
    const role = this.finishWorkflowRole(worklist);
    const status = worklist.workflowStatus ?? '';
    const userName = this.currentUserName();

    return (
      this.canPerformWorkflowLifecycleActions() &&
      Boolean(worklist.id) &&
      Boolean(userName) &&
      ((role === 'AUTHOR' &&
        status === 'EDITING_DONE' &&
        this.includesAssignmentName(worklist.authors, userName)) ||
        (role === 'REVIEWER' &&
          status === 'READY_FOR_PUBLICATION' &&
          this.includesAssignmentName(worklist.reviewers, userName)))
    );
  }

  private finishWorkflowRole(
    worklist: Checklist & Partial<Worklist>
  ): WorkflowAssignmentRole {
    return worklist.reviewers?.length ? 'REVIEWER' : 'AUTHOR';
  }

  private canStampWorkflowItem(
    kind: WorkflowListKind,
    item: Checklist & Partial<Worklist>
  ): boolean {
    if (
      !this.canPerformWorkflowLifecycleActions() ||
      !item.id
    ) {
      return false;
    }

    if (kind === 'checklist') {
      return true;
    }

    return (
      Boolean(item.reviewers?.length) &&
      this.latestWorkflowState(item) === 'Review Assigned'
    );
  }

  private canUnapproveWorkflowItem(
    _kind: WorkflowListKind,
    item: Checklist & Partial<Worklist>
  ): boolean {
    return (
      this.canPerformWorkflowLifecycleActions() &&
      Boolean(item.id)
    );
  }

  private workflowItemActionRequest(
    projectId: number,
    kind: WorkflowListKind,
    item: Checklist & Partial<Worklist>,
    action: WorkflowLifecycleAction,
    userName: string
  ): Observable<void | Worklist> {
    if (action === 'FINISH') {
      return this.api.performWorkflowAction(
        projectId,
        item.id!,
        userName,
        this.projectContext.projectRole() || 'REVIEWER',
        'FINISH'
      );
    }

    const activityId = item.name ?? String(item.id);
    const approve = action === 'STAMP';

    return kind === 'checklist'
      ? this.api.stampChecklist(projectId, item.id!, activityId, approve)
      : this.api.stampWorklist(projectId, item.id!, activityId, approve);
  }

  private workflowItemActionLabel(action: WorkflowLifecycleAction): string {
    switch (action) {
      case 'STAMP':
        return 'Stamp';
      case 'UNAPPROVE':
        return 'Unapprove';
      case 'FINISH':
        return 'Finish';
    }
  }

  private workflowItemActionPastTense(action: WorkflowLifecycleAction): string {
    switch (action) {
      case 'STAMP':
        return 'stamped';
      case 'UNAPPROVE':
        return 'unapproved';
      case 'FINISH':
        return 'finished';
    }
  }

  private currentUserName(): string {
    return this.auth.currentUser().userName ?? '';
  }

  private isWorkflowEditor(): boolean {
    const role = this.projectContext.projectRole();

    return role === 'AUTHOR' || role === 'REVIEWER';
  }

  private canPerformWorkflowLifecycleActions(): boolean {
    const role = this.projectContext.projectRole();

    return role === 'REVIEWER' || role === 'ADMINISTRATOR' || role === 'EDITOR5';
  }

  private isWorkflowComplete(worklist: Checklist & Partial<Worklist>): boolean {
    return worklist.workflowStatus === 'REVIEW_DONE';
  }

  private isAuthorAssignmentAvailable(
    worklist: Checklist & Partial<Worklist>
  ): boolean {
    return (
      Boolean(worklist.authorAvailable) ||
      (worklist.workflowStatus === 'NEW' && !(worklist.authors?.length))
    );
  }

  private isReviewerAssignmentAvailable(
    worklist: Checklist & Partial<Worklist>
  ): boolean {
    return (
      Boolean(worklist.reviewerAvailable) ||
      (worklist.workflowStatus === 'EDITING_DONE' &&
        !(worklist.reviewers?.length))
    );
  }

  private isCurrentUserAssigned(worklist: Checklist & Partial<Worklist>): boolean {
    const userName = this.currentUserName().toLocaleLowerCase();

    return (
      this.includesAssignmentName(worklist.authors, userName) ||
      this.includesAssignmentName(worklist.reviewers, userName)
    );
  }

  private includesAssignmentName(
    names: string[] | null | undefined,
    userName: string
  ): boolean {
    return Boolean(
      userName &&
        names?.some((name) => name.toLocaleLowerCase() === userName)
    );
  }

  private workflowStateTime(value: string | number): number {
    if (typeof value === 'number') {
      return value;
    }

    const parsed = new Date(value).getTime();
    return Number.isNaN(parsed) ? 0 : parsed;
  }

  private workflowNoteTime(note: WorkflowNote): number {
    const value = note.lastModified ?? note.timestamp;

    if (typeof value === 'number') {
      return value;
    }

    if (!value) {
      return 0;
    }

    const parsed = new Date(value).getTime();
    return Number.isNaN(parsed) ? 0 : parsed;
  }

  private workflowActionRole(
    worklist: Checklist & Partial<Worklist>,
    action: WorkflowAction
  ): string {
    const projectRole = this.projectContext.projectRole() || 'AUTHOR';

    if (action === 'ASSIGN' && this.isAuthorAssignmentAvailable(worklist)) {
      return 'AUTHOR';
    }

    if (
      (action === 'UNASSIGN' || action === 'REASSIGN') &&
      !(worklist.reviewers?.length)
    ) {
      return 'AUTHOR';
    }

    return projectRole;
  }

  private workflowProjectRoleForUser(user: OperationalUser): string | null {
    const userName = user.userName ?? '';
    const projectId = this.projectId();
    const projectRoleMap = this.selectedProject()?.userRoleMap ?? {};

    return (
      projectRoleMap[userName] ??
      (projectId ? user.projectRoleMap?.[String(projectId)] : null) ??
      null
    );
  }

  private workflowAssignmentSuccessMessage(action: WorkflowAction): string {
    switch (action) {
      case 'ASSIGN':
        return 'Worklist assigned.';
      case 'UNASSIGN':
        return 'Worklist unassigned.';
      case 'REASSIGN':
        return 'Worklist reassigned.';
      case 'FINISH':
        return 'Worklist finished.';
    }
  }

  private workflowAssignmentErrorMessage(action: WorkflowAction): string {
    switch (action) {
      case 'ASSIGN':
        return 'Worklist could not be assigned.';
      case 'UNASSIGN':
        return 'Worklist could not be unassigned.';
      case 'REASSIGN':
        return 'Worklist could not be reassigned.';
      case 'FINISH':
        return 'Worklist could not be finished.';
    }
  }

  private replaceWorklist(updatedWorklist: Worklist): void {
    this.worklists.update((worklists) =>
      worklists.map((worklist) =>
        worklist.id === updatedWorklist.id ? { ...worklist, ...updatedWorklist } : worklist
      )
    );

    const detail = this.workflowListDetail();

    if (detail?.kind === 'worklist' && detail.item.id === updatedWorklist.id) {
      this.workflowListDetail.set({
        ...detail,
        item: {
          ...detail.item,
          ...updatedWorklist
        }
      });
    }
  }

  protected canCreateChecklistForStat(
    bin: WorkflowBin,
    stats: ClusterTypeStats
  ): boolean {
    return (
      this.canCreateWorkflowLists() &&
      this.isBinEnabled(bin) &&
      !this.isAllClusterType(stats)
    );
  }

  protected canCreateWorklistForStat(
    bin: WorkflowBin,
    stats: ClusterTypeStats
  ): boolean {
    return (
      this.canCreateChecklistForStat(bin, stats) &&
      this.binStatValue(stats, 'unassigned') > 0
    );
  }

  protected canShowWorkflowBinActions(): boolean {
    return this.canManageWorkflow() || this.canCreateWorkflowLists();
  }

  protected clusterTypeLabel(clusterType: string | null | undefined): string {
    return clusterType || 'default';
  }

  private isAllClusterType(stats: ClusterTypeStats): boolean {
    return (stats.clusterType ?? '').toLocaleLowerCase() === 'all';
  }

  private validateChecklistCreationForm(
    form: WorkflowChecklistCreationForm
  ): string[] {
    const errors: string[] = [];
    const clusterCount = form.clusterCount ?? 0;
    const skipClusterCount = form.skipClusterCount ?? 0;

    if (!form.name.trim()) {
      errors.push('Checklist name must be set.');
    }

    if (!Number.isInteger(clusterCount) || clusterCount < 1) {
      errors.push('Cluster count must be a positive integer.');
    }

    if (!Number.isInteger(skipClusterCount) || skipClusterCount < 0) {
      errors.push('Skip clusters must be greater than or equal to 0.');
    }

    return errors;
  }

  private validateChecklistComputeForm(
    form: WorkflowChecklistComputeForm,
    requireName: boolean
  ): string[] {
    const errors: string[] = [];
    const clusterCount = form.clusterCount ?? 0;
    const skipClusterCount = form.skipClusterCount ?? 0;

    if (requireName && !form.name.trim()) {
      errors.push('Checklist name must be set.');
    }

    if (!form.query.trim()) {
      errors.push('Checklist query must be set.');
    }

    if (!form.queryType.trim()) {
      errors.push('Checklist query type must be set.');
    }

    if (!Number.isInteger(clusterCount) || clusterCount < 1) {
      errors.push('Cluster count must be a positive integer.');
    }

    if (!Number.isInteger(skipClusterCount) || skipClusterCount < 0) {
      errors.push('Skip clusters must be greater than or equal to 0.');
    }

    return errors;
  }

  private validateWorklistCreationForm(
    form: WorkflowWorklistCreationForm
  ): string[] {
    const errors: string[] = [];
    const clusterCount = form.clusterCount ?? 0;
    const numberOfWorklists = form.numberOfWorklists ?? 0;
    const skipClusterCount = form.skipClusterCount ?? 0;

    if (!Number.isInteger(numberOfWorklists) || numberOfWorklists < 1) {
      errors.push('Number of worklists must be a positive integer.');
    }

    if (!Number.isInteger(clusterCount) || clusterCount < 1) {
      errors.push('Cluster count must be a positive integer.');
    }

    if (clusterCount > 1000) {
      errors.push('Cluster count must be less than or equal to 1000.');
    }

    if (!Number.isInteger(skipClusterCount) || skipClusterCount < 0) {
      errors.push('Skip clusters must be greater than or equal to 0.');
    }

    return errors;
  }

  private ensureSelectedConfigType(configs: WorkflowConfig[]): void {
    const existingType = this.selectedConfigType();
    const type =
      configs.find((config) => config.type === existingType)?.type ??
      configs[0]?.type ??
      '';

    this.selectedConfigType.set(type);

    if (type) {
      this.loadBins(type);
    } else {
      this.bins.set([]);
      this.selectedBin.set(null);
    }
  }

  private loadBins(type: string, notifyOnError = true): void {
    const projectId = this.projectId();
    const selectedBinId = this.selectedBin()?.id ?? null;

    if (!projectId || !type) {
      return;
    }

    this.loadingBins.set(true);
    this.api
      .getWorkflowBins(projectId, type)
      .pipe(finalize(() => this.loadingBins.set(false)))
      .subscribe({
        next: (bins) => {
          const sortedBins = bins.items
            .map((bin, index) => ({ bin, index }))
            .sort((left, right) => {
              const leftRank = left.bin.rank ?? Number.MAX_SAFE_INTEGER;
              const rightRank = right.bin.rank ?? Number.MAX_SAFE_INTEGER;
              return leftRank === rightRank
                ? left.index - right.index
                : leftRank - rightRank;
            })
            .map((item) => item.bin);
          this.bins.set(sortedBins);
          this.binFilter.set('');
          this.binPage.set(1);
          this.selectedBin.set(
            sortedBins.find((bin) => bin.id === selectedBinId) ??
              sortedBins[0] ??
              null
          );
        },
        error: () => {
          if (notifyOnError) {
            this.notifications.error(`Workflow bins could not be loaded for ${type}.`);
          }
        }
      });
  }

  private loadAutofixAlgorithms(): void {
    const projectId = this.projectId();

    if (!projectId) {
      this.autofixAlgorithms.set([]);
      return;
    }

    this.loadingAutofixAlgorithms.set(true);
    this.api
      .getAlgorithmsForType(projectId, 'autofix')
      .pipe(finalize(() => this.loadingAutofixAlgorithms.set(false)))
      .subscribe({
        next: (algorithms) => {
          this.autofixAlgorithms.set(
            [...algorithms].sort((left, right) =>
              (left.value ?? left.key ?? '').localeCompare(
                right.value ?? right.key ?? ''
              )
            )
          );
        },
        error: () => {
          this.autofixAlgorithms.set([]);
        }
      });
  }

  private loadWorkflowAssignmentContext(projectId: number): void {
    if (!this.canManageWorklistAssignments()) {
      this.selectedProject.set(null);
      this.workflowProjectUsers.set([]);
      return;
    }

    this.loadingWorkflowProjectUsers.set(true);
    forkJoin({
      project: this.api.getProject(projectId),
      users: this.api.findAssignedProjectUsers(projectId, '', {
        ascending: true,
        maxResults: 500,
        sortField: 'userName',
        startIndex: 0
      })
    })
      .pipe(finalize(() => this.loadingWorkflowProjectUsers.set(false)))
      .subscribe({
        next: ({ project, users }) => {
          this.selectedProject.set(project);
          this.workflowProjectUsers.set(users.items);
        },
        error: () => {
          this.selectedProject.set(null);
          this.workflowProjectUsers.set([]);
          this.notifications.error('Workflow assignment users could not be loaded.');
        }
      });
  }

  private validateWorkflowConfigForm(form: WorkflowConfigForm): string[] {
    const errors: string[] = [];
    const type = form.type.trim();

    if (!type) {
      errors.push('Config type must be set.');
    }

    if (!form.queryStyle.trim()) {
      errors.push('Config query style must be set.');
    }

    const duplicateConfig = this.configs().find(
      (config) =>
        config.id !== form.workflowConfig?.id &&
        config.type?.trim().toLocaleLowerCase() === type.toLocaleLowerCase()
    );

    if (duplicateConfig) {
      errors.push(`Workflow configuration with type ${type} already exists.`);
    }

    return errors;
  }

  private buildWorkflowConfigPayload(form: WorkflowConfigForm): WorkflowConfig {
    return {
      ...(form.workflowConfig ?? {}),
      adminConfig: form.adminConfig,
      mutuallyExclusive: form.mutuallyExclusive,
      queryStyle: form.queryStyle.trim(),
      type: form.type.trim()
    };
  }

  private prepareWorkflowBinDefinitionForForm(
    definition: WorkflowBinDefinition,
    workflowConfig: WorkflowConfig
  ): WorkflowBinDefinition {
    return {
      ...definition,
      autofix: definition.autofix ?? '',
      description: definition.description ?? '',
      editable: definition.editable ?? true,
      enabled: definition.enabled ?? true,
      name: definition.name ?? '',
      query: definition.query ?? '',
      queryType: definition.queryType ?? this.queryTypes[0],
      required: definition.required ?? false,
      workflowConfig: {
        id: definition.workflowConfig?.id ?? workflowConfig.id
      },
      workflowConfigId: definition.workflowConfigId ?? workflowConfig.id
    };
  }

  private validateWorkflowBinDefinitionForm(
    form: WorkflowBinDefinitionForm
  ): string[] {
    const errors: string[] = [];
    const name = form.definition.name?.trim() ?? '';
    const query = form.definition.query?.trim() ?? '';
    const queryType = form.definition.queryType?.trim() ?? '';

    if (!name) {
      errors.push('Bin name must be set.');
    }

    if (!query) {
      errors.push('Bin query must be set.');
    }

    if (!queryType) {
      errors.push('Bin query type must be set.');
    }

    const duplicateBin = this.bins().find(
      (bin) =>
        bin.name?.trim().toLocaleLowerCase() === name.toLocaleLowerCase() &&
        bin.name?.trim().toLocaleLowerCase() !==
          form.originalName?.trim().toLocaleLowerCase()
    );

    if (duplicateBin) {
      errors.push(`Workflow bin with name ${name} already exists.`);
    }

    return errors;
  }

  private buildWorkflowBinDefinitionPayload(
    form: WorkflowBinDefinitionForm
  ): WorkflowBinDefinition {
    const workflowConfigId = form.workflowConfig.id ?? form.definition.workflowConfigId;

    return {
      ...form.definition,
      autofix: form.definition.autofix?.trim() ?? '',
      description: form.definition.description?.trim() ?? '',
      editable: Boolean(form.definition.editable),
      enabled: form.definition.enabled !== false,
      name: form.definition.name?.trim() ?? '',
      query: form.definition.query?.trim() ?? '',
      queryType: form.definition.queryType?.trim() ?? this.queryTypes[0],
      required: Boolean(form.definition.required),
      workflowConfig: {
        id: workflowConfigId
      },
      workflowConfigId
    };
  }

  private workflowExportFileName(config: WorkflowConfig): string {
    const id = config.id ?? 'config';
    const baseName =
      config.type
        ?.trim()
        .toLocaleLowerCase()
        .replace(/[^a-z0-9._-]+/g, '-')
        .replace(/^-+|-+$/g, '') || `workflow-${id}`;

    return `${baseName}.${id}.txt`;
  }

  private workflowListExportFileName(
    kind: WorkflowListKind,
    item: Checklist | Worklist
  ): string {
    const baseName = this.safeFileBaseName(item.name || `${kind}-${item.id ?? 'list'}`);

    return `${baseName}.xls`;
  }

  private expectedWorkflowReportFileName(
    worklist: Checklist & Partial<Worklist>
  ): string {
    return `${worklist.name || worklist.id || 'worklist'}_rpt.txt`;
  }

  private safeFileBaseName(value: string): string {
    return (
      value
        .trim()
        .toLocaleLowerCase()
        .replace(/[^a-z0-9._-]+/g, '-')
        .replace(/^-+|-+$/g, '') || 'download'
    );
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
}
