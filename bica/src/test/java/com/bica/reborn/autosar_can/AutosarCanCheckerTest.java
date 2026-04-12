package com.bica.reborn.autosar_can;

import com.bica.reborn.autosar_can.AutosarCanChecker.AutosarAnalysisResult;
import com.bica.reborn.autosar_can.AutosarCanChecker.AutosarProtocol;
import com.bica.reborn.autosar_can.AutosarCanChecker.SafetyClaim;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.bica.reborn.autosar_can.AutosarCanChecker.*;
import static org.junit.jupiter.api.Assertions.*;

/** Java parity tests for AutosarCanChecker — port of test_autosar_can.py. */
class AutosarCanCheckerTest {

    @Nested
    class RunnableDefinitions {
        @Test
        void periodicStructure() {
            AutosarProtocol p = runnablePeriodic();
            assertEquals("Runnable_Periodic", p.name());
            assertEquals("RTE", p.layer());
            assertEquals("B", p.asil());
            assertTrue(p.ecus().contains("SWC_Sensor"));
        }

        @Test
        void clientServer() {
            AutosarProtocol p = runnableClientServer();
            assertEquals("RTE", p.layer());
            assertEquals("C", p.asil());
        }

        @Test
        void eventTriggered() {
            assertEquals("A", runnableEventTriggered().asil());
        }

        @Test
        void modeSwitch() {
            assertEquals("D", runnableModeSwitch().asil());
        }

        @Test
        void allParse() {
            for (AutosarProtocol p : allRunnables()) {
                assertNotNull(autosarToSessionType(p));
            }
        }
    }

    @Nested
    class CanDefinitions {
        @Test
        void frameExchange() {
            AutosarProtocol p = canFrameExchange();
            assertEquals("CAN", p.layer());
            assertTrue(p.specReference().contains("ISO 11898"));
        }

        @Test
        void canFd() {
            AutosarProtocol p = canFdExchange();
            assertTrue(p.specReference().contains("CAN-FD") || p.specReference().contains("11898"));
        }

        @Test
        void txConfirmation() {
            assertTrue(canTxConfirmation().ecus().contains("Com"));
        }

        @Test
        void networkManagement() {
            assertEquals("A", canNetworkManagement().asil());
        }

        @Test
        void allParse() {
            for (AutosarProtocol p : allCanProtocols()) {
                assertNotNull(autosarToSessionType(p));
            }
        }
    }

    @Nested
    class Uds {
        @Test
        void diagnosticSession() {
            AutosarProtocol p = udsDiagnosticSession();
            assertEquals("UDS", p.layer());
            assertEquals("QM", p.asil());
        }

        @Test
        void parses() {
            for (AutosarProtocol p : allUdsProtocols()) {
                assertNotNull(autosarToSessionType(p));
            }
        }
    }

    @Nested
    class Validation {
        @Test
        void invalidAsil() {
            assertThrows(IllegalArgumentException.class, () ->
                    new AutosarProtocol("X", "CAN", "Z", List.of(), "end", "", ""));
        }

        @Test
        void invalidLayer() {
            assertThrows(IllegalArgumentException.class, () ->
                    new AutosarProtocol("X", "BOGUS", "A", List.of(), "end", "", ""));
        }

        @Test
        void asilLevels() {
            assertEquals(List.of("QM", "A", "B", "C", "D"), ASIL_LEVELS);
        }
    }

    @Nested
    class Verification {
        @Test
        void verifyPeriodic() {
            AutosarAnalysisResult r = verifyAutosarProtocol(runnablePeriodic());
            assertNotNull(r);
            assertTrue(r.isWellFormed());
            assertTrue(r.numStates() > 0);
            assertTrue(r.numTransitions() > 0);
        }

        @Test
        void verifyCanFrame() {
            assertTrue(verifyAutosarProtocol(canFrameExchange()).isWellFormed());
        }

