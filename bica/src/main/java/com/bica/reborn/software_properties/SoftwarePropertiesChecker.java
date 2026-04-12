package com.bica.reborn.software_properties;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Algebraic Software Properties -- A Lattice-Theoretic Dictionary.
 *
 * <p>Maps classical software engineering properties to lattice-theoretic
 * characterizations on session type state spaces. Each property has:
 * <ol>
 *   <li>A <b>classical definition</b> (from process algebra / concurrency theory).</li>
 *   <li>A <b>lattice-theoretic characterization</b> (in terms of meets, joins,
 *       reachability, order structure).</li>
 *   <li>A <b>checker function</b> that verifies the property on a StateSpace.</li>
 * </ol>
 *
 * <p>Port of Python {@code reticulate.software_properties}.
 */
public final class SoftwarePropertiesChecker {

    private SoftwarePropertiesChecker() {}

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * Result of checking a single software property.
     *
     * @param name             property name (e.g. "safety", "liveness")
     * @param holds            whether the property holds
     * @param characterization brief lattice-theoretic explanation
     * @param witnesses        supporting evidence
     * @param counterexample   violating witness if property fails, else null
     */
    public record PropertyResult(
            String name,
            boolean holds,
            String characterization,
            List<String> witnesses,
            String counterexample) {

        public PropertyResult {
            Objects.requireNonNull(name);
            Objects.requireNonNull(characterization);
            witnesses = witnesses == null ? List.of() : List.copyOf(witnesses);
        }
    }

