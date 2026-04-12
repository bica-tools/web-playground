package com.bica.reborn.kirchhoff;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.algebraic.matrix.MatrixOps;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link KirchhoffChecker} -- Java port of Python
 * {@code test_kirchhoff.py} (23 tests, Step 30h).
 */
class KirchhoffCheckerTest {

    private static final double EPS = 1e-8;

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // -----------------------------------------------------------------------
    // Determinant tests
    // -----------------------------------------------------------------------

    @Nested
    class DeterminantTests {

        @Test
        void det1x1() {
            assertEquals(5.0, MatrixOps.determinant(new double[][]{{5.0}}), EPS);
        }

        @Test
        void det2x2() {
            double det = MatrixOps.determinant(new double[][]{{1.0, 2.0}, {3.0, 4.0}});
            assertEquals(-2.0, det, EPS);
        }

        @Test
        void detIdentity() {
            double[][] I = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
            assertEquals(1.0, MatrixOps.determinant(I), EPS);
        }

        @Test
        void detSingular() {
            double[][] M = {{1, 2}, {2, 4}};
            assertEquals(0.0, MatrixOps.determinant(M), EPS);
        }
    }

    // -----------------------------------------------------------------------
    // Spanning tree count tests
    // -----------------------------------------------------------------------

    @Nested
    class SpanningTreeCountTests {

        @Test
        void endType() {
            // Single state: tau = 1 (trivial tree)
            assertEquals(1, KirchhoffChecker.spanningTreeCount(ss("end")));
        }

        @Test
        void singleEdge() {
            // K2: tau = 1
            assertEquals(1, KirchhoffChecker.spanningTreeCount(ss("&{a: end}")));
        }

        @Test
        void chain3() {
            // P3 (path of 3): tau = 1 (already a tree)
            assertEquals(1, KirchhoffChecker.spanningTreeCount(ss("&{a: &{b: end}}")));
        }

