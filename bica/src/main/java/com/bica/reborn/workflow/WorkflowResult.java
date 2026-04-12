package com.bica.reborn.workflow;

import java.util.List;
import java.util.Objects;

/**
 * Result from processing one research step through the workflow.
 *
 * @param stepNumber  The step identifier (e.g. "23", "70b").
 * @param phases      Results from each phase of the step lifecycle.
 * @param accepted    True if the step achieved A+ grade.
 * @param grade       The grade assigned ("A+", "A", "B", "C", "F").
 * @param fixes       List of required fixes if not accepted.
 * @param violations  Protocol violations detected during execution.
 * @param elapsedMs   Total execution time in milliseconds.
 */
public record WorkflowResult(
        String stepNumber,
        List<PhaseResult> phases,
        boolean accepted,
        String grade,
        List<String> fixes,
        List<String> violations,
        double elapsedMs) {

    public WorkflowResult {
        Objects.requireNonNull(stepNumber, "stepNumber must not be null");
        Objects.requireNonNull(phases, "phases must not be null");
        Objects.requireNonNull(grade, "grade must not be null");
        Objects.requireNonNull(fixes, "fixes must not be null");
        Objects.requireNonNull(violations, "violations must not be null");
        phases = List.copyOf(phases);
        fixes = List.copyOf(fixes);
        violations = List.copyOf(violations);
    }

    /**
     * Result from one phase (sequential or parallel) of the step lifecycle.
     *
     * @param name     Phase name (e.g. "research", "implement || write").
     * @param agents   Agent execution records within this phase.
     * @param parallel Whether agents in this phase ran in parallel.
     */
    public record PhaseResult(String name, List<AgentExecution> agents, boolean parallel) {

        public PhaseResult {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(agents, "agents must not be null");
            agents = List.copyOf(agents);
        }

        /** True if all agents in this phase succeeded. */
        public boolean allOk() {
            return agents.stream().allMatch(AgentExecution::success);
        }
    }

    /**
     * Execution record for one agent invocation.
     *
     * @param agentType   The agent type (e.g. "Researcher", "Implementer").
     * @param agentId     Unique identifier for this execution.
     * @param transport   Transport protocol ("MCP" or "A2A").
     * @param success     True if the agent completed without violation.
     * @param violated    True if a protocol violation was detected.
     * @param transitions The session type transitions taken.
     * @param elapsedMs   Execution time in milliseconds.
     */
    public record AgentExecution(
            String agentType,
            String agentId,
            String transport,
            boolean success,
            boolean violated,
            List<String> transitions,
            double elapsedMs) {

        public AgentExecution {
            Objects.requireNonNull(agentType, "agentType must not be null");
            Objects.requireNonNull(agentId, "agentId must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            Objects.requireNonNull(transitions, "transitions must not be null");
            transitions = List.copyOf(transitions);
        }
    }
}
