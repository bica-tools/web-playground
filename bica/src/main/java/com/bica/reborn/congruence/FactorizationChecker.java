package com.bica.reborn.congruence;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lattice factorization theory for session type state spaces (Step 363f).
 *
 * <p>Detects whether a session type lattice L(S) is directly decomposable
 * into L1 x L2, meaning the protocol has hidden parallelism. Uses the
 * factor congruence characterisation: L is directly decomposable iff Con(L)
 * contains a complementary pair (theta, phi) with theta meet phi = identity
 * and theta join phi = total, and additionally |L/theta| * |L/phi| = |L|.
 *
 * <p>Key concepts:
 * <ul>
 *   <li>Factor congruence pair: (theta, phi) with meet = identity, join = total</li>
 *   <li>Directly decomposable: L = L/theta x L/phi for some factor pair</li>
 *   <li>Indecomposable: no factor pair exists; the protocol is atomic</li>
 *   <li>Unique factorization: finite lattices have unique factorization into indecomposables</li>
 * </ul>
 */
public final class FactorizationChecker {

    private FactorizationChecker() {}

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * A complementary congruence pair (theta, phi) witnessing direct decomposition.
     */
    public record FactorPair(Congruence theta, Congruence phi) {
        public FactorPair {
            Objects.requireNonNull(theta, "theta must not be null");
            Objects.requireNonNull(phi, "phi must not be null");
        }
    }

    /**
     * An indexed factor pair: (i, j) referencing positions in Con(L).
     */
    public record IndexedFactorPair(int i, int j) {}