        @Test
        void chain4() {
            // P4: tau = 1
            assertEquals(1, KirchhoffChecker.spanningTreeCount(ss("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void parallel4cycle() {
            // (&{a:end} || &{b:end}): Hasse is C4, tau = 4
            int tau = KirchhoffChecker.spanningTreeCount(ss("(&{a: end} || &{b: end})"));
            assertEquals(4, tau);
        }

        @ParameterizedTest
        @CsvSource({
                "&{a: end}",
                "&{a: &{b: end}}",
                "(&{a: end} || &{b: end})"
        })
        void cofactorMatchesEigenvalue(String typ) {
            StateSpace s = ss(typ);
            double tauC = KirchhoffChecker.spanningTreeCountCofactor(s);
            double tauE = KirchhoffChecker.spanningTreeCountEigenvalue(s);
            assertEquals(tauC, tauE, 1.0,
                    "Cofactor=" + tauC + " vs eigenvalue=" + tauE + " for " + typ);
        }
    }

    // -----------------------------------------------------------------------
    // Tree detection tests
    // -----------------------------------------------------------------------

    @Nested
    class IsTreeTests {

        @Test
        void chainIsTree() {
            assertTrue(KirchhoffChecker.isTree(ss("&{a: end}")));
            assertTrue(KirchhoffChecker.isTree(ss("&{a: &{b: end}}")));
        }

        @Test
        void cycleNotTree() {
            assertFalse(KirchhoffChecker.isTree(ss("(&{a: end} || &{b: end})")));
        }
    }

    // -----------------------------------------------------------------------
    // Normalized count tests
    // -----------------------------------------------------------------------

    @Nested
    class NormalizedTests {

        @Test
        void chainNormalized() {
            // Chain of n: tau=1, ratio = 1/n^{n-2}
            StateSpace s = ss("&{a: &{b: end}}");
            double norm = KirchhoffChecker.normalizedTreeCount(s);
            assertTrue(norm > 0 && norm <= 1.0);
        }

        @Test
        void parallelNormalized() {
            StateSpace s = ss("(&{a: end} || &{b: end})");
            double norm = KirchhoffChecker.normalizedTreeCount(s);
            // C4: tau=4, Cayley(4)=16, ratio=0.25
            assertEquals(0.25, norm, 0.1);
        }
    }

    // -----------------------------------------------------------------------
    // Complexity ratio tests
    // -----------------------------------------------------------------------

    @Nested
    class ComplexityRatioTests {

        @Test
        void chainRatio() {
            // Chain of 3: tau=1, |E|=2, ratio=0.5
            StateSpace s = ss("&{a: &{b: end}}");
            double cr = KirchhoffChecker.complexityRatio(s);
            assertEquals(0.5, cr, 0.01);
        }

        @Test
        void parallelRatio() {
            // C4: tau=4, |E|=4, ratio=1.0
            StateSpace s = ss("(&{a: end} || &{b: end})");
            double cr = KirchhoffChecker.complexityRatio(s);
            assertEquals(1.0, cr, 0.1);
        }
    }

    // -----------------------------------------------------------------------
    // Composition tests
    // -----------------------------------------------------------------------

    @Nested
    class CompositionTests {

        @Test
        void productHasMoreTrees() {
            StateSpace s1 = ss("&{a: end}");
            StateSpace s2 = ss("&{b: end}");
            StateSpace sProd = ss("(&{a: end} || &{b: end})");
            assertTrue(KirchhoffChecker.verifyTreeCountProduct(s1, s2, sProd));
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis tests
    // -----------------------------------------------------------------------

    @Nested
    class AnalyzeTests {

        @Test
        void analyzeEnd() {
            var r = KirchhoffChecker.analyzeKirchhoff(ss("end"));
            assertEquals(1, r.numStates());
            assertEquals(1, r.spanningTreeCount());
            assertTrue(r.isTree());
        }

        @Test
        void analyzeChain() {
            var r = KirchhoffChecker.analyzeKirchhoff(ss("&{a: end}"));
            assertEquals(1, r.spanningTreeCount());
            assertTrue(r.isTree());
        }

        @Test
        void analyzeParallel() {
            var r = KirchhoffChecker.analyzeKirchhoff(ss("(&{a: end} || &{b: end})"));
            assertEquals(4, r.spanningTreeCount());
            assertFalse(r.isTree());
        }
    }

    // -----------------------------------------------------------------------
    // Benchmark tests
    // -----------------------------------------------------------------------

    @Nested
    class BenchmarkTests {

        @ParameterizedTest
        @CsvSource({
                "File Object,         '&{open: &{read: end, write: end}}'",
                "Simple SMTP,         '&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "Two-choice,          '&{a: end, b: end}'",
                "Nested,              '&{a: &{b: &{c: end}}}'",
                "Parallel,            '(&{a: end} || &{b: end})'"
        })
        void kirchhoffProperties(String name, String typ) {
            StateSpace s = ss(typ);
            var r = KirchhoffChecker.analyzeKirchhoff(s);
            assertTrue(r.spanningTreeCount() >= 1,
                    name + ": tau=" + r.spanningTreeCount());
            assertTrue(r.normalizedCount() >= 0,
                    name + ": norm=" + r.normalizedCount());
            assertTrue(r.normalizedCount() <= 1.01,
                    name + ": norm=" + r.normalizedCount());
        }

        @ParameterizedTest
        @CsvSource({
                "File Object,         '&{open: &{read: end, write: end}}'",
                "Simple SMTP,         '&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "Two-choice,          '&{a: end, b: end}'",
                "Nested,              '&{a: &{b: &{c: end}}}'",
                "Parallel,            '(&{a: end} || &{b: end})'"
        })
        void methodsAgree(String name, String typ) {
            StateSpace s = ss(typ);
            double tauC = KirchhoffChecker.spanningTreeCountCofactor(s);
            double tauE = KirchhoffChecker.spanningTreeCountEigenvalue(s);
            assertEquals(tauC, tauE, 1.0,
                    name + ": cofactor=" + tauC + " vs eigenvalue=" + tauE);
        }

        @ParameterizedTest
        @CsvSource({
                "File Object,         '&{open: &{read: end, write: end}}'",
                "Simple SMTP,         '&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "Two-choice,          '&{a: end, b: end}'",
                "Nested,              '&{a: &{b: &{c: end}}}'",
                "Parallel,            '(&{a: end} || &{b: end})'"
        })
        void treeIffNoCycles(String name, String typ) {
            StateSpace s = ss(typ);
            var r = KirchhoffChecker.analyzeKirchhoff(s);
            boolean expectedTree = (r.numEdges() == r.numStates() - 1);
            assertEquals(expectedTree, r.isTree(),
                    name + ": isTree=" + r.isTree() + ", edges=" + r.numEdges()
                            + ", states=" + r.numStates());
        }
    }
}
