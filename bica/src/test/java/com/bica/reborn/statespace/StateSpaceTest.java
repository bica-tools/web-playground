package com.bica.reborn.statespace;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace.Transition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for state-space construction ({@link StateSpaceBuilder})
 * and product construction ({@link ProductStateSpace}).
 *
 * <p>Ported from {@code reticulate/tests/test_statespace.py}.
 */
class StateSpaceTest {

    // -- helpers ---------------------------------------------------------------

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    private static Map<String, Integer> transitionsFrom(StateSpace ss, int state) {
        return ss.transitions().stream()
                .filter(t -> t.source() == state)
                .collect(Collectors.toMap(Transition::label, Transition::target));
    }

    private static Set<String> allLabels(StateSpace ss) {
        return ss.transitions().stream()
                .map(Transition::label)
                .collect(Collectors.toSet());
    }

    // =========================================================================
    // Basic constructs
    // =========================================================================

    @Nested
    class End {

        @Test
        void singleState() {
            var s = ss("end");
            assertEquals(1, s.states().size());
            assertEquals(s.top(), s.bottom());
            assertEquals(0, s.transitions().size());
        }

        @Test
        void labelIsEnd() {
            var s = ss("end");
            assertEquals("end", s.labels().get(s.top()));
        }
    }

    @Nested
    class SimpleChain {

        @Test
        void aDotEnd() {
            var s = ss("a . end");
            assertEquals(2, s.states().size());
            assertEquals(1, s.transitions().size());
            var tr = transitionsFrom(s, s.top());
            assertTrue(tr.containsKey("a"));
            assertEquals(s.bottom(), tr.get("a"));
        }

        @Test
        void aBEnd() {
            var s = ss("a . b . end");
            assertEquals(3, s.states().size());
            assertEquals(2, s.transitions().size());

            var tr1 = transitionsFrom(s, s.top());
            assertEquals(Set.of("a"), tr1.keySet());
            int mid = tr1.get("a");

            var tr2 = transitionsFrom(s, mid);
            assertEquals(Set.of("b"), tr2.keySet());
            assertEquals(s.bottom(), tr2.get("b"));
        }

        @Test
        void threeStepChain() {
            var s = ss("a . b . c . end");
            assertEquals(4, s.states().size());
            assertEquals(3, s.transitions().size());
        }
    }

    @Nested
    class BranchTests {

        @Test
        void twoBranches() {
            var s = ss("&{m: end, n: end}");
            assertEquals(2, s.states().size());
            var tr = transitionsFrom(s, s.top());
            assertEquals(Set.of("m", "n"), tr.keySet());
            assertEquals(s.bottom(), tr.get("m"));
            assertEquals(s.bottom(), tr.get("n"));
        }

        @Test
        void branchesToDifferentStates() {
            var s = ss("&{m: a . end, n: end}");
            assertEquals(3, s.states().size());
            var tr = transitionsFrom(s, s.top());
            assertTrue(tr.containsKey("m") && tr.containsKey("n"));
            assertNotEquals(s.bottom(), tr.get("m"));
            assertEquals(s.bottom(), tr.get("n"));
        }
    }

    @Nested
    class SelectTests {

        @Test
        void twoSelections() {
            var s = ss("+{OK: end, ERR: end}");
            assertEquals(2, s.states().size());
            var tr = transitionsFrom(s, s.top());
            assertEquals(Set.of("OK", "ERR"), tr.keySet());
        }

        @Test
        void selectWithContinuations() {
            var s = ss("+{OK: a . end, ERR: end}");
            assertEquals(3, s.states().size());
        }
    }

    // =========================================================================
    // Recursion
    // =========================================================================

    @Nested
    class Recursion {

        @Test
        void simpleLoop() {
            var s = ss("rec X . &{next: X, done: end}");
            assertEquals(2, s.states().size());
            assertEquals(2, s.transitions().size());
            var tr = transitionsFrom(s, s.top());
            assertEquals(Set.of("next", "done"), tr.keySet());
            assertEquals(s.top(), tr.get("next"));
            assertEquals(s.bottom(), tr.get("done"));
        }

        @Test
        void innerStateInLoop() {
            var s = ss("rec X . &{a: &{b: X}, done: end}");
            assertEquals(3, s.states().size());
            assertEquals(3, s.transitions().size());

            var trTop = transitionsFrom(s, s.top());
            assertTrue(trTop.containsKey("a") && trTop.containsKey("done"));
            int afterA = trTop.get("a");
            assertNotEquals(s.top(), afterA);

            var trAfter = transitionsFrom(s, afterA);
            assertEquals(Set.of("b"), trAfter.keySet());
            assertEquals(s.top(), trAfter.get("b"));
        }