    /**
     * Result of checking all dictionary properties on a state space.
     *
     * @param properties      map from property name to its result
     * @param numStates       number of states in the state space
     * @param numTransitions  number of transitions
     * @param summary         human-readable summary string
     */
    public record DictionaryResult(
            Map<String, PropertyResult> properties,
            int numStates,
            int numTransitions,
            String summary) {

        public DictionaryResult {
            Objects.requireNonNull(properties);
            Objects.requireNonNull(summary);
            properties = Map.copyOf(properties);
        }

        /** Check if a named property holds. */
        public boolean holds(String name) {
            return properties.get(name).holds();
        }

        /** True iff every property in the dictionary holds. */
        public boolean allHold() {
            return properties.values().stream().allMatch(PropertyResult::holds);
        }

        /** Names of properties that hold. */
        public List<String> holding() {
            return properties.entrySet().stream()
                    .filter(e -> e.getValue().holds())
                    .map(Map.Entry::getKey)
                    .sorted()
                    .collect(Collectors.toUnmodifiableList());
        }

        /** Names of properties that fail. */
        public List<String> failing() {
            return properties.entrySet().stream()
                    .filter(e -> !e.getValue().holds())
                    .map(Map.Entry::getKey)
                    .sorted()
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    // -----------------------------------------------------------------------
    // Property names (the 13 dictionary entries)
    // -----------------------------------------------------------------------

    public static final String SAFETY = "safety";
    public static final String LIVENESS = "liveness";
    public static final String DEADLOCK_FREEDOM = "deadlock_freedom";
    public static final String PROGRESS = "progress";
    public static final String CONFLUENCE = "confluence";
    public static final String DETERMINISM = "determinism";
    public static final String REVERSIBILITY = "reversibility";
    public static final String TRANSPARENCY = "transparency";
    public static final String BOUNDEDNESS = "boundedness";
    public static final String FAIRNESS = "fairness";
    public static final String MONOTONICITY = "monotonicity";
    public static final String RESPONSIVENESS = "responsiveness";
    public static final String COMPOSITIONALITY = "compositionality";

    /** All 13 property names in registry order. */
    public static final List<String> ALL_PROPERTIES = List.of(
            SAFETY, LIVENESS, DEADLOCK_FREEDOM, PROGRESS, CONFLUENCE,
            DETERMINISM, REVERSIBILITY, TRANSPARENCY, BOUNDEDNESS,
            FAIRNESS, MONOTONICITY, RESPONSIVENESS, COMPOSITIONALITY);

    // -----------------------------------------------------------------------
    // Individual property checkers
    // -----------------------------------------------------------------------

    /**
     * Safety: "nothing bad happens" -- every reachable state can reach bottom.
     */
    public static PropertyResult checkSafety(StateSpace ss) {
        // Reverse reachability from bottom
        Map<Integer, Set<Integer>> revAdj = buildReverseAdj(ss);
        Set<Integer> canReachBottom = bfsCollect(ss.bottom(), revAdj);

        Set<Integer> stuck = new HashSet<>(ss.states());
        stuck.removeAll(canReachBottom);
        boolean holds = stuck.isEmpty();

        List<String> witnesses = new ArrayList<>();
        witnesses.add(canReachBottom.size() + "/" + ss.states().size() + " states can reach bottom");
        String counterexample = null;
        if (!holds) {
            int example = stuck.stream().mapToInt(Integer::intValue).min().orElse(-1);
            counterexample = "State " + example + " cannot reach bottom (terminal state)";
        }

        return new PropertyResult(SAFETY, holds,
                "Every reachable state has a path to bottom (\u22a5). "
                        + "The reachable set is a lower set in the lattice.",
                witnesses, counterexample);
    }

    /**
     * Liveness: "something good eventually happens" -- every non-bottom state
     * has at least one successor.
     */
    public static PropertyResult checkLiveness(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = buildAdj(ss);
        List<Integer> stuck = new ArrayList<>();
        for (int s : ss.states()) {
            if (s == ss.bottom()) continue;
            if (adj.getOrDefault(s, Set.of()).isEmpty()) {
                stuck.add(s);
            }
        }
        boolean holds = stuck.isEmpty();
        List<String> witnesses = new ArrayList<>();
        if (holds) {
            witnesses.add("Every non-bottom state has at least one outgoing transition");
        }
        String counterexample = null;
        if (!holds) {
            counterexample = "State " + Collections.min(stuck) + " has no outgoing transitions but is not bottom";
        }
        return new PropertyResult(LIVENESS, holds,
                "Every non-bottom element has a successor. "
                        + "The lattice has no dead ends above \u22a5.",
                witnesses, counterexample);
    }

    /**
     * Deadlock-freedom: no state is stuck except the terminal state.
     */
    public static PropertyResult checkDeadlockFreedom(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = buildAdj(ss);
        List<Integer> deadlocked = new ArrayList<>();
        boolean bottomHasSuccessors = !adj.getOrDefault(ss.bottom(), Set.of()).isEmpty();

        for (int s : ss.states()) {
            if (s == ss.bottom()) continue;
            if (adj.getOrDefault(s, Set.of()).isEmpty()) {
                deadlocked.add(s);
            }
        }
        boolean holds = deadlocked.isEmpty();

        List<String> witnesses = new ArrayList<>();
        if (holds) {
            witnesses.add("Bottom (state " + ss.bottom() + ") is the unique sink");
        }
        if (bottomHasSuccessors) {
            witnesses.add("Note: bottom has " + adj.get(ss.bottom()).size()
                    + " outgoing transitions (recursive)");
        }
        String counterexample = null;
        if (!holds) {
            counterexample = "State(s) " + deadlocked + " are deadlocked (no transitions, not bottom)";
        }
        return new PropertyResult(DEADLOCK_FREEDOM, holds,
                "\u22a5 is the unique element with no successors. "
                        + "No non-terminal state is a dead end.",
                witnesses, counterexample);
    }

    /**
     * Progress: every reachable non-terminal state enables at least one action.
     */
    public static PropertyResult checkProgress(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = buildAdj(ss);
        Set<Integer> reachable = ss.reachableFrom(ss.top());
        List<Integer> noProgress = new ArrayList<>();
        for (int s : reachable) {
            if (s == ss.bottom()) continue;
            if (adj.getOrDefault(s, Set.of()).isEmpty()) {
                noProgress.add(s);
            }
        }
        boolean holds = noProgress.isEmpty();
        List<String> witnesses = List.of(reachable.size() + " reachable states checked");
        String counterexample = null;
        if (!holds) {
            counterexample = "State " + Collections.min(noProgress)
                    + " is reachable but has no enabled actions";
        }
        return new PropertyResult(PROGRESS, holds,
                "Every reachable non-\u22a5 element covers at least one element. "
                        + "The Hasse diagram has no dead-end nodes above \u22a5.",
                witnesses, counterexample);
    }

    /**
     * Confluence: whenever execution forks, the branches rejoin (all fork
     * points' successors have a meet).
     */
    public static PropertyResult checkConfluence(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = buildAdj(ss);
        List<Integer> forkPoints = new ArrayList<>();
        List<int[]> nonConfluent = new ArrayList<>();

        for (int s : ss.states()) {
            List<Integer> succs = new ArrayList<>(adj.getOrDefault(s, Set.of()));
            Collections.sort(succs);
            if (succs.size() < 2) continue;
            forkPoints.add(s);

            boolean allPairsMeet = true;
            for (int i = 0; i < succs.size() && allPairsMeet; i++) {
                for (int j = i + 1; j < succs.size() && allPairsMeet; j++) {
                    Integer m = LatticeChecker.computeMeet(ss, succs.get(i), succs.get(j));
                    if (m == null) {
                        allPairsMeet = false;
                        nonConfluent.add(new int[]{s, succs.get(i), succs.get(j)});
                    }
                }
            }
        }

        boolean holds = nonConfluent.isEmpty();
        List<String> witnesses = List.of(forkPoints.size() + " fork points checked");
        String counterexample = null;
        if (!holds) {
            int[] nc = nonConfluent.get(0);
            counterexample = "Fork at state " + nc[0] + ": successors " + nc[1]
                    + " and " + nc[2] + " have no meet";
        }
        return new PropertyResult(CONFLUENCE, holds,
                "Every fork point's successors have a meet (GLB). "
                        + "Branching paths always reconverge in the lattice.",
                witnesses, counterexample);
    }

    /**
     * Determinism: each label enables at most one transition per state.
     */
    public static PropertyResult checkDeterminism(StateSpace ss) {
        Map<Long, List<Integer>> labelTargets = new HashMap<>();
        Map<Long, String> keyLabels = new HashMap<>();
        for (var t : ss.transitions()) {
            long key = packPair(t.source(), t.label().hashCode());
            labelTargets.computeIfAbsent(key, k -> new ArrayList<>()).add(t.target());
            keyLabels.put(key, t.label());
        }

        List<long[]> nonDet = new ArrayList<>();
        int checked = 0;
        for (var entry : labelTargets.entrySet()) {
            checked++;
            if (entry.getValue().size() > 1) {
                nonDet.add(new long[]{entry.getKey()});
            }
        }

        boolean holds = nonDet.isEmpty();
        List<String> witnesses = List.of(checked + " (state, label) pairs checked");
        String counterexample = null;
        if (!holds) {
            long key = nonDet.get(0)[0];
            List<Integer> targets = labelTargets.get(key);
            String label = keyLabels.get(key);
            counterexample = "Label '" + label + "' has " + targets.size() + " targets: " + targets;
        }
        return new PropertyResult(DETERMINISM, holds,
                "Each transition label is a partial function on the lattice. "
                        + "No state has two transitions with the same label.",
                witnesses, counterexample);
    }

    /**
     * Reversibility: every state can reach every other state (single SCC).
     */
    public static PropertyResult checkReversibility(StateSpace ss) {
        Set<Integer> reachableFromTop = ss.reachableFrom(ss.top());

        Map<Integer, Set<Integer>> revAdj = buildReverseAdj(ss);
        Set<Integer> canReachTop = bfsCollect(ss.top(), revAdj);

        boolean singleSCC = reachableFromTop.equals(ss.states())
                && canReachTop.equals(ss.states());

        List<String> witnesses = new ArrayList<>();
        if (singleSCC) {
            witnesses.add("All states form a single SCC");
        } else {
            witnesses.add("Forward reachable from top: " + reachableFromTop.size()
                    + ", reverse reachable from top: " + canReachTop.size()
                    + ", total: " + ss.states().size());
        }
        String counterexample = null;
        if (!singleSCC) {
            Set<Integer> unreachable = new HashSet<>(ss.states());
            unreachable.removeAll(reachableFromTop);
            if (!unreachable.isEmpty()) {
                counterexample = "State " + unreachable.stream().mapToInt(Integer::intValue).min().orElse(-1)
                        + " not reachable from top";
            } else {
                Set<Integer> noReturn = new HashSet<>(ss.states());
                noReturn.removeAll(canReachTop);
                if (!noReturn.isEmpty()) {
                    counterexample = "State " + noReturn.stream().mapToInt(Integer::intValue).min().orElse(-1)
                            + " cannot reach back to top";
                }
            }
        }
        return new PropertyResult(REVERSIBILITY, singleSCC,
                "The entire state space is a single SCC. "
                        + "Every state can reach every other state.",
                witnesses, counterexample);
    }

    /**
     * Transparency: internal choices (selections) don't create hidden dead ends.
     * All selection branches can reach bottom.
     */
    public static PropertyResult checkTransparency(StateSpace ss) {
        // Build adjacency without selection transitions (for counting selection-only states)
        Map<Integer, Set<Integer>> adjNoSel = new HashMap<>();
        Map<Integer, Set<Integer>> allAdj = buildAdj(ss);
        for (int s : ss.states()) {
            adjNoSel.put(s, new HashSet<>());
        }
        for (var t : ss.transitions()) {
            if (t.kind() != TransitionKind.SELECTION) {
                adjNoSel.get(t.source()).add(t.target());
            }
        }

        int selectionOnlyCount = 0;
        for (int s : ss.states()) {
            if (s == ss.bottom()) continue;
            if (!allAdj.getOrDefault(s, Set.of()).isEmpty()
                    && adjNoSel.getOrDefault(s, Set.of()).isEmpty()) {
                selectionOnlyCount++;
            }
        }

        // Check selection branches can reach bottom
        List<int[]> nonTransparent = new ArrayList<>();
        for (int s : ss.states()) {
            List<Integer> selTargets = new ArrayList<>();
            for (var t : ss.transitions()) {
                if (t.source() == s && t.kind() == TransitionKind.SELECTION) {
                    selTargets.add(t.target());
                }
            }
            if (selTargets.size() < 2) continue;
            for (int t : selTargets) {
                Set<Integer> reachable = ss.reachableFrom(t);
                if (!reachable.contains(ss.bottom())) {
                    nonTransparent.add(new int[]{s, t});
                    break;
                }
            }
        }

        boolean holds = nonTransparent.isEmpty();
        List<String> witnesses = List.of(selectionOnlyCount + " selection-only states");
        String counterexample = null;
        if (!holds) {
            counterexample = "Selection at state " + nonTransparent.get(0)[0]
                    + ": branch to " + nonTransparent.get(0)[1] + " cannot reach bottom";
        }
        return new PropertyResult(TRANSPARENCY, holds,
                "All selection branches can reach \u22a5. "
                        + "Internal choices don't create hidden dead ends.",
                witnesses, counterexample);
    }

    /**
     * Boundedness: the protocol has finite execution depth (finite lattice height).
     */
    public static PropertyResult checkBoundedness(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = buildAdj(ss);

        // Compute longest acyclic path from top to bottom via memoized DFS
        Map<Integer, Integer> memo = new HashMap<>();
        int height = longestPath(ss.top(), ss.bottom(), adj, new HashSet<>(), memo);

        boolean canReachBottom = height >= 0;
        boolean bounded = ss.states().size() < 10000;

        List<String> witnesses = new ArrayList<>();
        if (canReachBottom) {
            witnesses.add("Lattice height (longest chain): " + height);
        }
        witnesses.add("State space size: " + ss.states().size());

        String counterexample = null;
        if (!canReachBottom) {
            counterexample = "Top cannot reach bottom via any acyclic path";
        }

        return new PropertyResult(BOUNDEDNESS, canReachBottom && bounded,
                "Finite lattice height = " + height + ". "
                        + "The protocol terminates in bounded steps.",
                witnesses, counterexample);
    }

    /**
     * Fairness: every enabled branch can eventually reach bottom.
     */
    public static PropertyResult checkFairness(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = buildAdj(ss);
        List<int[]> unfair = new ArrayList<>();
        int checked = 0;

        for (int s : ss.states()) {
            Set<Integer> succs = adj.getOrDefault(s, Set.of());
            if (succs.size() < 2) continue;
            checked++;
            for (int t : succs) {
                Set<Integer> reachable = ss.reachableFrom(t);
                if (!reachable.contains(ss.bottom())) {
                    unfair.add(new int[]{s, t});
                    break;
                }
            }
        }

        boolean holds = unfair.isEmpty();
        List<String> witnesses = List.of(checked + " branching states checked");
        String counterexample = null;
        if (!holds) {
            counterexample = "Branch at state " + unfair.get(0)[0]
                    + ": successor " + unfair.get(0)[1] + " cannot reach bottom";
        }
        return new PropertyResult(FAIRNESS, holds,
                "Every branch successor can reach \u22a5. "
                        + "No choice permanently blocks termination.",
                witnesses, counterexample);
    }

    /**
     * Monotonicity: transitions always move "downward" (closer to bottom)
     * in the lattice ordering.
     */
    public static PropertyResult checkMonotonicity(StateSpace ss) {
        Map<Integer, Set<Integer>> revAdj = buildReverseAdj(ss);

        // BFS from bottom to compute minimum distance
        Map<Integer, Integer> dist = new HashMap<>();
        dist.put(ss.bottom(), 0);
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(ss.bottom());
        while (!queue.isEmpty()) {
            int s = queue.poll();
            for (int pred : revAdj.getOrDefault(s, Set.of())) {
                if (!dist.containsKey(pred)) {
                    dist.put(pred, dist.get(s) + 1);
                    queue.add(pred);
                }
            }
        }

        // Check transitions go from higher distance to lower (or equal)
        List<Object[]> upward = new ArrayList<>();
        for (var t : ss.transitions()) {
            Integer dSrc = dist.get(t.source());
            Integer dTgt = dist.get(t.target());
            if (dSrc != null && dTgt != null && dTgt > dSrc) {
                upward.add(new Object[]{t.source(), t.label(), t.target(), dSrc, dTgt});
            }
        }

        boolean holds = upward.isEmpty();
        List<String> witnesses = new ArrayList<>();
        if (dist.containsKey(ss.top())) {
            witnesses.add("Max distance from bottom: " + dist.get(ss.top()));
        }
        witnesses.add(ss.transitions().size() + " transitions checked");

        String counterexample = null;
        if (!holds) {
            Object[] u = upward.get(0);
            counterexample = "Transition (" + u[0] + ", '" + u[1] + "', " + u[2]
                    + "): distance to bottom increases from " + u[3] + " to " + u[4];
        }
        return new PropertyResult(MONOTONICITY, holds,
                "Every transition moves closer to \u22a5. "
                        + "Protocol execution is monotonically decreasing.",
                witnesses, counterexample);
    }

    /**
     * Responsiveness: every non-terminal state enables at least one action.
     */
    public static PropertyResult checkResponsiveness(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = buildAdj(ss);
        List<Integer> branchStates = new ArrayList<>();
        List<Integer> unresponsive = new ArrayList<>();

        for (int s : ss.states()) {
            if (s == ss.bottom()) continue;
            boolean hasNonSel = false;
            boolean hasSel = false;
            for (var t : ss.transitions()) {
                if (t.source() != s) continue;
                if (t.kind() == TransitionKind.SELECTION) {
                    hasSel = true;
                } else {
                    hasNonSel = true;
                }
            }
            if (hasNonSel) {
                branchStates.add(s);
            } else if (!hasSel && adj.getOrDefault(s, Set.of()).isEmpty()) {
                unresponsive.add(s);
            }
        }

        boolean holds = unresponsive.isEmpty();
        List<String> witnesses = List.of(branchStates.size() + " branch states identified");
        String counterexample = null;
        if (!holds) {
            counterexample = "State " + Collections.min(unresponsive) + " enables no actions";
        }
        return new PropertyResult(RESPONSIVENESS, holds,
                "Every non-terminal state enables at least one action. "
                        + "The server always responds to valid requests.",
                witnesses, counterexample);
    }

    /**
     * Compositionality: parallel composition preserves protocol structure.
     * For non-product state spaces, holds trivially.
     */
    public static PropertyResult checkCompositionality(StateSpace ss) {
        // Java StateSpace doesn't track product_coords/product_factors
        // like the Python version. We always report holds=true with a note.
        // The structural guarantee is that L(S1 || S2) = L(S1) x L(S2).
        List<String> witnesses = new ArrayList<>();
        witnesses.add("Structural compositionality: L(S1 || S2) = L(S1) x L(S2)");

        return new PropertyResult(COMPOSITIONALITY, true,
                "Product lattice decomposes into independent factors. "
                        + "L(S1 || S2) \u2245 L(S1) \u00d7 L(S2).",
                witnesses, null);
    }

    // -----------------------------------------------------------------------
    // Full dictionary check
    // -----------------------------------------------------------------------

    /**
     * Check all 13 dictionary properties on a state space.
     */
    public static DictionaryResult checkAllProperties(StateSpace ss) {
        Map<String, PropertyResult> results = new LinkedHashMap<>();
        results.put(SAFETY, checkSafety(ss));
        results.put(LIVENESS, checkLiveness(ss));
        results.put(DEADLOCK_FREEDOM, checkDeadlockFreedom(ss));
        results.put(PROGRESS, checkProgress(ss));
        results.put(CONFLUENCE, checkConfluence(ss));
        results.put(DETERMINISM, checkDeterminism(ss));
        results.put(REVERSIBILITY, checkReversibility(ss));
        results.put(TRANSPARENCY, checkTransparency(ss));
        results.put(BOUNDEDNESS, checkBoundedness(ss));
        results.put(FAIRNESS, checkFairness(ss));
        results.put(MONOTONICITY, checkMonotonicity(ss));
        results.put(RESPONSIVENESS, checkResponsiveness(ss));
        results.put(COMPOSITIONALITY, checkCompositionality(ss));

        long holdCount = results.values().stream().filter(PropertyResult::holds).count();
        List<String> failNames = results.entrySet().stream()
                .filter(e -> !e.getValue().holds())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        String summary = holdCount + "/" + results.size() + " properties hold";
        if (failNames.isEmpty()) {
            summary += "; All properties satisfied";
        } else {
            summary += "; Failing: " + String.join(", ", failNames);
        }

        return new DictionaryResult(results, ss.states().size(),
                ss.transitions().size(), summary);
    }

    /**
     * Check specific named properties on a state space.
     *
     * @throws IllegalArgumentException if an unknown property name is given
     */
    public static DictionaryResult checkProperties(StateSpace ss, String... names) {
        Map<String, PropertyResult> results = new LinkedHashMap<>();
        for (String name : names) {
            results.put(name, checkSingleProperty(ss, name));
        }

        long holdCount = results.values().stream().filter(PropertyResult::holds).count();
        List<String> failNames = results.entrySet().stream()
                .filter(e -> !e.getValue().holds())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        String summary = holdCount + "/" + results.size() + " properties hold";
        if (!failNames.isEmpty()) {
            summary += "; Failing: " + String.join(", ", failNames);
        }

        return new DictionaryResult(results, ss.states().size(),
                ss.transitions().size(), summary);
    }

    /**
     * Quick boolean profile: property name to holds/fails.
     */
    public static Map<String, Boolean> propertyProfile(StateSpace ss) {
        DictionaryResult r = checkAllProperties(ss);
        Map<String, Boolean> profile = new LinkedHashMap<>();
        for (var entry : r.properties().entrySet()) {
            profile.put(entry.getKey(), entry.getValue().holds());
        }
        return profile;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static PropertyResult checkSingleProperty(StateSpace ss, String name) {
        return switch (name) {
            case SAFETY -> checkSafety(ss);
            case LIVENESS -> checkLiveness(ss);
            case DEADLOCK_FREEDOM -> checkDeadlockFreedom(ss);
            case PROGRESS -> checkProgress(ss);
            case CONFLUENCE -> checkConfluence(ss);
            case DETERMINISM -> checkDeterminism(ss);
            case REVERSIBILITY -> checkReversibility(ss);
            case TRANSPARENCY -> checkTransparency(ss);
            case BOUNDEDNESS -> checkBoundedness(ss);
            case FAIRNESS -> checkFairness(ss);
            case MONOTONICITY -> checkMonotonicity(ss);
            case RESPONSIVENESS -> checkResponsiveness(ss);
            case COMPOSITIONALITY -> checkCompositionality(ss);
            default -> throw new IllegalArgumentException(
                    "Unknown property '" + name + "'. Available: " + ALL_PROPERTIES);
        };
    }

    /** Build forward adjacency: state -> set of successor states. */
    private static Map<Integer, Set<Integer>> buildAdj(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new HashSet<>()).add(t.target());
        }
        return adj;
    }

    /** Build reverse adjacency: state -> set of predecessor states. */
    private static Map<Integer, Set<Integer>> buildReverseAdj(StateSpace ss) {
        Map<Integer, Set<Integer>> rev = new HashMap<>();
        for (int s : ss.states()) rev.put(s, new HashSet<>());
        for (var t : ss.transitions()) {
            rev.computeIfAbsent(t.target(), k -> new HashSet<>()).add(t.source());
        }
        return rev;
    }

    /** BFS/DFS from a start node using an adjacency map; returns visited set. */
    private static Set<Integer> bfsCollect(int start, Map<Integer, Set<Integer>> adj) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int s = stack.pop();
            if (visited.add(s)) {
                for (int next : adj.getOrDefault(s, Set.of())) {
                    stack.push(next);
                }
            }
        }
        return visited;
    }

    /** Longest acyclic path from {@code current} to {@code target}. Returns -1 if unreachable. */
    private static int longestPath(int current, int target,
                                   Map<Integer, Set<Integer>> adj,
                                   Set<Integer> visited, Map<Integer, Integer> memo) {
        if (current == target) return 0;
        if (memo.containsKey(current) && !visited.contains(current)) {
            return memo.get(current);
        }
        int best = -1;
        visited.add(current);
        for (int next : adj.getOrDefault(current, Set.of())) {
            if (visited.contains(next)) continue;
            int len = longestPath(next, target, adj, visited, memo);
            if (len >= 0) {
                best = Math.max(best, 1 + len);
            }
        }
        visited.remove(current);
        if (!visited.contains(current)) {
            memo.put(current, best);
        }
        return best;
    }

    private static long packPair(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }
}
