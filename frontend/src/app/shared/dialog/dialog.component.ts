import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

let nextDialogId = 0;

@Component({
  selector: 'meme-dialog',
  templateUrl: './dialog.component.html',
  styleUrl: './dialog.component.css'
})
export class DialogComponent {
  @Input() closeDisabled = false;
  @Input() size: 'md' | 'lg' | 'xl' = 'md';
  @Input({ required: true }) title = '';
  @Output() readonly closed = new EventEmitter<void>();

  protected readonly titleId = `meme-dialog-title-${nextDialogId++}`;

  @HostListener('document:keydown.escape', ['$event'])
  protected closeWithEscape(event: KeyboardEvent): void {
    event.preventDefault();
    this.close();
  }

  protected close(): void {
    if (this.closeDisabled) {
      return;
    }

    this.closed.emit();
  }
}
