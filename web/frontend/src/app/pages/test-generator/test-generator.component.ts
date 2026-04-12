import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ApiService } from '../../services/api.service';
import { TestGenRequest, CoverageFrameDto } from '../../models/api.models';

@Component({
  selector: 'app-test-generator',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatSnackBarModule,
    RouterLink,
  ],
  template: `
    <div class="tg-layout">

      <!-- ════════ LEFT PANE ════════ -->
      <div class="left-pane">
        <h1 class="pane-title">Test Generator</h1>
        <p class="pane-subtitle">Generate JUnit 5 tests with coverage storyboard.</p>

        <label for="tg-type-input" class="field-label">Session type</label>
        <textarea
          id="tg-type-input"
          class="type-input"
          [ngModel]="typeString()"
          (ngModelChange)="typeString.set($event)"
          placeholder="rec X . &{read: X, close: end}"
          spellcheck="false"
        ></textarea>

        <label for="tg-class-input" class="field-label" style="margin-top:12px">Class name</label>
        <input
          id="tg-class-input"
          class="class-input"
          [ngModel]="className()"
          (ngModelChange)="className.set($event)"
          placeholder="FileHandle"
          spellcheck="false"
        />

        <button class="generate-btn"
                [disabled]="generating() || !typeString().trim() || !className().trim()"
                (click)="generate()"
                aria-label="Generate tests">
          @if (generating()) {
            <mat-spinner diameter="18" class="gen-spinner"></mat-spinner>
          } @else {
            Generate Tests
          }
        </button>

        <!-- How it works -->
        <div class="how-it-works">
          <div class="hiw-title">Three kinds of tests</div>
          <div class="hiw-item">
            <span class="hiw-num">1</span>
            <div class="hiw-text"><strong>Valid paths</strong> &mdash; complete traces from top to end</div>
          </div>
          <div class="hiw-item">
            <span class="hiw-num">2</span>
            <div class="hiw-text"><strong>Violations</strong> &mdash; calls to disabled methods (expect rejection)</div>
          </div>
          <div class="hiw-item">
            <span class="hiw-num">3</span>
            <div class="hiw-text"><strong>Incomplete</strong> &mdash; partial executions that stop before end</div>
          </div>
        </div>

        <a class="back-link" routerLink="/tools/analyzer">&larr; Back to Analyzer</a>
      </div>

      <!-- ════════ RIGHT PANE ════════ -->
      <div class="right-pane">

        <!-- Empty state -->
        @if (!testSource() && !generating() && !error()) {
          <div class="empty-state">
            <div class="empty-icon">&#x2699;</div>
            <p class="empty-text">Enter a session type and class name</p>
            <p class="empty-hint">Tests and coverage storyboard will appear here</p>
          </div>
        }

        <!-- Loading -->
        @if (generating()) {
          <div class="empty-state">
            <mat-spinner diameter="40"></mat-spinner>
          </div>
        }

        <!-- Error -->
        @if (error()) {
          <div class="error-banner" role="alert">
            <mat-icon aria-hidden="true">error_outline</mat-icon>
            {{ error() }}
          </div>
        }

        <!-- ── Coverage Storyboard (primary view) ── -->
        @if (coverageFrames().length > 0) {
          <div class="storyboard-section">
            <h2 class="storyboard-header">Coverage Storyboard</h2>

            <div class="storyboard-stats">
              <span class="stat-chip">{{ coverageTotalTransitions() }} transitions</span>
              <span class="stat-chip">{{ coverageTotalStates() }} states</span>
              <span class="stat-chip">{{ coverageFrames().length }} test frames</span>
            </div>

            <!-- Frame navigator -->
            <div class="frame-nav">
              <button class="frame-btn" [disabled]="currentFrame() === 0" (click)="prevFrame()" aria-label="Previous frame">
                <mat-icon aria-hidden="true">chevron_left</mat-icon>
              </button>
              <div class="frame-info">
                <div class="frame-counter">{{ currentFrame() + 1 }} / {{ coverageFrames().length }}</div>
                <div class="frame-name">{{ coverageFrames()[currentFrame()].testName }}</div>
              </div>
              <button class="frame-btn" [disabled]="currentFrame() >= coverageFrames().length - 1" (click)="nextFrame()" aria-label="Next frame">
                <mat-icon aria-hidden="true">chevron_right</mat-icon>
              </button>
            </div>

            <!-- Frame metadata -->
            <div class="frame-meta">
              <span class="kind-badge"
                    [class.kind-valid]="coverageFrames()[currentFrame()].testKind === 'valid'"
                    [class.kind-violation]="coverageFrames()[currentFrame()].testKind === 'violation'"
                    [class.kind-incomplete]="coverageFrames()[currentFrame()].testKind === 'incomplete'">
                {{ coverageFrames()[currentFrame()].testKind }}
              </span>
              <span class="coverage-pct">
                Transitions: {{ (coverageFrames()[currentFrame()].transitionCoverage * 100).toFixed(0) }}%
              </span>
              <span class="coverage-pct">
                States: {{ (coverageFrames()[currentFrame()].stateCoverage * 100).toFixed(0) }}%
              </span>
            </div>

            <!-- Hasse with coverage highlighting -->
            <figure class="hasse-frame" role="img" aria-label="Coverage Hasse diagram" [innerHTML]="currentFrameSvg()"></figure>
          </div>
        }

        <!-- Loading coverage -->
        @if (loadingCoverage() && !generating()) {
          <div class="empty-state" style="min-height:200px">
            <mat-spinner diameter="32"></mat-spinner>
            <p class="empty-hint" style="margin-top:12px">Loading coverage storyboard...</p>
          </div>
        }

        <!-- ── Generated Code ── -->
        @if (testSource()) {
          <div class="code-section">
            <div class="code-header">
              <span class="code-title">Generated JUnit 5 Tests</span>
              <span class="code-meta">{{ className() }}ProtocolTest.java</span>
            </div>
            <button class="code-toggle" (click)="showCode.set(!showCode())" [attr.aria-expanded]="showCode()">
              {{ showCode() ? 'Hide' : 'Show' }} source code
            </button>
            @if (showCode()) {
              <div class="code-block">{{ testSource() }}</div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styleUrl: './test-generator.component.scss',
})
export class TestGeneratorComponent implements OnInit {
  readonly typeString = signal('');
  readonly className = signal('');
  readonly testSource = signal('');
  readonly error = signal('');
  readonly generating = signal(false);
  readonly showCode = signal(true);

  // Coverage storyboard
  readonly coverageFrames = signal<CoverageFrameDto[]>([]);
  readonly coverageTotalTransitions = signal(0);
  readonly coverageTotalStates = signal(0);
  readonly loadingCoverage = signal(false);
  readonly currentFrame = signal(0);
  readonly currentFrameSvg = signal<SafeHtml>('');

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['type']) this.typeString.set(params['type']);
      if (params['class']) this.className.set(params['class']);
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
        this.loadCoverage();
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
        if (res.frames.length > 0) {
          this.updateFrameSvg(0);
        }
      },
      error: (err) => {
        this.snackBar.open(
          err.error?.error || 'Coverage storyboard failed', 'Close',
          { duration: 5000 },
        );
        this.loadingCoverage.set(false);
      },
    });
  }

  prevFrame(): void {
    if (this.currentFrame() > 0) {
      const next = this.currentFrame() - 1;
      this.currentFrame.set(next);
      this.updateFrameSvg(next);
    }
  }

  nextFrame(): void {
    if (this.currentFrame() < this.coverageFrames().length - 1) {
      const next = this.currentFrame() + 1;
      this.currentFrame.set(next);
      this.updateFrameSvg(next);
    }
  }

  private updateFrameSvg(index: number): void {
    const frames = this.coverageFrames();
    if (index >= 0 && index < frames.length) {
      this.currentFrameSvg.set(
        this.sanitizer.bypassSecurityTrustHtml(frames[index].svgHtml),
      );
    }
  }
}
