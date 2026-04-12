package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HeatKernel: port of Python tests/test_heat_kernel.py (73 tests, Step 30q).
 */
class HeatKernelTest {

    private static final double TOL = 1e-6;

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    private static boolean approx(double a, double b) {
        return Math.abs(a - b) < TOL;
    }

    private static boolean approx(double a, double b, double tol) {
        return Math.abs(a - b) < tol;
    }

    private static double rowSum(double[] row) {
        double s = 0.0;
        for (double v : row) s += v;
        return s;
    }

    // -----------------------------------------------------------------------
    // Laplacian
    // -----------------------------------------------------------------------

    @Nested
    class LaplacianTests {

        @Test
        void testSingleState() {
            StateSpace ss = build("end");
            double[][] L = HeatKernel.graphLaplacian(ss);
            assertEquals(1, L.length);
            assertEquals(0.0, L[0][0]);
        }

        @Test
        void testTwoStateChain() {
            StateSpace ss = build("&{a: end}");
            double[][] L = HeatKernel.graphLaplacian(ss);
            int n = L.length;
            assertTrue(n >= 2);
            for (int i = 0; i < n; i++) assertTrue(approx(rowSum(L[i]), 0.0));
        }

        @Test
        void testSymmetric() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] L = HeatKernel.graphLaplacian(ss);
            int n = L.length;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    assertTrue(approx(L[i][j], L[j][i]));
        }

        @Test
        void testRowSumsZero() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            double[][] L = HeatKernel.graphLaplacian(ss);
            for (int i = 0; i < L.length; i++) assertTrue(approx(rowSum(L[i]), 0.0));
        }

        @Test
        void testDiagonalNonNegative() {
            StateSpace ss = build("&{a: end, b: &{c: end}}");
            double[][] L = HeatKernel.graphLaplacian(ss);
            for (int i = 0; i < L.length; i++) assertTrue(L[i][i] >= -TOL);
        }

        @Test
        void testOffDiagonalNonPositive() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] L = HeatKernel.graphLaplacian(ss);
            int n = L.length;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    if (i != j) assertTrue(L[i][j] <= TOL);
        }

        @Test
        void testPsdEigenvalues() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            var ed = HeatKernel.laplacianEigendecomposition(ss);
            for (double lam : ed.eigenvalues()) assertTrue(lam >= -TOL);
        }

        @Test
        void testSmallestEigenvalueZero() {
            StateSpace ss = build("&{a: end, b: end}");
            var ed = HeatKernel.laplacianEigendecomposition(ss);
            assertTrue(approx(ed.eigenvalues()[0], 0.0));
        }

        @Test
        void testParallelLaplacian() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            double[][] L = HeatKernel.graphLaplacian(ss);
            for (int i = 0; i < L.length; i++) assertTrue(approx(rowSum(L[i]), 0.0));
        }
    }

    // -----------------------------------------------------------------------
    // Eigendecomposition
    // -----------------------------------------------------------------------

    @Nested
    class EigendecompositionTests {

        @Test
        void testEigenvaluesSorted() {
            StateSpace ss = build("&{a: end, b: end}");
            double[] e = HeatKernel.laplacianEigendecomposition(ss).eigenvalues();
            for (int i = 0; i < e.length - 1; i++) assertTrue(e[i] <= e[i + 1] + TOL);
        }

        @Test
        void testEigenvectorsOrthogonal() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            var ed = HeatKernel.laplacianEigendecomposition(ss);
            int n = ed.eigenvalues().length;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double dot = 0.0;
                    for (int k = 0; k < n; k++)
                        dot += ed.eigenvectors()[i][k] * ed.eigenvectors()[j][k];
                    double expected = (i == j) ? 1.0 : 0.0;
                    assertTrue(approx(dot, expected, 1e-4),
                            "dot(v" + i + ",v" + j + ")=" + dot);
                }
            }
        }

        @Test
        void testEigenvectorsNormalized() {
            StateSpace ss = build("&{a: end, b: &{c: end}}");
            var ed = HeatKernel.laplacianEigendecomposition(ss);
            for (double[] v : ed.eigenvectors()) {
                double n = 0.0;
                for (double x : v) n += x * x;
                assertTrue(approx(Math.sqrt(n), 1.0, 1e-4));
            }
        }

        @Test
        void testSingleStateEigendecomposition() {
            StateSpace ss = build("end");
            var ed = HeatKernel.laplacianEigendecomposition(ss);
            assertEquals(1, ed.eigenvalues().length);
            assertEquals(0.0, ed.eigenvalues()[0]);
            assertEquals(1.0, ed.eigenvectors()[0][0]);
        }

        @Test
        void testEigenvalueCountMatchesStates() {
            StateSpace ss = build("&{a: end, b: end, c: end}");
            double[][] L = HeatKernel.graphLaplacian(ss);
            var ed = HeatKernel.laplacianEigendecomposition(ss);
            assertEquals(L.length, ed.eigenvalues().length);
            assertEquals(L.length, ed.eigenvectors().length);
        }
    }

    // -----------------------------------------------------------------------
    // Heat kernel matrix
    // -----------------------------------------------------------------------

    @Nested
    class HeatKernelMatrixTests {

        @Test
        void testH0IsIdentity() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] H = HeatKernel.heatKernelMatrix(ss, 0.0);
            int n = H.length;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    double expected = (i == j) ? 1.0 : 0.0;
                    assertTrue(approx(H[i][j], expected, 1e-4));
                }
        }

        @Test
        void testSymmetric() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            double[][] H = HeatKernel.heatKernelMatrix(ss, 1.0);
            int n = H.length;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    assertTrue(approx(H[i][j], H[j][i], 1e-4));
        }

        @Test
        void testPositiveEntries() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] H = HeatKernel.heatKernelMatrix(ss, 1.0);
            for (double[] row : H) for (double v : row) assertTrue(v >= -TOL);
        }

        @Test
        void testDiagonalDecreasingWithT() {
            StateSpace ss = build("&{a: end, b: end}");
            int n = HeatKernel.graphLaplacian(ss).length;
            double[] times = {0.0, 0.1, 1.0, 10.0};
            for (int i = 0; i < n; i++) {
                Double prev = null;
                for (double t : times) {
                    double v = HeatKernel.heatKernelMatrix(ss, t)[i][i];
                    if (prev != null) assertTrue(v <= prev + TOL);
                    prev = v;
                }
            }
        }

        @Test
        void testSingleStateHeatKernel() {
            StateSpace ss = build("end");
            double[][] H = HeatKernel.heatKernelMatrix(ss, 5.0);
            assertEquals(1, H.length);
            assertTrue(approx(H[0][0], 1.0));
        }

        @Test
        void testRowSumsConstant() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] H = HeatKernel.heatKernelMatrix(ss, 1.0);
            for (double[] row : H) assertTrue(approx(rowSum(row), 1.0, 1e-3));
        }
    }

    // -----------------------------------------------------------------------
    // Heat trace
    // -----------------------------------------------------------------------

    @Nested
    class HeatTraceTests {

        @Test
        void testZ0EqualsN() {
            StateSpace ss = build("&{a: end, b: end}");
            int n = HeatKernel.graphLaplacian(ss).length;
            assertTrue(approx(HeatKernel.heatTrace(ss, 0.0), n));
        }

        @Test
        void testTraceDecreasing() {
            StateSpace ss = build("&{a: end, b: end}");
            double[] times = {0.0, 0.1, 1.0, 5.0, 10.0};
            Double prev = null;
            for (double t : times) {
                double z = HeatKernel.heatTrace(ss, t);
                if (prev != null) assertTrue(z <= prev + TOL);
                prev = z;
            }
        }

        @Test
        void testTracePositive() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            for (double t : new double[]{0.01, 0.1, 1.0, 10.0})
                assertTrue(HeatKernel.heatTrace(ss, t) > 0.0);
        }

        @Test
        void testSingleStateTrace() {
            StateSpace ss = build("end");
            assertTrue(approx(HeatKernel.heatTrace(ss, 0.0), 1.0));
            assertTrue(approx(HeatKernel.heatTrace(ss, 100.0), 1.0));
        }

        @Test
        void testLongTimeLimit() {
            StateSpace ss = build("&{a: end, b: end}");
            assertTrue(approx(HeatKernel.heatTrace(ss, 100.0), 1.0, 0.01));
        }
    }

    // -----------------------------------------------------------------------
    // HKS
    // -----------------------------------------------------------------------

    @Nested
    class HksTests {

        @Test
        void testHksDecreasingInT() {
            StateSpace ss = build("&{a: end, b: end}");
            double[] times = {0.01, 0.1, 0.5, 1.0, 5.0, 10.0};
            for (int state : ss.states()) {
                double[] v = HeatKernel.heatKernelSignature(ss, state, times);
                for (int i = 0; i < v.length - 1; i++)
                    assertTrue(v[i] >= v[i + 1] - TOL);
            }
        }

        @Test
        void testHksPositive() {
            StateSpace ss = build("&{a: end, b: end}");
            double[] times = {0.1, 1.0, 5.0};
            for (int state : ss.states())
                for (double v : HeatKernel.heatKernelSignature(ss, state, times))
                    assertTrue(v >= -TOL);
        }

        @Test
        void testHksAtZero() {
            StateSpace ss = build("&{a: end, b: end}");
            for (int state : ss.states()) {
                double[] v = HeatKernel.heatKernelSignature(ss, state, new double[]{0.0});
                assertTrue(approx(v[0], 1.0, 1e-4));
            }
        }

        @Test
        void testHksSingleState() {
            StateSpace ss = build("end");
            double[] v = HeatKernel.heatKernelSignature(ss, ss.top(),
                    new double[]{0.0, 1.0, 10.0});
            for (double x : v) assertTrue(approx(x, 1.0));
        }
    }

    // -----------------------------------------------------------------------
    // Diffusion distance
    // -----------------------------------------------------------------------

    @Nested
    class DiffusionDistanceTests {

        @Test
        void testSelfDistanceZero() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] D = HeatKernel.diffusionDistance(ss, 1.0);
            for (int i = 0; i < D.length; i++) assertTrue(approx(D[i][i], 0.0));
        }

        @Test
        void testSymmetric() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            double[][] D = HeatKernel.diffusionDistance(ss, 1.0);
            int n = D.length;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    assertTrue(approx(D[i][j], D[j][i], 1e-4));
        }

        @Test
        void testNonNegative() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] D = HeatKernel.diffusionDistance(ss, 1.0);
            for (double[] row : D) for (double v : row) assertTrue(v >= -TOL);
        }

        @Test
        void testTriangleInequality() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            double[][] D = HeatKernel.diffusionDistance(ss, 1.0);
            int n = D.length;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    for (int k = 0; k < n; k++)
                        assertTrue(D[i][k] <= D[i][j] + D[j][k] + TOL);
        }

        @Test
        void testSingleState() {
            StateSpace ss = build("end");
            double[][] D = HeatKernel.diffusionDistance(ss, 1.0);
            assertEquals(1, D.length);
            assertEquals(0.0, D[0][0]);
        }

        @Test
        void testDistinctStatesPositiveDistance() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] D = HeatKernel.diffusionDistance(ss, 1.0);
            if (D.length > 1) assertTrue(D[0][1] > 0.0);
        }
    }

    // -----------------------------------------------------------------------
    // Total heat content
    // -----------------------------------------------------------------------

    @Nested
    class TotalHeatContentTests {

        @Test
        void testQ0EqualsN() {
            StateSpace ss = build("&{a: end, b: end}");
            int n = HeatKernel.graphLaplacian(ss).length;
            assertTrue(approx(HeatKernel.totalHeatContent(ss, 0.0), n));
        }

        @Test
        void testContentPositive() {
            StateSpace ss = build("&{a: end, b: end}");
            for (double t : new double[]{0.01, 0.1, 1.0, 10.0})
                assertTrue(HeatKernel.totalHeatContent(ss, t) > 0.0);
        }

        @Test
        void testSingleState() {
            StateSpace ss = build("end");
            assertTrue(approx(HeatKernel.totalHeatContent(ss, 0.0), 1.0));
            assertTrue(approx(HeatKernel.totalHeatContent(ss, 100.0), 1.0));
        }

        @Test
        void testLongTimeConnected() {
            StateSpace ss = build("&{a: end, b: end}");
            int n = HeatKernel.graphLaplacian(ss).length;
            assertTrue(approx(HeatKernel.totalHeatContent(ss, 100.0), n, 0.1));
        }
    }

    // -----------------------------------------------------------------------
    // Heat kernel PageRank
    // -----------------------------------------------------------------------

    @Nested
    class PageRankTests {

        @Test
        void testNonNegative() {
            StateSpace ss = build("&{a: end, b: end}");
            for (double v : HeatKernel.heatKernelPagerank(ss, 1.0)) assertTrue(v >= -TOL);
        }

        @Test
        void testSumsToOne() {
            StateSpace ss = build("&{a: end, b: end}");
            double[] pr = HeatKernel.heatKernelPagerank(ss, 1.0);
            double s = 0.0;
            for (double v : pr) s += v;
            assertTrue(approx(s, 1.0, 1e-4));
        }

        @Test
        void testUniformAtT0() {
            StateSpace ss = build("&{a: end, b: end}");
            int n = HeatKernel.graphLaplacian(ss).length;
            double[] pr = HeatKernel.heatKernelPagerank(ss, 0.0);
            for (double v : pr) assertTrue(approx(v, 1.0 / n, 1e-4));
        }

        @Test
        void testSingleState() {
            StateSpace ss = build("end");
            double[] pr = HeatKernel.heatKernelPagerank(ss, 1.0);
            assertEquals(1, pr.length);
            assertTrue(approx(pr[0], 1.0));
        }
    }

    // -----------------------------------------------------------------------
    // Return probability
    // -----------------------------------------------------------------------

    @Nested
    class ReturnProbabilityTests {

        @Test
        void testAtT0() {
            StateSpace ss = build("&{a: end, b: end}");
            for (int s : ss.states())
                assertTrue(approx(HeatKernel.returnProbability(ss, s, 0.0), 1.0, 1e-4));
        }

        @Test
        void testDecreasingInT() {
            StateSpace ss = build("&{a: end, b: end}");
            double[] times = {0.0, 0.1, 1.0, 10.0};
            for (int s : ss.states()) {
                Double prev = null;
                for (double t : times) {
                    double rp = HeatKernel.returnProbability(ss, s, t);
                    if (prev != null) assertTrue(rp <= prev + TOL);
                    prev = rp;
                }
            }
        }

        @Test
        void testPositive() {
            StateSpace ss = build("&{a: end, b: end}");
            for (int s : ss.states())
                assertTrue(HeatKernel.returnProbability(ss, s, 1.0) > 0.0);
        }
    }

    // -----------------------------------------------------------------------
    // Spectral gap
    // -----------------------------------------------------------------------

    @Nested
    class SpectralGapTests {

        @Test
        void testPositiveForConnected() {
            StateSpace ss = build("&{a: end, b: end}");
            assertTrue(HeatKernel.spectralGap(ss) > 0.0);
        }

        @Test
        void testSingleState() {
            StateSpace ss = build("end");
            assertTrue(approx(HeatKernel.spectralGap(ss), 0.0));
        }

        @Test
        void testGapIsSmallestNonzeroEigenvalue() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            double[] e = HeatKernel.laplacianEigendecomposition(ss).eigenvalues();
            double gap = HeatKernel.spectralGap(ss);
            double smallest = Double.POSITIVE_INFINITY;
            for (double lam : e) if (lam > 1e-10 && lam < smallest) smallest = lam;
            if (smallest != Double.POSITIVE_INFINITY)
                assertTrue(approx(gap, smallest, 1e-4));
        }
    }

    // -----------------------------------------------------------------------
    // Effective resistance
    // -----------------------------------------------------------------------

    @Nested
    class EffectiveResistanceTests {

        @Test
        void testSelfResistanceZero() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] R = HeatKernel.effectiveResistance(ss);
            for (int i = 0; i < R.length; i++) assertTrue(approx(R[i][i], 0.0));
        }

        @Test
        void testSymmetric() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] R = HeatKernel.effectiveResistance(ss);
            int n = R.length;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    assertTrue(approx(R[i][j], R[j][i], 1e-4));
        }

        @Test
        void testNonNegative() {
            StateSpace ss = build("&{a: end, b: end}");
            double[][] R = HeatKernel.effectiveResistance(ss);
            for (double[] row : R) for (double v : row) assertTrue(v >= -TOL);
        }
    }

    // -----------------------------------------------------------------------
    // Kemeny constant
    // -----------------------------------------------------------------------

    @Nested
    class KemenyConstantTests {

        @Test
        void testPositiveForNontrivial() {
            StateSpace ss = build("&{a: end, b: end}");
            assertTrue(HeatKernel.kemenyConstant(ss) > 0.0);
        }

        @Test
        void testSingleState() {
            StateSpace ss = build("end");
            assertTrue(approx(HeatKernel.kemenyConstant(ss), 0.0));
        }

        @Test
        void testIncreasesWithSize() {
            StateSpace ssSmall = build("&{a: end}");
            StateSpace ssLarge = build("&{a: &{b: end}, c: end}");
            double kSmall = HeatKernel.kemenyConstant(ssSmall);
            double kLarge = HeatKernel.kemenyConstant(ssLarge);
            assertTrue(kLarge >= kSmall - TOL);
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class AnalyzeTests {

        @Test
        void testResultType() {
            StateSpace ss = build("&{a: end, b: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertNotNull(r);
        }

        @Test
        void testSampleTimesDefault() {
            StateSpace ss = build("&{a: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertArrayEquals(HeatKernel.DEFAULT_SAMPLE_TIMES, r.sampleTimes(), 0.0);
        }

        @Test
        void testCustomSampleTimes() {
            StateSpace ss = build("&{a: end}");
            double[] times = {0.5, 2.0};
            var r = HeatKernel.analyzeHeatKernel(ss, times);
            assertArrayEquals(times, r.sampleTimes(), 0.0);
            assertEquals(2, r.heatTrace().length);
        }

        @Test
        void testLaplacianInResult() {
            StateSpace ss = build("&{a: end, b: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            int n = r.laplacian().length;
            assertTrue(n > 0);
            for (int i = 0; i < n; i++) assertTrue(approx(rowSum(r.laplacian()[i]), 0.0));
        }

        @Test
        void testEigenvaluesInResult() {
            StateSpace ss = build("&{a: end, b: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertEquals(r.laplacian().length, r.eigenvalues().length);
            assertTrue(approx(r.eigenvalues()[0], 0.0));
        }

        @Test
        void testHksDiagonalShape() {
            StateSpace ss = build("&{a: end, b: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            int n = r.laplacian().length;
            assertEquals(n, r.hksDiagonal().length);
            for (double[] row : r.hksDiagonal())
                assertEquals(r.sampleTimes().length, row.length);
        }

        @Test
        void testDiffusionDistanceInResult() {
            StateSpace ss = build("&{a: end, b: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            int n = r.laplacian().length;
            assertEquals(n, r.diffusionDistance().length);
            for (double[] row : r.diffusionDistance()) assertEquals(n, row.length);
        }

        @Test
        void testEmptyStateSpace() {
            StateSpace ss = build("end");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertEquals(1, r.laplacian().length);
            assertEquals(1, r.eigenvalues().length);
            assertEquals(0.0, r.eigenvalues()[0]);
        }

        @Test
        void testParallelAnalysis() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertTrue(r.laplacian().length > 0);
            for (double lam : r.eigenvalues()) assertTrue(lam >= -TOL);
        }

        @Test
        void testSelectionAnalysis() {
            StateSpace ss = build("+{a: end, b: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertTrue(r.laplacian().length > 0);
        }

        @Test
        void testDeeperTree() {
            StateSpace ss = build("&{a: &{b: end, c: end}, d: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertTrue(r.laplacian().length >= 3);
            for (double lam : r.eigenvalues()) assertTrue(lam >= -TOL);
        }
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    @Nested
    class BenchmarkTests {

        @Test
        void testSmtpLike() {
            StateSpace ss = build("&{ehlo: &{mail: &{rcpt: &{data: end}}, quit: end}}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertTrue(r.eigenvalues()[0] < TOL);
            for (double lam : r.eigenvalues()) assertTrue(lam >= -TOL);
        }

        @Test
        void testIterator() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertTrue(r.laplacian().length > 0);
        }

        @Test
        void testBinaryChoice() {
            StateSpace ss = build("+{ok: end, error: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            int n = r.laplacian().length;
            assertTrue(n > 0);
            assertTrue(approx(r.heatTrace()[0], n, 0.1));
        }

        @Test
        void testNestedBranch() {
            StateSpace ss = build("&{a: &{b: end}, c: &{d: end}}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertTrue(r.eigenvalues().length > 0);
            assertTrue(HeatKernel.spectralGap(ss) > 0.0);
        }

        @Test
        void testParallelProtocol() {
            StateSpace ss = build("(&{read: end} || &{write: end})");
            var r = HeatKernel.analyzeHeatKernel(ss);
            int n = r.laplacian().length;
            assertTrue(n > 0);
            for (int i = 0; i < n; i++) assertTrue(approx(r.diffusionDistance()[i][i], 0.0));
        }

        @Test
        void testRecursiveProtocol() {
            StateSpace ss = build("rec X . &{step: X, done: end}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            assertTrue(r.laplacian().length > 0);
        }

        @Test
        void testHeatTraceConsistency() {
            StateSpace ss = build("&{a: end, b: &{c: end}}");
            var r = HeatKernel.analyzeHeatKernel(ss);
            for (int i = 0; i < r.sampleTimes().length; i++) {
                double direct = HeatKernel.heatTrace(ss, r.sampleTimes()[i]);
                assertTrue(approx(r.heatTrace()[i], direct, 1e-3));
            }
        }
    }
}
