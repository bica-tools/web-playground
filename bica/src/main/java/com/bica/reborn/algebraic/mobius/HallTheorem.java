package com.bica.reborn.algebraic.mobius;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Philip Hall's theorem verification for session type lattices.
 *
 * <p>Hall's theorem states that the Moebius function can be computed by
 * counting chains in the interval:
 * <pre>
 *   mu(x, y) = sum_{k >= 1} (-1)^k * c_k
 * </pre>
 * where c_k is the number of chains of length k from x down to y.
 *
 * <p>A chain of length k is a sequence x = z_0 > z_1 > ... > z_k = y
 * where each z_i strictly above z_{i+1} in the poset ordering.
 */
public final class HallTheorem {

    private HallTheorem() {}

    /**
     * Verify Philip Hall's theorem for the interval [top, bottom].
     *
     * <p>Checks that mu(top, bottom) equals the alternating sum of chain counts.
     *
     * @param ss the state space
     * @return true if Hall's theorem is verified
     */
    public static boolean verify(StateSpace ss) {
        if (ss.states().size() <= 1) return true;

        int mobiusVal = MoebiusFunction.moebiusValue(ss);
        Map<Integer, Integer> chainCounts = countChains(ss, ss.top(), ss.bottom());

        int alternatingSum = 0;
        for (var entry : chainCounts.entrySet()) {
            int k = entry.getKey();
            int count = entry.getValue();
            alternatingSum += (k % 2 == 0 ? 1 : -1) * count;
        }

        return mobiusVal == alternatingSum;
    }

    /**
     * Compute the Euler characteristic mu(top, bottom).
     *
     * @param ss the state space
     * @return mu(top, bottom)
     */
    public static int eulerCharacteristic(StateSpace ss) {
        return MoebiusFunction.moebiusValue(ss);
    }

    /**
     * Count chains of each length from top to bottom.
     *
     * <p>A chain of length k from x to y is a sequence x = z_0 > z_1 > ... > z_k = y
     * where each z_i > z_{i+1} (strict order, not necessarily covering).
     *
     * @param ss     the state space
     * @param top    upper bound
     * @param bottom lower bound
     * @return mapping from chain length k to count of chains of that length
     */
    public static Map<Integer, Integer> countChains(StateSpace ss, int top, int bottom) {
        if (top == bottom) return Map.of();

        Map<Integer, Set<Integer>> reach = AlgebraicChecker.reachability(ss);

        // Check top can reach bottom
        if (!reach.get(top).contains(bottom)) {
            return Map.of();
        }

        // Find elements strictly between top and bottom
        List<Integer> between = new ArrayList<>();
        for (int s : ss.states()) {
            if (s == top || s == bottom) continue;
            if (reach.get(top).contains(s) && reach.get(s).contains(bottom)) {
                between.add(s);
            }
        }

        Map<Integer, Integer> counts = new TreeMap<>();

        // DFS to enumerate chains: top > z_1 > ... > z_{k-1} > bottom
        // A chain of length 1 is top > bottom (direct, no intermediaries)
        dfsChains(top, bottom, between, reach, 0, new HashSet<>(), counts);

        return counts;
    }

    private static void dfsChains(
            int current, int bottom, List<Integer> between,
            Map<Integer, Set<Integer>> reach,
            int depth, Set<Integer> used,
            Map<Integer, Integer> counts) {

        // Close the chain: current > bottom (chain of length depth+1)
        if (reach.get(current).contains(bottom) && current != bottom) {
            counts.merge(depth + 1, 1, Integer::sum);
        }

        // Extend through an unused intermediate
        for (int s : between) {
            if (used.contains(s)) continue;
            // current > s (s reachable from current, s != current)
            if (reach.get(current).contains(s) && s != current) {
                used.add(s);
                dfsChains(s, bottom, between, reach, depth + 1, used, counts);
                used.remove(s);
            }
        }
    }

    /**
     * Verify Hall's theorem for a specific interval [x, y].
     *
     * @param ss the state space
     * @param x  upper bound
     * @param y  lower bound
     * @return true if mu(x, y) equals the alternating chain sum
     */
    public static boolean verifyInterval(StateSpace ss, int x, int y) {
        if (x == y) return true; // mu(x,x) = 1 by convention

        int mobiusVal = MoebiusFunction.moebiusValueForPair(ss, x, y);
        Map<Integer, Integer> chainCounts = countChains(ss, x, y);

        int alternatingSum = 0;
        for (var entry : chainCounts.entrySet()) {
            int k = entry.getKey();
            int count = entry.getValue();
            alternatingSum += (k % 2 == 0 ? 1 : -1) * count;
        }

        return mobiusVal == alternatingSum;
    }
}
