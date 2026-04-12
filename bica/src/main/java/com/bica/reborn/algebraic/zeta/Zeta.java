package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Zeta matrix analysis for session type lattices (port of Python {@code reticulate.zeta}).
 *
 * <p>The zeta matrix Z of a finite poset P encodes the order relation:
 * {@code Z[x,y] = 1} iff {@code x >= y} (i.e. {@code y} is reachable from {@code x}).
 *
 * <p>This class is a faithful Java port of the full Python module: rank, height,
 * width, intervals, chain counting, Kronecker composition, and the incidence
 * algebra (zeta/delta/Moebius convolution). All computations use exact integer
 * arithmetic ({@code int[][]}) so results can be used as-is by the tropical
 * family (tropical determinant, max-plus paths, tropical eigen).
 *
 * <p>All methods are static. Matrices are {@code int[][]}. Interval keys use
 * {@link Pair}. See {@code reticulate/reticulate/zeta.py} for semantic reference.
 */
public final class Zeta {

    private Zeta() {}

    // =======================================================================
    // Result types
    // =======================================================================

    /** Integer (x, y) pair usable as a Map key. */
    public record Pair(int x, int y) {}

    /** Complete zeta analysis result. */
    public record ZetaResult(
            int numStates,
            int[][] zeta,
            List<Integer> states,
            int height,
            int width,
            Map<Integer, Integer> rank,
            int numComparablePairs,
            int numIntervals,
            double density,
            Map<Integer, Integer> chainCounts,
            boolean isGraded,
            boolean isRanked) {}

    /** Information about a single interval [x, y]. */
    public record IntervalInfo(
            int top,
            int bottom,
            Set<Integer> elements,
            int size,
            int chains,
            int height) {}

    /** Result of verifying Kronecker composition under parallel. */
    public record ZetaCompositionResult(
            int leftStates,
            int rightStates,
            int productStates,
            boolean kroneckerMatches,
            double densityLeft,
            double densityRight,
            double densityProduct,
            double densityProductExpected) {}

    // =======================================================================
    // Helpers
    // =======================================================================

    /** Sorted list of state IDs for consistent indexing. */
    public static List<Integer> stateList(StateSpace ss) {
        List<Integer> list = new ArrayList<>(ss.states());
        Collections.sort(list);
        return list;
    }

