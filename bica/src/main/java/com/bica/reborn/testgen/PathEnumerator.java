package com.bica.reborn.testgen;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumerates protocol paths from a session type state space for test generation.
 *
 * <p>Produces three categories:
 * <ol>
 *   <li><b>Valid paths</b> &mdash; bounded DFS from top to bottom</li>
 *   <li><b>Violation points</b> &mdash; BFS prefix + disabled methods at each reachable state</li>
 *   <li><b>Incomplete prefixes</b> &mdash; proper prefixes of valid paths where final state &ne; bottom</li>
 * </ol>
 */
public final class PathEnumerator {

    /** A single step in a path: transition label, target state, and transition kind. */
    public record Step(String label, int target, TransitionKind kind) {

        /** Backward-compatible 2-arg constructor; defaults to {@link TransitionKind#METHOD}. */
        public Step(String label, int target) {
            this(label, target, TransitionKind.METHOD);
        }
    }

    /** A complete top-to-bottom valid path. */
    public record ValidPath(List<Step> steps) {
        public ValidPath { steps = List.copyOf(steps); }

        /** Returns the label sequence of this path. */
        public List<String> labels() {
            return steps.stream().map(Step::label).toList();
        }
    }

    /** A reachable state where a method is disabled (protocol violation). */
    public record ViolationPoint(
            int state,
            String disabledMethod,
            Set<String> enabledMethods,
            List<Step> prefixPath) {
        public ViolationPoint {
            enabledMethods = Set.copyOf(enabledMethods);
            prefixPath = List.copyOf(prefixPath);
        }

        /** Returns the label sequence of the prefix path. */
        public List<String> prefixLabels() {
            return prefixPath.stream().map(Step::label).toList();
        }
    }

    /** An incomplete protocol execution (proper prefix of a valid path, not at bottom). */
    public record IncompletePrefix(List<Step> steps, Set<String> remainingMethods) {
        public IncompletePrefix {
            steps = List.copyOf(steps);
            remainingMethods = Set.copyOf(remainingMethods);
        }

        /** Returns the label sequence of this prefix. */
        public List<String> labels() {
            return steps.stream().map(Step::label).toList();
        }
    }

    /** Result of path enumeration. */
    public record EnumerationResult(
            List<ValidPath> validPaths,
            List<ViolationPoint> violations,
            List<IncompletePrefix> incompletePrefixes,
            boolean truncated) {
        public EnumerationResult {
            validPaths = List.copyOf(validPaths);
            violations = List.copyOf(violations);
            incompletePrefixes = List.copyOf(incompletePrefixes);
        }
    }

    // =========================================================================
    // Client program tree — selection-aware test structure
    // =========================================================================

    /**
     * A tree-shaped client program that folds selection transitions into switch statements.
     *
     * <p>At <b>branch</b> ({@code &}) states the client decides — producing separate programs.
     * At <b>selection</b> ({@code +}) states the object decides — producing a single
     * {@link SelectionSwitch} node with all cases.
     */
    public sealed interface ClientProgram permits
            ClientProgram.MethodCall, ClientProgram.SelectionSwitch, ClientProgram.Terminal {

        /** A method call followed by the next program step. */
        record MethodCall(String label, ClientProgram next) implements ClientProgram {
            public MethodCall {
                Objects.requireNonNull(label);
                Objects.requireNonNull(next);
            }
        }

        /** A method call whose return value determines the branch via a switch statement. */
        record SelectionSwitch(String methodLabel, Map<String, ClientProgram> branches)
                implements ClientProgram {
            public SelectionSwitch {
                Objects.requireNonNull(methodLabel);
                branches = Collections.unmodifiableMap(new LinkedHashMap<>(branches));
            }
        }

        /** Protocol complete — object is in terminal state. */
        record Terminal() implements ClientProgram {}
    }

    /** Result of client program enumeration. */
    public record ClientProgramResult(List<ClientProgram> programs, boolean truncated) {
        public ClientProgramResult { programs = List.copyOf(programs); }
    }

    private PathEnumerator() {}

    /**
     * Enumerates all three path categories from the given state space.
     */
    public static EnumerationResult enumerate(StateSpace ss, TestGenConfig config) {
        var validPaths = enumerateValidPaths(ss, config.maxRevisits(), config.maxPaths());
        var violations = enumerateViolations(ss);
        var incomplete = enumerateIncompletePrefixes(ss, validPaths.paths);
        return new EnumerationResult(validPaths.paths, violations, incomplete, validPaths.truncated);
    }

    // =========================================================================
    // Valid paths — bounded DFS
    // =========================================================================

    private record PathResult(List<ValidPath> paths, boolean truncated) {}

    /**
     * Bounded DFS from top to bottom. A state may be revisited at most
     * {@code maxRevisits} times per path to handle recursive protocols.
     */
    static List<ValidPath> enumerateValidPaths(StateSpace ss, int maxRevisits) {
        return enumerateValidPaths(ss, maxRevisits, Integer.MAX_VALUE).paths;
    }

