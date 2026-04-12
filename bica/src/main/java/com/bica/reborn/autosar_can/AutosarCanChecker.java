package com.bica.reborn.autosar_can;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.subtyping.SubtypingChecker;
import com.bica.reborn.subtyping.SubtypingResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * AUTOSAR/CAN protocol certification via lattice analysis (Step 76).
 *
 * <p>Java port of {@code reticulate/reticulate/autosar_can.py}. Models AUTOSAR
 * runnable communication, CAN bus message flows, and UDS diagnostic services
 * as session types, then uses lattice properties to support ISO 26262
 * functional safety certification.
 *
 * <p>Provides bidirectional morphisms
 * {@code phi : L(S) -> SafetyClaims} and {@code psi : SafetyClaims -> L(S)}
 * for the traceability axis of the ISO 26262 V-model.
 */
public final class AutosarCanChecker {

    private AutosarCanChecker() {}

    // -----------------------------------------------------------------------
    // Domain data types
    // -----------------------------------------------------------------------

    /** ISO 26262 ASIL levels (Automotive Safety Integrity Level). */
    public static final List<String> ASIL_LEVELS = List.of("QM", "A", "B", "C", "D");

    private static final Set<String> VALID_LAYERS = Set.of("RTE", "CAN", "UDS");

    /** An AUTOSAR / CAN protocol modelled as a session type. */
    public record AutosarProtocol(
            String name,
            String layer,
            String asil,
            List<String> ecus,
            String sessionTypeString,
            String specReference,
            String description) {

        public AutosarProtocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(layer);
            Objects.requireNonNull(asil);
            Objects.requireNonNull(ecus);
            Objects.requireNonNull(sessionTypeString);
            Objects.requireNonNull(specReference);
            Objects.requireNonNull(description);
            if (!ASIL_LEVELS.contains(asil)) {
                throw new IllegalArgumentException(
                        "Invalid ASIL level " + asil + "; expected one of " + ASIL_LEVELS);
            }
            if (!VALID_LAYERS.contains(layer)) {
                throw new IllegalArgumentException(
                        "Invalid layer " + layer + "; expected RTE, CAN, or UDS.");
            }
            ecus = List.copyOf(ecus);
        }
    }

    /** An ISO 26262 safety claim about a protocol state. */
    public record SafetyClaim(String kind, String asil, String rationale) {
        public SafetyClaim {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(asil);
            Objects.requireNonNull(rationale);
        }
    }

    /** Complete analysis result for an AUTOSAR / CAN protocol. */
    public record AutosarAnalysisResult(
            AutosarProtocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            DistributivityResult distributivity,
            int numStates,
            int numTransitions,
            boolean isWellFormed,
            Map<Integer, SafetyClaim> safetyClaims) {

        public AutosarAnalysisResult {
            Objects.requireNonNull(protocol);
            Objects.requireNonNull(ast);
            Objects.requireNonNull(stateSpace);
            Objects.requireNonNull(latticeResult);
            Objects.requireNonNull(distributivity);
            Objects.requireNonNull(safetyClaims);
            safetyClaims = Map.copyOf(safetyClaims);
        }
    }

    // -----------------------------------------------------------------------
    // AUTOSAR runnable protocol definitions
    // -----------------------------------------------------------------------

    public static AutosarProtocol runnablePeriodic() {
        return new AutosarProtocol(
                "Runnable_Periodic",
                "RTE",
                "B",
                List.of("SWC_Sensor", "SWC_Controller"),
                "&{rteRead: +{OK: &{compute: &{rteWrite: end}}, "
                        + "UNINIT: &{defaultValue: &{rteWrite: end}}}}",
                "AUTOSAR RTE R22-11 Section 4.3.1",
                "Periodic runnable: Rte_Read sensor value, compute actuation, "
                        + "Rte_Write to controller port. Handles uninitialised input.");
    }

    public static AutosarProtocol runnableClientServer() {
        return new AutosarProtocol(
                "Runnable_ClientServer",
                "RTE",
                "C",
                List.of("SWC_Client", "SWC_Server"),
                "&{rteCall: +{OK: &{serverExec: +{RESULT: end, "
                        + "APP_ERROR: &{errorHook: end}}}, "
                        + "E_TIMEOUT: &{errorHook: end}}}",
                "AUTOSAR RTE R22-11 Section 4.3.2",
                "Client-server runnable: synchronous Rte_Call to a server "
                        + "runnable. Handles timeout and application errors.");
    }

    public static AutosarProtocol runnableEventTriggered() {
        return new AutosarProtocol(
                "Runnable_EventTriggered",
                "RTE",
                "A",
                List.of("SWC_Event"),
                "&{eventArrival: &{rteReceive: +{DATA: &{processEvent: end}, "
                        + "NO_DATA: end}}}",
                "AUTOSAR RTE R22-11 Section 4.3.3",
                "Event-triggered runnable: activated on event arrival, "
                        + "receives data and processes it.");
    }

    public static AutosarProtocol runnableModeSwitch() {
        return new AutosarProtocol(
                "Runnable_ModeSwitch",
                "RTE",
                "D",
                List.of("SWC_ModeManager"),
                "&{modeRequest: +{TRANSITION_OK: "
                        + "&{onExit: &{onEntry: &{rteSwitch: end}}}, "
                        + "TRANSITION_DENIED: &{safeState: end}}}",
                "AUTOSAR RTE R22-11 Section 4.4",
                "Mode-switch runnable for ASIL-D functions. Exit/entry "
                        + "sequencing with safe-state fallback on denial.");
    }

    // -----------------------------------------------------------------------
    // CAN bus protocol definitions
    // -----------------------------------------------------------------------

    public static AutosarProtocol canFrameExchange() {
        return new AutosarProtocol(
                "CAN_FrameExchange",
                "CAN",
                "B",
                List.of("ECU_Tx", "ECU_Rx"),
                "&{arbitration: +{WON: "
                        + "&{transmitFrame: +{ACK: end, "
                        + "NO_ACK: &{errorFrame: end}}}, "
                        + "LOST: &{backoff: end}}}",
                "ISO 11898-1:2015 Section 10",
                "Classical CAN frame exchange: bus arbitration, frame "
                        + "transmission, ACK slot reception, and error frame on NAK.");
    }

    public static AutosarProtocol canFdExchange() {
        return new AutosarProtocol(
                "CAN_FD_FrameExchange",
                "CAN",
                "C",
                List.of("ECU_Tx", "ECU_Rx"),
                "&{arbitration: +{WON: "
                        + "&{switchBRS: &{transmitFrame: "
                        + "+{ACK: &{crcCheck: +{CRC_OK: end, "
                        + "CRC_FAIL: &{errorFrame: end}}}, "
                        + "NO_ACK: &{errorFrame: end}}}}, "
                        + "LOST: &{backoff: end}}}",
                "ISO 11898-1:2015 Section 11 (CAN-FD)",
                "CAN-FD frame: arbitration, bit-rate switch (BRS), data "
                        + "transmission, ACK slot, CRC-17/21 verification, error "
                        + "handling on mismatch.");
    }

    public static AutosarProtocol canTxConfirmation() {
        return new AutosarProtocol(
                "CAN_TxConfirmation",
                "CAN",
                "B",
                List.of("Com", "PduR", "CanIf"),
                "&{comSend: &{pduRTransmit: &{canIfTransmit: "
                        + "+{TX_OK: &{txConfirmation: end}, "
                        + "TX_FAIL: &{errorNotify: end}}}}}",
                "AUTOSAR Com R22-11 Section 7.3",
                "AUTOSAR Com Tx confirmation chain: Com -> PduR -> CanIf "
                        + "-> hardware, with confirmation propagation and error "
                        + "notification on failure.");
    }

    public static AutosarProtocol canNetworkManagement() {
        return new AutosarProtocol(
                "CAN_NM",
                "CAN",
                "A",
                List.of("ECU_A", "ECU_B"),
                "&{networkStart: &{repeatMessageState: "
                        + "+{STAY_AWAKE: &{normalOperation: &{prepareSleep: &{busSleep: end}}}, "
                        + "NO_TRAFFIC: &{busSleep: end}}}}",
                "AUTOSAR CanNm R22-11",
                "CAN Network Management state machine: repeat-message -> "
                        + "normal operation -> prepare sleep -> bus sleep.");
    }

    // -----------------------------------------------------------------------
    // UDS diagnostic protocol (ISO 14229)
    // -----------------------------------------------------------------------

    public static AutosarProtocol udsDiagnosticSession() {
        return new AutosarProtocol(
                "UDS_DiagnosticSession",
                "UDS",
                "QM",
                List.of("Tester", "ECU"),
                "&{diagSessionControl: +{POSITIVE: "
                        + "&{securityAccess: +{UNLOCKED: "
                        + "&{readDataByIdentifier: &{testerPresent: end}}, "
                        + "LOCKED: &{negativeResponse: end}}}, "
                        + "NEGATIVE: end}}",
                "ISO 14229-1:2020 Services 0x10, 0x27, 0x22, 0x3E",
                "UDS diagnostic session: tester requests session, performs "
                        + "security access, reads data identifiers, keeps session "
                        + "alive with TesterPresent.");
    }

    // -----------------------------------------------------------------------
    // Registries
    // -----------------------------------------------------------------------

    public static List<AutosarProtocol> allRunnables() {
        return List.of(
                runnablePeriodic(),
                runnableClientServer(),
                runnableEventTriggered(),
                runnableModeSwitch());
    }

    public static List<AutosarProtocol> allCanProtocols() {
        return List.of(
                canFrameExchange(),
                canFdExchange(),
                canTxConfirmation(),
                canNetworkManagement());
    }

    public static List<AutosarProtocol> allUdsProtocols() {
        return List.of(udsDiagnosticSession());
    }

    public static List<AutosarProtocol> allAutosarProtocols() {
        List<AutosarProtocol> all = new ArrayList<>();
        all.addAll(allRunnables());
        all.addAll(allCanProtocols());
        all.addAll(allUdsProtocols());
        return List.copyOf(all);
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphisms phi : L(S) -> SafetyClaims and psi back
    // -----------------------------------------------------------------------

    private record KeywordRule(List<String> keywords, String kind) {}

    private static final List<KeywordRule> CLAIM_KEYWORDS = List.of(
            new KeywordRule(List.of("safeState", "busSleep", "prepareSleep"), "SAFE_STATE"),
            new KeywordRule(List.of("errorFrame", "errorHook", "errorNotify", "negativeResponse"),
                    "FAULT_DETECTED"),
            new KeywordRule(List.of("txConfirmation", "rteWrite", "crcCheck", "crcOk", "CRC_OK"),
                    "FFI"),
            new KeywordRule(List.of("defaultValue", "backoff", "repeatMessageState"),
                    "FAIL_OPERATIONAL"),
            new KeywordRule(List.of("modeRequest", "onExit", "onEntry", "rteSwitch"),
                    "ASIL_DECOMPOSITION"),
            new KeywordRule(List.of("securityAccess", "testerPresent"), "HAZARD_CONTAINED"),
            new KeywordRule(List.of("arbitration", "transmitFrame", "switchBRS"), "FTTI"));

    /** phi : L(S) -> SafetyClaims (state-indexed). */
    public static SafetyClaim phiSafetyClaim(AutosarProtocol protocol, StateSpace ss, int state) {
        if (state == ss.bottom()) {
            return new SafetyClaim(
                    "SAFE_STATE",
                    protocol.asil(),
                    "State " + state + " is the lattice bottom; protocol has "
                            + "terminated and the ECU may enter its declared safe state.");
        }
        if (state == ss.top()) {
            return new SafetyClaim(
                    "FFI",
                    protocol.asil(),
                    "State " + state + " is the lattice top; all downstream "
                            + "partitions are isolated by the RTE (freedom from "
                            + "interference precondition).");
        }

        List<String> incident = new ArrayList<>();
        for (var t : ss.transitions()) {
            if (t.source() == state || t.target() == state) {
                incident.add(t.label());
            }
        }

        for (KeywordRule rule : CLAIM_KEYWORDS) {
            for (String kw : rule.keywords()) {
                for (String lbl : incident) {
                    if (lbl.contains(kw)) {
                        return new SafetyClaim(
                                rule.kind(),
                                protocol.asil(),
                                "State " + state + " witnesses claim " + rule.kind()
                                        + " via label containing '" + kw + "'.");
                    }
                }
            }
        }

        return new SafetyClaim(
                "NONE",
                protocol.asil(),
                "State " + state + " has no dominant ISO 26262 claim.");
    }

    /** Apply phi to every state of the state space. */
    public static Map<Integer, SafetyClaim> phiAllStates(AutosarProtocol protocol, StateSpace ss) {
        Map<Integer, SafetyClaim> result = new TreeMap<>();
        for (int s : ss.states()) {
            result.put(s, phiSafetyClaim(protocol, ss, s));
        }
        return result;
    }

    /** psi : SafetyClaims -> L(S). */
    public static Set<Integer> psiClaimToStates(
            AutosarProtocol protocol, StateSpace ss, String claimKind) {
        Map<Integer, SafetyClaim> claims = phiAllStates(protocol, ss);
        Set<Integer> result = new HashSet<>();
        for (var entry : claims.entrySet()) {
            if (entry.getValue().kind().equals(claimKind)) {
                result.add(entry.getKey());
            }
        }
        return Set.copyOf(result);
    }

    /** Classify the pair (phi, psi). */
    public static String classifyMorphismPair(AutosarProtocol protocol, StateSpace ss) {
        Map<Integer, SafetyClaim> claims = phiAllStates(protocol, ss);
        Set<String> kinds = new HashSet<>();
        for (SafetyClaim c : claims.values()) {
            kinds.add(c.kind());
        }
        if (kinds.size() == ss.states().size()) {
            return "isomorphism";
        }
        if (kinds.size() < ss.states().size()) {
            for (String kind : kinds) {
                Set<Integer> states = psiClaimToStates(protocol, ss, kind);
                for (int s : states) {
                    if (!phiSafetyClaim(protocol, ss, s).kind().equals(kind)) {
                        return "galois";
                    }
                }
            }
            return "section-retraction";
        }
        return "projection";
    }

    // -----------------------------------------------------------------------
    // Core verification pipeline
    // -----------------------------------------------------------------------

    /** Parse the protocol's session-type string. */
    public static SessionType autosarToSessionType(AutosarProtocol protocol) {
        return Parser.parse(protocol.sessionTypeString());
    }

    /** Run the full verification + certification pipeline. */
    public static AutosarAnalysisResult verifyAutosarProtocol(AutosarProtocol protocol) {
        SessionType ast = autosarToSessionType(protocol);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        DistributivityResult dist = DistributivityChecker.checkDistributive(ss);
        Map<Integer, SafetyClaim> claims = phiAllStates(protocol, ss);

        return new AutosarAnalysisResult(
                protocol, ast, ss, lr, dist,
                ss.states().size(),
                ss.transitions().size(),
                lr.isLattice(),
                claims);
    }

    /** Verify every pre-defined AUTOSAR / CAN / UDS protocol. */
    public static List<AutosarAnalysisResult> analyzeAllAutosar() {
        List<AutosarAnalysisResult> out = new ArrayList<>();
        for (AutosarProtocol p : allAutosarProtocols()) {
            out.add(verifyAutosarProtocol(p));
        }
        return List.copyOf(out);
    }

    /** Check ASIL decomposition: parent <= child_a (Gay-Hole subtyping). */
    public static SubtypingResult checkAsilDecomposition(
            AutosarProtocol parent, AutosarProtocol childA, AutosarProtocol childB) {
        return SubtypingChecker.check(
                autosarToSessionType(parent),
                autosarToSessionType(childA));
    }

    // -----------------------------------------------------------------------
    // Report formatting
    // -----------------------------------------------------------------------

    public static String formatAutosarReport(AutosarAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        AutosarProtocol proto = result.protocol();
        String bar = "=".repeat(72);
        sb.append(bar).append('\n');
        sb.append("  AUTOSAR / CAN PROTOCOL REPORT: ").append(proto.name()).append('\n');
        sb.append(bar).append('\n').append('\n');
        sb.append("  ").append(proto.description()).append('\n').append('\n');
        sb.append("--- Protocol Details ---\n");
        sb.append("  Layer: ").append(proto.layer()).append('\n');
        sb.append("  ASIL:  ").append(proto.asil()).append('\n');
        sb.append("  Spec:  ").append(proto.specReference()).append('\n');
        for (String e : proto.ecus()) {
            sb.append("  ECU:   ").append(e).append('\n');
        }
        sb.append('\n');
        sb.append("--- Session Type ---\n");
        sb.append("  ").append(proto.sessionTypeString()).append('\n').append('\n');
        sb.append("--- State Space ---\n");
        sb.append("  States:      ").append(result.numStates()).append('\n');
        sb.append("  Transitions: ").append(result.numTransitions()).append('\n');
        sb.append("  Top:         ").append(result.stateSpace().top()).append('\n');
        sb.append("  Bottom:      ").append(result.stateSpace().bottom()).append('\n').append('\n');
        sb.append("--- Lattice Analysis ---\n");
        sb.append("  Is lattice:   ").append(result.latticeResult().isLattice()).append('\n');
        sb.append("  Distributive: ").append(result.distributivity().isDistributive()).append('\n').append('\n');
        sb.append("--- Safety Claims (phi) ---\n");
        Map<String, Integer> counts = new TreeMap<>();
        for (SafetyClaim c : result.safetyClaims().values()) {
            counts.merge(c.kind(), 1, Integer::sum);
        }
        for (var e : counts.entrySet()) {
            sb.append(String.format("  %-22s: %d state(s)%n", e.getKey(), e.getValue()));
        }
        sb.append('\n');
        sb.append("--- Certification Verdict ---\n");
        if (result.isWellFormed()) {
            sb.append("  PASS: lattice well-formed; ASIL ").append(proto.asil())
                    .append(" claims traceable.\n");
        } else {
            sb.append("  FAIL: protocol is not a lattice; ISO 26262 traceability broken.\n");
        }
        sb.append('\n');
        return sb.toString();
    }

    public static String formatAutosarSummary(List<AutosarAnalysisResult> results) {
        StringBuilder sb = new StringBuilder();
        String bar = "=".repeat(82);
        sb.append(bar).append('\n');
        sb.append("  AUTOSAR / CAN / UDS CERTIFICATION SUMMARY\n");
        sb.append(bar).append('\n').append('\n');
        sb.append(String.format("  %-28s %-6s %-5s %6s %6s %8s %6s%n",
                "Protocol", "Layer", "ASIL", "States", "Trans", "Lattice", "Dist"));
        sb.append("  ").append("-".repeat(78)).append('\n');
        for (AutosarAnalysisResult r : results) {
            sb.append(String.format("  %-28s %-6s %-5s %6d %6d %8s %6s%n",
                    r.protocol().name(),
                    r.protocol().layer(),
                    r.protocol().asil(),
                    r.numStates(),
                    r.numTransitions(),
                    r.isWellFormed() ? "YES" : "NO",
                    r.distributivity().isDistributive() ? "YES" : "NO"));
        }
        sb.append('\n');
        boolean allLattice = results.stream().allMatch(AutosarAnalysisResult::isWellFormed);
        sb.append("  All protocols form lattices: ").append(allLattice ? "YES" : "NO").append('\n');
        return sb.toString();
    }
}
