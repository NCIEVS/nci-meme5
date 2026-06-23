import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import {
  ActivatedRoute,
  RouterLink,
  RouterLinkActive
} from '@angular/router';
import { finalize, map } from 'rxjs';

import { ProjectContextService } from '../../core/navigation/project-context.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { OperationalApiService } from '../operations/operational-api.service';
import {
  buildActionMutationReadiness,
  validationBlocksCommit,
  validationErrors,
  validationWarnings
} from './edit-mutation.helpers';
import { EditMutationApiService } from './edit-mutation-api.service';
import {
  EditMutationReadiness,
  EditValidationResult,
  EditUndoRedoRequest
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
  projectId?: string | null;
  terminology?: string | null;
  terminologyId?: string | null;
  type?: string | null;
  version?: string | null;
}

interface EditWorkbenchContextEntry {
  label: string;
  value: string;
}

@Component({
  selector: 'meme-edit-workbench',
  imports: [FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './edit-workbench.component.html',
  styleUrl: '../operations/operations.component.css'
})
export class EditWorkbenchComponent implements OnInit {
  private readonly mutationApi = inject(EditMutationApiService);
  private readonly notifications = inject(NotificationService);
  private readonly operationsApi = inject(OperationalApiService);
  private readonly route = inject(ActivatedRoute);
  protected readonly projectContext = inject(ProjectContextService);
  private readonly routeWorkbench = toSignal(
    this.route.data.pipe(
      map((data) => String(data['workbench'] ?? 'main'))
    ),
    {
      initialValue: String(this.route.snapshot.data['workbench'] ?? 'main')
    }
  );
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap
  });

  protected readonly actionActivityId = signal('');
  protected readonly actionForce = signal(false);
  protected readonly actionMolecularActionId = signal('');
  protected readonly actionResult = signal<EditValidationResult | null>(null);
  protected readonly loadingProjectContext = signal(false);
  protected readonly projectContextError = signal<string | null>(null);
  protected readonly projectEditingEnabled = signal<boolean | null>(null);
  protected readonly runningAction = signal<'redo' | 'undo' | null>(null);

  protected readonly projectId = computed(() => this.projectContext.projectId());
  protected readonly projectRole = computed(
    () => this.projectContext.projectRole() || 'n/a'
  );
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
      projectId: queryParams.get('projectId'),
      terminology: queryParams.get('terminology'),
      terminologyId: queryParams.get('terminologyId') ?? queryParams.get('id'),
      type: queryParams.get('type'),
      version: queryParams.get('version')
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

  ngOnInit(): void {
    this.loadProjectContext();
  }

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
            this.notifications.success(`${label} completed with warnings.`);
            return;
          }
          this.notifications.success(`${label} completed.`);
        },
        error: () => {
          this.notifications.error(`${label} could not be completed.`);
        }
      });
  }

  protected readonly workbenchLinks: EditWorkbenchLink[] = [
    { label: 'Main', route: '/edit', workbench: 'main' },
    {
      label: 'Semantic Types',
      route: '/edit/semantic-types',
      workbench: 'semantic-types'
    },
    { label: 'Code Concepts', route: '/edit/codeConcepts', workbench: 'code-concepts' },
    { label: 'Atoms', route: '/edit/atoms', workbench: 'atoms' },
    {
      label: 'Relationships',
      route: '/edit/relationships',
      workbench: 'relationships'
    },
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