    /** Adjacency list from transitions (dedup targets, preserve first-seen order). */
    public static Map<Integer, List<Integer>> adjacency(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            List<Integer> out = adj.get(t.source());
            if (!out.contains(t.target())) {
                out.add(t.target());
            }
        }
        return adj;
    }

    /** Transitive closure: reach[s] = all states reachable from s (including s). */
    public static Map<Integer, Set<Integer>> reachability(StateSpace ss) {
        Map<Integer, List<Integer>> adj = adjacency(ss);
        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (!visited.add(u)) continue;
                for (int v : adj.getOrDefault(u, List.of())) {
                    stack.push(v);
                }
            }
            reach.put(s, visited);
        }
        return reach;
    }

    /**
     * SCC decomposition (Tarjan's algorithm, iterative).
     * Returns an array of length 2: [sccMap, sccMembers] where
     * sccMap: state -> representative (min ID in SCC);
     * sccMembers: representative -> set of members.
     */
    public static SccResult computeSccs(StateSpace ss) {
        Map<Integer, List<Integer>> adj = adjacency(ss);
        int[] indexCounter = {0};
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> onStack = new HashSet<>();
        Map<Integer, Integer> index = new HashMap<>();
        Map<Integer, Integer> lowlink = new HashMap<>();
        Map<Integer, Integer> sccMap = new HashMap<>();
        Map<Integer, Set<Integer>> sccMembers = new HashMap<>();

        List<Integer> sortedStates = new ArrayList<>(ss.states());
        Collections.sort(sortedStates);

        for (int start : sortedStates) {
            if (index.containsKey(start)) continue;
            // work stack: pairs (v, childIdx)
            Deque<int[]> work = new ArrayDeque<>();
            work.push(new int[]{start, 0});

            while (!work.isEmpty()) {
                int[] frame = work.peek();
                int v = frame[0];
                int ci = frame[1];

                if (!index.containsKey(v)) {
                    index.put(v, indexCounter[0]);
                    lowlink.put(v, indexCounter[0]);
                    indexCounter[0]++;
                    stack.push(v);
                    onStack.add(v);
                }

                List<Integer> neighbors = adj.getOrDefault(v, List.of());
                boolean recurse = false;
                for (int i = ci; i < neighbors.size(); i++) {
                    int w = neighbors.get(i);
                    if (!index.containsKey(w)) {
                        frame[1] = i + 1;
                        work.push(new int[]{w, 0});
                        recurse = true;
                        break;
                    } else if (onStack.contains(w)) {
                        lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
                    }
                }
                if (recurse) continue;

                if (lowlink.get(v).intValue() == index.get(v).intValue()) {
                    Set<Integer> scc = new HashSet<>();
                    while (true) {
                        int w = stack.pop();
                        onStack.remove(w);
                        scc.add(w);
                        if (w == v) break;
                    }
                    int rep = Collections.min(scc);
                    for (int w : scc) sccMap.put(w, rep);
                    sccMembers.put(rep, scc);
                }
                work.pop();
                if (!work.isEmpty()) {
                    int parent = work.peek()[0];
                    lowlink.put(parent, Math.min(lowlink.get(parent), lowlink.get(v)));
                }
            }
        }

        return new SccResult(sccMap, sccMembers);
    }

    /** Pair of SCC map and members. */
    public record SccResult(Map<Integer, Integer> sccMap, Map<Integer, Set<Integer>> sccMembers) {}

    /** Covering relation: pairs (x, y) with x &gt; y and no intermediate z. */
    public static List<Pair> coveringRelation(StateSpace ss) {
        List<Integer> states = stateList(ss);
        Map<Integer, Set<Integer>> reach = reachability(ss);
        List<Pair> covers = new ArrayList<>();
        for (int x : states) {
            for (int y : states) {
                if (x == y) continue;
                if (!reach.get(x).contains(y)) continue;
                boolean isCover = true;
                for (int z : states) {
                    if (z == x || z == y) continue;
                    if (reach.get(x).contains(z) && reach.get(z).contains(y)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) covers.add(new Pair(x, y));
            }
        }
        return covers;
    }

    // =======================================================================
    // Zeta matrix
    // =======================================================================

    /** Compute the zeta matrix Z[i][j] = 1 if state_i &ge; state_j. */
    public static int[][] zetaMatrix(StateSpace ss) {
        List<Integer> states = stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        Map<Integer, Set<Integer>> reach = reachability(ss);

        int[][] Z = new int[n][n];
        for (int s : states) {
            for (int t : reach.get(s)) {
                Z[idx.get(s)][idx.get(t)] = 1;
            }
        }
        return Z;
    }

    /** Integer matrix multiplication (returns new matrix). */
    public static int[][] matMul(int[][] A, int[][] B) {
        int n = A.length;
        int p = B.length;
        int m = (p == 0) ? 0 : B[0].length;
        int[][] C = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < p; k++) {
                int aik = A[i][k];
                if (aik == 0) continue;
                for (int j = 0; j < m; j++) {
                    C[i][j] += aik * B[k][j];
                }
            }
        }
        return C;
    }

    /** Compute Z^k by fast exponentiation. */
    public static int[][] zetaPower(int[][] Z, int k) {
        int n = Z.length;
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) result[i][i] = 1;
        int[][] base = new int[n][];
        for (int i = 0; i < n; i++) base[i] = Z[i].clone();
        int exp = k;
        while (exp > 0) {
            if ((exp & 1) == 1) result = matMul(result, base);
            base = matMul(base, base);
            exp >>= 1;
        }
        return result;
    }

    // =======================================================================
    // Rank, height, width, graded
    // =======================================================================

    /** Rank function: rank(x) = length of longest chain from x to bottom. */
    public static Map<Integer, Integer> computeRank(StateSpace ss) {
        int bottom = ss.bottom();
        SccResult sccR = computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        Map<Integer, List<Integer>> adj = adjacency(ss);

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);
        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new HashSet<>());
        for (int s : ss.states()) {
            for (int t : adj.get(s)) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                if (sr != tr) qAdj.get(sr).add(tr);
            }
        }
        int bottomRep = sccMap.get(bottom);

        Map<Integer, Integer> qRank = new HashMap<>();
        for (int r : reps) {
            longestPath(r, bottomRep, qAdj, qRank, new HashSet<>());
        }

        Map<Integer, Integer> result = new HashMap<>();
        for (int s : ss.states()) {
            result.put(s, qRank.getOrDefault(sccMap.get(s), 0));
        }
        return result;
    }

    private static int longestPath(
            int r, int bottomRep,
            Map<Integer, Set<Integer>> qAdj,
            Map<Integer, Integer> qRank,
            Set<Integer> visited) {
        if (qRank.containsKey(r)) return qRank.get(r);
        if (r == bottomRep) {
            qRank.put(r, 0);
            return 0;
        }
        if (visited.contains(r)) return -1;
        visited.add(r);
        int best = -1;
        for (int t : qAdj.getOrDefault(r, Set.of())) {
            int d = longestPath(t, bottomRep, qAdj, qRank, visited);
            if (d >= 0) best = Math.max(best, d + 1);
        }
        visited.remove(r);
        qRank.put(r, Math.max(best, 0));
        return qRank.get(r);
    }

    /** Poset height = rank of top. */
    public static int computeHeight(StateSpace ss) {
        Map<Integer, Integer> rank = computeRank(ss);
        return rank.getOrDefault(ss.top(), 0);
    }

    /** Poset width via Dilworth/König: n - max bipartite matching on (i &lt; j) graph. */
    public static int computeWidth(StateSpace ss) {
        List<Integer> states = stateList(ss);
        int n = states.size();
        if (n <= 1) return n;
        Map<Integer, Set<Integer>> reach = reachability(ss);

        // Bipartite graph: edge i -> j if states[j] is reachable from states[i] (strict >).
        Map<Integer, List<Integer>> graph = new HashMap<>();
        for (int i = 0; i < n; i++) graph.put(i, new ArrayList<>());
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                int si = states.get(i);
                int sj = states.get(j);
                if (si != sj && reach.get(si).contains(sj)) {
                    graph.get(i).add(j);
                }
            }
        }

        Map<Integer, Integer> matchRight = new HashMap<>();
        int matches = 0;
        for (int u = 0; u < n; u++) {
            if (augment(u, graph, matchRight, new HashSet<>())) matches++;
        }
        return n - matches;
    }

    private static boolean augment(
            int u,
            Map<Integer, List<Integer>> graph,
            Map<Integer, Integer> matchRight,
            Set<Integer> visited) {
        for (int v : graph.get(u)) {
            if (!visited.add(v)) continue;
            if (!matchRight.containsKey(v) || augment(matchRight.get(v), graph, matchRight, visited)) {
                matchRight.put(v, u);
                return true;
            }
        }
        return false;
    }

    /** A poset is graded iff covering x &gt; y implies rank(x) = rank(y) + 1. */
    public static boolean checkGraded(StateSpace ss) {
        List<Pair> covers = coveringRelation(ss);
        Map<Integer, Integer> rank = computeRank(ss);
        for (Pair c : covers) {
            if (rank.get(c.x()) != rank.get(c.y()) + 1) return false;
        }
        return true;
    }

    // =======================================================================
    // Intervals
    // =======================================================================

    /** Compute interval [x, y] = {z : x &ge; z &ge; y}. */
    public static Set<Integer> computeInterval(StateSpace ss, int x, int y) {
        Map<Integer, Set<Integer>> reach = reachability(ss);
        if (!reach.get(x).contains(y)) return Set.of();
        Set<Integer> result = new HashSet<>();
        for (int z : ss.states()) {
            if (reach.get(x).contains(z) && reach.get(z).contains(y)) {
                result.add(z);
            }
        }
        return result;
    }

    /** Detailed information about the interval [x, y]. */
    public static IntervalInfo intervalInfo(StateSpace ss, int x, int y) {
        Set<Integer> elements = computeInterval(ss, x, y);
        if (elements.isEmpty()) {
            return new IntervalInfo(x, y, Set.of(), 0, 0, 0);
        }
        // Sub-adjacency within interval
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : elements) adj.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            if (elements.contains(t.source()) && elements.contains(t.target())) {
                List<Integer> out = adj.get(t.source());
                if (!out.contains(t.target())) out.add(t.target());
            }
        }
        // Count paths from x to y, tracking max length
        int[] chainCount = {0};
        int[] maxLength = {0};
        countChains(x, y, 0, adj, chainCount, maxLength);
        return new IntervalInfo(x, y, elements, elements.size(), chainCount[0], maxLength[0]);
    }

    private static void countChains(
            int s, int target, int length,
            Map<Integer, List<Integer>> adj,
            int[] chainCount, int[] maxLength) {
        if (s == target) {
            chainCount[0]++;
            if (length > maxLength[0]) maxLength[0] = length;
            return;
        }
        for (int t : adj.getOrDefault(s, List.of())) {
            countChains(t, target, length + 1, adj, chainCount, maxLength);
        }
    }

    /** Enumerate all non-trivial intervals [x, y] with x &gt; y. */
    public static List<IntervalInfo> enumerateIntervals(StateSpace ss) {
        List<Integer> states = stateList(ss);
        Map<Integer, Set<Integer>> reach = reachability(ss);
        List<IntervalInfo> result = new ArrayList<>();
        for (int x : states) {
            for (int y : states) {
                if (x != y && reach.get(x).contains(y)) {
                    IntervalInfo info = intervalInfo(ss, x, y);
                    if (info.size() > 0) result.add(info);
                }
            }
        }
        return result;
    }

    // =======================================================================
    // Chain counts (Hasse matrix powers)
    // =======================================================================

    /** Count chains of each length from top to bottom via Hasse matrix powers. */
    public static Map<Integer, Integer> chainCounts(StateSpace ss) {
        List<Integer> states = stateList(ss);
        int n = states.size();
        if (n == 0) return Map.of();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        int top = ss.top();
        int bottom = ss.bottom();
        int topI = idx.get(top);
        int bottomI = idx.get(bottom);

        // Hasse (covering) matrix
        List<Pair> covers = coveringRelation(ss);
        int[][] H = new int[n][n];
        for (Pair c : covers) H[idx.get(c.x())][idx.get(c.y())] = 1;

        Map<Integer, Integer> counts = new LinkedHashMap<>();
        int[][] power = new int[n][];
        for (int i = 0; i < n; i++) power[i] = H[i].clone();

        for (int k = 1; k <= n; k++) {
            int val = power[topI][bottomI];
            if (val > 0) counts.put(k, val);
            boolean allZero = true;
            outer:
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (power[i][j] != 0) { allZero = false; break outer; }
                }
            }
            if (allZero) break;
            power = matMul(power, H);
        }
        return counts;
    }

    // =======================================================================
    // Density, comparable pairs
    // =======================================================================

    /** Order density: fraction of pairs (i, j) with Z[i][j] = 1. */
    public static double orderDensity(int[][] Z) {
        int n = Z.length;
        if (n == 0) return 0.0;
        int total = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                total += Z[i][j];
        return (double) total / ((double) n * n);
    }

    /** Ordered pairs (i, j) with i &ne; j and Z[i][j] = 1 (strict off-diagonal). */
    public static int countComparablePairs(int[][] Z) {
        int n = Z.length;
        int sum = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (i != j) sum += Z[i][j];
        return sum;
    }

    // =======================================================================
    // Kronecker composition
    // =======================================================================

    /** Kronecker (tensor) product A &otimes; B (square inputs). */
    public static int[][] kroneckerProduct(int[][] A, int[][] B) {
        int na = A.length;
        int nb = B.length;
        int n = na * nb;
        int[][] C = new int[n][n];
        for (int i = 0; i < na; i++) {
            for (int j = 0; j < na; j++) {
                int aij = A[i][j];
                for (int k = 0; k < nb; k++) {
                    for (int l = 0; l < nb; l++) {
                        C[i * nb + k][j * nb + l] = aij * B[k][l];
                    }
                }
            }
        }
        return C;
    }

    /** Verify Z(product) = Z(left) &otimes; Z(right). */
    public static ZetaCompositionResult verifyKroneckerComposition(
            StateSpace ssLeft, StateSpace ssRight, StateSpace ssProduct) {
        int[][] Zl = zetaMatrix(ssLeft);
        int[][] Zr = zetaMatrix(ssRight);
        int[][] Zp = zetaMatrix(ssProduct);
        int[][] Ze = kroneckerProduct(Zl, Zr);

        int nl = Zl.length;
        int nr = Zr.length;
        int np = Zp.length;

        boolean matches = (np == nl * nr);
        if (matches) {
            outer:
            for (int i = 0; i < np; i++) {
                for (int j = 0; j < np; j++) {
                    if (Zp[i][j] != Ze[i][j]) { matches = false; break outer; }
                }
            }
        }

        double dl = orderDensity(Zl);
        double dr = orderDensity(Zr);
        double dp = orderDensity(Zp);
        return new ZetaCompositionResult(nl, nr, np, matches, dl, dr, dp, dl * dr);
    }

    // =======================================================================
    // Matrix properties
    // =======================================================================

    /** True iff Z is upper-triangular when rows/cols are ordered by descending rank. */
    public static boolean isUpperTriangular(int[][] Z, List<Integer> states, Map<Integer, Integer> rank) {
        int n = states.size();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Integer.compare(rank.get(states.get(b)), rank.get(states.get(a))));
        for (int rowPos = 0; rowPos < n; rowPos++) {
            int i = order[rowPos];
            for (int colPos = 0; colPos < n; colPos++) {
                int j = order[colPos];
                if (rowPos > colPos && Z[i][j] != 0) return false;
            }
        }
        return true;
    }

    /** Trace = sum of diagonal = number of states. */
    public static int zetaTrace(int[][] Z) {
        int n = Z.length;
        int s = 0;
        for (int i = 0; i < n; i++) s += Z[i][i];
        return s;
    }

    /** Row sums keyed by state: row_sum(x) = |down-set of x|. */
    public static Map<Integer, Integer> zetaRowSums(int[][] Z, List<Integer> states) {
        int n = Z.length;
        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int j = 0; j < n; j++) sum += Z[i][j];
            result.put(states.get(i), sum);
        }
        return result;
    }

    /** Column sums keyed by state: col_sum(y) = |up-set of y|. */
    public static Map<Integer, Integer> zetaColSums(int[][] Z, List<Integer> states) {
        int n = Z.length;
        Map<Integer, Integer> result = new HashMap<>();
        for (int j = 0; j < n; j++) {
            int sum = 0;
            for (int i = 0; i < n; i++) sum += Z[i][j];
            result.put(states.get(j), sum);
        }
        return result;
    }

    // =======================================================================
    // Incidence algebra
    // =======================================================================

    /** Zeta function &zeta;(x, y) = 1 if x &ge; y (SCC-aware). */
    public static Map<Pair, Integer> zetaFunction(StateSpace ss) {
        SccResult sccR = computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> qReach = quotientReachability(ss, sccR);

        Map<Pair, Integer> result = new HashMap<>();
        for (int x : ss.states()) {
            int xr = sccMap.get(x);
            for (int y : ss.states()) {
                int yr = sccMap.get(y);
                if (qReach.get(xr).contains(yr)) {
                    result.put(new Pair(x, y), 1);
                }
            }
        }
        return result;
    }

    /** Delta function &delta;(x, y) = 1 iff x, y in same SCC. */
    public static Map<Pair, Integer> deltaFunction(StateSpace ss) {
        SccResult sccR = computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Pair, Integer> result = new HashMap<>();
        for (int s : ss.states()) {
            for (int t : ss.states()) {
                if (sccMap.get(s).intValue() == sccMap.get(t).intValue()) {
                    result.put(new Pair(s, t), 1);
                }
            }
        }
        return result;
    }

    private static Map<Integer, Set<Integer>> quotientReachability(StateSpace ss, SccResult sccR) {
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        Map<Integer, List<Integer>> adj = adjacency(ss);
        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new HashSet<>());
        for (int s : ss.states()) {
            for (int t : adj.get(s)) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                if (sr != tr) qAdj.get(sr).add(tr);
            }
        }

        Map<Integer, Set<Integer>> qReach = new HashMap<>();
        for (int r : reps) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(r);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (!visited.add(u)) continue;
                for (int v : qAdj.get(u)) stack.push(v);
            }
            qReach.put(r, visited);
        }
        return qReach;
    }

    /** Moebius function &mu;(x, y) — inverse of &zeta; in the incidence algebra. */
    public static Map<Pair, Integer> mobiusFunction(StateSpace ss) {
        SccResult sccR = computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        Map<Integer, List<Integer>> adj = adjacency(ss);

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new HashSet<>());
        for (int s : ss.states()) {
            for (int t : adj.get(s)) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                if (sr != tr) qAdj.get(sr).add(tr);
            }
        }
        Map<Integer, Set<Integer>> qReach = quotientReachability(ss, sccR);

        int bottom = ss.bottom();
        int bottomRep = sccMap.getOrDefault(bottom, reps.isEmpty() ? 0 : reps.get(0));
        Map<Integer, Integer> qRank = new HashMap<>();
        for (int r : reps) longestPath(r, bottomRep, qAdj, qRank, new HashSet<>());

        Map<Pair, Integer> qMu = new HashMap<>();
        for (int x : reps) {
            qMu.put(new Pair(x, x), 1);
            List<Integer> reachable = new ArrayList<>();
            for (int r : reps) {
                if (r != x && qReach.get(x).contains(r)) reachable.add(r);
            }
            reachable.sort((a, b) -> Integer.compare(
                    qRank.getOrDefault(b, 0),
                    qRank.getOrDefault(a, 0)));
            for (int y : reachable) {
                int total = 0;
                for (int z : reps) {
                    if (z == y) continue;
                    if (qReach.get(x).contains(z) && qReach.get(z).contains(y)) {
                        total += qMu.getOrDefault(new Pair(x, z), 0);
                    }
                }
                qMu.put(new Pair(x, y), -total);
            }
        }

        Map<Pair, Integer> mu = new HashMap<>();
        for (int s : ss.states()) {
            for (int t : ss.states()) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                int val = qMu.getOrDefault(new Pair(sr, tr), 0);
                if (s == t) {
                    mu.put(new Pair(s, t), 1);
                } else if (val != 0) {
                    mu.put(new Pair(s, t), val);
                }
            }
        }
        return mu;
    }

    /** Convolution in the incidence algebra: (f * g)(x, y) = &Sigma; f(x,z) &middot; g(z,y). */
    public static Map<Pair, Integer> convolve(
            Map<Pair, Integer> f, Map<Pair, Integer> g, StateSpace ss) {
        SccResult sccR = computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        Map<Integer, Set<Integer>> qReach = quotientReachability(ss, sccR);
        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        Map<Pair, Integer> result = new HashMap<>();
        for (int x : ss.states()) {
            int xr = sccMap.get(x);
            for (int y : ss.states()) {
                int yr = sccMap.get(y);
                if (!qReach.get(xr).contains(yr)) continue;
                int total = 0;
                for (int zr : reps) {
                    if (qReach.get(xr).contains(zr) && qReach.get(zr).contains(yr)) {
                        int z = Collections.min(sccMembers.get(zr));
                        int fval = f.getOrDefault(new Pair(x, z), 0);
                        int gval = g.getOrDefault(new Pair(z, y), 0);
                        total += fval * gval;
                    }
                }
                if (total != 0) result.put(new Pair(x, y), total);
            }
        }
        return result;
    }

    // =======================================================================
    // Top-level analyser
    // =======================================================================

    /** Complete zeta analysis of a state space. */
    public static ZetaResult analyzeZeta(StateSpace ss) {
        int[][] Z = zetaMatrix(ss);
        List<Integer> states = stateList(ss);
        int n = states.size();

        Map<Integer, Integer> rank = computeRank(ss);
        int height = computeHeight(ss);
        int width = computeWidth(ss);
        int comparable = countComparablePairs(Z);
        double density = orderDensity(Z);
        Map<Integer, Integer> chains = chainCounts(ss);
        boolean graded = checkGraded(ss);

        Map<Integer, Set<Integer>> reach = reachability(ss);
        int numIntervals = 0;
        for (int x : states) {
            for (int y : states) {
                if (x != y && reach.get(x).contains(y)) numIntervals++;
            }
        }

        return new ZetaResult(
                n, Z, states, height, width, rank, comparable, numIntervals,
                density, chains, graded, graded);
    }
}
