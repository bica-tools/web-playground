package com.bica.reborn.workflow;

import com.bica.reborn.monitor.MonitorResult;
import com.bica.reborn.monitor.SessionMonitor;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;

/**
 * Session-type-enforced agent workflow engine.
 *
 * <p>Orchestrates the full step lifecycle:
 * <ol>
 *   <li>Scan (Supervisor)</li>
 *   <li>Research (Researcher)</li>
 *   <li>Implement || Write (parallel)</li>
 *   <li>Test || Prove (parallel)</li>
 *   <li>Evaluate (Evaluator)</li>
 *   <li>Review if needed (Reviewer)</li>
 * </ol>
 *
 * <p>Each agent has a session type protocol enforced by a {@link SessionMonitor}.
 * Protocol violations are tracked and reported in the {@link WorkflowResult}.
 */
public class WorkflowEngine {

    private final Map<String, SessionMonitor> agentMonitors = new HashMap<>();
    private int sprintCount = 0;

    /**
     * Create a new workflow engine.
     */
    public WorkflowEngine() {
        // monitors are created on-demand per agent invocation
    }

    /**
     * Run the full lifecycle for one research step.
     *
     * @param step the step number (e.g. "23")
     * @return the workflow result
     */
    public WorkflowResult runStep(String step) {
        Objects.requireNonNull(step, "step must not be null");

        long start = System.currentTimeMillis();
        var phases = new ArrayList<WorkflowResult.PhaseResult>();
        var violations = new ArrayList<String>();

        // Phase 1: Research
        var p1 = runPhase("research", step, false, "Researcher");
        phases.add(p1);

        // Phase 2: Implement || Write (parallel)
        var p2 = runPhase("implement || write", step, true, "Implementer", "Writer");
        phases.add(p2);

        // Phase 3: Test || Prove (parallel)
        var p3 = runPhase("test || prove", step, true, "Tester", "Prover");
        phases.add(p3);

        // Phase 4: Evaluate
        var p4 = runPhase("evaluate", step, false, "Evaluator");
        phases.add(p4);

        // Collect violations
        for (var phase : phases) {
            for (var agent : phase.agents()) {
                if (agent.violated()) {
                    violations.add(agent.agentType() + ": protocol violation");
                }
            }
        }

        double elapsed = System.currentTimeMillis() - start;

        // Determine grade based on phase outcomes
        boolean allOk = phases.stream().allMatch(WorkflowResult.PhaseResult::allOk);
        String grade = allOk ? "A+" : "B";
        boolean accepted = allOk;

        return new WorkflowResult(step, phases, accepted, grade,
                accepted ? List.of() : List.of("Review and fix failures"),
                violations, elapsed);
    }

    /**
     * Run a full sprint processing multiple steps sequentially.
     *
     * @param steps the step numbers to process
     * @return list of results, one per step
     */
    public List<WorkflowResult> runSprint(List<String> steps) {
        sprintCount++;
        var results = new ArrayList<WorkflowResult>();
        for (String step : steps) {
            results.add(runStep(step));
        }
        return results;
    }

    /**
     * Get the current sprint count.
     */
    public int sprintCount() {
        return sprintCount;
    }

    /**
     * Create a monitored agent and execute its protocol transitions.
     *
     * @param agentType the agent type name
     * @param step      the step number
     * @return the agent execution record
     */
    public WorkflowResult.AgentExecution executeAgent(String agentType, String step) {
        long start = System.currentTimeMillis();
        String agentId = agentType.toLowerCase() + "-step" + step + "-" + sprintCount;

        // Create a monitor for this agent's session type
        String protocol = WorkflowProtocol.protocolFor(agentType);
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(protocol));
        SessionMonitor monitor = new SessionMonitor(ss);
        agentMonitors.put(agentId, monitor);

        // Infer transitions for this agent type
        List<String> transitions = inferTransitions(agentType);

        // Execute transitions
        boolean violated = false;
        for (String label : transitions) {
            MonitorResult result = monitor.step(label);
            if (!result.success()) {
                violated = true;
                break;
            }
        }

        double elapsed = System.currentTimeMillis() - start;
        return new WorkflowResult.AgentExecution(
                agentType, agentId, "MCP",
                !violated, violated, transitions, elapsed);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private WorkflowResult.PhaseResult runPhase(
            String name, String step, boolean parallel, String... agentTypes) {
        var agents = new ArrayList<WorkflowResult.AgentExecution>();
        for (String agentType : agentTypes) {
            agents.add(executeAgent(agentType, step));
        }
        return new WorkflowResult.PhaseResult(name, agents, parallel);
    }

    private List<String> inferTransitions(String agentType) {
        return switch (agentType) {
            case "Supervisor" -> List.of("scan", "proposals");
            case "Researcher" -> List.of("investigate", "report");
            case "Evaluator" -> List.of("evaluate", "accepted");
            case "Implementer" -> List.of("implement", "moduleReady");
            case "Tester" -> List.of("writeTests", "testsPass");
            case "Writer" -> List.of("writePaper", "paperReady");
            case "Prover" -> List.of("writeProofs", "proofsReady");
            case "Reviewer" -> List.of("review", "fixes");
            default -> List.of();
        };
    }
}
