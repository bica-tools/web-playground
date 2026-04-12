package com.bica.reborn.workflow;

import java.util.*;
import java.util.function.Function;

/**
 * Orchestrates multiple agents following session type protocols.
 *
 * <p>Dispatches agents in phases (sequential or parallel), collects results,
 * and tracks protocol conformance across an entire sprint.
 *
 * <p>Phases per step:
 * <ol>
 *   <li>Research (sequential)</li>
 *   <li>Implement || Write (parallel, independent)</li>
 *   <li>Test || Prove (parallel, independent)</li>
 *   <li>Evaluate (sequential)</li>
 *   <li>Review if not accepted (sequential)</li>
 * </ol>
 */
public class Orchestrator {

    private final WorkflowEngine engine;
    private final Map<String, Function<String, Map<String, Object>>> handlers = new HashMap<>();
    private int sprintCount = 0;

    /**
     * Create an orchestrator with a new workflow engine.
     */
    public Orchestrator() {
        this.engine = new WorkflowEngine();
    }

    /**
     * Create an orchestrator with a given workflow engine.
     */
    public Orchestrator(WorkflowEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /**
     * Register a handler for an agent type.
     *
     * @param agentType the agent type name
     * @param handler   a function that takes a step number and returns results
     */
    public void registerHandler(String agentType, Function<String, Map<String, Object>> handler) {
        handlers.put(agentType, handler);
    }

    /**
     * Orchestrate a full sprint with the given steps.
     *
     * @param steps the step numbers to process
     * @return list of workflow results
     */
    public List<WorkflowResult> orchestrate(List<String> steps) {
        sprintCount++;
        return engine.runSprint(steps);
    }

    /**
     * Dispatch a single task to an agent.
     *
     * @param agentType  the agent type
     * @param step       the step number
     * @return the agent execution record
     */
    public WorkflowResult.AgentExecution dispatchTask(String agentType, String step) {
        return engine.executeAgent(agentType, step);
    }

    /**
     * Collect results from a completed sprint.
     *
     * @param results the list of workflow results
     * @return a summary map with conformance statistics
     */
    public Map<String, Object> collectResults(List<WorkflowResult> results) {
        int totalAgents = 0;
        int violations = 0;
        int accepted = 0;

        for (WorkflowResult wr : results) {
            if (wr.accepted()) accepted++;
            for (var phase : wr.phases()) {
                for (var agent : phase.agents()) {
                    totalAgents++;
                    if (agent.violated()) violations++;
                }
            }
        }

        double conformanceRate = totalAgents > 0
                ? (double) (totalAgents - violations) / totalAgents
                : 1.0;

        return Map.of(
                "sprintNumber", sprintCount,
                "stepsProcessed", results.size(),
                "stepsAccepted", accepted,
                "totalAgents", totalAgents,
                "violations", violations,
                "conformanceRate", conformanceRate
        );
    }

    /**
     * Get the sprint count.
     */
    public int sprintCount() {
        return sprintCount;
    }
}
