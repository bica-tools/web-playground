package com.bica.reborn.crapo_beta;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.crapo_beta.CrapoBetaChecker.BetaResult;
import com.bica.reborn.crapo_beta.CrapoBetaChecker.BetaTerm;
import com.bica.reborn.crapo_beta.CrapoBetaChecker.PsiWitness;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_crapo_beta.py}.
 */
class CrapoBetaCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    private static int mu01(StateSpace s) {
        int[][] M = AlgebraicChecker.computeMobiusMatrix(s);
        List<Integer> states = AlgebraicChecker.stateList(s);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);
        return M[idx.get(s.top())][idx.get(s.bottom())];
    }

    // ----- Core beta -----

    @Test
    void trivialSingleState() {
        BetaResult r = CrapoBetaChecker.analyzeBeta(ss("end"));
        assertTrue(r.trivial());
        assertEquals(0, r.beta());
        assertTrue(r.decomposable());
    }

    @Test
    void twoChain() {
        BetaResult r = CrapoBetaChecker.analyzeBeta(ss("&{a: end}"));
        assertEquals(2, r.numStates());
        assertEquals(-2, r.beta());
        assertFalse(r.decomposable());
    }

    @Test
    void threeChain() {
        assertEquals(1, CrapoBetaChecker.computeBeta(ss("&{a: &{b: end}}")));
    }

    @Test
    void fourChainIsZero() {
        assertEquals(0, CrapoBetaChecker.computeBeta(ss("&{a: &{b: &{c: end}}}")));
    }

    @Test
    void betaIsInteger() {
        for (String t : List.of("end", "&{a: end}", "&{a: end, b: end}",
                "(&{a: end} || &{b: end})", "&{a: &{b: end}}")) {
            // Just exercise the codepath -- int return type guarantees integrality.
            CrapoBetaChecker.computeBeta(ss(t));
        }
    }

    // ----- Multiplicativity -----

    @Test
    void productOfTwoChains() {
        StateSpace l1 = ss("&{a: end}");
        StateSpace l2 = ss("&{b: end}");
        StateSpace prod = ss("(&{a: end} || &{b: end})");
        assertEquals(
                CrapoBetaChecker.computeBeta(l1) * CrapoBetaChecker.computeBeta(l2),
                CrapoBetaChecker.computeBeta(prod));
    }

    @Test
    void productSquare() {
        StateSpace prod = ss("(&{a: end} || &{b: end})");
        StateSpace single = ss("&{a: end}");
        int b = CrapoBetaChecker.computeBeta(single);
        assertEquals(b * b, CrapoBetaChecker.computeBeta(prod));
    }

    @Test
    void productWithChain() {
        StateSpace prod = ss("(&{a: &{b: end}} || &{c: end})");
        StateSpace c3 = ss("&{a: &{b: end}}");
        StateSpace c2 = ss("&{c: end}");
        assertEquals(
                CrapoBetaChecker.computeBeta(c3) * CrapoBetaChecker.computeBeta(c2),
                CrapoBetaChecker.computeBeta(prod));
    }

    // ----- Decomposability detector -----

    @Test
    void trivialDecomposable() {
        assertTrue(CrapoBetaChecker.analyzeBeta(ss("end")).decomposable());
    }

    @Test
    void singleMethodNotDecomposable() {
        assertFalse(CrapoBetaChecker.analyzeBeta(ss("&{a: end}")).decomposable());
    }

    @Test
    void longChainDecomposable() {
        assertTrue(CrapoBetaChecker.analyzeBeta(ss("&{a: &{b: &{c: end}}}")).decomposable());
    }

    @Test
    void fanBranch() {
        BetaResult r = CrapoBetaChecker.analyzeBeta(ss("&{a: end, b: end, c: end}"));
        assertNotNull(r);
    }

    @Test
    void selectionLattice() {
        BetaResult r = CrapoBetaChecker.analyzeBeta(ss("+{x: end, y: end}"));
        assertEquals(2, r.numStates());
        assertEquals(-2, r.beta());
    }

    // ----- Contributing terms -----

    @Test
    void termsNonzero() {
        BetaResult r = CrapoBetaChecker.analyzeBeta(ss("&{a: end}"));
        for (BetaTerm t : r.contributingTerms()) {
            assertNotEquals(0, t.product());
            assertEquals(t.muBotX() * t.muXTop(), t.product());
        }
    }

    @Test
    void termsSumToBeta() {
        BetaResult r = CrapoBetaChecker.analyzeBeta(ss("(&{a: end} || &{b: end})"));
        int sum = 0;
        for (BetaTerm t : r.contributingTerms()) sum += t.product();
        assertEquals(r.beta(), sum);
    }

    // ----- Mobius cross-check -----

    @Test
    void mu01Recorded() {
        StateSpace s = ss("&{a: end}");
        BetaResult r = CrapoBetaChecker.analyzeBeta(s);
        assertEquals(mu01(s), r.mobius01());
    }

    @Test
    void muEndpointsContribute() {
        StateSpace s = ss("&{a: end}");
        int m = mu01(s);
        assertEquals(CrapoBetaChecker.computeBeta(s), 2 * m);
    }

    // ----- Bidirectional morphism -----

    @Test
    void phiTrivial() {
        assertEquals("trivial", CrapoBetaChecker.phiDecomposable(ss("end")));
    }

    @Test
    void phiConnected() {
        assertEquals("connected", CrapoBetaChecker.phiDecomposable(ss("&{a: end}")));
    }

    @Test
    void phiDecomposableChain() {
        assertEquals("decomposable",
                CrapoBetaChecker.phiDecomposable(ss("&{a: &{b: &{c: end}}}")));
    }

    @Test
    void psiRoundtripConnected() {
        PsiWitness w = CrapoBetaChecker.psiBound("connected");
        assertNull(w.betaBound());
        assertTrue(w.description().contains("irreducible"));
    }

    @Test
    void psiRoundtripDecomposable() {
        assertEquals(0, CrapoBetaChecker.psiBound("decomposable").betaBound());
    }

    @Test
    void psiTrivial() {
        assertEquals(0, CrapoBetaChecker.psiBound("trivial").betaBound());
    }

    @Test
    void classifyPairReturnsGalois() {
        for (String t : List.of("end", "&{a: end}", "&{a: &{b: end}}")) {
            assertEquals("galois", CrapoBetaChecker.classifyPair(ss(t)));
        }
    }

    @Test
    void phiPsiRoundtripOnClasses() {
        for (String t : List.of("end", "&{a: end}", "&{a: &{b: &{c: end}}}",
                "(&{a: end} || &{b: end})")) {
            StateSpace s = ss(t);
            String c = CrapoBetaChecker.phiDecomposable(s);
            PsiWitness w = CrapoBetaChecker.psiBound(c);
            if (w.betaBound() != null && w.betaBound() == 0 && c.equals("decomposable")) {
                assertEquals(0, CrapoBetaChecker.computeBeta(s));
            }
        }
    }

    // ----- Benchmark batch -----

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{open: &{close: end}}",
            "&{login: +{ok: &{logout: end}, err: end}}",
            "(&{a: end} || &{b: end})",
            "rec X . &{next: X, done: end}",
            "&{a: end, b: &{c: end}}",
    })
    void betaComputes(String typeString) {
        BetaResult r = CrapoBetaChecker.analyzeBeta(ss(typeString));
        assertNotNull(r);
        if (!r.trivial()) {
            assertEquals(r.beta() == 0, r.decomposable());
        }
    }
}
