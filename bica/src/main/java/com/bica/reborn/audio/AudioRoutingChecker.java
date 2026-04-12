package com.bica.reborn.audio;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;

/**
 * Verified audio routing via session types.
 *
 * <p>Models audio routing graphs (DAW signal flow) as session types and verifies
 * that every signal reaches the master output. The lattice property on the
 * resulting state space guarantees no dead-end signal paths.
 *
 * <p>Key insight: an audio routing graph with N parallel channels maps to an
 * N-ary parallel session type. Sequential effects chains map to branch sequences.
 */
public final class AudioRoutingChecker {

    private AudioRoutingChecker() {}

    // -----------------------------------------------------------------------
    // Session type construction helpers
    // -----------------------------------------------------------------------

    private static String sanitizeLabel(String name) {
        String s = name.replace(" ", "_").replace("-", "_");
        if (!s.isEmpty() && !Character.isLetter(s.charAt(0))) {
            s = "n" + s;
        }
        return s;
    }

    private static String buildParallelType(List<String> channels) {
        if (channels.isEmpty()) return "end";
        if (channels.size() == 1) return channels.getFirst();
        return "(" + String.join(" || ", channels) + ")";
    }

    // -----------------------------------------------------------------------
    // Core: audio graph -> session type
    // -----------------------------------------------------------------------

    /**
     * Convert an audio routing graph to a session type string.
     */
    public static String audioGraphToSessionType(AudioGraph graph) {
        List<String> sources = graph.sources();
        if (sources.isEmpty()) return "end";

        List<String> chainTypes = new ArrayList<>();
        for (String src : sources) {
            chainTypes.add(buildSourceType(graph, src, new HashSet<>()));
        }

        if (chainTypes.isEmpty()) return "end";
        return buildParallelType(chainTypes);
    }

    private static Set<String> detectCycleTargets(AudioGraph graph, String node, Set<String> visited) {
        Set<String> targets = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(node);
        Set<String> seen = new HashSet<>();
        while (!stack.isEmpty()) {
            String n = stack.pop();
            if (!seen.add(n)) continue;
            for (var entry : graph.successors(n)) {
                String dst = entry.getKey();
                if (visited.contains(dst) && !dst.equals(graph.masterOutput())) {
                    targets.add(dst);
                } else if (!seen.contains(dst) && !dst.equals(graph.masterOutput())) {
                    stack.push(dst);
                }
            }
        }
        return targets;
    }

    private static String buildSourceType(AudioGraph graph, String node, Set<String> visited) {
        if (node.equals(graph.masterOutput())) return "end";
        if (visited.contains(node)) {
            return sanitizeLabel(node).toUpperCase();
        }

        Set<String> expandedVisited = new HashSet<>(visited);
        expandedVisited.add(node);
        Set<String> cycleTargets = detectCycleTargets(graph, node, expandedVisited);
        boolean needsRec = cycleTargets.contains(node);

        var succs = graph.successors(node);
        String label = sanitizeLabel(node);

        if (succs.isEmpty()) {
            String inner = "&{" + label + ": end}";
            if (needsRec) {
                return "rec " + label.toUpperCase() + " . " + inner;
            }
            return inner;
        }

        String inner;
        if (succs.size() == 1) {
            String dst = succs.getFirst().getKey();
            String sub = buildSourceType(graph, dst, expandedVisited);
            inner = "&{" + label + ": " + sub + "}";
        } else {
            Map<String, AudioNode> nodeMap = graph.nodeMap();
            AudioNode nodeObj = nodeMap.get(node);
            List<String> subTypes = new ArrayList<>();
            for (var entry : succs) {
                subTypes.add(buildSourceType(graph, entry.getKey(), expandedVisited));
            }
            if (nodeObj != null && "bus".equals(nodeObj.nodeType())) {
                String par = buildParallelType(subTypes);
                inner = "&{" + label + ": " + par + "}";
            } else {
                StringJoiner choices = new StringJoiner(", ");
                for (int i = 0; i < succs.size(); i++) {
                    String choiceLabel = sanitizeLabel(succs.get(i).getValue());
                    choices.add(choiceLabel + ": " + subTypes.get(i));
                }
                inner = "&{" + label + ": &{" + choices + "}}";
            }
        }

        if (needsRec) {
            return "rec " + label.toUpperCase() + " . " + inner;
        }
        return inner;
    }

