package com.bica.reborn.agent.runtime;

import com.bica.reborn.agent.*;
import com.bica.reborn.agent.verify.AgentVerifier;
import com.bica.reborn.agent.verify.VerificationResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The execution engine for session-typed agents.
 *
 * <p>Manages agent lifecycles, protocol enforcement, and event dispatch.
 * Every registered agent is wrapped in a monitored context that validates
 * each transition against the session type state space.
 *
 * <pre>{@code
 * var runtime = AgentRuntime.builder()
 *     .violationPolicy(ViolationPolicy.THROW)
 *     .addEventListener(event -> System.out.println(event))
 *     .verify(true)
 *     .build();
 *
 * runtime.register(myAgent);
 * AgentResult result = runtime.invoke("myAgent", Map.of("input", "data"));
 * }</pre>
 */
public final class AgentRuntime {

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, List<String>> traces = new ConcurrentHashMap<>();
    private final ViolationPolicy violationPolicy;
    private final List<AgentEventListener> listeners;
    private final boolean preVerify;

    private AgentRuntime(ViolationPolicy violationPolicy, List<AgentEventListener> listeners, boolean preVerify) {
        this.violationPolicy = violationPolicy;
        this.listeners = new CopyOnWriteArrayList<>(listeners);
        this.preVerify = preVerify;
    }

    /**
     * Register an agent. Runs verification if enabled.
     *
     * @throws AgentException if verification fails
     */
    public void register(Agent agent) {
        Objects.requireNonNull(agent, "agent must not be null");
        if (preVerify) {
            VerificationResult vr = AgentVerifier.verify(agent);
            if (!vr.verified()) {
                throw new AgentException("Agent '" + agent.name() + "' failed verification: "
                        + vr.summary());
            }
        }
        agents.put(agent.name(), agent);
        traces.put(agent.name(), new ArrayList<>());
        fireEvent(new AgentEvent.Registered(agent.name(), agent.protocol(), System.currentTimeMillis()));
    }

    /**
     * Invoke an agent by name, stepping through its protocol.
     *
     * @param agentName the registered agent name
     * @param payload   input data for the agent
     * @return the result of the invocation
     * @throws AgentException if the agent is not registered or violates its protocol
     */
    public AgentResult invoke(String agentName, Map<String, Object> payload) {
        Agent agent = agents.get(agentName);
        if (agent == null) {
            throw new AgentException("Agent not registered: " + agentName);
        }

        long startMs = System.currentTimeMillis();
        AgentContext context = AgentContext.initial(agentName, agent.protocol(), payload);

        try {
            AgentResult result = agent.handle(context);

            // Record transitions
            List<String> trace = traces.get(agentName);
            trace.addAll(result.transitionsTaken());

            // Validate transitions against state space
            validateTransitions(agent, result.transitionsTaken());

            long elapsed = System.currentTimeMillis() - startMs;
            fireEvent(new AgentEvent.Completed(agentName, result.transitionsTaken(), elapsed));

            return result;
        } catch (AgentException e) {
            fireEvent(new AgentEvent.Error(agentName, e.getMessage(), e, System.currentTimeMillis()));
            throw e;
        } catch (Exception e) {
            fireEvent(new AgentEvent.Error(agentName, e.getMessage(), e, System.currentTimeMillis()));
            throw new AgentException("Agent '" + agentName + "' failed: " + e.getMessage(), e);
        }
    }

    /** Get the registered agent names. */
    public Set<String> agentNames() {
        return Set.copyOf(agents.keySet());
    }

    /** Get the trace of transitions for an agent. */
    public List<String> trace(String agentName) {
        return List.copyOf(traces.getOrDefault(agentName, List.of()));
    }

    /** Check if an agent is registered. */
    public boolean isRegistered(String agentName) {
        return agents.containsKey(agentName);
    }

    private void validateTransitions(Agent agent, List<String> transitions) {
        StateSpace ss = agent.protocol().stateSpace();
        int state = ss.top();
        for (String label : transitions) {
            var step = ss.step(state, label);
            if (step.isEmpty()) {
                handleViolation(agent.name(), label, ss.enabledLabels(state));
                return;
            }
            int fromState = state;
            state = step.getAsInt();
            fireEvent(new AgentEvent.TransitionTaken(
                    agent.name(), label, fromState, state, System.currentTimeMillis()));
        }
    }

    private void handleViolation(String agentName, String attempted, Set<String> enabled) {
        fireEvent(new AgentEvent.Violation(agentName, attempted, enabled, System.currentTimeMillis()));
        switch (violationPolicy) {
            case THROW -> throw new AgentException(
                    "Protocol violation in '" + agentName + "': attempted '" + attempted
                            + "' but enabled = " + enabled);
            case LOG_AND_CONTINUE -> System.err.println(
                    "[VIOLATION] " + agentName + ": " + attempted + " not in " + enabled);
            case QUARANTINE -> {
                agents.remove(agentName);
                System.err.println("[QUARANTINE] " + agentName + " removed after violation");
            }
        }
    }

    private void fireEvent(AgentEvent event) {
        for (AgentEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    /** Create a builder for configuring the runtime. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ViolationPolicy violationPolicy = ViolationPolicy.THROW;
        private final List<AgentEventListener> listeners = new ArrayList<>();
        private boolean preVerify = false;

        private Builder() {}

        public Builder violationPolicy(ViolationPolicy policy) {
            this.violationPolicy = Objects.requireNonNull(policy);
            return this;
        }

        public Builder addEventListener(AgentEventListener listener) {
            this.listeners.add(Objects.requireNonNull(listener));
            return this;
        }

        public Builder verify(boolean preVerify) {
            this.preVerify = preVerify;
            return this;
        }

        public AgentRuntime build() {
            return new AgentRuntime(violationPolicy, listeners, preVerify);
        }
    }
}
