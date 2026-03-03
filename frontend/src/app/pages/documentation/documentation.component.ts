import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatExpansionModule } from '@angular/material/expansion';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';

interface TocEntry {
  id: string;
  label: string;
  level: number;
}

@Component({
  selector: 'app-documentation',
  standalone: true,
  imports: [RouterLink, MatExpansionModule, CodeBlockComponent],
  template: `
    <header class="page-header">
      <h1>Documentation</h1>
      <p>Theory and frequently asked questions. Looking for hands-on guides? See the <a routerLink="/tutorials">tutorials</a>.</p>
    </header>

    <div class="doc-layout">
      <!-- Sticky sidebar -->
      <aside class="doc-sidebar">
        <nav class="sidebar-nav">
          <h3>Contents</h3>
          <ul>
            @for (entry of tocEntries; track entry.id) {
              <li [class.sub]="entry.level === 2"
                  [class.active]="activeSection === entry.id">
                <a (click)="scrollTo(entry.id)">{{ entry.label }}</a>
              </li>
            }
          </ul>
        </nav>
      </aside>

      <!-- Main content -->
      <div class="doc-content">

        <!-- ================================================================ -->
        <!-- THEORY                                                           -->
        <!-- ================================================================ -->
        <section class="doc-section" id="theory">
          <h2>Theory</h2>

          <div class="theory-section" id="session-types">
            <h3>Session Types</h3>
            <p>
              A <strong>session type</strong> describes a communication protocol on an object &mdash;
              the legal sequences of method calls, branches, and selections that a client may perform.
              Instead of a flat interface, an object's type evolves as methods are called, enforcing
              protocol compliance at compile time.
            </p>
            <div class="example-card">
              <h4>Example: File Object</h4>
              <app-code-block code="open . rec X . &{read: +{data: X, eof: close . end}}" label="Session type"></app-code-block>
              <p>
                Open the file, then repeatedly read: on <code>data</code>, loop back;
                on <code>eof</code>, close and terminate. The type ensures <code>close</code>
                is always called exactly once.
              </p>
            </div>
          </div>

          <div class="theory-section" id="grammar">
            <h3>Grammar</h3>
            <p>Session types are defined by the following grammar:</p>
            <app-code-block [code]="grammarCode" label="Grammar"></app-code-block>
          </div>

          <div class="theory-section" id="constructors">
            <h3>Constructors</h3>
            <div class="constructors-grid">
              <article class="constructor-card">
                <h4><code>&amp;&#123;...&#125;</code> &mdash; Branch</h4>
                <p>
                  <strong>External choice.</strong> The environment (client) chooses which method
                  to call. Each branch leads to a different continuation type.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>+&#123;...&#125;</code> &mdash; Selection</h4>
                <p>
                  <strong>Internal choice.</strong> The object (server) decides the outcome.
                  The client must handle all possibilities.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>( S1 || S2 )</code> &mdash; Parallel</h4>
                <p>
                  <strong>Concurrent access.</strong> Two execution paths run simultaneously
                  on a shared object. The combined state space is the product lattice.
                  This is the <strong>key novelty</strong> of this work.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>rec X . S</code> &mdash; Recursion</h4>
                <p>
                  <strong>Looping protocols.</strong> The variable <code>X</code> marks the
                  loop point. Well-formed recursive types must have an exit path.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>S1 . S2</code> &mdash; Sequencing</h4>
                <p>
                  <strong>Sequential composition.</strong> Syntactic sugar for a single-method
                  branch: <code>m . S</code> is equivalent to <code>&amp;&#123;m: S&#125;</code>.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>end</code> &mdash; Terminated</h4>
                <p>
                  <strong>Protocol end.</strong> No further operations are allowed. Every
                  well-formed session type must eventually reach <code>end</code>.
                </p>
              </article>
            </div>
          </div>

          <div class="theory-section" id="state-spaces">
            <h3>State Spaces</h3>
            <p>
              Given a session type <code>S</code>, we construct its <strong>state space</strong>
              <code>L(S)</code> &mdash; a directed graph where:
            </p>
            <ul>
              <li><strong>States</strong> are the reachable configurations of the protocol</li>
              <li><strong>Transitions</strong> are labeled edges (method calls, selections)</li>
              <li>The <strong>initial state</strong> (top) is the protocol's entry point</li>
              <li>The <strong>terminal state</strong> (bottom) corresponds to <code>end</code></li>
            </ul>
            <p>
              The <strong>reachability ordering</strong> defines a partial order on states:
              s1 &ge; s2 iff there is a path from s1 to s2.
            </p>
          </div>

          <div class="theory-section" id="lattice-properties">
            <h3>Lattice Properties</h3>
            <p>
              A state space is a <strong>lattice</strong> (a <em>reticulate</em>) if and only if:
            </p>
            <ol>
              <li>There is a <strong>top element</strong> (initial state)</li>
              <li>There is a <strong>bottom element</strong> (terminal state)</li>
              <li>Every pair of states has a <strong>meet</strong> (greatest lower bound)</li>
              <li>Every pair of states has a <strong>join</strong> (least upper bound)</li>
            </ol>
            <p>
              For cyclic state spaces (from recursion), we first <strong>quotient by SCCs</strong>
              to obtain an acyclic DAG, then check lattice properties on the quotient.
            </p>
          </div>

          <div class="theory-section" id="parallel-constructor">
            <h3>The Parallel Constructor</h3>
            <p>
              The <code>&parallel;</code> constructor is the key novelty of this work. When two
              branches execute in parallel on a shared object:
            </p>
            <div class="theory-highlight">
              <code>L(S1 &parallel; S2) = L(S1) &times; L(S2)</code>
            </div>
            <p>
              The product construction orders states componentwise.
              <strong>Crucially</strong>, the product of two lattices is always a lattice.
              This means that any well-formed session type using <code>&parallel;</code>
              <em>necessarily</em> has a lattice state space.
            </p>
          </div>

          <div class="theory-section" id="morphisms">
            <h3>Morphism Hierarchy</h3>
            <p>
              Between session-type state spaces, we define a hierarchy of structure-preserving maps:
            </p>
            <ol>
              <li><strong>Isomorphism</strong> &mdash; bijective, order-preserving and reflecting.</li>
              <li><strong>Embedding</strong> &mdash; injective, order-preserving and reflecting.</li>
              <li><strong>Projection</strong> &mdash; surjective, order-preserving.</li>
              <li><strong>Galois connection</strong> &mdash; an adjunction &alpha;(x) &le; y &hArr; x &le; &gamma;(y).</li>
            </ol>
          </div>
        </section>

        <!-- ================================================================ -->
        <!-- FAQ                                                              -->
        <!-- ================================================================ -->
        <section class="doc-section" id="faq">
          <h2>FAQ</h2>

          <mat-accordion>
            @for (q of faqItems; track q.question) {
              <mat-expansion-panel [expanded]="q.expanded">
                <mat-expansion-panel-header>
                  <mat-panel-title>{{ q.question }}</mat-panel-title>
                </mat-expansion-panel-header>
                <p [innerHTML]="q.answer"></p>
              </mat-expansion-panel>
            }
          </mat-accordion>
        </section>
      </div>
    </div>
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

    /* Sidebar layout */
    .doc-layout {
      display: flex;
      gap: 32px;
      align-items: flex-start;
    }

    .doc-sidebar {
      position: sticky;
      top: 80px;
      width: 240px;
      flex-shrink: 0;
      max-height: calc(100vh - 100px);
      overflow-y: auto;
    }

    .sidebar-nav h3 {
      font-size: 13px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: rgba(0, 0, 0, 0.5);
      margin: 0 0 12px;
      padding: 0 12px;
    }
    .sidebar-nav ul {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .sidebar-nav li {
      margin: 0;
    }
    .sidebar-nav li a {
      display: block;
      padding: 6px 12px;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.7);
      text-decoration: none;
      border-left: 3px solid transparent;
      cursor: pointer;
      transition: all 0.15s;
    }
    .sidebar-nav li.sub a {
      padding-left: 24px;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.55);
    }
    .sidebar-nav li a:hover {
      color: var(--brand-primary, #4338ca);
      background: rgba(67, 56, 202, 0.04);
    }
    .sidebar-nav li.active a {
      color: var(--brand-primary, #4338ca);
      border-left-color: var(--brand-primary, #4338ca);
      font-weight: 500;
      background: rgba(67, 56, 202, 0.06);
    }
    .sidebar-nav li.active.sub a {
      font-weight: 500;
    }

    .doc-content {
      flex: 1;
      min-width: 0;
    }

    @media (max-width: 900px) {
      .doc-layout {
        flex-direction: column;
      }
      .doc-sidebar {
        position: static;
        width: 100%;
        max-height: none;
        border: 1px solid rgba(0, 0, 0, 0.08);
        border-radius: 8px;
        padding: 12px 0;
        background: rgba(0, 0, 0, 0.01);
      }
    }

    /* Content styles */
    .doc-section {
      margin: 40px 0;
    }
    .doc-section h2 {
      font-size: 22px;
      font-weight: 500;
      margin-bottom: 16px;
    }
    .doc-section h3 {
      font-size: 18px;
      font-weight: 500;
      margin: 24px 0 12px;
    }
    .doc-section p {
      line-height: 1.7;
      margin-bottom: 12px;
    }
    .doc-section ul, .doc-section ol {
      line-height: 1.8;
      margin-bottom: 12px;
    }

    .theory-section {
      margin: 24px 0;
    }

    .example-card {
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      padding: 16px;
      margin: 12px 0;
      background: rgba(0, 0, 0, 0.01);
    }
    .example-card h4 {
      font-size: 14px;
      font-weight: 500;
      margin: 0 0 8px;
    }
    .example-card p {
      margin: 8px 0 0;
      font-size: 14px;
    }

    .constructors-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
      margin: 16px 0;
    }
    .constructor-card {
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      padding: 16px;
      background: rgba(0, 0, 0, 0.01);
    }
    .constructor-card h4 {
      font-size: 14px;
      font-weight: 500;
      margin: 0 0 8px;
    }
    .constructor-card p {
      font-size: 14px;
      line-height: 1.6;
      margin: 0;
    }

    .theory-highlight {
      text-align: center;
      font-size: 18px;
      padding: 16px;
      margin: 16px 0;
      background: rgba(67, 56, 202, 0.05);
      border-radius: 8px;
      border: 1px solid rgba(67, 56, 202, 0.15);
    }

  `],
})
export class DocumentationComponent implements OnInit, OnDestroy {
  activeSection = 'theory';
  private sectionIds: string[] = [];

