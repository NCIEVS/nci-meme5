import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { memeAppRouteUrl } from '../../core/meme-deployment-paths';
import { ContentEditApiService } from '../../features/content-edit/content-edit-api.service';
import { ContentComponent as ContentComponentDetail } from '../../features/content-edit/content-edit.models';
import { ConceptReportPanelComponent, LinkedConceptInfo, ReportPanelTab } from '../../shared/concept-report-panel/concept-report-panel.component';

@Component({
  selector: 'meme-concept-report-popup',
  standalone: true,
  imports: [ConceptReportPanelComponent],
  templateUrl: './concept-report.component.html',
  styleUrl: './concept-report.component.css'
})
export class ConceptReportComponent implements OnInit {
  private readonly api = inject(ContentEditApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly concept = signal<ContentComponentDetail | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly projectId = signal<number | null>(null);
  protected readonly initialTab = signal<ReportPanelTab>('Report');

  ngOnInit(): void {
    const params = this.route.snapshot.paramMap;
    const queryParams = this.route.snapshot.queryParamMap;
    const terminology = params.get('terminology');
    const version = params.get('version');
    const terminologyId = params.get('terminologyId');
    const projectIdParam = queryParams.get('projectId');
    const tabParam = queryParams.get('tab') as ReportPanelTab | null;
    const idParam = queryParams.get('id');

    if (projectIdParam) {
      const pid = Number(projectIdParam);
      if (Number.isFinite(pid)) this.projectId.set(pid);
    }

    if (tabParam === 'Interactive' || tabParam === 'Actions' || tabParam === 'Report') {
      this.initialTab.set(tabParam);
    }

    this.loading.set(true);
    let request$;
    if (terminology && version && terminologyId) {
      request$ = this.api.getComponentByTerminologyId('concept', terminology, version, terminologyId, this.projectId());
    } else if (idParam) {
      const numericId = Number(idParam);
      if (!Number.isFinite(numericId)) { this.error.set('Invalid concept id.'); this.loading.set(false); return; }
      request$ = this.api.getComponentById('concept', numericId, this.projectId());
    } else {
      this.error.set('Missing concept identifiers.');
      this.loading.set(false);
      return;
    }

    request$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (c) => {
        if (c) {
          this.concept.set(c);
          document.title = `${c.terminologyId} – ${c.name ?? 'Concept'}`;
        } else {
          this.error.set('Concept not found.');
        }
      },
      error: () => this.error.set('Could not load concept.')
    });
  }

  protected openLinkedConcept(info: LinkedConceptInfo): void {
    const params = new URLSearchParams();
    if (this.projectId()) params.set('projectId', String(this.projectId()));
    if (info.tab) params.set('tab', info.tab);

    let url: string;
    let windowName: string;
    if (info.terminologyId && info.terminology && info.version) {
      url = memeAppRouteUrl(
        `/concept-report/${encodeURIComponent(info.terminology)}/${encodeURIComponent(info.version)}/${encodeURIComponent(info.terminologyId)}`,
        params
      );
      windowName = `concept_${info.terminologyId}`;
    } else if (info.id) {
      params.set('id', String(info.id));
      url = memeAppRouteUrl('/concept-report', params);
      windowName = `concept_id_${info.id}`;
    } else {
      return;
    }
    window.open(url, windowName, 'width=700,height=700,scrollbars=yes');
  }
}