    /**
     * Complete factorization analysis of a session type lattice.
     *
     * @param isDecomposable           True if L can be expressed as a direct product.
     * @param isIndecomposable         Complement of isDecomposable.
     * @param factors                  Tuple of directly indecomposable factor StateSpaces.
     * @param factorCount              Number of indecomposable factors.
     * @param factorSizes              Sizes of each factor (number of states).
     * @param factorCongruencePairs    Indices into Con(L) for each factor pair found.
     */
    public record FactorizationAnalysis(
            boolean isDecomposable,
            boolean isIndecomposable,
            List<StateSpace> factors,
            int factorCount,
            List<Integer> factorSizes,
            List<IndexedFactorPair> factorCongruencePairs) {

        public FactorizationAnalysis {
            Objects.requireNonNull(factors, "factors must not be null");
            Objects.requireNonNull(factorSizes, "factorSizes must not be null");
            Objects.requireNonNull(factorCongruencePairs, "factorCongruencePairs must not be null");
            factors = List.copyOf(factors);
            factorSizes = List.copyOf(factorSizes);
            factorCongruencePairs = List.copyOf(factorCongruencePairs);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers: meet and join of congruences
    // -----------------------------------------------------------------------

    /**
     * Compute the meet (intersection) of two congruences.
     *
     * <p>x ≡ y (mod c1 ∧ c2) iff x ≡ y (mod c1) AND x ≡ y (mod c2).
     */
    static Congruence meetCongruences(StateSpace ss, Congruence c1, Congruence c2) {
        Map<Integer, Set<Integer>> class1 = new HashMap<>();
        for (Set<Integer> cls : c1.classes()) {
            for (int s : cls) class1.put(s, cls);
        }
        Map<Integer, Set<Integer>> class2 = new HashMap<>();
        for (Set<Integer> cls : c2.classes()) {
            for (int s : cls) class2.put(s, cls);
        }

        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);

        Map<List<Set<Integer>>, Set<Integer>> groups = new HashMap<>();
        for (int s : statesList) {
            Set<Integer> k1 = class1.getOrDefault(s, Set.of(s));
            Set<Integer> k2 = class2.getOrDefault(s, Set.of(s));
            List<Set<Integer>> key = List.of(k1, k2);
            groups.computeIfAbsent(key, k -> new HashSet<>()).add(s);
        }

        List<Set<Integer>> classes = new ArrayList<>();
        for (Set<Integer> g : groups.values()) classes.add(Set.copyOf(g));
        classes.sort((a, b) -> Integer.compare(Collections.min(a), Collections.min(b)));

        return new Congruence(classes, classes.size(), List.of());
    }

    /** Check if two congruences have the same partition (as sets of sets). */
    static boolean congruencesEqual(Congruence c1, Congruence c2) {
        if (c1.numClasses() != c2.numClasses()) return false;
        Set<Set<Integer>> s1 = new HashSet<>(c1.classes());
        Set<Set<Integer>> s2 = new HashSet<>(c2.classes());
        return s1.equals(s2);
    }

    // ---- Local union-find + congruence-join (CongruenceChecker.joinCongruences is private) ----

    private static final class UnionFind {
        private final Map<Integer, Integer> parent = new HashMap<>();
        private final Map<Integer, Integer> rank = new HashMap<>();

        UnionFind(Set<Integer> elements) {
            for (int e : elements) {
                parent.put(e, e);
                rank.put(e, 0);
            }
        }

        int find(int x) {
            while (parent.get(x) != x) {
                parent.put(x, parent.get(parent.get(x)));
                x = parent.get(x);
            }
            return x;
        }

        boolean union(int x, int y) {
            int rx = find(x), ry = find(y);
            if (rx == ry) return false;
            if (rank.get(rx) < rank.get(ry)) { int t = rx; rx = ry; ry = t; }
            parent.put(ry, rx);
            if (rank.get(rx).equals(rank.get(ry))) {
                rank.put(rx, rank.get(rx) + 1);
            }
            return true;
        }

        List<Set<Integer>> partition() {
            Map<Integer, Set<Integer>> groups = new HashMap<>();
            for (int e : parent.keySet()) {
                groups.computeIfAbsent(find(e), k -> new HashSet<>()).add(e);
            }
            List<Set<Integer>> out = new ArrayList<>();
            for (Set<Integer> g : groups.values()) out.add(Set.copyOf(g));
            out.sort((a, b) -> Integer.compare(Collections.min(a), Collections.min(b)));
            return out;
        }
    }

    static Congruence joinCongruences(StateSpace ss, Congruence c1, Congruence c2) {
        UnionFind uf = new UnionFind(ss.states());

        for (Set<Integer> cls : c1.classes()) {
            List<Integer> elems = new ArrayList<>(cls);
            Collections.sort(elems);
            for (int i = 1; i < elems.size(); i++) uf.union(elems.get(0), elems.get(i));
        }
        for (Set<Integer> cls : c2.classes()) {
            List<Integer> elems = new ArrayList<>(cls);
            Collections.sort(elems);
            for (int i = 1; i < elems.size(); i++) uf.union(elems.get(0), elems.get(i));
        }

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
    // Public API: Factor congruences
    // -----------------------------------------------------------------------

    /** Find all complementary (factor) pairs in Con(L). */
    public static List<FactorPair> findFactorCongruences(StateSpace ss) {
        return findFactorCongruences(ss, null);
    }

    public static List<FactorPair> findFactorCongruences(StateSpace ss, LatticeResult lr) {
        LatticeResult result = (lr == null) ? LatticeChecker.checkLattice(ss) : lr;
        if (!result.isLattice()) return List.of();

        var conLat = CongruenceChecker.congruenceLattice(ss);
        List<Congruence> congs = conLat.congruences();
        int n = congs.size();
        if (n <= 1) return List.of();

        Congruence identity = congs.get(conLat.bottom());
        Congruence total = congs.get(conLat.top());
        int nStates = ss.states().size();

        List<FactorPair> pairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Congruence ci = congs.get(i);
                Congruence cj = congs.get(j);
                if (ci.isTrivialBottom() || ci.isTrivialTop()) continue;
                if (cj.isTrivialBottom() || cj.isTrivialTop()) continue;

                Congruence meet = meetCongruences(ss, ci, cj);
                if (!congruencesEqual(meet, identity)) continue;

                Congruence join = joinCongruences(ss, ci, cj);
                if (!congruencesEqual(join, total)) continue;

                if (ci.numClasses() * cj.numClasses() != nStates) continue;

                pairs.add(new FactorPair(ci, cj));
            }
        }
        return pairs;
    }

