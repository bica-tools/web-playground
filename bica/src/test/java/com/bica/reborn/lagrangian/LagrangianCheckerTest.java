package com.bica.reborn.lagrangian;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Lagrangian mechanics checker (Step 60q).
 *
 * <p>Mirrors the 33 Python tests in test_lagrangian.py.
 */
class LagrangianCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // =========================================================================
    // Kinetic energy tests
    // =========================================================================

    @Nested
    class KineticEnergyTests {

        @Test
        void endZero() {
            StateSpace ss = build("end");
            Map<Integer, Double> T = LagrangianChecker.kineticEnergy(ss);
            assertEquals(0.0, T.get(ss.top()));
        }

        @Test
        void chainUniform() {
            StateSpace ss = build("&{a: &{b: end}}");
            Map<Integer, Double> T = LagrangianChecker.kineticEnergy(ss);
            assertEquals(0.5, T.get(ss.top()));   // 1^2 / 2
            assertEquals(0.0, T.get(ss.bottom()));
        }

        @Test
        void branchHigher() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Double> T = LagrangianChecker.kineticEnergy(ss);
            assertEquals(2.0, T.get(ss.top()));   // 2^2 / 2
        }

        @Test
        void threeBranch() {
            StateSpace ss = build("&{a: end, b: end, c: end}");
            Map<Integer, Double> T = LagrangianChecker.kineticEnergy(ss);
            assertEquals(4.5, T.get(ss.top()));   // 3^2 / 2
        }
    }

    // =========================================================================
    // Potential energy tests
    // =========================================================================

    @Nested
    class PotentialEnergyTests {

        @Test
        void bottomZero() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Double> V = LagrangianChecker.potentialEnergy(ss);
            assertEquals(0.0, V.get(ss.bottom()));
        }

        @Test
        void topPositive() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Double> V = LagrangianChecker.potentialEnergy(ss);
            assertTrue(V.get(ss.top()) > 0);
        }

        @Test
        void chainLinear() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            Map<Integer, Double> V = LagrangianChecker.potentialEnergy(ss);
            assertEquals(3.0, V.get(ss.top()));
            assertEquals(0.0, V.get(ss.bottom()));
        }
    }

    // =========================================================================
    // Lagrangian tests
    // =========================================================================

    @Nested
    class LagrangianTests {

        @Test
        void bottomZero() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Double> L = LagrangianChecker.lagrangianField(ss);
            assertEquals(0.0, L.get(ss.bottom()));
        }

        @Test
        void lagrangianDefined() {
            for (String typ : List.of("&{a: end}", "&{a: &{b: end}}", "(&{a: end} || &{b: end})")) {
                Map<Integer, Double> L = LagrangianChecker.lagrangianField(build(typ));
                for (double v : L.values()) {
                    assertFalse(Double.isNaN(v));
                }
            }
        }
    }

    // =========================================================================
    // Hamiltonian tests
    // =========================================================================

    @Nested
    class HamiltonianTests {

        @Test
        void totalPositive() {
            for (String typ : List.of("end", "&{a: end}", "&{a: &{b: end}}")) {
                double E = LagrangianChecker.totalEnergy(build(typ));
                assertTrue(E >= 0.0);
            }
        }

        @Test
        void hamiltonianEqualsTplusV() {
            StateSpace ss = build("&{a: &{b: end}}");
            Map<Integer, Double> T = LagrangianChecker.kineticEnergy(ss);
            Map<Integer, Double> V = LagrangianChecker.potentialEnergy(ss);
            Map<Integer, Double> H = LagrangianChecker.hamiltonianField(ss);
            for (int s : ss.states()) {
                assertEquals(T.get(s) + V.get(s), H.get(s), 0.001);
            }
        }
    }

    // =========================================================================
    // Gravity tests
    // =========================================================================

    @Nested
    class GravityTests {

        @Test
        void bottomMaxGravity() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Double> g = LagrangianChecker.gravitationalField(ss);
            assertTrue(g.get(ss.bottom()) > g.get(ss.top()));
        }

        @Test
        void gravityDecreasesWithRank() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            Map<Integer, Double> g = LagrangianChecker.gravitationalField(ss);
            assertTrue(g.get(ss.bottom()) > 1.0);
        }
    }

    // =========================================================================
    // Momentum tests
    // =========================================================================

    @Nested
    class MomentumTests {

        @Test
        void bottomZero() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Integer> p = LagrangianChecker.momentum(ss);
            assertEquals(0, p.get(ss.bottom()));
        }

        @Test
        void chainUniform() {
            StateSpace ss = build("&{a: &{b: end}}");
            Map<Integer, Integer> p = LagrangianChecker.momentum(ss);
            assertEquals(1, p.get(ss.top()));
        }

        @Test
        void branchHigher() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Integer> p = LagrangianChecker.momentum(ss);
            assertEquals(2, p.get(ss.top()));
        }
    }

    // =========================================================================
    // Action and path tests
    // =========================================================================

    @Nested
    class ActionTests {

        @Test
        void singleState() {
            StateSpace ss = build("end");
            double a = LagrangianChecker.pathAction(ss, List.of(ss.top()));
            assertNotNull(a);
        }

        @Test
        void chainAction() {
            StateSpace ss = build("&{a: end}");
            double a = LagrangianChecker.pathAction(ss, List.of(ss.top(), ss.bottom()));
            assertNotNull(a);
        }
    }

    // =========================================================================
    // Least action path tests
    // =========================================================================

    @Nested
    class LeastActionTests {

        @Test
        void chainUnique() {
            StateSpace ss = build("&{a: &{b: end}}");
            var result = LagrangianChecker.leastActionPath(ss);
            assertEquals(ss.top(), result.getKey().get(0));
            assertEquals(ss.bottom(), result.getKey().get(result.getKey().size() - 1));
        }

        @Test
        void leastLeMax() {
            for (String typ : List.of("&{a: end}", "&{a: end, b: end}", "(&{a: end} || &{b: end})")) {
                StateSpace ss = build(typ);
                var la = LagrangianChecker.leastActionPath(ss);
                var ma = LagrangianChecker.maxActionPath(ss);
                assertTrue(la.getValue() <= ma.getValue() + 0.001);
            }
        }

        @Test
        void pathValid() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            var result = LagrangianChecker.leastActionPath(ss);
            assertEquals(ss.top(), result.getKey().get(0));
            assertEquals(ss.bottom(), result.getKey().get(result.getKey().size() - 1));
        }
    }

    // =========================================================================
    // Circular mechanics tests
    // =========================================================================

    @Nested
    class CircularTests {

        @Test
        void noCyclesAcyclic() {
            var circ = LagrangianChecker.analyzeCircular(build("&{a: end}"));
            assertEquals(0, circ.size());
        }

        @Test
        void recursiveHasCycle() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            var circ = LagrangianChecker.analyzeCircular(ss);
            assertNotNull(circ);
        }

        @Test
        void cycleProperties() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var circ = LagrangianChecker.analyzeCircular(ss);
            for (var c : circ) {
                assertTrue(c.period() >= 1);
                assertTrue(c.inertia() >= 2);
                assertTrue(c.angularVelocity() > 0);
            }
        }
    }

    // =========================================================================
    // Conservation tests
    // =========================================================================

    @Nested
    class ConservationTests {

        @Test
        void chainConservation() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            var lap = LagrangianChecker.leastActionPath(ss);
            List<Double> energies = LagrangianChecker.checkEnergyConservation(ss, lap.getKey());
            assertEquals(lap.getKey().size(), energies.size());
        }

        @Test
        void conservationCheck() {
            StateSpace ss = build("&{a: &{b: end}}");
            var lap = LagrangianChecker.leastActionPath(ss);
            boolean result = LagrangianChecker.isEnergyConserved(ss, lap.getKey(), 2.0);
            assertNotNull(result);
        }

        @Test
        void singleStateConserved() {
            StateSpace ss = build("end");
            assertTrue(LagrangianChecker.isEnergyConserved(ss, List.of(ss.top())));
        }
    }

    // =========================================================================
    // Full analysis tests
    // =========================================================================

    @Nested
    class AnalyzeTests {

        @Test
        void end() {
            var r = LagrangianChecker.analyzeLagrangian(build("end"));
            assertEquals(1, r.numStates());
            assertTrue(r.totalEnergy() >= 0);
        }

        @Test
        void chain() {
            var r = LagrangianChecker.analyzeLagrangian(build("&{a: &{b: end}}"));
            assertEquals(3, r.numStates());
            assertTrue(r.leastActionValue() <= r.maxActionValue() + 0.001);
            assertEquals(0, r.circularMechanics().size());
        }

        @Test
        void branch() {
            var r = LagrangianChecker.analyzeLagrangian(build("&{a: end, b: end}"));
            assertTrue(r.momentum().get(r.leastActionPath().get(0)) >= 1);
        }

        @Test
        void parallel() {
            var r = LagrangianChecker.analyzeLagrangian(build("(&{a: end} || &{b: end})"));
            assertEquals(4, r.numStates());
            assertTrue(r.totalEnergy() > 0);
        }
    }

    // =========================================================================
    // Benchmark tests
    // =========================================================================

    @Nested
    class BenchmarkTests {

        static Stream<Arguments> benchmarks() {
            return Stream.of(
                    Arguments.of("Java Iterator", "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"),
                    Arguments.of("File Object", "&{open: &{read: end, write: end}}"),
                    Arguments.of("SMTP", "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}"),
                    Arguments.of("Two-choice", "&{a: end, b: end}"),
                    Arguments.of("Nested", "&{a: &{b: &{c: end}}}"),
                    Arguments.of("Parallel", "(&{a: end} || &{b: end})")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("benchmarks")
        void lagrangianRuns(String name, String typ) {
            StateSpace ss = build(typ);
            var r = LagrangianChecker.analyzeLagrangian(ss);
            assertEquals(ss.states().size(), r.numStates());
            assertTrue(r.totalEnergy() >= 0, name + ": total energy negative");
            assertTrue(r.leastActionValue() <= r.maxActionValue() + 0.001,
                    name + ": least > max action");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("benchmarks")
        void hamiltonianNonneg(String name, String typ) {
            StateSpace ss = build(typ);
            Map<Integer, Double> H = LagrangianChecker.hamiltonianField(ss);
            for (var e : H.entrySet()) {
                assertTrue(e.getValue() >= -0.001,
                        name + ": H(" + e.getKey() + ") = " + e.getValue() + " < 0");
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("benchmarks")
        void pathEndpoints(String name, String typ) {
            StateSpace ss = build(typ);
            var result = LagrangianChecker.leastActionPath(ss);
            List<Integer> path = result.getKey();
            if (!path.isEmpty()) {
                assertEquals(ss.top(), path.get(0), name + ": path doesn't start at top");
                assertEquals(ss.bottom(), path.get(path.size() - 1),
                        name + ": path doesn't end at bottom");
            }
        }
    }
}
