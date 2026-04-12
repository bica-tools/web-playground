package com.bica.reborn.audio;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A complete audio routing graph.
 *
 * @param nodes        list of audio nodes in the graph
 * @param routes       list of directed routes between nodes
 * @param masterOutput name of the master output node
 */
public record AudioGraph(
        List<AudioNode> nodes,
        List<AudioRoute> routes,
        String masterOutput) {

    public AudioGraph {
        Objects.requireNonNull(nodes, "nodes must not be null");
        Objects.requireNonNull(routes, "routes must not be null");
        Objects.requireNonNull(masterOutput, "masterOutput must not be null");
        nodes = List.copyOf(nodes);
        routes = List.copyOf(routes);
        Set<String> names = nodes.stream().map(AudioNode::name).collect(Collectors.toSet());
        if (!names.contains(masterOutput)) {
            throw new IllegalArgumentException(
                    "masterOutput '" + masterOutput + "' not in nodes: " + names);
        }
    }

    /** Map from node name to AudioNode. */
    public Map<String, AudioNode> nodeMap() {
        return nodes.stream().collect(Collectors.toMap(AudioNode::name, n -> n));
    }

    /** Return (dst, label) pairs for routes from the given node name. */
    public List<Map.Entry<String, String>> successors(String name) {
        return routes.stream()
                .filter(r -> r.src().equals(name))
                .map(r -> Map.entry(r.dst(), r.label()))
                .toList();
    }

    /** Return (src, label) pairs for routes into the given node name. */
    public List<Map.Entry<String, String>> predecessors(String name) {
        return routes.stream()
                .filter(r -> r.dst().equals(name))
                .map(r -> Map.entry(r.src(), r.label()))
                .toList();
    }

    /** Return names of source nodes. */
    public List<String> sources() {
        return nodes.stream()
                .filter(n -> "source".equals(n.nodeType()))
                .map(AudioNode::name)
                .toList();
    }

    /** Return names of output nodes. */
    public List<String> outputs() {
        return nodes.stream()
                .filter(n -> "output".equals(n.nodeType()))
                .map(AudioNode::name)
                .toList();
    }
}
