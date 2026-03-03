import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../services/api.service';
import { HomeStats } from '../../models/api.models';
import { HasseDiagramComponent } from '../../components/hasse-diagram/hasse-diagram.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatProgressSpinnerModule, HasseDiagramComponent],
  template: `
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <h1>Session Types as<br>Algebraic Reticulates</h1>
          <p class="author-line">Alexandre Zua Caldeira &middot; Vasco T. Vasconcelos</p>
          <p class="institution-line">LASIGE, Faculty of Sciences, University of Lisbon</p>
          <div class="hero-cta">
            <a mat-flat-button routerLink="/tools/analyzer" class="hero-btn-primary">
              Try the Analyzer
            </a>
            <a mat-stroked-button routerLink="/publications" class="hero-btn-outline">
              Read Papers
            </a>
          </div>
        </div>
        <div class="hero-diagram">
          @if (showcaseSvg()) {
            <app-hasse-diagram [svgHtml]="showcaseSvg()"></app-hasse-diagram>
          }
        </div>
      </div>
      <div class="hero-stats">
        @if (loading()) {
          <mat-spinner diameter="16"></mat-spinner>
          <span>Loading benchmarks&hellip;</span>
        } @else if (stats()) {
          <span>
            {{ stats()!.numBenchmarks }} benchmarks &middot;
            {{ stats()!.totalStates }} states analyzed &middot;
            {{ stats()!.totalTests }} tests generated &middot;
            @if (stats()!.allLattice) {
              All lattices &#x2713;
            } @else {
              Some non-lattices
            }
          </span>
        } @else if (statsError()) {
          <span>Could not load benchmark stats</span>
        }
      </div>
    </section>

    <!-- Abstract -->
    <section class="abstract">
      <h2>Abstract</h2>
      <p>
        Session types describe communication protocols on objects &mdash; the legal
        sequences of method calls, branches, and selections. We prove that the state
        space of any well-formed session type, ordered by reachability, forms a
        <strong>lattice</strong> (which we call a <em>reticulate</em>).
      </p>
      <p>
        The key insight comes from the <strong>parallel constructor</strong>
        (<code>&parallel;</code>), which models concurrent access to a shared object.
        When two execution paths run in parallel, their combined state space is the
        <strong>product lattice</strong> of the individual spaces &mdash; and products
        of lattices are lattices. This makes lattice structure <em>necessary</em>
        rather than merely nice.
      </p>
      <p>
        Building on this foundation, we develop a <strong>morphism hierarchy</strong>
        (isomorphism, embedding, projection, Galois connection) between session-type
        state spaces, connecting to classical results in bisimulation and abstract
        interpretation.
      </p>
    </section>

    <!-- Three pillars -->
    <section class="pillars">
      <h2>Three Pillars</h2>
      <div class="pillars-grid">
        <mat-card>
          <mat-card-header>
            <mat-card-title>Theory</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>
              Prove that session-type state spaces are lattices.
              Develop the morphism hierarchy and connect to bisimulation.
            </p>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/documentation">Explore the theory</a>
          </mat-card-actions>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>Tools &amp; Analyzer</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>
              Python library (Reticulate) and Java toolkit (BICA Reborn) that
              construct state spaces, check lattice properties, and generate tests.
            </p>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/tools/analyzer">Try the tools</a>
          </mat-card-actions>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>BICA Reborn</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>
              Java annotation-based session type checker for objects. Successor
              to the original BICA (2009), with the novel <code>&parallel;</code> constructor.
            </p>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/publications">Read the papers</a>
          </mat-card-actions>
        </mat-card>
      </div>
    </section>

    <!-- CTA -->
    <div class="cta">
      <a mat-flat-button color="primary" routerLink="/tools/analyzer">
        Try the interactive analyzer
      </a>
    </div>
  `,
  styles: [`
    /* Hero — full-width breakout from .main-content */
    .hero {
      width: 100vw;
      margin-left: calc(-50vw + 50%);
      margin-top: -24px;   /* negate parent padding-top */
      background: linear-gradient(135deg, var(--brand-primary-dark), var(--brand-primary-light));
      color: #fff;
      padding: 0 24px 16px;
    }

    .hero-inner {
      max-width: 1200px;
      margin: 0 auto;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
      align-items: center;
    }

    .hero-text h1 {
      font-size: 36px;
      font-weight: 600;
      margin: 0 0 8px;
      line-height: 1.2;
    }

    .hero-text .author-line {
      font-size: 16px;
      margin: 0 0 4px;
      opacity: 0.9;
    }

    .hero-text .institution-line {
      font-size: 14px;
      opacity: 0.7;
      margin: 0 0 16px;
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
    }

    .hero-btn-outline {
      border-color: rgba(255, 255, 255, 0.6) !important;
      color: #fff !important;
    }

    .hero-diagram {
      border-radius: 12px;
      padding: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 200px;
      transform: scale(0.75);
      transform-origin: center center;
    }

    .hero-stats {
      max-width: 1200px;
      margin: 8px auto 0;
      text-align: center;
      font-size: 14px;
      opacity: 0.8;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    /* Responsive: stack on mobile */
    @media (max-width: 768px) {
      .hero {
        padding: 40px 16px 24px;
      }
      .hero-inner {
        grid-template-columns: 1fr;
        gap: 32px;
      }
      .hero-text h1 {
        font-size: 28px;
      }
      .hero-diagram {
        max-height: 300px;
        overflow: auto;
      }
    }

    /* Abstract */
    .abstract {
      max-width: 720px;
      margin: 32px auto;
      padding: 0 16px;
    }
    .abstract h2 {
      font-size: 20px;
      font-weight: 500;
      margin-bottom: 12px;
    }
    .abstract p {
      line-height: 1.7;
      margin-bottom: 12px;
      text-align: justify;
    }

    /* Pillars */
    .pillars {
      margin: 32px 0;
    }
    .pillars h2 {
      text-align: center;
      font-size: 20px;
      font-weight: 500;
      margin-bottom: 24px;
    }
    .pillars-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 24px;
    }
    @media (max-width: 768px) {
      .pillars-grid {
        grid-template-columns: 1fr;
      }
    }
    mat-card {
      height: 100%;
      display: flex;
      flex-direction: column;
    }
    mat-card-content {
      flex: 1;
    }
    mat-card-content p {
      line-height: 1.6;
    }

    /* CTA */
    .cta {
      text-align: center;
      padding: 32px 0 48px;
    }
  `],
})
export class HomeComponent implements OnInit {
  readonly stats = signal<HomeStats | null>(null);
  readonly loading = signal(true);
  readonly statsError = signal(false);
  readonly showcaseSvg = signal('');

