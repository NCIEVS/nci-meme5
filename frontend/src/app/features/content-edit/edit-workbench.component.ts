import { Component, computed, effect, inject, OnInit, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ActivatedRoute,
  RouterLink,
  RouterLinkActive
} from '@angular/router';
import { finalize, map, of, switchMap } from 'rxjs';

import { ProjectContextService } from '../../core/navigation/project-context.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { DialogComponent } from '../../shared/dialog/dialog.component';
import { IconComponent, MemeIconName } from '../../shared/icon/icon.component';
import { OperationalApiService } from '../operations/operational-api.service';
import { WorkflowApiService } from './workflow-api.service';
import {
  buildContentPfs,
  buildContentSearchPfs
} from './content-edit-api.helpers';
import { ContentEditApiService } from './content-edit-api.service';
import {
  ContentAtom,
  ContentComponent as ContentComponentDetail,
  ContentKeyValuePair,
  ContentMetadata,
  ContentRelationship,
  ContentSearchResult,
  ContentSemanticType,
  ContentSemanticTypeMetadata,
  ContentTree,
  ContentTreePosition
} from './content-edit.models';
import {
  buildActionMutationReadiness,
  buildAtomMutationReadiness,
  buildMoveAtomsReadiness,
  buildRelationshipAddReadiness,
  buildRelationshipMutationReadiness,
  buildSemanticTypeAddReadiness,
  buildSemanticTypeMutationReadiness,
  buildMergeConceptReadiness,
  buildSplitConceptReadiness,
  validationBlocksCommit,
  validationErrors,
  validationNeedsWarningOverride,
  validationWarnings
} from './edit-mutation.helpers';
import { EditMutationApiService } from './edit-mutation-api.service';
import {
  EditAddAtomRequest,
  EditAddRelationshipRequest,
  EditAddRelationshipsRequest,
  EditAddSemanticTypeRequest,
  EditApproveConceptRequest,
  EditMoveAtomsRequest,
  EditMutationReadiness,
  EditRemoveAtomRequest,
  EditRemoveSemanticTypeRequest,
  EditMergeConceptRequest,
  EditSplitConceptRequest,
  EditUpdateAtomRequest,
  EditUndoRedoRequest,
  EditValidationResult
} from './edit-mutation.models';

interface EditInventoryItem {
  label: string;
  legacySource: string;
  notes: string;
}

interface EditActionStage {
  action: string;
  endpoint: string;
  readiness: string;
}

interface EditWorkbenchLink {
  label: string;
  route: string;
  workbench: string;
}

interface EditWorkbenchContext {
  activityId?: string | null;
  componentId?: string | null;
  isChecklist?: string | null;
  projectId?: string | null;
  recordId?: string | null;
  terminology?: string | null;
  terminologyId?: string | null;
  type?: string | null;
  version?: string | null;
  worklistId?: string | null;
}

interface EditWorkbenchContextEntry {
  label: string;
  value: string;
}

interface RelationshipTargetConcept extends ContentComponentDetail {
  selected?: boolean;
}

interface TreeNodeView {
  expanded: boolean;
  loaded: boolean;
}

@Component({
  selector: 'meme-edit-workbench',
  imports: [DialogComponent, FormsModule, IconComponent, NgTemplateOutlet, RouterLink, RouterLinkActive],
  templateUrl: './edit-workbench.component.html',
  styleUrl: '../operations/operations.component.css'
})
export class EditWorkbenchComponent implements OnInit {
  constructor() {
    // Safety net: if routeWorkbench signal resolves after ngOnInit (e.g. lazy-load
    // timing in a popup window), trigger concept loading reactively.
    effect(() => {
      const workbench = this.activeWorkbench();
      if (workbench === 'main') return;
      const alreadyTriggered = untracked(
        () => this.loadedConcept() !== null || this.loadingConcept() || this.conceptLoadError() !== null
      );
      if (!alreadyTriggered) {
        untracked(() => this.loadConcept());
      }
    });

    // When Angular's router reuses this component (same route, new query params),
    // detect a componentId change and reload the concept.
    effect(() => {
      const componentId = this.selectedContext()?.componentId;
      const workbench = this.activeWorkbench();
      if (!componentId || workbench === 'main') return;

      untracked(() => {
        const loaded = this.loadedConcept();
        if (loaded && String(loaded.id) !== componentId) {
          this.refreshConcept();
        }
      });
    });

    effect(() => {
      const workbench = this.activeWorkbench();
      const conceptId = this.loadedConcept()?.id;
      if (!conceptId) return;

      untracked(() => {
        if (workbench === 'relationships' && !this.loadingRelationships()) {
          this.loadRelationships();
        }
        if (workbench === 'contexts' && !this.loadingContextTreePositions()) {
          this.loadContextTreePositions();
        }
      });
    });
  }

