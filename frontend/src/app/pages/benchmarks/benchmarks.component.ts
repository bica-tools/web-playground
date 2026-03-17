import { Component, OnInit, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ApiService } from '../../services/api.service';
import { BenchmarkDto } from '../../models/api.models';

@Component({
  selector: 'app-benchmarks',
  standalone: true,
  imports: [RouterLink, MatProgressSpinnerModule],
  template: `
    <section class="bench-hero">
      <h1>Benchmark Observatory</h1>
      <p>Real-world protocols verified through the full analysis pipeline</p>
    </section>

    <!-- Stats -->
    @if (!loading()) {
      <section class="stats-bar">
        <div class="st-item">
          <span class="st-val">{{ benchmarks().length }}</span>
          <span class="st-lbl">Protocols</span>
        </div>
        <div class="st-item">
          <span class="st-val">{{ parallelCount() }}</span>
          <span class="st-lbl">Use &#x2225;</span>
        </div>
        <div class="st-item">
          <span class="st-val">{{ recursiveCount() }}</span>
          <span class="st-lbl">Recursive</span>
        </div>
        <div class="st-item">
          <span class="st-val">{{ totalStates() }}</span>
          <span class="st-lbl">Total states</span>
        </div>
        <div class="st-item">
          <span class="st-val">{{ totalTests() }}</span>
          <span class="st-lbl">Tests generated</span>
        </div>
      </section>
    }

    <!-- Filters -->
    <div class="filter-row">
      <span class="f-label">Filter:</span>
      <button class="f-chip" [class.active]="filter() === 'all'" (click)="filter.set('all')">All</button>
      <button class="f-chip" [class.active]="filter() === 'parallel'" (click)="filter.set('parallel')">&#x2225; Parallel</button>
      <button class="f-chip" [class.active]="filter() === 'recursive'" (click)="filter.set('recursive')">Recursive</button>
      <button class="f-chip" [class.active]="filter() === 'simple'" (click)="filter.set('simple')">Simple</button>
      <button class="f-chip" [class.active]="filter() === 'large'" (click)="filter.set('large')">Large (&gt;5 states)</button>
    </div>

    <!-- Loading -->
    @if (loading()) {
      <div class="loading-state">
        <mat-spinner diameter="32"></mat-spinner>
        <span>Loading benchmarks...</span>
      </div>
    }

    <!-- Selected detail panel -->
    @if (selected()) {
      <div class="detail-overlay">
        <div class="detail-panel">
          <div class="dp-header">
            <span class="dp-title">{{ selected()!.name }}</span>
            <button class="dp-close" (click)="selected.set(null)">&times;</button>
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
            <div class="dp-right" [innerHTML]="selectedSvg()"></div>
          </div>
        </div>
      </div>
    }

    <!-- Card grid -->
    @if (!loading()) {
      <div class="bench-grid">
        @for (b of filtered(); track b.name) {
          <div class="bench-card" [class.selected]="selected()?.name === b.name" (click)="selectBenchmark(b)">
            <div class="bc-header">
              <span class="bc-name">{{ b.name }}</span>
              <div class="bc-badges">
                @if (b.isLattice) { <span class="bc-badge badge-lattice">Lattice</span> }
                @if (b.usesParallel) { <span class="bc-badge badge-par">&#x2225;</span> }
                @if (b.isRecursive) { <span class="bc-badge badge-rec">rec</span> }
              </div>
            </div>
            <div class="bc-desc">{{ b.description }}</div>
            <div class="bc-metrics">
              <span>{{ b.numStates }}s</span>
              <span>{{ b.numTransitions }}t</span>
              <span>{{ b.numMethods }}m</span>
              <span>{{ b.numTests }} tests</span>
            </div>
            @if (b.svgHtml) {
              <div class="bc-hasse" [innerHTML]="getSafeSvg(b.name)"></div>
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
  readonly selected = signal<BenchmarkDto | null>(null);
  readonly selectedSvg = signal<SafeHtml>('');

  readonly parallelCount = signal(0);
  readonly recursiveCount = signal(0);
  readonly totalStates = signal(0);
  readonly totalTests = signal(0);

  private svgCache = new Map<string, SafeHtml>();

  readonly filtered = computed(() => {
    const f = this.filter();
    const all = this.benchmarks();
    switch (f) {
      case 'parallel': return all.filter(b => b.usesParallel);
      case 'recursive': return all.filter(b => b.isRecursive);
      case 'simple': return all.filter(b => !b.usesParallel && !b.isRecursive);
      case 'large': return all.filter(b => b.numStates > 5);
      default: return all;
    }
  });

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
