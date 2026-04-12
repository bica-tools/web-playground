package com.bica.reborn.fiedler;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.algebraic.eigenvalues.EigenvalueComputer;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Algebraic connectivity (Fiedler) analysis for session type lattices.
 *
 * <p>The Fiedler value lambda_2 (second-smallest Laplacian eigenvalue) measures
 * how well-connected a protocol's Hasse diagram is. This module provides:
 * <ul>
 *   <li><b>Fiedler value</b>: lambda_2(L) -- delegates to {@link EigenvalueComputer}</li>
 *   <li><b>Fiedler vector</b>: eigenvector for lambda_2 -- optimal graph bisection</li>
 *   <li><b>Cut vertices</b>: articulation points whose removal disconnects the graph</li>
 *   <li><b>Vertex connectivity</b>: minimum vertex cut size</li>
 *   <li><b>Edge connectivity</b>: minimum edge cut size</li>
 *   <li><b>Betweenness centrality</b>: bottleneck detection</li>
 *   <li><b>Cheeger inequality</b>: relates Fiedler value to conductance</li>
 *   <li><b>Composition</b>: lambda_2(L1 x L2) = min(lambda_2(L1), lambda_2(L2))</li>
 * </ul>
 *
 * <p>Port of Python {@code reticulate.fiedler} (Step 30e).
 */
public final class FiedlerChecker {

    private FiedlerChecker() {}

    // -----------------------------------------------------------------------
    // Result record
    // -----------------------------------------------------------------------

    /**
     * Complete algebraic connectivity analysis result.
     *
     * @param numStates          number of states
     * @param fiedlerValue       lambda_2(L) -- algebraic connectivity
     * @param isConnected        true iff lambda_2 > 0
     * @param cutVertices        states whose removal disconnects the Hasse diagram
     * @param vertexConnectivity minimum vertices to remove to disconnect
     * @param edgeConnectivity   minimum edges to remove to disconnect
     * @param numEdges           number of Hasse diagram edges
     * @param bottleneckState    state with highest betweenness (-1 if none)
     * @param cheegerLower       lower bound: h(G) >= lambda_2 / 2
     * @param cheegerUpper       upper bound: h(G) <= sqrt(2 * lambda_2)
     */
    public record FiedlerResult(
            int numStates,
            double fiedlerValue,
            boolean isConnected,
            List<Integer> cutVertices,
            int vertexConnectivity,
            int edgeConnectivity,
            int numEdges,
            int bottleneckState,
            double cheegerLower,
            double cheegerUpper) {

        public FiedlerResult {
            cutVertices = List.copyOf(cutVertices);
        }
    }

    // -----------------------------------------------------------------------
    // Fiedler value (delegates to EigenvalueComputer)
    // -----------------------------------------------------------------------

    /**
     * Compute the Fiedler value lambda_2(L).
     * Delegates to {@link EigenvalueComputer#fiedlerValue(StateSpace)}.
     */
    public static double fiedlerValue(StateSpace ss) {
        return EigenvalueComputer.fiedlerValue(ss);
    }

    // -----------------------------------------------------------------------
    // Fiedler vector via Jacobi eigenvector computation
    // -----------------------------------------------------------------------

    /**
     * Compute the Fiedler vector (eigenvector for lambda_2).
     *
     * <p>The signs of the Fiedler vector components give an optimal 2-partition
     * of the graph (spectral bisection).
     *
     * @param ss the state space
     * @return Fiedler vector (length = number of states), normalized
     */
    public static double[] fiedlerVector(StateSpace ss) {
        double[][] L = buildLaplacian(ss);
        int n = L.length;
        if (n <= 1) {
            return new double[n];
        }

        // Get eigenvalues to identify lambda_2
        double[] eigs = EigenvalueComputer.laplacianEigenvalues(ss);
        if (eigs.length < 2) {
            return new double[n];
        }
        double lambda2 = Math.max(0.0, eigs[1]);

        // Jacobi eigenvalue/eigenvector algorithm
        double[][] S = new double[n][n];
        double[][] V = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(L[i], 0, S[i], 0, n);
            V[i][i] = 1.0;
        }

