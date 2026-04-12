package com.bica.reborn.typestate;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;

/**
 * Pure state machine simulator for typestate checking.
 *
 * <p>Given a {@link StateSpace} and a sequence of {@link MethodCall}s,
 * verifies that each call is enabled in the current state and tracks
 * state transitions. Reports errors for disabled methods and for
 * variables not in terminal state at method return.
 *
 * <p>Handles selection (internal choice) states transparently: when the
 * current state is a pure selection state (only SELECTION transitions,
 * no callable methods), the checker auto-advances past it by trying
 * each selection branch to find one where the next method call is valid.
 *
 * <p>This class has no dependency on any compiler or IDE API — it operates
 * purely on the abstract {@code List<MethodCall>} representation.
 */
public final class TypestateChecker {

    private TypestateChecker() {}

    /**
     * Check a sequence of method calls against a state space.
     *
     * @param stateSpace    the protocol state machine
     * @param calls         ordered sequence of method calls
     * @param initialStates variable → initial state (use {@code stateSpace.top()} for new objects)
     * @param checkEnd      if true, report errors for variables not in bottom state at end
     * @return the typestate result with errors and final states
     */
    public static TypestateResult check(StateSpace stateSpace, List<MethodCall> calls,
                                         Map<String, Integer> initialStates, boolean checkEnd) {
        var currentStates = new HashMap<>(initialStates);
        var errors = new ArrayList<TypestateError>();

        for (MethodCall call : calls) {
            Integer state = currentStates.get(call.variable());
            if (state == null) {
                continue;
            }

            // Auto-advance past pure selection states
            state = advancePastSelections(stateSpace, state, call.method());
            currentStates.put(call.variable(), state);

            Set<String> enabled = stateSpace.enabledLabels(state);
            var next = stateSpace.step(state, call.method());

            if (next.isEmpty()) {
                errors.add(new TypestateError(
                        call.variable(),
                        call.method(),
                        state,
                        enabled,
                        String.format("Method '%s' is not enabled on '%s' in state %d (enabled: %s)",
                                call.method(), call.variable(), state, enabled)));
            } else {
                currentStates.put(call.variable(), next.getAsInt());
            }
        }

        // Check end-of-method: all tracked variables should be in bottom state
        if (checkEnd) {
            for (var entry : currentStates.entrySet()) {
                int finalState = advancePastSelectionsToBottom(stateSpace, entry.getValue());
                if (finalState != stateSpace.bottom()) {
                    Set<String> enabled = stateSpace.enabledLabels(finalState);
                    errors.add(new TypestateError(
                            entry.getKey(),
                            "<end-of-method>",
                            finalState,
                            enabled,
                            String.format("Variable '%s' is not in terminal state at end of method "
                                    + "(state %d, enabled: %s)",
                                    entry.getKey(), finalState, enabled)));
                }
            }
        }

        return new TypestateResult(errors.isEmpty(), errors, Map.copyOf(currentStates));
    }

    /**
     * Convenience overload: all variables start at {@code stateSpace.top()},
     * end-of-method check enabled.
     */
    public static TypestateResult check(StateSpace stateSpace, List<MethodCall> calls) {
        var variables = new HashSet<String>();
        for (var call : calls) {
            variables.add(call.variable());
        }
        var initialStates = new HashMap<String, Integer>();
        for (var v : variables) {
            initialStates.put(v, stateSpace.top());
        }
        return check(stateSpace, calls, initialStates, true);
    }

    /**
     * Check a tree of call nodes against a state space.
     *
     * <p>This method handles {@link CallNode.SelectionSwitch} nodes by forking:
     * when at a pure selection state, each selection branch is checked independently
     * against the corresponding call node branch. All branches must be valid.
     *
     * @param stateSpace    the protocol state machine
     * @param nodes         tree-structured call sequence
     * @param variable      the variable name being checked
     * @param initialState  starting state for the variable
     * @param checkEnd      if true, report errors for not reaching bottom
     * @return the typestate result
     */
    public static TypestateResult checkCallTree(StateSpace stateSpace, List<CallNode> nodes,
                                                  String variable, int initialState,
                                                  boolean checkEnd) {
        var errors = new ArrayList<TypestateError>();
        var finalStates = new ArrayList<Integer>();
        checkCallNodes(stateSpace, nodes, variable, initialState, errors, finalStates, checkEnd);

        // Use the first final state (or initial if empty) for the result map
        int resultState = finalStates.isEmpty() ? initialState : finalStates.get(0);
        return new TypestateResult(errors.isEmpty(), errors, Map.of(variable, resultState));
    }

    // -- private: selection auto-advance ----------------------------------------

