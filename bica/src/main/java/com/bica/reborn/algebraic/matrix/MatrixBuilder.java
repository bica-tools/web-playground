package com.bica.reborn.algebraic.matrix;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Builds matrix representations from a session type state space.
 *
 * <p>Provides four matrix views of a state space:
 * <ul>
 *   <li><b>Adjacency matrix</b> &mdash; directed transitions (A[i,j] = 1 if edge i to j)</li>
 *   <li><b>Zeta matrix</b> &mdash; reachability (Z[i,j] = 1 if j reachable from i)</li>
 *   <li><b>Laplacian matrix</b> &mdash; L = D - A of the undirected Hasse diagram</li>
 *   <li><b>Hasse matrix</b> &mdash; cover relations only (transitive reduction)</li>
 * </ul>
 *
 * <p>States are indexed in sorted order for consistent matrix layout.
 */
public final class MatrixBuilder {

    private MatrixBuilder() {}

    // -----------------------------------------------------------------------
    // State indexing
    // -----------------------------------------------------------------------

    /** Sorted list of state IDs for consistent matrix indexing. */
    static List<Integer> stateList(StateSpace ss) {
        var list = new ArrayList<>(ss.states());
        Collections.sort(list);
        return list;
    }

    /** State-to-index mapping. */
    static Map<Integer, Integer> stateIndex(List<Integer> states) {
        var idx = new HashMap<Integer, Integer>();
        for (int i = 0; i < states.size(); i++) {
            idx.put(states.get(i), i);
        }
        return idx;
    }

    // -----------------------------------------------------------------------
    // Adjacency matrix (directed transitions)
    // -----------------------------------------------------------------------

    /**
     * Build the directed adjacency matrix from the state space.
     *
     * <p>A[i,j] = 1.0 if there is a direct transition from state i to state j,
     * 0.0 otherwise. Multiple transitions between the same pair are collapsed
     * to a single 1.
     *
     * @param ss the state space
     * @return n x n adjacency matrix where n = |states|
     */
    public static double[][] buildAdjacencyMatrix(StateSpace ss) {
        var states = stateList(ss);
        int n = states.size();
        var idx = stateIndex(states);

        double[][] A = new double[n][n];
        for (var t : ss.transitions()) {
            int si = idx.get(t.source());
            int ti = idx.get(t.target());
            A[si][ti] = 1.0;
        }
        return A;
    }

    // -----------------------------------------------------------------------
    // Zeta matrix (reachability / order relation)
    // -----------------------------------------------------------------------

    /**
     * Build the zeta matrix (reachability matrix).
     *
     * <p>Z[i,j] = 1.0 if state j is reachable from state i (i &ge; j in the
     * session type ordering where top = initial state, bottom = end).
     * The diagonal is always 1 (reflexivity).
     *
     * @param ss the state space
     * @return n x n zeta matrix
     */
    public static double[][] buildZetaMatrix(StateSpace ss) {
        var states = stateList(ss);
        int n = states.size();
        var idx = stateIndex(states);

        // Compute reachability via DFS from each state
        Map<Integer, Set<Integer>> reach = new HashMap<>();
        Map<Integer, List<Integer>> adj = buildAdj(ss);

        for (int s : ss.states()) {
            var visited = new HashSet<Integer>();
            var stack = new ArrayDeque<Integer>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (visited.add(u)) {
                    for (int v : adj.getOrDefault(u, List.of())) {
                        stack.push(v);
                    }
                }
            }
            reach.put(s, visited);
        }

        double[][] Z = new double[n][n];
        for (int s : states) {
            for (int t : reach.get(s)) {
                Z[idx.get(s)][idx.get(t)] = 1.0;
            }
        }
        return Z;
    }

    // -----------------------------------------------------------------------
    // Laplacian matrix (L = D - A of undirected Hasse diagram)
    // -----------------------------------------------------------------------

    /**
     * Build the Laplacian matrix L = D - A of the undirected Hasse diagram.
     *
     * <p>The Hasse diagram uses cover relations (transitive reduction).
     * The Laplacian is symmetric: L[i,j] = -1 if i and j are connected
     * by a cover relation, L[i,i] = degree of state i.
     *
     * @param ss the state space
     * @return n x n Laplacian matrix
     */
    public static double[][] buildLaplacianMatrix(StateSpace ss) {
        double[][] H = buildHasseMatrix(ss);
        int n = H.length;
        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            double deg = 0;
            for (int j = 0; j < n; j++) {
                // Undirected: symmetrize the Hasse matrix
                double edge = Math.max(H[i][j], H[j][i]);
                if (i != j && edge > 0) {
                    L[i][j] = -1.0;
                    deg += 1.0;
                }
            }
            L[i][i] = deg;
        }
        return L;
    }

    // -----------------------------------------------------------------------
    // Hasse matrix (cover relations / transitive reduction)
    // -----------------------------------------------------------------------

    /**
     * Build the Hasse matrix (cover relations only).
     *
     * <p>H[i,j] = 1.0 if state i covers state j, meaning there is a direct
     * transition from i to j with no intermediate state k such that i can
     * reach k and k can reach j.
     *
     * @param ss the state space
     * @return n x n Hasse matrix
     */
    public static double[][] buildHasseMatrix(StateSpace ss) {
        var states = stateList(ss);
        int n = states.size();
        var idx = stateIndex(states);

        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new HashSet<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        double[][] H = new double[n][n];
        for (int a : ss.states()) {
            Set<Integer> directTargets = adj.get(a);
            for (int b : directTargets) {
                // a covers b iff b is not reachable from any other direct successor of a
                boolean isCovering = true;
                for (int c : directTargets) {
                    if (c == b) continue;
                    if (isReachable(c, b, adj)) {
                        isCovering = false;
                        break;
                    }
                }
                if (isCovering) {
                    H[idx.get(a)][idx.get(b)] = 1.0;
                }
            }
        }
        return H;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Map<Integer, List<Integer>> buildAdj(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }
        return adj;
    }

    /** BFS/DFS check: is target reachable from source? */
    private static boolean isReachable(int source, int target, Map<Integer, Set<Integer>> adj) {
        var visited = new HashSet<Integer>();
        var stack = new ArrayDeque<Integer>();
        stack.push(source);
        while (!stack.isEmpty()) {
            int u = stack.pop();
            if (u == target) return true;
            if (visited.add(u)) {
                for (int v : adj.getOrDefault(u, Set.of())) {
                    if (!visited.contains(v)) {
                        stack.push(v);
                    }
                }
            }
        }
        return false;
    }
}