  readonly tocEntries: TocEntry[] = [
    { id: 'theory', label: 'Theory', level: 1 },
    { id: 'session-types', label: 'Session Types', level: 2 },
    { id: 'grammar', label: 'Grammar', level: 2 },
    { id: 'constructors', label: 'Constructors', level: 2 },
    { id: 'state-spaces', label: 'State Spaces', level: 2 },
    { id: 'lattice-properties', label: 'Lattice Properties', level: 2 },
    { id: 'parallel-constructor', label: 'Parallel Constructor', level: 2 },
    { id: 'morphisms', label: 'Morphism Hierarchy', level: 2 },
    { id: 'faq', label: 'FAQ', level: 1 },
  ];

  ngOnInit(): void {
    this.sectionIds = this.tocEntries.map((e) => e.id);
  }

  ngOnDestroy(): void {}

  @HostListener('window:scroll')
  onScroll(): void {
    const offset = 120;
    for (let i = this.sectionIds.length - 1; i >= 0; i--) {
      const el = document.getElementById(this.sectionIds[i]);
      if (el && el.getBoundingClientRect().top <= offset) {
        this.activeSection = this.sectionIds[i];
        return;
      }
    }
    this.activeSection = this.sectionIds[0];
  }

  scrollTo(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }

  readonly grammarCode = `S  ::=  &{ m\u2081 : S\u2081 , \u2026 , m\u2099 : S\u2099 }    \u2014 branch (external choice)
     |  +{ l\u2081 : S\u2081 , \u2026 , l\u2099 : S\u2099 }    \u2014 selection (internal choice)
     |  ( S\u2081 || S\u2082 )                    \u2014 parallel
     |  rec X . S                        \u2014 recursion
     |  X                                \u2014 variable
     |  end                              \u2014 terminated
     |  S\u2081 . S\u2082                          \u2014 sequencing`;

