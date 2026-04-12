package com.bica.reborn.lattice;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Join and meet irreducible analysis for session type lattices.
 *
 * <p>An element j of a lattice L is <strong>join-irreducible</strong> if it has exactly
 * one lower cover in the Hasse diagram and is not the bottom. Dually, m is
 * <strong>meet-irreducible</strong> if it has exactly one upper cover and is not the top.
 *
 * <p>For distributive lattices, Birkhoff's representation theorem says L is isomorphic
 * to the lattice of downsets of its poset of join-irreducibles J(L).
 */
public final class IrreduciblesChecker {

    private IrreduciblesChecker() {}

    // -----------------------------------------------------------------------
    // Hasse diagram (covering relation)
    // -----------------------------------------------------------------------

    /**
     * Compute covering pairs (a, b) where a covers b (a &gt; b, nothing between).
     *
     * @return list of (upper, lower) pairs
     */
    public static List<int[]> hasseEdges(StateSpace ss) {
        var q = DistributivityChecker.QuotientPoset.build(ss);
        List<int[]> edges = new ArrayList<>();
        for (int a : q.nodes()) {
            Set<Integer> direct = q.fwdAdj().get(a);
            for (int b : direct) {
                boolean isCovering = true;
                for (int c : direct) {
                    if (c == b) continue;
                    if (q.fwdReach().get(c).contains(b)) {
                        isCovering = false;
                        break;
                    }
                }
                if (isCovering) {
                    edges.add(new int[]{q.rep().get(a), q.rep().get(b)});
                }
            }
        }
        return edges;
    }

    /**
     * For each state, compute its lower covers (elements it covers).
     */
    public static Map<Integer, List<Integer>> lowerCovers(StateSpace ss) {
        var hasse = hasseEdges(ss);
        Map<Integer, List<Integer>> result = new HashMap<>();
        for (int s : ss.states()) result.put(s, new ArrayList<>());
        for (int[] edge : hasse) {
            result.computeIfAbsent(edge[0], k -> new ArrayList<>()).add(edge[1]);
        }
        return result;
    }

