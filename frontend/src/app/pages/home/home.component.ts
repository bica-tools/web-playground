import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../services/api.service';
import { HomeStats } from '../../models/api.models';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatProgressSpinnerModule],
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
          <!-- Figure 1: Product lattice for a.b.end ∥ c.d.end (3×3 = 9 states) -->
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 340 440" class="hero-lattice-svg">
            <defs>
              <marker id="ah" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="rgba(255,255,255,0.5)"/>
              </marker>
            </defs>
            <!-- Edges (drawn first, behind nodes) -->
            <g class="edges" stroke="rgba(255,255,255,0.35)" stroke-width="1.2" fill="none" marker-end="url(#ah)">
              <!-- From top (⊤₁,⊤₂) -->
              <line x1="170" y1="38" x2="80" y2="98"/>
              <line x1="170" y1="38" x2="260" y2="98"/>
              <!-- From (sₐ,⊤₂) -->
              <line x1="70" y1="128" x2="30" y2="188"/>
              <line x1="70" y1="128" x2="170" y2="188"/>
              <!-- From (⊤₁,s_c) -->
              <line x1="270" y1="128" x2="170" y2="188"/>
              <line x1="270" y1="128" x2="310" y2="188"/>
              <!-- From (⊥₁,⊤₂) -->
              <line x1="20" y1="218" x2="70" y2="278"/>
              <!-- From (sₐ,s_c) -->
              <line x1="170" y1="218" x2="70" y2="278"/>
              <line x1="170" y1="218" x2="270" y2="278"/>
              <!-- From (⊤₁,⊥₂) -->
              <line x1="320" y1="218" x2="270" y2="278"/>
              <!-- From (⊥₁,s_c) -->
              <line x1="70" y1="308" x2="170" y2="368"/>
              <!-- From (sₐ,⊥₂) -->
              <line x1="270" y1="308" x2="170" y2="368"/>
            </g>
            <!-- Edge labels -->
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
            <!-- Nodes -->
            <g font-family="Inter,sans-serif" font-size="11" text-anchor="middle">
              <!-- Top: (⊤₁, ⊤₂) — blue tint -->
              <circle cx="170" cy="24" r="14" fill="rgba(147,197,253,0.3)" stroke="rgba(191,219,254,0.8)" stroke-width="1.5"/>
              <text x="170" y="58" fill="rgba(255,255,255,0.9)" font-size="10">(⊤₁, ⊤₂)</text>
              <!-- Row 2 left: (sₐ, ⊤₂) -->
              <circle cx="70" cy="114" r="14" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="70" y="144" fill="rgba(255,255,255,0.8)" font-size="10">(sₐ, ⊤₂)</text>
              <!-- Row 2 right: (⊤₁, s_c) -->
              <circle cx="270" cy="114" r="14" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="270" y="144" fill="rgba(255,255,255,0.8)" font-size="10">(⊤₁, s꜀)</text>
              <!-- Row 3 left: (⊥₁, ⊤₂) -->
              <circle cx="20" cy="204" r="14" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="20" y="234" fill="rgba(255,255,255,0.8)" font-size="10">(⊥₁, ⊤₂)</text>
              <!-- Row 3 center: (sₐ, s_c) -->
              <circle cx="170" cy="204" r="14" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="170" y="234" fill="rgba(255,255,255,0.8)" font-size="10">(sₐ, s꜀)</text>
              <!-- Row 3 right: (⊤₁, ⊥₂) -->
              <circle cx="320" cy="204" r="14" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="320" y="234" fill="rgba(255,255,255,0.8)" font-size="10">(⊤₁, ⊥₂)</text>
              <!-- Row 4 left: (⊥₁, s_c) -->
              <circle cx="70" cy="294" r="14" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="70" y="324" fill="rgba(255,255,255,0.8)" font-size="10">(⊥₁, s꜀)</text>
              <!-- Row 4 right: (sₐ, ⊥₂) -->
              <circle cx="270" cy="294" r="14" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="270" y="324" fill="rgba(255,255,255,0.8)" font-size="10">(sₐ, ⊥₂)</text>
              <!-- Bottom: (⊥₁, ⊥₂) — green tint -->
              <circle cx="170" cy="384" r="14" fill="rgba(134,239,172,0.25)" stroke="rgba(187,247,208,0.8)" stroke-width="1.5"/>
              <text x="170" y="414" fill="rgba(255,255,255,0.9)" font-size="10">(⊥₁, ⊥₂)</text>
            </g>
            <!-- Caption -->
            <text x="170" y="438" text-anchor="middle" fill="rgba(255,255,255,0.6)" font-family="Inter,sans-serif" font-size="10">
              a.b.end ∥ c.d.end — 3×3 product lattice
            </text>
          </svg>
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
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 200px;
    }
    .hero-lattice-svg {
      width: 100%;
      max-width: 340px;
      height: auto;
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
