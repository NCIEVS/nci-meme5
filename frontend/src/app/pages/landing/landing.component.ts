import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { NavigationService } from '../../core/navigation/navigation.service';

@Component({
  selector: 'meme-landing',
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css'
})
export class LandingComponent {
  protected readonly config = inject(RuntimeConfigService);
  private readonly navigation = inject(NavigationService);
  private readonly router = inject(Router);

  protected async launch(): Promise<void> {
    await this.router.navigateByUrl(this.navigation.routeAfterLaunch());
  }
}
