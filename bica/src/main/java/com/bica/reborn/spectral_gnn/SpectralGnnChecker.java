package com.bica.reborn.spectral_gnn;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.algebraic.eigenvalues.EigenvalueComputer;
import com.bica.reborn.algebraic.matrix.MatrixBuilder;
import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.characteristic.CharacteristicChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Spectral GNN features for session type lattices (Step 31b).
 *
 * <p>Graph Neural Network feature extraction from spectral properties of
 * session type state spaces. This module computes feature vectors
 * suitable for protocol classification and similarity search:
 *
 * <ul>
 *   <li><b>Spectral features</b>: eigenvalues, spectral moments, spectral gap</li>
 *   <li><b>Structural features</b>: degree distribution, diameter, width, height</li>
 *   <li><b>Algebraic features</b>: Moebius value, Whitney numbers, Fiedler value</li>
 *   <li><b>Feature vector</b>: fixed-length vector combining all invariants</li>
 *   <li><b>Similarity</b>: cosine and Euclidean distance between feature vectors</li>
 *   <li><b>Classification</b>: protocol family assignment from features</li>
 * </ul>
 *
 * <p>Faithful Java port of Python {@code reticulate.spectral_gnn}.
 */
public final class SpectralGnnChecker {

    private SpectralGnnChecker() {}

    // -----------------------------------------------------------------------
    // Result types
    // -----------------------------------------------------------------------

    /** Spectral GNN feature vector for a session type lattice. */
    public record SpectralFeatures(
            int numStates,
            List<Double> featureVector,
            List<String> featureNames,
            List<Double> spectralMoments,
            String protocolFamily) {}

    // -----------------------------------------------------------------------
    // Feature names (20 dimensions)
    // -----------------------------------------------------------------------

    public static final List<String> FEATURE_NAMES = List.of(
            "num_states",           // 0
            "num_transitions",      // 1
            "height",               // 2
            "width",                // 3
            "spectral_radius",      // 4
            "fiedler_value",        // 5
            "graph_energy",         // 6
            "mobius_value",         // 7
            "spectral_moment_1",    // 8  (mean eigenvalue)
            "spectral_moment_2",    // 9  (variance)
            "spectral_moment_3",    // 10 (skewness proxy)
            "spectral_moment_4",    // 11 (kurtosis proxy)
            "max_degree",           // 12
            "min_degree",           // 13
            "avg_degree",           // 14
            "density",              // 15
            "has_parallel",         // 16 (binary: width > 1)
            "has_recursion",        // 17 (binary: has cycles)
            "whitney_max",          // 18 (max Whitney number)
            "char_poly_degree"      // 19
    );

    // -----------------------------------------------------------------------
    // Spectral moments
    // -----------------------------------------------------------------------

    /**
     * Compute first k spectral moments: m_j = (1/n) Sum lambda_i^j.
     */
    public static List<Double> spectralMoments(double[] eigs, int k) {
        int n = eigs.length;
        if (n == 0) {
            List<Double> zeros = new ArrayList<>();
            for (int j = 0; j < k; j++) zeros.add(0.0);
            return zeros;
        }
        List<Double> moments = new ArrayList<>();
        for (int j = 1; j <= k; j++) {
            double sum = 0.0;
            for (double e : eigs) {
                sum += Math.pow(e, j);
            }
            moments.add(sum / n);
        }
        return moments;
    }

    // -----------------------------------------------------------------------
    // Feature extraction
    // -----------------------------------------------------------------------

