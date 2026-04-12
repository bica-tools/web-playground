package com.bica.reborn.agent;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Runtime context passed to agent handlers during invocation.
 *
 * <p>Provides the handler with everything it needs: current protocol state,
 * enabled methods, payload from the caller, and the ability to step through
 * the protocol via {@link #step(String)}.
 *
 * @param agentName         name of the agent being invoked
 * @param currentState      current state ID in the state space
 * @param enabledMethods    methods available at the current state
 * @param enabledSelections selection labels available at the current state
 * @param payload           input data from the caller
 * @param protocol          the agent's validated protocol
 */
public record AgentContext(
        String agentName,
        int currentState,
        Set<String> enabledMethods,
        Set<String> enabledSelections,
        Map<String, Object> payload,
        AgentProtocol protocol) {

    public AgentContext {
        Objects.requireNonNull(agentName, "agentName must not be null");
        Objects.requireNonNull(enabledMethods, "enabledMethods must not be null");
        Objects.requireNonNull(enabledSelections, "enabledSelections must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(protocol, "protocol must not be null");
        enabledMethods = Set.copyOf(enabledMethods);
        enabledSelections = Set.copyOf(enabledSelections);
        payload = Map.copyOf(payload);
    }

    /** Create initial context for an agent at its top state. */
    public static AgentContext initial(String agentName, AgentProtocol protocol, Map<String, Object> payload) {
        var ss = protocol.stateSpace();
        int top = ss.top();
        return new AgentContext(
                agentName, top,
                ss.enabledMethods(top),
                ss.enabledSelections(top),
                payload, protocol);
    }

    /** Whether the current state is the terminal (bottom) state. */
    public boolean isComplete() {
        return currentState == protocol.stateSpace().bottom();
    }

    /** Whether a given label is currently enabled (method or selection). */
    public boolean isEnabled(String label) {
        return enabledMethods.contains(label) || enabledSelections.contains(label);
    }
}
