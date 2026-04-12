package com.bica.reborn.livelock;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Livelock detection via tropical analysis for session types (Step 80c).
 *
 * <p>A livelock occurs when a protocol enters a cycle from which it can never
 * reach the terminal state (bottom). Unlike deadlock, where no transitions
 * are enabled, livelocked states have outgoing transitions but all paths
 * loop back without ever reaching completion.
 *
 * <p>Detection strategy:
 * <ol>
 *   <li><b>SCC decomposition</b> -- compute strongly connected components</li>
 *   <li><b>Exit analysis</b> -- check whether each non-trivial SCC can reach bottom</li>
 *   <li><b>Trapped SCCs</b> -- an SCC with no exit path to bottom is a livelock</li>
 *   <li><b>Tropical eigenvalue</b> -- max-plus eigenvalue on trapped SCC adjacency</li>
 *   <li><b>Heat kernel indicator</b> -- diagonal H_t(s,s) approaches 1.0 for trapped states</li>
 * </ol>
 */
public final class LivelockChecker {

    private LivelockChecker() {}

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * A single livelocked strongly connected component.
     *
     * @param states             the states in the trapped SCC
     * @param entryTransitions   transitions from outside that enter this SCC
     * @param tropicalEigenvalue max cycle mean within this SCC (greater than 0 means cycling)
     * @param cycleLabels        labels that appear on transitions within the SCC
     */
    public record LivelockSCC(
            Set<Integer> states,
            List<int[]> entryTransitions,
            double tropicalEigenvalue,
            List<String> cycleLabels) {
        public LivelockSCC {
            Objects.requireNonNull(states, "states must not be null");
            Objects.requireNonNull(entryTransitions, "entryTransitions must not be null");
            Objects.requireNonNull(cycleLabels, "cycleLabels must not be null");
            states = Set.copyOf(states);
            entryTransitions = List.copyOf(entryTransitions);
            cycleLabels = List.copyOf(cycleLabels);
        }
    }

    /**
     * Heat kernel based livelock indicator for a single state.
     *
     * @param state         the state ID
     * @param diagonalValue H_t(state, state) at the given time
     * @param isTrapped     true if diagonal value is greater than 0.5
     */
    public record HeatKernelIndicator(int state, double diagonalValue, boolean isTrapped) {}

