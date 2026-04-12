package com.bica.reborn.lgv;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.algebraic.zeta.Zeta.SccResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * LGV (Lindstrom-Gessel-Viennot) Lemma analysis for session type lattices.
 *
 * <p>The LGV Lemma relates the determinant of a path-count matrix to the
 * signed count of non-intersecting path systems in a directed acyclic graph.
 *
 * <p>For a DAG with sources s_1,...,s_n and sinks t_1,...,t_n, define
 * M[i,j] = number of directed paths from s_i to t_j. The LGV Lemma states:
 * <pre>
 *   det(M) = sum_{sigma in S_n} sgn(sigma) * (# non-intersecting path systems
 *            from (s_1,...,s_n) to (t_{sigma(1)},...,t_{sigma(n)}))
 * </pre>
 *
 * <p>Session type state spaces -- after SCC quotient -- are DAGs. This class
 * applies the LGV Lemma to these DAGs, using rank layers to identify natural
 * source and sink sets.
 *
 * <p>All computations use exact integer arithmetic (no floating point).
 *
 * <p>Port of Python {@code reticulate.lgv} (Step 30ac).
 */
public final class LgvChecker {

    private LgvChecker() {}

    // -----------------------------------------------------------------------
    // Result type
    // -----------------------------------------------------------------------

    /**
     * Complete LGV Lemma analysis result.
     *
     * @param pathMatrix          M[i][j] = number of directed paths from source_i to sink_j
     * @param determinant         det(M), computed via Leibniz/Bareiss
     * @param numSources          number of source states used
     * @param numSinks            number of sink states used
     * @param sources             list of source state IDs
     * @param sinks               list of sink state IDs
     * @param lgvVerified         true iff det(M) equals the signed non-intersecting count
     * @param numNonIntersecting  count of non-intersecting path systems (signed sum)
     */
    public record LgvResult(
            int[][] pathMatrix,
            int determinant,
            int numSources,
            int numSinks,
            List<Integer> sources,
            List<Integer> sinks,
            boolean lgvVerified,
            int numNonIntersecting) {}

    // -----------------------------------------------------------------------
    // Topological order (Kahn's algorithm)
    // -----------------------------------------------------------------------

