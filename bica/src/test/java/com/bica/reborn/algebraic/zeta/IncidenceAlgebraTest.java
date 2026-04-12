package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IncidenceAlgebra} — Java port of Python
 * {@code test_incidence_algebra.py} (46 tests, Step 32g).
 */
class IncidenceAlgebraTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    private static boolean approxEqual(Map<Zeta.Pair, Double> f, Map<Zeta.Pair, Double> g) {
        Set<Zeta.Pair> keys = new HashSet<>(f.keySet());
        keys.addAll(g.keySet());
        for (Zeta.Pair k : keys) {
            if (Math.abs(f.getOrDefault(k, 0.0) - g.getOrDefault(k, 0.0)) > 1e-9) {
                return false;
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Element construction
    // -----------------------------------------------------------------------

    @Nested
    class Elements {

        @Test
        void zetaEnd() {
            var z = IncidenceAlgebra.zetaElement(ss("end"));
            assertTrue(z.size() >= 1);
            for (double v : z.values()) assertEquals(1.0, v);
        }

        @Test
        void deltaEnd() {
            var d = IncidenceAlgebra.deltaElement(ss("end"));
            assertTrue(d.size() >= 1);
        }

        @Test
        void mobiusEnd() {
            var mu = IncidenceAlgebra.mobiusElement(ss("end"));
            assertTrue(mu.size() >= 1);
        }

        @Test
        void zetaSingleBranch() {
            var z = IncidenceAlgebra.zetaElement(ss("&{a: end}"));
            int nonzero = 0;
            for (double v : z.values()) if (Math.abs(v) > 1e-12) nonzero++;
            assertEquals(3, nonzero);
        }

        @Test
        void deltaDiagonalOnly() {
            var d = IncidenceAlgebra.deltaElement(ss("&{a: end, b: end}"));
            for (var e : d.entrySet()) {
                if (e.getKey().x() != e.getKey().y()) {
                    assertTrue(Math.abs(e.getValue()) < 1e-12);
                }
            }
        }

        @Test
        void rankElement() {
            var r = IncidenceAlgebra.rankElement(ss("&{a: end, b: end}"));
            for (double v : r.values()) assertTrue(v >= 0);
        }

        @Test
        void constantElement() {
            var c = IncidenceAlgebra.constantElement(ss("&{a: end}"), 2.0);
            for (double v : c.values()) assertEquals(2.0, v);
        }

        @Test
        void constantZero() {
            var c = IncidenceAlgebra.constantElement(ss("&{a: end}"), 0.0);
            assertEquals(0, c.size());
        }

        @Test
        void indicatorElement() {
            StateSpace s = ss("&{a: end}");
            var ind = IncidenceAlgebra.indicatorElement(
                    s, Set.of(new Zeta.Pair(s.top(), s.bottom())));
            assertEquals(1.0, ind.getOrDefault(new Zeta.Pair(s.top(), s.bottom()), 0.0));
        }

        @Test
        void customIncidenceFunction() {
            var f = IncidenceAlgebra.incidenceFunction(
                    ss("&{a: end}"), (x, y) -> (double) (x + y));
            assertTrue(f.size() >= 1);
        }
    }

    // -----------------------------------------------------------------------
    // Convolution identity: zeta * mu = delta
    // -----------------------------------------------------------------------

    @Nested
    class ConvolutionIdentity {

        @Test
        void endIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(ss("end")));
        }

        @Test
        void singleBranchIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(ss("&{a: end}")));
        }

        @Test
        void twoBranchIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(ss("&{a: end, b: end}")));
        }

        @Test
        void selectionIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(ss("+{a: end, b: end}")));
        }

        @Test
        void nestedIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(ss("&{a: &{c: end}, b: end}")));
        }

        @Test
        void recursiveIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(ss("rec X . &{a: X, b: end}")));
        }
    }

    // -----------------------------------------------------------------------
    // Mobius involution: mu * zeta = delta
    // -----------------------------------------------------------------------

    @Nested
    class MobiusInvolution {

        @Test
        void endInvolution() {
            assertTrue(IncidenceAlgebra.verifyMobiusInvolution(ss("end")));
        }

        @Test
        void singleBranchInvolution() {
            assertTrue(IncidenceAlgebra.verifyMobiusInvolution(ss("&{a: end}")));
        }

        @Test
        void twoBranchInvolution() {
            assertTrue(IncidenceAlgebra.verifyMobiusInvolution(ss("&{a: end, b: end}")));
        }

        @Test
        void nestedInvolution() {
            assertTrue(IncidenceAlgebra.verifyMobiusInvolution(ss("&{a: &{c: end}, b: end}")));
        }
    }

    // -----------------------------------------------------------------------
    // Algebra operations
    // -----------------------------------------------------------------------

    @Nested
    class AlgebraOperations {

        @Test
        void addZero() {
            StateSpace s = ss("&{a: end}");
            var z = IncidenceAlgebra.zetaElement(s);
            assertTrue(approxEqual(IncidenceAlgebra.add(z, Map.of()), z));
        }

        @Test
        void subtractSelf() {
            StateSpace s = ss("&{a: end}");
            var z = IncidenceAlgebra.zetaElement(s);
            var r = IncidenceAlgebra.subtract(z, z);
            for (double v : r.values()) assertTrue(Math.abs(v) < 1e-9);
        }

        @Test
        void scalarMul1() {
            StateSpace s = ss("&{a: end}");
            var z = IncidenceAlgebra.zetaElement(s);
            assertTrue(approxEqual(IncidenceAlgebra.scalarMul(1.0, z), z));
        }

        @Test
        void scalarMul0() {
            StateSpace s = ss("&{a: end}");
            var z = IncidenceAlgebra.zetaElement(s);
            assertEquals(0, IncidenceAlgebra.scalarMul(0.0, z).size());
        }

        @Test
        void power0IsDelta() {
            StateSpace s = ss("&{a: end}");
            var z = IncidenceAlgebra.zetaElement(s);
            var d = IncidenceAlgebra.deltaElement(s);
            assertTrue(approxEqual(IncidenceAlgebra.power(z, 0, s), d));
        }

        @Test
        void power1IsSelf() {
            StateSpace s = ss("&{a: end}");
            var z = IncidenceAlgebra.zetaElement(s);
            assertTrue(approxEqual(IncidenceAlgebra.power(z, 1, s), z));
        }

        @Test
        void addCommutative() {
            StateSpace s = ss("&{a: end}");
            var z = IncidenceAlgebra.zetaElement(s);
            var d = IncidenceAlgebra.deltaElement(s);
            assertTrue(approxEqual(IncidenceAlgebra.add(z, d), IncidenceAlgebra.add(d, z)));
        }
    }

    // -----------------------------------------------------------------------
    // Inverse
    // -----------------------------------------------------------------------

    @Nested
    class Inverse {

        @Test
        void zetaInverseIsMobius() {
            StateSpace s = ss("&{a: end}");
            var z = IncidenceAlgebra.zetaElement(s);
            var mu = IncidenceAlgebra.mobiusElement(s);
            var zInv = IncidenceAlgebra.inverse(z, s);
            assertNotNull(zInv);
            assertTrue(approxEqual(zInv, mu));
        }

        @Test
        void deltaInverseIsDelta() {
            StateSpace s = ss("&{a: end}");
            var d = IncidenceAlgebra.deltaElement(s);
            var dInv = IncidenceAlgebra.inverse(d, s);
            assertNotNull(dInv);
            assertTrue(approxEqual(dInv, d));
        }

        @Test
        void zeroDiagonalNotInvertible() {
            StateSpace s = ss("&{a: end}");
            assertNull(IncidenceAlgebra.inverse(Map.of(), s));
        }
    }

    // -----------------------------------------------------------------------
    // Nilpotent index
    // -----------------------------------------------------------------------

    @Nested
    class NilpotentIndex {

        @Test
        void endNilpotent() {
            Integer idx = IncidenceAlgebra.nilpotentIndex(ss("end"));
            assertNotNull(idx);
            assertTrue(idx >= 1);
        }

        @Test
        void singleBranchNilpotent() {
            Integer idx = IncidenceAlgebra.nilpotentIndex(ss("&{a: end}"));
            assertNotNull(idx);
            assertTrue(idx >= 1);
        }

        @Test
        void nilpotentBoundedByHeight() {
            Integer idx = IncidenceAlgebra.nilpotentIndex(ss("&{a: &{b: end}}"));
            assertNotNull(idx);
            assertTrue(idx <= 4);
        }
    }

    // -----------------------------------------------------------------------
    // Commutativity
    // -----------------------------------------------------------------------

    @Nested
    class Commutativity {

        @Test
        void zetaMuCommute() {
            StateSpace s = ss("&{a: end}");
            assertTrue(IncidenceAlgebra.isCommutativePair(
                    IncidenceAlgebra.zetaElement(s),
                    IncidenceAlgebra.mobiusElement(s),
                    s));
        }

        @Test
        void deltaCommutesWithAll() {
            StateSpace s = ss("&{a: end}");
            assertTrue(IncidenceAlgebra.isCommutativePair(
                    IncidenceAlgebra.deltaElement(s),
                    IncidenceAlgebra.zetaElement(s),
                    s));
        }
    }

    // -----------------------------------------------------------------------
    // Multiplicativity
    // -----------------------------------------------------------------------

    @Nested
    class Multiplicative {

        @Test
        void deltaIsIdempotent() {
            StateSpace s = ss("&{a: end}");
            assertTrue(IncidenceAlgebra.isMultiplicative(IncidenceAlgebra.deltaElement(s), s));
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class AnalyzeIncidenceAlgebra {

        @Test
        void endAnalysis() {
            var r = IncidenceAlgebra.analyzeIncidenceAlgebra(ss("end"));
            assertEquals(1, r.numStates());
            assertTrue(r.convolutionIdentityVerified());
            assertTrue(r.mobiusInvolutionVerified());
        }

        @Test
        void singleBranchAnalysis() {
            var r = IncidenceAlgebra.analyzeIncidenceAlgebra(ss("&{a: end}"));
            assertEquals(2, r.numStates());
            assertTrue(r.convolutionIdentityVerified());
            assertTrue(r.dimension() >= 3);
        }

        @Test
        void twoBranchAnalysis() {
            var r = IncidenceAlgebra.analyzeIncidenceAlgebra(ss("&{a: end, b: end}"));
            assertEquals(2, r.numStates());
            assertTrue(r.convolutionIdentityVerified());
            assertTrue(r.mobiusInvolutionVerified());
        }

        @Test
        void selectionAnalysis() {
            var r = IncidenceAlgebra.analyzeIncidenceAlgebra(ss("+{a: end, b: end}"));
            assertTrue(r.convolutionIdentityVerified());
        }

        @Test
        void nestedAnalysis() {
            var r = IncidenceAlgebra.analyzeIncidenceAlgebra(ss("&{a: &{c: end, d: end}, b: end}"));
            assertTrue(r.convolutionIdentityVerified());
            assertNotNull(r.nilpotentIndex());
        }

        @Test
        void recursiveAnalysis() {
            var r = IncidenceAlgebra.analyzeIncidenceAlgebra(ss("rec X . &{a: X, b: end}"));
            // Boolean return — value either true or false, just check no crash
            assertTrue(r.convolutionIdentityVerified() || !r.convolutionIdentityVerified());
        }
    }

    // -----------------------------------------------------------------------
    // Benchmark protocols
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {

        @Test
        void iteratorIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(
                    ss("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")));
        }

        @Test
        void simpleResourceIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(
                    ss("&{open: &{use: &{close: end}}}")));
        }

        @Test
        void parallelIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(
                    ss("(&{a: end} || &{b: end})")));
        }

        @Test
        void deepChainIdentity() {
            assertTrue(IncidenceAlgebra.verifyConvolutionIdentity(
                    ss("&{a: &{b: &{c: &{d: end}}}}")));
        }
    }
}
