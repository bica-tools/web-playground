package com.bica.reborn.subtyping;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Gay–Hole coinductive subtyping (Step 7 / Phase 8).
 *
 * <p>Mirrors the 56 Python tests in test_subtyping.py.
 */
class SubtypingCheckerTest {

    private static SessionType parse(String input) {
        return Parser.parse(input);
    }

    // =========================================================================
    // Basic subtyping: end
    // =========================================================================

    @Nested
    class EndSubtyping {

        @Test
        void endSubtypeEnd() {
            assertTrue(SubtypingChecker.isSubtype(new End(), new End()));
        }

        @Test
        void branchIsSubtypeOfEnd() {
            // &{a: end} ≤_GH end — by Sub-Branch: ∅ ⊆ {a}
            assertTrue(SubtypingChecker.isSubtype(parse("&{a: end}"), new End()));
        }

        @Test
        void endNotSubtypeOfBranch() {
            // end ≤_GH &{a: end} — FAILS: {a} ⊄ ∅
            assertFalse(SubtypingChecker.isSubtype(new End(), parse("&{a: end}")));
        }

        @Test
        void widerBranchSubtypeOfEnd() {
            // &{a: end, b: end, c: end} ≤_GH end — ∅ ⊆ {a,b,c}
            assertTrue(SubtypingChecker.isSubtype(
                    parse("&{a: end, b: end, c: end}"), new End()));
        }

        @Test
        void endIncomparableWithSelection() {
            // end (≡ &{}) and +{a: end} are incomparable (branch vs select)
            assertFalse(SubtypingChecker.isSubtype(new End(), parse("+{a: end}")));
            assertFalse(SubtypingChecker.isSubtype(parse("+{a: end}"), new End()));
        }

        @Test
        void nestedBranchSubtypeOfEnd() {
            // &{a: &{b: end}} ≤_GH end — ∅ ⊆ {a}
            assertTrue(SubtypingChecker.isSubtype(parse("&{a: &{b: end}}"), new End()));
        }

        @Test
        void recursiveSubtypeOfEnd() {
            // rec X . &{a: X, b: end} ≤_GH end — ∅ ⊆ {a,b}
            assertTrue(SubtypingChecker.isSubtype(
                    parse("rec X . &{a: X, b: end}"), new End()));
        }
    }

    // =========================================================================
    // Branch subtyping (width + depth)
    // =========================================================================

    @Nested
    class BranchSubtyping {

        @Test
        void sameBranch() {
            var s = parse("&{a: end}");
            assertTrue(SubtypingChecker.isSubtype(s, s));
        }

        @Test
        void widerIsSubtype() {
            // &{a: end, b: end} ≤ &{a: end} — more methods is subtype
            assertTrue(SubtypingChecker.isSubtype(
                    parse("&{a: end, b: end}"), parse("&{a: end}")));
        }

        @Test
        void narrowerNotSubtype() {
            // &{a: end} ≤ &{a: end, b: end} — FAILS: missing method b
            assertFalse(SubtypingChecker.isSubtype(
                    parse("&{a: end}"), parse("&{a: end, b: end}")));
        }

        @Test
        void covariantDepthWithEnd() {
            // &{a: &{b: end}} ≤ &{a: end} — TRUE because end ≡ &{}
            // continuation &{b: end} ≤_GH end ≡ &{} by Sub-Branch (∅ ⊆ {b})
            assertTrue(SubtypingChecker.isSubtype(
                    parse("&{a: &{b: end}}"), parse("&{a: end}")));
        }

        @Test
        void sameStructureDifferentDepth() {
            // &{a: &{b: end, c: end}} ≤ &{a: &{b: end}} — covariant depth
            assertTrue(SubtypingChecker.isSubtype(
                    parse("&{a: &{b: end, c: end}}"), parse("&{a: &{b: end}}")));
        }

        @Test
        void widthAndDepth() {
            // &{a: &{b: end, c: end}, d: end} ≤ &{a: &{b: end}} — both wider and deeper
            assertTrue(SubtypingChecker.isSubtype(
                    parse("&{a: &{b: end, c: end}, d: end}"), parse("&{a: &{b: end}}")));
        }

        @Test
        void threeMethodsChain() {
            var a = parse("&{a: end, b: end, c: end}");
            var b = parse("&{a: end, b: end}");
            var c = parse("&{a: end}");
            assertTrue(SubtypingChecker.isSubtype(a, b));
            assertTrue(SubtypingChecker.isSubtype(b, c));
            assertTrue(SubtypingChecker.isSubtype(a, c)); // transitivity
        }

        @Test
        void disjointMethodsIncomparable() {
            // &{a: end} and &{b: end} are incomparable
            assertFalse(SubtypingChecker.isSubtype(parse("&{a: end}"), parse("&{b: end}")));
            assertFalse(SubtypingChecker.isSubtype(parse("&{b: end}"), parse("&{a: end}")));
        }
    }

