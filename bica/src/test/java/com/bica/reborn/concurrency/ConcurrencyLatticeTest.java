package com.bica.reborn.concurrency;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.bica.reborn.concurrency.ConcurrencyLevel.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link ConcurrencyLevel} enum and {@link ConcurrencyLattice} operations.
 */
class ConcurrencyLatticeTest {

    // =========================================================================
    // ConcurrencyLevel — enum basics
    // =========================================================================

    @Nested
    class LevelBasics {

        @Test
        void fourValues() {
            assertEquals(4, ConcurrencyLevel.values().length);
        }

        @Test
        void symbols() {
            assertEquals("⊥", EXCLUSIVE.symbol());
            assertEquals("R", READ_ONLY.symbol());
            assertEquals("S", SYNC.symbol());
            assertEquals("⊤", SHARED.symbol());
        }

        @Test
        void ordinalOrder() {
            assertTrue(EXCLUSIVE.ordinal() < READ_ONLY.ordinal());
            assertTrue(READ_ONLY.ordinal() < SYNC.ordinal());
            assertTrue(SYNC.ordinal() < SHARED.ordinal());
        }
    }

    // =========================================================================
    // Meet — all 16 pairs
    // =========================================================================

    @Nested
    class Meet {

        @Test void exclusiveExclusive() { assertEquals(EXCLUSIVE, ConcurrencyLattice.meet(EXCLUSIVE, EXCLUSIVE)); }
        @Test void exclusiveReadOnly() { assertEquals(EXCLUSIVE, ConcurrencyLattice.meet(EXCLUSIVE, READ_ONLY)); }
        @Test void exclusiveSync() { assertEquals(EXCLUSIVE, ConcurrencyLattice.meet(EXCLUSIVE, SYNC)); }
        @Test void exclusiveShared() { assertEquals(EXCLUSIVE, ConcurrencyLattice.meet(EXCLUSIVE, SHARED)); }

        @Test void readOnlyExclusive() { assertEquals(EXCLUSIVE, ConcurrencyLattice.meet(READ_ONLY, EXCLUSIVE)); }
        @Test void readOnlyReadOnly() { assertEquals(READ_ONLY, ConcurrencyLattice.meet(READ_ONLY, READ_ONLY)); }
        @Test void readOnlySync() { assertEquals(EXCLUSIVE, ConcurrencyLattice.meet(READ_ONLY, SYNC)); }
        @Test void readOnlyShared() { assertEquals(READ_ONLY, ConcurrencyLattice.meet(READ_ONLY, SHARED)); }

        @Test void syncExclusive() { assertEquals(EXCLUSIVE, ConcurrencyLattice.meet(SYNC, EXCLUSIVE)); }
        @Test void syncReadOnly() { assertEquals(EXCLUSIVE, ConcurrencyLattice.meet(SYNC, READ_ONLY)); }
        @Test void syncSync() { assertEquals(SYNC, ConcurrencyLattice.meet(SYNC, SYNC)); }
        @Test void syncShared() { assertEquals(SYNC, ConcurrencyLattice.meet(SYNC, SHARED)); }

        @Test void sharedExclusive() { assertEquals(EXCLUSIVE, ConcurrencyLattice.meet(SHARED, EXCLUSIVE)); }
        @Test void sharedReadOnly() { assertEquals(READ_ONLY, ConcurrencyLattice.meet(SHARED, READ_ONLY)); }
        @Test void sharedSync() { assertEquals(SYNC, ConcurrencyLattice.meet(SHARED, SYNC)); }
        @Test void sharedShared() { assertEquals(SHARED, ConcurrencyLattice.meet(SHARED, SHARED)); }

        @Test
        void nullFirst() {
            assertThrows(NullPointerException.class,
                    () -> ConcurrencyLattice.meet(null, EXCLUSIVE));
        }

        @Test
        void nullSecond() {
            assertThrows(NullPointerException.class,
                    () -> ConcurrencyLattice.meet(EXCLUSIVE, null));
        }
    }

    // =========================================================================
    // Join — all 16 pairs
    // =========================================================================

    @Nested
    class Join {

