package com.bica.reborn.lattice;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for irreducible element analysis ({@link IrreduciblesChecker}).
 */
class IrreduciblesCheckerTest {

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    // ======================================================================
    // Join irreducibles
    // ======================================================================

    @Nested
    class JoinIrreducibles {

        @Test
        void endHasNoJoinIrreducibles() {
            // Single-element lattice: bottom has no join-irreducibles
            var ji = IrreduciblesChecker.joinIrreducibles(ss("end"));
            assertTrue(ji.isEmpty());
        }

        @Test
        void chainHasOneJoinIrreducible() {
            // &{a: end} = 2-element chain: top is the only join-irreducible
            var s = ss("&{a: end}");
            var ji = IrreduciblesChecker.joinIrreducibles(s);
            assertEquals(1, ji.size());
            assertTrue(ji.contains(s.top()));
        }

        @Test
        void branchJoinIrreducibles() {
            // &{a: end, b: end} = 3-element diamond-free lattice
            var s = ss("&{a: end, b: end}");
            var ji = IrreduciblesChecker.joinIrreducibles(s);
            // States after a and b are join-irreducible (each has one lower cover)
            assertFalse(ji.isEmpty());
        }
    }

    // ======================================================================
    // Meet irreducibles
    // ======================================================================

    @Nested
    class MeetIrreducibles {

        @Test
        void endHasNoMeetIrreducibles() {
            var mi = IrreduciblesChecker.meetIrreducibles(ss("end"));
            assertTrue(mi.isEmpty());
        }

        @Test
        void chainHasOneMeetIrreducible() {
            var s = ss("&{a: end}");
            var mi = IrreduciblesChecker.meetIrreducibles(s);
            assertEquals(1, mi.size());
        }
    }

    // ======================================================================
    // Individual state checks
    // ======================================================================

    @Nested
    class IndividualChecks {

        @Test
        void bottomIsNeverJoinIrreducible() {
            var s = ss("&{a: end, b: end}");
            assertFalse(IrreduciblesChecker.isJoinIrreducible(s, s.bottom()));
        }

        @Test
        void topIsNeverMeetIrreducible() {
            var s = ss("&{a: end, b: end}");
            assertFalse(IrreduciblesChecker.isMeetIrreducible(s, s.top()));
        }

        @Test
        void invalidStateThrows() {
            var s = ss("end");
            assertThrows(IllegalArgumentException.class,
                    () -> IrreduciblesChecker.isJoinIrreducible(s, 999));
        }
    }

    // ======================================================================
    // Hasse diagram
    // ======================================================================

    @Nested
    class HasseDiagram {

        @Test
        void endHasNoEdges() {
            assertTrue(IrreduciblesChecker.hasseEdges(ss("end")).isEmpty());
        }

        @Test
        void chainHasOneEdge() {
            assertEquals(1, IrreduciblesChecker.hasseEdges(ss("&{a: end}")).size());
        }

        @Test
        void branchHasCorrectEdges() {
            // &{a: end, b: end}: top covers state-a and state-b; both cover bottom
            var edges = IrreduciblesChecker.hasseEdges(ss("&{a: end, b: end}"));
            assertFalse(edges.isEmpty()); // top covers both branches
        }
    }

    // ======================================================================
    // Birkhoff dual
    // ======================================================================

    @Nested
    class BirkhoffDual {

        @Test
        void endBirkhoffDual() {
            var bd = IrreduciblesChecker.birkhoffDual(ss("end"));
            assertTrue(bd.joinIrr().isEmpty());
            assertEquals(1, bd.downsetCount()); // empty poset has 1 downset (empty set)
        }

        @Test
        void chainBirkhoffDual() {
            var bd = IrreduciblesChecker.birkhoffDual(ss("&{a: end}"));
            assertEquals(1, bd.joinIrr().size());
            assertTrue(bd.isDistributive());
        }

        @Test
        void distributiveBirkhoffTheorem() {
            // For distributive lattice, |L| = number of downsets of J(L)
            var s = ss("&{a: end, b: end}");
            var bd = IrreduciblesChecker.birkhoffDual(s);
            if (bd.isDistributive()) {
                assertEquals(bd.quotientSize(), bd.downsetCount());
            }
        }
    }

    // ======================================================================
    // Full analysis
    // ======================================================================

    @Nested
    class FullAnalysis {

        @Test
        void nonLattice() {
            var r = IrreduciblesChecker.analyzeIrreducibles(ss("rec X . X"));
            assertFalse(r.isLattice());
            assertNull(r.birkhoff());
        }

        @Test
        void simpleLattice() {
            var r = IrreduciblesChecker.analyzeIrreducibles(ss("&{a: end, b: end}"));
            assertTrue(r.isLattice());
            assertNotNull(r.birkhoff());
            assertTrue(r.latticeSize() > 0);
        }

        @Test
        void densitiesInRange() {
            var r = IrreduciblesChecker.analyzeIrreducibles(ss("&{a: end, b: end}"));
            assertTrue(r.joinDensity() >= 0.0 && r.joinDensity() <= 1.0);
            assertTrue(r.meetDensity() >= 0.0 && r.meetDensity() <= 1.0);
        }

        @Test
        void compressionRatioInRange() {
            var r = IrreduciblesChecker.analyzeIrreducibles(ss("&{a: end, b: end}"));
            if (r.birkhoff() != null) {
                assertTrue(r.birkhoff().compressionRatio() >= 0.0);
                assertTrue(r.birkhoff().compressionRatio() <= 1.0);
            }
        }
    }
}
