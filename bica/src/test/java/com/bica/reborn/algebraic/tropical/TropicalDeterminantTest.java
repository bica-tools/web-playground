package com.bica.reborn.algebraic.tropical;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bica.reborn.algebraic.tropical.TropicalDeterminant.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TropicalDeterminant}: port of Python
 * {@code tests/test_tropical_determinant.py} (Step 30m).
 */
class TropicalDeterminantTest {

    private static final double EPS = 1e-12;

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // =======================================================================
    // TestTropicalDet
    // =======================================================================

    @Nested
    class TestTropicalDet {

        @Test
        void emptyMatrix() {
            assertEquals(0.0, tropicalDet(new double[0][0]));
        }

        @Test
        void oneByOne() {
            assertEquals(5.0, tropicalDet(new double[][]{{5.0}}));
        }

        @Test
        void oneByOneNegInf() {
            assertEquals(NEG_INF, tropicalDet(new double[][]{{NEG_INF}}));
        }

        @Test
        void twoByTwoIdentity() {
            double[][] I = {{0.0, NEG_INF}, {NEG_INF, 0.0}};
            assertEquals(0.0, tropicalDet(I));
        }

        @Test
        void twoByTwoAllOnes() {
            double[][] A = {{1.0, 1.0}, {1.0, 1.0}};
            assertEquals(2.0, tropicalDet(A));
        }

        @Test
        void twoByTwoKnown() {
            double[][] A = {{3.0, 1.0}, {2.0, 4.0}};
            assertEquals(7.0, tropicalDet(A));
        }

        @Test
        void twoByTwoAntiDiagonal() {
            double[][] A = {{NEG_INF, 5.0}, {3.0, NEG_INF}};
            assertEquals(8.0, tropicalDet(A));
        }

        @Test
        void threeByThreeIdentity() {
            double[][] I = {
                    {0.0, NEG_INF, NEG_INF},
                    {NEG_INF, 0.0, NEG_INF},
                    {NEG_INF, NEG_INF, 0.0}};
            assertEquals(0.0, tropicalDet(I));
        }

        @Test
        void threeByThreeExample() {
            double[][] A = {
                    {1.0, 2.0, 3.0},
                    {4.0, 5.0, 6.0},
                    {7.0, 8.0, 9.0}};
            assertEquals(15.0, tropicalDet(A));
        }

        @Test
        void noFeasiblePermutation() {
            double[][] A = {{1.0, NEG_INF}, {NEG_INF, NEG_INF}};
            assertEquals(NEG_INF, tropicalDet(A));
        }

        @Test
        void fromStatespaceEnd() {
            double d = tropicalDetFromStateSpace(build("end"));
            assertTrue(Double.isFinite(d) || d == NEG_INF);
        }

        @Test
        void fromStatespaceBranch() {
            double d = tropicalDetFromStateSpace(build("&{a: end, b: end}"));
            assertTrue(Double.isFinite(d) || d == NEG_INF);
        }
    }

    // =======================================================================
    // TestOptimalPermutations
    // =======================================================================

    @Nested
    class TestOptimalPermutations {

        @Test
        void empty() {
            List<int[]> opts = optimalPermutations(new double[0][0]);
            assertEquals(1, opts.size());
            assertEquals(0, opts.get(0).length);
        }

        @Test
        void oneByOne() {
            List<int[]> opts = optimalPermutations(new double[][]{{7.0}});
            assertEquals(1, opts.size());
            assertArrayEquals(new int[]{0}, opts.get(0));
        }

        @Test
        void identityUnique() {
            double[][] I = {{0.0, NEG_INF}, {NEG_INF, 0.0}};
            List<int[]> opts = optimalPermutations(I);
            assertEquals(1, opts.size());
            assertArrayEquals(new int[]{0, 1}, opts.get(0));
        }

        @Test
        void antiDiagonalUnique() {
            double[][] A = {{NEG_INF, 5.0}, {3.0, NEG_INF}};
            List<int[]> opts = optimalPermutations(A);
            assertEquals(1, opts.size());
            assertArrayEquals(new int[]{1, 0}, opts.get(0));
        }

