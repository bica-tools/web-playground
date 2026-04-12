package com.bica.reborn.equivalence;

import com.bica.reborn.ccs.CCSChecker;
import com.bica.reborn.ccs.LTS;
import com.bica.reborn.csp.CSPChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;

/**
 * Process equivalence coincidence checker for session type state spaces.
 *
 * <p>Proves that for session type state spaces (deterministic, finite,
 * lattice-structured), three classical process equivalences COINCIDE:
 * <ol>
 *   <li><b>CCS strong bisimulation</b> (Milner)</li>
 *   <li><b>CSP trace equivalence</b> (Hoare)</li>
 *   <li><b>CSP failure equivalence</b> (Hoare/Roscoe)</li>
 * </ol>
 *
 * <p>The key insight: session type state spaces are deterministic and have
 * finite, lattice-ordered reachability structure. Under these conditions,
 * all three notions coincide.
 */
public final class ProcessEquivalenceChecker {

    private static final int DEFAULT_MAX_DEPTH = 10;

    private ProcessEquivalenceChecker() {}

    // -----------------------------------------------------------------------
    // Core: check all three equivalences on a pair
    // -----------------------------------------------------------------------

    /**
     * Run CCS bisimulation, CSP trace equivalence, and CSP failure equivalence
     * on two state spaces.
     *
     * @param ss1      first state space
     * @param ss2      second state space
     * @param maxDepth depth bound for trace/failure enumeration
     * @return a {@link ProcessEquivalenceResult} with all three verdicts
     */
    public static ProcessEquivalenceResult checkAllEquivalences(
            StateSpace ss1, StateSpace ss2, int maxDepth) {

        // 1. CCS bisimulation: convert state spaces to LTS and compare
        LTS lts1 = stateSpaceToLTS(ss1);
        LTS lts2 = stateSpaceToLTS(ss2);
        boolean bisim = CCSChecker.checkBisimulation(lts1, lts2);

        // 2. CSP trace equivalence
        boolean traceEq = CSPChecker.traceEquivalent(ss1, ss2, maxDepth);

        // 3. CSP failure equivalence
        boolean failEq = CSPChecker.failuresEquivalent(ss1, ss2, maxDepth);

        boolean det1 = CSPChecker.isDeterministic(ss1);
        boolean det2 = CSPChecker.isDeterministic(ss2);

        boolean allAgree = (bisim == traceEq) && (traceEq == failEq);

        String details;
        if (allAgree) {
            String verdict = bisim ? "equivalent" : "not equivalent";
            details = String.format(
                    "All three process equivalences agree: %s. " +
                    "ss1 deterministic=%b, ss2 deterministic=%b.",
                    verdict, det1, det2);
        } else {
            details = String.format(
                    "DISAGREEMENT: bisimulation=%b, trace_eq=%b, failure_eq=%b. " +
                    "ss1 deterministic=%b, ss2 deterministic=%b.",
                    bisim, traceEq, failEq, det1, det2);
        }

        return new ProcessEquivalenceResult(
                bisim, traceEq, failEq, allAgree, det1, det2, details);
    }

    /** Overload with default max depth. */
    public static ProcessEquivalenceResult checkAllEquivalences(
            StateSpace ss1, StateSpace ss2) {
        return checkAllEquivalences(ss1, ss2, DEFAULT_MAX_DEPTH);
    }

    // -----------------------------------------------------------------------
    // Coincidence check
    // -----------------------------------------------------------------------

    /**
     * Check whether all three process equivalences agree on the given pair.
     *
     * @param ss1 first state space
     * @param ss2 second state space
     * @return true iff bisimulation, trace equivalence, and failure equivalence
     *         all give the same verdict
     */
    public static boolean checkCoincidence(StateSpace ss1, StateSpace ss2) {
        return checkAllEquivalences(ss1, ss2).allAgree();
    }

    /**
     * Check coincidence with a custom depth bound.
     */
    public static boolean checkCoincidence(StateSpace ss1, StateSpace ss2, int maxDepth) {
        return checkAllEquivalences(ss1, ss2, maxDepth).allAgree();
    }

    // -----------------------------------------------------------------------
    // Sub-state-space construction
    // -----------------------------------------------------------------------

    /**
     * Build a sub-state-space rooted at the given state.
     *
     * <p>Restricts the original state space to states reachable from {@code root}.
     * The bottom is the original bottom if reachable, otherwise a terminal state.
     *
     * @param ss   the original state space
     * @param root the root state for the sub-space
     * @return a new StateSpace rooted at root, or empty if root has no reachable states
     */
    public static Optional<StateSpace> subStateSpace(StateSpace ss, int root) {
        Set<Integer> reachable = ss.reachableFrom(root);
        if (reachable.isEmpty()) {
            return Optional.empty();
        }

        var subTransitions = new ArrayList<StateSpace.Transition>();
        for (var t : ss.transitions()) {
            if (reachable.contains(t.source()) && reachable.contains(t.target())) {
                subTransitions.add(t);
            }
        }

        var subLabels = new HashMap<Integer, String>();
        for (int s : reachable) {
            String lbl = ss.labels().get(s);
            if (lbl != null) {
                subLabels.put(s, lbl);
            }
        }

        int bottom;
        if (reachable.contains(ss.bottom())) {
            bottom = ss.bottom();
        } else {
            // Find a terminal state (no outgoing transitions)
            Set<Integer> sources = new HashSet<>();
            for (var t : subTransitions) {
                sources.add(t.source());
            }
            int found = root;
            for (int s : reachable) {
                if (!sources.contains(s)) {
                    found = s;
                    break;
                }
            }
            bottom = found;
        }

        return Optional.of(new StateSpace(reachable, subTransitions, root, bottom, subLabels));
    }

    // -----------------------------------------------------------------------
    // LTS conversion
    // -----------------------------------------------------------------------

    /**
     * Convert a session-type {@link StateSpace} to a CCS {@link LTS}.
     *
     * <p>Selection transitions get a {@code tau_} prefix to match CCS convention
     * (internal/silent actions).
     */
    static LTS stateSpaceToLTS(StateSpace ss) {
        var transitions = new ArrayList<LTS.LTSTransition>();
        for (var t : ss.transitions()) {
            String action = (t.kind() == TransitionKind.SELECTION)
                    ? "tau_" + t.label()
                    : t.label();
            transitions.add(new LTS.LTSTransition(t.source(), action, t.target()));
        }
        var labels = new HashMap<Integer, String>();
        for (int s : ss.states()) {
            labels.put(s, ss.labels().getOrDefault(s, String.valueOf(s)));
        }
        return new LTS(ss.states(), transitions, ss.top(), labels);
    }
}
