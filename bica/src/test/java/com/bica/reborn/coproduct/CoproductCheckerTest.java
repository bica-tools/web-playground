package com.bica.reborn.coproduct;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CoproductChecker} — categorical coproduct analysis.
 */
class CoproductCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    @Test
    void coalescedSumHasCorrectStateCount() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        StateSpace sum = CoproductChecker.coalescedSum(left, right);
        // |L1| + |L2| - 1 (shared bottom) + 1 (fresh top)
        assertEquals(left.states().size() + right.states().size(), sum.states().size());
    }

    @Test
    void separatedSumHasCorrectStateCount() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        StateSpace sum = CoproductChecker.separatedSum(left, right);
        // |L1| + |L2| + 2 (fresh top and bottom)
        assertEquals(left.states().size() + right.states().size() + 2, sum.states().size());
    }

    @Test
    void linearSumHasCorrectStateCount() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        StateSpace sum = CoproductChecker.linearSum(left, right);
        // |L1| + |L2| - 1 (L1.bottom = L2.top)
        assertEquals(left.states().size() + right.states().size() - 1, sum.states().size());
    }

    @Test
    void coalescedSumIsLattice() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        StateSpace sum = CoproductChecker.coalescedSum(left, right);
        assertTrue(LatticeChecker.checkLattice(sum).isLattice());
    }

    @Test
    void separatedSumIsLattice() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        StateSpace sum = CoproductChecker.separatedSum(left, right);
        assertTrue(LatticeChecker.checkLattice(sum).isLattice());
    }

    @Test
    void linearSumIsLattice() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        StateSpace sum = CoproductChecker.linearSum(left, right);
        assertTrue(LatticeChecker.checkLattice(sum).isLattice());
    }

    @Test
    void checkCoproductReturnsCandidates() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        CoproductResult result = CoproductChecker.checkCoproduct(left, right);
        assertEquals(3, result.allCandidates().size());
    }

    @Test
    void buildCoproductCoalesced() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        StateSpace cand = CoproductChecker.buildCoproduct(left, right, "coalesced");
        assertNotNull(cand);
        assertTrue(cand.states().size() > 0);
    }

    @Test
    void buildCoproductInvalidThrows() {
        StateSpace left = build("end");
        StateSpace right = build("end");
        assertThrows(IllegalArgumentException.class,
                () -> CoproductChecker.buildCoproduct(left, right, "invalid"));
    }

    @Test
    void endCoproductWithEnd() {
        StateSpace left = build("end");
        StateSpace right = build("end");
        CoproductResult result = CoproductChecker.checkCoproduct(left, right);
        assertNotNull(result);
        // All candidates should exist
        assertEquals(3, result.allCandidates().size());
    }

    @Test
    void injectionsAreInjective() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        CoproductResult result = CoproductChecker.checkCoproduct(left, right);
        for (var cand : result.allCandidates()) {
            for (var inj : cand.injections()) {
                assertTrue(inj.isInjective(),
                        "Injection for " + cand.construction() + " should be injective");
            }
        }
    }
}
