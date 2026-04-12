package com.bica.reborn.confluence;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for confluence and Church-Rosser analysis (Step 80m).
 *
 * <p>Port of Python test_confluence.py -- verifies the main theorem:
 * every well-formed session type is confluent (its state space is a meet-semilattice).
 */
class ConfluenceCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    private static StateSpace makeSynthetic(
            Set<Integer> states,
            List<StateSpace.Transition> transitions,
            int top, int bottom) {
        Map<Integer, String> labels = new HashMap<>();
        for (int s : states) labels.put(s, String.valueOf(s));
        return new StateSpace(states, transitions, top, bottom, labels);
    }

    // =========================================================================
    // 1. Trivial cases
    // =========================================================================

    @Nested
    class TrivialCases {

        @Test
        void endIsConfluent() {
            var r = ConfluenceChecker.checkConfluence(build("end"));
            assertTrue(r.isLocallyConfluent());
            assertTrue(r.isGloballyConfluent());
            assertEquals(0, r.criticalPairCount());
        }

        @Test
        void singleBranchIsConfluent() {
            var r = ConfluenceChecker.checkConfluence(build("&{a: end}"));
            assertTrue(r.isLocallyConfluent());
            assertTrue(r.isGloballyConfluent());
        }

        @Test
        void singleSelectionIsConfluent() {
            var ss = build("+{l: end}");
            assertTrue(ConfluenceChecker.isLocallyConfluent(ss));
            assertTrue(ConfluenceChecker.isGloballyConfluent(ss));
        }

        @Test
        void resultFieldTypes() {
            var r = ConfluenceChecker.checkConfluence(build("end"));
            assertNotNull(r);
            assertInstanceOf(Integer.class, r.criticalPairCount());
            assertNotNull(r.missingMeets());
            assertNotNull(r.unclosedCriticalPairs());
        }
    }

    // =========================================================================
    // 2. Branch / selection critical pairs
    // =========================================================================

    @Nested
    class BranchSelectionCriticalPairs {

        @Test
        void twoBranchNoConflictWhenTargetsMerge() {
            var r = ConfluenceChecker.checkConfluence(build("&{a: end, b: end}"));
            assertTrue(r.isLocallyConfluent());
            assertTrue(r.isGloballyConfluent());
        }

        @Test
        void branchesWithDistinctContinuationsCreateCriticalPair() {
            var ss = build("&{a: +{x: end}, b: +{y: end}}");
            var pairs = ConfluenceChecker.findCriticalPairs(ss);
            long topPairs = pairs.stream()
                    .filter(p -> p.origin() == ss.top())
                    .count();
            assertTrue(topPairs >= 1);
            var r = ConfluenceChecker.checkConfluence(ss);
            assertTrue(r.isLocallyConfluent());
            assertTrue(r.isGloballyConfluent());
        }

        @Test
        void twoSelectionIsConfluent() {
            var r = ConfluenceChecker.checkConfluence(build("+{a: end, b: end}"));
            assertTrue(r.isLocallyConfluent());
            assertTrue(r.isGloballyConfluent());
        }
    }

    // =========================================================================
    // 3. Confluence closure (meet)
    // =========================================================================

    @Nested
    class ConfluenceClosure {

        @Test
        void closureOfDistinctBranchesIsBottom() {
            var ss = build("&{a: +{x: end}, b: +{y: end}}");
            var succs = new ArrayList<>(ss.successors(ss.top()));
            Collections.sort(succs);
            assertTrue(succs.size() >= 2);
            Integer w = ConfluenceChecker.confluenceClosure(ss, succs.get(0), succs.get(1));
            assertEquals(ss.bottom(), (int) w);
        }

        @Test
        void closureWithSelfIsSelf() {
            var ss = build("&{a: end, b: end}");
            assertEquals(ss.top(), (int) ConfluenceChecker.confluenceClosure(ss, ss.top(), ss.top()));
        }

        @Test
        void closureUnknownStateReturnsNull() {
            var ss = build("end");
            assertNull(ConfluenceChecker.confluenceClosure(ss, 999, ss.top()));
        }
    }

    // =========================================================================
    // 4. Parallel composition
    // =========================================================================

    @Nested
    class ParallelComposition {

        @Test
        void parallelIsConfluent() {
            var r = ConfluenceChecker.checkConfluence(build("(&{a: end} || &{b: end})"));
            assertTrue(r.isLocallyConfluent());
            assertTrue(r.isGloballyConfluent());
        }

        @Test
        void parallelWithBranchesIsConfluent() {
            var r = ConfluenceChecker.checkConfluence(
                    build("(&{a: end, b: end} || &{c: end, d: end})"));
            assertTrue(r.isLocallyConfluent());
            assertTrue(r.isGloballyConfluent());
        }

        @Test
        void parallelCriticalPairsClose() {
            var ss = build("(&{a: end} || &{b: end})");
            for (var cp : ConfluenceChecker.findCriticalPairs(ss)) {
                assertNotNull(ConfluenceChecker.confluenceClosure(ss, cp.branch1(), cp.branch2()));
            }
        }
    }

    // =========================================================================
    // 5. Equivalence with lattice property (main theorem)
    // =========================================================================

    @Nested
    class MainTheorem {

        @ParameterizedTest
        @CsvSource({
                "end",
                "'&{m: end}'",
                "'+{l: end}'",
                "'&{a: end, b: end}'",
                "'+{a: end, b: end}'",
                "'&{a: end, b: end, c: end}'",
                "'(&{a: end} || &{b: end})'",
                "'(+{a: end} || +{b: end})'",
                "'(&{a: end, b: end} || &{c: end})'",
                "'&{open: +{ok: end, err: end}}'",
        })
        void latticeIffConfluent(String src) {
            var ss = build(src);
            var lat = LatticeChecker.checkLattice(ss);
            var conf = ConfluenceChecker.checkConfluence(ss);
            assertEquals(lat.isLattice(), conf.isGloballyConfluent());
        }

        @ParameterizedTest
        @CsvSource({
                "end",
                "'&{m: end}'",
                "'+{l: end}'",
                "'&{a: end, b: end}'",
                "'+{a: end, b: end}'",
                "'&{a: end, b: end, c: end}'",
                "'(&{a: end} || &{b: end})'",
                "'(+{a: end} || +{b: end})'",
                "'(&{a: end, b: end} || &{c: end})'",
                "'&{open: +{ok: end, err: end}}'",
        })
        void globalConfluenceImpliesLocal(String src) {
            var conf = ConfluenceChecker.checkConfluence(build(src));
            if (conf.isGloballyConfluent()) {
                assertTrue(conf.isLocallyConfluent());
            }
        }

        @ParameterizedTest
        @CsvSource({
                "end",
                "'&{m: end}'",
                "'+{l: end}'",
                "'&{a: end, b: end}'",
                "'+{a: end, b: end}'",
                "'&{a: end, b: end, c: end}'",
                "'(&{a: end} || &{b: end})'",
                "'(+{a: end} || +{b: end})'",
                "'(&{a: end, b: end} || &{c: end})'",
                "'&{open: +{ok: end, err: end}}'",
        })
        void universalConfluenceWellFormed(String src) {
            assertTrue(ConfluenceChecker.isGloballyConfluent(build(src)));
        }
    }

    // =========================================================================
    // 6. Recursive types
    // =========================================================================

    @Nested
    class RecursiveTypes {

        @Test
        void iteratorIsConfluent() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var r = ConfluenceChecker.checkConfluence(ss);
            assertTrue(r.isGloballyConfluent());
        }

        @Test
        void simpleRecursionIsConfluent() {
            var ss = build("rec X . &{m: end, n: X}");
            assertTrue(ConfluenceChecker.isGloballyConfluent(ss));
        }
    }

    // =========================================================================
    // 7. Synthetic state spaces -- Newman's lemma sanity checks
    // =========================================================================

    @Nested
    class SyntheticStateMachines {

        @Test
        void locallyButNotGloballyConfluent() {
            var ss = makeSynthetic(
                    Set.of(0, 1, 2, 3, 4, 5),
                    List.of(
                            new StateSpace.Transition(0, "a", 1),
                            new StateSpace.Transition(0, "b", 2),
                            new StateSpace.Transition(1, "x", 3),
                            new StateSpace.Transition(1, "y", 4),
                            new StateSpace.Transition(2, "y", 4),
                            new StateSpace.Transition(2, "z", 5)),
                    0, 4);
            assertNull(ConfluenceChecker.confluenceClosure(ss, 3, 5));
            assertFalse(ConfluenceChecker.isGloballyConfluent(ss));
        }

        @Test
        void syntheticDiamondIsConfluent() {
            var ss = makeSynthetic(
                    Set.of(0, 1, 2, 3),
                    List.of(
                            new StateSpace.Transition(0, "a", 1),
                            new StateSpace.Transition(0, "b", 2),
                            new StateSpace.Transition(1, "b", 3),
                            new StateSpace.Transition(2, "a", 3)),
                    0, 3);
            var r = ConfluenceChecker.checkConfluence(ss);
            assertTrue(r.isLocallyConfluent());
            assertTrue(r.isGloballyConfluent());
            assertEquals(3, (int) ConfluenceChecker.confluenceClosure(ss, 1, 2));
        }

        @Test
        void noCriticalPairsInChain() {
            var ss = makeSynthetic(
                    Set.of(0, 1, 2, 3),
                    List.of(
                            new StateSpace.Transition(0, "a", 1),
                            new StateSpace.Transition(1, "b", 2),
                            new StateSpace.Transition(2, "c", 3)),
                    0, 3);
            assertEquals(0, ConfluenceChecker.findCriticalPairs(ss).size());
            var r = ConfluenceChecker.checkConfluence(ss);
            assertEquals(0, r.criticalPairCount());
            assertTrue(r.isLocallyConfluent());
        }

        @Test
        void unclosedCriticalPairReported() {
            var ss = makeSynthetic(
                    Set.of(0, 1, 2),
                    List.of(
                            new StateSpace.Transition(0, "a", 1),
                            new StateSpace.Transition(0, "b", 2)),
                    0, 1);
            var r = ConfluenceChecker.checkConfluence(ss);
            assertFalse(r.isLocallyConfluent());
            boolean found = r.unclosedCriticalPairs().stream()
                    .anyMatch(arr -> arr[0] == 0 && arr[1] == 1 && arr[2] == 2);
            assertTrue(found);
        }
    }

    // =========================================================================
    // 8. Algorithmic properties
    // =========================================================================

    @Nested
    class AlgorithmicProperties {

        @Test
        void criticalPairCountMatchesField() {
            var ss = build("&{a: end, b: end, c: end}");
            var pairs = ConfluenceChecker.findCriticalPairs(ss);
            var r = ConfluenceChecker.checkConfluence(ss);
            assertEquals(pairs.size(), r.criticalPairCount());
        }

        @Test
        void criticalPairsDeterministic() {
            var ss = build("&{a: end, b: end, c: end}");
            var p1 = ConfluenceChecker.findCriticalPairs(ss);
            var p2 = ConfluenceChecker.findCriticalPairs(ss);
            assertEquals(p1, p2);
        }

        @ParameterizedTest
        @CsvSource({
                "end",
                "'&{m: end}'",
                "'+{l: end}'",
                "'&{a: end, b: end}'",
                "'+{a: end, b: end}'",
                "'&{a: end, b: end, c: end}'",
                "'(&{a: end} || &{b: end})'",
                "'(+{a: end} || +{b: end})'",
                "'(&{a: end, b: end} || &{c: end})'",
                "'&{open: +{ok: end, err: end}}'",
        })
        void missingMeetsEmptyForWellFormed(String src) {
            var r = ConfluenceChecker.checkConfluence(build(src));
            assertTrue(r.missingMeets().isEmpty());
        }

        @ParameterizedTest
        @CsvSource({
                "end",
                "'&{m: end}'",
                "'+{l: end}'",
                "'&{a: end, b: end}'",
                "'+{a: end, b: end}'",
                "'&{a: end, b: end, c: end}'",
                "'(&{a: end} || &{b: end})'",
                "'(+{a: end} || +{b: end})'",
                "'(&{a: end, b: end} || &{c: end})'",
                "'&{open: +{ok: end, err: end}}'",
        })
        void unclosedCriticalPairsEmptyForWellFormed(String src) {
            var r = ConfluenceChecker.checkConfluence(build(src));
            assertTrue(r.unclosedCriticalPairs().isEmpty());
        }
    }
}
