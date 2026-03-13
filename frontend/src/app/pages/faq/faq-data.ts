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

  // ── Realizability (Step 156) ──────────────────────────────────

  {
    question: 'What is realizability?',
    answer:
      '<strong>Realizability</strong> answers the inverse problem: given a finite bounded lattice <em>L</em>, does there exist a session type <em>S</em> such that <code>L(S) \u2245 L</code>? If yes, <em>L</em> is <strong>realizable</strong>. The Characterization Theorem says: a lattice is realizable if and only if it is a bounded lattice with <strong>reticular form</strong>.',
    category: 'Realizability',
  },
  {
    question: 'Why do we need realizability? Isn\u2019t the Reticulate Theorem enough?',
    answer:
      'The Reticulate Theorem answers \u201cwhat do session types produce?\u201d (bounded lattices). Realizability answers the harder inverse: \u201cwhich bounded lattices come from session types?\u201d This is essential for tool builders: given an arbitrary state machine (e.g. extracted from legacy code), can we <em>type</em> it with a session type? Realizability gives a precise <strong>decision procedure</strong>.',
    category: 'Realizability',
  },
  {
    question: 'Is the realizability check decidable and efficient?',
    answer:
      'Yes. For a finite state space with <em>n</em> states and <em>m</em> transitions: (1) the lattice check is <em>O(n\u00B3)</em> (SCC quotient + pairwise meet/join), (2) reticular form check is <em>O(n \u00B7 m)</em> (classify each state). Both are implemented in <code>check_realizability(ss)</code> with polynomial-time algorithms.',
    category: 'Realizability',
  },
  {
    question: 'What are obstruction categories?',
    answer:
      'When a state space is <strong>not</strong> realizable, the failure decomposes into obstructions across 5 categories: <strong>A\u2014Structural</strong> (nondeterminism, unreachable states, dead ends); <strong>B\u2014Lattice</strong> (missing meet/join, not bounded); <strong>C\u2014Reticular</strong> (mixed states, label conflicts); <strong>D\u2014Automata</strong> (trace structure violations); <strong>E\u2014Counting</strong> (duplicate labels, empty non-bottom). The <code>find_obstructions(ss)</code> function diagnoses all of them.',
    category: 'Realizability',
  },
  {
    question: 'What is the non-realizable catalogue?',
    answer:
      'A collection of <strong>18 hand-crafted state spaces</strong> that are not realizable, organized by obstruction category. Examples include: <code>nondeterministic</code> (same label, two targets), <code>missing_meet</code> (W-shape with no GLB), <code>dead_end</code> (no path to bottom). Use <code>generate_non_realizable(kind)</code> to access any entry.',
    category: 'Realizability',
  },
  {
    question: 'Can a lattice be a lattice but not have reticular form?',
    answer:
      'In principle, yes: a lattice where some non-bottom state has both branch-like and selection-like transitions that don\u2019t decompose as a product. However, such lattices are rare and don\u2019t arise from standard protocol patterns. No natural example has been found among 79 benchmarks, 478,000+ enumerated types, or 6 wild state machines.',
    category: 'Realizability',
  },
  {
    question: 'What about infinite session types and realizability?',
    answer:
      'Realizability applies to <strong>finite state spaces</strong>. Recursive types (<code>\u03BCX.S</code>) produce finite state spaces via SCC quotient, so all well-formed session types are covered. Infinite-state systems (from unbounded recursion) are outside scope.',
    category: 'Realizability',
  },
  {
    question: 'How does realizability relate to subtyping?',
    answer:
      'Subtyping <code>S\u2081 \u2264 S\u2082</code> implies an order-embedding <code>L(S\u2082) \u21AA L(S\u2081)</code>. If <em>L\u2081</em> and <em>L\u2082</em> are both realizable and <em>L\u2082</em> embeds in <em>L\u2081</em>, then the corresponding session types are related by subtyping. Realizability is a <strong>prerequisite</strong> for this analysis.',
    category: 'Realizability',
  },
  {
    question: 'Why isn\u2019t nondeterminism in the session type grammar?',
    answer:
      'Session types model <strong>deterministic protocols</strong>: from any state, each method name leads to at most one successor. Nondeterminism would mean the same method call could lead to different states, violating protocol predictability. This is why nondeterministic LTSs are non-realizable.',
    category: 'Realizability',
  },
  {
    question: 'Can I use check_realizability on arbitrary state machines?',
    answer:
      'Yes. The function <code>check_realizability(ss)</code> accepts any <code>StateSpace</code> object \u2014 not just those built from session types. If the input is realizable, it returns the <strong>reconstructed session type</strong>. If not, it returns diagnostic obstructions explaining why. This makes it useful for checking whether legacy state machines can be typed.',
    category: 'Realizability',
  },
  {
    question: 'What is a channel in the reticulate framework?',
    answer:
      'A <strong>channel</strong> is an object with two roles (A and B) whose session types are <em>dual</em>: <code>S</code> and <code>dual(S)</code>. The channel\u2019s state space is the product <code>L(S) \u00D7 L(dual(S))</code>, which models all valid interleavings of the two roles\u2019 behaviour. This product is always a bounded lattice.',
    category: 'Channel Duality',
  },
  {
    question: 'Why is the channel product always a lattice?',
    answer:
      'Because (1) every well-formed session type has a lattice state space (Reticulate Theorem), (2) duality preserves lattice structure (<code>L(S) \u2245 L(dual(S))</code>), and (3) the product of two lattices is always a lattice. These three facts compose to give the result.',
    category: 'Channel Duality',
  },
  {
    question: 'What is branch complementarity?',
    answer:
      '<strong>Branch complementarity</strong> is the property that a branch transition for role A corresponds to a selection transition for role B, and vice versa. It captures the channel invariant: when one side waits for input (branch), the other side provides it (selection).',
    category: 'Channel Duality',
  },
  {
    question: 'How does the channel construction relate to the parallel constructor?',
    answer:
      'The channel <code>Ch(S) = L(S) \u00D7 L(dual(S))</code> uses the same product construction as the parallel operator <code>S\u2081 \u2225 S\u2082</code>. The difference is that the two components are <em>dual</em>, which adds complementarity and isomorphism properties that arbitrary parallel composition does not have.',
    category: 'Channel Duality',
  },
  {
    question: 'What does role embedding mean?',
    answer:
      'The <strong>role embedding</strong> \u03B9<sub>A</sub>: s \u21A6 (s, \u22A4<sub>B</sub>) maps each local state to a channel state. It is an <em>order-embedding</em>: it preserves and reflects the reachability ordering. This means a role\u2019s local view is faithfully represented in the channel \u2014 no ordering information is lost.',
    category: 'Channel Duality',
  },
  {
    question: 'Can I build a channel from a global type?',
    answer:
      'Yes. For a binary global type <code>G</code> between roles A and B, project onto each role, verify they are duals, and build the channel product. The function <code>channel_from_global(g, sender, receiver)</code> does this automatically.',
    category: 'Channel Duality',
  },
  {
    question: 'Does the channel construction work with recursive types?',
    answer:
      'Yes. Recursive types produce cyclic state spaces handled by SCC quotient. The product of two SCC-quotiented lattices is still a lattice, and branch complementarity is verified across the recursive structure. All recursive benchmarks pass.',
    category: 'Channel Duality',
  },
  {
    question: 'How are channels different from arbitrary objects?',
    answer:
      'A channel is a <strong>special case</strong> of an object: it has exactly two roles whose types are dual. This gives extra structure (isomorphism, complementarity, role embedding) that arbitrary multi-role objects do not have. The same lattice framework applies to both \u2014 channels are just the dual case.',
    category: 'Channel Duality',
  },
  {
    question: 'How does channel duality relate to linear logic?',
    answer:
      'In the Caires\u2013Pfenning\u2013Wadler correspondence, session type duality maps to <strong>linear negation</strong>. The channel product <code>L(S) \u00D7 L(dual(S))</code> can be seen as the semantic model of two complementary linear propositions, with the lattice structure providing the \u201Ctruth values.\u201D',
    category: 'Channel Duality',
  },
  {
    question: 'What was verified empirically for channel duality?',
    answer:
      'Across <strong>78 benchmarks</strong> (71 binary + 7 binary multiparty), the channel product is always a bounded lattice, the two local state spaces are isomorphic, branch complementarity holds, and both role embeddings are verified. All checks are automated in <code>test_channel.py</code> (132 tests).',
    category: 'Channel Duality',
  },
];
