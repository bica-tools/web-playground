import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../services/api.service';
import { BenchmarkDto, TestGenRequest } from '../../models/api.models';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';

@Component({
  selector: 'app-test-generator',
  standalone: true,
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatSnackBarModule,
    CodeBlockComponent,
  ],
  template: `
    <header class="page-header">
      <h1>Test Generator</h1>
      <p>Generate JUnit 5 tests from session type definitions: valid paths, protocol violations, and incomplete prefixes.</p>
    </header>

    <!-- Input form -->
    <section class="form-section">
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Session type</mat-label>
        <textarea matInput
                  [ngModel]="typeString()"
                  (ngModelChange)="typeString.set($event)"
                  rows="3"
                  placeholder="e.g. rec X . &{read: X, done: end}"></textarea>
      </mat-form-field>

      <div class="form-row">
        <mat-form-field appearance="outline" class="benchmark-select">
          <mat-label>Load benchmark</mat-label>
          <mat-select (selectionChange)="onBenchmarkSelect($event.value)">
            <mat-option value="">-- Select --</mat-option>
            @for (b of benchmarks(); track b.name) {
              <mat-option [value]="b.name">{{ b.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="class-name-input">
          <mat-label>Class name</mat-label>
          <input matInput
                 [ngModel]="className()"
                 (ngModelChange)="className.set($event)"
                 placeholder="e.g. FileHandle">
        </mat-form-field>
      </div>

      <div class="form-row">
        <button mat-flat-button
                color="primary"
                class="generate-btn"
                [disabled]="generating() || !typeString().trim() || !className().trim()"
                (click)="generate()">
          @if (generating()) {
            <mat-spinner diameter="20"></mat-spinner>
          } @else {
            Generate Tests
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

    <!-- Help text (when no result yet) -->
    @if (!testSource() && !generating() && !error()) {
      <section class="help-section">
        <h3>How it works</h3>
        <div class="help-grid">
          <div class="help-card">
            <div class="help-number">1</div>
            <div>
              <strong>Valid paths</strong>
              <p>Complete execution traces from initial state to end, exercising all reachable branches.</p>
            </div>
          </div>
          <div class="help-card">
            <div class="help-number">2</div>
            <div>
              <strong>Violations</strong>
              <p>Attempts to call methods that are not enabled in a given state &mdash; tests that the object rejects invalid operations.</p>
            </div>
          </div>
          <div class="help-card">
            <div class="help-number">3</div>
            <div>
              <strong>Incomplete prefixes</strong>
              <p>Partial executions that stop before reaching end &mdash; tests for detecting abandoned sessions.</p>
            </div>
          </div>
        </div>
      </section>
    }

    <!-- Result -->
    @if (testSource()) {
      <section class="result-section">
        <div class="result-header">
          <h3>Generated JUnit 5 Tests</h3>
          <span class="result-meta">Class: {{ className() }}ProtocolTest</span>
        </div>
        <app-code-block [code]="testSource()" label="JUnit 5"></app-code-block>
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
    .class-name-input { flex: 1; min-width: 180px; }
    .generate-btn { height: 56px; min-width: 160px; }

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

    /* Help section */
    .help-section {
      margin: 24px 0;
    }
    .help-section h3 {
      font-size: 15px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.7);
      margin: 0 0 16px;
    }
    .help-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }
    .help-card {
      display: flex;
      gap: 14px;
      padding: 18px;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 10px;
      background: #fafafa;
    }
    .help-number {
      flex-shrink: 0;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--brand-primary, #4338ca);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 14px;
    }
    .help-card strong {
      display: block;
      margin-bottom: 4px;
      font-size: 14px;
    }
    .help-card p {
      margin: 0;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.6);
      line-height: 1.5;
    }

    /* Result */
    .result-section { margin: 24px 0; }
    .result-header {
      display: flex;
      align-items: baseline;
      gap: 16px;
      margin-bottom: 12px;
    }
    .result-header h3 {
      font-size: 15px;
      font-weight: 600;
      margin: 0;
      color: rgba(0, 0, 0, 0.7);
    }
    .result-meta {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.45);
      font-family: 'JetBrains Mono', monospace;
    }
  `],
})
export class TestGeneratorComponent implements OnInit {
  readonly typeString = signal('');
  readonly className = signal('');
  readonly benchmarks = signal<BenchmarkDto[]>([]);
  readonly testSource = signal('');
  readonly error = signal('');
  readonly generating = signal(false);

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['type']) {
        this.typeString.set(params['type']);
      }
      if (params['class']) {
        this.className.set(params['class']);
      }
    });

    this.api.getBenchmarks().subscribe({
      next: (b) => this.benchmarks.set(b),
    });
  }

  onBenchmarkSelect(name: string): void {
    if (!name) return;
    const benchmark = this.benchmarks().find(b => b.name === name);
    if (benchmark) {
      this.typeString.set(benchmark.typeString);
      this.className.set(this.toClassName(benchmark.name));
    }
  }

  private toClassName(name: string): string {
    return name.replace(/[^a-zA-Z0-9]/g, '');
  }

  generate(): void {
    if (!this.typeString().trim() || !this.className().trim()) return;
    this.generating.set(true);
    this.testSource.set('');
    this.error.set('');

    const request: TestGenRequest = {
      typeString: this.typeString(),
      className: this.className(),
    };

    this.api.generateTests(request).subscribe({
      next: (res) => {
        this.testSource.set(res.testSource);
        this.generating.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || err.message || 'Test generation failed');
        this.generating.set(false);
      },
    });
  }
}
