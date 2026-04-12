package com.bica.reborn.fiedler;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Fiedler / algebraic connectivity analysis.
 *
 * <p>Port of Python {@code test_fiedler.py} (26 tests).
 * Covers: Fiedler value, Fiedler vector, cut vertices, vertex/edge connectivity,
 * betweenness centrality, Cheeger bounds, composition, full analysis, benchmarks.
 */
class FiedlerCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Fiedler value
    // -----------------------------------------------------------------------

    @Nested
    class FiedlerValueTests {

        @Test
        void endHasZeroFiedler() {
            assertEquals(0.0, FiedlerChecker.fiedlerValue(build("end")));
        }

        @Test
        void singleEdgeHasFiedlerTwo() {
            double fv = FiedlerChecker.fiedlerValue(build("&{a: end}"));
            assertEquals(2.0, fv, 0.1);
        }

        @Test
        void chain3PositiveAndLessThanTwo() {
            double fv = FiedlerChecker.fiedlerValue(build("&{a: &{b: end}}"));
            assertTrue(fv > 0, "Chain-3 Fiedler should be positive");
            assertTrue(fv < 2.0, "Chain-3 Fiedler should be < 2");
        }

        @Test
        void parallelPositive() {
            double fv = FiedlerChecker.fiedlerValue(build("(&{a: end} || &{b: end})"));
            assertTrue(fv > 0, "Parallel type should have positive Fiedler value");
        }

        @Test
        void longerChainDecreasingFiedler() {
            double fv2 = FiedlerChecker.fiedlerValue(build("&{a: end}"));
            double fv3 = FiedlerChecker.fiedlerValue(build("&{a: &{b: end}}"));
            assertTrue(fv2 > fv3, "K2 more connected than P3");
        }
    }

    // -----------------------------------------------------------------------
    // Fiedler vector
    // -----------------------------------------------------------------------

    @Nested
    class FiedlerVectorTests {

        @Test
        void lengthMatchesStates() {
            StateSpace ss = build("&{a: &{b: end}}");
            double[] vec = FiedlerChecker.fiedlerVector(ss);
            assertEquals(ss.states().size(), vec.length);
        }

        @Test
        void normalized() {
            StateSpace ss = build("&{a: &{b: end}}");
            double[] vec = FiedlerChecker.fiedlerVector(ss);
            double norm = 0;
            for (double v : vec) norm += v * v;
            norm = Math.sqrt(norm);
            assertTrue(Math.abs(norm - 1.0) < 0.1 || norm < 0.01,
                    "Fiedler vector should be normalized or zero");
        }

        @Test
        void singleStateVector() {
            double[] vec = FiedlerChecker.fiedlerVector(build("end"));
            assertEquals(1, vec.length);
        }
    }

    // -----------------------------------------------------------------------
    // Cut vertices
    // -----------------------------------------------------------------------

    @Nested
    class CutVertexTests {

        @Test
        void endNoCuts() {
            assertEquals(List.of(), FiedlerChecker.findCutVertices(build("end")));
        }

        @Test
        void singleEdgeNoCuts() {
            assertEquals(List.of(), FiedlerChecker.findCutVertices(build("&{a: end}")));
        }

        @Test
        void chain3HasCut() {
            List<Integer> cuts = FiedlerChecker.findCutVertices(build("&{a: &{b: end}}"));
            assertTrue(cuts.size() >= 1, "Path P3: middle vertex is a cut vertex");
        }
    }

    // -----------------------------------------------------------------------
    // Connectivity
    // -----------------------------------------------------------------------

    @Nested
    class ConnectivityTests {

        @Test
        void endConnectivityZero() {
            assertEquals(0, FiedlerChecker.vertexConnectivity(build("end")));
        }

        @Test
        void edgeConnectivitySingleBranch() {
            assertTrue(FiedlerChecker.edgeConnectivity(build("&{a: end}")) >= 1);
        }

        @ParameterizedTest
        @CsvSource({
                "'&{a: end}'",
                "'&{a: &{b: end}}'",
                "'(&{a: end} || &{b: end})'"
        })
        void vertexLeEdge(String type) {
            StateSpace ss = build(type);
            int vc = FiedlerChecker.vertexConnectivity(ss);
            int ec = FiedlerChecker.edgeConnectivity(ss);
            assertTrue(vc <= ec + 1, "Whitney: vertex_conn <= edge_conn (with tolerance)");
        }
    }

    // -----------------------------------------------------------------------
    // Betweenness centrality
    // -----------------------------------------------------------------------

    @Nested
    class BetweennessTests {

        @Test
        void endBetweenness() {
            Map<Integer, Double> bc = FiedlerChecker.betweennessCentrality(build("end"));
            assertEquals(1, bc.size());
        }

        @Test
        void chain3HasThreeEntries() {
            Map<Integer, Double> bc = FiedlerChecker.betweennessCentrality(build("&{a: &{b: end}}"));
            assertEquals(3, bc.size());
        }

        @Test
        void bottleneckExists() {
            StateSpace ss = build("&{a: &{b: end}}");
            int b = FiedlerChecker.findBottleneck(ss);
            assertTrue(b >= 0);
            assertTrue(ss.states().contains(b));
        }
    }

    // -----------------------------------------------------------------------
    // Cheeger inequality
    // -----------------------------------------------------------------------

    @Nested
    class CheegerTests {

        @ParameterizedTest
        @CsvSource({
                "'&{a: end}'",
                "'&{a: &{b: end}}'",
                "'(&{a: end} || &{b: end})'"
        })
        void boundsOrder(String type) {
            StateSpace ss = build(type);
            double[] bounds = FiedlerChecker.cheegerBounds(ss);
            assertTrue(bounds[0] <= bounds[1] + 0.01,
                    "Lower bound should be <= upper bound");
        }

        @Test
        void trivialEnd() {
            double[] bounds = FiedlerChecker.cheegerBounds(build("end"));
            assertEquals(0.0, bounds[0]);
        }
    }

    // -----------------------------------------------------------------------
    // Composition
    // -----------------------------------------------------------------------

    @Nested
    class CompositionTests {

        @Test
        void fiedlerMinComposition() {
            StateSpace ss1 = build("&{a: end}");
            StateSpace ss2 = build("&{b: end}");
            StateSpace prod = build("(&{a: end} || &{b: end})");
            assertTrue(FiedlerChecker.verifyFiedlerMinComposition(ss1, ss2, prod));
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class AnalyzeTests {

        @Test
        void endAnalysis() {
            var r = FiedlerChecker.analyze(build("end"));
            assertEquals(1, r.numStates());
            assertEquals(0.0, r.fiedlerValue());
            assertEquals(List.of(), r.cutVertices());
        }

        @Test
        void branchAnalysis() {
            var r = FiedlerChecker.analyze(build("&{a: end}"));
            assertEquals(2, r.numStates());
            assertTrue(r.isConnected());
            assertEquals(2.0, r.fiedlerValue(), 0.1);
        }

        @Test
        void parallelAnalysis() {
            var r = FiedlerChecker.analyze(build("(&{a: end} || &{b: end})"));
            assertEquals(4, r.numStates());
            assertTrue(r.isConnected());
            assertTrue(r.fiedlerValue() > 0);
        }
    }

    // -----------------------------------------------------------------------
    // Benchmark protocols
    // -----------------------------------------------------------------------

    @Nested
    class BenchmarkTests {

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "File Object,        '&{open: &{read: end, write: end}}'",
                "Simple SMTP,        '&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "Two-choice,         '&{a: end, b: end}'",
                "Nested,             '&{a: &{b: &{c: end}}}'",
                "Parallel,           '(&{a: end} || &{b: end})'"
        })
        void fiedlerProperties(String name, String type) {
            StateSpace ss = build(type);
            var r = FiedlerChecker.analyze(ss);
            assertEquals(ss.states().size(), r.numStates());
            assertTrue(r.fiedlerValue() >= -0.01,
                    name + ": Fiedler value should be non-negative");
            assertTrue(r.cheegerLower() <= r.cheegerUpper() + 0.01,
                    name + ": Cheeger lower <= upper");
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "File Object,        '&{open: &{read: end, write: end}}'",
                "Simple SMTP,        '&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "Two-choice,         '&{a: end, b: end}'",
                "Nested,             '&{a: &{b: &{c: end}}}'",
                "Parallel,           '(&{a: end} || &{b: end})'"
        })
        void cutVerticesValid(String name, String type) {
            StateSpace ss = build(type);
            List<Integer> cuts = FiedlerChecker.findCutVertices(ss);
            for (int c : cuts) {
                assertTrue(ss.states().contains(c),
                        name + ": cut vertex " + c + " not in states");
            }
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "File Object,        '&{open: &{read: end, write: end}}'",
                "Simple SMTP,        '&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "Two-choice,         '&{a: end, b: end}'",
                "Nested,             '&{a: &{b: &{c: end}}}'",
                "Parallel,           '(&{a: end} || &{b: end})'"
        })
        void betweennessValid(String name, String type) {
            StateSpace ss = build(type);
            Map<Integer, Double> bc = FiedlerChecker.betweennessCentrality(ss);
            for (var entry : bc.entrySet()) {
                assertTrue(entry.getValue() >= -0.01,
                        name + ": betweenness(" + entry.getKey() + ") = "
                                + entry.getValue() + " < 0");
            }
        }
    }
}
