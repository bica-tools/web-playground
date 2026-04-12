package com.bica.reborn.agent.handler;

import com.bica.reborn.agent.AgentContext;
import com.bica.reborn.agent.AgentException;
import com.bica.reborn.agent.AgentResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handler that dispatches to sub-handlers based on the branch label.
 *
 * <p>Use this when an agent's protocol starts with a branch {@code &{a: ..., b: ...}}
 * and different methods should trigger different behavior.
 *
 * <pre>{@code
 * var handler = BranchHandler.builder()
 *     .on("read", ctx -> AgentResult.success(Map.of("data", readFile())))
 *     .on("write", ctx -> { writeFile(ctx.payload()); return AgentResult.success(Map.of()); })
 *     .build();
 * }</pre>
 */
public final class BranchHandler implements AgentHandler {

    private final Map<String, AgentHandler> branches;
    private final AgentHandler fallback;

    private BranchHandler(Map<String, AgentHandler> branches, AgentHandler fallback) {
        this.branches = Map.copyOf(branches);
        this.fallback = fallback;
    }

    @Override
    public AgentResult handle(AgentContext context) {
        // Find which enabled method to dispatch on
        for (String method : context.enabledMethods()) {
            AgentHandler handler = branches.get(method);
            if (handler != null) {
                return handler.handle(context);
            }
        }
        if (fallback != null) {
            return fallback.handle(context);
        }
        throw new AgentException("No handler for enabled methods " + context.enabledMethods()
                + " in agent " + context.agentName());
    }

    /** Create a builder for constructing a BranchHandler. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, AgentHandler> branches = new HashMap<>();
        private AgentHandler fallback;

        private Builder() {}

        /** Register a handler for a specific branch label. */
        public Builder on(String label, AgentHandler handler) {
            Objects.requireNonNull(label, "label must not be null");
            Objects.requireNonNull(handler, "handler must not be null");
            branches.put(label, handler);
            return this;
        }

        /** Register a fallback handler for unmatched labels. */
        public Builder fallback(AgentHandler handler) {
            this.fallback = handler;
            return this;
        }

        public BranchHandler build() {
            return new BranchHandler(branches, fallback);
        }
    }
}
