package com.bica.reborn.typestate;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TypestateChecker} — pure state machine logic.
 *
 * <p>No compiler APIs involved; tests construct state spaces from session type
 * strings and check method call sequences against them.
 */
class TypestateCheckerTest {

    // -- helpers ---------------------------------------------------------------

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    private static MethodCall call(String var, String method) {
        return new MethodCall(var, method, 0);
    }

    // =========================================================================
    // Linear sequences
    // =========================================================================

    @Nested
    class LinearSequences {

        @Test
        void correctLinearSequence() {
            var s = ss("open . read . close . end");
            var result = TypestateChecker.check(s, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid(), "Correct sequence should be valid: " + result.errors());
            assertEquals(s.bottom(), result.finalStates().get("f"));
        }

        @Test
        void singleMethodToEnd() {
            var s = ss("done . end");
            var result = TypestateChecker.check(s, List.of(call("x", "done")));
            assertTrue(result.isValid());
        }

        @Test
        void emptyCallsOnEnd() {
            var s = ss("end");
            // No calls needed — end is already bottom
            var result = TypestateChecker.check(s, List.of(),
                    Map.of("f", s.top()), true);
            assertTrue(result.isValid(), "end state needs no calls");
        }
    }

    // =========================================================================
    // Wrong order / disabled method
    // =========================================================================

    @Nested
    class DisabledMethods {

        @Test
        void callBeforeOpen() {
            var s = ss("open . read . close . end");
            var result = TypestateChecker.check(s, List.of(call("f", "read")));
            assertFalse(result.isValid());
            // Expect 2 errors: disabled method + not at end state
            var disabledErrors = result.errors().stream()
                    .filter(e -> !e.method().equals("<end-of-method>"))
                    .toList();
            assertEquals(1, disabledErrors.size());
            var err = disabledErrors.get(0);
            assertEquals("f", err.variable());
            assertEquals("read", err.method());
            assertTrue(err.enabled().contains("open"));
            assertFalse(err.enabled().contains("read"));
        }

        @Test
        void wrongOrder() {
            var s = ss("open . read . close . end");
            var result = TypestateChecker.check(s, List.of(
                    call("f", "open"),
                    call("f", "close")));
            assertFalse(result.isValid());
            assertEquals("close", result.errors().get(0).method());
        }

        @Test
        void callAfterEnd() {
            var s = ss("done . end");
            var result = TypestateChecker.check(s, List.of(
                    call("f", "done"),
                    call("f", "done")));
            assertFalse(result.isValid());
            // Second "done" should fail — already at end
            var err = result.errors().get(0);
            assertEquals("done", err.method());
            assertTrue(err.enabled().isEmpty());
        }

        @Test
        void unknownMethodName() {
            var s = ss("open . end");
            var result = TypestateChecker.check(s, List.of(call("f", "nonexistent")));
            assertFalse(result.isValid());
            assertEquals("nonexistent", result.errors().get(0).method());
        }
    }

    // =========================================================================
    // End-of-method checks
    // =========================================================================

    @Nested
    class EndOfMethod {

        @Test
        void notClosedAtEnd() {
            var s = ss("open . close . end");
            var result = TypestateChecker.check(s, List.of(call("f", "open")));
            assertFalse(result.isValid());
            var endErr = result.errors().stream()
                    .filter(e -> e.method().equals("<end-of-method>"))
                    .findFirst();
            assertTrue(endErr.isPresent(), "Should report end-of-method error");
        }

        @Test
        void noCallsNotEnd() {
            var s = ss("open . end");
            var result = TypestateChecker.check(s, List.of(),
                    Map.of("f", s.top()), true);
            assertFalse(result.isValid());
            assertTrue(result.errors().get(0).message().contains("terminal"));
        }

        @Test
        void endCheckDisabled() {
            var s = ss("open . close . end");
            var result = TypestateChecker.check(s, List.of(call("f", "open")),
                    Map.of("f", s.top()), false);
            assertTrue(result.isValid(), "With checkEnd=false, partial sequence is OK");
        }
    }

