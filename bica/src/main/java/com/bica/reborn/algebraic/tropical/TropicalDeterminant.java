package com.bica.reborn.algebraic.tropical;

import com.bica.reborn.statespace.StateSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tropical determinant analysis for session type lattices (port of Python
 * {@code reticulate.tropical_determinant}, Step 30m).
 *
 * <p>The tropical determinant of a matrix {@code A} over the max-plus
 * semiring is
 * <pre>
 *   tdet(A) = max over permutations &sigma; of &Sigma;&#x1D62; A[i, &sigma;(i)]
 * </pre>
 * which coincides with the maximum-weight perfect matching / optimal
 * assignment problem.
 *
 * <p>Note: this class is distinct from {@link Tropical#tropicalDeterminant}
 * which computes a min-plus determinant of the distance matrix; here we
 * compute the max-plus determinant of arbitrary matrices (typically the
 * max-plus adjacency matrix of a state space).
 *
 * <p>All scalars are {@code double}. Tropical zero is
 * {@link Double#NEGATIVE_INFINITY}.
 */
public final class TropicalDeterminant {

    public static final double NEG_INF = Double.NEGATIVE_INFINITY;
    public static final double INF = Double.POSITIVE_INFINITY;

    private TropicalDeterminant() {}

    // =======================================================================
    // Result record
    // =======================================================================

    /** Complete tropical determinant analysis. */
    public record TropicalDetResult(
            double determinant,
            List<int[]> optimalPermutations,
            boolean isSingular,
            int numOptimal,
            double[][] adjugate,
            int tropicalRank,
            double assignmentWeight) {}

    // =======================================================================
    // Core: tropical determinant
    // =======================================================================

    /**
     * Tropical determinant: max over permutations &sigma; of &Sigma;&#x1D62;
     * A[i, &sigma;(i)]. Empty matrix returns {@code 0.0}; 1&times;1 returns
     * the single entry. For n &le; 8 enumerates all permutations; otherwise
     * delegates to {@link #hungarianAssignment(double[][])}.
     */
    public static double tropicalDet(double[][] matrix) {
        int n = matrix.length;
        if (n == 0) return 0.0;
        if (n == 1) return matrix[0][0];
        if (n <= 8) return maxPermSumDirect(matrix, n);
        return hungarianAssignment(matrix).weight();
    }

    private static double maxPermSumDirect(double[][] matrix, int n) {
        double[] best = {NEG_INF};
        int[] perm = new int[n];
        boolean[] used = new boolean[n];
        enumeratePerms(0, 0.0, true, matrix, n, perm, used, best);
        return best[0];
    }

    private static void enumeratePerms(
            int row, double cur, boolean feasible,
            double[][] matrix, int n, int[] perm, boolean[] used, double[] best) {
        if (row == n) {
            if (feasible && cur > best[0]) best[0] = cur;
            return;
        }
        for (int col = 0; col < n; col++) {
            if (used[col]) continue;
            double v = matrix[row][col];
            used[col] = true;
            perm[row] = col;
            if (v == NEG_INF) {
                // Skip infeasible branch for the running max but keep searching.
                // We don't need to track it further — result won't improve.
            } else {
                enumeratePerms(row + 1, cur + v, feasible, matrix, n, perm, used, best);
            }
            used[col] = false;
        }
    }

    /** Tropical determinant of the max-plus adjacency matrix of {@code ss}. */
    public static double tropicalDetFromStateSpace(StateSpace ss) {
        return tropicalDet(Tropical.maxPlusAdjacencyMatrix(ss));
    }

    // =======================================================================
    // Optimal permutations
    // =======================================================================

    /**
     * All permutations achieving {@link #tropicalDet(double[][])}. Each
     * permutation is returned as an {@code int[]} where entry {@code i} is
     * the column assigned to row {@code i}. An empty matrix returns a
     * singleton list containing the empty permutation.
     */
    public static List<int[]> optimalPermutations(double[][] matrix) {
        int n = matrix.length;
        List<int[]> result = new ArrayList<>();
        if (n == 0) {
            result.add(new int[0]);
            return result;
        }
        double detVal = tropicalDet(matrix);
        if (detVal == NEG_INF) return result;

        int[] perm = new int[n];
        boolean[] used = new boolean[n];
        collectOptimal(0, 0.0, matrix, n, perm, used, detVal, result);
        return result;
    }

    private static void collectOptimal(
            int row, double cur, double[][] matrix, int n,
            int[] perm, boolean[] used, double target, List<int[]> out) {
        if (row == n) {
            if (Math.abs(cur - target) <= 1e-12) {
                out.add(perm.clone());
            }
            return;
        }
        for (int col = 0; col < n; col++) {
            if (used[col]) continue;
            double v = matrix[row][col];
            if (v == NEG_INF) continue;
            used[col] = true;
            perm[row] = col;
            collectOptimal(row + 1, cur + v, matrix, n, perm, used, target, out);
            used[col] = false;
        }
    }

    // =======================================================================
    // Singularity
    // =======================================================================

    /**
     * A tropical matrix is singular iff the optimal assignment is not
     * unique (zero or multiple optima).
     */
    public static boolean isTropicallySingular(double[][] matrix) {
        return optimalPermutations(matrix).size() != 1;
    }

    // =======================================================================
    // Minors
    // =======================================================================

    private static double[][] submatrix(double[][] matrix, int excludeRow, int excludeCol) {
        int n = matrix.length;
        double[][] sub = new double[n - 1][n - 1];
        int ri = 0;
        for (int i = 0; i < n; i++) {
            if (i == excludeRow) continue;
            int rj = 0;
            for (int j = 0; j < n; j++) {
                if (j == excludeCol) continue;
                sub[ri][rj] = matrix[i][j];
                rj++;
            }
            ri++;
        }
        return sub;
    }

    /** Tropical determinant of the (n-1)&times;(n-1) minor with {@code row} and {@code col} removed. */
    public static double tropicalMinor(double[][] matrix, int row, int col) {
        return tropicalDet(submatrix(matrix, row, col));
    }

    /**
     * All k&times;k minors of {@code matrix}: for every choice of {@code k}
     * rows and {@code k} columns, the tropical determinant of the resulting
     * submatrix.
     */
    public static List<Double> allTropicalMinors(double[][] matrix, int k) {
        int n = matrix.length;
        List<Double> result = new ArrayList<>();
        if (k <= 0 || k > n) return result;

        int[] rows = new int[k];
        int[] cols = new int[k];
        combineRows(matrix, n, k, 0, 0, rows, cols, result);
        return result;
    }

    private static void combineRows(
            double[][] matrix, int n, int k, int start, int depth,
            int[] rows, int[] cols, List<Double> out) {
        if (depth == k) {
            combineCols(matrix, n, k, 0, 0, rows, cols, out);
            return;
        }
        for (int i = start; i <= n - (k - depth); i++) {
            rows[depth] = i;
            combineRows(matrix, n, k, i + 1, depth + 1, rows, cols, out);
        }
    }

    private static void combineCols(
            double[][] matrix, int n, int k, int start, int depth,
            int[] rows, int[] cols, List<Double> out) {
        if (depth == k) {
            double[][] sub = new double[k][k];
            for (int r = 0; r < k; r++) {
                for (int c = 0; c < k; c++) {
                    sub[r][c] = matrix[rows[r]][cols[c]];
                }
            }
            out.add(tropicalDet(sub));
            return;
        }
        for (int j = start; j <= n - (k - depth); j++) {
            cols[depth] = j;
            combineCols(matrix, n, k, j + 1, depth + 1, rows, cols, out);
        }
    }

    // =======================================================================
    // Adjugate and Cramer
    // =======================================================================

    /**
     * Tropical adjugate: {@code adj[i][j] = tdet(minor(j, i))} (with the
     * classical transpose).
     */
    public static double[][] tropicalAdjugate(double[][] matrix) {
        int n = matrix.length;
        if (n == 0) return new double[0][0];
        if (n == 1) return new double[][]{{0.0}};

        double[][] adj = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                adj[i][j] = tropicalMinor(matrix, j, i);
            }
        }
        return adj;
    }

    /**
     * Solve {@code A &otimes; x = b} in the max-plus semiring via tropical
     * Cramer's rule: {@code x[j] = max_i (adj[j][i] + b[i]) - tdet(A)}.
     * Returns {@code null} for dimension mismatch or infeasible systems,
     * and an empty list for the empty system.
     */
    public static double[] tropicalCramer(double[][] A, double[] b) {
        int n = A.length;
        if (n == 0) return new double[0];
        if (b.length != n) return null;

        double det = tropicalDet(A);
        if (det == NEG_INF) return null;

        double[][] adj = tropicalAdjugate(A);
        double[] x = new double[n];
        for (int j = 0; j < n; j++) {
            double best = NEG_INF;
            for (int i = 0; i < n; i++) {
                if (adj[j][i] != NEG_INF && b[i] != NEG_INF) {
                    double cand = adj[j][i] + b[i];
                    if (cand > best) best = cand;
                }
            }
            x[j] = (best == NEG_INF) ? NEG_INF : best - det;
        }
        return x;
    }

    // =======================================================================
    // Tropical rank
    // =======================================================================

    /**
     * Tropical rank: largest {@code k} such that some k&times;k submatrix
     * has finite tropical determinant (not {@link #NEG_INF}).
     */
    public static int tropicalRank(double[][] matrix) {
        int n = matrix.length;
        if (n == 0) return 0;
        for (int k = n; k >= 1; k--) {
            if (anyFiniteMinor(matrix, n, k)) return k;
        }
        return 0;
    }

    private static boolean anyFiniteMinor(double[][] matrix, int n, int k) {
        int[] rows = new int[k];
        int[] cols = new int[k];
        return searchRowsForFinite(matrix, n, k, 0, 0, rows, cols);
    }

    private static boolean searchRowsForFinite(
            double[][] matrix, int n, int k, int start, int depth,
            int[] rows, int[] cols) {
        if (depth == k) {
            return searchColsForFinite(matrix, n, k, 0, 0, rows, cols);
        }
        for (int i = start; i <= n - (k - depth); i++) {
            rows[depth] = i;
            if (searchRowsForFinite(matrix, n, k, i + 1, depth + 1, rows, cols)) return true;
        }
        return false;
    }

    private static boolean searchColsForFinite(
            double[][] matrix, int n, int k, int start, int depth,
            int[] rows, int[] cols) {
        if (depth == k) {
            double[][] sub = new double[k][k];
            for (int r = 0; r < k; r++) {
                for (int c = 0; c < k; c++) {
                    sub[r][c] = matrix[rows[r]][cols[c]];
                }
            }
            return tropicalDet(sub) != NEG_INF;
        }
        for (int j = start; j <= n - (k - depth); j++) {
            cols[depth] = j;
            if (searchColsForFinite(matrix, n, k, j + 1, depth + 1, rows, cols)) return true;
        }
        return false;
    }

    // =======================================================================
    // Hungarian algorithm (max-weight perfect matching)
    // =======================================================================

    /** Result of a max-weight perfect assignment. */
    public record Assignment(double weight, int[] assignment) {}

    /**
     * Hungarian algorithm for max-weight perfect assignment. Entries equal
     * to {@link #NEG_INF} denote forbidden cells. If no feasible
     * assignment exists returns {@code (NEG_INF, new int[0])}.
     */
    public static Assignment hungarianAssignment(double[][] costMatrix) {
        int n = costMatrix.length;
        if (n == 0) return new Assignment(0.0, new int[0]);
        if (n == 1) return new Assignment(costMatrix[0][0], new int[]{0});

        final double LARGE_NEG = -1e15;

        double[] u = new double[n + 1];
        double[] v = new double[n + 1];
        int[] p = new int[n + 1];
        int[] way = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minV = new double[n + 1];
            boolean[] used = new boolean[n + 1];
            Arrays.fill(minV, INF);

            while (true) {
                used[j0] = true;
                int i0 = p[j0];
                double delta = INF;
                int j1 = -1;

                for (int j = 1; j <= n; j++) {
                    if (used[j]) continue;
                    double raw = costMatrix[i0 - 1][j - 1];
                    double val = (raw == NEG_INF) ? LARGE_NEG : raw;
                    double cur = u[i0] + v[j] - val;
                    if (cur < minV[j]) {
                        minV[j] = cur;
                        way[j] = j0;
                    }
                    if (minV[j] < delta) {
                        delta = minV[j];
                        j1 = j;
                    }
                }

                if (j1 == -1) break;

                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[p[j]] -= delta;
                        v[j] += delta;
                    } else {
                        minV[j] -= delta;
                    }
                }

                j0 = j1;
                if (p[j0] == 0) break;
            }

            while (j0 != 0) {
                int j2 = way[j0];
                p[j0] = p[j2];
                j0 = j2;
            }
        }

        int[] assignment = new int[n];
        for (int j = 1; j <= n; j++) {
            int worker = p[j] - 1;
            if (worker >= 0 && worker < n) assignment[worker] = j - 1;
        }
        double total = 0.0;
        for (int i = 0; i < n; i++) {
            double c = costMatrix[i][assignment[i]];
            if (c == NEG_INF) {
                return new Assignment(NEG_INF, new int[0]);
            }
            total += c;
        }
        return new Assignment(total, assignment);
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    /**
     * Complete tropical determinant analysis of a state space. Computes
     * the determinant, all optimal permutations (for n &le; 8), the
     * adjugate (for n &le; 10), and the tropical rank (for n &le; 10).
     */
    public static TropicalDetResult analyzeTropicalDeterminant(StateSpace ss) {
        double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
        int n = A.length;

        double det = tropicalDet(A);
        List<int[]> opts;
        if (n <= 8) {
            opts = optimalPermutations(A);
        } else {
            Assignment a = hungarianAssignment(A);
            opts = new ArrayList<>();
            if (a.assignment().length > 0) opts.add(a.assignment());
        }
        boolean sing = opts.size() != 1;

        double[][] adj;
        if (n <= 10) {
            adj = tropicalAdjugate(A);
        } else {
            adj = new double[n][n];
            for (double[] row : adj) Arrays.fill(row, NEG_INF);
        }

        int rank = (n <= 10) ? tropicalRank(A) : n;

        return new TropicalDetResult(det, opts, sing, opts.size(), adj, rank, det);
    }
}
