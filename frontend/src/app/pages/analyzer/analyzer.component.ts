import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ApiService } from '../../services/api.service';
import { AnalyzeResponse } from '../../models/api.models';

interface QuickExample {
  label: string;
  typeString: string;
}

@Component({
  selector: 'app-analyzer',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="analyzer-layout">

      <!-- ════════ LEFT PANE: Input (sticky) ════════ -->
      <div class="left-pane">
        <h1 class="pane-title">Analyzer</h1>
        <p class="pane-subtitle">Type a session type. See the lattice.</p>

        <label class="input-label">Session type</label>
        <textarea
          class="type-input"
          [ngModel]="typeString()"
          (ngModelChange)="typeString.set($event)"
          (keydown.control.enter)="analyze()"
          placeholder="rec X . &{read: X, close: end}"
          spellcheck="false"
        ></textarea>
        <span class="input-hint">Ctrl+Enter to analyze</span>

        <div class="action-row">
          <button class="analyze-btn"
                  [disabled]="analyzing() || !typeString().trim()"
                  (click)="analyze()">
            @if (analyzing()) {
              <mat-spinner diameter="18" class="analyze-spinner"></mat-spinner>
            } @else {
              Analyze
            }
          </button>
          <button class="copy-btn"
                  (click)="copyLink()"
                  [disabled]="!typeString().trim()"
                  title="Copy shareable link">
            <mat-icon>link</mat-icon>
          </button>
        </div>

        <!-- Examples -->
        <div class="examples-section">
          <div class="examples-label">Examples</div>
          <div class="examples-grid">
            @for (ex of quickExamples; track ex.label) {
              <button class="example-chip" (click)="loadExample(ex)">{{ ex.label }}</button>
            }
          </div>
        </div>

        <!-- Grammar toggle -->
        <button class="grammar-toggle" (click)="showGrammar.set(!showGrammar())">
          {{ showGrammar() ? 'Hide' : 'Show' }} grammar reference
        </button>
        @if (showGrammar()) {
          <div class="grammar-block">{{ grammarRef }}</div>
        }
      </div>

      <!-- ════════ RIGHT PANE: Results ════════ -->
      <div class="right-pane">

        <!-- Empty state -->
        @if (!result() && !analyzing() && !error()) {
          <div class="empty-state">
            <div class="empty-icon">&#x25C7;</div>
            <p class="empty-text">Type a session type and click Analyze</p>
            <p class="empty-hint">or click an example on the left</p>
          </div>
        }

        <!-- Loading -->
        @if (analyzing()) {
          <div class="empty-state">
            <mat-spinner diameter="40"></mat-spinner>
          </div>
        }

        <!-- Error -->
        @if (error()) {
          <div class="error-banner">
            <mat-icon>error_outline</mat-icon>
            {{ error() }}
          </div>
        }

        <!-- Results -->
        @if (result()) {
          <!-- Header badges -->
          <div class="result-header">
            <span class="result-badge" [class.badge-lattice]="result()!.isLattice" [class.badge-not-lattice]="!result()!.isLattice">
              {{ result()!.isLattice ? 'Lattice' : 'Not a lattice' }}
            </span>
            <span class="result-stat">{{ result()!.numStates }} states</span>
            <span class="result-stat">{{ result()!.numTransitions }} transitions</span>
            <span class="result-stat">{{ result()!.numTests }} tests</span>
            @if (result()!.usesParallel) {
              <span class="result-stat parallel-tag">&#x2225; parallel</span>
            }
          </div>

          <!-- Pretty-printed -->
          <div class="pretty-block">{{ result()!.pretty }}</div>

          <!-- Hasse diagram (front and center) -->
          @if (result()!.svgHtml) {
            <div class="hasse-section" [innerHTML]="safeSvg()"></div>
          }

          <!-- Verdict chips -->
          <div class="verdict-row">
            <span class="verdict-chip" [class.verdict-pass]="result()!.isLattice" [class.verdict-fail]="!result()!.isLattice">
              {{ result()!.isLattice ? '\u2713' : '\u2717' }} Lattice
            </span>
            <span class="verdict-chip" [class.verdict-pass]="result()!.terminates" [class.verdict-fail]="!result()!.terminates">
              {{ result()!.terminates ? '\u2713' : '\u2717' }} Terminates
            </span>
            <span class="verdict-chip" [class.verdict-pass]="result()!.wfParallel" [class.verdict-fail]="!result()!.wfParallel">
              {{ result()!.wfParallel ? '\u2713' : '\u2717' }} WF-Par
            </span>
            @if (result()!.usesParallel) {
              <span class="verdict-chip" [class.verdict-pass]="result()!.threadSafe" [class.verdict-fail]="!result()!.threadSafe">
                {{ result()!.threadSafe ? '\u2713' : '\u2717' }} Thread Safe
              </span>
            }
          </div>

          @if (result()!.counterexample) {
            <div class="counterexample-text">Counterexample: {{ result()!.counterexample }}</div>
          }

          <!-- Details grid -->
          <div class="details-grid">
            <div class="detail-card">
              <div class="detail-label">States</div>
              <div class="detail-value">{{ result()!.numStates }}</div>
              <div class="detail-sub">{{ result()!.numSccs }} SCCs</div>
            </div>
            <div class="detail-card">
              <div class="detail-label">Transitions</div>
              <div class="detail-value">{{ result()!.numTransitions }}</div>
              <div class="detail-sub">{{ result()!.numMethods }} methods</div>
            </div>
            <div class="detail-card">
              <div class="detail-label">Test Paths</div>
              <div class="detail-value">{{ result()!.numTests }}</div>
              <div class="detail-sub">{{ result()!.numValidPaths }} valid &middot; {{ result()!.numViolations }} violations</div>
            </div>
            <div class="detail-card">
              <div class="detail-label">Properties</div>
              <div class="detail-value" style="font-size:14px">
                {{ result()!.isRecursive ? 'Recursive (depth ' + result()!.recDepth + ')' : 'Non-recursive' }}
              </div>
              <div class="detail-sub">{{ result()!.numIncomplete }} incomplete prefixes</div>
            </div>
          </div>

          <!-- Methods -->
          @if (result()!.methods && result()!.methods.length > 0) {
            <div class="methods-section">
              <div class="methods-label">Methods</div>
              <div class="methods-list">
                @for (m of result()!.methods; track m) {
                  <span class="method-chip">{{ m }}</span>
                }
              </div>
            </div>
          }

          <!-- DOT source toggle -->
          @if (result()!.dotSource) {
            <button class="dot-toggle" (click)="showDot.set(!showDot())">
              {{ showDot() ? 'Hide' : 'Show' }} DOT source
            </button>
            @if (showDot()) {
              <div class="dot-block">{{ result()!.dotSource }}</div>
            }
          }
        }
      </div>
    </div>
  `,
  styleUrl: './analyzer.component.scss',
})
export class AnalyzerComponent implements OnInit {
  readonly typeString = signal('');
  readonly result = signal<AnalyzeResponse | null>(null);
  readonly error = signal('');
  readonly analyzing = signal(false);
  readonly showGrammar = signal(false);
  readonly showDot = signal(false);
  readonly safeSvg = signal<SafeHtml>('');

  readonly quickExamples: QuickExample[] = [
    { label: 'Iterator', typeString: 'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}' },
    { label: 'SMTP', typeString: 'connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}' },
    { label: 'Two-Buyer', typeString: 'lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})' },
    { label: 'File Handle', typeString: '&{open: +{ok: (rec X . &{read: X, done: end} || rec Y . &{write: Y, done: end}) . &{close: end}, error: end}}' },
    { label: 'MCP', typeString: 'initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})' },
    { label: 'Simple', typeString: '&{a: &{b: end, c: end}}' },
  ];

  readonly grammarRef = `S  ::=  &{ m\u2081 : S\u2081 , ... , m\u2099 : S\u2099 }   branch
     |  +{ l\u2081 : S\u2081 , ... , l\u2099 : S\u2099 }   selection
     |  ( S\u2081 || S\u2082 )                   parallel
     |  rec X . S                       recursion
     |  X                               variable
     |  end                             terminated
     |  S\u2081 . S\u2082                         sequencing`;

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['type']) {
        this.typeString.set(params['type']);
        setTimeout(() => this.analyze(), 0);
      }
    });
  }

  loadExample(example: QuickExample): void {
    this.typeString.set(example.typeString);
    this.analyze();
  }

  analyze(): void {
    if (!this.typeString().trim()) return;
    this.analyzing.set(true);
    this.result.set(null);
    this.error.set('');
    this.showDot.set(false);

    this.api.analyze(this.typeString()).subscribe({
      next: (res) => {
        this.result.set(res);
        this.safeSvg.set(this.sanitizer.bypassSecurityTrustHtml(res.svgHtml));
        this.analyzing.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || err.message || 'Analysis failed');
        this.analyzing.set(false);
      },
    });
  }

  copyLink(): void {
    const url = new URL(window.location.href);
    url.pathname = '/tools/analyzer';
    url.searchParams.set('type', this.typeString());
    navigator.clipboard.writeText(url.toString()).then(() => {
      this.snackBar.open('Link copied to clipboard', '', { duration: 2000 });
    });
  }
}
