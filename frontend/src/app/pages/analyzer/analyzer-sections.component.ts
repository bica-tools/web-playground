import { Component, OnInit, signal, ElementRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AnalyzeResponse, BenchmarkDto, TestGenRequest } from '../../models/api.models';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';
import { HasseDiagramComponent } from '../../components/hasse-diagram/hasse-diagram.component';

interface QuickExample {
  label: string;
  type: string;
}

@Component({
  selector: 'app-analyzer-sections',
  standalone: true,
  imports: [
    FormsModule,
    CodeBlockComponent,
    HasseDiagramComponent,
  ],
  template: `
    <div class="analyzer-sections">
      <header class="page-header">
        <h1>Interactive Analyzer</h1>
        <p>Parse a session type, build its state space, check lattice properties, and visualize the Hasse diagram.</p>
      </header>

      <!-- ===== Input Card ===== -->
      <div class="section-card neutral">
        <div class="section-header">
          <h2 class="section-title">Input</h2>
          <button class="link-btn" (click)="copyLink()" title="Copy shareable link">
            <span class="link-icon">&#x1F517;</span> Copy link
          </button>
        </div>
        <div class="section-body">
          <div class="input-group">
            <label for="type-input" class="input-label">Session type</label>
            <textarea
              id="type-input"
              class="type-textarea"
              [ngModel]="typeString()"
              (ngModelChange)="typeString.set($event)"
              rows="3"
              placeholder="e.g. rec X . &{read: X, done: end}"
              (keydown.control.enter)="analyze()"
            ></textarea>
            <span class="input-hint">Press Ctrl+Enter to analyze</span>
          </div>

          <div class="quick-examples">
            <span class="quick-label">Quick examples:</span>
            @for (ex of quickExamples; track ex.label) {
              <button class="quick-btn" (click)="loadExample(ex)">{{ ex.label }}</button>
            }
          </div>

          <div class="form-row">
            <div class="select-group">
              <label for="benchmark-select" class="input-label">Load benchmark</label>
              <select
                id="benchmark-select"
                class="form-select"
                [ngModel]="selectedBenchmark()"
                (ngModelChange)="onBenchmarkSelect($event)"
              >
                <option value="">-- Select --</option>
                @for (b of benchmarks(); track b.name) {
                  <option [value]="b.typeString">{{ b.name }}</option>
                }
              </select>
            </div>

            <div class="input-field-group">
              <label for="class-name" class="input-label">Class name (for test gen)</label>
              <input
                id="class-name"
                type="text"
                class="form-input"
                [ngModel]="className()"
                (ngModelChange)="className.set($event)"
                placeholder="e.g. FileHandle"
              />
            </div>

            <button
              class="analyze-btn"
              [disabled]="analyzing() || !typeString().trim()"
              (click)="analyze()"
            >
              @if (analyzing()) {
                <span class="spinner"></span>
              } @else {
                Analyze
              }
            </button>
          </div>
        </div>
      </div>

      <!-- ===== Error ===== -->
      @if (error()) {
        <div class="error-banner">
          <span class="error-icon">&#x26A0;</span>
          <span>{{ error() }}</span>
        </div>
      }

      <!-- ===== Parse Result Card ===== -->
      @if (result()) {
        <div class="section-card" [class.pass]="!!result()!.pretty" [class.fail]="!result()!.pretty" #resultsSection>
          <div class="section-header">
            <h2 class="section-title">Parse</h2>
            <span class="status-chip" [class.chip-pass]="!!result()!.pretty" [class.chip-fail]="!result()!.pretty">
              {{ result()!.pretty ? '\u2713 Parsed' : '\u2717 Failed' }}
            </span>
          </div>
          <div class="section-body">
            <app-code-block [code]="result()!.pretty" label="Pretty-printed"></app-code-block>
          </div>
        </div>

        <!-- ===== Verification Summary Card ===== -->
        <div class="section-card" [class.pass]="allVerdictsPass()" [class.fail]="!allVerdictsPass()">
          <div class="section-header">
            <h2 class="section-title">Verification</h2>
          </div>
          <div class="section-body">
            <div class="verdict-grid">
              <div class="verdict-card" [class.v-pass]="result()!.isLattice" [class.v-fail]="!result()!.isLattice">
                <div class="verdict-icon">{{ result()!.isLattice ? '\u2713' : '\u2717' }}</div>
                <div class="verdict-label">Lattice</div>
              </div>
              <div class="verdict-card" [class.v-pass]="result()!.terminates" [class.v-fail]="!result()!.terminates">
                <div class="verdict-icon">{{ result()!.terminates ? '\u2713' : '\u2717' }}</div>
                <div class="verdict-label">Terminates</div>
              </div>
              <div class="verdict-card" [class.v-pass]="result()!.wfParallel" [class.v-fail]="!result()!.wfParallel">
                <div class="verdict-icon">{{ result()!.wfParallel ? '\u2713' : '\u2717' }}</div>
                <div class="verdict-label">WF-Par</div>
              </div>
              @if (result()!.usesParallel) {
                <div class="verdict-card" [class.v-pass]="result()!.threadSafe" [class.v-fail]="!result()!.threadSafe">
                  <div class="verdict-icon">{{ result()!.threadSafe ? '\u2713' : '\u2717' }}</div>
                  <div class="verdict-label">Thread Safe</div>
                </div>
              }
            </div>
            @if (result()!.counterexample) {
              <div class="counterexample">
                <strong>Counterexample:</strong> {{ result()!.counterexample }}
              </div>
            }
          </div>
        </div>

        <!-- ===== State Space Card ===== -->
        <div class="section-card neutral">
          <div class="section-header">
            <h2 class="section-title">State Space</h2>
            <span class="state-count-badge">{{ result()!.numStates }} states</span>
          </div>
          <div class="section-body">
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
                  <tr><td>Total tests</td><td>{{ result()!.numTests }}</td></tr>
                  <tr>
                    <td>Breakdown</td>
                    <td>Valid: {{ result()!.numValidPaths }}, Violations: {{ result()!.numViolations }}, Incomplete: {{ result()!.numIncomplete }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            @if (result()!.methods && result()!.methods.length > 0) {
              <div class="methods-section">
                <h3 class="subsection-title">Methods</h3>
                <div class="methods-list">
                  @for (m of result()!.methods; track m) {
                    <span class="method-chip">{{ m }}</span>
                  }
                </div>
              </div>
            }
          </div>
        </div>

        <!-- ===== Hasse Diagram Card ===== -->
        @if (result()!.svgHtml) {
          <div class="section-card neutral">
            <div class="section-header">
              <h2 class="section-title">Hasse Diagram</h2>
            </div>
            <div class="section-body hasse-body">
              <app-hasse-diagram [svgHtml]="result()!.svgHtml"></app-hasse-diagram>
            </div>
          </div>
        }

        <!-- ===== Test Generation Card ===== -->
        <div class="section-card neutral">
          <div class="section-header">
            <h2 class="section-title">Test Generation</h2>
            @if (result()!.numTests) {
              <span class="test-badge">{{ result()!.numTests }} tests</span>
            }
          </div>
          <div class="section-body">
            @if (!className().trim()) {
              <div class="test-prompt">
                <label for="test-class-name" class="input-label">Class name</label>
                <div class="test-prompt-row">
                  <input
                    id="test-class-name"
                    type="text"
                    class="form-input"
                    [ngModel]="testClassName()"
                    (ngModelChange)="testClassName.set($event)"
                    placeholder="e.g. FileHandle"
                  />
                  <button
                    class="generate-btn"
                    [disabled]="generatingTests() || !testClassName().trim()"
                    (click)="generateTestsWithName()"
                  >
                    @if (generatingTests()) {
                      <span class="spinner small"></span>
                    } @else {
                      Generate Tests
                    }
                  </button>
                </div>
              </div>
            } @else {
              @if (testSource()) {
                <app-code-block [code]="testSource()" label="JUnit 5"></app-code-block>
              } @else {
                <div class="test-prompt-row">
                  <button
                    class="generate-btn"
                    [disabled]="generatingTests()"
                    (click)="generateTests()"
                  >
                    @if (generatingTests()) {
                      <span class="spinner small"></span>
                    } @else {
                      Generate Tests for {{ className() }}
                    }
                  </button>
                </div>
              }
            }
          </div>
        </div>

        <!-- ===== DOT Source (collapsible) ===== -->
        @if (result()!.dotSource) {
          <details class="dot-details">
            <summary class="dot-summary">DOT Source</summary>
            <div class="dot-body">
              <app-code-block [code]="result()!.dotSource" label="DOT"></app-code-block>
            </div>
          </details>
        }
      }
    </div>
  `,
  styles: [`
    .analyzer-sections {
      max-width: 900px;
      margin: 0 auto;
      padding: 0 16px 48px;
    }

    /* Page header */
    .page-header {
      padding: 24px 0 20px;
    }
    .page-header h1 {
      font-size: 24px;
      font-weight: 600;
      margin: 0 0 8px;
    }
    .page-header p {
      color: rgba(0, 0, 0, 0.6);
      margin: 0;
      font-size: 15px;
    }

    /* Section cards */
    .section-card {
      background: #fff;
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-left: 4px solid #d1d5db;
      border-radius: 8px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
      margin-bottom: 20px;
      overflow: hidden;
    }
    .section-card.pass {
      border-left-color: #22c55e;
    }
    .section-card.fail {
      border-left-color: #ef4444;
    }
    .section-card.neutral {
      border-left-color: #94a3b8;
    }

    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 20px;
      background: #f8fafc;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    }
    .section-title {
      font-size: 16px;
      font-weight: 600;
      margin: 0;
      color: #1e293b;
    }

    .section-body {
      padding: 20px;
    }

    /* Status chips */
    .status-chip {
      display: inline-flex;
      align-items: center;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 13px;
      font-weight: 500;
    }
    .chip-pass {
      background: #ecfdf5;
      color: #065f46;
    }
    .chip-fail {
      background: #fef2f2;
      color: #991b1b;
    }

    .state-count-badge {
      display: inline-flex;
      align-items: center;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 13px;
      font-weight: 500;
      background: #eff6ff;
      color: #1e40af;
    }

    .test-badge {
      display: inline-flex;
      align-items: center;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 13px;
      font-weight: 500;
      background: #f5f3ff;
      color: #5b21b6;
    }

    /* Link button */
    .link-btn {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: none;
      border: 1px solid rgba(0, 0, 0, 0.15);
      border-radius: 6px;
      padding: 4px 10px;
      font-size: 13px;
      color: #475569;
      cursor: pointer;
      transition: background 0.15s;
    }
    .link-btn:hover {
      background: rgba(0, 0, 0, 0.04);
    }
    .link-icon {
      font-size: 14px;
    }

    /* Input area */
    .input-group {
      margin-bottom: 16px;
    }
    .input-label {
      display: block;
      font-size: 13px;
      font-weight: 500;
      color: #475569;
      margin-bottom: 6px;
    }
    .type-textarea {
      width: 100%;
      padding: 12px;
      border: 1px solid rgba(0, 0, 0, 0.2);
      border-radius: 6px;
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
      font-size: 14px;
      line-height: 1.5;
      resize: vertical;
      box-sizing: border-box;
      transition: border-color 0.15s;
    }
    .type-textarea:focus {
      outline: none;
      border-color: #6366f1;
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
    }
    .input-hint {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.45);
      margin-top: 4px;
      display: block;
    }

    /* Quick examples */
    .quick-examples {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }
    .quick-label {
      font-size: 13px;
      color: #64748b;
    }
    .quick-btn {
      padding: 4px 12px;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
      background: #f8fafc;
      font-size: 13px;
      color: #334155;
      cursor: pointer;
      transition: all 0.15s;
    }
    .quick-btn:hover {
      background: #e2e8f0;
      border-color: #94a3b8;
    }

    /* Form row */
    .form-row {
      display: flex;
      gap: 12px;
      align-items: flex-end;
      flex-wrap: wrap;
    }
    .select-group,
    .input-field-group {
      flex: 1;
      min-width: 180px;
    }
    .form-select,
    .form-input {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid rgba(0, 0, 0, 0.2);
      border-radius: 6px;
      font-size: 14px;
      box-sizing: border-box;
      background: #fff;
      transition: border-color 0.15s;
    }
    .form-select:focus,
    .form-input:focus {
      outline: none;
      border-color: #6366f1;
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
    }

    /* Analyze button */
    .analyze-btn {
      padding: 10px 28px;
      background: #6366f1;
      color: #fff;
      border: none;
      border-radius: 6px;
      font-size: 15px;
      font-weight: 500;
      cursor: pointer;
      min-width: 120px;
      height: 42px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      transition: background 0.15s;
    }
    .analyze-btn:hover:not(:disabled) {
      background: #4f46e5;
    }
    .analyze-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    /* Spinner */
    .spinner {
      display: inline-block;
      width: 20px;
      height: 20px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: #fff;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }
    .spinner.small {
      width: 16px;
      height: 16px;
      border-color: rgba(99, 102, 241, 0.3);
      border-top-color: #6366f1;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    /* Error */
    .error-banner {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 14px 18px;
      border: 1px solid #fca5a5;
      border-left: 4px solid #ef4444;
      border-radius: 8px;
      background: #fef2f2;
      color: #991b1b;
      font-size: 14px;
      margin-bottom: 20px;
    }
    .error-icon {
      font-size: 18px;
    }

    /* Verdict grid */
    .verdict-grid {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      margin-bottom: 12px;
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
    .verdict-card.v-pass {
      background: #ecfdf5;
      border-color: #a7f3d0;
      color: #065f46;
    }
    .verdict-card.v-fail {
      background: #fef2f2;
      border-color: #fecaca;
      color: #991b1b;
    }
    .verdict-icon {
      font-size: 18px;
    }
    .verdict-label {
      font-size: 13px;
    }

    .counterexample {
      font-size: 13px;
      color: #991b1b;
      background: #fef2f2;
      padding: 10px 14px;
      border-radius: 6px;
      border: 1px solid #fecaca;
      margin-top: 4px;
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
    .methods-section {
      margin-top: 20px;
    }
    .subsection-title {
      font-size: 14px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.65);
      margin: 0 0 10px;
    }
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

    /* Hasse body */
    .hasse-body {
      padding: 16px;
      display: flex;
      justify-content: center;
    }

    /* Test generation */
    .test-prompt {
      max-width: 400px;
    }
    .test-prompt-row {
      display: flex;
      gap: 10px;
      align-items: center;
    }
    .generate-btn {
      padding: 8px 20px;
      background: #fff;
      color: #6366f1;
      border: 1px solid #6366f1;
      border-radius: 6px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      white-space: nowrap;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.15s;
    }
    .generate-btn:hover:not(:disabled) {
      background: #eef2ff;
    }
    .generate-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    /* DOT collapsible */
    .dot-details {
      margin-bottom: 20px;
    }
    .dot-summary {
      padding: 12px 20px;
      background: #f8fafc;
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      color: #475569;
      cursor: pointer;
      user-select: none;
      transition: background 0.15s;
    }
    .dot-summary:hover {
      background: #f1f5f9;
    }
    .dot-details[open] .dot-summary {
      border-radius: 8px 8px 0 0;
      border-bottom: none;
    }
    .dot-body {
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-top: none;
      border-radius: 0 0 8px 8px;
      padding: 16px;
      background: #fff;
    }
  `],
})
export class AnalyzerSectionsComponent implements OnInit {
  @ViewChild('resultsSection') resultsSection?: ElementRef;

