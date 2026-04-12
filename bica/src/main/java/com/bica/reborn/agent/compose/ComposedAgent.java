package com.bica.reborn.agent.compose;

import com.bica.reborn.agent.Agent;
import com.bica.reborn.agent.AgentContext;
import com.bica.reborn.agent.AgentProtocol;
import com.bica.reborn.agent.AgentResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An agent created by algebraic composition of sub-agents.
 *
 * <p>Composed agents are built by {@link AgentComposer} using the session type
 * constructors: sequencing ({@code .}), parallel ({@code ||}), and branching
 * ({@code &{...}}).
 *
 * <p>The composed protocol is computed from the sub-agents' protocols. When
 * invoked, the composed agent orchestrates its sub-agents according to the
 * composition structure.
 */
public final class ComposedAgent implements Agent {

    /** How sub-agents are composed. */
    public enum CompositionKind { SEQUENCE, PARALLEL, BRANCH }

    private final String name;
    private final AgentProtocol protocol;
    private final CompositionKind kind;
    private final List<Agent> subAgents;

    ComposedAgent(String name, AgentProtocol protocol, CompositionKind kind, List<Agent> subAgents) {
        this.name = Objects.requireNonNull(name);
        this.protocol = Objects.requireNonNull(protocol);
        this.kind = Objects.requireNonNull(kind);
        this.subAgents = List.copyOf(subAgents);
    }

    @Override
    public AgentProtocol protocol() {
        return protocol;
    }

    @Override
    public String name() {
        return name;
    }

    /** The composition kind (SEQUENCE, PARALLEL, or BRANCH). */
    public CompositionKind kind() {
        return kind;
    }

    /** The sub-agents in this composition. */
    public List<Agent> subAgents() {
        return subAgents;
    }

    @Override
    public AgentResult handle(AgentContext context) {
        return switch (kind) {
            case SEQUENCE -> handleSequence(context);
            case PARALLEL -> handleParallel(context);
            case BRANCH -> handleBranch(context);
        };
    }

    private AgentResult handleSequence(AgentContext context) {
        Map<String, Object> accumOutput = new java.util.HashMap<>(context.payload());
        List<String> allTransitions = new java.util.ArrayList<>();
        for (Agent sub : subAgents) {
            var subCtx = AgentContext.initial(sub.name(), sub.protocol(), accumOutput);
            AgentResult result = sub.handle(subCtx);
            if (!result.success()) return result;
            accumOutput.putAll(result.output());
            allTransitions.addAll(result.transitionsTaken());
        }
        return AgentResult.success(accumOutput, allTransitions);
    }

    private AgentResult handleParallel(AgentContext context) {
        var futures = new java.util.ArrayList<java.util.concurrent.CompletableFuture<AgentResult>>();
        for (Agent sub : subAgents) {
            var subCtx = AgentContext.initial(sub.name(), sub.protocol(), context.payload());
            futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> sub.handle(subCtx)));
        }
        Map<String, Object> merged = new java.util.HashMap<>();
        List<String> allTransitions = new java.util.ArrayList<>();
        for (var future : futures) {
            AgentResult result = future.join();
            if (!result.success()) return result;
            merged.putAll(result.output());
            allTransitions.addAll(result.transitionsTaken());
        }
        return AgentResult.success(merged, allTransitions);
    }

    private AgentResult handleBranch(AgentContext context) {
        // Dispatch to the first sub-agent whose initial methods match the enabled methods
        for (Agent sub : subAgents) {
            var subMethods = sub.protocol().initialMethods();
            if (context.enabledMethods().stream().anyMatch(subMethods::contains)) {
                var subCtx = AgentContext.initial(sub.name(), sub.protocol(), context.payload());
                return sub.handle(subCtx);
            }
        }
        return AgentResult.failure("No sub-agent matches enabled methods: " + context.enabledMethods());
    }

    @Override
    public String toString() {
        return "ComposedAgent[" + name + ", " + kind + ", " + subAgents.size() + " sub-agents]";
    }
}
