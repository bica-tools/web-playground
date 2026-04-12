package com.bica.reborn.domain.mining;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.reticular.ReticularChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Protocol mining from observed traces.
 *
 * <p>Given observed method call sequences (traces), infer a session type
 * that describes the protocol. Three entry points:
 * <ul>
 *   <li>{@link #mineFromTraces(List)} &mdash; build prefix tree, merge, reconstruct</li>
 *   <li>{@link #mineFromStateSpace(StateSpace)} &mdash; reconstruct from existing SS</li>
 *   <li>{@link #mineFromLogs(List)} &mdash; parse log lines, extract traces, infer</li>
 * </ul>
 *
 * <p>Ported from {@code reticulate/reticulate/protocol_mining.py} (Step 83).
 */
public final class ProtocolMiner {

    private ProtocolMiner() {}

    // -----------------------------------------------------------------------
    // Result type
    // -----------------------------------------------------------------------

    /**
     * Result of protocol mining.
     */
    public record MiningResult(
            SessionType inferredType,
            String inferredTypeStr,
            double confidence,
            int numTraces,
            double coverage,
            boolean isLattice,
            int numStates,
            int numTransitions,
            StateSpace stateSpace) {}

    // -----------------------------------------------------------------------
    // Prefix tree acceptor (PTA)
    // -----------------------------------------------------------------------

    private static final class TrieNode {
        final Map<String, TrieNode> children = new LinkedHashMap<>();
        boolean isTerminal = false;
        int stateId = -1;
    }

    private static TrieNode buildPTA(List<List<String>> traces) {
        TrieNode root = new TrieNode();
        for (List<String> trace : traces) {
            TrieNode node = root;
            for (String label : trace) {
                node = node.children.computeIfAbsent(label, k -> new TrieNode());
            }
            node.isTerminal = true;
        }
        return root;
    }

    // -----------------------------------------------------------------------
    // State merging
    // -----------------------------------------------------------------------

    private static Object deepSignature(TrieNode node, int depth) {
        if (depth <= 0) {
            return List.of(node.isTerminal, new TreeSet<>(node.children.keySet()));
        }
        List<Object> childSigs = new ArrayList<>();
        for (var entry : new TreeMap<>(node.children).entrySet()) {
            childSigs.add(List.of(entry.getKey(), deepSignature(entry.getValue(), depth - 1)));
        }
        return List.of(node.isTerminal, childSigs);
    }

    private static void collectBySignature(TrieNode node,
                                            Map<Object, List<TrieNode>> sigMap,
                                            int depth) {
        Object sig = deepSignature(node, depth);
        sigMap.computeIfAbsent(sig, k -> new ArrayList<>()).add(node);
        for (TrieNode child : node.children.values()) {
            collectBySignature(child, sigMap, depth);
        }
    }

    private static TrieNode mergeEquivalent(TrieNode root, int mergeDepth) {
        Map<Object, List<TrieNode>> sigMap = new LinkedHashMap<>();
        collectBySignature(root, sigMap, mergeDepth);

        // node identity -> representative
        Map<Integer, TrieNode> nodeToRep = new IdentityHashMap<>();
        // We use System.identityHashCode workaround
        for (List<TrieNode> nodes : sigMap.values()) {
            TrieNode rep = nodes.get(0);
            for (TrieNode node : nodes) {
                nodeToRep.put(System.identityHashCode(node), rep);
            }
        }

        applyMerges(root, nodeToRep, new HashSet<>());
        return root;
    }

    private static void applyMerges(TrieNode node,
                                     Map<Integer, TrieNode> nodeToRep,
                                     Set<Integer> visited) {
        int nid = System.identityHashCode(node);
        if (visited.contains(nid)) return;
        visited.add(nid);

        for (String label : new ArrayList<>(node.children.keySet())) {
            TrieNode child = node.children.get(label);
            TrieNode rep = nodeToRep.getOrDefault(System.identityHashCode(child), child);
            node.children.put(label, rep);
            if (rep != child) {
                for (var entry : child.children.entrySet()) {
                    rep.children.putIfAbsent(entry.getKey(), entry.getValue());
                }
                if (child.isTerminal) rep.isTerminal = true;
            }
            applyMerges(rep, nodeToRep, visited);
        }
    }

    // -----------------------------------------------------------------------
    // PTA -> StateSpace conversion
    // -----------------------------------------------------------------------

    private static StateSpace ptaToStateSpace(TrieNode root) {
        Map<Integer, Integer> visited = new IdentityHashMap<>();
        int[] counter = {0};

        Set<Integer> states = new LinkedHashSet<>();
        List<Transition> transitions = new ArrayList<>();
        Map<Integer, String> labels = new LinkedHashMap<>();

        // Assign IDs via BFS
        visited.put(System.identityHashCode(root), counter[0]);
        root.stateId = counter[0];
        states.add(counter[0]);
        labels.put(counter[0], "s0");
        counter[0]++;

        int topId = 0;
        int bottomId = -1;

        Queue<TrieNode> queue = new LinkedList<>();
        queue.add(root);
        Set<Integer> processed = new HashSet<>();

        while (!queue.isEmpty()) {
            TrieNode node = queue.poll();
            int nid = System.identityHashCode(node);
            if (processed.contains(nid)) continue;
            processed.add(nid);

            Integer srcObj = visited.get(nid);
            if (srcObj == null) {
                srcObj = counter[0]++;
                visited.put(nid, srcObj);
                states.add(srcObj);
                labels.put(srcObj, "s" + srcObj);
            }
            int src = srcObj;

            if (node.isTerminal && node.children.isEmpty()) {
                if (bottomId == -1) bottomId = src;
            }

            for (var entry : new TreeMap<>(node.children).entrySet()) {
                TrieNode child = entry.getValue();
                int cid = System.identityHashCode(child);
                Integer tgt = visited.get(cid);
                if (tgt == null) {
                    tgt = counter[0]++;
                    visited.put(cid, tgt);
                    child.stateId = tgt;
                    states.add(tgt);
                    labels.put(tgt, "s" + tgt);
                }
                transitions.add(new Transition(src, entry.getKey(), tgt));
                queue.add(child);
            }
        }

        if (bottomId == -1) {
            bottomId = counter[0];
            states.add(bottomId);
            labels.put(bottomId, "s" + bottomId);
        }

        return new StateSpace(states, transitions, topId, bottomId, labels);
    }

    // -----------------------------------------------------------------------
    // Trace acceptance
    // -----------------------------------------------------------------------

    private static boolean acceptsTrace(StateSpace ss, List<String> trace) {
        int state = ss.top();
        for (String label : trace) {
            boolean found = false;
            for (var entry : ss.enabled(state)) {
                if (entry.getKey().equals(label)) {
                    state = entry.getValue();
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static double computeCoverage(StateSpace ss, List<List<String>> traces) {
        if (traces.isEmpty()) return 1.0;
        long accepted = traces.stream().filter(t -> acceptsTrace(ss, t)).count();
        return (double) accepted / traces.size();
    }

    private static double computeConfidence(double coverage, boolean isLattice,
                                             int numTraces, int numStates) {
        double traceFactor = Math.min(1.0, numTraces / 10.0);
        double latticeFactor = isLattice ? 1.0 : 0.3;
        double confidence = 0.5 * coverage + 0.3 * latticeFactor + 0.2 * traceFactor;
        return Math.round(Math.min(1.0, confidence) * 10000.0) / 10000.0;
    }

    // -----------------------------------------------------------------------
    // Log parsing
    // -----------------------------------------------------------------------

    private static final Pattern[] LOG_PATTERNS = {
            Pattern.compile("\\b(?:CALL|INVOKE|METHOD)\\s+(\\w+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:\\w+\\.)?(\\w+)\\(\\)"),
            Pattern.compile("[-=]>\\s*(\\w+)"),
            Pattern.compile("^(\\w+)$"),
    };

    private static final Pattern SESSION_DELIMITER = Pattern.compile(
            "(?:\\b(?:SESSION\\s+(?:START|END|BEGIN|CLOSE))\\b|^-{3,}$|^={3,}$)",
            Pattern.CASE_INSENSITIVE);

    /** Parse log lines into traces. */
    public static List<List<String>> parseLogLines(List<String> logLines) {
        List<List<String>> traces = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String line : logLines) {
            String stripped = line.strip();

            if (stripped.isEmpty() || SESSION_DELIMITER.matcher(stripped).find()) {
                if (!current.isEmpty()) {
                    traces.add(List.copyOf(current));
                    current.clear();
                }
                continue;
            }

            String method = extractMethod(stripped);
            if (method != null) {
                current.add(method);
            }
        }

        if (!current.isEmpty()) {
            traces.add(List.copyOf(current));
        }

        return traces;
    }

    private static String extractMethod(String line) {
        for (Pattern p : LOG_PATTERNS) {
            Matcher m = p.matcher(line);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Infer a session type from observed method call traces.
     */
    public static MiningResult mineFromTraces(List<List<String>> traces) {
        return mineFromTraces(traces, 2);
    }

    /**
     * Infer a session type from observed traces with configurable merge depth.
     */
    public static MiningResult mineFromTraces(List<List<String>> traces, int mergeDepth) {
        if (traces.isEmpty()) {
            return new MiningResult(null, "end", 0.0, 0, 0.0, true, 0, 0, null);
        }

        List<List<String>> nonEmpty = traces.stream()
                .filter(t -> !t.isEmpty()).toList();

        if (nonEmpty.isEmpty()) {
            return new MiningResult(null, "end", 1.0, traces.size(), 1.0, true, 1, 0, null);
        }

        // 1. Build PTA
        TrieNode root = buildPTA(nonEmpty);

        // 2. Merge equivalent states
        root = mergeEquivalent(root, mergeDepth);

        // 3. Convert to StateSpace
        StateSpace ss = ptaToStateSpace(root);

        // 4. Check lattice
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        boolean isLattice = lr.isLattice();

        // 5. Reconstruct session type
        SessionType inferred = null;
        String typeStr = "<reconstruction failed>";
        try {
            inferred = ReticularChecker.reconstruct(ss);
            typeStr = PrettyPrinter.pretty(inferred);
        } catch (Exception e) {
            // reconstruction may fail for certain structures
        }

        // 6. Coverage
        double coverage = computeCoverage(ss, traces);

        // 7. Confidence
        double confidence = computeConfidence(coverage, isLattice, traces.size(), ss.states().size());

        return new MiningResult(inferred, typeStr, confidence, traces.size(),
                coverage, isLattice, ss.states().size(), ss.transitions().size(), ss);
    }

    /**
     * Extract a session type from an existing state space.
     */
    public static MiningResult mineFromStateSpace(StateSpace ss) {
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        boolean isLattice = lr.isLattice();

        SessionType inferred = null;
        String typeStr = "<reconstruction failed>";
        try {
            inferred = ReticularChecker.reconstruct(ss);
            typeStr = PrettyPrinter.pretty(inferred);
        } catch (Exception e) {
            // reconstruction may fail
        }

        double confidence = computeConfidence(1.0, isLattice, 10, ss.states().size());

        return new MiningResult(inferred, typeStr, confidence, 0, 1.0,
                isLattice, ss.states().size(), ss.transitions().size(), ss);
    }

    /**
     * Parse log files, extract traces, and infer a protocol.
     */
    public static MiningResult mineFromLogs(List<String> logLines) {
        return mineFromLogs(logLines, 2);
    }

    /**
     * Parse log files, extract traces, and infer a protocol with configurable merge depth.
     */
    public static MiningResult mineFromLogs(List<String> logLines, int mergeDepth) {
        List<List<String>> traces = parseLogLines(logLines);
        return mineFromTraces(traces, mergeDepth);
    }
}
