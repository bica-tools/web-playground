import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <header class="page-header">
      <h1>About</h1>
      <p>The people and institutions behind the Reticulate project.</p>
    </header>

    <!-- Author -->
    <section class="about-section">
      <h2>Author</h2>
      <mat-card>
        <mat-card-header>
          <mat-card-title>Alexandre Zua Caldeira</mat-card-title>
          <mat-card-subtitle>Independent Researcher</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>
            Independent researcher based in Berlin, Germany.
            Research focus: session types, type theory, and programming language design.
          </p>
          <p>
            The Reticulate project develops the theory
            and toolchain for session types as algebraic reticulates.
          </p>
          <div class="person-links">
            <a href="https://www.zuacaldeira.com" target="_blank" rel="noopener">Website</a>
            <a href="https://github.com/zuacaldeira" target="_blank" rel="noopener">GitHub</a>
          </div>
        </mat-card-content>
      </mat-card>
    </section>

    <!-- Project -->
    <section class="about-section">
      <h2>The Project</h2>
      <p>
        <strong>Session Types as Algebraic Reticulates</strong> is a research project with three pillars:
      </p>
      <ol class="pillars-list">
        <li>
          <strong>Theory</strong> &mdash; Proving that session-type state spaces are lattices;
          developing the morphism hierarchy (isomorphism, embedding, projection, Galois connection);
          connecting to bisimulation and abstract interpretation. Two key lemmas formally
          verified in Lean 4 with zero sorry.
        </li>
        <li>
          <strong>Reticulate</strong> &mdash; A Python library (9 modules, 789 tests) that constructs
          state spaces from session type definitions, checks lattice properties, computes
          morphisms, generates tests, and visualizes Hasse diagrams.
        </li>
        <li>
          <strong>BICA Reborn</strong> &mdash; A Java 21 annotation-based session type checker
          (13 packages, 1,052 tests), successor to the original BICA (2009). Key novelty: the
          <code>&parallel;</code> (parallel) constructor for concurrent access, which forces
          lattice structure.
        </li>
      </ol>
    </section>

    <!-- By the numbers -->
    <section class="about-section">
      <h2>By the Numbers</h2>
      <div class="numbers-grid">
        <div class="number-item"><span class="number-value">1,841</span><span class="number-label">Total tests</span></div>
        <div class="number-item"><span class="number-value">34</span><span class="number-label">Benchmark protocols</span></div>
        <div class="number-item"><span class="number-value">5,183</span><span class="number-label">Generated JUnit tests</span></div>
        <div class="number-item"><span class="number-value">2</span><span class="number-label">Lean 4 proofs (0 sorry)</span></div>
        <div class="number-item"><span class="number-value">7</span><span class="number-label">Pipeline stages</span></div>
        <div class="number-item"><span class="number-value">6</span><span class="number-label">Papers in progress</span></div>
      </div>
    </section>

    <!-- Contact -->
    <section class="about-section">
      <h2>Contact</h2>
      <p>
        For questions about the research or collaboration inquiries, please
        reach out via the institutional channels listed above or through the
        <a href="https://github.com/zuacaldeira/SessionTypesResearch" target="_blank" rel="noopener">GitHub repository</a>.
      </p>
    </section>
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

    .about-section {
      margin: 32px 0;
    }
    .about-section h2 {
      font-size: 20px;
      font-weight: 600;
      margin-bottom: 16px;
    }
    .about-section p {
      line-height: 1.7;
      margin-bottom: 12px;
    }

    mat-card {
      margin-bottom: 16px;
    }
    mat-card-content p {
      line-height: 1.7;
    }

    .person-links {
      display: flex;
      gap: 16px;
      margin-top: 12px;
    }
    .person-links a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
      font-size: 14px;
    }
    .person-links a:hover {
      text-decoration: underline;
    }

    .pillars-list {
      padding-left: 24px;
    }
    .pillars-list li {
      margin-bottom: 12px;
      line-height: 1.7;
    }

    .numbers-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 16px;
    }
    .number-item {
      text-align: center;
      padding: 20px 12px;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      background: rgba(0, 0, 0, 0.01);
    }
    .number-value {
      display: block;
      font-size: 28px;
      font-weight: 700;
      color: var(--brand-primary, #4338ca);
    }
    .number-label {
      display: block;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.55);
      margin-top: 4px;
    }
  `],
})
export class AboutComponent {}
