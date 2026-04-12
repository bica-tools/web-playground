package com.bica.reborn.petri;

import com.bica.reborn.petri.PetriNet.Arc;
import com.bica.reborn.petri.ReachabilityGraph.Edge;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for constructing and analysing Petri nets from session type state spaces.
 *
 * <p>The construction uses a <strong>state-machine encoding</strong>: each state in the
 * original LTS becomes a place, and each transition becomes a Petri net transition that
 * consumes from the source place and produces to the target place. The reachability graph
 * of the resulting net is isomorphic to the original state space.
 */
public final class PetriNetBuilder {

    private PetriNetBuilder() {}

    // -----------------------------------------------------------------------
    // Marking utilities
    // -----------------------------------------------------------------------

    /** Convert a marking map to a canonical string key for hashing. */
    static String freezeMarking(Map<Integer, Integer> m) {
        return m.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }

    // -----------------------------------------------------------------------
    // Construction: StateSpace -> PetriNet
    // -----------------------------------------------------------------------

    /**
     * Construct a Petri net from a session type state space.
     *
     * <p>State-machine encoding: one place per state, one transition per edge.
     * Initial marking places one token on the top (initial) state.
     * The net is 1-safe: each reachable marking has exactly one token.
     */
    public static PetriNet buildPetriNet(StateSpace ss) {
        var places = new HashMap<Integer, Place>();
        var transitions = new HashMap<Integer, PetriTransition>();
        var pre = new HashMap<Integer, Set<Arc>>();
        var post = new HashMap<Integer, Set<Arc>>();

        // Create one place per state
        var sortedStates = new ArrayList<>(ss.states());
        Collections.sort(sortedStates);
        for (int stateId : sortedStates) {
            String label = ss.labels().getOrDefault(stateId, "s" + stateId);
            places.put(stateId, new Place(stateId, label, stateId));
        }

        // Create one transition per edge
        for (int tid = 0; tid < ss.transitions().size(); tid++) {
            var t = ss.transitions().get(tid);
            boolean isSel = t.kind() == TransitionKind.SELECTION;
            transitions.put(tid, new PetriTransition(
                    tid, t.label(), isSel, t.source(), t.target()));
            pre.put(tid, Set.of(new Arc(t.source(), 1)));
            post.put(tid, Set.of(new Arc(t.target(), 1)));
        }

        // Initial marking: one token on the top state
        var initialMarking = Map.of(ss.top(), 1);

        // Build state <-> marking correspondence
        var stateToMarking = new HashMap<Integer, Map<Integer, Integer>>();
        var markingToState = new HashMap<String, Integer>();
        for (int stateId : ss.states()) {
            var m = Map.of(stateId, 1);
            stateToMarking.put(stateId, m);
            markingToState.put(freezeMarking(m), stateId);
        }

        boolean isOcc = checkAcyclic(ss);
        boolean isFc = checkFreeChoice(pre);

        return new PetriNet(places, transitions, pre, post,
                initialMarking, stateToMarking, markingToState, isOcc, isFc);
    }

    // -----------------------------------------------------------------------
    // Core Petri net operations
    // -----------------------------------------------------------------------

