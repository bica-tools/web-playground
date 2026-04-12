package com.bica.reborn.lattice;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for lattice property checking ({@link LatticeChecker}).
 *
 * <p>Ported from {@code reticulate/tests/test_lattice.py}.
 */
class LatticeCheckerTest {

    // -- helpers ---------------------------------------------------------------

    private static LatticeResult check(String source) {
        return LatticeChecker.checkLattice(
                StateSpaceBuilder.build(Parser.parse(source)));
    }

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    // =========================================================================
    // Basic lattice checks
    // =========================================================================

    @Nested
    class SingleState {

        @Test
        void isLattice() {
            assertTrue(check("end").isLattice());
        }

        @Test
        void oneScc() {
            assertEquals(1, check("end").numScc());
        }

        @Test
        void hasTopAndBottom() {
            var r = check("end");
            assertTrue(r.hasTop());
            assertTrue(r.hasBottom());
        }

        @Test
        void noCounterexample() {
            assertNull(check("end").counterexample());
        }
    }

    @Nested
    class Chain {

        @Test
        void isLattice() {
            assertTrue(check("a . b . end").isLattice());
        }

        @Test
        void threeSccs() {
            assertEquals(3, check("a . b . end").numScc());
        }

        @Test
        void allProperties() {
            var r = check("a . b . end");
            assertTrue(r.hasTop());
            assertTrue(r.hasBottom());
            assertTrue(r.allMeetsExist());
            assertTrue(r.allJoinsExist());
        }
    }

    @Nested
    class BranchToEnd {

        @Test
        void isLattice() {
            assertTrue(check("&{m: end, n: end}").isLattice());
        }

        @Test
        void twoSccs() {
            assertEquals(2, check("&{m: end, n: end}").numScc());
        }
    }

    @Nested
    class BranchDiamond {

        @Test
        void isLattice() {
            assertTrue(check("&{m: a . end, n: b . end}").isLattice());
        }

        @Test
        void fourSccs() {
            assertEquals(4, check("&{m: a . end, n: b . end}").numScc());
        }

        @Test
        void meetOfBranchesIsBottom() {
            var s = ss("&{m: a . end, n: b . end}");
            var intermediates = s.states().stream()
                    .filter(st -> st != s.top() && st != s.bottom())
                    .sorted()
                    .toList();
            assertEquals(2, intermediates.size());
            assertEquals(s.bottom(),
                    LatticeChecker.computeMeet(s, intermediates.get(0), intermediates.get(1)));
        }

        @Test
        void joinOfBranchesIsTop() {
            var s = ss("&{m: a . end, n: b . end}");
            var intermediates = s.states().stream()
                    .filter(st -> st != s.top() && st != s.bottom())
                    .sorted()
                    .toList();
            assertEquals(2, intermediates.size());
            assertEquals(s.top(),
                    LatticeChecker.computeJoin(s, intermediates.get(0), intermediates.get(1)));
        }
    }

    // =========================================================================
    // Recursion and cycles
    // =========================================================================

    @Nested
    class SelfLoopRecursion {

        @Test
        void isLattice() {
            assertTrue(check("rec X . &{next: X, done: end}").isLattice());
        }

        @Test
        void twoSccs() {
            assertEquals(2, check("rec X . &{next: X, done: end}").numScc());
        }
    }

    @Nested
    class CycleRecursion {

        @Test
        void isLattice() {
            assertTrue(check("rec X . &{a: &{b: X}, done: end}").isLattice());
        }

        @Test
        void twoSccs() {
            assertEquals(2, check("rec X . &{a: &{b: X}, done: end}").numScc());
        }
    }

    @Nested
    class NestedRecTests {

        @Test
        void isLattice() {
            assertTrue(check("rec X . &{a: rec Y . &{b: Y, done: X}, done: end}").isLattice());
        }

        @Test
        void sccCount() {
            assertEquals(2,
                    check("rec X . &{a: rec Y . &{b: Y, done: X}, done: end}").numScc());
        }
    }

    // =========================================================================
    // Product (parallel) lattices
    // =========================================================================

    @Nested
    class Product2x2 {

        private StateSpace s;
        private LatticeResult result;

        @BeforeEach
        void setUp() {
            s = ss("(a . end || b . end)");
            result = LatticeChecker.checkLattice(s);
        }

        @Test
        void isLattice() {
            assertTrue(result.isLattice());
        }

        @Test
        void fourSccs() {
            assertEquals(4, result.numScc());
        }

