package com.bica.reborn.csp;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CSP trace and failures semantics for session type state spaces.
 *
 * <p>Translates session type state spaces (labeled transition systems) into
 * Hoare's CSP semantic models:
 * <ul>
 *   <li><b>Traces model</b>: all finite event sequences the process can perform</li>
 *   <li><b>Failures model</b>: pairs (trace, refusal_set) where the refusal set
 *       is the set of events that cannot occur after executing the trace</li>
 * </ul>
 *
 * <p>Key results:
 * <ul>
 *   <li>Trace refinement corresponds to session type subtyping</li>
 *   <li>Refusal sets correspond to disabled methods in the state space</li>
 *   <li>Complete traces (top to bottom) are maximal accepted behaviours</li>
 *   <li>Lattice meet/join determine failure structure for session type lattices</li>
 * </ul>
 */
public final class CSPChecker {

    private CSPChecker() {}

    // -----------------------------------------------------------------------
    // Trace extraction
    // -----------------------------------------------------------------------

    /**
     * Enumerate all finite traces from the state space via DFS.
     *
     * <p>A trace is any sequence of transition labels along a path starting
     * from {@code ss.top()}. The empty trace is always included.
     * Paths are explored up to {@code maxDepth} transitions.
     *
     * @param ss       the state space
     * @param maxDepth maximum trace length
     * @return set of all traces (each trace is a list of event names)
     */
    public static Set<List<String>> extractTraces(StateSpace ss, int maxDepth) {
        var traces = new HashSet<List<String>>();
        traceDfs(ss, ss.top(), new ArrayList<>(), traces, maxDepth);
        return traces;
    }

    private static void traceDfs(
            StateSpace ss, int state, List<String> current,
            Set<List<String>> traces, int remaining) {
        traces.add(List.copyOf(current));
        if (remaining <= 0) return;
        for (var entry : ss.enabled(state)) {
            current.add(entry.getKey());
            traceDfs(ss, entry.getValue(), current, traces, remaining - 1);
            current.removeLast();
        }
    }

    /**
     * Enumerate traces that reach the bottom state (complete executions).
     *
     * @param ss       the state space
     * @param maxDepth maximum trace length
     * @return set of complete traces
     */
    public static Set<List<String>> extractCompleteTraces(StateSpace ss, int maxDepth) {
        var traces = new HashSet<List<String>>();
        completeDfs(ss, ss.top(), new ArrayList<>(), traces, maxDepth);
        return traces;
    }

    private static void completeDfs(
            StateSpace ss, int state, List<String> current,
            Set<List<String>> traces, int remaining) {
        if (state == ss.bottom()) {
            traces.add(List.copyOf(current));
            return;
        }
        if (remaining <= 0) return;
        for (var entry : ss.enabled(state)) {
            current.add(entry.getKey());
            completeDfs(ss, entry.getValue(), current, traces, remaining - 1);
            current.removeLast();
        }
    }

    // -----------------------------------------------------------------------
    // Alphabet
    // -----------------------------------------------------------------------

    /**
     * Extract the event alphabet (all transition labels) from a state space.
     */
    public static Set<String> alphabet(StateSpace ss) {
        return ss.transitions().stream()
                .map(StateSpace.Transition::label)
                .collect(Collectors.toUnmodifiableSet());
    }

    // -----------------------------------------------------------------------
    // Refusals and acceptances
    // -----------------------------------------------------------------------

    /**
     * Compute the refusal set at a given state.
     *
     * <p>The refusal set is the complement of enabled labels relative to the
     * full alphabet. At the bottom state, the process refuses everything.
     */
    public static Set<String> computeRefusals(StateSpace ss, int state) {
        var alpha = alphabet(ss);
        var enabled = ss.enabledLabels(state);
        var refusals = new HashSet<>(alpha);
        refusals.removeAll(enabled);
        return Set.copyOf(refusals);
    }

    /**
     * Compute the refusal set for every state in the state space.
     */
    public static Map<Integer, Set<String>> computeAllRefusals(StateSpace ss) {
        var alpha = alphabet(ss);
        var result = new HashMap<Integer, Set<String>>();
        for (int state : ss.states()) {
            var enabled = ss.enabledLabels(state);
            var refusals = new HashSet<>(alpha);
            refusals.removeAll(enabled);
            result.put(state, Set.copyOf(refusals));
        }
        return Map.copyOf(result);
    }

    /**
     * Compute the acceptance set at a given state (the set of enabled labels).
     */
    public static Set<String> acceptanceSet(StateSpace ss, int state) {
        return ss.enabledLabels(state);
    }

    // -----------------------------------------------------------------------
    // Failures
    // -----------------------------------------------------------------------

