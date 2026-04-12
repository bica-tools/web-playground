package com.bica.reborn.kirchhoff;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.algebraic.matrix.MatrixBuilder;
import com.bica.reborn.algebraic.matrix.MatrixOps;
import com.bica.reborn.statespace.StateSpace;

/**
 * Kirchhoff's matrix-tree theorem for session type lattices (Step 30h).
 *
 * <p>Kirchhoff's theorem relates the number of spanning trees of a graph
 * to the determinant of a cofactor of the Laplacian matrix:
 * <pre>
 *     tau(G) = (1/n) * lambda_1 * lambda_2 * ... * lambda_{n-1}
 * </pre>
 * where lambda_i are the non-zero Laplacian eigenvalues.
 *
 * <p>For session type lattices, spanning trees of the Hasse diagram
 * represent minimal protocol skeletons -- the fewest edges needed
 * to maintain connectivity.
 *
 * <p>This class provides:
 * <ul>
 *   <li><b>Spanning tree count</b> via Kirchhoff's theorem (cofactor method)</li>
 *   <li><b>Spanning tree count</b> via eigenvalue product formula</li>
 *   <li><b>Normalized tree count</b>: tau(G) / n^{n-2} (comparison with complete graph)</li>
 *   <li><b>Complexity ratio</b>: tau(G) / |E| (spanning trees per edge)</li>
 *   <li><b>Tree detection</b>: tau(G) == 1</li>
 *   <li><b>Product verification</b>: tau(product) >= min(tau(left), tau(right))</li>
 * </ul>
 */
public final class KirchhoffChecker {

    private KirchhoffChecker() {}

    // -----------------------------------------------------------------------
    // Result record
    // -----------------------------------------------------------------------

    /**
     * Kirchhoff analysis results.
     *
     * @param numStates             Number of states.
     * @param numEdges              Number of undirected Hasse edges.
     * @param spanningTreeCount     tau(G) = number of spanning trees (integer).
     * @param spanningTreeCountEigenvalue tau(G) computed via eigenvalues (float).
     * @param normalizedCount       tau(G) / n^{n-2} (Cayley comparison).
     * @param complexityRatio       tau(G) / |E| (trees per edge).
     * @param laplacianDetCofactor  det of Laplacian with row/col 0 removed.
     * @param isTree                True iff tau(G) = 1 (the graph is already a tree).
     */
    public record KirchhoffResult(
            int numStates,
            int numEdges,
            int spanningTreeCount,
            double spanningTreeCountEigenvalue,
            double normalizedCount,
            double complexityRatio,
            double laplacianDetCofactor,
            boolean isTree
    ) {}

    // -----------------------------------------------------------------------
    // Determinant (delegates to MatrixOps but we also expose cofactor helper)
    // -----------------------------------------------------------------------

    /**
     * Compute determinant via LU decomposition (Gaussian elimination).
     * Delegates to {@link MatrixOps#determinant(double[][])}.
     */
    static double determinant(double[][] m) {
        return MatrixOps.determinant(m);
    }

    /**
     * Remove a row and column from a matrix (cofactor minor).
     *
     * @param matrix the original n x n matrix
     * @param row    row to remove
     * @param col    column to remove
     * @return (n-1) x (n-1) minor matrix
     */
    static double[][] cofactorMatrix(double[][] matrix, int row, int col) {
        int n = matrix.length;
        double[][] minor = new double[n - 1][n - 1];
        int mi = 0;
        for (int i = 0; i < n; i++) {
            if (i == row) continue;
            int mj = 0;
            for (int j = 0; j < n; j++) {
                if (j == col) continue;
                minor[mi][mj] = matrix[i][j];
                mj++;
            }
            mi++;
        }
        return minor;
    }

    // -----------------------------------------------------------------------
    // Spanning tree count — cofactor method
    // -----------------------------------------------------------------------

    /**
     * Count spanning trees via Laplacian cofactor (Kirchhoff's theorem).
     *
     * <p>tau(G) = det(L[0,0]) = determinant of Laplacian with row 0 and col 0 removed.
     *
     * @param ss the state space
     * @return tau(G) as a floating point value
     */
    public static double spanningTreeCountCofactor(StateSpace ss) {
        double[][] L = MatrixBuilder.buildLaplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return 1.0;
        double[][] minor = cofactorMatrix(L, 0, 0);
        return determinant(minor);
    }

    // -----------------------------------------------------------------------
    // Spanning tree count — eigenvalue method
    // -----------------------------------------------------------------------

