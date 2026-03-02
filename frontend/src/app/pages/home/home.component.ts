import { Component, OnInit } from '@angular/core';
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
    <!-- Paper-style header -->
    <section class="paper-header">
      <h1>Session Types as Algebraic Reticulates</h1>
      <p class="author-line">Alexandre Zua Caldeira &middot; Vasco T. Vasconcelos</p>
      <p class="institution-line">LASIGE, Faculty of Sciences, University of Lisbon</p>
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

    <!-- Stats line -->
    @if (loading) {
      <div class="stats-line">
        <mat-spinner diameter="18"></mat-spinner>
        <span>Loading benchmarks&hellip;</span>
      </div>
    } @else if (stats) {
      <p class="stats-line">
        {{ stats.numBenchmarks }} benchmarks &middot;
        {{ stats.totalStates }} states analyzed &middot;
        {{ stats.totalTests }} tests generated &middot;
        @if (stats.allLattice) {
          All lattices &#x2713;
        } @else {
          Some non-lattices
        }
      </p>
    } @else if (statsError) {
      <p class="stats-line stats-error">Could not load benchmark stats</p>
    }

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
    .paper-header {
      text-align: center;
      padding: 48px 0 24px;
    }
    .paper-header h1 {
      font-size: 28px;
      font-weight: 500;
      margin: 0 0 12px;
    }
    .author-line {
      font-size: 16px;
      margin: 0 0 4px;
    }
    .institution-line {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.6);
      margin: 0;
    }

    .abstract {
      max-width: 720px;
      margin: 24px auto;
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

    .stats-line {
      text-align: center;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.6);
      padding: 16px 0;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

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

    .cta {
      text-align: center;
      padding: 32px 0 48px;
    }
  `],
})
export class HomeComponent implements OnInit {
  stats: HomeStats | null = null;
  loading = true;
  statsError = false;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getBenchmarks().subscribe({
      next: (benchmarks) => {
        this.stats = {
          numBenchmarks: benchmarks.length,
          totalStates: benchmarks.reduce((sum, b) => sum + b.numStates, 0),
          totalTests: benchmarks.reduce((sum, b) => sum + b.numTests, 0),
          allLattice: benchmarks.every((b) => b.isLattice),
        };
        this.loading = false;
      },
      error: () => {
        this.statsError = true;
        this.loading = false;
      },
    });
  }
}
