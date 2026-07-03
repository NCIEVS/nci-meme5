import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { ProjectContextService } from '../../core/navigation/project-context.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { DialogComponent } from '../../shared/dialog/dialog.component';
import { OperationalApiService } from './operational-api.service';
import { SourceIdRange } from './operational.models';

interface RangeForm {
  beginSourceId: number | null;
  errors: string[];
  numberOfIds: number | null;
}

@Component({
  selector: 'meme-inversion',
  imports: [DialogComponent, FormsModule],
  templateUrl: './inversion.component.html',
  styleUrl: './operations.component.css'
})
export class InversionComponent implements OnInit {
  private readonly api = inject(OperationalApiService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly projectContext = inject(ProjectContextService);

  protected readonly projectId = computed(() => this.projectContext.projectId());
  protected readonly projectRole = computed(() => this.projectContext.projectRole() ?? 'n/a');
  protected readonly canRemoveRanges = computed(() => this.projectContext.hasPrivilegesOf('AUTHOR'));

  protected readonly vsab = signal('');
  protected readonly sourceRangesGroupOpen = signal(true);
  protected readonly ranges = signal<SourceIdRange[]>([]);
  protected readonly loading = signal(false);
  protected readonly searchError = signal<string | null>(null);

  protected readonly requestDialogOpen = signal(false);
  protected readonly requestForm = signal<RangeForm>({ beginSourceId: null, errors: [], numberOfIds: null });
  protected readonly submittingRequest = signal(false);

  protected readonly adjustDialogOpen = signal(false);
  protected readonly adjustForm = signal<RangeForm>({ beginSourceId: null, errors: [], numberOfIds: null });
  protected readonly submittingAdjust = signal(false);

  protected readonly removingId = signal<number | null>(null);

  protected readonly isSnomedVsab = computed(() => this.vsab().includes('SNOMED'));

  ngOnInit(): void {
    this.restoreInversionAccordionState();
  }

  protected onInversionAccordionToggle(event: Event): void {
    this.sourceRangesGroupOpen.set((event.target as HTMLDetailsElement).open);
    this.saveInversionAccordionState();
  }

  protected numberOfIds(range: SourceIdRange): number | null {
    return range.beginSourceId != null && range.endSourceId != null
      ? range.endSourceId - range.beginSourceId + 1
      : null;
  }

  protected toDateString(value: string | number | null | undefined): string {
    if (!value) return '';
    const ms = Number(value);
    return isNaN(ms) ? String(value) : new Date(ms).toLocaleDateString();
  }

  private restoreInversionAccordionState(): void {
    const raw = this.auth.currentUser().userPreferences?.properties?.['inversionGroups'];
    const groups = this.parseStoredGroups(raw);
    const group = groups.find((item) => item.title === 'Source Atom Id Ranges') ?? groups[0];

    if (typeof group?.open === 'boolean') {
      this.sourceRangesGroupOpen.set(group.open);
    }
  }

  private saveInversionAccordionState(): void {
    if (this.auth.isGuest()) {
      return;
    }

    const user = this.auth.currentUser();
    const preferences = user.userPreferences ?? { properties: {} };
    const nextPreferences = {
      ...preferences,
      properties: {
        ...(preferences.properties ?? {}),
        inversionGroups: JSON.stringify([
          { open: this.sourceRangesGroupOpen(), title: 'Source Atom Id Ranges' }
        ])
      }
    };

    this.api.updateUserPreferences(nextPreferences).subscribe({
      next: (saved) => this.auth.updateCurrentUserPreferences(saved ?? nextPreferences),
      error: () => {}
    });
  }

  private parseStoredGroups(raw: unknown): Array<{ open?: unknown; title?: unknown }> {
    if (Array.isArray(raw)) {
      return raw as Array<{ open?: unknown; title?: unknown }>;
    }

    if (typeof raw !== 'string' || !raw) {
      return [];
    }

    try {
      const parsed = JSON.parse(raw) as unknown;

      return Array.isArray(parsed)
        ? (parsed as Array<{ open?: unknown; title?: unknown }>)
        : [];
    } catch {
      return [];
    }
  }

  protected search(): void {
    const projectId = this.projectId();
    const vsab = this.vsab().trim();
    if (!projectId || !vsab) return;

    this.loading.set(true);
    this.searchError.set(null);
    this.ranges.set([]);

    this.api
      .getSourceIdRanges(projectId, vsab)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (ranges) => this.ranges.set(ranges),
        error: () => {
          this.searchError.set('Source ID ranges could not be loaded.');
          this.notifications.error('Source ID ranges could not be loaded.');
        }
      });
  }

  protected setRequestNumberOfIds(value: number | null): void {
    this.requestForm.update((f) => ({ ...f, numberOfIds: value, errors: [] }));
  }

  protected setRequestBeginSourceId(value: number | null): void {
    this.requestForm.update((f) => ({ ...f, beginSourceId: value }));
  }

  protected setAdjustNumberOfIds(value: number | null): void {
    this.adjustForm.update((f) => ({ ...f, numberOfIds: value, errors: [] }));
  }

  protected setAdjustBeginSourceId(value: number | null): void {
    this.adjustForm.update((f) => ({ ...f, beginSourceId: value }));
  }

  protected openRequestDialog(): void {
    this.requestForm.set({ beginSourceId: null, errors: [], numberOfIds: null });
    this.requestDialogOpen.set(true);
  }

  protected closeRequestDialog(): void {
    this.requestDialogOpen.set(false);
  }

  protected submitRequest(): void {
    const projectId = this.projectId();
    const vsab = this.vsab().trim();
    const form = this.requestForm();

    if (!form.numberOfIds || form.numberOfIds < 1) {
      this.requestForm.set({ ...form, errors: ['Number of IDs must be a positive integer.'] });
      return;
    }

    this.submittingRequest.set(true);
    this.api
      .requestSourceIdRange(projectId!, vsab, form.numberOfIds, form.beginSourceId)
      .pipe(finalize(() => this.submittingRequest.set(false)))
      .subscribe({
        next: (range) => {
          this.closeRequestDialog();
          this.ranges.set([range]);
          this.notifications.success('Source ID range requested.');
        },
        error: (err) => {
          const msg = err?.error?.userMessage || err?.error?.message || 'Range request failed.';
          this.requestForm.set({ ...form, errors: [msg] });
        }
      });
  }

  protected openAdjustDialog(): void {
    this.adjustForm.set({ beginSourceId: null, errors: [], numberOfIds: null });
    this.adjustDialogOpen.set(true);
  }

  protected closeAdjustDialog(): void {
    this.adjustDialogOpen.set(false);
  }

  protected submitAdjust(): void {
    const projectId = this.projectId();
    const vsab = this.vsab().trim();
    const form = this.adjustForm();

    if (!form.numberOfIds || form.numberOfIds < 1) {
      this.adjustForm.set({ ...form, errors: ['Number of IDs must be a positive integer.'] });
      return;
    }

    this.submittingAdjust.set(true);
    this.api
      .updateSourceIdRange(projectId!, vsab, form.numberOfIds, form.beginSourceId)
      .pipe(finalize(() => this.submittingAdjust.set(false)))
      .subscribe({
        next: (range) => {
          this.closeAdjustDialog();
          this.ranges.set([range]);
          this.notifications.success('Source ID range updated.');
        },
        error: (err) => {
          const msg = err?.error?.userMessage || err?.error?.message || 'Range adjustment failed.';
          this.adjustForm.set({ ...form, errors: [msg] });
        }
      });
  }

  protected removeRange(range: SourceIdRange): void {
    if (!range.id) return;
    const label = `${range.terminology} (${range.beginSourceId}–${range.endSourceId})`;
    if (!confirm(`Remove source ID range for ${label}?`)) return;

    this.removingId.set(range.id);
    this.api
      .removeSourceIdRange(range.id)
      .pipe(finalize(() => this.removingId.set(null)))
      .subscribe({
        next: () => {
          this.ranges.update((rs) => rs.filter((r) => r.id !== range.id));
          this.notifications.success('Source ID range removed.');
        },
        error: () => {
          this.notifications.error('Source ID range could not be removed.');
        }
      });
  }
}
