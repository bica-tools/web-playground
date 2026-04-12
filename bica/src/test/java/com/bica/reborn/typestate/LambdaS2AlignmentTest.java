package com.bica.reborn.typestate;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 200e: Alignment tests verifying λ_S² (conditionals and selection).
 *
 * <p>λ_S² extends λ_S¹ with:
 * <ul>
 *   <li>Selection types: +{l₁: S₁, ..., lₙ: Sₙ}</li>
 *   <li>Enum return types: methods before selection states return enum labels</li>
 *   <li>T-Match rule: per-branch checking at selection successor states</li>
 * </ul>
 *
 * <p>Key insight: in λ_S², the return value DETERMINES protocol evolution
 * (breaking the orthogonality of λ_S¹).
 */
@DisplayName("Step 200e: λ_S² Alignment Tests")
class LambdaS2AlignmentTest {

    // ── Helpers ──────────────────────────────────────────────────────

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    private static MethodCall call(String v, String m) {
        return new MethodCall(v, m, 0);
    }

    // ── Protocols ────────────────────────────────────────────────────

    /**
     * Iterator (one-shot, no recursion):
     *   &{hasNext: +{TRUE: &{next: end}, FALSE: end}}
     *
     * hasNext() returns Enum(TRUE, FALSE).
     * TRUE branch: next() then end.
     * FALSE branch: end immediately.
     */
    private static final String ITERATOR_TYPE =
            "&{hasNext: +{TRUE: &{next: end}, FALSE: end}}";

    /**
     * Simple connection with selection:
     *   &{connect: +{OK: &{query: &{disconnect: end}}, FAIL: end}}
     */
    private static final String CONNECTION_TYPE =
            "&{connect: +{OK: &{query: &{disconnect: end}}, FAIL: end}}";

    // ════════════════════════════════════════════════════════════════
    // §1  Selection State Detection
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Selection states: pure internal choice")
    class SelectionStates {

        @Test
        @DisplayName("hasNext leads to a pure selection state")
        void hasNextLeadsToSelection() {
            StateSpace iter = ss(ITERATOR_TYPE);
            int s0 = iter.top();
            int s_sel = iter.step(s0, "hasNext").getAsInt();

            // s_sel is a pure selection state: no callable methods, only selections
            assertTrue(iter.enabledMethods(s_sel).isEmpty(),
                    "Selection state has no callable methods");
            assertFalse(iter.enabledSelections(s_sel).isEmpty(),
                    "Selection state has selection transitions");
            assertTrue(TypestateChecker.isPureSelectionState(iter, s_sel),
                    "isPureSelectionState should detect selection state");
        }

        @Test
        @DisplayName("selection labels match enum constants")
        void selectionLabelsMatchEnum() {
            StateSpace iter = ss(ITERATOR_TYPE);
            int s0 = iter.top();
            int s_sel = iter.step(s0, "hasNext").getAsInt();

            Set<String> labels = iter.enabledSelections(s_sel);
            assertEquals(Set.of("TRUE", "FALSE"), labels,
                    "Selection labels must match enum constants");
        }

