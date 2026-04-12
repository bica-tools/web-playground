package com.bica.reborn.algebraic.tropical;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Tropical (max-plus) algebra for session type lattices (port of Python
 * {@code reticulate.tropical}, Steps 30k-30n).
 *
 * <p>Tropical algebra replaces {@code (+, ×)} with {@code (max, +)}, providing
 * shortest/longest path analysis through matrix operations:
 * <ul>
 *   <li>Step 30k: Tropical distance matrix (all-pairs shortest paths)</li>
 *   <li>Step 30l: Tropical eigenvalue (max cycle mean / critical circuit)</li>
 *   <li>Step 30m: Tropical determinant (min-plus permanent)</li>
 *   <li>Step 30n: Max-plus longest paths (protocol depth analysis)</li>
 * </ul>
 *
 * <p>All scalars are {@code double}. Tropical zero is
 * {@link Double#NEGATIVE_INFINITY} (max-plus identity for addition). Distance
 * "infinity" (no path) is {@link Double#POSITIVE_INFINITY}. Matrices are
 * {@code double[][]}. Uses {@link Zeta#stateList(StateSpace)} and
 * {@link Zeta#adjacency(StateSpace)} for consistent state indexing.
 */
public final class Tropical {

    public static final double INF = Double.POSITIVE_INFINITY;
    public static final double NEG_INF = Double.NEGATIVE_INFINITY;

    private Tropical() {}

    // =======================================================================
    // Result type
    // =======================================================================

    /** Complete tropical algebra analysis. */
    public record TropicalResult(
            int numStates,
            double[][] distanceMatrix,
            int diameter,
            double tropicalEigenvalue,
            double tropicalDeterminant,
            int longestPathTopBottom,
            int shortestPathTopBottom,
            Map<Integer, Integer> eccentricity,
            int radius,
            List<Integer> center) {}

    // =======================================================================
    // Max-plus primitives
    // =======================================================================

    /** Max-plus addition: max(a, b). Identity: -inf. */
    public static double tropicalAdd(double a, double b) {
        return Math.max(a, b);
    }

    /** Max-plus multiplication: a + b. Identity: 0. Absorbing: -inf. */
    public static double tropicalMul(double a, double b) {
        if (a == NEG_INF || b == NEG_INF) return NEG_INF;
        return a + b;
    }

    /**
     * Max-plus adjacency matrix for a state space.
     * A[i][j] = 1.0 if there is a transition from state i to state j, -inf otherwise.
     */
    public static double[][] maxPlusAdjacencyMatrix(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        double[][] A = new double[n][n];
        for (double[] row : A) Arrays.fill(row, NEG_INF);
        for (var t : ss.transitions()) {
            Integer i = idx.get(t.source());
            Integer j = idx.get(t.target());
            if (i != null && j != null) A[i][j] = 1.0;
        }
        return A;
    }

    // =======================================================================
    // Step 30k: distance matrix + graph metrics
    // =======================================================================

    /** All-pairs shortest path distances via Floyd-Warshall (unit edge weights). */
    public static double[][] tropicalDistance(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        double[][] D = new double[n][n];
        for (double[] row : D) Arrays.fill(row, INF);
        for (int i = 0; i < n; i++) D[i][i] = 0.0;

        for (var t : ss.transitions()) {
            int si = idx.get(t.source());
            int ti = idx.get(t.target());
            if (1.0 < D[si][ti]) D[si][ti] = 1.0;
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (D[i][k] == INF) continue;
                for (int j = 0; j < n; j++) {
                    double via = D[i][k] + D[k][j];
                    if (via < D[i][j]) D[i][j] = via;
                }
            }
        }
        return D;
    }

    /** Graph diameter: maximum finite distance between any two reachable states. */
    public static int diameter(StateSpace ss) {
        double[][] D = tropicalDistance(ss);
        int n = D.length;
        int max = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (D[i][j] < INF && D[i][j] > max) max = (int) D[i][j];
            }
        }
        return max;
    }

    /** Eccentricity of each state: max distance to any reachable state. */
    public static Map<Integer, Integer> eccentricity(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        double[][] D = tropicalDistance(ss);
        int n = states.size();
        Map<Integer, Integer> ecc = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int maxD = 0;
            for (int j = 0; j < n; j++) {
                if (D[i][j] < INF && D[i][j] > maxD) maxD = (int) D[i][j];
            }
            ecc.put(states.get(i), maxD);
        }
        return ecc;
    }

    /** Graph radius: minimum eccentricity. */
    public static int radius(StateSpace ss) {
        Map<Integer, Integer> ecc = eccentricity(ss);
        if (ecc.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (int v : ecc.values()) if (v < min) min = v;
        return min;
    }

    /** Graph center: sorted list of states with eccentricity = radius. */
    public static List<Integer> center(StateSpace ss) {
        Map<Integer, Integer> ecc = eccentricity(ss);
        if (ecc.isEmpty()) return List.of();
        int r = Integer.MAX_VALUE;
        for (int v : ecc.values()) if (v < r) r = v;
        List<Integer> result = new ArrayList<>();
        for (var e : ecc.entrySet()) if (e.getValue() == r) result.add(e.getKey());
        Collections.sort(result);
        return result;
    }

    // =======================================================================
    // Step 30l: tropical eigenvalue (max cycle mean) — Karp's algorithm
    // =======================================================================

    /** Maximum cycle mean (tropical eigenvalue). Unit-weight edges: 1 if cyclic, 0 if acyclic. */
    public static double tropicalEigenvalue(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n == 0) return 0.0;

        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        double[][] dk = new double[n + 1][n];
        double maxMean = 0.0;
        for (int srcI = 0; srcI < n; srcI++) {
            for (int k = 0; k <= n; k++) Arrays.fill(dk[k], INF);
            dk[0][srcI] = 0.0;

            for (int k = 0; k < n; k++) {
                for (int vIdx = 0; vIdx < n; vIdx++) {
                    if (dk[k][vIdx] >= INF) continue;
                    int v = states.get(vIdx);
                    for (int w : adj.getOrDefault(v, List.of())) {
                        int wIdx = idx.get(w);
                        double nd = dk[k][vIdx] + 1.0;
                        if (nd < dk[k + 1][wIdx]) dk[k + 1][wIdx] = nd;
                    }
                }
            }

            for (int v = 0; v < n; v++) {
                if (dk[n][v] >= INF) continue;
                double minRatio = INF;
                for (int k = 0; k < n; k++) {
                    if (dk[k][v] < INF) {
                        double ratio = (dk[n][v] - dk[k][v]) / (n - k);
                        if (ratio < minRatio) minRatio = ratio;
                    }
                }
                if (minRatio < INF && minRatio > maxMean) maxMean = minRatio;
            }
        }
        return maxMean;
    }

    /** Max-plus spectral radius = tropical eigenvalue. */
    public static double tropicalSpectralRadius(StateSpace ss) {
        return tropicalEigenvalue(ss);
    }

    /**
     * Approximate max-plus eigenvector via power iteration.
     * v &leftarrow; A &otimes; v, normalized by subtracting max.
     */
    public static double[] tropicalEigenvector(StateSpace ss) {
        double[][] A = maxPlusAdjacencyMatrix(ss);
        int n = A.length;
        if (n == 0) return new double[0];
        double[] v = new double[n];
        for (int iter = 0; iter <= n; iter++) {
            double[] nv = new double[n];
            Arrays.fill(nv, NEG_INF);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    nv[i] = tropicalAdd(nv[i], tropicalMul(A[i][j], v[j]));
                }
            }
            double mx = NEG_INF;
            for (double x : nv) if (x > mx) mx = x;
            if (mx > NEG_INF) {
                for (int i = 0; i < n; i++) v[i] = nv[i] - mx;
            } else {
                v = nv;
            }
        }
        return v;
    }

    /** Edges on critical cycles (achieving the max cycle mean). */
    public static List<int[]> criticalGraph(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n == 0) return List.of();

        double lam = tropicalEigenvalue(ss);
        if (lam == 0.0) return List.of();

        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        double[][] D = tropicalDistance(ss);

        List<int[]> critical = new ArrayList<>();
        for (var t : ss.transitions()) {
            Integer i = idx.get(t.source());
            Integer j = idx.get(t.target());
            if (i == null || j == null) continue;
            if (D[j][i] < INF) {
                double cycleLen = D[j][i] + 1.0;
                if (cycleLen > 0) {
                    double mean = cycleLen / cycleLen;
                    if (Math.abs(mean - lam) < 1e-10 || mean >= lam - 1e-10) {
                        critical.add(new int[]{t.source(), t.target()});
                    }
                }
            }
        }
        return critical;
    }

    // =======================================================================
    // Step 30m: tropical determinant (min-plus permanent)
    // =======================================================================

    /** Tropical determinant: min over permutations &sigma; of &Sigma;&#x1D62; D[i][&sigma;(i)]. */
    public static double tropicalDeterminant(StateSpace ss) {
        double[][] D = tropicalDistance(ss);
        int n = D.length;
        if (n == 0) return 0.0;
        if (n == 1) return D[0][0];
        if (n <= 10) return minPermSum(D, n);
        return greedyAssignment(D, n);
    }

    private static double minPermSum(double[][] D, int n) {
        double[] best = {INF};
        searchPerm(0, 0, 0.0, D, n, best);
        return best[0];
    }

    private static void searchPerm(int row, int used, double cur, double[][] D, int n, double[] best) {
        if (cur >= best[0]) return;
        if (row == n) {
            if (cur < best[0]) best[0] = cur;
            return;
        }
        for (int col = 0; col < n; col++) {
            if ((used & (1 << col)) != 0) continue;
            searchPerm(row + 1, used | (1 << col), cur + D[row][col], D, n, best);
        }
    }

    private static double greedyAssignment(double[][] D, int n) {
        Set<Integer> used = new HashSet<>();
        double total = 0.0;
        for (int i = 0; i < n; i++) {
            int bestJ = -1;
            double bestVal = INF;
            for (int j = 0; j < n; j++) {
                if (!used.contains(j) && D[i][j] < bestVal) {
                    bestVal = D[i][j];
                    bestJ = j;
                }
            }
            if (bestJ >= 0) {
                used.add(bestJ);
                total += bestVal;
            } else {
                total += INF;
            }
        }
        return total;
    }

    // =======================================================================
    // Step 30n: max-plus longest paths
    // =======================================================================

    /** All-pairs longest path matrix (max-plus closure). Well-defined for DAGs. */
    public static double[][] longestPathMatrix(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        double[][] L = new double[n][n];
        for (double[] row : L) Arrays.fill(row, NEG_INF);
        for (int i = 0; i < n; i++) L[i][i] = 0.0;

        for (var t : ss.transitions()) {
            int si = idx.get(t.source());
            int ti = idx.get(t.target());
            if (1.0 > L[si][ti]) L[si][ti] = 1.0;
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (L[i][k] == NEG_INF) continue;
                for (int j = 0; j < n; j++) {
                    if (L[k][j] == NEG_INF) continue;
                    double cand = L[i][k] + L[k][j];
                    if (cand > L[i][j]) L[i][j] = cand;
                }
            }
        }
        return L;
    }

    /** Longest directed path from top to bottom. */
    public static int longestPathTopBottom(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);
        double[][] L = longestPathMatrix(ss);
        int topI = idx.get(ss.top());
        int botI = idx.get(ss.bottom());
        double val = L[topI][botI];
        return val > NEG_INF ? (int) val : 0;
    }

    /** Shortest directed path from top to bottom. */
    public static int shortestPathTopBottom(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);
        double[][] D = tropicalDistance(ss);
        int topI = idx.get(ss.top());
        int botI = idx.get(ss.bottom());
        double val = D[topI][botI];
        return val < INF ? (int) val : 0;
    }

    // =======================================================================
    // Matrix operations (max-plus)
    // =======================================================================

    /** Max-plus matrix multiplication: C[i][j] = max&#x2096; (A[i][k] + B[k][j]). */
    public static double[][] tropicalMatrixMul(double[][] A, double[][] B) {
        int n = A.length;
        int p = B.length;
        int m = (p == 0) ? 0 : B[0].length;
        double[][] C = new double[n][m];
        for (double[] row : C) Arrays.fill(row, NEG_INF);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                for (int k = 0; k < p; k++) {
                    double val = tropicalMul(A[i][k], B[k][j]);
                    C[i][j] = tropicalAdd(C[i][j], val);
                }
            }
        }
        return C;
    }

    /** Max-plus matrix power A^k by fast exponentiation. */
    public static double[][] tropicalMatrixPower(double[][] A, int k) {
        int n = A.length;
        double[][] result = new double[n][n];
        for (double[] row : result) Arrays.fill(row, NEG_INF);
        for (int i = 0; i < n; i++) result[i][i] = 0.0;
        double[][] base = new double[n][n];
        for (int i = 0; i < n; i++) base[i] = A[i].clone();
        int exp = k;
        while (exp > 0) {
            if ((exp & 1) == 1) result = tropicalMatrixMul(result, base);
            base = tropicalMatrixMul(base, base);
            exp >>= 1;
        }
        return result;
    }

    // =======================================================================
    // Top-level analyser
    // =======================================================================

    /** Complete tropical algebra analysis. */
    public static TropicalResult analyzeTropical(StateSpace ss) {
        double[][] D = tropicalDistance(ss);
        int n = Zeta.stateList(ss).size();
        return new TropicalResult(
                n,
                D,
                diameter(ss),
                tropicalEigenvalue(ss),
                tropicalDeterminant(ss),
                longestPathTopBottom(ss),
                shortestPathTopBottom(ss),
                eccentricity(ss),
                radius(ss),
                center(ss));
    }
}