    /**
     * If {@code state} is a pure selection state, try each selection transition
     * and return the target where {@code method} is enabled. If no match found
     * (or state is not pure selection), return the original state.
     */
    private static int advancePastSelections(StateSpace ss, int state, String method) {
        int current = state;
        // Follow selection transitions (possibly multiple levels) to find the method
        for (int depth = 0; depth < 10; depth++) {
            if (!isPureSelectionState(ss, current)) {
                return current;
            }
            // Try each selection branch
            int found = -1;
            for (var t : ss.transitionsFrom(current)) {
                if (t.kind() == TransitionKind.SELECTION) {
                    // Check if method is directly enabled at target, or target is also selection
                    int target = t.target();
                    if (ss.enabledMethods(target).contains(method)) {
                        found = target;
                        break;
                    }
                    // Recursively check if method is reachable through nested selections
                    int nested = advancePastSelections(ss, target, method);
                    if (nested != target && ss.enabledMethods(nested).contains(method)) {
                        found = nested;
                        break;
                    }
                }
            }
            if (found == -1) {
                return current; // No matching branch
            }
            current = found;
        }
        return current;
    }

    /**
     * If state is a pure selection state and any branch leads to bottom,
     * advance to bottom. Used for end-of-method checks.
     */
    private static int advancePastSelectionsToBottom(StateSpace ss, int state) {
        if (!isPureSelectionState(ss, state)) {
            return state;
        }
        for (var t : ss.transitionsFrom(state)) {
            if (t.kind() == TransitionKind.SELECTION) {
                int target = advancePastSelectionsToBottom(ss, t.target());
                if (target == ss.bottom()) {
                    return target;
                }
            }
        }
        return state;
    }

    /**
     * Returns true if state has only SELECTION transitions (no callable methods).
     */
    static boolean isPureSelectionState(StateSpace ss, int state) {
        return ss.enabledMethods(state).isEmpty() && !ss.enabledSelections(state).isEmpty();
    }

    // -- private: call tree checking --------------------------------------------

    private static void checkCallNodes(StateSpace ss, List<CallNode> nodes, String variable,
                                         int state, List<TypestateError> errors,
                                         List<Integer> finalStates, boolean checkEnd) {
        int current = state;

        for (int i = 0; i < nodes.size(); i++) {
            CallNode node = nodes.get(i);
            switch (node) {
                case CallNode.Call c -> {
                    // Auto-advance past selections
                    current = advancePastSelections(ss, current, c.call().method());

                    Set<String> enabled = ss.enabledLabels(current);
                    var next = ss.step(current, c.call().method());
                    if (next.isEmpty()) {
                        errors.add(new TypestateError(
                                variable, c.call().method(), current, enabled,
                                String.format("Method '%s' is not enabled on '%s' in state %d (enabled: %s)",
                                        c.call().method(), variable, current, enabled)));
                    } else {
                        current = next.getAsInt();
                    }
                }
                case CallNode.SelectionSwitch sw -> {
                    // Current state must be a pure selection state
                    if (!isPureSelectionState(ss, current)) {
                        errors.add(new TypestateError(
                                variable, "<selection-switch>", current,
                                ss.enabledLabels(current),
                                String.format("Expected selection state for switch on '%s', "
                                        + "but state %d has callable methods: %s",
                                        sw.variable(), current, ss.enabledMethods(current))));
                        continue;
                    }

                    // Remaining nodes after this switch
                    List<CallNode> remaining = nodes.subList(i + 1, nodes.size());

                    // Check each branch independently, then continue with remaining nodes
                    for (var branch : sw.branches().entrySet()) {
                        String label = branch.getKey();
                        List<CallNode> branchNodes = branch.getValue();

                        var selStep = ss.step(current, label);
                        if (selStep.isEmpty()) {
                            errors.add(new TypestateError(
                                    variable, label, current, ss.enabledSelections(current),
                                    String.format("Selection label '%s' not available in state %d",
                                            label, current)));
                            continue;
                        }

                        int branchState = selStep.getAsInt();

                        // Combine branch body + remaining nodes after switch
                        var combinedNodes = new ArrayList<CallNode>(branchNodes);
                        combinedNodes.addAll(remaining);

                        checkCallNodes(ss, combinedNodes, variable, branchState,
                                errors, finalStates, checkEnd);
                    }
                    return; // All continuations handled recursively
                }
            }
        }

        finalStates.add(current);

        if (checkEnd) {
            int adjusted = advancePastSelectionsToBottom(ss, current);
            if (adjusted != ss.bottom()) {
                Set<String> enabled = ss.enabledLabels(adjusted);
                errors.add(new TypestateError(
                        variable, "<end-of-method>", adjusted, enabled,
                        String.format("Variable '%s' is not in terminal state at end of method "
                                + "(state %d, enabled: %s)", variable, adjusted, enabled)));
            }
        }
    }
}