  readonly typeString = signal('');
  readonly className = signal('');
  readonly testClassName = signal('');
  readonly selectedBenchmark = signal('');
  readonly benchmarks = signal<BenchmarkDto[]>([]);
  readonly result = signal<AnalyzeResponse | null>(null);
  readonly testSource = signal('');
  readonly error = signal('');
  readonly analyzing = signal(false);
  readonly generatingTests = signal(false);

  readonly quickExamples: QuickExample[] = [
    { label: 'Iterator', type: 'rec X . &{hasNext: +{True: &{next: X}, False: end}}' },
    { label: 'SMTP', type: '&{HELO: &{MAIL: &{RCPT: &{DATA: &{CRLF: +{Ok250: &{QUIT: end}, Err: &{QUIT: end}}}}}, QUIT: end}}' },
    { label: 'Two-Buyer', type: '&{quote: +{accept: &{deliver: end}, reject: end}}' },
    { label: 'MCP', type: '&{initialize: &{initialized: rec X . &{tools_list: X, call_tool: +{result: X, error: X}, ping: X, shutdown: end}}}' },
  ];

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
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

  allVerdictsPass(): boolean {
    const r = this.result();
    if (!r) return false;
    const base = r.isLattice && r.terminates && r.wfParallel;
    if (r.usesParallel) return base && r.threadSafe;
    return base;
  }

  onBenchmarkSelect(value: string): void {
    this.selectedBenchmark.set(value);
    if (value) {
      this.typeString.set(value);
      this.analyze();
    }
  }

  loadExample(ex: QuickExample): void {
    this.typeString.set(ex.type);
    this.analyze();
  }

  copyLink(): void {
    const encoded = encodeURIComponent(this.typeString());
    const url = `${window.location.origin}${window.location.pathname}?type=${encoded}`;
    navigator.clipboard.writeText(url).then(() => {
      // Brief visual feedback could be added here
    });
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
        this.error.set(err.error?.error || 'Test generation failed');
        this.generatingTests.set(false);
      },
    });
  }

  generateTestsWithName(): void {
    if (!this.typeString().trim() || !this.testClassName().trim()) return;
    this.className.set(this.testClassName());
    this.generateTests();
  }
}
