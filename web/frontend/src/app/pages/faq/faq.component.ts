import { Component, computed, signal } from '@angular/core';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { FAQ_DATA, FaqItem } from './faq-data';

@Component({
  selector: 'app-faq',
  standalone: true,
  imports: [
    ScrollingModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatIconModule,
  ],
  template: `
    <header class="page-header">
      <h1>FAQ</h1>
      <p>Frequently asked questions about session types, reticulates, and tooling ({{ filteredFaqs().length }} items)</p>
    </header>

    <div class="faq-controls">
      <mat-form-field appearance="outline" class="search-field">
        <mat-label>Search FAQ</mat-label>
        <input matInput
               [value]="searchTerm()"
               (input)="onSearch($event)"
               placeholder="Type to filter..." />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>

      <div class="category-chips">
        <mat-chip-set>
          <mat-chip [highlighted]="selectedCategory() === null"
                    (click)="selectCategory(null)">
            All
          </mat-chip>
          @for (cat of categories; track cat) {
            <mat-chip [highlighted]="selectedCategory() === cat"
                      (click)="selectCategory(cat)">
              {{ cat }}
            </mat-chip>
          }
        </mat-chip-set>
      </div>
    </div>

    @if (filteredFaqs().length === 0) {
      <div class="no-results">
        <mat-icon>search_off</mat-icon>
        <p>No matching questions found.</p>
      </div>
    } @else {
      <cdk-virtual-scroll-viewport itemSize="64" class="faq-viewport">
        <mat-accordion multi>
          @for (item of filteredFaqs(); track item.question) {
            <mat-expansion-panel>
              <mat-expansion-panel-header>
                <mat-panel-title>{{ item.question }}</mat-panel-title>
                <mat-panel-description>{{ item.category }}</mat-panel-description>
              </mat-expansion-panel-header>
              <p [innerHTML]="item.answer"></p>
            </mat-expansion-panel>
          }
        </mat-accordion>
      </cdk-virtual-scroll-viewport>
    }
  `,
  styles: [`
    .page-header {
      padding: 24px 0 16px;
    }
    .page-header h1 {
      font-size: 24px;
      font-weight: 600;
      margin: 0 0 8px;
    }
    .page-header p {
      color: rgba(0, 0, 0, 0.6);
      margin: 0;
    }

    .faq-controls {
      margin: 16px 0;
    }
    .search-field {
      width: 100%;
    }
    .category-chips {
      margin: 8px 0 16px;
    }
    mat-chip {
      cursor: pointer;
    }

    .faq-viewport {
      height: calc(100vh - 320px);
      min-height: 400px;
    }

    .no-results {
      text-align: center;
      padding: 48px 16px;
      color: rgba(0, 0, 0, 0.5);
    }
    .no-results mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      margin-bottom: 12px;
    }
    .no-results p {
      font-size: 16px;
      margin: 0;
    }
  `],
})
export class FaqComponent {
  readonly searchTerm = signal('');
  readonly selectedCategory = signal<string | null>(null);

  readonly categories = [...new Set(FAQ_DATA.map((f) => f.category))];

  readonly filteredFaqs = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const cat = this.selectedCategory();
    return FAQ_DATA.filter((item) => {
      if (cat && item.category !== cat) return false;
      if (term) {
        const haystack = (item.question + ' ' + item.answer).toLowerCase();
        if (!haystack.includes(term)) return false;
      }
      return true;
    });
  });

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchTerm.set(value);
  }

  selectCategory(cat: string | null): void {
    this.selectedCategory.set(cat);
  }
}
