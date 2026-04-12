package com.bica.reborn.species;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Combinatorial species analysis for session type lattices (port of Python
 * {@code reticulate.species}).
 *
 * <p>Combinatorial species (Joyal, 1981) provide a categorical framework for
 * counting structures. For session type lattices, we model each lattice as
 * a species F where:
 * <ul>
 *   <li>F[n] = isomorphism classes of session type lattices on n elements</li>
 *   <li>The type-generating function t_F(x) = sum_n |F[n]/~| x^n / n!</li>
 *   <li>The cycle index Z_F encodes symmetry structure</li>
 * </ul>
 *
 * <p>Key operations:
 * <ul>
 *   <li><b>Species product</b>: F * G corresponds to parallel composition (||)</li>
 *   <li><b>Species sum</b>: F + G corresponds to external choice (&amp;)</li>
 *   <li><b>Isomorphism types</b>: equivalence classes under order-preserving bijection</li>
 * </ul>
 *
 * <p>All methods are static. This is a faithful Java port of
 * {@code reticulate/reticulate/species.py}.
 */
public final class SpeciesChecker {

    private SpeciesChecker() {}

    // =========================================================================
    // Result types
    // =========================================================================

    /** Complete species analysis result. */
    public record SpeciesResult(
            int numStates,
            int numAutomorphisms,
            List<Map<Integer, Integer>> automorphismGenerators,
            List<Double> typeGeneratingCoefficients,
            int isomorphismTypeCount,
            List<CycleIndexTerm> cycleIndexTerms,
            List<Double> exponentialFormulaTerms,
            double symmetryFactor) {}

    /** A single term in the cycle index: coefficient * p_{partition}. */
    public record CycleIndexTerm(double coefficient, List<Integer> partition) {}

    /** Species product invariants. */
    public record SpeciesProductResult(
            int leftStates,
            int rightStates,
            int productStates,
            int leftAutomorphisms,
            int rightAutomorphisms,
            int productAutomorphismsUpper,
            int leftIsomorphismTypes,
            int rightIsomorphismTypes) {}

    /** Species sum invariants. */
    public record SpeciesSumResult(
            int leftStates,
            int rightStates,
            int sumStates,
            int leftIsomorphismTypes,
            int rightIsomorphismTypes) {}

    // =========================================================================
    // Automorphism group
    // =========================================================================

    /**
     * Find all automorphisms of the poset (order-preserving bijections to itself).
     *
     * <p>An automorphism is a permutation sigma: P -&gt; P such that
     * x &le; y iff sigma(x) &le; sigma(y).
     *
     * <p>Uses backtracking search with rank and degree pruning.
     */
    public static List<Map<Integer, Integer>> findAutomorphisms(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();

        if (n == 0) {
            return List.of(Map.of());
        }
        if (n == 1) {
            return List.of(Map.of(states.get(0), states.get(0)));
        }

        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        Map<Integer, Integer> rank = Zeta.computeRank(ss);

        // Group states by rank (automorphisms must preserve rank)
        Map<Integer, List<Integer>> rankGroups = new TreeMap<>();
        for (int s : states) {
            int r = rank.getOrDefault(s, 0);
            rankGroups.computeIfAbsent(r, k -> new ArrayList<>()).add(s);
        }

        // Order states by descending rank for assignment
        List<Integer> orderedStates = new ArrayList<>();
        List<Integer> ranksSorted = new ArrayList<>(rankGroups.keySet());
        ranksSorted.sort(Collections.reverseOrder());
        for (int r : ranksSorted) {
            List<Integer> group = rankGroups.get(r);
            Collections.sort(group);
            orderedStates.addAll(group);
        }

        // Out-degree and in-degree as invariants
        Map<Integer, Integer> outDeg = new HashMap<>();
        Map<Integer, Integer> inDeg = new HashMap<>();
        for (int s : states) {
            outDeg.put(s, 0);
            inDeg.put(s, 0);
        }
        for (var t : ss.transitions()) {
            if (outDeg.containsKey(t.source())) {
                outDeg.merge(t.source(), 1, Integer::sum);
            }
            if (inDeg.containsKey(t.target())) {
                inDeg.merge(t.target(), 1, Integer::sum);
            }
        }

        List<Map<Integer, Integer>> automorphisms = new ArrayList<>();

        backtrack(0, n, orderedStates, rank, rankGroups, outDeg, inDeg,
                  reach, new HashMap<>(), new HashSet<>(), automorphisms);

        if (automorphisms.isEmpty()) {
            Map<Integer, Integer> identity = new LinkedHashMap<>();
            for (int s : states) identity.put(s, s);
            return List.of(identity);
        }
        return automorphisms;
    }

