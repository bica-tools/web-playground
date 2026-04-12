package com.bica.reborn.zeta_polynomial;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Zeta polynomial and chain counting for session type lattices (Step 32f).
 *
 * <p>The zeta polynomial Z(P, k) counts the number of multichains of length k
 * in a finite poset P:
 * <pre>
 *     Z(P, k) = |{(x_0, x_1, ..., x_k) : x_0 &le; x_1 &le; ... &le; x_k}|
 * </pre>
 *
 * <p>Key properties:
 * <ul>
 *   <li>Z(P, 1) = |P|: the number of states</li>
 *   <li>Z(P, 2) = number of comparable pairs + |P|</li>
 *   <li>Z(P, -1) = mu(0,1): Philip Hall's theorem connects to Moebius function</li>
 *   <li>Z(P, k) is a polynomial in k of degree = height of P</li>
 * </ul>
 *
 * <p>All computations handle cycles via SCC quotient (from {@link Zeta}).
 * This is a faithful Java port of {@code reticulate/reticulate/zeta_polynomial.py}.
 */
public final class ZetaPolynomialChecker {

    private ZetaPolynomialChecker() {}

    // =======================================================================
    // Result type
    // =======================================================================

    /**
     * Complete zeta polynomial analysis result.
     *
     * @param numStates          number of states in the quotient poset
     * @param coefficients       polynomial coefficients [a_0, a_1, ..., a_d]
     * @param degree             degree of the polynomial (= height of poset)
     * @param values             Z(P, k) for k = 0, 1, ..., degree+2
     * @param mobiusValue        Z(P, -1) = mu(top, bottom) (Philip Hall)
     * @param hallVerified       true iff Z(P, -1) matches mu(top, bottom)
     * @param chainCounts        mapping length -&gt; number of strict chains
     * @param multichainCounts   mapping k -&gt; Z(P, k) for small k
     * @param totalMultichainsK2 Z(P, 2) = comparable pairs + n
     * @param isPalindromic      true iff Z is palindromic (self-dual lattice hint)
     */
    public record ZetaPolynomialResult(
            int numStates,
            List<Double> coefficients,
            int degree,
            Map<Integer, Integer> values,
            int mobiusValue,
            boolean hallVerified,
            Map<Integer, Integer> chainCounts,
            Map<Integer, Integer> multichainCounts,
            int totalMultichainsK2,
            boolean isPalindromic) {}

    // =======================================================================
    // Quotient data
    // =======================================================================

    /** Bundle of quotient poset data. */
    private record QuotientData(
            List<Integer> reps,
            Map<Integer, Integer> sccMap,
            Map<Integer, Set<Integer>> sccMembers,
            Map<Integer, Set<Integer>> qReach) {}

    private static QuotientData quotientData(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        // Build quotient adjacency
        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new HashSet<>());
        for (var t : ss.transitions()) {
            int sr = sccMap.get(t.source());
            int tr = sccMap.get(t.target());
            if (sr != tr) qAdj.get(sr).add(tr);
        }

