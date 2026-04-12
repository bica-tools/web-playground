package com.bica.reborn.coequalizer;

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
 * Tests for {@link CoequalizerChecker} — categorical coequalizer analysis.
 */
class CoequalizerCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    @Test
    void seedPairsWithIdenticalMappings() {
        StateSpace ss = build("&{a: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        Set<long[]> seeds = CoequalizerChecker.coequalizerSeedPairs(ss, id, id);
        // Each s maps to (s, s) — all diagonal
        assertEquals(ss.states().size(), seeds.size());
    }

    @Test
    void coequalizerOfIdentityPair() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        CoequalizerResult r = CoequalizerChecker.computeCoequalizer(ss, ss, id, id);
        assertTrue(r.isLattice());
        assertNotNull(r.quotientSpace());
        // Quotient of identity = same lattice
        assertEquals(ss.states().size(), r.quotientSpace().states().size());
    }

    @Test
    void quotientSpaceIsLattice() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        CoequalizerResult r = CoequalizerChecker.computeCoequalizer(ss, ss, id, id);
        assertTrue(LatticeChecker.checkLattice(r.quotientSpace()).isLattice());
    }

    @Test
    void coequalizerOnEnd() {
        StateSpace ss = build("end");
        Map<Integer, Integer> id = Map.of(ss.top(), ss.top());

        CoequalizerResult r = CoequalizerChecker.computeCoequalizer(ss, ss, id, id);
        assertTrue(r.hasCoequalizer());
    }

    @Test
    void congruenceClosureWithTrivialSeeds() {
        StateSpace ss = build("&{a: end, b: end}");
        // Trivial seeds: all diagonal pairs
        Set<long[]> seeds = Set.of();
        for (int s : ss.states()) {
            seeds = new java.util.HashSet<>(seeds);
            seeds.add(new long[]{s, s});
        }
        Map<Integer, Set<Integer>> cong = CoequalizerChecker.congruenceClosure(ss, seeds);
        // Each element in its own class
        assertEquals(ss.states().size(), cong.size());
    }

    @Test
    void quotientLatticePreservesSize() {
        StateSpace ss = build("&{a: end, b: end}");
        // Discrete congruence: each in its own class
        Map<Integer, Set<Integer>> classes = new HashMap<>();
        for (int s : ss.states()) {
            classes.put(s, Set.of(s));
        }
        StateSpace q = CoequalizerChecker.quotientLattice(ss, classes);
        assertEquals(ss.states().size(), q.states().size());
    }

    @Test
    void checkCoequalizerPropertyWithIdentity() {
        StateSpace ss = build("&{a: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        CoequalizerResult r = CoequalizerChecker.computeCoequalizer(ss, ss, id, id);
        assertTrue(CoequalizerChecker.checkCoequalizerProperty(
                r.quotientSpace(), ss, ss, id, id, r.quotientMap()));
    }

    @Test
    void isCoequalizerCheck() {
        StateSpace ss = build("&{a: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        CoequalizerResult r = CoequalizerChecker.computeCoequalizer(ss, ss, id, id);
        assertTrue(CoequalizerChecker.isCoequalizer(
                r.quotientSpace(), ss, ss, id, id, r.quotientMap()));
    }

    @Test
    void coequalizerWithSelection() {
        StateSpace ss = build("+{OK: end, ERR: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        CoequalizerResult r = CoequalizerChecker.computeCoequalizer(ss, ss, id, id);
        assertTrue(r.isLattice());
    }

    @Test
    void universalPropertyHoldsForIdentity() {
        StateSpace ss = build("&{a: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        CoequalizerResult r = CoequalizerChecker.computeCoequalizer(ss, ss, id, id);
        assertTrue(r.universalPropertyHolds());
    }

    @Test
    void congruenceClassesAreConsistent() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) id.put(s, s);

        CoequalizerResult r = CoequalizerChecker.computeCoequalizer(ss, ss, id, id);
        assertNotNull(r.congruenceClasses());
        // All states should appear in exactly one class
        Set<Integer> allMembers = new java.util.HashSet<>();
        for (var members : r.congruenceClasses().values()) {
            allMembers.addAll(members);
        }
        assertEquals(ss.states(), allMembers);
    }
}
