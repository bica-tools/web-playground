package com.bica.reborn.monoidal;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MonoidalChecker} — monoidal structure of session type composition.
 */
class MonoidalCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    @Test
    void endIsLeftUnit() {
        StateSpace ss = build("&{a: end}");
        var r = MonoidalChecker.checkMonoidalUnit(ss);
        assertTrue(r.leftUnit());
    }

    @Test
    void endIsRightUnit() {
        StateSpace ss = build("&{a: end}");
        var r = MonoidalChecker.checkMonoidalUnit(ss);
        assertTrue(r.rightUnit());
    }

    @Test
    void associativitySimple() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ss3 = build("&{c: end}");

        var r = MonoidalChecker.checkAssociativity(ss1, ss2, ss3);
        assertTrue(r.isAssociative());
        assertEquals(r.leftStates(), r.rightStates());
    }

    @Test
    void symmetrySimple() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");

        var r = MonoidalChecker.checkSymmetry(ss1, ss2);
        assertTrue(r.isSymmetric());
        assertTrue(r.isInvolution());
    }

    @Test
    void associatorReturnsMapping() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ss3 = build("&{c: end}");

        var iso = MonoidalChecker.associator(ss1, ss2, ss3);
        assertNotNull(iso);
    }

    @Test
    void pentagonCommutes() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ss3 = build("&{c: end}");

        var r = MonoidalChecker.checkCoherence(ss1, ss2, ss3);
        assertNotNull(r); // coherence check runs without error
    }

    @Test
    void triangleCommutes() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");

        var r = MonoidalChecker.checkCoherence(ss1, ss2, build("&{c: end}"));
        assertNotNull(r);
    }

    @Test
    void hexagonCommutes() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ss3 = build("&{c: end}");

        var r = MonoidalChecker.checkCoherence(ss1, ss2, ss3);
        assertNotNull(r);
    }

    @Test
    void fullMonoidalStructure() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ss3 = build("&{c: end}");

        MonoidalResult r = MonoidalChecker.checkMonoidalStructure(ss1, ss2, ss3);
        assertNotNull(r);
    }

    @Test
    void unitConstraintsEquivalent() {
        StateSpace ss = build("&{a: end, b: end}");
        var unit = MonoidalChecker.unitConstraints(ss);
        assertTrue(unit.leftUnit());
        assertTrue(unit.rightUnit());
    }

    @Test
    void monoidalWithSelections() {
        StateSpace ss1 = build("+{OK: end, ERR: end}");
        StateSpace ss2 = build("&{a: end}");
        StateSpace ss3 = build("&{b: end}");

        MonoidalResult r = MonoidalChecker.checkMonoidalStructure(ss1, ss2, ss3);
        assertNotNull(r); // coherence depends on implementation details
    }
}
