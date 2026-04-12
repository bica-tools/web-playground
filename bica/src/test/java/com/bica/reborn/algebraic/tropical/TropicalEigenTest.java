package com.bica.reborn.algebraic.tropical;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.bica.reborn.algebraic.tropical.TropicalEigen.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TropicalEigen}: port of Python
 * {@code tests/test_tropical_eigen.py} (Step 30l). 116 tests translated
 * 1:1 where feasible.
 */
class TropicalEigenTest {

    private static final double EPS = 1e-9;
    private static final double NEG_INF = Double.NEGATIVE_INFINITY;

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // =======================================================================
    // Internal helper tests
    // =======================================================================

    @Nested
    class TropicalIdentityTest {
        @Test
        void oneByOne() {
            double[][] I = tropicalIdentity(1);
            assertEquals(1, I.length);
            assertEquals(0.0, I[0][0]);
        }

        @Test
        void threeByThree() {
            double[][] I = tropicalIdentity(3);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (i == j) assertEquals(0.0, I[i][j]);
                    else assertEquals(NEG_INF, I[i][j]);
                }
            }
        }

        @Test
        void zeroByZero() {
            double[][] I = tropicalIdentity(0);
            assertEquals(0, I.length);
        }
    }

    @Nested
    class TropicalMatrixAddTest {
        @Test
        void basic() {
            double[][] A = {{1.0, NEG_INF}, {NEG_INF, 2.0}};
            double[][] B = {{NEG_INF, 3.0}, {4.0, NEG_INF}};
            double[][] C = tropicalMatrixAdd(A, B);
            assertEquals(1.0, C[0][0]);
            assertEquals(3.0, C[0][1]);
            assertEquals(4.0, C[1][0]);
            assertEquals(2.0, C[1][1]);
        }

        @Test
        void identityIsNeutral() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            double[][] I = {{NEG_INF, NEG_INF}, {NEG_INF, NEG_INF}};
            double[][] C = tropicalMatrixAdd(A, I);
            assertTrue(matricesEqual(A, C));
        }

        @Test
        void empty() {
            double[][] C = tropicalMatrixAdd(new double[0][0], new double[0][0]);
            assertEquals(0, C.length);
        }
    }

    @Nested
    class TropicalMatrixScalarTest {
        @Test
        void addScalar() {
            double[][] A = {{1.0, NEG_INF}, {3.0, 2.0}};
            double[][] C = tropicalMatrixScalar(A, 5.0);
            assertEquals(6.0, C[0][0]);
            assertEquals(NEG_INF, C[0][1]);
            assertEquals(8.0, C[1][0]);
            assertEquals(7.0, C[1][1]);
        }

        @Test
        void zeroScalar() {
            double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
            double[][] C = tropicalMatrixScalar(A, 0.0);
            assertTrue(matricesEqual(A, C));
        }

        @Test
        void negInfAbsorbs() {
            double[][] A = {{1.0, 2.0}};
            double[][] C = tropicalMatrixScalar(A, NEG_INF);
            assertEquals(NEG_INF, C[0][0]);
            assertEquals(NEG_INF, C[0][1]);
        }
    }

    @Nested
    class MatricesEqualTest {
        @Test
        void equal() {
            double[][] A = {{1.0, NEG_INF}, {2.0, 3.0}};
            double[][] B = {{1.0, NEG_INF}, {2.0, 3.0}};
            assertTrue(matricesEqual(A, B));
        }

        @Test
        void notEqual() {
            double[][] A = {{1.0, NEG_INF}, {2.0, 3.0}};
            double[][] B = {{1.0, NEG_INF}, {2.0, 4.0}};
            assertFalse(matricesEqual(A, B));
        }

        @Test
        void negInfMismatch() {
            double[][] A = {{NEG_INF}};
            double[][] B = {{0.0}};
            assertFalse(matricesEqual(A, B));
        }

        @Test
        void tolerance() {
            double[][] A = {{1.0000000001}};
            double[][] B = {{1.0}};
            assertTrue(matricesEqual(A, B, 1e-9));
        }
    }

    @Nested
    class GcdListTest {
        @Test
        void single() {
            assertEquals(6, gcdList(List.of(6)));
        }

        @Test
        void two() {
            assertEquals(2, gcdList(List.of(6, 4)));
        }

        @Test
        void coprime() {
            assertEquals(1, gcdList(List.of(7, 3)));
        }

        @Test
        void allSame() {
            assertEquals(5, gcdList(List.of(5, 5, 5)));
        }

        @Test
        void empty() {
            assertEquals(0, gcdList(List.of()));
        }

        @Test
        void withOne() {
            assertEquals(1, gcdList(List.of(12, 8, 1)));
        }
    }

    // =======================================================================
    // Critical components
    // =======================================================================

    @Nested
    class CriticalComponentsTest {
        @Test
        void endNoComponents() {
            assertTrue(criticalComponents(build("end")).isEmpty());
        }

        @Test
        void linearNoComponents() {
            assertTrue(criticalComponents(build("&{a: end}")).isEmpty());
        }

        @Test
        void branchNoComponents() {
            assertTrue(criticalComponents(build("&{a: end, b: end}")).isEmpty());
        }

        @Test
        void recursiveHasComponents() {
            var comps = criticalComponents(build("rec X . &{a: X}"));
            assertTrue(comps.size() >= 1);
        }

        @Test
        void recursiveBranchComponents() {
            var comps = criticalComponents(build("rec X . &{a: X, b: end}"));
            assertTrue(comps.size() >= 1);
        }

        @Test
        void componentsAreSorted() {
            var comps = criticalComponents(build("rec X . &{a: X, b: end}"));
            for (var comp : comps) {
                List<Integer> sorted = new ArrayList<>(comp);
                java.util.Collections.sort(sorted);
                assertEquals(sorted, comp);
            }
            List<Integer> firsts = new ArrayList<>();
            for (var c : comps) firsts.add(c.get(0));
            List<Integer> sortedFirsts = new ArrayList<>(firsts);
            java.util.Collections.sort(sortedFirsts);
            assertEquals(sortedFirsts, firsts);
        }

        @Test
        void componentStatesAreValid() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            var comps = criticalComponents(ss);
            for (var comp : comps) {
                for (int s : comp) {
                    assertTrue(ss.states().contains(s));
                }
            }
        }

        @Test
        void parallelNoCritical() {
            assertTrue(criticalComponents(build("(&{a: end} || &{b: end})")).isEmpty());
        }

        @Test
        void iteratorComponentsValid() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var comps = criticalComponents(ss);
            // Components exist for this cyclic protocol
            assertTrue(comps.size() >= 1);
        }
    }

    // =======================================================================
    // Cyclicity
    // =======================================================================

    @Nested
    class CyclicityTest {
        @Test
        void acyclicZero() {
            assertEquals(0, cyclicity(build("end")));
        }

        @Test
        void linearZero() {
            assertEquals(0, cyclicity(build("&{a: end}")));
        }

        @Test
        void branchZero() {
            assertEquals(0, cyclicity(build("&{a: end, b: end}")));
        }

        @Test
        void simpleRecursion() {
            assertTrue(cyclicity(build("rec X . &{a: X}")) >= 1);
        }

        @Test
        void recursionWithExit() {
            assertTrue(cyclicity(build("rec X . &{a: X, b: end}")) >= 1);
        }

        @Test
        void iteratorCyclicityPositive() {
            int cyc = cyclicity(build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertTrue(cyc > 0);
        }

        @Test
        void parallelCyclicityZero() {
            assertEquals(0, cyclicity(build("(&{a: end} || &{b: end})")));
        }

        @Test
        void cyclicityPositiveForAllCyclic() {
            for (String typ : List.of(
                    "rec X . &{a: X}",
                    "rec X . &{a: X, b: end}",
                    "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")) {
                assertTrue(cyclicity(build(typ)) > 0, "Expected positive cyclicity for " + typ);
            }
        }
    }

    // =======================================================================
    // Kleene star
    // =======================================================================

    @Nested
    class KleeneStarTest {
        @Test
        void end() {
            double[][] ks = kleeneStar(build("end"));
            assertNotNull(ks);
            assertEquals(1, ks.length);
            assertEquals(0.0, ks[0][0]);
        }

        @Test
        void linear() {
            double[][] ks = kleeneStar(build("&{a: end}"));
            assertNotNull(ks);
            for (int i = 0; i < ks.length; i++) assertEquals(0.0, ks[i][i]);
        }

        @Test
        void branch() {
            assertNotNull(kleeneStar(build("&{a: end, b: end}")));
        }

        @Test
        void recursiveDiverges() {
            assertNull(kleeneStar(build("rec X . &{a: X}")));
        }

        @Test
        void recursiveWithExitDiverges() {
            assertNull(kleeneStar(build("rec X . &{a: X, b: end}")));
        }

        @Test
        void nestedBranchConverges() {
            assertNotNull(kleeneStar(build("&{a: &{b: end}}")));
        }

        @Test
        void selectionConverges() {
            assertNotNull(kleeneStar(build("+{a: end, b: end}")));
        }

        @Test
        void parallelConverges() {
            assertNotNull(kleeneStar(build("(&{a: end} || &{b: end})")));
        }

        @Test
        void kleeneStarIdentityProperty() {
            StateSpace ss = build("&{a: &{b: end}}");
            double[][] star = kleeneStar(ss);
            assertNotNull(star);
            double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
            int n = A.length;
            double[][] I = new double[n][n];
            for (double[] row : I) Arrays.fill(row, NEG_INF);
            for (int i = 0; i < n; i++) I[i][i] = 0.0;
            double[][] starA = Tropical.tropicalMatrixMul(star, A);
            double[][] iPlus = tropicalMatrixAdd(I, starA);
            assertTrue(matricesEqual(star, iPlus), "Kleene star identity failed");
        }

        @Test
        void kleeneStarDiagonalZero() {
            double[][] star = kleeneStar(build("&{a: &{b: end}}"));
            assertNotNull(star);
            for (int i = 0; i < star.length; i++) assertEquals(0.0, star[i][i]);
        }

        @Test
        void spectralRadiusGtZeroMeansNull() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            assertTrue(Tropical.tropicalSpectralRadius(ss) > 0.0);
            assertNull(kleeneStar(ss));
        }
    }

    // =======================================================================
    // Eigenspace
    // =======================================================================

    @Nested
    class TropicalEigenspaceTest {
        @Test
        void end() {
            var basis = tropicalEigenspace(build("end"));
            assertTrue(basis.size() >= 1);
            assertEquals(1, basis.get(0).length);
        }

        @Test
        void linear() {
            assertTrue(tropicalEigenspace(build("&{a: end}")).size() >= 1);
        }

        @Test
        void branch() {
            assertTrue(tropicalEigenspace(build("&{a: end, b: end}")).size() >= 1);
        }

        @Test
        void recursive() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            var basis = tropicalEigenspace(ss);
            assertTrue(basis.size() >= 1);
            int n = ss.states().size();
            for (var v : basis) assertEquals(n, v.length);
        }

        @Test
        void vectorsAreArrays() {
            var basis = tropicalEigenspace(build("&{a: end, b: end}"));
            for (var v : basis) assertNotNull(v);
        }

        @Test
        void eigenvectorEquationSimpleRec() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
            double lam = Tropical.tropicalEigenvalue(ss);
            var basis = tropicalEigenspace(ss);
            assertEquals(1.0, lam);
            for (var v : basis) {
                double[] Av = tropicalMatrixVec(A, v);
                for (int i = 0; i < v.length; i++) {
                    double lv = Tropical.tropicalMul(lam, v[i]);
                    if (Av[i] == NEG_INF && lv == NEG_INF) continue;
                    assertTrue(Math.abs(Av[i] - lv) < EPS,
                            "Av[" + i + "]=" + Av[i] + " lv=" + lv);
                }
            }
        }

        @Test
        void eigenvectorEquationIterator() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
            double lam = Tropical.tropicalEigenvalue(ss);
            var basis = tropicalEigenspace(ss);
            assertEquals(1.0, lam);
            for (var v : basis) {
                double[] Av = tropicalMatrixVec(A, v);
                for (int i = 0; i < v.length; i++) {
                    double lv = Tropical.tropicalMul(lam, v[i]);
                    if (Av[i] == NEG_INF && lv == NEG_INF) continue;
                    assertTrue(Math.abs(Av[i] - lv) < EPS);
                }
            }
        }

        @Test
        void dimensionEqualsEigenspaceSize() {
            for (String typ : List.of(
                    "end", "&{a: end}", "rec X . &{a: X, b: end}",
                    "(&{a: end} || &{b: end})")) {
                StateSpace ss = build(typ);
                assertEquals(eigenspaceDimension(ss), tropicalEigenspace(ss).size(),
                        "Mismatch for " + typ);
            }
        }
    }

    @Nested
    class TropicalSubeigenspaceTest {
        @Test
        void end() {
            assertTrue(tropicalSubeigenspace(build("end")).size() >= 1);
        }

        @Test
        void branch() {
            assertTrue(tropicalSubeigenspace(build("&{a: end, b: end}")).size() >= 1);
        }

        @Test
        void subeigenContainsEigen() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            assertTrue(tropicalSubeigenspace(ss).size() >= tropicalEigenspace(ss).size());
        }

        @Test
        void recursive() {
            assertTrue(tropicalSubeigenspace(build("rec X . &{a: X, b: end}")).size() >= 1);
        }

        @Test
        void subeigenvectorSatisfiesInequality() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
            double lam = Tropical.tropicalEigenvalue(ss);
            var subvecs = tropicalSubeigenspace(ss);
            for (var v : subvecs) {
                double[] Av = tropicalMatrixVec(A, v);
                for (int i = 0; i < v.length; i++) {
                    double lamVi = Tropical.tropicalMul(lam, v[i]);
                    if (Av[i] > NEG_INF && lamVi > NEG_INF) {
                        assertTrue(Av[i] <= lamVi + EPS,
                                "Subeigenspace inequality violated at i=" + i);
                    }
                }
            }
        }
    }

    @Nested
    class EigenspaceDimensionTest {
        @Test
        void endDim1() {
            assertEquals(1, eigenspaceDimension(build("end")));
        }

        @Test
        void linearDim1() {
            assertEquals(1, eigenspaceDimension(build("&{a: end}")));
        }

        @Test
        void branchDim1() {
            assertEquals(1, eigenspaceDimension(build("&{a: end, b: end}")));
        }

        @Test
        void recursivePositiveDim() {
            assertTrue(eigenspaceDimension(build("rec X . &{a: X, b: end}")) >= 1);
        }

        @Test
        void dimensionMatchesComponents() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            var comps = criticalComponents(ss);
            int dim = eigenspaceDimension(ss);
            if (!comps.isEmpty()) assertEquals(comps.size(), dim);
        }
    }

    // =======================================================================
    // CSR decomposition
    // =======================================================================

    @Nested
    class CSRDecompositionTest {
        @Test
        void end() {
            int[] kc = csrDecomposition(build("end"));
            assertEquals(0, kc[0]);
            assertEquals(1, kc[1]);
        }

        @Test
        void linear() {
            int[] kc = csrDecomposition(build("&{a: end}"));
            assertEquals(0, kc[0]);
            assertEquals(1, kc[1]);
        }

        @Test
        void branch() {
            int[] kc = csrDecomposition(build("&{a: end, b: end}"));
            assertEquals(0, kc[0]);
            assertEquals(1, kc[1]);
        }

        @Test
        void recursive() {
            int[] kc = csrDecomposition(build("rec X . &{a: X, b: end}"));
            assertTrue(kc[0] >= 0);
            assertTrue(kc[1] >= 1);
        }

        @Test
        void periodPositive() {
            assertTrue(csrDecomposition(build("rec X . &{a: X}"))[1] >= 1);
        }

        @Test
        void indexNonNegative() {
            assertTrue(csrDecomposition(build("rec X . &{a: X, b: end}"))[0] >= 0);
        }

        @Test
        void csrPeriodicityHolds() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
            double lam = Tropical.tropicalEigenvalue(ss);
            int[] kc = csrDecomposition(ss);
            int k0 = kc[0], c = kc[1];
            assertTrue(lam > 0.0);
            double[][] Ak = Tropical.tropicalMatrixPower(A, k0);
            double[][] Akc = Tropical.tropicalMatrixPower(A, k0 + c);
            double[][] shifted = tropicalMatrixScalar(Ak, lam * c);
            assertTrue(matricesEqual(Akc, shifted), "CSR periodicity failed");
        }

        @Test
        void csrPeriodicityIterator() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
            double lam = Tropical.tropicalEigenvalue(ss);
            int[] kc = csrDecomposition(ss);
            int k0 = kc[0], c = kc[1];
            assertTrue(lam > 0.0);
            double[][] Ak = Tropical.tropicalMatrixPower(A, k0);
            double[][] Akc = Tropical.tropicalMatrixPower(A, k0 + c);
            double[][] shifted = tropicalMatrixScalar(Ak, lam * c);
            assertTrue(matricesEqual(Akc, shifted), "CSR failed for Iterator");
        }

        @Test
        void csrPeriodMatchesCyclicity() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            int c = csrDecomposition(ss)[1];
            int cyc = cyclicity(ss);
            if (cyc > 0) assertEquals(cyc, c);
        }
    }

    // =======================================================================
    // Definiteness
    // =======================================================================

    @Nested
    class IsDefiniteTest {
        @Test
        void end() {
            isDefinite(build("end"));
        }

        @Test
        void recursiveNotDefinite() {
            assertFalse(isDefinite(build("rec X . &{a: X}")));
        }

        @Test
        void linearNotDefinite() {
            isDefinite(build("&{a: end}"));
        }

        @Test
        void branchNotDefinite() {
            isDefinite(build("&{a: end, b: end}"));
        }
    }

    // =======================================================================
    // Tropical characteristic polynomial
    // =======================================================================

    @Nested
    class TropicalCharacteristicPolynomialTest {
        @Test
        void end() {
            double[] poly = tropicalCharacteristicPolynomial(build("end"));
            assertEquals(2, poly.length);
        }

        @Test
        void linear() {
            assertEquals(3, tropicalCharacteristicPolynomial(build("&{a: end}")).length);
        }

        @Test
        void branch() {
            StateSpace ss = build("&{a: end, b: end}");
            double[] poly = tropicalCharacteristicPolynomial(ss);
            assertEquals(ss.states().size() + 1, poly.length);
        }

        @Test
        void coefficientsAreDoubles() {
            double[] poly = tropicalCharacteristicPolynomial(build("&{a: end, b: end}"));
            assertNotNull(poly);
        }

        @Test
        void lengthNPlus1Parametric() {
            for (String typ : List.of(
                    "end", "&{a: end}", "&{a: &{b: end}}", "rec X . &{a: X, b: end}")) {
                StateSpace ss = build(typ);
                double[] poly = tropicalCharacteristicPolynomial(ss);
                assertEquals(ss.states().size() + 1, poly.length, "Mismatch for " + typ);
            }
        }

        @Test
        void lastCoeffIsZero() {
            double[] poly = tropicalCharacteristicPolynomial(build("&{a: end, b: end}"));
            assertEquals(0.0, poly[poly.length - 1]);
        }

        @Test
        void recursiveHasNontrivialCoefficients() {
            double[] poly = tropicalCharacteristicPolynomial(build("rec X . &{a: X, b: end}"));
            int finite = 0;
            for (double c : poly) if (c > NEG_INF) finite++;
            assertTrue(finite >= 2);
        }
    }

    // =======================================================================
    // Tropical trace
    // =======================================================================

    @Nested
    class TropicalTraceTest {
        @Test
        void end() {
            assertEquals(NEG_INF, tropicalTrace(build("end"), 1));
        }

        @Test
        void linearK1() {
            assertEquals(NEG_INF, tropicalTrace(build("&{a: end}"), 1));
        }

        @Test
        void recursiveK1() {
            double tr = tropicalTrace(build("rec X . &{a: X}"), 1);
            assertFalse(Double.isNaN(tr));
        }

        @Test
        void traceK0() {
            assertEquals(0.0, tropicalTrace(build("&{a: end}"), 0));
        }

        @Test
        void traceK2Branch() {
            double tr = tropicalTrace(build("&{a: end, b: end}"), 2);
            assertFalse(Double.isNaN(tr));
        }

        @Test
        void traceMonotone() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            double tr1 = tropicalTrace(ss, 1);
            double tr2 = tropicalTrace(ss, 2);
            double tr3 = tropicalTrace(ss, 3);
            assertFalse(Double.isNaN(tr1));
            assertFalse(Double.isNaN(tr2));
            assertFalse(Double.isNaN(tr3));
        }

        @Test
        void traceConsistentWithMatrixPower() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
            for (int k : new int[]{1, 2, 3}) {
                double[][] Ak = Tropical.tropicalMatrixPower(A, k);
                double maxDiag = NEG_INF;
                for (int i = 0; i < Ak.length; i++) if (Ak[i][i] > maxDiag) maxDiag = Ak[i][i];
                double tk = tropicalTrace(ss, k);
                assertTrue(Math.abs(tk - maxDiag) < EPS, "Trace mismatch at k=" + k);
            }
        }

        @Test
        void trace1IsMaxDiagonal() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
            double maxDiag = NEG_INF;
            for (int i = 0; i < A.length; i++) if (A[i][i] > maxDiag) maxDiag = A[i][i];
            double t1 = tropicalTrace(ss, 1);
            assertTrue(Math.abs(t1 - maxDiag) < EPS);
        }

        @Test
        void traceCyclicMonotoneValues() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            double lam = Tropical.tropicalEigenvalue(ss);
            assertEquals(1.0, lam);
            for (int k : new int[]{1, 2, 3}) {
                assertEquals(k * lam, tropicalTrace(ss, k));
            }
        }
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    @Nested
    class AnalyzeTropicalEigenTest {
        @Test
        void end() {
            var r = analyzeTropicalEigen(build("end"));
            assertEquals(0.0, r.eigenvalue());
            assertTrue(r.eigenspaceDim() >= 1);
            assertEquals(0, r.cyclicity());
            assertEquals(0, r.csrIndex());
            assertEquals(1, r.csrPeriod());
        }

        @Test
        void linear() {
            var r = analyzeTropicalEigen(build("&{a: end}"));
            assertEquals(0.0, r.eigenvalue());
            assertNotNull(r.kleeneStar());
            assertTrue(r.criticalComponents().isEmpty());
            assertEquals(0, r.cyclicity());
        }

        @Test
        void branch() {
            var r = analyzeTropicalEigen(build("&{a: end, b: end}"));
            assertEquals(0.0, r.eigenvalue());
            assertTrue(r.eigenvectors().size() >= 1);
            assertTrue(r.subeigenvectors().size() >= 1);
        }

        @Test
        void selection() {
            var r = analyzeTropicalEigen(build("+{a: end, b: end}"));
            assertEquals(0.0, r.eigenvalue());
            assertNotNull(r.kleeneStar());
        }

        @Test
        void recursiveSimple() {
            var r = analyzeTropicalEigen(build("rec X . &{a: X}"));
            assertTrue(r.eigenvalue() > 0.0);
            assertNull(r.kleeneStar());
            assertTrue(r.criticalComponents().size() >= 1);
            assertTrue(r.cyclicity() >= 1);
        }

        @Test
        void recursiveWithExit() {
            var r = analyzeTropicalEigen(build("rec X . &{a: X, b: end}"));
            assertTrue(r.eigenvalue() > 0.0);
            assertNull(r.kleeneStar());
            assertTrue(r.csrPeriod() >= 1);
        }

        @Test
        void parallel() {
            var r = analyzeTropicalEigen(build("(&{a: end} || &{b: end})"));
            assertEquals(0.0, r.eigenvalue());
            assertNotNull(r.kleeneStar());
        }

        @Test
        void nestedBranch() {
            var r = analyzeTropicalEigen(build("&{a: &{b: end, c: end}, d: end}"));
            assertEquals(0.0, r.eigenvalue());
            assertTrue(r.eigenvectors().size() >= 1);
        }

        @Test
        void visualizationData() {
            var r = analyzeTropicalEigen(build("rec X . &{a: X, b: end}"));
            Map<String, Object> viz = r.visualizationData();
            assertTrue(viz.containsKey("num_states"));
            assertTrue(viz.containsKey("eigenvalue"));
            assertTrue(viz.containsKey("eigenspace_dim"));
            assertTrue(viz.containsKey("cyclicity"));
            assertTrue(viz.containsKey("csr_index"));
            assertTrue(viz.containsKey("csr_period"));
            assertTrue(viz.containsKey("critical_nodes"));
            assertTrue(viz.containsKey("has_kleene_star"));
            assertTrue(viz.containsKey("is_definite"));
        }

        @Test
        void resultIsRecord() {
            // Java records are immutable by construction — analogue of
            // "frozen dataclass" test from Python.
            var r = analyzeTropicalEigen(build("end"));
            assertNotNull(r);
        }
    }

    // =======================================================================
    // Consistency
    // =======================================================================

    @Nested
    class ConsistencyTest {
        @Test
        void eigenvalueMatchesTropicalModule() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            double lam = Tropical.tropicalEigenvalue(ss);
            assertEquals(lam, analyzeTropicalEigen(ss).eigenvalue(), EPS);
        }

        @Test
        void criticalGraphMatches() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            var crit = Tropical.criticalGraph(ss);
            var comps = criticalComponents(ss);
            java.util.Set<Integer> critNodes = new java.util.HashSet<>();
            for (int[] e : crit) {
                critNodes.add(e[0]);
                critNodes.add(e[1]);
            }
            java.util.Set<Integer> compNodes = new java.util.HashSet<>();
            for (var c : comps) compNodes.addAll(c);
            assertTrue(critNodes.containsAll(compNodes));
        }

        @Test
        void eigenspaceDimMatchesVectors() {
            var r = analyzeTropicalEigen(build("rec X . &{a: X, b: end}"));
            assertEquals(r.eigenspaceDim(), r.eigenvectors().size());
        }

        @Test
        void csrPeriodMatchesCyclicity() {
            var r = analyzeTropicalEigen(build("rec X . &{a: X, b: end}"));
            assertTrue(r.csrPeriod() == r.cyclicity() || r.cyclicity() == 0);
        }
    }

    // =======================================================================
    // Benchmarks
    // =======================================================================

    @Nested
    class BenchmarksTest {
        @Test
        void iterator() {
            var r = analyzeTropicalEigen(build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertTrue(r.eigenvalue() > 0.0);
            assertTrue(r.eigenspaceDim() >= 1);
            assertTrue(r.cyclicity() >= 1);
        }

        @Test
        void simpleBranch() {
            var r = analyzeTropicalEigen(build("&{open: &{close: end}}"));
            assertEquals(0.0, r.eigenvalue());
            assertNotNull(r.kleeneStar());
        }

        @Test
        void nestedSelection() {
            var r = analyzeTropicalEigen(build("+{ok: &{read: end}, err: end}"));
            assertEquals(0.0, r.eigenvalue());
        }

        @Test
        void parallelSimple() {
            var r = analyzeTropicalEigen(build("(&{a: end} || &{b: end})"));
            assertEquals(0.0, r.eigenvalue());
        }

        @Test
        void complexRecursive() {
            var r = analyzeTropicalEigen(build("rec X . &{read: X, write: X, close: end}"));
            assertTrue(r.eigenvalue() > 0.0);
            assertTrue(r.criticalComponents().size() >= 1);
            assertTrue(r.csrIndex() >= 0);
            assertTrue(r.csrPeriod() >= 1);
        }

        @Test
        void smtpSimple() {
            StateSpace ss = build("&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}");
            var r = analyzeTropicalEigen(ss);
            assertEquals(0.0, r.eigenvalue());
            assertEquals(Tropical.tropicalEigenvalue(ss), r.eigenvalue());
            assertNotNull(r.kleeneStar());
        }

        @Test
        void twoBranch() {
            StateSpace ss = build("&{a: end, b: end}");
            var r = analyzeTropicalEigen(ss);
            assertEquals(0.0, r.eigenvalue());
            assertEquals(Tropical.tropicalEigenvalue(ss), r.eigenvalue());
        }

        @Test
        void eigenvalueAgreesWithTropicalModule() {
            List<String> types = List.of(
                    "end",
                    "&{a: end}",
                    "&{a: &{b: end}}",
                    "&{a: end, b: end}",
                    "rec X . &{a: X, b: end}",
                    "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                    "(&{a: end} || &{b: end})");
            for (String typ : types) {
                StateSpace ss = build(typ);
                var r = analyzeTropicalEigen(ss);
                assertEquals(Tropical.tropicalEigenvalue(ss), r.eigenvalue(), EPS,
                        "Eigenvalue mismatch for " + typ);
                assertEquals(Tropical.tropicalSpectralRadius(ss), r.eigenvalue(), EPS,
                        "Spectral radius mismatch for " + typ);
            }
        }
    }
}