        @Test
        void multipleOptimal() {
            double[][] A = {{1.0, 1.0}, {1.0, 1.0}};
            assertEquals(2, optimalPermutations(A).size());
        }

        @Test
        void threeByThreeAllSameSum() {
            double[][] A = {
                    {1.0, 2.0, 3.0},
                    {4.0, 5.0, 6.0},
                    {7.0, 8.0, 9.0}};
            assertEquals(6, optimalPermutations(A).size());
        }

        @Test
        void noFeasible() {
            double[][] A = {{NEG_INF, NEG_INF}, {NEG_INF, NEG_INF}};
            assertEquals(0, optimalPermutations(A).size());
        }

        @Test
        void uniqueOptimal3x3() {
            double[][] A = {
                    {10.0, 0.0, 0.0},
                    {0.0, 10.0, 0.0},
                    {0.0, 0.0, 10.0}};
            List<int[]> opts = optimalPermutations(A);
            assertEquals(1, opts.size());
            assertArrayEquals(new int[]{0, 1, 2}, opts.get(0));
        }
    }

    // =======================================================================
    // TestSingularity
    // =======================================================================

    @Nested
    class TestSingularity {

        @Test
        void identityNotSingular() {
            double[][] I = {{0.0, NEG_INF}, {NEG_INF, 0.0}};
            assertFalse(isTropicallySingular(I));
        }

        @Test
        void allOnesSingular() {
            double[][] A = {{1.0, 1.0}, {1.0, 1.0}};
            assertTrue(isTropicallySingular(A));
        }

        @Test
        void strongDiagonalNotSingular() {
            double[][] A = {{10.0, 1.0}, {1.0, 10.0}};
            assertFalse(isTropicallySingular(A));
        }

        @Test
        void equalDiagAntiDiagSingular() {
            double[][] A = {{5.0, 5.0}, {5.0, 5.0}};
            assertTrue(isTropicallySingular(A));
        }

        @Test
        void noFeasibleSingular() {
            double[][] A = {{NEG_INF, NEG_INF}, {NEG_INF, NEG_INF}};
            assertTrue(isTropicallySingular(A));
        }

        @Test
        void threeByThreeRegular() {
            double[][] A = {
                    {10.0, 0.0, 0.0},
                    {0.0, 10.0, 0.0},
                    {0.0, 0.0, 10.0}};
            assertFalse(isTropicallySingular(A));
        }

        @Test
        void threeByThreeSingular() {
            double[][] A = {
                    {1.0, 2.0, 3.0},
                    {4.0, 5.0, 6.0},
                    {7.0, 8.0, 9.0}};
            assertTrue(isTropicallySingular(A));
        }
    }

    // =======================================================================
    // TestMinors
    // =======================================================================

    @Nested
    class TestMinors {

        @Test
        void minor2x2() {
            double[][] A = {{3.0, 1.0}, {2.0, 4.0}};
            assertEquals(4.0, tropicalMinor(A, 0, 0));
            assertEquals(2.0, tropicalMinor(A, 0, 1));
            assertEquals(1.0, tropicalMinor(A, 1, 0));
            assertEquals(3.0, tropicalMinor(A, 1, 1));
        }

        @Test
        void minor3x3() {
            double[][] A = {
                    {10.0, 0.0, 0.0},
                    {0.0, 10.0, 0.0},
                    {0.0, 0.0, 10.0}};
            assertEquals(20.0, tropicalMinor(A, 0, 0));
        }

        @Test
        void allMinors1x1() {
            List<Double> m1 = allTropicalMinors(new double[][]{{5.0}}, 1);
            assertEquals(1, m1.size());
            assertEquals(5.0, m1.get(0));
        }