  constructor(private api: ApiService) {}

  /**
   * Simplify SVG for hero display: replace box nodes with labeled circles,
   * strip edge labels, recolored white for dark background.
   */
  private simplifyHeroSvg(svg: string): string {
    // Remove graphviz white background polygon
    let result = svg.replace(/<polygon fill="white"[^/]*\/>/,'');

    // Replace node shapes with circles + labels
    result = result.replace(
      /(<g\s+id="node\d+"[^>]*class="node"[^>]*>)([\s\S]*?)(<\/g>)/g,
      (_match, open: string, body: string, close: string) => {
        const textMatch = body.match(/<text[^>]*?\bx="([^"]*)"[^>]*?\by="([^"]*)"[^>]*>([^<]*)<\/text>/);
        if (!textMatch) return open + body + close;

        const cx = parseFloat(textMatch[1]);
        const cy = parseFloat(textMatch[2]) - 5;
        const rawLabel = textMatch[3].trim();

        // Extract display label from node text (e.g. "⊤ hasNext" → "hasNext", "+{TRUE, FALSE}" → "select")
        let label = rawLabel;
        const isTop = rawLabel.includes('\u22A4');
        const isBottom = rawLabel.includes('\u22A5');
        if (isTop) label = 'top';
        else if (isBottom) label = 'bottom';
        else if (rawLabel.startsWith('+{')) label = 'select';
        else if (rawLabel.startsWith('&{')) label = 'branch';

        const titleMatch = body.match(/<title>[^<]*<\/title>/);
        const title = titleMatch ? titleMatch[0] + '\n' : '';

        const nc = 'rgba(255,255,255,0.7)';
        const rebuilt = `${title}<circle cx="${cx}" cy="${cy}" r="16" fill="rgba(255,255,255,0.15)" stroke="${nc}" stroke-width="1.5"/>`
          + `<text x="${cx}" y="${cy + 28}" text-anchor="middle" fill="rgba(255,255,255,0.85)" font-family="Inter,sans-serif" font-size="11">${label}</text>`;
        return open + rebuilt + close;
      }
    );

    // Strip edge labels and recolor edges white
    result = result.replace(
      /(<g\s+id="edge\d+"[^>]*class="edge"[^>]*>)([\s\S]*?)(<\/g>)/g,
      (_match, open: string, body: string, close: string) => {
        let fixed = body;
        const c = 'rgba(255,255,255,0.7)';
        fixed = fixed.replace(/<text[^>]*>[^<]*<\/text>/g, '');
        fixed = fixed.replace(/(<path[^>]*?)stroke="[^"]*"/g, `$1stroke="${c}"`);
        fixed = fixed.replace(/(<polygon[^>]*?)fill="[^"]*"/g, `$1fill="${c}"`);
        fixed = fixed.replace(/(<polygon[^>]*?)stroke="[^"]*"/g, `$1stroke="${c}"`);
        return open + fixed + close;
      }
    );

    return result;
  }

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

        // Hero diagram: Java Iterator (classic recursive session type)
        const showcase = benchmarks.find((b) => b.name === 'Java Iterator')
          ?? benchmarks.find((b) => b.usesParallel && b.numStates >= 5 && b.numStates <= 15 && b.svgHtml)
          ?? benchmarks.find((b) => b.svgHtml)
          ?? null;
        if (showcase?.svgHtml) {
          this.showcaseSvg.set(this.simplifyHeroSvg(showcase.svgHtml));
        }
      },
      error: () => {
        this.statsError.set(true);
        this.loading.set(false);
      },
    });
  }
}