        int maxIter = 100 * n * n;
        for (int iter = 0; iter < maxIter; iter++) {
            // Find largest off-diagonal element
            int p = 0, q = 1;
            double maxVal = Math.abs(S[0][1]);
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (Math.abs(S[i][j]) > maxVal) {
                        maxVal = Math.abs(S[i][j]);
                        p = i;
                        q = j;
                    }
                }
            }
            if (maxVal < 1e-12) break;

            double theta;
            if (Math.abs(S[p][p] - S[q][q]) < 1e-15) {
                theta = Math.PI / 4;
            } else {
                theta = 0.5 * Math.atan2(2 * S[p][q], S[p][p] - S[q][q]);
            }

            double c = Math.cos(theta);
            double s = Math.sin(theta);

            // Apply rotation to S
            double[][] newS = new double[n][n];
            for (int i = 0; i < n; i++) {
                System.arraycopy(S[i], 0, newS[i], 0, n);
            }
            for (int i = 0; i < n; i++) {
                if (i != p && i != q) {
                    newS[i][p] = c * S[i][p] + s * S[i][q];
                    newS[p][i] = newS[i][p];
                    newS[i][q] = -s * S[i][p] + c * S[i][q];
                    newS[q][i] = newS[i][q];
                }
            }
            newS[p][p] = c * c * S[p][p] + 2 * s * c * S[p][q] + s * s * S[q][q];
            newS[q][q] = s * s * S[p][p] - 2 * s * c * S[p][q] + c * c * S[q][q];
            newS[p][q] = 0.0;
            newS[q][p] = 0.0;

            // Update eigenvector matrix V
            double[][] newV = new double[n][n];
            for (int i = 0; i < n; i++) {
                System.arraycopy(V[i], 0, newV[i], 0, n);
            }
            for (int i = 0; i < n; i++) {
                newV[i][p] = c * V[i][p] + s * V[i][q];
                newV[i][q] = -s * V[i][p] + c * V[i][q];
            }

            S = newS;
            V = newV;
        }

        // Find column of V closest to lambda_2
        int bestIdx = 0;
        double bestDist = Math.abs(S[0][0] - lambda2);
        for (int i = 1; i < n; i++) {
            double dist = Math.abs(S[i][i] - lambda2);
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }

        double[] vec = new double[n];
        double norm = 0.0;
        for (int i = 0; i < n; i++) {
            vec[i] = V[i][bestIdx];
            norm += vec[i] * vec[i];
        }
        norm = Math.sqrt(norm);
        if (norm > 1e-15) {
            for (int i = 0; i < n; i++) {
                vec[i] /= norm;
            }
        }
        return vec;
    }

    // -----------------------------------------------------------------------
    // Cut vertices (articulation points)
    // -----------------------------------------------------------------------

    /**
     * Find cut vertices (articulation points) of the Hasse diagram.
     *
     * <p>A cut vertex is a state whose removal disconnects the graph.
     * These represent protocol bottlenecks.
     *
     * @param ss the state space
     * @return sorted list of cut vertex IDs
     */
    public static List<Integer> findCutVertices(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = buildHasseAdj(ss);
        List<Integer> cuts = new ArrayList<>();
        List<Integer> sorted = new ArrayList<>(ss.states());
        Collections.sort(sorted);
        for (int s : sorted) {
            if (!isConnectedWithout(adj, s)) {
                cuts.add(s);
            }
        }
        return cuts;
    }

    // -----------------------------------------------------------------------
    // Vertex connectivity
    // -----------------------------------------------------------------------

    /**
     * Minimum number of vertices to remove to disconnect the graph.
     *
     * @param ss the state space
     * @return vertex connectivity (0 for single-state or disconnected graphs)
     */
    public static int vertexConnectivity(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 1) return 0;

        Map<Integer, Set<Integer>> adj = buildHasseAdj(ss);

        // Check if already disconnected
        if (!isConnectedWithout(adj, -1)) {
            return 0;
        }

        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);

        // Size 1: cut vertices
        for (int s : states) {
            if (!isConnectedWithout(adj, s)) {
                return 1;
            }
        }

        // For small graphs, check pairs
        if (n <= 20) {
            for (int i = 0; i < states.size(); i++) {
                for (int j = i + 1; j < states.size(); j++) {
                    int si = states.get(i);
                    int sj = states.get(j);
                    Map<Integer, Set<Integer>> remaining = new HashMap<>();
                    for (int s : states) {
                        if (s == si || s == sj) continue;
                        Set<Integer> nbrs = new HashSet<>(adj.get(s));
                        nbrs.remove(si);
                        nbrs.remove(sj);
                        remaining.put(s, nbrs);
                    }
                    List<Integer> nodes = new ArrayList<>(remaining.keySet());
                    if (nodes.isEmpty()) continue;
                    Set<Integer> vis = new HashSet<>();
                    Deque<Integer> stack = new ArrayDeque<>();
                    stack.push(nodes.get(0));
                    while (!stack.isEmpty()) {
                        int u = stack.pop();
                        if (vis.add(u)) {
                            for (int v : remaining.get(u)) {
                                if (!vis.contains(v)) {
                                    stack.push(v);
                                }
                            }
                        }
                    }
                    if (vis.size() < nodes.size()) {
                        return 2;
                    }
                }
            }
        }

        return Math.min(n - 1, 3);
    }

    // -----------------------------------------------------------------------
    // Edge connectivity
    // -----------------------------------------------------------------------

    /**
     * Minimum number of edges to remove to disconnect the graph.
     *
     * <p>By Whitney's theorem: vertex_conn <= edge_conn <= min_degree.
     *
     * @param ss the state space
     * @return edge connectivity
     */
    public static int edgeConnectivity(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        if (n <= 1) return 0;

        // Min degree gives upper bound; for simple cases edge_conn = min_degree
        int minDeg = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            int deg = 0;
            for (int j = 0; j < n; j++) {
                deg += (int) A[i][j];
            }
            minDeg = Math.min(minDeg, deg);
        }
        return minDeg;
    }

    // -----------------------------------------------------------------------
    // Betweenness centrality
    // -----------------------------------------------------------------------

    /**
     * Approximate betweenness centrality of each state in the Hasse diagram.
     *
     * <p>States with high betweenness are protocol bottlenecks -- many
     * shortest paths pass through them.
     *
     * @param ss the state space
     * @return map from state ID to betweenness centrality
     */
    public static Map<Integer, Double> betweennessCentrality(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = buildHasseAdj(ss);
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);
        int n = states.size();

        Map<Integer, Double> centrality = new LinkedHashMap<>();
        for (int s : states) {
            centrality.put(s, 0.0);
        }

        for (int source : states) {
            // BFS from source
            Map<Integer, Integer> dist = new HashMap<>();
            Map<Integer, Integer> numPaths = new HashMap<>();
            dist.put(source, 0);
            numPaths.put(source, 1);
            List<Integer> order = new ArrayList<>();

            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                order.add(u);
                for (int v : adj.getOrDefault(u, Set.of())) {
                    if (!dist.containsKey(v)) {
                        dist.put(v, dist.get(u) + 1);
                        numPaths.put(v, 0);
                        queue.add(v);
                    }
                    if (dist.get(v) == dist.get(u) + 1) {
                        numPaths.put(v, numPaths.get(v) + numPaths.get(u));
                    }
                }
            }

            // Back-propagate dependencies
            Map<Integer, Double> dep = new HashMap<>();
            for (int s : states) {
                dep.put(s, 0.0);
            }
            for (int i = order.size() - 1; i >= 0; i--) {
                int v = order.get(i);
                if (v == source) continue;
                for (int u : adj.getOrDefault(v, Set.of())) {
                    if (dist.containsKey(u) && dist.get(u) == dist.get(v) - 1) {
                        int np = numPaths.getOrDefault(v, 0);
                        double frac = (np > 0)
                                ? (double) numPaths.getOrDefault(u, 0) / np
                                : 0.0;
                        dep.put(u, dep.get(u) + frac * (1 + dep.get(v)));
                    }
                }
                centrality.put(v, centrality.get(v) + dep.get(v));
            }
        }

        // Normalize
        if (n > 2) {
            double norm = 1.0 / ((n - 1) * (n - 2));
            for (int s : states) {
                centrality.put(s, centrality.get(s) * norm);
            }
        }

        return centrality;
    }

    /**
     * Find the state with highest betweenness centrality.
     *
     * @param ss the state space
     * @return bottleneck state ID, or -1 if no states
     */
    public static int findBottleneck(StateSpace ss) {
        Map<Integer, Double> bc = betweennessCentrality(ss);
        if (bc.isEmpty()) return -1;
        return bc.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);
    }

    // -----------------------------------------------------------------------
    // Cheeger inequality
    // -----------------------------------------------------------------------

    /**
     * Compute Cheeger inequality bounds on the conductance h(G).
     *
     * <p>Cheeger's inequality: lambda_2/2 <= h(G) <= sqrt(2 * lambda_2).
     *
     * @param ss the state space
     * @return double[2] with {lower, upper}
     */
    public static double[] cheegerBounds(StateSpace ss) {
        double fv = fiedlerValue(ss);
        double lower = fv / 2.0;
        double upper = (fv >= 0) ? Math.sqrt(2.0 * fv) : 0.0;
        return new double[]{lower, upper};
    }

    // -----------------------------------------------------------------------
    // Composition verification
    // -----------------------------------------------------------------------

    /**
     * Verify lambda_2(L1 x L2) = min(lambda_2(L1), lambda_2(L2)).
     *
     * @param ssLeft    left state space
     * @param ssRight   right state space
     * @param ssProduct product state space
     * @param tol       tolerance for comparison
     * @return true if the composition property holds
     */
    public static boolean verifyFiedlerMinComposition(
            StateSpace ssLeft, StateSpace ssRight, StateSpace ssProduct, double tol) {
        double fLeft = fiedlerValue(ssLeft);
        double fRight = fiedlerValue(ssRight);
        double fProduct = fiedlerValue(ssProduct);
        double expected = Math.min(fLeft, fRight);
        return Math.abs(fProduct - expected) < tol;
    }

    /** Overload with default tolerance of 0.1. */
    public static boolean verifyFiedlerMinComposition(
            StateSpace ssLeft, StateSpace ssRight, StateSpace ssProduct) {
        return verifyFiedlerMinComposition(ssLeft, ssRight, ssProduct, 0.1);
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Complete algebraic connectivity analysis.
     *
     * @param ss the state space
     * @return {@link FiedlerResult} with all analysis results
     */
    public static FiedlerResult analyze(StateSpace ss) {
        double fv = fiedlerValue(ss);
        List<Integer> cuts = findCutVertices(ss);
        int vConn = vertexConnectivity(ss);
        int eConn = edgeConnectivity(ss);
        int numEdges = EigenvalueComputer.edgeCount(ss);
        int bottleneck = findBottleneck(ss);
        double[] cheeger = cheegerBounds(ss);

        return new FiedlerResult(
                ss.states().size(),
                fv,
                fv > 1e-10,
                cuts,
                vConn,
                eConn,
                numEdges,
                bottleneck,
                cheeger[0],
                cheeger[1]);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Build the Laplacian matrix L = D - A of the undirected Hasse diagram.
     * Uses AlgebraicChecker's adjacency matrix.
     */
    private static double[][] buildLaplacian(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            double deg = 0;
            for (int j = 0; j < n; j++) deg += A[i][j];
            for (int j = 0; j < n; j++) {
                L[i][j] = (i == j) ? deg : -A[i][j];
            }
        }
        return L;
    }

    /**
     * Build undirected adjacency from the Hasse diagram.
     */
    private static Map<Integer, Set<Integer>> buildHasseAdj(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new HashSet<>());
        }
        // Use AlgebraicChecker's hasseEdges
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        List<Integer> stateList = AlgebraicChecker.stateList(ss);
        int n = stateList.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (A[i][j] > 0.5 || A[j][i] > 0.5) {
                    int si = stateList.get(i);
                    int sj = stateList.get(j);
                    adj.get(si).add(sj);
                    adj.get(sj).add(si);
                }
            }
        }
        return adj;
    }

    /**
     * Check if the Hasse graph is connected after removing a vertex.
     * Pass -1 (or any non-existent state) to check without exclusion.
     */
    private static boolean isConnectedWithout(Map<Integer, Set<Integer>> adj, int exclude) {
        List<Integer> nodes = new ArrayList<>();
        for (int s : adj.keySet()) {
            if (s != exclude) nodes.add(s);
        }
        if (nodes.isEmpty()) return true;

        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(nodes.get(0));
        while (!stack.isEmpty()) {
            int u = stack.pop();
            if (visited.add(u)) {
                for (int v : adj.get(u)) {
                    if (v != exclude && !visited.contains(v)) {
                        stack.push(v);
                    }
                }
            }
        }
        return visited.size() == nodes.size();
    }
}