    /**
     * For each state, compute its upper covers (elements that cover it).
     */
    public static Map<Integer, List<Integer>> upperCovers(StateSpace ss) {
        var hasse = hasseEdges(ss);
        Map<Integer, List<Integer>> result = new HashMap<>();
        for (int s : ss.states()) result.put(s, new ArrayList<>());
        for (int[] edge : hasse) {
            result.computeIfAbsent(edge[1], k -> new ArrayList<>()).add(edge[0]);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Join and meet irreducibles
    // -----------------------------------------------------------------------

    /**
     * Find all join-irreducible elements: exactly one lower cover, not bottom.
     */
    public static Set<Integer> joinIrreducibles(StateSpace ss) {
        var lc = lowerCovers(ss);
        Set<Integer> result = new HashSet<>();
        for (int s : ss.states()) {
            if (s == ss.bottom()) continue;
            List<Integer> covers = lc.getOrDefault(s, List.of());
            if (covers.size() == 1) result.add(s);
        }
        return result;
    }

    /**
     * Find all meet-irreducible elements: exactly one upper cover, not top.
     */
    public static Set<Integer> meetIrreducibles(StateSpace ss) {
        var uc = upperCovers(ss);
        Set<Integer> result = new HashSet<>();
        for (int s : ss.states()) {
            if (s == ss.top()) continue;
            List<Integer> covers = uc.getOrDefault(s, List.of());
            if (covers.size() == 1) result.add(s);
        }
        return result;
    }

    /**
     * Check whether a specific state is join-irreducible.
     */
    public static boolean isJoinIrreducible(StateSpace ss, int state) {
        if (state == ss.bottom()) return false;
        if (!ss.states().contains(state))
            throw new IllegalArgumentException("State " + state + " not in state space");
        var lc = lowerCovers(ss);
        return lc.getOrDefault(state, List.of()).size() == 1;
    }

    /**
     * Check whether a specific state is meet-irreducible.
     */
    public static boolean isMeetIrreducible(StateSpace ss, int state) {
        if (state == ss.top()) return false;
        if (!ss.states().contains(state))
            throw new IllegalArgumentException("State " + state + " not in state space");
        var uc = upperCovers(ss);
        return uc.getOrDefault(state, List.of()).size() == 1;
    }

    // -----------------------------------------------------------------------
    // Birkhoff dual
    // -----------------------------------------------------------------------

    /**
     * Result of Birkhoff dual computation.
     *
     * @param joinIrr         Set of join-irreducible elements
     * @param meetIrr         Set of meet-irreducible elements
     * @param dualOrder       Partial order on J(L): pairs (a,b) where a ≤ b
     * @param dualHasse       Covering pairs of the dual poset
     * @param isDistributive  Whether the original lattice is distributive
     * @param downsetCount    Number of downsets of J(L)
     * @param latticeSize     Number of states in the original lattice
     * @param quotientSize    Number of SCC-quotient nodes
     * @param compressionRatio |J(L)| / |L|
     */
    public record BirkhoffDualResult(
            Set<Integer> joinIrr,
            Set<Integer> meetIrr,
            Set<int[]> dualOrder,
            Set<int[]> dualHasse,
            boolean isDistributive,
            int downsetCount,
            int latticeSize,
            int quotientSize,
            double compressionRatio) {}

    /**
     * Compute the Birkhoff dual: the poset of join-irreducibles J(L).
     *
     * <p>For a distributive lattice L, Birkhoff's representation theorem says
     * L is isomorphic to the lattice of downsets of J(L).
     */
    public static BirkhoffDualResult birkhoffDual(StateSpace ss) {
        Set<Integer> ji = joinIrreducibles(ss);
        Set<Integer> mi = meetIrreducibles(ss);

        var q = DistributivityChecker.QuotientPoset.build(ss);

        // Build order relation on J(L)
        Set<int[]> orderPairs = new HashSet<>();
        for (int j1 : ji) {
            for (int j2 : ji) {
                int n2 = q.stateToNode().get(j2);
                // j2 >= j1 means j2 reaches j1
                Set<Integer> reachableReps = new HashSet<>();
                for (int n : q.fwdReach().get(n2)) {
                    reachableReps.add(q.rep().get(n));
                }
                if (reachableReps.contains(j1)) {
                    orderPairs.add(new int[]{j1, j2}); // j1 <= j2
                }
            }
        }

        // Compute Hasse edges of dual poset
        Set<int[]> dualHasse = new HashSet<>();
        for (int[] pair : orderPairs) {
            int j1 = pair[0], j2 = pair[1];
            if (j1 == j2) continue;
            boolean isCover = true;
            for (int j3 : ji) {
                if (j3 == j1 || j3 == j2) continue;
                if (containsPair(orderPairs, j1, j3) && containsPair(orderPairs, j3, j2)) {
                    isCover = false;
                    break;
                }
            }
            if (isCover) dualHasse.add(new int[]{j1, j2});
        }

        // Distributivity
        var distResult = DistributivityChecker.checkDistributive(ss);
        boolean isDist = distResult.isDistributive();

        // Count downsets
        int downsets = countDownsets(ji, orderPairs);

        int latticeSize = ss.states().size();
        int quotientSize = q.nodes().size();
        double ratio = latticeSize > 0 ? (double) ji.size() / latticeSize : 0.0;

        return new BirkhoffDualResult(
                Set.copyOf(ji), Set.copyOf(mi), orderPairs, dualHasse,
                isDist, downsets, latticeSize, quotientSize, ratio);
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Summary of irreducible analysis.
     */
    public record IrreduciblesResult(
            boolean isLattice,
            Set<Integer> joinIrr,
            Set<Integer> meetIrr,
            int numJoinIrr,
            int numMeetIrr,
            int latticeSize,
            boolean isSelfDualCardinality,
            double joinDensity,
            double meetDensity,
            BirkhoffDualResult birkhoff) {}

    /**
     * Full irreducibles analysis for a state space.
     */
    public static IrreduciblesResult analyzeIrreducibles(StateSpace ss) {
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) {
            return new IrreduciblesResult(
                    false, Set.of(), Set.of(), 0, 0,
                    ss.states().size(), true, 0.0, 0.0, null);
        }

        Set<Integer> ji = joinIrreducibles(ss);
        Set<Integer> mi = meetIrreducibles(ss);
        int n = ss.states().size();

        BirkhoffDualResult bd = birkhoffDual(ss);

        return new IrreduciblesResult(
                true, Set.copyOf(ji), Set.copyOf(mi),
                ji.size(), mi.size(), n,
                ji.size() == mi.size(),
                n > 0 ? (double) ji.size() / n : 0.0,
                n > 0 ? (double) mi.size() / n : 0.0,
                bd);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static boolean containsPair(Set<int[]> pairs, int a, int b) {
        for (int[] p : pairs) {
            if (p[0] == a && p[1] == b) return true;
        }
        return false;
    }

    /**
     * Count downsets (order ideals) of a finite poset by brute-force enumeration.
     */
    static int countDownsets(Set<Integer> elements, Set<int[]> order) {
        List<Integer> elems = new ArrayList<>(elements);
        Collections.sort(elems);
        int n = elems.size();
        if (n == 0) return 1;

        int count = 0;
        for (int mask = 0; mask < (1 << n); mask++) {
            Set<Integer> subset = new HashSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) subset.add(elems.get(i));
            }

            boolean isDownset = true;
            for (int x : subset) {
                for (int[] pair : order) {
                    if (pair[1] == x && !subset.contains(pair[0])) {
                        isDownset = false;
                        break;
                    }
                }
                if (!isDownset) break;
            }
            if (isDownset) count++;
        }
        return count;
    }
}
