import { Component, signal, computed } from '@angular/core';
import { NgClass, NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';

type PubStatus = 'draft' | 'submitted' | 'accepted' | 'published';
type PubCategory = 'venue' | 'step' | 'tool' | 'reference';

interface Publication {
  title: string;
  category: PubCategory;
  status: PubStatus;
  venue?: string;
  deadline?: string;
  description: string;
  steps?: string[];
  pdfPath?: string;
  demoRoute?: string;
  webRoute?: string;
}

@Component({
  selector: 'app-publications',
  standalone: true,
  imports: [NgClass, NgTemplateOutlet, RouterLink, CodeBlockComponent],
  template: `
    <header class="page-header">
      <h1>Publications</h1>
      <p>Venue submissions, step documentation, tool papers, and reference materials.</p>
    </header>

    <!-- Summary counters -->
    <section class="summary-bar">
      <div class="sum-item">
        <span class="sum-value sum-step">{{ counts().step }}</span>
        <span class="sum-label">Step papers</span>
      </div>
      <div class="sum-item">
        <span class="sum-value sum-tool">{{ counts().tool }}</span>
        <span class="sum-label">Tool papers</span>
      </div>
      <div class="sum-item">
        <span class="sum-value sum-ref">{{ counts().reference }}</span>
        <span class="sum-label">Reference</span>
      </div>
      <div class="sum-item">
        <span class="sum-value">{{ counts().total }}</span>
        <span class="sum-label">Total</span>
      </div>
    </section>

    <!-- Filter chips -->
    <div class="filter-bar">
      <span class="filter-label">Filter:</span>
      @for (cat of categories; track cat.id) {
        <button class="filter-chip"
                [class.active]="activeFilter() === cat.id"
                (click)="setFilter(cat.id)">{{ cat.label }}</button>
      }
    </div>

    <!-- Step Papers -->
    @if (activeFilter() === 'all' || activeFilter() === 'step') {
      @if (stepItems().length) {
        <section class="pub-section">
          <h2>Step Papers</h2>
          <p class="section-desc">Educational write-ups documenting each research step with formal syntax, semantics, and proofs.</p>
          <div class="pub-grid">
            @for (p of stepItems(); track p.title) {
              <ng-container *ngTemplateOutlet="pubCard; context: { $implicit: p }"></ng-container>
            }
          </div>
        </section>
      }
    }

    <!-- Tool Papers -->
    @if (activeFilter() === 'all' || activeFilter() === 'tool') {
      @if (toolItems().length) {
        <section class="pub-section">
          <h2>Tool Papers</h2>
          <p class="section-desc">Implementation-focused papers describing the software artifacts.</p>
          <div class="pub-grid">
            @for (p of toolItems(); track p.title) {
              <ng-container *ngTemplateOutlet="pubCard; context: { $implicit: p }"></ng-container>
            }
          </div>
        </section>
      }
    }

    <!-- Reference Materials -->
    @if (activeFilter() === 'all' || activeFilter() === 'reference') {
      @if (referenceItems().length) {
        <section class="pub-section">
          <h2>Reference Materials</h2>
          <p class="section-desc">Slides, glossaries, and supporting documents.</p>
          <div class="pub-grid">
            @for (p of referenceItems(); track p.title) {
              <ng-container *ngTemplateOutlet="pubCard; context: { $implicit: p }"></ng-container>
            }
          </div>
        </section>
      }
    }

    <!-- Card template -->
    <ng-template #pubCard let-p>
      <div class="pub-card">
        <div class="card-top">
          <span class="status-badge" [ngClass]="'status-' + p.status">{{ statusLabel(p.status) }}</span>
          <span class="cat-badge" [ngClass]="'cat-' + p.category">{{ catLabel(p.category) }}</span>
        </div>
        <h3 class="card-title">{{ p.title }}</h3>
        <p class="card-desc">{{ p.description }}</p>
        @if (p.venue) {
          <div class="card-venue">{{ p.venue }}</div>
        }
        @if (p.deadline) {
          <div class="card-deadline">Deadline: {{ p.deadline }}</div>
        }
        @if (p.steps && p.steps.length) {
          <div class="card-steps">
            @for (s of p.steps; track s) {
              <span class="step-tag">Step {{ s }}</span>
            }
          </div>
        }
        <div class="card-links">
          @if (p.pdfPath) {
            <a [href]="p.pdfPath" target="_blank" rel="noopener" class="link-btn">PDF</a>
          }
          @if (p.demoRoute) {
            <a [routerLink]="p.demoRoute" class="link-btn link-demo">Live Demo</a>
          }
          @if (p.webRoute) {
            <a [routerLink]="p.webRoute" class="link-btn link-web">Web Version</a>
          }
        </div>
      </div>
    </ng-template>

    <!-- BibTeX -->
    <section class="bibtex-section">
      <h2>BibTeX</h2>
      <app-code-block [code]="bibtex" label="BibTeX"></app-code-block>
    </section>
  `,
  styles: [`
    :host { display: block; }

    .page-header { padding: 24px 0 16px; }
    .page-header h1 { font-size: 24px; font-weight: 600; margin: 0 0 8px; }
    .page-header p { color: rgba(0,0,0,0.6); margin: 0; }

    /* Summary bar */
    .summary-bar {
      background: #fafbfc; border-bottom: 1px solid rgba(0,0,0,0.06);
      display: flex; justify-content: center; gap: 40px;
      padding: 18px 16px; flex-wrap: wrap; margin: 0 0 8px;
    }
    .sum-item { text-align: center; }
    .sum-value {
      display: block; font-size: 22px; font-weight: 700;
      font-family: 'JetBrains Mono', monospace;
    }
    .sum-label {
      font-size: 11px; color: rgba(0,0,0,0.45);
      text-transform: uppercase; letter-spacing: 0.6px;
    }
    .sum-venue { color: #7c3aed; }
    .sum-step { color: #059669; }
    .sum-tool { color: #0284c7; }
    .sum-ref { color: #d97706; }

    /* Filter chips */
    .filter-bar {
      display: flex; gap: 8px; flex-wrap: wrap; align-items: center;
      padding: 16px 0;
    }
    .filter-label {
      font-size: 12px; text-transform: uppercase; letter-spacing: 0.6px;
      color: rgba(0,0,0,0.4); font-weight: 500;
    }
    .filter-chip {
      padding: 5px 14px; border: 1px solid rgba(0,0,0,0.12); border-radius: 20px;
      background: transparent; font-size: 12px; color: rgba(0,0,0,0.6);
      cursor: pointer; transition: all 0.15s; font-family: inherit;
    }
    .filter-chip:hover { border-color: var(--brand-primary); color: var(--brand-primary); }
    .filter-chip.active {
      background: var(--brand-primary); border-color: var(--brand-primary);
      color: #fff; font-weight: 500;
    }

    /* Section headers */
    .pub-section { margin: 32px 0 0; }
    .pub-section h2 { font-size: 20px; font-weight: 600; margin: 0 0 4px; }
    .section-desc { font-size: 13px; color: rgba(0,0,0,0.5); margin: 0 0 16px; }

    /* Card grid */
    .pub-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
      gap: 16px;
    }

    .pub-card {
      border: 1px solid rgba(0,0,0,0.08); border-radius: 10px;
      padding: 20px; background: #fff;
      transition: border-color 0.15s, box-shadow 0.15s;
      display: flex; flex-direction: column;
    }
    .pub-card:hover {
      border-color: rgba(67,56,202,0.2);
      box-shadow: 0 2px 12px rgba(67,56,202,0.06);
    }

    .card-top { display: flex; gap: 8px; margin-bottom: 10px; }

    /* Status badges */
    .status-badge {
      display: inline-block; padding: 2px 8px; border-radius: 4px;
      font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px;
    }
    .status-draft { background: #fff3e0; color: #e65100; }
    .status-submitted { background: #e0f2fe; color: #075985; }
    .status-accepted { background: #ecfdf5; color: #065f46; }
    .status-published { background: #ede9fe; color: #5b21b6; }

    /* Category badges */
    .cat-badge {
      display: inline-block; padding: 2px 8px; border-radius: 4px;
      font-size: 11px; font-weight: 500; letter-spacing: 0.3px;
    }
    .cat-venue { background: #f3e8ff; color: #7c3aed; }
    .cat-step { background: #ecfdf5; color: #059669; }
    .cat-tool { background: #e0f2fe; color: #0284c7; }
    .cat-reference { background: #fef3c7; color: #92400e; }

    .card-title { font-size: 15px; font-weight: 600; margin: 0 0 6px; color: rgba(0,0,0,0.85); }
    .card-desc { font-size: 13px; color: rgba(0,0,0,0.55); line-height: 1.5; margin: 0 0 10px; flex: 1; }
    .card-venue { font-size: 13px; color: rgba(0,0,0,0.6); font-style: italic; margin-bottom: 4px; }
    .card-deadline { font-size: 12px; color: #d97706; font-weight: 500; margin-bottom: 8px; }

    .card-steps { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 10px; }
    .step-tag {
      padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 500;
      background: rgba(67,56,202,0.06); color: var(--brand-primary);
    }

    .card-links { display: flex; gap: 8px; margin-top: auto; padding-top: 10px; }
    .link-btn {
      padding: 4px 12px; border-radius: 6px; font-size: 12px; font-weight: 500;
      text-decoration: none; border: 1px solid rgba(0,0,0,0.12);
      color: var(--brand-primary, #4338ca); transition: all 0.15s;
    }
    .link-btn:hover { background: rgba(67,56,202,0.05); border-color: var(--brand-primary); }
    .link-demo { border-color: #059669; color: #059669; }
    .link-demo:hover { background: rgba(5,150,105,0.05); }
    .link-web { border-color: #0284c7; color: #0284c7; }
    .link-web:hover { background: rgba(2,132,199,0.05); }

    .bibtex-section { margin: 40px 0 24px; }
    .bibtex-section h2 { font-size: 20px; font-weight: 600; margin-bottom: 12px; }

    @media (max-width: 600px) {
      .pub-grid { grid-template-columns: 1fr; }
      .summary-bar { gap: 24px; }
    }
  `],
})
export class PublicationsComponent {
  readonly activeFilter = signal<string>('all');

  readonly categories = [
    { id: 'all', label: 'All' },
    { id: 'step', label: 'Step Papers' },
    { id: 'tool', label: 'Tool Papers' },
    { id: 'reference', label: 'Reference' },
  ];

  readonly publications: Publication[] = [
    // Venue submissions hidden during double-blind review (ICE 2026, Apr 2)
    // Re-add after review results: CONCUR, ICE x3, EPTCS composition
    // ── Step Papers ──
    {
      title: 'Step 1: State Spaces as Posets',
      category: 'step', status: 'draft',
      description: 'Builds finite state machines from session type ASTs. Proves reachability yields a preorder; SCC quotient gives a partial order.',
      steps: ['1'],
      pdfPath: '/papers/step1-statespace.pdf',
    },
    {
      title: 'Step 2: Benchmarks and Empirical Validation',
      category: 'step', status: 'draft',
      description: '79 binary + 24 multiparty benchmark protocols. All terminating types form lattices empirically.',
      steps: ['2', '6'],
      pdfPath: '/papers/step2-benchmarks.pdf',
    },
    {
      title: 'Step 3: Branching and Meet Operations',
      category: 'step', status: 'draft',
      description: 'Meet and join computation on session-type state spaces. Branch preserves lattice structure.',
      steps: ['3'],
      pdfPath: '/papers/step3-branching-meet.pdf',
    },
    {
      title: 'Step 155b: Polarity and Formal Concept Analysis',
      category: 'step', status: 'draft',
      description: 'Galois connection between state closures and label closures. Birkhoff\u2019s fundamental theorem applied to session types.',
      steps: ['155b'],
      pdfPath: '/papers/step155b-polarity.pdf',
    },
    {
      title: 'Step 156: Realizability Characterisation',
      category: 'step', status: 'draft',
      description: 'A bounded lattice is realizable as a session type iff it has reticular form. Forward and backward direction proofs.',
      steps: ['156'],
      pdfPath: '/papers/step156-realizability.pdf',
    },
    {
      title: 'Step 157a: Channel Duality',
      category: 'step', status: 'draft',
      description: 'Ch(S) = L(S) \u00D7 L(dual(S)) is a bounded lattice with role embeddings and branch complementarity.',
      steps: ['157a'],
      pdfPath: '/papers/step157a-channel-duality.pdf',
    },
    {
      title: 'Step 157b: Asynchronous Channels',
      category: 'step', status: 'draft',
      description: 'Four-tuple state model for async channels. Half-duplex invariant and sync embedding.',
      steps: ['157b'],
      pdfPath: '/papers/step157b-async-channels.pdf',
    },
    {
      title: 'Step 13a: Unfolded Lattices',
      category: 'step', status: 'draft',
      description: 'Recursive type unfolding to finite depth. SCC analysis and quotient construction.',
      steps: ['13'],
      pdfPath: '/papers/step13a-unfolded-lattice.pdf',
    },
    {
      title: 'Step 163: The SessLat Category',
      category: 'step', status: 'draft',
      description: 'Session-type state spaces and lattice morphisms form a category. Identity, composition, associativity.',
      steps: ['163'],
      pdfPath: '/papers/step163-category.pdf',
    },
    {
      title: 'Step 164: Coproducts in SessLat',
      category: 'step', status: 'draft',
      description: 'Coproduct construction for session-type lattices with universal property.',
      steps: ['164'],
      pdfPath: '/papers/step164-coproduct.pdf',
    },
    {
      title: 'Step 165: Equalizers in SessLat',
      category: 'step', status: 'draft',
      description: 'Equalizer construction for session-type lattice morphisms.',
      steps: ['165'],
      pdfPath: '/papers/step165-equalizer.pdf',
    },
    {
      title: 'Step 166: Coequalizers in SessLat',
      category: 'step', status: 'draft',
      description: 'Coequalizer construction for session-type lattice morphisms.',
      steps: ['166'],
      pdfPath: '/papers/step166-coequalizer.pdf',
    },
    {
      title: 'Step 200a: \u03BB_S\u2070 \u2014 Basic Typestate Checking',
      category: 'step', status: 'draft',
      description: 'Progress and preservation for straight-line session-typed object calculus.',
      steps: ['200b'],
      pdfPath: '/papers/step200a-lambda-s0.pdf',
    },
    {
      title: 'Step 200d: \u03BB_S\u00B9 \u2014 Return Types',
      category: 'step', status: 'draft',
      description: 'Extension with method signatures and base-type return values. Conservative extension of \u03BB_S\u2070.',
      steps: ['200d'],
      pdfPath: '/papers/step200d-lambda-s1.pdf',
    },
    {
      title: 'Step 200e\u2013g: \u03BB_S Unified',
      category: 'step', status: 'draft',
      description: 'Unified lambda-S formulation combining branching, concurrency, and recursion extensions.',
      steps: ['200e', '200f', '200g'],
      pdfPath: '/papers/step200-lambda-s-unified.pdf',
    },
    // ── Tool Papers ──
    {
      title: 'Reticulate: A Lattice-Theoretic Toolkit for Session Types',
      category: 'tool', status: 'draft',
      venue: 'Target: TACAS 2027 (~Oct 2026)',
      description: 'Python library: parser, state-space builder, lattice checker, morphism hierarchy, test generator. 19 modules, 2,400+ tests.',
      pdfPath: '/papers/reticulate-tool.pdf',
    },
    {
      title: 'BICA Reborn: Annotation-Based Session Type Checking for Java',
      category: 'tool', status: 'draft',
      venue: 'Target: OOPSLA 2027 R1 (~Oct 2026)',
      description: 'Java 21 annotation processor: @Session types, compile-time checking, test generation. 13 packages, 1,600+ tests.',
      pdfPath: '/papers/bica-reborn.pdf',
    },
    {
      title: 'The Session Type Verification Pipeline',
      category: 'tool', status: 'draft',
      venue: 'Target: ACM SIGPLAN (Practitioner\'s report)',
      description: '7-stage pipeline walkthrough: Parse \u2192 Termination \u2192 WF-Par \u2192 State Space \u2192 Conformance \u2192 Lattice \u2192 Thread Safety.',
      pdfPath: '/papers/pipeline-article.pdf',
      webRoute: '/pipeline',
    },
    // ── Reference Materials ──
    {
      title: 'Session Types as Algebraic Reticulates \u2014 Presentation',
      category: 'reference', status: 'draft',
      description: 'Beamer presentation, 18 slides. Research overview for seminars and conferences.',
      pdfPath: '/papers/slides.pdf',
    },
    {
      title: 'Formal Definitions Glossary',
      category: 'reference', status: 'draft',
      description: '40+ formal definitions: session types, state spaces, lattice properties, morphisms, duality.',
      pdfPath: '/papers/definitions.pdf',
    },
    {
      title: 'Lattice Types \u2014 Internal Note',
      category: 'reference', status: 'draft',
      description: 'Lattice theory background: distributivity, modularity, N\u2085/M\u2083 counterexamples.',
      pdfPath: '/papers/lattice-types-note.pdf',
    },
    {
      title: 'Termination Discussion',
      category: 'reference', status: 'draft',
      description: 'Deep dive into decidable termination checking for recursive session types.',
      pdfPath: '/papers/termination-discussion.pdf',
    },
    {
      title: 'The Wait Construct',
      category: 'reference', status: 'draft',
      description: 'Specification of the wait construct for synchronising parallel branches.',
      pdfPath: '/papers/wait-construct.pdf',
    },
  ];

  readonly counts = computed(() => {
    const all = this.publications;
    return {
      total: all.length,
      step: all.filter(p => p.category === 'step').length,
      tool: all.filter(p => p.category === 'tool').length,
      reference: all.filter(p => p.category === 'reference').length,
    };
  });

  readonly stepItems = computed(() => this.filterBy('step'));
  readonly toolItems = computed(() => this.filterBy('tool'));
  readonly referenceItems = computed(() => this.filterBy('reference'));

  setFilter(id: string): void {
    this.activeFilter.set(id);
  }

  statusLabel(status: PubStatus): string {
    return { draft: 'Draft', submitted: 'Submitted', accepted: 'Accepted', published: 'Published' }[status];
  }

  catLabel(category: PubCategory): string {
    return { venue: 'Venue', step: 'Step Paper', tool: 'Tool', reference: 'Reference' }[category];
  }

  private filterBy(cat: PubCategory): Publication[] {
    const f = this.activeFilter();
    if (f !== 'all' && f !== cat) return [];
    return this.publications.filter(p => p.category === cat);
  }

  readonly bibtex = `@inproceedings{caldeira2026reticulate,
  author       = {Caldeira, Alexandre Zua},
  title        = {Session Type State Spaces Form Lattices},
  year         = {2026},
  note         = {Draft --- session types as algebraic reticulates}
}`;
}
