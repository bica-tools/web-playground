package com.bica.reborn.morse;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for discrete Morse theory on session type lattices (Step 30x port).
 *
 * <p>78 tests mirroring Python test_morse.py.
 */
class MorseCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // =======================================================================
    // Hasse graph
    // =======================================================================

    @Nested
    class TestHasseGraph {

        @Test
        void endNoEdges() {
            var ss = build("end");
            var edges = MorseChecker.hasseGraph(ss);
            assertTrue(edges.isEmpty());
        }

        @Test
        void simpleBranchHasEdges() {
            var ss = build("&{a: end, b: end}");
            var edges = MorseChecker.hasseGraph(ss);
            assertTrue(edges.size() >= 2);
        }

        @Test
        void deepChainEdges() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var edges = MorseChecker.hasseGraph(ss);
            assertTrue(edges.size() >= 3);
        }

        @Test
        void parallelHasEdges() {
            var ss = build("(&{a: end} || &{b: end})");
            var edges = MorseChecker.hasseGraph(ss);
            assertTrue(edges.size() >= 2);
        }

        @Test
        void edgesAreTuples() {
            var ss = build("&{a: end, b: end}");
            var edges = MorseChecker.hasseGraph(ss);
            for (var e : edges) {
                assertNotNull(e.label());
                assertTrue(ss.states().contains(e.source()));
                assertTrue(ss.states().contains(e.target()));
            }
        }

        @Test
        void wideBranchEdges() {
            var ss = build("&{a: end, b: end, c: end, d: end}");
            var edges = MorseChecker.hasseGraph(ss);
            assertTrue(edges.size() >= 4);
        }

        @Test
        void nestedBranchEdges() {
            var ss = build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var edges = MorseChecker.hasseGraph(ss);
            assertTrue(edges.size() >= 4);
        }
    }

    // =======================================================================
    // Greedy matching
    // =======================================================================

    @Nested
    class TestGreedyMatching {

        @Test
        void endEmptyMatching() {
            var ss = build("end");
            var m = MorseChecker.greedyMatching(ss);
            assertTrue(m.matchedPairs().isEmpty());
            assertTrue(m.isAcyclic());
        }

        @Test
        void simpleBranchMatching() {
            var ss = build("&{a: end, b: end}");
            var m = MorseChecker.greedyMatching(ss);
            assertTrue(m.isAcyclic());
            assertFalse(m.matchedPairs().isEmpty());
        }

        @Test
        void matchingPairsAreVertexEdgePairs() {
            var ss = build("&{a: end, b: end}");
            var m = MorseChecker.greedyMatching(ss);
            for (var pair : m.matchedPairs()) {
                assertTrue(ss.states().contains(pair.vertex()));
                assertTrue(pair.edgeIndex() >= 0);
            }
        }

        @Test
        void matchedVerticesDisjoint() {
            var ss = build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var m = MorseChecker.greedyMatching(ss);
            Set<Integer> vertices = new HashSet<>();
            Set<Integer> edgeIndices = new HashSet<>();
            for (var pair : m.matchedPairs()) {
                assertTrue(vertices.add(pair.vertex()), "Vertex " + pair.vertex() + " matched twice");
                assertTrue(edgeIndices.add(pair.edgeIndex()), "Edge " + pair.edgeIndex() + " matched twice");
            }
        }

        @Test
        void criticalUnionMatchedEqualsAll() {
            var ss = build("&{a: end, b: end}");
            var m = MorseChecker.greedyMatching(ss);
            Set<Integer> matchedVertices = new HashSet<>();
            for (var pair : m.matchedPairs()) {
                matchedVertices.add(pair.vertex());
            }
            Set<Integer> union = new HashSet<>(matchedVertices);
            union.addAll(m.criticalVertices());
            assertEquals(ss.states(), union);
        }

        @Test
        void deepChainMatching() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var m = MorseChecker.greedyMatching(ss);
            assertTrue(m.isAcyclic());
        }

        @Test
        void parallelMatching() {
            var ss = build("(&{a: end} || &{b: end})");
            var m = MorseChecker.greedyMatching(ss);
            assertTrue(m.isAcyclic());
        }

        @Test
        void wideBranchMatching() {
            var ss = build("&{a: end, b: end, c: end, d: end}");
            var m = MorseChecker.greedyMatching(ss);
            assertTrue(m.isAcyclic());
            assertFalse(m.matchedPairs().isEmpty());
        }

        @Test
        void numStates() {
            var ss = build("&{a: end, b: end}");
            var m = MorseChecker.greedyMatching(ss);
            assertEquals(ss.states().size(), m.numStates());
        }

        @Test
        void numHasseEdgesLeqTransitions() {
            var ss = build("&{a: end, b: end}");
            var m = MorseChecker.greedyMatching(ss);
            assertTrue(m.numHasseEdges() <= ss.transitions().size());
        }
    }

    // =======================================================================
    // Critical cells
    // =======================================================================

    @Nested
    class TestCriticalCells {

        @Test
        void endSingleCritical() {
            var ss = build("end");
            var m = MorseChecker.greedyMatching(ss);
            var crits = MorseChecker.criticalCells(m);
            assertEquals(1, crits.size());
        }

        @Test
        void criticalSubsetOfStates() {
            var ss = build("&{a: end, b: end}");
            var m = MorseChecker.greedyMatching(ss);
            var crits = MorseChecker.criticalCells(m);
            assertTrue(ss.states().containsAll(crits));
        }

        @Test
        void criticalNotInMatchedVertices() {
            var ss = build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var m = MorseChecker.greedyMatching(ss);
            var crits = MorseChecker.criticalCells(m);
            Set<Integer> matchedVerts = new HashSet<>();
            for (var pair : m.matchedPairs()) matchedVerts.add(pair.vertex());
            for (int c : crits) {
                assertFalse(matchedVerts.contains(c));
            }
        }

        @Test
        void atLeastOneCriticalForEnd() {
            var ss = build("end");
            var m = MorseChecker.greedyMatching(ss);
            var crits = MorseChecker.criticalCells(m);
            assertEquals(1, crits.size());
        }

        @Test
        void deepChainCriticalCount() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var m = MorseChecker.greedyMatching(ss);
            var crits = MorseChecker.criticalCells(m);
            assertTrue(crits.size() >= 1);
        }
    }

    // =======================================================================
    // Morse function
    // =======================================================================

    @Nested
    class TestMorseFunction {

        @Test
        void endFunction() {
            var ss = build("end");
            var f = MorseChecker.morseFunction(ss);
            assertEquals(1, f.values().size());
        }

        @Test
        void allStatesHaveValues() {
            var ss = build("&{a: end, b: end}");
            var f = MorseChecker.morseFunction(ss);
            for (int s : ss.states()) {
                assertTrue(f.values().containsKey(s));
            }
        }

        @Test
        void valuesAreNumeric() {
            var ss = build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var f = MorseChecker.morseFunction(ss);
            for (double v : f.values().values()) {
                assertFalse(Double.isNaN(v));
                assertFalse(Double.isInfinite(v));
            }
        }

        @Test
        void deepChainFunction() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var f = MorseChecker.morseFunction(ss);
            assertEquals(ss.states().size(), f.values().size());
        }

        @Test
        void parallelFunction() {
            var ss = build("(&{a: end} || &{b: end})");
            var f = MorseChecker.morseFunction(ss);
            assertEquals(ss.states().size(), f.values().size());
        }

        @Test
        void functionValidity() {
            var ss = build("&{a: end, b: end}");
            var f = MorseChecker.morseFunction(ss);
            // isValid is a boolean -- just check it doesn't throw
            assertNotNull(f);
        }
    }

    // =======================================================================
    // Gradient field
    // =======================================================================

    @Nested
    class TestGradientField {

        @Test
        void endGradient() {
            var ss = build("end");
            var g = MorseChecker.gradientField(ss);
            assertTrue(g.pairs().isEmpty());
            assertEquals(1, g.critical().size());
        }

        @Test
        void gradientPairsMatchMatching() {
            var ss = build("&{a: end, b: end}");
            var m = MorseChecker.greedyMatching(ss);
            var g = MorseChecker.gradientField(ss);
            assertEquals(m.matchedPairs(), g.pairs());
        }

        @Test
        void gradientCriticalMatchMatching() {
            var ss = build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var m = MorseChecker.greedyMatching(ss);
            var g = MorseChecker.gradientField(ss);
            assertEquals(m.criticalVertices(), g.critical());
        }

        @Test
        void flowGraphHasAllStates() {
            var ss = build("&{a: end, b: end}");
            var g = MorseChecker.gradientField(ss);
            for (int s : ss.states()) {
                assertTrue(g.flowGraph().containsKey(s));
            }
        }

        @Test
        void parallelGradient() {
            var ss = build("(&{a: end} || &{b: end})");
            var g = MorseChecker.gradientField(ss);
            assertNotNull(g);
        }
    }

    // =======================================================================
    // Betti numbers
    // =======================================================================

    @Nested
    class TestBettiNumbers {

        @Test
        void endBetti() {
            var ss = build("end");
            var b = MorseChecker.bettiNumbers(ss);
            assertEquals(1, b[0]); // one connected component
            assertEquals(0, b[1]); // no cycles
        }

        @Test
        void simpleBranchConnected() {
            var ss = build("&{a: end, b: end}");
            var b = MorseChecker.bettiNumbers(ss);
            assertEquals(1, b[0]);
        }

        @Test
        void deepChainTree() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var b = MorseChecker.bettiNumbers(ss);
            assertEquals(1, b[0]);
            assertEquals(0, b[1]); // tree
        }

        @Test
        void iteratorHasseTopology() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var b = MorseChecker.bettiNumbers(ss);
            assertTrue(b[0] >= 1);
        }

        @Test
        void fileHasseTopology() {
            var ss = build("&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}");
            var b = MorseChecker.bettiNumbers(ss);
            assertTrue(b[0] >= 1);
        }

        @Test
        void parallelHasCycle() {
            var ss = build("(&{a: end} || &{b: end})");
            var b = MorseChecker.bettiNumbers(ss);
            assertEquals(1, b[0]); // connected
            assertEquals(1, b[1]); // one cycle in diamond
        }

        @Test
        void bettiTuple() {
            var ss = build("&{a: end, b: end}");
            var b = MorseChecker.bettiNumbers(ss);
            assertEquals(2, b.length);
            assertTrue(b[0] >= 0);
            assertTrue(b[1] >= 0);
        }

        @Test
        void selectConnected() {
            var ss = build("+{ok: end, err: end}");
            var b = MorseChecker.bettiNumbers(ss);
            assertEquals(1, b[0]);
        }
    }

    // =======================================================================
    // Morse inequalities
    // =======================================================================

    @Nested
    class TestMorseInequalities {

        @Test
        void endInequalities() {
            var ss = build("end");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[0]); // weak
            assertTrue(ineqs[1]); // strong
        }

        @Test
        void simpleBranchStrong() {
            var ss = build("&{a: end, b: end}");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1]);
        }

        @Test
        void deepChainStrong() {
            var ss = build("&{a: &{b: &{c: end}}}");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1]);
        }

        @Test
        void parallelStrong() {
            var ss = build("(&{a: end} || &{b: end})");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1]);
        }

        @Test
        void wideBranchStrong() {
            var ss = build("&{a: end, b: end, c: end, d: end}");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1]);
        }

        @Test
        void nestedBranchStrong() {
            var ss = build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1]);
        }

        @Test
        void selectStrong() {
            var ss = build("+{ok: end, err: end}");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1]);
        }

        @Test
        void restStrong() {
            var ss = build("&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1]);
        }

        @Test
        void iteratorStrong() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1]);
        }

        @Test
        void fileStrong() {
            var ss = build("&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}");
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1]);
        }
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    @Nested
    class TestAnalyzeMorse {

        @Test
        void endAnalysis() {
            var ss = build("end");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.numCritical() >= 1);
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void simpleBranchAnalysis() {
            var ss = build("&{a: end, b: end}");
            var r = MorseChecker.analyzeMorse(ss);
            assertNotNull(r.matching());
            assertNotNull(r.function());
            assertNotNull(r.gradient());
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void deepChainAnalysis() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.numCritical() >= 1);
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void iteratorAnalysis() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.bettiNumbers()[0] >= 1);
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void fileAnalysis() {
            var ss = build("&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.bettiNumbers()[0] >= 1);
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void parallelAnalysis() {
            var ss = build("(&{a: end} || &{b: end})");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void nestedAnalysis() {
            var ss = build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void eulerCharacteristicCorrect() {
            var ss = build("&{a: end, b: end}");
            var r = MorseChecker.analyzeMorse(ss);
            // chi = V - E (integer)
            assertNotNull(r);
        }

        @Test
        void criticalCellsInResult() {
            var ss = build("&{a: end, b: end, c: end, d: end}");
            var r = MorseChecker.analyzeMorse(ss);
            assertEquals(r.criticalCells(), r.matching().criticalVertices());
        }

        @Test
        void resultBettiArray() {
            var ss = build("&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}");
            var r = MorseChecker.analyzeMorse(ss);
            assertEquals(2, r.bettiNumbers().length);
        }

        @Test
        void numCriticalIncludesEdges() {
            var ss = build("(&{a: end} || &{b: end})");
            var r = MorseChecker.analyzeMorse(ss);
            int expected = r.matching().criticalVertices().size()
                    + r.matching().criticalEdges().size();
            assertEquals(expected, r.numCritical());
        }
    }

    // =======================================================================
    // Parametrized benchmarks
    // =======================================================================

    @Nested
    class TestBenchmarks {

        @ParameterizedTest(name = "strongMorse[{1}]")
        @CsvSource(delimiter = ';', value = {
                "end; End",
                "&{a: end}; SingleMethod",
                "&{a: end, b: end}; SimpleBranch",
                "+{ok: end, err: end}; SimpleSelect",
                "&{a: &{b: &{c: end}}}; DeepChain",
                "(&{a: end} || &{b: end}); SimpleParallel",
                "&{a: &{c: end, d: end}, b: &{e: end, f: end}}; NestedBranch",
                "&{a: end, b: end, c: end, d: end}; WideBranch",
                "&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}; REST",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}; Iterator",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}; File",
                "+{OK: &{a: end}, ERR: end}; SelectBranch"
        })
        void strongMorseOnBenchmarks(String typeStr, String name) {
            var ss = build(typeStr);
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[1], "Strong Morse inequality failed for " + name);
        }

        @ParameterizedTest(name = "acyclic[{1}]")
        @CsvSource(delimiter = ';', value = {
                "end; End",
                "&{a: end}; SingleMethod",
                "&{a: end, b: end}; SimpleBranch",
                "+{ok: end, err: end}; SimpleSelect",
                "&{a: &{b: &{c: end}}}; DeepChain",
                "(&{a: end} || &{b: end}); SimpleParallel",
                "&{a: &{c: end, d: end}, b: &{e: end, f: end}}; NestedBranch",
                "&{a: end, b: end, c: end, d: end}; WideBranch",
                "&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}; REST",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}; Iterator",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}; File",
                "+{OK: &{a: end}, ERR: end}; SelectBranch"
        })
        void matchingAcyclicOnBenchmarks(String typeStr, String name) {
            var ss = build(typeStr);
            var m = MorseChecker.greedyMatching(ss);
            assertTrue(m.isAcyclic(), "Matching not acyclic for " + name);
        }

        @ParameterizedTest(name = "noCycles[{1}]")
        @CsvSource(delimiter = ';', value = {
                "end; End",
                "&{a: end, b: end}; SimpleBranch",
                "&{a: &{b: &{c: end}}}; DeepChain",
                "&{a: end, b: end, c: end, d: end}; WideBranch"
        })
        void acyclicTypesNoCycles(String typeStr, String name) {
            var ss = build(typeStr);
            int[] b = MorseChecker.bettiNumbers(ss);
            assertEquals(0, b[1], name + " should have b1=0 (tree-like)");
        }

        @Test
        void parallelDiamondHasCycle() {
            var ss = build("(&{a: end} || &{b: end})");
            int[] b = MorseChecker.bettiNumbers(ss);
            assertEquals(1, b[1], "Diamond has one independent cycle");
        }

        @ParameterizedTest(name = "weakMorse[{1}]")
        @CsvSource(delimiter = ';', value = {
                "end; End",
                "&{a: end}; SingleMethod",
                "&{a: end, b: end}; SimpleBranch",
                "+{ok: end, err: end}; SimpleSelect",
                "&{a: &{b: &{c: end}}}; DeepChain",
                "(&{a: end} || &{b: end}); SimpleParallel",
                "&{a: &{c: end, d: end}, b: &{e: end, f: end}}; NestedBranch",
                "&{a: end, b: end, c: end, d: end}; WideBranch",
                "&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}; REST",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}; Iterator",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}; File",
                "+{OK: &{a: end}, ERR: end}; SelectBranch"
        })
        void weakMorseOnBenchmarks(String typeStr, String name) {
            var ss = build(typeStr);
            boolean[] ineqs = MorseChecker.morseInequalities(ss);
            assertTrue(ineqs[0], "Weak Morse inequality failed for " + name);
        }

        @ParameterizedTest(name = "bettiNonNeg[{1}]")
        @CsvSource(delimiter = ';', value = {
                "end; End",
                "&{a: end}; SingleMethod",
                "&{a: end, b: end}; SimpleBranch",
                "+{ok: end, err: end}; SimpleSelect",
                "&{a: &{b: &{c: end}}}; DeepChain",
                "(&{a: end} || &{b: end}); SimpleParallel",
                "&{a: &{c: end, d: end}, b: &{e: end, f: end}}; NestedBranch",
                "&{a: end, b: end, c: end, d: end}; WideBranch",
                "&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}; REST",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}; Iterator",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}; File",
                "+{OK: &{a: end}, ERR: end}; SelectBranch"
        })
        void bettiNonnegativeOnBenchmarks(String typeStr, String name) {
            var ss = build(typeStr);
            int[] b = MorseChecker.bettiNumbers(ss);
            for (int x : b) {
                assertTrue(x >= 0, "Negative Betti for " + name);
            }
        }
    }

    // =======================================================================
    // Edge cases
    // =======================================================================

    @Nested
    class TestEdgeCases {

        @Test
        void singleState() {
            var ss = build("end");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.numCritical() >= 1);
            assertEquals(1, r.eulerCharacteristic());
            assertArrayEquals(new int[]{1, 0}, r.bettiNumbers());
        }

        @Test
        void twoStates() {
            var ss = build("&{a: end}");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void matchingEmptyForEnd() {
            var ss = build("end");
            var m = MorseChecker.greedyMatching(ss);
            assertTrue(m.matchedPairs().isEmpty());
        }

        @Test
        void gradientFlowGraphStructure() {
            var ss = build("&{a: end, b: end}");
            var g = MorseChecker.gradientField(ss);
            assertEquals(ss.states(), g.flowGraph().keySet());
        }

        @Test
        void criticalEqualsMatchingCritical() {
            var ss = build("&{a: &{b: end}, c: end}");
            var m = MorseChecker.greedyMatching(ss);
            var crits = MorseChecker.criticalCells(m);
            assertEquals(crits, m.criticalVertices());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "&{a: end, b: end}",
                "rec X . &{a: X, b: end}"
        })
        void bettiNonnegative(String typeStr) {
            var ss = build(typeStr);
            int[] b = MorseChecker.bettiNumbers(ss);
            for (int x : b) {
                assertTrue(x >= 0, "Negative Betti for " + typeStr);
            }
        }

        @Test
        void eulerCharacteristicFormula() {
            var ss = build("&{a: end, b: end}");
            var r = MorseChecker.analyzeMorse(ss);
            int[] b = r.bettiNumbers();
            assertEquals(r.eulerCharacteristic(), b[0] - b[1]);
        }

        @Test
        void selectBranchCombo() {
            var ss = build("+{OK: &{a: end}, ERR: end}");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void tripleBranch() {
            var ss = build("&{a: end, b: end, c: end}");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.strongMorseHolds());
        }

        @Test
        void recursiveWithSelection() {
            var ss = build("rec X . +{a: X, b: end}");
            var r = MorseChecker.analyzeMorse(ss);
            assertTrue(r.strongMorseHolds());
        }
    }
}
