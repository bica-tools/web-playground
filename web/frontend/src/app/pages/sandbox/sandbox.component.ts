import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-sandbox',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule],
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

            @if (traceResult()!.valid) {
              <div class="sb-current-state" [class.sb-current-complete]="traceResult()!.complete">
                <span class="sb-current-label">Current State</span>
                <span class="sb-current-id">{{ traceResult()!.currentState }}</span>
                @if (traceResult()!.complete) {
                  <span class="sb-current-badge">END</span>
                }
              </div>
            }
          }

          @if (traceResult()?.path; as path) {
            <div class="sb-path">
              @for (step of path; track $index) {
                <span class="sb-step"
                      [class.sb-step-valid]="step.valid"
                      [class.sb-step-invalid]="!step.valid"
                      [class.sb-step-last]="step.valid && $last">
                  <span class="sb-step-state">{{ step.from }}</span>
                  <span class="sb-step-arrow">→</span>
                  <span class="sb-step-method">{{ step.method }}</span>
                  <span class="sb-step-arrow">→</span>
                  <span class="sb-step-state">{{ step.to }}</span>
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
    .sb-current-state {
      display: flex; align-items: center; gap: 10px;
      margin: 10px 0 4px; padding: 10px 14px;
      background: linear-gradient(135deg, #eef2ff, #e0e7ff);
      border: 1px solid #c7d2fe; border-radius: 10px;
    }
    .sb-current-complete {
      background: linear-gradient(135deg, #ecfdf5, #d1fae5);
      border-color: #6ee7b7;
    }
    .sb-current-label {
      font-size: 10px; text-transform: uppercase; letter-spacing: 0.5px;
      color: rgba(0,0,0,0.45); font-weight: 600;
    }
    .sb-current-id {
      font-size: 22px; font-weight: 700; color: #4338ca;
      font-family: 'JetBrains Mono', monospace;
    }
    .sb-current-complete .sb-current-id { color: #059669; }
    .sb-current-badge {
      font-size: 9px; font-weight: 700; color: #fff;
      background: #059669; padding: 2px 8px; border-radius: 6px;
      letter-spacing: 0.5px;
    }
    .sb-path { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
    .sb-step {
      display: inline-flex; align-items: center; gap: 3px;
      padding: 3px 8px; border-radius: 8px; font-size: 11px;
      font-family: 'JetBrains Mono', monospace;
      transition: transform 0.15s ease;
    }
    .sb-step:hover { transform: scale(1.05); }
    .sb-step-valid { background: #ecfdf5; color: #065f46; }
    .sb-step-invalid { background: #fef2f2; color: #b91c1c; }
    .sb-step-last { background: #e0e7ff; color: #3730a3; border: 1px solid #a5b4fc; }
    .sb-step-state { font-weight: 700; font-size: 10px; opacity: 0.7; }
    .sb-step-arrow { font-size: 9px; opacity: 0.4; }
    .sb-step-method { font-weight: 600; }
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
  private rawSvg = '';

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
        this.rawSvg = res.svgHtml;
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
      this.hasseSvg.set(this.sanitizer.bypassSecurityTrustHtml(this.rawSvg));
      return;
    }
    this.http.post<any>('/api/validate-trace', {
      typeString: this.typeString.trim(),
      trace: methods,
    }).subscribe({
      next: (res) => {
        this.traceResult.set(res);
        this.highlightSvg(res);
      },
      error: () => {
        this.traceResult.set(null);
        this.hasseSvg.set(this.sanitizer.bypassSecurityTrustHtml(this.rawSvg));
      },
    });
  }

  private highlightSvg(result: any): void {
    if (!this.rawSvg || !result?.path) {
      this.hasseSvg.set(this.sanitizer.bypassSecurityTrustHtml(this.rawSvg));
      return;
    }

    const parser = new DOMParser();
    const doc = parser.parseFromString(this.rawSvg, 'image/svg+xml');
    const svg = doc.querySelector('svg');
    if (!svg) {
      this.hasseSvg.set(this.sanitizer.bypassSecurityTrustHtml(this.rawSvg));
      return;
    }

    // Collect visited states and the current state
    const visitedStates = new Set<number>();
    const visitedEdges = new Set<string>(); // "from->to"
    for (const step of result.path) {
      visitedStates.add(step.from);
      if (step.valid) {
        visitedStates.add(step.to);
        visitedEdges.add(`${step.from}->${step.to}`);
      }
    }
    const currentState: number = result.currentState;

    // Highlight nodes: graphviz nodes are <g class="node"> with <title>state_id</title>
    const nodes = svg.querySelectorAll('g.node');
    nodes.forEach(node => {
      const title = node.querySelector('title');
      if (!title) return;
      const stateId = parseInt(title.textContent || '', 10);
      if (isNaN(stateId)) return;

      const ellipse = node.querySelector('ellipse');
      const polygon = node.querySelector('polygon');
      const shape = ellipse || polygon;

      if (stateId === currentState) {
        if (shape) {
          shape.setAttribute('fill', result.complete ? '#d1fae5' : '#e0e7ff');
          shape.setAttribute('stroke', result.complete ? '#059669' : '#4338ca');
          shape.setAttribute('stroke-width', '3');
        }
      } else if (visitedStates.has(stateId)) {
        if (shape) {
          shape.setAttribute('fill', '#f0fdf4');
          shape.setAttribute('stroke', '#86efac');
          shape.setAttribute('stroke-width', '2');
        }
      }
    });

    // Highlight edges: <g class="edge"> with <title>from->to</title>
    const edges = svg.querySelectorAll('g.edge');
    edges.forEach(edge => {
      const title = edge.querySelector('title');
      if (!title) return;
      const edgeKey = (title.textContent || '').replace(/\s/g, '');
      if (visitedEdges.has(edgeKey)) {
        const path = edge.querySelector('path');
        const polyArrow = edge.querySelector('polygon');
        if (path) {
          path.setAttribute('stroke', '#4338ca');
          path.setAttribute('stroke-width', '2.5');
        }
        if (polyArrow) {
          polyArrow.setAttribute('fill', '#4338ca');
          polyArrow.setAttribute('stroke', '#4338ca');
        }
      }
    });

    const serializer = new XMLSerializer();
    const highlighted = serializer.serializeToString(svg);
    this.hasseSvg.set(this.sanitizer.bypassSecurityTrustHtml(highlighted));
  }

  loadExample(ex: { type: string; trace: string }): void {
    this.typeString = ex.type;
    this.traceText = ex.trace;
    this.loadType();
    setTimeout(() => this.onTraceChange(), 500);
  }
}
