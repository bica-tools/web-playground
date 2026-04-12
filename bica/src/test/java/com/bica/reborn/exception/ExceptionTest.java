package com.bica.reborn.exception;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Nested
    class ProtocolViolationTests {

        @Test
        void extendsIllegalStateException() {
            var ex = new ProtocolViolationException("read", "INITIAL", Set.of("open"));
            assertInstanceOf(IllegalStateException.class, ex);
        }

        @Test
        void messageContainsAttemptedMethod() {
            var ex = new ProtocolViolationException("read", "INITIAL", Set.of("open"));
            assertTrue(ex.getMessage().contains("read"));
        }

        @Test
        void messageContainsCurrentState() {
            var ex = new ProtocolViolationException("read", "INITIAL", Set.of("open"));
            assertTrue(ex.getMessage().contains("INITIAL"));
        }

        @Test
        void messageContainsEnabledMethods() {
            var ex = new ProtocolViolationException("read", "INITIAL", Set.of("open", "close"));
            assertTrue(ex.getMessage().contains("open"));
            assertTrue(ex.getMessage().contains("close"));
        }

        @Test
        void accessorsReturnCorrectValues() {
            var ex = new ProtocolViolationException("write", "OPENED", Set.of("read", "write"));
            assertEquals("write", ex.attemptedMethod());
            assertEquals("OPENED", ex.currentState());
            assertEquals(Set.of("read", "write"), ex.enabledMethods());
        }

        @Test
        void enabledMethodsAreImmutable() {
            var enabled = new java.util.HashSet<>(Set.of("open"));
            var ex = new ProtocolViolationException("read", "INITIAL", enabled);
            assertThrows(UnsupportedOperationException.class,
                    () -> ex.enabledMethods().add("hack"));
        }

        @Test
        void twoArgConstructorWorks() {
            var ex = new ProtocolViolationException("read", "INITIAL");
            assertEquals("read", ex.attemptedMethod());
            assertEquals("INITIAL", ex.currentState());
            assertEquals(Set.of(), ex.enabledMethods());
            assertTrue(ex.getMessage().contains("read"));
        }

        @Test
        void canBeCaughtAsIllegalStateException() {
            assertThrows(IllegalStateException.class, () -> {
                throw new ProtocolViolationException("m", "S");
            });
        }
    }

    @Nested
    class IncompleteSessionTests {

        @Test
        void extendsIllegalStateException() {
            var ex = new IncompleteSessionException("OPENED", Set.of("read"));
            assertInstanceOf(IllegalStateException.class, ex);
        }

        @Test
        void messageContainsCurrentState() {
            var ex = new IncompleteSessionException("OPENED", Set.of("read", "write"));
            assertTrue(ex.getMessage().contains("OPENED"));
        }

        @Test
        void messageContainsRemainingMethods() {
            var ex = new IncompleteSessionException("OPENED", Set.of("read", "write"));
            assertTrue(ex.getMessage().contains("read"));
            assertTrue(ex.getMessage().contains("write"));
        }

        @Test
        void accessorsReturnCorrectValues() {
            var ex = new IncompleteSessionException("OPENED", Set.of("close"));
            assertEquals("OPENED", ex.currentState());
            assertEquals(Set.of("close"), ex.remainingMethods());
        }

        @Test
        void remainingMethodsAreImmutable() {
            var remaining = new java.util.HashSet<>(Set.of("close"));
            var ex = new IncompleteSessionException("OPENED", remaining);
            assertThrows(UnsupportedOperationException.class,
                    () -> ex.remainingMethods().add("hack"));
        }

        @Test
        void canBeCaughtAsIllegalStateException() {
            assertThrows(IllegalStateException.class, () -> {
                throw new IncompleteSessionException("S", Set.of("m"));
            });
        }
    }
}
