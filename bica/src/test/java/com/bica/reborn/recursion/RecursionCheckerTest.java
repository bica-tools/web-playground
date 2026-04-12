package com.bica.reborn.recursion;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for recursion analysis (Phase 12).
 *
 * <p>Mirrors the 50 Python tests in test_recursion.py.
 */
class RecursionCheckerTest {

    private static SessionType parse(String input) {
        return Parser.parse(input);
    }

    // =========================================================================
    // Guardedness
    // =========================================================================

    @Nested
    class Guardedness {

        @Test
        void endIsGuarded() {
            assertTrue(RecursionChecker.isGuarded(new End()));
        }

        @Test
        void simpleBranchGuarded() {
            assertTrue(RecursionChecker.isGuarded(parse("&{a: end}")));
        }

        @Test
        void guardedRecursion() {
            // rec X . &{a: X} — X is under Branch, so guarded
            assertTrue(RecursionChecker.isGuarded(parse("rec X . &{a: X}")));
        }

        @Test
        void unguardedRecursion() {
            // rec X . X — X is NOT under any constructor
            assertFalse(RecursionChecker.isGuarded(parse("rec X . X")));
        }

        @Test
        void unguardedVarReported() {
            var result = RecursionChecker.checkGuardedness(parse("rec X . X"));
            assertFalse(result.isGuarded());
            assertEquals(List.of("X"), result.unguardedVars());
        }

        @Test
        void nestedRecGuarded() {
            // rec X . &{a: rec Y . &{b: Y, c: X}} — both guarded
            assertTrue(RecursionChecker.isGuarded(
                    parse("rec X . &{a: rec Y . &{b: Y, c: X}}")));
        }

        @Test
        void nestedRecOneUnguarded() {
            // rec X . rec Y . &{a: X, b: Y} — Y is unguarded (body of outer rec
            // goes directly to inner rec without constructor)
            var result = RecursionChecker.checkGuardedness(
                    parse("rec X . rec Y . &{a: X, b: Y}"));
            // X is unguarded too: rec X . (rec Y . ...) — X is not under constructor
            // between rec X and its use; but &{a: X, b: Y} puts X under Branch
            // Actually: rec X . rec Y . &{a: X, b: Y}
            //   X is unguarded in "rec Y . &{a: X, b: Y}" which is body of rec X
            //   But then X appears inside &{...} which IS a constructor → X is guarded
            //   Y is bound by inner rec, body is &{a: X, b: Y}, Y under Branch → guarded
            assertTrue(result.isGuarded());
        }

        @Test
        void selectionGuards() {
            assertTrue(RecursionChecker.isGuarded(parse("rec X . +{a: X}")));
        }

        @Test
        void parallelGuards() {
            assertTrue(RecursionChecker.isGuarded(
                    parse("rec X . (&{a: end} || &{b: X})")));
        }

        @Test
        void multipleUnguardedVars() {
            // rec X . rec Y . X — both X and Y are unguarded
            // Actually: X appears directly (not under constructor) in body of rec Y
            // and Y appears... wait, Y doesn't appear at all. Let's use a better example.
            // Build manually: rec X . rec Y . Y
            var type = new Rec("X", new Rec("Y", new Var("Y")));
            var result = RecursionChecker.checkGuardedness(type);
            assertFalse(result.isGuarded());
            assertTrue(result.unguardedVars().contains("Y"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "+{a: end, b: end}",
                "rec X . &{a: X, b: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
        })
        void standardTypesGuarded(String typeStr) {
            assertTrue(RecursionChecker.isGuarded(parse(typeStr)),
                    typeStr + " should be guarded");
        }
    }

    // =========================================================================
    // Contractivity
    // =========================================================================

    @Nested
    class Contractivity {

        @Test
        void endIsContractive() {
            assertTrue(RecursionChecker.isContractive(new End()));
        }

        @Test
        void simpleRecContractive() {
            // rec X . &{a: X} — body is Branch (not Var or Rec)
            assertTrue(RecursionChecker.isContractive(parse("rec X . &{a: X}")));
        }

        @Test
        void recVarNotContractive() {
            // rec X . X — body IS a Var
            assertFalse(RecursionChecker.isContractive(parse("rec X . X")));
        }

        @Test
        void recRecNotContractive() {
            // rec X . rec Y . X — outer body is Rec (not contractive for X)
            var result = RecursionChecker.checkContractivity(
                    parse("rec X . rec Y . X"));
            assertFalse(result.isContractive());
            assertTrue(result.nonContractiveVars().contains("X"));
        }

        @Test
        void nonContractiveVarReported() {
            var result = RecursionChecker.checkContractivity(parse("rec X . X"));
            assertEquals(List.of("X"), result.nonContractiveVars());
        }

