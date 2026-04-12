package com.bica.reborn.endomorphism;

import com.bica.reborn.ast.End;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for transitions as lattice endomorphisms (Step 10 / Phase 10).
 *
 * <p>Mirrors the 23 Python tests in test_endomorphism.py.
 */
class EndomorphismCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // =========================================================================
    // Transition map extraction
    // =========================================================================

    @Nested
    class TransitionMapExtraction {

        @Test
        void singleBranch() {
            var tmaps = EndomorphismChecker.extractTransitionMaps(build("&{a: end}"));
            assertEquals(1, tmaps.size());
            assertEquals("a", tmaps.getFirst().label());
            assertEquals(1, tmaps.getFirst().domainSize());
        }

        @Test
        void twoMethods() {
            var tmaps = EndomorphismChecker.extractTransitionMaps(build("&{a: end, b: end}"));
            assertEquals(2, tmaps.size());
            var labels = tmaps.stream().map(TransitionMap::label).collect(Collectors.toSet());
            assertEquals(Set.of("a", "b"), labels);
        }

        @Test
        void selectionMarked() {
            var tmaps = EndomorphismChecker.extractTransitionMaps(build("+{a: end, b: end}"));
            assertTrue(tmaps.stream().allMatch(TransitionMap::isSelection));
        }

        @Test
        void branchNotSelection() {
            var tmaps = EndomorphismChecker.extractTransitionMaps(build("&{a: end}"));
            assertFalse(tmaps.getFirst().isSelection());
        }

        @Test
        void nestedSameLabel() {
            // Label 'a' appears at two levels → one TransitionMap with domain size 2
            var tmaps = EndomorphismChecker.extractTransitionMaps(build("&{a: &{a: end}}"));
            var aMaps = tmaps.stream().filter(t -> t.label().equals("a")).toList();
            assertEquals(1, aMaps.size());
            assertEquals(2, aMaps.getFirst().domainSize());
        }

        @Test
        void recursiveLabels() {
            var tmaps = EndomorphismChecker.extractTransitionMaps(
                    build("rec X . &{a: X, b: end}"));
            var labels = tmaps.stream().map(TransitionMap::label).collect(Collectors.toSet());
            assertTrue(labels.contains("a"));
            assertTrue(labels.contains("b"));
        }
    }

    // =========================================================================
    // Order-preserving checks
    // =========================================================================

    @Nested
    class OrderPreserving {

        @Test
        void singleMethodTriviallyMonotone() {
            var ss = build("&{a: end}");
            var tmaps = EndomorphismChecker.extractTransitionMaps(ss);
            var result = EndomorphismChecker.checkEndomorphism(ss, tmaps.getFirst());
            assertTrue(result.isOrderPreserving());
        }

        @Test
        void nestedBranchMonotone() {
            var ss = build("&{a: &{b: end}}");
            var tmaps = EndomorphismChecker.extractTransitionMaps(ss);
            for (var tm : tmaps) {
                var result = EndomorphismChecker.checkEndomorphism(ss, tm);
                assertTrue(result.isOrderPreserving(),
                        "Label '%s' not order-preserving".formatted(tm.label()));
            }
        }

        @Test
        void sharedLabelMonotone() {
            var ss = build("&{a: &{a: end}}");
            var tmaps = EndomorphismChecker.extractTransitionMaps(ss);
            var aMap = tmaps.stream().filter(t -> t.label().equals("a")).findFirst().orElseThrow();
            var result = EndomorphismChecker.checkEndomorphism(ss, aMap);
            assertTrue(result.isOrderPreserving());
        }

        @Test
        void recursiveMonotone() {
            var summary = EndomorphismChecker.checkAll(build("rec X . &{a: X, b: end}"));
            assertTrue(summary.allOrderPreserving());
        }
    }

    // =========================================================================
    // Meet/join preservation
    // =========================================================================

    @Nested
    class MeetJoinPreservation {

        @Test
        void singleMethodTriviallyPreserves() {
            var ss = build("&{a: end}");
            var tmaps = EndomorphismChecker.extractTransitionMaps(ss);
            var result = EndomorphismChecker.checkEndomorphism(ss, tmaps.getFirst());
            assertTrue(result.isMeetPreserving());
            assertTrue(result.isJoinPreserving());
            assertTrue(result.isEndomorphism());
        }

        @Test
        void twoMethodsToEnd() {
            var summary = EndomorphismChecker.checkAll(build("&{a: end, b: end}"));
            assertTrue(summary.allMeetPreserving());
            assertTrue(summary.allJoinPreserving());
        }

        @Test
        void nestedPreserves() {
            var summary = EndomorphismChecker.checkAll(build("&{a: &{b: end, c: end}}"));
            assertTrue(summary.allMeetPreserving());
            assertTrue(summary.allJoinPreserving());
        }

        @Test
        void selectionPreserves() {
            var summary = EndomorphismChecker.checkAll(build("+{a: end, b: end}"));
            assertTrue(summary.allEndomorphisms());
        }
    }

    // =========================================================================
    // Full endomorphism check (checkAll)
    // =========================================================================

    @Nested
    class CheckAll {

        @Test
        void resultType() {
            var summary = EndomorphismChecker.checkAll(build("&{a: end}"));
            assertInstanceOf(EndomorphismSummary.class, summary);
        }

        @Test
        void numLabels() {
            var summary = EndomorphismChecker.checkAll(build("&{a: end, b: end, c: end}"));
            assertEquals(3, summary.numLabels());
        }

        @Test
        void endNoLabels() {
            var summary = EndomorphismChecker.checkAll(StateSpaceBuilder.build(new End()));
            assertEquals(0, summary.numLabels());
            assertTrue(summary.allEndomorphisms()); // vacuously true
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "&{a: end}",
                "&{a: end, b: end}",
                "+{a: end}",
                "+{a: end, b: end}",
                "&{a: &{b: end}}",
                "&{a: +{x: end, y: end}}",
        })
        void simpleTypesAllEndomorphisms(String typeStr) {
            var summary = EndomorphismChecker.checkAll(build(typeStr));
            assertTrue(summary.allOrderPreserving(),
                    "%s: not all order-preserving".formatted(typeStr));
        }
    }

    // =========================================================================
    // Iterator benchmark
    // =========================================================================

    @Nested
    class IteratorBenchmark {

        @Test
        void iteratorOrderPreserving() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var summary = EndomorphismChecker.checkAll(ss);
            assertTrue(summary.allOrderPreserving());
        }
    }

    // =========================================================================
    // Counterexample inspection
    // =========================================================================

    @Nested
    class Counterexamples {

        @Test
        void noCounterexamplesWhenPassing() {
            var ss = build("&{a: end}");
            var tmaps = EndomorphismChecker.extractTransitionMaps(ss);
            var result = EndomorphismChecker.checkEndomorphism(ss, tmaps.getFirst());
            assertNull(result.orderCounterexample());
            assertNull(result.meetCounterexample());
            assertNull(result.joinCounterexample());
        }

        @Test
        void resultFieldTypes() {
            var summary = EndomorphismChecker.checkAll(build("&{a: end, b: end}"));
            for (var r : summary.results()) {
                assertInstanceOf(EndomorphismResult.class, r);
                assertNotNull(r.label());
                assertTrue(r.domainSize() >= 0);
            }
        }
    }

    // =========================================================================
    // Mixed branch/selection types
    // =========================================================================

    @Nested
    class MixedTypes {

        @Test
        void branchWithNestedSelection() {
            var summary = EndomorphismChecker.checkAll(
                    build("&{a: +{x: end, y: end}, b: end}"));
            assertTrue(summary.allOrderPreserving());
        }

        @Test
        void selectionWithNestedBranch() {
            var summary = EndomorphismChecker.checkAll(
                    build("+{a: &{x: end}, b: &{y: end}}"));
            assertTrue(summary.allOrderPreserving());
        }

        @Test
        void deeplyNested() {
            var summary = EndomorphismChecker.checkAll(
                    build("&{a: &{b: +{c: end, d: end}}, e: end}"));
            assertTrue(summary.allOrderPreserving());
        }

        @Test
        void recursiveWithSelection() {
            var summary = EndomorphismChecker.checkAll(
                    build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertTrue(summary.allEndomorphisms());
        }
    }
}