    private static void backtrack(
            int idx, int n,
            List<Integer> orderedStates,
            Map<Integer, Integer> rank,
            Map<Integer, List<Integer>> rankGroups,
            Map<Integer, Integer> outDeg,
            Map<Integer, Integer> inDeg,
            Map<Integer, Set<Integer>> reach,
            Map<Integer, Integer> mapping,
            Set<Integer> used,
            List<Map<Integer, Integer>> automorphisms) {

        if (automorphisms.size() > 1000) return;

        if (idx == n) {
            automorphisms.add(new LinkedHashMap<>(mapping));
            return;
        }

        int s = orderedStates.get(idx);
        int r = rank.getOrDefault(s, 0);
        List<Integer> candidates = rankGroups.get(r);

        for (int c : candidates) {
            if (used.contains(c)) continue;

            // Pruning: check degree invariants
            if (!outDeg.get(s).equals(outDeg.get(c)) || !inDeg.get(s).equals(inDeg.get(c))) {
                continue;
            }

            // Check order consistency with already-mapped elements
            boolean ok = true;
            for (var entry : mapping.entrySet()) {
                int mappedS = entry.getKey();
                int mappedC = entry.getValue();

                // s >= mappedS iff c >= mappedC
                boolean sAbove = reach.get(s).contains(mappedS);
                boolean cAbove = reach.get(c).contains(mappedC);
                if (sAbove != cAbove) { ok = false; break; }

                // mappedS >= s iff mappedC >= c
                boolean sBelow = reach.get(mappedS).contains(s);
                boolean cBelow = reach.get(mappedC).contains(c);
                if (sBelow != cBelow) { ok = false; break; }
            }

            if (ok) {
                mapping.put(s, c);
                used.add(c);
                backtrack(idx + 1, n, orderedStates, rank, rankGroups,
                          outDeg, inDeg, reach, mapping, used, automorphisms);
                mapping.remove(s);
                used.remove(c);
            }
        }
    }

    /**
     * Size of the automorphism group |Aut(P)|.
     */
    public static int automorphismGroupSize(StateSpace ss) {
        return findAutomorphisms(ss).size();
    }

    // =========================================================================
    // Isomorphism types (sub-posets)
    // =========================================================================

    /**
     * Find distinct isomorphism types of non-empty sub-posets.
     *
     * <p>Two subsets S1, S2 of P are isomorphic if there is an order-preserving
     * bijection between the induced sub-posets. Classification uses
     * (size, sorted-row-sums, sorted-col-sums) as invariant.
     *
     * @return representative element sets for each isomorphism class
     */
    public static List<Set<Integer>> isomorphismTypes(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();

        if (n == 0) return List.of();

        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);

        int maxSize = Math.min(n, 6);
        List<Set<Integer>> types = new ArrayList<>();
        Set<String> seenSignatures = new HashSet<>();

        // Size 1: always one type
        types.add(Set.of(states.get(0)));
        seenSignatures.add(subSignature(List.of(states.get(0)), reach));

        // Size 2 to maxSize
        for (int size = 2; size <= maxSize; size++) {
            for (List<Integer> combo : combinations(states, size)) {
                String sig = subSignature(combo, reach);
                if (seenSignatures.add(sig)) {
                    types.add(new LinkedHashSet<>(combo));
                }
            }
        }

