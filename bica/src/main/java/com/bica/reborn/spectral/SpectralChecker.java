package com.bica.reborn.spectral;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Spectral clustering of session type lattices.
 *
 * <p>Extracts spectral feature vectors from state spaces, computes pairwise
 * distances, and clusters protocols by structural similarity. Java port of
 * Python {@code spectral.py}.
 *
 * <p>Feature vector components: spectral radius, Fiedler value, von Neumann
 * entropy, width, height, number of states, number of transitions.
 */
public final class SpectralChecker {

    private SpectralChecker() {}

    // -----------------------------------------------------------------------
    // Feature extraction
    // -----------------------------------------------------------------------

    /**
     * Extract the spectral feature vector from a session type state space.
     */
    public static SpectralFeatures extractFeatures(StateSpace ss) {
        double specRadius = AlgebraicChecker.spectralRadius(ss);
        double fiedler = AlgebraicChecker.fiedlerValue(ss);
        double entropy = AlgebraicChecker.vonNeumannEntropy(ss);
        int w = width(ss);
        int h = height(ss);
        int nStates = ss.states().size();
        int nTransitions = ss.transitions().size();

        return new SpectralFeatures(specRadius, fiedler, entropy, w, h, nStates, nTransitions);
    }

    // -----------------------------------------------------------------------
    // Poset metrics
    // -----------------------------------------------------------------------

    /**
     * Compute the width (maximum antichain size) of the state space poset.
     *
     * <p>Uses a greedy approach: assigns states to levels by longest path from
     * top, then takes the maximum level count.
     */
    public static int width(StateSpace ss) {
        if (ss.states().size() <= 1) return ss.states().size();

        // Build adjacency
        Map<Integer, List<Integer>> adj = new HashMap<>();
        Map<Integer, List<Integer>> predAdj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
            predAdj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
            predAdj.get(t.target()).add(t.source());
        }

        // Compute longest path from top to each state (level assignment)
        Map<Integer, Integer> level = new HashMap<>();
        for (int s : ss.states()) {
            level.put(s, 0);
        }

        // Topological order via Kahn's algorithm
        Map<Integer, Integer> inDeg = new HashMap<>();
        for (int s : ss.states()) {
            inDeg.put(s, 0);
        }
        for (var t : ss.transitions()) {
            inDeg.merge(t.target(), 1, Integer::sum);
        }

