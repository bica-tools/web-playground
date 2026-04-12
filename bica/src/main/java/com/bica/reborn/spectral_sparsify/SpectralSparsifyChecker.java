package com.bica.reborn.spectral_sparsify;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Spectral protocol compression via sparsification (Step 31g).
 *
 * <p>Spectral sparsification finds a subgraph with fewer edges that preserves
 * the spectral properties (eigenvalues of the Laplacian) of the original
 * Hasse diagram. This enables protocol compression: a simpler protocol
 * that behaves spectrally like the original.
 *
 * <p>A spectral sparsifier H of G satisfies, for all vectors x:
 * <pre>    (1-epsilon) * x^T L_G x  &lt;=  x^T L_H x  &lt;=  (1+epsilon) * x^T L_G x</pre>
 *
 * <p>Faithful Java port of Python {@code reticulate.spectral_sparsify}.
 */
public final class SpectralSparsifyChecker {

    private SpectralSparsifyChecker() {}

    // -----------------------------------------------------------------------
    // Result types
    // -----------------------------------------------------------------------

    /** Result of spectral sparsification. */
    public record SparsifyResult(
            int originalEdges,
            int sparsifiedEdges,
            double compressionRatio,
            double epsilon,
            double fiedlerOriginal,
            double fiedlerSparsified,
            double fiedlerRatio,
            List<int[]> keptEdges,
            List<Double> edgeWeights) {}

    /** Quality metrics for a subgraph as spectral approximation. */
    public record CompressionQuality(
            double maxDistortion,
            double fiedlerRatio,
            List<Double> eigenvalueDistances,
            boolean isGoodSparsifier) {}

    /** How compressible is this protocol spectrally? */
    public record SparsificationPotential(
            int numEdges,
            int numStates,
            int minEdgesNeeded,
            int maxRemovable,
            double removableFraction,
            Map<Integer, Double> effectiveResistances,
            int lowResistanceCount) {}

    // -----------------------------------------------------------------------
    // Hasse graph utilities
    // -----------------------------------------------------------------------

    /**
     * Compute covering relation edges (Hasse diagram).
     * Uses Zeta.coveringRelation which is public API.
     */
    static List<int[]> hasseEdges(StateSpace ss) {
        List<Zeta.Pair> covers = Zeta.coveringRelation(ss);
        List<int[]> edges = new ArrayList<>();
        for (Zeta.Pair p : covers) {
            edges.add(new int[]{p.x(), p.y()});
        }
        return edges;
    }

    /**
     * Build Laplacian matrix from edge list with optional weights.
     */
    static double[][] laplacianFromEdges(
            List<Integer> states, List<int[]> edges, List<Double> weights) {
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        double[][] L = new double[n][n];
        for (int k = 0; k < edges.size(); k++) {
            double w = (weights != null) ? weights.get(k) : 1.0;
            int[] e = edges.get(k);
            int i = idx.get(e[0]);
            int j = idx.get(e[1]);
            L[i][i] += w;
            L[j][j] += w;
            L[i][j] -= w;
            L[j][i] -= w;
        }
        return L;
    }

    /**
     * Compute sorted Laplacian eigenvalues from edge list.
     */
    static double[] laplacianEigenvalues(
            List<Integer> states, List<int[]> edges, List<Double> weights) {
        double[][] L = laplacianFromEdges(states, edges, weights);
        int n = L.length;
        if (n <= 1) {
            return new double[n];
        }
        double[] eigs = AlgebraicChecker.computeEigenvalues(L);
        Arrays.sort(eigs);
        return eigs;
    }

    /**
     * Compute Fiedler value from edge list.
     */
    static double fiedlerFromEdges(
            List<Integer> states, List<int[]> edges, List<Double> weights) {
        double[] eigs = laplacianEigenvalues(states, edges, weights);
        if (eigs.length < 2) return 0.0;
        return Math.max(0.0, eigs[1]);
    }

    // -----------------------------------------------------------------------
    // Gaussian elimination
    // -----------------------------------------------------------------------

    /**
     * Solve Ax = b via Gaussian elimination with partial pivoting.
     */
    static double[] solveLinear(double[][] A, double[] b) {
        int n = b.length;
        // Build augmented matrix
        double[][] M = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            M[i][n] = b[i];
        }