    // =========================================================================
    // Branch protocol
    // =========================================================================

    @Nested
    class BranchProtocol {

        @Test
        void eitherBranchValid() {
            var s = ss("&{read: end, write: end}");
            var r1 = TypestateChecker.check(s, List.of(call("f", "read")));
            assertTrue(r1.isValid());
            var r2 = TypestateChecker.check(s, List.of(call("f", "write")));
            assertTrue(r2.isValid());
        }

        @Test
        void branchThenContinuation() {
            var s = ss("&{open: close . end}");
            var result = TypestateChecker.check(s, List.of(
                    call("f", "open"),
                    call("f", "close")));
            assertTrue(result.isValid());
        }

        @Test
        void selectionBranch() {
            var s = ss("+{OK: done . end, ERR: end}");
            var r1 = TypestateChecker.check(s, List.of(
                    call("f", "OK"),
                    call("f", "done")));
            assertTrue(r1.isValid());
            var r2 = TypestateChecker.check(s, List.of(call("f", "ERR")));
            assertTrue(r2.isValid());
        }
    }

    // =========================================================================
    // Recursive protocol
    // =========================================================================

    @Nested
    class RecursiveProtocol {

        @Test
        void loopThenDone() {
            var s = ss("rec X . &{next: X, done: end}");
            var result = TypestateChecker.check(s, List.of(
                    call("it", "next"),
                    call("it", "next"),
                    call("it", "done")));
            assertTrue(result.isValid());
        }

        @Test
        void immediatelyDone() {
            var s = ss("rec X . &{next: X, done: end}");
            var result = TypestateChecker.check(s, List.of(call("it", "done")));
            assertTrue(result.isValid());
        }

        @Test
        void stoppedMidLoop() {
            var s = ss("rec X . &{next: X, done: end}");
            var result = TypestateChecker.check(s, List.of(call("it", "next")));
            // After "next", still in recursive state — not end
            assertFalse(result.isValid());
            assertTrue(result.errors().stream()
                    .anyMatch(e -> e.method().equals("<end-of-method>")));
        }
    }

    // =========================================================================
    // Multiple variables
    // =========================================================================

    @Nested
    class MultipleVariables {

        @Test
        void twoIndependentVariables() {
            var s = ss("open . close . end");
            var result = TypestateChecker.check(s, List.of(
                    call("a", "open"),
                    call("b", "open"),
                    call("a", "close"),
                    call("b", "close")));
            assertTrue(result.isValid());
        }

        @Test
        void oneValidOneInvalid() {
            var s = ss("open . close . end");
            var result = TypestateChecker.check(s, List.of(
                    call("a", "open"),
                    call("b", "close"),  // b hasn't opened yet
                    call("a", "close")));
            assertFalse(result.isValid());
            assertEquals(1, result.errors().stream()
                    .filter(e -> !e.method().equals("<end-of-method>")).count());
            assertEquals("b", result.errors().get(0).variable());
        }

        @Test
        void interleavedCalls() {
            var s = ss("a . b . end");
            var result = TypestateChecker.check(s, List.of(
                    call("x", "a"),
                    call("y", "a"),
                    call("x", "b"),
                    call("y", "b")));
            assertTrue(result.isValid());
        }
    }

    // =========================================================================
    // Explicit initial states
    // =========================================================================

    @Nested
    class InitialStates {

        @Test
        void startFromMidState() {
            var s = ss("open . read . close . end");
            // Find the state after "open"
            int afterOpen = s.step(s.top(), "open").orElseThrow();
            var result = TypestateChecker.check(s, List.of(
                            call("f", "read"),
                            call("f", "close")),
                    Map.of("f", afterOpen), true);
            assertTrue(result.isValid());
        }

        @Test
        void untrackedVariableIgnored() {
            var s = ss("open . end");
            // "g" is not in initialStates, so its calls are ignored
            var result = TypestateChecker.check(s, List.of(
                            call("f", "open"),
                            call("g", "whatever")),
                    Map.of("f", s.top()), true);
            assertTrue(result.isValid());
        }
    }

