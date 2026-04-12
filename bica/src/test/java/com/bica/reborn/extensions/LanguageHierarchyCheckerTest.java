package com.bica.reborn.extensions;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.parser.Parser;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LanguageHierarchyChecker} -- language hierarchy L0-L9.
 */
class LanguageHierarchyCheckerTest {

    private static SessionType parse(String source) {
        return Parser.parse(source);
    }

    // =========================================================================
    // L0 -- end only
    // =========================================================================

    @Nested
    class L0Classification {

        @Test
        void endIsL0() {
            assertEquals(LanguageLevel.L0,
                    LanguageHierarchyChecker.classifyLevel(new End()));
        }

        @Test
        void endParsedIsL0() {
            assertEquals(LanguageLevel.L0,
                    LanguageHierarchyChecker.classifyLevel(parse("end")));
        }
    }

    // =========================================================================
    // L1 -- branch
    // =========================================================================

    @Nested
    class L1Classification {

        @Test
        void simpleBranchIsL1() {
            assertEquals(LanguageLevel.L1,
                    LanguageHierarchyChecker.classifyLevel(parse("&{a: end}")));
        }

        @Test
        void nestedBranchIsL1() {
            assertEquals(LanguageLevel.L1,
                    LanguageHierarchyChecker.classifyLevel(parse("&{a: &{b: end}}")));
        }

        @Test
        void multiBranchIsL1() {
            assertEquals(LanguageLevel.L1,
                    LanguageHierarchyChecker.classifyLevel(parse("&{a: end, b: end}")));
        }
    }

    // =========================================================================
    // L2 -- selection
    // =========================================================================

    @Nested
    class L2Classification {

        @Test
        void simpleSelectionIsL2() {
            assertEquals(LanguageLevel.L2,
                    LanguageHierarchyChecker.classifyLevel(parse("+{a: end}")));
        }

        @Test
        void branchWithSelectionIsL2() {
            assertEquals(LanguageLevel.L2,
                    LanguageHierarchyChecker.classifyLevel(parse("&{a: +{b: end}}")));
        }

        @Test
        void selectionWithBranchIsL2() {
            assertEquals(LanguageLevel.L2,
                    LanguageHierarchyChecker.classifyLevel(parse("+{a: &{b: end}}")));
        }
    }

    // =========================================================================
    // L3 -- parallel
    // =========================================================================

    @Nested
    class L3Classification {

        @Test
        void simpleParallelIsL3() {
            assertEquals(LanguageLevel.L3,
                    LanguageHierarchyChecker.classifyLevel(parse("(&{a: end} || &{b: end})")));
        }

        @Test
        void parallelWithSelectionIsL3() {
            assertEquals(LanguageLevel.L3,
                    LanguageHierarchyChecker.classifyLevel(parse("(+{a: end} || &{b: end})")));
        }
    }

    // =========================================================================
    // L4 -- recursion
    // =========================================================================

    @Nested
    class L4Classification {

        @Test
        void simpleRecIsL4() {
            assertEquals(LanguageLevel.L4,
                    LanguageHierarchyChecker.classifyLevel(parse("rec X . &{a: X, b: end}")));
        }

        @Test
        void recWithSelectionIsL4() {
            assertEquals(LanguageLevel.L4,
                    LanguageHierarchyChecker.classifyLevel(parse("rec X . +{a: X, b: end}")));
        }
    }

    // =========================================================================
    // Constructor predicates
    // =========================================================================

    @Nested
    class Predicates {

        @Test
        void hasBranch_true() {
            assertTrue(LanguageHierarchyChecker.hasBranch(parse("&{a: end}")));
        }

        @Test
        void hasBranch_false() {
            assertFalse(LanguageHierarchyChecker.hasBranch(new End()));
        }

        @Test
        void hasSelection_true() {
            assertTrue(LanguageHierarchyChecker.hasSelection(parse("+{a: end}")));
        }