    // =========================================================================
    // Selection subtyping (contravariant width, covariant depth)
    // =========================================================================

    @Nested
    class SelectionSubtyping {

        @Test
        void sameSelection() {
            var s = parse("+{a: end}");
            assertTrue(SubtypingChecker.isSubtype(s, s));
        }

        @Test
        void fewerIsSubtype() {
            // +{a: end} ≤ +{a: end, b: end} — fewer selections is subtype
            assertTrue(SubtypingChecker.isSubtype(
                    parse("+{a: end}"), parse("+{a: end, b: end}")));
        }

        @Test
        void moreNotSubtype() {
            // +{a: end, b: end} ≤ +{a: end} — FAILS: extra selection b
            assertFalse(SubtypingChecker.isSubtype(
                    parse("+{a: end, b: end}"), parse("+{a: end}")));
        }

        @Test
        void covariantDepth() {
            // +{a: &{b: end, c: end}} ≤ +{a: &{b: end}} — covariant in continuation
            assertTrue(SubtypingChecker.isSubtype(
                    parse("+{a: &{b: end, c: end}}"), parse("+{a: &{b: end}}")));
        }

        @Test
        void selectionChain() {
            var a = parse("+{a: end}");
            var ab = parse("+{a: end, b: end}");
            var abc = parse("+{a: end, b: end, c: end}");
            assertTrue(SubtypingChecker.isSubtype(a, ab));
            assertTrue(SubtypingChecker.isSubtype(ab, abc));
            assertTrue(SubtypingChecker.isSubtype(a, abc)); // transitivity
        }

        @Test
        void disjointSelectionsIncomparable() {
            assertFalse(SubtypingChecker.isSubtype(parse("+{a: end}"), parse("+{b: end}")));
            assertFalse(SubtypingChecker.isSubtype(parse("+{b: end}"), parse("+{a: end}")));
        }
    }

    // =========================================================================
    // Mixed constructor subtyping
    // =========================================================================

    @Nested
    class MixedSubtyping {

        @Test
        void branchNotSubtypeOfSelection() {
            assertFalse(SubtypingChecker.isSubtype(parse("&{a: end}"), parse("+{a: end}")));
        }

        @Test
        void selectionNotSubtypeOfBranch() {
            assertFalse(SubtypingChecker.isSubtype(parse("+{a: end}"), parse("&{a: end}")));
        }

        @Test
        void nestedMixed() {
            // &{a: +{x: end}} ≤ &{a: +{x: end, y: end}} — selection inside branch
            // +{x: end} ≤ +{x: end, y: end} (fewer selections = subtype)
            assertTrue(SubtypingChecker.isSubtype(
                    parse("&{a: +{x: end}}"), parse("&{a: +{x: end, y: end}}")));
        }

        @Test
        void nestedMixedReverse() {
            // &{a: +{x: end, y: end}} is NOT ≤ &{a: +{x: end}} — extra selection
            assertFalse(SubtypingChecker.isSubtype(
                    parse("&{a: +{x: end, y: end}}"), parse("&{a: +{x: end}}")));
        }

        @Test
        void branchWithWiderAndSelectionWithFewer() {
            // &{a: +{x: end}, b: +{x: end}} ≤ &{a: +{x: end, y: end}}
            assertTrue(SubtypingChecker.isSubtype(
                    parse("&{a: +{x: end}, b: +{x: end}}"),
                    parse("&{a: +{x: end, y: end}}")));
        }
    }

    // =========================================================================
    // Recursive subtyping
    // =========================================================================

    @Nested
    class RecursiveSubtyping {

        @Test
        void sameRecursive() {
            var s = parse("rec X . &{a: X, b: end}");
            assertTrue(SubtypingChecker.isSubtype(s, s));
        }

        @Test
        void recursiveWiderBranch() {
            // rec X . &{a: X, b: end, c: end} ≤ rec X . &{a: X, b: end}
            assertTrue(SubtypingChecker.isSubtype(
                    parse("rec X . &{a: X, b: end, c: end}"),
                    parse("rec X . &{a: X, b: end}")));
        }

        @Test
        void recursiveNarrowerNotSubtype() {
            assertFalse(SubtypingChecker.isSubtype(
                    parse("rec X . &{a: X}"),
                    parse("rec X . &{a: X, b: end}")));
        }

        @Test
        void recursiveSameStructure() {
            // Two structurally identical recursive types with different var names
            var s1 = parse("rec X . &{a: X, b: end}");
            var s2 = parse("rec Y . &{a: Y, b: end}");
            assertTrue(SubtypingChecker.isSubtype(s1, s2));
            assertTrue(SubtypingChecker.isSubtype(s2, s1));
        }

