package com.bica.reborn.von_neumann;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;

/**
 * Von Neumann entropy analysis for session type lattices (Step 30r) &mdash;
 * Java port of {@code reticulate/reticulate/von_neumann.py}.
 *
 * <p>Computes the von Neumann entropy of the graph Laplacian density matrix,
 * Renyi entropies, effective dimension, relative entropy, mutual information,
 * and entropy rate. All linear algebra is pure Java (no external dependencies).
 */
public final class VonNeumannChecker {

    private VonNeumannChecker() {}

    // -----------------------------------------------------------------------
    // Result record
    // -----------------------------------------------------------------------

    /**
     * Complete von Neumann entropy analysis result.
     */
    public record VonNeumannResult(
            double entropy,
            double maxEntropy,
            double normalizedEntropy,
            double[] densityEigenvalues,
            double renyiEntropy2,
            double renyiEntropyInf,
            double effectiveDimension,
            boolean isMaximallyMixed
    ) {}

    // -----------------------------------------------------------------------
    // Undirected graph helpers
    // -----------------------------------------------------------------------

    static int[] stateList(StateSpace ss) {
        TreeSet<Integer> sorted = new TreeSet<>(ss.states());
        int[] arr = new int[sorted.size()];
        int i = 0;
        for (int s : sorted) arr[i++] = s;
        return arr;
    }

    /**
     * Undirected edges from the state space (self-loops excluded, duplicates collapsed).
     */
    static Set<Long> undirectedEdges(StateSpace ss) {
        Set<Long> edges = new LinkedHashSet<>();
        for (Transition t : ss.transitions()) {
            int src = t.source(), tgt = t.target();
            if (src == tgt) continue;
            int u = Math.min(src, tgt), v = Math.max(src, tgt);
            edges.add(packEdge(u, v));
        }
        return edges;
    }

    private static long packEdge(int u, int v) {
        return ((long) u << 32) | (v & 0xFFFFFFFFL);
    }

    private static int edgeU(long e) { return (int) (e >>> 32); }
    private static int edgeV(long e) { return (int) e; }

