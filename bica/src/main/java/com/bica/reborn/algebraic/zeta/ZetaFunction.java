package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Zeta function analysis for session type lattices.
 *
 * <p>The zeta matrix Z of a finite poset P encodes the order relation:
 * Z[x,y] = 1 if x &ge; y (y is reachable from x). This class provides:
 * <ul>
 *   <li>{@link #compute} &mdash; the zeta matrix itself</li>
 *   <li>{@link #mobiusInverse} &mdash; the Mobius inverse M = Z^{-1}</li>
 *   <li>{@link #chainCount} &mdash; chains of a given length from top to bottom</li>
 *   <li>{@link #density} &mdash; fraction of comparable pairs in the poset</li>
 * </ul>
 *
 * <p>Key composition property: Z(L1 x L2) = Z(L1) (Kronecker) Z(L2).
 */
public final class ZetaFunction {

    private ZetaFunction() {}

    // -----------------------------------------------------------------------
    // Zeta matrix computation
    // -----------------------------------------------------------------------

    /**
     * Compute the zeta matrix Z[i,j] = 1.0 if state_j is reachable from state_i.
     *
     * <p>States are indexed in sorted order. The diagonal is always 1 (reflexivity).
     *
     * @param ss the state space
     * @return n x n zeta matrix
     */
    public static double[][] compute(StateSpace ss) {
        List<Integer> states = stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = stateIndex(states);
        Map<Integer, Set<Integer>> reach = reachability(ss);

        double[][] Z = new double[n][n];
        for (int s : states) {
            for (int t : reach.get(s)) {
                Z[idx.get(s)][idx.get(t)] = 1.0;
            }
        }
        return Z;
    }

    // -----------------------------------------------------------------------
    // Mobius inverse
    // -----------------------------------------------------------------------

    /**
     * Compute the Mobius inverse M = Z^{-1} of the given zeta matrix.
     *
     * <p>Uses Gaussian elimination. For a valid zeta matrix of a poset,
     * the result has integer entries (up to floating-point rounding).
     * M encodes inclusion-exclusion on the lattice.
     *
     * @param zeta n x n zeta matrix
     * @return n x n Mobius matrix (inverse of zeta)
     * @throws IllegalArgumentException if the matrix is singular
     */
    public static double[][] mobiusInverse(double[][] zeta) {
        int n = zeta.length;
        if (n == 0) return new double[0][0];

        // Augmented matrix [Z | I]
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(zeta[i], 0, aug[i], 0, n);
            aug[i][n + i] = 1.0;
        }

        // Gauss-Jordan elimination with partial pivoting
        for (int col = 0; col < n; col++) {
            int pivotRow = col;
            double pivotVal = Math.abs(aug[col][col]);
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(aug[row][col]) > pivotVal) {
                    pivotVal = Math.abs(aug[row][col]);
                    pivotRow = row;
                }
            }
            if (pivotVal < 1e-14) {
                throw new IllegalArgumentException("Zeta matrix is singular");
            }

            if (pivotRow != col) {
                double[] tmp = aug[col];
                aug[col] = aug[pivotRow];
                aug[pivotRow] = tmp;
            }

            double scale = aug[col][col];
            for (int j = 0; j < 2 * n; j++) {
                aug[col][j] /= scale;
            }

            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double factor = aug[row][col];
                for (int j = 0; j < 2 * n; j++) {
                    aug[row][j] -= factor * aug[col][j];
                }
            }
        }

        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(aug[i], n, inv[i], 0, n);
        }
        return inv;
    }

    // -----------------------------------------------------------------------
    // Chain counting
    // -----------------------------------------------------------------------

    /**
     * Count chains of exactly the given length from top to bottom.
     *
     * <p>A chain of length k is a sequence top = s0 &gt; s1 &gt; ... &gt; sk = bottom
     * where each si covers s_{i+1} in the Hasse diagram. Uses matrix
     * exponentiation of the Hasse (covering) matrix: H^k[top, bottom] counts
     * chains of length exactly k.
     *
     * @param ss     the state space
     * @param length the desired chain length
     * @return number of chains of that length from top to bottom
     */
    public static int chainCount(StateSpace ss, int length) {
        if (length < 0) return 0;
        if (length == 0) {
            // A chain of length 0 exists only if top == bottom
            return (ss.top() == ss.bottom()) ? 1 : 0;
        }

        List<Integer> states = stateList(ss);
        int n = states.size();
        if (n == 0) return 0;
        Map<Integer, Integer> idx = stateIndex(states);

        // Build Hasse (covering) matrix
        int[][] H = buildHasseInt(ss, states, idx);

        // Compute H^length via repeated matrix multiplication
        int[][] power = matIdentity(n);
        int[][] base = H;
        int exp = length;
        while (exp > 0) {
            if ((exp & 1) == 1) {
                power = matMulInt(power, base);
            }
            base = matMulInt(base, base);
            exp >>= 1;
        }

        int topI = idx.get(ss.top());
        int bottomI = idx.get(ss.bottom());
        return power[topI][bottomI];
    }

    // -----------------------------------------------------------------------
    // Density
    // -----------------------------------------------------------------------

    /**
     * Compute the order density: fraction of (i,j) pairs where i &ge; j.
     *
     * <p>Density 1.0 means a total order; density 1/n means just the diagonal
     * (antichain). This measures how "ordered" the protocol state space is.
     *
     * @param ss the state space
     * @return density in [0, 1]
     */
    public static double density(StateSpace ss) {
        double[][] Z = compute(ss);
        int n = Z.length;
        if (n == 0) return 0.0;
        double total = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                total += Z[i][j];
            }
        }
        return total / ((double) n * n);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    static List<Integer> stateList(StateSpace ss) {
        var list = new ArrayList<>(ss.states());
        Collections.sort(list);
        return list;
    }

    static Map<Integer, Integer> stateIndex(List<Integer> states) {
        var idx = new HashMap<Integer, Integer>();
        for (int i = 0; i < states.size(); i++) {
            idx.put(states.get(i), i);
        }
        return idx;
    }

    static Map<Integer, Set<Integer>> reachability(StateSpace ss) {
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
                    for (int v : adj.getOrDefault(u, List.of())) {
                        stack.push(v);
                    }
                }
            }
            reach.put(s, visited);
        }
        return reach;
    }

    /** Build covering (Hasse) matrix as int[][] for chain counting. */
    private static int[][] buildHasseInt(StateSpace ss, List<Integer> states, Map<Integer, Integer> idx) {
        int n = states.size();
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new HashSet<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        int[][] H = new int[n][n];
        for (int a : ss.states()) {
            Set<Integer> directTargets = adj.get(a);
            for (int b : directTargets) {
                boolean isCovering = true;
                for (int c : directTargets) {
                    if (c == b) continue;
                    if (isReachable(c, b, adj)) {
                        isCovering = false;
                        break;
                    }
                }
                if (isCovering) {
                    H[idx.get(a)][idx.get(b)] = 1;
                }
            }
        }
        return H;
    }

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

    private static int[][] matIdentity(int n) {
        int[][] I = new int[n][n];
        for (int i = 0; i < n; i++) I[i][i] = 1;
        return I;
    }

    private static int[][] matMulInt(int[][] a, int[][] b) {
        int n = a.length;
        int m = b[0].length;
        int p = b.length;
        int[][] c = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                int sum = 0;
                for (int k = 0; k < p; k++) {
                    sum += a[i][k] * b[k][j];
                }
                c[i][j] = sum;
            }
        }
        return c;
    }
}
