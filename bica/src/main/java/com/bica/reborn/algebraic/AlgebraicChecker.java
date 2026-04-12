package com.bica.reborn.algebraic;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Algebraic invariants of session type lattices.
 *
 * <p>Computes matrix representations and algebraic invariants of session type
 * state spaces viewed as finite lattices:
 * <ul>
 *   <li><b>Zeta matrix</b> Z: Z[i,j] = 1 if i >= j (reachability)</li>
 *   <li><b>Mobius matrix</b> M = Z^-1: encodes inclusion-exclusion</li>
 *   <li><b>Mobius value</b> mu(top, bottom): Euler characteristic</li>
 *   <li><b>Rota characteristic polynomial</b> chi_P(t): lattice fingerprint</li>
 *   <li><b>Adjacency spectrum</b>: eigenvalues of the Hasse diagram</li>
 *   <li><b>Fiedler value</b> lambda_1: algebraic connectivity</li>
 *   <li><b>Von Neumann entropy</b>: spectral complexity</li>
 * </ul>
 *
 * <p>Key property: invariants compose cleanly under parallel:
 * mu is multiplicative, spectrum is additive, Fiedler takes min.
 */
public final class AlgebraicChecker {

    private AlgebraicChecker() {}

    // -----------------------------------------------------------------------
    // Helpers: state indexing and reachability
    // -----------------------------------------------------------------------

    /** Sorted list of state IDs for consistent indexing. */
    public static List<Integer> stateList(StateSpace ss) {
        var list = new ArrayList<>(ss.states());
        Collections.sort(list);
        return list;
    }

