package com.bica.reborn.agent;

import com.bica.reborn.agent.catalog.AgentCatalog;
import com.bica.reborn.agent.compose.AgentComposer;
import com.bica.reborn.agent.compose.ComposedAgent;
import com.bica.reborn.agent.handler.BranchHandler;
import com.bica.reborn.agent.runtime.AgentEvent;
import com.bica.reborn.agent.runtime.AgentRuntime;
import com.bica.reborn.agent.runtime.ViolationPolicy;
import com.bica.reborn.agent.verify.AgentVerifier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the session-typed multi-agent framework.
 */
class AgentFrameworkTest {

    // ======================================================================
    // AgentProtocol
    // ======================================================================

    @Nested
    class ProtocolTests {

        @Test
        void createValidProtocol() {
            var p = AgentProtocol.of("&{a: end, b: end}");
            assertTrue(p.isLattice());
            assertEquals(2, p.stateCount());
            assertTrue(p.initialMethods().contains("a"));
            assertTrue(p.initialMethods().contains("b"));
        }

        @Test
        void createRecursiveProtocol() {
            var p = AgentProtocol.of("rec X . &{next: +{TRUE: X, FALSE: end}}");
            assertTrue(p.isLattice());
        }

        @Test
        void invalidProtocolThrows() {
            assertThrows(AgentException.class, () -> AgentProtocol.of("rec X . X"));
        }

        @Test
        void uncheckedAllowsNonLattice() {
            var p = AgentProtocol.unchecked("rec X . X");
            assertFalse(p.isLattice());
        }

        @Test
        void researcherProtocol() {
            var p = AgentProtocol.of(AgentCatalog.RESEARCHER);
            assertTrue(p.isLattice());
            assertTrue(p.initialMethods().contains("investigate"));
        }

        @Test
        void implementerProtocol() {
            var p = AgentProtocol.of(AgentCatalog.IMPLEMENTER);
            assertTrue(p.isLattice());
        }
    }

    // ======================================================================
    // SimpleAgent
    // ======================================================================

    @Nested
    class SimpleAgentTests {

        @Test
        void createAndInvoke() {
            var agent = SimpleAgent.of("test", "&{work: end}",
                    ctx -> AgentResult.success(Map.of("done", true), List.of("work")));

            assertEquals("test", agent.name());
            assertTrue(agent.protocol().isLattice());

            var ctx = AgentContext.initial("test", agent.protocol(), Map.of());
            var result = agent.handle(ctx);
            assertTrue(result.success());
            assertEquals(List.of("work"), result.transitionsTaken());
        }

        @Test
        void agentWithSelection() {
            var agent = SimpleAgent.of("validator", "&{validate: +{OK: end, ERROR: end}}",
                    ctx -> {
                        boolean valid = (boolean) ctx.payload().getOrDefault("valid", false);
                        String outcome = valid ? "OK" : "ERROR";
                        return AgentResult.success(outcome, Map.of("validated", true),
                                List.of("validate", outcome));
                    });

            var ctx = AgentContext.initial("validator", agent.protocol(), Map.of("valid", true));
            var result = agent.handle(ctx);
            assertTrue(result.success());
            assertEquals("OK", result.selectionOutcome());
        }

        @Test
        void agentToString() {
            var agent = SimpleAgent.of("a", "&{m: end}", ctx -> AgentResult.success(Map.of()));
            assertTrue(agent.toString().contains("SimpleAgent"));
            assertTrue(agent.toString().contains("a"));
        }
    }

    // ======================================================================
    // AgentContext
    // ======================================================================

    @Nested
    class ContextTests {

        @Test
        void initialContextAtTop() {
            var protocol = AgentProtocol.of("&{a: end, b: end}");
            var ctx = AgentContext.initial("test", protocol, Map.of("key", "val"));
            assertEquals("test", ctx.agentName());
            assertTrue(ctx.enabledMethods().contains("a"));
            assertEquals("val", ctx.payload().get("key"));
            assertFalse(ctx.isComplete());
        }

        @Test
        void isEnabledChecks() {
            var protocol = AgentProtocol.of("&{a: +{x: end, y: end}}");
            var ctx = AgentContext.initial("test", protocol, Map.of());
            assertTrue(ctx.isEnabled("a"));
            assertFalse(ctx.isEnabled("x")); // x is deeper, not at top
        }
    }

