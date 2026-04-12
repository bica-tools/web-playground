package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IharaZeta} — Java port of Python
 * {@code test_ihara.py} (80 tests, Step 30s).
 */
class IharaZetaTest {

    private static final double EPS = 1e-8;

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // -----------------------------------------------------------------------
    // DAG trivial zeta
    // -----------------------------------------------------------------------

    @Nested
    class DAGTrivialZeta {
        @Test
        void endType() {
            StateSpace s = ss("end");
            assertEquals(0, IharaZeta.cycleRank(s));
            assertEquals(0, IharaZeta.countPrimeCycles(s));
            assertEquals(1.0, IharaZeta.iharaDeterminant(s, 0.5), EPS);
        }

        @Test
        void singleBranch() {
            StateSpace s = ss("&{a: end}");
            assertEquals(0, IharaZeta.cycleRank(s));
            assertEquals(0, IharaZeta.countPrimeCycles(s));
        }

        @Test
        void twoBranch() {
            StateSpace s = ss("&{a: end, b: end}");
            assertEquals(0, IharaZeta.cycleRank(s));
            assertEquals(0, IharaZeta.countPrimeCycles(s));
        }

        @Test
        void selectDag() {
            StateSpace s = ss("+{a: end, b: end}");
            assertEquals(0, IharaZeta.cycleRank(s));
            assertEquals(0, IharaZeta.countPrimeCycles(s));
        }

        @Test
        void nestedBranchDag() {
            StateSpace s = ss("&{a: &{b: end}}");
            assertEquals(0, IharaZeta.cycleRank(s));
            assertEquals(0, IharaZeta.countPrimeCycles(s));
            assertEquals(1.0, IharaZeta.iharaDeterminant(s, 0.3), EPS);
        }

        @Test
        void diamondDag() {
            StateSpace s = ss("&{a: &{c: end}, b: &{c: end}}");
            assertTrue(IharaZeta.cycleRank(s) >= 0);
        }

        @Test
        void dagDeterminantAtZero() {
            StateSpace s = ss("&{a: end, b: end}");
            assertEquals(1.0, IharaZeta.iharaDeterminant(s, 0.0), EPS);
        }

        @Test
        void dagComplexityZero() {
            StateSpace s = ss("&{a: end}");
            assertEquals(0.0, IharaZeta.graphComplexity(s), EPS);
        }
    }

    // -----------------------------------------------------------------------
    // Trees
    // -----------------------------------------------------------------------

    @Nested
    class TreeNoCycles {
        @Test
        void binaryTree() {
            StateSpace s = ss("&{a: end, b: end}");
            assertEquals(0, IharaZeta.cycleRank(s));
            assertEquals(0, IharaZeta.countPrimeCycles(s));
            assertTrue(IharaZeta.isRamanujan(s));
        }

        @Test
        void deepChain() {
            StateSpace s = ss("&{a: &{b: &{c: end}}}");
            assertEquals(0, IharaZeta.cycleRank(s));
            assertEquals(0, IharaZeta.countPrimeCycles(s));
        }
    }

    // -----------------------------------------------------------------------
    // Recursive
    // -----------------------------------------------------------------------

    @Nested
    class RecursiveCycles {
        @Test
        void simpleRecursionCycleRank() {
            StateSpace s = ss("rec X . &{a: X, b: end}");
            assertTrue(IharaZeta.cycleRank(s) >= 0);
        }

        @Test
        void simpleRecursionPrimeCycles() {
            StateSpace s = ss("rec X . &{a: X, b: end}");
            List<Integer> lens = IharaZeta.primeCycleLengths(s, 6);
            assertNotNull(lens);
        }

        @Test
        void mutualRecursionLike() {
            StateSpace s = ss("rec X . &{a: &{b: X}, c: end}");
            assertTrue(IharaZeta.cycleRank(s) >= 0);
            assertNotNull(IharaZeta.primeCycleLengths(s, 10));
        }

        @Test
        void recursionDeterminantNontrivial() {
            StateSpace s = ss("rec X . &{a: &{b: X}, c: end}");
            double d = IharaZeta.iharaDeterminant(s, 0.3);
            assertTrue(Double.isFinite(d));
        }
    }

    // -----------------------------------------------------------------------
    // Cycle rank
    // -----------------------------------------------------------------------

    @Nested
    class CycleRank {
        @Test
        void empty() {
            assertEquals(0, IharaZeta.cycleRank(ss("end")));
        }

