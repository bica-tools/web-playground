import { Component, OnInit, ViewChild, ElementRef, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../services/api.service';
import { AnalyzeResponse, BenchmarkDto, TestGenRequest } from '../../models/api.models';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';
import { HasseDiagramComponent } from '../../components/hasse-diagram/hasse-diagram.component';

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
    MatExpansionModule,
    MatIconModule,
    MatChipsModule,
    MatSnackBarModule,
    CodeBlockComponent,
    HasseDiagramComponent,
  ],
  template: `
    <header class="page-header">
      <h1>Interactive Analyzer</h1>
      <p>Parse a session type, build its state space, check lattice properties, and visualize the Hasse diagram.</p>
    </header>

    <!-- Form -->
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

        <mat-form-field appearance="outline" class="class-name-input">
          <mat-label>Class name (for test gen)</mat-label>
          <input matInput [ngModel]="className()" (ngModelChange)="className.set($event)" placeholder="e.g. FileHandle">
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
      </div>
    </section>

    <!-- Error -->
    @if (error()) {
      <section class="error-card">
        <mat-icon>error_outline</mat-icon>
        <span>{{ error() }}</span>
      </section>
    }

    <!-- Results -->
    @if (result()) {
      <section class="results" #resultsSection>

        <!-- Pretty-printed type -->
        <h2>Session Type</h2>
        <app-code-block [code]="result()!.pretty" label="Pretty-printed"></app-code-block>

        <!-- Verdict summary cards -->
        <div class="verdict-grid">
          <div class="verdict-card" [class.pass]="result()!.isLattice" [class.fail]="!result()!.isLattice">
            <div class="verdict-icon">{{ result()!.isLattice ? '&#x2713;' : '&#x2717;' }}</div>
            <div class="verdict-label">Lattice</div>
          </div>
          <div class="verdict-card" [class.pass]="result()!.terminates" [class.fail]="!result()!.terminates">
            <div class="verdict-icon">{{ result()!.terminates ? '&#x2713;' : '&#x2717;' }}</div>
            <div class="verdict-label">Terminates</div>
          </div>
          <div class="verdict-card" [class.pass]="result()!.wfParallel" [class.fail]="!result()!.wfParallel">
            <div class="verdict-icon">{{ result()!.wfParallel ? '&#x2713;' : '&#x2717;' }}</div>
            <div class="verdict-label">WF-Par</div>
          </div>
          @if (result()!.usesParallel) {
            <div class="verdict-card" [class.pass]="result()!.threadSafe" [class.fail]="!result()!.threadSafe">
              <div class="verdict-icon">{{ result()!.threadSafe ? '&#x2713;' : '&#x2717;' }}</div>
              <div class="verdict-label">Thread Safe</div>
            </div>
          }
        </div>

        @if (result()!.counterexample) {
          <p class="counterexample">Counterexample: {{ result()!.counterexample }}</p>
        }

        <!-- Metrics table -->
        <h3>State Space Metrics</h3>
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
                <td>{{ result()!.numValidPaths }}v / {{ result()!.numViolations }}x / {{ result()!.numIncomplete }}i</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Methods -->
        @if (result()!.methods && result()!.methods.length > 0) {
          <h3>Methods</h3>
          <div class="methods-list">
            @for (m of result()!.methods; track m) {
              <span class="method-chip">{{ m }}</span>
            }
          </div>
        }

        <!-- Hasse diagram -->
        @if (result()!.svgHtml) {
          <h3>Hasse Diagram</h3>
          <app-hasse-diagram [svgHtml]="result()!.svgHtml"></app-hasse-diagram>
        }

        <!-- DOT source -->
        @if (result()!.dotSource) {
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>DOT Source</mat-panel-title>
            </mat-expansion-panel-header>
            <app-code-block [code]="result()!.dotSource" label="DOT"></app-code-block>
          </mat-expansion-panel>
        }

        <!-- Test generation -->
        @if (className().trim()) {
          <mat-expansion-panel class="test-gen-panel">
            <mat-expansion-panel-header>
              <mat-panel-title>
                Test Generation
                @if (result()!.numTests) {
                  <span class="test-count-badge">{{ result()!.numTests }} tests</span>
                }
              </mat-panel-title>
            </mat-expansion-panel-header>
            @if (testSource()) {
              <app-code-block [code]="testSource()" label="JUnit 5"></app-code-block>
            } @else {
              <div class="test-gen-form">
                <button mat-stroked-button
                        [disabled]="generatingTests()"
                        (click)="generateTests()">
                  @if (generatingTests()) {
                    <mat-spinner diameter="18"></mat-spinner>
                  } @else {
                    Generate Tests
                  }
                </button>
              </div>
            }
          </mat-expansion-panel>
        }
      </section>
    }

    <!-- Grammar reference -->
    <mat-expansion-panel class="grammar-panel">
      <mat-expansion-panel-header>
        <mat-panel-title>Session Type Grammar Reference</mat-panel-title>
      </mat-expansion-panel-header>
      <app-code-block [code]="grammarRef" label="Grammar"></app-code-block>
    </mat-expansion-panel>
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
      gap: 16px;
      align-items: flex-start;
      flex-wrap: wrap;
    }
    .benchmark-select { flex: 1; min-width: 200px; }
    .class-name-input { flex: 1; min-width: 180px; }
    .analyze-btn { height: 56px; min-width: 120px; }

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

    /* Results */
    .results { margin: 24px 0; }
    .results h2 {
      font-size: 20px;
      font-weight: 600;
      margin-bottom: 12px;
    }
    .results h3 {
      font-size: 15px;
      font-weight: 600;
      margin: 24px 0 10px;
      color: rgba(0,0,0,0.7);
    }

    /* Verdict grid */
    .verdict-grid {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      margin: 20px 0 12px;
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
      margin: 8px 0;
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
      border: 1px solid rgba(0,0,0,0.08);
      border-radius: 16px;
      font-size: 13px;
      font-family: 'JetBrains Mono', monospace;
      color: rgba(0,0,0,0.7);
    }

    /* Test gen */
    .test-gen-panel { margin-top: 16px; }
    .test-gen-form {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .test-count-badge {
      font-size: 11px;
      background: var(--brand-primary-light, #6366f1);
      color: #fff;
      padding: 2px 8px;
      border-radius: 10px;
      margin-left: 8px;
      font-weight: 500;
    }

    .grammar-panel { margin: 32px 0; }
  `],
})
export class AnalyzerComponent implements OnInit {
  @ViewChild('resultsSection') resultsSection?: ElementRef;

  readonly typeString = signal('');
  readonly className = signal('');
  readonly benchmarks = signal<BenchmarkDto[]>([]);
  readonly result = signal<AnalyzeResponse | null>(null);
  readonly testSource = signal('');
  readonly error = signal('');
  readonly analyzing = signal(false);
  readonly generatingTests = signal(false);

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
    }
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
        setTimeout(() => {
          this.resultsSection?.nativeElement.scrollIntoView({
            behavior: 'smooth',
          });
        }, 100);
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
}
