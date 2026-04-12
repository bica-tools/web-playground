package com.bica.reborn.spectral_refactor;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.algebraic.eigenvalues.EigenvalueComputer;
import com.bica.reborn.fiedler.FiedlerChecker;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;

/**
 * Spectral protocol refactoring for session type lattices (Step 31i).
 *
 * <p>Three complementary approaches to protocol simplification guided by
 * spectral analysis of the state-space lattice:
 * <ul>
 *   <li><b>Fiedler bisection</b> -- cut at the weakest coupling point.</li>
 *   <li><b>Eigenvalue degeneracy</b> -- find and merge spectrally redundant states.</li>
 *   <li><b>Spectral clustering</b> -- partition states into k clusters, reconstruct.</li>
 * </ul>
 *
 * <p>Bidirectional morphisms:
 * <ul>
 *   <li>{@code spectral_embedding}: L(S) to R^k  (forward map phi)</li>
 *   <li>{@code reconstruct_from_clusters}: R^k to L(S)  (backward map psi)</li>
 *   <li>{@code round_trip_analysis}: measure what the phi;psi round-trip preserves/loses.</li>
 * </ul>
 *
 * <p>Java port of Python {@code reticulate.spectral_refactor}.
 */
public final class SpectralRefactorChecker {

    private SpectralRefactorChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /**
     * Embedding of lattice states into R^k via Laplacian eigenvectors.
     */
    public record SpectralEmbedding(
            List<Integer> stateIds,
            List<double[]> coordinates,
            double[] eigenvalues,
            int k) {

        public SpectralEmbedding {
            stateIds = List.copyOf(stateIds);
            coordinates = List.copyOf(coordinates);
            eigenvalues = eigenvalues.clone();
        }
    }

    /**
     * A group of near-degenerate eigenvalues.
     */
    public record DegeneracyGroup(
            double eigenvalue,
            List<Integer> indices,
            int multiplicity,
            double spread) {

        public DegeneracyGroup {
            indices = List.copyOf(indices);
        }
    }

    /**
     * A pair of states with near-identical spectral signatures.
     */
    public record RedundantPair(
            int stateA,
            int stateB,
            double distance,
            double[] coordA,
            double[] coordB) {

        public RedundantPair {
            coordA = coordA.clone();
            coordB = coordB.clone();
        }
    }

    /**
     * A plan for refactoring a session type based on spectral analysis.
     */
    public record RefactoringPlan(
            String approach,
            String description,
            List<int[]> mergePairs,
            Map<Integer, Integer> clusterMap,
            Set<Integer> partitionA,
            Set<Integer> partitionB,
            double estimatedReduction) {

        public RefactoringPlan {
            Objects.requireNonNull(approach);
            Objects.requireNonNull(description);
            mergePairs = mergePairs == null ? List.of() : List.copyOf(mergePairs);
            clusterMap = clusterMap == null ? Map.of() : Map.copyOf(clusterMap);
            partitionA = partitionA == null ? null : Set.copyOf(partitionA);
            partitionB = partitionB == null ? null : Set.copyOf(partitionB);
        }

        /** Convenience for plans with no partition. */
        public RefactoringPlan(String approach, String description, double estimatedReduction) {
            this(approach, description, List.of(), Map.of(), null, null, estimatedReduction);
        }
    }

    /**
     * Full spectral refactoring analysis.
     */
    public record SpectralRefactorAnalysis(
            SpectralEmbedding embedding,
            List<DegeneracyGroup> degeneracyGroups,
            List<RedundantPair> redundantPairs,
            RefactoringPlan fiedlerPlan,
            RefactoringPlan degeneracyPlan,
            RefactoringPlan clusteringPlan,
            double roundTripLoss,
            int numStates,
            int numStatesAfterMerge,
            int numClusters) {

        public SpectralRefactorAnalysis {
            degeneracyGroups = List.copyOf(degeneracyGroups);
            redundantPairs = List.copyOf(redundantPairs);
        }
    }