    // ======================================================================
    // BranchHandler
    // ======================================================================

    @Nested
    class BranchHandlerTests {

        @Test
        void dispatchesByLabel() {
            var handler = BranchHandler.builder()
                    .on("read", ctx -> AgentResult.success(Map.of("action", "read")))
                    .on("write", ctx -> AgentResult.success(Map.of("action", "write")))
                    .build();

            var protocol = AgentProtocol.of("&{read: end, write: end}");
            var ctx = AgentContext.initial("fh", protocol, Map.of());
            var result = handler.handle(ctx);
            assertTrue(result.success());
        }

        @Test
        void fallbackHandler() {
            var handler = BranchHandler.builder()
                    .fallback(ctx -> AgentResult.success(Map.of("fallback", true)))
                    .build();

            var protocol = AgentProtocol.of("&{unknown: end}");
            var ctx = AgentContext.initial("test", protocol, Map.of());
            var result = handler.handle(ctx);
            assertTrue(result.success());
            assertTrue((boolean) result.output().get("fallback"));
        }

        @Test
        void noMatchThrows() {
            var handler = BranchHandler.builder()
                    .on("x", ctx -> AgentResult.success(Map.of()))
                    .build();

            var protocol = AgentProtocol.of("&{y: end}");
            var ctx = AgentContext.initial("test", protocol, Map.of());
            assertThrows(AgentException.class, () -> handler.handle(ctx));
        }
    }

    // ======================================================================
    // Composition
    // ======================================================================

    @Nested
    class CompositionTests {

        @Test
        void sequentialComposition() {
            var a = SimpleAgent.of("A", "&{step1: end}",
                    ctx -> AgentResult.success(Map.of("a", true), List.of("step1")));
            var b = SimpleAgent.of("B", "&{step2: end}",
                    ctx -> AgentResult.success(Map.of("b", true), List.of("step2")));

            var composed = AgentComposer.sequence("AB", a, b);
            assertEquals(ComposedAgent.CompositionKind.SEQUENCE, composed.kind());
            assertEquals(2, composed.subAgents().size());
            assertEquals("AB", composed.name());
        }

        @Test
        void parallelComposition() {
            var left = SimpleAgent.of("L", "&{doL: end}",
                    ctx -> AgentResult.success(Map.of("l", true), List.of("doL")));
            var right = SimpleAgent.of("R", "&{doR: end}",
                    ctx -> AgentResult.success(Map.of("r", true), List.of("doR")));

            var composed = AgentComposer.parallel("LR", left, right);
            assertEquals(ComposedAgent.CompositionKind.PARALLEL, composed.kind());
        }

        @Test
        void builderComposition() {
            var a = SimpleAgent.of("A", "&{s1: end}", ctx -> AgentResult.success(Map.of()));
            var b = SimpleAgent.of("B", "&{s2: end}", ctx -> AgentResult.success(Map.of()));
            var c = SimpleAgent.of("C", "&{s3: end}", ctx -> AgentResult.success(Map.of()));

            var pipeline = AgentComposer.builder("Pipeline")
                    .then(a)
                    .then(b)
                    .then(c)
                    .build();

            assertNotNull(pipeline);
            assertEquals("Pipeline_seq_2", pipeline.name());
        }

        @Test
        void sequentialExecution() {
            var a = SimpleAgent.of("A", "&{s1: end}",
                    ctx -> AgentResult.success(Map.of("phase", "research"), List.of("s1")));
            var b = SimpleAgent.of("B", "&{s2: end}",
                    ctx -> AgentResult.success(Map.of("phase", "implement"), List.of("s2")));

            var composed = AgentComposer.sequence("AB", a, b);
            var ctx = AgentContext.initial("AB", composed.protocol(), Map.of());
            var result = composed.handle(ctx);
            assertTrue(result.success());
            assertEquals(List.of("s1", "s2"), result.transitionsTaken());
        }

