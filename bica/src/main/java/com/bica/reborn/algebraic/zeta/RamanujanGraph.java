package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Ramanujan optimality for session type lattice graphs
 * (port of Python {@code reticulate.ramanujan}, Step 31f).
 *
 * <p>Checks whether session type lattice Hasse diagrams are Ramanujan graphs:
 * for a {@code d}-regular graph, every non-trivial adjacency-matrix eigenvalue
 * satisfies {@code |lambda| <= 2*sqrt(d-1)} (the Alon-Boppana bound). Ramanujan
 * graphs are the optimal expanders for their degree.
 *
 * <p>Re-uses {@link ExpanderCodes#hasseEdges}, {@link ExpanderCodes#undirectedAdj},
 * and {@link HeatKernel#jacobiEigendecomposition} for spectral computations.
 * Bit-for-bit consistent with the Python reference up to numerical tolerance.
 */
public final class RamanujanGraph {

    private RamanujanGraph() {}

    private static final double TOL = 1e-9;

    // =======================================================================
    // Result types
    // =======================================================================

    /** Degree statistics of the undirected Hasse diagram. */
    public record DegreeStats(
            int minDegree,
            int maxDegree,
            double avgDegree,
            boolean isRegular,
            List<Integer> degreeSequence) {}

    /** Complete Ramanujan optimality analysis. */
    public record RamanujanResult(
            int numStates,
            int numEdges,
            DegreeStats degreeStats,
            double[] eigenvalues,
            double spectralRadius,
            double nontrivialRadius,
            double alonBoppana,
            boolean isRamanujan,
            double ramanujanGap,
            double ramanujanRatio,
            boolean optimalExpansion) {}

    // =======================================================================
    // Degree statistics
    // =======================================================================

    public static DegreeStats degreeStats(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = ExpanderCodes.undirectedAdj(ss);
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);
        List<Integer> degrees = new ArrayList<>();
        for (int s : states) degrees.add(adj.get(s).size());
        Collections.sort(degrees);

        if (degrees.isEmpty()) {
            return new DegreeStats(0, 0, 0.0, true, List.of());
        }
        int minD = degrees.get(0);
        int maxD = degrees.get(degrees.size() - 1);
        double sum = 0.0;
        for (int d : degrees) sum += d;
        double avg = sum / degrees.size();
        return new DegreeStats(minD, maxD, avg, minD == maxD, List.copyOf(degrees));
    }

    // =======================================================================
    // Adjacency eigenvalues
    // =======================================================================

    /** Sorted eigenvalues of the undirected Hasse adjacency matrix. */
    public static double[] adjacencyEigenvalues(StateSpace ss) {
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);
        int n = states.size();
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{0.0};
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        double[][] A = new double[n][n];
        for (int[] e : ExpanderCodes.hasseEdges(ss)) {
            int i = idx.get(e[0]);
            int j = idx.get(e[1]);
            A[i][j] = 1.0;
            A[j][i] = 1.0;
        }
        double[] eigs = jacobiEigenvalues(A, 100 * n * n);
        Arrays.sort(eigs);
        return eigs;
    }

    /**
     * Jacobi eigenvalues of a real symmetric matrix. Unlike
     * {@link HeatKernel#jacobiEigendecomposition}, this routine does NOT
     * clamp negative eigenvalues to zero (the adjacency matrix has genuine
     * negative eigenvalues, e.g. -1 for {@code P_2}, -2 for the 4-cycle).
     */
    private static double[] jacobiEigenvalues(double[][] M, int maxIter) {
        int n = M.length;
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{M[0][0]};
        double[][] A = new double[n][n];
        for (int i = 0; i < n; i++) A[i] = M[i].clone();
        double eps = 1e-12;
        for (int iter = 0; iter < maxIter; iter++) {
            double maxVal = 0.0;
            int p = 0, q = 1;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double a = Math.abs(A[i][j]);
                    if (a > maxVal) { maxVal = a; p = i; q = j; }
                }
            }
            if (maxVal < eps) break;
            double theta;
            if (Math.abs(A[p][p] - A[q][q]) < 1e-15) {
                theta = Math.PI / 4.0;
            } else {
                theta = 0.5 * Math.atan2(2.0 * A[p][q], A[p][p] - A[q][q]);
            }
            double c = Math.cos(theta);
            double s = Math.sin(theta);
            double[][] nA = new double[n][n];
            for (int i = 0; i < n; i++) nA[i] = A[i].clone();
            for (int i = 0; i < n; i++) {
                if (i != p && i != q) {
                    nA[i][p] = c * A[i][p] + s * A[i][q];
                    nA[p][i] = nA[i][p];
                    nA[i][q] = -s * A[i][p] + c * A[i][q];
                    nA[q][i] = nA[i][q];
                }
            }
            nA[p][p] = c * c * A[p][p] + 2 * s * c * A[p][q] + s * s * A[q][q];
            nA[q][q] = s * s * A[p][p] - 2 * s * c * A[p][q] + c * c * A[q][q];
            nA[p][q] = 0.0;
            nA[q][p] = 0.0;
            A = nA;
        }
        double[] eigs = new double[n];
        for (int i = 0; i < n; i++) eigs[i] = A[i][i];
        return eigs;
    }

    // =======================================================================
    // Alon-Boppana bound
    // =======================================================================

    /** Alon-Boppana bound {@code 2*sqrt(d-1)} for a {@code d}-regular graph. */
    public static double alonBoppanaBound(int d) {
        if (d <= 1) return 0.0;
        return 2.0 * Math.sqrt(d - 1);
    }

    // =======================================================================
    // Non-trivial spectral radius
    // =======================================================================

    private static double nontrivialRadius(double[] eigs) {
        if (eigs.length <= 1) return 0.0;
        // Largest eigenvalue is trivial; consider all others.
        double max = 0.0;
        for (int i = 0; i < eigs.length - 1; i++) {
            double a = Math.abs(eigs[i]);
            if (a > max) max = a;
        }
        return max;
    }

    // =======================================================================
    // Ramanujan check
    // =======================================================================

    public static boolean isRamanujan(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 2) return true;
        DegreeStats stats = degreeStats(ss);
        int dMax = stats.maxDegree();
        if (dMax <= 1) return true;
        double[] eigs = adjacencyEigenvalues(ss);
        double bound = alonBoppanaBound(dMax);
        double ntr = nontrivialRadius(eigs);
        return ntr <= bound + TOL;
    }

    public static double ramanujanGap(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 2) return 0.0;
        DegreeStats stats = degreeStats(ss);
        int dMax = stats.maxDegree();
        if (dMax <= 1) return 0.0;
        double[] eigs = adjacencyEigenvalues(ss);
        double bound = alonBoppanaBound(dMax);
        double ntr = nontrivialRadius(eigs);
        return ntr - bound;
    }

    public static double ramanujanRatio(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 2) return 0.0;
        DegreeStats stats = degreeStats(ss);
        int dMax = stats.maxDegree();
        double bound = alonBoppanaBound(dMax);
        if (bound < 1e-15) return 0.0;
        double[] eigs = adjacencyEigenvalues(ss);
        double ntr = nontrivialRadius(eigs);
        return ntr / bound;
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    public static RamanujanResult optimalExpansionCheck(StateSpace ss) {
        DegreeStats stats = degreeStats(ss);
        double[] eigs = adjacencyEigenvalues(ss);
        int n = ss.states().size();
        int nEdges = ExpanderCodes.hasseEdges(ss).size();

        double sp = 0.0;
        for (double e : eigs) {
            double a = Math.abs(e);
            if (a > sp) sp = a;
        }
        double ntr = nontrivialRadius(eigs);
        double bound = alonBoppanaBound(stats.maxDegree());

        boolean isRam = (n > 2 && stats.maxDegree() > 1) ? (ntr <= bound + TOL) : true;
        double gap = (n > 2 && stats.maxDegree() > 1) ? (ntr - bound) : 0.0;
        double ratio = (bound > 1e-15) ? (ntr / bound) : 0.0;

        // Optimal expansion = Ramanujan AND connected (positive Fiedler value of
        // the Hasse-edge Laplacian, matching Python's reticulate.matrix.fiedler_value).
        double fiedler = ExpanderCodes.spectralGap(ss);
        boolean optimal = isRam && fiedler > 1e-9;

        return new RamanujanResult(
                n, nEdges, stats, eigs, sp, ntr, bound,
                isRam, gap, ratio, optimal);
    }
}
