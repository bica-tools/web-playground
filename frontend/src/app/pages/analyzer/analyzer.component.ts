import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
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
                  [(ngModel)]="typeString"
                  rows="3"
                  placeholder="e.g. rec X . &{read: X, done: end}"></textarea>
      </mat-form-field>

      <div class="form-row">
        <mat-form-field appearance="outline" class="benchmark-select">
          <mat-label>Load benchmark</mat-label>
          <mat-select (selectionChange)="onBenchmarkSelect($event.value)">
            <mat-option value="">-- Select --</mat-option>
            @for (b of benchmarks; track b.name) {
              <mat-option [value]="b.typeString">{{ b.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="class-name-input">
          <mat-label>Class name (for test generation)</mat-label>
          <input matInput [(ngModel)]="className" placeholder="e.g. FileHandle">
        </mat-form-field>

        <button mat-flat-button
                color="primary"
                class="analyze-btn"
                [disabled]="analyzing || !typeString.trim()"
                (click)="analyze()">
          @if (analyzing) {
            <mat-spinner diameter="20"></mat-spinner>
          } @else {
            Analyze
          }
        </button>
      </div>
    </section>

    <!-- Error -->
    @if (error) {
      <section class="error-card">
        <mat-icon>error_outline</mat-icon>
        <span>{{ error }}</span>
      </section>
    }

    <!-- Results -->
    @if (result) {
      <section class="results">
        <h2>Analysis Result</h2>

        <!-- Pretty-printed type -->
        <h3>Session Type</h3>
        <app-code-block [code]="result.pretty" label="Pretty-printed"></app-code-block>

        <!-- Metrics -->
        <h3>State Space</h3>
        <table class="metrics-table">
          <tbody>
            <tr><td>States</td><td>{{ result.numStates }}</td></tr>
            <tr><td>Transitions</td><td>{{ result.numTransitions }}</td></tr>
            <tr><td>SCCs (after quotienting)</td><td>{{ result.numSccs }}</td></tr>
          </tbody>
        </table>

        <!-- Lattice verdict -->
        <h3>Lattice Check</h3>
        @if (result.isLattice) {
          <p class="verdict pass">&#x2713; The state space IS a lattice</p>
        } @else {
          <p class="verdict fail">&#x2717; The state space is NOT a lattice</p>
        }
        @if (result.counterexample) {
          <p class="verdict fail">Counterexample: {{ result.counterexample }}</p>
        }

        <!-- Termination -->
        <h3>Termination</h3>
        @if (result.terminates) {
          <p class="verdict pass">&#x2713; All recursive branches terminate</p>
        } @else {
          <p class="verdict fail">&#x2717; Non-terminating recursive branches detected</p>
        }

        <!-- WF-Par -->
        <h3>Well-Formed Parallel</h3>
        @if (result.wfParallel) {
          <p class="verdict pass">&#x2713; Parallel composition is well-formed</p>
        } @else {
          <p class="verdict fail">&#x2717; WF-Par violations detected</p>
        }

        <!-- Hasse diagram -->
        @if (result.svgHtml) {
          <h3>Hasse Diagram</h3>
          <app-hasse-diagram [svgHtml]="result.svgHtml"></app-hasse-diagram>
        }

        <!-- DOT source -->
        @if (result.dotSource) {
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>DOT Source</mat-panel-title>
            </mat-expansion-panel-header>
            <app-code-block [code]="result.dotSource" label="DOT"></app-code-block>
          </mat-expansion-panel>
        }

        <!-- Test generation -->
        <mat-expansion-panel class="test-gen-panel">
          <mat-expansion-panel-header>
            <mat-panel-title>Test Generation</mat-panel-title>
          </mat-expansion-panel-header>
          @if (testSource) {
            <app-code-block [code]="testSource" label="JUnit 5"></app-code-block>
          } @else {
            <div class="test-gen-form">
              @if (!className.trim()) {
                <p>Enter a class name above, then click Generate.</p>
              }
              <button mat-stroked-button
                      [disabled]="generatingTests || !className.trim()"
                      (click)="generateTests()">
                @if (generatingTests) {
                  <mat-spinner diameter="18"></mat-spinner>
                } @else {
                  Generate Tests
                }
              </button>
            </div>
          }
        </mat-expansion-panel>
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
      font-weight: 500;
      margin: 0 0 8px;
    }
    .page-header p {
      color: rgba(0, 0, 0, 0.6);
      margin: 0;
    }

    .form-section {
      margin-bottom: 24px;
    }
    .full-width {
      width: 100%;
    }
    .form-row {
      display: flex;
      gap: 16px;
      align-items: flex-start;
      flex-wrap: wrap;
    }
    .benchmark-select {
      flex: 1;
      min-width: 200px;
    }
    .class-name-input {
      flex: 1;
      min-width: 180px;
    }
    .analyze-btn {
      height: 56px;
      min-width: 120px;
    }

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

    .results {
      margin: 24px 0;
    }
    .results h2 {
      font-size: 22px;
      font-weight: 500;
      margin-bottom: 16px;
    }
    .results h3 {
      font-size: 16px;
      font-weight: 500;
      margin: 20px 0 8px;
    }

    .metrics-table {
      width: 100%;
      max-width: 400px;
      border-collapse: collapse;
    }
    .metrics-table td {
      padding: 8px 16px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    }
    .metrics-table td:last-child {
      text-align: right;
      font-weight: 500;
    }

    .verdict {
      font-weight: 500;
      padding: 8px 16px;
      border-radius: 4px;
      margin: 8px 0;
    }
    .verdict.pass {
      background: #e8f5e9;
      color: #2e7d32;
    }
    .verdict.fail {
      background: #fce4ec;
      color: #c62828;
    }

    .test-gen-panel {
      margin-top: 16px;
    }
    .test-gen-form {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .test-gen-form p {
      color: rgba(0, 0, 0, 0.6);
      font-size: 14px;
      margin: 0;
    }

    .grammar-panel {
      margin: 32px 0;
    }
  `],
})
export class AnalyzerComponent implements OnInit {
  typeString = '';
  className = '';
  benchmarks: BenchmarkDto[] = [];
  result: AnalyzeResponse | null = null;
  testSource = '';
  error = '';
  analyzing = false;
  generatingTests = false;

  readonly grammarRef = `S  ::=  &{ m₁ : S₁ , ... , mₙ : Sₙ }    -- branch (external choice)
     |  +{ l₁ : S₁ , ... , lₙ : Sₙ }    -- selection (internal choice)
     |  ( S₁ || S₂ )                    -- parallel
     |  rec X . S                        -- recursion
     |  X                                -- variable
     |  end                              -- terminated
     |  S₁ . S₂                          -- sequencing`;

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    // Pre-fill from URL param
    this.route.queryParams.subscribe((params) => {
      if (params['type']) {
        this.typeString = params['type'];
      }
    });

    // Load benchmarks for dropdown
    this.api.getBenchmarks().subscribe({
      next: (b) => (this.benchmarks = b),
    });
  }

  onBenchmarkSelect(value: string): void {
    if (value) {
      this.typeString = value;
    }
  }

  analyze(): void {
    if (!this.typeString.trim()) return;
    this.analyzing = true;
    this.result = null;
    this.testSource = '';
    this.error = '';

    this.api.analyze(this.typeString).subscribe({
      next: (res) => {
        this.result = res;
        this.analyzing = false;
      },
      error: (err) => {
        this.error = err.error?.error || err.message || 'Analysis failed';
        this.analyzing = false;
      },
    });
  }

  generateTests(): void {
    if (!this.typeString.trim() || !this.className.trim()) return;
    this.generatingTests = true;

    const request: TestGenRequest = {
      typeString: this.typeString,
      className: this.className,
    };

    this.api.generateTests(request).subscribe({
      next: (res) => {
        this.testSource = res.testSource;
        this.generatingTests = false;
      },
      error: (err) => {
        this.snackBar.open(
          err.error?.error || 'Test generation failed',
          'Dismiss',
          { duration: 5000 },
        );
        this.generatingTests = false;
      },
    });
  }
}
