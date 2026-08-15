import { Component, Input } from '@angular/core';

export type MemeIconName =
  | 'arrow-clockwise'
  | 'arrow-counterclockwise'
  | 'arrow-left'
  | 'arrow-right'
  | 'arrow-up'
  | 'arrow-down'
  | 'arrow-down-up'
  | 'box-arrow-up-right'
  | 'card-checklist'
  | 'chat'
  | 'check'
  | 'check-circle'
  | 'chevron-down'
  | 'chevron-left'
  | 'chevron-right'
  | 'dash'
  | 'download'
  | 'eye'
  | 'file-text'
  | 'flag'
  | 'files'
  | 'gear'
  | 'grip-vertical'
  | 'journal-text'
  | 'list-task'
  | 'pencil'
  | 'pencil-square'
  | 'person-plus'
  | 'plus'
  | 'trash'
  | 'upload'
  | 'x';

@Component({
  selector: 'meme-icon',
  templateUrl: './icon.component.html',
  styleUrl: './icon.component.css'
})
export class IconComponent {
  @Input({ required: true }) name: MemeIconName = 'x';
}
