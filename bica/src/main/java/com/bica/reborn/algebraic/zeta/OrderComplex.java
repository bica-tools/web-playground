package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Order complex and topological invariants of session type lattices
 * (port of Python {@code reticulate.order_complex}, Steps 30u-30x).
 *
 * <p>The order complex {@code Δ(P)} of a finite poset {@code P} is the abstract
 * simplicial complex whose {@code k}-simplices are chains of length {@code k+1}
 * in {@code P} (here, taken in the open interval between {@code top} and
 * {@code bottom}, SCC-quotiented).
 *
 * <p>Provides:
 * <ul>
 *   <li><b>Step 30u</b>: order complex construction, f-vector, dimension</li>
 *   <li><b>Step 30v</b>: Euler characteristic and reduced Euler characteristic
 *       (equal to the Möbius number {@code μ(top, bottom)} by Hall's theorem)</li>
 *   <li><b>Step 30w</b>: shellability (graded sufficient condition)</li>
 *   <li><b>Step 30x</b>: discrete Morse theory approximation</li>
 * </ul>
 *
 * <p>This is a faithful port of the Python module; all methods are static and
 * delegate to {@link Zeta} for reachability, SCC decomposition, and the
 * covering relation.
 */
public final class OrderComplex {

    private OrderComplex() {}

    // =======================================================================
    // Result type
    // =======================================================================

    /** Complete order complex analysis result. */
    public record OrderComplexResult(
            int numStates,
            List<Integer> fVector,
            int dimension,
            int eulerCharacteristic,
            int reducedEuler,
            int numMaximalChains,
            boolean isShellable,
            int morseNumber,
            List<Integer> hVector) {}

    // =======================================================================
    // Step 30u: chain enumeration, f-vector, dimension
    // =======================================================================

    /**
     * Enumerate all chains in the open interval {@code (top, bottom)}.
     * The empty chain is always included (representing {@code f_{-1} = 1}).
     */
    public static List<List<Integer>> enumerateChains(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        int top = ss.top();
        int bottom = ss.bottom();

        // Interior = strictly between top and bottom, by SCC
        List<Integer> sorted = new ArrayList<>(ss.states());
        Collections.sort(sorted);
        List<Integer> interior = new ArrayList<>();
        for (int s : sorted) {
            if (reach.get(top).contains(s)
                    && reach.get(s).contains(bottom)
                    && sccMap.get(s).intValue() != sccMap.get(top).intValue()
                    && sccMap.get(s).intValue() != sccMap.get(bottom).intValue()) {
                interior.add(s);
            }
        }

        // Deduplicate by SCC
        Set<Integer> seen = new HashSet<>();
        List<Integer> unique = new ArrayList<>();
        for (int s : interior) {
            int rep = sccMap.get(s);
            if (seen.add(rep)) unique.add(s);
        }

        List<List<Integer>> chains = new ArrayList<>();
        chains.add(new ArrayList<>()); // empty chain

        int n = unique.size();
        for (int mask = 1; mask < (1 << n); mask++) {
            List<Integer> subset = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) subset.add(unique.get(i));
            }

            boolean isChain = true;
            outer:
            for (int i = 0; i < subset.size(); i++) {
                for (int j = i + 1; j < subset.size(); j++) {
                    int a = subset.get(i);
                    int b = subset.get(j);
                    if (!reach.get(b).contains(a) && !reach.get(a).contains(b)) {
                        isChain = false;
                        break outer;
                    }
                    if (sccMap.get(a).intValue() == sccMap.get(b).intValue() && a != b) {
                        isChain = false;
                        break outer;
                    }
                }
            }

            if (isChain) {
                List<Integer> sortedChain = new ArrayList<>(subset);
                sortedChain.sort((x, y) -> Integer.compare(reach.get(y).size(), reach.get(x).size()));
                chains.add(sortedChain);
            }
        }
        return chains;
    }

    /**
     * f-vector of the order complex.
     *
     * <p>{@code f_k} = number of {@code k}-simplices = chains of length
     * {@code k+1} in the open interval. The returned list is indexed so that
     * {@code fv[0] = f_{-1} = 1} (the empty face), {@code fv[1] = f_0}
     * (vertices), etc.
     */
    public static List<Integer> fVector(StateSpace ss) {
        List<List<Integer>> chains = enumerateChains(ss);
        int maxLen = 0;
        for (List<Integer> c : chains) {
            if (c.size() > maxLen) maxLen = c.size();
        }
        List<Integer> fv = new ArrayList<>(Collections.nCopies(maxLen + 1, 0));
        for (List<Integer> c : chains) {
            int k = c.size();
            fv.set(k, fv.get(k) + 1);
        }
        return fv;
    }

    /** Dimension of the order complex (max chain length minus one, or -1 if empty). */
    public static int dimension(StateSpace ss) {
        List<Integer> fv = fVector(ss);
        for (int k = fv.size() - 1; k >= 0; k--) {
            if (fv.get(k) > 0) return k - 1;
        }
        return -1;
    }

    // =======================================================================
    // Step 30v: Euler characteristic
    // =======================================================================

    /** Euler characteristic: {@code χ = Σ_{k≥0} (-1)^k f_k}. */
    public static int eulerCharacteristic(StateSpace ss) {
        List<Integer> fv = fVector(ss);
        int chi = 0;
        for (int k = 1; k < fv.size(); k++) {
            int sign = ((k - 1) % 2 == 0) ? 1 : -1;
            chi += sign * fv.get(k);
        }
        return chi;
    }

    /**
     * Reduced Euler characteristic: {@code χ̃ = -f_{-1} + f_0 - f_1 + ...}.
     * By Hall's theorem this equals {@code μ(top, bottom)}.
     */
    public static int reducedEulerCharacteristic(StateSpace ss) {
        List<Integer> fv = fVector(ss);
        int chiTilde = 0;
        for (int k = 0; k < fv.size(); k++) {
            // (-1)^{k-1}
            int sign = (Math.floorMod(k - 1, 2) == 0) ? 1 : -1;
            chiTilde += sign * fv.get(k);
        }
        return chiTilde;
    }

    // =======================================================================
    // Step 30w: maximal chains, shellability
    // =======================================================================

    /** Enumerate maximal chains from {@code top} to {@code bottom} via the Hasse diagram. */
    static List<List<Integer>> maximalChains(StateSpace ss) {
        List<Zeta.Pair> covers = Zeta.coveringRelation(ss);
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (Zeta.Pair c : covers) adj.get(c.x()).add(c.y());

        List<List<Integer>> chains = new ArrayList<>();
        Deque<Integer> path = new ArrayDeque<>();
        path.push(ss.top());
        dfsMaximal(ss.top(), ss.bottom(), adj, path, chains);
        return chains;
    }

    private static void dfsMaximal(
            int current, int bottom,
            Map<Integer, List<Integer>> adj,
            Deque<Integer> path,
            List<List<Integer>> chains) {
        if (current == bottom) {
            List<Integer> snapshot = new ArrayList<>(path);
            Collections.reverse(snapshot);
            chains.add(snapshot);
            return;
        }
        for (int nxt : adj.getOrDefault(current, List.of())) {
            path.push(nxt);
            dfsMaximal(nxt, bottom, adj, path, chains);
            path.pop();
        }
    }

    /** Number of maximal chains from {@code top} to {@code bottom}. */
    public static int numMaximalChains(StateSpace ss) {
        return maximalChains(ss).size();
    }

    /**
     * Check if the order complex is shellable (sufficient condition:
     * all maximal chains have the same length, i.e. the poset is graded).
     */
    public static boolean isShellable(StateSpace ss) {
        List<List<Integer>> chains = maximalChains(ss);
        if (chains.size() <= 1) return true;
        Set<Integer> lengths = new HashSet<>();
        for (List<Integer> c : chains) lengths.add(c.size());
        return lengths.size() == 1;
    }

    // =======================================================================
    // Step 30x: discrete Morse theory approximation
    // =======================================================================

    /**
     * Morse number: minimum number of critical cells (approximation via the
     * reduced Euler characteristic).
     */
    public static int morseNumber(StateSpace ss) {
        int chiTilde = reducedEulerCharacteristic(ss);
        List<Integer> fv = fVector(ss);
        int dim = dimension(ss);
        if (dim < 0) return 0;
        if (dim == 0) return fv.get(1);
        if (chiTilde == 0) return 1;
        return Math.abs(chiTilde) + 1;
    }

    // =======================================================================
    // h-vector
    // =======================================================================

    /**
     * h-vector derived from the f-vector via the Dehn-Sommerville relations:
     * {@code h_k = Σ_{j=0}^{k} (-1)^{k-j} C(d-j, k-j) f_{j-1}}.
     */
    public static List<Integer> hVector(StateSpace ss) {
        List<Integer> fv = fVector(ss);
        int d = fv.size() - 1;
        if (d <= 0) return List.of(1);

        List<Integer> hv = new ArrayList<>(d + 1);
        for (int k = 0; k <= d; k++) {
            int hk = 0;
            for (int j = 0; j <= k; j++) {
                if (j < fv.size()) {
                    int sign = ((k - j) % 2 == 0) ? 1 : -1;
                    hk += sign * binomial(d - j, k - j) * fv.get(j);
                }
            }
            hv.add(hk);
        }
        return hv;
    }

    private static int binomial(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        int kk = Math.min(k, n - k);
        long result = 1;
        for (int i = 0; i < kk; i++) {
            result = result * (n - i) / (i + 1);
        }
        return (int) result;
    }

    // =======================================================================
    // Main analyser
    // =======================================================================

    /** Complete order complex analysis. */
    public static OrderComplexResult analyzeOrderComplex(StateSpace ss) {
        List<Integer> fv = fVector(ss);
        int dim = dimension(ss);
        int chi = eulerCharacteristic(ss);
        int chiTilde = reducedEulerCharacteristic(ss);
        int nMax = numMaximalChains(ss);
        boolean shell = isShellable(ss);
        int morse = morseNumber(ss);
        List<Integer> hv = hVector(ss);

        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Set<Integer> reps = new HashSet<>(sccR.sccMap().values());
        int nQuotient = reps.size();

        return new OrderComplexResult(
                nQuotient, fv, dim, chi, chiTilde, nMax, shell, morse, hv);
    }
}
