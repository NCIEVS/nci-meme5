import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { TAB_DEFINITIONS } from '../../core/config/runtime-config.models';
import { NavigationService } from '../../core/navigation/navigation.service';
import { PermissionService } from '../../core/navigation/permission.service';

@Component({
  selector: 'meme-feature-placeholder',
  templateUrl: './feature-placeholder.component.html',
  styleUrl: './feature-placeholder.component.css'
})
export class FeaturePlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly navigation = inject(NavigationService);
  private readonly permissions = inject(PermissionService);

  protected readonly tab = computed(() => {
    const tabKey = String(this.route.snapshot.data['tabKey'] ?? '');
    return TAB_DEFINITIONS[tabKey];
  });

  protected readonly canAccess = computed(() => {
    const tab = this.tab();
    return tab ? this.permissions.canAccessTab(tab) : false;
  });

  protected readonly legacyUrl = computed(() => {
    const tab = this.tab();
    return tab ? this.navigation.legacyUrl(tab) : '/umls-server-rest/';
  });
}
