import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
  signal,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { finalize } from 'rxjs';

import { ContentEditApiService } from '../../features/content-edit/content-edit-api.service';
import { EditMutationApiService } from '../../features/content-edit/edit-mutation-api.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { IconComponent } from '../icon/icon.component';
import {
  ContentComponent as ContentComponentDetail,
  ContentDefinition,
  ContentPfsParameter,
  ContentRelationship,
  MolecularAction
} from '../../features/content-edit/content-edit.models';
import { EditUndoRedoRequest } from '../../features/content-edit/edit-mutation.models';

export type ReportPanelTab = 'Report' | 'Interactive' | 'Actions';

export interface LinkedConceptInfo {
  id?: number | null;
  terminologyId?: string | null;
  terminology?: string | null;
  version?: string | null;
  tab?: ReportPanelTab;
}

@Component({
  selector: 'meme-concept-report-panel',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './concept-report-panel.component.html',
  styleUrl: './concept-report-panel.component.css'
})
export class ConceptReportPanelComponent implements OnChanges {
  @Input() concept: ContentComponentDetail | null = null;
  @Input() projectId: number | null = null;
  @Input() projectRole: string | null = null;
  @Input() editingEnabled: boolean | null = null;
  @Input() activityId: string | null = null;
  @Input() initialTab: ReportPanelTab = 'Report';

  @Output() linkClicked = new EventEmitter<LinkedConceptInfo>();
  @Output() tabChanged = new EventEmitter<ReportPanelTab>();

  private readonly api = inject(ContentEditApiService);
  private readonly mutationApi = inject(EditMutationApiService);
  private readonly notifications = inject(NotificationService);
  private readonly sanitizer = inject(DomSanitizer);

  protected readonly activeTab = signal<ReportPanelTab>('Report');
  protected readonly showHidden = signal(false);
  protected readonly secOpen = signal<Record<string, boolean>>({
    sty: true, def: true, atoms: true, attr: false, rels: true, deep: true
  });
  protected readonly atomOpen = signal<Record<number, boolean>>({});

  // Merged definitions (concept-level + atom-level, matching legacy preprocessing)
  protected readonly effectiveDefinitions = signal<ContentDefinition[]>([]);

  // Relationships (fetched separately — not included in concept object)
  protected readonly rels = signal<ContentRelationship[]>([]);
  protected readonly loadingRels = signal(false);

  // Deep relationships
  protected readonly deepRels = signal<ContentRelationship[]>([]);
  protected readonly loadingDeepRels = signal(false);

  // Report tab
  protected readonly reportHtml = signal<SafeHtml | null>(null);
  protected readonly loadingReport = signal(false);
  protected readonly reportError = signal<string | null>(null);

