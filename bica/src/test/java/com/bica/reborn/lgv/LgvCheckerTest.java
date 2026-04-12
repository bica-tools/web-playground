package com.bica.reborn.lgv;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.lgv.LgvChecker.LgvResult;
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
 * Port of reticulate/tests/test_lgv.py (60 tests).
 * Tests LGV Lemma analysis for session type lattices.
 */
class LgvCheckerTest {

    private static StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -------------------------------------------------------------------
    // Permutation sign
    // -------------------------------------------------------------------

    @Nested
    class PermSignTests {

        @Test
        void identity1() {
            assertEquals(1, LgvChecker.permSign(new int[]{0}));
        }

        @Test
        void identity3() {
            assertEquals(1, LgvChecker.permSign(new int[]{0, 1, 2}));
        }

        @Test
        void transposition() {
            assertEquals(-1, LgvChecker.permSign(new int[]{1, 0}));
        }

        @Test
        void cycle3() {
            // (0 1 2) = even permutation
            assertEquals(1, LgvChecker.permSign(new int[]{1, 2, 0}));
        }

        @Test
        void transposition3() {
            // (0 1) applied to 3 elements
            assertEquals(-1, LgvChecker.permSign(new int[]{1, 0, 2}));
        }

        @Test
        void identity4() {
            assertEquals(1, LgvChecker.permSign(new int[]{0, 1, 2, 3}));
        }

        @Test
        void doubleTransposition() {
            // Two transpositions = even
            assertEquals(1, LgvChecker.permSign(new int[]{1, 0, 3, 2}));
        }
    }

    // -------------------------------------------------------------------
    // Determinant
    // -------------------------------------------------------------------

    @Nested
    class DeterminantTests {

        @Test
        void empty() {
            assertEquals(0, LgvChecker.determinant(new int[0][0]));
        }

        @Test
        void oneByOne() {
            assertEquals(5, LgvChecker.determinant(new int[][]{{5}}));
        }

        @Test
        void twoByTwo() {
            assertEquals(-2, LgvChecker.determinant(new int[][]{{1, 2}, {3, 4}}));
        }

        @Test
        void twoByTwoIdentity() {
            assertEquals(1, LgvChecker.determinant(new int[][]{{1, 0}, {0, 1}}));
        }

        @Test
        void threeByThreeSingular() {
            int[][] M = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
            assertEquals(0, LgvChecker.determinant(M));
        }

        @Test
        void threeByThreeNonsingular() {
            int[][] M = {{1, 0, 2}, {0, 1, 0}, {3, 0, 1}};
            assertEquals(-5, LgvChecker.determinant(M));
        }

        @Test
        void leibnizAgreesBareiss() {
            int[][] M = {{2, 1, 3}, {0, 4, 1}, {1, 0, 2}};
            assertEquals(LgvChecker.determinantBareiss(M),
                         LgvChecker.determinantLeibniz(M));
        }

        @Test
        void diagonal() {
            int[][] M = {{2, 0, 0}, {0, 3, 0}, {0, 0, 5}};
            assertEquals(30, LgvChecker.determinant(M));
        }

        @Test
        void upperTriangular() {
            int[][] M = {{1, 2, 3}, {0, 4, 5}, {0, 0, 6}};
            assertEquals(24, LgvChecker.determinant(M));
        }

        @Test
        void zeroMatrix() {
            int[][] M = {{0, 0}, {0, 0}};
            assertEquals(0, LgvChecker.determinant(M));
        }
    }

    // -------------------------------------------------------------------
    // Path counting
    // -------------------------------------------------------------------

    @Nested
    class PathCountingTests {

        @Test
        void endTypeSelfPath() {
            StateSpace ss = build("end");
            assertEquals(1, LgvChecker.countPaths(ss, ss.top(), ss.bottom()));
        }

        @Test
        void singleBranchOnePath() {
            StateSpace ss = build("&{a: end}");
            assertEquals(1, LgvChecker.countPaths(ss, ss.top(), ss.bottom()));
        }

        @Test
        void twoBranchOnePath() {
            StateSpace ss = build("&{a: end, b: end}");
            // Only 2 states: top and bottom -- one path in DAG
            assertEquals(1, LgvChecker.countPaths(ss, ss.top(), ss.bottom()));
        }

        @Test
        void diamondPaths() {
            StateSpace ss = build("&{a: &{c: end}, b: &{c: end}}");
            int count = LgvChecker.countPaths(ss, ss.top(), ss.bottom());
            assertTrue(count >= 2, "Expected at least 2 paths through diamond");
        }

        @Test
        void chainPath() {
            StateSpace ss = build("&{a: &{b: end}}");
            assertEquals(1, LgvChecker.countPaths(ss, ss.top(), ss.bottom()));
        }