        @Test
        void allMinors2x2() {
            double[][] A = {{3.0, 1.0}, {2.0, 4.0}};
            List<Double> m1 = allTropicalMinors(A, 1);
            assertEquals(4, m1.size());
            List<Double> sorted = new java.util.ArrayList<>(m1);
            java.util.Collections.sort(sorted);
            assertEquals(List.of(1.0, 2.0, 3.0, 4.0), sorted);
        }

        @Test
        void allMinorsInvalidK() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            assertTrue(allTropicalMinors(A, 0).isEmpty());
            assertTrue(allTropicalMinors(A, 3).isEmpty());
        }

        @Test
        void allMinorsFull() {
            double[][] A = {{3.0, 1.0}, {2.0, 4.0}};
            List<Double> m2 = allTropicalMinors(A, 2);
            assertEquals(1, m2.size());
            assertEquals(tropicalDet(A), m2.get(0));
        }
    }

    // =======================================================================
    // TestAdjugate
    // =======================================================================

    @Nested
    class TestAdjugate {

        @Test
        void empty() {
            assertEquals(0, tropicalAdjugate(new double[0][0]).length);
        }

        @Test
        void oneByOne() {
            double[][] adj = tropicalAdjugate(new double[][]{{7.0}});
            assertArrayEquals(new double[]{0.0}, adj[0]);
        }

        @Test
        void twoByTwo() {
            double[][] A = {{3.0, 1.0}, {2.0, 4.0}};
            double[][] adj = tropicalAdjugate(A);
            assertEquals(4.0, adj[0][0]);
            assertEquals(1.0, adj[0][1]);
            assertEquals(2.0, adj[1][0]);
            assertEquals(3.0, adj[1][1]);
        }

        @Test
        void adjugateIdentity() {
            double[][] I = {{0.0, NEG_INF}, {NEG_INF, 0.0}};
            double[][] adj = tropicalAdjugate(I);
            assertEquals(0.0, adj[0][0]);
            assertEquals(0.0, adj[1][1]);
        }
    }

    // =======================================================================
    // TestTropicalRank
    // =======================================================================

    @Nested
    class TestTropicalRank {

        @Test
        void empty() {
            assertEquals(0, tropicalRank(new double[0][0]));
        }

        @Test
        void oneByOneFinite() {
            assertEquals(1, tropicalRank(new double[][]{{5.0}}));
        }

        @Test
        void oneByOneNegInf() {
            assertEquals(0, tropicalRank(new double[][]{{NEG_INF}}));
        }

        @Test
        void twoByTwoFullRank() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            assertEquals(2, tropicalRank(A));
        }

        @Test
        void twoByTwoRank1() {
            double[][] A = {{1.0, NEG_INF}, {NEG_INF, NEG_INF}};
            assertEquals(1, tropicalRank(A));
        }

        @Test
        void twoByTwoRank0() {
            double[][] A = {{NEG_INF, NEG_INF}, {NEG_INF, NEG_INF}};
            assertEquals(0, tropicalRank(A));
        }

        @Test
        void identityFullRank() {
            double[][] I = {
                    {0.0, NEG_INF, NEG_INF},
                    {NEG_INF, 0.0, NEG_INF},
                    {NEG_INF, NEG_INF, 0.0}};
            assertEquals(3, tropicalRank(I));
        }
    }

    // =======================================================================
    // TestHungarian
    // =======================================================================

    @Nested
    class TestHungarian {

        @Test
        void empty() {
            Assignment a = hungarianAssignment(new double[0][0]);
            assertEquals(0.0, a.weight());
            assertEquals(0, a.assignment().length);
        }

        @Test
        void oneByOne() {
            Assignment a = hungarianAssignment(new double[][]{{7.0}});
            assertEquals(7.0, a.weight());
            assertArrayEquals(new int[]{0}, a.assignment());
        }

        @Test
        void twoByTwoIdentity() {
            double[][] I = {{0.0, NEG_INF}, {NEG_INF, 0.0}};
            Assignment a = hungarianAssignment(I);
            assertEquals(0.0, a.weight());
            assertArrayEquals(new int[]{0, 1}, a.assignment());
        }

        @Test
        void twoByTwoKnown() {
            double[][] A = {{3.0, 1.0}, {2.0, 4.0}};
            Assignment a = hungarianAssignment(A);
            assertEquals(7.0, a.weight());
            assertEquals(0, a.assignment()[0]);
            assertEquals(1, a.assignment()[1]);
        }

        @Test
        void twoByTwoAntiDiagonal() {
            double[][] A = {{NEG_INF, 5.0}, {3.0, NEG_INF}};
            Assignment a = hungarianAssignment(A);
            assertEquals(8.0, a.weight());
            assertArrayEquals(new int[]{1, 0}, a.assignment());
        }

        @Test
        void threeByThreeStrongDiagonal() {
            double[][] A = {
                    {10.0, 1.0, 1.0},
                    {1.0, 10.0, 1.0},
                    {1.0, 1.0, 10.0}};
            Assignment a = hungarianAssignment(A);
            assertEquals(30.0, a.weight());
            assertArrayEquals(new int[]{0, 1, 2}, a.assignment());
        }

        @Test
        void infeasible() {
            double[][] A = {{NEG_INF, NEG_INF}, {NEG_INF, NEG_INF}};
            Assignment a = hungarianAssignment(A);
            assertEquals(NEG_INF, a.weight());
        }

        @Test
        void agreesWithBruteForce() {
            double[][] A = {
                    {2.0, 5.0, 3.0},
                    {4.0, 1.0, 6.0},
                    {7.0, 3.0, 2.0}};
            Assignment a = hungarianAssignment(A);
            double det = tropicalDet(A);
            assertEquals(det, a.weight(), EPS);
        }
    }

    // =======================================================================
    // TestCramer
    // =======================================================================

    @Nested
    class TestCramer {

        @Test
        void empty() {
            double[] x = tropicalCramer(new double[0][0], new double[0]);
            assertNotNull(x);
            assertEquals(0, x.length);
        }

        @Test
        void dimensionMismatch() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            assertNull(tropicalCramer(A, new double[]{1.0}));
        }

        @Test
        void infeasible() {
            double[][] A = {{NEG_INF, NEG_INF}, {NEG_INF, NEG_INF}};
            assertNull(tropicalCramer(A, new double[]{1.0, 2.0}));
        }

        @Test
        void identitySystem() {
            double[][] I = {{0.0, NEG_INF}, {NEG_INF, 0.0}};
            double[] x = tropicalCramer(I, new double[]{3.0, 5.0});
            assertNotNull(x);
            assertEquals(2, x.length);
            assertEquals(3.0, x[0], EPS);
            assertEquals(5.0, x[1], EPS);
        }

        @Test
        void returnsList() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            double[] x = tropicalCramer(A, new double[]{5.0, 6.0});
            assertNotNull(x);
            assertEquals(2, x.length);
        }
    }

    // =======================================================================
    // TestAnalyze
    // =======================================================================

    @Nested
    class TestAnalyze {

        @Test
        void end() {
            TropicalDetResult r = analyzeTropicalDeterminant(build("end"));
            assertNotNull(r);
            assertTrue(r.tropicalRank() >= 0);
        }

        @Test
        void simpleBranch() {
            TropicalDetResult r = analyzeTropicalDeterminant(build("&{a: end, b: end}"));
            assertTrue(r.tropicalRank() >= 0);
            assertEquals(r.determinant(), r.assignmentWeight());
        }

        @Test
        void selection() {
            TropicalDetResult r = analyzeTropicalDeterminant(build("+{x: end, y: end}"));
            assertEquals(r.numOptimal(), r.optimalPermutations().size());
        }

        @Test
        void nestedBranch() {
            TropicalDetResult r = analyzeTropicalDeterminant(
                    build("&{a: &{c: end, d: end}, b: end}"));
            assertTrue(r.tropicalRank() >= 1);
            assertNotNull(r.adjugate());
        }

        @Test
        void parallel() {
            TropicalDetResult r = analyzeTropicalDeterminant(build("(&{a: end} || &{b: end})"));
            assertNotNull(r);
        }

        @Test
        void resultConsistency() {
            for (String ty : List.of("end", "&{a: end}", "+{x: end, y: end}")) {
                TropicalDetResult r = analyzeTropicalDeterminant(build(ty));
                assertEquals(r.determinant(), r.assignmentWeight());
            }
        }

        @Test
        void optimalPermsCountMatches() {
            TropicalDetResult r = analyzeTropicalDeterminant(build("&{a: end, b: end}"));
            assertEquals(r.numOptimal(), r.optimalPermutations().size());
        }
    }

    // =======================================================================
    // TestBenchmarks
    // =======================================================================

    @Nested
    class TestBenchmarks {

        @Test
        void iterator() {
            TropicalDetResult r = analyzeTropicalDeterminant(
                    build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertNotNull(r);
            assertTrue(r.tropicalRank() >= 1);
        }

        @Test
        void fileObject() {
            TropicalDetResult r = analyzeTropicalDeterminant(
                    build("&{open: +{OK: &{read: end, close: end}, ERROR: end}}"));
            assertNotNull(r);
        }

        @Test
        void simpleParallel() {
            TropicalDetResult r = analyzeTropicalDeterminant(build("(&{a: end} || &{b: end})"));
            assertTrue(r.tropicalRank() >= 1);
        }

        @Test
        void deepBranch() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            TropicalDetResult r = analyzeTropicalDeterminant(ss);
            assertTrue(r.tropicalRank() >= 1);
            double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
            assertEquals(4, A.length);
        }

        @Test
        void wideBranch() {
            StateSpace ss = build("&{a: end, b: end, c: end, d: end}");
            TropicalDetResult r = analyzeTropicalDeterminant(ss);
            assertNotNull(r.adjugate());
            assertEquals(Tropical.maxPlusAdjacencyMatrix(ss).length, r.adjugate().length);
        }

        @Test
        void selectionBranchMix() {
            TropicalDetResult r = analyzeTropicalDeterminant(
                    build("+{x: &{a: end, b: end}, y: end}"));
            assertNotNull(r);
        }
    }

    // =======================================================================
    // TestEdgeCases
    // =======================================================================

    @Nested
    class TestEdgeCases {

        @Test
        void detEqualsHungarianSmall() {
            double[][][] matrices = {
                    {{1.0, 2.0}, {3.0, 4.0}},
                    {{0.0, NEG_INF}, {NEG_INF, 0.0}},
                    {{5.0, 5.0}, {5.0, 5.0}}};
            for (double[][] A : matrices) {
                double det = tropicalDet(A);
                double w = hungarianAssignment(A).weight();
                if (det != NEG_INF) {
                    assertEquals(det, w, EPS);
                }
            }
        }

        @Test
        void rankLeqN() {
            double[][] A = {
                    {1.0, 2.0, 3.0},
                    {4.0, 5.0, 6.0},
                    {7.0, 8.0, 9.0}};
            int r = tropicalRank(A);
            assertTrue(0 <= r && r <= 3);
        }

        @Test
        void adjugateSize() {
            double[][] A = {
                    {1.0, 2.0, 3.0},
                    {4.0, 5.0, 6.0},
                    {7.0, 8.0, 9.0}};
            double[][] adj = tropicalAdjugate(A);
            assertEquals(3, adj.length);
            for (double[] row : adj) assertEquals(3, row.length);
        }

        @Test
        void minorReducesSize() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            double m = tropicalMinor(A, 0, 0);
            assertEquals(4.0, m);
        }

        @Test
        void singularIffMultipleOptimal() {
            assertFalse(isTropicallySingular(new double[][]{{0.0, NEG_INF}, {NEG_INF, 0.0}}));
            assertTrue(isTropicallySingular(new double[][]{{1.0, 1.0}, {1.0, 1.0}}));
        }
    }
}
