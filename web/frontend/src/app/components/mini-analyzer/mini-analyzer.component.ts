import { Component, Input, Output, EventEmitter, OnInit, OnChanges, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { AnalyzeResponse } from '../../models/api.models';

@Component({
  selector: 'app-mini-analyzer',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule],
  template: `
    <div class="mini-analyzer">
      <label class="mini-label">Session type (editable)</label>
      <textarea
        class="mini-input"
        [ngModel]="currentType()"
        (ngModelChange)="onTypeChange($event)"
        spellcheck="false"
        rows="2"
      ></textarea>

      <div class="mini-result">
        @if (loading()) {
          <div class="mini-loading">
            <mat-spinner diameter="20"></mat-spinner>
          </div>
        }

        @if (error()) {
          <div class="mini-error">{{ error() }}</div>
        }

        @if (result() && !loading()) {
          <div class="mini-badges">
            <span class="mini-badge" [class.is-lattice]="result()!.isLattice">
              {{ result()!.isLattice ? 'Lattice' : 'Not a lattice' }}
            </span>
            <span class="mini-stat">{{ result()!.numStates }}s</span>
            <span class="mini-stat">{{ result()!.numTransitions }}t</span>
          </div>
          <figure class="mini-hasse" [innerHTML]="safeSvg()"></figure>
        }
      </div>
    </div>
  `,
  styles: [`
    .mini-analyzer {
      border: 1px solid rgba(99,102,241,0.15); border-radius: 10px;
      background: #fafbff; padding: 14px; margin: 12px 0;
    }
    .mini-label {
      font-size: 11px; text-transform: uppercase; letter-spacing: 0.4px;
      color: rgba(0,0,0,0.5); font-weight: 500; display: block; margin-bottom: 6px;
    }
    .mini-input {
      width: 100%; box-sizing: border-box; padding: 8px 10px;
      border: 1px solid rgba(0,0,0,0.12); border-radius: 6px;
      font-family: 'JetBrains Mono', monospace; font-size: 12px;
      resize: vertical; background: #fff;
    }
    .mini-input:focus { outline: none; border-color: #4338ca; }
    .mini-result { margin-top: 10px; min-height: 60px; }
    .mini-loading {
      display: flex; justify-content: center; padding: 16px 0;
    }
    .mini-error {
      font-size: 12px; color: #b91c1c; padding: 6px 0;
    }
    .mini-badges {
      display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
    }
    .mini-badge {
      padding: 2px 8px; border-radius: 8px; font-size: 10px; font-weight: 600;
      background: #fef2f2; color: #b91c1c;
    }
    .mini-badge.is-lattice { background: #ecfdf5; color: #065f46; }
    .mini-stat {
      font-size: 11px; color: rgba(0,0,0,0.45);
      font-family: 'JetBrains Mono', monospace;
    }
    .mini-hasse {
      display: flex; justify-content: center; margin: 0; padding: 4px 0;
    }
    :host ::ng-deep .mini-hasse svg { max-width: 100%; max-height: 250px; }
  `],
})
export class MiniAnalyzerComponent implements OnInit, OnChanges {
  @Input() typeString = '';
  @Output() analyzed = new EventEmitter<AnalyzeResponse>();

  readonly currentType = signal('');
  readonly result = signal<AnalyzeResponse | null>(null);
  readonly safeSvg = signal<SafeHtml>('');
  readonly loading = signal(false);
  readonly error = signal('');

  private typeSubject = new Subject<string>();

  constructor(private api: ApiService, private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.currentType.set(this.typeString);

    this.typeSubject.pipe(
      debounceTime(600),
      distinctUntilChanged(),
      switchMap((type) => {
        if (!type.trim()) {
          this.result.set(null);
          this.error.set('');
          return of(null);
        }
        this.loading.set(true);
        this.error.set('');
        return this.api.analyze(type);
      }),
    ).subscribe({
      next: (res) => {
        if (res) {
          this.result.set(res);
          this.safeSvg.set(this.sanitizer.bypassSecurityTrustHtml(res.svgHtml));
          this.analyzed.emit(res);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || 'Analysis failed');
        this.loading.set(false);
      },
    });

    // Trigger initial analysis
    if (this.typeString.trim()) {
      this.typeSubject.next(this.typeString);
    }
  }

  ngOnChanges(): void {
    this.currentType.set(this.typeString);
    this.typeSubject.next(this.typeString);
  }

  onTypeChange(value: string): void {
    this.currentType.set(value);
    this.typeSubject.next(value);
  }
}