    private static PathResult enumerateValidPaths(StateSpace ss, int maxRevisits, int maxPaths) {
        var paths = new ArrayList<ValidPath>();
        var visitCounts = new HashMap<Integer, Integer>();
        var currentPath = new ArrayList<Step>();
        boolean truncated = dfs(ss, ss.top(), currentPath, visitCounts, paths, maxRevisits, maxPaths);
        return new PathResult(paths, truncated);
    }

    private static boolean dfs(StateSpace ss, int state, List<Step> path,
                                Map<Integer, Integer> visitCounts,
                                List<ValidPath> paths, int maxRevisits, int maxPaths) {
        if (state == ss.bottom()) {
            paths.add(new ValidPath(path));
            return paths.size() >= maxPaths;
        }

        int count = visitCounts.getOrDefault(state, 0);
        if (count > maxRevisits) {
            return false;
        }

        visitCounts.put(state, count + 1);
        for (var t : ss.transitionsFrom(state)) {
            path.add(new Step(t.label(), t.target(), t.kind()));
            boolean done = dfs(ss, t.target(), path, visitCounts, paths, maxRevisits, maxPaths);
            path.removeLast();
            if (done) {
                visitCounts.put(state, count);
                return true;
            }
        }
        visitCounts.put(state, count);
        return false;
    }

    // =========================================================================
    // Client programs — tree-shaped DFS with selection switches
    // =========================================================================

    /**
     * Builds tree-shaped client programs where selection transitions are folded
     * into switch statements. At branch ({@code &}) states, separate programs are
     * produced (client decides). At selection ({@code +}) states after a method call,
     * a single {@link ClientProgram.SelectionSwitch} groups all cases.
     *
     * @param ss          the state space
     * @param maxRevisits maximum times a state may be revisited per path
     * @param maxPaths    maximum number of programs before truncation
     * @return client programs and whether truncation occurred
     */
    public static ClientProgramResult enumerateClientPrograms(StateSpace ss,
                                                               int maxRevisits, int maxPaths) {
        var visitCounts = new HashMap<Integer, Integer>();
        var programs = buildFromState(ss, ss.top(), visitCounts, maxRevisits);
        boolean truncated = programs.size() > maxPaths;
        if (truncated) {
            programs = new ArrayList<>(programs.subList(0, maxPaths));
        }
        return new ClientProgramResult(programs, truncated);
    }

    /**
     * Returns {@code true} if {@code state} has only SELECTION transitions
     * (the object decides, no client-callable methods).
     */
    private static boolean isPureSelectionState(StateSpace ss, int state) {
        return ss.enabledMethods(state).isEmpty() && !ss.enabledSelections(state).isEmpty();
    }

    /**
     * Recursively builds all possible client programs from {@code state}.
     * Returns one program per distinct client-choice path.
     */
    private static List<ClientProgram> buildFromState(StateSpace ss, int state,
                                                       Map<Integer, Integer> visitCounts,
                                                       int maxRevisits) {
        if (state == ss.bottom()) {
            return List.of(new ClientProgram.Terminal());
        }

        int count = visitCounts.getOrDefault(state, 0);
        if (count > maxRevisits) {
            return List.of(); // depth limit reached
        }

        visitCounts.put(state, count + 1);

        var methodTransitions = ss.transitionsFrom(state).stream()
                .filter(t -> t.kind() == TransitionKind.METHOD).toList();
        var selectionTransitions = ss.transitionsFrom(state).stream()
                .filter(t -> t.kind() == TransitionKind.SELECTION).toList();

        var result = new ArrayList<ClientProgram>();

        if (!methodTransitions.isEmpty()) {
            // Branch / method state — client chooses which method to call
            for (var mt : methodTransitions) {
                int target = mt.target();
                if (isPureSelectionState(ss, target)) {
                    // Method followed by pure selection → SelectionSwitch
                    result.addAll(buildSelectionSwitch(ss, mt.label(), target,
                            visitCounts, maxRevisits));
                } else {
                    // Normal method call → prepend MethodCall to each sub-program
                    var subs = buildFromState(ss, target, visitCounts, maxRevisits);
                    for (var sub : subs) {
                        result.add(new ClientProgram.MethodCall(mt.label(), sub));
                    }
                }
            }
        } else if (!selectionTransitions.isEmpty()) {
            // Top-level selection (no preceding method) — each branch is a separate program
            for (var st : selectionTransitions) {
                result.addAll(buildFromState(ss, st.target(), visitCounts, maxRevisits));
            }
        }

        visitCounts.put(state, count); // restore
        return result;
    }

