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
          <mat-card-subtitle>PhD Researcher</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>
            PhD student at the Faculty of Sciences, University of Lisbon (FCUL),
            affiliated with LASIGE (Large-Scale Informatics Systems Laboratory).
            Research focus: session types, type theory, and programming language design.
          </p>
          <p>
            The Reticulate project is Alexandre's doctoral research, developing the theory
            and toolchain for session types as algebraic reticulates.
          </p>
          <div class="person-links">
            <a href="https://www.zuacaldeira.com" target="_blank" rel="noopener">Website</a>
            <a href="https://github.com/zuacaldeira" target="_blank" rel="noopener">GitHub</a>
          </div>
        </mat-card-content>
      </mat-card>
    </section>

    <!-- Institution -->
    <section class="about-section">
      <h2>Institution</h2>
      <mat-card>
        <mat-card-header>
          <mat-card-title>LASIGE &mdash; University of Lisbon</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>
            <strong>LASIGE</strong> (Large-Scale Informatics Systems Laboratory) is a research
            unit of the Faculty of Sciences, University of Lisbon (FCUL). Rated "Excellent"
            by the Portuguese Foundation for Science and Technology (FCT), LASIGE conducts
            research in areas including dependable and secure computing, data and systems
            intelligence, and software engineering.
          </p>
          <p>
            The Reticulate project is developed within LASIGE's software engineering and
            programming languages research line.
          </p>
          <div class="person-links">
            <a href="https://www.lasige.pt" target="_blank" rel="noopener">LASIGE</a>
            <a href="https://ciencias.ulisboa.pt" target="_blank" rel="noopener">FCUL</a>
            <a href="https://www.ulisboa.pt" target="_blank" rel="noopener">University of Lisbon</a>
          </div>
        </mat-card-content>
      </mat-card>
    </section>

    <!-- Project -->
    <section class="about-section">
      <h2>The Project</h2>
      <p>
        <strong>Reticulate</strong> is a research project with three pillars:
      </p>
      <ol class="pillars-list">
        <li>
          <strong>Theory</strong> &mdash; Proving that session-type state spaces are lattices;
          developing the morphism hierarchy (isomorphism, embedding, projection, Galois connection);
          connecting to bisimulation and abstract interpretation.
        </li>
        <li>
          <strong>Reticulate (tool)</strong> &mdash; A Python library and web tool that constructs
          state spaces from session type definitions, checks lattice properties, computes
          morphisms, and visualizes Hasse diagrams.
        </li>
        <li>
          <strong>BICA Reborn</strong> &mdash; A Java annotation-based session type checker for
          objects, successor to the original BICA (2009). The key novelty is the
          <code>&parallel;</code> (parallel) constructor for concurrent access, which forces
          lattice structure.
        </li>
      </ol>
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
      font-weight: 500;
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
      font-weight: 500;
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
  `],
})
export class AboutComponent {}