        @Test
        void threeBranchesOnePath() {
            StateSpace ss = build("&{a: end, b: end, c: end}");
            assertEquals(1, LgvChecker.countPaths(ss, ss.top(), ss.bottom()));
        }

        @Test
        void unreachableReturnsZero() {
            StateSpace ss = build("&{a: end}");
            assertEquals(0, LgvChecker.countPaths(ss, ss.bottom(), ss.top()));
        }

        @Test
        void recursiveTypePathCount() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            int count = LgvChecker.countPaths(ss, ss.top(), ss.bottom());
            assertTrue(count >= 1);
        }
    }

    // -------------------------------------------------------------------
    // Path count matrix
    // -------------------------------------------------------------------

    @Nested
    class PathCountMatrixTests {

        @Test
        void oneByOneEnd() {
            StateSpace ss = build("end");
            int[][] M = LgvChecker.pathCountMatrix(ss,
                    List.of(ss.top()), List.of(ss.bottom()));
            assertArrayEquals(new int[]{1}, M[0]);
        }

        @Test
        void oneByOneBranch() {
            StateSpace ss = build("&{a: end}");
            int[][] M = LgvChecker.pathCountMatrix(ss,
                    List.of(ss.top()), List.of(ss.bottom()));
            assertArrayEquals(new int[]{1}, M[0]);
        }

        @Test
        void dimensions() {
            StateSpace ss = build("&{a: end, b: end}");
            int[][] M = LgvChecker.pathCountMatrix(ss,
                    List.of(ss.top()), List.of(ss.bottom()));
            assertEquals(1, M.length);
            assertEquals(1, M[0].length);
        }

        @Test
        void matrixValuesPositive() {
            StateSpace ss = build("&{a: &{c: end}, b: &{d: end}}");
            int[][] M = LgvChecker.pathCountMatrix(ss,
                    List.of(ss.top()), List.of(ss.bottom()));
            for (int[] row : M) {
                for (int val : row) {
                    assertTrue(val >= 0);
                }
            }
        }
    }

    // -------------------------------------------------------------------
    // Rank layers
    // -------------------------------------------------------------------

    @Nested
    class RankLayerTests {

        @Test
        void endSingleLayer() {
            StateSpace ss = build("end");
            Map<Integer, List<Integer>> layers = LgvChecker.findRankLayers(ss);
            assertEquals(1, layers.size());
            assertTrue(layers.containsKey(0));
        }

        @Test
        void branchTwoLayers() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, List<Integer>> layers = LgvChecker.findRankLayers(ss);
            assertEquals(2, layers.size());
            int maxRank = layers.keySet().stream().mapToInt(i -> i).max().orElse(0);
            assertTrue(layers.get(maxRank).contains(ss.top()));
            assertTrue(layers.get(0).contains(ss.bottom()));
        }

        @Test
        void chainThreeLayers() {
            StateSpace ss = build("&{a: &{b: end}}");
            Map<Integer, List<Integer>> layers = LgvChecker.findRankLayers(ss);
            assertEquals(3, layers.size());
        }

        @Test
        void parallelLayers() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            Map<Integer, List<Integer>> layers = LgvChecker.findRankLayers(ss);
            assertTrue(layers.size() >= 2);
        }

        @Test
        void layersSorted() {
            StateSpace ss = build("&{a: end, b: end, c: end}");
            Map<Integer, List<Integer>> layers = LgvChecker.findRankLayers(ss);
            for (List<Integer> states : layers.values()) {
                List<Integer> sorted = new ArrayList<>(states);
                Collections.sort(sorted);
                assertEquals(sorted, states);
            }
        }
    }

    // -------------------------------------------------------------------
    // LGV verification
    // -------------------------------------------------------------------

    @Nested
    class LgvVerificationTests {

        @Test
        void endLgv() {
            StateSpace ss = build("end");
            assertTrue(LgvChecker.verifyLgv(ss, List.of(ss.top()), List.of(ss.bottom())));
        }

        @Test
        void singleBranchLgv() {
            StateSpace ss = build("&{a: end}");
            assertTrue(LgvChecker.verifyLgv(ss, List.of(ss.top()), List.of(ss.bottom())));
        }

        @Test
        void twoBranchLgv() {
            StateSpace ss = build("&{a: end, b: end}");
            assertTrue(LgvChecker.verifyLgv(ss, List.of(ss.top()), List.of(ss.bottom())));
        }

        @Test
        void chainLgv() {
            StateSpace ss = build("&{a: &{b: end}}");
            assertTrue(LgvChecker.verifyLgv(ss, List.of(ss.top()), List.of(ss.bottom())));
        }

        @Test
        void mismatchedLengthsRaises() {
            StateSpace ss = build("&{a: end}");
            assertThrows(IllegalArgumentException.class,
                    () -> LgvChecker.verifyLgv(ss, List.of(ss.top()), List.of()));
        }

        @Test
        void lgvDeterminantMismatched() {
            StateSpace ss = build("&{a: end}");
            assertThrows(IllegalArgumentException.class,
                    () -> LgvChecker.lgvDeterminant(ss,
                            List.of(ss.top(), ss.bottom()), List.of(ss.bottom())));
        }
    }

    // -------------------------------------------------------------------
    // Non-intersecting count
    // -------------------------------------------------------------------

    @Nested
    class NonIntersectingTests {

        @Test
        void singlePath() {
            StateSpace ss = build("&{a: end}");
            assertEquals(1, LgvChecker.nonIntersectingCount(ss,
                    List.of(ss.top()), List.of(ss.bottom())));
        }

        @Test
        void empty() {
            StateSpace ss = build("end");
            assertEquals(1, LgvChecker.nonIntersectingCount(ss, List.of(), List.of()));
        }

        @Test
        void twoBranchSingleSystem() {
            StateSpace ss = build("&{a: end, b: end}");
            assertEquals(1, LgvChecker.nonIntersectingCount(ss,
                    List.of(ss.top()), List.of(ss.bottom())));
        }
    }

    // -------------------------------------------------------------------
    // Full analysis
    // -------------------------------------------------------------------

    @Nested
    class AnalyzeLgvTests {

        @Test
        void endAnalysis() {
            StateSpace ss = build("end");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.numSources() >= 1);
            assertTrue(r.numSinks() >= 1);
            assertTrue(r.lgvVerified());
        }

        @Test
        void singleBranchAnalysis() {
            StateSpace ss = build("&{a: end}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
            assertTrue(r.determinant() >= 0);
        }

        @Test
        void twoBranchAnalysis() {
            StateSpace ss = build("&{a: end, b: end}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }

        @Test
        void chainAnalysis() {
            StateSpace ss = build("&{a: &{b: end}}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
            assertEquals(1, r.determinant());
        }

        @Test
        void selectionAnalysis() {
            StateSpace ss = build("+{a: end, b: end}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }

        @Test
        void parallelAnalysis() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }

        @Test
        void recursiveAnalysis() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }

        @Test
        void resultFields() {
            StateSpace ss = build("&{a: end}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertNotNull(r.pathMatrix());
            assertNotNull(r.sources());
            assertNotNull(r.sinks());
        }

        @Test
        void pathMatrixDimensions() {
            StateSpace ss = build("&{a: end}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            int n = r.numSources();
            assertEquals(n, r.pathMatrix().length);
            for (int[] row : r.pathMatrix()) {
                assertEquals(r.numSinks(), row.length);
            }
        }
    }

    // -------------------------------------------------------------------
    // Benchmark protocols
    // -------------------------------------------------------------------

    @Nested
    class BenchmarkTests {

        @Test
        void iteratorLgv() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }

        @Test
        void fileObjectLgv() {
            StateSpace ss = build("&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }

        @Test
        void twoBranchProtocolLgv() {
            StateSpace ss = build("&{a: &{c: end}, b: &{d: end}}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }

        @Test
        void nestedSelectionLgv() {
            StateSpace ss = build("&{open: +{OK: &{use: end}, ERR: end}}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }

        @Test
        void deepChainLgv() {
            StateSpace ss = build("&{a: &{b: &{c: &{d: end}}}}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
            assertEquals(1, r.determinant());
        }

        @Test
        void wideBranchLgv() {
            StateSpace ss = build("&{a: end, b: end, c: end, d: end, e: end}");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }

        @Test
        void parallelDeepLgv() {
            StateSpace ss = build("(&{a: &{b: end}} || &{c: end})");
            LgvResult r = LgvChecker.analyzeLgv(ss);
            assertTrue(r.lgvVerified());
        }
    }

    // -------------------------------------------------------------------
    // Parameterized verification across session types
    // -------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{a: end}",
            "&{a: end, b: end}",
            "+{a: end, b: end}",
            "&{a: &{b: end}}",
            "&{a: &{c: end}, b: &{c: end}}",
            "rec X . &{a: X, b: end}",
            "(&{a: end} || &{b: end})",
            "&{open: +{OK: &{use: end}, ERR: end}}",
            "&{a: end, b: end, c: end, d: end, e: end}"
    })
    void lgvHoldsForType(String type) {
        StateSpace ss = build(type);
        LgvResult r = LgvChecker.analyzeLgv(ss);
        assertTrue(r.lgvVerified(), "LGV should hold for: " + type);
    }
}