        @Test
        void verifyAll() {
            List<AutosarAnalysisResult> results = analyzeAllAutosar();
            assertEquals(allAutosarProtocols().size(), results.size());
            for (AutosarAnalysisResult r : results) {
                assertTrue(r.isWellFormed(), r.protocol().name() + " is not a lattice");
            }
        }

        @Test
        void safetyClaimsPopulated() {
            AutosarAnalysisResult r = verifyAutosarProtocol(canFrameExchange());
            assertEquals(r.numStates(), r.safetyClaims().size());
            for (SafetyClaim c : r.safetyClaims().values()) {
                assertNotNull(c);
            }
        }
    }

    @Nested
    class Morphisms {
        @Test
        void phiBottomIsSafeState() {
            AutosarProtocol p = runnablePeriodic();
            StateSpace ss = StateSpaceBuilder.build(autosarToSessionType(p));
            assertEquals("SAFE_STATE", phiSafetyClaim(p, ss, ss.bottom()).kind());
        }

        @Test
        void phiTopIsFfi() {
            AutosarProtocol p = runnablePeriodic();
            StateSpace ss = StateSpaceBuilder.build(autosarToSessionType(p));
            assertEquals("FFI", phiSafetyClaim(p, ss, ss.top()).kind());
        }

        @Test
        void phiAllStatesComplete() {
            AutosarProtocol p = canFrameExchange();
            StateSpace ss = StateSpaceBuilder.build(autosarToSessionType(p));
            assertEquals(ss.states(), phiAllStates(p, ss).keySet());
        }

        @Test
        void psiPreimageCorrect() {
            AutosarProtocol p = canFrameExchange();
            StateSpace ss = StateSpaceBuilder.build(autosarToSessionType(p));
            var claims = phiAllStates(p, ss);
            Set<String> kinds = new java.util.HashSet<>();
            for (SafetyClaim c : claims.values()) kinds.add(c.kind());
            for (String kind : kinds) {
                Set<Integer> preimage = psiClaimToStates(p, ss, kind);
                for (int s : preimage) {
                    assertEquals(kind, phiSafetyClaim(p, ss, s).kind());
                }
            }
        }

        @Test
        void psiUnknownClaimIsEmpty() {
            AutosarProtocol p = runnablePeriodic();
            StateSpace ss = StateSpaceBuilder.build(autosarToSessionType(p));
            assertEquals(Set.of(), psiClaimToStates(p, ss, "NOT_A_CLAIM"));
        }

        @Test
        void classifyMorphism() {
            Set<String> valid = Set.of(
                    "isomorphism", "embedding", "projection", "galois", "section-retraction");
            for (AutosarProtocol p : allAutosarProtocols()) {
                StateSpace ss = StateSpaceBuilder.build(autosarToSessionType(p));
                assertTrue(valid.contains(classifyMorphismPair(p, ss)));
            }
        }

        @Test
        void phiPsiIdempotentOnImage() {
            for (AutosarProtocol p : allAutosarProtocols()) {
                StateSpace ss = StateSpaceBuilder.build(autosarToSessionType(p));
                for (int s : ss.states()) {
                    String k1 = phiSafetyClaim(p, ss, s).kind();
                    Set<Integer> preimage = psiClaimToStates(p, ss, k1);
                    assertTrue(preimage.contains(s));
                }
            }
        }
    }

    @Nested
    class AsilDecomposition {
        @Test
        void decompositionRuns() {
            assertNotNull(checkAsilDecomposition(
                    runnableModeSwitch(), runnablePeriodic(), runnableEventTriggered()));
        }
    }

    @Nested
    class Formatting {
        @Test
        void formatReport() {
            String text = formatAutosarReport(verifyAutosarProtocol(canFrameExchange()));
            assertTrue(text.contains("CAN_FrameExchange"));
            assertTrue(text.contains("ASIL"));
        }

        @Test
        void formatSummary() {
            String text = formatAutosarSummary(analyzeAllAutosar());
            assertTrue(text.contains("SUMMARY"));
            assertTrue(text.contains("CAN_FrameExchange"));
        }
    }
}