        @Test
        void parallelExecution() {
            var left = SimpleAgent.of("L", "&{doL: end}",
                    ctx -> AgentResult.success(Map.of("l", "done"), List.of("doL")));
            var right = SimpleAgent.of("R", "&{doR: end}",
                    ctx -> AgentResult.success(Map.of("r", "done"), List.of("doR")));

            var composed = AgentComposer.parallel("LR", left, right);
            var ctx = AgentContext.initial("LR", composed.protocol(), Map.of());
            var result = composed.handle(ctx);
            assertTrue(result.success());
            assertEquals("done", result.output().get("l"));
            assertEquals("done", result.output().get("r"));
        }
    }

    // ======================================================================
    // Runtime
    // ======================================================================

    @Nested
    class RuntimeTests {

        @Test
        void registerAndInvoke() {
            var agent = SimpleAgent.of("worker", "&{work: end}",
                    ctx -> AgentResult.success(Map.of("result", 42), List.of("work")));

            var runtime = AgentRuntime.builder()
                    .violationPolicy(ViolationPolicy.THROW)
                    .build();

            runtime.register(agent);
            assertTrue(runtime.isRegistered("worker"));

            var result = runtime.invoke("worker", Map.of());
            assertTrue(result.success());
            assertEquals(42, result.output().get("result"));
        }

        @Test
        void eventListenerFired() {
            var events = new ArrayList<AgentEvent>();

            var agent = SimpleAgent.of("observed", "&{act: end}",
                    ctx -> AgentResult.success(Map.of(), List.of("act")));

            var runtime = AgentRuntime.builder()
                    .addEventListener(events::add)
                    .build();

            runtime.register(agent);
            runtime.invoke("observed", Map.of());

            // Should have Registered + TransitionTaken(act) + Completed
            assertTrue(events.stream().anyMatch(e -> e instanceof AgentEvent.Registered));
            assertTrue(events.stream().anyMatch(e -> e instanceof AgentEvent.Completed));
        }

        @Test
        void violationDetected() {
            var agent = SimpleAgent.of("bad", "&{a: end}",
                    ctx -> AgentResult.success(Map.of(), List.of("a", "b"))); // b is invalid!

            var runtime = AgentRuntime.builder()
                    .violationPolicy(ViolationPolicy.THROW)
                    .build();

            runtime.register(agent);
            assertThrows(AgentException.class, () -> runtime.invoke("bad", Map.of()));
        }

        @Test
        void logAndContinuePolicy() {
            var agent = SimpleAgent.of("lenient", "&{a: end}",
                    ctx -> AgentResult.success(Map.of(), List.of("a", "b")));

            var runtime = AgentRuntime.builder()
                    .violationPolicy(ViolationPolicy.LOG_AND_CONTINUE)
                    .build();

            runtime.register(agent);
            var result = runtime.invoke("lenient", Map.of());
            assertTrue(result.success()); // continues despite violation
        }

        @Test
        void unregisteredAgentThrows() {
            var runtime = AgentRuntime.builder().build();
            assertThrows(AgentException.class, () -> runtime.invoke("ghost", Map.of()));
        }

        @Test
        void verificationOnRegister() {
            var agent = SimpleAgent.of("verified", "&{m: end}",
                    ctx -> AgentResult.success(Map.of()));

            var runtime = AgentRuntime.builder()
                    .verify(true)
                    .build();

            assertDoesNotThrow(() -> runtime.register(agent));
        }
    }

    // ======================================================================
    // Verification
    // ======================================================================

    @Nested
    class VerificationTests {

        @Test
        void verifySimpleAgent() {
            var agent = SimpleAgent.of("test", "&{m: end}",
                    ctx -> AgentResult.success(Map.of()));
            var result = AgentVerifier.verify(agent);
            assertTrue(result.verified());
            assertTrue(result.isLattice());
            assertNotNull(result.distributivity());
        }

        @Test
        void verifyRecursiveAgent() {
            var agent = SimpleAgent.of("iter",
                    "rec X . &{next: +{TRUE: X, FALSE: end}}",
                    ctx -> AgentResult.success(Map.of()));
            var result = AgentVerifier.verify(agent);
            assertTrue(result.verified());
        }

        @Test
        void verificationSummary() {
            var agent = SimpleAgent.of("ok", "&{m: end}",
                    ctx -> AgentResult.success(Map.of()));
            assertEquals("OK", AgentVerifier.verify(agent).summary());
        }
    }

    // ======================================================================
    // Catalog
    // ======================================================================