        @Test void exclusiveExclusive() { assertEquals(EXCLUSIVE, ConcurrencyLattice.join(EXCLUSIVE, EXCLUSIVE)); }
        @Test void exclusiveReadOnly() { assertEquals(READ_ONLY, ConcurrencyLattice.join(EXCLUSIVE, READ_ONLY)); }
        @Test void exclusiveSync() { assertEquals(SYNC, ConcurrencyLattice.join(EXCLUSIVE, SYNC)); }
        @Test void exclusiveShared() { assertEquals(SHARED, ConcurrencyLattice.join(EXCLUSIVE, SHARED)); }

        @Test void readOnlyExclusive() { assertEquals(READ_ONLY, ConcurrencyLattice.join(READ_ONLY, EXCLUSIVE)); }
        @Test void readOnlyReadOnly() { assertEquals(READ_ONLY, ConcurrencyLattice.join(READ_ONLY, READ_ONLY)); }
        @Test void readOnlySync() { assertEquals(SHARED, ConcurrencyLattice.join(READ_ONLY, SYNC)); }
        @Test void readOnlyShared() { assertEquals(SHARED, ConcurrencyLattice.join(READ_ONLY, SHARED)); }

        @Test void syncExclusive() { assertEquals(SYNC, ConcurrencyLattice.join(SYNC, EXCLUSIVE)); }
        @Test void syncReadOnly() { assertEquals(SHARED, ConcurrencyLattice.join(SYNC, READ_ONLY)); }
        @Test void syncSync() { assertEquals(SYNC, ConcurrencyLattice.join(SYNC, SYNC)); }
        @Test void syncShared() { assertEquals(SHARED, ConcurrencyLattice.join(SYNC, SHARED)); }

        @Test void sharedExclusive() { assertEquals(SHARED, ConcurrencyLattice.join(SHARED, EXCLUSIVE)); }
        @Test void sharedReadOnly() { assertEquals(SHARED, ConcurrencyLattice.join(SHARED, READ_ONLY)); }
        @Test void sharedSync() { assertEquals(SHARED, ConcurrencyLattice.join(SHARED, SYNC)); }
        @Test void sharedShared() { assertEquals(SHARED, ConcurrencyLattice.join(SHARED, SHARED)); }

        @Test
        void nullFirst() {
            assertThrows(NullPointerException.class,
                    () -> ConcurrencyLattice.join(null, SHARED));
        }

        @Test
        void nullSecond() {
            assertThrows(NullPointerException.class,
                    () -> ConcurrencyLattice.join(SHARED, null));
        }
    }

    // =========================================================================
    // Compatible — all 16 pairs
    // =========================================================================

    @Nested
    class Compatible {

        @Test void exclusiveExclusive() { assertFalse(ConcurrencyLattice.compatible(EXCLUSIVE, EXCLUSIVE)); }
        @Test void exclusiveReadOnly() { assertFalse(ConcurrencyLattice.compatible(EXCLUSIVE, READ_ONLY)); }
        @Test void exclusiveSync() { assertFalse(ConcurrencyLattice.compatible(EXCLUSIVE, SYNC)); }
        @Test void exclusiveShared() { assertFalse(ConcurrencyLattice.compatible(EXCLUSIVE, SHARED)); }

        @Test void readOnlyExclusive() { assertFalse(ConcurrencyLattice.compatible(READ_ONLY, EXCLUSIVE)); }
        @Test void readOnlyReadOnly() { assertTrue(ConcurrencyLattice.compatible(READ_ONLY, READ_ONLY)); }
        @Test void readOnlySync() { assertTrue(ConcurrencyLattice.compatible(READ_ONLY, SYNC)); }
        @Test void readOnlyShared() { assertTrue(ConcurrencyLattice.compatible(READ_ONLY, SHARED)); }

        @Test void syncExclusive() { assertFalse(ConcurrencyLattice.compatible(SYNC, EXCLUSIVE)); }
        @Test void syncReadOnly() { assertTrue(ConcurrencyLattice.compatible(SYNC, READ_ONLY)); }
        @Test void syncSync() { assertTrue(ConcurrencyLattice.compatible(SYNC, SYNC)); }
        @Test void syncShared() { assertTrue(ConcurrencyLattice.compatible(SYNC, SHARED)); }

