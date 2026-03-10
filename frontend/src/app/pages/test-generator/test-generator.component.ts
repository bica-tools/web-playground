import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../services/api.service';
import { TestGenRequest, CoverageFrameDto } from '../../models/api.models';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';
import { HasseDiagramComponent } from '../../components/hasse-diagram/hasse-diagram.component';

@Component({
  selector: 'app-test-generator',
  standalone: true,
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTabsModule,
    MatSnackBarModule,
    CodeBlockComponent,
    HasseDiagramComponent,
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

    <!-- Result tabs -->
    @if (testSource() || coverageFrames().length > 0) {
      <mat-tab-group class="result-tabs" animationDuration="200ms">
        @if (testSource()) {
          <mat-tab label="Generated Tests">
            <section class="result-section">
              <div class="result-header">
                <h3>JUnit 5 Tests</h3>
                <span class="result-meta">Class: {{ className() }}ProtocolTest</span>
              </div>
              <app-code-block [code]="testSource()" label="JUnit 5"></app-code-block>
            </section>
          </mat-tab>
        }
        <mat-tab>
          <ng-template mat-tab-label>
            Coverage Storyboard
            @if (loadingCoverage()) {
              <mat-spinner diameter="16" class="tab-spinner"></mat-spinner>
            }
          </ng-template>
          <section class="result-section">
            @if (coverageFrames().length === 0 && !loadingCoverage()) {
              <div class="coverage-prompt">
                <button mat-flat-button color="primary"
                        [disabled]="loadingCoverage() || !typeString().trim()"
                        (click)="loadCoverage()">
                  Generate Coverage Storyboard
                </button>
                <p>Visualise how each test covers the state space — green edges/states are exercised, gray are not.</p>
              </div>
            }
            @if (coverageFrames().length > 0) {
              <div class="coverage-stats">
                <span class="stat-chip">{{ coverageTotalTransitions() }} transitions</span>
                <span class="stat-chip">{{ coverageTotalStates() }} states</span>
                <span class="stat-chip">{{ coverageFrames().length }} frames</span>
              </div>
              <div class="coverage-controls">
                <button mat-icon-button [disabled]="currentFrame() === 0" (click)="prevFrame()">
                  <mat-icon>chevron_left</mat-icon>
                </button>
                <span class="frame-label">
                  {{ currentFrame() + 1 }} / {{ coverageFrames().length }}
                  &mdash; {{ coverageFrames()[currentFrame()].testName }}
                </span>
                <button mat-icon-button [disabled]="currentFrame() >= coverageFrames().length - 1" (click)="nextFrame()">
                  <mat-icon>chevron_right</mat-icon>
                </button>
              </div>
              <div class="frame-meta">
                <span class="kind-chip" [attr.data-kind]="coverageFrames()[currentFrame()].testKind">
                  {{ coverageFrames()[currentFrame()].testKind }}
                </span>
                <span>Transition coverage: {{ (coverageFrames()[currentFrame()].transitionCoverage * 100).toFixed(1) }}%</span>
                <span>State coverage: {{ (coverageFrames()[currentFrame()].stateCoverage * 100).toFixed(1) }}%</span>
              </div>
              <app-hasse-diagram [svgHtml]="coverageFrames()[currentFrame()].svgHtml"></app-hasse-diagram>
            }
          </section>
        </mat-tab>
      </mat-tab-group>
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
    .result-tabs { margin-top: 16px; }
    .result-section { padding: 16px 0; }
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
    .tab-spinner { display: inline-block; margin-left: 8px; }

    /* Coverage */
    .coverage-prompt {
      text-align: center;
      padding: 32px 0;
    }
    .coverage-prompt p {
      margin: 12px 0 0;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.5);
    }
    .coverage-stats {
      display: flex;
      gap: 10px;
      justify-content: center;
      padding: 8px 0 16px;
    }
    .stat-chip {
      display: inline-block;
      padding: 4px 12px;
      background: #f1f5f9;
      border: 1px solid rgba(0,0,0,0.06);
      border-radius: 16px;
      font-size: 12px;
      color: rgba(0,0,0,0.7);
    }
    .coverage-controls {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin-bottom: 8px;
    }
    .frame-label {
      font-size: 13px;
      font-weight: 500;
      min-width: 200px;
      text-align: center;
    }
    .frame-meta {
      display: flex;
      gap: 16px;
      justify-content: center;
      font-size: 13px;
      color: rgba(0,0,0,0.6);
      margin-bottom: 8px;
    }
    .kind-chip {
      padding: 2px 10px;
      border-radius: 10px;
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.3px;
    }
    .kind-chip[data-kind="valid"] { background: #dcfce7; color: #166534; }
    .kind-chip[data-kind="violation"] { background: #fee2e2; color: #991b1b; }
    .kind-chip[data-kind="incomplete"] { background: #fef3c7; color: #92400e; }
  `],
})
export class TestGeneratorComponent implements OnInit {
  readonly typeString = signal('');
  readonly className = signal('');
  readonly testSource = signal('');
  readonly error = signal('');
  readonly generating = signal(false);

  // Coverage storyboard
  readonly coverageFrames = signal<CoverageFrameDto[]>([]);
  readonly coverageTotalTransitions = signal(0);
  readonly coverageTotalStates = signal(0);
  readonly loadingCoverage = signal(false);
  readonly currentFrame = signal(0);

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
  }

  generate(): void {
    if (!this.typeString().trim() || !this.className().trim()) return;
    this.generating.set(true);
    this.testSource.set('');
    this.error.set('');
    this.coverageFrames.set([]);

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

  loadCoverage(): void {
    if (!this.typeString().trim()) return;
    this.loadingCoverage.set(true);
    this.currentFrame.set(0);

    this.api.coverageStoryboard(this.typeString()).subscribe({
      next: (res) => {
        this.coverageFrames.set(res.frames);
        this.coverageTotalTransitions.set(res.totalTransitions);
        this.coverageTotalStates.set(res.totalStates);
        this.loadingCoverage.set(false);
      },
      error: (err) => {
        this.snackBar.open(
          err.error?.error || 'Coverage storyboard failed',
          'Close',
          { duration: 5000 },
        );
        this.loadingCoverage.set(false);
      },
    });
  }

  prevFrame(): void {
    if (this.currentFrame() > 0) {
      this.currentFrame.set(this.currentFrame() - 1);
    }
  }

  nextFrame(): void {
    if (this.currentFrame() < this.coverageFrames().length - 1) {
      this.currentFrame.set(this.currentFrame() + 1);
    }
  }
}
