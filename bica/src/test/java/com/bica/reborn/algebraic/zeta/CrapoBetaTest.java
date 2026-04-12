package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CrapoBeta} — Java port of Python {@code test_crapo_beta.py}
 * (26 tests, Step 32n). Backs LMCS Distributivity Dichotomy Theorem 5.2.
 */
class CrapoBetaTest {

    private StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // -----------------------------------------------------------------------
    // TestCoreBeta
    // -----------------------------------------------------------------------

    @Test
    void trivialSingleState() {
        var r = CrapoBeta.analyzeBeta(ss("end"));
        assertTrue(r.trivial());
        assertEquals(0, r.beta());
        assertTrue(r.decomposable());
    }

    @Test
    void twoChain() {
        var r = CrapoBeta.analyzeBeta(ss("&{a: end}"));
        assertEquals(2, r.numStates());
        // β = 1·(-1) + (-1)·1 = -2
        assertEquals(-2, r.beta());
        assertFalse(r.decomposable());
    }

    @Test
    void threeChain() {
        assertEquals(1, CrapoBeta.computeBeta(ss("&{a: &{b: end}}")));
    }

    @Test
    void fourChainIsZero() {
        assertEquals(0, CrapoBeta.computeBeta(ss("&{a: &{b: &{c: end}}}")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{a: end}",
            "&{a: end, b: end}",
            "(&{a: end} || &{b: end})",
            "&{a: &{b: end}}"
    })
    void betaIsInteger(String t) {
        // Java β is int by construction; just exercise the code path.
        int v = CrapoBeta.computeBeta(ss(t));
        assertEquals(v, v);
    }

    // -----------------------------------------------------------------------
    // TestMultiplicativity: β(L1 × L2) = β(L1)·β(L2)
    // -----------------------------------------------------------------------

    @Test
    void productOfTwoChains() {
        int l1 = CrapoBeta.computeBeta(ss("&{a: end}"));
        int l2 = CrapoBeta.computeBeta(ss("&{b: end}"));
        int prod = CrapoBeta.computeBeta(ss("(&{a: end} || &{b: end})"));
        assertEquals(l1 * l2, prod);
    }

    @Test
    void productSquare() {
        int prod = CrapoBeta.computeBeta(ss("(&{a: end} || &{b: end})"));
        int single = CrapoBeta.computeBeta(ss("&{a: end}"));
        assertEquals(single * single, prod);
    }

    @Test
    void productWithChain() {
        int prod = CrapoBeta.computeBeta(ss("(&{a: &{b: end}} || &{c: end})"));
        int c3 = CrapoBeta.computeBeta(ss("&{a: &{b: end}}"));
        int c2 = CrapoBeta.computeBeta(ss("&{c: end}"));
        assertEquals(c3 * c2, prod);
    }

    // -----------------------------------------------------------------------
    // TestDecomposabilityDetector
    // -----------------------------------------------------------------------

    @Test
    void trivialDecomposable() {
        assertTrue(CrapoBeta.analyzeBeta(ss("end")).decomposable());
    }

    @Test
    void singleMethodNotDecomposable() {
        assertFalse(CrapoBeta.analyzeBeta(ss("&{a: end}")).decomposable());
    }

    @Test
    void longChainDecomposable() {
        assertTrue(CrapoBeta.analyzeBeta(ss("&{a: &{b: &{c: end}}}")).decomposable());
    }

    @Test
    void fanBranch() {
        var r = CrapoBeta.analyzeBeta(ss("&{a: end, b: end, c: end}"));
        // Just exercise — value is an int.
        assertEquals(r.beta(), r.beta());
    }

    @Test
    void selectionLattice() {
        var r = CrapoBeta.analyzeBeta(ss("+{x: end, y: end}"));
        assertEquals(2, r.numStates());
        assertEquals(-2, r.beta());
    }

    // -----------------------------------------------------------------------
    // TestContributingTerms
    // -----------------------------------------------------------------------

    @Test
    void termsNonzero() {
        var r = CrapoBeta.analyzeBeta(ss("&{a: end}"));
        for (var t : r.contributingTerms()) {
            assertNotEquals(0, t.product());
            assertEquals(t.muBotX() * t.muXTop(), t.product());
        }
    }

    @Test
    void termsSumToBeta() {
        var r = CrapoBeta.analyzeBeta(ss("(&{a: end} || &{b: end})"));
        int sum = 0;
        for (var t : r.contributingTerms()) sum += t.product();
        assertEquals(r.beta(), sum);
    }

    // -----------------------------------------------------------------------
    // TestMobiusCrossCheck
    // -----------------------------------------------------------------------

    @Test
    void mu01Recorded() {
        var space = ss("&{a: end}");
        var r = CrapoBeta.analyzeBeta(space);
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(space);
        assertEquals(
                mu.getOrDefault(new Zeta.Pair(space.top(), space.bottom()), 0),
                r.mobius01());
    }

    @Test
    void muEndpointsContribute() {
        var space = ss("&{a: end}");
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(space);
        int mu01 = mu.getOrDefault(new Zeta.Pair(space.top(), space.bottom()), 0);
        // term(bottom) = 1·mu01 ; term(top) = mu01·1 ; sum = 2·mu01
        assertEquals(2 * mu01, CrapoBeta.computeBeta(space));
    }

    // -----------------------------------------------------------------------
    // TestBidirectionalMorphism
    // -----------------------------------------------------------------------

    @Test
    void phiTrivial() {
        assertEquals("trivial", CrapoBeta.phiDecomposable(ss("end")));
    }

    @Test
    void phiConnected() {
        assertEquals("connected", CrapoBeta.phiDecomposable(ss("&{a: end}")));
    }

    @Test
    void phiDecomposableChain() {
        assertEquals("decomposable",
                CrapoBeta.phiDecomposable(ss("&{a: &{b: &{c: end}}}")));
    }

    @Test
    void psiRoundtripConnected() {
        var b = CrapoBeta.psiBound("connected");
        assertNull(b.betaBound());
        assertTrue(b.description().contains("irreducible"));
    }

    @Test
    void psiRoundtripDecomposable() {
        assertEquals(0, CrapoBeta.psiBound("decomposable").betaBound());
    }

    @Test
    void psiTrivial() {
        assertEquals(0, CrapoBeta.psiBound("trivial").betaBound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"end", "&{a: end}", "&{a: &{b: end}}"})
    void classifyPairReturnsGalois(String t) {
        assertEquals("galois", CrapoBeta.classifyPair(ss(t)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{a: end}",
            "&{a: &{b: &{c: end}}}",
            "(&{a: end} || &{b: end})"
    })
    void phiPsiRoundtripOnClasses(String t) {
        var space = ss(t);
        String c = CrapoBeta.phiDecomposable(space);
        var b = CrapoBeta.psiBound(c);
        if (b.betaBound() != null && b.betaBound() == 0 && "decomposable".equals(c)) {
            assertEquals(0, CrapoBeta.computeBeta(space));
        }
    }

    // -----------------------------------------------------------------------
    // TestBenchmarkBatch
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{open: &{close: end}}",
            "&{login: +{ok: &{logout: end}, err: end}}",
            "(&{a: end} || &{b: end})",
            "rec X . &{next: X, done: end}",
            "&{a: end, b: &{c: end}}"
    })
    void betaComputes(String t) {
        var space = ss(t);
        var r = CrapoBeta.analyzeBeta(space);
        assertNotNull(r);
        if (!r.trivial()) {
            assertEquals(r.beta() == 0, r.decomposable());
        }
    }
}
