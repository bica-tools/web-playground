package com.bica.reborn.birkhoff;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BirkhoffChecker} — Birkhoff's Representation Theorem.
 */
class BirkhoffCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    @Test
    void endIsRepresentable() {
        StateSpace ss = build("end");
        BirkhoffResult r = BirkhoffChecker.birkhoffRepresentation(ss);
        assertTrue(r.isLattice());
        assertTrue(r.isDistributive());
        assertTrue(r.isRepresentable());
    }

    @Test
    void simpleBranchIsDistributive() {
        StateSpace ss = build("&{a: end, b: end}");
        BirkhoffResult r = BirkhoffChecker.birkhoffRepresentation(ss);
        assertTrue(r.isLattice());
        assertTrue(r.isDistributive());
        assertTrue(r.isRepresentable());
        assertTrue(r.isomorphismVerified());
    }

    @Test
    void singleMethodBranch() {
        StateSpace ss = build("&{open: end}");
        BirkhoffResult r = BirkhoffChecker.birkhoffRepresentation(ss);
        assertTrue(r.isLattice());
        assertTrue(r.isRepresentable());
        assertEquals(2, r.latticeSize());
        assertTrue(r.compressionRatio() <= 1.0);
    }

    @Test
    void joinIrreduciblesNonEmpty() {
        StateSpace ss = build("&{a: end, b: end}");
        BirkhoffResult r = BirkhoffChecker.birkhoffRepresentation(ss);
        assertFalse(r.joinIrreducibles().isEmpty());
    }

    @Test
    void compressionRatioLessThanOrEqualOne() {
        StateSpace ss = build("&{a: &{b: end, c: end}}");
        BirkhoffResult r = BirkhoffChecker.birkhoffRepresentation(ss);
        assertTrue(r.compressionRatio() <= 1.0);
    }

    @Test
    void downsetLatticeFromEmptyPoset() {
        StateSpace dl = BirkhoffChecker.downsetLattice(Map.of());
        assertEquals(1, dl.states().size());
    }

    @Test
    void downsetLatticeFromChain() {
        // Poset: 1 > 0 (chain of 2)
        Map<Integer, Set<Integer>> poset = Map.of(1, Set.of(0), 0, Set.of());
        StateSpace dl = BirkhoffChecker.downsetLattice(poset);
        // Downsets of a 2-chain: {}, {0}, {0,1} = 3 elements
        assertEquals(3, dl.states().size());
    }

    @Test
    void downsetLatticeFromAntichain() {
        // Poset: two incomparable elements {0, 1}
        Map<Integer, Set<Integer>> poset = Map.of(0, Set.of(), 1, Set.of());
        StateSpace dl = BirkhoffChecker.downsetLattice(poset);
        // Downsets of 2-antichain: {}, {0}, {1}, {0,1} = 4 elements (Boolean lattice)
        assertEquals(4, dl.states().size());
    }

    @Test
    void reconstructFromPosetPreservesLatticeProperty() {
        Map<Integer, Set<Integer>> poset = Map.of(0, Set.of(), 1, Set.of());
        StateSpace reconstructed = BirkhoffChecker.reconstructFromPoset(poset);
        assertNotNull(reconstructed);
        assertTrue(reconstructed.states().size() >= 1);
    }

    @Test
    void isRepresentableSimple() {
        assertTrue(BirkhoffChecker.isRepresentable(build("&{a: end}")));
        assertTrue(BirkhoffChecker.isRepresentable(build("end")));
    }

    @Test
    void selectionIsDistributive() {
        StateSpace ss = build("+{OK: end, ERR: end}");
        BirkhoffResult r = BirkhoffChecker.birkhoffRepresentation(ss);
        assertTrue(r.isLattice());
        assertTrue(r.isDistributive());
    }

    @Test
    void nestedBranchRepresentation() {
        StateSpace ss = build("&{a: &{b: end}, c: end}");
        BirkhoffResult r = BirkhoffChecker.birkhoffRepresentation(ss);
        assertTrue(r.isLattice());
        assertTrue(r.isRepresentable());
        assertTrue(r.downsetLatticeSize() > 0);
    }
}
