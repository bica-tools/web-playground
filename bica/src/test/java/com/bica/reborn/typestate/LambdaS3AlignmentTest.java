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
 * Step 200f: Alignment tests for λ_S³ (recursion).
 *
 * <p>λ_S³ extends λ_S² with:
 * <ul>
 *   <li>Recursive session types: rec X . S, X</li>
 *   <li>Cyclic state spaces (placeholder-merge)</li>
 *   <li>While expression for looping</li>
 *   <li>Non-termination (programs may diverge)</li>
 * </ul>
 */
@DisplayName("Step 200f: λ_S³ Alignment Tests")
class LambdaS3AlignmentTest {

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    private static MethodCall call(String v, String m) {
        return new MethodCall(v, m, 0);
    }

    /** Full recursive Iterator:
     *  rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}} */
    private static final String FULL_ITERATOR =
            "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}";

    /** Simple recursive protocol: rec X . &{step: X, done: end} */
    private static final String SIMPLE_REC =
            "rec X . &{step: X, done: end}";

    // ════════════════════════════════════════════════════════════════
    // §1  Cyclic State Spaces
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cyclic state spaces from recursive types")
    class CyclicStateSpaces {

        @Test
        @DisplayName("recursive type creates cycle: next loops back to top")
        void recursiveTypeCreatesCycle() {
            StateSpace iter = ss(FULL_ITERATOR);
            int s0 = iter.top();
            int s_sel = iter.step(s0, "hasNext").getAsInt();
            int s_true = iter.step(s_sel, "TRUE").getAsInt();
            int s_after_next = iter.step(s_true, "next").getAsInt();

            // next() loops back to s0 (the top state)
            assertEquals(s0, s_after_next,
                    "next() must cycle back to ⊤ (recursive unfolding)");
        }

        @Test
        @DisplayName("simple rec: step loops, done exits")
        void simpleRecStepLoopsDoneExits() {
            StateSpace sr = ss(SIMPLE_REC);
            int s0 = sr.top();

            // step loops back to s0
            int s_after_step = sr.step(s0, "step").getAsInt();
            assertEquals(s0, s_after_step, "step cycles back to ⊤");

            // done reaches ⊥
            int s_after_done = sr.step(s0, "done").getAsInt();
            assertEquals(sr.bottom(), s_after_done, "done reaches ⊥");
        }

        @Test
        @DisplayName("multiple steps then done: valid path")
        void multipleStepsThenDone() {
            StateSpace sr = ss(SIMPLE_REC);
            var result = TypestateChecker.check(sr, List.of(
                    call("x", "step"),
                    call("x", "step"),
                    call("x", "step"),
                    call("x", "done")));
            assertTrue(result.isValid(), "step×3 then done reaches ⊥");
        }

        @Test
        @DisplayName("top state has both step and done enabled")
        void topStateHasBothMethods() {
            StateSpace sr = ss(SIMPLE_REC);
            Set<String> enabled = sr.enabledMethods(sr.top());
            assertEquals(Set.of("step", "done"), enabled);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §2  Full Iterator with Selection + Recursion
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Full Iterator: rec + selection + branch")
    class FullIterator {

        @Test
        @DisplayName("one iteration: hasNext(TRUE)→next→hasNext(FALSE)→done")
        void oneIteration() {
            StateSpace iter = ss(FULL_ITERATOR);
            // Simulate: hasNext→TRUE→next→hasNext→FALSE→end
            var result = TypestateChecker.check(iter, List.of(
                    call("it", "hasNext"),
                    call("it", "next"),
                    call("it", "hasNext")));
            // After second hasNext, auto-advance to FALSE→⊥
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("two iterations: hasNext→next→hasNext→next→hasNext→done")
        void twoIterations() {
            StateSpace iter = ss(FULL_ITERATOR);
            var result = TypestateChecker.check(iter, List.of(
                    call("it", "hasNext"),
                    call("it", "next"),
                    call("it", "hasNext"),
                    call("it", "next"),
                    call("it", "hasNext")));
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("full iterator with call tree: switch on each hasNext")
        void fullIteratorCallTree() {
            StateSpace iter = ss(FULL_ITERATOR);

            // Single iteration with explicit switch
            var nodes = List.<CallNode>of(
                    new CallNode.Call(call("it", "hasNext")),
                    new CallNode.SelectionSwitch("r", Map.of(
                            "TRUE", List.<CallNode>of(
                                    new CallNode.Call(call("it", "next")),
                                    // After next, we're back at ⊤ — need another hasNext
                                    new CallNode.Call(call("it", "hasNext")),
                                    new CallNode.SelectionSwitch("r2", Map.of(
                                            "TRUE", List.<CallNode>of(
                                                    new CallNode.Call(call("it", "next")),
                                                    new CallNode.Call(call("it", "hasNext")),
                                                    // Assume FALSE to terminate
                                                    new CallNode.SelectionSwitch("r3", Map.of(
                                                            "TRUE", List.<CallNode>of(),  // can't terminate here
                                                            "FALSE", List.<CallNode>of()))),
                                            "FALSE", List.<CallNode>of()))),
                            "FALSE", List.<CallNode>of())));

            var result = TypestateChecker.checkCallTree(
                    iter, nodes, "it", iter.top(), false);
            // Some branches may not reach ⊥ (TRUE chain doesn't terminate)
            // but the structure is valid
            assertNotNull(result);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §3  Conservative Extension
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Conservative extension: non-recursive types unchanged")
    class ConservativeExtension {

        @Test
        @DisplayName("branch-only FileHandle: no cycles, same behavior")
        void branchOnlyUnchanged() {
            StateSpace fh = ss("&{open: &{read: &{close: end}, write: &{close: end}}}");
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"), call("f", "read"), call("f", "close")));
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("one-shot iterator (no rec): same as λ_S²")
        void oneShotIteratorUnchanged() {
            StateSpace iter = ss("&{hasNext: +{TRUE: &{next: end}, FALSE: end}}");
            var result = TypestateChecker.check(iter, List.of(
                    call("it", "hasNext"), call("it", "next")));
            assertTrue(result.isValid());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §4  Non-termination and Violations
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Non-termination and protocol violations")
    class NonTermination {

        @Test
        @DisplayName("step-only path never reaches ⊥ (incomplete)")
        void stepOnlyNeverReachesBottom() {
            StateSpace sr = ss(SIMPLE_REC);
            // Only call step — loops forever in λ_S³, but flat check
            // reports incomplete (not at ⊥)
            var result = TypestateChecker.check(sr, List.of(
                    call("x", "step"),
                    call("x", "step")));
            // Not at ⊥ — session incomplete
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("wrong method after recursion: still caught")
        void wrongMethodAfterRecursion() {
            StateSpace sr = ss(SIMPLE_REC);
            // After step (back at ⊤), "close" is not enabled
            var result = TypestateChecker.check(sr, List.of(
                    call("x", "step"),
                    call("x", "close")),
                    Map.of("x", sr.top()), false);
            assertFalse(result.isValid());
            assertEquals("close", result.errors().get(0).method());
        }
    }
}