  // Actions tab
  protected readonly actions = signal<MolecularAction[]>([]);
  protected readonly actionsTotal = signal(0);
  protected readonly actionsPage = signal(1);
  protected readonly actionsPageSize = 15;
  protected readonly loadingActions = signal(false);
  protected readonly actionsTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.actionsTotal() / this.actionsPageSize))
  );
  protected readonly actionLogs = signal<Record<number, string | null>>({});
  protected readonly actionLogLoading = signal<Record<number, boolean>>({});
  protected readonly tooltipActionId = signal<number | null>(null);
  protected readonly tooltipTop = signal(0);
  protected readonly tooltipRight = signal(0);

  ngOnChanges(changes: SimpleChanges): void {
    // Apply initialTab first so the correct tab is active before we decide what to load.
    if (changes['initialTab']) {
      this.activeTab.set(this.initialTab);
    }
    const conceptChanged = Boolean(changes['concept'] || changes['projectId']);

    if (!conceptChanged) {
      if (changes['initialTab'] && this.concept) {
        this.loadActiveTabIfNeeded();
      }
      return;
    }

    this.reportHtml.set(null);
    this.reportError.set(null);
    this.actions.set([]);
    this.actionsTotal.set(0);
    this.actionsPage.set(1);
    this.actionLogs.set({});
    this.actionLogLoading.set({});
    this.tooltipActionId.set(null);
    this.showHidden.set(false);
    this.secOpen.set({ sty: true, def: true, atoms: true, attr: false, rels: true, deep: true });
    this.atomOpen.set({});
    this.rels.set([]);
    this.deepRels.set([]);
    this.effectiveDefinitions.set(this.buildEffectiveDefinitions());
    if (this.concept) {
      this.loadActiveTabIfNeeded();
    }
  }

  protected setTab(tab: ReportPanelTab): void {
    this.activeTab.set(tab);
    this.tabChanged.emit(tab);
    this.loadActiveTabIfNeeded();
  }

  private loadActiveTabIfNeeded(): void {
    const tab = this.activeTab();

    if (tab === 'Report' && !this.reportHtml() && !this.loadingReport()) {
      this.loadReport();
    } else if (tab === 'Actions' && !this.actions().length && !this.loadingActions()) {
      this.loadActions();
    } else if (tab === 'Interactive') {
      if (!this.rels().length && !this.loadingRels()) this.loadRelationships();
      if (!this.deepRels().length && !this.loadingDeepRels()) this.loadDeepRelationships();
    }
  }

  private processReportHtml(html: string): string {
    const doc = new DOMParser().parseFromString(html, 'text/html');
    const conceptLinkPattern = /\/content\/(?:report|content)\/CONCEPT\/[^/]+\/(\d+)/i;
    doc.querySelectorAll<HTMLAnchorElement>('a[href]').forEach((a) => {
      const match = (a.getAttribute('href') ?? '').match(conceptLinkPattern);
      if (!match) return;
      a.removeAttribute('onclick');
      a.removeAttribute('target');
      a.setAttribute('href', 'javascript:void(0)');
      a.setAttribute('data-concept-id', match[1]);
      a.classList.add('crp-report-link');
    });
    return doc.body.innerHTML;
  }

  private loadReport(): void {
    const concept = this.concept;
    if (!concept?.id) return;
    this.loadingReport.set(true);
    this.reportError.set(null);
    this.api
      .getComponentReport('concept', concept.id, this.projectId)
      .pipe(finalize(() => this.loadingReport.set(false)))
      .subscribe({
        next: (html) =>
          this.reportHtml.set(this.sanitizer.bypassSecurityTrustHtml(this.processReportHtml(html))),
        error: () => this.reportError.set('Could not load report.')
      });
  }

  protected loadActions(page?: number): void {
    const concept = this.concept;
    if (!concept?.id || !concept.terminology || !concept.version) return;
    if (page !== undefined) this.actionsPage.set(page);
    this.loadingActions.set(true);
    const pfs: ContentPfsParameter = {
      startIndex: (this.actionsPage() - 1) * this.actionsPageSize,
      maxResults: this.actionsPageSize,
      ascending: false,
      sortField: 'timestamp'
    };
    this.api
      .findMolecularActions(concept.id, concept.terminology, concept.version, '', pfs)
      .pipe(finalize(() => this.loadingActions.set(false)))
      .subscribe({
        next: (resp) => {
          this.actions.set(resp.actions ?? []);
          this.actionsTotal.set(resp.totalCount ?? 0);
        },
        error: () => this.notifications.error('Could not load actions.')
      });
  }

  protected canUndoRedo(): boolean {
    return !!this.editingEnabled;
  }

  protected isUndoable(idx: number): boolean {
    const list = this.actions();
    if (list[idx]?.undoneFlag) return false;
    for (let i = 0; i < idx; i++) {
      if (!list[i].undoneFlag) return false;
    }
    return true;
  }

  protected isRedoable(idx: number): boolean {
    const list = this.actions();
    if (!list[idx]?.undoneFlag) return false;
    for (let i = idx + 1; i < list.length; i++) {
      if (list[i].undoneFlag) return false;
    }
    return true;
  }

  protected undoAction(action: MolecularAction): void {
    const projectId = this.projectId;
    if (!action.id || !projectId) return;
    const request: EditUndoRedoRequest = {
      projectId,
      molecularActionId: action.id,
      activityId: String(action.activityId ?? ''),
      force: false
    };
    this.mutationApi.undoAction(request).subscribe({
      next: () => this.loadActions(),
      error: () => this.notifications.error('Could not undo action.')
    });
  }

  protected redoAction(action: MolecularAction): void {
    const projectId = this.projectId;
    if (!action.id || !projectId) return;
    const request: EditUndoRedoRequest = {
      projectId,
      molecularActionId: action.id,
      activityId: String(action.activityId ?? ''),
      force: false
    };
    this.mutationApi.redoAction(request).subscribe({
      next: () => this.loadActions(),
      error: () => this.notifications.error('Could not redo action.')
    });
  }

  protected preloadActionLog(action: MolecularAction): void {
    const id = action.id;
    if (id == null || this.actionLogs()[id] !== undefined || this.actionLogLoading()[id]) return;
    const projectId = this.projectId;
    if (!projectId) return;
    this.actionLogLoading.set({ ...this.actionLogLoading(), [id]: true });
    this.api.getActionLog(projectId, id).subscribe({
      next: (text) => {
        this.actionLogs.set({ ...this.actionLogs(), [id]: text?.trim() || 'No details.' });
        this.actionLogLoading.set({ ...this.actionLogLoading(), [id]: false });
      },
      error: () => {
        this.actionLogs.set({ ...this.actionLogs(), [id]: 'Could not load log.' });
        this.actionLogLoading.set({ ...this.actionLogLoading(), [id]: false });
      }
    });
  }

  protected toggleActionLogTooltip(event: MouseEvent, action: MolecularAction): void {
    event.stopPropagation();
    const id = action.id!;
    if (this.tooltipActionId() === id) {
      this.tooltipActionId.set(null);
      return;
    }
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    this.tooltipTop.set(rect.top);
    this.tooltipRight.set(window.innerWidth - rect.left + 6);
    this.tooltipActionId.set(id);
    this.preloadActionLog(action);
    const close = () => { this.tooltipActionId.set(null); document.removeEventListener('click', close); };
    document.addEventListener('click', close);
  }

  private buildEffectiveDefinitions(): ContentDefinition[] {
    const c = this.concept;
    if (!c) return [];
    const defs: ContentDefinition[] = [...(c.definitions ?? [])];
    for (const atom of c.atoms ?? []) {
      for (const def of atom.definitions ?? []) {
        defs.push({
          ...def,
          atomElement: true,
          atomElementStr: `${atom.name} [${atom.terminology}/${atom.termType}]`
        });
      }
    }
    return defs;
  }

  protected toggleSection(key: string): void {
    const cur = this.secOpen();
    const opening = !cur[key];
    this.secOpen.set({ ...cur, [key]: opening });
    if (key === 'rels' && opening && !this.rels().length && !this.loadingRels()) {
      this.loadRelationships();
    }
    if (key === 'deep' && opening && !this.deepRels().length && !this.loadingDeepRels()) {
      this.loadDeepRelationships();
    }
  }

  private loadRelationships(): void {
    const c = this.concept;
    if (!c?.terminology || !c.version || !c.terminologyId) return;
    this.loadingRels.set(true);
    this.api
      .findRelationships('concept', c.terminology, c.version, c.terminologyId, {
        startIndex: 0, maxResults: 200, ascending: true,
        queryRestriction: 'stated:true'
      })
      .pipe(finalize(() => this.loadingRels.set(false)))
      .subscribe({ next: (res) => this.rels.set(res.items ?? []) });
  }

  private loadDeepRelationships(): void {
    const c = this.concept;
    if (!c?.terminology || !c.version || !c.terminologyId) return;
    this.loadingDeepRels.set(true);
    this.api
      .findDeepRelationships(c.terminology, c.version, c.terminologyId, {
        startIndex: 0, maxResults: 200, ascending: true
      })
      .pipe(finalize(() => this.loadingDeepRels.set(false)))
      .subscribe({ next: (res) => this.deepRels.set(res.items ?? []) });
  }

  protected toggleAtom(idx: number): void {
    const cur = this.atomOpen();
    this.atomOpen.set({ ...cur, [idx]: !cur[idx] });
  }

  protected openLinkedConcept(rel: ContentRelationship): void {
    this.linkClicked.emit({
      id: rel.toId,
      terminologyId: rel.toTerminologyId,
      terminology: rel.toTerminology ?? this.concept?.terminology,
      version: rel.toVersion ?? this.concept?.version,
      tab: this.activeTab()
    });
  }

  protected handleReportClick(event: MouseEvent): void {
    const anchor = (event.target as HTMLElement).closest('a[data-concept-id]') as HTMLAnchorElement | null;
    if (!anchor) return;
    event.preventDefault();
    const numericId = Number(anchor.getAttribute('data-concept-id'));
    if (!Number.isFinite(numericId)) return;
    const params = new URLSearchParams();
    if (this.projectId) params.set('projectId', String(this.projectId));
    params.set('tab', 'Report');
    params.set('id', String(numericId));
    window.open(`/concept-report?${params}`, `concept_id_${numericId}`, 'width=700,height=700,scrollbars=yes');
  }

  protected formatTimestamp(ts: string | null | undefined): string {
    if (!ts) return '';
    try {
      return new Date(ts).toLocaleString();
    } catch {
      return ts;
    }
  }
}
