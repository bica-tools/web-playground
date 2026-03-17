import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ApiService } from '../../services/api.service';
import { HomeStats, AnalyzeResponse } from '../../models/api.models';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, FormsModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- HERO — The Hook                                        -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <p class="hero-eyebrow">Session Types as Algebraic Reticulates</p>
          <h1 class="hero-title">
            Every session-type state space<br/>
            forms a <span class="hero-highlight">lattice</span>.
          </h1>
          <p class="hero-subtitle">We prove it. We mechanise it. We build tools for it.</p>
          <div class="hero-cta">
            <a mat-flat-button routerLink="/tools/analyzer" class="btn-primary">
              Try the Playground
            </a>
            <a mat-stroked-button routerLink="/publications" class="btn-outline">
              Read the Paper
            </a>
          </div>
        </div>
        <div class="hero-viz">
          <!-- Animated lattice that builds itself -->
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 400" class="hero-lattice">
            <!-- Edges animate in after nodes -->
            <g class="lattice-edges" stroke="rgba(255,255,255,0.3)" stroke-width="1.5" fill="none">
              <line x1="150" y1="48" x2="60" y2="128" class="edge e1"/>
              <line x1="150" y1="48" x2="240" y2="128" class="edge e2"/>
              <line x1="60" y1="128" x2="30" y2="208" class="edge e3"/>
              <line x1="60" y1="128" x2="150" y2="208" class="edge e4"/>
              <line x1="240" y1="128" x2="150" y2="208" class="edge e5"/>
              <line x1="240" y1="128" x2="270" y2="208" class="edge e6"/>
              <line x1="30" y1="208" x2="60" y2="288" class="edge e7"/>
              <line x1="150" y1="208" x2="60" y2="288" class="edge e8"/>
              <line x1="150" y1="208" x2="240" y2="288" class="edge e9"/>
              <line x1="270" y1="208" x2="240" y2="288" class="edge e10"/>
              <line x1="60" y1="288" x2="150" y2="368" class="edge e11"/>
              <line x1="240" y1="288" x2="150" y2="368" class="edge e12"/>
            </g>
            <!-- Edge labels -->
            <g fill="rgba(255,255,255,0.45)" font-family="'JetBrains Mono',monospace" font-size="10" font-style="italic" class="edge-labels">
              <text x="97" y="82" class="elbl el1">a</text>
              <text x="200" y="82" class="elbl el2">c</text>
              <text x="36" y="164" class="elbl el3">b</text>
              <text x="113" y="164" class="elbl el4">c</text>
              <text x="200" y="164" class="elbl el5">a</text>
              <text x="262" y="164" class="elbl el6">d</text>
              <text x="36" y="244" class="elbl el7">c</text>
              <text x="97" y="244" class="elbl el8">b</text>
              <text x="207" y="244" class="elbl el9">d</text>
              <text x="264" y="244" class="elbl el10">a</text>
              <text x="97" y="324" class="elbl el11">d</text>
              <text x="200" y="324" class="elbl el12">b</text>
            </g>
            <!-- Nodes animate in one by one -->
            <g class="lattice-nodes">
              <g class="node n1">
                <circle cx="150" cy="40" r="16" fill="rgba(147,197,253,0.35)" stroke="rgba(191,219,254,0.9)" stroke-width="1.5"/>
                <text x="150" y="44" text-anchor="middle" fill="white" font-size="9" font-family="'JetBrains Mono',monospace">top</text>
              </g>
              <g class="node n2">
                <circle cx="60" cy="128" r="14" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.45)" stroke-width="1.2"/>
                <text x="60" y="132" text-anchor="middle" fill="rgba(255,255,255,0.85)" font-size="8" font-family="'JetBrains Mono',monospace">s&#x2081;</text>
              </g>
              <g class="node n3">
                <circle cx="240" cy="128" r="14" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.45)" stroke-width="1.2"/>
                <text x="240" y="132" text-anchor="middle" fill="rgba(255,255,255,0.85)" font-size="8" font-family="'JetBrains Mono',monospace">s&#x2082;</text>
              </g>
              <g class="node n4">
                <circle cx="30" cy="208" r="14" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.45)" stroke-width="1.2"/>
                <text x="30" y="212" text-anchor="middle" fill="rgba(255,255,255,0.85)" font-size="8" font-family="'JetBrains Mono',monospace">s&#x2083;</text>
              </g>
              <g class="node n5">
                <circle cx="150" cy="208" r="14" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.45)" stroke-width="1.2"/>
                <text x="150" y="212" text-anchor="middle" fill="rgba(255,255,255,0.85)" font-size="8" font-family="'JetBrains Mono',monospace">s&#x2081;&#x2227;s&#x2082;</text>
              </g>
              <g class="node n6">
                <circle cx="270" cy="208" r="14" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.45)" stroke-width="1.2"/>
                <text x="270" y="212" text-anchor="middle" fill="rgba(255,255,255,0.85)" font-size="8" font-family="'JetBrains Mono',monospace">s&#x2084;</text>
              </g>
              <g class="node n7">
                <circle cx="60" cy="288" r="14" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.45)" stroke-width="1.2"/>
                <text x="60" y="292" text-anchor="middle" fill="rgba(255,255,255,0.85)" font-size="8" font-family="'JetBrains Mono',monospace">s&#x2085;</text>
              </g>
              <g class="node n8">
                <circle cx="240" cy="288" r="14" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.45)" stroke-width="1.2"/>
                <text x="240" y="292" text-anchor="middle" fill="rgba(255,255,255,0.85)" font-size="8" font-family="'JetBrains Mono',monospace">s&#x2086;</text>
              </g>
              <g class="node n9">
                <circle cx="150" cy="368" r="16" fill="rgba(134,239,172,0.25)" stroke="rgba(187,247,208,0.8)" stroke-width="1.5"/>
                <text x="150" y="372" text-anchor="middle" fill="white" font-size="9" font-family="'JetBrains Mono',monospace">end</text>
              </g>
            </g>
            <!-- Caption -->
            <text x="150" y="398" text-anchor="middle" fill="rgba(255,255,255,0.5)" font-family="'JetBrains Mono',monospace" font-size="9">
              a.b.end &#x2225; c.d.end
            </text>
          </svg>
        </div>
      </div>
      <!-- Scroll indicator -->
      <div class="scroll-hint">
        <div class="scroll-arrow"></div>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- STATS — Proof by numbers                               -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <section class="stats-bar">
      @if (loading()) {
        <mat-spinner diameter="16"></mat-spinner>
      } @else if (stats()) {
        <div class="stat-item">
          <span class="stat-value">{{ stats()!.numBenchmarks }}</span>
          <span class="stat-label">Protocol benchmarks</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats()!.totalStates }}</span>
          <span class="stat-label">States verified</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats()!.totalTests }}</span>
          <span class="stat-label">Tests generated</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">4,100+</span>
          <span class="stat-label">Library tests</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">28</span>
          <span class="stat-label">Lean 4 modules</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">0</span>
          <span class="stat-label">sorry</span>
        </div>
      }
    </section>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- LIVE PLAYGROUND — Interactive within 10 seconds         -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <section class="playground">
      <div class="playground-inner">
        <h2 class="section-title">Try it now</h2>
        <p class="section-desc">Type a session type. See the lattice.</p>
        <div class="playground-container">
          <div class="playground-input-area">
            <div class="playground-input-wrapper">
              <label class="playground-label">Session type</label>
              <input
                type="text"
                class="playground-input"
                [value]="playgroundInput()"
                (input)="onPlaygroundInput($event)"
                (keydown.enter)="analyzePlayground()"
                placeholder="rec X . &{read: X, close: end}"
                spellcheck="false"
              />
            </div>
            <button class="playground-btn" (click)="analyzePlayground()" [disabled]="playgroundLoading()">
              @if (playgroundLoading()) {
                <mat-spinner diameter="18" class="playground-spinner"></mat-spinner>
              } @else {
                Analyze
              }
            </button>
          </div>
          <div class="playground-examples">
            <span class="examples-label">Examples:</span>
            @for (ex of quickExamples; track ex.name) {
              <button class="example-chip" (click)="loadExample(ex.type)">{{ ex.name }}</button>
            }
          </div>
          @if (playgroundResult()) {
            <div class="playground-result">
              <div class="result-meta">
                <div class="result-badge" [class.lattice]="playgroundResult()!.isLattice" [class.not-lattice]="!playgroundResult()!.isLattice">
                  {{ playgroundResult()!.isLattice ? 'Lattice' : 'Not a lattice' }}
                </div>
                <span class="result-stat">{{ playgroundResult()!.numStates }} states</span>
                <span class="result-stat">{{ playgroundResult()!.numTransitions }} transitions</span>
                <span class="result-stat">{{ playgroundResult()!.numTests }} tests</span>
                @if (playgroundResult()!.usesParallel) {
                  <span class="result-stat parallel-badge">&#x2225; parallel</span>
                }
              </div>
              <div class="result-hasse" [innerHTML]="playgroundSvg()"></div>
              <a class="result-expand" routerLink="/tools/analyzer" [queryParams]="{type: playgroundInput()}">
                Open in full analyzer &rarr;
              </a>
            </div>
          }
          @if (playgroundError()) {
            <div class="playground-error">{{ playgroundError() }}</div>
          }
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- THREE PILLARS — Theory / Tools / Proofs                -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <section class="pillars">
      <div class="pillars-inner">
        <div class="pillar" routerLink="/theory">
          <div class="pillar-icon">
            <svg viewBox="0 0 48 48" width="48" height="48">
              <path d="M24 6 L40 14 L40 34 L24 42 L8 34 L8 14 Z" fill="none" stroke="currentColor" stroke-width="2"/>
              <line x1="24" y1="6" x2="24" y2="42" stroke="currentColor" stroke-width="1.5" opacity="0.5"/>
              <line x1="8" y1="14" x2="40" y2="14" stroke="currentColor" stroke-width="1" opacity="0.3"/>
              <line x1="8" y1="34" x2="40" y2="34" stroke="currentColor" stroke-width="1" opacity="0.3"/>
            </svg>
          </div>
          <h3>Theory</h3>
          <p>19 research steps proving that session-type state spaces form lattices.
             Subtyping, duality, morphisms, multiparty projection, recursion analysis.</p>
          <span class="pillar-link">Explore the registry &rarr;</span>
        </div>
        <div class="pillar" routerLink="/tools/analyzer">
          <div class="pillar-icon">
            <svg viewBox="0 0 48 48" width="48" height="48">
              <rect x="6" y="10" width="36" height="28" rx="3" fill="none" stroke="currentColor" stroke-width="2"/>
              <line x1="6" y1="18" x2="42" y2="18" stroke="currentColor" stroke-width="1.5"/>
              <circle cx="12" cy="14" r="2" fill="currentColor" opacity="0.4"/>
              <circle cx="18" cy="14" r="2" fill="currentColor" opacity="0.4"/>
              <text x="24" y="32" text-anchor="middle" fill="currentColor" font-size="10" font-family="monospace">&gt;_</text>
            </svg>
          </div>
          <h3>Tools</h3>
          <p>Reticulate (Python) and BICA Reborn (Java). Parse, verify, visualise,
             generate tests. 5 interactive tools in the browser.</p>
          <span class="pillar-link">Open the playground &rarr;</span>
        </div>
        <div class="pillar" routerLink="/proofs">
          <div class="pillar-icon">
            <svg viewBox="0 0 48 48" width="48" height="48">
              <path d="M10 38 L10 10 L26 10 L26 6 L38 18 L26 18 L26 14 L14 14 L14 38 Z" fill="none" stroke="currentColor" stroke-width="2"/>
              <line x1="18" y1="22" x2="34" y2="22" stroke="currentColor" stroke-width="1.5" opacity="0.5"/>
              <line x1="18" y1="28" x2="30" y2="28" stroke="currentColor" stroke-width="1.5" opacity="0.5"/>
              <line x1="18" y1="34" x2="26" y2="34" stroke="currentColor" stroke-width="1.5" opacity="0.5"/>
              <text x="36" y="36" fill="currentColor" font-size="11" font-weight="bold" opacity="0.6">0</text>
            </svg>
          </div>
          <h3>Mechanised Proofs</h3>
          <p>28 Lean 4 modules, zero sorry. Progress, preservation, substitution,
             lattice properties, channel duality &mdash; all mechanised.</p>
          <span class="pillar-link">Browse the proofs &rarr;</span>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- THE INSIGHT — Visual explanation                        -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <section class="insight">
      <div class="insight-inner">
        <h2 class="section-title">The Key Insight</h2>
        <div class="insight-content">
          <div class="insight-text">
            <div class="insight-step">
              <div class="step-num">1</div>
              <div>
                <strong>Session types</strong> describe protocols on objects &mdash;
                which methods can be called, in what order, with what choices.
              </div>
            </div>
            <div class="insight-step">
              <div class="step-num">2</div>
              <div>
                The <strong>state space</strong> of a session type, ordered by reachability,
                has a top (initial state) and bottom (end) and all pairwise
                <strong>meets and joins</strong>.
              </div>
            </div>
            <div class="insight-step">
              <div class="step-num">3</div>
              <div>
                The <strong>parallel constructor</strong> (<code>&#x2225;</code>) models
                concurrent access. Two parallel paths create a <strong>product lattice</strong>
                &mdash; making lattice structure <em>necessary</em>.
              </div>
            </div>
            <div class="insight-step">
              <div class="step-num">4</div>
              <div>
                This lattice structure (the <strong>reticulate</strong>) unlocks morphisms,
                abstract interpretation, and a categorical bridge to multiparty session types.
              </div>
            </div>
          </div>
          <div class="insight-pipeline">
            <div class="pipeline-item">
              <div class="pipeline-label">Session type</div>
              <code class="pipeline-code">rec X . &amp;&#123;read: X, close: end&#125;</code>
            </div>
            <div class="pipeline-arrow">&darr;</div>
            <div class="pipeline-item">
              <div class="pipeline-label">State space</div>
              <code class="pipeline-code">2 states, 2 transitions</code>
            </div>
            <div class="pipeline-arrow">&darr;</div>
            <div class="pipeline-item">
              <div class="pipeline-label">Lattice check</div>
              <code class="pipeline-code success">&#x2713; Is a lattice</code>
            </div>
            <div class="pipeline-arrow">&darr;</div>
            <div class="pipeline-item">
              <div class="pipeline-label">Test generation</div>
              <code class="pipeline-code">3 valid paths, 2 violations</code>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- BENCHMARKS PREVIEW                                     -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <section class="benchmarks-preview">
      <h2 class="section-title">Verified on Real Protocols</h2>
      <p class="section-desc">
        34 protocols from software, distributed systems, security, biology, and physics &mdash;
        all verified as lattices.
      </p>
      <div class="benchmark-chips">
        @for (name of benchmarkNames; track name) {
          <span class="bench-chip">{{ name }}</span>
        }
      </div>
      <a mat-stroked-button routerLink="/benchmarks" class="benchmarks-link">
        Explore all benchmarks &rarr;
      </a>
    </section>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- PUBLICATIONS                                           -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <section class="pubs">
      <h2 class="section-title">Publications</h2>
      <div class="pub-grid">
        <div class="pub-card">
          <div class="pub-venue">ICE 2026 &mdash; DisCoTec</div>
          <div class="pub-title">Session Type State Spaces Form Lattices</div>
          <div class="pub-authors">A. Zua Caldeira</div>
        </div>
        <div class="pub-card">
          <div class="pub-venue">Tool paper (forthcoming)</div>
          <div class="pub-title">Reticulate: A Lattice Checker for Session Types</div>
          <div class="pub-authors">A. Zua Caldeira</div>
        </div>
        <div class="pub-card">
          <div class="pub-venue">Tool paper (forthcoming)</div>
          <div class="pub-title">BICA Reborn: Annotation-Based Session Types for Java</div>
          <div class="pub-authors">A. Zua Caldeira</div>
        </div>
      </div>
      <a mat-stroked-button routerLink="/publications" class="pubs-link">
        All publications &rarr;
      </a>
    </section>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- CTA — Final call to action                             -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <section class="cta-section">
      <div class="cta-inner">
        <h2>Start exploring</h2>
        <p>Type a session type. See the lattice. Generate tests.</p>
        <div class="cta-buttons">
          <a mat-flat-button routerLink="/tools/analyzer" class="btn-primary-dark">
            Open Playground
          </a>
          <a mat-stroked-button routerLink="/tutorials" class="btn-outline-dark">
            Read the Tutorials
          </a>
          <a mat-stroked-button routerLink="/benchmarks" class="btn-outline-dark">
            Browse Benchmarks
          </a>
        </div>
      </div>
    </section>
  `,
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  readonly stats = signal<HomeStats | null>(null);
  readonly loading = signal(true);

  // Playground state
  readonly playgroundInput = signal('rec X . &{read: X, close: end}');
  readonly playgroundLoading = signal(false);
  readonly playgroundResult = signal<AnalyzeResponse | null>(null);
  readonly playgroundSvg = signal<SafeHtml>('');
  readonly playgroundError = signal<string>('');

  readonly quickExamples = [
    { name: 'Java Iterator', type: 'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}' },
    { name: 'SMTP', type: 'connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}' },
    { name: 'MCP', type: 'initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})' },
    { name: 'Two-Buyer', type: 'lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})' },
  ];

  readonly benchmarkNames = [
    'SMTP (email)', 'OAuth 2.0 (auth)', 'MCP (AI agents)', 'Two-Buyer (e-commerce)',
    'Raft (consensus)', 'JDBC (database)', 'Ion Channel (biology)', 'Enzyme Kinetics (biochemistry)',
  ];

  constructor(private api: ApiService, private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    // Load stats from benchmarks
    this.api.getBenchmarks().subscribe({
      next: (benchmarks) => {
        this.stats.set({
          numBenchmarks: benchmarks.length,
          totalStates: benchmarks.reduce((sum, b) => sum + b.numStates, 0),
          totalTests: benchmarks.reduce((sum, b) => sum + b.numTests, 0),
          allLattice: benchmarks.every((b) => b.isLattice),
        });
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    // Auto-analyze the default example
    this.analyzePlayground();
  }

  onPlaygroundInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.playgroundInput.set(input.value);
  }

  loadExample(type: string): void {
    this.playgroundInput.set(type);
    this.analyzePlayground();
  }

  analyzePlayground(): void {
    const input = this.playgroundInput().trim();
    if (!input) return;

    this.playgroundLoading.set(true);
    this.playgroundError.set('');
    this.playgroundResult.set(null);

    this.api.analyze(input).subscribe({
      next: (result) => {
        this.playgroundResult.set(result);
        this.playgroundSvg.set(this.sanitizer.bypassSecurityTrustHtml(result.svgHtml));
        this.playgroundLoading.set(false);
      },
      error: (err) => {
        this.playgroundError.set(err.error?.error || 'Parse error — check your session type syntax');
        this.playgroundLoading.set(false);
      },
    });
  }
}