    /**
     * Count spanning trees via Laplacian eigenvalues.
     *
     * <p>tau(G) = (1/n) * product_{i=1}^{n-1} lambda_i
     * where lambda_0 = 0 &le; lambda_1 &le; ... &le; lambda_{n-1} are Laplacian eigenvalues.
     *
     * @param ss the state space
     * @return tau(G) computed via eigenvalue product
     */
    public static double spanningTreeCountEigenvalue(StateSpace ss) {
        double[][] L = MatrixBuilder.buildLaplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return 1.0;

        double[] eigs = AlgebraicChecker.computeEigenvalues(L);
        java.util.Arrays.sort(eigs);

        // Product of non-zero eigenvalues divided by n
        double product = 1.0;
        for (int i = 1; i < n; i++) { // skip lambda_0 ~ 0
            product *= Math.max(eigs[i], 0.0);
        }
        return product / n;
    }

    // -----------------------------------------------------------------------
    // Integer spanning tree count
    // -----------------------------------------------------------------------

    /**
     * Count spanning trees (integer result via cofactor method).
     *
     * @param ss the state space
     * @return tau(G) as an integer
     */
    public static int spanningTreeCount(StateSpace ss) {
        double val = spanningTreeCountCofactor(ss);
        return Math.max(0, (int) Math.round(val));
    }

    // -----------------------------------------------------------------------
    // Derived metrics
    // -----------------------------------------------------------------------

    /**
     * tau(G) / n^{n-2} -- comparison with complete graph (Cayley's formula).
     *
     * <p>For K_n, tau = n^{n-2}. This ratio measures how tree-rich the graph
     * is compared to the maximum possible.
     *
     * @param ss the state space
     * @return normalized tree count in [0, 1]
     */
    public static double normalizedTreeCount(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 2) return 1.0;
        int tau = spanningTreeCount(ss);
        double cayley = Math.pow(n, n - 2);
        return (cayley > 0) ? tau / cayley : 0.0;
    }

    /**
     * tau(G) / |E| -- spanning trees per edge.
     *
     * <p>Higher values indicate more redundancy in the graph structure.
     *
     * @param ss the state space
     * @return complexity ratio
     */
    public static double complexityRatio(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        // Count undirected edges (upper triangle of symmetric adjacency)
        double numEdges = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                numEdges += A[i][j];
            }
        }
        if (numEdges == 0) return 0.0;
        int tau = spanningTreeCount(ss);
        return tau / numEdges;
    }

    /**
     * Check if the Hasse diagram is already a tree (tau = 1).
     *
     * @param ss the state space
     * @return true iff tau(G) = 1
     */
    public static boolean isTree(StateSpace ss) {
        return spanningTreeCount(ss) == 1;
    }

    // -----------------------------------------------------------------------
    // Composition
    // -----------------------------------------------------------------------

    /**
     * Check spanning tree count relationship for product lattices.
     *
     * <p>Weaker check: verify the product count is at least as large as the
     * minimum of the two factor counts.
     *
     * @param ssLeft    left factor state space
     * @param ssRight   right factor state space
     * @param ssProduct product state space
     * @return true if tau(product) &ge; min(tau(left), tau(right))
     */
    public static boolean verifyTreeCountProduct(
            StateSpace ssLeft, StateSpace ssRight, StateSpace ssProduct) {
        int tauLeft = spanningTreeCount(ssLeft);
        int tauRight = spanningTreeCount(ssRight);
        int tauProduct = spanningTreeCount(ssProduct);
        return tauProduct >= Math.min(tauLeft, tauRight);
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Complete Kirchhoff analysis.
     *
     * @param ss the state space
     * @return {@link KirchhoffResult} with all metrics
     */
    public static KirchhoffResult analyzeKirchhoff(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        int numEdges = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                numEdges += (int) A[i][j];
            }
        }

        double tauCofactor = spanningTreeCountCofactor(ss);
        double tauEig = spanningTreeCountEigenvalue(ss);
        int tau = Math.max(0, (int) Math.round(tauCofactor));
        double norm = normalizedTreeCount(ss);
        double cr = complexityRatio(ss);

        return new KirchhoffResult(
                n, numEdges, tau, tauEig, norm, cr, tauCofactor, tau == 1
        );
    }

    // -----------------------------------------------------------------------
    // Edge count helper (package-private for tests)
    // -----------------------------------------------------------------------

    /**
     * Count undirected Hasse edges.
     */
    static int countUndirectedEdges(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        int count = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                count += (int) A[i][j];
            }
        }
        return count;
    }
}
