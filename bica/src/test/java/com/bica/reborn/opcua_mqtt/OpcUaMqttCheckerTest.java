package com.bica.reborn.opcua_mqtt;

import com.bica.reborn.opcua_mqtt.OpcUaMqttChecker.IndustrialAnalysisResult;
import com.bica.reborn.opcua_mqtt.OpcUaMqttChecker.IndustrialProtocol;
import com.bica.reborn.opcua_mqtt.OpcUaMqttChecker.SafetyCheck;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OPC-UA / MQTT industrial protocol verification (Step 79).
 * Java port mirroring {@code reticulate/tests/test_opcua_mqtt.py}.
 */
class OpcUaMqttCheckerTest {

    private static final List<Supplier<IndustrialProtocol>> PROTOCOLS = List.of(
            OpcUaMqttChecker::opcuaClientSession,
            OpcUaMqttChecker::opcuaSubscription,
            OpcUaMqttChecker::opcuaSecureChannelRenew,
            OpcUaMqttChecker::mqttSparkplugEdge,
            OpcUaMqttChecker::mqttTlsSecured);

    private static StateSpace ssOf(IndustrialProtocol p) {
        return StateSpaceBuilder.build(Parser.parse(p.sessionTypeString()));
    }

    // ----- Constructors -----

    @Test
    void iec62443HasSevenFrs() {
        assertEquals(7, OpcUaMqttChecker.IEC62443_FRS.size());
        for (String fr : OpcUaMqttChecker.IEC62443_FRS) {
            assertTrue(fr.startsWith("FR"));
        }
    }

    @Test
    void opcuaClientSessionFields() {
        IndustrialProtocol p = OpcUaMqttChecker.opcuaClientSession();
        assertEquals("OPCUA_ClientSession", p.name());
        assertEquals("OPCUA", p.family());
        assertTrue(p.sessionTypeString().contains("hello"));
        assertTrue(p.sessionTypeString().contains("closeSession"));
    }

    @Test
    void opcuaSubscriptionFields() {
        IndustrialProtocol p = OpcUaMqttChecker.opcuaSubscription();
        assertEquals("OPCUA", p.family());
        assertTrue(p.sessionTypeString().contains("createSubscription"));
    }

    @Test
    void opcuaSecureChannelRenewFields() {
        assertTrue(OpcUaMqttChecker.opcuaSecureChannelRenew()
                .sessionTypeString().contains("renewSecureChannel"));
    }

    @Test
    void mqttSparkplugEdgeFields() {
        IndustrialProtocol p = OpcUaMqttChecker.mqttSparkplugEdge();
        assertEquals("MQTT", p.family());
        assertTrue(p.sessionTypeString().contains("nbirth"));
        assertTrue(p.sessionTypeString().contains("ndeath"));
    }

    @Test
    void mqttTlsSecuredFields() {
        IndustrialProtocol p = OpcUaMqttChecker.mqttTlsSecured();
        assertTrue(p.sessionTypeString().contains("tlsHandshake"));
        assertTrue(p.iec62443Scope().contains("FR4_DataConfidentiality"));
    }

    @Test
    void allIndustrialProtocolsCount() {
        assertEquals(5, OpcUaMqttChecker.allIndustrialProtocols().size());
    }

    @Test
    void allProtocolsParse() {
        for (IndustrialProtocol p : OpcUaMqttChecker.allIndustrialProtocols()) {
            assertNotNull(Parser.parse(p.sessionTypeString()));
        }
    }

    // ----- Lattice properties -----

    @Test
    void allProtocolsAreLattices() {
        for (Supplier<IndustrialProtocol> s : PROTOCOLS) {
            IndustrialAnalysisResult r = OpcUaMqttChecker.verifyIndustrialProtocol(s.get());
            assertTrue(r.isWellFormed(), r.protocol().name());
            assertTrue(r.latticeResult().isLattice(), r.protocol().name());
        }
    }

    @Test
    void sparkplugIsDistributive() {
        IndustrialAnalysisResult r =
                OpcUaMqttChecker.verifyIndustrialProtocol(OpcUaMqttChecker.mqttSparkplugEdge());
        assertTrue(r.distributivity().isDistributive());
    }

    @Test
    void stateCountsPositive() {
        for (IndustrialProtocol p : OpcUaMqttChecker.allIndustrialProtocols()) {
            IndustrialAnalysisResult r = OpcUaMqttChecker.verifyIndustrialProtocol(p);
            assertTrue(r.numStates() >= 3);
            assertTrue(r.numTransitions() >= 2);
        }
    }

    // ----- phi / psi morphisms -----

    @Test
    void phiIsTotalOnAllProtocols() {
        for (IndustrialProtocol p : OpcUaMqttChecker.allIndustrialProtocols()) {
            StateSpace ss = ssOf(p);
            List<SafetyCheck> checks = OpcUaMqttChecker.phiLatticeToChecks(ss);
            assertTrue(OpcUaMqttChecker.isPhiTotal(ss, checks), p.name());
        }
    }

    @Test
    void psiIsTotalOnPhiImage() {
        for (IndustrialProtocol p : OpcUaMqttChecker.allIndustrialProtocols()) {
            StateSpace ss = ssOf(p);
            List<SafetyCheck> checks = OpcUaMqttChecker.phiLatticeToChecks(ss);
            assertTrue(OpcUaMqttChecker.isPsiTotal(ss, checks), p.name());
        }
    }

