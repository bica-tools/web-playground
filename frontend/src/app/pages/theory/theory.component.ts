import { Component, signal, computed } from '@angular/core';
import { NgClass } from '@angular/common';

interface TheoremEntry {
  step: string;
  name: string;
  description: string;
  category: string;
  status: 'proved' | 'mechanised' | 'implemented' | 'partial' | 'open';
  paper: boolean;
  lean: boolean | 'partial';
  reticulate: boolean;
  bica: boolean;
  leanFile?: string;
  reticulateModule?: string;
  bicaPackage?: string;
}

@Component({
  selector: 'app-theory',
  standalone: true,
  imports: [NgClass],
  templateUrl: './theory.component.html',
  styleUrl: './theory.component.scss',
})
export class TheoryComponent {
  readonly activeFilter = signal<string>('all');

  readonly theorems: TheoremEntry[] = [
    // ── Ground Truth (Steps 1–6) ──
    {
      step: '1', name: 'State spaces are posets',
      description: 'Reachability on session-type state spaces is a preorder; SCC quotient yields a partial order.',
      category: 'ground-truth', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'SCC.lean', reticulateModule: 'statespace.py', bicaPackage: 'statespace',
    },
    {
      step: '2a', name: 'End produces a bounded lattice',
      description: 'L(end) is a singleton bounded lattice — the base case.',
      category: 'ground-truth', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'EndLemma.lean', reticulateModule: 'lattice.py', bicaPackage: 'lattice',
    },
    {
      step: '2b', name: 'Branch preserves bounded lattice',
      description: 'Adding a new bottom (method call) to a bounded lattice yields a bounded lattice.',
      category: 'ground-truth', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'BranchLemma.lean', reticulateModule: 'lattice.py', bicaPackage: 'lattice',
    },
    {
      step: '2c', name: 'Parallel produces product lattice',
      description: 'L(S\u2081 \u2225 S\u2082) = L(S\u2081) \u00D7 L(S\u2082) ordered componentwise is a bounded lattice.',
      category: 'ground-truth', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'ParallelLemma.lean', reticulateModule: 'product.py', bicaPackage: 'statespace',
    },
    {
      step: '2d', name: 'Bottom absorption preserves lattice',
      description: 'Collapsing a downward-closed set containing \u22A5 preserves bounded lattice structure.',
      category: 'ground-truth', status: 'mechanised',
      paper: true, lean: true, reticulate: false, bica: false,
      leanFile: 'BottomAbsorption.lean',
    },
    {
      step: '2e', name: 'Recursion preserves bounded lattice',
      description: 'SCC quotient of recursive type\'s state space is a bounded lattice via bottom absorption.',
      category: 'ground-truth', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'RecursionLemma.lean', reticulateModule: 'lattice.py', bicaPackage: 'lattice',
    },
    {
      step: '2', name: 'Reticulate Theorem',
      description: 'Every well-formed session type\'s state space (SCC quotient) is a bounded lattice.',
      category: 'ground-truth', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'ReticulateTheorem.lean', reticulateModule: 'lattice.py', bicaPackage: 'lattice',
    },
    {
      step: '6', name: 'Universality (exhaustive enumeration)',
      description: 'All terminating types at depth 3 form lattices; counterexamples only from non-terminating types.',
      category: 'ground-truth', status: 'implemented',
      paper: true, lean: false, reticulate: true, bica: true,
      reticulateModule: 'enumerate_types.py', bicaPackage: 'enumeration',
    },
    // ── Morphism Hierarchy (Steps 7–10) ──
    {
      step: '7', name: 'Gay\u2013Hole subtyping \u21D4 lattice embedding',
      description: 'S\u2081 \u2264 S\u2082 iff L(S\u2082) embeds in L(S\u2081). Functor is faithful but not full.',
      category: 'morphism', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'Subtyping.lean', reticulateModule: 'subtyping.py', bicaPackage: 'subtyping',
    },
    {
      step: '8', name: 'Duality is an involution',
      description: 'dual(dual(S)) = S. Duality preserves state-space isomorphism and reverses subtyping.',
      category: 'morphism', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'Duality.lean', reticulateModule: 'duality.py', bicaPackage: 'duality',
    },
    {
      step: '9', name: 'Reticular form characterisation',
      description: 'Necessary and sufficient conditions for a state machine to be the state space of a session type.',
      category: 'morphism', status: 'implemented',
      paper: true, lean: false, reticulate: true, bica: true,
      reticulateModule: 'reticular.py', bicaPackage: 'reticular',
    },
    {
      step: '10', name: '97% of transitions are lattice endomorphisms',
      description: 'Transition maps preserve meet and join across 34 benchmarks.',
      category: 'morphism', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'Endomorphism.lean', reticulateModule: 'endomorphism.py', bicaPackage: 'endomorphism',
    },
    // ── Multiparty (Steps 11–12, 15) ──
    {
      step: '11', name: 'Global type state spaces are lattices',
      description: 'Multiparty global types with role-annotated transitions form lattices.',
      category: 'multiparty', status: 'implemented',
      paper: true, lean: false, reticulate: true, bica: true,
      reticulateModule: 'global_types.py', bicaPackage: 'global',
    },
    {
      step: '12', name: 'Projection preserves morphisms',
      description: 'Projecting a global type onto a role yields a surjective, order-preserving map.',
      category: 'multiparty', status: 'implemented',
      paper: true, lean: false, reticulate: true, bica: true,
      reticulateModule: 'projection.py', bicaPackage: 'projection',
    },
    {
      step: '15', name: 'Bottom-up composition',
      description: 'Free and synchronised products of local types; compatibility checking; Galois connection to global type.',
      category: 'multiparty', status: 'implemented',
      paper: true, lean: false, reticulate: true, bica: true,
      reticulateModule: 'composition.py', bicaPackage: 'composition',
    },
    // ── Structural Analysis (Steps 13–14) ──
    {
      step: '13', name: 'Recursive type analysis',
      description: 'Guardedness, contractiveness, tail recursion, SCC analysis. All 34 benchmarks guarded & contractive.',
      category: 'structural', status: 'implemented',
      paper: true, lean: false, reticulate: true, bica: true,
      reticulateModule: 'recursion.py', bicaPackage: 'recursion',
    },
    {
      step: '14', name: 'Chomsky classification of trace languages',
      description: 'All 34 benchmarks have finite state spaces and regular trace languages.',
      category: 'structural', status: 'implemented',
      paper: true, lean: false, reticulate: true, bica: true,
      reticulateModule: 'context_free.py', bicaPackage: 'chomsky',
    },
    // ── Advanced Theory (Steps 155b, 156, 157a) ──
    {
      step: '155b', name: 'Polarity and formal concept analysis',
      description: 'Galois connection between state closures and label closures; Birkhoff\u2019s fundamental theorem.',
      category: 'advanced', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: false,
      leanFile: 'Polarity.lean', reticulateModule: 'polarity.py',
    },
    {
      step: '156', name: 'Realizability characterisation',
      description: 'A bounded lattice is realizable as a session type iff it has reticular form.',
      category: 'advanced', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: false,
      leanFile: 'Realizability.lean', reticulateModule: 'realizability.py',
    },
    {
      step: '157a', name: 'Channel duality',
      description: 'Ch(S) = L(S) \u00D7 L(dual(S)) is a bounded lattice with role embeddings and branch complementarity.',
      category: 'advanced', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: false,
      leanFile: 'ChannelDuality.lean', reticulateModule: 'channel.py',
    },
    // ── Category Theory (Steps 163–166) ──
    {
      step: '163', name: 'SessLat category',
      description: 'Session-type state spaces and lattice morphisms form a category.',
      category: 'categorical', status: 'implemented',
      paper: false, lean: false, reticulate: true, bica: false,
      reticulateModule: 'category.py',
    },
    {
      step: '164', name: 'Coproducts in SessLat',
      description: 'Coproduct construction for session-type lattices.',
      category: 'categorical', status: 'implemented',
      paper: false, lean: false, reticulate: true, bica: false,
      reticulateModule: 'coproduct.py',
    },
    {
      step: '165', name: 'Equalizers in SessLat',
      description: 'Equalizer construction for session-type lattice morphisms.',
      category: 'categorical', status: 'implemented',
      paper: false, lean: false, reticulate: true, bica: false,
      reticulateModule: 'equalizer.py',
    },
    {
      step: '166', name: 'Coequalizers in SessLat',
      description: 'Coequalizer construction for session-type lattice morphisms.',
      category: 'categorical', status: 'implemented',
      paper: false, lean: false, reticulate: true, bica: false,
      reticulateModule: 'coequalizer.py',
    },
    // ── Lambda Calculus (Steps 200b–200g) ──
    {
      step: '200b', name: '\u03BB_S\u2070 — Basic typestate checking',
      description: 'Progress and preservation for straight-line session-typed object calculus.',
      category: 'metatheory', status: 'mechanised',
      paper: false, lean: 'partial', reticulate: false, bica: false,
      leanFile: 'LambdaS0/',
    },
    {
      step: '200d', name: '\u03BB_S\u00B9 — Return types',
      description: 'Extension with method signatures and base-type return values.',
      category: 'metatheory', status: 'partial',
      paper: false, lean: 'partial', reticulate: false, bica: false,
      leanFile: 'LambdaS1/',
    },
    // ── Counterexamples ──
    {
      step: 'C1', name: 'Non-terminating \u21D2 non-lattice',
      description: 'Counterexample: &{a: end, b: rec X . &{a: X}} has no bottom \u21D2 not a lattice.',
      category: 'ground-truth', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: true,
      leanFile: 'Counterexample.lean', reticulateModule: 'enumerate_types.py',
    },
    {
      step: 'N5', name: 'N\u2085 counterexample (non-distributive)',
      description: 'Session-type lattices are not always distributive; N\u2085 witness by decide.',
      category: 'advanced', status: 'mechanised',
      paper: true, lean: true, reticulate: true, bica: false,
      leanFile: 'Distributivity.lean', reticulateModule: 'distributive.py',
    },
  ];

