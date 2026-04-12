package com.bica.reborn.protocol_engineering;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for protocol engineering toolkit (Step 60r).
 *
 * <p>Mirrors the 26 Python tests in test_protocol_engineering.py.
 */
class ProtocolEngineeringCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // =========================================================================
    // 1. DESCRIBE tests
    // =========================================================================

    @Nested
    class DescribeTests {

        @Test
        void endProtocol() {
            var profiles = ProtocolEngineeringChecker.describeProtocol(build("end"));
            assertEquals(1, profiles.size());
            assertEquals(0.0, profiles.get(0).potential());
            assertEquals(0.0, profiles.get(0).kinetic());
        }

        @Test
        void chain() {
            StateSpace ss = build("&{a: &{b: end}}");
            var profiles = ProtocolEngineeringChecker.describeProtocol(ss);
            assertEquals(3, profiles.size());
            var topP = profiles.stream().filter(p -> p.stateId() == ss.top()).findFirst().orElseThrow();
            assertTrue(topP.potential() > 0);
        }

        @Test
        void branchHotspot() {
            StateSpace ss = build("&{a: end, b: end, c: end}");
            var profiles = ProtocolEngineeringChecker.describeProtocol(ss);
            var topP = profiles.stream().filter(p -> p.stateId() == ss.top()).findFirst().orElseThrow();
            assertTrue(topP.isHotspot(), "3-way branch top should be hotspot");
        }

        @ParameterizedTest
        @CsvSource({
            "&{a: end}",
            "&{a: &{b: end}}",
            "(&{a: end} || &{b: end})"
        })
        void noDeadlocksInValid(String type) {
            var profiles = ProtocolEngineeringChecker.describeProtocol(build(type));
            assertFalse(profiles.stream().anyMatch(ProtocolEngineeringChecker.StateProfile::isDeadlock));
        }
    }

    // =========================================================================
    // 2. BUILD tests
    // =========================================================================

    @Nested
    class BuildTests {

        @Test
        void chainScore() {
            var score = ProtocolEngineeringChecker.evaluateDesign(build("&{a: &{b: end}}"));
            assertTrue(score.termination());
            assertTrue(score.overall() > 0);
            assertTrue(score.efficiency() > 0);
        }

        @Test
        void parallelMoreRobust() {
            var sChain = ProtocolEngineeringChecker.evaluateDesign(build("&{a: &{b: end}}"));
            var sPar = ProtocolEngineeringChecker.evaluateDesign(build("(&{a: end} || &{b: end})"));
            assertTrue(sPar.robustness() >= sChain.robustness());
        }

        @Test
        void suggestionsForSimple() {
            var score = ProtocolEngineeringChecker.evaluateDesign(build("&{a: end}"));
            assertNotNull(score.suggestions());
        }

        @ParameterizedTest
        @CsvSource({
            "end",
            "&{a: end}",
            "'&{a: end, b: end}'",
            "(&{a: end} || &{b: end})"
        })
        void allScoresBounded(String type) {
            var score = ProtocolEngineeringChecker.evaluateDesign(build(type));
            assertTrue(score.efficiency() >= 0 && score.efficiency() <= 1.01,
                    "efficiency=" + score.efficiency());
            assertTrue(score.smoothness() >= 0 && score.smoothness() <= 1.01,
                    "smoothness=" + score.smoothness());
            assertTrue(score.robustness() >= 0 && score.robustness() <= 1.01,
                    "robustness=" + score.robustness());
            assertTrue(score.overall() >= 0 && score.overall() <= 1.01,
                    "overall=" + score.overall());
        }
    }

    // =========================================================================
    // 3. VERIFY tests
    // =========================================================================

    @Nested
    class VerifyTests {

        @Test
        void validChain() {
            var r = ProtocolEngineeringChecker.verifyProtocol(build("&{a: &{b: end}}"));
            assertTrue(r.allPassed());
        }

        @Test
        void validParallel() {
            var r = ProtocolEngineeringChecker.verifyProtocol(build("(&{a: end} || &{b: end})"));
            assertTrue(r.allPassed());
        }

        @Test
        void checkNames() {
            var r = ProtocolEngineeringChecker.verifyProtocol(build("&{a: end}"));
            var names = r.checks().stream().map(ProtocolEngineeringChecker.VerificationResult.Check::name).toList();
            assertTrue(names.contains("no_deadlock"));
            assertTrue(names.contains("termination"));
            assertTrue(names.contains("no_isolated"));
        }

        @Test
        void recursiveVerified() {
            var r = ProtocolEngineeringChecker.verifyProtocol(build("rec X . &{a: X, b: end}"));
            var termCheck = r.checks().stream()
                    .filter(c -> c.name().equals("termination"))
                    .allMatch(ProtocolEngineeringChecker.VerificationResult.Check::passed);
            assertTrue(termCheck);
        }
    }

    // =========================================================================
    // 4. MONITOR tests
    // =========================================================================

    @Nested
    class MonitorTests {

        @Test
        void simpleExecution() {
            StateSpace ss = build("&{a: &{b: end}}");
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            assertEquals(ss.top(), mon.currentState());
            assertFalse(mon.isTerminated());

            var anomalies = mon.step("a");
            assertTrue(anomalies.isEmpty());

            anomalies = mon.step("b");
            assertTrue(anomalies.isEmpty());
            assertTrue(mon.isTerminated());
        }

        @Test
        void invalidTransition() {
            StateSpace ss = build("&{a: end}");
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            var anomalies = mon.step("nonexistent");
            assertEquals(1, anomalies.size());
            assertTrue(anomalies.get(0).contains("INVALID_TRANSITION"));
        }

        @Test
        void energyDecreases() {
            StateSpace ss = build("&{a: &{b: end}}");
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            double e0 = mon.currentPotential();
            mon.step("a");
            double e1 = mon.currentPotential();
            assertTrue(e1 < e0, "Potential should decrease: " + e0 + " -> " + e1);
        }

        @Test
        void branchExecution() {
            StateSpace ss = build("&{a: end, b: end}");
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            mon.step("a");
            assertTrue(mon.isTerminated());
        }

        @Test
        void parallelExecution() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            mon.step("a");
            assertFalse(mon.isTerminated());
            mon.step("b");
            assertTrue(mon.isTerminated());
        }

        @Test
        void historyTracking() {
            StateSpace ss = build("&{a: &{b: end}}");
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            mon.step("a");
            mon.step("b");
            assertEquals(2, mon.history().size());
        }

        @Test
        void energyTrace() {
            StateSpace ss = build("&{a: &{b: end}}");
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            mon.step("a");
            mon.step("b");
            var trace = mon.energyTrace();
            assertTrue(trace.size() >= 2);
        }

        @Test
        void allAnomalies() {
            StateSpace ss = build("&{a: end}");
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            mon.step("wrong");
            assertTrue(mon.allAnomalies().size() >= 1);
        }
    }

    // =========================================================================
    // 5. Integration tests
    // =========================================================================

    @Nested
    class IntegrationTests {

        @Test
        void fullPipelineChain() {
            StateSpace ss = build("&{a: &{b: end}}");

            var profiles = ProtocolEngineeringChecker.describeProtocol(ss);
            assertEquals(3, profiles.size());

            var score = ProtocolEngineeringChecker.evaluateDesign(ss);
            assertTrue(score.termination());

            var vr = ProtocolEngineeringChecker.verifyProtocol(ss);
            assertTrue(vr.allPassed());

            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            mon.step("a");
            mon.step("b");
            assertTrue(mon.isTerminated());
            assertTrue(mon.allAnomalies().isEmpty());
        }

        @Test
        void fullPipelineParallel() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            ProtocolEngineeringChecker.describeProtocol(ss);
            ProtocolEngineeringChecker.evaluateDesign(ss);
            var vr = ProtocolEngineeringChecker.verifyProtocol(ss);
            assertTrue(vr.allPassed());
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            mon.step("a");
            mon.step("b");
            assertTrue(mon.isTerminated());
        }

        @Test
        void fullPipelineBranch() {
            StateSpace ss = build("&{a: end, b: end}");
            ProtocolEngineeringChecker.describeProtocol(ss);
            ProtocolEngineeringChecker.evaluateDesign(ss);
            ProtocolEngineeringChecker.verifyProtocol(ss);
            var mon = new ProtocolEngineeringChecker.RuntimeMonitor(ss);
            mon.step("b");
            assertTrue(mon.isTerminated());
        }
    }

    // =========================================================================
    // Benchmark tests
    // =========================================================================

    @Nested
    class BenchmarkTests {

        @ParameterizedTest
        @CsvSource({
            "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}', Java Iterator",
            "'&{open: &{read: end, write: end}}', File Object",
            "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}', SMTP",
            "'&{a: end, b: end}', Two-choice",
            "'(&{a: end} || &{b: end})', Parallel"
        })
        void describe(String type, String name) {
            var profiles = ProtocolEngineeringChecker.describeProtocol(build(type));
            assertTrue(profiles.size() >= 1);
        }

        @ParameterizedTest
        @CsvSource({
            "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}', Java Iterator",
            "'&{open: &{read: end, write: end}}', File Object",
            "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}', SMTP",
            "'&{a: end, b: end}', Two-choice",
            "'(&{a: end} || &{b: end})', Parallel"
        })
        void designScore(String type, String name) {
            var score = ProtocolEngineeringChecker.evaluateDesign(build(type));
            assertTrue(score.overall() >= 0 && score.overall() <= 1.01);
        }

        @ParameterizedTest
        @CsvSource({
            "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}', Java Iterator",
            "'&{open: &{read: end, write: end}}', File Object",
            "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}', SMTP",
            "'&{a: end, b: end}', Two-choice",
            "'(&{a: end} || &{b: end})', Parallel"
        })
        void verify(String type, String name) {
            var vr = ProtocolEngineeringChecker.verifyProtocol(build(type));
            assertTrue(vr.checks().size() >= 3);
        }
    }
}