        // Quotient reachability
        Map<Integer, Set<Integer>> qReach = new HashMap<>();
        for (int r : reps) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(r);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (!visited.add(u)) continue;
                for (int v : qAdj.getOrDefault(u, Set.of())) stack.push(v);
            }
            qReach.put(r, visited);
        }

        return new QuotientData(reps, sccMap, sccMembers, qReach);
    }

    // =======================================================================
    // Multichain counting
    // =======================================================================

    /**
     * Count multichains of length k in the poset.
     *
     * <p>A multichain of length k is a sequence x_0 &le; x_1 &le; ... &le; x_k
     * (with repetition allowed). Works on SCC quotient for cycle handling.
     *
     * <p>Z(P, 0) = 1 (empty chain), Z(P, 1) = n (singletons).
     */
    public static int multichainCount(StateSpace ss, int k) {
        if (k < 0) return zetaPolyAtNegative(ss, k);
        if (k == 0) return 1;

        QuotientData qd = quotientData(ss);
        int nReps = qd.reps.size();
        if (nReps == 0) return 0;
        if (k == 1) return nReps;

        // DP: count k-multichains
        // mc_prev[r] = number of (i)-multichains ending at r
        Map<Integer, Integer> mcPrev = new HashMap<>();
        for (int r : qd.reps) mcPrev.put(r, 1); // length 1

        for (int step = 1; step < k; step++) {
            Map<Integer, Integer> mcNext = new HashMap<>();
            for (int r : qd.reps) mcNext.put(r, 0);
            for (int r : qd.reps) {
                // r can extend chains ending at s where s >= r (s reaches r)
                for (int s : qd.reps) {
                    if (qd.qReach.get(s).contains(r)) { // s >= r
                        mcNext.put(r, mcNext.get(r) + mcPrev.get(s));
                    }
                }
            }
            mcPrev = mcNext;
        }

        int total = 0;
        for (int v : mcPrev.values()) total += v;
        return total;
    }

    private static int zetaPolyAtNegative(StateSpace ss, int k) {
        if (k == -1) {
            Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
            return mu.getOrDefault(new Zeta.Pair(ss.top(), ss.bottom()), 0);
        }
        // For k < -1, use polynomial interpolation
        List<Double> coeffs = zetaPolynomialCoefficients(ss);
        return evalPoly(coeffs, k);
    }

    // =======================================================================
    // Strict chain counting
    // =======================================================================

    /**
     * Count strict chains of a given length (no repeated elements).
     *
     * <p>A strict chain of length k is x_0 &lt; x_1 &lt; ... &lt; x_k.
     */
    public static int chainCount(StateSpace ss, int length) {
        if (length < 0) return 0;
        if (length == 0) return 1; // empty chain

        QuotientData qd = quotientData(ss);
        if (length == 1) return qd.reps.size();

        // Build strict-below relation on quotient
        Map<Integer, List<Integer>> strictBelow = new HashMap<>();
        for (int r : qd.reps) {
            List<Integer> below = new ArrayList<>();
            for (int s : qd.reps) {
                if (r != s && qd.qReach.get(r).contains(s)) { // r > s
                    below.add(s);
                }
            }
            strictBelow.put(r, below);
        }

        // Count chains via DFS
        int[] count = {0};
        for (int r : qd.reps) {
            dfsChain(r, length - 1, strictBelow, count);
        }
        return count[0];
    }

    private static void dfsChain(int current, int remaining,
                                  Map<Integer, List<Integer>> strictBelow,
                                  int[] count) {
        if (remaining == 0) {
            count[0]++;
            return;
        }
        for (int s : strictBelow.get(current)) {
            dfsChain(s, remaining - 1, strictBelow, count);
        }
    }

    /**
     * Count strict chains of every length.
     *
     * @return mapping length -&gt; count (only positive counts)
     */
    public static Map<Integer, Integer> allChainCounts(StateSpace ss) {
        QuotientData qd = quotientData(ss);
        int height = computeHeightFromReps(qd.reps, qd.qReach);

        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int k = 0; k <= height + 1; k++) {
            int c = chainCount(ss, k);
            if (c > 0) result.put(k, c);
        }
        return result;
    }

    /**
     * Count strict chains of each length from top to bottom.
     *
     * <p>Philip Hall's theorem: mu(top, bottom) = sum_{k&ge;1} (-1)^k c_k
     * where c_k is the number of such chains.
     */
    public static Map<Integer, Integer> chainsTopToBottom(StateSpace ss) {
        QuotientData qd = quotientData(ss);
        int topRep = qd.sccMap.get(ss.top());
        int botRep = qd.sccMap.get(ss.bottom());

        if (topRep == botRep) return Map.of();

        // Build strict-below on quotient
        Map<Integer, List<Integer>> strictBelow = new HashMap<>();
        for (int r : qd.reps) {
            List<Integer> below = new ArrayList<>();
            for (int s : qd.reps) {
                if (r != s && qd.qReach.get(r).contains(s)) below.add(s);
            }
            strictBelow.put(r, below);
        }

        Map<Integer, Integer> counts = new HashMap<>();
        dfsTopBottom(topRep, botRep, 0, strictBelow, counts);
        return counts;
    }

    private static void dfsTopBottom(int current, int botRep, int depth,
                                      Map<Integer, List<Integer>> strictBelow,
                                      Map<Integer, Integer> counts) {
        if (current == botRep) {
            counts.merge(depth, 1, Integer::sum);
            return;
        }
        for (int s : strictBelow.get(current)) {
            dfsTopBottom(s, botRep, depth + 1, strictBelow, counts);
        }
    }

    private static int computeHeightFromReps(List<Integer> reps,
                                              Map<Integer, Set<Integer>> qReach) {
        if (reps.isEmpty()) return 0;
        Map<Integer, Integer> memo = new HashMap<>();
        int max = 0;
        for (int r : reps) max = Math.max(max, longestChain(r, reps, qReach, memo));
        return max;
    }

    private static int longestChain(int r, List<Integer> reps,
                                     Map<Integer, Set<Integer>> qReach,
                                     Map<Integer, Integer> memo) {
        if (memo.containsKey(r)) return memo.get(r);
        int best = 0;
        for (int s : reps) {
            if (s != r && qReach.get(r).contains(s)) {
                best = Math.max(best, 1 + longestChain(s, reps, qReach, memo));
            }
        }
        memo.put(r, best);
        return best;
    }

    // =======================================================================
    // Polynomial computation via Lagrange interpolation
    // =======================================================================

    private static int evalPoly(List<Double> coeffs, int x) {
        double val = 0.0;
        for (int i = 0; i < coeffs.size(); i++) {
            val += coeffs.get(i) * Math.pow(x, i);
        }
        return (int) Math.round(val);
    }

    /**
     * Lagrange interpolation: given (x, y) points, compute polynomial coefficients.
     *
     * @return coefficients [a_0, a_1, ..., a_d]
     */
    static List<Double> lagrangeInterpolation(List<int[]> points) {
        int n = points.size();
        if (n == 0) return List.of(0.0);

        double[] result = new double[n];

        for (int i = 0; i < n; i++) {
            int xi = points.get(i)[0];
            int yi = points.get(i)[1];

            // Compute basis polynomial L_i(x) = prod_{j!=i} (x - x_j) / (x_i - x_j)
            double[] basis = {1.0};
            for (int j = 0; j < n; j++) {
                if (j == i) continue;
                int xj = points.get(j)[0];
                double denom = xi - xj;
                double[] newBasis = new double[basis.length + 1];
                for (int k = 0; k < basis.length; k++) {
                    newBasis[k + 1] += basis[k] / denom;
                    newBasis[k] -= basis[k] * xj / denom;
                }
                basis = newBasis;
            }

            // Add yi * basis to result
            for (int k = 0; k < basis.length && k < n; k++) {
                result[k] += yi * basis[k];
            }
        }

        // Clean up near-zero coefficients
        for (int k = 0; k < result.length; k++) {
            if (Math.abs(result[k]) < 1e-9) {
                result[k] = 0.0;
            } else if (Math.abs(result[k] - Math.round(result[k])) < 1e-9) {
                result[k] = Math.round(result[k]);
            }
        }

        // Trim trailing zeros
        int len = result.length;
        while (len > 1 && Math.abs(result[len - 1]) < 1e-9) len--;

        List<Double> out = new ArrayList<>(len);
        for (int i = 0; i < len; i++) out.add(result[i]);
        return out;
    }

    /**
     * Compute the coefficients of the zeta polynomial Z(P, k).
     *
     * <p>The zeta polynomial has degree equal to the height of P.
     * We compute Z(P, k) for k = 0, 1, ..., height+1 and interpolate.
     */
    public static List<Double> zetaPolynomialCoefficients(StateSpace ss) {
        QuotientData qd = quotientData(ss);
        int height = computeHeightFromReps(qd.reps, qd.qReach);
        int numPoints = height + 2; // degree + 2 for safety

        List<int[]> points = new ArrayList<>();
        for (int k = 0; k < numPoints; k++) {
            int val = multichainCount(ss, k);
            points.add(new int[]{k, val});
        }
        return lagrangeInterpolation(points);
    }

    /**
     * Evaluate the zeta polynomial Z(P, k) at integer k.
     *
     * <p>For k &ge; 0, counts multichains directly.
     * For negative k, uses polynomial evaluation (Philip Hall's theorem).
     */
    public static int zetaPolynomial(StateSpace ss, int k) {
        if (k >= 0) return multichainCount(ss, k);
        List<Double> coeffs = zetaPolynomialCoefficients(ss);
        return evalPoly(coeffs, k);
    }

    // =======================================================================
    // Palindromicity
    // =======================================================================

    /**
     * Check if the zeta polynomial is palindromic.
     *
     * <p>Equivalent to: coefficients read the same forwards and backwards.
     * This hints at self-duality of the lattice.
     */
    public static boolean isPalindromic(StateSpace ss) {
        List<Double> coeffs = zetaPolynomialCoefficients(ss);
        int n = coeffs.size();
        for (int i = 0; i <= n / 2; i++) {
            if (Math.abs(coeffs.get(i) - coeffs.get(n - 1 - i)) > 1e-9) {
                return false;
            }
        }
        return true;
    }

    // =======================================================================
    // Whitney numbers
    // =======================================================================

    /**
     * Compute Whitney numbers of the second kind W_k(P).
     *
     * <p>W_k = number of elements of rank k.
     */
    public static List<Integer> whitneyNumbersSecondKind(StateSpace ss) {
        Map<Integer, Integer> rank = Zeta.computeRank(ss);
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        List<Integer> reps = new ArrayList<>(sccR.sccMembers().keySet());
        Collections.sort(reps);

        Map<Integer, Integer> repRank = new HashMap<>();
        for (int r : reps) repRank.put(r, rank.getOrDefault(r, 0));

        int maxRank = 0;
        for (int v : repRank.values()) maxRank = Math.max(maxRank, v);

        int[] w = new int[maxRank + 1];
        for (int r : reps) w[repRank.get(r)]++;

        List<Integer> result = new ArrayList<>(w.length);
        for (int v : w) result.add(v);
        return result;
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    /**
     * Complete zeta polynomial analysis of a session type state space.
     */
    public static ZetaPolynomialResult analyzeZetaPolynomial(StateSpace ss) {
        List<Double> coeffs = zetaPolynomialCoefficients(ss);

        // Compute effective degree
        int degree = coeffs.size() - 1;
        while (degree > 0 && Math.abs(coeffs.get(degree)) < 1e-9) degree--;

        // Compute values for several k
        Map<Integer, Integer> values = new LinkedHashMap<>();
        for (int k = 0; k < degree + 3; k++) {
            values.put(k, multichainCount(ss, k));
        }

        // Moebius value mu(top, bottom)
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
        int muTopBot = mu.getOrDefault(new Zeta.Pair(ss.top(), ss.bottom()), 0);

        // Chain counts
        Map<Integer, Integer> chains = allChainCounts(ss);

        // Philip Hall verification
        QuotientData qd = quotientData(ss);
        Map<Integer, Integer> tbChains = chainsTopToBottom(ss);
        int hallAltSum = 0;
        for (var entry : tbChains.entrySet()) {
            int k = entry.getKey();
            if (k >= 1) {
                hallAltSum += (k % 2 == 0 ? 1 : -1) * entry.getValue();
            }
        }
        boolean hallVerified;
        if (qd.reps.size() == 1) {
            hallVerified = (muTopBot == 1);
        } else {
            hallVerified = (hallAltSum == muTopBot);
        }

        // Multichain counts
        Map<Integer, Integer> mc = new LinkedHashMap<>(values);

        int z2 = values.getOrDefault(2, 0);
        boolean palindromic = isPalindromic(ss);

        return new ZetaPolynomialResult(
                qd.reps.size(),
                coeffs,
                degree,
                values,
                muTopBot,
                hallVerified,
                chains,
                mc,
                z2,
                palindromic);
    }
}
