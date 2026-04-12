package com.bica.reborn.session_zeta;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.session_zeta.SessionZetaChecker.EulerFactorization;
import com.bica.reborn.session_zeta.SessionZetaChecker.EulerVerification;
import com.bica.reborn.session_zeta.SessionZetaChecker.PointCheck;
import com.bica.reborn.session_zeta.SessionZetaChecker.Rational;
import com.bica.reborn.session_zeta.SessionZetaChecker.ZetaSeries;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_session_zeta.py}.
 */
class SessionZetaCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // -------------------- downset sizes --------------------

    @Test
    void downsetSizesEnd() {
        assertEquals(List.of(1), SessionZetaChecker.downsetSizes(ss("end")));
    }

    @Test
    void downsetSizesSingleBranch() {
        assertEquals(List.of(1, 2), SessionZetaChecker.downsetSizes(ss("&{a: end}")));
    }

    @Test
    void downsetSizesBinaryBranch() {
        StateSpace s = ss("&{a: end, b: end}");
        List<Integer> sizes = SessionZetaChecker.downsetSizes(s);
        assertTrue(sizes.contains(1));
        int max = 0;
        for (int v : sizes) if (v > max) max = v;
        assertEquals(s.states().size(), max);
    }

    @Test
    void downsetSizesChain() {
        assertEquals(List.of(1, 2, 3),
                SessionZetaChecker.downsetSizes(ss("&{a: &{b: end}}")));
    }

    // -------------------- ZetaSeries basics --------------------

    @Test
    void zetaSeriesFromSizes() {
        ZetaSeries z = ZetaSeries.fromSizes(List.of(1, 2, 2, 3));
        assertEquals(Map.of(1, 1, 2, 2, 3, 1), z.asMap());
        assertEquals(4, z.totalMass());
    }

    @Test
    void zetaSeriesEvaluateAt1() {
        ZetaSeries z = ZetaSeries.fromSizes(List.of(1, 2, 4));
        assertEquals(Rational.of(7, 4), z.evaluate(1));
    }

    @Test
    void zetaSeriesEvaluateAt2() {
        ZetaSeries z = ZetaSeries.fromSizes(List.of(1, 2));
        assertEquals(Rational.of(1).add(Rational.of(1, 4)), z.evaluate(2));
    }

    @Test
    void zetaSeriesEvaluateAt0IsCardinality() {
        ZetaSeries z = ZetaSeries.fromSizes(List.of(1, 2, 3));
        assertEquals(Rational.of(3), z.evaluate(0));
    }

    @Test
    void zetaSeriesMultiplicationCommutative() {
        ZetaSeries a = ZetaSeries.fromSizes(List.of(1, 2));
        ZetaSeries b = ZetaSeries.fromSizes(List.of(1, 3));
        assertEquals(b.multiply(a), a.multiply(b));
    }

    @Test
    void zetaSeriesMultiplicationDirichlet() {
        ZetaSeries a = ZetaSeries.fromSizes(List.of(1, 2));
        ZetaSeries b = ZetaSeries.fromSizes(List.of(1, 3));
        assertEquals(Map.of(1, 1, 2, 1, 3, 1, 6, 1), a.multiply(b).asMap());
    }

    @Test
    void zetaSeriesEqualityIgnoresOrder() {
        assertEquals(ZetaSeries.fromSizes(List.of(3, 2, 1)),
                ZetaSeries.fromSizes(List.of(1, 2, 3)));
    }

    // -------------------- session_zeta --------------------

    @Test
    void sessionZetaEnd() {
        assertEquals(Rational.of(1), SessionZetaChecker.sessionZeta(ss("end"), 1));
    }

    @Test
    void sessionZetaBinaryBranch() {
        assertEquals(Rational.of(3, 2),
                SessionZetaChecker.sessionZeta(ss("&{a: end, b: end}"), 1));
    }

    @Test
    void sessionZetaAtZeroIsCardinality() {
        StateSpace s = ss("&{a: &{b: end, c: end}}");
        assertEquals(Rational.of(s.states().size()),
                SessionZetaChecker.sessionZeta(s, 0));
    }

    // -------------------- Euler product --------------------

    @Test
    void eulerProductSimpleParallel() {
        EulerVerification r = SessionZetaChecker.verifyEulerProduct(
                ss("&{a: end}"), ss("&{b: end}"), ss("(&{a: end} || &{b: end})"));
        assertTrue(r.seriesEqual());
        assertTrue(r.allPointsEqual());
    }

    @Test
    void eulerProductBranchingParallel() {
        EulerVerification r = SessionZetaChecker.verifyEulerProduct(
                ss("&{a: end, b: end}"), ss("&{x: end}"),
                ss("(&{a: end, b: end} || &{x: end})"));
        assertTrue(r.seriesEqual());
        assertTrue(r.allPointsEqual());
    }

    @Test
    void eulerProductBothBranching() {
        EulerVerification r = SessionZetaChecker.verifyEulerProduct(
                ss("&{a: end, b: end}"), ss("&{c: end, d: end}"),
                ss("(&{a: end, b: end} || &{c: end, d: end})"));
        assertTrue(r.seriesEqual());
        assertTrue(r.allPointsEqual());
        int lMax = r.leftSizes().get(r.leftSizes().size() - 1);
        int rMax = r.rightSizes().get(r.rightSizes().size() - 1);
        int pMax = r.productSizes().get(r.productSizes().size() - 1);
        assertEquals(lMax * rMax, pMax);
    }

    @Test
    void eulerProductDeepChainParallel() {
        EulerVerification r = SessionZetaChecker.verifyEulerProduct(
                ss("&{a: &{b: end}}"), ss("&{x: end}"),
                ss("(&{a: &{b: end}} || &{x: end})"));
        assertTrue(r.seriesEqual());
    }

    @Test
    void eulerProductPointEvaluations() {
        EulerVerification r = SessionZetaChecker.verifyEulerProduct(
                ss("&{a: end, b: end}"), ss("&{x: end}"),
                ss("(&{a: end, b: end} || &{x: end})"),
                new int[]{1, 2, 3, 5});
        for (PointCheck p : r.pointChecks()) {
            assertTrue(p.ok(), "mismatch at s=" + p.s() + ": " + p.lhs() + " vs " + p.rhs());
        }
    }

    // -------------------- irreducibility --------------------

    @Test
    void endIsIrreducible() {
        assertTrue(SessionZetaChecker.isZetaIrreducible(ss("end")));
    }

    @Test
    void singleMethodIsIrreducible() {
        assertTrue(SessionZetaChecker.isZetaIrreducible(ss("&{a: end}")));
    }

    @Test
    void chainThreeIrreducible() {
        assertTrue(SessionZetaChecker.isZetaIrreducible(ss("&{a: &{b: end}}")));
    }

    @Test
    void binaryBranchIrreducible() {
        assertTrue(SessionZetaChecker.isZetaIrreducible(ss("&{a: end, b: end}")));
    }

    @Test
    void parallelReducibleWhenBothNontrivial() {
        assertFalse(SessionZetaChecker.isZetaIrreducible(ss("(&{a: end} || &{b: end})")));
    }

    @Test
    void seriesIrreducibleSingleton() {
        assertTrue(SessionZetaChecker.seriesIrreducible(ZetaSeries.fromSizes(List.of(1))));
    }

    @Test
    void seriesReducibleProductOfTwoElement() {
        ZetaSeries z = ZetaSeries.fromSizes(List.of(1, 2))
                .multiply(ZetaSeries.fromSizes(List.of(1, 2)));
        assertFalse(SessionZetaChecker.seriesIrreducible(z));
    }

    @Test
    void compositionsBasic() {
        assertEquals(List.of(List.of(3)), SessionZetaChecker.compositions(3, 1));
        List<List<Integer>> twoParts = SessionZetaChecker.compositions(3, 2);
        assertEquals(2, twoParts.size());
        assertTrue(twoParts.contains(List.of(1, 2)));
        assertTrue(twoParts.contains(List.of(2, 1)));
        assertEquals(3, SessionZetaChecker.compositions(4, 2).size());
    }

    @Test
    void trySplitProduct() {
        ZetaSeries A = ZetaSeries.fromSizes(List.of(1, 2));
        ZetaSeries B = ZetaSeries.fromSizes(List.of(1, 3));
        ZetaSeries Z = A.multiply(B);
        ZetaSeries[] split = SessionZetaChecker.trySplit(Z, 2, 2);
        assertNotNull(split);
        assertEquals(Z, split[0].multiply(split[1]));
    }

    // -------------------- Euler factorization --------------------

    @Test
    void eulerFactorIrreducible() {
        EulerFactorization f = SessionZetaChecker.eulerFactor(ss("&{a: &{b: end}}"));
        assertTrue(f.matches());
        assertEquals(1, f.factors().size());
    }

    @Test
    void eulerFactorParallel() {
        EulerFactorization f = SessionZetaChecker.eulerFactor(ss("(&{a: end} || &{b: end})"));
        assertTrue(f.matches());
        assertTrue(f.factors().size() >= 2);
    }

    @Test
    void eulerFactorProductReconstructs() {
        EulerFactorization f = SessionZetaChecker.eulerFactor(
                ss("(&{a: end, b: end} || &{c: end})"));
        assertTrue(f.matches());
        assertEquals(f.original(), f.product());
    }

    @Test
    void tripleParallelEuler() {
        EulerFactorization f = SessionZetaChecker.eulerFactor(
                ss("((&{a: end} || &{b: end}) || &{c: end})"));
        assertTrue(f.matches());
        assertTrue(f.factors().size() >= 2);
    }

    @Test
    void sessionZetaMultiplicativeUnderParallelAtS2() {
        StateSpace left = ss("&{a: &{b: end}}");
        StateSpace right = ss("&{c: end, d: end}");
        StateSpace prod = ss("(&{a: &{b: end}} || &{c: end, d: end})");
        Rational lhs = SessionZetaChecker.sessionZeta(prod, 2);
        Rational rhs = SessionZetaChecker.sessionZeta(left, 2)
                .multiply(SessionZetaChecker.sessionZeta(right, 2));
        assertEquals(rhs, lhs);
    }
}