        return types;
    }

    /**
     * Compute an isomorphism invariant signature for a sub-poset.
     */
    private static String subSignature(List<Integer> elems, Map<Integer, Set<Integer>> reach) {
        int k = elems.size();
        int[] rowSums = new int[k];
        int[] colSums = new int[k];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                if (reach.get(elems.get(i)).contains(elems.get(j))) {
                    rowSums[i]++;
                    colSums[j]++;
                }
            }
        }
        Arrays.sort(rowSums);
        Arrays.sort(colSums);
        return k + ":" + Arrays.toString(rowSums) + ":" + Arrays.toString(colSums);
    }

    // =========================================================================
    // Type-generating series
    // =========================================================================

    /**
     * Compute the type-generating series coefficients.
     *
     * <p>f_n counts the number of isomorphism types of sub-posets on n elements.
     *
     * @param ss    state space
     * @param maxN  maximum size to compute (0 = use number of states)
     * @return coefficients [a_0, a_1, ..., a_maxN]
     */
    public static List<Double> typeGeneratingSeries(StateSpace ss, int maxN) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();

        if (maxN <= 0) maxN = n;
        maxN = Math.min(maxN, Math.min(n, 7));

        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);

        double[] coefficients = new double[maxN + 1];
        coefficients[0] = 1.0; // empty sub-poset by convention

        for (int size = 1; size <= maxN; size++) {
            Set<String> seen = new HashSet<>();
            for (List<Integer> combo : combinations(states, size)) {
                int k = combo.size();
                int[] rowSums = new int[k];
                int[] colSums = new int[k];
                for (int i = 0; i < k; i++) {
                    for (int j = 0; j < k; j++) {
                        if (reach.get(combo.get(i)).contains(combo.get(j))) {
                            rowSums[i]++;
                            colSums[j]++;
                        }
                    }
                }
                Arrays.sort(rowSums);
                Arrays.sort(colSums);
                String sig = k + ":" + Arrays.toString(rowSums) + ":" + Arrays.toString(colSums);
                seen.add(sig);
            }
            coefficients[size] = seen.size();
        }

        List<Double> result = new ArrayList<>(maxN + 1);
        for (double c : coefficients) result.add(c);
        return result;
    }

    /** Overload with default maxN = 0. */
    public static List<Double> typeGeneratingSeries(StateSpace ss) {
        return typeGeneratingSeries(ss, 0);
    }

    // =========================================================================
    // Cycle index
    // =========================================================================

    /**
     * Extract cycle type (partition) from a permutation.
     * Returns sorted list of cycle lengths in descending order.
     */
    static List<Integer> partitionFromPermutation(Map<Integer, Integer> perm) {
        Set<Integer> visited = new HashSet<>();
        List<Integer> cycles = new ArrayList<>();

        List<Integer> keys = new ArrayList<>(perm.keySet());
        Collections.sort(keys);

        for (int start : keys) {
            if (visited.contains(start)) continue;
            int length = 0;
            int current = start;
            while (!visited.contains(current)) {
                visited.add(current);
                current = perm.get(current);
                length++;
            }
            cycles.add(length);
        }

        cycles.sort(Collections.reverseOrder());
        return cycles;
    }

    /**
     * Compute the cycle index of the automorphism group.
     *
     * <p>Z(Aut(P)) = (1/|Aut|) * sum_{sigma in Aut} p_{lambda(sigma)}
     *
     * @return list of (coefficient, partition) terms
     */
    public static List<CycleIndexTerm> cycleIndex(StateSpace ss) {
        List<Map<Integer, Integer>> auts = findAutomorphisms(ss);
        int nAut = auts.size();

        if (nAut == 0) {
            return List.of(new CycleIndexTerm(1.0, List.of(1)));
        }

        // Count cycle types
        Map<List<Integer>, Integer> typeCounts = new TreeMap<>((a, b) -> {
            int cmp = Integer.compare(a.size(), b.size());
            if (cmp != 0) return cmp;
            for (int i = 0; i < a.size(); i++) {
                cmp = Integer.compare(a.get(i), b.get(i));
                if (cmp != 0) return cmp;
            }
            return 0;
        });

        for (var perm : auts) {
            List<Integer> partition = partitionFromPermutation(perm);
            typeCounts.merge(partition, 1, Integer::sum);
        }

        List<CycleIndexTerm> terms = new ArrayList<>();
        for (var entry : typeCounts.entrySet()) {
            double coeff = (double) entry.getValue() / nAut;
            terms.add(new CycleIndexTerm(coeff, entry.getKey()));
        }

        return terms;
    }

    // =========================================================================
    // Species operations
    // =========================================================================

    /**
     * Compute species product invariants F * G.
     *
     * <p>The species product corresponds to disjoint union of structures,
     * which for session types is the parallel composition (||).
     */
    public static SpeciesProductResult speciesProduct(StateSpace ss1, StateSpace ss2) {
        int n1 = ss1.states().size();
        int n2 = ss2.states().size();

        int aut1 = automorphismGroupSize(ss1);
        int aut2 = automorphismGroupSize(ss2);

        return new SpeciesProductResult(
                n1, n2, n1 * n2,
                aut1, aut2, aut1 * aut2,
                isomorphismTypes(ss1).size(),
                isomorphismTypes(ss2).size());
    }

    /**
     * Compute species sum invariants F + G.
     *
     * <p>The species sum corresponds to disjoint union (external choice &amp;).
     */
    public static SpeciesSumResult speciesSum(StateSpace ss1, StateSpace ss2) {
        int n1 = ss1.states().size();
        int n2 = ss2.states().size();

        return new SpeciesSumResult(
                n1, n2, n1 + n2,
                isomorphismTypes(ss1).size(),
                isomorphismTypes(ss2).size());
    }

    // =========================================================================
    // Exponential formula
    // =========================================================================

    /**
     * Compute terms from the exponential formula.
     *
     * <p>If T(x) = sum a_n x^n/n! is the exponential generating function for
     * labeled structures, and C(x) = sum c_n x^n/n! counts connected ones,
     * then T(x) = exp(C(x)).
     *
     * <p>For a lattice (always connected), C = log(T) at the species level.
     * Returns the first few terms of log of the type-generating series.
     */
    public static List<Double> exponentialFormulaTerms(StateSpace ss, int maxN) {
        List<Double> tgs = typeGeneratingSeries(ss, maxN);
        int n = tgs.size();

        if (n <= 1) return new ArrayList<>(tgs);

        // Compute log of the series (formal power series log)
        double[] logCoeffs = new double[n];
        if (Math.abs(tgs.get(0) - 1.0) > 1e-12) {
            List<Double> result = new ArrayList<>(n);
            for (int i = 0; i < n; i++) result.add(0.0);
            return result;
        }

        for (int i = 1; i < n; i++) {
            double s = tgs.get(i);
            for (int k = 1; k < i; k++) {
                s -= k * logCoeffs[k] * tgs.get(i - k) / i;
            }
            logCoeffs[i] = s;
        }

        List<Double> result = new ArrayList<>(n);
        for (double c : logCoeffs) result.add(c);
        return result;
    }

    /** Overload with default maxN = 0. */
    public static List<Double> exponentialFormulaTerms(StateSpace ss) {
        return exponentialFormulaTerms(ss, 0);
    }

    // =========================================================================
    // Symmetry factor
    // =========================================================================

    /**
     * Compute |Aut(P)| / |P|! -- the symmetry factor.
     *
     * <p>A higher symmetry factor indicates more symmetry in the lattice.
     * Total order: Aut = {id}, factor = 1/n!.
     * Antichain: Aut = S_n, factor = 1.
     */
    public static double symmetryFactor(StateSpace ss) {
        int n = ss.states().size();
        if (n == 0) return 1.0;
        int autSize = automorphismGroupSize(ss);
        return (double) autSize / factorial(n);
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Complete combinatorial species analysis.
     */
    public static SpeciesResult analyzeSpecies(StateSpace ss) {
        List<Map<Integer, Integer>> auts = findAutomorphisms(ss);
        int nAut = auts.size();
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();

        List<Double> tgs = typeGeneratingSeries(ss);
        List<Set<Integer>> isoTypes = isomorphismTypes(ss);
        List<CycleIndexTerm> ci = cycleIndex(ss);
        List<Double> expTerms = exponentialFormulaTerms(ss);
        double sf = symmetryFactor(ss);

        // Extract generators (first few non-identity)
        Map<Integer, Integer> identity = new LinkedHashMap<>();
        for (int s : states) identity.put(s, s);
        List<Map<Integer, Integer>> generators = new ArrayList<>();
        for (var a : auts) {
            if (!a.equals(identity) && generators.size() < 5) {
                generators.add(a);
            }
        }

        return new SpeciesResult(
                n, nAut, generators, tgs, isoTypes.size(),
                ci, expTerms, sf);
    }

    // =========================================================================
    // Utility: combinations
    // =========================================================================

    /**
     * Generate all C(n, k) combinations of the given list.
     */
    private static List<List<Integer>> combinations(List<Integer> items, int k) {
        List<List<Integer>> result = new ArrayList<>();
        combinationsHelper(items, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static void combinationsHelper(
            List<Integer> items, int k, int start,
            List<Integer> current, List<List<Integer>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            combinationsHelper(items, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private static long factorial(int n) {
        long f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        return f;
    }
}
