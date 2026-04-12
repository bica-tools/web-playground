package com.bica.reborn.morse;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Discrete Morse theory for session type lattices (port of Python {@code reticulate.morse}).
 *
 * <p>Discrete Morse theory (Forman, 1998) constructs acyclic matchings on a CW complex
 * to compute homology efficiently. For a graph (1-dimensional CW complex), the cells are:
 * <ul>
 *   <li>0-cells: vertices (states)</li>
 *   <li>1-cells: edges (covering relations in the Hasse diagram)</li>
 * </ul>
 *
 * <p>All methods are static. This is a faithful port of the full Python module.
 */
public final class MorseChecker {

    private MorseChecker() {}

    // =======================================================================
    // Result types
    // =======================================================================

    /** An acyclic matching on the CW complex of the Hasse diagram. */
    public record MorseMatching(
            Set<VertexEdgePair> matchedPairs,
            Set<Integer> criticalVertices,
            Set<Integer> criticalEdges,
            int numStates,
            int numHasseEdges,
            boolean isAcyclic) {

        /** All critical cells (vertices only, for backward compat). */
        public Set<Integer> criticalCells() {
            return criticalVertices;
        }
    }

    /** A vertex-edge matched pair. */
    public record VertexEdgePair(int vertex, int edgeIndex) {}

    /** A discrete Morse function on the state space. */
    public record MorseFunction(
            Map<Integer, Double> values,
            boolean isValid) {}

    /** Gradient vector field from a Morse matching. */
    public record GradientField(
            Set<VertexEdgePair> pairs,
            Set<Integer> critical,
            Map<Integer, List<Integer>> flowGraph) {}

    /** Full Morse theory analysis result. */
    public record MorseResult(
            MorseMatching matching,
            MorseFunction function,
            GradientField gradient,
            int[] bettiNumbers,
            Set<Integer> criticalCells,
            int numCritical,
            int eulerCharacteristic,
            boolean weakMorseHolds,
            boolean strongMorseHolds) {}

    /** A directed edge in the Hasse diagram with label. */
    public record HasseLabeledEdge(int source, int target, String label) {}

    // =======================================================================
    // Internal: Reachability and SCCs
    // =======================================================================

