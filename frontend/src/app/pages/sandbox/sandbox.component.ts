import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-sandbox',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, RouterLink],
  template: `
    <section class="sb-header">
      <h1>Live Sandbox</h1>
      <p>Write method calls against a session type — watch the Hasse diagram track your position</p>
    </section>

    <div class="sb-layout">
      <!-- Left: protocol + trace input -->
      <div class="sb-left">
        <label class="sb-label">Session type</label>
        <textarea class="sb-type" [(ngModel)]="typeString" spellcheck="false" rows="2"
                  placeholder="rec X . &{read: X, close: end}"></textarea>

        <button class="sb-load-btn" (click)="loadType()" [disabled]="!typeString.trim() || loadingType()">
          {{ loadingType() ? 'Loading...' : 'Load Protocol' }}
        </button>

        @if (hasseLoaded()) {
          <label class="sb-label">Method calls (one per line)</label>
          <textarea class="sb-trace" [(ngModel)]="traceText" spellcheck="false" rows="8"
                    placeholder="read\nread\nclose"
                    (ngModelChange)="onTraceChange()"></textarea>

          @if (traceResult()) {
            <div class="sb-verdict" [class.sb-valid]="traceResult()!.valid" [class.sb-invalid]="!traceResult()!.valid">
              @if (traceResult()!.valid && traceResult()!.complete) {
                Complete path — reached end state
              } @else if (traceResult()!.valid) {
                Valid so far — at state {{ traceResult()!.currentState }}
              } @else {
                Violation at {{ traceResult()!.violationAt }}
              }
            </div>
          }

          @if (traceResult()?.path; as path) {
            <div class="sb-path">
              @for (step of path; track $index) {
                <span class="sb-step" [class.sb-step-valid]="step.valid" [class.sb-step-invalid]="!step.valid">
                  {{ step.method }}
                </span>
              }
            </div>
          }
        }

        <div class="sb-examples">
          <span class="sb-ex-label">Examples:</span>
          @for (ex of examples; track ex.name) {
            <button class="sb-ex-chip" (click)="loadExample(ex)">{{ ex.name }}</button>
          }
        </div>
      </div>

      <!-- Right: Hasse diagram -->
      <div class="sb-right">
        @if (!hasseLoaded() && !loadingType()) {
          <div class="sb-empty">Load a protocol to see the Hasse diagram</div>
        }
        @if (loadingType()) {
          <div class="sb-empty"><mat-spinner diameter="28"></mat-spinner></div>
        }
        @if (hasseSvg()) {
          <figure class="sb-hasse" [innerHTML]="hasseSvg()"></figure>
        }
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .sb-header {
      width: 100vw; margin-left: calc(-50vw + 50%); margin-top: -24px;
      background: linear-gradient(145deg, #1e1b4b, #312e81 60%, #4338ca);
      color: #fff; padding: 36px 24px 32px; text-align: center;
      h1 { font-size: 28px; font-weight: 700; margin: 0 0 6px; }
      p { font-size: 13px; color: rgba(255,255,255,0.85); margin: 0; }
    }
    .sb-layout {
      display: grid; grid-template-columns: 1fr 1fr; gap: 24px;
      max-width: 1200px; margin: 24px auto; padding: 0 16px;
    }
    @media (max-width: 768px) { .sb-layout { grid-template-columns: 1fr; } }
    .sb-label {
      display: block; font-size: 11px; text-transform: uppercase;
      letter-spacing: 0.4px; color: rgba(0,0,0,0.5); font-weight: 500;
      margin: 12px 0 4px;
    }
    .sb-type, .sb-trace {
      width: 100%; box-sizing: border-box; padding: 8px 10px;
      border: 1px solid rgba(0,0,0,0.12); border-radius: 6px;
      font-family: 'JetBrains Mono', monospace; font-size: 12px; resize: vertical;
    }
    .sb-type:focus, .sb-trace:focus { outline: none; border-color: #4338ca; }
    .sb-load-btn {
      margin-top: 8px; padding: 6px 16px; background: #4338ca; color: #fff;
      border: none; border-radius: 6px; font-size: 12px; cursor: pointer;
      font-family: inherit;
      &:hover { background: #3730a3; }
      &:disabled { opacity: 0.5; cursor: not-allowed; }
    }
    .sb-verdict {
      margin: 8px 0; padding: 6px 12px; border-radius: 8px;
      font-size: 12px; font-weight: 500;
    }
    .sb-valid { background: #ecfdf5; color: #065f46; }
    .sb-invalid { background: #fef2f2; color: #b91c1c; }
    .sb-path { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
    .sb-step {
      padding: 2px 8px; border-radius: 8px; font-size: 11px;
      font-family: 'JetBrains Mono', monospace;
    }
    .sb-step-valid { background: #ecfdf5; color: #065f46; }
    .sb-step-invalid { background: #fef2f2; color: #b91c1c; }
    .sb-examples {
      margin-top: 16px; display: flex; gap: 4px; flex-wrap: wrap; align-items: center;
    }
    .sb-ex-label { font-size: 11px; color: rgba(0,0,0,0.45); }
    .sb-ex-chip {
      padding: 3px 10px; border: 1px solid rgba(0,0,0,0.1); border-radius: 12px;
      background: transparent; font-size: 11px; cursor: pointer; font-family: inherit;
      &:hover { border-color: #4338ca; color: #4338ca; }
    }
    .sb-right {
      background: #fafbfc; border: 1px solid rgba(0,0,0,0.06);
      border-radius: 12px; padding: 16px; min-height: 300px;
      display: flex; align-items: center; justify-content: center;
    }
    .sb-empty { color: rgba(0,0,0,0.35); font-size: 13px; text-align: center; }
    .sb-hasse { margin: 0; display: flex; justify-content: center; }
    :host ::ng-deep .sb-hasse svg { max-width: 100%; max-height: 500px; }
  `],
})
export class SandboxComponent {
  typeString = '';
  traceText = '';
  readonly hasseLoaded = signal(false);
  readonly loadingType = signal(false);
  readonly hasseSvg = signal<SafeHtml>('');
  readonly traceResult = signal<any>(null);

