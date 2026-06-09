import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { NotificationService } from '../../core/notifications/notification.service';
import { buildPfs } from './admin-api.helpers';
import { AdminApiService } from './admin-api.service';
import { AdminProject, AdminUser } from './admin.models';

type UserSortField = 'userName' | 'name' | 'email' | 'applicationRole';
type ProjectSortField = 'lastModified' | 'id' | 'name' | 'terminology';

interface RoleEntry {
  key: string;
  value: string;
}

@Component({
  selector: 'meme-admin',
  imports: [FormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
  private readonly api = inject(AdminApiService);
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
  protected readonly loadingRoles = signal(false);
  protected readonly loadingUsers = signal(false);
  protected readonly selectedProject = signal<AdminProject | null>(null);
  protected readonly selectedUser = signal<AdminUser | null>(null);
  protected readonly users = signal<AdminUser[]>([]);
  protected readonly userFilter = signal('');
  protected readonly userPage = signal(1);
  protected readonly userPageSize = signal(10);
  protected readonly userSortAscending = signal(true);
  protected readonly userSortField = signal<UserSortField>('userName');
  protected readonly userTotalCount = signal(0);

  protected readonly projectTotalPages = computed(() =>
    this.pageCount(this.projectTotalCount(), this.projectPageSize())
  );
  protected readonly userTotalPages = computed(() =>
    this.pageCount(this.userTotalCount(), this.userPageSize())
  );

  ngOnInit(): void {
    this.loadRoles();
    this.loadProjects();
    this.loadUsers();
  }

  protected loadProjects(): void {
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
          this.selectProject(state.items[0] ?? null);
        },
        error: () => {
          this.notifications.error('Projects could not be loaded.');
        }
      });
  }

  protected loadUsers(): void {
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
          this.selectUser(state.items[0] ?? null);
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

  protected setProjectFilter(value: string): void {
    this.projectFilter.set(value);
    this.projectPage.set(1);
  }

  protected setUserFilter(value: string): void {
    this.userFilter.set(value);
    this.userPage.set(1);
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

  protected listValue(values: Array<string | null> | null | undefined): string {
    const usableValues = values?.filter((value): value is string => Boolean(value));
    return usableValues?.length ? usableValues.join(', ') : 'n/a';
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

  private pageCount(totalCount: number, pageSize: number): number {
    return Math.max(1, Math.ceil(totalCount / pageSize));
  }
}
