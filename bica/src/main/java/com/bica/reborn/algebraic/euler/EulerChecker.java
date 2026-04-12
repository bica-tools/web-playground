package com.bica.reborn.algebraic.euler;

import com.bica.reborn.algebraic.zeta.OrderComplex;
import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Euler characteristic consolidation for session type lattices
 * (port of Python {@code reticulate.euler}, Step 30v).
 *
 * <p>Consolidates Euler characteristic computation from multiple modules
 * (order complex, homology, Moebius) into a single diagnostic interface.
 *
 * <p>Key theorems verified:
 * <ul>
 *   <li><b>Philip Hall's theorem</b>: mu(top, bot) = reduced_euler(Delta(P_bar))</li>
 *   <li><b>Kuenneth formula</b>: chi(L1 x L2) = chi(L1) * chi(L2)</li>
 * </ul>
 *
 * <p>Provides interval-level Euler distribution as a protocol complexity
 * diagnostic.
 */
public final class EulerChecker {

    private EulerChecker() {}

    // =======================================================================
    // Result types
    // =======================================================================

    /** Result of verifying Philip Hall's theorem. */
    public record HallVerification(int moebiusValue, int reducedEuler, boolean verified) {}

    /** Result of verifying the Kuenneth multiplicativity formula. */
    public record KunnethVerification(
            int chiLeft, int chiRight, int chiProduct,
            int expected, boolean verified) {}

    /** Result of comparing multiple Euler characteristic methods. */
    public record ComparisonResult(
            int fromFaces, int fromMoebius, int fromBetti,
            boolean allAgree, List<Integer> fVector, List<Integer> bettiNumbers) {}

    /** Complete Euler characteristic analysis. */
    public record EulerAnalysis(
            int eulerCharacteristic,
            int reducedEuler,
            HallVerification hall,
            Map<Long, Integer> intervalSeries,
            Map<Integer, Integer> distribution,
            List<int[]> obstructions,
            ComparisonResult comparison,
            int numIntervals) {}

    // =======================================================================
    // Core functions
    // =======================================================================

    /**
     * Euler characteristic chi = sum_{k>=0} (-1)^k f_k.
     * Delegates to {@link OrderComplex#eulerCharacteristic(StateSpace)}.
     */
    public static int eulerCharacteristic(StateSpace ss) {
        return OrderComplex.eulerCharacteristic(ss);
    }

    /**
     * Reduced Euler characteristic chi_tilde.
     * By Philip Hall's theorem, this equals mu(top, bot).
     * Delegates to {@link OrderComplex#reducedEulerCharacteristic(StateSpace)}.
     */
    public static int reducedEulerCharacteristic(StateSpace ss) {
        return OrderComplex.reducedEulerCharacteristic(ss);
    }

    // =======================================================================
    // Hall's theorem verification
    // =======================================================================

    /**
     * Verify Philip Hall's theorem: mu(top, bot) = chi_tilde(Delta(P_bar)).
     *
     * <p>Special case: when top == bottom (single-state lattice), mu(x,x) = 1
     * by definition and the open interval is empty. Treated as trivially verified.
     */
    public static HallVerification verifyHallTheorem(StateSpace ss) {
        int muVal = moebiusTopBottom(ss);
        int redEuler = reducedEulerCharacteristic(ss);

        if (ss.top() == ss.bottom()) {
            return new HallVerification(muVal, redEuler, true);
        }

        return new HallVerification(muVal, redEuler, muVal == redEuler);
    }

    // =======================================================================
    // Kuenneth multiplicativity
    // =======================================================================

    /**
     * Verify Kuenneth formula: mu(L1 x L2) = mu(L1) * mu(L2).
     *
     * <p>For finite lattices, the Moebius function is multiplicative under
     * direct product. For session types, the parallel constructor produces
     * the product lattice.
     */
    public static KunnethVerification verifyKunneth(
            StateSpace ssLeft, StateSpace ssRight, StateSpace ssProduct) {
        int muL = moebiusTopBottom(ssLeft);
        int muR = moebiusTopBottom(ssRight);
        int muP = moebiusTopBottom(ssProduct);
        int expected = muL * muR;
        return new KunnethVerification(muL, muR, muP, expected, muP == expected);
    }

    // =======================================================================
    // Interval Euler series
    // =======================================================================

