package com.bica.reborn.concurrency;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bica.reborn.concurrency.ConcurrencyLevel.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ThreadSafetyResult} and its nested {@link ThreadSafetyResult.Violation}.
 */
class ThreadSafetyResultTest {

    // =========================================================================
    // Violation record
    // =========================================================================

    @Nested
    class ViolationRecord {

        @Test
        void fields() {
            var v = new ThreadSafetyResult.Violation("read", READ_ONLY, "write", EXCLUSIVE);
            assertEquals("read", v.method1());
            assertEquals(READ_ONLY, v.level1());
            assertEquals("write", v.method2());
            assertEquals(EXCLUSIVE, v.level2());
        }

        @Test
        void equality() {
            var a = new ThreadSafetyResult.Violation("a", SYNC, "b", EXCLUSIVE);
            var b = new ThreadSafetyResult.Violation("a", SYNC, "b", EXCLUSIVE);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void nullMethod1() {
            assertThrows(NullPointerException.class,
                    () -> new ThreadSafetyResult.Violation(null, SYNC, "b", EXCLUSIVE));
        }

        @Test
        void nullLevel1() {
            assertThrows(NullPointerException.class,
                    () -> new ThreadSafetyResult.Violation("a", null, "b", EXCLUSIVE));
        }

        @Test
        void nullMethod2() {
            assertThrows(NullPointerException.class,
                    () -> new ThreadSafetyResult.Violation("a", SYNC, null, EXCLUSIVE));
        }

        @Test
        void nullLevel2() {
            assertThrows(NullPointerException.class,
                    () -> new ThreadSafetyResult.Violation("a", SYNC, "b", null));
        }
    }

    // =========================================================================
    // ThreadSafetyResult record
    // =========================================================================

    @Nested
    class ResultRecord {

        @Test
        void safeResult() {
            var r = new ThreadSafetyResult(true, List.of(), List.of(), List.of());
            assertTrue(r.isSafe());
            assertTrue(r.violations().isEmpty());
            assertTrue(r.trustBoundaries().isEmpty());
            assertTrue(r.classifications().isEmpty());
        }

        @Test
        void unsafeResult() {
            var violation = new ThreadSafetyResult.Violation("read", READ_ONLY, "write", EXCLUSIVE);
            var r = new ThreadSafetyResult(false, List.of(violation), List.of(), List.of());
            assertFalse(r.isSafe());
            assertEquals(1, r.violations().size());
        }

        @Test
        void withTrustBoundaries() {
            var mc = new MethodClassification("read", READ_ONLY, "annotation");
            var r = new ThreadSafetyResult(true, List.of(), List.of("read"), List.of(mc));
            assertEquals(List.of("read"), r.trustBoundaries());
            assertEquals(1, r.classifications().size());
        }

        @Test
        void nullViolations() {
            assertThrows(NullPointerException.class,
                    () -> new ThreadSafetyResult(true, null, List.of(), List.of()));
        }

        @Test
        void nullTrustBoundaries() {
            assertThrows(NullPointerException.class,
                    () -> new ThreadSafetyResult(true, List.of(), null, List.of()));
        }

        @Test
        void nullClassifications() {
            assertThrows(NullPointerException.class,
                    () -> new ThreadSafetyResult(true, List.of(), List.of(), null));
        }

        @Test
        void violationsImmutable() {
            var r = new ThreadSafetyResult(true, List.of(), List.of(), List.of());
            assertThrows(UnsupportedOperationException.class,
                    () -> r.violations().add(
                            new ThreadSafetyResult.Violation("a", EXCLUSIVE, "b", EXCLUSIVE)));
        }

        @Test
        void trustBoundariesImmutable() {
            var r = new ThreadSafetyResult(true, List.of(), List.of(), List.of());
            assertThrows(UnsupportedOperationException.class,
                    () -> r.trustBoundaries().add("x"));
        }

        @Test
        void classificationsImmutable() {
            var r = new ThreadSafetyResult(true, List.of(), List.of(), List.of());
            assertThrows(UnsupportedOperationException.class,
                    () -> r.classifications().add(
                            new MethodClassification("x", EXCLUSIVE, "test")));
        }
    }
}