    /** Compute transitive closure: reach[s] = set of states reachable from s. */
    public static Map<Integer, Set<Integer>> reachability(StateSpace ss) {
        // Build adjacency
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            var visited = new HashSet<Integer>();
            var stack = new ArrayDeque<Integer>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (visited.add(u)) {
                    for (int v : adj.get(u)) {
                        stack.push(v);
                    }
                }
            }
            reach.put(s, visited);
        }
        return reach;
    }

    // -----------------------------------------------------------------------
    // Zeta and Mobius matrices
    // -----------------------------------------------------------------------

    /**
     * Compute the zeta matrix Z where Z[i,j] = 1 if state i >= state j.
     *
     * <p>Ordering: i >= j iff j is reachable from i.
     */
    public static int[][] computeZetaMatrix(StateSpace ss) {
        var states = stateList(ss);
        int n = states.size();
        var idx = new HashMap<Integer, Integer>();
        for (int i = 0; i < n; i++) {
            idx.put(states.get(i), i);
        }
        var reach = reachability(ss);

        int[][] Z = new int[n][n];
        for (int s : states) {
            for (int t : reach.get(s)) {
                Z[idx.get(s)][idx.get(t)] = 1;
            }
        }
        return Z;
    }

    /**
     * Compute the Mobius matrix M = Z^-1 (inverse of the zeta matrix).
     *
     * <p>Uses the recursive definition: mu(x,x) = 1, and for x > y:
     * mu(x,y) = -sum_{z: x>=z>y} mu(x,z).
     */
    public static int[][] computeMobiusMatrix(StateSpace ss) {
        var states = stateList(ss);
        int n = states.size();
        var idx = new HashMap<Integer, Integer>();
        for (int i = 0; i < n; i++) {
            idx.put(states.get(i), i);
        }
        var reach = reachability(ss);

        // Build adjacency
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        int[][] M = new int[n][n];

        for (int xi = 0; xi < n; xi++) {
            int x = states.get(xi);
            M[xi][xi] = 1;

            // BFS from x to compute mu(x,y) in topological order
            var bfsOrder = new ArrayList<Integer>();
            var visited = new HashSet<Integer>();
            visited.add(x);
            var queue = new ArrayDeque<Integer>();
            queue.add(x);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int v : adj.get(u)) {
                    if (visited.add(v)) {
                        bfsOrder.add(v);
                        queue.add(v);
                    }
                }
            }

            for (int y : bfsOrder) {
                int yi = idx.get(y);
                // mu(x,y) = -sum_{z: x>=z>y} mu(x,z)
                int total = 0;
                for (int z : states) {
                    if (z == y) continue;
                    int zi = idx.get(z);
                    if (reach.get(x).contains(z) && reach.get(z).contains(y)) {
                        total += M[xi][zi];
                    }
                }
                M[xi][yi] = -total;
            }
        }
        return M;
    }

    /**
     * Compute mu(top, bottom) -- the Mobius function value from top to bottom.
     */
    public static int computeMobiusValue(StateSpace ss) {
        int[][] M = computeMobiusMatrix(ss);
        var states = stateList(ss);
        var idx = new HashMap<Integer, Integer>();
        for (int i = 0; i < states.size(); i++) {
            idx.put(states.get(i), i);
        }
        return M[idx.get(ss.top())][idx.get(ss.bottom())];
    }

    // -----------------------------------------------------------------------
    // Rota characteristic polynomial
    // -----------------------------------------------------------------------

    /**
     * Compute Rota's characteristic polynomial chi_P(t).
     *
     * <p>chi_P(t) = sum_x mu(top, x) * t^{height - rank(x)}
     *
     * <p>Returns coefficients [a_n, a_{n-1}, ..., a_0] (highest degree first).
     */
    public static List<Integer> computeRotaPolynomial(StateSpace ss) {
        var states = stateList(ss);
        int n = states.size();
        var idx = new HashMap<Integer, Integer>();
        for (int i = 0; i < n; i++) {
            idx.put(states.get(i), i);
        }
        int[][] M = computeMobiusMatrix(ss);
        int topIdx = idx.get(ss.top());

        // Compute rank via BFS from top
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        Map<Integer, Integer> rank = new HashMap<>();
        for (int s : ss.states()) {
            rank.put(s, -1);
        }
        rank.put(ss.top(), 0);
        var queue = new ArrayDeque<Integer>();
        queue.add(ss.top());
        var visited = new HashSet<Integer>();
        visited.add(ss.top());
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : adj.get(u)) {
                if (visited.add(v)) {
                    rank.put(v, rank.get(u) + 1);
                    queue.add(v);
                }
            }
        }
        // Assign rank 0 to unreachable
        for (int s : ss.states()) {
            if (rank.get(s) < 0) {
                rank.put(s, 0);
            }
        }

        int maxRank = rank.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        int[] coeffs = new int[maxRank + 1];
        for (int x : states) {
            int xi = idx.get(x);
            int muVal = M[topIdx][xi];
            int r = rank.get(x);
            int degree = maxRank - r;
            if (degree >= 0 && degree <= maxRank) {
                coeffs[maxRank - degree] += muVal;
            }
        }

        // Remove leading zeros
        int start = 0;
        while (start < coeffs.length - 1 && coeffs[start] == 0) {
            start++;
        }
        var result = new ArrayList<Integer>();
        for (int i = start; i < coeffs.length; i++) {
            result.add(coeffs[i]);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Hasse edges and adjacency matrix
    // -----------------------------------------------------------------------

    /**
     * Compute the covering relation (Hasse diagram edges).
     *
     * <p>(a, b) is a covering pair iff a > b and no c with a > c > b.
     */
    public static List<int[]> hasseEdges(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new HashSet<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        var edges = new ArrayList<int[]>();
        for (int a : ss.states()) {
            Set<Integer> directTargets = adj.get(a);
            for (int b : directTargets) {
                // a covers b iff b is not reachable from any other direct successor
                boolean isCovering = true;
                for (int c : directTargets) {
                    if (c == b) continue;
                    // BFS/DFS from c to check if b is reachable
                    var visited = new HashSet<Integer>();
                    var stack = new ArrayDeque<Integer>();
                    stack.push(c);
                    boolean found = false;
                    while (!stack.isEmpty()) {
                        int u = stack.pop();
                        if (u == b) {
                            found = true;
                            break;
                        }
                        if (visited.add(u)) {
                            for (int v : adj.get(u)) {
                                if (!visited.contains(v)) {
                                    stack.push(v);
                                }
                            }
                        }
                    }
                    if (found) {
                        isCovering = false;
                        break;
                    }
                }
                if (isCovering) {
                    edges.add(new int[]{a, b});
                }
            }
        }
        return edges;
    }

    /**
     * Compute the adjacency matrix of the undirected Hasse diagram.
     */
    public static double[][] computeAdjacencyMatrix(StateSpace ss) {
        var states = stateList(ss);
        int n = states.size();
        var idx = new HashMap<Integer, Integer>();
        for (int i = 0; i < n; i++) {
            idx.put(states.get(i), i);
        }

        double[][] A = new double[n][n];
        for (int[] edge : hasseEdges(ss)) {
            int ai = idx.get(edge[0]);
            int bi = idx.get(edge[1]);
            A[ai][bi] = 1.0;
            A[bi][ai] = 1.0;
        }
        return A;
    }

    // -----------------------------------------------------------------------
    // Eigenvalue computation (Jacobi iteration for symmetric matrices)
    // -----------------------------------------------------------------------

    /**
     * Compute eigenvalues of a real symmetric matrix via Jacobi iteration.
     *
     * <p>Simple O(n^3) implementation sufficient for small matrices (less than 100).
     *
     * @param A a real symmetric matrix
     * @return sorted eigenvalues
     */
    public static double[] computeEigenvalues(double[][] A) {
        int n = A.length;
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{A[0][0]};
        if (n == 2) {
            double a = A[0][0], b = A[0][1], d = A[1][1];
            double trace = a + d;
            double det = a * d - b * b;
            double disc = trace * trace - 4 * det;
            if (disc < 0) disc = 0.0;
            double sqrtDisc = Math.sqrt(disc);
            double[] result = {(trace - sqrtDisc) / 2.0, (trace + sqrtDisc) / 2.0};
            Arrays.sort(result);
            return result;
        }

        // Jacobi eigenvalue algorithm
        double[][] S = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, S[i], 0, n);
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

            // Compute rotation angle
            double theta;
            if (Math.abs(S[p][p] - S[q][q]) < 1e-15) {
                theta = Math.PI / 4;
            } else {
                theta = 0.5 * Math.atan2(2 * S[p][q], S[p][p] - S[q][q]);
            }

            double c = Math.cos(theta);
            double s = Math.sin(theta);

            // Apply Givens rotation
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
            S = newS;
        }

        double[] eigenvalues = new double[n];
        for (int i = 0; i < n; i++) {
            eigenvalues[i] = S[i][i];
        }
        Arrays.sort(eigenvalues);
        return eigenvalues;
    }

    // -----------------------------------------------------------------------
    // Spectral radius
    // -----------------------------------------------------------------------

    /**
     * Compute the spectral radius (largest absolute eigenvalue of the Hasse adjacency).
     */
    public static double spectralRadius(StateSpace ss) {
        double[][] A = computeAdjacencyMatrix(ss);
        double[] eigs = computeEigenvalues(A);
        if (eigs.length == 0) return 0.0;
        double max = 0.0;
        for (double e : eigs) {
            max = Math.max(max, Math.abs(e));
        }
        return max;
    }

    // -----------------------------------------------------------------------
    // Laplacian and Fiedler value
    // -----------------------------------------------------------------------

    /**
     * Compute the Laplacian L = D - A of the undirected Hasse diagram.
     */
    static double[][] laplacianMatrix(StateSpace ss) {
        double[][] A = computeAdjacencyMatrix(ss);
        int n = A.length;
        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            double deg = 0;
            for (int j = 0; j < n; j++) {
                deg += A[i][j];
            }
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    L[i][j] = deg;
                } else {
                    L[i][j] = -A[i][j];
                }
            }
        }
        return L;
    }

    /**
     * Compute the Fiedler value (algebraic connectivity).
     *
     * <p>This is the second-smallest eigenvalue of the Laplacian.
     * lambda_1 > 0 iff the Hasse diagram is connected.
     */
    public static double fiedlerValue(StateSpace ss) {
        double[][] L = laplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return 0.0;

        double[] eigs = computeEigenvalues(L);
        Arrays.sort(eigs);
        if (eigs.length < 2) return 0.0;
        return Math.max(0.0, eigs[1]); // Clamp numerical noise
    }

    // -----------------------------------------------------------------------
    // Tropical distance and eigenvalue
    // -----------------------------------------------------------------------

    /**
     * Compute the tropical diameter: longest shortest path over all reachable pairs.
     *
     * <p>Uses Floyd-Warshall for all-pairs shortest paths.
     */
    public static int tropicalDiameter(StateSpace ss) {
        var states = stateList(ss);
        int n = states.size();
        var idx = new HashMap<Integer, Integer>();
        for (int i = 0; i < n; i++) {
            idx.put(states.get(i), i);
        }
        int INF = n + 1;

        int[][] D = new int[n][n];
        for (int[] row : D) Arrays.fill(row, INF);
        for (int i = 0; i < n; i++) D[i][i] = 0;

        for (var t : ss.transitions()) {
            int si = idx.get(t.source());
            int ti = idx.get(t.target());
            D[si][ti] = Math.min(D[si][ti], 1);
        }

        // Floyd-Warshall
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (D[i][k] + D[k][j] < D[i][j]) {
                        D[i][j] = D[i][k] + D[k][j];
                    }
                }
            }
        }

        int maxD = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (D[i][j] < INF && D[i][j] > maxD) {
                    maxD = D[i][j];
                }
            }
        }
        return maxD;
    }

    /**
     * Compute the tropical (max-plus) eigenvalue: maximum cycle mean.
     *
     * <p>For unit-weight session types: 1.0 if cycles exist, 0.0 otherwise.
     */
    public static double tropicalEigenvalue(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        // DFS cycle detection
        int WHITE = 0, GRAY = 1, BLACK = 2;
        Map<Integer, Integer> color = new HashMap<>();
        for (int s : ss.states()) {
            color.put(s, WHITE);
        }

        for (int s : ss.states()) {
            if (color.get(s) == WHITE) {
                if (hasCycleDfs(s, adj, color, WHITE, GRAY, BLACK)) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }

    private static boolean hasCycleDfs(
            int u, Map<Integer, List<Integer>> adj, Map<Integer, Integer> color,
            int WHITE, int GRAY, int BLACK) {
        color.put(u, GRAY);
        for (int v : adj.get(u)) {
            if (color.get(v) == GRAY) return true;
            if (color.get(v) == WHITE) {
                if (hasCycleDfs(v, adj, color, WHITE, GRAY, BLACK)) return true;
            }
        }
        color.put(u, BLACK);
        return false;
    }

    // -----------------------------------------------------------------------
    // Von Neumann entropy
    // -----------------------------------------------------------------------

    /**
     * Compute the von Neumann entropy of the Laplacian.
     *
     * <p>S(G) = -sum_k (lambda_k / sum_j lambda_j) * log(lambda_k / sum_j lambda_j)
     * where lambda_k are the nonzero Laplacian eigenvalues.
     */
    public static double vonNeumannEntropy(StateSpace ss) {
        double[][] L = laplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return 0.0;

        double[] eigs = computeEigenvalues(L);
        // Filter positive eigenvalues
        double total = 0.0;
        int count = 0;
        for (double e : eigs) {
            if (e > 1e-10) {
                total += e;
                count++;
            }
        }
        if (count == 0 || total < 1e-15) return 0.0;

        double entropy = 0.0;
        for (double e : eigs) {
            if (e > 1e-10) {
                double p = e / total;
                if (p > 1e-15) {
                    entropy -= p * Math.log(p);
                }
            }
        }
        return entropy;
    }

    // -----------------------------------------------------------------------
    // Join-irreducibles and meet-irreducibles
    // -----------------------------------------------------------------------

    /**
     * Find join-irreducible elements: elements with exactly one lower cover.
     */
    public static Set<Integer> joinIrreducibles(StateSpace ss) {
        var hasse = hasseEdges(ss);
        // Lower covers of a: edges (a, b) where a covers b
        Map<Integer, Integer> lowerCoverCount = new HashMap<>();
        for (int s : ss.states()) {
            lowerCoverCount.put(s, 0);
        }
        for (int[] edge : hasse) {
            lowerCoverCount.merge(edge[0], 1, Integer::sum);
        }
        var result = new HashSet<Integer>();
        for (int s : ss.states()) {
            if (lowerCoverCount.get(s) == 1 && s != ss.bottom()) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Find meet-irreducible elements: elements with exactly one upper cover.
     */
    public static Set<Integer> meetIrreducibles(StateSpace ss) {
        var hasse = hasseEdges(ss);
        Map<Integer, Integer> upperCoverCount = new HashMap<>();
        for (int s : ss.states()) {
            upperCoverCount.put(s, 0);
        }
        for (int[] edge : hasse) {
            // a covers b: a is upper cover of b
            upperCoverCount.merge(edge[1], 1, Integer::sum);
        }
        var result = new HashSet<Integer>();
        for (int s : ss.states()) {
            if (upperCoverCount.get(s) == 1 && s != ss.top()) {
                result.add(s);
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // High-level: compute all invariants
    // -----------------------------------------------------------------------

    /**
     * Compute all algebraic invariants of a session type lattice.
     *
     * <p>This is the main entry point for algebraic analysis.
     */
    public static AlgebraicInvariants computeInvariants(StateSpace ss) {
        int mu = computeMobiusValue(ss);
        var rota = computeRotaPolynomial(ss);

        double[][] adj = computeAdjacencyMatrix(ss);
        double[] eigs = computeEigenvalues(adj);
        var eigList = new ArrayList<Double>();
        for (double e : eigs) {
            eigList.add(e);
        }

        double specRadius = 0.0;
        for (double e : eigs) {
            specRadius = Math.max(specRadius, Math.abs(e));
        }

        double fiedler = fiedlerValue(ss);
        int tropDiam = tropicalDiameter(ss);
        double tropEig = tropicalEigenvalue(ss);
        double vnEntropy = vonNeumannEntropy(ss);
        int numJI = joinIrreducibles(ss).size();
        int numMI = meetIrreducibles(ss).size();

        return new AlgebraicInvariants(
                ss.states().size(), mu, rota, eigList, specRadius,
                fiedler, tropDiam, tropEig, vnEntropy, numJI, numMI);
    }
}