    /**
     * Moebius function mu(x, y) for all comparable pairs x >= y.
     *
     * <p>Returns a map from packed pair (x,y) to mu(x,y). Pairs are packed
     * as {@code ((long)x << 32) | (y & 0xFFFFFFFFL)}.
     */
    public static Map<Long, Integer> eulerSeries(StateSpace ss) {
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
        Map<Long, Integer> result = new HashMap<>();
        for (var entry : mu.entrySet()) {
            long key = packPair(entry.getKey().x(), entry.getKey().y());
            result.put(key, entry.getValue());
        }
        return result;
    }

    /**
     * Histogram of interval mu values: value -> count.
     *
     * <p>Counts how many intervals [x, y] (x != y) have each mu value.
     */
    public static Map<Integer, Integer> intervalEulerDistribution(StateSpace ss) {
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
        Map<Integer, Integer> dist = new TreeMap<>();
        for (var entry : mu.entrySet()) {
            if (entry.getKey().x() != entry.getKey().y()) {
                dist.merge(entry.getValue(), 1, Integer::sum);
            }
        }
        return dist;
    }

    // =======================================================================
    // Euler obstructions
    // =======================================================================

    /**
     * Intervals where |mu(x, y)| > 1.
     *
     * <p>These are indicators of non-distributivity. In a distributive
     * lattice, |mu(x, y)| <= 1 for all intervals.
     *
     * @return list of [x, y, mu_value] triples sorted by |mu| descending
     */
    public static List<int[]> eulerObstruction(StateSpace ss) {
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
        List<int[]> obs = new ArrayList<>();
        for (var entry : mu.entrySet()) {
            int x = entry.getKey().x();
            int y = entry.getKey().y();
            int val = entry.getValue();
            if (x != y && Math.abs(val) > 1) {
                obs.add(new int[]{x, y, val});
            }
        }
        obs.sort((a, b) -> Integer.compare(Math.abs(b[2]), Math.abs(a[2])));
        return obs;
    }

    // =======================================================================
    // Betti numbers (simplicial homology, inlined)
    // =======================================================================

    /**
     * Compute Betti numbers b_k = rank(H_k) of the order complex.
     *
     * <p>Uses integer row reduction (fraction-free Gaussian elimination)
     * to compute boundary matrix ranks. No external dependencies.
     */
    public static List<Integer> bettiNumbers(StateSpace ss) {
        List<List<Integer>> chains = OrderComplex.enumerateChains(ss);
        // Group by dimension (chain length - 1; empty chain = dim -1)
        // Non-empty chains only for simplices
        List<List<List<Integer>>> byDim = new ArrayList<>();
        int maxDim = -1;
        for (List<Integer> chain : chains) {
            if (chain.isEmpty()) continue; // skip empty chain
            int dim = chain.size() - 1;
            if (dim > maxDim) {
                while (byDim.size() <= dim) byDim.add(new ArrayList<>());
                maxDim = dim;
            }
            byDim.get(dim).add(chain);
        }

        if (maxDim < 0) return List.of();

        // Compute boundary matrices and their ranks
        // ranks[0] = rank(d_0) = 0 (no boundary below dim 0)
        int[] ranks = new int[maxDim + 2];
        ranks[0] = 0;

        for (int k = 1; k <= maxDim; k++) {
            List<List<Integer>> kSimplices = k < byDim.size() ? byDim.get(k) : List.of();
            List<List<Integer>> km1Simplices = (k - 1) < byDim.size() ? byDim.get(k - 1) : List.of();

            if (kSimplices.isEmpty() || km1Simplices.isEmpty()) {
                ranks[k] = 0;
                continue;
            }

            // Index (k-1)-simplices by their content for lookup
            Map<List<Integer>, Integer> faceIndex = new HashMap<>();
            for (int i = 0; i < km1Simplices.size(); i++) {
                faceIndex.put(km1Simplices.get(i), i);
            }

            int m = km1Simplices.size(); // rows
            int n = kSimplices.size();    // cols
            int[][] mat = new int[m][n];

            for (int j = 0; j < n; j++) {
                List<Integer> sigma = kSimplices.get(j);
                for (int i = 0; i < sigma.size(); i++) {
                    // Remove vertex i to get face
                    List<Integer> face = new ArrayList<>(sigma);
                    face.remove(i);
                    int sign = (i % 2 == 0) ? 1 : -1;
                    Integer row = faceIndex.get(face);
                    if (row != null) {
                        mat[row][j] += sign;
                    }
                }
            }

            ranks[k] = matrixRank(mat);
        }

        // Betti numbers: b_k = dim(C_k) - rank(d_k) - rank(d_{k+1})
        List<Integer> betti = new ArrayList<>();
        for (int k = 0; k <= maxDim; k++) {
            int dimCk = k < byDim.size() ? byDim.get(k).size() : 0;
            int rankDk = ranks[k];
            int rankDkPlus1 = (k + 1 < ranks.length) ? ranks[k + 1] : 0;
            betti.add(Math.max(0, dimCk - rankDk - rankDkPlus1));
        }

        return betti;
    }