    // =========================================================================
    // Error message quality
    // =========================================================================

    @Nested
    class ErrorMessages {

        @Test
        void disabledMethodErrorContainsEnabled() {
            var s = ss("&{read: end, write: end}");
            var result = TypestateChecker.check(s, List.of(call("f", "close")));
            assertFalse(result.isValid());
            var err = result.errors().get(0);
            assertTrue(err.message().contains("close"));
            assertTrue(err.message().contains("not enabled"));
            assertTrue(err.enabled().contains("read"));
            assertTrue(err.enabled().contains("write"));
        }

        @Test
        void endOfMethodErrorMentionsState() {
            var s = ss("open . end");
            var result = TypestateChecker.check(s, List.of(),
                    Map.of("f", s.top()), true);
            var err = result.errors().get(0);
            assertTrue(err.message().contains("terminal"));
            assertEquals(s.top(), err.state());
        }

        @Test
        void multipleErrorsReported() {
            var s = ss("open . close . end");
            var result = TypestateChecker.check(s, List.of(
                    call("f", "close"),    // wrong: close before open
                    call("f", "open")));   // after error, still in initial state → this works
            // At least one error for the first "close"
            assertFalse(result.isValid());
            assertTrue(result.errors().stream()
                    .anyMatch(e -> e.method().equals("close")));
        }
    }

    // =========================================================================
    // Selection auto-advance
    // =========================================================================

    @Nested
    class SelectionStates {

        @Test
        void autoAdvancePastSelection() {
            // open . +{OK: read . end, ERR: end}
            // After open, state is pure selection. Calling "read" should
            // auto-advance through OK branch.
            var s = ss("open . +{OK: read . end, ERR: end}");
            var result = TypestateChecker.check(s, List.of(
                    call("f", "open"),
                    call("f", "read")));
            assertTrue(result.isValid(), "Should auto-advance past selection: " + result.errors());
        }

        @Test
        void autoAdvancePastSelectionToEnd() {
            // open . +{OK: end, ERR: end}
            // After open, state is pure selection. All branches lead to end.
            // End-of-method check should pass.
            var s = ss("open . +{OK: end, ERR: end}");
            var result = TypestateChecker.check(s, List.of(call("f", "open")));
            assertTrue(result.isValid(), "Should auto-advance to end: " + result.errors());
        }

        @Test
        void recursiveSelection() {
            // rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}
            // After hasNext, auto-advance through TRUE to reach next.
            var s = ss("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var result = TypestateChecker.check(s, List.of(
                    call("it", "hasNext"),
                    call("it", "next"),
                    call("it", "hasNext")),
                    Map.of("it", s.top()), false);
            assertTrue(result.isValid(), "Iterator pattern should work: " + result.errors());
        }

        @Test
        void noMatchingBranch() {
            // +{OK: read . end, ERR: end}
            // "write" is not in any branch
            var s = ss("+{OK: read . end, ERR: end}");
            var result = TypestateChecker.check(s, List.of(call("f", "write")),
                    Map.of("f", s.top()), false);
            assertFalse(result.isValid());
        }

        @Test
        void nestedSelection() {
            // open . +{OK: read . +{DONE: close . end, MORE: read . end}, ERR: end}
            var s = ss("open . +{OK: read . +{DONE: close . end, MORE: end}, ERR: end}");
            var result = TypestateChecker.check(s, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid(), "Nested selection auto-advance: " + result.errors());
        }

        @Test
        void isPureSelectionState() {
            var s = ss("+{OK: end, ERR: end}");
            assertTrue(TypestateChecker.isPureSelectionState(s, s.top()));
        }

