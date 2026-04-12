package com.bica.reborn.agent.runtime;

import com.bica.reborn.agent.AgentProtocol;

import java.util.List;
import java.util.Set;

/**
 * Events emitted by the agent runtime for observability.
 *
 * <p>Sealed interface ensures exhaustive pattern matching:
 * <pre>{@code
 * switch (event) {
 *     case AgentEvent.Registered r -> log("Registered: " + r.agentName());
 *     case AgentEvent.TransitionTaken t -> log(t.agentName() + ": " + t.label());
 *     case AgentEvent.Violation v -> alert(v.agentName() + " violated protocol!");
 *     case AgentEvent.Completed c -> log(c.agentName() + " done in " + c.elapsedMs() + "ms");
 *     case AgentEvent.Error e -> error(e.agentName() + ": " + e.message());
 * }
 * }</pre>
 */
public sealed interface AgentEvent {

    /** Agent was registered in the runtime. */
    record Registered(String agentName, AgentProtocol protocol, long timestampMs) implements AgentEvent {}

    /** A protocol transition was successfully taken. */
    record TransitionTaken(String agentName, String label, int fromState, int toState, long timestampMs) implements AgentEvent {}

    /** A protocol violation was detected. */
    record Violation(String agentName, String attemptedLabel, Set<String> enabledLabels, long timestampMs) implements AgentEvent {}

    /** Agent completed its protocol successfully. */
    record Completed(String agentName, List<String> trace, long elapsedMs) implements AgentEvent {}

    /** An error occurred during agent execution. */
    record Error(String agentName, String message, Throwable cause, long timestampMs) implements AgentEvent {}
}