    /**
     * Compute the failure set of a state space.
     *
     * <p>A failure {@code (t, X)} means: after performing trace {@code t},
     * the process can refuse all events in {@code X}.
     */
    public static Set<Failure> failures(StateSpace ss, int maxDepth) {
        var alpha = alphabet(ss);
        var result = new HashSet<Failure>();
        failuresDfs(ss, ss.top(), new ArrayList<>(), result, alpha, maxDepth);
        return result;
    }

    private static void failuresDfs(
            StateSpace ss, int state, List<String> current,
            Set<Failure> result, Set<String> alpha, int remaining) {
        var enabled = ss.enabledLabels(state);
        var refusals = new HashSet<>(alpha);
        refusals.removeAll(enabled);
        result.add(new Failure(List.copyOf(current), refusals));
        if (remaining <= 0) return;
        for (var entry : ss.enabled(state)) {
            current.add(entry.getKey());
            failuresDfs(ss, entry.getValue(), current, result, alpha, remaining - 1);
            current.removeLast();
        }
    }

    // -----------------------------------------------------------------------
    // Refinement checks
    // -----------------------------------------------------------------------

    /**
     * Check trace refinement: {@code t1 ⊆ t2}.
     *
     * <p>In CSP, P refines Q in the traces model iff traces(P) ⊆ traces(Q).
     */
    public static boolean traceRefinement(Set<List<String>> t1, Set<List<String>> t2) {
        return t2.containsAll(t1);
    }

    /**
     * Check failures refinement between two state spaces.
     *
     * <p>P refines Q in the failures model iff:
     * <ol>
     *   <li>traces(P) ⊆ traces(Q)</li>
     *   <li>failures(P) ⊆ failures(Q)</li>
     * </ol>
     */
    public static FailuresRefinementResult checkFailuresRefinement(
            StateSpace ss1, StateSpace ss2, int maxDepth) {
        var traces1 = extractTraces(ss1, maxDepth);
        var traces2 = extractTraces(ss2, maxDepth);
        var failures1 = failures(ss1, maxDepth);
        var failures2 = failures(ss2, maxDepth);

        boolean tracesOk = traces2.containsAll(traces1);
        boolean failuresOk = failures2.containsAll(failures1);

        Set<List<String>> traceCounterexamples;
        if (tracesOk) {
            traceCounterexamples = Set.of();
        } else {
            traceCounterexamples = new HashSet<>(traces1);
            traceCounterexamples.removeAll(traces2);
        }

        Set<Failure> failureCounterexamples;
        if (failuresOk) {
            failureCounterexamples = Set.of();
        } else {
            failureCounterexamples = new HashSet<>(failures1);
            failureCounterexamples.removeAll(failures2);
        }

        return new FailuresRefinementResult(
                tracesOk && failuresOk,
                tracesOk,
                failuresOk,
                traceCounterexamples,
                failureCounterexamples);
    }

    // -----------------------------------------------------------------------
    // Determinism
    // -----------------------------------------------------------------------