  examples = [
    { name: 'Iterator', type: 'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}', trace: 'hasNext\nnext\nhasNext' },
    { name: 'File', type: 'open . rec X . &{read: +{data: X, eof: close . end}}', trace: 'open\nread\nread\nclose' },
    { name: 'ATM', type: 'insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: ejectCard . end}', trace: 'insertCard\nenterPIN\ncheckBalance\nwithdraw\nejectCard' },
  ];

  constructor(
    private http: HttpClient,
    private api: ApiService,
    private sanitizer: DomSanitizer,
  ) {}

  loadType(): void {
    if (!this.typeString.trim()) return;
    this.loadingType.set(true);
    this.api.analyze(this.typeString.trim()).subscribe({
      next: (res) => {
        this.hasseSvg.set(this.sanitizer.bypassSecurityTrustHtml(res.svgHtml));
        this.hasseLoaded.set(true);
        this.loadingType.set(false);
        this.traceResult.set(null);
      },
      error: () => this.loadingType.set(false),
    });
  }

  onTraceChange(): void {
    const methods = this.traceText.split('\n').map(s => s.trim()).filter(s => s.length > 0);
    if (methods.length === 0) {
      this.traceResult.set(null);
      return;
    }
    this.http.post<any>('/api/validate-trace', {
      typeString: this.typeString.trim(),
      trace: methods,
    }).subscribe({
      next: (res) => this.traceResult.set(res),
      error: () => this.traceResult.set(null),
    });
  }

  loadExample(ex: { type: string; trace: string }): void {
    this.typeString = ex.type;
    this.traceText = ex.trace;
    this.loadType();
    setTimeout(() => this.onTraceChange(), 500);
  }
}
