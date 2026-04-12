package com.bica.reborn.livelock;

import com.bica.reborn.livelock.LivelockChecker.*;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for livelock detection module (Step 80c).
 * Mirrors all 46 Python tests from test_livelock.py.
 */
class LivelockCheckerTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    /**
     * State space with a trapped cycle: 0 -> 1 -> 2 -> 1, bottom = 3.
     * States 1 and 2 form a cycle with no exit to bottom (state 3).
     */
    private StateSpace makeLivelockedSs() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new Transition(0, "enter", 1),
                        new Transition(1, "loop_a", 2),
                        new Transition(2, "loop_b", 1)),
                0, 3,
                Map.of(0, "top", 1, "cycle_a", 2, "cycle_b", 3, "end"));
    }

    /**
     * State space where cycle has an exit to bottom.
     * 0 -> 1 -> 2 -> 1 (cycle), but 2 -> 3 (exit to bottom).
     */
    private StateSpace makeLivelockFreeSs() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new Transition(0, "enter", 1),
                        new Transition(1, "loop_a", 2),
                        new Transition(2, "loop_b", 1),
                        new Transition(2, "exit", 3)),
                0, 3,
                Map.of(0, "top", 1, "cycle_a", 2, "cycle_b", 3, "end"));
    }

    /**
     * State space with a self-loop livelock: 0 -> 1 -> 1, bottom = 2.
     */
    private StateSpace makeSelfLoopLivelock() {
        return new StateSpace(
                Set.of(0, 1, 2),
                List.of(
                        new Transition(0, "enter", 1),
                        new Transition(1, "spin", 1)),
                0, 2,
                Map.of(0, "top", 1, "spinner", 2, "end"));
    }

    /**
     * State space with two trapped SCCs and one safe SCC.
     * SCC1: {1, 2} trapped cycle
     * SCC2: {4, 5} trapped cycle
     * SCC3: {6, 7} safe cycle (7 -> 8 = bottom)
     */
    private StateSpace makeMultipleSccsSs() {
        return new StateSpace(
                Set.of(0, 1, 2, 4, 5, 6, 7, 8),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(0, "b", 4),
                        new Transition(0, "c", 6),
                        new Transition(1, "x", 2),
                        new Transition(2, "y", 1),
                        new Transition(4, "p", 5),
                        new Transition(5, "q", 4),
                        new Transition(6, "r", 7),
                        new Transition(7, "s", 6),
                        new Transition(7, "exit", 8)),
                0, 8,
                Map.of(0, "top", 1, "c1a", 2, "c1b", 4, "c2a", 5, "c2b",
                        6, "c3a", 7, "c3b", 8, "end"));
    }

    /**
     * Linear: 0 -> 1 -> 2 (bottom). No cycles.
     */
    private StateSpace makeSimpleLinear() {
        return new StateSpace(
                Set.of(0, 1, 2),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(1, "b", 2)),
                0, 2,
                Map.of(0, "top", 1, "mid", 2, "end"));
    }

    // -----------------------------------------------------------------------
    // Tests: detectLivelock
    // -----------------------------------------------------------------------

    @Nested
    class DetectLivelockTests {

        @Test
        void trappedCycle() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.detectLivelock(ss);
            assertTrue(result.contains(1));
            assertTrue(result.contains(2));
            assertFalse(result.contains(0));
            assertFalse(result.contains(3));
        }

        @Test
        void safeCycle() {
            var ss = makeLivelockFreeSs();
            var result = LivelockChecker.detectLivelock(ss);
            assertEquals(0, result.size());
        }

        @Test
        void linearNoLivelock() {
            var ss = makeSimpleLinear();
            var result = LivelockChecker.detectLivelock(ss);
            assertEquals(0, result.size());
        }

        @Test
        void selfLoopLivelock() {
            var ss = makeSelfLoopLivelock();
            var result = LivelockChecker.detectLivelock(ss);
            assertTrue(result.contains(1));
        }

        @Test
        void multipleTrappedSccs() {
            var ss = makeMultipleSccsSs();
            var result = LivelockChecker.detectLivelock(ss);
            assertTrue(result.contains(1));
            assertTrue(result.contains(2));
            assertTrue(result.contains(4));
            assertTrue(result.contains(5));
            // Safe cycle {6, 7} should not be livelocked
            assertFalse(result.contains(6));
            assertFalse(result.contains(7));
        }

        @Test
        void returnsUnmodifiableSet() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.detectLivelock(ss);
            assertThrows(UnsupportedOperationException.class,
                    () -> ((Set<Integer>) result).add(99));
        }

        @Test
        void endOnly() {
            // Single end state: no livelock
            var ss = new StateSpace(
                    Set.of(0),
                    List.of(),
                    0, 0,
                    Map.of(0, "end"));
            var result = LivelockChecker.detectLivelock(ss);
            assertEquals(0, result.size());
        }
    }

    // -----------------------------------------------------------------------
    // Tests: livelockSccs
    // -----------------------------------------------------------------------

    @Nested
    class LivelockSccsTests {

        @Test
        void trappedCycleReturnsOneScc() {
            var ss = makeLivelockedSs();
            var sccs = LivelockChecker.livelockSccs(ss);
            assertEquals(1, sccs.size());
            assertEquals(Set.of(1, 2), sccs.get(0));
        }

        @Test
        void safeCycleReturnsEmpty() {
            var ss = makeLivelockFreeSs();
            var sccs = LivelockChecker.livelockSccs(ss);
            assertEquals(0, sccs.size());
        }

        @Test
        void multipleTrapped() {
            var ss = makeMultipleSccsSs();
            var sccs = LivelockChecker.livelockSccs(ss);
            assertEquals(2, sccs.size());
            Set<Set<Integer>> sccSets = new HashSet<>(sccs);
            assertTrue(sccSets.contains(Set.of(1, 2)));
            assertTrue(sccSets.contains(Set.of(4, 5)));
        }

        @Test
        void selfLoopScc() {
            var ss = makeSelfLoopLivelock();
            var sccs = LivelockChecker.livelockSccs(ss);
            assertEquals(1, sccs.size());
            assertTrue(sccs.get(0).contains(1));
        }

        @Test
        void linearNoSccs() {
            var ss = makeSimpleLinear();
            var sccs = LivelockChecker.livelockSccs(ss);
            assertEquals(0, sccs.size());
        }
    }

    // -----------------------------------------------------------------------
    // Tests: tropicalLivelockScore
    // -----------------------------------------------------------------------

    @Nested
    class TropicalLivelockScoreTests {

        @Test
        void trappedPositiveScore() {
            var ss = makeLivelockedSs();
            double score = LivelockChecker.tropicalLivelockScore(ss);
            assertTrue(score > 0.0);
        }

        @Test
        void safeZeroScore() {
            var ss = makeLivelockFreeSs();
            double score = LivelockChecker.tropicalLivelockScore(ss);
            assertEquals(0.0, score);
        }

        @Test
        void linearZeroScore() {
            var ss = makeSimpleLinear();
            double score = LivelockChecker.tropicalLivelockScore(ss);
            assertEquals(0.0, score);
        }

        @Test
        void selfLoopPositive() {
            var ss = makeSelfLoopLivelock();
            double score = LivelockChecker.tropicalLivelockScore(ss);
            assertTrue(score > 0.0);
        }

        @Test
        void unitWeightEigenvalue() {
            // For unit-weight cycles, tropical eigenvalue = 1.0
            var ss = makeLivelockedSs();
            double score = LivelockChecker.tropicalLivelockScore(ss);
            assertEquals(1.0, score, 0.01);
        }
    }

    // -----------------------------------------------------------------------
    // Tests: heatKernelLivelock
    // -----------------------------------------------------------------------

    @Nested
    class HeatKernelLivelockTests {

        @Test
        void trappedStatesHighDiagonal() {
            var ss = makeLivelockedSs();
            var indicators = LivelockChecker.heatKernelLivelock(ss, 10.0);
            assertFalse(indicators.isEmpty());
            for (var ind : indicators) {
                assertTrue(Set.of(1, 2).contains(ind.state()));
                assertTrue(ind.diagonalValue() >= 0.0);
            }
        }

        @Test
        void noLivelockNoIndicators() {
            var ss = makeLivelockFreeSs();
            var indicators = LivelockChecker.heatKernelLivelock(ss, 10.0);
            assertTrue(indicators.isEmpty());
        }

        @Test
        void linearNoIndicators() {
            var ss = makeSimpleLinear();
            var indicators = LivelockChecker.heatKernelLivelock(ss, 5.0);
            assertTrue(indicators.isEmpty());
        }

        @Test
        void indicatorType() {
            var ss = makeLivelockedSs();
            var indicators = LivelockChecker.heatKernelLivelock(ss, 5.0);
            for (var ind : indicators) {
                assertInstanceOf(HeatKernelIndicator.class, ind);
            }
        }

        @Test
        void smallTStillDetects() {
            var ss = makeLivelockedSs();
            var indicators = LivelockChecker.heatKernelLivelock(ss, 0.1);
            assertFalse(indicators.isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // Tests: isLivelockFree
    // -----------------------------------------------------------------------

    @Nested
    class IsLivelockFreeTests {

        @Test
        void trappedNotFree() {
            var ss = makeLivelockedSs();
            assertFalse(LivelockChecker.isLivelockFree(ss));
        }

        @Test
        void safeCycleFree() {
            var ss = makeLivelockFreeSs();
            assertTrue(LivelockChecker.isLivelockFree(ss));
        }

        @Test
        void linearFree() {
            var ss = makeSimpleLinear();
            assertTrue(LivelockChecker.isLivelockFree(ss));
        }

        @Test
        void selfLoopNotFree() {
            var ss = makeSelfLoopLivelock();
            assertFalse(LivelockChecker.isLivelockFree(ss));
        }
    }

    // -----------------------------------------------------------------------
    // Tests: analyzeLivelock
    // -----------------------------------------------------------------------

    @Nested
    class AnalyzeLivelockTests {

        @Test
        void resultType() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            assertInstanceOf(LivelockResult.class, result);
        }

        @Test
        void trappedAnalysis() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            assertFalse(result.isLivelockFree());
            assertEquals(1, result.numTrappedSccs());
            assertEquals(2, result.totalTrappedStates());
            assertTrue(result.maxTropicalEigenvalue() > 0.0);
            assertTrue(result.livelocked().contains(1));
            assertTrue(result.livelocked().contains(2));
        }

        @Test
        void safeAnalysis() {
            var ss = makeLivelockFreeSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            assertTrue(result.isLivelockFree());
            assertEquals(0, result.numTrappedSccs());
            assertEquals(0, result.totalTrappedStates());
            assertEquals(0.0, result.maxTropicalEigenvalue());
        }

        @Test
        void summaryMessage() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            assertTrue(result.summary().contains("LIVELOCK"));
        }

        @Test
        void summaryFree() {
            var ss = makeLivelockFreeSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            assertTrue(result.summary().toLowerCase().contains("free"));
        }

        @Test
        void reachableFromTop() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            // States 1 and 2 are reachable from top (0 -> 1 -> 2)
            assertTrue(result.reachableFromTop().contains(1));
            assertTrue(result.reachableFromTop().contains(2));
        }

        @Test
        void heatIndicatorsPresent() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            assertFalse(result.heatIndicators().isEmpty());
        }

        @Test
        void numStatesAndTransitions() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            assertEquals(4, result.numStates());
            assertEquals(3, result.numTransitions());
        }

        @Test
        void trappedSccDetails() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            assertEquals(1, result.trappedSccs().size());
            var scc = result.trappedSccs().get(0);
            assertInstanceOf(LivelockSCC.class, scc);
            assertEquals(Set.of(1, 2), scc.states());
            assertTrue(scc.tropicalEigenvalue() > 0.0);
            assertTrue(scc.cycleLabels().contains("loop_a")
                    || scc.cycleLabels().contains("loop_b"));
        }

        @Test
        void entryTransitions() {
            var ss = makeLivelockedSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            var scc = result.trappedSccs().get(0);
            // Entry: (0, "enter", 1) -- stored as [0, 1] in our representation
            assertEquals(1, scc.entryTransitions().size());
            assertEquals(0, scc.entryTransitions().get(0)[0]);
            assertEquals(1, scc.entryTransitions().get(0)[1]);
        }

        @Test
        void multipleSccsAnalysis() {
            var ss = makeMultipleSccsSs();
            var result = LivelockChecker.analyzeLivelock(ss);
            assertFalse(result.isLivelockFree());
            assertEquals(2, result.numTrappedSccs());
            assertEquals(4, result.totalTrappedStates());
        }
    }

    // -----------------------------------------------------------------------
    // Tests with parsed session types
    // -----------------------------------------------------------------------

    @Nested
    class ParsedTypesTests {

        @Test
        void simpleBranchNoLivelock() {
            var ss = build("&{a: end, b: end}");
            assertTrue(LivelockChecker.isLivelockFree(ss));
        }

        @Test
        void recursiveWithExit() {
            var ss = build("rec X . &{a: X, b: end}");
            assertTrue(LivelockChecker.isLivelockFree(ss));
        }

        @Test
        void recursiveNoExitLivelock() {
            // rec X . &{a: X} -- pure loop with no exit
            var ss = build("rec X . &{a: X}");
            var livelocked = LivelockChecker.detectLivelock(ss);
            // The top state forms a self-loop SCC
            assertTrue(livelocked.size() > 0 || ss.top() == ss.bottom());
        }

        @Test
        void selectionWithExit() {
            var ss = build("+{ok: end, err: end}");
            assertTrue(LivelockChecker.isLivelockFree(ss));
        }

        @Test
        void nestedRecSafe() {
            var ss = build("rec X . &{a: &{b: X, c: end}}");
            assertTrue(LivelockChecker.isLivelockFree(ss));
        }

        @Test
        void parallelBranches() {
            var ss = build("(&{a: end} || &{b: end})");
            assertTrue(LivelockChecker.isLivelockFree(ss));
        }

        @Test
        void endType() {
            var ss = build("end");
            assertTrue(LivelockChecker.isLivelockFree(ss));
        }

        @Test
        void scoreOnParsedSafe() {
            var ss = build("rec X . &{a: X, b: end}");
            assertEquals(0.0, LivelockChecker.tropicalLivelockScore(ss));
        }

        @Test
        void analyzeOnParsed() {
            var ss = build("&{a: &{b: end}, c: end}");
            var result = LivelockChecker.analyzeLivelock(ss);
            assertTrue(result.isLivelockFree());
            assertEquals(0, result.numTrappedSccs());
        }
    }
}
