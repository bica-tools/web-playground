package com.bica.reborn.duality;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;
import com.bica.reborn.subtyping.SubtypingChecker;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.bica.reborn.duality.DualityChecker.dual;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for session type duality (Step 8 / Phase 9).
 *
 * <p>Mirrors the 58 Python tests in test_duality.py.
 */
class DualityCheckerTest {

    private static SessionType parse(String input) {
        return Parser.parse(input);
    }

    // =========================================================================
    // Basic dual operation
    // =========================================================================

    @Nested
    class DualBasic {

        @Test
        void dualEnd() {
            assertEquals(new End(), dual(new End()));
        }

        @Test
        void dualVar() {
            assertEquals(new Var("X"), dual(new Var("X")));
        }
    }

    // =========================================================================
    // Branch ↔ Selection swap
    // =========================================================================

    @Nested
    class DualBranchSelection {

        @Test
        void branchBecomesSelection() {
            var s = parse("&{a: end}");
            var d = dual(s);
            assertInstanceOf(Select.class, d);
            var sel = (Select) d;
            assertEquals(1, sel.choices().size());
            assertEquals("a", sel.choices().getFirst().label());
        }

        @Test
        void selectionBecomesBranch() {
            var s = parse("+{a: end}");
            var d = dual(s);
            assertInstanceOf(Branch.class, d);
        }

        @Test
        void branchMultipleMethods() {
            var s = parse("&{a: end, b: end}");
            var d = dual(s);
            assertInstanceOf(Select.class, d);
            assertEquals(2, ((Select) d).choices().size());
        }

        @Test
        void selectionMultipleLabels() {
            var s = parse("+{a: end, b: end}");
            var d = dual(s);
            assertInstanceOf(Branch.class, d);
            assertEquals(2, ((Branch) d).choices().size());
        }

        @Test
        void nestedBranchInSelection() {
            // dual(&{a: +{x: end}}) = +{a: &{x: end}}
            var s = parse("&{a: +{x: end}}");
            var d = dual(s);
            assertInstanceOf(Select.class, d);
            var inner = ((Select) d).choices().getFirst().body();
            assertInstanceOf(Branch.class, inner);
        }

        @Test
        void nestedSelectionInBranch() {
            // dual(+{a: &{x: end}}) = &{a: +{x: end}}
            var s = parse("+{a: &{x: end}}");
            var d = dual(s);
            assertInstanceOf(Branch.class, d);
            var inner = ((Branch) d).choices().getFirst().body();
            assertInstanceOf(Select.class, inner);
        }

        @Test
        void deeplyNested() {
            // dual(&{a: &{b: +{c: end}}}) = +{a: +{b: &{c: end}}}
            var s = parse("&{a: &{b: +{c: end}}}");
            var d = dual(s);
            assertInstanceOf(Select.class, d);
            var mid = ((Select) d).choices().getFirst().body();
            assertInstanceOf(Select.class, mid);
            var inner = ((Select) mid).choices().getFirst().body();
            assertInstanceOf(Branch.class, inner);
        }
    }

    // =========================================================================
    // Recursion
    // =========================================================================

    @Nested
    class DualRecursion {

        @Test
        void recursiveBranch() {
            var s = parse("rec X . &{a: X, b: end}");
            var d = dual(s);
            assertInstanceOf(Rec.class, d);
            assertEquals("X", ((Rec) d).var());
            assertInstanceOf(Select.class, ((Rec) d).body());
        }

        @Test
        void recursiveSelection() {
            var s = parse("rec X . +{a: X, b: end}");
            var d = dual(s);
            assertInstanceOf(Rec.class, d);
            assertInstanceOf(Branch.class, ((Rec) d).body());
        }

        @Test
        void recursiveVarPreserved() {
            var s = parse("rec X . &{a: X}");
            var d = dual(s);
            var body = (Select) ((Rec) d).body();
            assertEquals(new Var("X"), body.choices().getFirst().body());
        }

        @Test
        void nestedRecursion() {
            // dual(rec X . &{a: rec Y . +{b: Y, c: X}})
            var s = parse("rec X . &{a: rec Y . +{b: Y, c: X}}");
            var d = dual(s);
            assertInstanceOf(Rec.class, d);
            assertInstanceOf(Select.class, ((Rec) d).body()); // was Branch
            var innerRec = ((Select) ((Rec) d).body()).choices().getFirst().body();
            assertInstanceOf(Rec.class, innerRec);
            assertInstanceOf(Branch.class, ((Rec) innerRec).body()); // was Select
        }
    }

    // =========================================================================
    // Parallel and Sequence
    // =========================================================================

    @Nested
    class DualParallelSequence {

        @Test
        void parallel() {
            var s = new Parallel(parse("&{a: end}"), parse("+{b: end}"));
            var d = dual(s);
            assertInstanceOf(Parallel.class, d);
            assertInstanceOf(Select.class, ((Parallel) d).left());
            assertInstanceOf(Branch.class, ((Parallel) d).right());
        }

