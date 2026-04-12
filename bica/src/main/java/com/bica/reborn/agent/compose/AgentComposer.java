package com.bica.reborn.agent.compose;

import com.bica.reborn.agent.Agent;
import com.bica.reborn.agent.AgentProtocol;
import com.bica.reborn.ast.*;
import com.bica.reborn.parser.PrettyPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for composing agents using session type constructors.
 *
 * <p>Supports three composition modes:
 * <ul>
 *   <li>{@link #sequence(String, Agent, Agent)} — sequencing ({@code A . B})</li>
 *   <li>{@link #parallel(String, Agent, Agent)} — parallel ({@code A || B})</li>
 *   <li>{@link #branch(String, Map)} — branching ({@code &{l: A, m: B}})</li>
 * </ul>
 *
 * <p>The composed protocol is computed algebraically from the sub-agents' ASTs,
 * then validated through the full BICA pipeline (parse → build → lattice check).
 *
 * <pre>{@code
 * var pipeline = AgentComposer.builder("Pipeline")
 *     .then(researcher)
 *     .parallel(implementer, writer)
 *     .then(evaluator)
 *     .build();
 * }</pre>
 */
public final class AgentComposer {

    private AgentComposer() {}

    /** Compose two agents sequentially: first, then second. */
    public static ComposedAgent sequence(String name, Agent first, Agent second) {
        SessionType composed = new Sequence(first.protocol().ast(), second.protocol().ast());
        AgentProtocol protocol = AgentProtocol.unchecked(PrettyPrinter.pretty(composed));
        return new ComposedAgent(name, protocol, ComposedAgent.CompositionKind.SEQUENCE,
                List.of(first, second));
    }

    /** Compose two agents in parallel: left || right. */
    public static ComposedAgent parallel(String name, Agent left, Agent right) {
        SessionType composed = new Parallel(left.protocol().ast(), right.protocol().ast());
        AgentProtocol protocol = AgentProtocol.unchecked(PrettyPrinter.pretty(composed));
        return new ComposedAgent(name, protocol, ComposedAgent.CompositionKind.PARALLEL,
                List.of(left, right));
    }

    /** Compose agents as branches: &{label1: agent1, label2: agent2, ...}. */
    public static ComposedAgent branch(String name, Map<String, Agent> branches) {
        List<Branch.Choice> choices = new ArrayList<>();
        List<Agent> subAgents = new ArrayList<>();
        for (var entry : branches.entrySet()) {
            choices.add(new Branch.Choice(entry.getKey(), entry.getValue().protocol().ast()));
            subAgents.add(entry.getValue());
        }
        SessionType composed = new Branch(choices);
        AgentProtocol protocol = AgentProtocol.unchecked(PrettyPrinter.pretty(composed));
        return new ComposedAgent(name, protocol, ComposedAgent.CompositionKind.BRANCH, subAgents);
    }

    /** Create a builder for fluent composition. */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Fluent builder for multi-step compositions.
     */
    public static final class Builder {
        private final String name;
        private final List<Agent> steps = new ArrayList<>();

        private Builder(String name) {
            this.name = Objects.requireNonNull(name);
        }

        /** Add an agent as the next sequential step. */
        public Builder then(Agent agent) {
            steps.add(agent);
            return this;
        }

        /** Add a parallel fork-join as the next step. */
        public Builder parallel(Agent left, Agent right) {
            steps.add(AgentComposer.parallel(name + "_parallel_" + steps.size(), left, right));
            return this;
        }

        /**
         * Build the composed agent.
         *
         * <p>If only one step, returns that step directly (unwrapped).
         * If multiple steps, composes them sequentially via left-fold.
         */
        public ComposedAgent build() {
            if (steps.isEmpty()) {
                throw new IllegalStateException("Cannot build an empty composition");
            }
            if (steps.size() == 1 && steps.getFirst() instanceof ComposedAgent ca) {
                return ca;
            }
            // Left-fold: step1 . step2 . step3 ...
            Agent current = steps.getFirst();
            for (int i = 1; i < steps.size(); i++) {
                current = AgentComposer.sequence(
                        name + "_seq_" + i, current, steps.get(i));
            }
            if (current instanceof ComposedAgent ca) {
                return ca;
            }
            // Wrap a single SimpleAgent in a trivial sequence
            return new ComposedAgent(name, current.protocol(),
                    ComposedAgent.CompositionKind.SEQUENCE, List.of(current));
        }
    }
}