        @Test
        void meetOfIntermediates() {
            var intermediates = s.states().stream()
                    .filter(st -> st != s.top() && st != s.bottom())
                    .sorted()
                    .toList();
            assertEquals(2, intermediates.size());
            assertEquals(s.bottom(),
                    LatticeChecker.computeMeet(s, intermediates.get(0), intermediates.get(1)));
        }

        @Test
        void joinOfIntermediates() {
            var intermediates = s.states().stream()
                    .filter(st -> st != s.top() && st != s.bottom())
                    .sorted()
                    .toList();
            assertEquals(2, intermediates.size());
            assertEquals(s.top(),
                    LatticeChecker.computeJoin(s, intermediates.get(0), intermediates.get(1)));
        }

        @Test
        void meetWithSelf() {
            for (int st : s.states()) {
                assertEquals(st, LatticeChecker.computeMeet(s, st, st));
            }
        }

        @Test
        void joinWithSelf() {
            for (int st : s.states()) {
                assertEquals(st, LatticeChecker.computeJoin(s, st, st));
            }
        }
    }

    @Nested
    class Product3x3 {

        private LatticeResult result;

        @BeforeEach
        void setUp() {
            result = check("(a . b . end || c . d . end)");
        }

        @Test
        void isLattice() {
            assertTrue(result.isLattice());
        }

        @Test
        void nineSccs() {
            assertEquals(9, result.numScc());
        }
    }

    @Nested
    class RecursiveProduct {

        private LatticeResult result;

        @BeforeEach
        void setUp() {
            result = check(
                    "(rec X . &{a: X, done: end} || rec Y . &{c: Y, stop: end})");
        }

        @Test
        void isLattice() {
            assertTrue(result.isLattice());
        }

        @Test
        void fourSccs() {
            assertEquals(4, result.numScc());
        }
    }

    // =========================================================================
    // Non-lattice (manually constructed state spaces)
    // =========================================================================

    @Nested
    class NonLatticeNoMeet {

        private StateSpace s;

        @BeforeEach
        void setUp() {
            // top -> a, top -> b, a -> c, a -> d, b -> c, b -> d, c -> end, d -> end
            s = new StateSpace(
                    Set.of(0, 1, 2, 3, 4, 5),
                    List.of(
                            new Transition(0, "go_a", 1),
                            new Transition(0, "go_b", 2),
                            new Transition(1, "to_c", 3),
                            new Transition(1, "to_d", 4),
                            new Transition(2, "to_c", 3),
                            new Transition(2, "to_d", 4),
                            new Transition(3, "finish", 5),
                            new Transition(4, "finish", 5)),
                    0, 5,
                    Map.of(0, "top", 1, "a", 2, "b", 3, "c", 4, "d", 5, "end"));
        }

        @Test
        void notLattice() {
            assertFalse(LatticeChecker.checkLattice(s).isLattice());
        }

        @Test
        void counterexampleReportsFailure() {
            var r = LatticeChecker.checkLattice(s);
            assertNotNull(r.counterexample());
            assertTrue(
                    r.counterexample().kind().equals("no_meet")
                            || r.counterexample().kind().equals("no_join"));
        }

        @Test
        void allMeetsFalse() {
            assertFalse(LatticeChecker.checkLattice(s).allMeetsExist());
        }

        @Test
        void hasTopAndBottom() {
            var r = LatticeChecker.checkLattice(s);
            assertTrue(r.hasTop());
            assertTrue(r.hasBottom());
        }
    }

    @Nested
    class NonLatticeNoJoin {

        private StateSpace s;

        @BeforeEach
        void setUp() {
            // top -> c, top -> d, c -> a, c -> b, d -> a, d -> b, a -> end, b -> end
            s = new StateSpace(
                    Set.of(0, 1, 2, 3, 4, 5),
                    List.of(
                            new Transition(0, "go_c", 1),
                            new Transition(0, "go_d", 2),
                            new Transition(1, "to_a", 3),
                            new Transition(1, "to_b", 4),
                            new Transition(2, "to_a", 3),
                            new Transition(2, "to_b", 4),
                            new Transition(3, "finish", 5),
                            new Transition(4, "finish", 5)),
                    0, 5,
                    Map.of(0, "top", 1, "c", 2, "d", 3, "a", 4, "b", 5, "end"));
        }

        @Test
        void notLattice() {
            assertFalse(LatticeChecker.checkLattice(s).isLattice());
        }

        @Test
        void counterexampleExists() {
            assertNotNull(LatticeChecker.checkLattice(s).counterexample());
        }
    }

    // =========================================================================
    // Meet/Join API tests
    // =========================================================================

    @Nested
    class MeetJoinAPI {

        @Test
        void meetTopWithAnything() {
            var s = ss("a . b . end");
            for (int st : s.states()) {
                assertEquals(st, LatticeChecker.computeMeet(s, s.top(), st));
            }
        }

