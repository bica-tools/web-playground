package com.bica.reborn.characteristic;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Rota characteristic polynomial for session type lattices (Step 30c).
 *
 * <p>The characteristic polynomial chi_P(t) of a finite poset P with rank function
 * rho is defined as:
 *
 * <pre>    chi_P(t) = Sum_{x in P} mu(top, x) * t^{h - corank(x)}</pre>
 *
 * <p>where h = max corank (height) and mu is the Moebius function.
 *
 * <p>This class provides:
 * <ul>
 *   <li>Characteristic polynomial with proper corank from BFS on quotient DAG</li>
 *   <li>Whitney numbers of the first and second kind</li>
 *   <li>Evaluation at specific points: chi(0), chi(1), chi(-1)</li>
 *   <li>Factorization under parallel: chi(L1 x L2) = chi(L1) * chi(L2)</li>
 *   <li>Log-concavity test on absolute Whitney numbers</li>
 *   <li>Polynomial arithmetic: multiply, evaluate, derivative</li>
 * </ul>
 *
 * <p>Faithful Java port of Python {@code reticulate.characteristic}.
 */
public final class CharacteristicChecker {

    private CharacteristicChecker() {}

    // =========================================================================
    // Result types
    // =========================================================================

    /** Complete characteristic polynomial analysis. */
    public record CharPolyResult(
            List<Integer> coefficients,
            int degree,
            int height,
            Map<Integer, Integer> whitneyFirst,
            Map<Integer, Integer> whitneySecond,
            int evalAt0,
            int evalAt1,
            int evalAtNeg1,
            boolean isLogConcave,
            int numStates) {}

    // =========================================================================
    // Polynomial arithmetic
    // =========================================================================

    /**
     * Evaluate polynomial with coefficients [a_n, ..., a_0] at t using Horner's method.
     */
    public static long polyEvaluate(List<Integer> coeffs, int t) {
        long result = 0;
        for (int c : coeffs) {
            result = result * t + c;
        }
        return result;
    }

    /**
     * Multiply two polynomials (coefficient lists, highest degree first).
     */
    public static List<Integer> polyMultiply(List<Integer> a, List<Integer> b) {
        if (a.isEmpty() || b.isEmpty()) return List.of(0);
        int na = a.size(), nb = b.size();
        int[] result = new int[na + nb - 1];
        for (int i = 0; i < na; i++) {
            for (int j = 0; j < nb; j++) {
                result[i + j] += a.get(i) * b.get(j);
            }
        }
        List<Integer> out = new ArrayList<>(result.length);
        for (int v : result) out.add(v);
        return out;
    }

