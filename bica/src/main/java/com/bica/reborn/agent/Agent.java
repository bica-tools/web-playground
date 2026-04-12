package com.bica.reborn.agent;

import com.bica.reborn.agent.compose.ComposedAgent;

/**
 * A session-typed agent: an entity with a formal protocol, a name, and behavior.
 *
 * <p>This is the core abstraction of the multi-agent framework. Every agent has:
 * <ul>
 *   <li>A {@link AgentProtocol} — the session type governing its communication</li>
 *   <li>A name — for registry lookup and observability</li>
 *   <li>A handler — the behavior executed when the agent is invoked</li>
 * </ul>
 *
 * <p>Agents are either leaf agents ({@link SimpleAgent}) backed by handler lambdas,
 * or composed agents ({@link ComposedAgent}) built by algebraic composition
 * (sequencing, parallel, branching).
 *
 * <p>The protocol is enforced at two levels:
 * <ul>
 *   <li><strong>Compile-time</strong>: The {@code @Session} annotation processor
 *       validates method signatures against the session type.</li>
 *   <li><strong>Runtime</strong>: The {@link com.bica.reborn.agent.runtime.AgentRuntime}
 *       wraps each agent in a {@code SessionMonitor} that validates every transition.</li>
 * </ul>
 */
public interface Agent {

    /** The session type protocol this agent follows. */
    AgentProtocol protocol();

    /** Human-readable name (used for registry lookup and logging). */
    String name();

    /**
     * Handle one invocation of this agent.
     *
     * <p>For {@link SimpleAgent}, this delegates to the handler lambda.
     * For {@link ComposedAgent}, this orchestrates sub-agents according
     * to the composition structure (sequence, parallel, branch).
     *
     * @param context the runtime context with protocol state, enabled methods, and payload
     * @return the result of the invocation
     */
    AgentResult handle(AgentContext context);
}