    /**
     * Compute a topological ordering of states using Kahn's algorithm.
     * Ties broken by state ID for determinism.
     */
    static List<Integer> topologicalOrder(Map<Integer, List<Integer>> adj, Set<Integer> states) {
        Map<Integer, Integer> inDeg = new HashMap<>();
        for (int s : states) inDeg.put(s, 0);
        for (int s : states) {
            for (int t : adj.getOrDefault(s, List.of())) {
                if (states.contains(t)) {
                    inDeg.merge(t, 1, Integer::sum);
                }
            }
        }

        PriorityQueue<Integer> queue = new PriorityQueue<>();
        for (int s : states) {
            if (inDeg.get(s) == 0) queue.add(s);
        }

        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order.add(u);
            for (int v : adj.getOrDefault(u, List.of())) {
                if (states.contains(v)) {
                    int deg = inDeg.get(v) - 1;
                    inDeg.put(v, deg);
                    if (deg == 0) queue.add(v);
                }
            }
        }
        return order;
    }

    // -----------------------------------------------------------------------
    // Path counting (DP on DAG)
    // -----------------------------------------------------------------------

    /**
     * Count directed paths from src to tgt in a DAG using DP.
     * dp[tgt] = 1, dp[v] = sum(dp[w] for w in adj[v]).
     */
    static int countPathsDag(Map<Integer, List<Integer>> adj, Set<Integer> states, int src, int tgt) {
        if (!states.contains(src) || !states.contains(tgt)) return 0;

        List<Integer> topo = topologicalOrder(adj, states);
        Map<Integer, Integer> dp = new HashMap<>();
        for (int s : states) dp.put(s, 0);
        dp.put(tgt, 1);

        // Process in reverse topological order
        for (int i = topo.size() - 1; i >= 0; i--) {
            int v = topo.get(i);
            if (v == tgt) continue;
            int total = 0;
            for (int w : adj.getOrDefault(v, List.of())) {
                if (states.contains(w)) {
                    total += dp.get(w);
                }
            }
            dp.put(v, total);
        }
        return dp.getOrDefault(src, 0);
    }

    /**
     * Count directed paths from src to tgt in a state space.
     * Quotients by SCCs first to obtain a DAG.
     */
    public static int countPaths(StateSpace ss, int src, int tgt) {
        SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);

        // Build quotient DAG
        Set<Integer> reps = sccMembers.keySet();
        Map<Integer, List<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new ArrayList<>());
        Set<long[]> seenEdges = new HashSet<>(); // use a set of (sr,tr) pairs
        Map<Long, Boolean> seen = new HashMap<>();
        for (int s : ss.states()) {
            for (int t : adj.getOrDefault(s, List.of())) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                if (sr != tr) {
                    long key = ((long) sr << 32) | (tr & 0xFFFFFFFFL);
                    if (!seen.containsKey(key)) {
                        qAdj.get(sr).add(tr);
                        seen.put(key, true);
                    }
                }
            }
        }

        int srcRep = sccMap.getOrDefault(src, src);
        int tgtRep = sccMap.getOrDefault(tgt, tgt);
        return countPathsDag(qAdj, new HashSet<>(reps), srcRep, tgtRep);
    }

    // -----------------------------------------------------------------------
    // Path count matrix
    // -----------------------------------------------------------------------

    /**
     * Build path-count matrix M[i][j] = #paths from sources[i] to sinks[j].
     */
    public static int[][] pathCountMatrix(StateSpace ss, List<Integer> sources, List<Integer> sinks) {
        int n = sources.size();
        int m = sinks.size();
        int[][] M = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                M[i][j] = countPaths(ss, sources.get(i), sinks.get(j));
            }
        }
        return M;
    }

    // -----------------------------------------------------------------------
    // Determinant (Leibniz for small, Bareiss for larger)
    // -----------------------------------------------------------------------

    /**
     * Sign of a permutation: +1 for even, -1 for odd.
     */
    public static int permSign(int[] perm) {
        int n = perm.length;
        boolean[] visited = new boolean[n];
        int sign = 1;
        for (int i = 0; i < n; i++) {
            if (visited[i]) continue;
            int j = i;
            int cycleLen = 0;
            while (!visited[j]) {
                visited[j] = true;
                j = perm[j];
                cycleLen++;
            }
            if (cycleLen % 2 == 0) sign = -sign;
        }
        return sign;
    }

    /**
     * Determinant via Leibniz formula (permutation expansion).
     * Exact for integers, O(n!) complexity. Only suitable for small n.
     */
    static int determinantLeibniz(int[][] matrix) {
        int n = matrix.length;
        if (n == 0) return 0;
        if (n == 1) return matrix[0][0];

        int total = 0;
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;

        // Generate all permutations iteratively (Heap's algorithm)
        do {
            int sgn = permSign(perm);
            int prod = 1;
            for (int i = 0; i < n; i++) {
                prod *= matrix[i][perm[i]];
                if (prod == 0) break;
            }
            total += sgn * prod;
        } while (nextPermutation(perm));

        return total;
    }

    /**
     * Generate next lexicographic permutation in-place.
     * Returns false when wrapped back to identity.
     */
    private static boolean nextPermutation(int[] arr) {
        int n = arr.length;
        int i = n - 2;
        while (i >= 0 && arr[i] >= arr[i + 1]) i--;
        if (i < 0) return false;
        int j = n - 1;
        while (arr[j] <= arr[i]) j--;
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        // reverse from i+1 to end
        int lo = i + 1, hi = n - 1;
        while (lo < hi) {
            tmp = arr[lo]; arr[lo] = arr[hi]; arr[hi] = tmp;
            lo++; hi--;
        }
        return true;
    }

    /**
     * Determinant via Bareiss algorithm (exact integer division).
     */
    static int determinantBareiss(int[][] matrix) {
        int n = matrix.length;
        if (n == 0) return 0;
        if (n == 1) return matrix[0][0];
        if (n == 2) return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];

        // Work on copy
        int[][] M = new int[n][];
        for (int i = 0; i < n; i++) M[i] = matrix[i].clone();
        int sign = 1;

        for (int k = 0; k < n - 1; k++) {
            // Find pivot
            int pivotRow = -1;
            for (int i = k; i < n; i++) {
                if (M[i][k] != 0) { pivotRow = i; break; }
            }
            if (pivotRow == -1) return 0;

            if (pivotRow != k) {
                int[] tmp = M[k]; M[k] = M[pivotRow]; M[pivotRow] = tmp;
                sign = -sign;
            }

            for (int i = k + 1; i < n; i++) {
                for (int j = k + 1; j < n; j++) {
                    M[i][j] = M[k][k] * M[i][j] - M[i][k] * M[k][j];
                    if (k > 0) {
                        M[i][j] /= M[k - 1][k - 1];
                    }
                }
                M[i][k] = 0;
            }
        }
        return sign * M[n - 1][n - 1];
    }

    /**
     * Safe determinant: Leibniz for small matrices (n <= 6), Bareiss otherwise.
     */
    public static int determinant(int[][] matrix) {
        int n = matrix.length;
        if (n == 0) return 0;
        if (n == 1) return matrix[0][0];
        if (n == 2) return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
        if (n <= 6) return determinantLeibniz(matrix);
        return determinantBareiss(matrix);
    }

    // -----------------------------------------------------------------------
    // LGV determinant
    // -----------------------------------------------------------------------

    /**
     * Compute det(M) where M is the path-count matrix.
     *
     * @throws IllegalArgumentException if sources and sinks have different lengths
     */
    public static int lgvDeterminant(StateSpace ss, List<Integer> sources, List<Integer> sinks) {
        if (sources.size() != sinks.size()) {
            throw new IllegalArgumentException(
                    "sources and sinks must have same length, got "
                    + sources.size() + " and " + sinks.size());
        }
        int[][] M = pathCountMatrix(ss, sources, sinks);
        return determinant(M);
    }

    // -----------------------------------------------------------------------
    // Rank layers (source/sink detection)
    // -----------------------------------------------------------------------

    /**
     * Find states at each rank level.
     * Returns rank -> sorted list of state IDs. Rank 0 = bottom, max rank = top.
     */
    public static Map<Integer, List<Integer>> findRankLayers(StateSpace ss) {
        Map<Integer, Integer> ranks = Zeta.computeRank(ss);
        Map<Integer, List<Integer>> layers = new TreeMap<>();
        for (var entry : ranks.entrySet()) {
            layers.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        for (var list : layers.values()) {
            Collections.sort(list);
        }
        return layers;
    }

    // -----------------------------------------------------------------------
    // Quotient DAG builder (shared helper)
    // -----------------------------------------------------------------------

    private static QuotientDag buildQuotientDag(StateSpace ss) {
        SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);

        Set<Integer> reps = sccMembers.keySet();
        Map<Integer, List<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new ArrayList<>());
        Map<Long, Boolean> seen = new HashMap<>();
        for (int s : ss.states()) {
            for (int t : adj.getOrDefault(s, List.of())) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                if (sr != tr) {
                    long key = ((long) sr << 32) | (tr & 0xFFFFFFFFL);
                    if (!seen.containsKey(key)) {
                        qAdj.get(sr).add(tr);
                        seen.put(key, true);
                    }
                }
            }
        }
        return new QuotientDag(sccMap, reps, qAdj);
    }

    private record QuotientDag(
            Map<Integer, Integer> sccMap,
            Set<Integer> reps,
            Map<Integer, List<Integer>> qAdj) {}

    // -----------------------------------------------------------------------
    // All paths enumeration in quotient DAG
    // -----------------------------------------------------------------------

    private static List<List<Integer>> allPaths(int srcRep, int tgtRep,
                                                 Map<Integer, List<Integer>> qAdj) {
        if (srcRep == tgtRep) {
            List<List<Integer>> result = new ArrayList<>();
            result.add(List.of(srcRep));
            return result;
        }
        List<List<Integer>> result = new ArrayList<>();
        Deque<Object[]> stack = new ArrayDeque<>();
        stack.push(new Object[]{srcRep, new ArrayList<>(List.of(srcRep))});
        while (!stack.isEmpty()) {
            Object[] frame = stack.pop();
            int u = (int) frame[0];
            @SuppressWarnings("unchecked")
            List<Integer> path = (List<Integer>) frame[1];
            for (int v : qAdj.getOrDefault(u, List.of())) {
                if (v == tgtRep) {
                    List<Integer> newPath = new ArrayList<>(path);
                    newPath.add(v);
                    result.add(newPath);
                } else if (!path.contains(v)) {
                    List<Integer> newPath = new ArrayList<>(path);
                    newPath.add(v);
                    stack.push(new Object[]{v, newPath});
                }
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Non-intersecting path system counting
    // -----------------------------------------------------------------------

    /**
     * Count non-intersecting (vertex-disjoint) path systems for identity permutation.
     */
    public static int nonIntersectingCount(StateSpace ss, List<Integer> sources, List<Integer> sinks) {
        int n = sources.size();
        if (n == 0) return 1;
        if (n == 1) return countPaths(ss, sources.get(0), sinks.get(0));

        QuotientDag qd = buildQuotientDag(ss);

        List<Integer> srcReps = new ArrayList<>();
        List<Integer> tgtReps = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            srcReps.add(qd.sccMap.getOrDefault(sources.get(i), sources.get(i)));
            tgtReps.add(qd.sccMap.getOrDefault(sinks.get(i), sinks.get(i)));
        }

        List<List<List<Integer>>> pathsPerPair = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pathsPerPair.add(allPaths(srcReps.get(i), tgtReps.get(i), qd.qAdj));
        }

        return countNonIntersecting(0, new HashSet<>(), pathsPerPair, n);
    }

    private static int countNonIntersecting(int idx, Set<Integer> usedVertices,
                                             List<List<List<Integer>>> pathsPerPair, int n) {
        if (idx == n) return 1;
        int total = 0;
        for (List<Integer> p : pathsPerPair.get(idx)) {
            Set<Integer> allVerts = new HashSet<>(p);
            if (!Collections.disjoint(allVerts, usedVertices)) continue;
            Set<Integer> newUsed = new HashSet<>(usedVertices);
            newUsed.addAll(allVerts);
            total += countNonIntersecting(idx + 1, newUsed, pathsPerPair, n);
        }
        return total;
    }

    // -----------------------------------------------------------------------
    // Signed non-intersecting count (full LGV sum)
    // -----------------------------------------------------------------------

    /**
     * Signed count: sum over all permutations sigma of
     * sgn(sigma) * (# non-intersecting systems from sources[i] to sinks[sigma(i)]).
     */
    static int signedNonIntersectingCount(StateSpace ss, List<Integer> sources, List<Integer> sinks) {
        int n = sources.size();
        if (n == 0) return 1;

        QuotientDag qd = buildQuotientDag(ss);
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;

        int total = 0;
        do {
            int sgn = permSign(perm);

            List<Integer> srcReps = new ArrayList<>();
            List<Integer> tgtReps = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                srcReps.add(qd.sccMap.getOrDefault(sources.get(i), sources.get(i)));
                tgtReps.add(qd.sccMap.getOrDefault(sinks.get(perm[i]), sinks.get(perm[i])));
            }

            List<List<List<Integer>>> pathsPerPair = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                pathsPerPair.add(allPaths(srcReps.get(i), tgtReps.get(i), qd.qAdj));
            }

            total += sgn * countNonIntersecting(0, new HashSet<>(), pathsPerPair, n);
        } while (nextPermutation(perm));

        return total;
    }

    // -----------------------------------------------------------------------
    // LGV verification
    // -----------------------------------------------------------------------

    /**
     * Verify the LGV Lemma: det(M) == signed non-intersecting count.
     *
     * @throws IllegalArgumentException if sources and sinks have different lengths
     */
    public static boolean verifyLgv(StateSpace ss, List<Integer> sources, List<Integer> sinks) {
        if (sources.size() != sinks.size()) {
            throw new IllegalArgumentException(
                    "sources and sinks must have same length, got "
                    + sources.size() + " and " + sinks.size());
        }
        int det = lgvDeterminant(ss, sources, sinks);
        int signedNi = signedNonIntersectingCount(ss, sources, sinks);
        return det == signedNi;
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Full LGV analysis: auto-detect sources/sinks from rank layers.
     * Sources = max rank (top layer), sinks = min rank (rank 0, bottom layer).
     */
    public static LgvResult analyzeLgv(StateSpace ss) {
        Map<Integer, List<Integer>> layers = findRankLayers(ss);
        if (layers.isEmpty()) {
            return new LgvResult(new int[0][0], 0, 0, 0,
                    List.of(), List.of(), true, 0);
        }

        int maxRank = layers.keySet().stream().mapToInt(i -> i).max().orElse(0);
        int minRank = layers.keySet().stream().mapToInt(i -> i).min().orElse(0);

        List<Integer> sources = layers.get(maxRank);
        List<Integer> sinks = layers.get(minRank);

        int n = Math.min(sources.size(), sinks.size());
        List<Integer> sourcesUsed = sources.subList(0, n);
        List<Integer> sinksUsed = sinks.subList(0, n);

        if (n == 0) {
            return new LgvResult(new int[0][0], 0, 0, 0,
                    List.of(), List.of(), true, 0);
        }

        int[][] M = pathCountMatrix(ss, sourcesUsed, sinksUsed);
        int det = determinant(M);
        int ni = signedNonIntersectingCount(ss, sourcesUsed, sinksUsed);
        boolean verified = (det == ni);

        return new LgvResult(M, det, n, n,
                List.copyOf(sourcesUsed), List.copyOf(sinksUsed), verified, ni);
    }
}