    // -----------------------------------------------------------------------
    // Internal: linear algebra helpers
    // -----------------------------------------------------------------------

    /**
     * Build the graph Laplacian of the state space (undirected).
     * Returns {stateList, laplacianMatrix}.
     */
    static double[][] buildLaplacian(StateSpace ss, List<Integer> outStates) {
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);
        outStates.addAll(states);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        // Build undirected adjacency set
        Map<Integer, Set<Integer>> adjSet = new HashMap<>();
        for (int s : states) adjSet.put(s, new HashSet<>());
        for (var t : ss.transitions()) {
            adjSet.get(t.source()).add(t.target());
            adjSet.get(t.target()).add(t.source());
        }

        double[][] L = new double[n][n];
        for (int s : states) {
            int i = idx.get(s);
            int deg = adjSet.get(s).size();
            L[i][i] = deg;
            for (int t : adjSet.get(s)) {
                int j = idx.get(t);
                L[i][j] -= 1.0;
            }
        }
        return L;
    }

    /** Compute eigenvalues of a real symmetric matrix. Delegates to AlgebraicChecker. */
    static double[] eigenvalues(double[][] A) {
        return AlgebraicChecker.computeEigenvalues(A);
    }

    /** Euclidean distance between two vectors. */
    static double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    /**
     * Compute top-k Laplacian eigenvectors via subspace iteration.
     *
     * Returns {eigenvalues[k], eigenvectors[k][n]}.
     */
    private static double[][] laplacianEigenvectors(double[][] L, int k) {
        int n = L.length;
        if (n == 0) return new double[0][];
        if (n == 1) return new double[][]{{0.0}, {1.0}};

        double[] eigs = eigenvalues(L);
        k = Math.min(k, n);

        // Subspace iteration
        double[][] V = new double[k][n];
        for (int col = 0; col < k; col++) {
            for (int row = 0; row < n; row++) {
                V[col][row] = Math.sin((col + 1) * (row + 1) * 0.7);
            }
            double norm = vecNorm(V[col]);
            if (norm > 1e-14) {
                for (int i = 0; i < n; i++) V[col][i] /= norm;
            }
        }

        for (int iter = 0; iter < 300; iter++) {
            double[][] W = new double[k][n];
            for (int col = 0; col < k; col++) {
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        W[col][i] += L[i][j] * V[col][j];
                    }
                }
            }
            // QR (Gram-Schmidt)
            for (int col = 0; col < k; col++) {
                for (int prev = 0; prev < col; prev++) {
                    double dot = dot(W[col], W[prev], n);
                    for (int i = 0; i < n; i++) W[col][i] -= dot * W[prev][i];
                }
                double norm = vecNorm(W[col]);
                if (norm > 1e-14) {
                    for (int i = 0; i < n; i++) W[col][i] /= norm;
                }
            }
            V = W;
        }

        // Rayleigh quotients for sorting
        double[][] result = new double[1 + k][];
        double[] rqVals = new double[k];
        int[] rqIdx = new int[k];
        for (int col = 0; col < k; col++) {
            double[] Lv = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    Lv[i] += L[i][j] * V[col][j];
                }
            }
            rqVals[col] = dot(V[col], Lv, n);
            rqIdx[col] = col;
        }

        // Sort by Rayleigh quotient ascending
        Integer[] indices = new Integer[k];
        for (int i = 0; i < k; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(i -> rqVals[i]));

        double[] sortedEvals = new double[k];
        double[][] sortedEvecs = new double[k][];
        for (int i = 0; i < k; i++) {
            sortedEvals[i] = rqVals[indices[i]];
            sortedEvecs[i] = V[indices[i]];
        }

        // Pack: first row = eigenvalues, rest = eigenvectors
        result[0] = sortedEvals;
        for (int i = 0; i < k; i++) {
            result[1 + i] = sortedEvecs[i];
        }
        return result;
    }

    private static double vecNorm(double[] v) {
        double s = 0;
        for (double x : v) s += x * x;
        return Math.sqrt(s);
    }

    private static double dot(double[] a, double[] b, int n) {
        double s = 0;
        for (int i = 0; i < n; i++) s += a[i] * b[i];
        return s;
    }

    // -----------------------------------------------------------------------
    // Approach A: Fiedler bisection refactoring
    // -----------------------------------------------------------------------

    /**
     * Cut at the weakest coupling point using Fiedler vector bisection.
     */
    public static RefactoringPlan fiedlerRefactoring(StateSpace ss) {
        int n = ss.states().size();
        if (n <= 1) {
            return new RefactoringPlan("fiedler",
                    "Trivial protocol (1 state), no bisection possible.", 0.0);
        }

        double fiedler = EigenvalueComputer.fiedlerValue(ss);
        double[] fvec = FiedlerChecker.fiedlerVector(ss);
        List<Integer> stateList = AlgebraicChecker.stateList(ss);

        // Bisect by Fiedler vector sign
        Set<Integer> partA = new HashSet<>();
        Set<Integer> partB = new HashSet<>();
        for (int i = 0; i < stateList.size(); i++) {
            if (fvec.length > i && fvec[i] >= 0) {
                partA.add(stateList.get(i));
            } else {
                partB.add(stateList.get(i));
            }
        }

        // Ensure both partitions are non-empty
        if (partA.isEmpty()) {
            partA.add(partB.iterator().next());
            partB.remove(partA.iterator().next());
        } else if (partB.isEmpty()) {
            partB.add(partA.iterator().next());
            partA.remove(partB.iterator().next());
        }

        // Count crossing edges for cut ratio
        int crossingEdges = 0;
        int totalEdges = 0;
        for (var t : ss.transitions()) {
            totalEdges++;
            boolean srcInA = partA.contains(t.source());
            boolean tgtInA = partA.contains(t.target());
            if (srcInA != tgtInA) crossingEdges++;
        }
        double cutRatio = totalEdges > 0 ? (double) crossingEdges / totalEdges : 0.0;
        double estReduction = n > 2 ? Math.max(0.0, 1.0 - cutRatio) * 0.5 : 0.0;

        String desc = String.format(
                "Fiedler bisection: partition into %d + %d states, cut ratio %.3f, Fiedler value %.4f.",
                partA.size(), partB.size(), cutRatio, fiedler);

        return new RefactoringPlan("fiedler", desc, List.of(), Map.of(),
                partA, partB, estReduction);
    }

    // -----------------------------------------------------------------------
    // Approach B: Spectral embedding + degeneracy
    // -----------------------------------------------------------------------

    /**
     * Embed lattice states in R^k via top-k Laplacian eigenvectors.
     */
    public static SpectralEmbedding spectralEmbedding(StateSpace ss, int k) {
        int n = ss.states().size();
        k = Math.min(k, Math.max(1, n - 1));

        List<Integer> stateList = new ArrayList<>();
        double[][] L = buildLaplacian(ss, stateList);

        if (n <= 1) {
            List<double[]> coords = new ArrayList<>();
            for (int i = 0; i < stateList.size(); i++) {
                coords.add(new double[k]);
            }
            return new SpectralEmbedding(stateList, coords, new double[Math.min(k, n)], k);
        }

        double[][] evData = laplacianEigenvectors(L, k + 1);
        double[] allEvals = evData[0];

        // Skip trivial eigenvector (eigenvalue ~0)
        List<Double> usedEvals = new ArrayList<>();
        List<double[]> usedEvecs = new ArrayList<>();
        for (int i = 0; i < allEvals.length; i++) {
            if (i == 0 && Math.abs(allEvals[i]) < 0.01) continue;
            usedEvals.add(allEvals[i]);
            usedEvecs.add(evData[1 + i]);
            if (usedEvals.size() >= k) break;
        }

        // Pad if not enough
        while (usedEvals.size() < k) {
            usedEvals.add(0.0);
            usedEvecs.add(new double[n]);
        }

        // Build coordinate list
        List<double[]> coords = new ArrayList<>();
        for (int i = 0; i < stateList.size(); i++) {
            double[] coord = new double[k];
            for (int d = 0; d < k; d++) {
                coord[d] = usedEvecs.get(d)[i];
            }
            coords.add(coord);
        }

        double[] evalArr = new double[k];
        for (int i = 0; i < k; i++) evalArr[i] = usedEvals.get(i);

        return new SpectralEmbedding(stateList, coords, evalArr, k);
    }

    /** Default k=3. */
    public static SpectralEmbedding spectralEmbedding(StateSpace ss) {
        return spectralEmbedding(ss, 3);
    }

    /**
     * Find near-degenerate eigenvalues in the Laplacian spectrum.
     */
    public static List<DegeneracyGroup> eigenvalueDegeneracy(StateSpace ss, double tol) {
        List<Integer> stateList = new ArrayList<>();
        double[][] L = buildLaplacian(ss, stateList);
        if (stateList.isEmpty()) return List.of();

        double[] eigs = eigenvalues(L);
        if (eigs.length <= 1) return List.of();
        Arrays.sort(eigs);

        List<DegeneracyGroup> groups = new ArrayList<>();
        Set<Integer> used = new HashSet<>();

        for (int i = 0; i < eigs.length; i++) {
            if (used.contains(i)) continue;
            List<Integer> groupIndices = new ArrayList<>();
            groupIndices.add(i);
            used.add(i);
            for (int j = i + 1; j < eigs.length; j++) {
                if (used.contains(j)) continue;
                if (Math.abs(eigs[j] - eigs[i]) <= tol) {
                    groupIndices.add(j);
                    used.add(j);
                }
            }
            if (groupIndices.size() > 1) {
                double sum = 0, min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
                for (int idx : groupIndices) {
                    sum += eigs[idx];
                    min = Math.min(min, eigs[idx]);
                    max = Math.max(max, eigs[idx]);
                }
                groups.add(new DegeneracyGroup(
                        sum / groupIndices.size(),
                        groupIndices,
                        groupIndices.size(),
                        max - min));
            }
        }
        return groups;
    }

    /** Default tol=0.1. */
    public static List<DegeneracyGroup> eigenvalueDegeneracy(StateSpace ss) {
        return eigenvalueDegeneracy(ss, 0.1);
    }

    /**
     * Find pairs of states with near-identical spectral signatures.
     */
    public static List<RedundantPair> redundantStates(StateSpace ss, double tol) {
        int n = ss.states().size();
        if (n <= 1) return List.of();

        int k = Math.min(3, n - 1);
        SpectralEmbedding emb = spectralEmbedding(ss, k);

        List<RedundantPair> pairs = new ArrayList<>();
        for (int i = 0; i < emb.stateIds().size(); i++) {
            for (int j = i + 1; j < emb.stateIds().size(); j++) {
                double d = euclideanDistance(emb.coordinates().get(i), emb.coordinates().get(j));
                if (d <= tol) {
                    pairs.add(new RedundantPair(
                            emb.stateIds().get(i),
                            emb.stateIds().get(j),
                            d,
                            emb.coordinates().get(i),
                            emb.coordinates().get(j)));
                }
            }
        }
        pairs.sort(Comparator.comparingDouble(RedundantPair::distance));
        return pairs;
    }

    /** Default tol=0.1. */
    public static List<RedundantPair> redundantStates(StateSpace ss) {
        return redundantStates(ss, 0.1);
    }

    /**
     * Merge spectrally redundant state pairs into a simplified state space.
     */
    public static StateSpace mergeRedundant(StateSpace ss, List<RedundantPair> pairs) {
        if (pairs.isEmpty()) return ss;

        // Build merge map
        Map<Integer, Integer> mergeMap = new HashMap<>();
        for (int s : ss.states()) mergeMap.put(s, s);

        for (var pair : pairs) {
            int a = pair.stateA(), b = pair.stateB();
            int keep = Math.min(a, b), remove = Math.max(a, b);
            while (mergeMap.get(keep) != keep) keep = mergeMap.get(keep);
            mergeMap.put(remove, keep);
            for (int s : new ArrayList<>(mergeMap.keySet())) {
                if (mergeMap.get(s) == remove) mergeMap.put(s, keep);
            }
        }

        // Resolve transitives
        for (int s : new ArrayList<>(mergeMap.keySet())) {
            int rep = s;
            while (mergeMap.get(rep) != rep) rep = mergeMap.get(rep);
            mergeMap.put(s, rep);
        }

        Set<Integer> newStates = new HashSet<>(mergeMap.values());
        List<Transition> newTrans = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (var t : ss.transitions()) {
            int src = mergeMap.get(t.source());
            int tgt = mergeMap.get(t.target());
            if (src == tgt) continue;
            String key = src + ":" + t.label() + ":" + tgt;
            if (seen.add(key)) {
                newTrans.add(new Transition(src, t.label(), tgt, t.kind()));
            }
        }

        int newTop = mergeMap.get(ss.top());
        int newBottom = mergeMap.get(ss.bottom());

        Map<Integer, String> labels = new HashMap<>();
        for (int s : newStates) labels.put(s, "[" + s + "]");

        return new StateSpace(newStates, newTrans, newTop, newBottom, labels);
    }

    // -----------------------------------------------------------------------
    // Approach C: Spectral clustering
    // -----------------------------------------------------------------------

    /**
     * Simple k-means clustering. Deterministic initialization.
     */
    static int[] kmeans(List<double[]> points, int k, int maxIter) {
        int n = points.size();
        if (n == 0) return new int[0];
        if (k >= n) {
            int[] result = new int[n];
            for (int i = 0; i < n; i++) result[i] = i;
            return result;
        }

        int dim = points.get(0).length;
        double[][] centroids = new double[k][];
        for (int i = 0; i < k; i++) {
            centroids[i] = points.get(i * n / k).clone();
        }

        int[] assignments = new int[n];

        for (int iter = 0; iter < maxIter; iter++) {
            int[] newAssign = new int[n];
            for (int i = 0; i < n; i++) {
                int bestC = 0;
                double bestD = euclideanDistance(points.get(i), centroids[0]);
                for (int c = 1; c < k; c++) {
                    double d = euclideanDistance(points.get(i), centroids[c]);
                    if (d < bestD) {
                        bestD = d;
                        bestC = c;
                    }
                }
                newAssign[i] = bestC;
            }

            if (Arrays.equals(newAssign, assignments)) break;
            assignments = newAssign;

            // Update centroids
            for (int c = 0; c < k; c++) {
                double[] sum = new double[dim];
                int count = 0;
                for (int i = 0; i < n; i++) {
                    if (assignments[i] == c) {
                        for (int d = 0; d < dim; d++) sum[d] += points.get(i)[d];
                        count++;
                    }
                }
                if (count > 0) {
                    centroids[c] = new double[dim];
                    for (int d = 0; d < dim; d++) centroids[c][d] = sum[d] / count;
                }
            }
        }
        return assignments;
    }

    /**
     * Partition states into k clusters using spectral embedding + k-means.
     */
    public static Map<Integer, Integer> spectralClusters(StateSpace ss, int k) {
        int n = ss.states().size();
        if (n <= 1) {
            Map<Integer, Integer> result = new HashMap<>();
            for (int s : ss.states()) result.put(s, 0);
            return result;
        }

        k = Math.min(k, n);
        int embK = Math.min(k, n - 1);
        SpectralEmbedding emb = spectralEmbedding(ss, embK);

        int[] assignments = kmeans(emb.coordinates(), k, 100);

        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 0; i < emb.stateIds().size(); i++) {
            result.put(emb.stateIds().get(i), assignments[i]);
        }
        return result;
    }

    /**
     * Build a simplified state space from spectral clusters.
     */
    public static StateSpace reconstructFromClusters(StateSpace ss, Map<Integer, Integer> clusters) {
        if (clusters.isEmpty()) return ss;

        // Use minimum state ID in each cluster as representative
        Map<Integer, Integer> clusterReps = new HashMap<>();
        var sortedEntries = new ArrayList<>(clusters.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        for (var entry : sortedEntries) {
            int state = entry.getKey();
            int cid = entry.getValue();
            clusterReps.merge(cid, state, Math::min);
        }

        Map<Integer, Integer> stateToRep = new HashMap<>();
        for (var entry : clusters.entrySet()) {
            stateToRep.put(entry.getKey(), clusterReps.get(entry.getValue()));
        }

        Set<Integer> newStates = new HashSet<>(clusterReps.values());
        List<Transition> newTrans = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (var t : ss.transitions()) {
            int src = stateToRep.getOrDefault(t.source(), t.source());
            int tgt = stateToRep.getOrDefault(t.target(), t.target());
            if (src == tgt) continue;
            String key = src + ":" + t.label() + ":" + tgt;
            if (seen.add(key)) {
                newTrans.add(new Transition(src, t.label(), tgt, t.kind()));
            }
        }

        int newTop = stateToRep.getOrDefault(ss.top(), ss.top());
        int newBottom = stateToRep.getOrDefault(ss.bottom(), ss.bottom());
        newStates.add(newTop);
        newStates.add(newBottom);

        Map<Integer, String> labels = new HashMap<>();
        for (int s : newStates) labels.put(s, "cluster(" + s + ")");

        return new StateSpace(newStates, newTrans, newTop, newBottom, labels);
    }

    // -----------------------------------------------------------------------
    // Round-trip analysis
    // -----------------------------------------------------------------------

    /**
     * Measure what the spectral round-trip preserves.
     */
    public static Map<String, Object> roundTripAnalysis(StateSpace ss, int k) {
        Map<Integer, Integer> clusters = spectralClusters(ss, k);
        StateSpace reconstructed = reconstructFromClusters(ss, clusters);

        int origStates = ss.states().size();
        int newStates = reconstructed.states().size();
        double statePres = origStates > 0 ? (double) newStates / origStates : 1.0;

        int origTrans = ss.transitions().size();
        int newTrans = reconstructed.transitions().size();
        double transPres = origTrans > 0 ? (double) newTrans / origTrans : 1.0;

        boolean topOk = reconstructed.states().contains(reconstructed.top());
        boolean bottomOk = reconstructed.states().contains(reconstructed.bottom());

        LatticeResult lr = LatticeChecker.checkLattice(reconstructed);
        boolean latticeOk = lr.isLattice();

        // Selection preservation
        long origSel = ss.transitions().stream()
                .filter(t -> t.kind() == TransitionKind.SELECTION).count();
        long newSel = reconstructed.transitions().stream()
                .filter(t -> t.kind() == TransitionKind.SELECTION).count();
        double selPres = origSel > 0 ? (double) newSel / origSel : 1.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("state_preservation", statePres);
        result.put("transition_preservation", transPres);
        result.put("top_bottom_preserved", topOk && bottomOk);
        result.put("lattice_preserved", latticeOk);
        result.put("selection_preservation", selPres);
        result.put("original_states", origStates);
        result.put("reconstructed_states", newStates);
        return result;
    }

    // -----------------------------------------------------------------------
    // Pipeline
    // -----------------------------------------------------------------------

    /**
     * Compare original and refactored state spaces.
     */
    public static Map<String, Object> compareOriginalRefactored(StateSpace ss, StateSpace refactored) {
        LatticeResult origLR = LatticeChecker.checkLattice(ss);
        LatticeResult refLR = LatticeChecker.checkLattice(refactored);

        Set<String> origMethods = new HashSet<>();
        for (var t : ss.transitions()) origMethods.add(t.label());
        Set<String> refMethods = new HashSet<>();
        for (var t : refactored.transitions()) refMethods.add(t.label());

        Set<String> lost = new HashSet<>(origMethods);
        lost.removeAll(refMethods);
        Set<String> gained = new HashSet<>(refMethods);
        gained.removeAll(origMethods);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("original_states", ss.states().size());
        result.put("refactored_states", refactored.states().size());
        result.put("state_reduction", ss.states().size() > 0
                ? 1.0 - (double) refactored.states().size() / ss.states().size() : 0.0);
        result.put("original_is_lattice", origLR.isLattice());
        result.put("refactored_is_lattice", refLR.isLattice());
        result.put("lattice_preserved",
                origLR.isLattice() == refLR.isLattice() || refLR.isLattice());
        result.put("methods_preserved", lost.isEmpty());
        result.put("lost_methods", lost);
        result.put("gained_methods", gained);
        result.put("original_transitions", ss.transitions().size());
        result.put("refactored_transitions", refactored.transitions().size());
        return result;
    }

    /**
     * Full spectral refactoring pipeline.
     */
    public static SpectralRefactorAnalysis refactoringPipeline(StateSpace ss) {
        int n = ss.states().size();
        int k = Math.min(3, Math.max(1, n - 1));

        // Embedding
        SpectralEmbedding emb = spectralEmbedding(ss, k);

        // A: Fiedler
        RefactoringPlan fiedlerPlan = fiedlerRefactoring(ss);

        // B: Degeneracy
        List<DegeneracyGroup> degGroups = eigenvalueDegeneracy(ss, 0.1);
        List<RedundantPair> redPairs = redundantStates(ss, 0.1);

        List<int[]> mergeTuples = new ArrayList<>();
        for (var p : redPairs) mergeTuples.add(new int[]{p.stateA(), p.stateB()});

        StateSpace mergedSs = mergeRedundant(ss, redPairs);
        int nAfterMerge = mergedSs.states().size();
        double degReduction = n > 0 ? 1.0 - (double) nAfterMerge / n : 0.0;

        RefactoringPlan degPlan = new RefactoringPlan(
                "degeneracy",
                String.format("Degeneracy analysis: %d degenerate group(s), "
                                + "%d redundant pair(s), merge reduces %d -> %d states.",
                        degGroups.size(), redPairs.size(), n, nAfterMerge),
                mergeTuples, Map.of(), null, null, degReduction);

        // C: Clustering
        int nClusters = n > 2 ? Math.min(Math.max(2, n / 3), n) : Math.max(1, n);
        Map<Integer, Integer> clusters = spectralClusters(ss, nClusters);
        int actualClusters = clusters.isEmpty() ? 0 : new HashSet<>(clusters.values()).size();
        double clusterReduction = n > 0 ? 1.0 - (double) actualClusters / n : 0.0;

        RefactoringPlan clusterPlan = new RefactoringPlan(
                "clustering",
                String.format("Spectral clustering: %d clusters from %d states, "
                        + "estimated reduction %.1f%%.", actualClusters, n, clusterReduction * 100),
                List.of(), new HashMap<>(clusters), null, null, clusterReduction);

        // Round-trip loss
        Map<String, Object> rt = roundTripAnalysis(ss, nClusters);
        double statePres = ((Number) rt.get("state_preservation")).doubleValue();
        double transPres = ((Number) rt.get("transition_preservation")).doubleValue();
        boolean latticePres = (Boolean) rt.get("lattice_preserved");
        double rtLoss = 1.0 - (statePres * 0.4 + transPres * 0.4
                + (latticePres ? 1.0 : 0.0) * 0.2);
        rtLoss = Math.max(0.0, Math.min(1.0, rtLoss));

        return new SpectralRefactorAnalysis(
                emb, degGroups, redPairs,
                fiedlerPlan, degPlan, clusterPlan,
                rtLoss, n, nAfterMerge, actualClusters);
    }
}
