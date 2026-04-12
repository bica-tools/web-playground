package com.bica.reborn.agent;

import com.bica.reborn.agent.handler.AgentHandler;

import java.util.Objects;

/**
 * A leaf agent backed by a handler lambda.
 *
 * <p>This is the simplest way to create an agent: provide a name, a session type
 * string, and a handler that implements the agent's behavior.
 *
 * <pre>{@code
 * var researcher = SimpleAgent.of(
 *     "Researcher",
 *     "&{investigate: +{report: end}}",
 *     ctx -> {
 *         var findings = doResearch(ctx.payload());
 *         return AgentResult.success("report", findings,
 *             List.of("investigate", "report"));
 *     });
 * }</pre>
 *
 * <p>The protocol is validated at construction time: if the session type does
 * not form a lattice, construction fails with an {@link AgentException}.
 */
public final class SimpleAgent implements Agent {

    private final String name;
    private final AgentProtocol protocol;
    private final AgentHandler handler;

    private SimpleAgent(String name, AgentProtocol protocol, AgentHandler handler) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.protocol = Objects.requireNonNull(protocol, "protocol must not be null");
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
    }

    /**
     * Create a simple agent with a validated protocol.
     *
     * @param name        human-readable name
     * @param sessionType session type string in BICA syntax
     * @param handler     the behavior lambda
     * @throws AgentException if the session type does not form a lattice
     */
    public static SimpleAgent of(String name, String sessionType, AgentHandler handler) {
        return new SimpleAgent(name, AgentProtocol.of(sessionType), handler);
    }

    /**
     * Create a simple agent with a pre-validated protocol.
     */
    public static SimpleAgent of(String name, AgentProtocol protocol, AgentHandler handler) {
        return new SimpleAgent(name, protocol, handler);
    }

    @Override
    public AgentProtocol protocol() {
        return protocol;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public AgentResult handle(AgentContext context) {
        return handler.handle(context);
    }

    @Override
    public String toString() {
        return "SimpleAgent[" + name + ", " + protocol.typeString() + "]";
    }
}
