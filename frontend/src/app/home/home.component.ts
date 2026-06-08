import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';

import { BackendProbeService } from '../core/backend-probe.service';

@Component({
  selector: 'meme-home',
  imports: [AsyncPipe],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  private readonly backendProbe = inject(BackendProbeService);

  protected readonly probe$ = this.backendProbe.probe();
}
