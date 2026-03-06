import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';

@Component({
  selector: 'app-publications',
  standalone: true,
  imports: [RouterLink, CodeBlockComponent],
  template: `
    <header class="page-header">
      <h1>Publications</h1>
      <p>Papers, slides, and formal definitions from the Reticulate project.</p>
    </header>

    <h2>Research Papers</h2>
    <ol class="pub-list">
      <li>
        <span class="pub-title">Session Type State Spaces Form Lattices</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Target: CONCUR 2026, Liverpool, Sep 1&ndash;4</em><br>
        <span class="pub-links">
          [<a href="/papers/step5-lattice.pdf" target="_blank" rel="noopener">PDF</a>]
          [<a routerLink="/tools/analyzer">Live Demo</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">Session Type State Spaces Form Lattices (Extended Abstract)</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">ICE 2026, Urbino, Jun 12 (DisCoTec satellite workshop)</em><br>
        <span class="pub-links">
          [<a href="/papers/ice-2026.pdf" target="_blank" rel="noopener">PDF</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">Reticulate: A Tool for Lattice Analysis of Session Type State Spaces</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">ICE 2026 Oral Communication, Urbino, Jun 12</em><br>
        <span class="pub-links">
          [<a href="/papers/ice-2026-oral.pdf" target="_blank" rel="noopener">PDF</a>]
          [<a routerLink="/tools/analyzer">Live Demo</a>]
        </span>
      </li>
    </ol>

    <h2>Tool Papers</h2>
    <ol class="pub-list">
      <li>
        <span class="pub-title">Reticulate: A Lattice-Theoretic Toolkit for Session Types</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Target: TACAS 2027 (~Oct 2026) &mdash; Python library, 9 modules, 383 tests</em><br>
        <span class="pub-links">
          [<a href="/papers/reticulate-tool.pdf" target="_blank" rel="noopener">PDF</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">BICA Reborn: Annotation-Based Session Type Checking for Java</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Target: OOPSLA 2027 R1 (~Oct 2026) &mdash; Java 21, 13 packages, 1,052 tests</em><br>
        <span class="pub-links">
          [<a href="/papers/bica-reborn.pdf" target="_blank" rel="noopener">PDF</a>]
        </span>
      </li>
    </ol>

    <h2>Supplementary Materials</h2>
    <ol class="pub-list">
      <li>
        <span class="pub-title">The Session Type Verification Pipeline</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Pipeline walkthrough &mdash; 7 stages, error catalogue, examples</em><br>
        <span class="pub-links">
          [<a href="/papers/pipeline-article.pdf" target="_blank" rel="noopener">PDF</a>]
          [<a routerLink="/pipeline">Web version</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">Session Types as Algebraic Reticulates &mdash; Presentation</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Research presentation &mdash; Beamer, 18 slides</em><br>
        <span class="pub-links">
          [<a href="/papers/slides.pdf" target="_blank" rel="noopener">Slides (PDF)</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">Formal Definitions</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Glossary &mdash; 40+ definitions, internal reference document</em><br>
        <span class="pub-links">
          [<a href="/papers/definitions.pdf" target="_blank" rel="noopener">PDF</a>]
        </span>
      </li>
    </ol>

    <section class="bibtex-section">
      <h2>BibTeX</h2>
      <app-code-block [code]="bibtex" label="BibTeX"></app-code-block>
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

    h2 {
      font-size: 20px;
      font-weight: 500;
      margin: 32px 0 16px;
    }

    .pub-list {
      list-style: decimal;
      padding-left: 24px;
    }
    .pub-list li {
      margin-bottom: 24px;
      line-height: 1.6;
    }
    .pub-title {
      font-weight: 500;
      font-size: 16px;
    }
    .badge-draft {
      display: inline-block;
      padding: 2px 8px;
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      background: #fff3e0;
      color: #e65100;
      border-radius: 4px;
      margin-left: 8px;
      vertical-align: middle;
    }
    .pub-authors {
      color: rgba(0, 0, 0, 0.7);
      font-size: 14px;
    }
    .pub-venue {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.6);
    }
    .pub-links {
      font-size: 14px;
    }
    .pub-links a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
    }
    .pub-links a:hover {
      text-decoration: underline;
    }

    .bibtex-section {
      margin: 32px 0;
    }
  `],
})
export class PublicationsComponent {
  readonly bibtex = `@inproceedings{caldeira2026reticulate,
  author       = {Caldeira, Alexandre Zua},
  title        = {Session Type State Spaces Form Lattices},
  year         = {2026},
  note         = {Draft --- session types as algebraic reticulates}
}`;
}