    // -----------------------------------------------------------------------
    // Core: verification
    // -----------------------------------------------------------------------

    /**
     * Verify that every signal in the audio graph reaches master output.
     */
    public static AudioRoutingResult verifyRouting(AudioGraph graph) {
        String stStr = audioGraphToSessionType(graph);
        SessionType ast = Parser.parse(stStr);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        List<String> dead = findDeadSignals(graph);
        List<List<String>> loops = findFeedbackLoops(graph);
        boolean fbSafe = isFeedbackSafe(graph);

        return new AudioRoutingResult(
                lr.isLattice() && dead.isEmpty(),
                stStr, ast, ss, lr, dead, loops, fbSafe,
                graph.nodes().size(), graph.routes().size());
    }

    // -----------------------------------------------------------------------
    // Dead signal detection
    // -----------------------------------------------------------------------

    /**
     * Identify nodes that cannot reach the master output.
     */
    public static List<String> findDeadSignals(AudioGraph graph) {
        Set<String> reachable = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(graph.masterOutput());
        while (!stack.isEmpty()) {
            String node = stack.pop();
            if (!reachable.add(node)) continue;
            for (var entry : graph.predecessors(node)) {
                stack.push(entry.getKey());
            }
        }

        List<String> dead = new ArrayList<>();
        for (AudioNode n : graph.nodes()) {
            if (!reachable.contains(n.name()) && !n.name().equals(graph.masterOutput())) {
                dead.add(n.name());
            }
        }
        Collections.sort(dead);
        return dead;
    }

    // -----------------------------------------------------------------------
    // Feedback loop detection
    // -----------------------------------------------------------------------

    /**
     * Find all cycles in the audio routing graph.
     */
    public static List<List<String>> findFeedbackLoops(AudioGraph graph) {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (AudioNode n : graph.nodes()) {
            if (!visited.contains(n.name())) {
                dfs(graph, n.name(), new ArrayList<>(), new HashSet<>(), visited, cycles);
            }
        }
        return cycles;
    }

    private static void dfs(AudioGraph graph, String node, List<String> path,
                             Set<String> onStack, Set<String> visited,
                             List<List<String>> cycles) {
        visited.add(node);
        onStack.add(node);
        path.add(node);

        for (var entry : graph.successors(node)) {
            String dst = entry.getKey();
            if (onStack.contains(dst)) {
                int idx = path.indexOf(dst);
                List<String> cycle = new ArrayList<>(path.subList(idx, path.size()));
                cycle.add(dst);
                cycles.add(cycle);
            } else if (!visited.contains(dst)) {
                dfs(graph, dst, path, onStack, visited, cycles);
            }
        }

        path.removeLast();
        onStack.remove(node);
    }