    @Test
    void psiPhiRecoversReachable() {
        for (IndustrialProtocol p : OpcUaMqttChecker.allIndustrialProtocols()) {
            StateSpace ss = ssOf(p);
            List<SafetyCheck> checks = OpcUaMqttChecker.phiLatticeToChecks(ss);
            Set<Integer> recovered = new HashSet<>(OpcUaMqttChecker.psiChecksToLattice(checks, ss));
            Set<Integer> expected = new HashSet<>();
            expected.add(ss.top());
            for (var t : ss.transitions()) expected.add(t.target());
            assertEquals(expected, recovered, p.name());
        }
    }

    @Test
    void galoisPairHoldsForAll() {
        for (IndustrialProtocol p : OpcUaMqttChecker.allIndustrialProtocols()) {
            StateSpace ss = ssOf(p);
            assertTrue(OpcUaMqttChecker.isGaloisPair(ss), p.name());
        }
    }

    @Test
    void phiMapsTlsToAuthenticationFr() {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse("&{tlsHandshake: end}"));
        List<SafetyCheck> checks = OpcUaMqttChecker.phiLatticeToChecks(ss);
        Set<String> frs = new HashSet<>();
        for (SafetyCheck c : checks) frs.add(c.fr());
        assertTrue(frs.contains("FR1_IdentificationAndAuthenticationControl"));
    }

    @Test
    void phiMapsPublishToIntegrity() {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse("&{publish: end}"));
        List<SafetyCheck> checks = OpcUaMqttChecker.phiLatticeToChecks(ss);
        assertTrue(checks.stream().anyMatch(c -> c.fr().equals("FR3_SystemIntegrity")));
    }

    @Test
    void phiSeverityCriticalOnFailureLabel() {
        StateSpace ss = StateSpaceBuilder.build(
                Parser.parse("&{connect: +{CONNACK_FAIL: end, OK: end}}"));
        List<SafetyCheck> checks = OpcUaMqttChecker.phiLatticeToChecks(ss);
        assertTrue(checks.stream().anyMatch(c -> c.severity().equals("critical")));
    }

    @Test
    void phiSeverityInfoForBenignLabel() {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse("&{read: end}"));
        List<SafetyCheck> checks = OpcUaMqttChecker.phiLatticeToChecks(ss);
        assertTrue(checks.stream().anyMatch(
                c -> c.severity().equals("info") && c.action().contains("read")));
    }

    @Test
    void psiFiltersOutUnreachableStateIds() {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse("&{a: end}"));
        List<SafetyCheck> fake = List.of(new SafetyCheck(
                999, "FR1_IdentificationAndAuthenticationControl", "x", "info"));
        List<Integer> recovered = OpcUaMqttChecker.psiChecksToLattice(fake, ss);
        assertFalse(recovered.contains(999));
    }

    // ----- Verification driver + report -----

    @Test
    void verifyReturnsCompleteResult() {
        IndustrialAnalysisResult r =
                OpcUaMqttChecker.verifyIndustrialProtocol(OpcUaMqttChecker.opcuaClientSession());
        assertEquals("OPCUA_ClientSession", r.protocol().name());
        assertNotNull(r.ast());
        assertEquals(r.stateSpace().states().size(), r.numStates());
        assertFalse(r.safetyChecks().isEmpty());
    }

    @Test
    void formatIndustrialReportMentionsProtocolName() {
        IndustrialAnalysisResult r =
                OpcUaMqttChecker.verifyIndustrialProtocol(OpcUaMqttChecker.opcuaClientSession());
        String s = OpcUaMqttChecker.formatIndustrialReport(r);
        assertTrue(s.contains("OPCUA_ClientSession"));
        assertTrue(s.contains("lattice:"));
        assertTrue(s.contains("phi total"));
    }

    @Test
    void opcuaClientSessionEmitsAuthenticationChecks() {
        IndustrialAnalysisResult r =
                OpcUaMqttChecker.verifyIndustrialProtocol(OpcUaMqttChecker.opcuaClientSession());
        long n = r.safetyChecks().stream()
                .filter(c -> c.fr().equals("FR1_IdentificationAndAuthenticationControl"))
                .count();
        assertTrue(n >= 2);
    }

    @Test
    void sparkplugEmitsTimelyResponseChecks() {
        IndustrialAnalysisResult r =
                OpcUaMqttChecker.verifyIndustrialProtocol(OpcUaMqttChecker.mqttSparkplugEdge());
        long n = r.safetyChecks().stream()
                .filter(c -> c.fr().equals("FR6_TimelyResponseToEvents"))
                .count();
        assertTrue(n >= 1);
    }

    @Test
    void tlsMqttEmitsConfidentialityScope() {
        assertTrue(OpcUaMqttChecker.mqttTlsSecured()
                .iec62443Scope().contains("FR4_DataConfidentiality"));
    }

    @Test
    void phiTotalCountMatchesTransitionsPlusOne() {
        for (IndustrialProtocol p : OpcUaMqttChecker.allIndustrialProtocols()) {
            StateSpace ss = ssOf(p);
            List<SafetyCheck> checks = OpcUaMqttChecker.phiLatticeToChecks(ss);
            assertEquals(ss.transitions().size() + 1, checks.size(), p.name());
        }
    }

    @Test
    void verificationResultGaloisFlag() {
        for (IndustrialProtocol p : OpcUaMqttChecker.allIndustrialProtocols()) {
            IndustrialAnalysisResult r = OpcUaMqttChecker.verifyIndustrialProtocol(p);
            assertTrue(r.galois(), p.name());
            assertTrue(r.phiTotal(), p.name());
            assertTrue(r.psiTotal(), p.name());
        }
    }
}