    /**
     * Comprehensive livelock analysis result.
     *
     * @param isLivelockFree        true iff no trapped SCCs exist
     * @param livelocked            all states that are part of trapped SCCs
     * @param trappedSccs           details of each trapped SCC
     * @param numTrappedSccs        number of trapped SCCs
     * @param totalTrappedStates    total states in trapped SCCs
     * @param maxTropicalEigenvalue maximum tropical eigenvalue across trapped SCCs
     * @param heatIndicators        heat kernel indicators per livelocked state
     * @param reachableFromTop      livelocked states reachable from top
     * @param numStates             total states in the state space
     * @param numTransitions        total transitions
     * @param summary               human-readable summary
     */
    public record LivelockResult(
            boolean isLivelockFree,
            Set<Integer> livelocked,
            List<LivelockSCC> trappedSccs,
            int numTrappedSccs,
            int totalTrappedStates,
            double maxTropicalEigenvalue,
            List<HeatKernelIndicator> heatIndicators,
            Set<Integer> reachableFromTop,
            int numStates,
            int numTransitions,
            String summary) {
        public LivelockResult {
            Objects.requireNonNull(livelocked, "livelocked must not be null");
            Objects.requireNonNull(trappedSccs, "trappedSccs must not be null");
            Objects.requireNonNull(heatIndicators, "heatIndicators must not be null");
            Objects.requireNonNull(reachableFromTop, "reachableFromTop must not be null");
            Objects.requireNonNull(summary, "summary must not be null");
            livelocked = Set.copyOf(livelocked);
            trappedSccs = List.copyOf(trappedSccs);
            heatIndicators = List.copyOf(heatIndicators);
            reachableFromTop = Set.copyOf(reachableFromTop);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Return outgoing (label, target) pairs for a state. */
    private static List<Map.Entry<String, Integer>> outgoing(StateSpace ss, int state) {
        var result = new ArrayList<Map.Entry<String, Integer>>();
        for (var t : ss.transitions()) {
            if (t.source() == state) {
                result.add(Map.entry(t.label(), t.target()));
            }
        }
        return result;
    }

    /** States that can reach target via reverse edges. */
    private static Set<Integer> reverseReachable(StateSpace ss, int target) {
        Map<Integer, List<Integer>> rev = new HashMap<>();
        for (int s : ss.states()) rev.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            rev.computeIfAbsent(t.target(), k -> new ArrayList<>()).add(t.source());
        }
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(target);
        queue.add(target);
        while (!queue.isEmpty()) {
            int s = queue.poll();
            for (int pred : rev.getOrDefault(s, List.of())) {
                if (visited.add(pred)) {
                    queue.add(pred);
                }
            }
        }
        return visited;
    }

    /** States reachable from start via forward edges. */
    private static Set<Integer> reachableFrom(StateSpace ss, int start) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            int s = queue.poll();
            for (var entry : outgoing(ss, s)) {
                int t = entry.getValue();
                if (visited.add(t)) {
                    queue.add(t);
                }
            }
        }
        return visited;
    }

    /** Compute SCCs using iterative Tarjan's algorithm. */
    private static List<Set<Integer>> computeSccs(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>()).add(t.target());
        }

        int[] indexCounter = {0};
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> onStack = new HashSet<>();
        Map<Integer, Integer> index = new HashMap<>();
        Map<Integer, Integer> lowlink = new HashMap<>();
        List<Set<Integer>> result = new ArrayList<>();

        List<Integer> sortedStates = new ArrayList<>(ss.states());
        Collections.sort(sortedStates);

        for (int start : sortedStates) {
            if (index.containsKey(start)) continue;

            Deque<int[]> callStack = new ArrayDeque<>();
            index.put(start, indexCounter[0]);
            lowlink.put(start, indexCounter[0]);
            indexCounter[0]++;
            stack.push(start);
            onStack.add(start);
            callStack.push(new int[]{start, 0});

            while (!callStack.isEmpty()) {
                int[] frame = callStack.peek();
                int v = frame[0];
                int ni = frame[1];
                List<Integer> neighbors = adj.getOrDefault(v, List.of());

                if (ni < neighbors.size()) {
                    frame[1] = ni + 1;
                    int w = neighbors.get(ni);

                    if (!index.containsKey(w)) {
                        index.put(w, indexCounter[0]);
                        lowlink.put(w, indexCounter[0]);
                        indexCounter[0]++;
                        stack.push(w);
                        onStack.add(w);
                        callStack.push(new int[]{w, 0});
                    } else if (onStack.contains(w)) {
                        lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
                    }
                } else {
                    if (lowlink.get(v).equals(index.get(v))) {
                        Set<Integer> sccMembers = new HashSet<>();
                        while (true) {
                            int w = stack.pop();
                            onStack.remove(w);
                            sccMembers.add(w);
                            if (w == v) break;
                        }
                        result.add(sccMembers);
                    }

                    callStack.pop();
                    if (!callStack.isEmpty()) {
                        int parent = callStack.peek()[0];
                        lowlink.put(parent,
                                Math.min(lowlink.get(parent), lowlink.get(v)));
                    }
                }
            }
        }

        return result;
    }

    /** Check if an SCC is non-trivial (has cycles: size > 1 or has self-loop). */
    private static boolean isNontrivialScc(Set<Integer> scc, StateSpace ss) {
        if (scc.size() > 1) return true;
        int s = scc.iterator().next();
        for (var t : ss.transitions()) {
            if (t.source() == s && t.target() == s) return true;
        }
        return false;
    }

    /** Check if any state in SCC can reach bottom via a path exiting the SCC. */
    private static boolean sccCanReachBottom(Set<Integer> scc, StateSpace ss,
                                              Set<Integer> bottomReachable) {
        // If bottom is inside the SCC, it can trivially reach bottom.
        if (scc.contains(ss.bottom())) return true;
        for (int s : scc) {
            for (var entry : outgoing(ss, s)) {
                int t = entry.getValue();
                if (!scc.contains(t) && bottomReachable.contains(t)) return true;
            }
        }
        return false;
    }

    /**
     * Compute the tropical (max-plus) eigenvalue on a subgraph.
     * The tropical eigenvalue is the maximum cycle mean.
     * For unit-weight edges, this is 1.0 if any cycle exists, 0.0 otherwise.
     * Uses Karp's algorithm.
     */
    private static double tropicalEigenvalueOnSubgraph(Set<Integer> states, StateSpace ss) {
        if (states.size() <= 1) {
            int s = states.iterator().next();
            for (var t : ss.transitions()) {
                if (t.source() == s && t.target() == s) return 1.0;
            }
            return 0.0;
        }

        List<Integer> stateList = new ArrayList<>(states);
        Collections.sort(stateList);
        int n = stateList.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(stateList.get(i), i);

        // Build adjacency within the SCC
        @SuppressWarnings("unchecked")
        List<Integer>[] adj = new List[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
        for (var t : ss.transitions()) {
            if (states.contains(t.source()) && states.contains(t.target())) {
                adj[idx.get(t.source())].add(idx.get(t.target()));
            }
        }

        // Karp's algorithm: D[k][v] = max weight of path of exactly k edges ending at v
        double NEG_INF = Double.NEGATIVE_INFINITY;
        double[][] D = new double[n + 1][n];
        for (double[] row : D) Arrays.fill(row, NEG_INF);

        // Start from all vertices
        for (int s = 0; s < n; s++) D[0][s] = 0.0;

        for (int k = 1; k <= n; k++) {
            for (int u = 0; u < n; u++) {
                if (D[k - 1][u] == NEG_INF) continue;
                for (int v : adj[u]) {
                    double val = D[k - 1][u] + 1.0;
                    if (val > D[k][v]) D[k][v] = val;
                }
            }
        }

        // Compute max cycle mean
        double maxMean = NEG_INF;
        for (int v = 0; v < n; v++) {
            if (D[n][v] == NEG_INF) continue;
            double minRatio = Double.POSITIVE_INFINITY;
            for (int k = 0; k < n; k++) {
                if (D[k][v] == NEG_INF) continue;
                double ratio = (D[n][v] - D[k][v]) / (n - k);
                minRatio = Math.min(minRatio, ratio);
            }
            if (minRatio != Double.POSITIVE_INFINITY) {
                maxMean = Math.max(maxMean, minRatio);
            }
        }

        return maxMean == NEG_INF ? 0.0 : maxMean;
    }

    /**
     * Compute heat kernel diagonal H_t(s,s) for states of interest.
     * Uses Taylor series approximation of exp(-tL) where L is the graph Laplacian.
     */
    private static Map<Integer, Double> heatKernelDiagonal(
            StateSpace ss, Set<Integer> statesOfInterest, double t) {
        if (statesOfInterest.isEmpty() || ss.states().isEmpty()) return Map.of();

        List<Integer> stateList = new ArrayList<>(statesOfInterest);
        Collections.sort(stateList);
        int n = stateList.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(stateList.get(i), i);

        if (n == 0) return Map.of();

        // Build symmetric adjacency for Laplacian
        double[][] A = new double[n][n];
        for (var tr : ss.transitions()) {
            if (statesOfInterest.contains(tr.source()) && statesOfInterest.contains(tr.target())) {
                int si = idx.get(tr.source());
                int ti = idx.get(tr.target());
                A[si][ti] = 1.0;
                A[ti][si] = 1.0; // symmetrize
            }
        }

        // Degree and Laplacian
        double[] deg = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) deg[i] += A[i][j];
        }

        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                L[i][j] = (i == j) ? deg[i] - A[i][j] : -A[i][j];
            }
        }

        // -tL
        double[][] tL = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tL[i][j] = -t * L[i][j];
            }
        }

        // Taylor: H = I + tL + (tL)^2/2! + ...
        double[][] H = new double[n][n];
        for (int i = 0; i < n; i++) H[i][i] = 1.0;

        double[][] term = new double[n][n];
        for (int i = 0; i < n; i++) term[i][i] = 1.0;

        for (int k = 1; k < 20; k++) {
            double[][] newTerm = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    for (int p = 0; p < n; p++) {
                        newTerm[i][j] += term[i][p] * tL[p][j];
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    newTerm[i][j] /= k;
                }
            }
            term = newTerm;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    H[i][j] += term[i][j];
                }
            }
        }

        Map<Integer, Double> result = new HashMap<>();
        for (int s : statesOfInterest) {
            if (idx.containsKey(s)) {
                int i = idx.get(s);
                result.put(s, Math.max(0.0, Math.min(1.0, H[i][i])));
            } else {
                result.put(s, 0.0);
            }
        }
        return result;
    }

    /** Get transition labels within an SCC. */
    private static List<String> getSccLabels(Set<Integer> scc, StateSpace ss) {
        Set<String> labels = new TreeSet<>();
        for (var t : ss.transitions()) {
            if (scc.contains(t.source()) && scc.contains(t.target())) {
                labels.add(t.label());
            }
        }
        return List.copyOf(labels);
    }

    /** Get transitions from outside the SCC into it. */
    private static List<int[]> getEntryTransitions(Set<Integer> scc, StateSpace ss) {
        List<int[]> entries = new ArrayList<>();
        for (var t : ss.transitions()) {
            if (!scc.contains(t.source()) && scc.contains(t.target())) {
                entries.add(new int[]{t.source(), t.target()});
            }
        }
        entries.sort(Comparator.<int[], Integer>comparing(a -> a[0]).thenComparing(a -> a[1]));
        return entries;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Find all livelocked states: states in SCCs with no exit to bottom.
     *
     * <p>A state is livelocked if it belongs to a non-trivial SCC (has cycles)
     * and no state in that SCC can reach bottom.
     *
     * @param ss the state space to analyze
     * @return set of livelocked state IDs
     */
    public static Set<Integer> detectLivelock(StateSpace ss) {
        List<Set<Integer>> sccs = computeSccs(ss);
        Set<Integer> bottomReachable = reverseReachable(ss, ss.bottom());

        Set<Integer> livelocked = new HashSet<>();
        for (var scc : sccs) {
            if (!isNontrivialScc(scc, ss)) continue;
            if (!sccCanReachBottom(scc, ss, bottomReachable)) {
                livelocked.addAll(scc);
            }
        }
        return Set.copyOf(livelocked);
    }

    /**
     * Return the SCCs that are livelocked (trapped cycles).
     *
     * <p>An SCC is livelocked if it is non-trivial (has cycles) and no state
     * in it can reach bottom via a path that exits the SCC.
     *
     * @param ss the state space to analyze
     * @return list of trapped SCCs, each a set of state IDs
     */
    public static List<Set<Integer>> livelockSccs(StateSpace ss) {
        List<Set<Integer>> sccs = computeSccs(ss);
        Set<Integer> bottomReachable = reverseReachable(ss, ss.bottom());

        List<Set<Integer>> trapped = new ArrayList<>();
        for (var scc : sccs) {
            if (!isNontrivialScc(scc, ss)) continue;
            if (!sccCanReachBottom(scc, ss, bottomReachable)) {
                trapped.add(Set.copyOf(scc));
            }
        }
        return List.copyOf(trapped);
    }

    /**
     * Compute tropical eigenvalue on trapped SCCs.
     *
     * <p>Returns the maximum tropical eigenvalue across all trapped SCCs.
     * A positive score (greater than 0) indicates livelock: there is cyclic activity
     * with no progress toward termination.
     *
     * @param ss the state space to analyze
     * @return maximum tropical eigenvalue, or 0.0 if livelock-free
     */
    public static double tropicalLivelockScore(StateSpace ss) {
        List<Set<Integer>> trapped = livelockSccs(ss);
        if (trapped.isEmpty()) return 0.0;

        double maxEigenvalue = 0.0;
        for (var scc : trapped) {
            double ev = tropicalEigenvalueOnSubgraph(scc, ss);
            maxEigenvalue = Math.max(maxEigenvalue, ev);
        }
        return maxEigenvalue;
    }

    /**
     * Heat kernel based livelock indicators.
     *
     * <p>For each livelocked state, compute H_t(s,s). A value close to 1.0
     * indicates that probability mass stays trapped at that state.
     *
     * @param ss the state space to analyze
     * @param t  time parameter for the heat kernel
     * @return list of heat kernel indicators for each livelocked state
     */
    public static List<HeatKernelIndicator> heatKernelLivelock(StateSpace ss, double t) {
        Set<Integer> livelocked = detectLivelock(ss);
        if (livelocked.isEmpty()) return List.of();

        Map<Integer, Double> diag = heatKernelDiagonal(ss, livelocked, t);

        List<HeatKernelIndicator> indicators = new ArrayList<>();
        List<Integer> sorted = new ArrayList<>(livelocked);
        Collections.sort(sorted);
        for (int s : sorted) {
            double val = diag.getOrDefault(s, 0.0);
            indicators.add(new HeatKernelIndicator(s, val, val > 0.5));
        }
        return List.copyOf(indicators);
    }

    /**
     * Return true iff no trapped SCCs exist.
     *
     * <p>A state space is livelock-free if every non-trivial SCC has at least
     * one exit path to the bottom state.
     *
     * @param ss the state space to check
     * @return true if livelock-free
     */
    public static boolean isLivelockFree(StateSpace ss) {
        return livelockSccs(ss).isEmpty();
    }

    /**
     * Comprehensive livelock analysis combining all techniques.
     *
     * <p>Performs SCC decomposition, exit analysis, tropical eigenvalue
     * computation, and heat kernel analysis.
     *
     * @param ss the state space to analyze
     * @return complete livelock analysis result
     */
    public static LivelockResult analyzeLivelock(StateSpace ss) {
        List<Set<Integer>> sccs = computeSccs(ss);
        Set<Integer> bottomReachable = reverseReachable(ss, ss.bottom());
        Set<Integer> topReachable = reachableFrom(ss, ss.top());

        List<LivelockSCC> trappedSccList = new ArrayList<>();
        Set<Integer> allLivelocked = new HashSet<>();

        for (var scc : sccs) {
            if (!isNontrivialScc(scc, ss)) continue;
            if (sccCanReachBottom(scc, ss, bottomReachable)) continue;

            // This is a trapped SCC
            allLivelocked.addAll(scc);
            double ev = tropicalEigenvalueOnSubgraph(scc, ss);
            List<String> labels = getSccLabels(scc, ss);
            List<int[]> entries = getEntryTransitions(scc, ss);

            trappedSccList.add(new LivelockSCC(scc, entries, ev, labels));
        }

        Set<Integer> reachableLivelocked = new HashSet<>(allLivelocked);
        reachableLivelocked.retainAll(topReachable);

        // Heat kernel analysis
        List<HeatKernelIndicator> heatIndicators = new ArrayList<>();
        if (!allLivelocked.isEmpty()) {
            Map<Integer, Double> diag = heatKernelDiagonal(ss, allLivelocked, 10.0);
            List<Integer> sorted = new ArrayList<>(allLivelocked);
            Collections.sort(sorted);
            for (int s : sorted) {
                double val = diag.getOrDefault(s, 0.0);
                heatIndicators.add(new HeatKernelIndicator(s, val, val > 0.5));
            }
        }

        double maxEv = trappedSccList.stream()
                .mapToDouble(LivelockSCC::tropicalEigenvalue)
                .max()
                .orElse(0.0);

        boolean isFree = trappedSccList.isEmpty();

        String summary;
        if (isFree) {
            summary = "Livelock-free: all SCCs have exit paths to bottom.";
        } else {
            summary = String.format(
                    "LIVELOCK DETECTED: %d trapped SCC(s) containing %d state(s). "
                            + "Max tropical eigenvalue: %.2f. "
                            + "%d livelocked state(s) reachable from top.",
                    trappedSccList.size(), allLivelocked.size(),
                    maxEv, reachableLivelocked.size());
        }

        return new LivelockResult(
                isFree,
                allLivelocked,
                trappedSccList,
                trappedSccList.size(),
                allLivelocked.size(),
                maxEv,
                heatIndicators,
                reachableLivelocked,
                ss.states().size(),
                ss.transitions().size(),
                summary);
    }
}
