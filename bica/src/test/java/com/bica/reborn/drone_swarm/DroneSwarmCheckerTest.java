package com.bica.reborn.drone_swarm;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.drone_swarm.DroneSwarmChecker.SwarmAnalysisResult;
import com.bica.reborn.drone_swarm.DroneSwarmChecker.SwarmProtocol;
import com.bica.reborn.drone_swarm.DroneSwarmChecker.SwarmVerdict;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bica.reborn.drone_swarm.DroneSwarmChecker.*;
import static org.junit.jupiter.api.Assertions.*;

/** Java parity tests for DroneSwarmChecker -- port of test_drone_swarm.py. */
class DroneSwarmCheckerTest {

    @Nested
    class ProtocolDefinitions {
        @Test
        void leaderElectionOk() {
            SwarmProtocol p = leaderElection();
            assertEquals("LeaderElection", p.name());
            assertEquals("election", p.scenario());
            assertEquals("SAL2", p.sal());
            assertTrue(p.roles().contains("drone1"));
            assertTrue(p.roles().contains("drone2"));
        }

        @Test
        void formationOk() {
            SwarmProtocol p = formationFlying();
            assertEquals("formation", p.scenario());
            assertEquals("SAL3", p.sal());
            assertTrue(p.roles().contains("leader"));
        }

        @Test
        void taskAllocOk() {
            assertEquals("task_alloc", taskAllocation().scenario());
        }

        @Test
        void collisionOk() {
            assertEquals("SAL4", collisionAvoidance().sal());
        }

        @Test
        void failoverOk() {
            assertTrue(leaderFailover().roles().contains("successor"));
        }

        @Test
        void registrySize() {
            assertEquals(5, allSwarmProtocols().size());
        }

        @Test
        void invalidSal() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SwarmProtocol("x", "election", "SAL9",
                            List.of("a"), "end", "", ""));
        }

        @Test
        void invalidScenario() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SwarmProtocol("x", "bogus", "SAL1",
                            List.of("a"), "end", "", ""));
        }

        @Test
        void invalidVerdict() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SwarmVerdict("WAT", "SAL1", ""));
        }
    }

    @Nested
    class StateSpaceConstruction {
        @Test
        void allParse() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                assertNotNull(swarmToSessionType(p));
            }
        }

        @Test
        void allHaveStates() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                StateSpace ss = StateSpaceBuilder.build(swarmToSessionType(p));
                assertFalse(ss.states().isEmpty());
                assertFalse(ss.transitions().isEmpty());
            }
        }
    }

    @Nested
    class Verification {
        @Test
        void verifyAll() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                SwarmAnalysisResult r = verifySwarmProtocol(p);
                assertNotNull(r);
                assertTrue(r.numStates() > 0);
            }
        }

        @Test
        void analyzeAll() {
            assertEquals(5, analyzeAllSwarms().size());
        }

        @Test
        void allLive() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                assertTrue(verifySwarmProtocol(p).livenessHolds());
            }
        }

        @Test
        void allFaultTolerant() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                assertTrue(verifySwarmProtocol(p).faultTolerant());
            }
        }

        @Test
        void collisionScenarioSafe() {
            assertTrue(verifySwarmProtocol(collisionAvoidance()).noCollision());
        }

        @Test
        void formationFlaggedAbort() {
            // Documented exception: formation flying contains 'abortMission'.
            assertFalse(verifySwarmProtocol(formationFlying()).noCollision());
        }

        @Test
        void allConsensusProgress() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                assertTrue(verifySwarmProtocol(p).consensusProgress());
            }
        }
    }

    @Nested
    class Morphisms {
        @Test
        void phiTotal() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                StateSpace ss = StateSpaceBuilder.build(swarmToSessionType(p));
                Map<Integer, SwarmVerdict> v = phiAllStates(p, ss);
                assertEquals(ss.states().size(), v.size());
                for (SwarmVerdict sv : v.values()) {
                    assertTrue(SWARM_VERDICTS.contains(sv.verdict()));
                }
            }
        }

        @Test
        void psiInverse() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                StateSpace ss = StateSpaceBuilder.build(swarmToSessionType(p));
                Map<Integer, SwarmVerdict> v = phiAllStates(p, ss);
                for (var e : v.entrySet()) {
                    Set<Integer> states = psiVerdictToStates(p, ss, e.getValue().verdict());
                    assertTrue(states.contains(e.getKey()));
                }
            }
        }

        @Test
        void sectionRetraction() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                StateSpace ss = StateSpaceBuilder.build(swarmToSessionType(p));
                Map<Integer, SwarmVerdict> v = phiAllStates(p, ss);
                Set<String> image = new java.util.HashSet<>();
                for (SwarmVerdict sv : v.values()) image.add(sv.verdict());
                for (String verdict : image) {
                    Set<Integer> states = psiVerdictToStates(p, ss, verdict);
                    for (int s : states) {
                        assertEquals(verdict, phiSwarmVerdict(p, ss, s).verdict());
                    }
                }
            }
        }

        @Test
        void psiInvalidVerdictThrows() {
            SwarmProtocol p = leaderElection();
            StateSpace ss = StateSpaceBuilder.build(swarmToSessionType(p));
            assertThrows(IllegalArgumentException.class,
                    () -> psiVerdictToStates(p, ss, "BOGUS"));
        }

        @Test
        void classifyAll() {
            for (SwarmProtocol p : allSwarmProtocols()) {
                StateSpace ss = StateSpaceBuilder.build(swarmToSessionType(p));
                String kind = classifyMorphismPair(p, ss);
                assertTrue(Set.of("isomorphism", "embedding", "projection",
                        "galois", "section-retraction").contains(kind));
            }
        }

        @Test
        void bottomIsIdle() {
            SwarmProtocol p = formationFlying();
            StateSpace ss = StateSpaceBuilder.build(swarmToSessionType(p));
            assertEquals("IDLE", phiSwarmVerdict(p, ss, ss.bottom()).verdict());
        }
    }

    @Nested
    class Reports {
        @Test
        void formatReport() {
            String text = formatSwarmReport(verifySwarmProtocol(formationFlying()));
            assertTrue(text.contains("FormationFlying"));
        }
    }
}