    // =======================================================================
    // Multi-method comparison
    // =======================================================================

    /**
     * Compare three independent Euler characteristic computations.
     *
     * <ol>
     *   <li>Face count: chi = sum (-1)^k f_k (from order complex).</li>
     *   <li>Moebius: mu(top, bot) (reduced Euler by Hall's theorem).</li>
     *   <li>Betti: chi = sum (-1)^k b_k (from simplicial homology).</li>
     * </ol>
     *
     * <p>All three should agree. Disagreement indicates a bug.
     */
    public static ComparisonResult compareEulerMethods(StateSpace ss) {
        int chiFaces = eulerCharacteristic(ss);
        int muVal = moebiusTopBottom(ss);

        List<Integer> betti = bettiNumbers(ss);
        int chiBetti = 0;
        for (int k = 0; k < betti.size(); k++) {
            chiBetti += (k % 2 == 0 ? 1 : -1) * betti.get(k);
        }

        List<Integer> fv = OrderComplex.fVector(ss);
        int redEuler = reducedEulerCharacteristic(ss);

        boolean allAgree;
        if (ss.top() == ss.bottom()) {
            allAgree = (chiFaces == chiBetti);
        } else {
            allAgree = (chiFaces == chiBetti) && (muVal == redEuler);
        }

        return new ComparisonResult(chiFaces, muVal, chiBetti, allAgree, fv, betti);
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    /**
     * Complete Euler characteristic analysis of a session type state space.
     */
    public static EulerAnalysis analyzeEuler(StateSpace ss) {
        int chi = eulerCharacteristic(ss);
        int red = reducedEulerCharacteristic(ss);
        HallVerification hall = verifyHallTheorem(ss);
        Map<Long, Integer> series = eulerSeries(ss);
        Map<Integer, Integer> dist = intervalEulerDistribution(ss);
        List<int[]> obs = eulerObstruction(ss);
        ComparisonResult comp = compareEulerMethods(ss);

        int nIntervals = 0;
        for (var entry : series.entrySet()) {
            long key = entry.getKey();
            int x = (int) (key >> 32);
            int y = (int) key;
            if (x != y) nIntervals++;
        }

        return new EulerAnalysis(chi, red, hall, series, dist, obs, comp, nIntervals);
    }

    // =======================================================================
    // Private helpers
    // =======================================================================

    /**
     * Compute mu(top, bottom) using the SCC-aware Zeta.mobiusFunction,
     * which handles recursive session types correctly.
     */
    private static int moebiusTopBottom(StateSpace ss) {
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
        return mu.getOrDefault(new Zeta.Pair(ss.top(), ss.bottom()), 0);
    }

    private static long packPair(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    /**
     * Integer matrix rank via fraction-free Gaussian elimination.
     */
    static int matrixRank(int[][] mat) {
        if (mat.length == 0 || mat[0].length == 0) return 0;

        int m = mat.length;
        int n = mat[0].length;
        // Work copy
        int[][] work = new int[m][n];
        for (int i = 0; i < m; i++) {
            System.arraycopy(mat[i], 0, work[i], 0, n);
        }

        int rank = 0;
        for (int col = 0; col < n && rank < m; col++) {
            // Find pivot
            int pivotRow = -1;
            for (int row = rank; row < m; row++) {
                if (work[row][col] != 0) {
                    pivotRow = row;
                    break;
                }
            }
            if (pivotRow < 0) continue;

            // Swap
            int[] tmp = work[rank];
            work[rank] = work[pivotRow];
            work[pivotRow] = tmp;

            // Eliminate below
            int pivotVal = work[rank][col];
            for (int row = rank + 1; row < m; row++) {
                if (work[row][col] != 0) {
                    int factor = work[row][col];
                    for (int c = 0; c < n; c++) {
                        work[row][c] = work[row][c] * pivotVal - factor * work[rank][c];
                    }
                    // GCD reduction to prevent overflow
                    int g = 0;
                    for (int c = 0; c < n; c++) {
                        g = gcd(g, Math.abs(work[row][c]));
                    }
                    if (g > 1) {
                        for (int c = 0; c < n; c++) {
                            work[row][c] /= g;
                        }
                    }
                }
            }

            rank++;
        }

        return rank;
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }
}
