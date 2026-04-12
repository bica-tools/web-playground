package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Protocol-aware error correction via expander codes
 * (port of Python {@code reticulate.expander_codes}, Step 31e).
 *
 * <p>Uses spectral expansion properties of session-type lattice Hasse diagrams
 * to build LDPC-style error-correcting codes for protocol messages. Provides
 * spectral gap, vertex-expansion ratio, Cheeger bounds, parity-check matrix,
 * code distance, and error-correction capacity.
 *
 * <p>The Hasse adjacency matches Python's {@code _hasse_edges} algorithm
 * (raw transitions, no SCC quotient). Eigenvalues of the resulting Laplacian
 * are computed via {@link HeatKernel#jacobiEigendecomposition}. Results are
 * bit-for-bit consistent with the Python reference up to numerical tolerance.
 */
public final class ExpanderCodes {

    private ExpanderCodes() {}

    // =======================================================================
    // Result types
    // =======================================================================

    /** Cheeger inequality bounds. */
    public record CheegerBounds(
            double lower,
            double upper,
            double spectralGap,
            int maxDegree) {}

    /** Complete expander-code analysis of a session type lattice. */
    public record ExpanderCodeResult(
            int numStates,
            int numEdges,
            double spectralGap,
            double expansionRatio,
            boolean isExpander,
            CheegerBounds cheeger,
            int codeDistance,
            int correctableErrors,
            int parityCheckRows,
            int parityCheckCols,
            double rate) {}

    // =======================================================================
    // Hasse graph utilities
    // =======================================================================

    /** Compute covering relation edges of the Hasse diagram (matches Python). */
    static List<int[]> hasseEdges(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (StateSpace.Transition tr : ss.transitions()) {
            adj.get(tr.source()).add(tr.target());
        }
        List<int[]> edges = new ArrayList<>();
        for (int a : ss.states()) {
            Set<Integer> direct = adj.get(a);
            for (int b : direct) {
                boolean isCover = true;
                for (int c : direct) {
                    if (c == b) continue;
                    Set<Integer> visited = new HashSet<>();
                    Deque<Integer> stack = new ArrayDeque<>();
                    stack.push(c);
                    boolean found = false;
                    while (!stack.isEmpty()) {
                        int u = stack.pop();
                        if (u == b) { found = true; break; }
                        if (!visited.add(u)) continue;
                        for (int v : adj.get(u)) {
                            if (!visited.contains(v)) stack.push(v);
                        }
                    }
                    if (found) { isCover = false; break; }
                }
                if (isCover) edges.add(new int[]{a, b});
            }
        }
        return edges;
    }

    static Map<Integer, Set<Integer>> undirectedAdj(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (int[] e : hasseEdges(ss)) {
            adj.get(e[0]).add(e[1]);
            adj.get(e[1]).add(e[0]);
        }
        return adj;
    }

    static Map<Integer, Integer> degreeSequence(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = undirectedAdj(ss);
        Map<Integer, Integer> deg = new HashMap<>();
        for (int s : ss.states()) deg.put(s, adj.get(s).size());
        return deg;
    }

    private static List<Integer> sortedStates(StateSpace ss) {
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);
        return states;
    }

    // =======================================================================
    // Laplacian and Fiedler value (matches Python reticulate.matrix.fiedler_value)
    // =======================================================================

    static double[][] hasseLaplacian(StateSpace ss) {
        List<Integer> states = sortedStates(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        double[][] A = new double[n][n];
        for (int[] e : hasseEdges(ss)) {
            int i = idx.get(e[0]);
            int j = idx.get(e[1]);
            A[i][j] = 1.0;
            A[j][i] = 1.0;
        }
        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            double d = 0.0;
            for (int j = 0; j < n; j++) d += A[i][j];
            for (int j = 0; j < n; j++) {
                L[i][j] = (i == j) ? d : -A[i][j];
            }
        }
        return L;
    }

    // =======================================================================
    // Spectral gap (Fiedler value of Hasse Laplacian)
    // =======================================================================

    /**
     * Spectral gap (Fiedler value / algebraic connectivity) of the
     * undirected Hasse diagram. Second-smallest Laplacian eigenvalue.
     */
    public static double spectralGap(StateSpace ss) {
        double[][] L = hasseLaplacian(ss);
        int n = L.length;
        if (n <= 1) return 0.0;
        HeatKernel.Eigendecomp ed = HeatKernel.jacobiEigendecomposition(L, 200);
        double[] eigs = ed.eigenvalues();
        if (eigs.length < 2) return 0.0;
        double[] sorted = eigs.clone();
        Arrays.sort(sorted);
        return Math.max(0.0, sorted[1]);
    }

    // =======================================================================
    // Vertex expansion ratio
    // =======================================================================

    /**
     * Vertex expansion ratio {@code h(G) = min_{|S|<=n/2} |boundary(S)|/|S|}.
     * Exact for n &le; 20; Cheeger lower bound otherwise.
     */
    public static double expansionRatio(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 1) return 0.0;
        List<Integer> states = sortedStates(ss);
        Map<Integer, Set<Integer>> adj = undirectedAdj(ss);

        if (n > 20) {
            return spectralGap(ss) / 2.0;
        }

        double minRatio = Double.POSITIVE_INFINITY;
        long total = 1L << n;
        for (long mask = 1; mask < total; mask++) {
            int size = Long.bitCount(mask);
            if (size > n / 2) continue;
            Set<Integer> subset = new HashSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1L << i)) != 0) subset.add(states.get(i));
            }
            Set<Integer> boundary = new HashSet<>();
            for (int u : subset) {
                for (int v : adj.get(u)) {
                    if (!subset.contains(v)) boundary.add(v);
                }
            }
            double ratio = (double) boundary.size() / size;
            if (ratio < minRatio) minRatio = ratio;
        }
        return Double.isInfinite(minRatio) ? 0.0 : minRatio;
    }

    // =======================================================================
    // Expander test
    // =======================================================================

    public static boolean isExpander(StateSpace ss, double threshold) {
        return spectralGap(ss) >= threshold;
    }

    public static boolean isExpander(StateSpace ss) {
        return isExpander(ss, 0.5);
    }

    // =======================================================================
    // Cheeger bounds
    // =======================================================================

    public static CheegerBounds cheegerBounds(StateSpace ss) {
        double gap = spectralGap(ss);
        Map<Integer, Integer> degrees = degreeSequence(ss);
        int dMax = 0;
        for (int d : degrees.values()) if (d > dMax) dMax = d;
        double lower = gap / 2.0;
        double upper = (gap > 0 && dMax > 0) ? Math.sqrt(2.0 * dMax * gap) : 0.0;
        return new CheegerBounds(lower, upper, gap, dMax);
    }

    // =======================================================================
    // Parity check matrix (vertex-edge incidence of Hasse diagram)
    // =======================================================================

    public static int[][] parityCheckMatrix(StateSpace ss) {
        List<Integer> states = sortedStates(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);
        List<int[]> edges = hasseEdges(ss);
        int n = states.size();
        int m = edges.size();
        int[][] H = new int[n][m];
        for (int j = 0; j < m; j++) {
            int[] e = edges.get(j);
            H[idx.get(e[0])][j] = 1;
            H[idx.get(e[1])][j] = 1;
        }
        return H;
    }

    // =======================================================================
    // Code distance
    // =======================================================================

    public static int codeDistance(StateSpace ss) {
        int[][] H = parityCheckMatrix(ss);
        if (H.length == 0) return 0;
        int nRows = H.length;
        int nCols = H[0].length;
        if (nCols == 0) return 0;

        if (nCols <= 16) {
            int minWeight = nCols + 1;
            long total = 1L << nCols;
            for (long mask = 1; mask < total; mask++) {
                int[] c = new int[nCols];
                for (int j = 0; j < nCols; j++) {
                    if ((mask & (1L << j)) != 0) c[j] = 1;
                }
                boolean valid = true;
                for (int i = 0; i < nRows; i++) {
                    int s = 0;
                    for (int j = 0; j < nCols; j++) s += H[i][j] * c[j];
                    if ((s & 1) != 0) { valid = false; break; }
                }
                if (valid) {
                    int w = 0;
                    for (int x : c) if (x != 0) w++;
                    if (w < minWeight) minWeight = w;
                }
            }
            return (minWeight <= nCols) ? minWeight : 0;
        } else {
            double gap = spectralGap(ss);
            Map<Integer, Integer> degrees = degreeSequence(ss);
            int dMax = 1;
            for (int d : degrees.values()) if (d > dMax) dMax = d;
            if (dMax > 0 && gap > 0) {
                int bound = (int) (gap / dMax * nCols);
                return Math.max(1, bound);
            }
            return 1;
        }
    }

    // =======================================================================
    // Error correction capacity
    // =======================================================================

    public static int errorCorrectionCapacity(StateSpace ss) {
        int d = codeDistance(ss);
        return Math.max(0, (d - 1) / 2);
    }

    // =======================================================================
    // High-level analysis
    // =======================================================================

    public static ExpanderCodeResult analyzeExpanderCode(StateSpace ss) {
        return analyzeExpanderCode(ss, 0.5);
    }

    public static ExpanderCodeResult analyzeExpanderCode(StateSpace ss, double threshold) {
        double gap = spectralGap(ss);
        double expRatio = expansionRatio(ss);
        boolean expander = gap >= threshold;
        CheegerBounds cheeger = cheegerBounds(ss);
        int dist = codeDistance(ss);
        int correctable = Math.max(0, (dist - 1) / 2);
        int[][] H = parityCheckMatrix(ss);
        int nRows = H.length;
        int nCols = (H.length > 0) ? H[0].length : 0;
        double rate = (nCols > 0) ? Math.max(0.0, 1.0 - (double) nRows / nCols) : 0.0;
        return new ExpanderCodeResult(
                ss.states().size(),
                nCols,
                gap,
                expRatio,
                expander,
                cheeger,
                dist,
                correctable,
                nRows,
                nCols,
                rate);
    }
}
