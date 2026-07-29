import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { NavigationService } from '../../core/navigation/navigation.service';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'meme-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly navigation = inject(NavigationService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  protected readonly config = inject(RuntimeConfigService);
  protected readonly working = signal(false);

  protected readonly form = this.formBuilder.group({
    userName: ['', Validators.required],
    password: ['', Validators.required]
  });

  protected async login(): Promise<void> {
    this.form.markAllAsTouched();

    if (this.form.invalid || this.working()) {
      return;
    }

    const { userName, password } = this.form.getRawValue();
    this.working.set(true);

    try {
      await this.auth.authenticate(userName, password);
      await this.router.navigateByUrl(this.navigation.routeForStartingTab());
    } catch {
      this.notifications.error('Login failed.');
    } finally {
      this.working.set(false);
    }
  }
}
