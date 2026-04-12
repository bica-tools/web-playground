package com.bica.reborn.petri;

import java.util.*;

/**
 * The reachability graph of a Petri net.
 *
 * @param markings    Set of reachable markings (as frozen string keys).
 * @param edges       List of (sourceKey, transitionLabel, targetKey) triples.
 * @param initial     The initial marking key.
 * @param numMarkings Number of reachable markings.
 */
public record ReachabilityGraph(
        Set<String> markings,
        List<Edge> edges,
        String initial,
        int numMarkings) {

    /**
     * An edge in the reachability graph.
     */
    public record Edge(String sourceMarking, String label, String targetMarking) {}

    public ReachabilityGraph {
        markings = Set.copyOf(markings);
        edges = List.copyOf(edges);
        Objects.requireNonNull(initial);
    }
}