        @Test
        void nestedRec() {
            var s = ss("rec X . &{a: rec Y . &{b: Y, done: X}, done: end}");
            assertEquals(3, s.states().size());
            assertEquals(4, s.transitions().size());

            var trTop = transitionsFrom(s, s.top());
            int inner = trTop.get("a");
            assertEquals(s.bottom(), trTop.get("done"));

            var trInner = transitionsFrom(s, inner);
            assertEquals(inner, trInner.get("b"));
            assertEquals(s.top(), trInner.get("done"));
        }

        @Test
        void multipleLoopBack() {
            var s = ss("rec X . &{read: X, peek: X, done: end}");
            assertEquals(2, s.states().size());
            var tr = transitionsFrom(s, s.top());
            assertEquals(s.top(), tr.get("read"));
            assertEquals(s.top(), tr.get("peek"));
            assertEquals(s.bottom(), tr.get("done"));
        }
    }

    // =========================================================================
    // Sequence (complex left-hand side)
    // =========================================================================

    @Nested
    class SequenceTests {

        @Test
        void branchThenMethod() {
            var s = ss("&{m: end} . close . end");
            assertEquals(3, s.states().size());
            var trTop = transitionsFrom(s, s.top());
            assertTrue(trTop.containsKey("m"));
            int mid = trTop.get("m");
            var trMid = transitionsFrom(s, mid);
            assertTrue(trMid.containsKey("close"));
            assertEquals(s.bottom(), trMid.get("close"));
        }
    }

    // =========================================================================
    // Parallel — product construction (spec section 4.4)
    // =========================================================================

    @Nested
    class ParallelFinite {

        private StateSpace s;

        @BeforeEach
        void setUp() {
            s = ss("(a . b . end || c . d . end)");
        }

        @Test
        void stateCount() {
            assertEquals(9, s.states().size());
        }

        @Test
        void allTransitionLabels() {
            assertEquals(Set.of("a", "b", "c", "d"), allLabels(s));
        }

        @Test
        void topHasTwoTransitions() {
            var tr = transitionsFrom(s, s.top());
            assertEquals(Set.of("a", "c"), tr.keySet());
        }

        @Test
        void bottomHasNoTransitions() {
            var tr = transitionsFrom(s, s.bottom());
            assertEquals(0, tr.size());
        }

        @Test
        void bottomReachable() {
            assertTrue(s.reachableFrom(s.top()).contains(s.bottom()));
        }

        @Test
        void transitionCount() {
            assertEquals(12, s.transitions().size());
        }
    }

    @Nested
    class ParallelSimple {

        private StateSpace s;

        @BeforeEach
        void setUp() {
            s = ss("(a . end || b . end)");
        }

        @Test
        void stateCount() {
            assertEquals(4, s.states().size());
        }

        @Test
        void topTransitions() {
            var tr = transitionsFrom(s, s.top());
            assertEquals(Set.of("a", "b"), tr.keySet());
        }

        @Test
        void interleaving() {
            var trTop = transitionsFrom(s, s.top());
            // Path 1: a then b
            int afterA = trTop.get("a");
            var trAfterA = transitionsFrom(s, afterA);
            assertTrue(trAfterA.containsKey("b"));
            assertEquals(s.bottom(), trAfterA.get("b"));

            // Path 2: b then a
            int afterB = trTop.get("b");
            var trAfterB = transitionsFrom(s, afterB);
            assertTrue(trAfterB.containsKey("a"));
            assertEquals(s.bottom(), trAfterB.get("a"));
        }
    }

    @Nested
    class ParallelRecursive {

        private StateSpace s;

        @BeforeEach
        void setUp() {
            s = ss("(rec X . &{a: X, done: end} || rec Y . &{c: Y, stop: end})");
        }

        @Test
        void stateCount() {
            assertEquals(4, s.states().size());
        }

        @Test
        void topHasLoopAndExit() {
            var tr = transitionsFrom(s, s.top());
            assertEquals(Set.of("a", "c", "done", "stop"), tr.keySet());
        }

        @Test
        void loopBack() {
            var tr = transitionsFrom(s, s.top());
            assertEquals(s.top(), tr.get("a"));
            assertEquals(s.top(), tr.get("c"));
        }

        @Test
        void bottomReachable() {
            assertTrue(s.reachableFrom(s.top()).contains(s.bottom()));
        }
    }

    @Nested
    class ParallelWithSequence {

        private StateSpace s;

        @BeforeEach
        void setUp() {
            s = ss("(a . end || b . end) . close . end");
        }

