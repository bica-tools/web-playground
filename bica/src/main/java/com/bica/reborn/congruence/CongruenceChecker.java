package com.bica.reborn.congruence;

import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Congruence lattice analysis for session type state spaces.
 *
 * <p>The <b>congruence lattice</b> Con(L) of a lattice L catalogues ALL valid
 * quotients (abstractions) of a protocol. A congruence θ is an equivalence
 * relation on L compatible with meet and join.
 *
 * <p>Key theorems:
 * <ul>
 *   <li><b>Funayama-Nakayama (1942)</b>: Con(L) is always distributive for any finite lattice L</li>
 *   <li><b>Birkhoff</b>: For distributive L, congruences biject with downsets of J(L)</li>
 *   <li><b>Products</b>: Con(L₁ × L₂) ≅ Con(L₁) × Con(L₂)</li>
 *   <li><b>Simplicity</b>: Con(L) = {0,1} iff L has no non-trivial quotients</li>
 * </ul>
 */
public final class CongruenceChecker {

    private CongruenceChecker() {}

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * The congruence lattice Con(L).
     *
     * @param congruences     All congruences, indexed by position.
     * @param bottom          Index of the identity congruence (finest).
     * @param top             Index of the total congruence (coarsest).
     * @param ordering        (i,j) in ordering iff congruences[i] refines congruences[j].
     * @param numCongruences  |Con(L)|.
     * @param isDistributive  Always true for finite lattices (Funayama-Nakayama).
     */
    public record CongruenceLatticeResult(
            List<Congruence> congruences,
            int bottom,
            int top,
            Set<List<Integer>> ordering,
            int numCongruences,
            boolean isDistributive) {

        public CongruenceLatticeResult {
            Objects.requireNonNull(congruences, "congruences must not be null");
            Objects.requireNonNull(ordering, "ordering must not be null");
            congruences = List.copyOf(congruences);
            ordering = ordering.stream()
                    .map(List::copyOf)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * Full congruence analysis of a session type lattice.
     *
     * @param conLattice             The congruence lattice Con(L).
     * @param numCongruences         |Con(L)|.
     * @param isSimple               True iff Con(L) = {⊥, ⊤}.
     * @param isModular              True iff L is modular.
     * @param isDistributiveLattice  True iff L is distributive.
     * @param numJoinIrreducibles    |J(L)|.
     * @param maxQuotientSize        Largest non-trivial quotient size.
     * @param minQuotientSize        Smallest non-trivial quotient size.
     * @param numStates              |L|.
     */
    public record CongruenceAnalysis(
            CongruenceLatticeResult conLattice,
            int numCongruences,
            boolean isSimple,
            boolean isModular,
            boolean isDistributiveLattice,
            int numJoinIrreducibles,
            int maxQuotientSize,
            int minQuotientSize,
            int numStates) {

        public CongruenceAnalysis {
            Objects.requireNonNull(conLattice, "conLattice must not be null");
        }
    }

    // -----------------------------------------------------------------------
    // Union-Find (efficient partition management)
    // -----------------------------------------------------------------------

    private static final class UnionFind {
        private final Map<Integer, Integer> parent;
        private final Map<Integer, Integer> rank;

        UnionFind(Set<Integer> elements) {
            parent = new HashMap<>();
            rank = new HashMap<>();
            for (int e : elements) {
                parent.put(e, e);
                rank.put(e, 0);
            }
        }

        int find(int x) {
            while (parent.get(x) != x) {
                parent.put(x, parent.get(parent.get(x))); // path halving
                x = parent.get(x);
            }
            return x;
        }

        boolean union(int x, int y) {
            int rx = find(x), ry = find(y);
            if (rx == ry) return false;
            if (rank.get(rx) < rank.get(ry)) { int tmp = rx; rx = ry; ry = tmp; }
            parent.put(ry, rx);
            if (rank.get(rx).equals(rank.get(ry))) {
                rank.put(rx, rank.get(rx) + 1);
            }
            return true;
        }

        List<Set<Integer>> partition() {
            Map<Integer, Set<Integer>> groups = new HashMap<>();
            for (int e : parent.keySet()) {
                int r = find(e);
                groups.computeIfAbsent(r, k -> new HashSet<>()).add(e);
            }
            return groups.values().stream()
                    .sorted(Comparator.comparingInt(s -> Collections.min(s)))
                    .map(Set::copyOf)
                    .toList();
        }
    }

    // -----------------------------------------------------------------------
    // Public API: Principal congruence
    // -----------------------------------------------------------------------

    /**
     * Compute θ(a,b): the smallest congruence identifying a and b.
     *
     * <p>Uses iterative closure: start with a ≡ b, propagate through ∧ and ∨.
     */
    public static Congruence principalCongruence(StateSpace ss, int a, int b) {
        UnionFind uf = new UnionFind(ss.states());
        uf.union(a, b);

        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);
        int maxIters = statesList.size() * statesList.size();

        boolean changed = true;
        for (int iter = 0; iter < maxIters && changed; iter++) {
            changed = false;
            for (int s1 : statesList) {
                for (int s2 : statesList) {
                    if (s1 >= s2 || uf.find(s1) != uf.find(s2)) continue;
                    for (int x : statesList) {
                        Integer m1 = LatticeChecker.computeMeet(ss, s1, x);
                        Integer m2 = LatticeChecker.computeMeet(ss, s2, x);
                        if (m1 != null && m2 != null && uf.union(m1, m2)) {
                            changed = true;
                        }

                        Integer j1 = LatticeChecker.computeJoin(ss, s1, x);
                        Integer j2 = LatticeChecker.computeJoin(ss, s2, x);
                        if (j1 != null && j2 != null && uf.union(j1, j2)) {
                            changed = true;
                        }
                    }
                }
            }
        }

        List<Set<Integer>> classes = uf.partition();
        return new Congruence(classes, classes.size(), List.of(new int[]{a, b}));
    }

    // -----------------------------------------------------------------------
    // Public API: Enumerate congruences
    // -----------------------------------------------------------------------

    /**
     * Enumerate all congruences of the lattice.
     *
     * <p>Strategy: generate all principal congruences θ(a,b), then close
     * under join (finest common coarsening) to get Con(L).
     */
    public static List<Congruence> enumerateCongruences(StateSpace ss) {
        return enumerateCongruences(ss, 1000);
    }

    /**
     * Enumerate congruences with a cap on the maximum number.
     */
    public static List<Congruence> enumerateCongruences(StateSpace ss, int maxCongruences) {
        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);
        int n = statesList.size();

        // Identity congruence
        Congruence identity = new Congruence(
                statesList.stream().map(s -> Set.of(s)).collect(Collectors.toList()),
                n, List.of());

        // Total congruence
        Congruence total = new Congruence(
                List.of(Set.copyOf(ss.states())), 1, List.of());

        if (n <= 1) return List.of(identity);

        // Collect unique congruences keyed by their class structure
        Map<List<Set<Integer>>, Congruence> all = new LinkedHashMap<>();
        all.put(identity.classes(), identity);
        all.put(total.classes(), total);

        // Principal congruences
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (all.size() >= maxCongruences) break;
                Congruence pc = principalCongruence(ss, statesList.get(i), statesList.get(j));
                all.putIfAbsent(pc.classes(), pc);
            }
        }

        // Close under join
        boolean changed = true;
        while (changed && all.size() < maxCongruences) {
            changed = false;
            List<Congruence> current = new ArrayList<>(all.values());
            for (int i = 0; i < current.size(); i++) {
                for (int j = i + 1; j < current.size(); j++) {
                    if (all.size() >= maxCongruences) break;
                    Congruence joined = joinCongruences(ss, current.get(i), current.get(j));
                    if (!all.containsKey(joined.classes())) {
                        all.put(joined.classes(), joined);
                        changed = true;
                    }
                }
            }
        }

        return all.values().stream()
                .sorted(Comparator.comparingInt(Congruence::numClasses))
                .toList();
    }

    // -----------------------------------------------------------------------
    // Public API: Congruence lattice
    // -----------------------------------------------------------------------

    /**
     * Build the full congruence lattice Con(L).
     */
    public static CongruenceLatticeResult congruenceLattice(StateSpace ss) {
        List<Congruence> congs = enumerateCongruences(ss);

        int bottomIdx = 0;
        int topIdx = congs.size() - 1;
        for (int i = 0; i < congs.size(); i++) {
            if (congs.get(i).isTrivialBottom()) bottomIdx = i;
            if (congs.get(i).isTrivialTop()) topIdx = i;
        }

        // Refinement ordering
        Set<List<Integer>> ordering = new HashSet<>();
        for (int i = 0; i < congs.size(); i++) {
            for (int j = 0; j < congs.size(); j++) {
                if (refines(congs.get(i), congs.get(j))) {
                    ordering.add(List.of(i, j));
                }
            }
        }

        // Con(L) of a finite lattice is always distributive (Funayama-Nakayama)
        return new CongruenceLatticeResult(
                congs, bottomIdx, topIdx, ordering, congs.size(), true);
    }

    // -----------------------------------------------------------------------
    // Public API: Quotient lattice
    // -----------------------------------------------------------------------

    /**
     * Build the quotient lattice L/θ as a new StateSpace.
     *
     * <p>Each equivalence class becomes a state. Transitions between classes
     * are induced by the original transitions.
     */
    public static StateSpace quotientLattice(StateSpace ss, Congruence cong) {
        // Map each state to its class representative (minimum element)
        Map<Integer, Integer> classOf = new HashMap<>();
        for (Set<Integer> cls : cong.classes()) {
            int rep = Collections.min(cls);
            for (int s : cls) classOf.put(s, rep);
        }

        Set<Integer> qStates = new HashSet<>();
        for (int s : ss.states()) qStates.add(classOf.get(s));

        Set<StateSpace.Transition> seen = new HashSet<>();
        List<StateSpace.Transition> qTransitions = new ArrayList<>();

        for (var t : ss.transitions()) {
            int qSrc = classOf.get(t.source());
            int qTgt = classOf.get(t.target());
            if (qSrc != qTgt) {
                var qt = new StateSpace.Transition(qSrc, t.label(), qTgt);
                if (seen.add(qt)) {
                    qTransitions.add(qt);
                }
            }
        }

        int qTop = classOf.get(ss.top());
        int qBottom = classOf.containsKey(ss.bottom())
                ? classOf.get(ss.bottom())
                : Collections.min(qStates);

        Map<Integer, String> qLabels = new HashMap<>();
        for (int s : qStates) qLabels.put(s, "[" + s + "]");

        return new StateSpace(qStates, qTransitions, qTop, qBottom, qLabels);
    }

    // -----------------------------------------------------------------------
    // Public API: Simplicity
    // -----------------------------------------------------------------------

    /**
     * Check if L is simple: Con(L) = {⊥, ⊤} (only trivial congruences).
     */
    public static boolean isSimple(StateSpace ss) {
        List<Congruence> congs = enumerateCongruences(ss);
        return congs.size() <= 2;
    }

    // -----------------------------------------------------------------------
    // Public API: Birkhoff fast path
    // -----------------------------------------------------------------------

    /**
     * Fast congruence enumeration for distributive lattices.
     *
     * <p>Birkhoff's theorem: congruences of a distributive lattice biject with
     * downsets of the poset of join-irreducible elements J(L).
     *
     * @return list of congruences, or {@code null} if L is not distributive
     */
    public static List<Congruence> birkhoffCongruences(StateSpace ss) {
        DistributivityResult dist = DistributivityChecker.checkDistributive(ss);
        if (!dist.isDistributive()) return null;

        var q = DistributivityChecker.QuotientPoset.build(ss);
        Map<Integer, Set<Integer>> reach = reachability(ss);
        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);

        // Build covering relation
        Map<Integer, List<Integer>> covers = new HashMap<>();
        for (int s : statesList) covers.put(s, new ArrayList<>());

        for (int s : statesList) {
            for (int t : statesList) {
                if (s == t) continue;
                if (!reach.getOrDefault(s, Set.of()).contains(t)) continue;
                boolean isCover = true;
                for (int u : statesList) {
                    if (u == s || u == t) continue;
                    if (reach.getOrDefault(s, Set.of()).contains(u)
                            && reach.getOrDefault(u, Set.of()).contains(t)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) covers.get(s).add(t);
            }
        }

        // Find join-irreducibles: elements covering exactly one element
        List<Integer> joinIrr = new ArrayList<>();
        for (int s : statesList) {
            if (s == ss.bottom()) continue;
            if (covers.get(s).size() == 1) joinIrr.add(s);
        }

        int nJi = joinIrr.size();
        if (nJi > 20) return null; // Too many

        // J(L) ordering
        Map<Integer, Set<Integer>> jiOrder = new HashMap<>();
        for (int j : joinIrr) jiOrder.put(j, new HashSet<>());
        Set<Integer> jiSet = new HashSet<>(joinIrr);
        for (int j1 : joinIrr) {
            for (int j2 : joinIrr) {
                if (j1 != j2 && reach.getOrDefault(j2, Set.of()).contains(j1)) {
                    jiOrder.get(j2).add(j1); // j2 >= j1
                }
            }
        }

        // Enumerate downsets
        List<Set<Integer>> downsets = new ArrayList<>();
        downsets.add(Set.of()); // empty downset

        for (int mask = 1; mask < (1 << nJi); mask++) {
            Set<Integer> subset = new HashSet<>();
            for (int i = 0; i < nJi; i++) {
                if ((mask & (1 << i)) != 0) subset.add(joinIrr.get(i));
            }
            boolean isDownset = true;
            for (int j : subset) {
                for (int below : jiOrder.get(j)) {
                    if (jiSet.contains(below) && !subset.contains(below)) {
                        isDownset = false;
                        break;
                    }
                }
                if (!isDownset) break;
            }
            if (isDownset) downsets.add(Set.copyOf(subset));
        }

        // Convert each downset to a congruence
        Set<List<Set<Integer>>> seen = new HashSet<>();
        List<Congruence> result = new ArrayList<>();

        // Identity congruence (empty downset)
        Congruence identity = new Congruence(
                statesList.stream().map(Set::<Integer>of).collect(Collectors.toList()),
                statesList.size(), List.of());
        seen.add(identity.classes());
        result.add(identity);

        for (Set<Integer> ds : downsets) {
            if (ds.isEmpty()) continue;
            UnionFind uf = new UnionFind(ss.states());
            for (int j : ds) {
                List<Integer> lc = covers.get(j);
                if (!lc.isEmpty()) uf.union(j, lc.get(0));
            }

            // Close under meet/join
            boolean changed = true;
            for (int iter = 0; iter < statesList.size() * statesList.size() && changed; iter++) {
                changed = false;
                for (int s1 : statesList) {
                    for (int s2 : statesList) {
                        if (s1 >= s2 || uf.find(s1) != uf.find(s2)) continue;
                        for (int x : statesList) {
                            Integer m1 = LatticeChecker.computeMeet(ss, s1, x);
                            Integer m2 = LatticeChecker.computeMeet(ss, s2, x);
                            if (m1 != null && m2 != null && uf.union(m1, m2)) changed = true;
                            Integer j1 = LatticeChecker.computeJoin(ss, s1, x);
                            Integer j2 = LatticeChecker.computeJoin(ss, s2, x);
                            if (j1 != null && j2 != null && uf.union(j1, j2)) changed = true;
                        }
                    }
                }
            }

            List<Set<Integer>> classes = uf.partition();
            if (seen.add(classes)) {
                result.add(new Congruence(classes, classes.size(), List.of()));
            }
        }

        return result.stream()
                .sorted(Comparator.comparingInt(Congruence::numClasses))
                .toList();
    }

    // -----------------------------------------------------------------------
    // Public API: Simplification options
    // -----------------------------------------------------------------------

    /**
     * Return ranked list of non-trivial quotients, ordered by quotient size.
     *
     * @return list of (congruence, quotient_size) entries; smaller = more abstract
     */
    public static List<Map.Entry<Congruence, Integer>> simplificationOptions(StateSpace ss) {
        List<Congruence> congs = enumerateCongruences(ss);
        return congs.stream()
                .filter(c -> !c.isTrivialBottom() && !c.isTrivialTop())
                .map(c -> Map.entry(c, c.numClasses()))
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .toList();
    }

    // -----------------------------------------------------------------------
    // Public API: Full analysis
    // -----------------------------------------------------------------------

    /**
     * Full congruence lattice analysis.
     */
    public static CongruenceAnalysis analyzeCongruences(StateSpace ss) {
        CongruenceLatticeResult con = congruenceLattice(ss);
        DistributivityResult dist = DistributivityChecker.checkDistributive(ss);

        boolean isMod = dist.isLattice() && dist.isModular();
        boolean isDist = dist.isLattice() && dist.isDistributive();

        // Count join-irreducibles
        Map<Integer, Set<Integer>> reach = reachability(ss);
        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);

        Map<Integer, Integer> coversCount = new HashMap<>();
        for (int s : statesList) coversCount.put(s, 0);

        for (int s : statesList) {
            for (int t : statesList) {
                if (s == t || !reach.getOrDefault(s, Set.of()).contains(t)) continue;
                boolean isCover = true;
                for (int u : statesList) {
                    if (u == s || u == t) continue;
                    if (reach.getOrDefault(s, Set.of()).contains(u)
                            && reach.getOrDefault(u, Set.of()).contains(t)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) coversCount.merge(s, 1, Integer::sum);
            }
        }

        int nJi = 0;
        for (int s : statesList) {
            if (s != ss.bottom() && coversCount.get(s) == 1) nJi++;
        }

        // Non-trivial quotient sizes
        List<Congruence> nonTrivial = con.congruences().stream()
                .filter(c -> !c.isTrivialBottom() && !c.isTrivialTop())
                .toList();

        int maxQ = nonTrivial.stream().mapToInt(Congruence::numClasses).max().orElse(0);
        int minQ = nonTrivial.stream().mapToInt(Congruence::numClasses).min().orElse(0);

        return new CongruenceAnalysis(
                con, con.numCongruences(), nonTrivial.isEmpty(),
                isMod, isDist, nJi, maxQ, minQ, ss.states().size());
    }

    // -----------------------------------------------------------------------
    // Internal: join two congruences
    // -----------------------------------------------------------------------

    private static Congruence joinCongruences(StateSpace ss, Congruence c1, Congruence c2) {
        UnionFind uf = new UnionFind(ss.states());

        // Merge pairs from c1
        for (Set<Integer> cls : c1.classes()) {
            List<Integer> elems = new ArrayList<>(cls);
            Collections.sort(elems);
            for (int i = 1; i < elems.size(); i++) uf.union(elems.get(0), elems.get(i));
        }

        // Merge pairs from c2
        for (Set<Integer> cls : c2.classes()) {
            List<Integer> elems = new ArrayList<>(cls);
            Collections.sort(elems);
            for (int i = 1; i < elems.size(); i++) uf.union(elems.get(0), elems.get(i));
        }

        // Close under meet/join
        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);
        boolean changed = true;
        for (int iter = 0; iter < statesList.size() * statesList.size() && changed; iter++) {
            changed = false;
            for (int s1 : statesList) {
                for (int s2 : statesList) {
                    if (s1 >= s2 || uf.find(s1) != uf.find(s2)) continue;
                    for (int x : statesList) {
                        Integer m1 = LatticeChecker.computeMeet(ss, s1, x);
                        Integer m2 = LatticeChecker.computeMeet(ss, s2, x);
                        if (m1 != null && m2 != null && uf.union(m1, m2)) changed = true;
                        Integer j1 = LatticeChecker.computeJoin(ss, s1, x);
                        Integer j2 = LatticeChecker.computeJoin(ss, s2, x);
                        if (j1 != null && j2 != null && uf.union(j1, j2)) changed = true;
                    }
                }
            }
        }

        List<Set<Integer>> classes = uf.partition();
        return new Congruence(classes, classes.size(), List.of());
    }

    // -----------------------------------------------------------------------
    // Internal: refinement check
    // -----------------------------------------------------------------------

    private static boolean refines(Congruence c1, Congruence c2) {
        for (Set<Integer> cls1 : c1.classes()) {
            boolean found = false;
            for (Set<Integer> cls2 : c2.classes()) {
                if (cls2.containsAll(cls1)) { found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Internal: reachability
    // -----------------------------------------------------------------------

    private static Map<Integer, Set<Integer>> reachability(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : ss.transitions()) adj.get(t.source()).add(t.target());

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int v = stack.pop();
                if (!visited.add(v)) continue;
                for (int t : adj.getOrDefault(v, Set.of())) stack.push(t);
            }
            reach.put(s, visited);
        }
        return reach;
    }
}