        @Test
        void nestedContractiveOk() {
            // rec X . &{a: rec Y . &{b: Y}} — both bodies start with Branch
            assertTrue(RecursionChecker.isContractive(
                    parse("rec X . &{a: rec Y . &{b: Y}}")));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "rec X . &{a: X}",
                "rec X . +{a: X, b: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
        })
        void standardTypesContractive(String typeStr) {
            assertTrue(RecursionChecker.isContractive(parse(typeStr)),
                    typeStr + " should be contractive");
        }
    }

    // =========================================================================
    // Unfolding
    // =========================================================================

    @Nested
    class Unfolding {

        @Test
        void unfoldNonRec() {
            var end = new End();
            assertSame(end, RecursionChecker.unfold(end));
        }

        @Test
        void unfoldSimple() {
            // rec X . &{a: X} → &{a: rec X . &{a: X}}
            var type = parse("rec X . &{a: X}");
            var unfolded = RecursionChecker.unfold(type);
            assertInstanceOf(Branch.class, unfolded);
            var branch = (Branch) unfolded;
            assertEquals(1, branch.choices().size());
            assertEquals("a", branch.choices().getFirst().label());
            // continuation should be the original rec
            assertInstanceOf(Rec.class, branch.choices().getFirst().body());
        }

        @Test
        void unfoldDepthZero() {
            var type = parse("rec X . &{a: X}");
            assertSame(type, RecursionChecker.unfoldDepth(type, 0));
        }

        @Test
        void unfoldDepthOne() {
            var type = parse("rec X . &{a: X}");
            var unfolded = RecursionChecker.unfoldDepth(type, 1);
            assertInstanceOf(Branch.class, unfolded);
        }

        @Test
        void unfoldDepthTwo() {
            var type = parse("rec X . &{a: X}");
            var unfolded = RecursionChecker.unfoldDepth(type, 2);
            // After 1 unfold: &{a: rec X . &{a: X}} — no longer Rec at top
            // So depth 2 has same effect as depth 1
            assertInstanceOf(Branch.class, unfolded);
        }

        @Test
        void unfoldStopsAtNonRec() {
            var end = new End();
            assertEquals(end, RecursionChecker.unfoldDepth(end, 5));
        }

        @Test
        void substituteSimple() {
            var body = new Var("X");
            var replacement = new End();
            assertEquals(replacement, RecursionChecker.substitute(body, "X", replacement));
        }

        @Test
        void substituteShadowed() {
            // rec X . X — substituting X should not go inside rec X
            var inner = new Rec("X", new Var("X"));
            var result = RecursionChecker.substitute(inner, "X", new End());
            // The outer rec shadows X, so no substitution inside
            assertInstanceOf(Rec.class, result);
        }
    }

    // =========================================================================
    // Metrics
    // =========================================================================

    @Nested
    class Metrics {

        @Test
        void recDepthEnd() {
            assertEquals(0, RecursionChecker.recDepth(new End()));
        }

        @Test
        void recDepthSimple() {
            assertEquals(1, RecursionChecker.recDepth(parse("rec X . &{a: X}")));
        }

        @Test
        void recDepthNested() {
            assertEquals(2, RecursionChecker.recDepth(
                    parse("rec X . &{a: rec Y . &{b: Y}}")));
        }

        @Test
        void recDepthNoRec() {
            assertEquals(0, RecursionChecker.recDepth(parse("&{a: end, b: end}")));
        }

        @Test
        void countRecBindersZero() {
            assertEquals(0, RecursionChecker.countRecBinders(new End()));
        }

        @Test
        void countRecBindersOne() {
            assertEquals(1, RecursionChecker.countRecBinders(parse("rec X . &{a: X}")));
        }

        @Test
        void countRecBindersTwo() {
            assertEquals(2, RecursionChecker.countRecBinders(
                    parse("rec X . &{a: rec Y . &{b: Y}}")));
        }

        @Test
        void recursiveVarsEmpty() {
            assertTrue(RecursionChecker.recursiveVars(new End()).isEmpty());
        }

        @Test
        void recursiveVarsOne() {
            assertEquals(Set.of("X"),
                    RecursionChecker.recursiveVars(parse("rec X . &{a: X}")));
        }

        @Test
        void recursiveVarsMultiple() {
            assertEquals(Set.of("X", "Y"),
                    RecursionChecker.recursiveVars(
                            parse("rec X . &{a: rec Y . &{b: Y}, c: X}")));
        }
    }

    // =========================================================================
    // Tail recursion
    // =========================================================================

    @Nested
    class TailRecursion {

