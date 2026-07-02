import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

let nextPagerId = 0;

@Component({
  selector: 'meme-pager',
  imports: [FormsModule],
  templateUrl: './pager.component.html',
  styleUrl: './pager.component.css'
})
export class PagerComponent {
  @Input({ required: true }) page = 1;
  @Input({ required: true }) totalPages = 1;
  @Input({ required: true }) pageSize = 10;
  @Input() pageSizeOptions: number[] = [10, 25, 50];
  @Input() label = 'Pages';

  @Output() readonly pageChange = new EventEmitter<number>();
  @Output() readonly pageSizeChange = new EventEmitter<number>();

  protected readonly pageSizeId = `pager-page-size-${nextPagerId++}`;

  protected setPageSize(value: string): void {
    this.pageSizeChange.emit(Number(value));
  }

  protected previous(): void {
    if (this.page > 1) {
      this.pageChange.emit(this.page - 1);
    }
  }

  protected next(): void {
    if (this.page < this.totalPages) {
      this.pageChange.emit(this.page + 1);
    }
  }
}
