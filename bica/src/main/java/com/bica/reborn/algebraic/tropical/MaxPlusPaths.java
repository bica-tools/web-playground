package com.bica.reborn.algebraic.tropical;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Max-plus longest paths for session type lattices (port of Python
 * {@code reticulate.max_plus_paths}, Step 30n).
 *
 * <p>Higher-level path analyses built on top of {@link Tropical}: longest
 * and shortest top&#x2192;bottom paths, critical path reconstruction,
 * all-pairs longest paths, path width (= Dilworth width), path counting
 * with edge multiplicities, bottleneck paths, geodesic check, and full
 * path-length histograms. All computations work on the SCC quotient DAG
 * so that recursive session types (with cycles) are handled correctly.
 *
 * <p>Tropical zero is {@link Double#NEGATIVE_INFINITY}, consistent with
 * the rest of the tropical family.
 */
public final class MaxPlusPaths {

    public static final double INF = Double.POSITIVE_INFINITY;
    public static final double NEG_INF = Double.NEGATIVE_INFINITY;

    private MaxPlusPaths() {}

    // =======================================================================
    // Result type
    // =======================================================================

    /** Complete max-plus path analysis. */
    public record MaxPlusPathResult(
            int longestPath,
            int shortestPath,
            List<Integer> criticalPath,
            double[][] allPairsLongest,
            int diameter,
            int width,
            long pathCountTopBottom,
            double bottleneckValue,
            boolean isGeodesic) {}

    // =======================================================================
    // Quotient DAG helper
    // =======================================================================

    private record Quotient(
            List<Integer> reps,
            Map<Integer, Integer> sccMap,
            Map<Integer, Set<Integer>> sccMembers,
            Map<Integer, List<Integer>> qAdj,
            int topRep,
            int botRep) {}

    private static Quotient quotientDag(StateSpace ss) {
        Zeta.SccResult scc = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = scc.sccMap();
        Map<Integer, Set<Integer>> sccMembers = scc.sccMembers();
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        java.util.Collections.sort(reps);

        Map<Integer, List<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new ArrayList<>());
        Set<Long> seen = new HashSet<>();
        for (int s : ss.states()) {
            int sr = sccMap.get(s);
            for (int t : adj.getOrDefault(s, List.of())) {
                int tr = sccMap.get(t);
                if (sr != tr) {
                    long key = ((long) sr << 32) | (tr & 0xffffffffL);
                    if (seen.add(key)) qAdj.get(sr).add(tr);
                }
            }
        }
        int topRep = sccMap.get(ss.top());
        int botRep = sccMap.get(ss.bottom());
        return new Quotient(reps, sccMap, sccMembers, qAdj, topRep, botRep);
    }

    private static List<Integer> topologicalSort(
            List<Integer> reps, Map<Integer, List<Integer>> qAdj) {
        Map<Integer, Integer> inDeg = new HashMap<>();
        for (int r : reps) inDeg.put(r, 0);
        for (int r : reps) {
            for (int t : qAdj.getOrDefault(r, List.of())) {
                inDeg.merge(t, 1, Integer::sum);
            }
        }
        List<Integer> queue = new ArrayList<>();
        for (int r : reps) if (inDeg.get(r) == 0) queue.add(r);
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            java.util.Collections.sort(queue);
            int node = queue.remove(0);
            order.add(node);
            for (int t : qAdj.getOrDefault(node, List.of())) {
                int nd = inDeg.get(t) - 1;
                inDeg.put(t, nd);
                if (nd == 0) queue.add(t);
            }
        }
        return order;
    }

    // =======================================================================
    // Longest path
    // =======================================================================

    /** Length of longest path from top to bottom in the SCC quotient DAG. */
    public static int longestPathLength(StateSpace ss) {
        Quotient q = quotientDag(ss);
        if (q.topRep == q.botRep) return 0;
        List<Integer> topo = topologicalSort(q.reps, q.qAdj);
        Map<Integer, Integer> dist = new HashMap<>();
        for (int r : q.reps) dist.put(r, -1);
        dist.put(q.topRep, 0);
        for (int r : topo) {
            int dr = dist.get(r);
            if (dr < 0) continue;
            for (int t : q.qAdj.getOrDefault(r, List.of())) {
                if (dr + 1 > dist.get(t)) dist.put(t, dr + 1);
            }
        }
        return Math.max(dist.get(q.botRep), 0);
    }

    // =======================================================================
    // Shortest path
    // =======================================================================

    /** Length of shortest path from top to bottom (BFS on quotient DAG). */
    public static int shortestPathLength(StateSpace ss) {
        Quotient q = quotientDag(ss);
        if (q.topRep == q.botRep) return 0;
        Set<Integer> visited = new HashSet<>();
        visited.add(q.topRep);
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{q.topRep, 0});
        while (!queue.isEmpty()) {
            int[] cur = queue.pollFirst();
            for (int t : q.qAdj.getOrDefault(cur[0], List.of())) {
                if (t == q.botRep) return cur[1] + 1;
                if (visited.add(t)) queue.add(new int[]{t, cur[1] + 1});
            }
        }
        return 0;
    }

    // =======================================================================
    // Critical path
    // =======================================================================

    /** Longest path as a sequence of original state IDs from top to bottom. */
    public static List<Integer> criticalPath(StateSpace ss) {
        Quotient q = quotientDag(ss);
        if (q.topRep == q.botRep) {
            List<Integer> single = new ArrayList<>();
            single.add(ss.top());
            return single;
        }
        List<Integer> topo = topologicalSort(q.reps, q.qAdj);
        Map<Integer, Integer> dist = new HashMap<>();
        Map<Integer, Integer> pred = new HashMap<>();
        for (int r : q.reps) dist.put(r, -1);
        dist.put(q.topRep, 0);
        for (int r : topo) {
            int dr = dist.get(r);
            if (dr < 0) continue;
            for (int t : q.qAdj.getOrDefault(r, List.of())) {
                if (dr + 1 > dist.get(t)) {
                    dist.put(t, dr + 1);
                    pred.put(t, r);
                }
            }
        }
        if (dist.get(q.botRep) < 0) {
            List<Integer> single = new ArrayList<>();
            single.add(ss.top());
            return single;
        }
        List<Integer> repPath = new ArrayList<>();
        int cur = q.botRep;
        while (cur != q.topRep) {
            repPath.add(cur);
            cur = pred.get(cur);
        }
        repPath.add(q.topRep);
        java.util.Collections.reverse(repPath);

        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);
        List<Integer> result = new ArrayList<>();
        for (int rep : repPath) {
            if (rep == q.topRep) {
                result.add(ss.top());
            } else if (rep == q.botRep) {
                result.add(ss.bottom());
            } else {
                List<Integer> members = new ArrayList<>(q.sccMembers.get(rep));
                java.util.Collections.sort(members);
                Integer chosen = null;
                if (!result.isEmpty()) {
                    int prev = result.get(result.size() - 1);
                    List<Integer> prevAdj = adj.getOrDefault(prev, List.of());
                    for (int m : members) {
                        if (prevAdj.contains(m)) { chosen = m; break; }
                    }
                }
                if (chosen == null) chosen = members.get(0);
                result.add(chosen);
            }
        }
        return result;
    }

    // =======================================================================
    // All-pairs longest paths
    // =======================================================================

    /** Modified Floyd-Warshall for longest paths on the original state space. */
    public static double[][] allPairsLongestPaths(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        double[][] L = new double[n][n];
        for (double[] row : L) Arrays.fill(row, NEG_INF);
        for (int i = 0; i < n; i++) L[i][i] = 0.0;

        for (var t : ss.transitions()) {
            Integer si = idx.get(t.source());
            Integer ti = idx.get(t.target());
            if (si == null || ti == null) continue;
            if (1.0 > L[si][ti]) L[si][ti] = 1.0;
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (L[i][k] <= NEG_INF) continue;
                for (int j = 0; j < n; j++) {
                    if (L[k][j] <= NEG_INF) continue;
                    double cand = L[i][k] + L[k][j];
                    if (cand > L[i][j]) L[i][j] = cand;
                }
            }
        }
        return L;
    }

    // =======================================================================
    // Path width (Dilworth)
    // =======================================================================

    /** Maximum antichain size. Delegates to {@link Zeta#computeWidth(StateSpace)}. */
    public static int pathWidth(StateSpace ss) {
        return Zeta.computeWidth(ss);
    }

    // =======================================================================
    // Path counting
    // =======================================================================

    /** Number of distinct directed paths from {@code src} to {@code tgt}. */
    public static long countPaths(StateSpace ss, int src, int tgt) {
        if (src == tgt) return 1L;
        Zeta.SccResult scc = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = scc.sccMap();
        Map<Integer, Set<Integer>> sccMembers = scc.sccMembers();
        int srcRep = sccMap.get(src);
        int tgtRep = sccMap.get(tgt);
        if (srcRep == tgtRep) return 1L;

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        java.util.Collections.sort(reps);

        Map<Integer, Set<Integer>> qAdjSet = new HashMap<>();
        Map<Long, Long> qEdgeCount = new HashMap<>();
        for (int r : reps) qAdjSet.put(r, new HashSet<>());
        for (var t : ss.transitions()) {
            int sr = sccMap.get(t.source());
            int tr = sccMap.get(t.target());
            if (sr != tr) {
                qAdjSet.get(sr).add(tr);
                long key = ((long) sr << 32) | (tr & 0xffffffffL);
                qEdgeCount.merge(key, 1L, Long::sum);
            }
        }
        Map<Integer, List<Integer>> qAdj = new HashMap<>();
        for (int r : reps) {
            List<Integer> sorted = new ArrayList<>(qAdjSet.get(r));
            java.util.Collections.sort(sorted);
            qAdj.put(r, sorted);
        }

        List<Integer> topo = topologicalSort(reps, qAdj);
        Map<Integer, Long> cnt = new HashMap<>();
        for (int r : reps) cnt.put(r, 0L);
        cnt.put(srcRep, 1L);
        for (int r : topo) {
            long cr = cnt.get(r);
            if (cr == 0L) continue;
            for (int t : qAdj.get(r)) {
                long key = ((long) r << 32) | (t & 0xffffffffL);
                long mult = qEdgeCount.getOrDefault(key, 1L);
                cnt.put(t, cnt.get(t) + cr * mult);
            }
        }
        return cnt.get(tgtRep);
    }

    /** Total number of distinct top&#x2192;bottom paths. */
    public static long totalPathsTopBottom(StateSpace ss) {
        return countPaths(ss, ss.top(), ss.bottom());
    }

    // =======================================================================
    // Bottleneck path
    // =======================================================================

    /** Bottleneck path record: value + state sequence. */
    public record BottleneckPath(double value, List<Integer> path) {}

    /** Bottleneck path: max of min edge weight along a top&#x2192;bottom path. */
    public static BottleneckPath bottleneckPath(StateSpace ss) {
        Quotient q = quotientDag(ss);
        if (q.topRep == q.botRep) {
            List<Integer> p = new ArrayList<>();
            p.add(ss.top());
            return new BottleneckPath(0.0, p);
        }

        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> pred = new HashMap<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(q.topRep);
        boolean found = false;
        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (!visited.add(node)) continue;
            if (node == q.botRep) { found = true; break; }
            for (int t : q.qAdj.getOrDefault(node, List.of())) {
                if (!visited.contains(t)) {
                    pred.put(t, node);
                    stack.push(t);
                }
            }
        }
        if (!found) {
            List<Integer> p = new ArrayList<>();
            p.add(ss.top());
            return new BottleneckPath(0.0, p);
        }

        List<Integer> repPath = new ArrayList<>();
        int cur = q.botRep;
        while (cur != q.topRep) {
            repPath.add(cur);
            cur = pred.get(cur);
        }
        repPath.add(q.topRep);
        java.util.Collections.reverse(repPath);

        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);
        List<Integer> result = new ArrayList<>();
        for (int rep : repPath) {
            if (rep == q.topRep) {
                result.add(ss.top());
            } else if (rep == q.botRep) {
                result.add(ss.bottom());
            } else {
                List<Integer> members = new ArrayList<>(q.sccMembers.get(rep));
                java.util.Collections.sort(members);
                Integer chosen = null;
                if (!result.isEmpty()) {
                    int prev = result.get(result.size() - 1);
                    List<Integer> prevAdj = adj.getOrDefault(prev, List.of());
                    for (int m : members) {
                        if (prevAdj.contains(m)) { chosen = m; break; }
                    }
                }
                if (chosen == null) chosen = members.get(0);
                result.add(chosen);
            }
        }
        return new BottleneckPath(1.0, result);
    }

    // =======================================================================
    // Path histogram + geodesic
    // =======================================================================

    /** Distribution of top&#x2192;bottom path lengths: length &#x2192; count. */
    public static Map<Integer, Long> pathHistogram(StateSpace ss) {
        Zeta.SccResult scc = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = scc.sccMap();
        Map<Integer, Set<Integer>> sccMembers = scc.sccMembers();
        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        java.util.Collections.sort(reps);
        int topRep = sccMap.get(ss.top());
        int botRep = sccMap.get(ss.bottom());

        if (topRep == botRep) {
            Map<Integer, Long> res = new LinkedHashMap<>();
            res.put(0, 1L);
            return res;
        }

        Map<Integer, Set<Integer>> qAdjSet = new HashMap<>();
        Map<Long, Long> qEdgeCount = new HashMap<>();
        for (int r : reps) qAdjSet.put(r, new HashSet<>());
        for (var t : ss.transitions()) {
            int sr = sccMap.get(t.source());
            int tr = sccMap.get(t.target());
            if (sr != tr) {
                qAdjSet.get(sr).add(tr);
                long key = ((long) sr << 32) | (tr & 0xffffffffL);
                qEdgeCount.merge(key, 1L, Long::sum);
            }
        }
        Map<Integer, List<Integer>> qAdj = new HashMap<>();
        for (int r : reps) {
            List<Integer> sorted = new ArrayList<>(qAdjSet.get(r));
            java.util.Collections.sort(sorted);
            qAdj.put(r, sorted);
        }

        List<Integer> topo = topologicalSort(reps, qAdj);
        Map<Integer, Map<Integer, Long>> lengthCounts = new HashMap<>();
        for (int r : reps) lengthCounts.put(r, new HashMap<>());
        lengthCounts.get(topRep).put(0, 1L);

        for (int r : topo) {
            Map<Integer, Long> lc = lengthCounts.get(r);
            if (lc.isEmpty()) continue;
            for (int t : qAdj.get(r)) {
                long key = ((long) r << 32) | (t & 0xffffffffL);
                long mult = qEdgeCount.getOrDefault(key, 1L);
                Map<Integer, Long> dst = lengthCounts.get(t);
                for (var e : lc.entrySet()) {
                    int newLen = e.getKey() + 1;
                    dst.merge(newLen, e.getValue() * mult, Long::sum);
                }
            }
        }
        Map<Integer, Long> result = lengthCounts.get(botRep);
        return new TreeMap<>(result);
    }

    /** True iff every top&#x2192;bottom path has the same length. */
    public static boolean isGeodesic(StateSpace ss) {
        return pathHistogram(ss).size() <= 1;
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    /** Complete max-plus path analysis. */
    public static MaxPlusPathResult analyzeMaxPlusPaths(StateSpace ss) {
        int longest = longestPathLength(ss);
        int shortest = shortestPathLength(ss);
        List<Integer> crit = criticalPath(ss);
        double[][] apl = allPairsLongestPaths(ss);

        int diam = 0;
        for (double[] row : apl) {
            for (double v : row) {
                if (v > NEG_INF && v < INF && (int) v > diam) diam = (int) v;
            }
        }
        int w = pathWidth(ss);
        long pc = totalPathsTopBottom(ss);
        BottleneckPath bp = bottleneckPath(ss);
        boolean geo = isGeodesic(ss);

        return new MaxPlusPathResult(
                longest, shortest, crit, apl, diam, w, pc, bp.value(), geo);
    }
}
