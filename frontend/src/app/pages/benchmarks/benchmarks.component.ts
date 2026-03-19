import { Component, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ApiService } from '../../services/api.service';
import { BenchmarkDto } from '../../models/api.models';
import { FadeInDirective } from '../../shared/fade-in.directive';
import { CounterComponent } from '../../shared/counter/counter.component';

@Component({
  selector: 'app-benchmarks',
  standalone: true,
  imports: [FormsModule, RouterLink, MatProgressSpinnerModule, FadeInDirective, CounterComponent],
  template: `
    <section class="bench-hero">
      <h1>Benchmark Observatory</h1>
      <p>Real-world protocols verified through the full analysis pipeline</p>
    </section>

    <!-- Stats -->
    @if (!loading()) {
      <section class="stats-bar">
        <div class="st-item">
          <span class="st-val"><app-counter [target]="benchmarks().length"></app-counter></span>
          <span class="st-lbl">Protocols</span>
        </div>
        <div class="st-item">
          <span class="st-val"><app-counter [target]="parallelCount()"></app-counter></span>
          <span class="st-lbl">Use &#x2225;</span>
        </div>
        <div class="st-item">
          <span class="st-val"><app-counter [target]="recursiveCount()"></app-counter></span>
          <span class="st-lbl">Recursive</span>
        </div>
        <div class="st-item">
          <span class="st-val"><app-counter [target]="totalStates()"></app-counter></span>
          <span class="st-lbl">Total states</span>
        </div>
        <div class="st-item">
          <span class="st-val"><app-counter [target]="totalTests()"></app-counter></span>
          <span class="st-lbl">Tests generated</span>
        </div>
      </section>
    }

    <!-- Search + Filters + Sort -->
    <div class="search-row">
      <input class="search-input" type="text" placeholder="Search protocols..." [ngModel]="search()" (ngModelChange)="search.set($event)" aria-label="Search benchmarks" />
      <select class="sort-select" [ngModel]="sortBy()" (ngModelChange)="sortBy.set($event)" aria-label="Sort benchmarks">
        <option value="name">Name</option>
        <option value="states-asc">States (asc)</option>
        <option value="states-desc">States (desc)</option>
        <option value="transitions-desc">Transitions (desc)</option>
        <option value="tests-desc">Tests (desc)</option>
        <option value="methods-desc">Methods (desc)</option>
      </select>
    </div>

    <div class="filter-row" role="group" aria-label="Benchmark filters">
      <span class="f-label" id="filter-label">Filter:</span>
      <button class="f-chip" [class.active]="filter() === 'all'" [attr.aria-pressed]="filter() === 'all'" (click)="filter.set('all')">All</button>
      <button class="f-chip" [class.active]="filter() === 'parallel'" [attr.aria-pressed]="filter() === 'parallel'" (click)="filter.set('parallel')">&#x2225; Parallel</button>
      <button class="f-chip" [class.active]="filter() === 'recursive'" [attr.aria-pressed]="filter() === 'recursive'" (click)="filter.set('recursive')">Recursive</button>
      <button class="f-chip" [class.active]="filter() === 'simple'" [attr.aria-pressed]="filter() === 'simple'" (click)="filter.set('simple')">Simple</button>
      <button class="f-chip" [class.active]="filter() === 'large'" [attr.aria-pressed]="filter() === 'large'" (click)="filter.set('large')">Large (&gt;5 states)</button>
    </div>

    <!-- Tag filters -->
    @if (allTags().length > 0) {
      <div class="filter-row" role="group" aria-label="Tag filters">
        <span class="f-label">Tags:</span>
        @for (tag of allTags(); track tag) {
          <button class="f-chip tag-chip" [class.active]="activeTag() === tag" [attr.aria-pressed]="activeTag() === tag" (click)="toggleTag(tag)">{{ tag }}</button>
        }
      </div>
    }

    <!-- Result count -->
    @if (!loading()) {
      <div class="result-count">
        {{ filtered().length }} of {{ benchmarks().length }} protocols
        @if (search() || activeTag() || filter() !== 'all') {
          <button class="clear-btn" (click)="clearFilters()">Clear filters</button>
        }
      </div>
    }

    <!-- Loading -->
    @if (loading()) {
      <div class="loading-state">
        <mat-spinner diameter="32"></mat-spinner>
        <span>Loading benchmarks...</span>
      </div>
    }

    <!-- Selected detail panel -->
    @if (selected()) {
      <div class="detail-overlay" role="dialog" aria-modal="true" [attr.aria-label]="'Details for ' + selected()!.name">
        <div class="detail-panel">
          <div class="dp-header">
            <span class="dp-title">{{ selected()!.name }}</span>
            <button class="dp-close" (click)="selected.set(null)" aria-label="Close detail panel">&times;</button>
          </div>
          <div class="dp-body">
            <div class="dp-left">
              <div class="dp-pretty">{{ selected()!.pretty }}</div>
              <div class="dp-desc">{{ selected()!.description }}</div>
              <div class="dp-metrics">
                <div class="dp-metric">
                  <div class="dp-m-label">States</div>
                  <div class="dp-m-value">{{ selected()!.numStates }}</div>
                </div>
                <div class="dp-metric">
                  <div class="dp-m-label">Transitions</div>
                  <div class="dp-m-value">{{ selected()!.numTransitions }}</div>
                </div>
                <div class="dp-metric">
                  <div class="dp-m-label">Tests</div>
                  <div class="dp-m-value">{{ selected()!.numTests }}</div>
                </div>
                <div class="dp-metric">
                  <div class="dp-m-label">SCCs</div>
                  <div class="dp-m-value">{{ selected()!.numSccs }}</div>
                </div>
              </div>
              @if (selected()!.methods && selected()!.methods.length > 0) {
                <div class="dp-methods">
                  @for (m of selected()!.methods; track m) {
                    <span class="dp-method">{{ m }}</span>
                  }
                </div>
              }
              <div class="dp-links">
                <a class="dp-link" [routerLink]="['/tools/analyzer']" [queryParams]="{type: selected()!.typeString}">
                  Open in Analyzer &rarr;
                </a>
                <a class="dp-link" [routerLink]="['/tools/test-generator']"
                   [queryParams]="{type: selected()!.typeString, class: toClassName(selected()!.name)}">
                  Generate Tests &rarr;
                </a>
              </div>
            </div>
            <figure class="dp-right" role="img" [attr.aria-label]="'Hasse diagram of ' + selected()!.name" [innerHTML]="selectedSvg()"></figure>
          </div>
        </div>
      </div>
    }

    <!-- Card grid -->
    @if (!loading()) {
      <div class="bench-grid">
        @for (b of filtered(); track b.name; let i = $index) {
          <div class="bench-card" [appFadeIn]="i * 60" role="button" tabindex="0" [attr.aria-label]="'View details for ' + b.name" [class.selected]="selected()?.name === b.name" (click)="selectBenchmark(b)" (keydown.enter)="selectBenchmark(b)">
            <div class="bc-header">
              <span class="bc-name">{{ b.name }}</span>
              <div class="bc-badges">
                @if (b.isLattice) { <span class="bc-badge badge-lattice">Lattice</span> }
                @if (b.usesParallel) { <span class="bc-badge badge-par">&#x2225;</span> }
                @if (b.isRecursive) { <span class="bc-badge badge-rec">rec</span> }
              </div>
            </div>
            @if (b.tags && b.tags.length > 0) {
              <div class="bc-tags">
                @for (tag of b.tags; track tag) {
                  <span class="bc-tag" (click)="$event.stopPropagation(); toggleTag(tag)">{{ tag }}</span>
                }
              </div>
            }
            <div class="bc-desc">{{ b.description }}</div>
            <div class="bc-metrics">
              <span>{{ b.numStates }}s</span>
              <span>{{ b.numTransitions }}t</span>
              <span>{{ b.numMethods }}m</span>
              <span>{{ b.numTests }} tests</span>
            </div>
            @if (b.svgHtml) {
              <div class="bc-hasse" [innerHTML]="getSafeSvg(b.name)" role="img" [attr.aria-label]="'Hasse diagram of ' + b.name"></div>
            }
            <div class="bc-actions">
              <a class="bc-link" [routerLink]="['/tools/analyzer']" [queryParams]="{type: b.typeString}" (click)="$event.stopPropagation()">
                Analyze
              </a>
              <a class="bc-link" [routerLink]="['/tools/test-generator']"
                 [queryParams]="{type: b.typeString, class: toClassName(b.name)}" (click)="$event.stopPropagation()">
                Tests
              </a>
            </div>
          </div>
        }
      </div>
    }
  `,
  styleUrl: './benchmarks.component.scss',
})
export class BenchmarksComponent implements OnInit {
  readonly benchmarks = signal<BenchmarkDto[]>([]);
  readonly loading = signal(true);
  readonly filter = signal('all');
  readonly search = signal('');
  readonly activeTag = signal('');
  readonly sortBy = signal('name');
  readonly selected = signal<BenchmarkDto | null>(null);
  readonly selectedSvg = signal<SafeHtml>('');

