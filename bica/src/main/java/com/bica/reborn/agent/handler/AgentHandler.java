package com.bica.reborn.agent.handler;

import com.bica.reborn.agent.AgentContext;
import com.bica.reborn.agent.AgentResult;

/**
 * A handler that processes one invocation of an agent.
 *
 * <p>This is the core abstraction for agent behavior. Implement this interface
 * (or use a lambda) to define what an agent does when invoked. The handler
 * receives an {@link AgentContext} with the current protocol state and payload,
 * and returns an {@link AgentResult} describing the outcome.
 *
 * <p>The handler is responsible for making transitions through the protocol
 * by returning results with appropriate selection outcomes and transition lists.
 * The runtime validates these transitions against the session type.
 */
@FunctionalInterface
public interface AgentHandler {

    /**
     * Handle one invocation of the agent.
     *
     * @param context the runtime context with protocol state, enabled methods, and payload
     * @return the result of the invocation
     */
    AgentResult handle(AgentContext context);
}
