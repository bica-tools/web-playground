package com.bica.reborn.visualize;

import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Hasse diagram representations in DOT/Graphviz format.
 *
 * <p>Produces DOT source strings (pure stdlib, no graphviz dependency).
 * The generated DOT can be rendered using the graphviz command-line tools
 * or the graphviz-java library.
 *
 * <p>Features:
 * <ul>
 *   <li>Top/bottom state coloring (blue/green)</li>
 *   <li>Counterexample highlighting (red border)</li>
 *   <li>Optional state and edge labels</li>
 *   <li>Selection transitions shown as dashed edges</li>
 * </ul>
 */
public final class HasseDiagram {

    private static final int MAX_LABEL_LEN = 40;

    private HasseDiagram() {} // utility class

    /**
     * Generate DOT source string for the Hasse diagram of a state space.
     *
     * @param ss     the state space
     * @return DOT source string
     */
    public static String dotSource(StateSpace ss) {
        return dotSource(ss, null, null, true, true);
    }

    /**
     * Generate DOT source string with options.
     *
     * @param ss         the state space
     * @param result     optional lattice result for counterexample highlighting
     * @param title      optional diagram title
     * @param labels     whether to show state labels
     * @param edgeLabels whether to show transition labels
     * @return DOT source string
     */
    public static String dotSource(
            StateSpace ss,
            LatticeResult result,
            String title,
            boolean labels,
            boolean edgeLabels) {
        var sb = new StringBuilder();
        sb.append("digraph {\n");
        sb.append("    rankdir=TB;\n");
        sb.append("    node [shape=box, style=\"filled,rounded\", fontname=\"Helvetica\"];\n");
        sb.append("    edge [fontname=\"Helvetica\", fontsize=10];\n");

        if (title != null) {
            sb.append("    label=\"").append(escapeDot(title)).append("\";\n");
            sb.append("    labelloc=t;\n");
            sb.append("    fontsize=14;\n");
        }

        // Determine counterexample states
        Set<Integer> counterStates = new HashSet<>();
        if (result != null && result.counterexample() != null) {
            counterStates.add(result.counterexample().a());
            counterStates.add(result.counterexample().b());
        }

        // Emit nodes
        for (int sid : sorted(ss.states())) {
            var attrs = new LinkedHashMap<String, String>();

            // Label
            String nodeLabel;
            if (!labels) {
                nodeLabel = String.valueOf(sid);
            } else {
                nodeLabel = ss.labels().getOrDefault(sid, String.valueOf(sid));
            }
            if (sid == ss.top()) {
                nodeLabel = "\u22a4 " + nodeLabel;
            } else if (sid == ss.bottom()) {
                nodeLabel = "\u22a5 " + nodeLabel;
            }
            attrs.put("label", truncate(nodeLabel));

            // Color
            if (sid == ss.top()) {
                attrs.put("fillcolor", "#bfdbfe");
            } else if (sid == ss.bottom()) {
                attrs.put("fillcolor", "#bbf7d0");
            } else {
                attrs.put("fillcolor", "#f8fafc");
            }

            // Counterexample highlighting
            if (counterStates.contains(sid)) {
                attrs.put("color", "red");
                attrs.put("penwidth", "2");
            }

            sb.append("    ").append(sid).append(" [").append(formatAttrs(attrs)).append("];\n");
        }

        // Emit edges
        for (Transition t : ss.transitions()) {
            var attrs = new LinkedHashMap<String, String>();
            if (edgeLabels) {
                attrs.put("label", t.label());
            }
            sb.append("    ").append(t.source()).append(" -> ").append(t.target());
            if (!attrs.isEmpty()) {
                sb.append(" [").append(formatAttrs(attrs)).append("]");
            }
            sb.append(";\n");
        }

        // Layout hints
        sb.append("    {rank=source; ").append(ss.top()).append("}\n");
        sb.append("    {rank=sink; ").append(ss.bottom()).append("}\n");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Generate a DOT string for rendering (alias for dotSource).
     */
    public static String renderDot(StateSpace ss) {
        return dotSource(ss);
    }

    /**
     * Generate a DOT string with lattice result.
     */
    public static String renderDot(StateSpace ss, LatticeResult result) {
        return dotSource(ss, result, null, true, true);
    }

    /**
     * Compute the Hasse edges (transitive reduction) of the state space.
     *
     * <p>Returns only the edges that form the Hasse diagram (i.e., removes
     * edges that are implied by transitivity).
     *
     * @param ss the state space
     * @return list of transitions forming the Hasse diagram
     */
    public static List<Transition> hasseEdges(StateSpace ss) {
        // Build adjacency map
        Map<Integer, Set<Integer>> succs = new HashMap<>();
        for (int s : ss.states()) {
            succs.put(s, new HashSet<>());
        }
        for (Transition t : ss.transitions()) {
            succs.computeIfAbsent(t.source(), k -> new HashSet<>()).add(t.target());
        }

        // Compute reachability (transitive closure)
        Map<Integer, Set<Integer>> reachable = new HashMap<>();
        for (int s : ss.states()) {
            reachable.put(s, computeReachable(s, succs));
        }

        // A transition s->t is a Hasse edge iff there is no intermediate u
        // such that s->u and u reaches t
        var hasse = new ArrayList<Transition>();
        for (Transition t : ss.transitions()) {
            boolean redundant = false;
            for (int u : succs.getOrDefault(t.source(), Set.of())) {
                if (u != t.target() && reachable.getOrDefault(u, Set.of()).contains(t.target())) {
                    redundant = true;
                    break;
                }
            }
            if (!redundant) {
                hasse.add(t);
            }
        }
        return hasse;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static Set<Integer> computeReachable(int start, Map<Integer, Set<Integer>> succs) {
        var visited = new HashSet<Integer>();
        var stack = new ArrayDeque<Integer>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int s = stack.pop();
            if (visited.add(s)) {
                for (int next : succs.getOrDefault(s, Set.of())) {
                    stack.push(next);
                }
            }
        }
        visited.remove(start); // reachable does not include self
        return visited;
    }

    private static String truncate(String label) {
        if (label.length() <= MAX_LABEL_LEN) return label;
        return label.substring(0, MAX_LABEL_LEN) + "\u2026";
    }

    private static String escapeDot(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String formatAttrs(Map<String, String> attrs) {
        return attrs.entrySet().stream()
                .map(e -> e.getKey() + "=\"" + escapeDot(e.getValue()) + "\"")
                .collect(Collectors.joining(", "));
    }

    private static List<Integer> sorted(Set<Integer> states) {
        var list = new ArrayList<>(states);
        Collections.sort(list);
        return list;
    }
}
