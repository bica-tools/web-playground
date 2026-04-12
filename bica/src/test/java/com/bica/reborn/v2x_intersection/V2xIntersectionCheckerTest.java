package com.bica.reborn.v2x_intersection;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.v2x_intersection.V2xIntersectionChecker.RowVerdict;
import com.bica.reborn.v2x_intersection.V2xIntersectionChecker.V2xAnalysisResult;
import com.bica.reborn.v2x_intersection.V2xIntersectionChecker.V2xProtocol;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bica.reborn.v2x_intersection.V2xIntersectionChecker.*;
import static org.junit.jupiter.api.Assertions.*;

/** Java parity tests for V2xIntersectionChecker — port of test_v2x_intersection.py. */
class V2xIntersectionCheckerTest {

    @Nested
    class ProtocolDefinitions {
        @Test
        void fourWayStopOk() {
            V2xProtocol p = fourWayStop();
            assertEquals("FourWayStop", p.name());
            assertEquals("stop_sign", p.scenario());
            assertEquals("CAL2", p.cal());
            assertTrue(p.roles().contains("ego"));
            assertTrue(p.roles().contains("other"));
        }

        @Test
        void signalizedOk() {
            V2xProtocol p = signalizedIntersection();
            assertEquals("signalized", p.scenario());
            assertTrue(p.roles().contains("signal"));
        }

        @Test
        void leftTurnOk() {
            V2xProtocol p = unprotectedLeftTurn();
            assertEquals("left_turn", p.scenario());
            assertEquals("CAL3", p.cal());
        }

        @Test
        void emergencyOk() {
            V2xProtocol p = emergencyVehiclePreemption();
            assertEquals("CAL4", p.cal());
            assertTrue(p.roles().contains("emergency"));
        }

        @Test
        void pedestrianOk() {
            assertTrue(pedestrianCrossing().roles().contains("pedestrian"));
        }

        @Test
        void registrySize() {
            assertEquals(5, allV2xProtocols().size());
        }

        @Test
        void invalidCal() {
            assertThrows(IllegalArgumentException.class, () -> new V2xProtocol(
                    "X", "stop_sign", "CAL9", List.of("ego"), "end", "", ""));
        }

        @Test
        void invalidScenario() {
            assertThrows(IllegalArgumentException.class, () -> new V2xProtocol(
                    "X", "bogus", "CAL1", List.of("ego"), "end", "", ""));
        }
    }

    @Nested
    class Parsing {
        @Test
        void allParse() {
            for (V2xProtocol p : allV2xProtocols()) {
                assertNotNull(v2xToSessionType(p));
                StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
                assertFalse(ss.states().isEmpty());
            }
        }

        @Test
        void fourWayStopStateCount() {
            V2xProtocol p = fourWayStop();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            assertTrue(ss.states().size() >= 10);
        }
    }

    @Nested
    class Morphisms {
        @Test
        void phiBottomClear() {
            V2xProtocol p = fourWayStop();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            assertEquals("CLEAR", phiRowVerdict(p, ss, ss.bottom()).verdict());
        }

        @Test
        void phiTopWait() {
            V2xProtocol p = fourWayStop();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            assertEquals("WAIT", phiRowVerdict(p, ss, ss.top()).verdict());
        }

        @Test
        void phiAllStatesCovered() {
            V2xProtocol p = signalizedIntersection();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            Map<Integer, RowVerdict> v = phiAllStates(p, ss);
            assertEquals(ss.states().size(), v.size());
        }

        @Test
        void psiInverse() {
            V2xProtocol p = fourWayStop();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            for (String verdict : ROW_VERDICTS) {
                Set<Integer> states = psiVerdictToStates(p, ss, verdict);
                for (int s : states) {
                    assertEquals(verdict, phiRowVerdict(p, ss, s).verdict());
                }
            }
        }

        @Test
        void psiInvalidVerdict() {
            V2xProtocol p = fourWayStop();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            assertThrows(IllegalArgumentException.class,
                    () -> psiVerdictToStates(p, ss, "BOGUS"));
        }

        @Test
        void classify() {
            V2xProtocol p = fourWayStop();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            String c = classifyMorphismPair(p, ss);
            assertTrue(Set.of("isomorphism", "embedding", "projection",
                    "galois", "section-retraction").contains(c));
        }

        @Test
        void sectionRetractionProperty() {
            V2xProtocol p = signalizedIntersection();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            Map<Integer, RowVerdict> verdicts = phiAllStates(p, ss);
            Set<String> image = new java.util.HashSet<>();
            for (RowVerdict v : verdicts.values()) image.add(v.verdict());
            for (String k : image) {
                Set<Integer> states = psiVerdictToStates(p, ss, k);
                for (int s : states) {
                    assertEquals(k, phiRowVerdict(p, ss, s).verdict());
                }
            }
        }
    }

    @Nested
    class Safety {
        @Test
        void collisionFreeStopSign() {
            V2xProtocol p = fourWayStop();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            assertTrue(checkCollisionFree(p, ss));
        }

        @Test
        void fairnessStopSign() {
            V2xProtocol p = fourWayStop();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            assertTrue(checkFairness(p, ss));
        }

        @Test
        void livenessAll() {
            for (V2xProtocol p : allV2xProtocols()) {
                StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
                assertTrue(checkLiveness(p, ss));
            }
        }

        @Test
        void emergencyAllowed() {
            V2xProtocol p = emergencyVehiclePreemption();
            StateSpace ss = StateSpaceBuilder.build(v2xToSessionType(p));
            assertTrue(checkCollisionFree(p, ss));
        }
    }

    @Nested
    class Pipeline {
        @Test
        void verifyReturnsResult() {
            V2xProtocol p = fourWayStop();
            V2xAnalysisResult r = verifyV2xProtocol(p);
            assertNotNull(r);
            assertTrue(r.numStates() > 0);
            assertTrue(r.numTransitions() > 0);
        }

        @Test
        void analyzeAll() {
            List<V2xAnalysisResult> rs = analyzeAllV2x();
            assertEquals(allV2xProtocols().size(), rs.size());
            for (V2xAnalysisResult r : rs) {
                assertTrue(r.livenessHolds());
            }
        }

        @Test
        void reportFormat() {
            V2xAnalysisResult r = verifyV2xProtocol(fourWayStop());
            String text = formatV2xReport(r);
            assertTrue(text.contains("FourWayStop"));
            assertTrue(text.contains("ROW Verdicts"));
        }
    }

    @Nested
    class RowVerdictTests {
        @Test
        void invalidVerdict() {
            assertThrows(IllegalArgumentException.class,
                    () -> new RowVerdict("BOGUS", "CAL1", "x"));
        }

        @Test
        void validVerdicts() {
            for (String v : ROW_VERDICTS) {
                assertEquals(v, new RowVerdict(v, "CAL1", "ok").verdict());
            }
        }
    }

    @Nested
    class Constants {
        @Test
        void calLevels() {
            assertTrue(CAL_LEVELS.contains("CAL1"));
            assertTrue(CAL_LEVELS.contains("CAL4"));
        }

        @Test
        void rowVerdicts() {
            assertTrue(ROW_VERDICTS.contains("CLEAR"));
            assertTrue(ROW_VERDICTS.contains("EMERGENCY_BRAKE"));
            assertEquals(5, ROW_VERDICTS.size());
        }
    }
}
