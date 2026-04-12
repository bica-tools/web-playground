package com.bica.reborn.spectral_prob;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.algebraic.matrix.MatrixBuilder;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Spectral-probabilistic analysis for session type lattices (Steps 30o-30r).
 *
 * <p>Probabilistic and information-theoretic invariants derived from the
 * Hasse diagram's spectral structure:
 * <ul>
 *   <li><b>Step 30o</b>: Cheeger constant (edge expansion / conductance)</li>
 *   <li><b>Step 30p</b>: Random walk mixing time</li>
 *   <li><b>Step 30q</b>: Heat kernel trace and diagonal</li>
 *   <li><b>Step 30r</b>: Von Neumann entropy of the Laplacian</li>
 * </ul>
 *
 * <p>Java port of Python {@code spectral_prob.py}.
 */
public final class SpectralProbChecker {

    private SpectralProbChecker() {}

    // -----------------------------------------------------------------------
    // Step 30o: Cheeger constant
    // -----------------------------------------------------------------------

    /**
     * Compute the Cheeger constant (edge expansion) h(G).
     *
     * <p>h(G) = min over subsets S with |S| &le; n/2 of |boundary(S)| / |S|
     * where boundary(S) = edges between S and V \ S.
     * For small graphs we compute exactly by trying all subsets.
     */
    public static double cheegerConstant(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        if (n <= 1) return 0.0;

        double minExpansion = Double.MAX_VALUE;

        // Try all non-empty subsets of size <= n/2
        for (int mask = 1; mask < (1 << n); mask++) {
            int size = Integer.bitCount(mask);
            if (size > n / 2) continue;

            // Count boundary edges
            double boundary = 0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) == 0) continue;
                for (int j = 0; j < n; j++) {
                    if ((mask & (1 << j)) != 0) continue;
                    boundary += A[i][j];
                }
            }

            double expansion = boundary / size;
            minExpansion = Math.min(minExpansion, expansion);
        }

        return minExpansion < Double.MAX_VALUE ? minExpansion : 0.0;
    }

    /**
     * Cheeger inequality bounds: lambda_2/2 &le; h(G) &le; sqrt(2 * lambda_2).
     *
     * @return array of two doubles: [lower, upper]
     */
    public static double[] cheegerBounds(StateSpace ss) {
        double[][] L = MatrixBuilder.buildLaplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return new double[]{0.0, 0.0};

        double[] eigs = AlgebraicChecker.computeEigenvalues(L);
        Arrays.sort(eigs);
        double lambda2 = eigs.length >= 2 ? Math.max(0.0, eigs[1]) : 0.0;
        return new double[]{
                lambda2 / 2.0,
                lambda2 > 0 ? Math.sqrt(2.0 * lambda2) : 0.0
        };
    }

    // -----------------------------------------------------------------------
    // Step 30p: Random walk mixing time
    // -----------------------------------------------------------------------

    /**
     * Upper bound on random walk mixing time from spectral gap.
     *
     * <p>t_mix &le; (1/lambda_2) * log(n).
     */
    public static double mixingTimeBound(StateSpace ss) {
        double[][] L = MatrixBuilder.buildLaplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return 0.0;

        double[] eigs = AlgebraicChecker.computeEigenvalues(L);
        Arrays.sort(eigs);
        double lambda2 = eigs.length >= 2 ? Math.max(eigs[1], 1e-15) : 1e-15;
        return (1.0 / lambda2) * Math.log(Math.max(n, 2));
    }

    /**
     * Stationary distribution of the random walk on the Hasse diagram.
     *
     * <p>For an undirected graph, pi(v) = deg(v) / (2|E|).
     */
    public static Map<Integer, Double> stationaryDistribution(StateSpace ss) {
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        int n = A.length;
        List<Integer> states = sortedStates(ss);

        double totalDegree = 0;
        double[] degrees = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                degrees[i] += A[i][j];
            }
            totalDegree += degrees[i];
        }

        Map<Integer, Double> result = new LinkedHashMap<>();
        if (totalDegree == 0) {
            for (int s : states) {
                result.put(s, 1.0 / n);
            }
        } else {
            for (int i = 0; i < n; i++) {
                result.put(states.get(i), degrees[i] / totalDegree);
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Step 30q: Heat kernel
    // -----------------------------------------------------------------------

    /**
     * Heat kernel trace: Z(t) = sum exp(-lambda_i * t).
     *
     * <p>At t=0: Z = n. As t tends to infinity: Z tends to 1.
     */
    public static double heatKernelTrace(StateSpace ss, double t) {
        double[][] L = MatrixBuilder.buildLaplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return n;

        double[] eigs = AlgebraicChecker.computeEigenvalues(L);
        double sum = 0;
        for (double e : eigs) {
            sum += Math.exp(-e * t);
        }
        return sum;
    }

    /** Heat kernel trace at t=1. */
    public static double heatKernelTrace(StateSpace ss) {
        return heatKernelTrace(ss, 1.0);
    }

    /**
     * Diagonal of the heat kernel: h_t(v,v) approximated as Z(t)/n.
     */
    public static Map<Integer, Double> heatKernelDiagonal(StateSpace ss, double t) {
        double Z = heatKernelTrace(ss, t);
        int n = ss.states().size();
        List<Integer> states = sortedStates(ss);
        Map<Integer, Double> result = new LinkedHashMap<>();
        for (int s : states) {
            result.put(s, Z / n);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Step 30r: Von Neumann entropy
    // -----------------------------------------------------------------------

    /**
     * Von Neumann entropy of the normalized Laplacian.
     *
     * <p>S(G) = -sum_k (lambda_k / sum_j lambda_j) * log(lambda_k / sum_j lambda_j)
     * where the sum is over non-zero Laplacian eigenvalues.
     */
    public static double vonNeumannEntropy(StateSpace ss) {
        double[][] L = MatrixBuilder.buildLaplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return 0.0;

        double[] eigs = AlgebraicChecker.computeEigenvalues(L);
        double total = 0;
        int count = 0;
        for (double e : eigs) {
            if (e > 1e-10) {
                total += e;
                count++;
            }
        }
        if (count == 0 || total < 1e-15) return 0.0;

        double entropy = 0;
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

    /**
     * Normalized Von Neumann entropy: S / log(n-1).
     *
     * <p>Ranges from 0 (one dominant eigenvalue) to 1 (uniform spectrum).
     */
    public static double normalizedEntropy(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 2) return 0.0;
        double S = vonNeumannEntropy(ss);
        double maxS = Math.log(n - 1);
        return maxS > 0 ? S / maxS : 0.0;
    }

    // -----------------------------------------------------------------------
    // Main analysis
    // -----------------------------------------------------------------------

    /**
     * Complete spectral-probabilistic analysis.
     */
    public static SpectralProbResult analyze(StateSpace ss) {
        int n = ss.states().size();

        // Cheeger is expensive for large n, skip if > 20
        double h = (n <= 20) ? cheegerConstant(ss) : 0.0;
        double[] bounds = cheegerBounds(ss);
        double tMix = mixingTimeBound(ss);
        double Z = heatKernelTrace(ss);
        double S = vonNeumannEntropy(ss);
        double sNorm = normalizedEntropy(ss);
        Map<Integer, Double> pi = stationaryDistribution(ss);

        return new SpectralProbResult(
                n, h, bounds[0], bounds[1],
                tMix, Z, S, sNorm, pi);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static List<Integer> sortedStates(StateSpace ss) {
        var list = new ArrayList<>(ss.states());
        Collections.sort(list);
        return list;
    }
}
