export interface FaqItem {
  question: string;
  answer: string;
  category: string;
}

export const FAQ_DATA: FaqItem[] = [
  {
    question: 'What is a session type?',
    answer:
      'A <strong>session type</strong> describes the communication protocol governing interaction with an object \u2014 the legal sequences of method calls, branches, and selections a client may perform. Unlike a flat interface, a session type makes the object\'s type <em>evolve</em> as methods are called, enforcing protocol compliance <strong>statically</strong>.',
    category: 'Fundamentals',
  },
  {
    question: 'What is a state space?',
    answer:
      'A <strong>state space</strong> <code>L(S)</code> is the labeled transition system obtained by "executing" a session type. Each state represents a protocol stage, each transition a permitted action. It has a unique initial state (top) and terminal state (bottom). The reachability ordering gives it the structure of a bounded lattice (after SCC quotient).',
    category: 'Fundamentals',
  },
  {
    question: 'What is a bounded lattice?',
    answer:
      'A <strong>bounded lattice</strong> is a partially ordered set where every pair has a meet (greatest lower bound) and join (least upper bound), plus top and bottom elements. In session types: top = initial state, bottom = terminal state, meet = convergence point, join = divergence point.',
    category: 'Theory',
  },
  {
    question: 'What is the parallel constructor?',
    answer:
      'The <strong>parallel constructor</strong> <code>S\u2081 \u2225 S\u2082</code> models two sub-protocols executing concurrently on a shared object. Its state space is the product of the two components. This is the <strong>key novelty</strong> \u2014 the product of two lattices is always a lattice, making lattice structure <em>necessary</em>.',
    category: 'Constructs',
  },
  {
    question: 'What is a reticulate?',
    answer:
      'A <strong>reticulate</strong> is the bounded lattice formed by the state space of a well-formed session type, after quotienting by SCCs. The Reticulate Theorem proves every well-formed session type produces one \u2014 it is a guaranteed structural property.',
    category: 'Theory',
  },
  {
    question: 'What is an SCC quotient?',
    answer:
      'An <strong>SCC quotient</strong> collapses every strongly connected component into a single node, turning a cyclic graph into a DAG. In session types, cycles from recursion are collapsed, restoring antisymmetry for the partial order.',
    category: 'Theory',
  },
  {
    question: 'What is top absorption?',
    answer:
      '<strong>Top absorption</strong> is the key lemma for recursion. It states: if you collapse an upward-closed set containing top into a single element, the result is still a bounded lattice. This has been mechanically verified in Lean 4.',
    category: 'Theory',
  },
  {
    question: 'What is WF-Par?',
    answer:
      '<strong>WF-Par</strong> is the well-formedness condition for the parallel constructor. It requires both branches to be terminating, well-formed, variable-disjoint, and free of nested <code>\u2225</code>.',
    category: 'Constructs',
  },
  {
    question: 'What is a morphism between session types?',
    answer:
      'A <strong>morphism</strong> is a structure-preserving map between state spaces. The hierarchy: homomorphism (order-preserving), projection (surjective), embedding (injective + reflecting), isomorphism (bijective embedding). Additionally, Galois connections capture approximation relationships.',
    category: 'Theory',
  },
  {
    question: 'Why do session type state spaces form lattices?',
    answer:
      'Because every constructor preserves lattice structure, and the proof goes by structural induction: <code>end</code> is trivial; sequencing adds a maximum; branch/selection create joins and meets; recursion is absorbed by SCC quotient; parallel takes the product. Since every constructor preserves the property and the base case has it, every well-formed session type has it.',
    category: 'Fundamentals',
  },
  {
    question: 'What is the lattice classification hierarchy?',
    answer:
      'Every session type state space is classified in the lattice hierarchy: <strong>boolean</strong> (power-of-2 elements) \u2282 <strong>distributive</strong> (no M\u2083, no N\u2085) \u2282 <strong>modular</strong> (has M\u2083 but no N\u2085) \u2282 <strong>lattice</strong> (has N\u2085). Across 79 benchmarks: 72% are distributive, 3% modular, 25% lattice-only. Non-distributivity typically arises from the interaction of branching/selection with parallel composition.',
    category: 'Theory',
  },
  {
    question: 'What is Gay\u2013Hole subtyping?',
    answer:
      'Gay\u2013Hole subtyping is a coinductive relation on session types. For branches, the subtype offers <em>more</em> methods (covariant width). For selections, the subtype selects <em>fewer</em> labels (contravariant width). For parallel, subtyping is componentwise: <code>(S\u2081 \u2225 S\u2082) \u2264 (T\u2081 \u2225 T\u2082)</code> iff <code>S\u2081 \u2264 T\u2081</code> and <code>S\u2082 \u2264 T\u2082</code>. The key result: subtyping corresponds to lattice embedding of state spaces.',
    category: 'Theory',
  },
  {
    question: 'Does the parallel constructor support more than two branches?',
    answer:
      'Yes. The parallel constructor is <strong>n-ary</strong>: <code>S\u2081 \u2225 S\u2082 \u2225 \u2026 \u2225 S\u2099</code> composes any number of concurrent sub-protocols. The state space is the n-fold product lattice. Since lattice products are associative, n-ary parallel is semantically equivalent to nested binary \u2014 but the flat form expresses the intent that all branches are peers at the same level.',
    category: 'Constructs',
  },
  {
    question: 'Can session types model security protocols?',
    answer:
      'Yes. The benchmark suite includes 10 security protocols: Needham\u2013Schroeder, Kerberos, SSH, Diffie\u2013Hellman, Mutual TLS, Signal (Double Ratchet), X.509 Certificate Chain, SAML SSO, Zero-Knowledge Proofs, and DNSSEC. All form lattices. Selections model verifier decisions (TRUSTED/UNTRUSTED, PASS/FAIL), and parallel models concurrent certificate verification (e.g., mTLS).',
    category: 'Benchmarks',
  },
  {
    question: 'How many benchmark protocols are there?',
    answer:
      'The suite contains <strong>79 benchmarks</strong> across 7 domains: software protocols (15), distributed systems (15), industry/Ki3 (3), physics (16), molecular biology (12), cell biology (8), and security (10). All 79 form lattices under the reachability ordering \u2014 no counterexample has been found among well-formed session types.',
    category: 'Benchmarks',
  },
];