    /**
     * Adjacency matrix of the undirected graph.
     */
    static double[][] adjacencyMatrix(StateSpace ss) {
        int[] states = stateList(ss);
        int n = states.length;
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states[i], i);
        double[][] A = new double[n][n];
        for (long edge : undirectedEdges(ss)) {
            int ui = idx.get(edgeU(edge));
            int vi = idx.get(edgeV(edge));
            A[ui][vi] = 1.0;
            A[vi][ui] = 1.0;
        }
        return A;
    }

    /**
     * Degree matrix D where D[i][i] = degree of vertex i in the undirected graph.
     */
    static double[][] degreeMatrix(StateSpace ss) {
        double[][] A = adjacencyMatrix(ss);
        int n = A.length;
        double[][] D = new double[n][n];
        for (int i = 0; i < n; i++) {
            double deg = 0.0;
            for (int j = 0; j < n; j++) deg += A[i][j];
            D[i][i] = deg;
        }
        return D;
    }

    // -----------------------------------------------------------------------
    // Linear algebra primitives
    // -----------------------------------------------------------------------

    static double trace(double[][] M) {
        double s = 0.0;
        for (int i = 0; i < M.length; i++) s += M[i][i];
        return s;
    }

    static double[][] scale(double[][] M, double c) {
        int n = M.length;
        double[][] R = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                R[i][j] = M[i][j] * c;
        return R;
    }

    // -----------------------------------------------------------------------
    // Jacobi eigensolver (symmetric matrices)
    // -----------------------------------------------------------------------

    /**
     * Eigenvalues of a real symmetric matrix via Jacobi iteration.
     * Sorted descending.
     */
    public static double[] symmetricEigenvalues(double[][] A) {
        return symmetricEigenvalues(A, 1e-12, 200);
    }

    public static double[] symmetricEigenvalues(double[][] A, double tol, int maxSweeps) {
        int n = A.length;
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{A[0][0]};
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(A[i], 0, M[i], 0, n);

        for (int sweep = 0; sweep < maxSweeps * n * n; sweep++) {
            int p = 0, q = 1;
            double best = 0.0;
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++) {
                    double a = Math.abs(M[i][j]);
                    if (a > best) { best = a; p = i; q = j; }
                }
            if (best < tol) break;

            double app = M[p][p], aqq = M[q][q], apq = M[p][q];
            double theta;
            if (Math.abs(app - aqq) < 1e-15) {
                theta = Math.PI / 4;
            } else {
                theta = 0.5 * Math.atan2(2.0 * apq, app - aqq);
            }
            double c = Math.cos(theta), s = Math.sin(theta);

            double newPP = c * c * app + 2 * s * c * apq + s * s * aqq;
            double newQQ = s * s * app - 2 * s * c * apq + c * c * aqq;

            for (int i = 0; i < n; i++) {
                if (i == p || i == q) continue;
                double mip = M[i][p], miq = M[i][q];
                M[i][p] = c * mip + s * miq;
                M[p][i] = M[i][p];
                M[i][q] = -s * mip + c * miq;
                M[q][i] = M[i][q];
            }
            M[p][p] = newPP;
            M[q][q] = newQQ;
            M[p][q] = 0.0;
            M[q][p] = 0.0;
        }

        double[] eigs = new double[n];
        for (int i = 0; i < n; i++) eigs[i] = M[i][i];
        // Sort descending
        Arrays.sort(eigs);
        for (int i = 0, j = n - 1; i < j; i++, j--) {
            double tmp = eigs[i]; eigs[i] = eigs[j]; eigs[j] = tmp;
        }
        return eigs;
    }

    // -----------------------------------------------------------------------
    // Laplacian and density matrix
    // -----------------------------------------------------------------------

    /**
     * Graph Laplacian L = D - A of the undirected state space graph.
     */
    public static double[][] laplacianMatrix(StateSpace ss) {
        double[][] A = adjacencyMatrix(ss);
        double[][] D = degreeMatrix(ss);
        int n = A.length;
        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                L[i][j] = D[i][j] - A[i][j];
        return L;
    }

    /**
     * Density matrix rho = L / tr(L).
     * For single-state graphs, returns [[1.0]].
     * For graphs with no edges, returns I/n.
     */
    public static double[][] densityMatrix(StateSpace ss) {
        double[][] L = laplacianMatrix(ss);
        int n = L.length;
        if (n == 0) return new double[0][0];
        if (n == 1) return new double[][]{{1.0}};
        double tr = trace(L);
        if (tr < 1e-15) {
            // No edges: maximally mixed
            double[][] rho = new double[n][n];
            for (int i = 0; i < n; i++) rho[i][i] = 1.0 / n;
            return rho;
        }
        return scale(L, 1.0 / tr);
    }

    /**
     * Eigenvalues of the density matrix (non-negative, sum to 1).
     */
    public static double[] densityEigenvalues(StateSpace ss) {
        double[][] rho = densityMatrix(ss);
        if (rho.length == 0) return new double[0];
        double[] eigs = symmetricEigenvalues(rho);
        for (int i = 0; i < eigs.length; i++) eigs[i] = Math.max(0.0, eigs[i]);
        return eigs;
    }

    // -----------------------------------------------------------------------
    // Entropy computations
    // -----------------------------------------------------------------------

    static double xlogx(double x) {
        if (x < 1e-15) return 0.0;
        return x * Math.log(x);
    }

    /**
     * Von Neumann entropy S(rho) = -sum_i lambda_i log(lambda_i).
     */
    public static double vonNeumannEntropy(StateSpace ss) {
        double[] eigs = densityEigenvalues(ss);
        if (eigs.length == 0) return 0.0;
        double s = 0.0;
        for (double lam : eigs) s += xlogx(lam);
        return Math.max(0.0, -s);
    }

    /**
     * Maximum possible entropy log(n).
     */
    public static double maxEntropy(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 1) return 0.0;
        return Math.log(n);
    }

    /**
     * Normalized entropy S(rho)/log(n) in [0,1].
     */
    public static double normalizedEntropy(StateSpace ss) {
        double me = maxEntropy(ss);
        if (me < 1e-15) return 0.0;
        return vonNeumannEntropy(ss) / me;
    }

    /**
     * Renyi entropy S_alpha = (1/(1-alpha)) log(tr(rho^alpha)).
     *
     * @throws IllegalArgumentException if alpha &lt;= 0 or alpha == 1
     */
    public static double renyiEntropy(StateSpace ss, double alpha) {
        if (alpha <= 0) throw new IllegalArgumentException("Renyi alpha must be > 0, got " + alpha);
        if (Math.abs(alpha - 1.0) < 1e-10) return vonNeumannEntropy(ss);

        double[] eigs = densityEigenvalues(ss);
        if (eigs.length == 0) return 0.0;

        if (Double.isInfinite(alpha) || alpha > 1e10) {
            // Min-entropy
            double maxEig = 0.0;
            for (double e : eigs) if (e > maxEig) maxEig = e;
            if (maxEig < 1e-15) return 0.0;
            return -Math.log(maxEig);
        }

        double trRhoAlpha = 0.0;
        for (double lam : eigs) {
            if (lam > 1e-15) trRhoAlpha += Math.pow(lam, alpha);
        }
        if (trRhoAlpha < 1e-15) return 0.0;
        double result = (1.0 / (1.0 - alpha)) * Math.log(trRhoAlpha);
        return Math.max(0.0, result);
    }

    /**
     * Renyi entropy at alpha=2 (collision entropy).
     */
    public static double renyiEntropy2(StateSpace ss) {
        return renyiEntropy(ss, 2.0);
    }

    /**
     * Renyi entropy at alpha -&gt; infinity (min-entropy).
     */
    public static double renyiEntropyInf(StateSpace ss) {
        double[] eigs = densityEigenvalues(ss);
        if (eigs.length == 0) return 0.0;
        double maxEig = 0.0;
        for (double e : eigs) if (e > maxEig) maxEig = e;
        if (maxEig < 1e-15) return 0.0;
        return Math.max(0.0, -Math.log(maxEig));
    }

    /**
     * Effective dimension exp(S(rho)) in [1, n].
     */
    public static double effectiveDimension(StateSpace ss) {
        return Math.exp(vonNeumannEntropy(ss));
    }

    // -----------------------------------------------------------------------
    // Relative entropy and mutual information
    // -----------------------------------------------------------------------

    /**
     * Quantum relative entropy S(rho1 || rho2).
     * Returns {@link Double#NaN} if dimensions differ or support condition violated.
     */
    public static double relativeEntropy(StateSpace ss1, StateSpace ss2) {
        int n1 = ss1.states().size();
        int n2 = ss2.states().size();
        if (n1 != n2) return Double.NaN;
        if (n1 <= 1) return 0.0;

        double[] eigs1 = densityEigenvalues(ss1);
        double[] eigs2 = densityEigenvalues(ss2);

        // Check support condition
        for (int i = 0; i < eigs1.length; i++) {
            if (eigs1[i] > 1e-15 && eigs2[i] < 1e-15) return Double.NaN;
        }

        double result = 0.0;
        for (int i = 0; i < eigs1.length; i++) {
            if (eigs1[i] < 1e-15) continue;
            result += eigs1[i] * (Math.log(eigs1[i]) - Math.log(eigs2[i]));
        }
        return result;
    }

    /**
     * Mutual information I = log(n) - S(rho).
     */
    public static double mutualInformation(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 1) return 0.0;
        return maxEntropy(ss) - vonNeumannEntropy(ss);
    }

    /**
     * Entropy rate S(rho) / n.
     */
    public static double entropyRate(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 1) return 0.0;
        return vonNeumannEntropy(ss) / n;
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Complete von Neumann entropy analysis.
     */
    public static VonNeumannResult analyze(StateSpace ss) {
        double[] eigs = densityEigenvalues(ss);
        double ent = vonNeumannEntropy(ss);
        double me = maxEntropy(ss);
        double ne = normalizedEntropy(ss);
        double r2 = renyiEntropy2(ss);
        double ri = renyiEntropyInf(ss);
        double ed = effectiveDimension(ss);
        boolean isMax = me > 1e-15 && ne > 0.99;

        return new VonNeumannResult(ent, me, ne, eigs, r2, ri, ed, isMax);
    }
}
