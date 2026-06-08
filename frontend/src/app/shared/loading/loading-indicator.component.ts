import { Component, inject } from '@angular/core';

import { LoadingService } from '../../core/http/loading.service';

@Component({
  selector: 'meme-loading-indicator',
  templateUrl: './loading-indicator.component.html',
  styleUrl: './loading-indicator.component.css'
})
export class LoadingIndicatorComponent {
  protected readonly loading = inject(LoadingService);
}
