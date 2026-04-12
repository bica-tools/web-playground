package com.bica.reborn.contextfree;

import com.bica.reborn.ast.*;
import com.bica.reborn.parser.Parser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Chomsky classification (Phase 13).
 *
 * <p>Mirrors the 23 Python tests in test_context_free.py.
 */
class ContextFreeCheckerTest {

    private static SessionType parse(String input) {
        return Parser.parse(input);
    }

    // =========================================================================
    // Regularity
    // =========================================================================

    @Nested
    class Regularity {

        @Test
        void endRegular() {
            assertTrue(ContextFreeChecker.isRegular(new End()));
        }

        @Test
        void simpleBranchRegular() {
            assertTrue(ContextFreeChecker.isRegular(parse("&{a: end, b: end}")));
        }

        @Test
        void tailRecursiveRegular() {
            assertTrue(ContextFreeChecker.isRegular(parse("rec X . &{a: X, b: end}")));
        }

        @Test
        void nestedTailRecursiveRegular() {
            assertTrue(ContextFreeChecker.isRegular(parse(
                    "rec X . &{a: rec Y . &{b: Y, c: X}, d: end}")));
        }

        @Test
        void iteratorRegular() {
            assertTrue(ContextFreeChecker.isRegular(parse(
                    "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")));
        }

        @Test
        void selectionRegular() {
            assertTrue(ContextFreeChecker.isRegular(parse("+{a: end, b: end}")));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "&{a: end, b: end}",
                "+{a: end}",
                "rec X . &{a: X, b: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
        })
        void standardTypesRegular(String typeStr) {
            assertTrue(ContextFreeChecker.isRegular(parse(typeStr)),
                    typeStr + " should be regular");
        }
    }

    // =========================================================================
    // Continuation recursion
    // =========================================================================

    @Nested
    class ContinuationRecursion {

        @Test
        void noContinuationRecursionSimple() {
            assertFalse(ContextFreeChecker.hasContinuationRecursion(
                    parse("rec X . &{a: X}")));
        }

        @Test
        void noContinuationRecursionEnd() {
            assertFalse(ContextFreeChecker.hasContinuationRecursion(new End()));
        }

        @Test
        void noContinuationRecursionNonRec() {
            assertFalse(ContextFreeChecker.hasContinuationRecursion(
                    parse("&{a: end, b: end}")));
        }

        @Test
        void noContinuationRecursionIterator() {
            assertFalse(ContextFreeChecker.hasContinuationRecursion(parse(
                    "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")));
        }
    }

    // =========================================================================
    // Stack depth
    // =========================================================================

    @Nested
    class StackDepth {

        @Test
        void noRecursionZero() {
            assertEquals(0, ContextFreeChecker.stackDepthBound(new End()));
        }

        @Test
        void tailRecursiveZero() {
            assertEquals(0, ContextFreeChecker.stackDepthBound(
                    parse("rec X . &{a: X, b: end}")));
        }

        @Test
        void simpleRegularZero() {
            assertEquals(0, ContextFreeChecker.stackDepthBound(parse("&{a: end}")));
        }

        @Test
        void selectionZero() {
            assertEquals(0, ContextFreeChecker.stackDepthBound(
                    parse("+{a: end, b: end}")));
        }
    }

    // =========================================================================
    // Chomsky classification
    // =========================================================================

    @Nested
    class Classification {

        @Test
        void endFinite() {
            var result = ContextFreeChecker.classifyChomsky(new End());
            assertEquals("regular", result.level());
            assertTrue(result.isRegular());
            assertTrue(result.traceLanguageClass().contains("finite"));
        }

        @Test
        void branchFinite() {
            var result = ContextFreeChecker.classifyChomsky(parse("&{a: end}"));
            assertEquals("regular", result.level());
            assertEquals(0, result.numRecBinders());
        }

        @Test
        void tailRecursiveClassifiedRegular() {
            var result = ContextFreeChecker.classifyChomsky(
                    parse("rec X . &{a: X, b: end}"));
            assertEquals("regular", result.level());
            assertTrue(result.isRegular());
            assertTrue(result.traceLanguageClass().contains("tail-recursive"));
        }

        @Test
        void resultFields() {
            var result = ContextFreeChecker.classifyChomsky(
                    parse("rec X . &{a: X, b: end}"));
            assertInstanceOf(ChomskyClassification.class, result);
            assertEquals(1, result.numRecBinders());
            assertEquals(1, result.maxRecDepth());
            assertEquals(0, result.stackDepthBound());
        }

        @Test
        void nestedRecClassification() {
            var result = ContextFreeChecker.classifyChomsky(
                    parse("rec X . &{a: rec Y . &{b: Y, c: X}}"));
            assertEquals("regular", result.level());
            assertEquals(2, result.numRecBinders());
            assertEquals(2, result.maxRecDepth());
        }
    }

    // =========================================================================
    // Trace language analysis
    // =========================================================================

    @Nested
    class TraceLanguage {

        @Test
        void finiteTrace() {
            var result = ContextFreeChecker.analyzeTraceLanguage(parse("&{a: end}"));
            assertTrue(result.isFinite());
            assertTrue(result.isRegular());
            assertEquals("regular", result.chomskyClass());
            assertFalse(result.usesRecursion());
        }

        @Test
        void regularTrace() {
            var result = ContextFreeChecker.analyzeTraceLanguage(
                    parse("rec X . &{a: X, b: end}"));
            assertFalse(result.isFinite());
            assertTrue(result.isRegular());
            assertTrue(result.usesRecursion());
        }

        @Test
        void stateCount() {
            var result = ContextFreeChecker.analyzeTraceLanguage(
                    parse("&{a: end, b: end}"));
            assertEquals(2, result.stateCount());
            assertEquals(2, result.transitionCount());
        }

        @Test
        void noParallelNoSequence() {
            var result = ContextFreeChecker.analyzeTraceLanguage(parse("&{a: end}"));
            assertFalse(result.usesParallel());
            assertFalse(result.usesSequence());
        }

        @Test
        void resultType() {
            var result = ContextFreeChecker.analyzeTraceLanguage(new End());
            assertInstanceOf(TraceLanguageAnalysis.class, result);
        }
    }

    // =========================================================================
    // AST predicates
    // =========================================================================

    @Nested
    class AstPredicates {

        @Test
        void noParallelInBranch() {
            assertFalse(ContextFreeChecker.containsParallel(parse("&{a: end}")));
        }

        @Test
        void noSequenceInBranch() {
            assertFalse(ContextFreeChecker.containsSequence(parse("&{a: end}")));
        }

        @Test
        void endHasNeither() {
            assertFalse(ContextFreeChecker.containsParallel(new End()));
            assertFalse(ContextFreeChecker.containsSequence(new End()));
        }

        @Test
        void selectionNoParallel() {
            assertFalse(ContextFreeChecker.containsParallel(
                    parse("+{a: end, b: end}")));
        }

        @Test
        void recursionNoParallel() {
            assertFalse(ContextFreeChecker.containsParallel(
                    parse("rec X . &{a: X, b: end}")));
        }
    }
}
