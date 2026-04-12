import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ApiService } from '../../services/api.service';
import { AnalyzeResponse } from '../../models/api.models';
import { MiniAnalyzerComponent } from '../../components/mini-analyzer/mini-analyzer.component';
import { ProgressService } from '../../services/progress.service';

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
    RouterLink,
    MiniAnalyzerComponent,
  ],
  template: `
    <div class="analyzer-layout">

      <!-- ════════ LEFT PANE: Input (sticky) ════════ -->
      <section class="left-pane" aria-label="Input">
        <h1 class="pane-title">Analyzer</h1>
        <p class="pane-subtitle">Type a session type. See the lattice.</p>

        <label for="type-input" class="input-label">Session type</label>
        <textarea
          id="type-input"
          class="type-input"
          [ngModel]="typeString()"
          (ngModelChange)="typeString.set($event)"
          (keydown.control.enter)="analyze()"
          placeholder="rec X . &{read: X, close: end}"
          spellcheck="false"
          aria-describedby="input-hint"
        ></textarea>
        <span id="input-hint" class="input-hint">Ctrl+Enter to analyze</span>

        <div class="action-row">
          <button class="analyze-btn"
                  [disabled]="analyzing() || !typeString().trim()"
                  (click)="analyze()"
                  aria-label="Analyze session type">
            @if (analyzing()) {
              <mat-spinner diameter="18" class="analyze-spinner"></mat-spinner>
            } @else {
              Analyze
            }
          </button>
          <button class="copy-btn"
                  (click)="copyLink()"
                  [disabled]="!typeString().trim()"
                  aria-label="Copy shareable link">
            <mat-icon>link</mat-icon>
          </button>
        </div>

        <!-- Examples -->
        <div class="examples-section" role="group" aria-label="Quick examples">
          <div class="examples-label">Examples</div>
          <div class="examples-grid">
            @for (ex of quickExamples; track ex.label) {
              <button class="example-chip" (click)="loadExample(ex)" [attr.aria-label]="'Load ' + ex.label + ' example'">{{ ex.label }}</button>
            }
          </div>
        </div>

        <!-- Grammar toggle -->
        <button class="grammar-toggle"
                (click)="showGrammar.set(!showGrammar())"
                [attr.aria-expanded]="showGrammar()">
          {{ showGrammar() ? 'Hide' : 'Show' }} grammar reference
        </button>
        @if (showGrammar()) {
          <div class="grammar-block" role="region" aria-label="Grammar reference">{{ grammarRef }}</div>
        }

        <!-- Upload Java file -->
        <div class="upload-section">
          <label class="upload-label" for="java-upload">
            Or upload a Java file with &#64;Session annotations
          </label>
          <input type="file" id="java-upload" accept=".java" class="upload-input"
                 (change)="onFileUpload($event)" />
          @if (uploadMessage()) {
            <div class="upload-msg" [class.upload-error]="uploadError()">{{ uploadMessage() }}</div>
          }
        </div>
      </section>

      <!-- ════════ RIGHT PANE: Results ════════ -->
      <section class="right-pane" aria-label="Results" aria-live="polite">

        <!-- Empty state -->
        @if (!result() && !analyzing() && !error()) {
          <div class="empty-state">
            <div class="empty-icon" aria-hidden="true">&#x25C7;</div>
            <p class="empty-text">Type a session type and click Analyze</p>
            <p class="empty-hint">or click an example on the left</p>
          </div>
        }

        <!-- Loading -->
        @if (analyzing()) {
          <div class="empty-state" role="status" aria-label="Analyzing">
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

        <!-- Results -->
        @if (result()) {
          <!-- Header badges -->
          <div class="result-header">
            <span class="result-badge" [class.badge-lattice]="result()!.isLattice" [class.badge-not-lattice]="!result()!.isLattice"
                  [attr.aria-label]="result()!.isLattice ? 'Verdict: is a lattice' : 'Verdict: not a lattice'">
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
          <div class="pretty-block" aria-label="Pretty-printed session type">{{ result()!.pretty }}</div>

          <!-- Hasse diagram (front and center) -->
          @if (result()!.svgHtml) {
            <figure class="hasse-section" role="img" [attr.aria-label]="'Hasse diagram of ' + result()!.pretty" [innerHTML]="safeSvg()"></figure>
          }

          <!-- Explain this -->
          <button class="explain-toggle"
                  (click)="toggleExplain()"
                  [attr.aria-expanded]="showExplain()">
            {{ showExplain() ? 'Hide explanation' : 'Explain this protocol' }}
          </button>
          @if (showExplain() && explanation()) {
            <div class="explain-panel" role="region" aria-label="Protocol explanation" [innerHTML]="explanationHtml()"></div>
          }
          @if (showExplain() && !explanation() && loadingExplain()) {
            <div class="explain-panel explain-loading">Loading explanation...</div>
          }

          <!-- Protocol Story -->
          <button class="explain-toggle"
                  (click)="toggleStory()"
                  [attr.aria-expanded]="showStory()">
            {{ showStory() ? 'Hide story' : 'Tell as a story' }}
          </button>
          @if (showStory() && storyHtml()) {
            <div class="explain-panel story-panel" role="region" aria-label="Protocol story" [innerHTML]="storyHtml()"></div>
          }
          @if (showStory() && !storyText() && loadingStory()) {
            <div class="explain-panel explain-loading">Crafting story...</div>
          }

          <!-- Verdict chips -->
          <div class="verdict-row" role="list" aria-label="Property verdicts">
            <span class="verdict-chip" role="listitem" [class.verdict-pass]="result()!.isLattice" [class.verdict-fail]="!result()!.isLattice">
              {{ result()!.isLattice ? '\u2713' : '\u2717' }} Lattice
            </span>
            <span class="verdict-chip" role="listitem" [class.verdict-pass]="result()!.terminates" [class.verdict-fail]="!result()!.terminates">
              {{ result()!.terminates ? '\u2713' : '\u2717' }} Terminates
            </span>
            <span class="verdict-chip" role="listitem" [class.verdict-pass]="result()!.wfParallel" [class.verdict-fail]="!result()!.wfParallel">
              {{ result()!.wfParallel ? '\u2713' : '\u2717' }} WF-Par
            </span>
            @if (result()!.usesParallel) {
              <span class="verdict-chip" role="listitem" [class.verdict-pass]="result()!.threadSafe" [class.verdict-fail]="!result()!.threadSafe">
                {{ result()!.threadSafe ? '\u2713' : '\u2717' }} Thread Safe
              </span>
            }
          </div>

          @if (result()!.counterexample) {
            <div class="counterexample-section">
              <div class="counterexample-text" role="alert">Counterexample: {{ result()!.counterexample }}</div>
              <button class="explain-toggle" (click)="showFixer.set(!showFixer())"
                      [attr.aria-expanded]="showFixer()">
                {{ showFixer() ? 'Hide playground' : 'Fix it — edit and try again' }}
              </button>
              @if (showFixer()) {
                <div class="fixer-hint">Modify the type below until the lattice check passes:</div>
                <app-mini-analyzer [typeString]="typeString()" (analyzed)="onFixerAnalyzed($event)"></app-mini-analyzer>
              }
            </div>
          }

          <!-- Details grid -->
          <div class="details-grid" role="list" aria-label="Analysis details">
            <div class="detail-card" role="listitem">
              <div class="detail-label">States</div>
              <div class="detail-value">{{ result()!.numStates }}</div>
              <div class="detail-sub">{{ result()!.numSccs }} SCCs</div>
            </div>
            <div class="detail-card" role="listitem">
              <div class="detail-label">Transitions</div>
              <div class="detail-value">{{ result()!.numTransitions }}</div>
              <div class="detail-sub">{{ result()!.numMethods }} methods</div>
            </div>
            <div class="detail-card" role="listitem">
              <div class="detail-label">Test Paths</div>
              <div class="detail-value">{{ result()!.numTests }}</div>
              <div class="detail-sub">{{ result()!.numValidPaths }} valid &middot; {{ result()!.numViolations }} violations</div>
            </div>
            <div class="detail-card" role="listitem">
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
              <div class="methods-list" role="list" aria-label="Method names">
                @for (m of result()!.methods; track m) {
                  <span class="method-chip" role="listitem">{{ m }}</span>
                }
              </div>
            </div>
          }

          <!-- DOT source toggle -->
          @if (result()!.dotSource) {
            <button class="dot-toggle"
                    (click)="showDot.set(!showDot())"
                    [attr.aria-expanded]="showDot()">
              {{ showDot() ? 'Hide' : 'Show' }} DOT source
            </button>
            @if (showDot()) {
              <div class="dot-block" role="region" aria-label="DOT source code">{{ result()!.dotSource }}</div>
            }
          }

          <!-- Cross-tool navigation -->
          <nav class="cross-links" aria-label="Related tools">
            <a class="cross-link" [routerLink]="['/tools/test-generator']" [queryParams]="{type: typeString()}">Generate Tests &rarr;</a>
            <a class="cross-link" [routerLink]="['/tools/compare']">Compare with Dual &rarr;</a>
            <a class="cross-link" [routerLink]="['/tools/test-generator']" [queryParams]="{type: typeString()}">View Coverage &rarr;</a>
          </nav>
        }
      </section>
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
  readonly showExplain = signal(false);
  readonly explanation = signal('');
  readonly explanationHtml = signal<SafeHtml>('');
  readonly loadingExplain = signal(false);
  readonly showStory = signal(false);
  readonly storyText = signal('');
  readonly storyHtml = signal<SafeHtml>('');
  readonly loadingStory = signal(false);
  readonly uploadMessage = signal('');
  readonly uploadError = signal(false);
  readonly showFixer = signal(false);
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
    private progress: ProgressService,
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
    this.showExplain.set(false);
    this.explanation.set('');
    this.explanationHtml.set('');
    this.showStory.set(false);
    this.storyText.set('');
    this.storyHtml.set('');

    this.api.analyze(this.typeString()).subscribe({
      next: (res) => {
        this.result.set(res);
        this.safeSvg.set(this.sanitizer.bypassSecurityTrustHtml(res.svgHtml));
        this.analyzing.set(false);
        this.progress.recordAnalysis();
      },
      error: (err) => {
        this.error.set(err.error?.error || err.message || 'Analysis failed');
        this.analyzing.set(false);
      },
    });
  }

  toggleExplain(): void {
    if (this.showExplain()) {
      this.showExplain.set(false);
      return;
    }
    this.showExplain.set(true);
    if (this.explanation()) return; // already loaded
    this.loadingExplain.set(true);
    this.api.explain(this.typeString()).subscribe({
      next: (res) => {
        this.explanation.set(res.explanation);
        // Convert markdown-like **bold** to <strong> and newlines to <br>/<p>
        const html = res.explanation
          .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
          .replace(/\n\n/g, '</p><p>')
          .replace(/\n- /g, '<br>&bull; ')
          .replace(/\n/g, '<br>');
        this.explanationHtml.set(this.sanitizer.bypassSecurityTrustHtml('<p>' + html + '</p>'));
        this.loadingExplain.set(false);
      },
      error: () => {
        this.explanation.set('Could not generate explanation.');
        this.explanationHtml.set('Could not generate explanation.');
        this.loadingExplain.set(false);
      },
    });
  }

  toggleStory(): void {
    if (this.showStory()) {
      this.showStory.set(false);
      return;
    }
    this.showStory.set(true);
    if (this.storyText()) return;
    this.loadingStory.set(true);
    this.api.story(this.typeString()).subscribe({
      next: (res) => {
        this.storyText.set(res.story);
        const html = res.story
          .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
          .replace(/\n\n/g, '</p><p>')
          .replace(/\n- /g, '<br>&bull; ')
          .replace(/\n/g, '<br>');
        this.storyHtml.set(this.sanitizer.bypassSecurityTrustHtml('<p>' + html + '</p>'));
        this.loadingStory.set(false);
      },
      error: () => {
        this.storyText.set('Could not generate story.');
        this.storyHtml.set('Could not generate story.');
        this.loadingStory.set(false);
      },
    });
  }

  onFileUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    this.uploadMessage.set('');
    this.uploadError.set(false);

    this.api.extractSession(file).subscribe({
      next: (res) => {
        if (res.found && res.annotations.length > 0) {
          const ann = res.annotations[0];
          this.typeString.set(ann.typeString);
          this.uploadMessage.set(`Found @Session on ${ann.className}` +
            (res.annotations.length > 1 ? ` (+${res.annotations.length - 1} more)` : ''));
          this.analyze();
        } else {
          this.uploadMessage.set(res.message || 'No @Session annotations found');
          this.uploadError.set(true);
        }
      },
      error: () => {
        this.uploadMessage.set('Failed to process file');
        this.uploadError.set(true);
      },
    });
    input.value = '';
  }

  onFixerAnalyzed(result: AnalyzeResponse): void {
    if (result.isLattice) {
      this.progress.recordCounterexampleFix();
    }
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