        for (int col = 0; col < n; col++) {
            // Partial pivoting
            int maxRow = col;
            double maxVal = Math.abs(M[col][col]);
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(M[row][col]) > maxVal) {
                    maxVal = Math.abs(M[row][col]);
                    maxRow = row;
                }
            }
            if (maxVal < 1e-14) continue;
            double[] tmp = M[col]; M[col] = M[maxRow]; M[maxRow] = tmp;

            // Eliminate
            double pivot = M[col][col];
            for (int row = col + 1; row < n; row++) {
                double factor = M[row][col] / pivot;
                for (int j = col; j <= n; j++) {
                    M[row][j] -= factor * M[col][j];
                }
            }
        }

        // Back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            if (Math.abs(M[i][i]) < 1e-14) {
                x[i] = 0.0;
                continue;
            }
            double s = M[i][n];
            for (int j = i + 1; j < n; j++) {
                s -= M[i][j] * x[j];
            }
            x[i] = s / M[i][i];
        }
        return x;
    }

    // -----------------------------------------------------------------------
    // Effective resistance
    // -----------------------------------------------------------------------

    /**
     * Compute effective resistance for each Hasse edge.
     *
     * <p>The effective resistance R_eff(e) of edge e = (u,v) is:
     *     R_eff(e) = (chi_u - chi_v)^T L^+ (chi_u - chi_v)
     *
     * <p>Edges with low effective resistance are redundant; edges with high
     * resistance are critical (bridges have R_eff = 1).
     */
    public static Map<Integer, Double> effectiveResistance(StateSpace ss) {
        List<Integer> states = AlgebraicChecker.stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        List<int[]> edges = hasseEdges(ss);

        if (n <= 1 || edges.isEmpty()) return Map.of();

        // Build Laplacian
        double[][] L = laplacianFromEdges(states, edges, null);

        // Regularized Laplacian: L_reg = L + J/n
        double[][] Lreg = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Lreg[i][j] = L[i][j] + 1.0 / n;
            }
        }

        Map<Integer, Double> resistances = new HashMap<>();
        for (int k = 0; k < edges.size(); k++) {
            int[] e = edges.get(k);
            int ai = idx.get(e[0]);
            int bi = idx.get(e[1]);
            double[] rhs = new double[n];
            rhs[ai] = 1.0;
            rhs[bi] = -1.0;

            double[] x = solveLinear(Lreg, rhs);
            double rEff = x[ai] - x[bi];
            resistances.put(k, Math.max(0.0, rEff));
        }
        return resistances;
    }

    // -----------------------------------------------------------------------
    // Spectral sparsification
    // -----------------------------------------------------------------------

    /**
     * Find a spectral sparsifier of the Hasse diagram.
     *
     * <p>Uses a greedy approach: try removing each edge (sorted by effective
     * resistance, low first) and keep it only if removal would distort the
     * Fiedler value by more than epsilon.
     *
     * @param ss      a session type state space
     * @param epsilon quality parameter (0 &lt; epsilon &lt; 1)
     * @return SparsifyResult with the sparsified edge set and quality metrics
     */
    public static SparsifyResult spectralSparsify(StateSpace ss, double epsilon) {
        List<Integer> states = AlgebraicChecker.stateList(ss);
        int n = states.size();
        List<int[]> edges = hasseEdges(ss);
        int m = edges.size();

        if (m == 0 || n <= 1) {
            return new SparsifyResult(m, 0, 0.0, epsilon, 0.0, 0.0, 0.0,
                    List.of(), List.of());
        }

        double fiedlerOrig = fiedlerFromEdges(states, edges, null);

        if (m <= n) {
            // Already a tree or near-tree; cannot sparsify further
            List<Double> weights = new ArrayList<>();
            for (int i = 0; i < m; i++) weights.add(1.0);
            return new SparsifyResult(m, m, 1.0, epsilon, fiedlerOrig, fiedlerOrig,
                    1.0, new ArrayList<>(edges), weights);
        }

        // Greedy sparsification
        Map<Integer, Double> resistances = effectiveResistance(ss);

        // Sort edges by resistance (low = redundant)
        Integer[] order = new Integer[m];
        for (int i = 0; i < m; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> resistances.getOrDefault(i, 1.0)));

        boolean[] kept = new boolean[m];
        Arrays.fill(kept, true);
        double[] keptWeights = new double[m];
        Arrays.fill(keptWeights, 1.0);

        for (int idx : order) {
            kept[idx] = false;
            List<int[]> currentEdges = new ArrayList<>();
            List<Double> currentWeights = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                if (kept[i]) {
                    currentEdges.add(edges.get(i));
                    currentWeights.add(keptWeights[i]);
                }
            }

            if (currentEdges.isEmpty()) {
                kept[idx] = true;
                continue;
            }

            double fiedlerNew = fiedlerFromEdges(states, currentEdges, currentWeights);
            double distortion;
            if (fiedlerOrig > 1e-10) {
                distortion = Math.abs(fiedlerNew - fiedlerOrig) / fiedlerOrig;
            } else {
                distortion = Math.abs(fiedlerNew - fiedlerOrig);
            }

            if (distortion > epsilon) {
                kept[idx] = true;
            }
        }

        List<int[]> finalEdges = new ArrayList<>();
        List<Double> finalWeights = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            if (kept[i]) {
                finalEdges.add(edges.get(i));
                finalWeights.add(keptWeights[i]);
            }
        }

        double fiedlerSparse = fiedlerFromEdges(states, finalEdges, finalWeights);
        double fiedlerRatio = (fiedlerOrig > 1e-10) ? fiedlerSparse / fiedlerOrig : 1.0;

        return new SparsifyResult(
                m, finalEdges.size(),
                (m > 0) ? (double) finalEdges.size() / m : 1.0,
                epsilon, fiedlerOrig, fiedlerSparse, fiedlerRatio,
                finalEdges, finalWeights);
    }

    /** Overload with default epsilon = 0.3. */
    public static SparsifyResult spectralSparsify(StateSpace ss) {
        return spectralSparsify(ss, 0.3);
    }

    // -----------------------------------------------------------------------
    // Compression quality
    // -----------------------------------------------------------------------

    /**
     * Measure how well a subgraph preserves the spectrum.
     */
    public static CompressionQuality compressionQuality(
            StateSpace ss, List<int[]> keptEdges, List<Double> weights, double epsilon) {
        List<Integer> states = AlgebraicChecker.stateList(ss);
        List<int[]> origEdges = hasseEdges(ss);

        double[] eigsOrig = laplacianEigenvalues(states, origEdges, null);
        double[] eigsSub = laplacianEigenvalues(states, keptEdges, weights);

        int n = Math.max(eigsOrig.length, eigsSub.length);
        double[] eo = new double[n];
        double[] es = new double[n];
        System.arraycopy(eigsOrig, 0, eo, 0, eigsOrig.length);
        System.arraycopy(eigsSub, 0, es, 0, eigsSub.length);

        List<Double> distances = new ArrayList<>();
        double maxDist = 0.0;
        for (int i = 0; i < n; i++) {
            double d = Math.abs(eo[i] - es[i]);
            distances.add(d);
            maxDist = Math.max(maxDist, d);
        }

        double fiedlerOrig = (eo.length > 1) ? Math.max(0.0, eo[1]) : 0.0;
        double fiedlerSub = (es.length > 1) ? Math.max(0.0, es[1]) : 0.0;
        double fiedlerRatio = (fiedlerOrig > 1e-10) ? fiedlerSub / fiedlerOrig : 1.0;

        double maxOrig = 0.0;
        for (double e : eo) maxOrig = Math.max(maxOrig, Math.abs(e));
        double maxDistortion = (maxOrig > 1e-10) ? maxDist / maxOrig : maxDist;

        return new CompressionQuality(maxDistortion, fiedlerRatio, distances,
                maxDistortion <= epsilon);
    }

    // -----------------------------------------------------------------------
    // Fiedler preservation check
    // -----------------------------------------------------------------------

    /**
     * Check whether a subgraph preserves the Fiedler value.
     */
    public static boolean preserveFiedler(
            StateSpace ss, List<int[]> keptEdges, List<Double> weights, double tolerance) {
        List<Integer> states = AlgebraicChecker.stateList(ss);
        List<int[]> origEdges = hasseEdges(ss);

        double fiedlerOrig = fiedlerFromEdges(states, origEdges, null);
        double fiedlerSub = fiedlerFromEdges(states, keptEdges, weights);

        if (fiedlerOrig < 1e-10) {
            return fiedlerSub < 1e-10 + tolerance;
        }
        return Math.abs(fiedlerSub - fiedlerOrig) / fiedlerOrig <= tolerance;
    }

    /** Overload without weights. */
    public static boolean preserveFiedler(
            StateSpace ss, List<int[]> keptEdges, double tolerance) {
        return preserveFiedler(ss, keptEdges, null, tolerance);
    }

    // -----------------------------------------------------------------------
    // Sparsification potential
    // -----------------------------------------------------------------------

    /**
     * Analyze how much the protocol can be compressed.
     */
    public static SparsificationPotential sparsificationPotential(StateSpace ss) {
        List<Integer> states = AlgebraicChecker.stateList(ss);
        int n = states.size();
        List<int[]> edges = hasseEdges(ss);
        int m = edges.size();

        int minEdges = Math.max(0, n - 1);
        int maxRemovable = Math.max(0, m - minEdges);
        double removableFrac = (m > 0) ? (double) maxRemovable / m : 0.0;

        Map<Integer, Double> resistances = effectiveResistance(ss);

        int lowCount = 0;
        if (!resistances.isEmpty()) {
            List<Double> vals = new ArrayList<>(resistances.values());
            Collections.sort(vals);
            double median = vals.get(vals.size() / 2);
            for (double r : resistances.values()) {
                if (r < median) lowCount++;
            }
        }

        return new SparsificationPotential(
                m, n, minEdges, maxRemovable, removableFrac,
                resistances, lowCount);
    }
}