  private readonly contentApi = inject(ContentEditApiService);
  private readonly mutationApi = inject(EditMutationApiService);
  private readonly notifications = inject(NotificationService);
  private readonly operationsApi = inject(OperationalApiService);
  private readonly workflowApi = inject(WorkflowApiService);
  private readonly route = inject(ActivatedRoute);
  protected readonly projectContext = inject(ProjectContextService);
  private readonly routeWorkbench = toSignal(
    this.route.data.pipe(
      map((data) => String(data['workbench'] ?? 'main'))
    ),
    {
      initialValue: String(
        this.route.snapshot.routeConfig?.data?.['workbench'] ?? 'main'
      )
    }
  );
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap
  });

  protected readonly relationshipTypeOptions = ['RO', 'RB', 'RN', 'BRO', 'BRB', 'BRN'];
  protected readonly atomStatusOptions = ['NEEDS_REVIEW', 'READY_FOR_PUBLICATION'];

  // Undo/redo action state
  protected readonly actionActivityId = signal('');
  protected readonly actionForce = signal(false);
  protected readonly actionMolecularActionId = signal('');
  protected readonly actionResult = signal<EditValidationResult | null>(null);
  protected readonly loadingProjectContext = signal(false);
  protected readonly projectContextError = signal<string | null>(null);
  protected readonly projectEditingEnabled = signal<boolean | null>(null);
  protected readonly runningAction = signal<'redo' | 'undo' | null>(null);

  // Concept loading
  protected readonly loadedConcept = signal<ContentComponentDetail | null>(null);
  protected readonly loadingConcept = signal(false);
  protected readonly conceptLoadError = signal<string | null>(null);
  protected readonly metadata = signal<ContentMetadata | null>(null);

  // Shared workbench editing
  protected readonly workbenchActivityId = signal('');
  protected readonly loadingNext = signal(false);

  // Semantic types workbench
  protected readonly semanticTypeOptions = signal<ContentSemanticTypeMetadata[]>([]);
  protected readonly loadingSemanticTypeOptions = signal(false);
  protected readonly semanticTypeOptionsError = signal<string | null>(null);
  protected readonly addingSemanticType = signal(false);
  protected readonly semanticTypeAddValue = signal('');
  protected readonly semanticTypeAddResult = signal<EditValidationResult | null>(null);
  protected readonly semanticTypeAddPendingValue = signal<string | null>(null);
  protected readonly removingSemanticTypeId = signal<number | null>(null);
  protected readonly semanticTypeRemovalResult = signal<EditValidationResult | null>(null);
  protected readonly semanticTypeRemovalPendingType = signal<ContentSemanticType | null>(null);

  // STY available-list paging, sorting, filtering
  protected readonly styFilter = signal('');
  protected readonly styPage = signal(1);
  protected readonly styPageSize = signal(5);
  protected readonly stySortField = signal('typeId');
  protected readonly stySortAscending = signal(false);

  // Atoms workbench — project config
  protected readonly newAtomTermgroups = signal<string[]>([]);

  // Atoms workbench — table state
  protected readonly atomUpdateStatus = signal('NEEDS_REVIEW');
  protected readonly atomRemovalResult = signal<EditValidationResult | null>(null);
  protected readonly atomRemovalPendingAtom = signal<ContentAtom | null>(null);
  protected readonly atomUpdateResult = signal<EditValidationResult | null>(null);
  protected readonly atomUpdatePendingAtom = signal<ContentAtom | null>(null);
  protected readonly removingAtomId = signal<number | null>(null);
  protected readonly updatingAtomId = signal<number | null>(null);
  protected readonly atomFilter = signal('');
  protected readonly atomPage = signal(1);
  protected readonly atomPageSize = signal(100);
  protected readonly atomSortField = signal<string | null>(null);
  protected readonly atomSortAscending = signal(false);
  protected readonly expandedAtomIds = signal<ReadonlySet<number>>(new Set());
  protected readonly selectedAtomIds = signal<ReadonlySet<number>>(new Set());

  // Atoms workbench — add/edit inline form
  protected readonly atomFormMode = signal<'add' | 'edit' | null>(null);
  protected readonly atomFormName = signal('');
  protected readonly atomFormTermgroup = signal('');
  protected readonly atomFormLanguage = signal('ENG');
  protected readonly atomFormCodeId = signal('');
  protected readonly atomFormConceptId = signal('');
  protected readonly atomFormDescriptorId = signal('');
  protected readonly atomFormPublishable = signal(true);
  protected readonly atomFormSuppressible = signal(false);
  protected readonly atomFormVersion = signal('');
  protected readonly atomFormTargetId = signal<number | null>(null);
  protected readonly atomFormOriginalAtom = signal<ContentAtom | null>(null);
  protected readonly atomFormClientErrors = signal<string[]>([]);
  protected readonly atomFormResult = signal<EditValidationResult | null>(null);
  protected readonly atomFormPending = signal(false);

  // Atoms workbench — move dialog
  protected readonly moveDialogOpen = signal(false);
  protected readonly movePeerConcepts = signal<ContentComponentDetail[]>([]);
  protected readonly moveTargetConceptId = signal('');
  protected readonly moveTargetConcept = signal<ContentComponentDetail | null>(null);
  protected readonly moveLookupPending = signal(false);
  protected readonly moveLookupError = signal<string | null>(null);
  protected readonly moveResult = signal<EditValidationResult | null>(null);
  protected readonly movePending = signal(false);

  // Atoms workbench — split dialog
  protected readonly splitDialogOpen = signal(false);
  protected readonly splitCopy = signal(true);
  protected readonly splitRelationshipType = signal('RO');
  protected readonly splitResult = signal<EditValidationResult | null>(null);
  protected readonly splitPending = signal(false);

  protected readonly splitRelationshipTypes = ['RO', 'RB', 'RN', 'RQ', 'XR'];

  // Atoms workbench — merge dialog
  protected readonly mergeDialogOpen = signal(false);
  protected readonly mergePeerConcepts = signal<ContentComponentDetail[]>([]);
  protected readonly mergeTargetConceptId = signal('');
  protected readonly mergeTargetConcept = signal<ContentComponentDetail | null>(null);
  protected readonly mergeLookupPending = signal(false);
  protected readonly mergeLookupError = signal<string | null>(null);
  protected readonly mergeResult = signal<EditValidationResult | null>(null);
  protected readonly mergePending = signal(false);

  // Relationships workbench
  protected readonly addingRelationship = signal(false);
  protected readonly addRelationshipDialogOpen = signal(false);
  protected readonly relationshipAddManualTargetId = signal('');
  protected readonly relationshipAddManualTargetError = signal<string | null>(null);
  protected readonly relationshipAddTargets = signal<RelationshipTargetConcept[]>([]);
  protected readonly relationshipFinderDialogOpen = signal(false);
  protected readonly relationshipFinderQuery = signal('');
  protected readonly relationshipFinderResults = signal<ContentSearchResult[]>([]);
  protected readonly relationshipFinderPage = signal(1);
  protected readonly relationshipFinderPageSize = 10;
  protected readonly relationshipFinderError = signal<string | null>(null);
  protected readonly searchingRelationshipFinder = signal(false);
  protected readonly relationshipAddType = signal('RO');
  protected readonly relationshipAddTargetConceptId = signal('');
  protected readonly relationshipAddPendingRelationships = signal<ContentRelationship[] | null>(null);
  protected readonly relationshipAddResult = signal<EditValidationResult | null>(null);
  protected readonly relationshipAddPendingRelationship = signal<ContentRelationship | null>(null);
  protected readonly relationshipPage = signal(1);
  protected readonly relationshipPageSize = signal(20);
  protected readonly relationshipSortField = signal('lastModified');
  protected readonly relationshipSortAscending = signal(false);
  protected readonly relationshipPreferredOnly = signal(true);
  protected readonly relationships = signal<ContentRelationship[]>([]);
  protected readonly relationshipCount = signal(0);
  protected readonly relationshipError = signal<string | null>(null);
  protected readonly loadingRelationships = signal(false);
  protected readonly selectedRelationshipIds = signal<ReadonlySet<number>>(new Set());
  protected readonly removingRelationshipId = signal<number | null>(null);
  protected readonly relationshipRemovalResult = signal<EditValidationResult | null>(null);
  protected readonly relationshipRemovalPendingRelationship = signal<ContentRelationship | null>(null);

  // Code concepts workbench
  protected readonly codeConceptTarget = signal<ContentAtom | null>(null);
  protected readonly codeConceptResults = signal<ContentSearchResult[]>([]);
  protected readonly codeConceptTotalCount = signal(0);
  protected readonly loadingCodeConcepts = signal(false);
  protected readonly codeConceptError = signal<string | null>(null);

  // Contexts workbench
  protected readonly contextTreePositions = signal<ContentTreePosition[]>([]);
  protected readonly contextTreePositionCount = signal(0);
  protected readonly contextTreePositionError = signal<string | null>(null);
  protected readonly loadingContextTreePositions = signal(false);
  protected readonly contextPage = signal(1);
  protected readonly contextPageSize = signal(5);
  protected readonly contextSortField = signal('terminology');
  protected readonly contextSortAscending = signal(false);
  protected readonly selectedContextTreePosition = signal<ContentTreePosition | null>(null);
  protected readonly contextTree = signal<ContentTree | null>(null);
  protected readonly contextTreeCount = signal(0);
  protected readonly contextTreeViewed = signal(0);
  protected readonly contextTreeError = signal<string | null>(null);
  protected readonly loadingContextTree = signal(false);
  protected readonly treeNodeViews = signal<Record<string, TreeNodeView>>({});
  protected readonly loadingTreeChildrenKey = signal<string | null>(null);

  // Existing computed — prefer projectId from URL context (popup), fall back to user preferences
  protected readonly projectId = computed<number | null>(() => {
    const contextProjectId = this.selectedContext()?.projectId;
    if (contextProjectId) {
      const parsed = Number(contextProjectId);
      if (Number.isFinite(parsed) && parsed > 0) return parsed;
    }
    return this.projectContext.projectId();
  });
  protected readonly projectRole = computed(() => {
    const projectId = this.projectId();
    if (projectId) {
      const role = this.projectContext.projectRoleForId(projectId);
      if (role) return role;
    }
    return this.projectContext.projectRole() || 'n/a';
  });
  protected readonly activeWorkbench = computed(() => this.routeWorkbench());
  protected readonly canOpenWorkbench = computed(() =>
    this.projectContext.hasPrivilegesOf('AUTHOR')
  );
  protected readonly editRoleReady = computed(() =>
    this.projectContext.hasPrivilegesOf('AUTHOR')
  );
  protected readonly actionMolecularActionIdValue = computed(() => {
    const value = Number(this.actionMolecularActionId());

    return Number.isFinite(value) && value > 0 ? value : null;
  });
  protected readonly actionReadiness = computed<EditMutationReadiness>(() => {
    const readiness = buildActionMutationReadiness(
      this.projectId(),
      this.actionMolecularActionIdValue(),
      this.actionActivityId(),
      this.projectRole(),
      this.projectEditingEnabled() !== false
    );
    const reasons = [...readiness.reasons];

    if (this.projectId() && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (this.projectId() && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (
      this.projectId() &&
      this.projectEditingEnabled() === null &&
      !this.loadingProjectContext()
    ) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });
  protected readonly actionErrors = computed(() =>
    validationErrors(this.actionResult())
  );
  protected readonly actionWarnings = computed(() =>
    validationWarnings(this.actionResult())
  );
  protected readonly actionComments = computed(() =>
    Array.from(this.actionResult()?.comments ?? [])
  );
  protected readonly selectedContext = computed<EditWorkbenchContext | null>(() => {
    const queryParams = this.queryParamMap();
    const context: EditWorkbenchContext = {
      activityId: queryParams.get('activityId'),
      componentId: queryParams.get('componentId'),
      isChecklist: queryParams.get('isChecklist'),
      projectId: queryParams.get('projectId'),
      recordId: queryParams.get('recordId'),
      terminology: queryParams.get('terminology'),
      terminologyId: queryParams.get('terminologyId') ?? queryParams.get('id'),
      type: queryParams.get('type'),
      version: queryParams.get('version'),
      worklistId: queryParams.get('worklistId')
    };

    return Object.values(context).some(Boolean) ? context : null;
  });
  protected readonly selectedContextEntries = computed<
    EditWorkbenchContextEntry[]
  >(() => {
    const context = this.selectedContext();

    if (!context) {
      return [];
    }

    return [
      { label: 'Type', value: context.type || 'n/a' },
      { label: 'Terminology', value: context.terminology || 'n/a' },
      { label: 'Version', value: context.version || 'n/a' },
      {
        label: 'Terminology ID',
        value: context.terminologyId || 'n/a'
      },
      { label: 'Component ID', value: context.componentId || 'n/a' },
      { label: 'Project', value: context.projectId || 'n/a' },
      { label: 'Activity ID', value: context.activityId || 'n/a' }
    ];
  });
  protected readonly selectedContextQueryParams = computed(() =>
    this.toRouteQueryParams(this.selectedContext())
  );
  protected readonly editDetailQueryParams = computed(() => {
    const context = this.selectedContext();

    if (!context) {
      return {};
    }

    return this.toRouteQueryParams({
      activityId: context.activityId,
      componentId: context.componentId,
      projectId: context.projectId,
      terminology: context.terminology,
      terminologyId: context.terminologyId,
      type: context.type,
      version: context.version
    });
  });

  // Concept-derived computed
  protected readonly conceptLastModified = computed(() =>
    this.toEpochMillis(this.loadedConcept()?.lastModified)
  );
  protected readonly conceptTerminology = computed(
    () => this.loadedConcept()?.terminology || this.selectedContext()?.terminology || ''
  );
  protected readonly conceptVersion = computed(
    () => this.loadedConcept()?.version || this.selectedContext()?.version || ''
  );
  protected readonly conceptSemanticTypes = computed<ContentSemanticType[]>(
    () => Array.from(this.loadedConcept()?.semanticTypes ?? [])
  );
  protected readonly conceptAtoms = computed<ContentAtom[]>(
    () => Array.from(this.loadedConcept()?.atoms ?? [])
  );
  protected readonly filteredAtoms = computed<ContentAtom[]>(() => {
    const atoms = this.conceptAtoms();
    const filter = this.atomFilter().trim().toLowerCase();
    const field = this.atomSortField();
    const ascending = this.atomSortAscending();
    let list = filter
      ? atoms.filter((a) =>
          [a.name, a.termType, a.terminology, a.codeId, a.conceptId, a.descriptorId, a.lastModifiedBy]
            .some((v) => v?.toLowerCase().includes(filter))
        )
      : atoms;
    if (field) {
      list = [...list].sort((a, b) => {
        const av = String((a as Record<string, unknown>)[field] ?? '');
        const bv = String((b as Record<string, unknown>)[field] ?? '');
        return ascending ? av.localeCompare(bv) : bv.localeCompare(av);
      });
    }
    return list;
  });
  protected readonly atomTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredAtoms().length / this.atomPageSize()))
  );
  protected readonly pagedAtoms = computed<ContentAtom[]>(() => {
    const start = (this.atomPage() - 1) * this.atomPageSize();
    return this.filteredAtoms().slice(start, start + this.atomPageSize());
  });
  protected readonly selectedAtomCount = computed(() => this.selectedAtomIds().size);
  protected readonly atomFormTermType = computed(() => {
    const termgroup = this.atomFormTermgroup();
    const slashIdx = termgroup.indexOf('/');
    if (slashIdx >= 0) {
      return termgroup.substring(slashIdx + 1);
    }
    return this.atomFormOriginalAtom()?.termType ?? '';
  });
  protected readonly atomFormShowPublishable = computed(() => this.atomFormTermType() === 'PN');
  protected readonly atomLanguageOptions = computed<ContentKeyValuePair[]>(() => {
    const languages = this.metadata()?.languages ?? [];
    return languages.length ? languages : [{ key: 'ENG', value: 'English' }];
  });
  protected readonly atomFormErrors = computed(() => [
    ...this.atomFormClientErrors(),
    ...validationErrors(this.atomFormResult())
  ]);
  protected readonly atomFormWarnings = computed(() => validationWarnings(this.atomFormResult()));
  protected readonly atomFormNeedsOverride = computed(
    () => validationNeedsWarningOverride(this.atomFormResult())
  );
  protected readonly moveReadiness = computed(() =>
    buildMoveAtomsReadiness(
      this.projectId(),
      this.loadedConcept()?.id,
      this.moveTargetConcept()?.id ?? null,
      Array.from(this.selectedAtomIds()),
      this.workbenchActivityId(),
      this.conceptLastModified(),
      this.projectRole(),
      this.projectEditingEnabled() === true
    )
  );
  protected readonly moveErrors = computed(() => validationErrors(this.moveResult()));
  protected readonly moveWarnings = computed(() => validationWarnings(this.moveResult()));
  protected readonly moveNeedsOverride = computed(() => validationNeedsWarningOverride(this.moveResult()));
  protected readonly splitReadiness = computed(() =>
    buildSplitConceptReadiness(
      this.projectId(),
      this.loadedConcept()?.id,
      Array.from(this.selectedAtomIds()),
      this.splitRelationshipType(),
      this.workbenchActivityId(),
      this.conceptLastModified(),
      this.projectRole(),
      this.projectEditingEnabled() === true
    )
  );
  protected readonly splitErrors = computed(() => validationErrors(this.splitResult()));
  protected readonly splitWarnings = computed(() => validationWarnings(this.splitResult()));
  protected readonly splitNeedsOverride = computed(() => validationNeedsWarningOverride(this.splitResult()));
  protected readonly mergeReadiness = computed(() =>
    buildMergeConceptReadiness(
      this.projectId(),
      this.loadedConcept()?.id,
      this.mergeTargetConcept()?.id ?? null,
      this.workbenchActivityId(),
      this.conceptLastModified(),
      this.projectRole(),
      this.projectEditingEnabled() === true
    )
  );
  protected readonly mergeErrors = computed(() => validationErrors(this.mergeResult()));
  protected readonly mergeWarnings = computed(() => validationWarnings(this.mergeResult()));
  protected readonly mergeNeedsOverride = computed(() => validationNeedsWarningOverride(this.mergeResult()));
  protected readonly conceptRelationships = computed<ContentRelationship[]>(
    () => Array.from(this.loadedConcept()?.relationships ?? [])
  );
  protected readonly relationshipTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.relationshipCount() / this.relationshipPageSize()))
  );
  protected readonly selectedRelationships = computed(() => {
    const ids = this.selectedRelationshipIds();
    return this.relationships().filter((relationship) =>
      relationship.id ? ids.has(relationship.id) : false
    );
  });
  protected readonly hasSelectedRelationships = computed(() =>
    this.selectedRelationshipIds().size > 0
  );
  protected readonly selectedRelationshipTargetsForDialog = computed(() =>
    this.relationshipAddTargets().filter((target) => target.selected)
  );
  protected readonly acceptedRelationshipTypeOptions = computed(() => {
    const types = ['XR', ...this.relationshipTypeOptions];
    if (this.loadedConcept()?.publishable === false) {
      types.push('BBT');
    }
    const metadataTypes = this.metadata()?.relationshipTypes ?? [];
    return Array.from(new Set(types)).map((key) => {
      if (key === 'XR') {
        return { key, value: '(none)' };
      }
      return metadataTypes.find((type) => type.key === key) ?? { key, value: '' };
    });
  });
  protected readonly availableSemanticTypeOptions = computed<ContentSemanticTypeMetadata[]>(() => {
    const existing = new Set(
      this.conceptSemanticTypes()
        .map((sty) => sty.semanticType?.trim())
        .filter((v): v is string => Boolean(v))
    );
    return this.semanticTypeOptions().filter((opt) => {
      const value = opt.expandedForm || opt.abbreviation || opt.typeId || '';
      return value && !existing.has(value);
    });
  });
  protected readonly filteredSortedStys = computed<ContentSemanticTypeMetadata[]>(() => {
    const filter = this.styFilter().trim().toLowerCase();
    const field = this.stySortField();
    const ascending = this.stySortAscending();
    let list = this.availableSemanticTypeOptions();
    if (filter) {
      list = list.filter((s) =>
        (s.expandedForm ?? '').toLowerCase().includes(filter) ||
        (s.typeId ?? '').toLowerCase().includes(filter) ||
        (s.treeNumber ?? '').toLowerCase().includes(filter)
      );
    }
    return [...list].sort((a, b) => {
      const av = String((a as Record<string, unknown>)[field] ?? '');
      const bv = String((b as Record<string, unknown>)[field] ?? '');
      return ascending ? av.localeCompare(bv) : bv.localeCompare(av);
    });
  });
  protected readonly styFilteredCount = computed(() => this.filteredSortedStys().length);
  protected readonly styTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.styFilteredCount() / this.styPageSize()))
  );
  protected readonly styPageRange = computed(() => {
    const total = this.styTotalPages();
    const cur = this.styPage();
    let start = Math.max(1, cur - 2);
    const end = Math.min(total, start + 4);
    start = Math.max(1, end - 4);
    const range: number[] = [];
    for (let p = start; p <= end; p++) range.push(p);
    return range;
  });
  protected readonly pagedStys = computed<ContentSemanticTypeMetadata[]>(() => {
    const start = (this.styPage() - 1) * this.styPageSize();
    return this.filteredSortedStys().slice(start, start + this.styPageSize());
  });
  protected readonly semanticTypeAddReadiness = computed<EditMutationReadiness>(() => {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const reasons = [
      ...buildSemanticTypeAddReadiness(
        projectId,
        concept?.id,
        this.semanticTypeAddValue(),
        this.workbenchActivityId(),
        this.conceptLastModified(),
        this.projectRole(),
        this.projectEditingEnabled() !== false
      ).reasons
    ];

    if (!concept) {
      reasons.push('Concept must be loaded.');
    }
    if (this.loadingSemanticTypeOptions()) {
      reasons.push('Semantic type options are loading.');
    }
    if (this.semanticTypeOptionsError()) {
      reasons.push('Semantic type options could not be loaded.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && this.projectEditingEnabled() === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return { canExecute: reasons.length === 0, reasons: Array.from(new Set(reasons)) };
  });
  protected readonly atomMutationBaseReadiness = computed<EditMutationReadiness>(() => {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const reasons = buildAtomMutationReadiness(
      projectId,
      concept?.id,
      null,
      this.workbenchActivityId(),
      this.conceptLastModified(),
      this.projectRole(),
      this.projectEditingEnabled() !== false
    ).reasons.filter((r) => r !== 'Atom id is required.');

    if (!concept) {
      reasons.push('Concept must be loaded.');
    }
    if (!this.atomUpdateStatus().trim()) {
      reasons.push('Target status is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && this.projectEditingEnabled() === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return { canExecute: reasons.length === 0, reasons: Array.from(new Set(reasons)) };
  });
  protected readonly relationshipAddReadiness = computed<EditMutationReadiness>(() => {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const targetId = this.parsePositiveInteger(this.relationshipAddTargetConceptId());
    const reasons = [
      ...buildRelationshipAddReadiness(
        projectId,
        concept?.id,
        targetId,
        this.relationshipAddType(),
        this.workbenchActivityId(),
        this.conceptLastModified(),
        this.projectRole(),
        this.projectEditingEnabled() !== false
      ).reasons
    ];

    if (!concept) {
      reasons.push('Concept must be loaded.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && this.projectEditingEnabled() === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return { canExecute: reasons.length === 0, reasons: Array.from(new Set(reasons)) };
  });
  protected readonly relationshipRemovalBaseReadiness = computed<EditMutationReadiness>(() => {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const reasons = buildRelationshipMutationReadiness(
      projectId,
      concept?.id,
      null,
      this.workbenchActivityId(),
      this.conceptLastModified(),
      this.projectRole(),
      this.projectEditingEnabled() !== false
    ).reasons.filter((r) => r !== 'Relationship id is required.');

    if (!concept) {
      reasons.push('Concept must be loaded.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && this.projectEditingEnabled() === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return { canExecute: reasons.length === 0, reasons: Array.from(new Set(reasons)) };
  });

  // Per-result feedback computed
  protected readonly semanticTypeAddErrors = computed(() => validationErrors(this.semanticTypeAddResult()));
  protected readonly semanticTypeAddWarnings = computed(() => validationWarnings(this.semanticTypeAddResult()));
  protected readonly semanticTypeAddNeedsOverride = computed(
    () => Boolean(this.semanticTypeAddPendingValue()) && validationNeedsWarningOverride(this.semanticTypeAddResult())
  );
  protected readonly semanticTypeRemovalErrors = computed(() => validationErrors(this.semanticTypeRemovalResult()));
  protected readonly semanticTypeRemovalWarnings = computed(() => validationWarnings(this.semanticTypeRemovalResult()));
  protected readonly semanticTypeRemovalNeedsOverride = computed(
    () => Boolean(this.semanticTypeRemovalPendingType()) && validationNeedsWarningOverride(this.semanticTypeRemovalResult())
  );
  protected readonly atomUpdateErrors = computed(() => validationErrors(this.atomUpdateResult()));
  protected readonly atomUpdateWarnings = computed(() => validationWarnings(this.atomUpdateResult()));
  protected readonly atomUpdateNeedsOverride = computed(
    () => Boolean(this.atomUpdatePendingAtom()) && validationNeedsWarningOverride(this.atomUpdateResult())
  );
  protected readonly atomRemovalErrors = computed(() => validationErrors(this.atomRemovalResult()));
  protected readonly atomRemovalWarnings = computed(() => validationWarnings(this.atomRemovalResult()));
  protected readonly atomRemovalNeedsOverride = computed(
    () => Boolean(this.atomRemovalPendingAtom()) && validationNeedsWarningOverride(this.atomRemovalResult())
  );
  protected readonly relationshipAddErrors = computed(() => validationErrors(this.relationshipAddResult()));
  protected readonly relationshipAddWarnings = computed(() => validationWarnings(this.relationshipAddResult()));
  protected readonly relationshipAddNeedsOverride = computed(
    () =>
      (Boolean(this.relationshipAddPendingRelationship()) ||
        Boolean(this.relationshipAddPendingRelationships())) &&
      validationNeedsWarningOverride(this.relationshipAddResult())
  );
  protected readonly relationshipRemovalErrors = computed(() => validationErrors(this.relationshipRemovalResult()));
  protected readonly relationshipRemovalWarnings = computed(() => validationWarnings(this.relationshipRemovalResult()));
  protected readonly relationshipRemovalNeedsOverride = computed(
    () => Boolean(this.relationshipRemovalPendingRelationship()) && validationNeedsWarningOverride(this.relationshipRemovalResult())
  );
  protected readonly contextTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.contextTreePositionCount() / this.contextPageSize()))
  );

  protected readonly workbenchLinks: EditWorkbenchLink[] = [
    { label: 'Main', route: '/edit', workbench: 'main' },
    { label: 'Semantic Types', route: '/edit/semantic-types', workbench: 'semantic-types' },
    { label: 'Code Concepts', route: '/edit/codeConcepts', workbench: 'code-concepts' },
    { label: 'Atoms', route: '/edit/atoms', workbench: 'atoms' },
    { label: 'Relationships', route: '/edit/relationships', workbench: 'relationships' },
    { label: 'Contexts', route: '/contexts', workbench: 'contexts' }
  ];

  protected readonly routeInventory: EditInventoryItem[] = [
    {
      label: 'Main edit workbench',
      legacySource: '/edit -> EditCtrl + edit.html',
      notes: 'Concept load, editor state, approvals, workflow finish, metadata tools, and popout launchers.'
    },
    {
      label: 'Semantic type editor',
      legacySource: '/edit/semantic-types -> SemanticTypesCtrl',
      notes: 'Popout window for semantic type add/remove and approval refresh events.'
    },
    {
      label: 'Code concept editor',
      legacySource: '/edit/codeConcepts -> CodeConceptsCtrl',
      notes: 'Popout window for code concept workflow and websocket refresh handling.'
    },
    {
      label: 'Atom editor',
      legacySource: '/edit/atoms -> AtomsCtrl',
      notes: 'Atom add/update/remove plus merge, move, split entry points.'
    },
    {
      label: 'Relationship editor',
      legacySource: '/edit/relationships -> RelationshipsCtrl',
      notes: 'Relationship add/remove/update plus transfer-to-editor behavior.'
    },
    {
      label: 'Context editor',
      legacySource: '/contexts -> ContextsCtrl',
      notes: 'Legacy route is not nested under /edit, but it belongs to the edit popout family.'
    }
  ];

  protected readonly mutationInventory: EditInventoryItem[] = [
    {
      label: 'Simple content edits',
      legacySource: 'editService -> /edit/*',
      notes: 'Simple atom, semantic type, concept, and bulk concept removal calls.'
    },
    {
      label: 'Full meta edits',
      legacySource: 'metaEditingService -> /meta/*',
      notes: 'Atom, attribute, relationship, semantic type, merge, move, split, approve, undo, and redo operations.'
    },
    {
      label: 'Workflow finish',
      legacySource: 'FinishWorkflowModalCtrl',
      notes: 'Finish workflow interactions tied to the selected editing activity.'
    },
    {
      label: 'Safety layer',
      legacySource: 'lastModified, overrideWarnings, action warnings',
      notes: 'Dirty-state, stale-update, destructive-action, and warning override behavior must be explicit.'
    }
  ];

  protected readonly stagedActions: EditActionStage[] = [
    {
      action: 'Update concept',
      endpoint: 'POST /edit/concept',
      readiness: 'Requires persisted concept, project editing flag, stale-update guard, and confirmation.'
    },
    {
      action: 'Simple atom update',
      endpoint: 'POST /edit/atom',
      readiness: 'Requires persisted concept and atom, project editing flag, editable atom fields, and confirmation.'
    },
    {
      action: 'Update atom status, add child metadata, and remove child components',
      endpoint: 'POST /meta/atom/add|update; POST /meta/attribute|sty/add; POST /meta/*/remove/{id}',
      readiness: 'Requires activityId, lastModified, destructive-action guard, validation review, and refresh.'
    },
    {
      action: 'Approve concept',
      endpoint: 'POST /meta/concept/approve',
      readiness: 'Requires activityId, lastModified, validation warning review, and overrideWarnings retry.'
    },
    {
      action: 'Undo/redo action',
      endpoint: 'POST /meta/action/undo|redo',
      readiness: 'Requires molecularActionId, activityId, force confirmation, and websocket refresh parity.'
    }
  ];

  protected readonly safetyChecks: string[] = [
    'Author-level project role before exposing mutation buttons',
    'Project editing-enabled check before request submission',
    'lastModified freshness on molecular actions',
    'Validation errors block commit; warning-only results require explicit override',
    'Destructive actions require confirmation and post-action component refresh'
  ];

  ngOnInit(): void {
    this.loadProjectContext();
    const context = this.selectedContext();
    this.workbenchActivityId.set(
      context?.activityId || `activity-${Date.now()}`
    );
    if (context) {
      this.loadConcept();
    }
  }

  // --- Existing undo/redo setters ---

  protected setActionActivityId(value: string): void {
    this.actionActivityId.set(value);
  }

  protected setActionForce(value: boolean): void {
    this.actionForce.set(value);
  }

  protected setActionMolecularActionId(value: string): void {
    this.actionMolecularActionId.set(value);
  }

  protected performAction(action: 'redo' | 'undo'): void {
    const request = this.buildActionRequest();

    if (!request || !this.actionReadiness().canExecute) {
      return;
    }

    const label = action === 'undo' ? 'Undo' : 'Redo';
    const forceLabel = request.force ? ' with force' : '';

    if (
      !window.confirm(
        `${label} molecular action ${request.molecularActionId}${forceLabel}?`
      )
    ) {
      return;
    }

    this.runningAction.set(action);
    this.actionResult.set(null);
    const actionRequest =
      action === 'undo'
        ? this.mutationApi.undoAction(request)
        : this.mutationApi.redoAction(request);

    actionRequest
      .pipe(finalize(() => this.runningAction.set(null)))
      .subscribe({
        next: (result) => {
          this.actionResult.set(result);
          if (validationBlocksCommit(result)) {
            this.notifications.error(`${label} failed validation.`);
            return;
          }
          if (validationWarnings(result).length) {
            return;
          }
        },
        error: () => {
          this.notifications.error(`${label} could not be completed.`);
        }
      });
  }

  // --- Workbench setters ---

  protected setWorkbenchActivityId(value: string): void {
    this.workbenchActivityId.set(value);
  }

  protected setAtomUpdateStatus(value: string): void {
    this.atomUpdateStatus.set(value);
  }

  // --- Atoms workbench table controls ---

  protected setAtomFilter(value: string): void {
    this.atomFilter.set(value);
    this.atomPage.set(1);
  }

  protected setAtomPage(page: number): void {
    this.atomPage.set(page);
  }

  protected setAtomPageSize(size: number): void {
    this.atomPageSize.set(size);
    this.atomPage.set(1);
  }

  protected toggleAtomSort(field: string): void {
    if (this.atomSortField() === field) {
      if (this.atomSortAscending()) {
        this.atomSortAscending.set(false);
      } else {
        this.atomSortField.set(null);
      }
    } else {
      this.atomSortField.set(field);
      this.atomSortAscending.set(true);
    }
  }

  protected getAtomRowClass(atom: ContentAtom): string {
    if (atom.workflowStatus === 'NEEDS_REVIEW') return 'atom-row-needs-review';
    if (!atom.publishable) return 'atom-row-unreleasable';
    if (atom.terminology === 'RXNORM') return 'atom-row-rxnorm';
    if (atom.obsolete) return 'atom-row-obsolete';
    return '';
  }

  protected isAtomDeletable(atom: ContentAtom): boolean {
    const termgroups = this.newAtomTermgroups();
    if (!termgroups.length) return false;
    return termgroups.includes(`${atom.terminology}/${atom.termType}`);
  }

  protected toggleAtomExpand(atomId: number | null | undefined): void {
    if (!atomId) return;
    const ids = new Set(this.expandedAtomIds());
    if (ids.has(atomId)) { ids.delete(atomId); } else { ids.add(atomId); }
    this.expandedAtomIds.set(ids);
  }

  protected toggleAtomSelection(atomId: number | null | undefined): void {
    if (!atomId) return;
    const ids = new Set(this.selectedAtomIds());
    if (ids.has(atomId)) { ids.delete(atomId); } else { ids.add(atomId); }
    this.selectedAtomIds.set(ids);
  }

  protected clearAtomSelections(): void {
    this.selectedAtomIds.set(new Set());
  }

  // --- Atoms add/edit inline form ---

  protected openAddAtom(): void {
    const firstTermgroup = this.newAtomTermgroups()[0] ?? '';
    const firstTerminology = firstTermgroup.split('/')[0] ?? '';
    const versionFromAtoms = this.conceptAtoms().find(
      (a) => a.terminology === firstTerminology
    )?.version ?? this.conceptVersion();
    this.atomFormMode.set('add');
    this.atomFormName.set('');
    this.atomFormTermgroup.set(firstTermgroup);
    this.atomFormLanguage.set('ENG');
    this.atomFormCodeId.set('NOCODE');
    this.atomFormConceptId.set('');
    this.atomFormDescriptorId.set('');
    this.atomFormPublishable.set(true);
    this.atomFormSuppressible.set(false);
    this.atomFormVersion.set(versionFromAtoms);
    this.atomFormTargetId.set(null);
    this.atomFormClientErrors.set([]);
    this.atomFormResult.set(null);
  }

  protected openEditAtom(atom: ContentAtom): void {
    this.atomFormMode.set('edit');
    this.atomFormOriginalAtom.set(atom);
    this.atomFormName.set(atom.name ?? '');
    this.atomFormTermgroup.set(`${atom.terminology ?? ''}/${atom.termType ?? ''}`);
    this.atomFormLanguage.set(atom.language ?? 'ENG');
    this.atomFormCodeId.set(atom.codeId ?? '');
    this.atomFormConceptId.set(atom.conceptId ?? '');
    this.atomFormDescriptorId.set(atom.descriptorId ?? '');
    this.atomFormPublishable.set(atom.publishable !== false);
    this.atomFormSuppressible.set(atom.suppressible === true);
    this.atomFormVersion.set(atom.version ?? '');
    this.atomFormTargetId.set(atom.id ?? null);
    this.atomFormClientErrors.set([]);
    this.atomFormResult.set(null);
  }

  protected cancelAtomForm(): void {
    this.atomFormMode.set(null);
    this.atomFormOriginalAtom.set(null);
    this.atomFormClientErrors.set([]);
    this.atomFormResult.set(null);
  }

  protected submitAtomForm(overrideWarnings = false): void {
    const mode = this.atomFormMode();
    if (!mode) return;
    const projectId = this.projectId();
    const concept = this.loadedConcept();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId();
    if (!projectId || !concept?.id || !lastModified) return;
    this.atomFormClientErrors.set([]);

    let atom: ContentAtom;
    if (mode === 'edit') {
      // Spread the full original atom so no "unchanged" fields differ from the DB.
      // The backend (UpdateAtomMolecularAction) only permits publishable/suppressible/
      // obsolete/workflowStatus to change; all other field changes throw.
      const original = this.atomFormOriginalAtom()!;
      atom = {
        ...original,
        publishable: this.atomFormPublishable(),
        suppressible: this.atomFormSuppressible(),
      };
    } else {
      const termgroup = this.atomFormTermgroup();
      const slashIdx = termgroup.indexOf('/');
      const terminology = slashIdx >= 0 ? termgroup.substring(0, slashIdx) : termgroup;
      const termType = slashIdx >= 0 ? termgroup.substring(slashIdx + 1) : '';
      const codeId = this.atomFormCodeId().trim();
      const conceptId = this.atomFormConceptId().trim();
      const descriptorId = this.atomFormDescriptorId().trim();
      if (!this.atomFormName().trim() || !termgroup.trim() || (!codeId && !conceptId && !descriptorId)) {
        this.atomFormClientErrors.set([
          'Name, termgroup and at least one id must be entered for new atom'
        ]);
        return;
      }
      const version = this.conceptAtoms().find((a) => a.terminology === terminology)?.version
        ?? this.conceptVersion();
      atom = {
        name: this.atomFormName(),
        terminology,
        termType,
        version: version || undefined,
        language: this.atomFormLanguage() || 'ENG',
        terminologyId: '',
        codeId,
        conceptId,
        descriptorId,
        publishable: this.atomFormPublishable(),
        suppressible: this.atomFormSuppressible(),
        workflowStatus: 'NEEDS_REVIEW',
      };
    }

    const request: EditAddAtomRequest = {
      activityId,
      atom,
      conceptId: concept.id,
      lastModified,
      overrideWarnings,
      projectId
    };

    this.atomFormPending.set(true);
    this.atomFormResult.set(null);

    const call$ = mode === 'edit'
      ? this.mutationApi.updateAtomOnConcept(request as EditUpdateAtomRequest)
      : this.mutationApi.addAtomToConcept(request);

    call$.pipe(finalize(() => this.atomFormPending.set(false))).subscribe({
      next: (result) => {
        this.atomFormResult.set(result);
        if (!validationBlocksCommit(result) && !validationNeedsWarningOverride(result)) {
          this.atomFormMode.set(null);
          this.refreshConcept();
        }
      },
      error: () => {
        this.notifications.error(`Atom could not be ${mode === 'edit' ? 'updated' : 'added'}.`);
      }
    });
  }

  protected openMoveDialog(): void {
    const currentId = this.loadedConcept()?.id;
    const peers = (
      (window.opener as any)?.__memeGetPeerConcepts?.(currentId) as ContentComponentDetail[]
    ) ?? [];
    this.movePeerConcepts.set(peers);
    this.moveTargetConceptId.set('');
    this.moveTargetConcept.set(peers.length === 1 ? peers[0] : null);
    this.moveLookupPending.set(false);
    this.moveLookupError.set(null);
    this.moveResult.set(null);
    this.moveDialogOpen.set(true);
  }

  protected cancelMoveDialog(): void {
    this.moveDialogOpen.set(false);
    this.moveResult.set(null);
  }

  protected lookupMoveConcept(idStr: string): void {
    this.moveTargetConceptId.set(idStr);
    this.moveTargetConcept.set(null);
    this.moveLookupError.set(null);
    const id = parseInt(idStr, 10);
    const projectId = this.projectId();
    if (!id || id <= 0 || !projectId) return;
    this.moveLookupPending.set(true);
    this.contentApi.getComponentById('concept', id, projectId).subscribe({
      next: (concept) => {
        this.moveLookupPending.set(false);
        if (concept) {
          this.moveTargetConcept.set(concept);
        } else {
          this.moveLookupError.set(`No concept found with ID ${id}.`);
        }
      },
      error: () => {
        this.moveLookupPending.set(false);
        this.moveLookupError.set(`Could not load concept ${id}.`);
      }
    });
  }

  protected submitMove(overrideWarnings = false): void {
    const projectId = this.projectId();
    const concept = this.loadedConcept();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId();
    const targetConceptId = this.moveTargetConcept()?.id;
    if (!projectId || !concept?.id || !lastModified || !targetConceptId) return;

    const atomIds = Array.from(this.selectedAtomIds());
    if (!atomIds.length) return;

    const request: EditMoveAtomsRequest = {
      projectId,
      conceptId: concept.id,
      activityId,
      lastModified,
      overrideWarnings,
      atomIds,
      conceptId2: targetConceptId,
    };

    this.movePending.set(true);
    this.moveResult.set(null);

    this.mutationApi.moveAtoms(request).pipe(finalize(() => this.movePending.set(false))).subscribe({
      next: (result) => {
        this.moveResult.set(result);
        if (!validationBlocksCommit(result) && !validationNeedsWarningOverride(result)) {
          this.moveDialogOpen.set(false);
          this.clearAtomSelections();
          this.refreshConcept();
        }
      },
      error: () => {
        this.notifications.error('Atoms could not be moved.');
      }
    });
  }

  protected openSplitDialog(): void {
    this.splitCopy.set(true);
    this.splitRelationshipType.set('RO');
    this.splitResult.set(null);
    this.splitDialogOpen.set(true);
  }

  protected cancelSplitDialog(): void {
    this.splitDialogOpen.set(false);
    this.splitResult.set(null);
  }

  protected submitSplit(overrideWarnings = false): void {
    const projectId = this.projectId();
    const concept = this.loadedConcept();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId();
    const atomIds = Array.from(this.selectedAtomIds());
    if (!projectId || !concept?.id || !lastModified || !atomIds.length) return;

    const request: EditSplitConceptRequest = {
      projectId,
      conceptId: concept.id,
      activityId,
      lastModified,
      overrideWarnings,
      atomIds,
      copyRelationships: this.splitCopy(),
      copySemanticTypes: this.splitCopy(),
      relationshipType: this.splitRelationshipType(),
    };

    this.splitPending.set(true);
    this.splitResult.set(null);

    const fromConceptId = concept.id as number;

    this.mutationApi.splitConcept(request).pipe(finalize(() => this.splitPending.set(false))).subscribe({
      next: (result) => {
        this.splitResult.set(result);
        if (!validationBlocksCommit(result) && !validationNeedsWarningOverride(result)) {
          this.splitDialogOpen.set(false);
          this.clearAtomSelections();
          const newIdComment = result.comments?.find(c => c.startsWith('newConceptId:'));
          const newConceptId = newIdComment ? parseInt(newIdComment.split(':')[1], 10) : null;
          if (newConceptId) this.broadcastConceptSplit(fromConceptId, newConceptId);
          this.refreshConcept();
        }
      },
      error: () => {
        this.notifications.error('Concept could not be split.');
      }
    });
  }

  protected openMergeDialog(): void {
    const currentId = this.loadedConcept()?.id;
    const peers = (
      (window.opener as any)?.__memeGetPeerConcepts?.(currentId) as ContentComponentDetail[]
    ) ?? [];
    this.mergePeerConcepts.set(peers);
    this.mergeTargetConceptId.set('');
    this.mergeTargetConcept.set(peers.length === 1 ? peers[0] : null);
    this.mergeLookupPending.set(false);
    this.mergeLookupError.set(null);
    this.mergeResult.set(null);
    this.mergeDialogOpen.set(true);
  }

  protected cancelMergeDialog(): void {
    this.mergeDialogOpen.set(false);
    this.mergeResult.set(null);
  }

  protected lookupMergeConcept(idStr: string): void {
    this.mergeTargetConceptId.set(idStr);
    this.mergeTargetConcept.set(null);
    this.mergeLookupError.set(null);
    const id = parseInt(idStr, 10);
    const projectId = this.projectId();
    if (!id || id <= 0 || !projectId) return;
    this.mergeLookupPending.set(true);
    this.contentApi.getComponentById('concept', id, projectId).subscribe({
      next: (concept) => {
        this.mergeLookupPending.set(false);
        if (concept) {
          this.mergeTargetConcept.set(concept);
        } else {
          this.mergeLookupError.set(`No concept found with ID ${id}.`);
        }
      },
      error: () => {
        this.mergeLookupPending.set(false);
        this.mergeLookupError.set(`Could not load concept ${id}.`);
      }
    });
  }

  protected submitMerge(overrideWarnings = false): void {
    const projectId = this.projectId();
    const concept = this.loadedConcept();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId();
    const targetConceptId = this.mergeTargetConcept()?.id;
    if (!projectId || !concept?.id || !lastModified || !targetConceptId) return;

    const fromConceptId = concept.id as number;
    const toConceptId = targetConceptId as number;

    const request: EditMergeConceptRequest = {
      projectId,
      conceptId: fromConceptId,
      activityId,
      lastModified,
      overrideWarnings,
      conceptId2: toConceptId,
    };

    this.mergePending.set(true);
    this.mergeResult.set(null);

    this.mutationApi.mergeConcepts(request).pipe(finalize(() => this.mergePending.set(false))).subscribe({
      next: (result) => {
        this.mergeResult.set(result);
        if (!validationBlocksCommit(result) && !validationNeedsWarningOverride(result)) {
          this.mergeDialogOpen.set(false);
          this.broadcastConceptMerged(fromConceptId, toConceptId);
          // FROM concept is deleted after merge; navigate to the surviving TO concept
          const url = new URL(window.location.href);
          url.searchParams.set('componentId', String(toConceptId));
          url.searchParams.delete('terminologyId');
          window.location.href = url.toString();
        }
      },
      error: () => {
        this.notifications.error('Concepts could not be merged.');
      }
    });
  }

  protected setRelationshipAddType(value: string): void {
    this.relationshipAddType.set(value);
    this.relationshipAddPendingRelationship.set(null);
    this.relationshipAddPendingRelationships.set(null);
    this.relationshipAddResult.set(null);
  }

  protected relationshipTypeOptionLabel(option: ContentKeyValuePair): string {
    const key = option.key?.trim() ?? '';
    const value = option.value?.trim() ?? '';

    return [key, value].filter(Boolean).join(' ');
  }

  protected atomLanguageOptionValue(option: ContentKeyValuePair): string {
    return option.key?.trim() || option.value?.trim() || '';
  }

  protected atomLanguageOptionLabel(option: ContentKeyValuePair): string {
    return option.value?.trim() || option.key?.trim() || '';
  }

  protected atomLanguageOptionTrack(option: ContentKeyValuePair, index: number): string {
    return option.key?.trim() || option.value?.trim() || String(index);
  }

  protected setRelationshipAddTargetConceptId(value: string): void {
    this.relationshipAddTargetConceptId.set(value);
    this.relationshipAddPendingRelationship.set(null);
    this.relationshipAddPendingRelationships.set(null);
    this.relationshipAddResult.set(null);
  }

  protected setSemanticTypeAddValue(value: string): void {
    this.semanticTypeAddValue.set(value);
    this.semanticTypeAddPendingValue.set(null);
    this.semanticTypeAddResult.set(null);
  }

  protected setRelationshipPage(page: number): void {
    if (page < 1 || page > this.relationshipTotalPages()) return;
    this.relationshipPage.set(page);
    this.loadRelationships();
  }

  protected setRelationshipPageSize(size: number): void {
    this.relationshipPageSize.set(Number(size));
    this.relationshipPage.set(1);
    this.loadRelationships();
  }

  protected toggleRelationshipSort(field: string): void {
    if (this.relationshipSortField() === field) {
      this.relationshipSortAscending.set(!this.relationshipSortAscending());
    } else {
      this.relationshipSortField.set(field);
      this.relationshipSortAscending.set(false);
    }
    this.relationshipPage.set(1);
    this.loadRelationships();
  }

  protected setRelationshipPreferredOnly(value: boolean): void {
    this.relationshipPreferredOnly.set(value);
    this.relationshipPage.set(1);
    this.loadRelationships();
  }

  protected toggleRelationshipSelection(relationship: ContentRelationship): void {
    if (!relationship.id) return;
    const ids = new Set(this.selectedRelationshipIds());
    if (ids.has(relationship.id)) {
      ids.delete(relationship.id);
    } else {
      ids.add(relationship.id);
    }
    this.selectedRelationshipIds.set(ids);
  }

  protected isRelationshipSelected(relationship: ContentRelationship): boolean {
    return Boolean(relationship.id && this.selectedRelationshipIds().has(relationship.id));
  }

  protected setContextPage(page: number): void {
    if (page < 1 || page > this.contextTotalPages()) return;
    this.contextPage.set(page);
    this.loadContextTreePositions();
  }

  protected setContextPageSize(size: number): void {
    this.contextPageSize.set(Number(size));
    this.contextPage.set(1);
    this.loadContextTreePositions();
  }

  protected toggleContextSort(field: string): void {
    if (this.contextSortField() === field) {
      this.contextSortAscending.set(!this.contextSortAscending());
    } else {
      this.contextSortField.set(field);
      this.contextSortAscending.set(false);
    }
    this.contextPage.set(1);
    this.loadContextTreePositions();
  }

  // --- Concept loading ---

  protected loadConcept(): void {
    const context = this.selectedContext();
    const projectId = this.projectId();

    if (!context) {
      this.conceptLoadError.set(
        'No concept context in query params. Open this window from the main Edit tab.'
      );
      return;
    }

    const terminology = context.terminology;
    const version = context.version;
    const terminologyId = context.terminologyId;
    const componentId = context.componentId ? Number(context.componentId) : null;

    const request =
      terminology && version && terminologyId
        ? this.contentApi.getComponentByTerminologyId(
            'CONCEPT', terminology, version, terminologyId, projectId
          )
        : componentId && Number.isFinite(componentId)
          ? this.contentApi.getComponentById('CONCEPT', componentId, projectId)
          : null;

    if (!request) {
      this.conceptLoadError.set(
        'Context does not have enough identifiers to load the concept.'
      );
      return;
    }

    this.loadingConcept.set(true);
    this.loadedConcept.set(null);
    this.conceptLoadError.set(null);

    request
      .pipe(finalize(() => this.loadingConcept.set(false)))
      .subscribe({
        next: (concept) => {
          if (!concept) {
            this.conceptLoadError.set('Concept could not be found.');
            return;
          }
          this.loadedConcept.set(concept);
          this.loadMetadata(concept);
          const workbench = String(
            this.route.snapshot.data?.['workbench'] ??
            this.route.snapshot.routeConfig?.data?.['workbench'] ??
            this.activeWorkbench()
          );
          if (workbench === 'semantic-types') {
            this.loadSemanticTypeOptions();
          }
        },
        error: () => {
          this.conceptLoadError.set('Concept could not be loaded.');
          this.notifications.error('Concept could not be loaded.');
        }
      });
  }

  private loadMetadata(concept: ContentComponentDetail): void {
    if (!concept.terminology || !concept.version) {
      this.metadata.set(null);
      return;
    }

    this.contentApi.getAllMetadata(concept.terminology, concept.version).subscribe({
      next: (metadata) => this.metadata.set(metadata),
      error: () => this.metadata.set(null)
    });
  }

  protected refreshConcept(): void {
    this.atomRemovalPendingAtom.set(null);
    this.atomRemovalResult.set(null);
    this.atomUpdatePendingAtom.set(null);
    this.atomUpdateResult.set(null);
    this.semanticTypeAddPendingValue.set(null);
    this.semanticTypeAddResult.set(null);
    this.semanticTypeRemovalPendingType.set(null);
    this.semanticTypeRemovalResult.set(null);
    this.relationshipAddPendingRelationship.set(null);
    this.relationshipAddResult.set(null);
    this.relationshipRemovalPendingRelationship.set(null);
    this.relationshipRemovalResult.set(null);
    this.loadConcept();
  }

  // --- STY available-list helpers ---

  protected setStyFilter(value: string): void {
    this.styFilter.set(value);
    this.styPage.set(1);
  }

  protected setStyPageSize(value: number): void {
    this.styPageSize.set(Number(value));
    this.styPage.set(1);
  }

  protected setStyPage(page: number): void {
    const total = this.styTotalPages();
    if (page >= 1 && page <= total) this.styPage.set(page);
  }

  protected setStySort(field: string): void {
    if (this.stySortField() === field) {
      this.stySortAscending.set(!this.stySortAscending());
    } else {
      this.stySortField.set(field);
      this.stySortAscending.set(false);
    }
    this.styPage.set(1);
  }

  protected addSemanticTypeRow(expandedForm: string | null | undefined): void {
    if (!expandedForm) return;
    this.setSemanticTypeAddValue(expandedForm);
    this.addSemanticTypeToConcept();
  }

  protected canApproveConcept(): boolean {
    return (
      !!this.projectEditingEnabled() &&
      !!this.loadedConcept()?.id &&
      !!this.conceptLastModified()
    );
  }

  protected approveConcept(): void {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const activityId = this.workbenchActivityId();
    const lastModified = this.conceptLastModified();
    if (!concept?.id || !projectId || !lastModified) return;
    const request: EditApproveConceptRequest = {
      activityId,
      conceptId: concept.id,
      lastModified,
      overrideWarnings: false,
      projectId
    };
    this.mutationApi.approveConcept(request).subscribe({
      next: (result) => {
        if (validationBlocksCommit(result)) {
          this.notifications.error('Concept approval failed validation.');
          return;
        }
        this.broadcastConceptApproved(concept.id!);
        this.refreshConcept();
      },
      error: () => this.notifications.error('Concept could not be approved.')
    });
  }

  protected canGoNext(): boolean {
    return !!this.selectedContext()?.worklistId;
  }

  protected goNext(): void {
    const context = this.selectedContext();
    const projectId = this.projectId();
    if (!context?.worklistId || !projectId) return;

    const worklistId = Number(context.worklistId);
    const recordId = Number(context.recordId);
    const isChecklist = context.isChecklist === 'true';

    this.loadingNext.set(true);
    const source$ = isChecklist
      ? this.workflowApi.findTrackingRecordsForChecklist(projectId, worklistId, buildContentPfs(1, 1000, 'clusterId', true, ''))
      : this.workflowApi.findTrackingRecordsForWorklist(projectId, worklistId, buildContentPfs(1, 1000, 'clusterId', true, ''));

    source$.pipe(finalize(() => this.loadingNext.set(false))).subscribe({
      next: (resp) => {
        const records = resp.records ?? resp.objects ?? [];
        const idx = records.findIndex((r) => r.id === recordId);
        if (idx < 0 || idx >= records.length - 1) return;
        const nextRecord = records[idx + 1];
        const nextConceptId = nextRecord.concepts?.[0]?.id;
        if (!nextConceptId) return;

        const url = new URL(window.location.href);
        url.searchParams.set('componentId', String(nextConceptId));
        url.searchParams.set('recordId', String(nextRecord.id));
        // Remove terminologyId so loadConcept() uses componentId, not the previous concept's CUI
        url.searchParams.delete('terminologyId');
        window.location.href = url.toString();
      },
      error: () => this.notifications.error('Could not load next concept.')
    });
  }

  protected goApproveNext(): void {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const lastModified = this.conceptLastModified();
    if (!concept?.id || !projectId || !lastModified) return;

    const request: EditApproveConceptRequest = {
      activityId: this.workbenchActivityId(),
      conceptId: concept.id,
      lastModified,
      overrideWarnings: false,
      projectId
    };
    this.mutationApi.approveConcept(request).subscribe({
      next: (result) => {
        if (validationBlocksCommit(result)) {
          this.notifications.error('Concept approval failed validation.');
          return;
        }
        this.broadcastConceptApproved(concept.id!);
        this.goNext();
      },
      error: () => this.notifications.error('Concept could not be approved.')
    });
  }

  // --- Semantic types workbench ---

  protected loadSemanticTypeOptions(): void {
    const terminology = this.conceptTerminology();
    const version = this.conceptVersion();

    if (!terminology || !version) {
      return;
    }

    this.loadingSemanticTypeOptions.set(true);
    this.semanticTypeOptionsError.set(null);

    this.contentApi
      .getSemanticTypes(terminology, version)
      .pipe(finalize(() => this.loadingSemanticTypeOptions.set(false)))
      .subscribe({
        next: (options) => {
          this.semanticTypeOptions.set(
            [...options].sort((a, b) =>
              this.semanticTypeOptionDisplay(a).localeCompare(this.semanticTypeOptionDisplay(b))
            )
          );
        },
        error: () => {
          this.semanticTypeOptionsError.set('Semantic type options could not be loaded.');
        }
      });
  }

  protected addSemanticTypeToConcept(overrideWarnings = false): void {
    const request = this.buildAddSemanticTypeRequest(overrideWarnings);
    if (!request || !this.semanticTypeAddReadiness().canExecute) {
      return;
    }
    const label = overrideWarnings ? 'Override warnings and add' : 'Add';
    if (!window.confirm(`${label} semantic type "${request.semanticType}"?`)) {
      return;
    }
    this.addingSemanticType.set(true);
    this.semanticTypeAddResult.set(null);
    this.mutationApi
      .addSemanticTypeToConcept(request)
      .pipe(finalize(() => this.addingSemanticType.set(false)))
      .subscribe({
        next: (result) => {
          this.semanticTypeAddResult.set(result);
          if (validationBlocksCommit(result)) {
            this.semanticTypeAddPendingValue.set(null);
            this.notifications.error('Semantic type add failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.semanticTypeAddPendingValue.set(request.semanticType);
            this.notifications.error(
              'Semantic type add returned warnings. Review and override to continue.'
            );
            return;
          }
          this.semanticTypeAddPendingValue.set(null);
          this.semanticTypeAddValue.set('');
          this.loadConcept();
        },
        error: () => {
          this.notifications.error('Semantic type could not be added.');
        }
      });
  }

  protected canRemoveSemanticType(type: ContentSemanticType): boolean {
    if (this.removingSemanticTypeId() !== null || !type.id) {
      return false;
    }
    return buildSemanticTypeMutationReadiness(
      this.projectId(),
      this.loadedConcept()?.id,
      type.id,
      this.workbenchActivityId(),
      this.conceptLastModified(),
      this.projectRole(),
      this.projectEditingEnabled() !== false
    ).canExecute;
  }

  protected removeSemanticTypeFromConcept(
    type: ContentSemanticType,
    overrideWarnings = false
  ): void {
    const request = this.buildRemoveSemanticTypeRequest(type, overrideWarnings);
    if (!request || !this.canRemoveSemanticType(type)) {
      return;
    }
    const label = overrideWarnings ? 'Override warnings and remove' : 'Remove';
    if (!window.confirm(`${label} semantic type "${type.semanticType}"?`)) {
      return;
    }
    this.removingSemanticTypeId.set(type.id ?? null);
    this.semanticTypeRemovalResult.set(null);
    this.mutationApi
      .removeSemanticTypeFromConcept(request)
      .pipe(finalize(() => this.removingSemanticTypeId.set(null)))
      .subscribe({
        next: (result) => {
          this.semanticTypeRemovalResult.set(result);
          if (validationBlocksCommit(result)) {
            this.semanticTypeRemovalPendingType.set(null);
            this.notifications.error('Semantic type removal failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.semanticTypeRemovalPendingType.set(type);
            this.notifications.error(
              'Semantic type removal returned warnings. Review and override to continue.'
            );
            return;
          }
          this.semanticTypeRemovalPendingType.set(null);
          this.loadConcept();
        },
        error: () => {
          this.notifications.error('Semantic type could not be removed.');
        }
      });
  }

  // --- Atoms workbench ---

  protected canUpdateAtomStatus(atom: ContentAtom): boolean {
    if (this.updatingAtomId() !== null || !atom.id || !this.atomUpdateStatus().trim()) {
      return false;
    }
    return buildAtomMutationReadiness(
      this.projectId(),
      this.loadedConcept()?.id,
      atom.id,
      this.workbenchActivityId(),
      this.conceptLastModified(),
      this.projectRole(),
      this.projectEditingEnabled() !== false
    ).canExecute;
  }

  protected updateAtomStatus(atom: ContentAtom, overrideWarnings = false): void {
    const request = this.buildUpdateAtomStatusRequest(atom, overrideWarnings);
    if (!request || !this.canUpdateAtomStatus(atom)) {
      return;
    }
    const workflowStatus = this.atomUpdateStatus();
    const label = overrideWarnings ? 'Override warnings and update' : 'Update';
    if (!window.confirm(`${label} atom "${this.atomDisplay(atom)}" status to "${workflowStatus}"?`)) {
      return;
    }
    this.updatingAtomId.set(atom.id ?? null);
    this.atomUpdateResult.set(null);
    this.mutationApi
      .updateAtomOnConcept(request)
      .pipe(finalize(() => this.updatingAtomId.set(null)))
      .subscribe({
        next: (result) => {
          this.atomUpdateResult.set(result);
          if (validationBlocksCommit(result)) {
            this.atomUpdatePendingAtom.set(null);
            this.notifications.error('Atom status update failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.atomUpdatePendingAtom.set(request.atom);
            this.notifications.error(
              'Atom status update returned warnings. Review and override to continue.'
            );
            return;
          }
          this.atomUpdatePendingAtom.set(null);
          this.loadConcept();
        },
        error: () => {
          this.notifications.error('Atom status could not be updated.');
        }
      });
  }

  protected canRemoveAtom(atom: ContentAtom): boolean {
    if (this.removingAtomId() !== null || !atom.id) {
      return false;
    }
    return buildAtomMutationReadiness(
      this.projectId(),
      this.loadedConcept()?.id,
      atom.id,
      this.workbenchActivityId(),
      this.conceptLastModified(),
      this.projectRole(),
      this.projectEditingEnabled() !== false
    ).canExecute;
  }

  protected removeAtomFromConcept(atom: ContentAtom, overrideWarnings = false): void {
    const request = this.buildRemoveAtomRequest(atom, overrideWarnings);
    if (!request || !this.canRemoveAtom(atom)) {
      return;
    }
    const label = overrideWarnings ? 'Override warnings and remove' : 'Remove';
    if (!window.confirm(`${label} atom "${this.atomDisplay(atom)}"?`)) {
      return;
    }
    this.removingAtomId.set(atom.id ?? null);
    this.atomRemovalResult.set(null);
    this.mutationApi
      .removeAtomFromConcept(request)
      .pipe(finalize(() => this.removingAtomId.set(null)))
      .subscribe({
        next: (result) => {
          this.atomRemovalResult.set(result);
          if (validationBlocksCommit(result)) {
            this.atomRemovalPendingAtom.set(null);
            this.notifications.error('Atom removal failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.atomRemovalPendingAtom.set(atom);
            this.notifications.error(
              'Atom removal returned warnings. Review and override to continue.'
            );
            return;
          }
          this.atomRemovalPendingAtom.set(null);
          this.loadConcept();
        },
        error: () => {
          this.notifications.error('Atom could not be removed.');
        }
      });
  }

  // --- Relationships workbench ---

  protected loadRelationships(): void {
    const concept = this.loadedConcept();
    const terminology = this.conceptTerminology();
    const version = this.conceptVersion();
    const terminologyId = concept?.terminologyId;

    if (!terminology || !version || !terminologyId) {
      this.relationshipError.set('Concept must be loaded with terminology identifiers.');
      return;
    }

    this.relationshipError.set(null);
    this.loadingRelationships.set(true);
    this.contentApi
      .findDeepRelationships(
        terminology,
        version,
        terminologyId,
        buildContentPfs(
          this.relationshipPage(),
          this.relationshipPageSize(),
          this.relationshipSortField(),
          this.relationshipSortAscending(),
          ''
        ),
        {
          includeConceptRels: true,
          includeSelfReferential: false,
          inverseFlag: true,
          preferredOnly: this.relationshipPreferredOnly()
        }
      )
      .pipe(finalize(() => this.loadingRelationships.set(false)))
      .subscribe({
        next: (response) => {
          this.relationships.set(response.items);
          this.relationshipCount.set(response.totalCount);
          const visibleIds = new Set(response.items.map((rel) => rel.id).filter(Boolean));
          this.selectedRelationshipIds.update((ids) =>
            new Set(Array.from(ids).filter((id) => visibleIds.has(id)))
          );
        },
        error: () => {
          this.relationshipError.set('Relationships could not be loaded.');
          this.notifications.error('Relationships could not be loaded.');
        }
      });
  }

  protected relationshipLevel(rel: ContentRelationship): string {
    if (rel.workflowStatus === 'DEMOTION') return 'P';
    if (rel.terminology && rel.fromTerminology && rel.terminology === rel.fromTerminology) {
      return 'C';
    }
    return 'S';
  }

  protected relationshipRowClass(rel: ContentRelationship): string {
    if (rel.workflowStatus === 'DEMOTION') return 'DEMOTION';
    if (rel.workflowStatus === 'NEEDS_REVIEW') return 'NEEDS_REVIEW';
    if (rel.publishable === false) return 'UNRELEASABLE';
    if (rel.obsolete) return 'OBSOLETE';
    if (rel.terminology === 'RXNORM') return 'RXNORM';
    return '';
  }

  protected canDeleteRelationship(rel: ContentRelationship): boolean {
    return (
      this.canRemoveRelationship(rel) &&
      this.relationshipLevel(rel) !== 'S' &&
      rel.workflowStatus !== 'DEMOTION' &&
      this.projectEditingEnabled() === true
    );
  }

  protected openAddRelationshipDialog(): void {
    const concept = this.loadedConcept();
    const selectedFromIds = new Set(
      this.selectedRelationships()
        .map((relationship) => relationship.fromId)
        .filter((id): id is number => Boolean(id))
    );
    const peerConcepts =
      ((window.opener as any)?.__memeGetPeerConcepts?.(concept?.id) as ContentComponentDetail[] | undefined) ?? [];
    const targets = new Map<number, RelationshipTargetConcept>();

    peerConcepts.forEach((peer) => {
      if (!peer.id || peer.id === concept?.id) return;
      targets.set(peer.id, { ...peer, selected: selectedFromIds.has(peer.id) });
    });
    this.selectedRelationships().forEach((relationship) => {
      if (!relationship.fromId || relationship.fromId === concept?.id) return;
      if (targets.has(relationship.fromId)) {
        targets.set(relationship.fromId, {
          ...targets.get(relationship.fromId)!,
          selected: true
        });
        return;
      }
      targets.set(relationship.fromId, {
        id: relationship.fromId,
        name: relationship.fromName,
        terminology: relationship.fromTerminology,
        terminologyId: relationship.fromTerminologyId,
        type: 'CONCEPT',
        version: relationship.fromVersion,
        selected: true
      });
    });

    this.relationshipAddTargets.set(Array.from(targets.values()));
    this.relationshipAddManualTargetId.set('');
    this.relationshipAddManualTargetError.set(null);
    this.relationshipAddResult.set(null);
    this.relationshipAddPendingRelationship.set(null);
    this.relationshipAddPendingRelationships.set(null);
    this.addRelationshipDialogOpen.set(true);
  }

  protected closeAddRelationshipDialog(): void {
    this.addRelationshipDialogOpen.set(false);
    this.relationshipAddPendingRelationship.set(null);
    this.relationshipAddPendingRelationships.set(null);
    this.relationshipAddResult.set(null);
  }

  protected setRelationshipTargetSelected(target: RelationshipTargetConcept, selected: boolean): void {
    if (!target.id) return;
    this.relationshipAddTargets.update((targets) =>
      targets.map((candidate) =>
        candidate.id === target.id ? { ...candidate, selected } : candidate
      )
    );
    this.relationshipAddPendingRelationships.set(null);
    this.relationshipAddResult.set(null);
  }

  protected addManualRelationshipTarget(): void {
    const targetId = this.parsePositiveInteger(this.relationshipAddManualTargetId());
    const projectId = this.projectId();
    const conceptId = this.loadedConcept()?.id;

    this.relationshipAddManualTargetError.set(null);
    if (!targetId || !projectId) {
      this.relationshipAddManualTargetError.set('Enter a concept id.');
      return;
    }
    if (targetId === conceptId) {
      this.relationshipAddManualTargetError.set('The current concept cannot be a target.');
      return;
    }
    if (this.relationshipAddTargets().some((target) => target.id === targetId)) {
      this.relationshipAddTargets.update((targets) =>
        targets.map((target) =>
          target.id === targetId ? { ...target, selected: true } : target
        )
      );
      this.relationshipAddManualTargetId.set('');
      return;
    }

    this.contentApi.getComponentById('CONCEPT', targetId, projectId).subscribe({
      next: (target) => {
        if (!target) {
          this.relationshipAddManualTargetError.set(`No concept found with id ${targetId}.`);
          return;
        }
        this.relationshipAddTargets.update((targets) => [
          ...targets,
          { ...target, selected: true }
        ]);
        this.relationshipAddManualTargetId.set('');
      },
      error: () => {
        this.relationshipAddManualTargetError.set(`Could not load concept ${targetId}.`);
      }
    });
  }

  protected openRelationshipFinderDialog(): void {
    this.relationshipFinderQuery.set('');
    this.relationshipFinderResults.set([]);
    this.relationshipFinderPage.set(1);
    this.relationshipFinderError.set(null);
    this.relationshipFinderDialogOpen.set(true);
  }

  protected closeRelationshipFinderDialog(): void {
    this.relationshipFinderDialogOpen.set(false);
  }

  protected searchRelationshipFinder(): void {
    const query = this.relationshipFinderQuery().trim();
    const terminology = this.conceptTerminology();
    const version = this.conceptVersion();
    const conceptId = this.loadedConcept()?.id;

    this.relationshipFinderError.set(null);
    if (!query || !terminology || !version) {
      this.relationshipFinderError.set('Enter a search query.');
      this.relationshipFinderResults.set([]);
      return;
    }

    this.searchingRelationshipFinder.set(true);
    this.contentApi
      .findComponents(
        'CONCEPT',
        terminology,
        version,
        query,
        buildContentSearchPfs(
          this.relationshipFinderPage(),
          this.relationshipFinderPageSize,
          '',
          false,
          'CONCEPT'
        )
      )
      .pipe(finalize(() => this.searchingRelationshipFinder.set(false)))
      .subscribe({
        next: (response) => {
          this.relationshipFinderResults.set(
            response.items.filter((result) => result.id !== conceptId)
          );
        },
        error: () => {
          this.relationshipFinderResults.set([]);
          this.relationshipFinderError.set('Finder search could not be loaded.');
        }
      });
  }

  protected addRelationshipFinderTarget(result: ContentSearchResult): void {
    const targetId = result.id;
    const projectId = this.projectId();
    const conceptId = this.loadedConcept()?.id;

    if (!targetId || !projectId || targetId === conceptId) {
      return;
    }

    if (this.relationshipAddTargets().some((target) => target.id === targetId)) {
      this.relationshipAddTargets.update((targets) =>
        targets.map((target) =>
          target.id === targetId ? { ...target, selected: true } : target
        )
      );
      this.closeRelationshipFinderDialog();
      return;
    }

    this.contentApi.getComponentById('CONCEPT', targetId, projectId).subscribe({
      next: (target) => {
        if (!target) {
          this.relationshipFinderError.set(`No concept found with id ${targetId}.`);
          return;
        }
        this.relationshipAddTargets.update((targets) => [
          ...targets,
          { ...target, selected: true }
        ]);
        this.closeRelationshipFinderDialog();
      },
      error: () => {
        this.relationshipFinderError.set(`Could not load concept ${targetId}.`);
      }
    });
  }

  protected relationshipFinderResultName(result: ContentSearchResult): string {
    return result.value || result.name || result.terminologyId || `#${result.id}`;
  }

  protected addRelationshipToConcept(overrideWarnings = false): void {
    const request = this.buildAddRelationshipRequest(overrideWarnings);
    if (!request || !this.relationshipAddReadiness().canExecute) {
      return;
    }
    const rel = request.relationship;
    const label = overrideWarnings ? 'Override warnings and add' : 'Add';
    if (!window.confirm(`${label} ${rel.relationshipType} relationship to concept ${rel.toId}?`)) {
      return;
    }
    this.addingRelationship.set(true);
    this.relationshipAddResult.set(null);
    this.mutationApi
      .addRelationshipToConcept(request)
      .pipe(finalize(() => this.addingRelationship.set(false)))
      .subscribe({
        next: (result) => {
          this.relationshipAddResult.set(result);
          if (validationBlocksCommit(result)) {
            this.relationshipAddPendingRelationship.set(null);
            this.notifications.error('Relationship add failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.relationshipAddPendingRelationship.set(rel);
            this.notifications.error(
              'Relationship add returned warnings. Review and override to continue.'
            );
            return;
          }
          this.relationshipAddPendingRelationship.set(null);
          this.relationshipAddTargetConceptId.set('');
          this.loadConcept();
        },
        error: () => {
          this.notifications.error('Relationship could not be added.');
        }
      });
  }

  protected addSelectedRelationshipsToConcept(overrideWarnings = false): void {
    const request = this.buildAddRelationshipsRequest(overrideWarnings);
    const selectedType = this.relationshipAddType().trim();

    if (!request) {
      this.relationshipAddManualTargetError.set('Select at least one other concept.');
      return;
    }

    const actionLabel = overrideWarnings ? 'Override warnings and add' : 'Add';
    if (
      !window.confirm(
        `${actionLabel} ${request.relationships.length} relationship(s) of type "${selectedType}"?`
      )
    ) {
      return;
    }

    const request$ =
      overrideWarnings && this.relationshipAddPendingRelationships()
        ? this.mutationApi.addRelationshipsToConcept(request)
        : this.contentApi
            .getInverseRelationshipType(
              request.relationships[0]?.terminology || this.conceptTerminology(),
              request.relationships[0]?.version || this.conceptVersion(),
              selectedType
            )
            .pipe(
              switchMap((inverseRelationshipType) =>
                this.mutationApi.addRelationshipsToConcept({
                  ...request,
                  relationships: request.relationships.map((relationship) => ({
                    ...relationship,
                    relationshipType:
                      inverseRelationshipType.trim() || relationship.relationshipType
                  }))
                })
              )
            );

    this.addingRelationship.set(true);
    this.relationshipAddResult.set(null);
    request$
      .pipe(finalize(() => this.addingRelationship.set(false)))
      .subscribe({
        next: (result) => {
          this.relationshipAddResult.set(result);
          if (validationBlocksCommit(result)) {
            this.relationshipAddPendingRelationships.set(null);
            this.notifications.error('Relationship add failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.relationshipAddPendingRelationships.set(request.relationships);
            this.notifications.error('Relationship add returned warnings. Review and override to continue.');
            return;
          }

          this.relationshipAddPendingRelationships.set(null);
          this.addRelationshipDialogOpen.set(false);
          this.loadRelationships();
          this.refreshConcept();
        },
        error: () => {
          this.notifications.error('Relationships could not be added.');
        }
      });
  }

  protected canRemoveRelationship(rel: ContentRelationship): boolean {
    if (this.removingRelationshipId() !== null || !rel.id) {
      return false;
    }
    return buildRelationshipMutationReadiness(
      this.projectId(),
      this.loadedConcept()?.id,
      rel.id,
      this.workbenchActivityId(),
      this.conceptLastModified(),
      this.projectRole(),
      this.projectEditingEnabled() !== false
    ).canExecute;
  }

  protected removeRelationshipFromConcept(
    rel: ContentRelationship,
    overrideWarnings = false
  ): void {
    if (!this.canRemoveRelationship(rel) || !rel.id) {
      return;
    }
    const label = overrideWarnings ? 'Override warnings and remove' : 'Remove';
    if (
      !window.confirm(
        `${label} ${rel.relationshipType} relationship to ${rel.toTerminologyId || rel.toId}?`
      )
    ) {
      return;
    }
    this.removingRelationshipId.set(rel.id ?? null);
    this.relationshipRemovalResult.set(null);
    this.relationshipRemovalContext(rel, overrideWarnings)
      .pipe(
        switchMap((request) => this.mutationApi.removeRelationshipFromConcept(request)),
        finalize(() => this.removingRelationshipId.set(null))
      )
      .subscribe({
        next: (result) => {
          this.relationshipRemovalResult.set(result);
          if (validationBlocksCommit(result)) {
            this.relationshipRemovalPendingRelationship.set(null);
            this.notifications.error('Relationship removal failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.relationshipRemovalPendingRelationship.set(rel);
            this.notifications.error(
              'Relationship removal returned warnings. Review and override to continue.'
            );
            return;
          }
          this.relationshipRemovalPendingRelationship.set(null);
          this.loadRelationships();
          this.refreshConcept();
        },
        error: () => {
          this.notifications.error('Relationship could not be removed.');
        }
      });
  }

  protected transferSelectedRelationshipsToEditor(): void {
    const conceptIds = this.selectedRelationships()
      .map((relationship) => relationship.fromId)
      .filter((id): id is number => Boolean(id));

    if (!conceptIds.length) {
      return;
    }

    (window.opener as Window | null)?.postMessage(
      { type: 'concept-transfer', conceptIds },
      window.location.origin
    );
  }

  // --- Code concepts workbench ---

  protected findCodeConcepts(atom: ContentAtom): void {
    const codeId = atom.codeId?.trim();
    const terminology = this.conceptTerminology();
    const version = this.conceptVersion();

    if (!codeId || !terminology || !version) {
      return;
    }

    this.codeConceptTarget.set(atom);
    this.codeConceptResults.set([]);
    this.codeConceptTotalCount.set(0);
    this.codeConceptError.set(null);
    this.loadingCodeConcepts.set(true);

    this.contentApi
      .findComponents(
        'CONCEPT',
        terminology,
        version,
        `atoms.codeId:${codeId}`,
        buildContentPfs(1, 25, 'name', true, '')
      )
      .pipe(finalize(() => this.loadingCodeConcepts.set(false)))
      .subscribe({
        next: (response) => {
          this.codeConceptResults.set(response.items);
          this.codeConceptTotalCount.set(response.totalCount);
        },
        error: () => {
          this.codeConceptError.set('Code concepts could not be loaded.');
          this.notifications.error('Code concepts could not be loaded.');
        }
      });
  }

  // --- Contexts workbench ---

  protected loadContextTreePositions(): void {
    const concept = this.loadedConcept();
    const terminology = this.conceptTerminology();
    const version = this.conceptVersion();
    const terminologyId = concept?.terminologyId;

    if (!terminology || !version || !terminologyId) {
      this.contextTreePositionError.set(
        'Concept must be loaded with terminology identifiers.'
      );
      return;
    }

    this.contextTreePositionError.set(null);
    this.contextTreePositions.set([]);
    this.contextTreePositionCount.set(0);
    this.selectedContextTreePosition.set(null);
    this.contextTree.set(null);
    this.contextTreeCount.set(0);
    this.contextTreeViewed.set(0);
    this.loadingContextTreePositions.set(true);

    this.contentApi
      .findDeepTreePositions(
        terminology,
        version,
        terminologyId,
        '',
        buildContentPfs(
          this.contextPage(),
          this.contextPageSize(),
          this.contextSortField(),
          this.contextSortAscending(),
          ''
        )
      )
      .pipe(finalize(() => this.loadingContextTreePositions.set(false)))
      .subscribe({
        next: (response) => {
          this.contextTreePositions.set(response.items);
          this.contextTreePositionCount.set(response.totalCount);
          if (response.items.length) {
            this.selectContextTreePosition(response.items[0]);
          }
        },
        error: () => {
          this.contextTreePositionError.set('Context tree positions could not be loaded.');
          this.notifications.error('Context tree positions could not be loaded.');
        }
      });
  }

  protected selectContextTreePosition(position: ContentTreePosition): void {
    this.selectedContextTreePosition.set(position);
    this.loadContextTree(0);
  }

  protected isContextPositionSelected(position: ContentTreePosition): boolean {
    return this.selectedContextTreePosition() === position;
  }

  protected loadContextTree(offset: number): void {
    const position = this.selectedContextTreePosition();

    if (!position) {
      return;
    }

    const type = String(position.type || 'CONCEPT').toUpperCase();
    const pfs = buildContentPfs(offset + 1, 1, 'ancestorPath', true, '');
    const request =
      type === 'ATOM' && position.nodeId
        ? this.contentApi.findAtomTrees(position.nodeId, pfs)
        : this.contentApi.findTrees(
            type,
            position.nodeTerminology || position.terminology || '',
            position.nodeVersion || position.version || '',
            position.nodeTerminologyId || '',
            pfs
          );

    this.contextTreeError.set(null);
    this.contextTree.set(null);
    this.treeNodeViews.set({});
    this.loadingContextTree.set(true);
    request
      .pipe(finalize(() => this.loadingContextTree.set(false)))
      .subscribe({
        next: (response) => {
          const tree = response.items[0] ?? null;
          this.contextTree.set(tree);
          this.contextTreeCount.set(response.totalCount);
          this.contextTreeViewed.set(tree ? offset : 0);
          if (tree) {
            this.treeNodeViews.set({ [this.treeNodeKey(tree)]: { expanded: true, loaded: true } });
            this.loadInitialSiblingChildren(tree);
          }
        },
        error: () => {
          this.contextTreeError.set('Hierarchy tree could not be loaded.');
          this.notifications.error('Hierarchy tree could not be loaded.');
        }
      });
  }

  protected loadContextTreeByOffset(offset: number): void {
    const count = this.contextTreeCount();
    if (!count) return;
    let next = this.contextTreeViewed() + offset;
    if (next >= count) next -= count;
    if (next < 0) next += count;
    this.loadContextTree(next);
  }

  protected treeNodeKey(tree: ContentTree): string {
    return `${tree.nodeId ?? ''}|${tree.nodeTerminologyId ?? ''}|${tree.nodeName ?? ''}`;
  }

  protected treeNodeExpanded(tree: ContentTree): boolean {
    return this.treeNodeViews()[this.treeNodeKey(tree)]?.expanded !== false;
  }

  protected treeChildren(tree: ContentTree | null | undefined): ContentTree[] {
    return Array.from(tree?.children ?? []);
  }

  protected toggleTreeNode(tree: ContentTree): void {
    const key = this.treeNodeKey(tree);
    const view = this.treeNodeViews()[key] ?? { expanded: true, loaded: true };
    const childCount = tree.childCt ?? 0;
    const childLength = tree.children?.length ?? 0;

    if (!view.expanded) {
      this.treeNodeViews.update((views) => ({
        ...views,
        [key]: { ...view, expanded: true }
      }));
      return;
    }

    if (childCount > childLength && childLength < 10) {
      this.loadTreeChildren(tree);
      return;
    }

    this.treeNodeViews.update((views) => ({
      ...views,
      [key]: { ...view, expanded: false }
    }));
  }

  protected loadMoreTreeChildren(tree: ContentTree): void {
    this.loadTreeChildren(tree);
  }

  protected treeNodeIcon(tree: ContentTree): MemeIconName {
    const childCount = tree.childCt ?? 0;
    const childLength = tree.children?.length ?? 0;
    if (!childCount) return 'dash';
    if (!this.treeNodeExpanded(tree) || childLength === 0) return 'chevron-right';
    if (childLength !== childCount && childLength < 10) return 'plus';
    return 'chevron-down';
  }

  protected isCurrentTreeNode(tree: ContentTree): boolean {
    return Boolean(
      tree.nodeTerminologyId &&
      tree.nodeTerminologyId === this.selectedContextTreePosition()?.nodeTerminologyId
    );
  }

  private loadInitialSiblingChildren(tree: ContentTree): void {
    let parentTree = tree;
    while (parentTree.children?.length) {
      const firstChild = parentTree.children[0];
      if (!firstChild.children?.length) {
        break;
      }
      parentTree = firstChild;
    }
    this.loadTreeChildren(parentTree, true);
  }

  private loadTreeChildren(tree: ContentTree, mergeWithExisting = false): void {
    const selectedPosition = this.selectedContextTreePosition();
    if (!selectedPosition) return;
    const type = String(selectedPosition.type || 'CONCEPT').toUpperCase();
    const startIndex = Math.max(0, (tree.children?.length ?? 0) - 1);
    const key = this.treeNodeKey(tree);

    this.loadingTreeChildrenKey.set(key);
    this.contentApi
      .findTreeChildren(type, tree, {
        ascending: true,
        maxResults: 10,
        queryRestriction: undefined,
        sortField: 'nodeName',
        startIndex
      })
      .pipe(finalize(() => this.loadingTreeChildrenKey.set(null)))
      .subscribe({
        next: (response) => {
          const existing = mergeWithExisting ? Array.from(tree.children ?? []) : Array.from(tree.children ?? []);
          tree.children = this.concatTreeChildren(existing, response.items);
          this.contextTree.update((current) => (current ? { ...current } : current));
          this.treeNodeViews.update((views) => ({
            ...views,
            [key]: { expanded: true, loaded: true }
          }));
        },
        error: () => {
          this.contextTreeError.set('Tree children could not be loaded.');
        }
      });
  }

  private concatTreeChildren(existing: ContentTree[], incoming: ContentTree[]): ContentTree[] {
    const currentTerminologyId = this.selectedContextTreePosition()?.nodeTerminologyId;
    const existingIds = new Set(existing.map((item) => item.nodeTerminologyId || item.nodeId));
    return [...existing, ...incoming.filter((item) => !existingIds.has(item.nodeTerminologyId || item.nodeId))]
      .sort((a, b) => {
        if (a.nodeTerminologyId === currentTerminologyId) return -1;
        if (b.nodeTerminologyId === currentTerminologyId) return 1;
        return String(a.nodeName ?? '').localeCompare(String(b.nodeName ?? ''));
      });
  }

  // --- Display helpers ---

  protected atomDisplay(atom: ContentAtom): string {
    return (
      [atom.name, atom.termType, atom.terminology].filter(Boolean).join(' / ') ||
      String(atom.id ?? 'n/a')
    );
  }

  protected semanticTypeOptionDisplay(opt: ContentSemanticTypeMetadata): string {
    const label = opt.expandedForm || opt.abbreviation || opt.typeId || 'n/a';
    return opt.typeId && opt.typeId !== label ? `${label} [${opt.typeId}]` : label;
  }

  protected semanticTypeOptionValue(opt: ContentSemanticTypeMetadata): string {
    return opt.expandedForm || opt.abbreviation || opt.typeId || '';
  }

  protected contextPositionDisplay(pos: ContentTreePosition): string {
    return (
      [pos.nodeTerminologyId, pos.nodeName].filter(Boolean).join(' ') ||
      pos.terminology ||
      'n/a'
    );
  }

  // --- Private request builders ---

  private toEpochMillis(value: string | number | null | undefined): number | null {
    if (typeof value === 'number') {
      return Number.isFinite(value) ? value : null;
    }
    if (!value) {
      return null;
    }
    const numeric = Number(value);
    if (Number.isFinite(numeric)) {
      return numeric;
    }
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private parsePositiveInteger(value: string | number | null | undefined): number | null {
    const parsed = Number(String(value ?? '').trim());
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }

  private loadProjectContext(): void {
    const projectId = this.projectId();

    this.projectEditingEnabled.set(null);
    this.projectContextError.set(null);
    if (!projectId) {
      return;
    }

    this.loadingProjectContext.set(true);
    this.operationsApi
      .getProject(projectId)
      .pipe(finalize(() => this.loadingProjectContext.set(false)))
      .subscribe({
        next: (project) => {
          this.projectEditingEnabled.set(project.editingEnabled === true);
          this.newAtomTermgroups.set(project.newAtomTermgroups ?? []);
        },
        error: () => {
          this.projectContextError.set('Project context could not be loaded.');
          this.projectEditingEnabled.set(null);
        }
      });
  }

  private buildActionRequest(): EditUndoRedoRequest | null {
    const projectId = this.projectId();
    const molecularActionId = this.actionMolecularActionIdValue();
    const activityId = this.actionActivityId().trim();

    if (!projectId || !molecularActionId || !activityId) {
      return null;
    }

    return {
      activityId,
      force: this.actionForce(),
      molecularActionId,
      projectId
    };
  }

  private buildAddSemanticTypeRequest(
    overrideWarnings: boolean
  ): EditAddSemanticTypeRequest | null {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId().trim();
    const pendingValue = overrideWarnings ? this.semanticTypeAddPendingValue() : null;
    const semanticType = pendingValue || this.semanticTypeAddValue().trim();

    if (!projectId || !concept?.id || !semanticType || !lastModified) {
      return null;
    }

    return { activityId, conceptId: concept.id, lastModified, overrideWarnings, projectId, semanticType };
  }

  private buildRemoveSemanticTypeRequest(
    type: ContentSemanticType,
    overrideWarnings: boolean
  ): EditRemoveSemanticTypeRequest | null {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId().trim();

    if (!projectId || !concept?.id || !type.id || !lastModified) {
      return null;
    }

    return {
      activityId,
      conceptId: concept.id,
      lastModified,
      overrideWarnings,
      projectId,
      semanticTypeId: type.id
    };
  }

  private buildUpdateAtomStatusRequest(
    atom: ContentAtom,
    overrideWarnings: boolean
  ): EditUpdateAtomRequest | null {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId().trim();
    const workflowStatus = this.atomUpdateStatus().trim();
    const pendingAtom = overrideWarnings ? this.atomUpdatePendingAtom() : null;
    const updatedAtom =
      pendingAtom && pendingAtom.id === atom.id
        ? pendingAtom
        : { ...atom, workflowStatus };

    if (!projectId || !concept?.id || !atom.id || !lastModified || !activityId || !workflowStatus) {
      return null;
    }

    return { activityId, atom: updatedAtom, conceptId: concept.id, lastModified, overrideWarnings, projectId };
  }

  private buildRemoveAtomRequest(
    atom: ContentAtom,
    overrideWarnings: boolean
  ): EditRemoveAtomRequest | null {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId().trim();

    if (!projectId || !concept?.id || !atom.id || !lastModified || !activityId) {
      return null;
    }

    return { activityId, atomId: atom.id, conceptId: concept.id, lastModified, overrideWarnings, projectId };
  }

  private buildAddRelationshipRequest(
    overrideWarnings: boolean
  ): EditAddRelationshipRequest | null {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId().trim();
    const pendingRelationship = overrideWarnings
      ? this.relationshipAddPendingRelationship()
      : null;
    const targetId =
      pendingRelationship?.toId ??
      this.parsePositiveInteger(this.relationshipAddTargetConceptId());
    const relationshipType = this.relationshipAddType().trim();

    if (
      !projectId || !concept?.id || !targetId ||
      !concept.terminology || !concept.version ||
      !lastModified || !activityId || !relationshipType
    ) {
      return null;
    }

    const selectedTarget = this.relationshipAddTargets().find((target) => target.id === targetId) ?? null;
    const relationship: ContentRelationship = pendingRelationship ?? this.buildRelationshipPayload(
      concept,
      targetId,
      relationshipType,
      selectedTarget
    );

    return { activityId, conceptId: concept.id, lastModified, overrideWarnings, projectId, relationship };
  }

  private buildAddRelationshipsRequest(
    overrideWarnings: boolean
  ): EditAddRelationshipsRequest | null {
    const pendingRelationships = overrideWarnings
      ? this.relationshipAddPendingRelationships()
      : null;
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const lastModified = this.conceptLastModified();
    const activityId = this.workbenchActivityId().trim();
    const relationshipType = this.relationshipAddType().trim();
    const relationships = pendingRelationships ??
      (concept
        ? this.selectedRelationshipTargetsForDialog()
            .map((target) =>
              target.id
                ? this.buildRelationshipPayload(concept, target.id, relationshipType, target)
                : null
            )
            .filter((relationship): relationship is ContentRelationship => Boolean(relationship))
        : []);

    if (!projectId || !concept?.id || !relationships.length || !lastModified || !activityId) {
      return null;
    }

    return { activityId, conceptId: concept.id, lastModified, overrideWarnings, projectId, relationships };
  }

  private buildRelationshipPayload(
    concept: ContentComponentDetail,
    targetId: number,
    relationshipType: string,
    selectedTarget: ContentComponentDetail | null
  ): ContentRelationship {
    const terminology = concept.terminology || this.conceptTerminology();
    const version = concept.version || this.conceptVersion();

    return {
      additionalRelationshipType: '',
      assertedDirection: false,
      fromId: concept.id,
      fromName: concept.name,
      fromTerminology: concept.terminology,
      fromTerminologyId: concept.terminologyId,
      fromVersion: concept.version,
      group: null,
      hierarchical: false,
      inferred: false,
      name: null,
      obsolete: false,
      published: false,
      relationshipType,
      stated: false,
      suppressible: false,
      terminology,
      terminologyId: '',
      toId: targetId,
      toName: selectedTarget?.name ?? '',
      toTerminology: selectedTarget?.terminology ?? terminology,
      toTerminologyId: selectedTarget?.terminologyId ?? '',
      toVersion: selectedTarget?.version ?? version,
      type: 'RELATIONSHIP',
      version,
      workflowStatus: 'NEEDS_REVIEW'
    };
  }

  private relationshipRemovalContext(
    rel: ContentRelationship,
    overrideWarnings: boolean
  ) {
    const concept = this.loadedConcept();
    const projectId = this.projectId();
    const activityId = this.workbenchActivityId().trim();
    const relationshipId = rel.id;
    const sourceConceptId = rel.fromId ?? concept?.id;

    if (!projectId || !sourceConceptId || !relationshipId || !activityId) {
      throw new Error('Relationship removal context is incomplete.');
    }

    const currentLastModified = this.conceptLastModified();
    if (sourceConceptId === concept?.id && currentLastModified) {
      return of({
        activityId,
        conceptId: sourceConceptId,
        lastModified: currentLastModified,
        overrideWarnings,
        projectId,
        relationshipId
      });
    }

    return this.contentApi.getComponentById('CONCEPT', sourceConceptId, projectId).pipe(
      map((sourceConcept) => {
        const lastModified = this.toEpochMillis(sourceConcept?.lastModified);
        if (!sourceConcept?.id || !lastModified) {
          throw new Error('Relationship source concept could not be loaded.');
        }
        return {
          activityId,
          conceptId: sourceConcept.id,
          lastModified,
          overrideWarnings,
          projectId,
          relationshipId
        };
      })
    );
  }

  private broadcastConceptApproved(conceptId: number): void {
    (window.opener as Window | null)?.postMessage(
      { type: 'concept-approved', conceptId },
      window.location.origin
    );
  }

  private broadcastConceptSplit(fromConceptId: number, newConceptId: number): void {
    (window.opener as Window | null)?.postMessage(
      { type: 'concept-split', fromConceptId, newConceptId },
      window.location.origin
    );
  }

  private broadcastConceptMerged(fromConceptId: number, toConceptId: number): void {
    (window.opener as Window | null)?.postMessage(
      { type: 'concept-merged', fromConceptId, toConceptId },
      window.location.origin
    );
  }

  private toRouteQueryParams(
    context: EditWorkbenchContext | null
  ): Record<string, string> {
    const queryParams: Record<string, string> = {};

    if (!context) {
      return queryParams;
    }

    Object.entries(context).forEach(([key, value]) => {
      if (value) {
        queryParams[key] = value;
      }
    });

    return queryParams;
  }
}