        @Test
        void branchStateIsNotPureSelection() {
            var s = ss("&{read: end, write: end}");
            assertFalse(TypestateChecker.isPureSelectionState(s, s.top()));
        }
    }

    // =========================================================================
    // Call tree checking
    // =========================================================================

    @Nested
    class CallTree {

        @Test
        void flatCallNodesWork() {
            var s = ss("open . close . end");
            var nodes = List.<CallNode>of(
                    new CallNode.Call(call("f", "open")),
                    new CallNode.Call(call("f", "close")));
            var result = TypestateChecker.checkCallTree(s, nodes, "f", s.top(), true);
            assertTrue(result.isValid(), "Flat call nodes: " + result.errors());
        }

        @Test
        void switchBranching() {
            // open . +{OK: read . close . end, ERR: end}
            var s = ss("open . +{OK: read . close . end, ERR: end}");
            var nodes = List.<CallNode>of(
                    new CallNode.Call(call("f", "open")),
                    new CallNode.SelectionSwitch("r", Map.of(
                            "OK", List.of(
                                    new CallNode.Call(call("f", "read")),
                                    new CallNode.Call(call("f", "close"))),
                            "ERR", List.of())));
            var result = TypestateChecker.checkCallTree(s, nodes, "f", s.top(), true);
            assertTrue(result.isValid(), "Switch branching: " + result.errors());
        }

        @Test
        void allBranchesMustBeValid() {
            // open . +{OK: read . close . end, ERR: end}
            // OK branch calls "write" which is invalid
            var s = ss("open . +{OK: read . close . end, ERR: end}");
            var nodes = List.<CallNode>of(
                    new CallNode.Call(call("f", "open")),
                    new CallNode.SelectionSwitch("r", Map.of(
                            "OK", List.of(
                                    new CallNode.Call(call("f", "write"))),  // invalid
                            "ERR", List.of())));
            var result = TypestateChecker.checkCallTree(s, nodes, "f", s.top(), false);
            assertFalse(result.isValid());
        }

        @Test
        void switchOnNonSelectionState() {
            // &{read: end, write: end} — branch state, not selection
            var s = ss("&{read: end, write: end}");
            var nodes = List.<CallNode>of(
                    new CallNode.SelectionSwitch("r", Map.of(
                            "read", List.of())));
            var result = TypestateChecker.checkCallTree(s, nodes, "f", s.top(), false);
            assertFalse(result.isValid());
            assertTrue(result.errors().get(0).message().contains("selection state"));
        }

        @Test
        void callNodeRecords() {
            var c = new CallNode.Call(call("f", "open"));
            assertEquals("f", c.call().variable());
            assertEquals("open", c.call().method());

            var sw = new CallNode.SelectionSwitch("r", Map.of("OK", List.of()));
            assertEquals("r", sw.variable());
            assertEquals(1, sw.branches().size());
        }

        @Test
        void callNodeNullChecks() {
            assertThrows(NullPointerException.class, () -> new CallNode.Call(null));
            assertThrows(NullPointerException.class,
                    () -> new CallNode.SelectionSwitch(null, Map.of()));
            assertThrows(NullPointerException.class,
                    () -> new CallNode.SelectionSwitch("r", null));
        }
    }

    // =========================================================================
    // Records
    // =========================================================================

    @Nested
    class Records {

        @Test
        void methodCallRecord() {
            var mc = new MethodCall("f", "read", 42);
            assertEquals("f", mc.variable());
            assertEquals("read", mc.method());
            assertEquals(42, mc.position());
        }

        @Test
        void methodCallNullVariable() {
            assertThrows(NullPointerException.class, () -> new MethodCall(null, "m", 0));
        }

        @Test
        void methodCallNullMethod() {
            assertThrows(NullPointerException.class, () -> new MethodCall("v", null, 0));
        }

        @Test
        void typestateErrorRecord() {
            var err = new TypestateError("f", "read", 0, java.util.Set.of("open"), "msg");
            assertEquals("f", err.variable());
            assertEquals("read", err.method());
            assertEquals(0, err.state());
            assertEquals(java.util.Set.of("open"), err.enabled());
            assertEquals("msg", err.message());
        }

        @Test
        void typestateResultValid() {
            var r = TypestateResult.valid(Map.of("f", 0));
            assertTrue(r.isValid());
            assertTrue(r.errors().isEmpty());
            assertEquals(0, r.finalStates().get("f"));
        }
    }
}