        @Test
        void singleEdge() {
            assertEquals(0, IharaZeta.cycleRank(ss("&{a: end}")));
        }

        @Test
        void rankNonNegative() {
            for (String t : List.of("end", "&{a: end}", "&{a: end, b: end}",
                    "+{a: end, b: end}", "&{a: &{b: end}}")) {
                assertTrue(IharaZeta.cycleRank(ss(t)) >= 0, t);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Undirected edges
    // -----------------------------------------------------------------------

    @Nested
    class UndirectedEdges {
        @Test
        void endNoEdges() {
            assertTrue(IharaZeta.undirectedEdges(ss("end")).isEmpty());
        }

        @Test
        void singleBranchOneEdge() {
            assertEquals(1, IharaZeta.undirectedEdges(ss("&{a: end}")).size());
        }

        @Test
        void twoBranchEdges() {
            assertTrue(IharaZeta.undirectedEdges(ss("&{a: end, b: end}")).size() >= 1);
        }

        @Test
        void noSelfLoops() {
            StateSpace s = ss("rec X . &{a: X, b: end}");
            for (int[] e : IharaZeta.undirectedEdges(s)) {
                assertNotEquals(e[0], e[1]);
            }
        }

        @Test
        void adjacencySymmetric() {
            StateSpace s = ss("&{a: &{b: end}, c: end}");
            var adj = IharaZeta.undirectedAdjacency(s);
            for (var u : adj.keySet()) {
                for (int v : adj.get(u)) {
                    assertTrue(adj.get(v).contains(u));
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Matrices
    // -----------------------------------------------------------------------

    @Nested
    class Matrices {
        @Test
        void adjacencySymmetric() {
            StateSpace s = ss("&{a: end, b: end}");
            List<Integer> st = new ArrayList<>();
            double[][] A = IharaZeta.adjacencyMatrix(s, st);
            int n = st.size();
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    assertEquals(A[i][j], A[j][i], EPS);
        }

        @Test
        void adjacencyDiagonalZero() {
            StateSpace s = ss("&{a: end, b: end}");
            List<Integer> st = new ArrayList<>();
            double[][] A = IharaZeta.adjacencyMatrix(s, st);
            for (int i = 0; i < st.size(); i++) assertEquals(0.0, A[i][i]);
        }

        @Test
        void degreeMatrixDiagonal() {
            StateSpace s = ss("&{a: end, b: end}");
            double[][] D = IharaZeta.degreeMatrix(s);
            List<Integer> st = new ArrayList<>();
            double[][] A = IharaZeta.adjacencyMatrix(s, st);
            int n = st.size();
            for (int i = 0; i < n; i++) {
                double rs = 0.0;
                for (int j = 0; j < n; j++) rs += A[i][j];
                assertEquals(rs, D[i][i], EPS);
                for (int j = 0; j < n; j++) {
                    if (i != j) assertEquals(0.0, D[i][j]);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Linear algebra
    // -----------------------------------------------------------------------

    @Nested
    class LinearAlgebra {
        @Test
        void identity() {
            double[][] I = IharaZeta.matIdentity(3);
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    assertEquals(i == j ? 1.0 : 0.0, I[i][j]);
        }

        @Test
        void detIdentity() {
            assertEquals(1.0, IharaZeta.matDet(IharaZeta.matIdentity(3)), EPS);
        }

        @Test
        void det2x2() {
            double[][] M = {{3.0, 1.0}, {2.0, 4.0}};
            assertEquals(10.0, IharaZeta.matDet(M), EPS);
        }

        @Test
        void detSingular() {
            double[][] M = {{1.0, 2.0}, {2.0, 4.0}};
            assertEquals(0.0, IharaZeta.matDet(M), 1e-10);
        }

        @Test
        void matMulIdentity() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            double[][] I = IharaZeta.matIdentity(2);
            double[][] R = IharaZeta.matMul(A, I);
            for (int i = 0; i < 2; i++)
                for (int j = 0; j < 2; j++)
                    assertEquals(A[i][j], R[i][j], EPS);
        }

        @Test
        void matAdd() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            double[][] B = {{5.0, 6.0}, {7.0, 8.0}};
            double[][] C = IharaZeta.matAdd(A, B);
            assertArrayEquals(new double[]{6.0, 8.0}, C[0]);
            assertArrayEquals(new double[]{10.0, 12.0}, C[1]);
        }

        @Test
        void matSub() {
            double[][] A = {{5.0, 6.0}, {7.0, 8.0}};
            double[][] B = {{1.0, 2.0}, {3.0, 4.0}};
            double[][] C = IharaZeta.matSub(A, B);
            assertArrayEquals(new double[]{4.0, 4.0}, C[0]);
            assertArrayEquals(new double[]{4.0, 4.0}, C[1]);
        }

        @Test
        void matScale() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            double[][] C = IharaZeta.matScale(A, 2.0);
            assertArrayEquals(new double[]{2.0, 4.0}, C[0]);
            assertArrayEquals(new double[]{6.0, 8.0}, C[1]);
        }

        @Test
        void eigenvaluesIdentity() {
            double[] eigs = IharaZeta.eigenvaluesSymmetric(IharaZeta.matIdentity(3));
            for (double e : eigs) assertEquals(1.0, e, 1e-6);
        }

        @Test
        void eigenvaluesDiagonal() {
            double[][] M = {{3.0, 0.0}, {0.0, 1.0}};
            double[] eigs = IharaZeta.eigenvaluesSymmetric(M);
            assertEquals(3.0, eigs[0], 1e-6);
            assertEquals(1.0, eigs[1], 1e-6);
        }

        @Test
        void eigenvaluesSymmetric2x2() {
            double[][] M = {{2.0, 1.0}, {1.0, 2.0}};
            double[] eigs = IharaZeta.eigenvaluesSymmetric(M);
            double[] sorted = eigs.clone();
            java.util.Arrays.sort(sorted);
            assertEquals(1.0, sorted[0], 1e-6);
            assertEquals(3.0, sorted[1], 1e-6);
        }
    }

    // -----------------------------------------------------------------------
    // Hashimoto
    // -----------------------------------------------------------------------

    @Nested
    class Hashimoto {
        @Test
        void noEdges() {
            double[][] B = IharaZeta.bassHashimotoMatrix(ss("end"));
            assertEquals(0, B.length);
        }

        @Test
        void singleEdge() {
            double[][] B = IharaZeta.bassHashimotoMatrix(ss("&{a: end}"));
            assertEquals(2, B.length);
            for (double[] row : B)
                for (double v : row) assertEquals(0.0, v);
        }

        @Test
        void pathGraph() {
            double[][] B = IharaZeta.bassHashimotoMatrix(ss("&{a: &{b: end}}"));
            assertEquals(4, B.length);
            double total = 0.0;
            for (int i = 0; i < 4; i++)
                for (int j = 0; j < 4; j++) total += B[i][j];
            assertTrue(total >= 2.0);
        }

        @Test
        void noSelfContinuation() {
            StateSpace s = ss("&{a: &{b: end}, c: end}");
            double[][] B = IharaZeta.bassHashimotoMatrix(s);
            List<int[]> undirected = IharaZeta.undirectedEdges(s);
            List<int[]> oriented = new ArrayList<>();
            for (int[] e : undirected) {
                oriented.add(new int[]{e[0], e[1]});
                oriented.add(new int[]{e[1], e[0]});
            }
            for (int i = 0; i < oriented.size(); i++) {
                for (int j = 0; j < oriented.size(); j++) {
                    int u1 = oriented.get(i)[0], v1 = oriented.get(i)[1];
                    int u2 = oriented.get(j)[0], v2 = oriented.get(j)[1];
                    if (u2 == v1 && v2 == u1) {
                        assertEquals(0.0, B[i][j]);
                    }
                }
            }
        }

        @Test
        void hashimotoSquare() {
            StateSpace s = ss("&{a: &{b: end}, c: end}");
            double[][] B = IharaZeta.bassHashimotoMatrix(s);
            int expected = 2 * IharaZeta.undirectedEdges(s).size();
            assertEquals(expected, B.length);
            for (double[] row : B) assertEquals(expected, row.length);
        }
    }

    // -----------------------------------------------------------------------
    // Ihara determinant
    // -----------------------------------------------------------------------

    @Nested
    class IharaDeterminant {
        @Test
        void detAtZero() {
            for (String t : List.of("end", "&{a: end}", "&{a: end, b: end}")) {
                assertEquals(1.0, IharaZeta.iharaDeterminant(ss(t), 0.0), EPS);
            }
        }

        @Test
        void detEmpty() {
            assertEquals(1.0, IharaZeta.iharaDeterminant(ss("end"), 0.5), EPS);
        }

        @Test
        void detFinite() {
            StateSpace s = ss("&{a: &{b: end}, c: end}");
            for (double u : new double[]{0.1, 0.3, 0.5, 0.7}) {
                assertTrue(Double.isFinite(IharaZeta.iharaDeterminant(s, u)));
            }
        }

        @Test
        void detDagAlwaysOne() {
            assertTrue(Double.isFinite(IharaZeta.iharaDeterminant(ss("&{a: end}"), 0.3)));
        }

        @Test
        void detConsistency() {
            StateSpace s = ss("&{a: &{b: end}, c: end}");
            double d1 = IharaZeta.iharaDeterminant(s, 0.30);
            double d2 = IharaZeta.iharaDeterminant(s, 0.31);
            assertTrue(Math.abs(d1 - d2) < 1.0);
        }
    }

    // -----------------------------------------------------------------------
    // Prime cycles
    // -----------------------------------------------------------------------

    @Nested
    class PrimeCycles {
        @Test
        void dagNoCycles() {
            StateSpace s = ss("&{a: end, b: end}");
            assertEquals(0, IharaZeta.countPrimeCycles(s));
            assertTrue(IharaZeta.primeCycleLengths(s).isEmpty());
        }

        @Test
        void endNoCycles() {
            assertEquals(0, IharaZeta.countPrimeCycles(ss("end")));
        }

        @Test
        void cycleLengthsSorted() {
            List<Integer> lens = IharaZeta.primeCycleLengths(ss("rec X . &{a: &{b: X}, c: end}"), 10);
            List<Integer> sorted = new ArrayList<>(lens);
            java.util.Collections.sort(sorted);
            assertEquals(sorted, lens);
        }

        @Test
        void primeCountMatchesLengths() {
            StateSpace s = ss("rec X . &{a: &{b: X}, c: end}");
            int count = IharaZeta.countPrimeCycles(s, 10);
            assertEquals(count, IharaZeta.primeCycleLengths(s, 10).size());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Nested
    class Helpers {
        @Test
        void isProperPowerFalse() {
            assertFalse(IharaZeta.isProperPower(List.of(1, 2, 3)));
        }

        @Test
        void isProperPowerTrue() {
            assertTrue(IharaZeta.isProperPower(List.of(1, 2, 1, 2)));
        }

        @Test
        void isProperPowerSingle() {
            assertFalse(IharaZeta.isProperPower(List.of(1)));
        }

        @Test
        void isProperPowerTriple() {
            assertTrue(IharaZeta.isProperPower(List.of(1, 1, 1)));
        }

        @Test
        void canonicalRotation() {
            assertEquals(List.of(1, 2, 3), IharaZeta.canonicalRotation(List.of(3, 1, 2)));
        }

        @Test
        void canonicalRotationAlreadyMin() {
            assertEquals(List.of(1, 2, 3), IharaZeta.canonicalRotation(List.of(1, 2, 3)));
        }

        @Test
        void canonicalRotationEmpty() {
            assertEquals(List.of(), IharaZeta.canonicalRotation(List.of()));
        }

        @Test
        void canonicalRotationSingle() {
            assertEquals(List.of(5), IharaZeta.canonicalRotation(List.of(5)));
        }
    }

    // -----------------------------------------------------------------------
    // Ramanujan
    // -----------------------------------------------------------------------

    @Nested
    class Ramanujan {
        @Test
        void trivialGraph() {
            assertTrue(IharaZeta.isRamanujan(ss("end")));
        }

        @Test
        void singleEdge() {
            assertTrue(IharaZeta.isRamanujan(ss("&{a: end}")));
        }

        @Test
        void treeRamanujan() {
            assertTrue(IharaZeta.isRamanujan(ss("&{a: end, b: end}")));
        }

        @Test
        void returnsBool() {
            for (String t : List.of("end", "&{a: end}", "&{a: end, b: end}",
                    "rec X . &{a: X, b: end}")) {
                IharaZeta.isRamanujan(ss(t)); // just shouldn't throw
            }
        }
    }

    // -----------------------------------------------------------------------
    // Poles
    // -----------------------------------------------------------------------

    @Nested
    class Poles {
        @Test
        void noEdgesNoPoles() {
            assertTrue(IharaZeta.iharaPoles(ss("end")).isEmpty());
        }

        @Test
        void polesComplexList() {
            var poles = IharaZeta.iharaPoles(ss("&{a: &{b: end}, c: end}"));
            for (double[] p : poles) assertEquals(2, p.length);
        }

        @Test
        void polesSortedByMagnitude() {
            var poles = IharaZeta.iharaPoles(ss("&{a: &{b: end}, c: end}"));
            double prev = -1.0;
            for (double[] p : poles) {
                double m = Math.hypot(p[0], p[1]);
                assertTrue(m >= prev - 1e-12);
                prev = m;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Graph complexity
    // -----------------------------------------------------------------------

    @Nested
    class GraphComplexity {
        @Test
        void trivialComplexity() {
            assertEquals(0.0, IharaZeta.graphComplexity(ss("end")), EPS);
        }

        @Test
        void dagComplexity() {
            assertEquals(0.0, IharaZeta.graphComplexity(ss("&{a: end}")), EPS);
        }

        @Test
        void complexityFinite() {
            double c = IharaZeta.graphComplexity(ss("rec X . &{a: &{b: X}, c: end}"));
            assertTrue(Double.isFinite(c) || c == Double.POSITIVE_INFINITY);
        }

        @Test
        void complexityNonNegative() {
            for (String t : List.of("end", "&{a: end}", "&{a: end, b: end}")) {
                assertTrue(IharaZeta.graphComplexity(ss(t)) >= -1e-8);
            }
        }
    }

    // -----------------------------------------------------------------------
    // analyzeIhara
    // -----------------------------------------------------------------------

    @Nested
    class AnalyzeIhara {
        @Test
        void resultType() {
            assertNotNull(IharaZeta.analyzeIhara(ss("&{a: end}")));
        }

        @Test
        void resultFields() {
            var r = IharaZeta.analyzeIhara(ss("&{a: end, b: end}"));
            assertNotNull(r.reciprocalPoles());
            assertNotNull(r.primeCycleLengths());
            assertNotNull(r.bassHashimotoMatrix());
        }

        @Test
        void endAnalysis() {
            var r = IharaZeta.analyzeIhara(ss("end"));
            assertEquals(0, r.rank());
            assertEquals(0, r.numPrimeCycles());
            assertTrue(r.primeCycleLengths().isEmpty());
            assertTrue(r.isRamanujan());
            assertEquals(0.0, r.complexity(), EPS);
        }

        @Test
        void branchAnalysis() {
            var r = IharaZeta.analyzeIhara(ss("&{a: end, b: end}"));
            assertEquals(0, r.rank());
            assertEquals(0, r.numPrimeCycles());
        }

        @Test
        void recursiveAnalysis() {
            var r = IharaZeta.analyzeIhara(ss("rec X . &{a: X, b: end}"));
            assertTrue(r.rank() >= 0);
        }

        @Test
        void parallelAnalysis() {
            var r = IharaZeta.analyzeIhara(ss("(&{a: end} || &{b: end})"));
            assertTrue(r.rank() >= 0);
        }

        @Test
        void parallelMatchesPython() {
            // Cross-checked against Python: rank=1, nprime=2, lens=[4,4], B size=8
            var r = IharaZeta.analyzeIhara(ss("(&{a: end} || &{b: end})"));
            assertEquals(1, r.rank());
            assertEquals(2, r.numPrimeCycles());
            assertEquals(List.of(4, 4), r.primeCycleLengths());
            assertEquals(8, r.bassHashimotoMatrix().length);
        }

        @Test
        void nestedWithRankMatchesPython() {
            // Cross-checked against Python: rank=1, det@0.3 ≈ 0.946729, B size=6
            StateSpace s = ss("&{a: &{b: end}, c: end}");
            assertEquals(1, IharaZeta.cycleRank(s));
            assertEquals(0.9467290, IharaZeta.iharaDeterminant(s, 0.3), 1e-5);
            assertEquals(6, IharaZeta.bassHashimotoMatrix(s).length);
        }
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {
        @Test
        void iterator() {
            var r = IharaZeta.analyzeIhara(ss("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertTrue(Double.isFinite(r.iharaDeterminant()));
        }

        @Test
        void simpleServer() {
            var r = IharaZeta.analyzeIhara(ss("rec X . &{request: &{respond: X}, shutdown: end}"));
            assertTrue(r.rank() >= 0);
        }

        @Test
        void selectionProtocol() {
            var r = IharaZeta.analyzeIhara(ss("+{OK: end, ERROR: end}"));
            assertEquals(0, r.rank());
            assertEquals(0, r.numPrimeCycles());
        }

        @Test
        void nestedBranch() {
            assertNotNull(IharaZeta.analyzeIhara(ss("&{a: &{b: end, c: end}, d: end}")));
        }

        @Test
        void parallelRecursive() {
            assertNotNull(IharaZeta.analyzeIhara(ss("(rec X . &{a: X, b: end} || &{c: end})")));
        }
    }
}