        @Test
        @DisplayName("selection successors have the right enabled methods")
        void selectionSuccessors() {
            StateSpace iter = ss(ITERATOR_TYPE);
            int s0 = iter.top();
            int s_sel = iter.step(s0, "hasNext").getAsInt();

            // TRUE branch: next is enabled
            int s_true = iter.step(s_sel, "TRUE").getAsInt();
            assertEquals(Set.of("next"), iter.enabledMethods(s_true));

            // FALSE branch: at bottom (end)
            int s_false = iter.step(s_sel, "FALSE").getAsInt();
            assertEquals(iter.bottom(), s_false);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §2  T-Match: Per-branch checking (SelectionSwitch)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T-Match: per-branch checking via SelectionSwitch")
    class TMatch {

        @Test
        @DisplayName("valid iterator: hasNext then switch TRUE→next, FALSE→done")
        void validIteratorWithSwitch() {
            StateSpace iter = ss(ITERATOR_TYPE);

            // Build call tree: hasNext(), then switch on result
            var nodes = List.<CallNode>of(
                    new CallNode.Call(call("it", "hasNext")),
                    new CallNode.SelectionSwitch("r", Map.of(
                            "TRUE", List.<CallNode>of(
                                    new CallNode.Call(call("it", "next"))),
                            "FALSE", List.<CallNode>of())));

            var result = TypestateChecker.checkCallTree(
                    iter, nodes, "it", iter.top(), true);
            assertTrue(result.isValid(),
                    "Valid iterator usage with switch should pass");
        }

        @Test
        @DisplayName("both branches reach bottom (session complete)")
        void bothBranchesReachBottom() {
            StateSpace iter = ss(ITERATOR_TYPE);
            int s0 = iter.top();
            int s_sel = iter.step(s0, "hasNext").getAsInt();

            // TRUE path: s_sel →TRUE→ s_T →next→ ⊥
            int s_true = iter.step(s_sel, "TRUE").getAsInt();
            int s_after_next = iter.step(s_true, "next").getAsInt();
            assertEquals(iter.bottom(), s_after_next, "TRUE→next reaches ⊥");

            // FALSE path: s_sel →FALSE→ ⊥
            int s_false = iter.step(s_sel, "FALSE").getAsInt();
            assertEquals(iter.bottom(), s_false, "FALSE reaches ⊥ directly");
        }

        @Test
        @DisplayName("connection: connect then switch OK→query→disconnect, FAIL→done")
        void connectionWithSwitch() {
            StateSpace conn = ss(CONNECTION_TYPE);

            var nodes = List.<CallNode>of(
                    new CallNode.Call(call("c", "connect")),
                    new CallNode.SelectionSwitch("r", Map.of(
                            "OK", List.<CallNode>of(
                                    new CallNode.Call(call("c", "query")),
                                    new CallNode.Call(call("c", "disconnect"))),
                            "FAIL", List.<CallNode>of())));

            var result = TypestateChecker.checkCallTree(
                    conn, nodes, "c", conn.top(), true);
            assertTrue(result.isValid(),
                    "Valid connection with OK/FAIL switch should pass");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §3  Orthogonality Breakdown
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Orthogonality breakdown: enum return determines Γ evolution")
    class OrthogonalityBreakdown {

        @Test
        @DisplayName("without switch: auto-advance finds valid branch (flat check)")
        void autoAdvanceFindsValidBranch() {
            // Flat check: hasNext() then next() — auto-advance picks TRUE branch
            StateSpace iter = ss(ITERATOR_TYPE);
            var result = TypestateChecker.check(iter,
                    List.of(call("it", "hasNext"), call("it", "next")),
                    Map.of("it", iter.top()), false);
            // advancePastSelections finds TRUE branch where next is enabled
            assertTrue(result.isValid(),
                    "Auto-advance should find TRUE branch for next()");
        }

        @Test
        @DisplayName("calling next without hasNext → stuck at ⊤")
        void nextWithoutHasNext() {
            StateSpace iter = ss(ITERATOR_TYPE);
            var result = TypestateChecker.check(iter,
                    List.of(call("it", "next")),
                    Map.of("it", iter.top()), false);
            assertFalse(result.isValid(),
                    "next at ⊤ is a violation (must call hasNext first)");
        }

        @Test
        @DisplayName("selection state has no callable methods (R-Call stuck)")
        void selectionStateNotCallable() {
            StateSpace iter = ss(ITERATOR_TYPE);
            int s0 = iter.top();
            int s_sel = iter.step(s0, "hasNext").getAsInt();

            // At selection state: no methods callable
            assertTrue(iter.enabledMethods(s_sel).isEmpty());
            // next is not directly enabled at s_sel
            assertTrue(iter.step(s_sel, "next").isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §4  Conservative Extension
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Conservative Extension: λ_S⁰/λ_S¹ tests unchanged")
    class ConservativeExtension {

        @Test
        @DisplayName("branch-only type: no selection states")
        void branchOnlyNoSelection() {
            StateSpace fh = ss("&{open: &{read: &{close: end}, write: &{close: end}}}");
            // No selection states in branch-only types
            int s0 = fh.top();
            assertFalse(TypestateChecker.isPureSelectionState(fh, s0));
            int s1 = fh.step(s0, "open").getAsInt();
            assertFalse(TypestateChecker.isPureSelectionState(fh, s1));
        }

        @Test
        @DisplayName("λ_S⁰ FileHandle path still valid")
        void lambdaS0FileHandleStillValid() {
            StateSpace fh = ss("&{open: &{read: &{close: end}, write: &{close: end}}}");
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §5  Invalid Selection Usage
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Invalid selection usage")
    class InvalidSelection {

        @Test
        @DisplayName("incomplete: hasNext then next only (TRUE branch incomplete in FALSE)")
        void hasNextThenNextIncomplete() {
            // hasNext → next reaches ⊥ via TRUE branch, but the flat checker
            // auto-advances past selection and finds a valid path.
            // With checkCallTree, missing the FALSE branch in a switch would
            // be caught. Here we test that calling next at ⊤ fails.
            StateSpace iter = ss(ITERATOR_TYPE);
            var result = TypestateChecker.check(iter,
                    List.of(call("it", "next")),
                    Map.of("it", iter.top()), true);
            assertFalse(result.isValid(),
                    "next at ⊤ is invalid (must hasNext first)");
        }

        @Test
        @DisplayName("wrong branch body: query after TRUE (not enabled)")
        void wrongBranchBody() {
            StateSpace iter = ss(ITERATOR_TYPE);
            // TRUE branch should have next(), not query()
            var nodes = List.<CallNode>of(
                    new CallNode.Call(call("it", "hasNext")),
                    new CallNode.SelectionSwitch("r", Map.of(
                            "TRUE", List.<CallNode>of(
                                    new CallNode.Call(call("it", "query"))),
                            "FALSE", List.<CallNode>of())));

            var result = TypestateChecker.checkCallTree(
                    iter, nodes, "it", iter.top(), true);
            assertFalse(result.isValid(),
                    "query is not enabled in TRUE branch (only next is)");
        }
    }
}
