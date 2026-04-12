package com.bica.reborn.agent.runtime;

/**
 * Observer for agent runtime events.
 *
 * <p>Register listeners with {@link AgentRuntime.Builder#addEventListener(AgentEventListener)}
 * to receive notifications for every protocol transition, violation, completion, and error.
 */
@FunctionalInterface
public interface AgentEventListener {

    /** Called when an agent event occurs. */
    void onEvent(AgentEvent event);
}
