package com.bica.reborn.tutte;

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
 * Tests for TutteChecker: Tutte polynomial, spanning trees, reliability,
 * Kirchhoff's theorem (port of Python test_tutte.py).
 */
class TutteCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // =======================================================================
    // Helper functions
    // =======================================================================

    @Nested
    class Helpers {

        @Test
        void combBasic() {
            assertEquals(10, TutteChecker.comb(5, 2));
            assertEquals(1, TutteChecker.comb(4, 0));
            assertEquals(1, TutteChecker.comb(4, 4));
        }

        @Test
        void combEdge() {
            assertEquals(1, TutteChecker.comb(0, 0));
            assertEquals(0, TutteChecker.comb(3, 5));
        }

        @Test
        void expandBinomialZero() {
            // (x-1)^0 = 1
            var result = TutteChecker.expandBinomial(0);
            assertEquals(1, result.getOrDefault(0, 0));
        }

        @Test
        void expandBinomialOne() {
            // (x-1)^1 = x - 1
            var result = TutteChecker.expandBinomial(1);
            assertEquals(1, result.getOrDefault(1, 0));
            assertEquals(-1, result.getOrDefault(0, 0));
        }

        @Test
        void expandBinomialTwo() {
            // (x-1)^2 = x^2 - 2x + 1
            var result = TutteChecker.expandBinomial(2);
            assertEquals(1, result.getOrDefault(2, 0));
            assertEquals(-2, result.getOrDefault(1, 0));
            assertEquals(1, result.getOrDefault(0, 0));
        }

        @Test
        void connectedComponentsSingle() {
            assertEquals(1, TutteChecker.connectedComponents(List.of(0), List.of()));
        }

        @Test
        void connectedComponentsDisconnected() {
            assertEquals(3, TutteChecker.connectedComponents(List.of(0, 1, 2), List.of()));
        }

        @Test
        void connectedComponentsConnected() {
            assertEquals(1, TutteChecker.connectedComponents(
                    List.of(0, 1, 2),
                    List.of(new int[]{0, 1}, new int[]{1, 2})));
        }

        @Test
        void determinant1x1() {
            assertEquals(3.0, TutteChecker.determinant(new double[][]{{3.0}}), 1e-10);
        }

        @Test
        void determinant2x2() {
            double det = TutteChecker.determinant(new double[][]{{2.0, 1.0}, {1.0, 3.0}});
            assertEquals(5.0, det, 1e-10);
        }

        @Test
        void determinantSingular() {
            double det = TutteChecker.determinant(new double[][]{{1.0, 2.0}, {2.0, 4.0}});
            assertEquals(0.0, det, 1e-10);
        }
    }

    // =======================================================================
    // Hasse undirected
    // =======================================================================

    @Nested
    class HasseUndirected {

        @Test
        void endHasSingleNode() {
            var ss = build("end");
            var g = TutteChecker.hasseUndirected(ss);
            assertEquals(1, g.nodes().size());
            assertTrue(g.edges().isEmpty());
        }

        @Test
        void singleMethodHasNodes() {
            var ss = build("&{a: end}");
            var g = TutteChecker.hasseUndirected(ss);
            assertTrue(g.nodes().size() >= 1);
        }
    }

    // =======================================================================
    // Tutte polynomial
    // =======================================================================

    @Nested
    class TuttePolynomial {

        @Test
        void singleNodeEvaluatesToOne() {
            var ss = build("end");
            var coeffs = TutteChecker.tuttePolynomial(ss);
            assertEquals(1.0, TutteChecker.evalTutte(coeffs, 1.0, 1.0), 1e-10);
        }

        @Test
        void singleEdgePositiveAtTwoOne() {
            var ss = build("&{a: end}");
            var coeffs = TutteChecker.tuttePolynomial(ss);
            assertTrue(TutteChecker.evalTutte(coeffs, 2.0, 1.0) > 0);
        }

        @Test
        void parallelNonEmpty() {
            var ss = build("(&{a: end} || &{b: end})");
            var coeffs = TutteChecker.tuttePolynomial(ss);
            assertFalse(coeffs.isEmpty());
        }

        @Test
        void chainNonEmpty() {
            var ss = build("&{a: &{b: end}}");
            var coeffs = TutteChecker.tuttePolynomial(ss);
            assertFalse(coeffs.isEmpty());
        }
    }

    // =======================================================================
    // Spanning trees
    // =======================================================================

    @Nested
    class SpanningTrees {

        @Test
        void singleNodeTreeCountOne() {
            assertEquals(1, TutteChecker.countSpanningTrees(build("end")));
        }

        @Test
        void singleEdgeTreeCountOne() {
            assertEquals(1, TutteChecker.countSpanningTrees(build("&{a: end}")));
        }

        @Test
        void chainTreeCountOne() {
            assertEquals(1, TutteChecker.countSpanningTrees(build("&{a: &{b: end}}")));
        }

        @Test
        void parallelTreeCountAtLeastOne() {
            int trees = TutteChecker.countSpanningTrees(build("(&{a: end} || &{b: end})"));
            assertTrue(trees >= 1);
        }

        @Test
        void forestsGreaterOrEqualTrees() {
            var ss = build("(&{a: end} || &{b: end})");
            int trees = TutteChecker.countSpanningTrees(ss);
            int forests = TutteChecker.countSpanningForests(ss);
            assertTrue(forests >= trees);
        }
    }

    // =======================================================================
    // Kirchhoff's theorem
    // =======================================================================

    @Nested
    class Kirchhoff {

        @Test
        void singleNode() {
            assertEquals(1, TutteChecker.kirchhoffSpanningTrees(build("end")));
        }

        @Test
        void singleEdge() {
            assertEquals(1, TutteChecker.kirchhoffSpanningTrees(build("&{a: end}")));
        }

        @Test
        void chain() {
            assertEquals(1, TutteChecker.kirchhoffSpanningTrees(build("&{a: &{b: end}}")));
        }

        @ParameterizedTest
        @CsvSource({
                "&{a: end}",
                "&{a: &{b: end}}",
                "'(&{a: end} || &{b: end})'",
                "'&{a: end, b: end}'"
        })
        void kirchhoffMatchesTutte(String type) {
            var ss = build(type);
            int tutte = TutteChecker.countSpanningTrees(ss);
            int kirch = TutteChecker.kirchhoffSpanningTrees(ss);
            assertEquals(tutte, kirch, "Mismatch for " + type);
        }
    }

    // =======================================================================
    // Reliability polynomial
    // =======================================================================

    @Nested
    class Reliability {

        @Test
        void singleNodeReliabilityOne() {
            double rel = TutteChecker.evaluateReliability(build("end"), 1.0);
            assertEquals(1.0, rel, 1e-10);
        }

        @Test
        void fullyReliableAtPOne() {
            double rel = TutteChecker.evaluateReliability(build("&{a: &{b: end}}"), 1.0);
            assertEquals(1.0, rel, 1e-10);
        }

        @Test
        void reliabilityMonotone() {
            var ss = build("(&{a: end} || &{b: end})");
            double rLow = TutteChecker.evaluateReliability(ss, 0.3);
            double rHigh = TutteChecker.evaluateReliability(ss, 0.9);
            assertTrue(rHigh >= rLow - 1e-10);
        }
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    @Nested
    class Analysis {

        @Test
        void endAnalysis() {
            var result = TutteChecker.analyzeTutte(build("end"));
            assertEquals(1, result.numStates());
            assertEquals(1, result.numSpanningTrees());
        }

        @Test
        void chainAnalysis() {
            var result = TutteChecker.analyzeTutte(build("&{a: &{b: end}}"));
            assertTrue(result.isTree());
            assertEquals(1, result.numSpanningTrees());
            assertEquals(1, result.laplacianSpanningTrees());
        }

        @Test
        void parallelAnalysis() {
            var result = TutteChecker.analyzeTutte(build("(&{a: end} || &{b: end})"));
            assertTrue(result.numStates() >= 2);
            assertTrue(result.numSpanningTrees() >= 1);
            assertEquals(result.numSpanningTrees(), result.laplacianSpanningTrees());
        }

        @Test
        void branchAnalysis() {
            var result = TutteChecker.analyzeTutte(build("&{a: end, b: end}"));
            assertTrue(result.numEdges() >= 1);
            assertTrue(result.numSpanningForests() >= result.numSpanningTrees());
        }

        @Test
        void recursiveAnalysis() {
            var result = TutteChecker.analyzeTutte(build("rec X . &{a: X, b: end}"));
            assertTrue(result.numStates() >= 1);
        }
    }

    // =======================================================================
    // Benchmark protocols (parity with Python benchmarks)
    // =======================================================================

    @Nested
    class Benchmarks {

        @ParameterizedTest
        @CsvSource({
                "&{a: end}",
                "'&{a: end, b: end}'",
                "&{a: &{b: end}}",
                "'(&{a: end} || &{b: end})'",
                "'rec X . &{a: X, b: end}'",
                "+{a: end}",
                "'+{a: end, b: end}'",
                "'&{a: +{ok: end, err: end}}'",
                "'&{a: +{ok: &{b: end}, err: end}}'"
        })
        void tutteAndKirchhoffAgree(String type) {
            var ss = build(type);
            int tutte = TutteChecker.countSpanningTrees(ss);
            int kirch = TutteChecker.kirchhoffSpanningTrees(ss);
            assertEquals(tutte, kirch, "Spanning tree mismatch for: " + type);
        }

        @ParameterizedTest
        @CsvSource({
                "&{a: end}",
                "'&{a: end, b: end}'",
                "&{a: &{b: end}}",
                "'(&{a: end} || &{b: end})'",
                "'+{a: end, b: end}'"
        })
        void forestsAtLeastTrees(String type) {
            var ss = build(type);
            int trees = TutteChecker.countSpanningTrees(ss);
            int forests = TutteChecker.countSpanningForests(ss);
            assertTrue(forests >= trees, "Forests < trees for: " + type);
        }

        @ParameterizedTest
        @CsvSource({
                "&{a: end}",
                "'&{a: end, b: end}'",
                "&{a: &{b: end}}",
                "'(&{a: end} || &{b: end})'",
                "'+{a: end, b: end}'"
        })
        void reliabilityAtPOneIsOne(String type) {
            var ss = build(type);
            double rel = TutteChecker.evaluateReliability(ss, 1.0);
            assertEquals(1.0, rel, 1e-8, "R(1.0) != 1 for: " + type);
        }
    }

    // =======================================================================
    // Laplacian matrix
    // =======================================================================

    @Nested
    class LaplacianMatrix {

        @Test
        void singleEdgeLaplacian() {
            int[][] L = TutteChecker.laplacianMatrix(List.of(0, 1), List.of(new int[]{0, 1}));
            assertEquals(2, L.length);
            assertEquals(1, L[0][0]);
            assertEquals(-1, L[0][1]);
            assertEquals(-1, L[1][0]);
            assertEquals(1, L[1][1]);
        }

        @Test
        void triangleLaplacian() {
            int[][] L = TutteChecker.laplacianMatrix(
                    List.of(0, 1, 2),
                    List.of(new int[]{0, 1}, new int[]{0, 2}, new int[]{1, 2}));
            // Each node has degree 2
            assertEquals(2, L[0][0]);
            assertEquals(2, L[1][1]);
            assertEquals(2, L[2][2]);
        }
    }

    // =======================================================================
    // Additional edge cases
    // =======================================================================

    @Nested
    class EdgeCases {

        @Test
        void deepChain() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var result = TutteChecker.analyzeTutte(ss);
            assertTrue(result.isTree());
            assertEquals(1, result.numSpanningTrees());
        }

        @Test
        void evalTutteEmpty() {
            assertEquals(0.0, TutteChecker.evalTutte(Map.of(), 1.0, 1.0), 1e-10);
        }
    }
}