        @Test
        void stateCount() {
            assertEquals(5, s.states().size());
        }

        @Test
        void reachable() {
            assertTrue(s.reachableFrom(s.top()).contains(s.bottom()));
        }
    }

    // =========================================================================
    // Spec examples
    // =========================================================================

    @Nested
    class SpecSharedFile {

        private StateSpace s;

        @BeforeEach
        void setUp() {
            s = ss("init . &{open: +{OK: (read . end || write . end) . close . end, ERROR: end}}");
        }

        @Test
        void reachable() {
            assertTrue(s.reachableFrom(s.top()).contains(s.bottom()));
        }

        @Test
        void initTransition() {
            var tr = transitionsFrom(s, s.top());
            assertEquals(Set.of("init"), tr.keySet());
        }

        @Test
        void allMethodsPresent() {
            var labels = allLabels(s);
            assertTrue(labels.containsAll(
                    Set.of("init", "open", "OK", "ERROR", "read", "write", "close")));
        }
    }

    @Nested
    class SpecConcurrentFileRecursive {

        private StateSpace s;

        @BeforeEach
        void setUp() {
            s = ss("open . (rec X . &{read: X, doneReading: end} "
                    + "|| rec Y . &{write: Y, doneWriting: end}) . close . end");
        }

        @Test
        void stateCount() {
            assertEquals(6, s.states().size());
        }

        @Test
        void allMethods() {
            var labels = allLabels(s);
            assertTrue(labels.containsAll(
                    Set.of("open", "read", "write", "doneReading", "doneWriting", "close")));
        }

        @Test
        void reachable() {
            assertTrue(s.reachableFrom(s.top()).contains(s.bottom()));
        }
    }

    // =========================================================================
    // StateSpace methods
    // =========================================================================

    @Nested
    class Methods {

        @Test
        void enabled() {
            var s = ss("&{m: end, n: end}");
            var enabled = s.enabled(s.top());
            var labels = enabled.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            assertEquals(Set.of("m", "n"), labels);
        }

        @Test
        void successors() {
            var s = ss("a . b . end");
            var succ = s.successors(s.top());
            assertEquals(1, succ.size());
            int mid = succ.iterator().next();
            assertEquals(Set.of(s.bottom()), s.successors(mid));
        }

        @Test
        void reachableFromBottom() {
            var s = ss("a . end");
            assertEquals(Set.of(s.bottom()), s.reachableFrom(s.bottom()));
        }
    }

    // =========================================================================
    // step() and enabledLabels()
    // =========================================================================

    @Nested
    class StepAndEnabledLabels {

        @Test
        void stepReturnsTarget() {
            var s = ss("a . b . end");
            var result = s.step(s.top(), "a");
            assertTrue(result.isPresent());
            // The target of 'a' should have 'b' enabled
            assertEquals(Set.of("b"), s.enabledLabels(result.getAsInt()));
        }

        @Test
        void stepReturnsEmptyForDisabledLabel() {
            var s = ss("a . end");
            assertTrue(s.step(s.top(), "b").isEmpty());
        }

        @Test
        void stepAtBottom() {
            var s = ss("a . end");
            assertTrue(s.step(s.bottom(), "a").isEmpty());
        }

        @Test
        void enabledLabelsAtTop() {
            var s = ss("&{m: end, n: end}");
            assertEquals(Set.of("m", "n"), s.enabledLabels(s.top()));
        }

        @Test
        void enabledLabelsAtBottom() {
            var s = ss("a . end");
            assertEquals(Set.of(), s.enabledLabels(s.bottom()));
        }

        @Test
        void stepThroughChain() {
            var s = ss("a . b . c . end");
            var s1 = s.step(s.top(), "a");
            assertTrue(s1.isPresent());
            var s2 = s.step(s1.getAsInt(), "b");
            assertTrue(s2.isPresent());
            var s3 = s.step(s2.getAsInt(), "c");
            assertTrue(s3.isPresent());
            assertEquals(s.bottom(), s3.getAsInt());
        }

        @Test
        void enabledLabelsOnBranch() {
            var s = ss("&{open: +{OK: end, ERR: end}}");
            assertEquals(Set.of("open"), s.enabledLabels(s.top()));
            var after = s.step(s.top(), "open");
            assertTrue(after.isPresent());
            assertEquals(Set.of("OK", "ERR"), s.enabledLabels(after.getAsInt()));
        }
    }

    // =========================================================================
    // TransitionKind — selection vs method
    // =========================================================================

    @Nested
    class TransitionKindTests {

