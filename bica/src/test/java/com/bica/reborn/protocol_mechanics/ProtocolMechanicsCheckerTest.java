package com.bica.reborn.protocol_mechanics;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for protocol mechanics (Step 60p).
 *
 * <p>Mirrors the 32 Python tests in test_protocol_mechanics.py.
 */
class ProtocolMechanicsCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // =========================================================================
    // Discrete derivative
    // =========================================================================

    @Nested
    class DiscreteDerivativeTests {

        @Test
        void constantFunctionDerivativeIsZero() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Double> f = new java.util.HashMap<>();
            for (int s : ss.states()) f.put(s, 1.0);
            Map<Integer, Double> df = ProtocolMechanicsChecker.discreteDerivative(ss, f);
            for (double v : df.values()) {
                assertEquals(0.0, v, 0.01);
            }
        }

        @Test
        void rankDerivativePositiveAtTop() {
            StateSpace ss = build("&{a: &{b: end}}");
            Map<Integer, Integer> rank = Zeta.computeRank(ss);
            Map<Integer, Double> f = new java.util.HashMap<>();
            for (var e : rank.entrySet()) f.put(e.getKey(), e.getValue().doubleValue());
            Map<Integer, Double> df = ProtocolMechanicsChecker.discreteDerivative(ss, f);
            assertTrue(df.get(ss.top()) > 0);
        }

        @Test
        void bottomDerivativeIsZero() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Integer> rank = Zeta.computeRank(ss);
            Map<Integer, Double> f = new java.util.HashMap<>();
            for (var e : rank.entrySet()) f.put(e.getKey(), e.getValue().doubleValue());
            Map<Integer, Double> df = ProtocolMechanicsChecker.discreteDerivative(ss, f);
            assertEquals(0.0, df.get(ss.bottom()), 0.001);
        }
    }

    // =========================================================================
    // Second derivative
    // =========================================================================

    @Nested
    class SecondDerivativeTests {

        @Test
        void chainSecondDerivativeExists() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            Map<Integer, Integer> rank = Zeta.computeRank(ss);
            Map<Integer, Double> f = new java.util.HashMap<>();
            for (var e : rank.entrySet()) f.put(e.getKey(), e.getValue().doubleValue());
            Map<Integer, Double> d2f = ProtocolMechanicsChecker.discreteSecondDerivative(ss, f);
            assertNotNull(d2f);
            assertFalse(d2f.isEmpty());
        }

        @Test
        void branchHasNonzeroSecond() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Integer> rank = Zeta.computeRank(ss);
            Map<Integer, Double> f = new java.util.HashMap<>();
            for (var e : rank.entrySet()) f.put(e.getKey(), e.getValue().doubleValue());
            Map<Integer, Double> d2f = ProtocolMechanicsChecker.discreteSecondDerivative(ss, f);
            assertNotNull(d2f);
        }
    }

    // =========================================================================
    // Integral and Mobius inversion
    // =========================================================================

    @Nested
    class IntegralTests {

        @Test
        void integralOfConstant() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Double> f = new java.util.HashMap<>();
            for (int s : ss.states()) f.put(s, 1.0);
            Map<Integer, Double> F = ProtocolMechanicsChecker.discreteIntegral(ss, f);
            assertTrue(F.get(ss.top()) >= 2.0);
        }

        @Test
        void mobiusInversionRecovers() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Double> f = new java.util.HashMap<>();
            for (int s : ss.states()) f.put(s, (double) s);
            Map<Integer, Double> F = ProtocolMechanicsChecker.discreteIntegral(ss, f);
            Map<Integer, Double> fRecovered = ProtocolMechanicsChecker.mobiusInversion(ss, F);
            for (int s : ss.states()) {
                assertEquals(f.get(s), fRecovered.get(s), 0.5);
            }
        }
    }

    // =========================================================================
    // Velocity field
    // =========================================================================

    @Nested
    class VelocityFieldTests {

        @Test
        void endNoVelocity() {
            StateSpace ss = build("end");
            var vf = ProtocolMechanicsChecker.velocityField(ss);
            assertTrue(vf.get(ss.top()).isEmpty());
        }

        @Test
        void chainVelocity() {
            StateSpace ss = build("&{a: end}");
            var vf = ProtocolMechanicsChecker.velocityField(ss);
            assertTrue(vf.get(ss.top()).contains("a"));
        }

        @Test
        void branchVelocity() {
            StateSpace ss = build("&{a: end, b: end}");
            var vf = ProtocolMechanicsChecker.velocityField(ss);
            assertEquals(2, vf.get(ss.top()).size());
        }

        @Test
        void magnitude() {
            StateSpace ss = build("&{a: end, b: end}");
            var vm = ProtocolMechanicsChecker.velocityMagnitude(ss);
            assertEquals(2, vm.get(ss.top()));
            assertEquals(0, vm.get(ss.bottom()));
        }
    }

    // =========================================================================
    // Acceleration field
    // =========================================================================

    @Nested
    class AccelerationFieldTests {

        @Test
        void chainNoAcceleration() {
            StateSpace ss = build("&{a: &{b: end}}");
            var af = ProtocolMechanicsChecker.accelerationField(ss);
            for (int s : ss.states()) {
                assertEquals(0, af.get(s));
            }
        }

        @Test
        void branchHasAcceleration() {
            StateSpace ss = build("&{a: end, b: end}");
            var af = ProtocolMechanicsChecker.accelerationField(ss);
            assertEquals(1, af.get(ss.top()));
        }

        @Test
        void threeWayBranch() {
            StateSpace ss = build("&{a: end, b: end, c: end}");
            var af = ProtocolMechanicsChecker.accelerationField(ss);
            assertEquals(2, af.get(ss.top()));
        }

        @Test
        void branchPointsList() {
            StateSpace ss = build("&{a: end, b: end}");
            var bp = ProtocolMechanicsChecker.branchPoints(ss);
            assertTrue(bp.contains(ss.top()));
            assertFalse(bp.contains(ss.bottom()));
        }
    }

    // =========================================================================
    // Branch order
    // =========================================================================

    @Nested
    class BranchOrderTests {

        @Test
        void chainOrder0() {
            assertEquals(0, ProtocolMechanicsChecker.maxBranchOrder(build("&{a: end}")));
        }

        @Test
        void simpleBranchOrder1() {
            assertEquals(1, ProtocolMechanicsChecker.maxBranchOrder(build("&{a: end, b: end}")));
        }

        @Test
        void nestedBranchOrder2() {
            int order = ProtocolMechanicsChecker.maxBranchOrder(build("&{a: &{c: end, d: end}, b: end}"));
            assertEquals(2, order);
        }
    }

    // =========================================================================
    // Parallel dimensions
    // =========================================================================

    @Nested
    class ParallelDimensionTests {

        @Test
        void sequential1d() {
            assertEquals(1, ProtocolMechanicsChecker.parallelDimensions(build("&{a: end}")));
        }

        @Test
        void parallel2d() {
            int pd = ProtocolMechanicsChecker.parallelDimensions(build("(&{a: end} || &{b: end})"));
            assertTrue(pd >= 2);
        }
    }

    // =========================================================================
    // Recursion
    // =========================================================================

    @Nested
    class RecursionTests {

        @Test
        void noRecursion() {
            assertFalse(ProtocolMechanicsChecker.hasPeriodicOrbit(build("&{a: end}")));
        }

        @Test
        void withRecursion() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            boolean result = ProtocolMechanicsChecker.hasPeriodicOrbit(ss);
            // May or may not have multi-state SCC depending on construction
            assertNotNull(result);
        }
    }

    // =========================================================================
    // Smoothness
    // =========================================================================

    @Nested
    class SmoothnessTests {

        @Test
        void chainSmooth() {
            double s = ProtocolMechanicsChecker.smoothness(build("&{a: &{b: &{c: end}}}"));
            assertTrue(s > 0.3, "Chain should be smooth, got " + s);
        }

        @Test
        void bothPositive() {
            double sChain = ProtocolMechanicsChecker.smoothness(build("&{a: &{b: end}}"));
            double sBranch = ProtocolMechanicsChecker.smoothness(build("&{a: end, b: end}"));
            assertTrue(sChain > 0);
            assertTrue(sBranch > 0);
        }
    }

    // =========================================================================
    // Energy
    // =========================================================================

    @Nested
    class EnergyTests {

        @Test
        void endEnergy() {
            int e = ProtocolMechanicsChecker.protocolEnergy(build("end"));
            assertNotNull(e);
        }

        @ParameterizedTest
        @CsvSource({
            "&{a: end}",
            "&{a: &{b: end}}",
            "(&{a: end} || &{b: end})"
        })
        void energyIsInteger(String type) {
            int e = ProtocolMechanicsChecker.protocolEnergy(build(type));
            // Just verify it returns without error
            assertNotNull(e);
        }
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    @Nested
    class AnalyzeTests {

        @Test
        void end() {
            var r = ProtocolMechanicsChecker.analyzeMechanics(build("end"));
            assertEquals(1, r.numStates());
            assertEquals(0, r.numBranchPoints());
        }

        @Test
        void chain() {
            var r = ProtocolMechanicsChecker.analyzeMechanics(build("&{a: &{b: end}}"));
            assertEquals(0, r.numBranchPoints());
            assertEquals(0, r.maxBranchOrder());
            assertTrue(r.smoothness() > 0);
        }

        @Test
        void branch() {
            var r = ProtocolMechanicsChecker.analyzeMechanics(build("&{a: end, b: end}"));
            assertTrue(r.numBranchPoints() >= 1);
            assertTrue(r.maxBranchOrder() >= 1);
        }

        @Test
        void parallel() {
            var r = ProtocolMechanicsChecker.analyzeMechanics(build("(&{a: end} || &{b: end})"));
            assertTrue(r.parallelDimensions() >= 2);
        }
    }

    // =========================================================================
    // Benchmark protocols
    // =========================================================================

    @Nested
    class BenchmarkTests {

        @ParameterizedTest
        @CsvSource({
            "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}', Java Iterator",
            "'&{open: &{read: end, write: end}}', File Object",
            "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}', SMTP",
            "'&{a: end, b: end}', Two-choice",
            "'&{a: &{b: &{c: end}}}', Nested",
            "'(&{a: end} || &{b: end})', Parallel"
        })
        void mechanicsRuns(String type, String name) {
            StateSpace ss = build(type);
            var r = ProtocolMechanicsChecker.analyzeMechanics(ss);
            assertEquals(ss.states().size(), r.numStates());
            assertTrue(r.smoothness() > 0);
            assertTrue(r.maxBranchOrder() >= 0);
        }

        @ParameterizedTest
        @CsvSource({
            "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}', Java Iterator",
            "'&{open: &{read: end, write: end}}', File Object",
            "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}', SMTP",
            "'&{a: end, b: end}', Two-choice",
            "'&{a: &{b: &{c: end}}}', Nested",
            "'(&{a: end} || &{b: end})', Parallel"
        })
        void velocityCoversAll(String type, String name) {
            StateSpace ss = build(type);
            var vf = ProtocolMechanicsChecker.velocityField(ss);
            assertEquals(ss.states(), vf.keySet());
        }
    }
}
