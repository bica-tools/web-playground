package com.bica.reborn.algebraic.eigenvalues;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Computes eigenvalues of adjacency and Laplacian matrices for session type lattices.
 *
 * <p>The Hasse diagram of a lattice, viewed as an undirected graph, carries
 * spectral information that characterises the protocol's structure:
 * <ul>
 *   <li><b>Adjacency eigenvalues</b> encode graph symmetry and expansion</li>
 *   <li><b>Laplacian eigenvalues</b> encode connectivity and partitioning</li>
 *   <li><b>Spectral radius</b> bounds chromatic number and independence number</li>
 *   <li><b>Spectral gap</b> measures expansion / rapid mixing</li>
 * </ul>
 *
 * <p>Eigenvalue computation uses Jacobi iteration from
 * {@link AlgebraicChecker#computeEigenvalues(double[][])}, which is
 * sufficient for small matrices (n <= 20).
 */
public final class EigenvalueComputer {

    private EigenvalueComputer() {}

    /**
     * Compute sorted eigenvalues of the undirected Hasse adjacency matrix.
     *
     * @param ss the state space
     * @return eigenvalues sorted in ascending order
     */
    public static double[] adjacencyEigenvalues(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        return AlgebraicChecker.computeEigenvalues(A);
    }

    /**
     * Compute sorted eigenvalues of the Laplacian L = D - A.
     *
     * <p>The smallest eigenvalue is always 0 (for connected graphs, with
     * multiplicity equal to the number of connected components).
     *
     * @param ss the state space
     * @return eigenvalues sorted in ascending order
     */
    public static double[] laplacianEigenvalues(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        if (n == 0) return new double[0];

        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            double deg = 0;
            for (int j = 0; j < n; j++) deg += A[i][j];
            for (int j = 0; j < n; j++) {
                L[i][j] = (i == j) ? deg : -A[i][j];
            }
        }

        double[] eigs = AlgebraicChecker.computeEigenvalues(L);
        // Clamp tiny negative values to zero (numerical noise)
        for (int i = 0; i < eigs.length; i++) {
            if (eigs[i] < 0 && eigs[i] > -1e-10) eigs[i] = 0.0;
        }
        Arrays.sort(eigs);
        return eigs;
    }

    /**
     * Compute the spectral radius: max |lambda_i| of the adjacency matrix.
     *
     * @param ss the state space
     * @return spectral radius (>= 0)
     */
    public static double spectralRadius(StateSpace ss) {
        double[] eigs = adjacencyEigenvalues(ss);
        if (eigs.length == 0) return 0.0;
        double max = 0.0;
        for (double e : eigs) max = Math.max(max, Math.abs(e));
        return max;
    }

    /**
     * Compute the spectral gap: lambda_max - lambda_second.
     *
     * <p>A large spectral gap indicates a well-connected protocol with
     * rapid mixing and fast convergence properties.
     *
     * @param ss the state space
     * @return spectral gap (>= 0)
     */
    public static double spectralGap(StateSpace ss) {
        double[] eigs = adjacencyEigenvalues(ss);
        if (eigs.length < 2) return 0.0;
        // Sorted ascending, so largest is last
        return eigs[eigs.length - 1] - eigs[eigs.length - 2];
    }

    /**
     * Compute the graph energy: E(G) = sum |lambda_i|.
     *
     * <p>Higher energy indicates more complex graph structure.
     *
     * @param ss the state space
     * @return graph energy (>= 0)
     */
    public static double energy(StateSpace ss) {
        double[] eigs = adjacencyEigenvalues(ss);
        double sum = 0;
        for (double e : eigs) sum += Math.abs(e);
        return sum;
    }

    /**
     * Compute the Fiedler value (algebraic connectivity).
     *
     * <p>This is the second-smallest Laplacian eigenvalue.
     * Fiedler > 0 iff the Hasse diagram is connected.
     *
     * @param ss the state space
     * @return Fiedler value (>= 0)
     */
    public static double fiedlerValue(StateSpace ss) {
        double[] eigs = laplacianEigenvalues(ss);
        if (eigs.length < 2) return 0.0;
        return Math.max(0.0, eigs[1]);
    }

    /**
     * Count edges in the Hasse diagram (undirected).
     *
     * @param ss the state space
     * @return number of edges
     */
    public static int edgeCount(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        int count = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (A[i][j] > 0.5) count++;
            }
        }
        return count;
    }

    /**
     * Compute average degree of the Hasse diagram.
     *
     * @param ss the state space
     * @return average degree
     */
    public static double averageDegree(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        if (n == 0) return 0.0;
        double totalDegree = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                totalDegree += A[i][j];
            }
        }
        return totalDegree / n;
    }

    /**
     * Compute a compact spectral fingerprint summarising all key invariants.
     *
     * @param ss the state space
     * @return spectral fingerprint record
     */
    public static SpectralFingerprint compute(StateSpace ss) {
        double[] adjEigs = adjacencyEigenvalues(ss);
        double[] lapEigs = laplacianEigenvalues(ss);

        List<Double> adjList = new ArrayList<>();
        for (double e : adjEigs) adjList.add(e);
        List<Double> lapList = new ArrayList<>();
        for (double e : lapEigs) lapList.add(e);

        return new SpectralFingerprint(
                ss.states().size(),
                adjList,
                lapList,
                spectralRadius(ss),
                spectralGap(ss),
                fiedlerValue(ss),
                energy(ss),
                edgeCount(ss),
                averageDegree(ss));
    }
}