        var queue = new ArrayDeque<Integer>();
        for (int s : ss.states()) {
            if (inDeg.get(s) == 0) {
                queue.add(s);
            }
        }

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : adj.get(u)) {
                level.put(v, Math.max(level.get(v), level.get(u) + 1));
                inDeg.put(v, inDeg.get(v) - 1);
                if (inDeg.get(v) == 0) {
                    queue.add(v);
                }
            }
        }

        // Count states per level
        Map<Integer, Integer> levelCount = new HashMap<>();
        for (int lv : level.values()) {
            levelCount.merge(lv, 1, Integer::sum);
        }

        return levelCount.values().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    /**
     * Compute the height (longest chain top to bottom) of the state space poset.
     *
     * <p>Height = number of edges on the longest path from top to bottom.
     */
    public static int height(StateSpace ss) {
        if (ss.states().size() <= 1) return 0;

        // BFS/DFS for longest path from top to bottom
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        // Topological order + longest path via DP
        Map<Integer, Integer> dist = new HashMap<>();
        for (int s : ss.states()) {
            dist.put(s, -1);
        }
        dist.put(ss.top(), 0);

        Map<Integer, Integer> inDeg = new HashMap<>();
        for (int s : ss.states()) {
            inDeg.put(s, 0);
        }
        for (var t : ss.transitions()) {
            inDeg.merge(t.target(), 1, Integer::sum);
        }

        var queue = new ArrayDeque<Integer>();
        for (int s : ss.states()) {
            if (inDeg.get(s) == 0) {
                queue.add(s);
            }
        }

        while (!queue.isEmpty()) {
            int u = queue.poll();
            if (dist.get(u) >= 0) {
                for (int v : adj.get(u)) {
                    dist.put(v, Math.max(dist.get(v), dist.get(u) + 1));
                    inDeg.put(v, inDeg.get(v) - 1);
                    if (inDeg.get(v) == 0) {
                        queue.add(v);
                    }
                }
            } else {
                for (int v : adj.get(u)) {
                    inDeg.put(v, inDeg.get(v) - 1);
                    if (inDeg.get(v) == 0) {
                        queue.add(v);
                    }
                }
            }
        }

        int d = dist.getOrDefault(ss.bottom(), 0);
        return Math.max(0, d);
    }

    // -----------------------------------------------------------------------
    // Distance computation
    // -----------------------------------------------------------------------

    /**
     * Compute Euclidean distance between two feature vectors.
     */
    public static double featureDistance(SpectralFeatures a, SpectralFeatures b) {
        double[] va = a.asVector();
        double[] vb = b.asVector();
        return euclideanDistance(va, vb);
    }

    private static double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    // -----------------------------------------------------------------------
    // k-means clustering
    // -----------------------------------------------------------------------

    /**
     * Cluster feature vectors using k-means (Lloyd's algorithm).
     *
     * <p>Returns a list of k clusters, where each cluster is a list of indices
     * into the input features list.
     *
     * @param features list of feature vectors to cluster
     * @param k        number of clusters
     * @return list of clusters (each cluster is a list of original indices)
     */
    public static List<List<Integer>> cluster(List<SpectralFeatures> features, int k) {
        int n = features.size();
        if (n == 0 || k <= 0) {
            return List.of();
        }
        k = Math.min(k, n);

        // Convert to raw vectors
        double[][] vectors = new double[n][];
        for (int i = 0; i < n; i++) {
            vectors[i] = features.get(i).asVector();
        }

        // Normalize
        int dim = vectors[0].length;
        double[] mins = new double[dim];
        double[] maxs = new double[dim];
        Arrays.fill(mins, Double.MAX_VALUE);
        Arrays.fill(maxs, -Double.MAX_VALUE);
        for (double[] v : vectors) {
            for (int d = 0; d < dim; d++) {
                mins[d] = Math.min(mins[d], v[d]);
                maxs[d] = Math.max(maxs[d], v[d]);
            }
        }
        double[] ranges = new double[dim];
        for (int d = 0; d < dim; d++) {
            ranges[d] = (maxs[d] > mins[d]) ? maxs[d] - mins[d] : 1.0;
        }

        double[][] norm = new double[n][dim];
        for (int i = 0; i < n; i++) {
            for (int d = 0; d < dim; d++) {
                norm[i][d] = (vectors[i][d] - mins[d]) / ranges[d];
            }
        }

        // Deterministic initialization: evenly spaced
        double[][] centroids = new double[k][dim];
        for (int c = 0; c < k; c++) {
            int idx = c * n / k;
            System.arraycopy(norm[idx], 0, centroids[c], 0, dim);
        }

        int[] assignments = new int[n];
        for (int iter = 0; iter < 100; iter++) {
            int[] newAssignments = new int[n];
            for (int i = 0; i < n; i++) {
                int bestC = 0;
                double bestD = euclideanDistance(norm[i], centroids[0]);
                for (int c = 1; c < k; c++) {
                    double d = euclideanDistance(norm[i], centroids[c]);
                    if (d < bestD) {
                        bestD = d;
                        bestC = c;
                    }
                }
                newAssignments[i] = bestC;
            }

            if (Arrays.equals(newAssignments, assignments)) break;
            assignments = newAssignments;

            // Update centroids
            for (int c = 0; c < k; c++) {
                double[] sum = new double[dim];
                int count = 0;
                for (int i = 0; i < n; i++) {
                    if (assignments[i] == c) {
                        for (int d = 0; d < dim; d++) {
                            sum[d] += norm[i][d];
                        }
                        count++;
                    }
                }
                if (count > 0) {
                    for (int d = 0; d < dim; d++) {
                        centroids[c][d] = sum[d] / count;
                    }
                }
            }
        }

        // Build result
        List<List<Integer>> result = new ArrayList<>();
        for (int c = 0; c < k; c++) {
            result.add(new ArrayList<>());
        }
        for (int i = 0; i < n; i++) {
            result.get(assignments[i]).add(i);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Similarity search
    // -----------------------------------------------------------------------

    /**
     * Find the n most similar feature vectors to a query.
     *
     * @param query  the query feature vector
     * @param corpus list of feature vectors to search
     * @param n      number of nearest neighbors to return
     * @return list of indices into corpus, sorted by ascending distance
     */
    public static List<Integer> findSimilar(SpectralFeatures query, List<SpectralFeatures> corpus, int n) {
        if (corpus.isEmpty() || n <= 0) {
            return List.of();
        }
        n = Math.min(n, corpus.size());

        // Compute distances
        record DistEntry(double distance, int index) implements Comparable<DistEntry> {
            @Override
            public int compareTo(DistEntry other) {
                return Double.compare(this.distance, other.distance);
            }
        }

        var distances = new ArrayList<DistEntry>();
        for (int i = 0; i < corpus.size(); i++) {
            double d = featureDistance(query, corpus.get(i));
            distances.add(new DistEntry(d, i));
        }
        Collections.sort(distances);

        var result = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            result.add(distances.get(i).index());
        }
        return result;
    }
}
