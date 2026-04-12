package com.bica.reborn.algebraic.mobius;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Computes the Moebius function on session type lattices.
 *
 * <p>The Moebius function mu(x, y) is the inverse of the zeta function on a poset.
 * It encodes inclusion-exclusion and is the lattice-theoretic generalisation of
 * the number-theoretic Moebius function.
 *
 * <p>Key properties:
 * <ul>
 *   <li>mu(x, x) = 1 for all x</li>
 *   <li>mu(x, y) = -sum_{z: x >= z > y} mu(x, z) for x > y</li>
 *   <li>mu(top, bottom) is the Euler characteristic of the lattice</li>
 *   <li>mu is multiplicative under products: mu(L1 x L2) = mu(L1) * mu(L2)</li>
 * </ul>
 */
public final class MoebiusFunction {

    private MoebiusFunction() {}

    /**
     * Compute the full Moebius matrix M[i][j] = mu(state_i, state_j).
     *
     * <p>Delegates to {@link AlgebraicChecker#computeMobiusMatrix(StateSpace)}.
     *
     * @param ss the state space
     * @return Moebius matrix indexed by sorted state list
     */
    public static int[][] compute(StateSpace ss) {
        return AlgebraicChecker.computeMobiusMatrix(ss);
    }

    /**
     * Compute mu(top, bottom) -- the Euler characteristic of the lattice.
     *
     * @param ss the state space
     * @return mu(top, bottom)
     */
    public static int moebiusValue(StateSpace ss) {
        return AlgebraicChecker.computeMobiusValue(ss);
    }

    /**
     * Compute the Moebius spectrum: histogram of all mu(x, y) values
     * across comparable pairs x > y.
     *
     * @param ss the state space
     * @return mapping from mu value to count of intervals with that value
     */
    public static Map<Integer, Integer> moebiusSpectrum(StateSpace ss) {
        int[][] M = compute(ss);
        List<Integer> states = AlgebraicChecker.stateList(ss);
        Map<Integer, Set<Integer>> reach = AlgebraicChecker.reachability(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idx.put(states.get(i), i);
        }

        Map<Integer, Integer> spectrum = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            int x = states.get(i);
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                int y = states.get(j);
                // x > y means y reachable from x (and x != y)
                if (reach.get(x).contains(y)) {
                    spectrum.merge(M[i][j], 1, Integer::sum);
                }
            }
        }
        return spectrum;
    }

    /**
     * Compute mu(x, y) for a specific pair of states.
     *
     * @param ss the state space
     * @param x  the upper state
     * @param y  the lower state
     * @return mu(x, y), or 0 if x does not reach y
     */
    public static int moebiusValueForPair(StateSpace ss, int x, int y) {
        if (x == y) return 1;
        List<Integer> states = AlgebraicChecker.stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idx.put(states.get(i), i);
        }
        if (!idx.containsKey(x) || !idx.containsKey(y)) return 0;
        int[][] M = compute(ss);
        return M[idx.get(x)][idx.get(y)];
    }

    /**
     * Maximum absolute Moebius value across all intervals.
     *
     * <p>|mu(x,y)| <= 1 for all intervals is a necessary condition for
     * distributivity (though not sufficient -- N5 is a counterexample).
     *
     * @param ss the state space
     * @return max |mu(x, y)| across all comparable pairs
     */
    public static int maxAbsMoebius(StateSpace ss) {
        int[][] M = compute(ss);
        List<Integer> states = AlgebraicChecker.stateList(ss);
        Map<Integer, Set<Integer>> reach = AlgebraicChecker.reachability(ss);
        int n = states.size();

        int maxVal = 0;
        for (int i = 0; i < n; i++) {
            int x = states.get(i);
            for (int j = 0; j < n; j++) {
                int y = states.get(j);
                if (reach.get(x).contains(y)) {
                    maxVal = Math.max(maxVal, Math.abs(M[i][j]));
                }
            }
        }
        return maxVal;
    }
}
