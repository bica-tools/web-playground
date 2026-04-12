package com.bica.reborn.concurrency;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.bica.reborn.concurrency.ConcurrencyLevel.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MethodClassification} and {@link MethodClassifier}.
 */
class MethodClassifierTest {

    // =========================================================================
    // MethodClassification record
    // =========================================================================

    @Nested
    class ClassificationRecord {

        @Test
        void fields() {
            var mc = new MethodClassification("read", READ_ONLY, "annotation @ReadOnly");
            assertEquals("read", mc.methodName());
            assertEquals(READ_ONLY, mc.level());
            assertEquals("annotation @ReadOnly", mc.source());
        }

        @Test
        void nullMethodName() {
            assertThrows(NullPointerException.class,
                    () -> new MethodClassification(null, EXCLUSIVE, "default"));
        }

        @Test
        void nullLevel() {
            assertThrows(NullPointerException.class,
                    () -> new MethodClassification("read", null, "default"));
        }

        @Test
        void nullSource() {
            assertThrows(NullPointerException.class,
                    () -> new MethodClassification("read", READ_ONLY, null));
        }

        @Test
        void equality() {
            var a = new MethodClassification("read", READ_ONLY, "map");
            var b = new MethodClassification("read", READ_ONLY, "map");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void inequality() {
            var a = new MethodClassification("read", READ_ONLY, "map");
            var b = new MethodClassification("write", EXCLUSIVE, "map");
            assertNotEquals(a, b);
        }
    }

    // =========================================================================
    // MethodClassifier.fromMap
    // =========================================================================

    @Nested
    class FromMap {

        @Test
        void lookupKnownMethod() {
            var classifier = MethodClassifier.fromMap(Map.of(
                    "read", READ_ONLY,
                    "write", EXCLUSIVE));
            var result = classifier.classify("read");
            assertNotNull(result);
            assertEquals("read", result.methodName());
            assertEquals(READ_ONLY, result.level());
            assertEquals("map", result.source());
        }

        @Test
        void lookupUnknownReturnsNull() {
            var classifier = MethodClassifier.fromMap(Map.of("read", READ_ONLY));
            assertNull(classifier.classify("write"));
        }

        @Test
        void emptyMap() {
            var classifier = MethodClassifier.fromMap(Map.of());
            assertNull(classifier.classify("anything"));
        }

        @Test
        void nullMapThrows() {
            assertThrows(NullPointerException.class,
                    () -> MethodClassifier.fromMap(null));
        }

        @Test
        void mapIsCopied() {
            var map = new java.util.HashMap<String, ConcurrencyLevel>();
            map.put("read", READ_ONLY);
            var classifier = MethodClassifier.fromMap(map);
            map.put("write", EXCLUSIVE); // modify original
            assertNull(classifier.classify("write")); // should not see mutation
        }

        @Test
        void allLevels() {
            var classifier = MethodClassifier.fromMap(Map.of(
                    "a", EXCLUSIVE,
                    "b", READ_ONLY,
                    "c", SYNC,
                    "d", SHARED));
            assertEquals(EXCLUSIVE, classifier.classify("a").level());
            assertEquals(READ_ONLY, classifier.classify("b").level());
            assertEquals(SYNC, classifier.classify("c").level());
            assertEquals(SHARED, classifier.classify("d").level());
        }
    }

    // =========================================================================
    // Lambda implementation
    // =========================================================================

    @Nested
    class LambdaClassifier {

        @Test
        void customLambda() {
            MethodClassifier classifier = name ->
                    new MethodClassification(name, SHARED, "custom");
            var result = classifier.classify("anything");
            assertEquals(SHARED, result.level());
            assertEquals("custom", result.source());
        }
    }
}
