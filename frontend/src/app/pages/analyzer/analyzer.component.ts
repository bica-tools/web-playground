import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { ApiService } from '../../services/api.service';
import { AnalyzeResponse, BenchmarkDto, TestGenRequest } from '../../models/api.models';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';
import { HasseDiagramComponent } from '../../components/hasse-diagram/hasse-diagram.component';

interface QuickExample {
  label: string;
  typeString: string;
}

@Component({
  selector: 'app-analyzer',
  standalone: true,
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatSnackBarModule,
    MatTabsModule,
    CodeBlockComponent,
    HasseDiagramComponent,
  ],
  template: `
    <header class="page-header">
      <h1>Interactive Analyzer</h1>
      <p>Parse a session type, build its state space, check lattice properties, and visualize the Hasse diagram.</p>
    </header>

    <!-- Input form (always visible) -->
    <section class="form-section">
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Session type</mat-label>
        <textarea matInput
                  [ngModel]="typeString()"
                  (ngModelChange)="typeString.set($event)"
                  rows="3"
                  placeholder="e.g. rec X . &{read: X, done: end}"
                  (keydown.control.enter)="analyze()"></textarea>
        <mat-hint>Press Ctrl+Enter to analyze</mat-hint>
      </mat-form-field>

      <div class="form-row">
        <mat-form-field appearance="outline" class="benchmark-select">
          <mat-label>Load benchmark</mat-label>
          <mat-select (selectionChange)="onBenchmarkSelect($event.value)">
            <mat-option value="">-- Select --</mat-option>
            @for (b of benchmarks(); track b.name) {
              <mat-option [value]="b.typeString">{{ b.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <button mat-flat-button
                color="primary"
                class="analyze-btn"
                [disabled]="analyzing() || !typeString().trim()"
                (click)="analyze()">
          @if (analyzing()) {
            <mat-spinner diameter="20"></mat-spinner>
          } @else {
            Analyze
          }
        </button>

        <button mat-stroked-button
                class="copy-link-btn"
                (click)="copyLink()"
                [disabled]="!typeString().trim()">
          <mat-icon>link</mat-icon>
          Copy link
        </button>
      </div>
    </section>

    <!-- Error -->
    @if (error()) {
      <section class="error-card">
        <mat-icon>error_outline</mat-icon>
        <span>{{ error() }}</span>
      </section>
    }

    <!-- Quick examples (when no result yet) -->
    @if (!result() && !analyzing()) {
      <section class="quick-examples">
        <p class="quick-examples-label">Quick examples:</p>
        <div class="quick-examples-row">
          @for (ex of quickExamples; track ex.label) {
            <button mat-stroked-button
                    class="quick-example-btn"
                    (click)="loadExample(ex)">
              {{ ex.label }}
            </button>
          }
        </div>
      </section>

      <!-- Grammar reference -->
      <section class="grammar-section">
        <h3>Session Type Grammar</h3>
        <app-code-block [code]="grammarRef" label="Grammar"></app-code-block>
      </section>
    }

    <!-- Results tabs -->
    @if (result()) {
      <section class="results-tabs">
        <app-code-block [code]="result()!.pretty" label="Pretty-printed"></app-code-block>

        <mat-tab-group animationDuration="200ms" class="result-tab-group">

          <!-- Overview tab -->
          <mat-tab label="Overview">
            <div class="tab-content">
              <div class="verdict-grid">
                <div class="verdict-card" [class.pass]="result()!.isLattice" [class.fail]="!result()!.isLattice">
                  <div class="verdict-icon">{{ result()!.isLattice ? '\u2713' : '\u2717' }}</div>
                  <div class="verdict-label">Lattice</div>
                </div>
                <div class="verdict-card" [class.pass]="result()!.terminates" [class.fail]="!result()!.terminates">
                  <div class="verdict-icon">{{ result()!.terminates ? '\u2713' : '\u2717' }}</div>
                  <div class="verdict-label">Terminates</div>
                </div>
                <div class="verdict-card" [class.pass]="result()!.wfParallel" [class.fail]="!result()!.wfParallel">
                  <div class="verdict-icon">{{ result()!.wfParallel ? '\u2713' : '\u2717' }}</div>
                  <div class="verdict-label">WF-Par</div>
                </div>
                @if (result()!.usesParallel) {
                  <div class="verdict-card" [class.pass]="result()!.threadSafe" [class.fail]="!result()!.threadSafe">
                    <div class="verdict-icon">{{ result()!.threadSafe ? '\u2713' : '\u2717' }}</div>
                    <div class="verdict-label">Thread Safe</div>
                  </div>
                }
              </div>

              @if (result()!.counterexample) {
                <p class="counterexample">Counterexample: {{ result()!.counterexample }}</p>
              }

              <h3>Summary</h3>
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="summary-value">{{ result()!.numStates }}</span>
                  <span class="summary-label">States</span>
                </div>
                <div class="summary-item">
                  <span class="summary-value">{{ result()!.numTransitions }}</span>
                  <span class="summary-label">Transitions</span>
                </div>
                <div class="summary-item">
                  <span class="summary-value">{{ result()!.numMethods }}</span>
                  <span class="summary-label">Methods</span>
                </div>
                <div class="summary-item">
                  <span class="summary-value">{{ result()!.numTests }}</span>
                  <span class="summary-label">Test Paths</span>
                </div>
              </div>
            </div>
          </mat-tab>

          <!-- State Space tab -->
          <mat-tab label="State Space">
            <div class="tab-content">
              <h3>Metrics</h3>
              <div class="metrics-grid">
                <table class="metrics-table">
                  <tbody>
                    <tr><td>States</td><td>{{ result()!.numStates }}</td></tr>
                    <tr><td>Transitions</td><td>{{ result()!.numTransitions }}</td></tr>
                    <tr><td>SCCs</td><td>{{ result()!.numSccs }}</td></tr>
                    <tr><td>Methods</td><td>{{ result()!.numMethods }}</td></tr>
                  </tbody>
                </table>
                <table class="metrics-table">
                  <tbody>
                    <tr><td>Uses parallel</td><td>{{ result()!.usesParallel ? 'Yes' : 'No' }}</td></tr>
                    <tr><td>Recursive</td><td>{{ result()!.isRecursive ? 'Yes (depth ' + result()!.recDepth + ')' : 'No' }}</td></tr>
                    <tr><td>Test paths</td><td>{{ result()!.numTests }}</td></tr>
                    <tr>
                      <td>Breakdown</td>
                      <td>{{ result()!.numValidPaths }} valid &middot; {{ result()!.numViolations }} violations &middot; {{ result()!.numIncomplete }} incomplete</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              @if (result()!.methods && result()!.methods.length > 0) {
                <h3>Methods</h3>
                <div class="methods-list">
                  @for (m of result()!.methods; track m) {
                    <span class="method-chip">{{ m }}</span>
                  }
                </div>
              }
            </div>
          </mat-tab>

          <!-- Hasse Diagram tab -->
          <mat-tab label="Hasse Diagram">
            <div class="tab-content">
              @if (result()!.svgHtml) {
                <div class="hasse-container">
                  <app-hasse-diagram [svgHtml]="result()!.svgHtml"></app-hasse-diagram>
                </div>
              } @else {
                <p class="tab-empty">No diagram available for this session type.</p>
              }
            </div>
          </mat-tab>

          <!-- Tests tab -->
          <mat-tab label="Tests">
            <div class="tab-content">
              <div class="test-gen-controls">
                <mat-form-field appearance="outline" class="class-name-input">
                  <mat-label>Class name</mat-label>
                  <input matInput
                         [ngModel]="className()"
                         (ngModelChange)="className.set($event)"
                         placeholder="e.g. FileHandle">
                </mat-form-field>

                <button mat-flat-button
                        color="primary"
                        [disabled]="generatingTests() || !className().trim()"
                        (click)="generateTests()">
                  @if (generatingTests()) {
                    <mat-spinner diameter="18"></mat-spinner>
                  } @else {
                    Generate Tests
                  }
                </button>
              </div>

              @if (result()!.numTests) {
                <p class="test-summary">
                  {{ result()!.numTests }} tests available:
                  {{ result()!.numValidPaths }} valid paths,
                  {{ result()!.numViolations }} violations,
                  {{ result()!.numIncomplete }} incomplete prefixes
                </p>
              }

              @if (testSource()) {
                <app-code-block [code]="testSource()" label="JUnit 5"></app-code-block>
              } @else if (!className().trim()) {
                <p class="tab-empty">Enter a class name above and click "Generate Tests" to produce JUnit 5 test source.</p>
              }
            </div>
          </mat-tab>

          <!-- DOT tab -->
          <mat-tab label="DOT">
            <div class="tab-content">
              @if (result()!.dotSource) {
                <app-code-block [code]="result()!.dotSource" label="DOT"></app-code-block>
              } @else {
                <p class="tab-empty">No DOT source available.</p>
              }
            </div>
          </mat-tab>

        </mat-tab-group>
      </section>
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

    .form-section { margin-bottom: 24px; }
    .full-width { width: 100%; }
    .form-row {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      flex-wrap: wrap;
    }
    .benchmark-select { flex: 1; min-width: 200px; }
    .analyze-btn { height: 56px; min-width: 120px; }
    .copy-link-btn { height: 56px; }

    .error-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      margin: 16px 0;
      border: 2px solid #d32f2f;
      border-radius: 8px;
      background: #fce4ec;
      color: #b71c1c;
    }

    /* Quick examples */
    .quick-examples {
      margin: 24px 0;
      padding: 20px;
      border: 1px dashed rgba(0, 0, 0, 0.15);
      border-radius: 12px;
      text-align: center;
    }
    .quick-examples-label {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.5);
      margin: 0 0 12px;
    }
    .quick-examples-row {
      display: flex;
      gap: 8px;
      justify-content: center;
      flex-wrap: wrap;
    }
    .quick-example-btn {
      font-size: 13px;
      border-color: var(--brand-primary, #4338ca);
      color: var(--brand-primary, #4338ca);
    }

    /* Grammar */
    .grammar-section {
      margin: 24px 0;
    }
    .grammar-section h3 {
      font-size: 15px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.7);
      margin: 0 0 10px;
    }

    /* Results tabs */
    .results-tabs { margin: 24px 0; }

    .result-tab-group {
      margin-top: 16px;
    }

    .tab-content {
      padding: 20px 0;
    }
    .tab-content h3 {
      font-size: 15px;
      font-weight: 600;
      margin: 20px 0 10px;
      color: rgba(0, 0, 0, 0.7);
    }
    .tab-content h3:first-child {
      margin-top: 0;
    }
    .tab-empty {
      color: rgba(0, 0, 0, 0.45);
      font-size: 14px;
      text-align: center;
      padding: 32px 16px;
    }

    /* Verdict grid */
    .verdict-grid {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }
    .verdict-card {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 18px;
      border-radius: 8px;
      font-weight: 500;
      font-size: 14px;
      border: 1px solid;
    }
    .verdict-card.pass {
      background: #ecfdf5;
      border-color: #a7f3d0;
      color: #065f46;
    }
    .verdict-card.fail {
      background: #fef2f2;
      border-color: #fecaca;
      color: #991b1b;
    }
    .verdict-icon { font-size: 18px; }
    .verdict-label { font-size: 13px; }

    .counterexample {
      font-size: 13px;
      color: #991b1b;
      background: #fef2f2;
      padding: 8px 14px;
      border-radius: 6px;
      border: 1px solid #fecaca;
      margin: 12px 0;
    }

    /* Summary grid (overview tab) */
    .summary-grid {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
    }
    .summary-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px 24px;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      min-width: 100px;
    }
    .summary-value {
      font-size: 28px;
      font-weight: 600;
      color: var(--brand-primary, #4338ca);
    }
    .summary-label {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.5);
      margin-top: 4px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    /* Metrics */
    .metrics-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }
    @media (max-width: 600px) {
      .metrics-grid { grid-template-columns: 1fr; }
    }
    .metrics-table {
      width: 100%;
      border-collapse: collapse;
    }
    .metrics-table td {
      padding: 7px 14px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      font-size: 14px;
    }
    .metrics-table td:last-child {
      text-align: right;
      font-weight: 500;
    }

    /* Methods */
    .methods-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
    .method-chip {
      display: inline-block;
      padding: 4px 12px;
      background: #f1f5f9;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 16px;
      font-size: 13px;
      font-family: 'JetBrains Mono', monospace;
      color: rgba(0, 0, 0, 0.7);
    }

    /* Hasse container */
    .hasse-container {
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      background: #fafafa;
      padding: 8px;
    }

    /* Test gen */
    .test-gen-controls {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      flex-wrap: wrap;
    }
    .class-name-input { flex: 1; min-width: 180px; }
    .test-summary {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.6);
      margin: 0 0 12px;
    }
  `],
})
export class AnalyzerComponent implements OnInit {
  readonly typeString = signal('');
  readonly className = signal('');
  readonly benchmarks = signal<BenchmarkDto[]>([]);
  readonly result = signal<AnalyzeResponse | null>(null);
  readonly testSource = signal('');
  readonly error = signal('');
  readonly analyzing = signal(false);
  readonly generatingTests = signal(false);