        @Test
        void recursiveIncomparable() {
            // rec X . &{a: X} and rec X . &{b: X} are incomparable
            assertFalse(SubtypingChecker.isSubtype(
                    parse("rec X . &{a: X}"), parse("rec X . &{b: X}")));
            assertFalse(SubtypingChecker.isSubtype(
                    parse("rec X . &{b: X}"), parse("rec X . &{a: X}")));
        }
    }

    // =========================================================================
    // check() result object
    // =========================================================================

    @Nested
    class CheckResult {

        @Test
        void subtypeResultTrue() {
            var result = SubtypingChecker.check(
                    parse("&{a: end, b: end}"), parse("&{a: end}"));
            assertInstanceOf(SubtypingResult.class, result);
            assertTrue(result.isSubtype());
            assertNull(result.reason());
        }

        @Test
        void subtypeResultFalseMissingMethods() {
            var result = SubtypingChecker.check(
                    parse("&{a: end}"), parse("&{a: end, b: end}"));
            assertFalse(result.isSubtype());
            assertTrue(result.reason().contains("missing"));
        }

        @Test
        void subtypeResultFalseExtraSelection() {
            var result = SubtypingChecker.check(
                    parse("+{a: end, b: end}"), parse("+{a: end}"));
            assertFalse(result.isSubtype());
            assertTrue(result.reason().toLowerCase().contains("select")
                    || result.reason().toLowerCase().contains("label"));
        }

        @Test
        void subtypeResultFalseConstructors() {
            var result = SubtypingChecker.check(
                    parse("&{a: end}"), parse("+{a: end}"));
            assertFalse(result.isSubtype());
            assertTrue(result.reason().toLowerCase().contains("incompatible"));
        }

        @Test
        void subtypeResultLhsRhs() {
            var s1 = parse("&{a: end}");
            var s2 = parse("&{b: end}");
            var result = SubtypingChecker.check(s1, s2);
            assertEquals(PrettyPrinter.pretty(s1), result.lhs());
            assertEquals(PrettyPrinter.pretty(s2), result.rhs());
        }
    }

    // =========================================================================
    // Reflexivity and transitivity
    // =========================================================================

