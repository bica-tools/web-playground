import { Component, signal, computed } from '@angular/core';

interface ProofModule {
  file: string;
  description: string;
  category: string;
  status: 'proved' | 'partial';
  lines?: number;
  step?: string;
  depends?: string[];
  theorems: { name: string; note?: string }[];
}

@Component({
  selector: 'app-proofs',
  standalone: true,
  templateUrl: './proofs.component.html',
  styleUrl: './proofs.component.scss',
})
export class ProofsComponent {
  readonly activeFilter = signal('all');

  readonly modules: ProofModule[] = [
    // ── Foundations ──
    {
      file: 'Reachability.lean', category: 'foundations',
      description: 'Reachability is reflexive, transitive; mutual reachability is an equivalence relation.',
      status: 'proved', step: '1',
      theorems: [
        { name: 'Reachable.refl' },
        { name: 'Reachable.trans' },
        { name: 'mutuallyReachable_equivalence' },
      ],
    },
    {
      file: 'SCC.lean', category: 'foundations',
      description: 'SCC quotient type with partial order from reachability.',
      status: 'proved', step: '1',
      theorems: [
        { name: 'SCCQuotient.instPartialOrder' },
        { name: 'le_antisymm\'' },
      ],
    },
    {
      file: 'DirectedGraph.lean', category: 'foundations',
      description: 'Finite directed graph type with transition functions.',
      status: 'proved', step: '1',
      theorems: [{ name: 'FinDiGraph', note: 'definition' }],
    },
    // ── Reticulate Theorem ──
    {
      file: 'EndLemma.lean', category: 'reticulate',
      description: 'L(end) is a singleton bounded lattice — the base case.',
      status: 'proved', step: '2a',
      theorems: [
        { name: 'end_bounded_lattice' },
        { name: 'Lattice EndState', note: 'instance' },
      ],
    },
    {
      file: 'BranchLemma.lean', category: 'reticulate',
      description: 'Adding a new bottom to a bounded lattice preserves the structure.',
      status: 'proved', step: '2b',
      theorems: [
        { name: 'branch_preserves_lattice' },
        { name: 'WithBot\'.instLattice', note: 'instance' },
      ],
    },
    {
      file: 'SequencingLemma.lean', category: 'reticulate',
      description: 'L(m.S) preserves bounded lattice (corollary of BranchLemma).',
      status: 'proved', step: '2b', depends: ['BranchLemma'],
      theorems: [{ name: 'sequencing_preserves_lattice' }],
    },
    {
      file: 'ParallelLemma.lean', category: 'reticulate',
      description: 'L(S\u2081 \u2225 S\u2082) = L(S\u2081) \u00D7 L(S\u2082) is a bounded lattice. N-ary variant included.',
      status: 'proved', step: '2c',
      theorems: [
        { name: 'parallel_bounded_lattice' },
        { name: 'nary_parallel_bounded_lattice' },
        { name: 'prod_inf_eq' },
        { name: 'prod_sup_eq' },
      ],
    },
    {
      file: 'BottomAbsorption.lean', category: 'reticulate',
      description: 'Collapsing a downward-closed set containing \u22A5 preserves bounded lattice.',
      status: 'proved', step: '2d',
      theorems: [
        { name: 'bottom_absorption' },
        { name: 'mkLattice' },
        { name: 'mkOrderTop' },
      ],
    },
    {
      file: 'RecursionLemma.lean', category: 'reticulate', lines: 476,
      description: 'SCC quotient of recursive type is bounded lattice via bottom absorption. 15+ lemmas.',
      status: 'proved', step: '2e', depends: ['BottomAbsorption'],
      theorems: [
        { name: 'recursion_preserves_lattice' },
        { name: 'var_mutual_initial' },
        { name: 'phi_bijective' },
        { name: 'absorbed_lower_closed' },
      ],
    },
    {
      file: 'ReticulateTheorem.lean', category: 'reticulate',
      description: 'Assembly of all constructor cases: end, sequencing, parallel. The main theorem.',
      status: 'proved', step: '2',
      depends: ['EndLemma', 'BranchLemma', 'ParallelLemma', 'RecursionLemma'],
      theorems: [{ name: 'reticulate_theorem', note: 'main result' }],
    },
    {
      file: 'UniqueExtrema.lean', category: 'reticulate',
      description: 'Unique top and bottom elements in session type state spaces.',
      status: 'proved', step: '2',
      theorems: [{ name: 'unique_top' }, { name: 'unique_bot' }],
    },
    {
      file: 'WFParSufficiency.lean', category: 'reticulate',
      description: 'Sufficient conditions for parallel to preserve lattice structure.',
      status: 'proved', step: '2c',
      theorems: [{ name: 'wf_par_sufficient' }],
    },
    // ── Morphisms & Subtyping ──
    {
      file: 'Subtyping.lean', category: 'morphisms',
      description: 'Gay\u2013Hole subtyping: 14 inductive rules. Faithful functor (not full).',
      status: 'proved', step: '7',
      theorems: [
        { name: 'GHSubtype', note: '14 constructors' },
        { name: 'gh_reflexive' },
        { name: 'FaithfulFunctor', note: 'axioms' },
        { name: 'faithful_parallel' },
      ],
    },
    {
      file: 'FaithfulNotFull.lean', category: 'morphisms',
      description: 'The subtyping functor is faithful (injective on morphisms) but not full.',
      status: 'proved', step: '7', depends: ['Subtyping'],
      theorems: [{ name: 'faithful_not_full', note: 'witness' }],
    },
    {
      file: 'Duality.lean', category: 'morphisms',
      description: 'SType AST with dual function. dual(dual(S)) = S involution proved.',
      status: 'proved', step: '8',
      theorems: [
        { name: 'dual_involution' },
        { name: 'dual_injective' },
        { name: 'dual_surjective' },
      ],
    },
    {
      file: 'DualityBridge.lean', category: 'morphisms',
      description: 'Bridges AST duality and state-space duality via StateSpaceFunctor.',
      status: 'proved', step: '8', depends: ['Duality'],
      theorems: [
        { name: 'StateSpaceFunctor', note: 'axioms' },
        { name: 'prodSwap' },
      ],
    },
    {
      file: 'Endomorphism.lean', category: 'morphisms',
      description: 'Transition maps as lattice endomorphisms. Composition, singleton domain.',
      status: 'proved', step: '10',
      theorems: [
        { name: 'endo_is_order_preserving' },
        { name: 'endo_comp' },
        { name: 'singleton_domain_is_endo' },
      ],
    },
    // ── Advanced Theory ──
    {
      file: 'Polarity.lean', category: 'advanced',
      description: 'Galois connection between state/label closures. Birkhoff\u2019s theorem.',
      status: 'proved', step: '155b',
      theorems: [
        { name: 'galois_connection' },
        { name: 'fixed_point_bijection' },
        { name: 'state_closure_idempotent' },
      ],
    },
    {
      file: 'Realizability.lean', category: 'advanced',
      description: 'Realizable \u2194 bounded lattice with reticular form (forward direction).',
      status: 'proved', step: '156',
      theorems: [
        { name: 'forward_direction' },
        { name: 'empty_not_lattice' },
        { name: 'end_realizable' },
      ],
    },
    {
      file: 'ChannelDuality.lean', category: 'advanced',
      description: 'Ch(S) = L(S) \u00D7 L(dual(S)) is bounded lattice with role embeddings.',
      status: 'proved', step: '157a', depends: ['Duality', 'ParallelLemma'],
      theorems: [
        { name: 'channel_bounded_lattice' },
        { name: 'left_role_embedding' },
        { name: 'channel_duality_theorem', note: '4-part' },
      ],
    },
    {
      file: 'AsyncChannel.lean', category: 'advanced',
      description: 'Asynchronous channel model with four-tuple state and half-duplex invariant.',
      status: 'proved', step: '157b',
      theorems: [
        { name: 'async_channel_bounded' },
        { name: 'sync_embedding' },
      ],
    },
    {
      file: 'Distributivity.lean', category: 'advanced',
      description: 'Product distributivity, N\u2085 counterexample (by decide), Birkhoff hierarchy.',
      status: 'proved',
      theorems: [
        { name: 'product_distributive' },
        { name: 'n5_counterexample', note: 'by decide' },
      ],
    },
    {
      file: 'Counterexample.lean', category: 'advanced',
      description: 'Non-terminating type that fails to form a lattice (no bottom).',
      status: 'proved',
      theorems: [{ name: 'non_terminating_not_lattice' }],
    },
    // ── Composition ──
    {
      file: 'ThreeLevelHierarchy.lean', category: 'composition',
      description: 'Three-part morphism hierarchy: isomorphism, embedding, projection.',
      status: 'proved', step: '5-7',
      theorems: [{ name: 'three_level_hierarchy' }],
    },
    {
      file: 'InverseProjection.lean', category: 'composition',
      description: 'Inverse projection conjecture: global \u2192 local \u2192 global round-trip.',
      status: 'proved', step: '12',
      theorems: [{ name: 'inverse_projection_conj1' }],
    },
    {
      file: 'SyncLattice.lean', category: 'composition',
      description: 'Synchronized product conjecture reduced to sublattice closure.',
      status: 'proved',
      theorems: [
        { name: 'sublattice_is_lattice' },
        { name: 'sync_lattice_if_sublattice' },
      ],
    },
    {
      file: 'ConcurrencyLattice.lean', category: 'composition',
      description: 'BICA concurrency diamond: EXCLUSIVE \u2264 SYNC/READ_ONLY \u2264 SHARED.',
      status: 'proved',
      theorems: [
        { name: 'instLattice', note: 'D\u2084 diamond' },
        { name: 'sync_readOnly_incomparable' },
      ],
    },
    // ── Lambda Calculus ──
    {
      file: 'LambdaS0/', category: 'lambda', lines: 800,
      description: '\u03BB_S\u2070: basic typestate checking. Progress + preservation for session-typed objects.',
      status: 'partial', step: '200b',
      theorems: [
        { name: 'substitution_lemma', note: 'proved' },
        { name: 'expr_progress', note: 'proved' },
        { name: 'type_soundness', note: 'proved' },
        { name: 'session_completion', note: '2 sorry' },
      ],
    },
    {
      file: 'LambdaS1/', category: 'lambda',
      description: '\u03BB_S\u00B9: return types extension with method signatures.',
      status: 'partial', step: '200d',
      theorems: [
        { name: 'conservative_extension', note: 'proved' },
        { name: 'preservation', note: '8 sorry' },
      ],
    },
  ];

  readonly categories = [
    { id: 'all', label: 'All' },
    { id: 'foundations', label: 'Foundations' },
    { id: 'reticulate', label: 'Reticulate Theorem' },
    { id: 'morphisms', label: 'Morphisms' },
    { id: 'advanced', label: 'Advanced' },
    { id: 'composition', label: 'Composition' },
    { id: 'lambda', label: 'Lambda Calculus' },
  ];

  readonly filtered = computed(() => {
    const f = this.activeFilter();
    if (f === 'all') return this.modules;
    return this.modules.filter(m => m.category === f);
  });

  readonly counts = computed(() => {
    const all = this.modules;
    const proved = all.filter(m => m.status === 'proved').length;
    const thmCount = all.reduce((s, m) => s + m.theorems.length, 0);
    const sorryCount = all
      .filter(m => m.status === 'partial')
      .reduce((s, m) => s + m.theorems.filter(t => t.note?.includes('sorry')).length, 0);
    return { total: all.length, proved, thmCount, sorryCount };
  });

  setFilter(id: string): void {
    this.activeFilter.set(id);
  }
}