    /**
     * Check if the state space is deterministic.
     *
     * <p>A state space is deterministic if from every state, each label leads
     * to at most one target state.
     */
    public static boolean isDeterministic(StateSpace ss) {
        for (int state : ss.states()) {
            var seen = new HashMap<String, Integer>();
            for (var entry : ss.enabled(state)) {
                Integer prev = seen.put(entry.getKey(), entry.getValue());
                if (prev != null && !prev.equals(entry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Equivalence checks
    // -----------------------------------------------------------------------

    /**
     * Check trace equivalence: traces(ss1) == traces(ss2).
     */
    public static boolean traceEquivalent(StateSpace ss1, StateSpace ss2, int maxDepth) {
        return extractTraces(ss1, maxDepth).equals(extractTraces(ss2, maxDepth));
    }

    /**
     * Check failures equivalence: failures(ss1) == failures(ss2).
     */
    public static boolean failuresEquivalent(StateSpace ss1, StateSpace ss2, int maxDepth) {
        return failures(ss1, maxDepth).equals(failures(ss2, maxDepth));
    }

    // -----------------------------------------------------------------------
    // Full CSP analysis
    // -----------------------------------------------------------------------

    /**
     * Full CSP semantic analysis of a state space.
     */
    public static CSPResult cspAnalysis(StateSpace ss, int maxDepth) {
        var t = extractTraces(ss, maxDepth);
        var ct = extractCompleteTraces(ss, maxDepth);
        var f = failures(ss, maxDepth);
        var a = alphabet(ss);
        return new CSPResult(t, ct, f, a);
    }

    // -----------------------------------------------------------------------
    // Must / May testing
    // -----------------------------------------------------------------------

    /**
     * After executing trace, what labels MUST be accepted?
     *
     * <p>Returns the intersection of acceptance sets over all states reachable
     * after the trace. In a deterministic system, this equals the acceptance
     * set of the unique reached state.
     */
    public static Set<String> mustTesting(StateSpace ss, List<String> trace) {
        var states = executeTrace(ss, trace);
        if (states.isEmpty()) return Set.of();

        Set<String> result = null;
        for (int state : states) {
            var acc = ss.enabledLabels(state);
            if (result == null) {
                result = new HashSet<>(acc);
            } else {
                result.retainAll(acc);
            }
        }
        return result != null ? Set.copyOf(result) : Set.of();
    }

    /**
     * After executing trace, what labels MAY be accepted?
     *
     * <p>Returns the union of acceptance sets over all states reachable
     * after the trace.
     */
    public static Set<String> mayTesting(StateSpace ss, List<String> trace) {
        var states = executeTrace(ss, trace);
        if (states.isEmpty()) return Set.of();

        var result = new HashSet<String>();
        for (int state : states) {
            result.addAll(ss.enabledLabels(state));
        }
        return Set.copyOf(result);
    }

    /**
     * Execute a trace from the top state, returning set of reachable states.
     */
    private static Set<Integer> executeTrace(StateSpace ss, List<String> trace) {
        Set<Integer> current = new HashSet<>(Set.of(ss.top()));

        for (String label : trace) {
            Set<Integer> next = new HashSet<>();
            for (int state : current) {
                for (var t : ss.transitions()) {
                    if (t.source() == state && t.label().equals(label)) {
                        next.add(t.target());
                    }
                }
            }
            if (next.isEmpty()) return Set.of();
            current = next;
        }
        return current;
    }

    // -----------------------------------------------------------------------
    // Lattice determines failures
    // -----------------------------------------------------------------------

    /**
     * Verify that the lattice ordering determines the failure structure.
     *
     * <p>Two properties are checked over the common local alphabet of each pair:
     * <ol>
     *   <li><b>Meet-refusal property</b>: shared refusals of s1 and s2 are
     *       also refused at meet(s1, s2)</li>
     *   <li><b>Join-acceptance property</b>: shared acceptances of s1 and s2
     *       are also accepted at join(s1, s2)</li>
     * </ol>
     *
     * @return true if the state space is a lattice and both properties hold for all pairs
     */
    public static boolean latticeDeterminesFailures(StateSpace ss) {
        var lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return false;

        var allRefusals = computeAllRefusals(ss);
        var allAcceptances = new HashMap<Integer, Set<String>>();
        for (int state : ss.states()) {
            allAcceptances.put(state, ss.enabledLabels(state));
        }

        var statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);

        for (int i = 0; i < statesList.size(); i++) {
            int s1 = statesList.get(i);
            for (int j = i + 1; j < statesList.size(); j++) {
                int s2 = statesList.get(j);

                // Common local alphabet: labels enabled at s1 or s2
                var localAlpha = new HashSet<>(allAcceptances.get(s1));
                localAlpha.addAll(allAcceptances.get(s2));
                if (localAlpha.isEmpty()) continue;

                // Property 1: Meet-refusal over common alphabet
                Integer m = LatticeChecker.computeMeet(ss, s1, s2);
                if (m != null) {
                    var meetRef = new HashSet<>(allRefusals.get(m));
                    meetRef.retainAll(localAlpha);

                    var sharedRef = new HashSet<>(allRefusals.get(s1));
                    sharedRef.retainAll(allRefusals.get(s2));
                    sharedRef.retainAll(localAlpha);

                    if (!meetRef.containsAll(sharedRef)) return false;
                }

                // Property 2: Join-acceptance over common alphabet
                Integer jo = LatticeChecker.computeJoin(ss, s1, s2);
                if (jo != null) {
                    var joinAcc = new HashSet<>(allAcceptances.get(jo));
                    joinAcc.retainAll(localAlpha);

                    var sharedAcc = new HashSet<>(allAcceptances.get(s1));
                    sharedAcc.retainAll(allAcceptances.get(s2));
                    sharedAcc.retainAll(localAlpha);

                    if (!joinAcc.containsAll(sharedAcc)) return false;
                }
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Full failure analysis
    // -----------------------------------------------------------------------

    /**
     * Perform complete failure analysis on a state space.
     */
    public static FailureAnalysisResult analyzeFailures(StateSpace ss) {
        var refusalMap = computeAllRefusals(ss);
        var acceptanceMap = new HashMap<Integer, Set<String>>();
        for (int state : ss.states()) {
            acceptanceMap.put(state, ss.enabledLabels(state));
        }
        boolean deterministic = isDeterministic(ss);
        boolean latDet = latticeDeterminesFailures(ss);

        String details;
        if (latDet) {
            details = "Lattice meet/join fully determine failure structure.";
        } else {
            var lr = LatticeChecker.checkLattice(ss);
            if (!lr.isLattice()) {
                details = "State space is not a lattice; lattice-determines check not applicable.";
            } else {
                details = "Lattice meet/join do not fully determine failure structure.";
            }
        }

        return new FailureAnalysisResult(
                refusalMap, Map.copyOf(acceptanceMap), deterministic, latDet, details);
    }
}