    /**
     * Formal derivative of polynomial.
     */
    public static List<Integer> polyDerivative(List<Integer> coeffs) {
        int n = coeffs.size() - 1; // degree
        if (n <= 0) return List.of(0);
        List<Integer> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add((n - i) * coeffs.get(i));
        }
        return result;
    }

    /**
     * Pretty-print polynomial.
     */
    public static String polyToString(List<Integer> coeffs, String var) {
        int n = coeffs.size() - 1;
        List<String> terms = new ArrayList<>();
        for (int i = 0; i < coeffs.size(); i++) {
            int c = coeffs.get(i);
            int deg = n - i;
            if (c == 0) continue;
            if (deg == 0) {
                terms.add(String.valueOf(c));
            } else if (deg == 1) {
                if (c == 1) terms.add(var);
                else if (c == -1) terms.add("-" + var);
                else terms.add(c + var);
            } else {
                if (c == 1) terms.add(var + "^" + deg);
                else if (c == -1) terms.add("-" + var + "^" + deg);
                else terms.add(c + var + "^" + deg);
            }
        }
        if (terms.isEmpty()) return "0";
        StringBuilder sb = new StringBuilder(terms.get(0));
        for (int i = 1; i < terms.size(); i++) {
            String t = terms.get(i);
            if (t.startsWith("-")) {
                sb.append(" - ").append(t.substring(1));
            } else {
                sb.append(" + ").append(t);
            }
        }
        return sb.toString();
    }

    public static String polyToString(List<Integer> coeffs) {
        return polyToString(coeffs, "t");
    }

    // =========================================================================
    // Corank computation (BFS from top on quotient DAG)
    // =========================================================================

    /**
     * BFS distance from top on quotient DAG.
     * Returns map: state -> corank.
     */
    public static Map<Integer, Integer> computeCorank(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        // Build quotient adjacency
        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new HashSet<>());
        for (int s : ss.states()) {
            for (int t : adj.get(s)) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                if (sr != tr) qAdj.get(sr).add(tr);
            }
        }

        int topRep = sccMap.get(ss.top());
        Map<Integer, Integer> corank = new HashMap<>();
        for (int r : reps) corank.put(r, -1);
        corank.put(topRep, 0);

        // BFS
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(topRep);
        Set<Integer> visited = new HashSet<>();
        visited.add(topRep);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : qAdj.getOrDefault(u, Set.of())) {
                if (!visited.contains(v)) {
                    visited.add(v);
                    corank.put(v, corank.get(u) + 1);
                    queue.add(v);
                }
            }
        }
        for (int r : reps) {
            if (corank.get(r) < 0) corank.put(r, 0);
        }

        // Map back to states
        Map<Integer, Integer> result = new HashMap<>();
        for (int s : ss.states()) {
            result.put(s, corank.get(sccMap.get(s)));
        }
        return result;
    }

    // =========================================================================
    // Characteristic polynomial
    // =========================================================================

    /**
     * Compute the characteristic polynomial chi_P(t).
     *
     * <p>chi_P(t) = Sum_{x} mu(top, x) * t^{h - corank(x)}
     *
     * <p>Returns coefficients [a_h, a_{h-1}, ..., a_0] (highest degree first).
     */
    public static List<Integer> characteristicPolynomial(StateSpace ss) {
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
        int top = ss.top();
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();

        Map<Integer, Integer> corankMap = computeCorank(ss);
        int maxCorank = corankMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int height = maxCorank;

        int[] coeffs = new int[height + 1];

        Set<Integer> countedSccs = new HashSet<>();
        List<Integer> sortedStates = new ArrayList<>(ss.states());
        Collections.sort(sortedStates);
        for (int x : sortedStates) {
            int rep = sccMap.get(x);
            if (countedSccs.contains(rep)) continue;
            countedSccs.add(rep);

            int muVal = mu.getOrDefault(new Zeta.Pair(top, x), 0);
            int cr = corankMap.get(x);
            int degree = maxCorank - cr;
            if (degree >= 0 && degree <= maxCorank) {
                coeffs[maxCorank - degree] += muVal;
            }
        }

        // Remove leading zeros
        List<Integer> result = new ArrayList<>();
        boolean leading = true;
        for (int i = 0; i < coeffs.length; i++) {
            if (leading && coeffs[i] == 0 && i < coeffs.length - 1) continue;
            leading = false;
            result.add(coeffs[i]);
        }
        if (result.isEmpty()) result.add(0);
        return result;
    }

    // =========================================================================
    // Whitney numbers
    // =========================================================================

    /**
     * Whitney numbers of the first kind: w_k = Sum_{corank(x)=k} mu(top, x).
     */
    public static Map<Integer, Integer> whitneyNumbersFirst(StateSpace ss) {
        Map<Integer, Integer> corank = computeCorank(ss);
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
        int top = ss.top();
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();

        Map<Integer, Integer> w = new HashMap<>();
        Set<Integer> counted = new HashSet<>();
        List<Integer> sortedStates = new ArrayList<>(ss.states());
        Collections.sort(sortedStates);
        for (int x : sortedStates) {
            int rep = sccMap.get(x);
            if (counted.contains(rep)) continue;
            counted.add(rep);
            int cr = corank.get(x);
            int muVal = mu.getOrDefault(new Zeta.Pair(top, x), 0);
            w.merge(cr, muVal, Integer::sum);
        }
        return w;
    }

    /**
     * Whitney numbers of the second kind: W_k = |{x : corank(x) = k}|.
     */
    public static Map<Integer, Integer> whitneyNumbersSecond(StateSpace ss) {
        Map<Integer, Integer> corank = computeCorank(ss);
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();

        Map<Integer, Integer> W = new HashMap<>();
        Set<Integer> counted = new HashSet<>();
        List<Integer> sortedStates = new ArrayList<>(ss.states());
        Collections.sort(sortedStates);
        for (int x : sortedStates) {
            int rep = sccMap.get(x);
            if (counted.contains(rep)) continue;
            counted.add(rep);
            int cr = corank.get(x);
            W.merge(cr, 1, Integer::sum);
        }
        return W;
    }

    // =========================================================================
    // Log-concavity
    // =========================================================================

    /**
     * Check if |values[0]|, |values[1]|, ..., |values[n]| is log-concave.
     *
     * <p>A sequence a_0, a_1, ..., a_n is log-concave iff a_k^2 >= a_{k-1}*a_{k+1}
     * for all 1 <= k <= n-1.
     */
    public static boolean checkLogConcave(List<Integer> values) {
        List<Long> abs = new ArrayList<>();
        for (int v : values) abs.add((long) Math.abs(v));
        for (int k = 1; k < abs.size() - 1; k++) {
            if (abs.get(k) * abs.get(k) < abs.get(k - 1) * abs.get(k + 1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the absolute Whitney numbers of the first kind are log-concave.
     */
    public static boolean isWhitneyLogConcave(StateSpace ss) {
        Map<Integer, Integer> w = whitneyNumbersFirst(ss);
        if (w.isEmpty()) return true;
        int maxRank = w.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Integer> values = new ArrayList<>();
        for (int k = 0; k <= maxRank; k++) {
            values.add(w.getOrDefault(k, 0));
        }
        return checkLogConcave(values);
    }

    // =========================================================================
    // Factorization under parallel
    // =========================================================================

    /**
     * Verify chi(L1 x L2)(t) = chi(L1)(t) * chi(L2)(t).
     */
    public static boolean verifyFactorization(
            StateSpace ssLeft, StateSpace ssRight, StateSpace ssProduct) {
        List<Integer> chiLeft = characteristicPolynomial(ssLeft);
        List<Integer> chiRight = characteristicPolynomial(ssRight);
        List<Integer> chiProduct = new ArrayList<>(characteristicPolynomial(ssProduct));
        List<Integer> chiExpected = new ArrayList<>(polyMultiply(chiLeft, chiRight));

        // Remove leading zeros
        while (chiProduct.size() > 1 && chiProduct.get(0) == 0) chiProduct.remove(0);
        while (chiExpected.size() > 1 && chiExpected.get(0) == 0) chiExpected.remove(0);

        return chiProduct.equals(chiExpected);
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Complete characteristic polynomial analysis.
     */
    public static CharPolyResult analyzeCharacteristic(StateSpace ss) {
        List<Integer> coeffs = characteristicPolynomial(ss);
        int degree = coeffs.size() - 1;
        int height = degree;

        Map<Integer, Integer> wFirst = whitneyNumbersFirst(ss);
        Map<Integer, Integer> wSecond = whitneyNumbersSecond(ss);

        int eval0 = (int) polyEvaluate(coeffs, 0);
        int eval1 = (int) polyEvaluate(coeffs, 1);
        int evalNeg1 = (int) polyEvaluate(coeffs, -1);

        int maxCorank = wFirst.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Integer> wSeq = new ArrayList<>();
        for (int k = 0; k <= maxCorank; k++) {
            wSeq.add(wFirst.getOrDefault(k, 0));
        }
        boolean logConc = checkLogConcave(wSeq);

        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        int nQuotient = new HashSet<>(sccR.sccMap().values()).size();

        return new CharPolyResult(
                coeffs, degree, height,
                wFirst, wSecond,
                eval0, eval1, evalNeg1,
                logConc, nQuotient);
    }
}
