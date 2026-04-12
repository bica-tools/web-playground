package com.bica.reborn.workflow;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for workflow engine, orchestrator, supervisor, evaluator, and protocols.
 */
class WorkflowTest {

    // =========================================================================
    // WorkflowProtocol
    // =========================================================================

    @Nested
    class ProtocolTests {

        @Test
        void allAgentProtocolsParse() {
            for (String agent : WorkflowProtocol.AGENT_TYPES) {
                String type = WorkflowProtocol.protocolFor(agent);
                assertDoesNotThrow(() -> Parser.parse(type),
                        "Failed to parse protocol for " + agent);
            }
        }

        @Test
        void allAgentProtocolsBuildStateSpace() {
            for (String agent : WorkflowProtocol.AGENT_TYPES) {
                String type = WorkflowProtocol.protocolFor(agent);
                StateSpace ss = StateSpaceBuilder.build(Parser.parse(type));
                assertTrue(ss.states().size() >= 1,
                        "State space for " + agent + " should have at least 1 state");
            }
        }

        @Test
        void allAgentProtocolsFormLattices() {
            for (String agent : WorkflowProtocol.AGENT_TYPES) {
                String type = WorkflowProtocol.protocolFor(agent);
                StateSpace ss = StateSpaceBuilder.build(Parser.parse(type));
                LatticeResult lr = LatticeChecker.checkLattice(ss);
                assertTrue(lr.isLattice(),
                        "Protocol for " + agent + " should form a lattice");
            }
        }

        @Test
        void orchestratorProtocolParses() {
            assertDoesNotThrow(() -> Parser.parse(WorkflowProtocol.ORCHESTRATOR));
        }

        @Test
        void unknownAgentTypeThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> WorkflowProtocol.protocolFor("Unknown"));
        }
    }

    // =========================================================================
    // WorkflowEngine
    // =========================================================================

    @Nested
    class EngineTests {

        @Test
        void runStepProducesResult() {
            var engine = new WorkflowEngine();
            WorkflowResult result = engine.runStep("1");
            assertNotNull(result);
            assertEquals("1", result.stepNumber());
        }

        @Test
        void runStepHasFourPhases() {
            var engine = new WorkflowEngine();
            WorkflowResult result = engine.runStep("1");
            assertEquals(4, result.phases().size());
        }

        @Test
        void executeAgentReturnsExecution() {
            var engine = new WorkflowEngine();
            var exec = engine.executeAgent("Researcher", "1");
            assertEquals("Researcher", exec.agentType());
            assertTrue(exec.success());
            assertFalse(exec.violated());
        }

        @Test
        void executeAgentRecordsTransitions() {
            var engine = new WorkflowEngine();
            var exec = engine.executeAgent("Supervisor", "1");
            assertEquals(List.of("scan", "proposals"), exec.transitions());
        }

        @Test
        void runSprintProcessesMultipleSteps() {
            var engine = new WorkflowEngine();
            var results = engine.runSprint(List.of("1", "2"));
            assertEquals(2, results.size());
            assertEquals(1, engine.sprintCount());
        }
    }

    // =========================================================================
    // Orchestrator
    // =========================================================================

    @Nested
    class OrchestratorTests {

        @Test
        void orchestrateProcessesSteps() {
            var orch = new Orchestrator();
            var results = orch.orchestrate(List.of("1"));
            assertEquals(1, results.size());
        }

        @Test
        void dispatchTaskReturnsResult() {
            var orch = new Orchestrator();
            var exec = orch.dispatchTask("Writer", "5");
            assertEquals("Writer", exec.agentType());
            assertTrue(exec.success());
        }

        @Test
        void collectResultsGivesConformanceStats() {
            var orch = new Orchestrator();
            var results = orch.orchestrate(List.of("1"));
            var stats = orch.collectResults(results);
            assertEquals(1, stats.get("stepsProcessed"));
            assertTrue((int) stats.get("totalAgents") > 0);
            assertEquals(0, stats.get("violations"));
        }

        @Test
        void sprintCountIncrements() {
            var orch = new Orchestrator();
            orch.orchestrate(List.of("1"));
            assertEquals(1, orch.sprintCount());
            orch.orchestrate(List.of("2"));
            assertEquals(2, orch.sprintCount());
        }
    }

    // =========================================================================
    // Supervisor
    // =========================================================================

    @Nested
    class SupervisorTests {

        @Test
        void scanProgrammeReturnsSnapshot() {
            var sup = new Supervisor();
            sup.addStep(new Supervisor.StepStatus("1", "State Space", "A+", "complete", true, true, 50));
            sup.addStep(new Supervisor.StepStatus("2", "Lattice", "B", "draft", true, true, 30));
            var snapshot = sup.scanProgramme();
            assertEquals(2, snapshot.totalStepsPlanned());
            assertEquals(1, snapshot.completeSteps());
        }

        @Test
        void identifyGapsFindsQuickWins() {
            var sup = new Supervisor();
            sup.addStep(new Supervisor.StepStatus("5", "Universality", "B", "draft", false, true, 20));
            var gaps = sup.identifyGaps();
            assertFalse(gaps.isEmpty());
            assertEquals("high", gaps.get(0).priority());
        }

        @Test
        void prioritizeSortsHighFirst() {
            var sup = new Supervisor();
            var proposals = List.of(
                    new Supervisor.Proposal("1", "Low", "reason", "low"),
                    new Supervisor.Proposal("2", "High", "reason", "high"),
                    new Supervisor.Proposal("3", "Medium", "reason", "medium")
            );
            var sorted = sup.prioritize(proposals);
            assertEquals("high", sorted.get(0).priority());
            assertEquals("medium", sorted.get(1).priority());
            assertEquals("low", sorted.get(2).priority());
        }
    }

    // =========================================================================
    // Evaluator
    // =========================================================================

    @Nested
    class EvaluatorTests {

        @Test
        void perfectStepGetsAPlus() {
            var eval = new Evaluator();
            var d = new Evaluator.StepDeliverables("1", true, 6000, true, true, true, 50, 6, 3, 10);
            var result = eval.evaluate(d);
            assertEquals("A+", result.grade());
            assertTrue(result.accepted());
        }

        @Test
        void noPaperGetsF() {
            var eval = new Evaluator();
            var d = new Evaluator.StepDeliverables("1", false, 0, false, false, false, 0, 0, 0, 0);
            var result = eval.evaluate(d);
            assertEquals("F", result.grade());
            assertFalse(result.accepted());
            assertFalse(result.fixes().isEmpty());
        }

        @Test
        void computeScoreNormalizesTo100() {
            var eval = new Evaluator();
            var criteria = List.of(
                    new Evaluator.Criterion("A", 10, 10, true, "ok"),
                    new Evaluator.Criterion("B", 10, 5, false, "half")
            );
            assertEquals(75, eval.computeScore(criteria));
        }
    }
}