    /** Find factor congruence pairs as indices into Con(L). */
    public static List<IndexedFactorPair> findFactorCongruencesIndexed(StateSpace ss) {
        return findFactorCongruencesIndexed(ss, null);
    }

    public static List<IndexedFactorPair> findFactorCongruencesIndexed(
            StateSpace ss, LatticeResult lr) {
        LatticeResult result = (lr == null) ? LatticeChecker.checkLattice(ss) : lr;
        if (!result.isLattice()) return List.of();

        var conLat = CongruenceChecker.congruenceLattice(ss);
        List<Congruence> congs = conLat.congruences();
        int n = congs.size();
        if (n <= 1) return List.of();

        Congruence identity = congs.get(conLat.bottom());
        Congruence total = congs.get(conLat.top());
        int nStates = ss.states().size();

        List<IndexedFactorPair> pairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Congruence ci = congs.get(i);
                Congruence cj = congs.get(j);
                if (ci.isTrivialBottom() || ci.isTrivialTop()) continue;
                if (cj.isTrivialBottom() || cj.isTrivialTop()) continue;

                Congruence meet = meetCongruences(ss, ci, cj);
                if (!congruencesEqual(meet, identity)) continue;
                Congruence join = joinCongruences(ss, ci, cj);
                if (!congruencesEqual(join, total)) continue;
                if (ci.numClasses() * cj.numClasses() != nStates) continue;

                pairs.add(new IndexedFactorPair(i, j));
            }
        }
        return pairs;
    }

    // -----------------------------------------------------------------------
    // Public API: Decomposability
    // -----------------------------------------------------------------------

    public static boolean isDirectlyDecomposable(StateSpace ss) {
        return isDirectlyDecomposable(ss, null);
    }

    public static boolean isDirectlyDecomposable(StateSpace ss, LatticeResult lr) {
        LatticeResult result = (lr == null) ? LatticeChecker.checkLattice(ss) : lr;
        if (!result.isLattice()) return false;
        if (ss.states().size() <= 2) return false;
        return !findFactorCongruences(ss, result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Public API: Factorize
    // -----------------------------------------------------------------------

    /**
     * Recursively decompose L(S) into directly indecomposable factors.
     */
    public static List<StateSpace> factorize(StateSpace ss) {
        return factorize(ss, null);
    }

    public static List<StateSpace> factorize(StateSpace ss, LatticeResult lr) {
        LatticeResult result = (lr == null) ? LatticeChecker.checkLattice(ss) : lr;
        if (!result.isLattice()) return List.of(ss);
        if (ss.states().size() <= 2) return List.of(ss);

        List<FactorPair> pairs = findFactorCongruences(ss, result);
        if (pairs.isEmpty()) return List.of(ss);

        FactorPair first = pairs.get(0);
        StateSpace factor1 = CongruenceChecker.quotientLattice(ss, first.theta());
        StateSpace factor2 = CongruenceChecker.quotientLattice(ss, first.phi());

        List<StateSpace> out = new ArrayList<>();
        out.addAll(factorize(factor1));
        out.addAll(factorize(factor2));
        return out;
    }

    // -----------------------------------------------------------------------
    // Public API: Full analysis
    // -----------------------------------------------------------------------

    public static FactorizationAnalysis analyzeFactorization(StateSpace ss) {
        return analyzeFactorization(ss, null);
    }

    public static FactorizationAnalysis analyzeFactorization(StateSpace ss, LatticeResult lr) {
        LatticeResult result = (lr == null) ? LatticeChecker.checkLattice(ss) : lr;
        if (!result.isLattice()) {
            return new FactorizationAnalysis(
                    false, true,
                    List.of(ss), 1,
                    List.of(ss.states().size()),
                    List.of());
        }

        boolean decomposable = isDirectlyDecomposable(ss, result);
        List<StateSpace> factors = factorize(ss, result);
        List<IndexedFactorPair> indexed = findFactorCongruencesIndexed(ss, result);

        List<Integer> sizes = new ArrayList<>();
        for (StateSpace f : factors) sizes.add(f.states().size());

        return new FactorizationAnalysis(
                decomposable, !decomposable,
                factors, factors.size(),
                sizes, indexed);
    }
}