        @Test
        void joinBottomWithAnything() {
            var s = ss("a . b . end");
            for (int st : s.states()) {
                assertEquals(st, LatticeChecker.computeJoin(s, s.bottom(), st));
            }
        }

        @Test
        void meetBottomWithAnything() {
            var s = ss("a . b . end");
            for (int st : s.states()) {
                assertEquals(s.bottom(), LatticeChecker.computeMeet(s, s.bottom(), st));
            }
        }

        @Test
        void joinTopWithAnything() {
            var s = ss("a . b . end");
            for (int st : s.states()) {
                assertEquals(s.top(), LatticeChecker.computeJoin(s, s.top(), st));
            }
        }

        @Test
        void meetInChain() {
            var s = ss("a . b . end");
            var tr = s.transitions().stream()
                    .filter(t -> t.source() == s.top())
                    .collect(Collectors.toMap(Transition::label, Transition::target));
            int mid = tr.get("a");
            assertEquals(mid, LatticeChecker.computeMeet(s, s.top(), mid));
            assertEquals(s.bottom(), LatticeChecker.computeMeet(s, mid, s.bottom()));
        }

        @Test
        void joinInChain() {
            var s = ss("a . b . end");
            var tr = s.transitions().stream()
                    .filter(t -> t.source() == s.top())
                    .collect(Collectors.toMap(Transition::label, Transition::target));
            int mid = tr.get("a");
            assertEquals(s.top(), LatticeChecker.computeJoin(s, s.top(), mid));
            assertEquals(mid, LatticeChecker.computeJoin(s, mid, s.bottom()));
        }

        @Test
        void meetNonexistentState() {
            var s = ss("end");
            assertNull(LatticeChecker.computeMeet(s, s.top(), 999));
        }

        @Test
        void joinNonexistentState() {
            var s = ss("end");
            assertNull(LatticeChecker.computeJoin(s, s.top(), 999));
        }
    }

    // =========================================================================
    // SCC map tests
    // =========================================================================

    @Nested
    class SCCMap {

        @Test
        void selfLoopSameScc() {
            var s = ss("rec X . &{next: X, done: end}");
            var r = LatticeChecker.checkLattice(s);
            assertEquals(s.top(), r.sccMap().get(s.top()));
        }

        @Test
        void cycleSameScc() {
            var s = ss("rec X . &{a: &{b: X}, done: end}");
            var r = LatticeChecker.checkLattice(s);
            var tr = s.transitions().stream()
                    .filter(t -> t.source() == s.top())
                    .collect(Collectors.toMap(Transition::label, Transition::target));
            int afterA = tr.get("a");
            assertEquals(r.sccMap().get(s.top()), r.sccMap().get(afterA));
        }

        @Test
        void endSeparateScc() {
            var s = ss("a . b . end");
            var r = LatticeChecker.checkLattice(s);
            assertEquals(s.bottom(), r.sccMap().get(s.bottom()));
        }
    }

    // =========================================================================
    // Full spec examples
    // =========================================================================

    @Nested
    class SpecSharedFile {

        @Test
        void isLattice() {
            assertTrue(check(
                    "init . &{open: +{OK: (read . end || write . end) . close . end, ERROR: end}}")
                    .isLattice());
        }

        @Test
        void hasAllProperties() {
            var r = check(
                    "init . &{open: +{OK: (read . end || write . end) . close . end, ERROR: end}}");
            assertTrue(r.hasTop());
            assertTrue(r.hasBottom());
            assertTrue(r.allMeetsExist());
            assertTrue(r.allJoinsExist());
        }
    }

    @Nested
    class SpecConcurrentFileRecursive {

        @Test
        void isLattice() {
            assertTrue(check(
                    "open . (rec X . &{read: X, doneReading: end} "
                            + "|| rec Y . &{write: Y, doneWriting: end}) . close . end")
                    .isLattice());
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    class EdgeCases {

        @Test
        void singleMethod() {
            var r = check("a . end");
            assertTrue(r.isLattice());
            assertEquals(2, r.numScc());
        }

        @Test
        void longChain() {
            var r = check("a . b . c . d . e . end");
            assertTrue(r.isLattice());
            assertEquals(6, r.numScc());
        }

        @Test
        void selectDiamond() {
            var r = check("+{OK: a . end, ERR: b . end}");
            assertTrue(r.isLattice());
            assertEquals(4, r.numScc());
        }

        @Test
        void threeBranches() {
            var r = check("&{a: end, b: end, c: end}");
            assertTrue(r.isLattice());
            assertEquals(2, r.numScc());
        }
    }
}
