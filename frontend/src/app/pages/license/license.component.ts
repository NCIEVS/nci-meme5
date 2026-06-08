import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { NavigationService } from '../../core/navigation/navigation.service';

@Component({
  selector: 'meme-license',
  templateUrl: './license.component.html',
  styleUrl: './license.component.css'
})
export class LicenseComponent implements OnInit {
  protected readonly config = inject(RuntimeConfigService);
  protected readonly licenseChecked = signal(false);
  private readonly auth = inject(AuthService);
  private readonly navigation = inject(NavigationService);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    if (this.auth.acceptsLicense()) {
      await this.router.navigateByUrl(this.navigation.routeForStartingTab());
      return;
    }

    this.licenseChecked.set(true);
  }

  protected async acceptLicense(): Promise<void> {
    this.auth.acceptLicense();
    await this.router.navigateByUrl(this.navigation.routeForStartingTab());
  }
}