  readonly parallelCount = signal(0);
  readonly recursiveCount = signal(0);
  readonly totalStates = signal(0);
  readonly totalTests = signal(0);

  private svgCache = new Map<string, SafeHtml>();

  readonly allTags = computed(() => {
    const tagSet = new Set<string>();
    for (const b of this.benchmarks()) {
      if (b.tags) b.tags.forEach(t => tagSet.add(t));
    }
    return [...tagSet].sort();
  });

  readonly filtered = computed(() => {
    const f = this.filter();
    const q = this.search().toLowerCase().trim();
    const tag = this.activeTag();
    const sort = this.sortBy();
    let list = this.benchmarks();

    // Property filter
    switch (f) {
      case 'parallel': list = list.filter(b => b.usesParallel); break;
      case 'recursive': list = list.filter(b => b.isRecursive); break;
      case 'simple': list = list.filter(b => !b.usesParallel && !b.isRecursive); break;
      case 'large': list = list.filter(b => b.numStates > 5); break;
    }

    // Tag filter
    if (tag) {
      list = list.filter(b => b.tags && b.tags.includes(tag));
    }

    // Search filter
    if (q) {
      list = list.filter(b =>
        b.name.toLowerCase().includes(q) ||
        b.description.toLowerCase().includes(q) ||
        (b.methods && b.methods.some(m => m.toLowerCase().includes(q))) ||
        (b.tags && b.tags.some(t => t.toLowerCase().includes(q)))
      );
    }

    // Sort
    list = [...list];
    switch (sort) {
      case 'name': list.sort((a, b) => a.name.localeCompare(b.name)); break;
      case 'states-asc': list.sort((a, b) => a.numStates - b.numStates); break;
      case 'states-desc': list.sort((a, b) => b.numStates - a.numStates); break;
      case 'transitions-desc': list.sort((a, b) => b.numTransitions - a.numTransitions); break;
      case 'tests-desc': list.sort((a, b) => b.numTests - a.numTests); break;
      case 'methods-desc': list.sort((a, b) => b.numMethods - a.numMethods); break;
    }

    return list;
  });

