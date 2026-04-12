package com.bica.reborn.monitor;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Runtime protocol conformance monitor.
 *
 * <p>Tracks the current state in a session type state space, validates
 * transitions, detects violations, and records the full trace history.
 *
 * <p>This is a <b>stateful</b> object: each instance tracks one protocol
 * session from initial state through to completion (or violation).
 *
 * <p>Usage:
 * <pre>{@code
 * var ss = StateSpaceBuilder.build(Parser.parse("&{open: &{read: end, close: end}}"));
 * var monitor = new SessionMonitor(ss);
 * MonitorResult r1 = monitor.step("open");   // success
 * MonitorResult r2 = monitor.step("read");   // success
 * MonitorResult r3 = monitor.step("write");  // violation!
 * }</pre>
 */
@Session("rec X . &{step: +{ok: X, violation: end}, reset: X, complete: end}")
public class SessionMonitor {

    private final StateSpace stateSpace;
    private final Map<Integer, Map<String, Integer>> transitionMap;
    private int currentState;
    private boolean violated;
    private final List<String> trace;

    /**
     * Create a monitor initialized at the top (initial) state of the state space.
     *
     * @param ss the state space defining the protocol
     */
    public SessionMonitor(StateSpace ss) {
        Objects.requireNonNull(ss, "state space must not be null");
        this.stateSpace = ss;
        this.transitionMap = buildTransitionMap(ss);
        this.currentState = ss.top();
        this.violated = false;
        this.trace = new ArrayList<>();
    }

    /**
     * Attempt a transition with the given label.
     *
     * <p>If the label is enabled at the current state, the monitor advances
     * and records the transition in the trace. Otherwise, the monitor enters
     * the violated state and does not advance.
     *
     * @param label the method or selection label to attempt
     * @return a {@link MonitorResult} describing the outcome
     */
    public MonitorResult step(String label) {
        Objects.requireNonNull(label, "label must not be null");

        Set<String> enabled = enabledMethods();
        int from = currentState;

        if (violated) {
            return new MonitorResult(false, from, from, label, enabled);
        }

        Map<String, Integer> outgoing = transitionMap.getOrDefault(currentState, Map.of());
        Integer target = outgoing.get(label);

        if (target == null) {
            violated = true;
            return new MonitorResult(false, from, from, label, enabled);
        }

        currentState = target;
        trace.add(label);
        return new MonitorResult(true, from, currentState, label, enabled);
    }

    /**
     * Check if the monitor has reached the bottom (terminal) state.
     *
     * @return true if the current state equals the bottom state
     */
    public boolean isComplete() {
        return currentState == stateSpace.bottom();
    }

    /**
     * Check if the protocol has been violated.
     *
     * @return true if an invalid transition was attempted
     */
    public boolean isViolation() {
        return violated;
    }

    /**
     * Return the current state ID.
     */
    public int currentState() {
        return currentState;
    }

    /**
     * Return the set of method/selection labels enabled at the current state.
     */
    public Set<String> enabledMethods() {
        if (violated) {
            return Set.of();
        }
        Map<String, Integer> outgoing = transitionMap.getOrDefault(currentState, Map.of());
        return Set.copyOf(outgoing.keySet());
    }

    /**
     * Return the set of selection labels enabled at the current state
     * (only transitions with {@link TransitionKind#SELECTION}).
     */
    public Set<String> enabledSelections() {
        if (violated) {
            return Set.of();
        }
        return stateSpace.enabledSelections(currentState);
    }

    /**
     * Reset the monitor to the initial state, clearing the trace and violation flag.
     */
    public void reset() {
        currentState = stateSpace.top();
        violated = false;
        trace.clear();
    }

    /**
     * Return the history of successfully taken transitions (labels only).
     */
    public List<String> trace() {
        return List.copyOf(trace);
    }

    /**
     * Return the underlying state space.
     */
    public StateSpace stateSpace() {
        return stateSpace;
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private static Map<Integer, Map<String, Integer>> buildTransitionMap(StateSpace ss) {
        var map = new HashMap<Integer, Map<String, Integer>>();
        for (int state : ss.states()) {
            map.put(state, new HashMap<>());
        }
        for (Transition t : ss.transitions()) {
            map.computeIfAbsent(t.source(), k -> new HashMap<>())
               .put(t.label(), t.target());
        }
        return map;
    }
}