  readonly quickExamples: QuickExample[] = [
    { label: 'Iterator', typeString: 'rec X . &{hasNext: +{true: &{next: X}, false: end}}' },
    { label: 'SMTP', typeString: '&{ehlo: &{mail: &{rcpt: &{data: &{send: +{ok: end, error: end}}}}}}' },
    { label: 'Two-Buyer', typeString: '&{quote: +{accept: &{deliver: end}, reject: end}}' },
    { label: 'File Handle', typeString: '&{open: +{ok: (rec X . &{read: X, done: end} || rec Y . &{write: Y, done: end}) . &{close: end}, error: end}}' },
    { label: 'Simple', typeString: '&{a: &{b: end, c: end}}' },
  ];

  readonly grammarRef = `S  ::=  &{ m\u2081 : S\u2081 , ... , m\u2099 : S\u2099 }    -- branch (external choice)
     |  +{ l\u2081 : S\u2081 , ... , l\u2099 : S\u2099 }    -- selection (internal choice)
     |  ( S\u2081 || S\u2082 )                    -- parallel
     |  rec X . S                        -- recursion
     |  X                                -- variable
     |  end                              -- terminated
     |  S\u2081 . S\u2082                          -- sequencing`;

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['type']) {
        this.typeString.set(params['type']);
        setTimeout(() => this.analyze(), 0);
      }
    });

    this.api.getBenchmarks().subscribe({
      next: (b) => this.benchmarks.set(b),
    });
  }

  onBenchmarkSelect(value: string): void {
    if (value) {
      this.typeString.set(value);
      const benchmark = this.benchmarks().find(b => b.typeString === value);
      if (benchmark) {
        this.className.set(this.toClassName(benchmark.name));
      }
      this.analyze();
    }
  }

  loadExample(example: QuickExample): void {
    this.typeString.set(example.typeString);
    this.className.set(this.toClassName(example.label));
    this.analyze();
  }

  private toClassName(name: string): string {
    return name.replace(/[^a-zA-Z0-9]/g, '') + 'Test';
  }

  analyze(): void {
    if (!this.typeString().trim()) return;
    this.analyzing.set(true);
    this.result.set(null);
    this.testSource.set('');
    this.error.set('');

    this.api.analyze(this.typeString()).subscribe({
      next: (res) => {
        this.result.set(res);
        this.analyzing.set(false);
        if (this.className().trim()) {
          this.generateTests();
        }
      },
      error: (err) => {
        this.error.set(err.error?.error || err.message || 'Analysis failed');
        this.analyzing.set(false);
      },
    });
  }

  generateTests(): void {
    if (!this.typeString().trim() || !this.className().trim()) return;
    this.generatingTests.set(true);

    const request: TestGenRequest = {
      typeString: this.typeString(),
      className: this.className(),
    };

    this.api.generateTests(request).subscribe({
      next: (res) => {
        this.testSource.set(res.testSource);
        this.generatingTests.set(false);
      },
      error: (err) => {
        this.snackBar.open(
          err.error?.error || 'Test generation failed',
          'Dismiss',
          { duration: 5000 },
        );
        this.generatingTests.set(false);
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
