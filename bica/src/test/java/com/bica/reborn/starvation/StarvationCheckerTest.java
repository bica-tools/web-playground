package com.bica.reborn.starvation;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.starvation.StarvationChecker.*;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for starvation detection module (Step 80d).
 * Mirrors all 44 Python tests from test_starvation.py.
 */
class StarvationCheckerTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    /**
     * State space with a starved transition.
     * 0 (top) -> 1 -> 3 (bottom)
     * 0 -> 2 (dead end, cannot reach bottom)
     * 2 -> 2 (self-loop, starved)
     */
    private StateSpace makeStarvedSs() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(0, "b", 2),
                        new Transition(1, "c", 3),
                        new Transition(2, "spin", 2)),
                0, 3,
                Map.of(0, "top", 1, "mid", 2, "dead", 3, "end"));
    }

    /**
     * State space where all transitions are on top-to-bottom paths.
     * 0 -> 1 -> 3, 0 -> 2 -> 3
     */
    private StateSpace makeAllReachableSs() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(0, "b", 2),
                        new Transition(1, "c", 3),
                        new Transition(2, "d", 3)),
                0, 3,
                Map.of(0, "top", 1, "left", 2, "right", 3, "end"));
    }

    /**
     * State space with a state not reachable from top.
     * 0 -> 1 -> 2 (bottom), 3 -> 2 (state 3 unreachable from top)
     */
    private StateSpace makeUnreachableFromTop() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(1, "b", 2),
                        new Transition(3, "c", 2)),
                0, 2,
                Map.of(0, "top", 1, "mid", 2, "end", 3, "orphan"));
    }

    /** Simple linear: 0 -> 1 -> 2. */
    private StateSpace makeLinear() {
        return new StateSpace(
                Set.of(0, 1, 2),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(1, "b", 2)),
                0, 2,
                Map.of(0, "top", 1, "mid", 2, "end"));
    }

    /** State space with both branch and select transitions. */
    private StateSpace makeMixedPolarity() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(0, "b", 2, TransitionKind.SELECTION),
                        new Transition(1, "c", 3),
                        new Transition(2, "d", 3)),
                0, 3,
                Map.of(0, "top", 1, "left", 2, "right", 3, "end"));
    }

    /** Wide branching: top -> many children -> bottom. */
    private StateSpace makeWideBranch() {
        return new StateSpace(
                Set.of(0, 1, 2, 3, 4, 5, 6),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(0, "b", 2),
                        new Transition(0, "c", 3),
                        new Transition(0, "d", 4),
                        new Transition(0, "e", 5),
                        new Transition(1, "f", 6),
                        new Transition(2, "f", 6),
                        new Transition(3, "f", 6),
                        new Transition(4, "f", 6),
                        new Transition(5, "f", 6)),
                0, 6,
                Map.of(0, "s0", 1, "s1", 2, "s2", 3, "s3", 4, "s4", 5, "s5", 6, "s6"));
    }

    /** Empty/trivial state space: single state, no transitions. */
    private StateSpace makeEmpty() {
        return new StateSpace(
                Set.of(0),
                List.of(),
                0, 0,
                Map.of(0, "top"));
    }

    // -----------------------------------------------------------------------
    // Test: detectStarvation
    // -----------------------------------------------------------------------

    @Nested
    class DetectStarvation {

        @Test
        void starvedTransitions() {
            var ss = makeStarvedSs();
            var starved = StarvationChecker.detectStarvation(ss);
            var tuples = new HashSet<String>();
            for (var s : starved) {
                tuples.add(s.source() + "," + s.label() + "," + s.target());
            }
            assertTrue(tuples.contains("0,b,2"));
            assertTrue(tuples.contains("2,spin,2"));
        }

        @Test
        void allReachable() {
            var ss = makeAllReachableSs();
            assertEquals(0, StarvationChecker.detectStarvation(ss).size());
        }

        @Test
        void unreachableSource() {
            var ss = makeUnreachableFromTop();
            var starved = StarvationChecker.detectStarvation(ss);
            assertEquals(1, starved.size());
            assertEquals(3, starved.get(0).source());
            assertEquals("c", starved.get(0).label());
        }

        @Test
        void linearNoStarvation() {
            var ss = makeLinear();
            assertEquals(0, StarvationChecker.detectStarvation(ss).size());
        }

        @Test
        void returnsStarvedTransitionType() {
            var ss = makeStarvedSs();
            var starved = StarvationChecker.detectStarvation(ss);
            for (var s : starved) {
                assertInstanceOf(StarvedTransition.class, s);
                assertNotNull(s.reason());
            }
        }

        @Test
        void emptyStateSpace() {
            var ss = makeEmpty();
            assertEquals(0, StarvationChecker.detectStarvation(ss).size());
        }

        @Test
        void reasonIncludesContext() {
            var ss = makeStarvedSs();
            var starved = StarvationChecker.detectStarvation(ss);
            var reasons = starved.stream().map(StarvedTransition::reason).toList();
            assertTrue(reasons.stream().anyMatch(r -> r.contains("bottom") || r.contains("top")));
        }
    }

    // -----------------------------------------------------------------------
    // Test: perRoleSpectralGap
    // -----------------------------------------------------------------------

    @Nested
    class PerRoleSpectralGap {

        @Test
        void allReachableLabels() {
            var ss = makeAllReachableSs();
            var gaps = StarvationChecker.perRoleSpectralGap(ss);
            assertFalse(gaps.isEmpty());
            for (var entry : gaps.entrySet()) {
                var rsg = entry.getValue();
                assertInstanceOf(RoleSpectralGap.class, rsg);
                assertEquals(entry.getKey(), rsg.label());
                assertFalse(rsg.isStarved());
            }
        }

        @Test
        void starvedLabelDetected() {
            var ss = makeStarvedSs();
            var gaps = StarvationChecker.perRoleSpectralGap(ss);
            assertTrue(gaps.containsKey("spin"));
            assertTrue(gaps.get("spin").isStarved());
        }

        @Test
        void spectralGapRange() {
            var ss = makeAllReachableSs();
            var gaps = StarvationChecker.perRoleSpectralGap(ss);
            for (var rsg : gaps.values()) {
                assertTrue(rsg.spectralGap() >= 0.0 && rsg.spectralGap() <= 1.0);
            }
        }

        @Test
        void countMatches() {
            var ss = makeAllReachableSs();
            var gaps = StarvationChecker.perRoleSpectralGap(ss);
            int total = gaps.values().stream().mapToInt(RoleSpectralGap::count).sum();
            assertEquals(ss.transitions().size(), total);
        }

        @Test
        void wideBranchLabels() {
            var ss = makeWideBranch();
            var gaps = StarvationChecker.perRoleSpectralGap(ss);
            assertTrue(gaps.containsKey("f"));
            assertEquals(5, gaps.get("f").count());
        }
    }

    // -----------------------------------------------------------------------
    // Test: fairnessScore
    // -----------------------------------------------------------------------

    @Nested
    class FairnessScore {

        @Test
        void linearFairness() {
            var ss = makeLinear();
            double score = StarvationChecker.fairnessScore(ss);
            assertTrue(score >= 0.0 && score <= 1.0);
        }

        @Test
        void allReachableFairness() {
            var ss = makeAllReachableSs();
            double score = StarvationChecker.fairnessScore(ss);
            assertTrue(score >= 0.0 && score <= 1.0);
        }

        @Test
        void starvedLowerFairness() {
            var ss = makeStarvedSs();
            double score = StarvationChecker.fairnessScore(ss);
            assertTrue(score < 1.0);
        }

        @Test
        void emptyPerfectFairness() {
            var ss = makeEmpty();
            assertEquals(1.0, StarvationChecker.fairnessScore(ss));
        }

        @Test
        void wideBranchFairness() {
            var ss = makeWideBranch();
            double score = StarvationChecker.fairnessScore(ss);
            assertTrue(score >= 0.0 && score <= 1.0);
        }
    }

    // -----------------------------------------------------------------------
    // Test: pendularFairness
    // -----------------------------------------------------------------------

    @Nested
    class PendularFairness {

        @Test
        void allBranchTransitions() {
            var ss = makeAllReachableSs();
            double balance = StarvationChecker.pendularFairness(ss);
            assertTrue(balance >= 0.0 && balance <= 1.0);
        }

        @Test
        void mixedPolarity() {
            var ss = makeMixedPolarity();
            double balance = StarvationChecker.pendularFairness(ss);
            assertTrue(balance > 0.0 && balance <= 1.0);
        }

        @Test
        void emptyProtocol() {
            var ss = makeEmpty();
            assertEquals(1.0, StarvationChecker.pendularFairness(ss));
        }

        @Test
        void range() {
            var ss = makeWideBranch();
            double balance = StarvationChecker.pendularFairness(ss);
            assertTrue(balance >= 0.0 && balance <= 1.0);
        }
    }

    // -----------------------------------------------------------------------
    // Test: isStarvationFree
    // -----------------------------------------------------------------------

    @Nested
    class IsStarvationFree {

        @Test
        void starvedNotFree() {
            assertFalse(StarvationChecker.isStarvationFree(makeStarvedSs()));
        }

        @Test
        void allReachableFree() {
            assertTrue(StarvationChecker.isStarvationFree(makeAllReachableSs()));
        }

        @Test
        void linearFree() {
            assertTrue(StarvationChecker.isStarvationFree(makeLinear()));
        }

        @Test
        void unreachableSourceNotFree() {
            assertFalse(StarvationChecker.isStarvationFree(makeUnreachableFromTop()));
        }
    }

    // -----------------------------------------------------------------------
    // Test: analyzeStarvation
    // -----------------------------------------------------------------------

    @Nested
    class AnalyzeStarvation {

        @Test
        void resultType() {
            var result = StarvationChecker.analyzeStarvation(makeStarvedSs());
            assertInstanceOf(StarvationResult.class, result);
        }

        @Test
        void starvedAnalysis() {
            var result = StarvationChecker.analyzeStarvation(makeStarvedSs());
            assertFalse(result.isStarvationFree());
            assertTrue(result.numStarved() > 0);
            assertTrue(result.coverageRatio() < 1.0);
        }

        @Test
        void cleanAnalysis() {
            var result = StarvationChecker.analyzeStarvation(makeAllReachableSs());
            assertTrue(result.isStarvationFree());
            assertEquals(0, result.numStarved());
            assertEquals(1.0, result.coverageRatio());
        }

        @Test
        void summaryMessageStarved() {
            var result = StarvationChecker.analyzeStarvation(makeStarvedSs());
            assertTrue(result.summary().contains("STARVATION"));
        }

        @Test
        void summaryMessageFree() {
            var result = StarvationChecker.analyzeStarvation(makeAllReachableSs());
            assertTrue(result.summary().toLowerCase().contains("free"));
        }

        @Test
        void reachableStates() {
            var result = StarvationChecker.analyzeStarvation(makeAllReachableSs());
            assertEquals(Set.of(0, 1, 2, 3), result.reachableStates());
            assertTrue(result.unreachableStates().isEmpty());
        }

        @Test
        void unreachableStates() {
            var result = StarvationChecker.analyzeStarvation(makeUnreachableFromTop());
            assertTrue(result.unreachableStates().contains(3));
        }

        @Test
        void totalTransitions() {
            var result = StarvationChecker.analyzeStarvation(makeStarvedSs());
            assertEquals(4, result.numTotalTransitions());
        }

        @Test
        void spectralGapsPresent() {
            var result = StarvationChecker.analyzeStarvation(makeAllReachableSs());
            assertFalse(result.roleSpectralGaps().isEmpty());
        }

        @Test
        void fairnessInResult() {
            var result = StarvationChecker.analyzeStarvation(makeAllReachableSs());
            assertTrue(result.fairness() >= 0.0 && result.fairness() <= 1.0);
        }

        @Test
        void pendularInResult() {
            var result = StarvationChecker.analyzeStarvation(makeAllReachableSs());
            assertTrue(result.pendularBalance() >= 0.0 && result.pendularBalance() <= 1.0);
        }
    }

    // -----------------------------------------------------------------------
    // Tests with parsed session types
    // -----------------------------------------------------------------------

    @Nested
    class ParsedTypes {

        @Test
        void simpleBranch() {
            assertTrue(StarvationChecker.isStarvationFree(build("&{a: end, b: end}")));
        }

        @Test
        void recursiveSafe() {
            var result = StarvationChecker.analyzeStarvation(
                    build("rec X . &{a: X, b: end}"));
            assertInstanceOf(StarvationResult.class, result);
        }

        @Test
        void selection() {
            assertTrue(StarvationChecker.isStarvationFree(build("+{ok: end, err: end}")));
        }

        @Test
        void nestedBranch() {
            assertTrue(StarvationChecker.isStarvationFree(
                    build("&{a: &{b: end, c: end}, d: end}")));
        }

        @Test
        void parallel() {
            var result = StarvationChecker.analyzeStarvation(
                    build("(&{a: end} || &{b: end})"));
            assertInstanceOf(StarvationResult.class, result);
        }

        @Test
        void endType() {
            assertTrue(StarvationChecker.isStarvationFree(build("end")));
        }

        @Test
        void fairnessOnParsed() {
            double score = StarvationChecker.fairnessScore(build("&{a: end, b: end}"));
            assertTrue(score >= 0.0 && score <= 1.0);
        }

        @Test
        void spectralGapOnParsed() {
            var gaps = StarvationChecker.perRoleSpectralGap(build("&{a: end, b: end, c: end}"));
            assertFalse(gaps.isEmpty());
        }
    }
}
