import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../services/api.service';
import { HomeStats } from '../../models/api.models';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <h1>Session Types as Lattices</h1>
          <p class="hero-sub">Verify object protocols. Visualize state spaces. Generate tests.</p>
          <p class="hero-desc">
            A theory and toolchain proving that session-type state spaces form
            <strong>lattices</strong> &mdash; with the parallel constructor forcing
            product lattice structure for concurrent access.
          </p>
          <div class="hero-cta">
            <a mat-flat-button routerLink="/tools/analyzer" class="hero-btn-primary">
              Try the Analyzer
            </a>
            <a mat-stroked-button routerLink="/publications" class="hero-btn-outline">
              Read the Paper
            </a>
          </div>
          <p class="hero-author">Alexandre Zua Caldeira &mdash; Independent Researcher</p>
        </div>
        <div class="hero-diagram">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 340 440" class="hero-lattice-svg">
            <defs>
              <marker id="ah" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="rgba(255,255,255,0.5)"/>
              </marker>
            </defs>
            <g class="edges" stroke="rgba(255,255,255,0.35)" stroke-width="1.2" fill="none" marker-end="url(#ah)">
              <line x1="157" y1="36" x2="83" y2="102"/>
              <line x1="183" y1="36" x2="257" y2="102"/>
              <line x1="61" y1="130" x2="29" y2="188"/>
              <line x1="83" y1="126" x2="157" y2="192"/>
              <line x1="257" y1="126" x2="183" y2="192"/>
              <line x1="279" y1="130" x2="311" y2="188"/>
              <line x1="29" y1="220" x2="61" y2="278"/>
              <line x1="157" y1="216" x2="83" y2="282"/>
              <line x1="183" y1="216" x2="257" y2="282"/>
              <line x1="311" y1="220" x2="279" y2="278"/>
              <line x1="83" y1="306" x2="157" y2="372"/>
              <line x1="257" y1="306" x2="183" y2="372"/>
            </g>
            <g fill="rgba(255,255,255,0.55)" font-family="Inter,sans-serif" font-size="11" font-style="italic">
              <text x="112" y="63">a</text>
              <text x="222" y="63">c</text>
              <text x="38" y="156">b</text>
              <text x="128" y="156">c</text>
              <text x="212" y="156">a</text>
              <text x="298" y="156">d</text>
              <text x="30" y="252">c</text>
              <text x="108" y="252">b</text>
              <text x="230" y="252">d</text>
              <text x="306" y="252">a</text>
              <text x="108" y="342">d</text>
              <text x="228" y="342">b</text>
            </g>
            <g font-family="Inter,sans-serif" font-size="7" text-anchor="middle" dominant-baseline="central">
              <circle cx="170" cy="24" r="18" fill="rgba(147,197,253,0.3)" stroke="rgba(191,219,254,0.8)" stroke-width="1.5"/>
              <text x="170" y="24" fill="rgba(255,255,255,0.95)">(&#x22A4;&#x2081;, &#x22A4;&#x2082;)</text>
              <circle cx="70" cy="114" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="70" y="114" fill="rgba(255,255,255,0.9)">(s&#x2090;, &#x22A4;&#x2082;)</text>
              <circle cx="270" cy="114" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="270" y="114" fill="rgba(255,255,255,0.9)">(&#x22A4;&#x2081;, s&#x1D9C;)</text>
              <circle cx="20" cy="204" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="20" y="204" fill="rgba(255,255,255,0.9)">(&#x22A5;&#x2081;, &#x22A4;&#x2082;)</text>
              <circle cx="170" cy="204" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="170" y="204" fill="rgba(255,255,255,0.9)">(s&#x2090;, s&#x1D9C;)</text>
              <circle cx="320" cy="204" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="320" y="204" fill="rgba(255,255,255,0.9)">(&#x22A4;&#x2081;, &#x22A5;&#x2082;)</text>
              <circle cx="70" cy="294" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="70" y="294" fill="rgba(255,255,255,0.9)">(&#x22A5;&#x2081;, s&#x1D9C;)</text>
              <circle cx="270" cy="294" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="270" y="294" fill="rgba(255,255,255,0.9)">(s&#x2090;, &#x22A5;&#x2082;)</text>
              <circle cx="170" cy="384" r="18" fill="rgba(134,239,172,0.25)" stroke="rgba(187,247,208,0.8)" stroke-width="1.5"/>
              <text x="170" y="384" fill="rgba(255,255,255,0.95)">(&#x22A5;&#x2081;, &#x22A5;&#x2082;)</text>
            </g>
            <text x="170" y="438" text-anchor="middle" fill="rgba(255,255,255,0.6)" font-family="Inter,sans-serif" font-size="10">
              a.b.end &#x2225; c.d.end &mdash; 3&#xD7;3 product lattice
            </text>
          </svg>
        </div>
      </div>
    </section>

    <!-- Stats bar -->
    <section class="stats-bar">
      @if (loading()) {
        <mat-spinner diameter="16"></mat-spinner>
      } @else if (stats()) {
        <div class="stat-item">
          <span class="stat-value">{{ stats()!.numBenchmarks }}</span>
          <span class="stat-label">Benchmarks</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats()!.totalStates }}</span>
          <span class="stat-label">States analyzed</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats()!.totalTests }}</span>
          <span class="stat-label">Tests generated</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">19</span>
          <span class="stat-label">Analysis modules</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">2,458</span>
          <span class="stat-label">Total tests</span>
        </div>
      }
    </section>

    <!-- Core concept -->
    <section class="concept">
      <h2>The Key Insight</h2>
      <div class="concept-content">
        <div class="concept-text">
          <p>
            <strong>Session types</strong> describe communication protocols on objects &mdash;
            the legal sequences of method calls, branches, and selections.
            We prove that the state space of any well-formed session type,
            ordered by reachability, forms a <strong>lattice</strong>
            (which we call a <em>reticulate</em>).
          </p>
          <p>
            The <strong>parallel constructor</strong> (<code>&#x2225;</code>) models concurrent
            access to a shared object. When two execution paths run in parallel, their
            combined state space is the <strong>product lattice</strong> &mdash; and products
            of lattices are lattices. This makes lattice structure <em>necessary</em>
            rather than merely nice.
          </p>
          <p>
            Building on this foundation, we develop a <strong>morphism hierarchy</strong>
            (isomorphism, embedding, projection, Galois connection) between session-type
            state spaces, connecting to bisimulation, abstract interpretation, and
            multiparty session types.
          </p>
        </div>
        <div class="concept-example">
          <div class="code-example">
            <div class="code-label">Session type</div>
            <code>rec X . &amp;&#123;read: X, close: end&#125;</code>
          </div>
          <div class="arrow-down">&#x2193;</div>
          <div class="code-example">
            <div class="code-label">State space (lattice)</div>
            <code>&#x22A4; &rarr; read &rarr; &#x22A4; | close &rarr; &#x22A5;</code>
          </div>
          <div class="arrow-down">&#x2193;</div>
          <div class="code-example result">
            <div class="code-label">Verdict</div>
            <code>&#x2713; Is a lattice &middot; 2 states &middot; 2 transitions</code>
          </div>
        </div>
      </div>
    </section>

    <!-- Capabilities -->
    <section class="capabilities">
      <h2>Capabilities</h2>
      <div class="cap-grid">
        <div class="cap-card" routerLink="/tools/analyzer">
          <div class="cap-icon">&#x2713;</div>
          <h3>Lattice Verification</h3>
          <p>Parse session types, build state spaces, check lattice properties.
             Detect counterexamples, SCCs, and distributivity.</p>
        </div>
        <div class="cap-card" routerLink="/benchmarks">
          <div class="cap-icon">&#x25A6;</div>
          <h3>34 Protocol Benchmarks</h3>
          <p>SMTP, OAuth 2.0, MCP, A2A, Raft, Saga, 2PC, WebSocket, Kafka,
             gRPC, and more &mdash; all verified as lattices.</p>
        </div>
        <div class="cap-card" routerLink="/tools/analyzer">
          <div class="cap-icon">&#x2699;</div>
          <h3>Test Generation</h3>
          <p>Generate JUnit 5 tests from session types: valid paths,
             protocol violations, and incomplete prefixes.</p>
        </div>
        <div class="cap-card" routerLink="/tools/analyzer">
          <div class="cap-icon">&#x25C7;</div>
          <h3>Hasse Diagrams</h3>
          <p>Interactive visualization of state-space lattices with
             counterexample highlighting and role-colored edges.</p>
        </div>
      </div>
    </section>

    <!-- Live example -->
    <section class="live-example">
      <h2>Try an Example</h2>
      <p class="live-desc">
        See the analyzer in action with a real protocol benchmark.
      </p>
      <div class="example-buttons">
        <a mat-stroked-button
           [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}'}">
          Java Iterator
        </a>
        <a mat-stroked-button
           [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}'}">
          SMTP
        </a>
        <a mat-stroked-button
           [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})'}">
          MCP Protocol
        </a>
        <a mat-stroked-button
           [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})'}">
          Two-Buyer
        </a>
      </div>
    </section>

    <!-- Publications -->
    <section class="publications">
      <h2>Publications</h2>
      <div class="pub-list">
        <div class="pub-item">
          <div class="pub-venue">ICE 2026 &mdash; DisCoTec Workshop</div>
          <div class="pub-title">Session Type State Spaces Form Lattices</div>
          <div class="pub-authors">A. Zua Caldeira</div>
          <a mat-button routerLink="/publications">View paper</a>
        </div>
        <div class="pub-item">
          <div class="pub-venue">Tool paper (in preparation)</div>
          <div class="pub-title">Reticulate: A Lattice Checker for Session Types</div>
          <div class="pub-authors">A. Zua Caldeira</div>
          <a mat-button routerLink="/publications">View paper</a>
        </div>
        <div class="pub-item">
          <div class="pub-venue">Tool paper (in preparation)</div>
          <div class="pub-title">BICA Reborn: Annotation-Based Session Types for Java Objects</div>
          <div class="pub-authors">A. Zua Caldeira</div>
          <a mat-button routerLink="/publications">View paper</a>
        </div>
      </div>
    </section>

    <!-- How to cite -->
    <section class="cite">
      <h2>How to Cite</h2>
      <div class="cite-box">
        <code>
A. Zua Caldeira. "Session Type State Spaces Form Lattices."
In: ICE 2026 &mdash; Interaction and Concurrency Experience,
DisCoTec Satellite Workshop, Urbino, Italy, June 2026.
        </code>
      </div>
    </section>

    <!-- Tools -->
    <section class="tools-section">
      <h2>Two Implementations</h2>
      <div class="tools-grid">
        <mat-card>
          <mat-card-header>
            <mat-card-title>Reticulate (Python)</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>
              19 modules, 1,406 tests. Parser, state space, lattice checker,
              morphisms, test generation, multiparty projection, recursion analysis,
              Chomsky classification, coverage, visualization.
            </p>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/tools/analyzer">Online analyzer</a>
          </mat-card-actions>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>BICA Reborn (Java)</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>
              7 phases, 1,052 tests. Annotation-based session type checker
              with concurrency analysis, typestate checking, and JUnit 5
              test generation from <code>&#64;Session</code> annotations.
            </p>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/documentation">Documentation</a>
          </mat-card-actions>
        </mat-card>
      </div>
    </section>
  `,
  styles: [`
    /* -------------------------------------------------------- */
    /* Hero — full-width breakout                               */
    /* -------------------------------------------------------- */
    .hero {
      width: 100vw;
      margin-left: calc(-50vw + 50%);
      margin-top: -24px;
      background: linear-gradient(135deg, var(--brand-primary-dark), var(--brand-primary-light));
      color: #fff;
      padding: 64px 24px 48px;
    }

    .hero-inner {
      max-width: 1200px;
      margin: 0 auto;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 48px;
      align-items: center;
    }

    .hero-text h1 {
      font-size: 40px;
      font-weight: 700;
      margin: 0 0 12px;
      line-height: 1.15;
      letter-spacing: -0.5px;
    }

    .hero-sub {
      font-size: 18px;
      font-weight: 400;
      margin: 0 0 16px;
      opacity: 0.95;
      line-height: 1.5;
    }

    .hero-desc {
      font-size: 15px;
      line-height: 1.6;
      margin: 0 0 24px;
      opacity: 0.85;
    }

    .hero-cta {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .hero-btn-primary {
      background: #fff !important;
      color: var(--brand-primary-dark) !important;
      font-weight: 500;
      font-size: 15px;
      padding: 8px 24px;
    }

    .hero-btn-outline {
      border-color: rgba(255, 255, 255, 0.6) !important;
      color: #fff !important;
    }

    .hero-author {
      font-size: 13px;
      opacity: 0.65;
      margin: 16px 0 0;
    }

    .hero-diagram {
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .hero-lattice-svg {
      width: 100%;
      max-width: 340px;
      height: auto;
    }

    @media (max-width: 768px) {
      .hero { padding: 40px 16px 32px; }
      .hero-inner { grid-template-columns: 1fr; gap: 32px; }
      .hero-text h1 { font-size: 28px; }
      .hero-diagram { max-height: 300px; overflow: auto; }
    }

    /* -------------------------------------------------------- */
    /* Stats bar                                                */
    /* -------------------------------------------------------- */
    .stats-bar {
      width: 100vw;
      margin-left: calc(-50vw + 50%);
      background: #f8fafc;
      border-bottom: 1px solid rgba(0,0,0,0.06);
      display: flex;
      justify-content: center;
      gap: 48px;
      padding: 20px 16px;
      flex-wrap: wrap;
    }

    .stat-item {
      text-align: center;
    }

    .stat-value {
      display: block;
      font-size: 22px;
      font-weight: 700;
      color: var(--brand-primary);
    }

    .stat-label {
      font-size: 12px;
      color: rgba(0,0,0,0.5);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    /* -------------------------------------------------------- */
    /* Shared section styles                                    */
    /* -------------------------------------------------------- */
    section:not(.hero):not(.stats-bar) {
      max-width: 960px;
      margin: 0 auto;
      padding: 48px 16px;
    }

    h2 {
      font-size: 24px;
      font-weight: 600;
      margin: 0 0 24px;
      text-align: center;
      color: rgba(0,0,0,0.85);
    }

    /* -------------------------------------------------------- */
    /* Core concept                                             */
    /* -------------------------------------------------------- */
    .concept-content {
      display: grid;
      grid-template-columns: 1.4fr 1fr;
      gap: 40px;
      align-items: flex-start;
    }

    .concept-text p {
      line-height: 1.7;
      margin: 0 0 14px;
      color: rgba(0,0,0,0.75);
    }

    .concept-example {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
    }

    .code-example {
      width: 100%;
      background: #f1f5f9;
      border-radius: 8px;
      padding: 14px 16px;
      border: 1px solid rgba(0,0,0,0.06);
    }

    .code-example.result {
      background: #ecfdf5;
      border-color: #a7f3d0;
    }

    .code-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: rgba(0,0,0,0.45);
      margin-bottom: 6px;
    }

    .code-example code {
      font-size: 13px;
      color: rgba(0,0,0,0.8);
    }

    .arrow-down {
      font-size: 18px;
      color: rgba(0,0,0,0.25);
    }

    @media (max-width: 768px) {
      .concept-content { grid-template-columns: 1fr; }
    }

    /* -------------------------------------------------------- */
    /* Capabilities                                             */
    /* -------------------------------------------------------- */
    .cap-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 20px;
    }

    .cap-card {
      background: #fff;
      border: 1px solid rgba(0,0,0,0.08);
      border-radius: 12px;
      padding: 24px 20px;
      cursor: pointer;
      transition: box-shadow 0.2s, border-color 0.2s;
    }

    .cap-card:hover {
      box-shadow: 0 4px 16px rgba(0,0,0,0.08);
      border-color: var(--brand-primary-light);
    }

    .cap-icon {
      font-size: 28px;
      margin-bottom: 12px;
      color: var(--brand-primary);
    }

    .cap-card h3 {
      font-size: 15px;
      font-weight: 600;
      margin: 0 0 8px;
    }

    .cap-card p {
      font-size: 13px;
      line-height: 1.6;
      color: rgba(0,0,0,0.6);
      margin: 0;
    }

    @media (max-width: 768px) {
      .cap-grid { grid-template-columns: 1fr 1fr; }
    }
    @media (max-width: 480px) {
      .cap-grid { grid-template-columns: 1fr; }
    }

    /* -------------------------------------------------------- */
    /* Live example                                             */
    /* -------------------------------------------------------- */
    .live-example {
      text-align: center;
      background: #f8fafc;
      border-radius: 12px;
      padding: 40px 24px !important;
    }

    .live-desc {
      color: rgba(0,0,0,0.6);
      margin: 0 0 20px;
    }

    .example-buttons {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      justify-content: center;
    }

    /* -------------------------------------------------------- */
    /* Publications                                             */
    /* -------------------------------------------------------- */
    .pub-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .pub-item {
      border: 1px solid rgba(0,0,0,0.08);
      border-radius: 8px;
      padding: 16px 20px;
      background: #fff;
    }

    .pub-venue {
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--brand-primary);
      font-weight: 500;
      margin-bottom: 4px;
    }

    .pub-title {
      font-size: 16px;
      font-weight: 600;
      color: rgba(0,0,0,0.85);
      margin-bottom: 2px;
    }

    .pub-authors {
      font-size: 14px;
      color: rgba(0,0,0,0.55);
      margin-bottom: 8px;
    }

    /* -------------------------------------------------------- */
    /* Citation                                                 */
    /* -------------------------------------------------------- */
    .cite-box {
      background: #f1f5f9;
      border: 1px solid rgba(0,0,0,0.08);
      border-radius: 8px;
      padding: 20px 24px;
      max-width: 640px;
      margin: 0 auto;
    }

    .cite-box code {
      white-space: pre-line;
      font-size: 13px;
      line-height: 1.6;
      color: rgba(0,0,0,0.75);
    }

    /* -------------------------------------------------------- */
    /* Tools section                                            */
    /* -------------------------------------------------------- */
    .tools-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }

    .tools-grid mat-card {
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .tools-grid mat-card-content {
      flex: 1;
    }

    .tools-grid mat-card-content p {
      line-height: 1.6;
      color: rgba(0,0,0,0.7);
    }

    @media (max-width: 768px) {
      .tools-grid { grid-template-columns: 1fr; }
    }
  `],
})
export class HomeComponent implements OnInit {
  readonly stats = signal<HomeStats | null>(null);
  readonly loading = signal(true);
  readonly statsError = signal(false);

  constructor(private api: ApiService) {}

  ngOnInit(): void {
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
      error: () => {
        this.statsError.set(true);
        this.loading.set(false);
      },
    });
  }
}
