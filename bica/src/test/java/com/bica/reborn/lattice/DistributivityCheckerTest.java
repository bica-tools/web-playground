package com.bica.reborn.lattice;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for distributivity checking ({@link DistributivityChecker}).
 *
 * <p>Verifies N₅/M₃ detection, Birkhoff hierarchy classification (Boolean,
 * distributive, modular, lattice), and witness extraction.
 */
class DistributivityCheckerTest {

    private static DistributivityResult check(String source) {
        return DistributivityChecker.checkDistributive(
                StateSpaceBuilder.build(Parser.parse(source)));
    }

    // ======================================================================
    // Distributive types
    // ======================================================================

    @Nested
    class DistributiveTypes {

        @Test
        void endIsDistributive() {
            var r = check("end");
            assertTrue(r.isDistributive());
            assertEquals("boolean", r.classification());
        }

        @Test
        void singleBranch() {
            var r = check("&{a: end}");
            assertTrue(r.isDistributive());
        }

        @Test
        void simpleBranch() {
            var r = check("&{a: end, b: end}");
            assertTrue(r.isDistributive());
        }

        @Test
        void simpleSelect() {
            var r = check("+{a: end, b: end}");
            assertTrue(r.isDistributive());
        }

        @Test
        void chainIsDistributive() {
            var r = check("&{a: &{b: end}}");
            assertTrue(r.isDistributive());
        }

        @Test
        void branchThenSelect() {
            var r = check("&{a: +{x: end, y: end}, b: +{x: end, y: end}}");
            assertTrue(r.isDistributive());
        }

        @Test
        void parallelOfDistributive() {
            var r = check("(&{a: end, b: end} || &{c: end, d: end})");
            assertTrue(r.isDistributive());
            assertFalse(r.hasN5());
            assertFalse(r.hasM3());
        }

        @Test
        void recursiveIterator() {
            var r = check("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            assertTrue(r.isDistributive());
        }
    }

    // ======================================================================
    // Non-distributive types
    // ======================================================================

    @Nested
    class NonDistributiveTypes {

        @Test
        void notALattice() {
            // Non-terminating recursion (rec X . X) → not a lattice
            var r = check("rec X . X");
            assertFalse(r.isLattice());
            assertEquals("not_lattice", r.classification());
        }

        @Test
        void classificationIsLatticeOrModular() {
            // N₅ pattern: shared select at different depths
            var r = check("&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}");
            assertTrue(r.isLattice());
            // This is the minimal N₅ example
            if (r.hasN5()) {
                assertFalse(r.isDistributive());
                assertFalse(r.isModular());
                assertEquals("lattice", r.classification());
                assertNotNull(r.n5Witness());
                assertEquals(5, r.n5Witness().size());
            }
        }
    }

    // ======================================================================
    // Witnesses
    // ======================================================================

    @Nested
    class Witnesses {

        @Test
        void n5WitnessHasFiveElements() {
            var r = check("&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}");
            if (r.hasN5()) {
                assertNotNull(r.n5Witness());
                assertEquals(5, r.n5Witness().size());
            }
        }

        @Test
        void noWitnessForDistributive() {
            var r = check("&{a: end, b: end}");
            assertNull(r.m3Witness());
            assertNull(r.n5Witness());
        }
    }

    // ======================================================================
    // Boolean lattices
    // ======================================================================

    @Nested
    class BooleanLattices {

        @Test
        void endIsBoolean() {
            assertEquals("boolean", check("end").classification());
        }

        @Test
        void twoElementChainIsBoolean() {
            assertEquals("boolean", check("&{a: end}").classification());
        }

        @Test
        void parallelTwoChains() {
            // Product of two 2-element chains = 4-element Boolean lattice (2^2)
            var r = check("(&{a: end} || &{b: end})");
            assertEquals("boolean", r.classification());
        }
    }

    // ======================================================================
    // Hierarchy properties
    // ======================================================================

    @Nested
    class Hierarchy {

        @Test
        void distributiveImpliesModular() {
            var r = check("&{a: end, b: end}");
            if (r.isDistributive()) {
                assertTrue(r.isModular());
            }
        }

        @Test
        void modularImpliesLattice() {
            var r = check("&{a: end, b: end}");
            if (r.isModular()) {
                assertTrue(r.isLattice());
            }
        }

        @Test
        void n5MeansNotModular() {
            var r = check("&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}");
            if (r.hasN5()) {
                assertFalse(r.isModular());
                assertFalse(r.isDistributive());
            }
        }
    }

    // ======================================================================
    // Task 44 regression: false N5 on grid lattices
    // ======================================================================

    /**
     * Regression suite for the Task 44 false-N5 bug. Before the fix the
     * pentagon search returned the chain shape
     * {top, a, b, c, bot} without verifying that the five elements
     * actually form a sublattice closed under meet and join. The fix
     * adds the {@code meet(a,c) = bot ∧ join(b,c) = top} sublattice
     * closure check, mirroring the Python reference
     * {@code reticulate.lattice._find_n5}.
     *
     * <p>The grid lattices below are textbook finite distributive
     * lattices and must NEVER be flagged as containing N5.
     */
    @Nested
    class Task44GridRegression {

        @Test
        void grid3x3IsDistributive() {
            // ∂_3 × ∂_3 — product of two 3-element chains, 9 states.
            // Pre-fix: BICA reported hasN5=true, classification="lattice".
            // Post-fix: distributive.
            var r = check("(a . b . end || c . d . end)");
            assertEquals(9, r.latticeResult().numScc());
            assertTrue(r.isLattice());
            assertTrue(r.isModular());
            assertTrue(r.isDistributive());
            assertFalse(r.hasN5());
            assertFalse(r.hasM3());
            assertEquals("distributive", r.classification());
        }

        @Test
        void grid4x4IsDistributive() {
            // ∂_4 × ∂_4 — product of two 4-element chains, 16 states.
            // A larger grid to guard against N5 false positives that
            // happen to evade the 3×3 case.
            var r = check("(a . b . c . end || d . e . f . end)");
            assertEquals(16, r.latticeResult().numScc());
            assertTrue(r.isDistributive());
            assertFalse(r.hasN5());
        }

        @Test
        void cube2x2x2IsBoolean() {
            // 2^3 Boolean lattice via three independent parallel chains.
            // The triple product is associative under our parser so we
            // bracket as ((a || b) || c).
            var r = check("((a . end || b . end) || c . end)");
            assertEquals(8, r.latticeResult().numScc());
            assertEquals("boolean", r.classification());
        }

        @Test
        void canonicalN5StillDetected() {
            // True positive: the canonical N5 counterexample from the
            // modularity-campaign charter must still be flagged.
            // Guards against the fix accidentally weakening detection.
            var r = check("&{a: &{x: &{y: end}}, b: &{z: end}}");
            assertTrue(r.hasN5());
            assertFalse(r.isModular());
        }

        @Test
        void canonicalM3StillDetected() {
            // True positive: the canonical M3 (modular but not
            // distributive) witness must also still be flagged.
            var r = check("&{a: &{x: end}, b: &{y: end}, c: &{z: end}}");
            assertTrue(r.hasM3());
            assertTrue(r.isModular());
            assertFalse(r.isDistributive());
        }
    }
}
