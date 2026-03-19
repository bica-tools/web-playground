import { Component, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

interface ZooEntry {
  name: string;
  numStates: number;
  numTransitions: number;
  isLattice: boolean;
  svgHtml: string;
  typeString: string;
  tags: string[];
}

@Component({
  selector: 'app-zoo',
  standalone: true,
  imports: [RouterLink, MatProgressSpinnerModule],
  template: `
    <section class="zoo-hero">
      <h1>Lattice Zoo</h1>
      <p>A gallery of session type lattices — the geometry of protocols</p>
    </section>

    @if (loading()) {
      <div class="zoo-loading"><mat-spinner diameter="32"></mat-spinner></div>
    }

    <div class="zoo-grid">
      @for (entry of entries(); track entry.name) {
        <div class="zoo-card" [class.selected]="selected()?.name === entry.name" (click)="select(entry)">
          <figure class="zoo-hasse" [innerHTML]="getSafeSvg(entry.name)"></figure>
          <div class="zoo-info">
            <span class="zoo-name">{{ entry.name }}</span>
            <span class="zoo-meta">{{ entry.numStates }}s {{ entry.numTransitions }}t</span>
          </div>
        </div>
      }
    </div>

    @if (selected()) {
      <div class="zoo-detail" role="dialog">
        <div class="zoo-detail-inner">
          <div class="zoo-detail-header">
            <h2>{{ selected()!.name }}</h2>
            <button class="zoo-close" (click)="selected.set(null)">&times;</button>
          </div>
          <figure class="zoo-detail-hasse" [innerHTML]="selectedSvg()"></figure>
          <code class="zoo-detail-type">{{ selected()!.typeString }}</code>
          <div class="zoo-detail-tags">
            @for (tag of selected()!.tags; track tag) {
              <span class="zoo-tag">{{ tag }}</span>
            }
          </div>
          <div class="zoo-detail-actions">
            <a class="zoo-action" [routerLink]="['/tools/analyzer']" [queryParams]="{type: selected()!.typeString}">
              Analyze &rarr;
            </a>
            <button class="zoo-action" (click)="downloadSvg()">Download SVG</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .zoo-hero {
      width: 100vw; margin-left: calc(-50vw + 50%); margin-top: -24px;
      background: linear-gradient(145deg, #0f172a, #1e1b4b 40%, #312e81 70%, #4338ca);
      color: #fff; padding: 48px 24px 40px; text-align: center;
      h1 { font-size: 32px; font-weight: 700; margin: 0 0 8px; }
      p { font-size: 14px; color: rgba(255,255,255,0.75); margin: 0; }
    }
    .zoo-loading { display: flex; justify-content: center; padding: 48px; }
    .zoo-grid {
      max-width: 1200px; margin: 24px auto; padding: 0 16px 48px;
      display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px;
    }
    .zoo-card {
      border: 1px solid rgba(0,0,0,0.06); border-radius: 12px;
      background: #fff; overflow: hidden; cursor: pointer;
      transition: all 0.2s;
      &:hover { box-shadow: 0 8px 32px rgba(99,102,241,0.12); transform: translateY(-2px); }
      &.selected { border-color: #4338ca; box-shadow: 0 0 0 2px rgba(99,102,241,0.2); }
    }
    .zoo-hasse {
      display: flex; justify-content: center; align-items: center;
      padding: 16px 12px; background: #fafbfc; min-height: 140px; margin: 0;
    }
    :host ::ng-deep .zoo-hasse svg { max-width: 100%; max-height: 160px; }
    .zoo-info {
      padding: 10px 14px; display: flex; justify-content: space-between; align-items: center;
      border-top: 1px solid rgba(0,0,0,0.04);
    }
    .zoo-name { font-size: 12px; font-weight: 600; color: rgba(0,0,0,0.75); }
    .zoo-meta { font-size: 10px; color: rgba(0,0,0,0.4); font-family: 'JetBrains Mono', monospace; }

    /* Detail overlay */
    .zoo-detail {
      position: fixed; inset: 0; z-index: 1000;
      background: rgba(0,0,0,0.5); display: flex;
      align-items: center; justify-content: center; padding: 24px;
    }
    .zoo-detail-inner {
      background: #fff; border-radius: 16px; max-width: 700px; width: 100%;
      max-height: 90vh; overflow-y: auto; padding: 24px;
    }
    .zoo-detail-header {
      display: flex; justify-content: space-between; align-items: center;
      h2 { font-size: 20px; font-weight: 600; margin: 0; }
    }
    .zoo-close {
      background: none; border: none; font-size: 24px; cursor: pointer;
      color: rgba(0,0,0,0.5); &:hover { color: rgba(0,0,0,0.8); }
    }
    .zoo-detail-hasse {
      display: flex; justify-content: center; margin: 16px 0; padding: 16px;
      background: #fafbfc; border-radius: 10px;
    }
    :host ::ng-deep .zoo-detail-hasse svg { max-width: 100%; max-height: 400px; }
    .zoo-detail-type {
      display: block; font-size: 12px; color: rgba(0,0,0,0.55);
      font-family: 'JetBrains Mono', monospace; background: #f8f9fb;
      border-radius: 8px; padding: 10px 14px; margin: 12px 0;
      white-space: pre-wrap; word-break: break-all;
    }
    .zoo-detail-tags { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 12px; }
    .zoo-tag {
      padding: 2px 8px; border-radius: 8px; font-size: 10px;
      background: #f1f5f9; color: rgba(0,0,0,0.5);
    }
    .zoo-detail-actions { display: flex; gap: 12px; }
    .zoo-action {
      font-size: 13px; color: #4338ca; text-decoration: none;
      background: none; border: none; cursor: pointer; font-family: inherit;
      &:hover { text-decoration: underline; }
    }
  `],
})
export class ZooComponent implements OnInit {
  readonly entries = signal<ZooEntry[]>([]);
  readonly loading = signal(true);
  readonly selected = signal<ZooEntry | null>(null);
  readonly selectedSvg = signal<SafeHtml>('');
  private svgCache = new Map<string, SafeHtml>();

  constructor(private http: HttpClient, private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.http.get<ZooEntry[]>('/api/lattice-zoo').subscribe({
      next: (data) => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  getSafeSvg(name: string): SafeHtml {
    if (this.svgCache.has(name)) return this.svgCache.get(name)!;
    const entry = this.entries().find(e => e.name === name);
    if (!entry?.svgHtml) return '';
    const safe = this.sanitizer.bypassSecurityTrustHtml(entry.svgHtml);
    this.svgCache.set(name, safe);
    return safe;
  }

  select(entry: ZooEntry): void {
    this.selected.set(entry);
    this.selectedSvg.set(this.sanitizer.bypassSecurityTrustHtml(entry.svgHtml));
  }

  downloadSvg(): void {
    const entry = this.selected();
    if (!entry) return;
    const blob = new Blob([entry.svgHtml], { type: 'image/svg+xml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = entry.name.replace(/[^a-zA-Z0-9]/g, '-').toLowerCase() + '.svg';
    a.click();
    URL.revokeObjectURL(url);
  }
}