    /**
     * Extract a 20-dimensional feature vector from a session type state space.
     */
    public static List<Double> extractFeatures(StateSpace ss) {
        int n = ss.states().size();
        int m = ss.transitions().size();

        // Height and rank
        Map<Integer, Integer> rank = Zeta.computeRank(ss);
        int height = rank.getOrDefault(ss.top(), 0);

        // Width (max same-rank elements in quotient)
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Integer> rankCounts = new HashMap<>();
        Set<Integer> seen = new HashSet<>();
        for (int s : ss.states()) {
            int rep = sccMap.get(s);
            if (seen.add(rep)) {
                int r = rank.get(s);
                rankCounts.merge(r, 1, Integer::sum);
            }
        }
        int width = rankCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        // Spectral properties from adjacency
        double[] eigs = EigenvalueComputer.adjacencyEigenvalues(ss);
        double sr = EigenvalueComputer.spectralRadius(ss);
        double fv = EigenvalueComputer.fiedlerValue(ss);
        double energy = EigenvalueComputer.energy(ss);
        int mu = AlgebraicChecker.computeMobiusValue(ss);
        List<Double> moments = spectralMoments(eigs, 4);

        // Degree info from adjacency matrix
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        double maxDeg = 0, minDeg = Double.MAX_VALUE, totalDeg = 0;
        if (A.length > 0) {
            for (double[] row : A) {
                double deg = 0;
                for (double v : row) deg += v;
                maxDeg = Math.max(maxDeg, deg);
                minDeg = Math.min(minDeg, deg);
                totalDeg += deg;
            }
        } else {
            minDeg = 0;
        }
        double avgDeg = (A.length > 0) ? totalDeg / A.length : 0.0;

        // Density
        double maxEdges = (double) n * (n - 1) / 2.0;
        double density = (maxEdges > 0) ? m / maxEdges : 0.0;

        // Binary features
        double hasParallel = (width > 1) ? 1.0 : 0.0;

        // Recursion detection: any SCC with more than one state
        boolean hasRec = false;
        for (var entry : sccR.sccMembers().entrySet()) {
            if (entry.getValue().size() > 1) {
                hasRec = true;
                break;
            }
        }
        double hasRecursion = hasRec ? 1.0 : 0.0;

        // Whitney max
        Map<Integer, Integer> W = CharacteristicChecker.whitneyNumbersSecond(ss);
        int whitneyMax = W.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        // Characteristic polynomial degree
        List<Integer> cp = CharacteristicChecker.characteristicPolynomial(ss);
        int cpDeg = cp.size() - 1;

        return List.of(
                (double) n, (double) m, (double) height, (double) width,
                sr, fv, energy, (double) mu,
                moments.get(0), moments.get(1), moments.get(2), moments.get(3),
                maxDeg, minDeg, avgDeg, density,
                hasParallel, hasRecursion,
                (double) whitneyMax, (double) cpDeg);
    }

    // -----------------------------------------------------------------------
    // Similarity
    // -----------------------------------------------------------------------

    /**
     * Cosine similarity between two feature vectors.
     */
    public static double cosineSimilarity(List<Double> v1, List<Double> v2) {
        double dot = 0, norm1 = 0, norm2 = 0;
        int n = Math.min(v1.size(), v2.size());
        for (int i = 0; i < n; i++) {
            dot += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);
        if (norm1 < 1e-15 || norm2 < 1e-15) return 0.0;
        return dot / (norm1 * norm2);
    }

    /**
     * Euclidean distance between two feature vectors.
     */
    public static double euclideanDistance(List<Double> v1, List<Double> v2) {
        double sum = 0;
        int n = Math.min(v1.size(), v2.size());
        for (int i = 0; i < n; i++) {
            double d = v1.get(i) - v2.get(i);
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    // -----------------------------------------------------------------------
    // Classification
    // -----------------------------------------------------------------------

    /**
     * Classify protocol into a family based on spectral features.
     *
     * <p>Families: "chain", "branch", "parallel", "recursive", "mixed".
     */
    public static String classifyProtocol(StateSpace ss) {
        List<Double> features = extractFeatures(ss);
        double width = features.get(3);
        boolean hasParallel = features.get(16) > 0.5;
        boolean hasRecursion = features.get(17) > 0.5;
        double nTrans = features.get(1);
        double nStates = features.get(0);

        if (hasRecursion && hasParallel) return "mixed";
        if (hasRecursion) return "recursive";
        if (hasParallel) return "parallel";
        if (nTrans > nStates - 0.5) return "branch";
        return "chain";
    }

    // -----------------------------------------------------------------------
    // Main analysis
    // -----------------------------------------------------------------------

    /**
     * Complete spectral GNN feature extraction.
     */
    public static SpectralFeatures analyzeSpectralGnn(StateSpace ss) {
        List<Double> features = extractFeatures(ss);
        double[] eigs = EigenvalueComputer.adjacencyEigenvalues(ss);
        List<Double> moments = spectralMoments(eigs, 4);
        String family = classifyProtocol(ss);

        return new SpectralFeatures(
                ss.states().size(), features, FEATURE_NAMES, moments, family);
    }
}
