package com.bica.reborn.equalizer;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EqualizerChecker} — categorical equalizer analysis.
 */
class EqualizerCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    @Test
    void agreementSetWithIdenticalMappings() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        Set<Integer> agree = EqualizerChecker.agreementSet(ss, id, id);
        assertEquals(ss.states(), agree);
    }

    @Test
    void equalizerOfIdentityPair() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        EqualizerResult r = EqualizerChecker.computeEqualizer(ss, ss, id, id);
        assertTrue(r.hasEqualizer());
        assertNotNull(r.equalizerSpace());
        assertEquals(ss.states().size(), r.equalizerSpace().states().size());
    }

    @Test
    void equalizerSpaceIsLattice() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        EqualizerResult r = EqualizerChecker.computeEqualizer(ss, ss, id, id);
        assertTrue(LatticeChecker.checkLattice(r.equalizerSpace()).isLattice());
    }

    @Test
    void equalizerOnEnd() {
        StateSpace ss = build("end");
        Map<Integer, Integer> id = Map.of(ss.top(), ss.top());

        EqualizerResult r = EqualizerChecker.computeEqualizer(ss, ss, id, id);
        assertTrue(r.hasEqualizer());
    }

    @Test
    void universalPropertyHoldsForIdentity() {
        StateSpace ss = build("&{a: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        EqualizerResult r = EqualizerChecker.computeEqualizer(ss, ss, id, id);
        assertTrue(r.universalPropertyHolds());
    }

    @Test
    void isEqualizerCheck() {
        StateSpace ss = build("&{a: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        EqualizerResult r = EqualizerChecker.computeEqualizer(ss, ss, id, id);
        assertTrue(EqualizerChecker.isEqualizer(
                r.equalizerSpace(), ss, ss, id, id, r.inclusion()));
    }

    @Test
    void kernelPairOfIdentity() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        var kp = EqualizerChecker.computeKernelPair(ss, id);
        assertTrue(kp.isCongruence());
        // Identity kernel pair: only diagonal pairs
        assertEquals(ss.states().size(), kp.pairs().size());
    }

    @Test
    void subsetEqualizerPreservesTopBottom() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        StateSpace eq = EqualizerChecker.subsetEqualizer(ss, id, id);
        assertNotNull(eq);
        assertTrue(eq.states().contains(eq.top()));
    }

    @Test
    void finiteCompletenessWithSimpleLattices() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{a: end, b: end}");

        var r = EqualizerChecker.checkFiniteCompleteness(java.util.List.of(ss1, ss2));
        assertTrue(r.hasProducts());
        assertTrue(r.hasEqualizers());
        assertTrue(r.isFinitelyComplete());
    }

    @Test
    void equalizerWithSelection() {
        StateSpace ss = build("+{OK: end, ERR: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        EqualizerResult r = EqualizerChecker.computeEqualizer(ss, ss, id, id);
        assertTrue(r.hasEqualizer());
    }

    @Test
    void checkEqualizerUniversalPropertyDirect() {
        StateSpace ss = build("&{a: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        EqualizerResult r = EqualizerChecker.computeEqualizer(ss, ss, id, id);
        assertTrue(EqualizerChecker.checkEqualizerUniversalProperty(
                r.equalizerSpace(), ss, id, id, r.inclusion()));
    }
}
