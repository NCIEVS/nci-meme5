import { Component } from '@angular/core';

@Component({
  selector: 'meme-startup',
  template: '<p class="sr-only">Starting application</p>',
  styles: [
    '.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); }'
  ]
})
export class StartupComponent {}
