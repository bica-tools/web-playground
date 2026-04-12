package com.bica.reborn.snf;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.snf.SmithNormalFormChecker.*;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Smith Normal Form analysis (Step 30ae).
 * Mirrors all 91 Python tests from test_smith_normal_form.py.
 */
class SmithNormalFormCheckerTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    private static boolean isDiagonal(int[][] M) {
        int m = M.length;
        if (m == 0) return true;
        int n = M[0].length;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && M[i][j] != 0) return false;
            }
        }
        return true;
    }

    private static boolean matrixEquals(int[][] A, int[][] B) {
        if (A.length != B.length) return false;
        for (int i = 0; i < A.length; i++) {
            if (!Arrays.equals(A[i], B[i])) return false;
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // TestExtendedGCD
    // -----------------------------------------------------------------------

    @Nested
    class TestExtendedGCD {

        @Test
        void basic() {
            int[] r = SmithNormalFormChecker.extendedGcd(12, 8);
            assertEquals(4, r[0]);
            assertEquals(4, 12 * r[1] + 8 * r[2]);
        }

        @Test
        void coprime() {
            int[] r = SmithNormalFormChecker.extendedGcd(7, 5);
            assertEquals(1, r[0]);
            assertEquals(1, 7 * r[1] + 5 * r[2]);
        }

        @Test
        void zeroSecond() {
            int[] r = SmithNormalFormChecker.extendedGcd(5, 0);
            assertEquals(5, r[0]);
            assertEquals(5, 5 * r[1] + 0 * r[2]);
        }

        @Test
        void bothZero() {
            int[] r = SmithNormalFormChecker.extendedGcd(0, 0);
            assertEquals(0, r[0]);
        }

        @Test
        void negativeFirst() {
            int[] r = SmithNormalFormChecker.extendedGcd(-6, 4);
            assertEquals(2, r[0]);
            assertEquals(2, -6 * r[1] + 4 * r[2]);
        }

        @Test
        void gcdAlwaysPositive() {
            int[] r = SmithNormalFormChecker.extendedGcd(-15, -10);
            assertTrue(r[0] > 0);
        }
    }

    // -----------------------------------------------------------------------
    // TestHelpers
    // -----------------------------------------------------------------------

    @Nested
    class TestHelpers {

        @Test
        void identity3x3() {
            int[][] I = SmithNormalFormChecker.identity(3);
            int[][] expected = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
            assertTrue(matrixEquals(I, expected));
        }

        @Test
        void identity1x1() {
            int[][] I = SmithNormalFormChecker.identity(1);
            assertTrue(matrixEquals(I, new int[][]{{1}}));
        }

        @Test
        void identity0x0() {
            int[][] I = SmithNormalFormChecker.identity(0);
            assertEquals(0, I.length);
        }

        @Test
        void deepCopyIndependent() {
            int[][] M = {{1, 2}, {3, 4}};
            int[][] C = SmithNormalFormChecker.deepCopy(M);
            C[0][0] = 99;
            assertEquals(1, M[0][0]);
        }
    }

    // -----------------------------------------------------------------------
    // TestDeterminant
    // -----------------------------------------------------------------------

    @Nested
    class TestDeterminant {

        @Test
        void det1x1() {
            assertEquals(7, SmithNormalFormChecker.determinant(new int[][]{{7}}));
        }

        @Test
        void det2x2() {
            assertEquals(-2, SmithNormalFormChecker.determinant(new int[][]{{1, 2}, {3, 4}}));
        }

        @Test
        void det3x3Identity() {
            assertEquals(1, SmithNormalFormChecker.determinant(
                    new int[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}));
        }

        @Test
        void detSingular() {
            assertEquals(0, SmithNormalFormChecker.determinant(new int[][]{{1, 2}, {2, 4}}));
        }

        @Test
        void detEmpty() {
            assertEquals(1, SmithNormalFormChecker.determinant(new int[0][0]));
        }

        @Test
        void detNegative() {
            assertEquals(-3, SmithNormalFormChecker.determinant(new int[][]{{-3}}));
        }
    }

    // -----------------------------------------------------------------------
    // TestAdjacencyMatrix
    // -----------------------------------------------------------------------

    @Nested
    class TestAdjacencyMatrix {

        @Test
        void endSingleState() {
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(build("end"));
            assertTrue(matrixEquals(A, new int[][]{{0}}));
        }

        @Test
        void branchTwoStates() {
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(build("&{a: end}"));
            assertEquals(2, A.length);
            assertEquals(2, A[0].length);
            int total = 0;
            for (int[] row : A) for (int v : row) total += v;
            assertEquals(1, total);
        }

        @Test
        void twoBranchSameTarget() {
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(build("&{a: end, b: end}"));
            assertEquals(2, A.length);
            int total = 0;
            for (int[] row : A) for (int v : row) total += v;
            assertEquals(1, total);
        }

        @Test
        void chainThreeStates() {
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(build("&{a: &{b: end}}"));
            assertEquals(3, A.length);
            int total = 0;
            for (int[] row : A) for (int v : row) total += v;
            assertEquals(2, total);
        }

        @Test
        void dimensionsMatchQuotient() {
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(build("&{a: end, b: &{c: end}}"));
            int n = A.length;
            assertTrue(n > 0);
            for (int[] row : A) assertEquals(n, row.length);
        }

        @Test
        void entriesNonnegative() {
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(build("&{a: &{b: end}, c: end}"));
            for (int[] row : A) for (int v : row) assertTrue(v >= 0);
        }

        @Test
        void noSelfLoops() {
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(build("&{a: &{b: end}}"));
            for (int i = 0; i < A.length; i++) assertEquals(0, A[i][i]);
        }

        @Test
        void diamondOutDegree() {
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(build("&{a: &{c: end}, b: &{c: end}}"));
            assertEquals(4, A.length);
            int maxOutDeg = 0;
            for (int[] row : A) {
                int deg = 0;
                for (int v : row) deg += v;
                maxOutDeg = Math.max(maxOutDeg, deg);
            }
            assertEquals(2, maxOutDeg);
        }
    }

    // -----------------------------------------------------------------------
    // TestLaplacianMatrix
    // -----------------------------------------------------------------------

    @Nested
    class TestLaplacianMatrix {

        @Test
        void endZero() {
            int[][] L = SmithNormalFormChecker.laplacianMatrix(build("end"));
            assertTrue(matrixEquals(L, new int[][]{{0}}));
        }

        @Test
        void diagonalEqualsOutDegree() {
            var ss = build("&{a: end, b: end}");
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(ss);
            int[][] L = SmithNormalFormChecker.laplacianMatrix(ss);
            int n = A.length;
            for (int i = 0; i < n; i++) {
                int outDeg = 0;
                for (int j = 0; j < n; j++) outDeg += A[i][j];
                assertEquals(outDeg, L[i][i]);
            }
        }

        @Test
        void offDiagonalNegativeAdjacency() {
            var ss = build("&{a: &{b: end}}");
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(ss);
            int[][] L = SmithNormalFormChecker.laplacianMatrix(ss);
            int n = A.length;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != j) assertEquals(-A[i][j], L[i][j]);
                }
            }
        }

        @Test
        void rowSumsZero() {
            int[][] L = SmithNormalFormChecker.laplacianMatrix(build("&{a: end, b: &{c: end}}"));
            for (int[] row : L) {
                int sum = 0;
                for (int v : row) sum += v;
                assertEquals(0, sum);
            }
        }

        @Test
        void dimensionsMatchAdjacency() {
            var ss = build("&{a: end}");
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(ss);
            int[][] L = SmithNormalFormChecker.laplacianMatrix(ss);
            assertEquals(A.length, L.length);
            assertEquals(A[0].length, L[0].length);
        }

        @Test
        void diagonalNonnegative() {
            int[][] L = SmithNormalFormChecker.laplacianMatrix(build("&{a: end, b: end}"));
            for (int i = 0; i < L.length; i++) assertTrue(L[i][i] >= 0);
        }

        @Test
        void diamondRowSums() {
            int[][] L = SmithNormalFormChecker.laplacianMatrix(build("&{a: &{c: end}, b: &{c: end}}"));
            for (int[] row : L) {
                int sum = 0;
                for (int v : row) sum += v;
                assertEquals(0, sum);
            }
        }
    }

    // -----------------------------------------------------------------------
    // TestIncidenceMatrix
    // -----------------------------------------------------------------------

    @Nested
    class TestIncidenceMatrix {

        @Test
        void endNoEdges() {
            int[][] B = SmithNormalFormChecker.incidenceMatrix(build("end"));
            assertEquals(1, B.length);
            assertEquals(0, B[0].length);
        }

        @Test
        void branchSingleEdge() {
            int[][] B = SmithNormalFormChecker.incidenceMatrix(build("&{a: end}"));
            assertEquals(2, B.length);
            assertEquals(1, B[0].length);
        }

        @Test
        void chainTwoEdges() {
            int[][] B = SmithNormalFormChecker.incidenceMatrix(build("&{a: &{b: end}}"));
            assertEquals(2, B[0].length);
        }

        @Test
        void columnSumsZero() {
            int[][] B = SmithNormalFormChecker.incidenceMatrix(build("&{a: &{b: end}, c: end}"));
            if (B.length > 0 && B[0].length > 0) {
                int nEdges = B[0].length;
                for (int j = 0; j < nEdges; j++) {
                    int colSum = 0;
                    for (int[] row : B) colSum += row[j];
                    assertEquals(0, colSum);
                }
            }
        }

        @Test
        void columnExactlyOnePlusOneMinus() {
            int[][] B = SmithNormalFormChecker.incidenceMatrix(build("&{a: &{b: end}, c: end}"));
            if (B.length > 0 && B[0].length > 0) {
                int nStates = B.length;
                int nEdges = B[0].length;
                for (int j = 0; j < nEdges; j++) {
                    int plus1 = 0, minus1 = 0, zeros = 0;
                    for (int[] row : B) {
                        if (row[j] == 1) plus1++;
                        else if (row[j] == -1) minus1++;
                        else if (row[j] == 0) zeros++;
                    }
                    assertEquals(1, plus1);
                    assertEquals(1, minus1);
                    assertEquals(nStates - 2, zeros);
                }
            }
        }

        @Test
        void dimensionsStatesByEdges() {
            var ss = build("&{a: &{c: end}, b: &{c: end}}");
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(ss);
            int[][] B = SmithNormalFormChecker.incidenceMatrix(ss);
            assertEquals(A.length, B.length);
            int nEdgesAdj = 0;
            for (int[] row : A) for (int v : row) nEdgesAdj += v;
            int nEdgesInc = B[0].length;
            assertEquals(nEdgesAdj, nEdgesInc);
        }
    }

    // -----------------------------------------------------------------------
    // TestSmithNormalForm
    // -----------------------------------------------------------------------

    @Nested
    class TestSmithNormalForm {

        @Test
        void identitySnf() {
            var snf = SmithNormalFormChecker.smithNormalForm(new int[][]{{1, 0}, {0, 1}});
            assertTrue(matrixEquals(snf.d(), new int[][]{{1, 0}, {0, 1}}));
        }

        @Test
        void zeroMatrixSnf() {
            var snf = SmithNormalFormChecker.smithNormalForm(new int[][]{{0, 0}, {0, 0}});
            assertTrue(matrixEquals(snf.d(), new int[][]{{0, 0}, {0, 0}}));
        }

        @Test
        void oneByOnePositive() {
            var snf = SmithNormalFormChecker.smithNormalForm(new int[][]{{5}});
            assertTrue(matrixEquals(snf.d(), new int[][]{{5}}));
        }

        @Test
        void oneByOneNegative() {
            var snf = SmithNormalFormChecker.smithNormalForm(new int[][]{{-3}});
            assertTrue(matrixEquals(snf.d(), new int[][]{{3}}));
        }

        @Test
        void dIsDiagonal2x2() {
            var snf = SmithNormalFormChecker.smithNormalForm(new int[][]{{1, 2}, {3, 4}});
            assertTrue(isDiagonal(snf.d()));
        }

        @Test
        void dIsDiagonal3x3() {
            var snf = SmithNormalFormChecker.smithNormalForm(
                    new int[][]{{2, 4, 4}, {0, 6, 12}, {0, 0, 8}});
            assertTrue(isDiagonal(snf.d()));
        }

        @Test
        void factorization2x2() {
            int[][] A = {{2, 4}, {6, 8}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            assertTrue(matrixEquals(
                    SmithNormalFormChecker.matmul(SmithNormalFormChecker.matmul(snf.u(), A), snf.v()),
                    snf.d()));
        }

        @Test
        void factorization3x3() {
            int[][] A = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            assertTrue(matrixEquals(
                    SmithNormalFormChecker.matmul(SmithNormalFormChecker.matmul(snf.u(), A), snf.v()),
                    snf.d()));
        }

        @Test
        void factorizationRectangular3x2() {
            int[][] A = {{0, -1}, {1, 0}, {-1, 1}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            assertTrue(matrixEquals(
                    SmithNormalFormChecker.matmul(SmithNormalFormChecker.matmul(snf.u(), A), snf.v()),
                    snf.d()));
        }

        @Test
        void factorizationRectangular2x3() {
            int[][] A = {{1, 2, 3}, {4, 5, 6}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            assertTrue(matrixEquals(
                    SmithNormalFormChecker.matmul(SmithNormalFormChecker.matmul(snf.u(), A), snf.v()),
                    snf.d()));
        }

        @Test
        void uUnimodular() {
            int[][] A = {{2, 4}, {6, 8}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            assertEquals(1, Math.abs(SmithNormalFormChecker.determinant(snf.u())));
        }

        @Test
        void vUnimodular() {
            int[][] A = {{2, 4}, {6, 8}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            assertEquals(1, Math.abs(SmithNormalFormChecker.determinant(snf.v())));
        }

        @Test
        void unimodular3x3() {
            int[][] A = {{3, 1, 0}, {1, 2, 1}, {0, 1, 3}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            assertEquals(1, Math.abs(SmithNormalFormChecker.determinant(snf.u())));
            assertEquals(1, Math.abs(SmithNormalFormChecker.determinant(snf.v())));
        }

        @Test
        void diagonalNonnegative() {
            int[][] A = {{3, 6}, {9, 12}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            int n = Math.min(snf.d().length, snf.d()[0].length);
            for (int i = 0; i < n; i++) assertTrue(snf.d()[i][i] >= 0);
        }

        @Test
        void divisibilityChain() {
            int[][] A = {{2, 4}, {6, 8}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            int[][] D = snf.d();
            List<Integer> diag = new ArrayList<>();
            for (int i = 0; i < Math.min(D.length, D[0].length); i++) {
                if (D[i][i] != 0) diag.add(D[i][i]);
            }
            for (int i = 0; i < diag.size() - 1; i++) {
                assertEquals(0, diag.get(i + 1) % diag.get(i));
            }
        }

        @Test
        void knownFactors2And4() {
            assertEquals(List.of(2, 4), SmithNormalFormChecker.invariantFactors(new int[][]{{2, 4}, {6, 8}}));
        }

        @Test
        void emptyMatrix() {
            var snf = SmithNormalFormChecker.smithNormalForm(new int[0][0]);
            assertEquals(0, snf.d().length);
        }

        @Test
        void preservesRank() {
            int[][] A = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
            var snf = SmithNormalFormChecker.smithNormalForm(A);
            int rank = 0;
            for (int i = 0; i < 3; i++) { if (snf.d()[i][i] != 0) rank++; }
            assertEquals(2, rank);
        }

        @Test
        void factorizationVariety() {
            int[][][] matrices = {
                    {{1, 0}, {0, 1}},
                    {{2, 3}, {4, 5}},
                    {{6, 0, 0}, {0, 3, 0}, {0, 0, 15}},
                    {{1, 2}, {3, 4}, {5, 6}},
            };
            for (int[][] M : matrices) {
                var snf = SmithNormalFormChecker.smithNormalForm(M);
                assertTrue(matrixEquals(
                        SmithNormalFormChecker.matmul(SmithNormalFormChecker.matmul(snf.u(), M), snf.v()),
                        snf.d()), "Failed for matrix");
            }
        }

        @Test
        void diagonalCoprimeBecomes1AndProduct() {
            var snf = SmithNormalFormChecker.smithNormalForm(new int[][]{{2, 0}, {0, 3}});
            List<Integer> diag = new ArrayList<>();
            for (int i = 0; i < 2; i++) diag.add(snf.d()[i][i]);
            Collections.sort(diag);
            assertEquals(List.of(1, 6), diag);
        }
    }

    // -----------------------------------------------------------------------
    // TestInvariantFactors
    // -----------------------------------------------------------------------

    @Nested
    class TestInvariantFactors {

        @Test
        void identityFactors() {
            assertEquals(List.of(1, 1),
                    SmithNormalFormChecker.invariantFactors(new int[][]{{1, 0}, {0, 1}}));
        }

        @Test
        void zeroMatrixNoFactors() {
            assertEquals(List.of(),
                    SmithNormalFormChecker.invariantFactors(new int[][]{{0, 0}, {0, 0}}));
        }

        @Test
        void factorsPositive() {
            for (int f : SmithNormalFormChecker.invariantFactors(new int[][]{{2, 4}, {6, 8}})) {
                assertTrue(f > 0);
            }
        }

        @Test
        void factorsDivisibility() {
            var factors = SmithNormalFormChecker.invariantFactors(new int[][]{{3, 6}, {9, 12}});
            for (int i = 0; i < factors.size() - 1; i++) {
                assertEquals(0, factors.get(i + 1) % factors.get(i));
            }
        }

        @Test
        void countEqualsRank() {
            assertEquals(2, SmithNormalFormChecker.invariantFactors(new int[][]{{1, 2}, {3, 4}}).size());
        }

        @Test
        void emptyMatrix() {
            assertEquals(List.of(), SmithNormalFormChecker.invariantFactors(new int[0][0]));
        }

        @Test
        void scalar() {
            assertEquals(List.of(7), SmithNormalFormChecker.invariantFactors(new int[][]{{7}}));
        }

        @Test
        void productEqualsAbsDet() {
            int[][] A = {{2, 4}, {6, 8}};
            var factors = SmithNormalFormChecker.invariantFactors(A);
            int product = 1;
            for (int f : factors) product *= f;
            int det = Math.abs(A[0][0] * A[1][1] - A[0][1] * A[1][0]);
            assertEquals(det, product);
        }

        @Test
        void threeByThreeDiagonalFactors() {
            var factors = SmithNormalFormChecker.invariantFactors(
                    new int[][]{{6, 0, 0}, {0, 12, 0}, {0, 0, 60}});
            for (int i = 0; i < factors.size() - 1; i++) {
                assertEquals(0, factors.get(i + 1) % factors.get(i));
            }
        }
    }

    // -----------------------------------------------------------------------
    // TestCriticalGroup
    // -----------------------------------------------------------------------

    @Nested
    class TestCriticalGroup {

        @Test
        void endTrivial() {
            assertEquals(List.of(), SmithNormalFormChecker.criticalGroup(build("end")));
        }

        @Test
        void branchTrivial() {
            assertEquals(List.of(), SmithNormalFormChecker.criticalGroup(build("&{a: end}")));
        }

        @Test
        void chainTrivial() {
            assertEquals(List.of(), SmithNormalFormChecker.criticalGroup(build("&{a: &{b: end}}")));
        }

        @Test
        void diamondNontrivial() {
            assertEquals(List.of(2),
                    SmithNormalFormChecker.criticalGroup(build("&{a: &{c: end}, b: &{c: end}}")));
        }

        @Test
        void parallelNontrivial() {
            var cg = SmithNormalFormChecker.criticalGroup(build("(&{a: end} || &{b: end})"));
            assertFalse(cg.isEmpty());
        }

        @Test
        void groupOrderPositive() {
            for (String s : List.of("end", "&{a: end}", "&{a: &{b: end}}", "(&{a: end} || &{b: end})")) {
                assertTrue(SmithNormalFormChecker.criticalGroupOrder(build(s)) > 0);
            }
        }

        @Test
        void orderIsProductOfFactors() {
            var ss = build("&{a: &{c: end}, b: &{c: end}}");
            var cg = SmithNormalFormChecker.criticalGroup(ss);
            int order = SmithNormalFormChecker.criticalGroupOrder(ss);
            if (!cg.isEmpty()) {
                int product = 1;
                for (int f : cg) product *= f;
                assertEquals(product, order);
            }
        }

        @Test
        void treeOrderOne() {
            for (String s : List.of("end", "&{a: end}", "&{a: &{b: end}}", "&{a: &{b: &{c: end}}}")) {
                assertEquals(1, SmithNormalFormChecker.criticalGroupOrder(build(s)));
            }
        }
    }

    // -----------------------------------------------------------------------
    // TestAnalyze
    // -----------------------------------------------------------------------

    @Nested
    class TestAnalyze {

        @Test
        void endAnalysis() {
            var r = SmithNormalFormChecker.analyze(build("end"));
            assertNotNull(r);
            assertEquals(List.of(), r.adjacencySnf());
            assertEquals(0, r.matrixRank());
            assertEquals(1, r.nullity());
        }

        @Test
        void branchAnalysis() {
            var r = SmithNormalFormChecker.analyze(build("&{a: end}"));
            assertEquals(List.of(1), r.adjacencySnf());
            assertEquals(1, r.matrixRank());
            assertEquals(1, r.nullity());
        }

        @Test
        void chainAnalysis() {
            var r = SmithNormalFormChecker.analyze(build("&{a: &{b: end}}"));
            assertEquals(List.of(1, 1), r.adjacencySnf());
            assertEquals(2, r.matrixRank());
            assertEquals(1, r.nullity());
            assertEquals(List.of(), r.criticalGroup());
            assertEquals(1, r.criticalGroupOrder());
        }

        @Test
        void parallelAnalysis() {
            var r = SmithNormalFormChecker.analyze(build("(&{a: end} || &{b: end})"));
            assertTrue(r.matrixRank() > 0);
            assertTrue(r.criticalGroupOrder() > 0);
            assertFalse(r.laplacianSnf().isEmpty());
        }

        @Test
        void diamondAnalysis() {
            var r = SmithNormalFormChecker.analyze(build("&{a: &{c: end}, b: &{c: end}}"));
            assertEquals(List.of(2), r.criticalGroup());
            assertEquals(2, r.criticalGroupOrder());
        }

        @Test
        void allFieldsPopulated() {
            var r = SmithNormalFormChecker.analyze(build("&{a: end}"));
            assertNotNull(r.adjacencySnf());
            assertNotNull(r.laplacianSnf());
            assertNotNull(r.incidenceSnf());
            assertNotNull(r.criticalGroup());
        }

        @Test
        void rankPlusNullityEqualsN() {
            for (String s : List.of("end", "&{a: end}", "&{a: &{b: end}}", "&{a: end, b: end}")) {
                var ss = build(s);
                var r = SmithNormalFormChecker.analyze(ss);
                int n = SmithNormalFormChecker.adjacencyMatrix(ss).length;
                assertEquals(n, r.matrixRank() + r.nullity());
            }
        }

        @Test
        void selectionAnalysis() {
            var r = SmithNormalFormChecker.analyze(build("+{a: end, b: end}"));
            assertNotNull(r.adjacencySnf());
            assertNotNull(r.laplacianSnf());
        }

        @Test
        void recursiveType() {
            var r = SmithNormalFormChecker.analyze(build("rec X . &{a: X, b: end}"));
            assertNotNull(r);
            assertTrue(r.matrixRank() >= 0);
        }

        @Test
        void invariantFactorsDivisibility() {
            var r = SmithNormalFormChecker.analyze(build("&{a: &{b: end}, c: end}"));
            for (var factors : List.of(r.adjacencySnf(), r.laplacianSnf(), r.incidenceSnf())) {
                for (int i = 0; i < factors.size() - 1; i++) {
                    if (factors.get(i) != 0) {
                        assertEquals(0, factors.get(i + 1) % factors.get(i));
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // TestBenchmarks
    // -----------------------------------------------------------------------

    @Nested
    class TestBenchmarks {

        @ParameterizedTest
        @CsvSource({
                "end,end",
                "branch,&{a: end}",
                "two-branch,&{a: end; b: end}",
                "selection,+{ok: end; err: end}",
                "mixed-depth,&{a: end; b: &{c: end}}",
                "open-select,&{open: +{ok: &{read: end}; err: end}}",
                "simple-rec,rec X . &{next: X; stop: end}",
                "iterator,rec X . &{hasNext: +{TRUE: &{next: X}; FALSE: end}}",
                "parallel-basic,(&{a: end} || &{b: end})",
                "parallel-multi,(&{a: end; b: end} || &{c: end})",
                "file-object,&{open: rec X . &{read: +{data: X; eof: &{close: end}}}}",
        })
        void analysisValid(String name, String typeStr) {
            // CsvSource uses ; as separator within type strings to avoid CSV issues
            typeStr = typeStr.replace(';', ',');
            var ss = build(typeStr);
            var r = SmithNormalFormChecker.analyze(ss);
            assertNotNull(r);
            int n = SmithNormalFormChecker.adjacencyMatrix(ss).length;
            assertEquals(n, r.matrixRank() + r.nullity());
            assertTrue(r.criticalGroupOrder() >= 1);
        }

        @ParameterizedTest
        @CsvSource({
                "end,end",
                "branch,&{a: end}",
                "two-branch,&{a: end; b: end}",
                "selection,+{ok: end; err: end}",
                "mixed-depth,&{a: end; b: &{c: end}}",
                "open-select,&{open: +{ok: &{read: end}; err: end}}",
                "simple-rec,rec X . &{next: X; stop: end}",
                "iterator,rec X . &{hasNext: +{TRUE: &{next: X}; FALSE: end}}",
                "parallel-basic,(&{a: end} || &{b: end})",
                "parallel-multi,(&{a: end; b: end} || &{c: end})",
                "file-object,&{open: rec X . &{read: +{data: X; eof: &{close: end}}}}",
        })
        void laplacianRowSums(String name, String typeStr) {
            typeStr = typeStr.replace(';', ',');
            int[][] L = SmithNormalFormChecker.laplacianMatrix(build(typeStr));
            for (int[] row : L) {
                int sum = 0;
                for (int v : row) sum += v;
                assertEquals(0, sum);
            }
        }

        @ParameterizedTest
        @CsvSource({
                "end,end",
                "branch,&{a: end}",
                "two-branch,&{a: end; b: end}",
                "selection,+{ok: end; err: end}",
                "mixed-depth,&{a: end; b: &{c: end}}",
                "open-select,&{open: +{ok: &{read: end}; err: end}}",
                "simple-rec,rec X . &{next: X; stop: end}",
                "iterator,rec X . &{hasNext: +{TRUE: &{next: X}; FALSE: end}}",
                "parallel-basic,(&{a: end} || &{b: end})",
                "parallel-multi,(&{a: end; b: end} || &{c: end})",
                "file-object,&{open: rec X . &{read: +{data: X; eof: &{close: end}}}}",
        })
        void incidenceColSums(String name, String typeStr) {
            typeStr = typeStr.replace(';', ',');
            int[][] B = SmithNormalFormChecker.incidenceMatrix(build(typeStr));
            if (B.length > 0 && B[0].length > 0) {
                for (int j = 0; j < B[0].length; j++) {
                    int colSum = 0;
                    for (int[] row : B) colSum += row[j];
                    assertEquals(0, colSum);
                }
            }
        }

        @ParameterizedTest
        @CsvSource({
                "end,end",
                "branch,&{a: end}",
                "two-branch,&{a: end; b: end}",
                "selection,+{ok: end; err: end}",
                "mixed-depth,&{a: end; b: &{c: end}}",
                "open-select,&{open: +{ok: &{read: end}; err: end}}",
                "simple-rec,rec X . &{next: X; stop: end}",
                "iterator,rec X . &{hasNext: +{TRUE: &{next: X}; FALSE: end}}",
                "parallel-basic,(&{a: end} || &{b: end})",
                "parallel-multi,(&{a: end; b: end} || &{c: end})",
                "file-object,&{open: rec X . &{read: +{data: X; eof: &{close: end}}}}",
        })
        void adjacencySnfFactorization(String name, String typeStr) {
            typeStr = typeStr.replace(';', ',');
            var ss = build(typeStr);
            int[][] A = SmithNormalFormChecker.adjacencyMatrix(ss);
            if (A.length > 0 && A[0].length > 0) {
                var snf = SmithNormalFormChecker.smithNormalForm(A);
                assertTrue(matrixEquals(
                        SmithNormalFormChecker.matmul(SmithNormalFormChecker.matmul(snf.u(), A), snf.v()),
                        snf.d()));
            }
        }

        @Test
        void parallelChainCriticalGroup() {
            var r = SmithNormalFormChecker.analyze(build("(&{a: &{b: end}} || &{c: end})"));
            assertTrue(r.criticalGroupOrder() >= 1);
            assertTrue(r.matrixRank() >= 2);
        }

        @Test
        void nestedParallel() {
            var r = SmithNormalFormChecker.analyze(build("((&{a: end} || &{b: end}) || &{c: end})"));
            assertTrue(r.matrixRank() >= 1);
            assertTrue(r.criticalGroupOrder() >= 1);
        }
    }
}
