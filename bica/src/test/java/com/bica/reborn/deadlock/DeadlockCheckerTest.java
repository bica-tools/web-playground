package com.bica.reborn.deadlock;

import com.bica.reborn.deadlock.DeadlockChecker.*;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for deadlock detection module (Step 80b).
 * Mirrors all 71 Python tests from test_deadlock.py.
 */
class DeadlockCheckerTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    /**
     * Create a hand-crafted state space with a deadlock.
     * States: 0 (top), 1 (deadlock), 2 (bottom)
     * Transitions: 0->1 via 'a', 0->2 via 'b'
     * State 1 has no outgoing transitions and is not bottom.
     */
    private StateSpace makeDeadlockedSs() {
        return new StateSpace(
                Set.of(0, 1, 2),
                List.of(new Transition(0, "a", 1), new Transition(0, "b", 2)),
                0, 2,
                Map.of(0, "top", 1, "deadlocked", 2, "end"));
    }

    /**
     * Create a state space with a black hole (cycle not reaching bottom).
     * States: 0 (top), 1, 2, 3 (bottom)
     * Transitions: 0->1 via 'a', 0->3 via 'b', 1->2 via 'c', 2->1 via 'd'
     * States 1,2 form a cycle that never reaches bottom (3).
     */
    private StateSpace makeBlackHoleSs() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(0, "b", 3),
                        new Transition(1, "c", 2),
                        new Transition(2, "d", 1)),
                0, 3,
                Map.of(0, "top", 1, "orbit1", 2, "orbit2", 3, "end"));
    }

    /**
     * Create a state space with multiple deadlocks.
     * 0 -> 1 (a), 0 -> 2 (b), 0 -> 3 (c)
     * 1 is deadlocked, 2 is deadlocked, 3 is bottom.
     */
    private StateSpace makeMultiDeadlockSs() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(0, "b", 2),
                        new Transition(0, "c", 3)),
                0, 3,
                Map.of(0, "top", 1, "dead1", 2, "dead2", 3, "end"));
    }

    // -----------------------------------------------------------------------
    // Test: detectDeadlocks
    // -----------------------------------------------------------------------

    @Nested
    class DetectDeadlocks {

        @Test
        void endIsDeadlockFree() {
            var ss = build("end");
            assertEquals(0, DeadlockChecker.detectDeadlocks(ss).size());
        }

        @Test
        void simpleBranchDeadlockFree() {
            var ss = build("&{a: end, b: end}");
            assertEquals(0, DeadlockChecker.detectDeadlocks(ss).size());
        }

        @Test
        void handcraftedDeadlock() {
            var ss = makeDeadlockedSs();
            var dl = DeadlockChecker.detectDeadlocks(ss);
            assertEquals(1, dl.size());
            assertEquals(1, dl.get(0).state());
        }

        @Test
        void multiDeadlock() {
            var ss = makeMultiDeadlockSs();
            var dl = DeadlockChecker.detectDeadlocks(ss);
            assertEquals(2, dl.size());
            var states = new HashSet<Integer>();
            dl.forEach(d -> states.add(d.state()));
            assertEquals(Set.of(1, 2), states);
        }

        @Test
        void deadlockLabels() {
            var ss = makeDeadlockedSs();
            var dl = DeadlockChecker.detectDeadlocks(ss);
            assertEquals("deadlocked", dl.get(0).label());
        }

        @Test
        void deadlockDepth() {
            var ss = makeDeadlockedSs();
            var dl = DeadlockChecker.detectDeadlocks(ss);
            assertEquals(1, dl.get(0).depthFromTop());
        }

        @Test
        void iteratorDeadlockFree() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            assertEquals(0, DeadlockChecker.detectDeadlocks(ss).size());
        }

        @Test
        void selectionDeadlockFree() {
            var ss = build("+{ok: end, err: end}");
            assertEquals(0, DeadlockChecker.detectDeadlocks(ss).size());
        }

        @Test
        void parallelDeadlockFree() {
            var ss = build("(&{a: end} || &{b: end})");
            assertEquals(0, DeadlockChecker.detectDeadlocks(ss).size());
        }
    }

    // -----------------------------------------------------------------------
    // Test: isDeadlockFree
    // -----------------------------------------------------------------------

    @Nested
    class IsDeadlockFree {

        @Test
        void endFree() {
            assertTrue(DeadlockChecker.isDeadlockFree(build("end")));
        }

        @Test
        void branchFree() {
            assertTrue(DeadlockChecker.isDeadlockFree(build("&{open: &{close: end}}")));
        }

        @Test
        void handcraftedNotFree() {
            assertFalse(DeadlockChecker.isDeadlockFree(makeDeadlockedSs()));
        }

        @Test
        void recursiveFree() {
            assertTrue(DeadlockChecker.isDeadlockFree(
                    build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")));
        }

        @Test
        void nestedBranchFree() {
            assertTrue(DeadlockChecker.isDeadlockFree(build("&{a: &{b: end}, c: end}")));
        }
    }

    // -----------------------------------------------------------------------
    // Test: latticeDeadlockCertificate
    // -----------------------------------------------------------------------

    @Nested
    class TestLatticeCertificate {

        @Test
        void latticeEnd() {
            var cert = DeadlockChecker.latticeDeadlockCertificate(build("end"));
            assertTrue(cert.isLattice());
            assertTrue(cert.certificateValid());
        }

        @Test
        void latticeBranch() {
            var cert = DeadlockChecker.latticeDeadlockCertificate(build("&{a: end, b: end}"));
            assertTrue(cert.isLattice());
            assertTrue(cert.certificateValid());
            assertTrue(cert.explanation().contains("DEADLOCK-FREE"));
        }

        @Test
        void latticeIterator() {
            var cert = DeadlockChecker.latticeDeadlockCertificate(
                    build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertTrue(cert.certificateValid());
        }

        @Test
        void nonLatticeDeadlocked() {
            var ss = makeDeadlockedSs();
            var cert = DeadlockChecker.latticeDeadlockCertificate(ss);
            // The handcrafted ss is not a lattice
            assertTrue(!cert.certificateValid() || !cert.isLattice());
        }

        @Test
        void hasUniqueBottom() {
            var cert = DeadlockChecker.latticeDeadlockCertificate(build("&{a: end, b: end}"));
            assertTrue(cert.hasUniqueBottom());
        }

        @Test
        void allReachBottom() {
            var cert = DeadlockChecker.latticeDeadlockCertificate(build("&{a: end, b: end}"));
            assertTrue(cert.allReachBottom());
        }
    }

    // -----------------------------------------------------------------------
    // Test: blackHoleDetection
    // -----------------------------------------------------------------------

    @Nested
    class TestBlackHoleDetection {

        @Test
        void noBlackHolesEnd() {
            var bh = DeadlockChecker.blackHoleDetection(build("end"));
            assertEquals(0, bh.totalTrappedStates());
        }

        @Test
        void noBlackHolesBranch() {
            var bh = DeadlockChecker.blackHoleDetection(build("&{a: end, b: end}"));
            assertEquals(0, bh.totalTrappedStates());
            assertEquals(0, bh.blackHoles().size());
        }

        @Test
        void blackHoleCycle() {
            var ss = makeBlackHoleSs();
            var bh = DeadlockChecker.blackHoleDetection(ss);
            assertEquals(2, bh.totalTrappedStates());
            assertEquals(Set.of(1, 2), new HashSet<>(bh.blackHoles()));
        }

        @Test
        void eventHorizons() {
            var ss = makeBlackHoleSs();
            var bh = DeadlockChecker.blackHoleDetection(ss);
            // Transition 0->1 via 'a' is the event horizon
            assertEquals(1, bh.eventHorizons().size());
            assertEquals(0, bh.eventHorizons().get(0)[0]); // source
            assertEquals(1, bh.eventHorizons().get(0)[1]); // target
        }

        @Test
        void noBlackHolesRecursive() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var bh = DeadlockChecker.blackHoleDetection(ss);
            assertEquals(0, bh.totalTrappedStates());
        }

        @Test
        void noBlackHolesParallel() {
            var ss = build("(&{a: end} || &{b: end})");
            var bh = DeadlockChecker.blackHoleDetection(ss);
            assertEquals(0, bh.totalTrappedStates());
        }
    }

    // -----------------------------------------------------------------------
    // Test: spectralDeadlockRisk
    // -----------------------------------------------------------------------

    @Nested
    class TestSpectralRisk {

        @Test
        void endRisk() {
            var risk = DeadlockChecker.spectralDeadlockRisk(build("end"));
            assertNotNull(risk);
        }

        @Test
        void branchRisk() {
            var risk = DeadlockChecker.spectralDeadlockRisk(build("&{a: end, b: end}"));
            assertTrue(risk.fiedlerValue() >= 0);
            assertTrue(risk.riskScore() >= 0 && risk.riskScore() <= 1);
        }

        @Test
        void wellConnectedLowRisk() {
            var ss = build("&{a: end, b: end}");
            var risk = DeadlockChecker.spectralDeadlockRisk(ss);
            assertTrue(risk.riskScore() < 0.8);
        }

        @Test
        void diagnosisPresent() {
            var risk = DeadlockChecker.spectralDeadlockRisk(build("&{a: end, b: end}"));
            assertFalse(risk.diagnosis().isEmpty());
        }

        @Test
        void spectralGapNonneg() {
            var risk = DeadlockChecker.spectralDeadlockRisk(build("&{a: &{b: end}, c: end}"));
            assertTrue(risk.spectralGap() >= 0);
        }

        @Test
        void bottleneckStatesList() {
            var risk = DeadlockChecker.spectralDeadlockRisk(build("&{a: end, b: end}"));
            assertNotNull(risk.bottleneckStates());
        }
    }

    // -----------------------------------------------------------------------
    // Test: compositionalDeadlockCheck
    // -----------------------------------------------------------------------

    @Nested
    class TestCompositionalCheck {

        @Test
        void twoSimpleProtocols() {
            var ss1 = build("&{a: end}");
            var ss2 = build("&{b: end}");
            var result = DeadlockChecker.compositionalDeadlockCheck(ss1, ss2);
            assertTrue(result.isDeadlockFree());
        }

        @Test
        void bothIndividuallyFree() {
            var ss1 = build("&{a: end, b: end}");
            var ss2 = build("&{c: end}");
            var result = DeadlockChecker.compositionalDeadlockCheck(ss1, ss2);
            var individual = result.individualDeadlockFree();
            assertTrue(individual[0]);
            assertTrue(individual[1]);
        }

        @Test
        void oneDeadlockedComponent() {
            var ss1 = makeDeadlockedSs();
            var ss2 = build("&{x: end}");
            var result = DeadlockChecker.compositionalDeadlockCheck(ss1, ss2);
            assertFalse(result.individualDeadlockFree()[0]);
        }

        @Test
        void productStatesCount() {
            var ss1 = build("&{a: end, b: end}");
            var ss2 = build("&{c: end}");
            var result = DeadlockChecker.compositionalDeadlockCheck(ss1, ss2);
            assertTrue(result.isDeadlockFree());
        }

        @Test
        void explanationPresent() {
            var ss1 = build("&{a: end}");
            var ss2 = build("&{b: end}");
            var result = DeadlockChecker.compositionalDeadlockCheck(ss1, ss2);
            assertFalse(result.explanation().isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // Test: bufferDeadlockAnalysis
    // -----------------------------------------------------------------------

    @Nested
    class TestBufferDeadlockAnalysis {

        @Test
        void simpleSynchronousSafe() {
            var ss = build("&{a: end}");
            var result = DeadlockChecker.bufferDeadlockAnalysis(ss, 3);
            assertTrue(result.isSynchronousSafe());
        }

        @Test
        void minCapacityZeroForSimple() {
            var ss = build("&{a: end, b: end}");
            var result = DeadlockChecker.bufferDeadlockAnalysis(ss, 3);
            assertEquals(0, result.minSafeCapacity());
        }

        @Test
        void deadlocksPerCapacityPopulated() {
            var ss = build("&{a: end}");
            var result = DeadlockChecker.bufferDeadlockAnalysis(ss, 3);
            assertTrue(result.deadlocksPerCapacity().containsKey(0));
            assertTrue(result.deadlocksPerCapacity().containsKey(1));
        }

        @Test
        void growthRatePopulated() {
            var ss = build("&{a: end}");
            var result = DeadlockChecker.bufferDeadlockAnalysis(ss, 3);
            assertTrue(Set.of("constant-zero", "constant", "increasing", "decreasing", "non-monotonic")
                    .contains(result.growthRate()));
        }

        @Test
        void explanationPresent() {
            var ss = build("+{ok: end, err: end}");
            var result = DeadlockChecker.bufferDeadlockAnalysis(ss, 3);
            assertFalse(result.explanation().isEmpty());
        }

        @Test
        void deadlockedSsBufferAnalysis() {
            var ss = makeDeadlockedSs();
            var result = DeadlockChecker.bufferDeadlockAnalysis(ss, 3);
            // At capacity 0, should detect the deadlock
            assertEquals(1, (int) result.deadlocksPerCapacity().get(0));
        }
    }

    // -----------------------------------------------------------------------
    // Test: siphonTrapAnalysis
    // -----------------------------------------------------------------------

    @Nested
    class TestSiphonTrapAnalysis {

        @Test
        void simpleBranchSiphonTrap() {
            var ss = build("&{a: end, b: end}");
            var result = DeadlockChecker.siphonTrapAnalysis(ss);
            assertNotNull(result);
        }

        @Test
        void endSiphonTrap() {
            var ss = build("end");
            var result = DeadlockChecker.siphonTrapAnalysis(ss);
            assertTrue(result.isDeadlockFree());
        }

        @Test
        void siphonsAreSets() {
            var ss = build("&{a: end, b: end}");
            var result = DeadlockChecker.siphonTrapAnalysis(ss);
            for (var s : result.siphons()) {
                assertNotNull(s);
                assertTrue(s instanceof Set);
            }
        }

        @Test
        void trapsAreSets() {
            var ss = build("&{a: end, b: end}");
            var result = DeadlockChecker.siphonTrapAnalysis(ss);
            for (var t : result.traps()) {
                assertNotNull(t);
                assertTrue(t instanceof Set);
            }
        }

        @Test
        void iteratorSiphonTrap() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var result = DeadlockChecker.siphonTrapAnalysis(ss);
            assertNotNull(result);
        }

        @Test
        void explanationPresent() {
            var ss = build("&{a: end}");
            var result = DeadlockChecker.siphonTrapAnalysis(ss);
            assertFalse(result.explanation().isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // Test: reticularDeadlockCertificate
    // -----------------------------------------------------------------------

    @Nested
    class TestReticularCertificate {

        @Test
        void endReticulate() {
            var cert = DeadlockChecker.reticularDeadlockCertificate(build("end"));
            assertTrue(cert.isReticulate());
            assertTrue(cert.certificateValid());
        }

        @Test
        void branchReticulate() {
            var cert = DeadlockChecker.reticularDeadlockCertificate(build("&{a: end, b: end}"));
            assertTrue(cert.isReticulate());
            assertTrue(cert.certificateValid());
            assertTrue(cert.explanation().contains("DEADLOCK-FREE"));
        }

        @Test
        void selectionReticulate() {
            var cert = DeadlockChecker.reticularDeadlockCertificate(build("+{ok: end, err: end}"));
            assertTrue(cert.certificateValid());
        }

        @Test
        void classificationsPopulated() {
            var cert = DeadlockChecker.reticularDeadlockCertificate(build("&{a: end, b: end}"));
            assertFalse(cert.stateClassifications().isEmpty());
        }

        @Test
        void nonReticulateHandcrafted() {
            var ss = makeDeadlockedSs();
            var cert = DeadlockChecker.reticularDeadlockCertificate(ss);
            assertNotNull(cert);
        }
    }

    // -----------------------------------------------------------------------
    // Test: analyzeDeadlock (unified)
    // -----------------------------------------------------------------------

    @Nested
    class TestAnalyzeDeadlock {

        @Test
        void endAnalysis() {
            var result = DeadlockChecker.analyzeDeadlock(build("end"));
            assertTrue(result.isDeadlockFree());
            assertTrue(result.summary().contains("DEADLOCK-FREE"));
        }

        @Test
        void branchAnalysis() {
            var result = DeadlockChecker.analyzeDeadlock(build("&{a: end, b: end}"));
            assertTrue(result.isDeadlockFree());
        }

        @Test
        void deadlockedAnalysis() {
            var result = DeadlockChecker.analyzeDeadlock(makeDeadlockedSs());
            assertFalse(result.isDeadlockFree());
            assertTrue(result.summary().contains("DEADLOCK DETECTED"));
        }

        @Test
        void analysisHasAllFields() {
            var result = DeadlockChecker.analyzeDeadlock(build("&{a: end}"));
            assertNotNull(result.latticeCertificate());
            assertNotNull(result.blackHoles());
            assertNotNull(result.spectralRisk());
            assertNotNull(result.siphonTrap());
            assertNotNull(result.reticularCertificate());
        }

        @Test
        void analysisNumStates() {
            var ss = build("&{a: end, b: end}");
            var result = DeadlockChecker.analyzeDeadlock(ss);
            assertEquals(ss.states().size(), result.numStates());
        }

        @Test
        void analysisNumTransitions() {
            var ss = build("&{a: end, b: end}");
            var result = DeadlockChecker.analyzeDeadlock(ss);
            assertEquals(ss.transitions().size(), result.numTransitions());
        }

        @Test
        void iteratorUnified() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var result = DeadlockChecker.analyzeDeadlock(ss);
            assertTrue(result.isDeadlockFree());
        }

        @Test
        void parallelUnified() {
            var ss = build("(&{a: end} || &{b: end})");
            var result = DeadlockChecker.analyzeDeadlock(ss);
            assertTrue(result.isDeadlockFree());
        }

        @Test
        void selectionUnified() {
            var ss = build("+{ok: end, err: end}");
            var result = DeadlockChecker.analyzeDeadlock(ss);
            assertTrue(result.isDeadlockFree());
        }

        @Test
        void blackHoleDetectedInUnified() {
            var ss = makeBlackHoleSs();
            var result = DeadlockChecker.analyzeDeadlock(ss);
            assertTrue(result.blackHoles().totalTrappedStates() > 0);
        }

        @Test
        void multipleCertificates() {
            var result = DeadlockChecker.analyzeDeadlock(build("&{a: end, b: end}"));
            int verdicts = 0;
            if (result.latticeCertificate().certificateValid()) verdicts++;
            if (result.blackHoles().totalTrappedStates() == 0) verdicts++;
            if (result.reticularCertificate().certificateValid()) verdicts++;
            assertTrue(verdicts >= 2);
        }

        @Test
        void deadlockStateDetails() {
            var result = DeadlockChecker.analyzeDeadlock(makeDeadlockedSs());
            assertEquals(1, result.deadlockedStates().size());
            var dl = result.deadlockedStates().get(0);
            assertEquals(1, dl.state());
            assertEquals(1, dl.depthFromTop());
        }
    }

    // -----------------------------------------------------------------------
    // Test: Benchmark protocols (all should be deadlock-free)
    // -----------------------------------------------------------------------

    @Nested
    class TestBenchmarkDeadlockFreedom {

        @ParameterizedTest(name = "{1}")
        @CsvSource(delimiter = ';', value = {
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}; Iterator",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}; File",
                "&{a: end, b: end}; Simple Branch",
                "+{ok: end, err: end}; Simple Selection",
                "(&{a: end} || &{b: end}); Simple Parallel",
                "&{connect: +{OK: rec X . &{query: +{result: X, done: &{disconnect: end}}}, FAIL: end}}; Database",
                "&{login: +{OK: rec X . &{send: X, recv: X, quit: end}, FAIL: end}}; Chat"
        })
        void benchmarkDeadlockFree(String typeStr, String name) {
            var ss = build(typeStr);
            var result = DeadlockChecker.analyzeDeadlock(ss);
            assertTrue(result.isDeadlockFree(),
                    "Protocol " + name + " has deadlock: " + result.summary());
        }
    }

    // -----------------------------------------------------------------------
    // Test: Edge cases
    // -----------------------------------------------------------------------

    @Nested
    class TestEdgeCases {

        @Test
        void singleStateEnd() {
            var ss = build("end");
            assertTrue(DeadlockChecker.isDeadlockFree(ss));
        }

        @Test
        void selfLoopRecursive() {
            var ss = build("rec X . &{loop: X, stop: end}");
            assertTrue(DeadlockChecker.isDeadlockFree(ss));
        }

        @Test
        void deeplyNestedBranch() {
            var ss = build("&{a: &{b: &{c: end}}}");
            assertTrue(DeadlockChecker.isDeadlockFree(ss));
            var result = DeadlockChecker.analyzeDeadlock(ss);
            assertTrue(result.latticeCertificate().certificateValid());
        }

        @Test
        void parallelWithSelection() {
            var ss = build("(+{a: end} || &{b: end})");
            assertTrue(DeadlockChecker.isDeadlockFree(ss));
        }
    }
}