        @Test void sharedExclusive() { assertFalse(ConcurrencyLattice.compatible(SHARED, EXCLUSIVE)); }
        @Test void sharedReadOnly() { assertTrue(ConcurrencyLattice.compatible(SHARED, READ_ONLY)); }
        @Test void sharedSync() { assertTrue(ConcurrencyLattice.compatible(SHARED, SYNC)); }
        @Test void sharedShared() { assertTrue(ConcurrencyLattice.compatible(SHARED, SHARED)); }

        @Test
        void nullFirst() {
            assertThrows(NullPointerException.class,
                    () -> ConcurrencyLattice.compatible(null, SHARED));
        }

        @Test
        void nullSecond() {
            assertThrows(NullPointerException.class,
                    () -> ConcurrencyLattice.compatible(SHARED, null));
        }
    }

    // =========================================================================
    // Lattice axioms — algebraic properties
    // =========================================================================

    @Nested
    class LatticeAxioms {

        private final ConcurrencyLevel[] ALL = ConcurrencyLevel.values();

        @Test
        void meetCommutativity() {
            for (var a : ALL)
                for (var b : ALL)
                    assertEquals(ConcurrencyLattice.meet(a, b), ConcurrencyLattice.meet(b, a),
                            "meet(" + a + ", " + b + ") should be commutative");
        }

        @Test
        void joinCommutativity() {
            for (var a : ALL)
                for (var b : ALL)
                    assertEquals(ConcurrencyLattice.join(a, b), ConcurrencyLattice.join(b, a),
                            "join(" + a + ", " + b + ") should be commutative");
        }

        @Test
        void meetAssociativity() {
            for (var a : ALL)
                for (var b : ALL)
                    for (var c : ALL)
                        assertEquals(
                                ConcurrencyLattice.meet(ConcurrencyLattice.meet(a, b), c),
                                ConcurrencyLattice.meet(a, ConcurrencyLattice.meet(b, c)),
                                "meet should be associative for " + a + ", " + b + ", " + c);
        }

        @Test
        void joinAssociativity() {
            for (var a : ALL)
                for (var b : ALL)
                    for (var c : ALL)
                        assertEquals(
                                ConcurrencyLattice.join(ConcurrencyLattice.join(a, b), c),
                                ConcurrencyLattice.join(a, ConcurrencyLattice.join(b, c)),
                                "join should be associative for " + a + ", " + b + ", " + c);
        }

        @Test
        void meetIdempotency() {
            for (var a : ALL)
                assertEquals(a, ConcurrencyLattice.meet(a, a),
                        "meet(" + a + ", " + a + ") should be idempotent");
        }

        @Test
        void joinIdempotency() {
            for (var a : ALL)
                assertEquals(a, ConcurrencyLattice.join(a, a),
                        "join(" + a + ", " + a + ") should be idempotent");
        }

        @Test
        void absorption1() {
            // a ∧ (a ∨ b) = a
            for (var a : ALL)
                for (var b : ALL)
                    assertEquals(a,
                            ConcurrencyLattice.meet(a, ConcurrencyLattice.join(a, b)),
                            "absorption a ∧ (a ∨ b) = a for " + a + ", " + b);
        }

        @Test
        void absorption2() {
            // a ∨ (a ∧ b) = a
            for (var a : ALL)
                for (var b : ALL)
                    assertEquals(a,
                            ConcurrencyLattice.join(a, ConcurrencyLattice.meet(a, b)),
                            "absorption a ∨ (a ∧ b) = a for " + a + ", " + b);
        }

        @Test
        void topIdentityForMeet() {
            for (var a : ALL)
                assertEquals(a, ConcurrencyLattice.meet(SHARED, a),
                        "SHARED should be identity for meet");
        }

        @Test
        void bottomIdentityForJoin() {
            for (var a : ALL)
                assertEquals(a, ConcurrencyLattice.join(EXCLUSIVE, a),
                        "EXCLUSIVE should be identity for join");
        }

        @Test
        void compatibilitySymmetric() {
            for (var a : ALL)
                for (var b : ALL)
                    assertEquals(
                            ConcurrencyLattice.compatible(a, b),
                            ConcurrencyLattice.compatible(b, a),
                            "compatible(" + a + ", " + b + ") should be symmetric");
        }
    }
}
