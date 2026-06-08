import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';
import { RuntimeConfigService } from '../../core/config/runtime-config.service';

@Component({
  selector: 'meme-footer',
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.css'
})
export class FooterComponent {
  protected readonly auth = inject(AuthService);
  protected readonly config = inject(RuntimeConfigService);

  protected truncatedVersion(): string {
    const version = this.config.get('projectVersion') ?? '';
    return version.includes('-') ? version.substring(0, version.indexOf('-')) : version;
  }
}
