import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { NotificationService } from '../../core/notifications/notification.service';
import { TerminologyApiService } from './terminology-api.service';
import { formatCitation, formatContact } from './terminology-formatters';
import { RootTerminology, Terminology } from './terminology.models';

type SortField = 'preferredName' | 'terminology' | 'version' | 'organizingClassType';

@Component({
  selector: 'meme-terminology',
  imports: [FormsModule],
  templateUrl: './terminology.component.html',
  styleUrl: './terminology.component.css'
})
export class TerminologyComponent implements OnInit {
  private readonly api = inject(TerminologyApiService);
  private readonly notifications = inject(NotificationService);

  protected readonly citationValue = formatCitation;
  protected readonly contactValue = formatContact;
  protected readonly filter = signal('');
  protected readonly loadingDetails = signal(false);
  protected readonly page = signal(1);
  protected readonly pageSize = signal(10);
  protected readonly selectedRootTerminology = signal<RootTerminology | null>(null);
  protected readonly selectedTerminology = signal<Terminology | null>(null);
  protected readonly sortAscending = signal(true);
  protected readonly sortField = signal<SortField>('preferredName');
  protected readonly terminologies = signal<Terminology[]>([]);

  protected readonly filteredTerminologies = computed(() => {
    const filter = this.filter().trim().toLocaleLowerCase();

    return this.terminologies().filter((terminology) => {
      if (!filter) {
        return true;
      }

      return [
        terminology.preferredName,
        terminology.terminology,
        terminology.version,
        terminology.organizingClassType
      ].some((value) => value?.toLocaleLowerCase().includes(filter));
    });
  });

  protected readonly sortedTerminologies = computed(() => {
    const field = this.sortField();
    const direction = this.sortAscending() ? 1 : -1;

    return [...this.filteredTerminologies()].sort((left, right) =>
      direction * this.compareValues(left[field], right[field])
    );
  });

  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.sortedTerminologies().length / this.pageSize()))
  );

  protected readonly pagedTerminologies = computed(() => {
    const start = (this.page() - 1) * this.pageSize();
    return this.sortedTerminologies().slice(start, start + this.pageSize());
  });

  async ngOnInit(): Promise<void> {
    this.api.getCurrentTerminologies().subscribe({
      next: (terminologies) => {
        this.terminologies.set(terminologies);
        this.selectPreferredTerminology(terminologies);
      },
      error: () => {
        this.notifications.error('Terminologies could not be loaded.');
      }
    });
  }

  protected selectTerminology(terminology: Terminology): void {
    if (this.selectedTerminology()?.id === terminology.id) {
      this.selectedTerminology.set(null);
      this.selectedRootTerminology.set(null);
      return;
    }

    this.selectedTerminology.set(terminology);
    this.selectedRootTerminology.set(terminology.rootTerminology ?? null);
    this.loadingDetails.set(true);

    this.api.getRootTerminology(terminology.terminology).subscribe({
      next: (rootTerminology) => {
        this.selectedRootTerminology.set(rootTerminology);
        this.loadingDetails.set(false);
      },
      error: () => {
        this.loadingDetails.set(false);
        this.notifications.error(
          `Root terminology details could not be loaded for ${terminology.terminology}.`
        );
      }
    });
  }

  protected setFilter(value: string): void {
    this.filter.set(value);
    this.page.set(1);
  }

  protected setPageSize(value: string): void {
    this.pageSize.set(Number(value));
    this.page.set(1);
  }

  protected setSortField(field: SortField): void {
    if (this.sortField() === field) {
      this.sortAscending.update((ascending) => !ascending);
    } else {
      this.sortField.set(field);
      this.sortAscending.set(true);
    }
  }

  protected sortIndicator(field: SortField): string {
    if (this.sortField() !== field) {
      return '';
    }

    return this.sortAscending() ? 'ascending' : 'descending';
  }

  protected previousPage(): void {
    this.page.update((page) => Math.max(1, page - 1));
  }

  protected nextPage(): void {
    this.page.update((page) => Math.min(this.totalPages(), page + 1));
  }

  private selectPreferredTerminology(terminologies: Terminology[]): void {
    const firstTerminology = terminologies[0];

    if (firstTerminology) {
      this.selectTerminology(firstTerminology);
    }
  }

  private compareValues(
    left: string | number | boolean | null | undefined,
    right: string | number | boolean | null | undefined
  ): number {
    return String(left ?? '').localeCompare(String(right ?? ''), undefined, {
      numeric: true,
      sensitivity: 'base'
    });
  }
}
