package com.bica.reborn.ccsds;

import com.bica.reborn.ccsds.CcsdsChecker.CcsdsAnalysisResult;
import com.bica.reborn.ccsds.CcsdsChecker.CcsdsProtocol;
import com.bica.reborn.ccsds.CcsdsChecker.LinkMode;
import com.bica.reborn.statespace.StateSpace;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.bica.reborn.ccsds.CcsdsChecker.*;
import static org.junit.jupiter.api.Assertions.*;

/** Java parity tests for CcsdsChecker — port of test_ccsds.py (Step 802). */
class CcsdsCheckerTest {

    @Nested
    class ProtocolDefinitions {
        @Test
        void tcCop1Structure() {
            CcsdsProtocol p = tcCop1FopFarm();
            assertEquals("TcCop1FopFarm", p.name());
            assertEquals("COP1", p.layer());
            assertTrue(p.roles().contains("ground_mcc"));
            assertTrue(p.roles().contains("spacecraft"));
        }

        @Test
        void tcSpaceDataLink() {
            assertEquals("TC", CcsdsChecker.tcSpaceDataLink().layer());
        }

        @Test
        void tmDownlink() {
            CcsdsProtocol p = tmDownlinkVirtualChannel();
            assertEquals("TM", p.layer());
            assertTrue(p.sessionTypeString().contains("rsEncode"));
        }

        @Test
        void aosUsesParallel() {
            CcsdsProtocol p = aosMpduBpduParallel();
            assertEquals("AOS", p.layer());
            assertTrue(p.sessionTypeString().contains("||"));
        }

        @Test
        void cfdpClass1() {
            assertEquals("CFDP", cfdpClass1Put().layer());
        }

        @Test
        void cfdpClass2() {
            CcsdsProtocol p = cfdpClass2Put();
            assertEquals("CFDP", p.layer());
            assertTrue(p.sessionTypeString().toLowerCase().contains("retransmit"));
        }

        @Test
        void blackout() {
            CcsdsProtocol p = blackoutAutonomousWindow();
            assertEquals("RF", p.layer());
            assertTrue(p.sessionTypeString().contains("enterBlackout"));
        }

        @Test
        void rfAcquire() {
            assertEquals("RF", rfAcquireLock().layer());
        }

        @Test
        void invalidLayerRejected() {
            assertThrows(IllegalArgumentException.class, () -> new CcsdsProtocol(
                    "X", "BOGUS", List.of("ground_mcc"), "end", "x", "x"));
        }

        @Test
        void registriesNonempty() {
            assertTrue(allTcProtocols().size() >= 2);
            assertTrue(allTmProtocols().size() >= 1);
            assertTrue(allAosProtocols().size() >= 1);
            assertTrue(allCfdpProtocols().size() >= 2);
            assertTrue(allRfProtocols().size() >= 2);
            int total = allTcProtocols().size() + allTmProtocols().size()
                    + allAosProtocols().size() + allCfdpProtocols().size()
                    + allRfProtocols().size();
            assertEquals(total, allCcsdsProtocols().size());
        }
    }

    @Nested
    class Verification {
        @Test
        void verifyAll() {
            for (CcsdsProtocol p : allCcsdsProtocols()) {
                CcsdsAnalysisResult r = verifyCcsdsProtocol(p);
                assertNotNull(r);
                assertTrue(r.numStates() >= 2);
                assertTrue(r.numTransitions() >= 1);
            }
        }

        @Test
        void allFormLattices() {
            for (CcsdsAnalysisResult r : analyzeAllCcsds()) {
                assertTrue(r.isWellFormed(), r.protocol().name() + " is not a lattice");
            }
        }

        @Test
        void noDeadlocks() {
            for (CcsdsAnalysisResult r : analyzeAllCcsds()) {
                assertTrue(r.deadlockStates().isEmpty(),
                        r.protocol().name() + " has deadlocks");
            }
        }
    }

    @Nested
    class PhiPsi {
        @Test
        void phiBottomTerminated() {
            CcsdsProtocol p = tcCop1FopFarm();
            StateSpace ss = verifyCcsdsProtocol(p).stateSpace();
            assertEquals("TERMINATED", phiLinkMode(p, ss, ss.bottom()).kind());
        }

        @Test
        void phiTopIdle() {
            CcsdsProtocol p = tcCop1FopFarm();
            StateSpace ss = verifyCcsdsProtocol(p).stateSpace();
            assertEquals("IDLE", phiLinkMode(p, ss, ss.top()).kind());
        }

        @Test
        void phiOnlyValidModes() {
            for (CcsdsProtocol p : allCcsdsProtocols()) {
                StateSpace ss = verifyCcsdsProtocol(p).stateSpace();
                for (int s : ss.states()) {
                    LinkMode m = phiLinkMode(p, ss, s);
                    assertTrue(LINK_MODES.contains(m.kind()),
                            "Unknown mode " + m.kind() + " in " + p.name());
                }
            }
        }

        @Test
        void psiSectionProperty() {
            CcsdsProtocol p = cfdpClass2Put();
            StateSpace ss = verifyCcsdsProtocol(p).stateSpace();
            for (String kind : LINK_MODES) {
                Set<Integer> states = psiModeToStates(p, ss, kind);
                for (int s : states) {
                    assertEquals(kind, phiLinkMode(p, ss, s).kind());
                }
            }
        }

        @Test
        void retransmitInCop1() {
            CcsdsProtocol p = tcCop1FopFarm();
            StateSpace ss = verifyCcsdsProtocol(p).stateSpace();
            assertFalse(psiModeToStates(p, ss, "RETRANSMIT").isEmpty());
        }

        @Test
        void blackoutInBlackoutProtocol() {
            CcsdsProtocol p = blackoutAutonomousWindow();
            StateSpace ss = verifyCcsdsProtocol(p).stateSpace();
            assertFalse(psiModeToStates(p, ss, "BLACKOUT").isEmpty());
        }

        @Test
        void fileTransferInCfdp() {
            CcsdsProtocol p = cfdpClass2Put();
            StateSpace ss = verifyCcsdsProtocol(p).stateSpace();
            assertFalse(psiModeToStates(p, ss, "FILE_TRANSFER").isEmpty());
        }

        @Test
        void classifyMorphismValid() {
            for (CcsdsProtocol p : allCcsdsProtocols()) {
                StateSpace ss = verifyCcsdsProtocol(p).stateSpace();
                String kind = classifyMorphismPair(p, ss);
                assertTrue(Set.of("isomorphism", "embedding", "projection",
                        "galois", "section-retraction").contains(kind));
            }
        }
    }

    @Nested
    class AosProduct {
        @Test
        void aosHasSixteenStates() {
            CcsdsProtocol p = aosMpduBpduParallel();
            CcsdsAnalysisResult r = verifyCcsdsProtocol(p);
            assertEquals(16, r.numStates());
        }
    }

    @Nested
    class Roles {
        @Test
        void rolesRecognised() {
            for (CcsdsProtocol p : allCcsdsProtocols()) {
                for (String r : p.roles()) {
                    assertTrue(CCSDS_ROLES.contains(r), "Unknown role " + r);
                }
            }
        }
    }
}
