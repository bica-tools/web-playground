package com.bica.reborn.agent.handler;

import com.bica.reborn.agent.AgentContext;

/**
 * Decides which selection label to take at a {@code +{...}} node.
 *
 * <p>In session types, selections represent <em>internal choice</em>: the agent
 * (server) decides which branch to take. This interface models that decision.
 *
 * <pre>{@code
 * SelectionDecider decider = ctx -> {
 *     boolean valid = validate(ctx.payload());
 *     return valid ? "OK" : "ERROR";
 * };
 * }</pre>
 */
@FunctionalInterface
public interface SelectionDecider {

    /**
     * Given the current context, decide which selection label to take.
     *
     * @param context the runtime context
     * @return the selection label (must be one of {@code context.enabledSelections()})
     */
    String decide(AgentContext context);
}
