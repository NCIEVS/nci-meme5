import {
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  HostListener,
  Input,
  Output,
  ViewChild
} from '@angular/core';

let nextDialogId = 0;

@Component({
  selector: 'meme-dialog',
  templateUrl: './dialog.component.html',
  styleUrl: './dialog.component.css'
})
export class DialogComponent {
  @Input() closeDisabled = false;
  @Input() movable = false;
  @Input() resizable = false;
  @Input() size: 'md' | 'lg' | 'xl' = 'md';
  @Input() showBackdrop = true;
  @Input({ required: true }) title = '';

  @HostBinding('class')
  get hostClasses(): string {
    return [
      `dialog-size-${this.size}`,
      this.movable ? 'dialog-movable' : '',
      this.resizable ? 'dialog-resizable' : '',
      this.dragState ? 'dialog-dragging' : '',
      this.showBackdrop ? '' : 'dialog-no-backdrop'
    ].filter(Boolean).join(' ');
  }
  @Output() readonly closed = new EventEmitter<void>();

  @ViewChild('panel') private panel?: ElementRef<HTMLElement>;

  protected readonly titleId = `meme-dialog-title-${nextDialogId++}`;
  protected offsetX = 0;
  protected offsetY = 0;

  private dragState: {
    pointerX: number;
    pointerY: number;
    offsetX: number;
    offsetY: number;
  } | null = null;

  protected get panelTransform(): string | null {
    return this.offsetX || this.offsetY
      ? `translate3d(${this.offsetX}px, ${this.offsetY}px, 0)`
      : null;
  }

  @HostListener('document:keydown.escape', ['$event'])
  protected closeWithEscape(event: KeyboardEvent): void {
    event.preventDefault();
    this.close();
  }

  @HostListener('document:pointermove', ['$event'])
  protected dragDialog(event: PointerEvent): void {
    if (!this.dragState) {
      return;
    }

    const nextX =
      this.dragState.offsetX + event.clientX - this.dragState.pointerX;
    const nextY =
      this.dragState.offsetY + event.clientY - this.dragState.pointerY;
    const clamped = this.clampOffset(nextX, nextY);
    this.offsetX = clamped.x;
    this.offsetY = clamped.y;
  }

  @HostListener('document:pointerup')
  @HostListener('document:pointercancel')
  protected stopDragging(): void {
    this.dragState = null;
  }

  protected startDragging(event: PointerEvent): void {
    if (!this.movable || event.button !== 0 || this.isInteractiveElement(event.target)) {
      return;
    }

    event.preventDefault();
    this.dragState = {
      pointerX: event.clientX,
      pointerY: event.clientY,
      offsetX: this.offsetX,
      offsetY: this.offsetY
    };
  }

  protected close(): void {
    if (this.closeDisabled) {
      return;
    }

    this.closed.emit();
  }

  private clampOffset(x: number, y: number): { x: number; y: number } {
    const rect = this.panel?.nativeElement.getBoundingClientRect();
    const width = rect?.width ?? 720;
    const height = rect?.height ?? 400;
    const margin = 8;
    const baseLeft = (window.innerWidth - width) / 2;
    const baseTop = (window.innerHeight - height) / 2;

    return {
      x: this.clampValue(
        x,
        margin - baseLeft,
        window.innerWidth - width - margin - baseLeft
      ),
      y: this.clampValue(
        y,
        margin - baseTop,
        window.innerHeight - height - margin - baseTop
      )
    };
  }

  private clampValue(value: number, min: number, max: number): number {
    if (min > max) {
      return 0;
    }

    return Math.min(Math.max(value, min), max);
  }

  private isInteractiveElement(target: EventTarget | null): boolean {
    return target instanceof HTMLElement && Boolean(
      target.closest('button, input, select, textarea, a, [role="button"]')
    );
  }
}
