package com.bica.reborn.char_poly;

import com.bica.reborn.characteristic.CharacteristicChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for characteristic polynomial via deletion-contraction (Step 32c) -- Java port.
 */
class CharPolyCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Polynomial arithmetic
    // -----------------------------------------------------------------------

    @Nested
    class PolyArithmetic {

        @Test
        void subtractEqual() {
            assertEquals(List.of(0),
                    CharPolyChecker.polySubtract(List.of(1, 2, 3), List.of(1, 2, 3)));
        }

        @Test
        void subtractDifferentLength() {
            // t^2 - t = t^2 - t
            assertEquals(List.of(1, -1, 0),
                    CharPolyChecker.polySubtract(List.of(1, 0, 0), List.of(0, 1, 0)));
        }

        @Test
        void addBasic() {
            assertEquals(List.of(1, 1),
                    CharPolyChecker.polyAdd(List.of(1, 0), List.of(0, 1)));
        }

        @Test
        void addDifferentLength() {
            assertEquals(List.of(1, 1, 0),
                    CharPolyChecker.polyAdd(List.of(1, 0, 0), List.of(1, 0)));
        }

        @Test
        void subtractLeadingZeros() {
            assertEquals(List.of(0),
                    CharPolyChecker.polySubtract(List.of(1, 0), List.of(1, 0)));
        }
    }

    // -----------------------------------------------------------------------
    // Graph operations
    // -----------------------------------------------------------------------

    @Nested
    class GraphOperations {

        @Test
        void hasseGraphEnd() {
            var ss = build("end");
            var hg = CharPolyChecker.hasseGraph(ss);
            assertEquals(1, CharPolyChecker.nodes(hg).length);
            assertEquals(0, CharPolyChecker.edges(hg).size());
        }

        @Test
        void hasseGraphSingle() {
            var ss = build("&{a: end}");
            var hg = CharPolyChecker.hasseGraph(ss);
            assertTrue(CharPolyChecker.nodes(hg).length >= 1);
        }

        @Test
        void deleteEdge() {
            List<Integer> nodes = List.of(0, 1, 2);
            List<int[]> edges = List.of(new int[]{0, 1}, new int[]{1, 2});
            List<Integer> outNodes = new ArrayList<>();
            List<int[]> outEdges = new ArrayList<>();
            CharPolyChecker.graphDelete(nodes, edges, new int[]{0, 1}, outNodes, outEdges);
            assertEquals(1, outEdges.size());
            assertEquals(1, outEdges.get(0)[0]);
            assertEquals(2, outEdges.get(0)[1]);
            assertEquals(3, outNodes.size());
        }

        @Test
        void contractEdge() {
            List<Integer> nodes = List.of(0, 1, 2);
            List<int[]> edges = List.of(new int[]{0, 1}, new int[]{1, 2});
            List<Integer> outNodes = new ArrayList<>();
            List<int[]> outEdges = new ArrayList<>();
            CharPolyChecker.graphContract(nodes, edges, new int[]{0, 1}, outNodes, outEdges);
            assertFalse(outNodes.contains(1)); // v=1 merged into u=0
            assertEquals(2, outNodes.size());
            // Edge (1,2) becomes (0,2)
            assertTrue(outEdges.stream().anyMatch(e -> e[0] == 0 && e[1] == 2));
        }

        @Test
        void contractNoSelfLoop() {
            List<Integer> nodes = List.of(0, 1);
            List<int[]> edges = List.of(new int[]{0, 1});
            List<Integer> outNodes = new ArrayList<>();
            List<int[]> outEdges = new ArrayList<>();
            CharPolyChecker.graphContract(nodes, edges, new int[]{0, 1}, outNodes, outEdges);
            assertEquals(1, outNodes.size());
            assertEquals(0, outEdges.size());
        }

        @Test
        void deletionApi() {
            var ss = build("&{a: &{b: end}}");
            var result = CharPolyChecker.deletion(ss, 0);
            assertTrue(result.get(0) instanceof int[]);
        }

        @Test
        void contractionApi() {
            var ss = build("&{a: &{b: end}}");
            var result = CharPolyChecker.contraction(ss, 0);
            assertTrue(result.get(0) instanceof int[]);
        }
    }

    // -----------------------------------------------------------------------
    // Deletion-contraction algorithm
    // -----------------------------------------------------------------------

    @Nested
    class DeletionContraction {

        @Test
        void noEdges() {
            // Graph with no edges: p(G, t) = t^3
            var result = CharPolyChecker.charPolyDcGraph(
                    List.of(0, 1, 2), List.of(), new HashMap<>());
            assertEquals(List.of(1, 0, 0, 0), result);
        }

        @Test
        void singleEdge() {
            // K_2: p(G, t) = t(t-1) = t^2 - t
            var result = CharPolyChecker.charPolyDcGraph(
                    List.of(0, 1), List.of(new int[]{0, 1}), new HashMap<>());
            assertEquals(List.of(1, -1, 0), result);
            assertEquals(2, CharacteristicChecker.polyEvaluate(result, 2));
        }

        @Test
        void pathThree() {
            // Path P_3: p = t(t-1)^2 = t^3 - 2t^2 + t
            var result = CharPolyChecker.charPolyDcGraph(
                    List.of(0, 1, 2),
                    List.of(new int[]{0, 1}, new int[]{1, 2}),
                    new HashMap<>());
            assertEquals(List.of(1, -2, 1, 0), result);
            assertEquals(2, CharacteristicChecker.polyEvaluate(result, 2));
        }

        @Test
        void triangle() {
            // K_3: p = t(t-1)(t-2) = t^3 - 3t^2 + 2t
            var result = CharPolyChecker.charPolyDcGraph(
                    List.of(0, 1, 2),
                    List.of(new int[]{0, 1}, new int[]{0, 2}, new int[]{1, 2}),
                    new HashMap<>());
            assertEquals(List.of(1, -3, 2, 0), result);
            assertEquals(6, CharacteristicChecker.polyEvaluate(result, 3));
        }

        @Test
        void singleNode() {
            var result = CharPolyChecker.charPolyDcGraph(
                    List.of(0), List.of(), new HashMap<>());
            assertEquals(List.of(1, 0), result);
        }
    }

    @Nested
    class CharPolynomial {

        @Test
        void end() {
            var ss = build("end");
            var coeffs = CharPolyChecker.charPolynomial(ss);
            assertNotNull(coeffs);
            assertFalse(coeffs.isEmpty());
        }

        @Test
        void singleMethod() {
            var ss = build("&{a: end}");
            var coeffs = CharPolyChecker.charPolynomial(ss);
            assertNotNull(coeffs);
        }

        @Test
        void chainThree() {
            var ss = build("&{a: &{b: end}}");
            var coeffs = CharPolyChecker.charPolynomial(ss);
            assertNotNull(coeffs);
            assertTrue(CharacteristicChecker.polyEvaluate(coeffs, 2) > 0);
        }

        @Test
        void evaluate() {
            var ss = build("&{a: &{b: end}}");
            long val = CharPolyChecker.evaluateCharPoly(ss, 3);
            assertTrue(val > 0);
        }

        @Test
        void parallel() {
            var ss = build("(&{a: end} || &{b: end})");
            var coeffs = CharPolyChecker.charPolynomial(ss);
            assertNotNull(coeffs);
            assertFalse(coeffs.isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // Chromatic number bound
    // -----------------------------------------------------------------------

    @Nested
    class ChromaticBound {

        @Test
        void endChromatic() {
            assertTrue(CharPolyChecker.chromaticNumberBound(build("end")) >= 1);
        }

        @Test
        void chainChromatic() {
            int bound = CharPolyChecker.chromaticNumberBound(build("&{a: &{b: end}}"));
            assertTrue(bound <= 3);
        }

        @Test
        void parallelChromatic() {
            assertTrue(CharPolyChecker.chromaticNumberBound(build("(&{a: end} || &{b: end})")) >= 1);
        }
    }

    // -----------------------------------------------------------------------
    // Comparison with Moebius
    // -----------------------------------------------------------------------

    @Nested
    class Comparison {

        @Test
        void endMatch() {
            var result = CharPolyChecker.analyzeCharPoly(build("end"));
            assertNotNull(result.coefficientsDc());
            assertNotNull(result.coefficientsMobius());
        }

        @Test
        void chainAnalysis() {
            var result = CharPolyChecker.analyzeCharPoly(build("&{a: &{b: end}}"));
            assertTrue(result.degree() >= 0);
            assertTrue(result.numStates() >= 1);
            assertTrue(result.numEdges() >= 0);
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class Analysis {

        @Test
        void endAnalysis() {
            var result = CharPolyChecker.analyzeCharPoly(build("end"));
            assertTrue(result.numStates() >= 1);
            assertTrue(result.chromaticBound() >= 1);
        }

        @Test
        void branchAnalysis() {
            var result = CharPolyChecker.analyzeCharPoly(build("&{a: end, b: end}"));
            assertTrue(result.degree() >= 0);
            assertTrue(result.chromaticBound() >= 1);
        }

        @Test
        void selectionAnalysis() {
            var result = CharPolyChecker.analyzeCharPoly(build("+{a: end, b: end}"));
            assertTrue(result.numEdges() >= 0);
        }

        @Test
        void recursiveAnalysis() {
            var result = CharPolyChecker.analyzeCharPoly(build("rec X . &{a: X, b: end}"));
            assertTrue(result.numStates() >= 1);
            assertTrue(result.chromaticBound() >= 1);
        }

        @Test
        void parallelAnalysis() {
            var result = CharPolyChecker.analyzeCharPoly(build("(&{a: end} || &{b: end})"));
            assertTrue(result.numStates() >= 2);
            assertTrue(result.chromaticBound() >= 1);
        }

        @Test
        void evalAt0Chain() {
            var result = CharPolyChecker.analyzeCharPoly(build("&{a: &{b: end}}"));
            assertEquals(0, result.evalAt0());
        }

        @Test
        void evalAt1Chain() {
            var result = CharPolyChecker.analyzeCharPoly(build("&{a: &{b: end}}"));
            assertEquals(0, result.evalAt1());
        }
    }
}
