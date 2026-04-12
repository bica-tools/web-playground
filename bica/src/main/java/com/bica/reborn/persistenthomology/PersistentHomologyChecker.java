package com.bica.reborn.persistenthomology;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Persistent homology of session type lattices (Step 33c).
 *
 * <p>Persistent homology tracks how the topology of the order complex changes
 * as it is built up through a filtration. It answers: "Which topological
 * features of a protocol are robust (long bars) and which are fragile
 * (short bars)?"
 *
 * <p>A <b>filtration</b> is a nested sequence of subcomplexes:
 * {@code {} = K_0 <= K_1 <= ... <= K_n = K}.
 * As simplices are added, homology classes are <b>born</b> (new holes appear)
 * or <b>die</b> (holes get filled). The persistence diagram records (birth, death)
 * pairs. Long bars = robust features. Short bars = noise.
 *
 * <p>Filtrations for session types:
 * <ol>
 *   <li><b>Rank filtration</b> -- add states by rank level (bottom to top)</li>
 *   <li><b>Reverse rank</b> -- add from top to bottom</li>
 *   <li><b>Sublevel</b> -- by BFS distance from a center state</li>
 * </ol>
 *
 * <p>Key methods:
 * <ul>
 *   <li>{@link #rankFiltration(StateSpace)} -- build rank-based filtration</li>
 *   <li>{@link #computePersistence(Filtration)} -- persistence via column reduction</li>
 *   <li>{@link #bottleneckDistance(PersistenceDiagram, PersistenceDiagram)} -- metric between protocols</li>
 *   <li>{@link #persistenceEntropy(PersistenceDiagram)} -- Shannon entropy of bar lengths</li>
 *   <li>{@link #analyzePersistence(StateSpace)} -- full analysis</li>
 * </ul>
 */
public final class PersistentHomologyChecker {

    private PersistentHomologyChecker() {}

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * A birth-death pair in the persistence diagram.
     *
     * @param birth       filtration level at which the feature is born
     * @param death       filtration level at which the feature dies (POSITIVE_INFINITY if it persists)
     * @param dimension   homological dimension (0 = component, 1 = loop, ...)
     */
    public record PersistencePair(double birth, double death, int dimension) {

        /** Persistence (lifetime) of this feature: death - birth. */
        public double persistence() {
            if (Double.isInfinite(death)) return Double.POSITIVE_INFINITY;
            return death - birth;
        }

        /** True if this feature never dies. */
        public boolean isInfinite() {
            return Double.isInfinite(death);
        }
    }

    /**
     * Persistence diagram: collection of birth-death pairs.
     *
     * @param pairs          all finite persistence pairs
     * @param infinitePairs  pairs that never die (essential features)
     * @param maxFiltration  maximum filtration level
     */
    public record PersistenceDiagram(
            List<PersistencePair> pairs,
            List<PersistencePair> infinitePairs,
            double maxFiltration) {

        public PersistenceDiagram {
            Objects.requireNonNull(pairs, "pairs must not be null");
            Objects.requireNonNull(infinitePairs, "infinitePairs must not be null");
            pairs = List.copyOf(pairs);
            infinitePairs = List.copyOf(infinitePairs);
        }

        /** All pairs (finite + infinite). */
        public List<PersistencePair> allPairs() {
            var result = new ArrayList<>(pairs);
            result.addAll(infinitePairs);
            return List.copyOf(result);
        }

        /** Total number of pairs. */
        public int numPairs() {
            return pairs.size() + infinitePairs.size();
        }

        /** Pairs in a given homological dimension. */
        public List<PersistencePair> pairsInDim(int dim) {
            return allPairs().stream()
                    .filter(p -> p.dimension() == dim)
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    /**
     * A filtration of simplices with filtration values.
     *
     * @param simplices         list of simplices (each a sorted list of vertex IDs)
     * @param filtrationValues  filtration level for each simplex
     * @param maxLevel          maximum filtration level
     * @param numSimplices      total number of simplices
     */
    public record Filtration(
            List<List<Integer>> simplices,
            List<Double> filtrationValues,
            double maxLevel,
            int numSimplices) {

        public Filtration {
            Objects.requireNonNull(simplices, "simplices must not be null");
            Objects.requireNonNull(filtrationValues, "filtrationValues must not be null");
            simplices = simplices.stream()
                    .map(List::copyOf)
                    .collect(Collectors.toUnmodifiableList());
            filtrationValues = List.copyOf(filtrationValues);
        }
    }

    /**
     * Full persistent homology analysis.
     *
     * @param diagram              the persistence diagram
     * @param filtration           the filtration used
     * @param totalPersistence     sum of all finite bar lengths
     * @param maxPersistence       length of the longest finite bar
     * @param persistenceEntropy   Shannon entropy of bar length distribution
     * @param numBornDim0          number of 0-dimensional features born
     * @param numBornDim1          number of 1-dimensional features born
     * @param betti0               final Betti number beta_0
     * @param betti1               final Betti number beta_1
     * @param numStates            number of lattice elements
     */
    public record PersistenceAnalysis(
            PersistenceDiagram diagram,
            Filtration filtration,
            double totalPersistence,
            double maxPersistence,
            double persistenceEntropy,
            int numBornDim0,
            int numBornDim1,
            int betti0,
            int betti1,
            int numStates) {

        public PersistenceAnalysis {
            Objects.requireNonNull(diagram, "diagram must not be null");
            Objects.requireNonNull(filtration, "filtration must not be null");
        }
    }

    // -----------------------------------------------------------------------
    // Internal: rank computation
    // -----------------------------------------------------------------------

    /** BFS distance from bottom (rank), traversing edges in reverse. */
    private static Map<Integer, Integer> rank(StateSpace ss) {
        // Build reverse adjacency
        Map<Integer, Set<Integer>> rev = new HashMap<>();
        for (int s : ss.states()) rev.put(s, new HashSet<>());
        for (var t : ss.transitions()) {
            rev.computeIfAbsent(t.target(), k -> new HashSet<>()).add(t.source());
        }

        Map<Integer, Integer> dist = new HashMap<>();
        if (ss.states().contains(ss.bottom())) {
            dist.put(ss.bottom(), 0);
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(ss.bottom());
            while (!queue.isEmpty()) {
                int s = queue.poll();
                for (int pred : rev.getOrDefault(s, Set.of())) {
                    if (!dist.containsKey(pred)) {
                        dist.put(pred, dist.get(s) + 1);
                        queue.add(pred);
                    }
                }
            }
        }
        // States not reached get rank 0
        for (int s : ss.states()) {
            dist.putIfAbsent(s, 0);
        }
        return dist;
    }

    /** Covering relations (Hasse edges) with cycle avoidance. */
    private static List<int[]> coveringRelations(StateSpace ss) {
        // Forward adjacency
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new HashSet<>()).add(t.target());
        }

        // Reachability via DFS
        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int v = stack.pop();
                if (visited.add(v)) {
                    for (int t : adj.getOrDefault(v, Set.of())) {
                        stack.push(t);
                    }
                }
            }
            reach.put(s, visited);
        }

        List<int[]> covers = new ArrayList<>();
        for (int s : ss.states()) {
            for (int t : adj.getOrDefault(s, Set.of())) {
                if (t == s) continue;
                boolean isCover = true;
                for (int u : adj.getOrDefault(s, Set.of())) {
                    if (u != t && u != s && reach.getOrDefault(u, Set.of()).contains(t)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) {
                    covers.add(new int[]{s, t});
                }
            }
        }
        return covers;
    }

    // -----------------------------------------------------------------------
    // Public API: Filtrations
    // -----------------------------------------------------------------------

    /**
     * Build a rank-based filtration.
     *
     * <p>Add vertices by rank (bottom = rank 0 first), then edges between
     * vertices that are both present, then 2-simplices for chains of length 3.
     *
     * @param ss the state space
     * @return the rank filtration
     */
    public static Filtration rankFiltration(StateSpace ss) {
        Map<Integer, Integer> r = rank(ss);
        List<int[]> covers = coveringRelations(ss);

        // Sort states by rank
        List<Integer> states = new ArrayList<>(ss.states());
        states.sort(Comparator.comparingInt(s -> r.getOrDefault(s, 0)));

        List<List<Integer>> simplices = new ArrayList<>();
        List<Double> filtVals = new ArrayList<>();

        // Add vertices at their rank level
        for (int s : states) {
            simplices.add(List.of(s));
            filtVals.add((double) r.getOrDefault(s, 0));
        }

        // Add edges at max(rank of endpoints)
        for (int[] cover : covers) {
            double level = Math.max(r.getOrDefault(cover[0], 0), r.getOrDefault(cover[1], 0));
            List<Integer> edge = new ArrayList<>(List.of(cover[0], cover[1]));
            Collections.sort(edge);
            simplices.add(List.copyOf(edge));
            filtVals.add(level);
        }

        // Add 2-simplices (triangles) for chains of length 3
        Set<Long> coverSet = new HashSet<>();
        for (int[] c : covers) {
            coverSet.add(pairKey(c[0], c[1]));
        }
        Set<List<Integer>> addedTriangles = new HashSet<>();
        for (int s : ss.states()) {
            for (int t : ss.states()) {
                if (t == s) continue;
                for (int u : ss.states()) {
                    if (u == s || u == t) continue;
                    if (coverSet.contains(pairKey(s, t)) && coverSet.contains(pairKey(t, u))) {
                        List<Integer> tri = new ArrayList<>(List.of(s, t, u));
                        Collections.sort(tri);
                        if (addedTriangles.add(tri)) {
                            double level = Math.max(Math.max(
                                    r.getOrDefault(s, 0),
                                    r.getOrDefault(t, 0)),
                                    r.getOrDefault(u, 0));
                            simplices.add(List.copyOf(tri));
                            filtVals.add(level);
                        }
                    }
                }
            }
        }

        double maxLevel = filtVals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        return new Filtration(simplices, filtVals, maxLevel, simplices.size());
    }

    /** Encode a pair of ints into a long key for set lookups. */
    private static long pairKey(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    /**
     * Build a reverse-rank filtration (top to bottom).
     *
     * @param ss the state space
     * @return the reverse rank filtration
     */
    public static Filtration reverseRankFiltration(StateSpace ss) {
        Map<Integer, Integer> r = rank(ss);
        int maxR = r.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<int[]> covers = coveringRelations(ss);

        List<Integer> states = new ArrayList<>(ss.states());
        states.sort(Comparator.comparingInt(s -> -(r.getOrDefault(s, 0))));

        List<List<Integer>> simplices = new ArrayList<>();
        List<Double> filtVals = new ArrayList<>();

        for (int s : states) {
            simplices.add(List.of(s));
            filtVals.add((double) (maxR - r.getOrDefault(s, 0)));
        }

        for (int[] cover : covers) {
            double level = Math.max(
                    maxR - r.getOrDefault(cover[0], 0),
                    maxR - r.getOrDefault(cover[1], 0));
            List<Integer> edge = new ArrayList<>(List.of(cover[0], cover[1]));
            Collections.sort(edge);
            simplices.add(List.copyOf(edge));
            filtVals.add(level);
        }

        double maxLevel = filtVals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        return new Filtration(simplices, filtVals, maxLevel, simplices.size());
    }

    /**
     * Build a sublevel filtration by BFS distance from center.
     *
     * @param ss     the state space
     * @param center the center state for BFS
     * @return the sublevel filtration
     */
    public static Filtration sublevelFiltration(StateSpace ss, int center) {
        // Undirected BFS adjacency
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new HashSet<>()).add(t.target());
            adj.computeIfAbsent(t.target(), k -> new HashSet<>()).add(t.source());
        }

        Map<Integer, Integer> dist = new HashMap<>();
        dist.put(center, 0);
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(center);
        while (!queue.isEmpty()) {
            int s = queue.poll();
            for (int t : adj.getOrDefault(s, Set.of())) {
                if (!dist.containsKey(t)) {
                    dist.put(t, dist.get(s) + 1);
                    queue.add(t);
                }
            }
        }
        for (int s : ss.states()) {
            dist.putIfAbsent(s, ss.states().size());
        }

        List<int[]> covers = coveringRelations(ss);

        List<Integer> states = new ArrayList<>(ss.states());
        states.sort(Comparator.comparingInt(dist::get));

        List<List<Integer>> simplices = new ArrayList<>();
        List<Double> filtVals = new ArrayList<>();

        for (int s : states) {
            simplices.add(List.of(s));
            filtVals.add((double) dist.get(s));
        }

        for (int[] cover : covers) {
            double level = Math.max(
                    dist.getOrDefault(cover[0], 0),
                    dist.getOrDefault(cover[1], 0));
            List<Integer> edge = new ArrayList<>(List.of(cover[0], cover[1]));
            Collections.sort(edge);
            simplices.add(List.copyOf(edge));
            filtVals.add(level);
        }

        double maxLevel = filtVals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        return new Filtration(simplices, filtVals, maxLevel, simplices.size());
    }

    // -----------------------------------------------------------------------
    // Public API: Persistence computation
    // -----------------------------------------------------------------------

    /**
     * Compute persistent homology via the standard column reduction algorithm.
     *
     * <p>Sorts simplices by filtration value, builds boundary matrix, reduces
     * columns to extract birth-death pairs.
     *
     * @param filtration the filtration to compute persistence for
     * @return the persistence diagram
     */
    public static PersistenceDiagram computePersistence(Filtration filtration) {
        if (filtration.numSimplices() == 0) {
            return new PersistenceDiagram(List.of(), List.of(), 0.0);
        }

        // Build indexed list and sort by (filtration_value, dimension, lexicographic)
        int n = filtration.numSimplices();
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        Arrays.sort(indices, (a, b) -> {
            int cmp = Double.compare(filtration.filtrationValues().get(a),
                                     filtration.filtrationValues().get(b));
            if (cmp != 0) return cmp;
            cmp = Integer.compare(filtration.simplices().get(a).size(),
                                  filtration.simplices().get(b).size());
            if (cmp != 0) return cmp;
            return filtration.simplices().get(a).toString()
                    .compareTo(filtration.simplices().get(b).toString());
        });

        List<List<Integer>> simplices = new ArrayList<>();
        double[] filtVals = new double[n];
        for (int i = 0; i < n; i++) {
            simplices.add(filtration.simplices().get(indices[i]));
            filtVals[i] = filtration.filtrationValues().get(indices[i]);
        }

        // Simplex index lookup
        Map<List<Integer>, Integer> simplexIdx = new HashMap<>();
        for (int i = 0; i < n; i++) {
            simplexIdx.put(simplices.get(i), i);
        }

        // Build boundary matrix (sparse: sets of column indices)
        @SuppressWarnings("unchecked")
        Set<Integer>[] boundary = new Set[n];
        for (int j = 0; j < n; j++) {
            boundary[j] = new HashSet<>();
            List<Integer> sigma = simplices.get(j);
            if (sigma.size() <= 1) continue; // 0-simplices have empty boundary
            for (int i = 0; i < sigma.size(); i++) {
                List<Integer> face = new ArrayList<>(sigma);
                face.remove(i);
                Collections.sort(face);
                Integer faceIdx = simplexIdx.get(face);
                if (faceIdx != null) {
                    boundary[j].add(faceIdx);
                }
            }
        }

        // Column reduction (standard persistence algorithm)
        int[] low = new int[n];
        for (int j = 0; j < n; j++) {
            low[j] = boundary[j].isEmpty() ? -1 : Collections.max(boundary[j]);
        }

        for (int j = 0; j < n; j++) {
            while (low[j] >= 0) {
                boolean found = false;
                for (int jPrime = 0; jPrime < j; jPrime++) {
                    if (low[jPrime] == low[j]) {
                        // Add column jPrime to j (mod 2 = symmetric difference)
                        Set<Integer> symDiff = new HashSet<>(boundary[j]);
                        for (int x : boundary[jPrime]) {
                            if (!symDiff.remove(x)) symDiff.add(x);
                        }
                        boundary[j] = symDiff;
                        low[j] = boundary[j].isEmpty() ? -1 : Collections.max(boundary[j]);
                        found = true;
                        break;
                    }
                }
                if (!found) break;
            }
        }

        // Extract persistence pairs
        List<PersistencePair> finitePairs = new ArrayList<>();
        List<PersistencePair> infinitePairs = new ArrayList<>();
        Set<Integer> paired = new HashSet<>();

        for (int j = 0; j < n; j++) {
            if (low[j] >= 0) {
                int i = low[j];
                paired.add(i);
                paired.add(j);
                double birth = filtVals[i];
                double death = filtVals[j];
                int dim = simplices.get(i).size() - 1;
                if (Math.abs(death - birth) > 1e-12) {
                    finitePairs.add(new PersistencePair(birth, death, dim));
                }
            }
        }

        // Unpaired simplices give infinite bars
        for (int j = 0; j < n; j++) {
            if (!paired.contains(j)) {
                double birth = filtVals[j];
                int dim = simplices.get(j).size() - 1;
                infinitePairs.add(new PersistencePair(birth, Double.POSITIVE_INFINITY, dim));
            }
        }

        finitePairs.sort(Comparator.<PersistencePair>comparingInt(PersistencePair::dimension)
                .thenComparingDouble(PersistencePair::birth));
        infinitePairs.sort(Comparator.<PersistencePair>comparingInt(PersistencePair::dimension)
                .thenComparingDouble(PersistencePair::birth));

        return new PersistenceDiagram(finitePairs, infinitePairs, filtration.maxLevel());
    }

    // -----------------------------------------------------------------------
    // Public API: Persistence invariants
    // -----------------------------------------------------------------------

    /**
     * Sum of all finite bar lengths.
     */
    public static double totalPersistence(PersistenceDiagram pd) {
        return pd.pairs().stream()
                .filter(p -> !p.isInfinite())
                .mapToDouble(PersistencePair::persistence)
                .sum();
    }

    /**
     * Length of the longest finite bar.
     */
    public static double maxPersistence(PersistenceDiagram pd) {
        return pd.pairs().stream()
                .filter(p -> !p.isInfinite())
                .mapToDouble(PersistencePair::persistence)
                .max()
                .orElse(0.0);
    }

    /**
     * Shannon entropy of the bar length distribution.
     *
     * <p>Normalized bar lengths as probabilities: p_i = l_i / sum(l_j).
     */
    public static double persistenceEntropy(PersistenceDiagram pd) {
        double[] lengths = pd.pairs().stream()
                .filter(p -> !p.isInfinite() && p.persistence() > 0)
                .mapToDouble(PersistencePair::persistence)
                .toArray();
        if (lengths.length == 0) return 0.0;
        double total = 0.0;
        for (double l : lengths) total += l;
        if (total <= 0) return 0.0;
        double entropy = 0.0;
        for (double l : lengths) {
            double p = l / total;
            if (p > 0) {
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    // -----------------------------------------------------------------------
    // Public API: Distances between persistence diagrams
    // -----------------------------------------------------------------------

    /**
     * Bottleneck distance between two persistence diagrams (greedy approximation).
     *
     * <p>d_B = inf over matchings M of sup |p - M(p)|_inf.
     */
    public static double bottleneckDistance(PersistenceDiagram pd1, PersistenceDiagram pd2) {
        double[][] pts1 = pd1.pairs().stream()
                .map(p -> new double[]{p.birth(), p.death()})
                .toArray(double[][]::new);
        double[][] pts2 = pd2.pairs().stream()
                .map(p -> new double[]{p.birth(), p.death()})
                .toArray(double[][]::new);

        if (pts1.length == 0 && pts2.length == 0) return 0.0;

        Set<Integer> used2 = new HashSet<>();
        double maxCost = 0.0;

        for (int i = 0; i < pts1.length; i++) {
            int bestJ = -1;
            double bestCost = Double.MAX_VALUE;
            for (int j = 0; j < pts2.length; j++) {
                if (used2.contains(j)) continue;
                double cost = Math.max(
                        Math.abs(pts1[i][0] - pts2[j][0]),
                        Math.abs(pts1[i][1] - pts2[j][1]));
                if (cost < bestCost) {
                    bestCost = cost;
                    bestJ = j;
                }
            }
            if (bestJ >= 0) {
                used2.add(bestJ);
                maxCost = Math.max(maxCost, bestCost);
            } else {
                double diagCost = (pts1[i][1] - pts1[i][0]) / 2.0;
                maxCost = Math.max(maxCost, diagCost);
            }
        }

        for (int j = 0; j < pts2.length; j++) {
            if (!used2.contains(j)) {
                double diagCost = (pts2[j][1] - pts2[j][0]) / 2.0;
                maxCost = Math.max(maxCost, diagCost);
            }
        }

        return maxCost;
    }

    /**
     * p-Wasserstein distance between persistence diagrams (greedy approximation).
     */
    public static double wassersteinDistance(PersistenceDiagram pd1, PersistenceDiagram pd2,
                                             double p) {
        double[][] pts1 = pd1.pairs().stream()
                .map(pt -> new double[]{pt.birth(), pt.death()})
                .toArray(double[][]::new);
        double[][] pts2 = pd2.pairs().stream()
                .map(pt -> new double[]{pt.birth(), pt.death()})
                .toArray(double[][]::new);

        if (pts1.length == 0 && pts2.length == 0) return 0.0;

        Set<Integer> used2 = new HashSet<>();
        double totalCost = 0.0;

        for (int i = 0; i < pts1.length; i++) {
            int bestJ = -1;
            double bestCost = Double.MAX_VALUE;
            for (int j = 0; j < pts2.length; j++) {
                if (used2.contains(j)) continue;
                double cost = Math.max(
                        Math.abs(pts1[i][0] - pts2[j][0]),
                        Math.abs(pts1[i][1] - pts2[j][1]));
                if (cost < bestCost) {
                    bestCost = cost;
                    bestJ = j;
                }
            }
            if (bestJ >= 0) {
                used2.add(bestJ);
                totalCost += Math.pow(bestCost, p);
            } else {
                double diagCost = (pts1[i][1] - pts1[i][0]) / 2.0;
                totalCost += Math.pow(diagCost, p);
            }
        }

        for (int j = 0; j < pts2.length; j++) {
            if (!used2.contains(j)) {
                double diagCost = (pts2[j][1] - pts2[j][0]) / 2.0;
                totalCost += Math.pow(diagCost, p);
            }
        }

        return p > 0 ? Math.pow(totalCost, 1.0 / p) : totalCost;
    }

    /**
     * 1-Wasserstein distance (convenience overload).
     */
    public static double wassersteinDistance(PersistenceDiagram pd1, PersistenceDiagram pd2) {
        return wassersteinDistance(pd1, pd2, 1.0);
    }

    // -----------------------------------------------------------------------
    // Public API: Convenience
    // -----------------------------------------------------------------------

    /**
     * Compute persistence pairs using rank filtration.
     */
    public static List<PersistencePair> persistencePairs(StateSpace ss) {
        Filtration filt = rankFiltration(ss);
        PersistenceDiagram pd = computePersistence(filt);
        return pd.allPairs();
    }

    /**
     * Barcodes per dimension: dimension to list of (birth, death) pairs.
     */
    public static Map<Integer, List<double[]>> bettiBarcodes(StateSpace ss) {
        Filtration filt = rankFiltration(ss);
        PersistenceDiagram pd = computePersistence(filt);
        Map<Integer, List<double[]>> result = new HashMap<>();
        for (var p : pd.allPairs()) {
            result.computeIfAbsent(p.dimension(), k -> new ArrayList<>())
                    .add(new double[]{p.birth(), p.death()});
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Public API: Full analysis
    // -----------------------------------------------------------------------

    /**
     * Full persistent homology analysis.
     */
    public static PersistenceAnalysis analyzePersistence(StateSpace ss) {
        Filtration filt = rankFiltration(ss);
        PersistenceDiagram pd = computePersistence(filt);

        double tp = totalPersistence(pd);
        double mp = maxPersistence(pd);
        double pe = persistenceEntropy(pd);

        int dim0Born = (int) pd.allPairs().stream().filter(p -> p.dimension() == 0).count();
        int dim1Born = (int) pd.allPairs().stream().filter(p -> p.dimension() == 1).count();

        int betti0 = (int) pd.infinitePairs().stream().filter(p -> p.dimension() == 0).count();
        int betti1 = (int) pd.infinitePairs().stream().filter(p -> p.dimension() == 1).count();

        return new PersistenceAnalysis(
                pd, filt, tp, mp, pe,
                dim0Born, dim1Born,
                betti0, betti1,
                ss.states().size());
    }
}