    /** Check whether a transition is enabled under the given marking. */
    public static boolean isEnabled(PetriNet net, Map<Integer, Integer> marking, int transitionId) {
        var arcs = net.pre().get(transitionId);
        if (arcs == null) return false;
        for (var arc : arcs) {
            if (marking.getOrDefault(arc.placeId(), 0) < arc.weight()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fire a transition under the given marking.
     *
     * @return the new marking after firing, or null if the transition is not enabled.
     */
    public static Map<Integer, Integer> fire(PetriNet net, Map<Integer, Integer> marking, int transitionId) {
        if (!isEnabled(net, marking, transitionId)) return null;
        var newMarking = new HashMap<>(marking);
        // Remove tokens from input places
        for (var arc : net.pre().get(transitionId)) {
            int count = newMarking.getOrDefault(arc.placeId(), 0) - arc.weight();
            if (count == 0) {
                newMarking.remove(arc.placeId());
            } else {
                newMarking.put(arc.placeId(), count);
            }
        }
        // Add tokens to output places
        for (var arc : net.post().get(transitionId)) {
            newMarking.merge(arc.placeId(), arc.weight(), Integer::sum);
        }
        return newMarking;
    }

    /** Return IDs of all transitions enabled under the given marking. */
    public static List<Integer> enabledTransitions(PetriNet net, Map<Integer, Integer> marking) {
        var result = new ArrayList<Integer>();
        for (int tid : net.transitions().keySet().stream().sorted().toList()) {
            if (isEnabled(net, marking, tid)) {
                result.add(tid);
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Reachability graph
    // -----------------------------------------------------------------------

    /** Compute the reachability graph via BFS. */
    public static ReachabilityGraph buildReachabilityGraph(PetriNet net) {
        var initial = freezeMarking(net.initialMarking());
        var visited = new HashSet<String>();
        visited.add(initial);
        var edges = new ArrayList<Edge>();
        var queue = new ArrayDeque<Map<Integer, Integer>>();
        queue.add(new HashMap<>(net.initialMarking()));

        while (!queue.isEmpty()) {
            var current = queue.poll();
            String frozenCurrent = freezeMarking(current);
            for (int tid : net.transitions().keySet().stream().sorted().toList()) {
                var newMarking = fire(net, current, tid);
                if (newMarking != null) {
                    String frozenNew = freezeMarking(newMarking);
                    edges.add(new Edge(frozenCurrent, net.transitions().get(tid).label(), frozenNew));
                    if (visited.add(frozenNew)) {
                        queue.add(newMarking);
                    }
                }
            }
        }

        return new ReachabilityGraph(visited, edges, initial, visited.size());
    }

    // -----------------------------------------------------------------------
    // Verification: reachability graph isomorphic to state space
    // -----------------------------------------------------------------------

    /**
     * Verify that the reachability graph of the Petri net is isomorphic
     * to the original state space.
     */
    public static boolean verifyIsomorphism(StateSpace ss, PetriNet net) {
        var rg = buildReachabilityGraph(net);

        // 1. Same number of states/markings
        if (rg.numMarkings() != ss.states().size()) return false;

        // 2. Every reachable marking corresponds to a state
        for (String fm : rg.markings()) {
            if (!net.markingToState().containsKey(fm)) return false;
        }

        // 3. Every edge in the reachability graph corresponds to a transition
        var rgEdgesMapped = new HashSet<String>();
        for (var edge : rg.edges()) {
            Integer src = net.markingToState().get(edge.sourceMarking());
            Integer tgt = net.markingToState().get(edge.targetMarking());
            if (src == null || tgt == null) return false;
            rgEdgesMapped.add(src + ":" + edge.label() + ":" + tgt);
        }

        var ssEdges = new HashSet<String>();
        for (var t : ss.transitions()) {
            ssEdges.add(t.source() + ":" + t.label() + ":" + t.target());
        }

        return rgEdgesMapped.equals(ssEdges);
    }

    // -----------------------------------------------------------------------
    // Structural analysis
    // -----------------------------------------------------------------------

    /**
     * Find places that have multiple consuming transitions (conflict).
     * These correspond to branching/selection points in the session type.
     *
     * @return list of entries: (placeId, list of consuming transition IDs).
     */
    public static List<Map.Entry<Integer, List<Integer>>> conflictPlaces(PetriNet net) {
        var placeConsumers = new HashMap<Integer, List<Integer>>();
        for (var entry : net.pre().entrySet()) {
            int tid = entry.getKey();
            for (var arc : entry.getValue()) {
                placeConsumers.computeIfAbsent(arc.placeId(), k -> new ArrayList<>()).add(tid);
            }
        }
        return placeConsumers.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .sorted(Map.Entry.comparingByKey())
                .map(e -> Map.entry(e.getKey(), List.copyOf(e.getValue())))
                .toList();
    }

    /**
     * Compute S-invariants (place invariants) of the net.
     *
     * <p>For state-machine encoded session types, the trivial invariant is
     * the all-ones vector (token conservation: exactly one token total).
     *
     * @return list of invariant vectors (as placeId to coefficient maps).
     */
    public static List<Map<Integer, Integer>> placeInvariants(PetriNet net) {
        var invariants = new ArrayList<Map<Integer, Integer>>();

        // Trivial invariant: all places with coefficient 1
        var trivial = new HashMap<Integer, Integer>();
        for (int pid : net.places().keySet()) {
            trivial.put(pid, 1);
        }

        if (verifyInvariant(net, trivial)) {
            invariants.add(Map.copyOf(trivial));
        }

        return invariants;
    }

    /** Verify that y is an S-invariant: y . C = 0 for every transition. */
    static boolean verifyInvariant(PetriNet net, Map<Integer, Integer> y) {
        for (int tid : net.transitions().keySet()) {
            var preMap = new HashMap<Integer, Integer>();
            var preArcs = net.pre().get(tid);
            if (preArcs != null) {
                for (var arc : preArcs) preMap.put(arc.placeId(), arc.weight());
            }
            var postMap = new HashMap<Integer, Integer>();
            var postArcs = net.post().get(tid);
            if (postArcs != null) {
                for (var arc : postArcs) postMap.put(arc.placeId(), arc.weight());
            }

            var allPlaces = new HashSet<>(preMap.keySet());
            allPlaces.addAll(postMap.keySet());

            int dot = 0;
            for (int pid : allPlaces) {
                int c = postMap.getOrDefault(pid, 0) - preMap.getOrDefault(pid, 0);
                dot += y.getOrDefault(pid, 0) * c;
            }
            if (dot != 0) return false;
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // DOT visualization
    // -----------------------------------------------------------------------

    /** Generate a DOT representation of the Petri net. */
    public static String petriDot(PetriNet net) {
        var sb = new StringBuilder();
        sb.append("digraph PetriNet {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [fontsize=10];\n\n");

        // Places
        sb.append("  // Places\n");
        net.places().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    int pid = e.getKey();
                    var place = e.getValue();
                    int tokens = net.initialMarking().getOrDefault(pid, 0);
                    String tokenStr = tokens > 0 ? " \u2022".repeat(tokens) : "";
                    sb.append(String.format("  p%d [shape=circle, label=\"%s%s\"];\n",
                            pid, place.label(), tokenStr));
                });

        sb.append("\n  // Transitions\n");
        net.transitions().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    int tid = e.getKey();
                    var trans = e.getValue();
                    String style = trans.isSelection() ? " style=\"filled\" fillcolor=\"#e0e0ff\"" : "";
                    sb.append(String.format("  t%d [shape=box, label=\"%s\"%s];\n",
                            tid, trans.label(), style));
                });

        sb.append("\n  // Arcs (pre)\n");
        net.pre().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    int tid = e.getKey();
                    e.getValue().stream().sorted().forEach(arc -> {
                        String wLabel = arc.weight() > 1 ? " [label=\"" + arc.weight() + "\"]" : "";
                        sb.append(String.format("  p%d -> t%d%s;\n", arc.placeId(), tid, wLabel));
                    });
                });

        sb.append("\n  // Arcs (post)\n");
        net.post().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    int tid = e.getKey();
                    e.getValue().stream().sorted().forEach(arc -> {
                        String wLabel = arc.weight() > 1 ? " [label=\"" + arc.weight() + "\"]" : "";
                        sb.append(String.format("  t%d -> p%d%s;\n", tid, arc.placeId(), wLabel));
                    });
                });

        sb.append("}\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // High-level: build + verify
    // -----------------------------------------------------------------------

    /**
     * Convert a state space to a Petri net and verify the construction.
     *
     * <p>Builds the Petri net, computes the reachability graph, and verifies
     * isomorphism with the original state space.
     */
    public static PetriNetResult sessionTypeToPetriNet(StateSpace ss) {
        var net = buildPetriNet(ss);
        boolean iso = verifyIsomorphism(ss, net);
        var rg = buildReachabilityGraph(net);

        return new PetriNetResult(net,
                net.places().size(),
                net.transitions().size(),
                net.isOccurrenceNet(),
                net.isFreeChoice(),
                iso,
                rg.numMarkings());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Check whether the state space has no cycles (DFS cycle detection). */
    private static boolean checkAcyclic(StateSpace ss) {
        // 0=WHITE, 1=GRAY, 2=BLACK
        var color = new HashMap<Integer, Integer>();
        for (int s : ss.states()) color.put(s, 0);

        var adj = new HashMap<Integer, List<Integer>>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>()).add(t.target());
        }

        for (int s : ss.states()) {
            if (color.get(s) == 0) {
                if (hasCycleDfs(s, adj, color)) return false;
            }
        }
        return true;
    }

    private static boolean hasCycleDfs(int u, Map<Integer, List<Integer>> adj, Map<Integer, Integer> color) {
        color.put(u, 1); // GRAY
        for (int v : adj.getOrDefault(u, List.of())) {
            if (color.get(v) == 1) return true; // back edge
            if (color.get(v) == 0 && hasCycleDfs(v, adj, color)) return true;
        }
        color.put(u, 2); // BLACK
        return false;
    }

    /**
     * Check the free-choice property: for every two transitions t1, t2,
     * if they share an input place, they have the same set of input places.
     */
    private static boolean checkFreeChoice(Map<Integer, Set<Arc>> pre) {
        var placeToTransitions = new HashMap<Integer, List<Integer>>();
        for (var entry : pre.entrySet()) {
            int tid = entry.getKey();
            for (var arc : entry.getValue()) {
                placeToTransitions.computeIfAbsent(arc.placeId(), k -> new ArrayList<>()).add(tid);
            }
        }

        for (var tids : placeToTransitions.values()) {
            if (tids.size() <= 1) continue;
            Set<Arc> firstPre = pre.get(tids.get(0));
            for (int i = 1; i < tids.size(); i++) {
                if (!pre.get(tids.get(i)).equals(firstPre)) return false;
            }
        }
        return true;
    }
}