    @Nested
    class CatalogTests {

        @Test
        void allProtocolsAreLattices() {
            int total = AgentCatalog.all().size();
            int lattices = AgentCatalog.validateAll();
            assertEquals(total, lattices, "All catalog protocols should form lattices");
        }

        @Test
        void catalogHas18Protocols() {
            assertTrue(AgentCatalog.all().size() >= 18);
        }

        @Test
        void researcherInCatalog() {
            assertTrue(AgentCatalog.all().containsKey("Researcher"));
        }

        @Test
        void mcpProtocolInCatalog() {
            assertTrue(AgentCatalog.all().containsKey("MCP"));
            var p = AgentProtocol.of(AgentCatalog.MCP_PROTOCOL);
            assertTrue(p.isLattice());
        }

        @Test
        void a2aProtocolInCatalog() {
            assertTrue(AgentCatalog.all().containsKey("A2A"));
            var p = AgentProtocol.of(AgentCatalog.A2A_PROTOCOL);
            assertTrue(p.isLattice());
        }
    }

    // ======================================================================
    // Integration: Full Pipeline
    // ======================================================================

    @Nested
    class IntegrationTests {

        @Test
        void researchPipeline() {
            var researcher = SimpleAgent.of("Researcher", AgentCatalog.RESEARCHER,
                    ctx -> AgentResult.success("report", Map.of("findings", "data"),
                            List.of("investigate", "report")));

            var writer = SimpleAgent.of("Writer", AgentCatalog.WRITER,
                    ctx -> AgentResult.success("paperReady", Map.of("paper", "done"),
                            List.of("writePaper", "paperReady")));

            // Sequential: research then write
            var pipeline = AgentComposer.sequence("ResearchWrite", researcher, writer);

            var events = new ArrayList<AgentEvent>();
            var runtime = AgentRuntime.builder()
                    .addEventListener(events::add)
                    .verify(true)
                    .build();

            runtime.register(pipeline);
            var result = runtime.invoke("ResearchWrite", Map.of("step", "23"));

            assertTrue(result.success());
            // Events: Registered + transitions + Completed
            assertFalse(events.isEmpty());
        }

        @Test
        void parallelImplementAndWrite() {
            var impl = SimpleAgent.of("Impl", "&{implement: +{moduleReady: end}}",
                    ctx -> AgentResult.success("moduleReady", Map.of("module", "ok"),
                            List.of("implement", "moduleReady")));

            var writer = SimpleAgent.of("Writer", AgentCatalog.WRITER,
                    ctx -> AgentResult.success("paperReady", Map.of("paper", "ok"),
                            List.of("writePaper", "paperReady")));

            var par = AgentComposer.parallel("ImplWrite", impl, writer);
            var ctx = AgentContext.initial("ImplWrite", par.protocol(), Map.of());
            var result = par.handle(ctx);

            assertTrue(result.success());
            assertEquals("ok", result.output().get("module"));
            assertEquals("ok", result.output().get("paper"));
        }

        @Test
        void fullStepWorkflow() {
            // research . (implement || write) . evaluate
            var research = SimpleAgent.of("R", AgentCatalog.RESEARCHER,
                    ctx -> AgentResult.success("report", Map.of(), List.of("investigate", "report")));
            var impl = SimpleAgent.of("I", "&{implement: +{moduleReady: end}}",
                    ctx -> AgentResult.success("moduleReady", Map.of(), List.of("implement", "moduleReady")));
            var write = SimpleAgent.of("W", AgentCatalog.WRITER,
                    ctx -> AgentResult.success("paperReady", Map.of(), List.of("writePaper", "paperReady")));
            var eval = SimpleAgent.of("E", AgentCatalog.EVALUATOR,
                    ctx -> AgentResult.success("accepted", Map.of("grade", "A+"),
                            List.of("evaluate", "accepted")));

            var pipeline = AgentComposer.builder("StepWorkflow")
                    .then(research)
                    .parallel(impl, write)
                    .then(eval)
                    .build();

            var ctx = AgentContext.initial("StepWorkflow", pipeline.protocol(), Map.of("step", "30i"));
            var result = pipeline.handle(ctx);

            assertTrue(result.success());
            assertEquals("A+", result.output().get("grade"));
        }
    }
}