        @Test
        void endIsTailRecursive() {
            assertTrue(RecursionChecker.isTailRecursive(new End()));
        }

        @Test
        void simpleTailRecursion() {
            // rec X . &{a: X, b: end} — X is directly in Branch choice (tail)
            assertTrue(RecursionChecker.isTailRecursive(
                    parse("rec X . &{a: X, b: end}")));
        }

        @Test
        void iteratorTailRecursive() {
            assertTrue(RecursionChecker.isTailRecursive(
                    parse("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")));
        }

        @Test
        void nonRecursiveIsTailRecursive() {
            assertTrue(RecursionChecker.isTailRecursive(parse("&{a: end, b: end}")));
        }

        @Test
        void noRecTypes() {
            assertTrue(RecursionChecker.isTailRecursive(parse("&{a: &{b: end}}")));
        }
    }

    // =========================================================================
    // SCC analysis
    // =========================================================================

    @Nested
    class SccAnalysis {

        @Test
        void endNoSccs() {
            int[] result = RecursionChecker.analyzeSccs(new End());
            assertTrue(result[0] > 0); // at least 1 SCC (the single state)
        }

        @Test
        void nonRecursiveAllTrivial() {
            int[] result = RecursionChecker.analyzeSccs(parse("&{a: end}"));
            // All SCCs should be trivial (size 1) → no non-trivial entries
            assertEquals(1, result.length); // just sccCount, no sizes
        }

        @Test
        void recursiveHasScc() {
            int[] result = RecursionChecker.analyzeSccs(parse("rec X . &{a: X, b: end}"));
            // Recursive type creates a state space with SCCs
            assertTrue(result[0] > 0, "Should have at least one SCC");
        }

        @Test
        void sccCountPositive() {
            int[] result = RecursionChecker.analyzeSccs(parse("&{a: &{b: end}, c: end}"));
            assertTrue(result[0] > 0);
        }
    }

    // =========================================================================
    // Complete analysis
    // =========================================================================

    @Nested
    class CompleteAnalysis {

        @Test
        void endAnalysis() {
            var analysis = RecursionChecker.analyzeRecursion(new End());
            assertEquals(0, analysis.numRecBinders());
            assertEquals(0, analysis.maxNestingDepth());
            assertTrue(analysis.isGuarded());
            assertTrue(analysis.isContractive());
            assertTrue(analysis.recursiveVars().isEmpty());
            assertTrue(analysis.isTailRecursive());
        }

        @Test
        void simpleRecAnalysis() {
            var analysis = RecursionChecker.analyzeRecursion(
                    parse("rec X . &{a: X, b: end}"));
            assertEquals(1, analysis.numRecBinders());
            assertEquals(1, analysis.maxNestingDepth());
            assertTrue(analysis.isGuarded());
            assertTrue(analysis.isContractive());
            assertEquals(Set.of("X"), analysis.recursiveVars());
            assertTrue(analysis.isTailRecursive());
            assertTrue(analysis.sccCount() > 0);
        }

        @Test
        void iteratorAnalysis() {
            var analysis = RecursionChecker.analyzeRecursion(
                    parse("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertEquals(1, analysis.numRecBinders());
            assertTrue(analysis.isGuarded());
            assertTrue(analysis.isContractive());
            assertEquals(Set.of("X"), analysis.recursiveVars());
            assertTrue(analysis.isTailRecursive());
        }

        @Test
        void nestedRecAnalysis() {
            var analysis = RecursionChecker.analyzeRecursion(
                    parse("rec X . &{a: rec Y . &{b: Y, c: X}}"));
            assertEquals(2, analysis.numRecBinders());
            assertEquals(2, analysis.maxNestingDepth());
            assertTrue(analysis.isGuarded());
            assertTrue(analysis.isContractive());
            assertEquals(Set.of("X", "Y"), analysis.recursiveVars());
        }

        @Test
        void unguardedAnalysis() {
            var analysis = RecursionChecker.analyzeRecursion(parse("rec X . X"));
            assertFalse(analysis.isGuarded());
            assertFalse(analysis.isContractive());
        }

        @Test
        void resultType() {
            var analysis = RecursionChecker.analyzeRecursion(parse("&{a: end}"));
            assertInstanceOf(RecursionAnalysis.class, analysis);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "rec X . &{a: X, b: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "rec X . &{a: rec Y . &{b: Y, c: X}}",
        })
        void wellFormedTypesGuardedAndContractive(String typeStr) {
            var analysis = RecursionChecker.analyzeRecursion(parse(typeStr));
            assertTrue(analysis.isGuarded(),
                    typeStr + " should be guarded");
            assertTrue(analysis.isContractive(),
                    typeStr + " should be contractive");
        }
    }
}
