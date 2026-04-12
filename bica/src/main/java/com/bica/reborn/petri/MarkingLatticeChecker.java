package com.bica.reborn.petri;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Lattice of reachable markings from Petri nets (Step 22 port).
 *
 * <p>Converts the reachability graph to a StateSpace for lattice analysis
 * and verifies that the marking lattice is isomorphic to L(S).
 */
public final class MarkingLatticeChecker {

    private MarkingLatticeChecker() {}

    // =========================================================================
    // Conversion: ReachabilityGraph -> StateSpace
    // =========================================================================

    /**
     * Convert a reachability graph to a StateSpace.
     */
    public static StateSpace reachabilityToStatespace(ReachabilityGraph rg, PetriNet net) {
        List<String> sortedMarkings = new ArrayList<>(rg.markings());
        Collections.sort(sortedMarkings);
        Map<String, Integer> markingToId = new HashMap<>();
        for (int i = 0; i < sortedMarkings.size(); i++) {
            markingToId.put(sortedMarkings.get(i), i);
        }

        Set<Integer> states = new HashSet<>();
        for (int i = 0; i < sortedMarkings.size(); i++) states.add(i);

        List<StateSpace.Transition> transitions = new ArrayList<>();
        for (var edge : rg.edges()) {
            int src = markingToId.get(edge.sourceMarking());
            int tgt = markingToId.get(edge.targetMarking());
            transitions.add(new StateSpace.Transition(src, edge.label(), tgt));
        }

        int top = markingToId.get(rg.initial());

        // Bottom = sink (no outgoing)
        Map<Integer, Integer> outDeg = new HashMap<>();
        for (int s : states) outDeg.put(s, 0);
        for (var t : transitions) outDeg.merge(t.source(), 1, Integer::sum);
        int bottom = top;
        for (var e : outDeg.entrySet()) {
            if (e.getValue() == 0) { bottom = e.getKey(); break; }
        }

        Map<Integer, String> labels = new HashMap<>();
        for (var e : markingToId.entrySet()) {
            labels.put(e.getValue(), "m" + e.getValue());
        }

        return new StateSpace(states, transitions, top, bottom, labels);
    }

    // =========================================================================
    // Poset metrics
    // =========================================================================

    /**
     * Compute the height (longest path from top to bottom).
     */
    public static int computeHeight(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (var t : ss.transitions()) adj.get(t.source()).add(t.target());

        int[] maxDepth = {0};
        computeHeightDfs(adj, ss.top(), 0, ss.bottom(), new HashSet<>(Set.of(ss.top())), maxDepth);
        return maxDepth[0];
    }

    private static void computeHeightDfs(Map<Integer, List<Integer>> adj, int u, int depth,
                                          int bottom, Set<Integer> visited, int[] maxDepth) {
        if (u == bottom) { maxDepth[0] = Math.max(maxDepth[0], depth); return; }
        for (int v : adj.getOrDefault(u, List.of())) {
            if (!visited.contains(v)) {
                visited.add(v);
                computeHeightDfs(adj, v, depth + 1, bottom, visited, maxDepth);
                visited.remove(v);
            }
        }
    }

    /**
     * Count maximal chains (top-to-bottom simple paths).
     */
    public static int countMaximalChains(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (var t : ss.transitions()) adj.get(t.source()).add(t.target());

        int[] count = {0};
        chainDfs(adj, ss.top(), ss.bottom(), new HashSet<>(Set.of(ss.top())), count);
        return Math.max(count[0], 1);
    }

    private static void chainDfs(Map<Integer, List<Integer>> adj, int u, int bottom,
                                  Set<Integer> visited, int[] count) {
        if (u == bottom) { count[0]++; return; }
        for (int v : adj.getOrDefault(u, List.of())) {
            if (!visited.contains(v)) {
                visited.add(v);
                chainDfs(adj, v, bottom, visited, count);
                visited.remove(v);
            }
        }
    }

    /**
     * Compute the covering relation (Hasse diagram edges).
     */
    public static List<int[]> computeCoveringRelation(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = computeReachability(ss);
        List<int[]> covering = new ArrayList<>();
        for (int a : ss.states()) {
            for (int b : reach.getOrDefault(a, Set.of())) {
                boolean isCovering = true;
                for (int c : reach.getOrDefault(a, Set.of())) {
                    if (c != b && reach.getOrDefault(c, Set.of()).contains(b)) {
                        isCovering = false;
                        break;
                    }
                }
                if (isCovering) covering.add(new int[]{a, b});
            }
        }
        return covering;
    }

    private static Map<Integer, Set<Integer>> computeReachability(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : ss.transitions()) adj.get(t.source()).add(t.target());

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (!visited.add(u)) continue;
                for (int v : adj.getOrDefault(u, Set.of())) stack.push(v);
            }
            visited.remove(s);
            reach.put(s, visited);
        }
        return reach;
    }

    // =========================================================================
    // Isomorphism check
    // =========================================================================

    /**
     * Verify that M(N(S)) is isomorphic to L(S).
     */
    public static boolean checkMarkingIsomorphism(StateSpace ss, PetriNet net) {
        ReachabilityGraph rg = PetriNetBuilder.buildReachabilityGraph(net);
        if (rg.numMarkings() != ss.states().size()) return false;

        StateSpace mss = reachabilityToStatespace(rg, net);
        LatticeResult lrOrig = LatticeChecker.checkLattice(ss);
        LatticeResult lrMark = LatticeChecker.checkLattice(mss);

        return lrOrig.isLattice() == lrMark.isLattice();
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Build marking lattice from a state space and check properties.
     */
    public static MarkingLatticeResult buildMarkingLattice(StateSpace ss) {
        PetriNet net = PetriNetBuilder.buildPetriNet(ss);
        ReachabilityGraph rg = PetriNetBuilder.buildReachabilityGraph(net);
        StateSpace mss = reachabilityToStatespace(rg, net);

        LatticeResult lr = LatticeChecker.checkLattice(mss);
        int height = computeHeight(mss);
        List<int[]> covering = computeCoveringRelation(mss);
        int chains = countMaximalChains(mss);
        boolean iso = checkMarkingIsomorphism(ss, net);

        // Width: approximate via BFS level counts
        int width = computeWidthApprox(mss);

        return new MarkingLatticeResult(lr, width, height, rg.numMarkings(),
                covering.size(), iso, chains);
    }

    /**
     * Check marking lattice properties.
     */
    public static MarkingLatticeResult checkMarkingLattice(StateSpace ss) {
        return buildMarkingLattice(ss);
    }

    private static int computeWidthApprox(StateSpace ss) {
        Map<Integer, Integer> depth = new HashMap<>();
        depth.put(ss.top(), 0);
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(ss.top());
        while (!queue.isEmpty()) {
            int s = queue.poll();
            for (var t : ss.transitionsFrom(s)) {
                if (!depth.containsKey(t.target())) {
                    depth.put(t.target(), depth.get(s) + 1);
                    queue.add(t.target());
                }
            }
        }
        if (depth.isEmpty()) return 1;
        Map<Integer, Integer> levelCounts = new HashMap<>();
        for (int d : depth.values()) levelCounts.merge(d, 1, Integer::sum);
        return levelCounts.values().stream().mapToInt(i -> i).max().orElse(1);
    }
}
