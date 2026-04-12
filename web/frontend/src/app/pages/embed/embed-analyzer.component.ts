import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../services/api.service';
import { AnalyzeResponse } from '../../models/api.models';

@Component({
  selector: 'app-embed-analyzer',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    @if (loading()) {
      <div class="embed-loading">
        <mat-spinner diameter="28"></mat-spinner>
      </div>
    }

    @if (error()) {
      <div class="embed-error">{{ error() }}</div>
    }

    @if (result()) {
      <div class="embed-result">
        <div class="embed-header">
          <span class="embed-badge" [class.is-lattice]="result()!.isLattice">
            {{ result()!.isLattice ? 'Lattice' : 'Not a lattice' }}
          </span>
          <span class="embed-stat">{{ result()!.numStates }}s</span>
          <span class="embed-stat">{{ result()!.numTransitions }}t</span>
        </div>
        <figure class="embed-hasse" [innerHTML]="safeSvg()"></figure>
        <div class="embed-footer">
          <code class="embed-type">{{ result()!.pretty }}</code>
          <a class="embed-link" [href]="analyzerUrl" target="_blank" rel="noopener">
            Open in BICA Tools &rarr;
          </a>
        </div>
      </div>
    }
  `,
  styles: [`
    :host {
      display: block; font-family: 'Inter', sans-serif; background: #fff;
      color: rgba(0,0,0,0.8); overflow: hidden;
    }
    .embed-loading {
      display: flex; align-items: center; justify-content: center;
      min-height: 120px;
    }
    .embed-error {
      padding: 16px; color: #b91c1c; font-size: 13px; text-align: center;
    }
    .embed-result { padding: 8px; }
    .embed-header {
      display: flex; align-items: center; gap: 8px; padding: 4px 0 8px;
    }
    .embed-badge {
      padding: 2px 8px; border-radius: 8px; font-size: 11px; font-weight: 600;
      background: #fef2f2; color: #b91c1c;
    }
    .embed-badge.is-lattice { background: #ecfdf5; color: #065f46; }
    .embed-stat {
      font-size: 11px; color: rgba(0,0,0,0.45);
      font-family: 'JetBrains Mono', monospace;
    }
    .embed-hasse {
      display: flex; justify-content: center; margin: 0; padding: 4px 0;
    }
    :host ::ng-deep .embed-hasse svg { max-width: 100%; max-height: 300px; }
    .embed-footer {
      border-top: 1px solid rgba(0,0,0,0.06); padding: 8px 0 4px;
      display: flex; align-items: center; justify-content: space-between; gap: 8px;
    }
    .embed-type {
      font-size: 11px; color: rgba(0,0,0,0.5); overflow: hidden;
      text-overflow: ellipsis; white-space: nowrap; flex: 1;
      font-family: 'JetBrains Mono', monospace;
    }
    .embed-link {
      font-size: 11px; color: #4338ca; text-decoration: none; white-space: nowrap;
    }
    .embed-link:hover { text-decoration: underline; }
  `],
})
export class EmbedAnalyzerComponent implements OnInit {
  readonly loading = signal(true);
  readonly error = signal('');
  readonly result = signal<AnalyzeResponse | null>(null);
  readonly safeSvg = signal<SafeHtml>('');
  analyzerUrl = '';

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const type = params['type'];
      if (!type) {
        this.loading.set(false);
        this.error.set('No type parameter provided');
        return;
      }
      this.analyzerUrl = 'https://bica-tools.org/tools/analyzer?type=' + encodeURIComponent(type);
      this.api.analyze(type).subscribe({
        next: (res) => {
          this.result.set(res);
          this.safeSvg.set(this.sanitizer.bypassSecurityTrustHtml(res.svgHtml));
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.error || 'Analysis failed');
          this.loading.set(false);
        },
      });
    });
  }
}
