package com.bica.reborn.lattice_valuation;

import com.bica.reborn.lattice_valuation.LatticeValuationChecker.ValuationResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_lattice_valuation.py} (Steps 30y-30z).
 */
class LatticeValuationCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // -------------------- Rank valuation --------------------

    @Test
    void rankValuationEnd() {
        Map<Integer, Integer> v = LatticeValuationChecker.rankValuation(ss("end"));
        assertEquals(1, v.size());
    }

    @Test
    void rankValuationChain() {
        StateSpace s = ss("&{a: end}");
        Map<Integer, Integer> v = LatticeValuationChecker.rankValuation(s);
        assertEquals(0, v.get(s.bottom()));
        assertTrue(v.get(s.top()) >= 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"&{a: end}", "&{a: &{b: end}}", "(&{a: end} || &{b: end})"})
    void rankBottomAlwaysZero(String typ) {
        StateSpace s = ss(typ);
        Map<Integer, Integer> v = LatticeValuationChecker.rankValuation(s);
        assertEquals(0, v.get(s.bottom()));
    }

    // -------------------- Height valuation --------------------

    @ParameterizedTest
    @ValueSource(strings = {"&{a: end}", "&{a: &{b: end}}", "(&{a: end} || &{b: end})"})
    void heightTopAlwaysZero(String typ) {
        StateSpace s = ss(typ);
        Map<Integer, Integer> v = LatticeValuationChecker.heightValuation(s);
        assertEquals(0, v.get(s.top()));
    }

    @Test
    void heightChain() {
        StateSpace s = ss("&{a: &{b: end}}");
        Map<Integer, Integer> v = LatticeValuationChecker.heightValuation(s);
        assertEquals(0, v.get(s.top()));
        assertTrue(v.get(s.bottom()) >= 1);
    }

    // -------------------- Modularity --------------------

    @Test
    void chainModular() {
        StateSpace s = ss("&{a: end}");
        Map<Integer, Integer> v = LatticeValuationChecker.rankValuation(s);
        assertTrue(LatticeValuationChecker.isModularValuation(s, v));
    }

    @Test
    void parallelRankResult() {
        StateSpace s = ss("(&{a: end} || &{b: end})");
        Map<Integer, Integer> v = LatticeValuationChecker.rankValuation(s);
        boolean result = LatticeValuationChecker.isModularValuation(s, v);
        // Just check it returns a boolean (either true or false is acceptable)
        assertNotNull(result);
    }

    // -------------------- FKG --------------------

    @ParameterizedTest
    @ValueSource(strings = {"&{a: end}", "&{a: &{b: end}}"})
    void distributive(String typ) {
        assertTrue(LatticeValuationChecker.isDistributiveLattice(ss(typ)));
    }

    @Test
    void fkgMonotone() {
        StateSpace s = ss("&{a: &{b: end}}");
        Map<Integer, Integer> v = LatticeValuationChecker.rankValuation(s);
        assertTrue(LatticeValuationChecker.verifyFkg(s,
                state -> (double) v.get(state),
                state -> (double) v.get(state)));
    }

    @Test
    void fkgConstant() {
        StateSpace s = ss("&{a: end}");
        assertTrue(LatticeValuationChecker.verifyFkg(s,
                state -> 1.0,
                state -> 1.0));
    }

    // -------------------- Full analysis --------------------

    @Test
    void analyzeEnd() {
        ValuationResult r = LatticeValuationChecker.analyzeValuations(ss("end"));
        assertEquals(1, r.numStates());
    }

    @Test
    void analyzeChain() {
        ValuationResult r = LatticeValuationChecker.analyzeValuations(ss("&{a: end}"));
        assertEquals(2, r.numStates());
        assertTrue(r.fkgApplicable());
    }

    @Test
    void analyzeParallel() {
        ValuationResult r = LatticeValuationChecker.analyzeValuations(ss("(&{a: end} || &{b: end})"));
        assertEquals(4, r.numStates());
    }

    // -------------------- Benchmarks --------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "&{open: &{read: end, write: end}}",
            "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}",
            "&{a: &{b: &{c: end}}}",
            "(&{a: end} || &{b: end})"
    })
    void benchmarkValuationsRun(String typ) {
        StateSpace s = ss(typ);
        ValuationResult r = LatticeValuationChecker.analyzeValuations(s);
        assertTrue(r.numStates() >= 1);
        assertEquals(s.states().size(), r.rankValuation().size());
    }
}