    /**
     * Builds SelectionSwitch programs for a method whose target is a pure selection state.
     * Uses zip matching: the program count equals {@code max(|branch_i|)} across all
     * selection arms, with shorter lists cycling. This avoids exponential cartesian
     * explosion because the object (not the client) chooses the selection arm at runtime.
     */
    private static List<ClientProgram> buildSelectionSwitch(StateSpace ss, String method,
                                                             int selState,
                                                             Map<Integer, Integer> visitCounts,
                                                             int maxRevisits) {
        var selTransitions = ss.transitionsFrom(selState).stream()
                .filter(t -> t.kind() == TransitionKind.SELECTION).toList();

        var branchPrograms = new LinkedHashMap<String, List<ClientProgram>>();
        for (var st : selTransitions) {
            var subs = buildFromState(ss, st.target(), visitCounts, maxRevisits);
            if (subs.isEmpty()) {
                subs = List.of(new ClientProgram.Terminal()); // dead-end bounded by maxRevisits
            }
            branchPrograms.put(st.label(), subs);
        }

        return zipSelectionBranches(method, branchPrograms);
    }

    /**
     * Zips selection branch programs: produces {@code max(|branch_i|)} switches,
     * cycling shorter branch lists. This ensures full coverage of all client-choice
     * paths within each selection arm without exponential blowup.
     */
    private static List<ClientProgram> zipSelectionBranches(String method,
                                                             LinkedHashMap<String, List<ClientProgram>> branchPrograms) {
        int maxLen = branchPrograms.values().stream()
                .mapToInt(List::size).max().orElse(0);
        if (maxLen == 0) return List.of();

        var keys = new ArrayList<>(branchPrograms.keySet());
        var result = new ArrayList<ClientProgram>();

        for (int i = 0; i < maxLen; i++) {
            var branches = new LinkedHashMap<String, ClientProgram>();
            for (var key : keys) {
                var list = branchPrograms.get(key);
                branches.put(key, list.get(i % list.size()));
            }
            result.add(new ClientProgram.SelectionSwitch(method, branches));
        }
        return result;
    }

    // =========================================================================
    // Violation points — BFS prefix + disabled labels
    // =========================================================================

    /**
     * BFS from top to find shortest prefix to each reachable state, then
     * identifies disabled methods at each non-bottom state.
     */
    static List<ViolationPoint> enumerateViolations(StateSpace ss) {
        // Collect only METHOD labels — selection labels are not callable by the client
        Set<String> allMethodLabels = ss.transitions().stream()
                .filter(t -> t.kind() == TransitionKind.METHOD)
                .map(StateSpace.Transition::label)
                .collect(Collectors.toSet());

        // BFS for shortest prefix path to each state
        Map<Integer, List<Step>> shortestPrefix = bfsShortestPrefixes(ss);

        var violations = new ArrayList<ViolationPoint>();
        for (var entry : shortestPrefix.entrySet()) {
            int state = entry.getKey();
            if (state == ss.bottom()) continue;

            Set<String> enabledMethods = ss.enabledMethods(state);
            // Skip pure selection states: the client has no agency here —
            // the object decides internally and the preceding method already
            // resolved the selection via its return value.
            if (enabledMethods.isEmpty() && !ss.enabledSelections(state).isEmpty()) {
                continue;
            }

            Set<String> disabled = new TreeSet<>(allMethodLabels);
            disabled.removeAll(enabledMethods);

            for (String method : disabled) {
                violations.add(new ViolationPoint(state, method, enabledMethods, entry.getValue()));
            }
        }
        return violations;
    }

    private static Map<Integer, List<Step>> bfsShortestPrefixes(StateSpace ss) {
        var prefixes = new LinkedHashMap<Integer, List<Step>>();
        prefixes.put(ss.top(), List.of());

        var queue = new ArrayDeque<Integer>();
        queue.add(ss.top());

        while (!queue.isEmpty()) {
            int state = queue.poll();
            List<Step> currentPrefix = prefixes.get(state);
            for (var t : ss.transitionsFrom(state)) {
                int target = t.target();
                if (!prefixes.containsKey(target)) {
                    var newPrefix = new ArrayList<>(currentPrefix);
                    newPrefix.add(new Step(t.label(), target, t.kind()));
                    prefixes.put(target, List.copyOf(newPrefix));
                    queue.add(target);
                }
            }
        }
        return prefixes;
    }

    // =========================================================================
    // Incomplete prefixes — proper prefixes of valid paths, not at bottom
    // =========================================================================

    /**
     * Generates all proper prefixes of valid paths where the final state is not bottom.
     * Deduplicates by label sequence.
     */
    static List<IncompletePrefix> enumerateIncompletePrefixes(StateSpace ss, List<ValidPath> validPaths) {
        var seen = new LinkedHashSet<List<String>>();
        var result = new ArrayList<IncompletePrefix>();

        for (var path : validPaths) {
            List<Step> steps = path.steps();
            // Proper prefixes: length 1 to steps.size()-1
            for (int len = 1; len < steps.size(); len++) {
                List<Step> prefix = steps.subList(0, len);
                List<String> labelSeq = prefix.stream().map(Step::label).toList();
                if (seen.add(labelSeq)) {
                    int finalState = prefix.getLast().target();
                    if (finalState != ss.bottom()) {
                        Set<String> remaining = ss.enabledLabels(finalState);
                        result.add(new IncompletePrefix(prefix, remaining));
                    }
                }
            }
        }
        return result;
    }
}