  toggleTag(tag: string): void {
    this.activeTag.set(this.activeTag() === tag ? '' : tag);
  }

  clearFilters(): void {
    this.search.set('');
    this.filter.set('all');
    this.activeTag.set('');
    this.sortBy.set('name');
  }

  constructor(private api: ApiService, private sanitizer: DomSanitizer) {}

  toClassName(name: string): string {
    return name.replace(/[^a-zA-Z0-9]/g, '');
  }

  getSafeSvg(name: string): SafeHtml {
    if (this.svgCache.has(name)) return this.svgCache.get(name)!;
    const b = this.benchmarks().find(x => x.name === name);
    if (!b?.svgHtml) return '';
    const safe = this.sanitizer.bypassSecurityTrustHtml(b.svgHtml);
    this.svgCache.set(name, safe);
    return safe;
  }

  selectBenchmark(b: BenchmarkDto): void {
    if (this.selected()?.name === b.name) {
      this.selected.set(null);
      return;
    }
    this.selected.set(b);
    this.selectedSvg.set(this.sanitizer.bypassSecurityTrustHtml(b.svgHtml));
  }

  ngOnInit(): void {
    this.api.getBenchmarks().subscribe({
      next: (data) => {
        this.benchmarks.set(data);
        this.parallelCount.set(data.filter(b => b.usesParallel).length);
        this.recursiveCount.set(data.filter(b => b.isRecursive).length);
        this.totalStates.set(data.reduce((s, b) => s + b.numStates, 0));
        this.totalTests.set(data.reduce((s, b) => s + b.numTests, 0));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