    /**
     * Check whether all feedback loops are safe (guarded by a delay).
     */
    public static boolean isFeedbackSafe(AudioGraph graph) {
        List<List<String>> loops = findFeedbackLoops(graph);
        if (loops.isEmpty()) return true;

        Map<String, AudioNode> nodeMap = graph.nodeMap();
        for (List<String> cycle : loops) {
            boolean hasDelay = false;
            for (int i = 0; i < cycle.size() - 1; i++) {
                AudioNode n = nodeMap.get(cycle.get(i));
                if (n != null && n.name().toLowerCase().contains("delay")) {
                    hasDelay = true;
                    break;
                }
            }
            if (!hasDelay) return false;
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Pre-built routing scenarios
    // -----------------------------------------------------------------------

    /** Basic mixing setup: kick, snare, vocal -> master. */
    public static AudioGraph basicMix() {
        return new AudioGraph(
                List.of(new AudioNode("kick", "source", "mono"),
                        new AudioNode("snare", "source", "mono"),
                        new AudioNode("vocal", "source", "mono"),
                        new AudioNode("master", "output", "stereo")),
                List.of(new AudioRoute("kick", "master", "send"),
                        new AudioRoute("snare", "master", "send"),
                        new AudioRoute("vocal", "master", "send")),
                "master");
    }

    /** Mastering chain: input -> EQ -> compressor -> limiter -> output. */
    public static AudioGraph masteringChain() {
        return new AudioGraph(
                List.of(new AudioNode("input", "source"),
                        new AudioNode("eq", "effect"),
                        new AudioNode("compressor", "effect"),
                        new AudioNode("limiter", "effect"),
                        new AudioNode("master", "output")),
                List.of(new AudioRoute("input", "eq", "insert"),
                        new AudioRoute("eq", "compressor", "insert"),
                        new AudioRoute("compressor", "limiter", "insert"),
                        new AudioRoute("limiter", "master", "insert")),
                "master");
    }

    /** Multiband processing: input -> splitter -> (low || mid || high) -> sum -> master. */
    public static AudioGraph multibandSplit() {
        return new AudioGraph(
                List.of(new AudioNode("input", "source"),
                        new AudioNode("splitter", "bus"),
                        new AudioNode("low_band", "effect"),
                        new AudioNode("mid_band", "effect"),
                        new AudioNode("high_band", "effect"),
                        new AudioNode("sum_bus", "bus"),
                        new AudioNode("master", "output")),
                List.of(new AudioRoute("input", "splitter", "send"),
                        new AudioRoute("splitter", "low_band", "split_low"),
                        new AudioRoute("splitter", "mid_band", "split_mid"),
                        new AudioRoute("splitter", "high_band", "split_high"),
                        new AudioRoute("low_band", "sum_bus", "return"),
                        new AudioRoute("mid_band", "sum_bus", "return"),
                        new AudioRoute("high_band", "sum_bus", "return"),
                        new AudioRoute("sum_bus", "master", "send")),
                "master");
    }

    /** Sidechain compression: trigger + main -> compressor -> master. */
    public static AudioGraph sidechainCompress() {
        return new AudioGraph(
                List.of(new AudioNode("trigger", "source", "mono"),
                        new AudioNode("main", "source"),
                        new AudioNode("detector", "effect", "mono"),
                        new AudioNode("compressor", "effect"),
                        new AudioNode("master", "output")),
                List.of(new AudioRoute("trigger", "detector", "sidechain"),
                        new AudioRoute("detector", "compressor", "control"),
                        new AudioRoute("main", "compressor", "audio"),
                        new AudioRoute("compressor", "master", "send")),
                "master");
    }

    /** Feedback delay: input -> delay -> output, with delay -> feedback -> delay loop. */
    public static AudioGraph feedbackDelay() {
        return new AudioGraph(
                List.of(new AudioNode("input", "source"),
                        new AudioNode("delay", "effect"),
                        new AudioNode("feedback", "effect"),
                        new AudioNode("master", "output")),
                List.of(new AudioRoute("input", "delay", "send"),
                        new AudioRoute("delay", "master", "output"),
                        new AudioRoute("delay", "feedback", "tap"),
                        new AudioRoute("feedback", "delay", "return")),
                "master");
    }

    /** Unsafe feedback: direct loop without delay node. */
    public static AudioGraph unsafeFeedback() {
        return new AudioGraph(
                List.of(new AudioNode("input", "source"),
                        new AudioNode("effect_a", "effect"),
                        new AudioNode("effect_b", "effect"),
                        new AudioNode("master", "output")),
                List.of(new AudioRoute("input", "effect_a", "send"),
                        new AudioRoute("effect_a", "effect_b", "send"),
                        new AudioRoute("effect_b", "effect_a", "feedback"),
                        new AudioRoute("effect_b", "master", "send")),
                "master");
    }

    /** Recording setup: mic -> preamp -> converter -> DAW -> monitor. */
    public static AudioGraph recordingSetup() {
        return new AudioGraph(
                List.of(new AudioNode("mic", "source", "mono"),
                        new AudioNode("preamp", "effect", "mono"),
                        new AudioNode("converter", "effect"),
                        new AudioNode("daw", "effect"),
                        new AudioNode("master", "output")),
                List.of(new AudioRoute("mic", "preamp", "xlr"),
                        new AudioRoute("preamp", "converter", "line"),
                        new AudioRoute("converter", "daw", "digital"),
                        new AudioRoute("daw", "master", "monitor")),
                "master");
    }

    /** Graph with a dead signal: orphaned bus with no route to master. */
    public static AudioGraph deadSignalGraph() {
        return new AudioGraph(
                List.of(new AudioNode("input", "source"),
                        new AudioNode("eq", "effect"),
                        new AudioNode("orphan_bus", "bus"),
                        new AudioNode("orphan_fx", "effect"),
                        new AudioNode("master", "output")),
                List.of(new AudioRoute("input", "eq", "insert"),
                        new AudioRoute("eq", "master", "send"),
                        new AudioRoute("orphan_bus", "orphan_fx", "send")),
                "master");
    }
}