  readonly categories = [
    { id: 'all', label: 'All' },
    { id: 'ground-truth', label: 'Ground Truth' },
    { id: 'morphism', label: 'Morphisms' },
    { id: 'multiparty', label: 'Multiparty' },
    { id: 'structural', label: 'Structural' },
    { id: 'advanced', label: 'Advanced' },
    { id: 'categorical', label: 'Categorical' },
    { id: 'metatheory', label: 'Metatheory' },
  ];

  readonly filteredTheorems = computed(() => {
    const filter = this.activeFilter();
    if (filter === 'all') return this.theorems;
    return this.theorems.filter(t => t.category === filter);
  });

  readonly counts = computed(() => {
    const all = this.theorems;
    return {
      total: all.length,
      proved: all.filter(t => t.status === 'proved' || t.status === 'mechanised').length,
      mechanised: all.filter(t => t.lean === true).length,
      implemented: all.filter(t => t.reticulate || t.bica).length,
      withPaper: all.filter(t => t.paper).length,
    };
  });

  setFilter(id: string): void {
    this.activeFilter.set(id);
  }

  statusClass(status: string): string {
    return 'status-' + status;
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      proved: 'Proved', mechanised: 'Mechanised', implemented: 'Implemented',
      partial: 'Partial', open: 'Open',
    };
    return labels[status] || status;
  }

  pillarClass(value: boolean | 'partial'): string {
    if (value === true) return 'dot-yes';
    if (value === 'partial') return 'dot-partial';
    return 'dot-no';
  }

  pillarSymbol(value: boolean | 'partial'): string {
    if (value === true) return '\u2713';
    if (value === 'partial') return '\u25CB';
    return '\u2014';
  }
}