        @Test
        void sequence() {
            var s = new Sequence(parse("&{a: end}"), parse("+{b: end}"));
            var d = dual(s);
            assertInstanceOf(Sequence.class, d);
            assertInstanceOf(Select.class, ((Sequence) d).left());
            assertInstanceOf(Branch.class, ((Sequence) d).right());
        }
    }

    // =========================================================================
    // Involution: dual(dual(S)) = S
    // =========================================================================

    @Nested
    class DualInvolution {

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "+{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
                "&{a: +{x: end, y: end}, b: end}",
                "+{a: &{x: end}, b: &{y: end}}",
                "rec X . &{a: X, b: end}",
                "rec X . +{a: X, b: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
        })
        void involution(String typeStr) {
            var s = parse(typeStr);
            assertEquals(s, dual(dual(s)));
        }

        @Test
        void involutionParallel() {
            var s = new Parallel(parse("&{a: end}"), parse("+{b: end}"));
            assertEquals(s, dual(dual(s)));
        }
    }

    // =========================================================================
    // Pretty-printing of duals
    // =========================================================================

    @Nested
    class DualPrettyPrint {

        @Test
        void branchToSelectionPretty() {
            var d = dual(parse("&{a: end, b: end}"));
            assertEquals("+{a: end, b: end}", PrettyPrinter.pretty(d));
        }

        @Test
        void selectionToBranchPretty() {
            var d = dual(parse("+{a: end, b: end}"));
            assertEquals("&{a: end, b: end}", PrettyPrinter.pretty(d));
        }

        @Test
        void recursivePretty() {
            var d = dual(parse("rec X . &{a: X, b: end}"));
            assertEquals("rec X . +{a: X, b: end}", PrettyPrinter.pretty(d));
        }
    }

    // =========================================================================
    // State-space isomorphism: L(S) ≅ L(dual(S))
    // =========================================================================

    @Nested
    class DualStateSpace {

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "+{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
                "&{a: &{b: end}}",
                "+{a: +{b: end}}",
                "&{a: +{x: end, y: end}}",
                "rec X . &{a: X, b: end}",
                "rec X . +{a: X, b: end}",
        })
        void isomorphic(String typeStr) {
            var s = parse(typeStr);
            var d = dual(s);
            var ssS = StateSpaceBuilder.build(s);
            var ssD = StateSpaceBuilder.build(d);
            var iso = MorphismChecker.findIsomorphism(ssS, ssD);
            assertNotNull(iso, "L(%s) not isomorphic to L(dual(%s))".formatted(typeStr, typeStr));
        }

        @Test
        void sameNumberOfStates() {
            var s = parse("&{a: end, b: &{c: end}}");
            var d = dual(s);
            var ssS = StateSpaceBuilder.build(s);
            var ssD = StateSpaceBuilder.build(d);
            assertEquals(ssS.states().size(), ssD.states().size());
            assertEquals(ssS.transitions().size(), ssD.transitions().size());
        }

        @Test
        void sameLatticeStructure() {
            var s = parse("&{a: end, b: &{c: end, d: end}}");
            var d = dual(s);
            var lrS = LatticeChecker.checkLattice(StateSpaceBuilder.build(s));
            var lrD = LatticeChecker.checkLattice(StateSpaceBuilder.build(d));
            assertTrue(lrS.isLattice());
            assertTrue(lrD.isLattice());
        }
    }

    // =========================================================================
    // Selection annotation flip
    // =========================================================================

    @Nested
    class SelectionFlip {

        @Test
        void branchSelectionsFlip() {
            var s = parse("&{a: end}");
            var d = dual(s);
            var ssS = StateSpaceBuilder.build(s);
            var ssD = StateSpaceBuilder.build(d);
            long selCountS = ssS.transitions().stream()
                    .filter(t -> t.kind() == TransitionKind.SELECTION).count();
            long selCountD = ssD.transitions().stream()
                    .filter(t -> t.kind() == TransitionKind.SELECTION).count();
            assertEquals(0, selCountS);
            assertEquals(1, selCountD);
        }

        @Test
        void selectionTransitionsFlip() {
            var s = parse("+{a: end}");
            var d = dual(s);
            var ssS = StateSpaceBuilder.build(s);
            var ssD = StateSpaceBuilder.build(d);
            long selCountS = ssS.transitions().stream()
                    .filter(t -> t.kind() == TransitionKind.SELECTION).count();
            long selCountD = ssD.transitions().stream()
                    .filter(t -> t.kind() == TransitionKind.SELECTION).count();
            assertEquals(1, selCountS);
            assertEquals(0, selCountD);
        }

        @Test
        void mixedAnnotationsFlip() {
            // &{a: +{x: end, y: end}}: 1 branch (a), 2 selections (x,y)
            var s = parse("&{a: +{x: end, y: end}}");
            var d = dual(s);
            var ssS = StateSpaceBuilder.build(s);
            var ssD = StateSpaceBuilder.build(d);

            long selCountS = ssS.transitions().stream()
                    .filter(t -> t.kind() == TransitionKind.SELECTION).count();
            long selCountD = ssD.transitions().stream()
                    .filter(t -> t.kind() == TransitionKind.SELECTION).count();

            assertEquals(2, selCountS);  // x, y
            assertEquals(3, ssS.transitions().size());
            assertEquals(1, selCountD);  // a
            assertEquals(3, ssD.transitions().size());
        }
    }

    // =========================================================================
    // check_duality result
    // =========================================================================

    @Nested
    class CheckDuality {

        @Test
        void simpleBranch() {
            var result = DualityChecker.check(parse("&{a: end, b: end}"));
            assertInstanceOf(DualityResult.class, result);
            assertTrue(result.isInvolution());
            assertTrue(result.isIsomorphic());
            assertTrue(result.selectionFlipped());
        }

        @Test
        void simpleSelection() {
            var result = DualityChecker.check(parse("+{a: end}"));
            assertTrue(result.isInvolution());
            assertTrue(result.isIsomorphic());
            assertTrue(result.selectionFlipped());
        }

        @Test
        void recursive() {
            var result = DualityChecker.check(parse("rec X . &{a: X, b: end}"));
            assertTrue(result.isInvolution());
            assertTrue(result.isIsomorphic());
        }

        @Test
        void end() {
            var result = DualityChecker.check(new End());
            assertTrue(result.isInvolution());
            assertTrue(result.isIsomorphic());
        }

        @Test
        void nestedMixed() {
            var result = DualityChecker.check(parse("&{a: +{x: end, y: end}, b: end}"));
            assertTrue(result.isInvolution());
            assertTrue(result.isIsomorphic());
            assertTrue(result.selectionFlipped());
        }

        @Test
        void resultStrings() {
            var result = DualityChecker.check(parse("&{a: end}"));
            // T2b (2026-04-11): single-arm Branch prints as &{a: end}
            // explicitly. The `a . end` surface form is reserved for Chain,
            // which is now a distinct primitive.
            assertEquals("&{a: end}", result.typeStr());
            assertEquals("+{a: end}", result.dualStr());
        }
    }

    // =========================================================================
    // Subtyping and duality
    // =========================================================================

    @Nested
    class DualSubtyping {

        @Test
        void branchWidthReverses() {
            // &{a,b} ≤ &{a} but dual: +{a,b} NOT ≤ +{a}; rather +{a} ≤ +{a,b}
            var wide = parse("&{a: end, b: end}");
            var narrow = parse("&{a: end}");

            assertTrue(SubtypingChecker.isSubtype(wide, narrow));
            assertFalse(SubtypingChecker.isSubtype(narrow, wide));

            var dWide = dual(wide);
            var dNarrow = dual(narrow);
            assertFalse(SubtypingChecker.isSubtype(dWide, dNarrow));
            assertTrue(SubtypingChecker.isSubtype(dNarrow, dWide));
        }

        @Test
        void selectionWidthReverses() {
            var few = parse("+{a: end}");
            var many = parse("+{a: end, b: end}");

            assertTrue(SubtypingChecker.isSubtype(few, many));
            assertFalse(SubtypingChecker.isSubtype(many, few));

            var dFew = dual(few);
            var dMany = dual(many);
            assertFalse(SubtypingChecker.isSubtype(dFew, dMany));
            assertTrue(SubtypingChecker.isSubtype(dMany, dFew));
        }

        @Test
        void subtypeContravariantlyDual() {
            // S₁ ≤ S₂ iff dual(S₂) ≤ dual(S₁)
            String[][] pairs = {
                    {"&{a: end, b: end}", "&{a: end}"},
                    {"+{a: end}", "+{a: end, b: end}"},
                    {"&{a: end, b: end, c: end}", "&{a: end}"},
                    {"&{a: &{b: end, c: end}}", "&{a: &{b: end}}"},
            };
            for (String[] pair : pairs) {
                var s1 = parse(pair[0]);
                var s2 = parse(pair[1]);
                assertTrue(SubtypingChecker.isSubtype(s1, s2),
                        "%s should be ≤ %s".formatted(pair[0], pair[1]));
                assertTrue(SubtypingChecker.isSubtype(dual(s2), dual(s1)),
                        "dual(%s) should be ≤ dual(%s)".formatted(pair[1], pair[0]));
            }
        }

        @Test
        void nonSubtypePreserved() {
            var s1 = parse("&{a: end}");
            var s2 = parse("&{a: end, b: end}");
            assertFalse(SubtypingChecker.isSubtype(s1, s2));
            assertFalse(SubtypingChecker.isSubtype(dual(s2), dual(s1)));
        }
    }
}