        @Test
        void selectProducesSelectionKind() {
            var s = ss("+{OK: end, ERR: end}");
            for (var t : s.transitions()) {
                assertEquals(TransitionKind.SELECTION, t.kind(),
                        "Select transitions should have SELECTION kind");
            }
        }

        @Test
        void branchProducesMethodKind() {
            var s = ss("&{m: end, n: end}");
            for (var t : s.transitions()) {
                assertEquals(TransitionKind.METHOD, t.kind(),
                        "Branch transitions should have METHOD kind");
            }
        }

        @Test
        void mixedProtocol_methodThenSelection() {
            // m . +{OK: end, ERR: end}
            var s = ss("m . +{OK: end, ERR: end}");
            var topTr = s.transitionsFrom(s.top());
            assertEquals(1, topTr.size());
            assertEquals("m", topTr.getFirst().label());
            assertEquals(TransitionKind.METHOD, topTr.getFirst().kind());

            int selectState = topTr.getFirst().target();
            var selectTr = s.transitionsFrom(selectState);
            assertEquals(2, selectTr.size());
            for (var t : selectTr) {
                assertEquals(TransitionKind.SELECTION, t.kind());
            }
        }

        @Test
        void enabledMethodsFiltersSelections() {
            var s = ss("m . +{OK: end, ERR: end}");
            // At top: only method 'm'
            assertEquals(Set.of("m"), s.enabledMethods(s.top()));
            assertEquals(Set.of(), s.enabledSelections(s.top()));
        }

        @Test
        void enabledSelectionsFiltersMethod() {
            var s = ss("m . +{OK: end, ERR: end}");
            int selectState = s.transitionsFrom(s.top()).getFirst().target();
            assertEquals(Set.of("OK", "ERR"), s.enabledSelections(selectState));
            assertEquals(Set.of(), s.enabledMethods(selectState));
        }

        @Test
        void transitionsFromReturnsFullObjects() {
            var s = ss("&{m: end, n: end}");
            var tr = s.transitionsFrom(s.top());
            assertEquals(2, tr.size());
            for (var t : tr) {
                assertEquals(s.top(), t.source());
                assertNotNull(t.kind());
            }
        }

        @Test
        void productPreservesSelectionKind() {
            // (m . end || +{OK: end, ERR: end})
            var s = ss("(m . end || +{OK: end, ERR: end})");
            boolean hasMethodM = false;
            boolean hasSelectionOK = false;
            boolean hasSelectionERR = false;
            for (var t : s.transitions()) {
                if (t.label().equals("m") && t.kind() == TransitionKind.METHOD) hasMethodM = true;
                if (t.label().equals("OK") && t.kind() == TransitionKind.SELECTION) hasSelectionOK = true;
                if (t.label().equals("ERR") && t.kind() == TransitionKind.SELECTION) hasSelectionERR = true;
            }
            assertTrue(hasMethodM, "m should be METHOD");
            assertTrue(hasSelectionOK, "OK should be SELECTION");
            assertTrue(hasSelectionERR, "ERR should be SELECTION");
        }

        @Test
        void recursionPreservesSelectionKind() {
            var s = ss("rec X . +{OK: &{m: X}, ERR: end}");
            boolean hasSelectionOK = false;
            boolean hasMethodM = false;
            for (var t : s.transitions()) {
                if (t.label().equals("OK") && t.kind() == TransitionKind.SELECTION) hasSelectionOK = true;
                if (t.label().equals("m") && t.kind() == TransitionKind.METHOD) hasMethodM = true;
            }
            assertTrue(hasSelectionOK, "OK should be SELECTION after recursion");
            assertTrue(hasMethodM, "m should be METHOD after recursion");
        }

        @Test
        void enabledLabelsReturnsAll() {
            // enabledLabels should still return all labels (both kinds)
            var s = ss("m . +{OK: end, ERR: end}");
            int selectState = s.transitionsFrom(s.top()).getFirst().target();
            assertEquals(Set.of("OK", "ERR"), s.enabledLabels(selectState));
        }

        @Test
        void backwardCompatTransitionDefaultsToMethod() {
            // 3-arg Transition constructor defaults to METHOD
            var t = new StateSpace.Transition(0, "foo", 1);
            assertEquals(TransitionKind.METHOD, t.kind());
        }
    }

    // =========================================================================
    // Error cases
    // =========================================================================

    @Nested
    class Errors {

        @Test
        void unboundVariable() {
            var ex = assertThrows(IllegalArgumentException.class,
                    () -> StateSpaceBuilder.build(Parser.parse("X")));
            assertTrue(ex.getMessage().contains("unbound"));
        }
    }
}
