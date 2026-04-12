package com.bica.reborn.laplacian_flow;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.algebraic.eigenvalues.EigenvalueComputer;
import com.bica.reborn.algebraic.matrix.MatrixBuilder;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Laplacian optimization for protocol connectivity (Step 31h).
 *
 * <p>Gradient flow on the Laplacian: optimize protocol structure for better
 * connectivity by suggesting edges (transitions) to add. The Fiedler value
 * (algebraic connectivity) is the objective; we compute its gradient with
 * respect to edge additions and propose improvements.
 *
 * <p>Java port of Python {@code reticulate.laplacian_flow}.
 *
 * <h3>Main API</h3>
 * <ul>
 *   <li>{@link #laplacianGradient(StateSpace)} &mdash; gradient of Fiedler w.r.t. edges</li>
 *   <li>{@link #optimizeConnectivity(StateSpace, int)} &mdash; suggest k best edges</li>
 *   <li>{@link #suggestImprovements(StateSpace, int, int)} &mdash; full analysis</li>
 *   <li>{@link #connectivityScore(StateSpace)} &mdash; normalized connectivity [0, 1]</li>
 *   <li>{@link #bottleneckEdges(StateSpace, int)} &mdash; critical edges</li>
 * </ul>
 */
public final class LaplacianFlowChecker {

    private LaplacianFlowChecker() {}

    // -----------------------------------------------------------------------
    // Result types
    // -----------------------------------------------------------------------

    /** A candidate edge to add for connectivity improvement. */
    public record EdgeCandidate(int src, int tgt, double fiedlerGain, double relativeGain) {}

    /** An edge whose removal most degrades connectivity. */
    public record BottleneckEdge(int src, int tgt, double fiedlerLoss, double relativeLoss, boolean isBridge) {}

    /** Complete Laplacian optimization analysis. */
    public record ImprovementResult(
            int numStates,
            int numEdges,
            double fiedlerCurrent,
            double connectivityScore,
            List<EdgeCandidate> candidates,
            List<BottleneckEdge> bottlenecks,
            double fiedlerAfterBest,
            double maxPossibleFiedler,
            double improvementPotential) {

        public ImprovementResult {
            candidates = List.copyOf(candidates);
            bottlenecks = List.copyOf(bottlenecks);
        }
    }

    // -----------------------------------------------------------------------
    // Hasse graph utilities
    // -----------------------------------------------------------------------

    /** Compute covering relation edges as pairs [src, tgt]. */
    static List<int[]> hasseEdges(StateSpace ss) {
        return AlgebraicChecker.hasseEdges(ss);
    }

    /** Get undirected edge set from Hasse edges (normalized: smaller first). */
    static Set<long[]> undirectedEdgeSet(StateSpace ss) {
        Set<Long> packed = new HashSet<>();
        List<int[]> result = new ArrayList<>();
        for (int[] e : hasseEdges(ss)) {
            int lo = Math.min(e[0], e[1]);
            int hi = Math.max(e[0], e[1]);
            long key = ((long) lo << 32) | (hi & 0xFFFFFFFFL);
            if (packed.add(key)) {
                result.add(new int[]{lo, hi});
            }
        }
        // Return as set of pairs -- we use the packed set for lookup
        Set<long[]> res = new LinkedHashSet<>();
        for (int[] r : result) {
            res.add(new long[]{r[0], r[1]});
        }
        return res;
    }

    /** Pack two ints into a long for edge set membership. */
    private static long packEdge(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    /** Build Laplacian from undirected edge list. */
    private static double[][] laplacianFromEdges(List<Integer> states, List<int[]> edges) {
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        double[][] L = new double[n][n];
        for (int[] e : edges) {
            int i = idx.get(e[0]);
            int j = idx.get(e[1]);
            L[i][i] += 1.0;
            L[j][j] += 1.0;
            L[i][j] -= 1.0;
            L[j][i] -= 1.0;
        }
        return L;
    }

    /** Compute Fiedler value from edge list. */
    private static double fiedlerFromEdges(List<Integer> states, List<int[]> edges) {
        double[][] L = laplacianFromEdges(states, edges);
        int n = L.length;
        if (n <= 1) return 0.0;
        double[] eigs = AlgebraicChecker.computeEigenvalues(L);
        Arrays.sort(eigs);
        if (eigs.length < 2) return 0.0;
        return Math.max(0.0, eigs[1]);
    }

    /**
     * Compute the Fiedler vector (eigenvector for lambda_2).
     *
     * <p>Uses power iteration on (lambda_max * I - L) to find the second-smallest
     * eigenvector, deflating the all-ones direction.
     */
    private static double[] fiedlerVector(List<Integer> states, List<int[]> edges) {
        int n = states.size();
        if (n <= 1) return new double[n];

        double[][] L = laplacianFromEdges(states, edges);
        double[] eigs = AlgebraicChecker.computeEigenvalues(L);
        Arrays.sort(eigs);
        double lambdaMax = eigs.length > 0 ? eigs[eigs.length - 1] : 1.0;

        // Shifted matrix: M = lambdaMax * I - L
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                M[i][j] = -L[i][j];
            }
            M[i][i] += lambdaMax;
        }

        // Start with vector orthogonal to all-ones
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            v[i] = (double) i - (n - 1) / 2.0;
        }
        double norm = vecNorm(v);
        if (norm < 1e-15) return new double[n];
        for (int i = 0; i < n; i++) v[i] /= norm;

        for (int iter = 0; iter < 200; iter++) {
            // Multiply by M
            double[] w = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    w[i] += M[i][j] * v[j];
                }
            }

            // Project out all-ones direction
            double avg = 0;
            for (double x : w) avg += x;
            avg /= n;
            for (int i = 0; i < n; i++) w[i] -= avg;

            // Normalize
            norm = vecNorm(w);
            if (norm < 1e-15) break;
            for (int i = 0; i < n; i++) v[i] = w[i] / norm;
        }

        return v;
    }

    private static double vecNorm(double[] v) {
        double sum = 0;
        for (double x : v) sum += x * x;
        return Math.sqrt(sum);
    }

    // -----------------------------------------------------------------------
    // Laplacian gradient
    // -----------------------------------------------------------------------

    /**
     * Compute the gradient of the Fiedler value with respect to edge additions.
     *
     * <p>For each non-existing edge (u, v), the gain in Fiedler value from
     * adding it is approximately (f_u - f_v)^2 where f is the Fiedler vector.
     *
     * @param ss a session type state space
     * @return map from packed edge (lo, hi) to gradient value
     */
    public static Map<long[], Double> laplacianGradient(StateSpace ss) {
        List<Integer> states = sortedStates(ss);
        int n = states.size();
        if (n <= 2) return Collections.emptyMap();

        List<int[]> edges = hasseEdges(ss);
        Set<Long> existing = new HashSet<>();
        for (int[] e : edges) {
            existing.add(packEdge(e[0], e[1]));
        }

        double[] f = fiedlerVector(states, edges);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        Map<long[], Double> gradient = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int s = states.get(i);
                int t = states.get(j);
                long key = packEdge(s, t);
                if (existing.contains(key)) continue;
                double diff = f[idx.get(s)] - f[idx.get(t)];
                double grad = diff * diff;
                gradient.put(new long[]{Math.min(s, t), Math.max(s, t)}, grad);
            }
        }

        return gradient;
    }

    // -----------------------------------------------------------------------
    // Optimize connectivity
    // -----------------------------------------------------------------------

    /**
     * Suggest the k best edges to add for connectivity improvement.
     *
     * @param ss a session type state space
     * @param k  number of candidates to return
     * @return list of EdgeCandidate sorted by fiedlerGain (descending)
     */
    public static List<EdgeCandidate> optimizeConnectivity(StateSpace ss, int k) {
        List<Integer> states = sortedStates(ss);
        int n = states.size();
        if (n <= 2) return Collections.emptyList();

        List<int[]> edges = hasseEdges(ss);
        double fiedlerCurrent = fiedlerFromEdges(states, edges);

        Map<long[], Double> grad = laplacianGradient(ss);
        if (grad.isEmpty()) return Collections.emptyList();

        // Sort by gradient (descending)
        List<Map.Entry<long[], Double>> sorted = new ArrayList<>(grad.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<EdgeCandidate> results = new ArrayList<>();
        int limit = Math.min(sorted.size(), k * 2);
        for (int i = 0; i < limit; i++) {
            long[] edge = sorted.get(i).getKey();
            int u = (int) edge[0];
            int v = (int) edge[1];
            // Verify actual gain
            List<int[]> newEdges = new ArrayList<>(edges);
            newEdges.add(new int[]{u, v});
            double fiedlerNew = fiedlerFromEdges(states, newEdges);
            double gain = fiedlerNew - fiedlerCurrent;
            double relGain = fiedlerCurrent > 1e-10 ? gain / fiedlerCurrent : gain;
            results.add(new EdgeCandidate(u, v, gain, relGain));
        }

        results.sort((a, b) -> Double.compare(b.fiedlerGain(), a.fiedlerGain()));
        return results.subList(0, Math.min(results.size(), k));
    }

    /** Overload with default k=3. */
    public static List<EdgeCandidate> optimizeConnectivity(StateSpace ss) {
        return optimizeConnectivity(ss, 3);
    }

    // -----------------------------------------------------------------------
    // Connectivity score
    // -----------------------------------------------------------------------

    /**
     * Compute a normalized connectivity score in [0, 1].
     *
     * <p>The score is the Fiedler value divided by n (max possible for K_n).
     *
     * @param ss a session type state space
     * @return normalized connectivity score
     */
    public static double connectivityScore(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 1) return 1.0;

        double fiedler = EigenvalueComputer.fiedlerValue(ss);
        double maxFiedler = (double) n;
        return Math.min(1.0, fiedler / maxFiedler);
    }

    // -----------------------------------------------------------------------
    // Bottleneck edges
    // -----------------------------------------------------------------------

    /**
     * Find the k edges whose removal most degrades connectivity.
     *
     * @param ss a session type state space
     * @param k  number of bottlenecks to return
     * @return list of BottleneckEdge sorted by fiedlerLoss (descending)
     */
    public static List<BottleneckEdge> bottleneckEdges(StateSpace ss, int k) {
        List<Integer> states = sortedStates(ss);
        int n = states.size();
        if (n <= 2) return Collections.emptyList();

        List<int[]> edges = hasseEdges(ss);
        double fiedlerCurrent = fiedlerFromEdges(states, edges);

        List<BottleneckEdge> results = new ArrayList<>();
        for (int idx = 0; idx < edges.size(); idx++) {
            int[] e = edges.get(idx);
            // Remove this edge
            List<int[]> remaining = new ArrayList<>();
            for (int i = 0; i < edges.size(); i++) {
                if (i != idx) remaining.add(edges.get(i));
            }
            double fiedlerWithout = fiedlerFromEdges(states, remaining);

            double loss = fiedlerCurrent - fiedlerWithout;
            double relLoss = fiedlerCurrent > 1e-10 ? loss / fiedlerCurrent : loss;
            boolean isBridge = fiedlerWithout < 1e-10;

            results.add(new BottleneckEdge(e[0], e[1], loss, relLoss, isBridge));
        }

        results.sort((a, b) -> Double.compare(b.fiedlerLoss(), a.fiedlerLoss()));
        return results.subList(0, Math.min(results.size(), k));
    }

    /** Overload with default k=3. */
    public static List<BottleneckEdge> bottleneckEdges(StateSpace ss) {
        return bottleneckEdges(ss, 3);
    }

    // -----------------------------------------------------------------------
    // Full improvement analysis
    // -----------------------------------------------------------------------

    /**
     * Complete Laplacian optimization analysis.
     *
     * @param ss              a session type state space
     * @param numCandidates   number of edge candidates to suggest
     * @param numBottlenecks  number of bottleneck edges to identify
     * @return ImprovementResult with all metrics
     */
    public static ImprovementResult suggestImprovements(
            StateSpace ss, int numCandidates, int numBottlenecks) {

        List<Integer> states = sortedStates(ss);
        int n = states.size();
        List<int[]> edges = hasseEdges(ss);
        int m = edges.size();

        double fiedlerCurrent = EigenvalueComputer.fiedlerValue(ss);
        double score = connectivityScore(ss);

        List<EdgeCandidate> candidates = optimizeConnectivity(ss, numCandidates);
        List<BottleneckEdge> bottlenecks = bottleneckEdges(ss, numBottlenecks);

        // Fiedler after adding best candidate
        double fiedlerAfter;
        if (!candidates.isEmpty()) {
            fiedlerAfter = fiedlerCurrent + candidates.get(0).fiedlerGain();
        } else {
            fiedlerAfter = fiedlerCurrent;
        }

        // Max possible Fiedler (complete graph K_n has Fiedler = n)
        double maxFiedler = n > 1 ? (double) n : 0.0;
        double potential = maxFiedler > 1e-10
                ? (maxFiedler - fiedlerCurrent) / maxFiedler
                : 0.0;

        return new ImprovementResult(
                n, m, fiedlerCurrent, score, candidates, bottlenecks,
                fiedlerAfter, maxFiedler, potential);
    }

    /** Overload with default counts. */
    public static ImprovementResult suggestImprovements(StateSpace ss) {
        return suggestImprovements(ss, 5, 5);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static List<Integer> sortedStates(StateSpace ss) {
        var list = new ArrayList<>(ss.states());
        Collections.sort(list);
        return list;
    }
}