    @Nested
    class SubtypingProperties {

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "&{a: end, b: end}",
                "+{a: end}",
                "+{a: end, b: end}",
                "&{a: +{x: end}}",
                "rec X . &{a: X, b: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
        })
        void reflexivity(String typeStr) {
            var s = parse(typeStr);
            assertTrue(SubtypingChecker.isSubtype(s, s));
        }

        @Test
        void transitivityBranch() {
            var a = parse("&{a: end, b: end, c: end}");
            var b = parse("&{a: end, b: end}");
            var c = parse("&{a: end}");
            assertTrue(SubtypingChecker.isSubtype(a, b));
            assertTrue(SubtypingChecker.isSubtype(b, c));
            assertTrue(SubtypingChecker.isSubtype(a, c));
        }

        @Test
        void transitivityBranchToEnd() {
            // Full chain: &{a,b,c} ≤ &{a,b} ≤ &{a} ≤ end
            var a = parse("&{a: end, b: end, c: end}");
            var b = parse("&{a: end, b: end}");
            var c = parse("&{a: end}");
            var d = new End();
            assertTrue(SubtypingChecker.isSubtype(a, b));
            assertTrue(SubtypingChecker.isSubtype(b, c));
            assertTrue(SubtypingChecker.isSubtype(c, d));
            assertTrue(SubtypingChecker.isSubtype(a, d));
        }

        @Test
        void transitivitySelection() {
            var a = parse("+{a: end}");
            var b = parse("+{a: end, b: end}");
            var c = parse("+{a: end, b: end, c: end}");
            assertTrue(SubtypingChecker.isSubtype(a, b));
            assertTrue(SubtypingChecker.isSubtype(b, c));
            assertTrue(SubtypingChecker.isSubtype(a, c));
        }

        @Test
        void antisymmetryUpToEquivalence() {
            // S₁ ≤ S₂ and S₂ ≤ S₁ implies structural equivalence
            var s1 = parse("rec X . &{a: X, b: end}");
            var s2 = parse("rec Y . &{a: Y, b: end}");
            assertTrue(SubtypingChecker.isSubtype(s1, s2));
            assertTrue(SubtypingChecker.isSubtype(s2, s1));
        }
    }

    // =========================================================================
    // Width subtyping ↔ embedding correspondence
    // =========================================================================

    @Nested
    class WidthEmbedding {

        @Test
        void branchWidthEmbedding() {
            // &{a: end, b: end} ≤ &{a: end} ↔ L(&{a: end}) embeds in L(&{a:end, b:end})
            var result = SubtypingChecker.checkWidthEmbedding(
                    parse("&{a: end, b: end}"), parse("&{a: end}"));
            assertTrue(result.isSubtype());
            assertTrue(result.hasEmbedding());
            assertTrue(result.coincides());
        }

        @Test
        void branchNotSubtypeNoEmbedding() {
            // &{a: end} NOT ≤ &{a: end, b: end}
            var result = SubtypingChecker.checkWidthEmbedding(
                    parse("&{a: end}"), parse("&{a: end, b: end}"));
            assertFalse(result.isSubtype());
        }

        @Test
        void sameTypeEmbedding() {
            var result = SubtypingChecker.checkWidthEmbedding(
                    parse("&{a: end}"), parse("&{a: end}"));
            assertTrue(result.isSubtype());
            assertTrue(result.hasEmbedding());
            assertTrue(result.coincides());
        }

        @Test
        void recursiveWidthEmbedding() {
            var result = SubtypingChecker.checkWidthEmbedding(
                    parse("rec X . &{a: X, b: end, c: end}"),
                    parse("rec X . &{a: X, b: end}"));
            assertTrue(result.isSubtype());
            assertTrue(result.hasEmbedding());
            assertTrue(result.coincides());
        }

        @Test
        void selectionWidthEmbedding() {
            // +{a: end} ≤ +{a: end, b: end} ↔ L(+{a:end, b:end}) embeds in L(+{a:end})
            var result = SubtypingChecker.checkWidthEmbedding(
                    parse("+{a: end}"), parse("+{a: end, b: end}"));
            assertTrue(result.isSubtype());
            assertTrue(result.hasEmbedding());
            assertTrue(result.coincides());
        }

        @Test
        void threeMethodsWidth() {
            var result = SubtypingChecker.checkWidthEmbedding(
                    parse("&{a: end, b: end, c: end}"), parse("&{a: end}"));
            assertTrue(result.isSubtype());
            assertTrue(result.hasEmbedding());
            assertTrue(result.coincides());
        }
    }

    // =========================================================================
    // Result type tests
    // =========================================================================

    @Nested
    class ResultTypes {

        @Test
        void subtypingResultFields() {
            var result = new SubtypingResult("end", "end", true);
            assertEquals("end", result.lhs());
            assertEquals("end", result.rhs());
            assertTrue(result.isSubtype());
            assertNull(result.reason());
        }

        @Test
        void widthSubtypingResultFields() {
            var result = new WidthSubtypingResult(
                    "&{a: end}", "&{a: end}", true, true, true);
            assertTrue(result.coincides());
        }
    }

    // =========================================================================
    // Substitution and unfolding (internal, but worth testing directly)
    // =========================================================================

    @Nested
    class SubstitutionTests {

        @Test
        void unfoldRecursion() {
            // rec X . &{a: X, b: end} → &{a: rec X . &{a: X, b: end}, b: end}
            var rec = parse("rec X . &{a: X, b: end}");
            var unfolded = SubtypingChecker.unfold(rec);
            assertInstanceOf(Branch.class, unfolded);
            var b = (Branch) unfolded;
            assertEquals(2, b.choices().size());
            assertInstanceOf(Rec.class, b.choices().get(0).body());
        }

        @Test
        void unfoldNonRec() {
            // Non-recursive types are returned unchanged
            var s = parse("&{a: end}");
            assertSame(s, SubtypingChecker.unfold(s));
        }

        @Test
        void substituteShadowed() {
            // rec X . rec X . &{a: X} — inner X is shadowed
            var inner = new Rec("X", new Branch(List.of(
                    new Choice("a", new Var("X")))));
            var outer = new Rec("X", inner);
            var unfolded = SubtypingChecker.unfold(outer);
            // Should get rec X . &{a: X}, where X still refers to inner binding
            assertInstanceOf(Rec.class, unfolded);
        }
    }

    // =========================================================================
    // Parallel subtyping
    // =========================================================================

    @Nested
    class ParallelSubtyping {

        @Test
        void sameParallel() {
            var s = parse("(&{a: end} || &{b: end})");
            assertTrue(SubtypingChecker.isSubtype(s, s));
        }

        @Test
        void parallelCovariant() {
            // (&{a: end, c: end} || &{b: end}) ≤ (&{a: end} || &{b: end})
            assertTrue(SubtypingChecker.isSubtype(
                    parse("(&{a: end, c: end} || &{b: end})"),
                    parse("(&{a: end} || &{b: end})")));
        }

        @Test
        void parallelNotSubtypeOfBranch() {
            assertFalse(SubtypingChecker.isSubtype(
                    parse("(&{a: end} || &{b: end})"),
                    parse("&{a: end}")));
        }
    }
}
