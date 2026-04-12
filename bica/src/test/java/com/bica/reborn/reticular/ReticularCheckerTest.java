package com.bica.reborn.reticular;

import com.bica.reborn.ast.*;
import com.bica.reborn.duality.DualityChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for reticular form: characterisation and reconstruction (Step 9 / Phase 11).
 *
 * <p>Mirrors the 55 Python tests in test_reticular.py.
 */
class ReticularCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // =========================================================================
    // State classification
    // =========================================================================

    @Nested
    class ClassifyState {

        @Test
        void endState() {
            var ss = build("&{a: end}");
            var c = ReticularChecker.classifyState(ss, ss.bottom());
            assertEquals("end", c.kind());
            assertTrue(c.labels().isEmpty());
        }

        @Test
        void branchState() {
            var ss = build("&{a: end, b: end}");
            var c = ReticularChecker.classifyState(ss, ss.top());
            assertEquals("branch", c.kind());
            assertEquals(Set.of("a", "b"), new HashSet<>(c.labels()));
        }

        @Test
        void selectionState() {
            var ss = build("+{a: end, b: end}");
            var c = ReticularChecker.classifyState(ss, ss.top());
            assertEquals("select", c.kind());
            assertEquals(Set.of("a", "b"), new HashSet<>(c.labels()));
        }

        @Test
        void nestedBranchSelect() {
            var ss = build("&{a: +{x: end}}");
            var topC = ReticularChecker.classifyState(ss, ss.top());
            assertEquals("branch", topC.kind());

            // Find inner state (target of "a")
            int inner = ss.transitionsFrom(ss.top()).stream()
                    .filter(t -> t.label().equals("a"))
                    .findFirst().orElseThrow().target();
            var innerC = ReticularChecker.classifyState(ss, inner);
            assertEquals("select", innerC.kind());
        }
    }

    @Nested
    class ClassifyAllStates {

        @Test
        void simpleBranch() {
            var ss = build("&{a: end}");
            var classifications = ReticularChecker.classifyAllStates(ss);
            var kinds = classifications.stream()
                    .map(StateClassification::kind)
                    .collect(java.util.stream.Collectors.toSet());
            assertTrue(kinds.contains("branch") || kinds.contains("end"));
        }

        @Test
        void allClassified() {
            var ss = build("&{a: end, b: &{c: end}}");
            var classifications = ReticularChecker.classifyAllStates(ss);
            assertEquals(ss.states().size(), classifications.size());
        }
    }

    // =========================================================================
    // Reconstruction: StateSpace → SessionType
    // =========================================================================

    @Nested
    class ReconstructBasic {

        @Test
        void endType() {
            var ss = StateSpaceBuilder.build(new End());
            var result = ReticularChecker.reconstruct(ss);
            assertEquals(new End(), result);
        }

        @Test
        void singleBranch() {
            var ss = build("&{a: end}");
            var result = ReticularChecker.reconstruct(ss);
            assertInstanceOf(Branch.class, result);
            var b = (Branch) result;
            assertEquals(1, b.choices().size());
            assertEquals("a", b.choices().getFirst().label());
            assertEquals(new End(), b.choices().getFirst().body());
        }

        @Test
        void singleSelection() {
            var ss = build("+{a: end}");
            var result = ReticularChecker.reconstruct(ss);
            assertInstanceOf(Select.class, result);
            assertEquals(1, ((Select) result).choices().size());
        }

        @Test
        void twoMethodBranch() {
            var ss = build("&{a: end, b: end}");
            var result = ReticularChecker.reconstruct(ss);
            assertInstanceOf(Branch.class, result);
            var labels = ((Branch) result).choices().stream()
                    .map(Branch.Choice::label).collect(java.util.stream.Collectors.toSet());
            assertEquals(Set.of("a", "b"), labels);
        }

        @Test
        void twoLabelSelection() {
            var ss = build("+{x: end, y: end}");
            var result = ReticularChecker.reconstruct(ss);
            assertInstanceOf(Select.class, result);
            var labels = ((Select) result).choices().stream()
                    .map(Branch.Choice::label).collect(java.util.stream.Collectors.toSet());
            assertEquals(Set.of("x", "y"), labels);
        }

        @Test
        void nestedBranch() {
            var ss = build("&{a: &{b: end}}");
            var result = ReticularChecker.reconstruct(ss);
            assertInstanceOf(Branch.class, result);
            var inner = ((Branch) result).choices().stream()
                    .filter(c -> c.label().equals("a")).findFirst().orElseThrow().body();
            assertInstanceOf(Branch.class, inner);
        }

        @Test
        void nestedMixed() {
            var ss = build("&{a: +{x: end, y: end}}");
            var result = ReticularChecker.reconstruct(ss);
            assertInstanceOf(Branch.class, result);
            var inner = ((Branch) result).choices().stream()
                    .filter(c -> c.label().equals("a")).findFirst().orElseThrow().body();
            assertInstanceOf(Select.class, inner);
            assertEquals(2, ((Select) inner).choices().size());
        }
    }

    @Nested
    class ReconstructRecursion {

        @Test
        void simpleRecursion() {
            var ss = build("rec X . &{a: X, b: end}");
            var result = ReticularChecker.reconstruct(ss);
            assertInstanceOf(Rec.class, result);
            assertInstanceOf(Branch.class, ((Rec) result).body());
        }

        @Test
        void recursiveSelection() {
            var ss = build("rec X . +{a: X, b: end}");
            var result = ReticularChecker.reconstruct(ss);
            assertInstanceOf(Rec.class, result);
            assertInstanceOf(Select.class, ((Rec) result).body());
        }

        @Test
        void recursiveIterator() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var result = ReticularChecker.reconstruct(ss);
            assertTrue(containsRec(result));
        }

        private boolean containsRec(SessionType s) {
            return switch (s) {
                case Rec r -> true;
                case Branch b -> b.choices().stream().anyMatch(c -> containsRec(c.body()));
                case Select sel -> sel.choices().stream().anyMatch(c -> containsRec(c.body()));
                case Parallel p -> containsRec(p.left()) || containsRec(p.right());
                case Sequence seq -> containsRec(seq.left()) || containsRec(seq.right());
                default -> false;
            };
        }
    }

    // =========================================================================
    // Round-trip: S → L(S) → S'
    // =========================================================================

    @Nested
    class RoundTrip {

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "&{a: end, b: end}",
                "&{a: end, b: end, c: end}",
                "+{a: end}",
                "+{a: end, b: end}",
                "&{a: &{b: end}}",
                "&{a: +{x: end, y: end}}",
                "+{a: &{x: end, y: end}}",
                "&{a: &{b: end, c: end}, d: end}",
        })
        void roundTripPreservesStructure(String typeStr) {
            var original = Parser.parse(typeStr);
            var ss = StateSpaceBuilder.build(original);
            var reconstructed = ReticularChecker.reconstruct(ss);
            var ss2 = StateSpaceBuilder.build(reconstructed);

            assertEquals(ss.states().size(), ss2.states().size(),
                    "State count mismatch for %s".formatted(typeStr));
            assertEquals(ss.transitions().size(), ss2.transitions().size(),
                    "Transition count mismatch for %s".formatted(typeStr));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "+{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
        })
        void roundTripSameLabels(String typeStr) {
            var original = Parser.parse(typeStr);
            var ss = StateSpaceBuilder.build(original);
            var reconstructed = ReticularChecker.reconstruct(ss);
            var ss2 = StateSpaceBuilder.build(reconstructed);

            var labels1 = ss.transitions().stream().map(StateSpace.Transition::label).sorted().toList();
            var labels2 = ss2.transitions().stream().map(StateSpace.Transition::label).sorted().toList();
            assertEquals(labels1, labels2);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "rec X . &{a: X, b: end}",
                "rec X . +{a: X, b: end}",
        })
        void roundTripRecursive(String typeStr) {
            var original = Parser.parse(typeStr);
            var ss = StateSpaceBuilder.build(original);
            var reconstructed = ReticularChecker.reconstruct(ss);
            var ss2 = StateSpaceBuilder.build(reconstructed);

            assertEquals(ss.states().size(), ss2.states().size());
            assertEquals(ss.transitions().size(), ss2.transitions().size());
        }
    }

    // =========================================================================
    // Reticular form check
    // =========================================================================

    @Nested
    class IsReticulate {

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "+{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
                "&{a: +{x: end, y: end}}",
                "+{a: &{x: end, y: end}}",
                "rec X . &{a: X, b: end}",
                "rec X . +{a: X, b: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
        })
        void sessionTypesAreReticulates(String typeStr) {
            assertTrue(ReticularChecker.isReticulate(build(typeStr)),
                    "%s should be a reticulate".formatted(typeStr));
        }

        @Test
        void productStateIsReticulate() {
            // Mixed method+selection transitions from the same state
            var transitions = List.of(
                    new StateSpace.Transition(0, "a", 1, TransitionKind.SELECTION),
                    new StateSpace.Transition(0, "b", 2, TransitionKind.METHOD)
            );
            var ss = new StateSpace(
                    Set.of(0, 1, 2), transitions, 0, 1, Map.of());
            assertTrue(ReticularChecker.isReticulate(ss));
        }
    }

    @Nested
    class CheckReticularForm {

        @Test
        void resultType() {
            var result = ReticularChecker.checkReticularForm(build("&{a: end}"));
            assertInstanceOf(ReticularFormResult.class, result);
        }

        @Test
        void reticularHasReconstruction() {
            var result = ReticularChecker.checkReticularForm(build("&{a: end, b: end}"));
            assertTrue(result.isReticulate());
            assertNotNull(result.reconstructed());
        }

        @Test
        void productStateClassified() {
            var transitions = List.of(
                    new StateSpace.Transition(0, "a", 1, TransitionKind.SELECTION),
                    new StateSpace.Transition(0, "b", 2, TransitionKind.METHOD)
            );
            var ss = new StateSpace(
                    Set.of(0, 1, 2), transitions, 0, 1, Map.of());
            var result = ReticularChecker.checkReticularForm(ss);
            assertTrue(result.isReticulate());
            long productCount = result.classifications().stream()
                    .filter(c -> c.kind().equals("product")).count();
            assertEquals(1, productCount);
        }

        @Test
        void classificationsPresent() {
            var ss = build("&{a: +{x: end}}");
            var result = ReticularChecker.checkReticularForm(ss);
            assertEquals(ss.states().size(), result.classifications().size());
        }
    }

    // =========================================================================
    // Properties
    // =========================================================================

    @Nested
    class ReticularProperties {

        @Test
        void endIsSimplestReticulate() {
            var ss = StateSpaceBuilder.build(new End());
            assertTrue(ReticularChecker.isReticulate(ss));
            assertEquals(1, ss.states().size());
            assertEquals(0, ss.transitions().size());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "&{a: end}",
                "+{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
                "&{a: +{x: end, y: end}}",
                "rec X . &{a: X, b: end}",
        })
        void dualPreservesReticularForm(String typeStr) {
            var d = DualityChecker.dual(Parser.parse(typeStr));
            var ssD = StateSpaceBuilder.build(d);
            assertTrue(ReticularChecker.isReticulate(ssD),
                    "dual(%s) should be a reticulate".formatted(typeStr));
        }

        @Test
        void reconstructionPreservesSelectionKind() {
            for (String typeStr : List.of("&{a: end, b: end}", "+{a: end, b: end}")) {
                var original = Parser.parse(typeStr);
                var ss = StateSpaceBuilder.build(original);
                var reconstructed = ReticularChecker.reconstruct(ss);
                assertEquals(original.getClass(), reconstructed.getClass(),
                        "Constructor mismatch for %s".formatted(typeStr));
            }
        }
    }
}