        @Test
        void hasSelection_false() {
            assertFalse(LanguageHierarchyChecker.hasSelection(parse("&{a: end}")));
        }

        @Test
        void hasParallel_true() {
            assertTrue(LanguageHierarchyChecker.hasParallel(
                    parse("(&{a: end} || &{b: end})")));
        }

        @Test
        void hasParallel_false() {
            assertFalse(LanguageHierarchyChecker.hasParallel(parse("&{a: end}")));
        }

        @Test
        void hasRecursion_true() {
            assertTrue(LanguageHierarchyChecker.hasRecursion(
                    parse("rec X . &{a: X, b: end}")));
        }

        @Test
        void hasRecursion_false() {
            assertFalse(LanguageHierarchyChecker.hasRecursion(parse("&{a: end}")));
        }
    }

    // =========================================================================
    // Separation witnesses
    // =========================================================================

    @Nested
    class SeparationWitnesses {

        @Test
        void witnessL0L1() {
            String witness = LanguageHierarchyChecker.separationWitness(
                    LanguageLevel.L0, LanguageLevel.L1);
            assertNotNull(witness);
            assertFalse(witness.isEmpty());
        }

        @Test
        void witnessL1L2() {
            String witness = LanguageHierarchyChecker.separationWitness(
                    LanguageLevel.L0, LanguageLevel.L2);
            assertNotNull(witness);
        }

        @Test
        void witnessL2L3() {
            String witness = LanguageHierarchyChecker.separationWitness(
                    LanguageLevel.L2, LanguageLevel.L3);
            assertNotNull(witness);
        }

        @Test
        void witnessL3L4() {
            String witness = LanguageHierarchyChecker.separationWitness(
                    LanguageLevel.L3, LanguageLevel.L4);
            assertNotNull(witness);
        }

        @Test
        void witnessInvalidThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> LanguageHierarchyChecker.separationWitness(
                            LanguageLevel.L2, LanguageLevel.L1));
        }

        @Test
        void witnessEqualThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> LanguageHierarchyChecker.separationWitness(
                            LanguageLevel.L2, LanguageLevel.L2));
        }

        @Test
        void witnessL0Invalid() {
            assertThrows(IllegalArgumentException.class,
                    () -> LanguageHierarchyChecker.separationWitness(
                            LanguageLevel.L1, LanguageLevel.L0));
        }
    }

    // =========================================================================
    // Level descriptions
    // =========================================================================

    @Nested
    class Descriptions {

        @Test
        void allLevelsHaveDescriptions() {
            for (LanguageLevel level : LanguageLevel.values()) {
                String desc = LanguageHierarchyChecker.levelDescription(level);
                assertNotNull(desc);
                assertFalse(desc.isEmpty());
            }
        }

        @Test
        void l0Description() {
            assertTrue(LanguageHierarchyChecker.levelDescription(LanguageLevel.L0)
                    .toLowerCase().contains("terminated"));
        }
    }

    // =========================================================================
    // Monotonicity: level never decreases when adding constructors
    // =========================================================================

    @Nested
    class Monotonicity {

        @Test
        void addingBranchIncreasesOrKeepsLevel() {
            SessionType base = new End();
            SessionType withBranch = new Branch(List.of(new Choice("a", base)));
            assertTrue(LanguageHierarchyChecker.classifyLevel(withBranch).compareTo(
                    LanguageHierarchyChecker.classifyLevel(base)) >= 0);
        }

        @Test
        void addingSelectionIncreasesOrKeepsLevel() {
            SessionType base = new Branch(List.of(new Choice("a", new End())));
            SessionType withSelect = new Select(List.of(new Choice("x", base)));
            assertTrue(LanguageHierarchyChecker.classifyLevel(withSelect).compareTo(
                    LanguageHierarchyChecker.classifyLevel(base)) >= 0);
        }
    }
}