  readonly faqItems = [
    {
      question: 'What is a session type?',
      answer: 'A <strong>session type</strong> describes the communication protocol governing interaction with an object \u2014 the legal sequences of method calls, branches, and selections a client may perform. Unlike a flat interface, a session type makes the object\'s type <em>evolve</em> as methods are called, enforcing protocol compliance <strong>statically</strong>.',
      expanded: true,
    },
    {
      question: 'What is a state space?',
      answer: 'A <strong>state space</strong> <code>L(S)</code> is the labeled transition system obtained by "executing" a session type. Each state represents a protocol stage, each transition a permitted action. It has a unique initial state (top) and terminal state (bottom). The reachability ordering gives it the structure of a bounded lattice (after SCC quotient).',
      expanded: false,
    },
    {
      question: 'What is a bounded lattice?',
      answer: 'A <strong>bounded lattice</strong> is a partially ordered set where every pair has a meet (greatest lower bound) and join (least upper bound), plus top and bottom elements. In session types: top = initial state, bottom = terminal state, meet = convergence point, join = divergence point.',
      expanded: false,
    },
    {
      question: 'What is the parallel constructor?',
      answer: 'The <strong>parallel constructor</strong> <code>S\u2081 \u2225 S\u2082</code> models two sub-protocols executing concurrently on a shared object. Its state space is the product of the two components. This is the <strong>key novelty</strong> \u2014 the product of two lattices is always a lattice, making lattice structure <em>necessary</em>.',
      expanded: false,
    },
    {
      question: 'What is a reticulate?',
      answer: 'A <strong>reticulate</strong> is the bounded lattice formed by the state space of a well-formed session type, after quotienting by SCCs. The Reticulate Theorem proves every well-formed session type produces one \u2014 it is a guaranteed structural property.',
      expanded: false,
    },
    {
      question: 'What is an SCC quotient?',
      answer: 'An <strong>SCC quotient</strong> collapses every strongly connected component into a single node, turning a cyclic graph into a DAG. In session types, cycles from recursion are collapsed, restoring antisymmetry for the partial order.',
      expanded: false,
    },
    {
      question: 'What is top absorption?',
      answer: '<strong>Top absorption</strong> is the key lemma for recursion. It states: if you collapse an upward-closed set containing top into a single element, the result is still a bounded lattice. This has been mechanically verified in Lean 4.',
      expanded: false,
    },
    {
      question: 'What is WF-Par?',
      answer: '<strong>WF-Par</strong> is the well-formedness condition for the parallel constructor. It requires both branches to be terminating, well-formed, variable-disjoint, and free of nested <code>\u2225</code>.',
      expanded: false,
    },
    {
      question: 'What is a morphism between session types?',
      answer: 'A <strong>morphism</strong> is a structure-preserving map between state spaces. The hierarchy: homomorphism (order-preserving), projection (surjective), embedding (injective + reflecting), isomorphism (bijective embedding). Additionally, Galois connections capture approximation relationships.',
      expanded: false,
    },
    {
      question: 'Why do session type state spaces form lattices?',
      answer: 'Because every constructor preserves lattice structure, and the proof goes by structural induction: <code>end</code> is trivial; sequencing adds a maximum; branch/selection create joins and meets; recursion is absorbed by SCC quotient; parallel takes the product. Since every constructor preserves the property and the base case has it, every well-formed session type has it.',
      expanded: false,
    },
  ];
}