    private static Map<Integer, Set<Integer>> buildReachability(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new HashSet<>());
        }
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new HashSet<>()).add(t.target());
        }

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int v = stack.pop();
                if (visited.add(v)) {
                    for (int tgt : adj.getOrDefault(v, Set.of())) {
                        stack.push(tgt);
                    }
                }
            }
            reach.put(s, visited);
        }
        return reach;
    }

    private static Map<Integer, Integer> computeSccs(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = buildReachability(ss);
        Map<Integer, Integer> sccMap = new HashMap<>();
        for (int s : ss.states()) {
            int minMember = Integer.MAX_VALUE;
            for (int t : reach.get(s)) {
                if (reach.getOrDefault(t, Set.of()).contains(s)) {
                    minMember = Math.min(minMember, t);
                }
            }
            sccMap.put(s, minMember);
        }
        return sccMap;
    }

    // =======================================================================
    // Internal: Hasse diagram construction
    // =======================================================================

    private static List<HasseLabeledEdge> buildCovering(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = buildReachability(ss);
        Map<Integer, Integer> scc = computeSccs(ss);

        Map<Integer, List<Map.Entry<Integer, String>>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>())
               .add(Map.entry(t.target(), t.label()));
        }

        List<HasseLabeledEdge> covers = new ArrayList<>();
        for (int s : ss.states()) {
            for (var entry : adj.getOrDefault(s, List.of())) {
                int t = entry.getKey();
                String label = entry.getValue();
                if (t == s) continue;
                if (scc.get(s).equals(scc.get(t))) continue;

                boolean isCover = true;
                for (var uEntry : adj.getOrDefault(s, List.of())) {
                    int u = uEntry.getKey();
                    if (u != t && u != s
                            && !scc.get(u).equals(scc.get(s))
                            && reach.getOrDefault(u, Set.of()).contains(t)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) {
                    covers.add(new HasseLabeledEdge(s, t, label));
                }
            }
        }
        return covers;
    }

    /** Deduplicated Hasse edges: one edge per covering pair (s, t). */
    private static List<int[]> deduplicatedHasse(StateSpace ss) {
        List<HasseLabeledEdge> covers = buildCovering(ss);
        Set<Long> seen = new HashSet<>();
        List<int[]> result = new ArrayList<>();
        for (var c : covers) {
            long key = ((long) c.source() << 32) | (c.target() & 0xFFFFFFFFL);
            if (seen.add(key)) {
                result.add(new int[]{c.source(), c.target()});
            }
        }
        return result;
    }

    /** Undirected edges from the deduplicated Hasse diagram. */
    private static Set<Long> hasseUndirected(StateSpace ss) {
        List<int[]> hasse = deduplicatedHasse(ss);
        Set<Long> edges = new HashSet<>();
        for (int[] e : hasse) {
            if (e[0] != e[1]) {
                int lo = Math.min(e[0], e[1]);
                int hi = Math.max(e[0], e[1]);
                edges.add(((long) lo << 32) | (hi & 0xFFFFFFFFL));
            }
        }
        return edges;
    }

    private static int eulerCharacteristic(StateSpace ss) {
        return ss.states().size() - hasseUndirected(ss).size();
    }

    // =======================================================================
    // Internal: Connected components
    // =======================================================================

    private static int connectedComponentsCount(Set<Integer> states, List<int[]> edges) {
        if (states.isEmpty()) return 0;

        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : states) {
            adj.put(s, new HashSet<>());
        }
        for (int[] e : edges) {
            adj.computeIfAbsent(e[0], k -> new HashSet<>()).add(e[1]);
            adj.computeIfAbsent(e[1], k -> new HashSet<>()).add(e[0]);
        }

        Set<Integer> visited = new HashSet<>();
        int count = 0;
        for (int s : states) {
            if (visited.contains(s)) continue;
            count++;
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(s);
            visited.add(s);
            while (!queue.isEmpty()) {
                int v = queue.poll();
                for (int nb : adj.getOrDefault(v, Set.of())) {
                    if (visited.add(nb)) {
                        queue.add(nb);
                    }
                }
            }
        }
        return count;
    }

    // =======================================================================
    // Internal: Acyclicity check
    // =======================================================================

    private static boolean matchingIsAcyclicCw(
            Set<Integer> states,
            List<int[]> hasseEdges,
            Map<Integer, Integer> vertexToEdge) {

        Map<Integer, Integer> gradientSucc = new HashMap<>();
        for (var entry : vertexToEdge.entrySet()) {
            int v = entry.getKey();
            int ei = entry.getValue();
            int[] edge = hasseEdges.get(ei);
            int other = (v == edge[0]) ? edge[1] : edge[0];
            gradientSucc.put(v, other);
        }

        Set<Integer> visited = new HashSet<>();
        for (int start : states) {
            if (!vertexToEdge.containsKey(start) || visited.contains(start)) continue;

            Set<Integer> path = new HashSet<>();
            Integer current = start;
            while (current != null && vertexToEdge.containsKey(current)) {
                if (path.contains(current)) return false; // cycle
                if (visited.contains(current)) break;
                path.add(current);
                current = gradientSucc.get(current);
            }
            visited.addAll(path);
        }
        return true;
    }

    // =======================================================================
    // Public API: Hasse graph
    // =======================================================================

    /**
     * Return edges of the Hasse diagram as labeled triples.
     * Source covers target (source > target in the poset ordering).
     */
    public static List<HasseLabeledEdge> hasseGraph(StateSpace ss) {
        return buildCovering(ss);
    }

    // =======================================================================
    // Public API: Greedy acyclic matching
    // =======================================================================

    /**
     * Compute a greedy acyclic matching on the CW complex.
     * Pairs vertices (0-cells) with incident edges (1-cells).
     */
    public static MorseMatching greedyMatching(StateSpace ss) {
        List<int[]> hasseEdges = deduplicatedHasse(ss);
        int numEdges = hasseEdges.size();

        // Incident edge indices per vertex
        Map<Integer, List<Integer>> incident = new HashMap<>();
        for (int s : ss.states()) {
            incident.put(s, new ArrayList<>());
        }
        for (int i = 0; i < numEdges; i++) {
            int[] e = hasseEdges.get(i);
            incident.computeIfAbsent(e[0], k -> new ArrayList<>()).add(i);
            incident.computeIfAbsent(e[1], k -> new ArrayList<>()).add(i);
        }

        Map<Integer, Integer> degree = new HashMap<>();
        for (int s : ss.states()) {
            degree.put(s, incident.getOrDefault(s, List.of()).size());
        }

        Set<Integer> matchedVertices = new HashSet<>();
        Set<Integer> matchedEdgeIndices = new HashSet<>();
        Set<VertexEdgePair> matchedPairs = new HashSet<>();
        Map<Integer, Integer> vertexToEdge = new HashMap<>();

        // Sort edge indices by minimum endpoint degree
        Integer[] edgeOrder = new Integer[numEdges];
        for (int i = 0; i < numEdges; i++) edgeOrder[i] = i;
        Arrays.sort(edgeOrder, (a, b) -> {
            int degA = Math.min(degree.getOrDefault(hasseEdges.get(a)[0], 0),
                                degree.getOrDefault(hasseEdges.get(a)[1], 0));
            int degB = Math.min(degree.getOrDefault(hasseEdges.get(b)[0], 0),
                                degree.getOrDefault(hasseEdges.get(b)[1], 0));
            return Integer.compare(degA, degB);
        });

        for (int ei : edgeOrder) {
            if (matchedEdgeIndices.contains(ei)) continue;
            int[] edge = hasseEdges.get(ei);
            int u = edge[0], v = edge[1];

            // Try matching with lower endpoint (v) first, then upper (u)
            for (int vertex : new int[]{v, u}) {
                if (matchedVertices.contains(vertex)) continue;

                Map<Integer, Integer> candidateV2e = new HashMap<>(vertexToEdge);
                candidateV2e.put(vertex, ei);
                if (matchingIsAcyclicCw(ss.states(), hasseEdges, candidateV2e)) {
                    matchedPairs.add(new VertexEdgePair(vertex, ei));
                    matchedVertices.add(vertex);
                    matchedEdgeIndices.add(ei);
                    vertexToEdge.put(vertex, ei);
                    break;
                }
            }
        }

        Set<Integer> critVerts = new HashSet<>(ss.states());
        critVerts.removeAll(matchedVertices);

        Set<Integer> critEdges = new HashSet<>();
        for (int i = 0; i < numEdges; i++) {
            if (!matchedEdgeIndices.contains(i)) critEdges.add(i);
        }

        return new MorseMatching(
                Set.copyOf(matchedPairs),
                Set.copyOf(critVerts),
                Set.copyOf(critEdges),
                ss.states().size(),
                numEdges,
                true // acyclic by construction
        );
    }

    // =======================================================================
    // Public API: Critical cells
    // =======================================================================

    /** Return critical vertices (unmatched 0-cells). */
    public static Set<Integer> criticalCells(MorseMatching matching) {
        return matching.criticalVertices();
    }

    // =======================================================================
    // Public API: Morse function
    // =======================================================================

    /**
     * Assign discrete Morse function values to vertices.
     * Uses topological-order assignment with Forman-condition adjustment.
     */
    public static MorseFunction morseFunction(StateSpace ss) {
        MorseMatching matching = greedyMatching(ss);
        List<int[]> hasseEdges = deduplicatedHasse(ss);

        // Topological ordering via Kahn's algorithm
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        Map<Integer, Integer> inDegree = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new HashSet<>());
            inDegree.put(s, 0);
        }
        for (int[] e : hasseEdges) {
            adj.get(e[0]).add(e[1]);
            inDegree.merge(e[1], 1, Integer::sum);
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (int s : ss.states()) {
            if (inDegree.getOrDefault(s, 0) == 0) {
                queue.add(s);
            }
        }

        List<Integer> topoOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            int s = queue.poll();
            topoOrder.add(s);
            for (int t : adj.getOrDefault(s, Set.of())) {
                int newDeg = inDegree.merge(t, -1, Integer::sum);
                if (newDeg == 0) queue.add(t);
            }
        }

        // Add remaining states (from cycles)
        Set<Integer> inTopo = new HashSet<>(topoOrder);
        List<Integer> remaining = new ArrayList<>();
        for (int s : ss.states()) {
            if (!inTopo.contains(s)) remaining.add(s);
        }
        Collections.sort(remaining);
        topoOrder.addAll(remaining);

        int n = topoOrder.size();
        Map<Integer, Double> values = new HashMap<>();
        for (int i = 0; i < n; i++) {
            values.put(topoOrder.get(i), (double) (n - i));
        }

        // Adjust matched pairs for Morse condition
        Map<Integer, Integer> matchedEdgeToVertex = new HashMap<>();
        for (var pair : matching.matchedPairs()) {
            matchedEdgeToVertex.put(pair.edgeIndex(), pair.vertex());
        }

        for (var entry : matchedEdgeToVertex.entrySet()) {
            int ei = entry.getKey();
            int vertex = entry.getValue();
            int[] edge = hasseEdges.get(ei);
            double valU = values.getOrDefault(edge[0], 0.0);
            double valV = values.getOrDefault(edge[1], 0.0);
            values.put(vertex, (valU + valV) / 2.0 + 0.1);
        }

        // Validate: discrete Morse condition
        Map<Integer, List<Integer>> coverDown = new HashMap<>();
        Map<Integer, List<Integer>> coverUp = new HashMap<>();
        for (int s : ss.states()) {
            coverDown.put(s, new ArrayList<>());
            coverUp.put(s, new ArrayList<>());
        }
        for (int[] e : hasseEdges) {
            coverDown.get(e[0]).add(e[1]);
            coverUp.get(e[1]).add(e[0]);
        }

        boolean isValid = true;
        for (int s : ss.states()) {
            long exceptionalDown = coverDown.getOrDefault(s, List.of()).stream()
                    .filter(t -> values.getOrDefault(t, 0.0) >= values.getOrDefault(s, 0.0))
                    .count();
            long exceptionalUp = coverUp.getOrDefault(s, List.of()).stream()
                    .filter(t -> values.getOrDefault(t, 0.0) <= values.getOrDefault(s, 0.0))
                    .count();
            if (exceptionalDown > 1 || exceptionalUp > 1) {
                isValid = false;
                break;
            }
        }

        return new MorseFunction(Map.copyOf(values), isValid);
    }

    // =======================================================================
    // Public API: Gradient vector field
    // =======================================================================

    /** Compute gradient vector field from the acyclic matching. */
    public static GradientField gradientField(StateSpace ss) {
        MorseMatching matching = greedyMatching(ss);
        List<int[]> hasseEdges = deduplicatedHasse(ss);

        Set<Integer> matchedIndices = matching.matchedPairs().stream()
                .map(VertexEdgePair::edgeIndex)
                .collect(Collectors.toSet());

        Map<Integer, List<Integer>> flow = new HashMap<>();
        for (int s : ss.states()) {
            flow.put(s, new ArrayList<>());
        }
        for (int i = 0; i < hasseEdges.size(); i++) {
            int[] e = hasseEdges.get(i);
            if (matchedIndices.contains(i)) {
                flow.get(e[1]).add(e[0]); // reverse matched edge
            } else {
                flow.get(e[0]).add(e[1]); // keep unmatched direction
            }
        }

        // Make flow lists immutable
        Map<Integer, List<Integer>> immutableFlow = new HashMap<>();
        for (var entry : flow.entrySet()) {
            immutableFlow.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        return new GradientField(
                matching.matchedPairs(),
                matching.criticalVertices(),
                Map.copyOf(immutableFlow)
        );
    }

    // =======================================================================
    // Public API: Betti numbers
    // =======================================================================

    /**
     * Compute Betti numbers for the Hasse CW complex.
     * b0 = number of connected components, b1 = cycle rank.
     */
    public static int[] bettiNumbers(StateSpace ss) {
        Set<Long> undirected = hasseUndirected(ss);
        int v = ss.states().size();
        int e = undirected.size();

        List<int[]> edgeList = new ArrayList<>();
        for (long packed : undirected) {
            int lo = (int) (packed >>> 32);
            int hi = (int) (packed & 0xFFFFFFFFL);
            edgeList.add(new int[]{lo, hi});
        }

        int b0 = connectedComponentsCount(ss.states(), edgeList);
        int b1 = Math.max(0, e - v + b0);
        return new int[]{b0, b1};
    }

    // =======================================================================
    // Public API: Morse inequalities
    // =======================================================================

    /**
     * Verify weak and strong Morse inequalities.
     * @return {weakHolds, strongHolds}
     */
    public static boolean[] morseInequalities(StateSpace ss) {
        MorseMatching matching = greedyMatching(ss);
        int[] betti = bettiNumbers(ss);

        int c0 = matching.criticalVertices().size();
        int c1 = matching.criticalEdges().size();

        boolean weakHolds = true;
        if (betti.length > 0 && c0 < betti[0]) weakHolds = false;
        if (betti.length > 1 && c1 < betti[1]) weakHolds = false;

        int chi = eulerCharacteristic(ss);
        boolean strongHolds = (c0 - c1 == chi);

        return new boolean[]{weakHolds, strongHolds};
    }

    // =======================================================================
    // Public API: Full analysis
    // =======================================================================

    /** Full Morse theory analysis of a session type state space. */
    public static MorseResult analyzeMorse(StateSpace ss) {
        MorseMatching matching = greedyMatching(ss);
        MorseFunction func = morseFunction(ss);
        GradientField grad = gradientField(ss);
        int[] betti = bettiNumbers(ss);
        int chi = eulerCharacteristic(ss);
        boolean[] ineqs = morseInequalities(ss);

        int numCrit = matching.criticalVertices().size() + matching.criticalEdges().size();

        return new MorseResult(
                matching,
                func,
                grad,
                betti,
                matching.criticalVertices(),
                numCrit,
                chi,
                ineqs[0],
                ineqs[1]
        );
    }
}
