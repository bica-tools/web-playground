package com.bica.reborn.opcua_mqtt;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * OPC-UA and MQTT industrial safety verification (Step 79).
 *
 * <p>Java port of {@code reticulate/reticulate/opcua_mqtt.py} for BICA Reborn.
 * Models OPC-UA client/server sessions and MQTT publish/subscribe flows
 * as session types, verifies lattice properties and derives IEC 62443
 * industrial-security checks via the bidirectional morphisms phi/psi.
 */
public final class OpcUaMqttChecker {

    private OpcUaMqttChecker() {}

    // -----------------------------------------------------------------------
    // IEC 62443 foundational requirements
    // -----------------------------------------------------------------------

    /** IEC 62443-3-3 Foundational Requirements (FR1 .. FR7). */
    public static final List<String> IEC62443_FRS = List.of(
            "FR1_IdentificationAndAuthenticationControl",
            "FR2_UseControl",
            "FR3_SystemIntegrity",
            "FR4_DataConfidentiality",
            "FR5_RestrictedDataFlow",
            "FR6_TimelyResponseToEvents",
            "FR7_ResourceAvailability");

    // -----------------------------------------------------------------------
    // Records
    // -----------------------------------------------------------------------

    /** A named industrial protocol modelled as a session type. */
    public record IndustrialProtocol(
            String name,
            String sessionTypeString,
            String family,
            String description,
            List<String> iec62443Scope) {
        public IndustrialProtocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(sessionTypeString);
            Objects.requireNonNull(family);
            Objects.requireNonNull(description);
            iec62443Scope = List.copyOf(iec62443Scope);
        }
    }

    /** A concrete safety or security check derived from a lattice state. */
    public record SafetyCheck(int stateId, String fr, String action, String severity) {
        public SafetyCheck {
            Objects.requireNonNull(fr);
            Objects.requireNonNull(action);
            Objects.requireNonNull(severity);
        }
    }

    /** Full analysis result for an industrial protocol. */
    public record IndustrialAnalysisResult(
            IndustrialProtocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            DistributivityResult distributivity,
            int numStates,
            int numTransitions,
            List<SafetyCheck> safetyChecks,
            boolean phiTotal,
            boolean psiTotal,
            boolean galois,
            boolean isWellFormed) {
        public IndustrialAnalysisResult {
            safetyChecks = List.copyOf(safetyChecks);
        }
    }

    // -----------------------------------------------------------------------
    // OPC-UA protocols
    // -----------------------------------------------------------------------

    public static IndustrialProtocol opcuaClientSession() {
        String st = "&{hello: +{ACK: "
                + "&{openSecureChannel: +{OPN_OK: "
                + "&{createSession: +{SESSION_OK: "
                + "&{activateSession: +{ACTIVATED: "
                + "+{read: &{closeSession: end}, "
                + "write: &{closeSession: end}, "
                + "browse: &{closeSession: end}}, "
                + "AUTH_FAIL: end}}, "
                + "SESSION_REJECT: end}}, "
                + "OPN_FAIL: end}}, "
                + "HEL_REJECT: end}}";
        return new IndustrialProtocol(
                "OPCUA_ClientSession",
                st,
                "OPCUA",
                "OPC-UA binary client: HEL/ACK, SecureChannel open, "
                        + "CreateSession, ActivateSession, a single Read/Write/Browse "
                        + "operation, then CloseSession.",
                List.of("FR1_IdentificationAndAuthenticationControl",
                        "FR2_UseControl",
                        "FR3_SystemIntegrity",
                        "FR4_DataConfidentiality"));
    }

    public static IndustrialProtocol opcuaSubscription() {
        String st = "&{activateSession: "
                + "&{createSubscription: +{SUB_OK: "
                + "&{createMonitoredItem: &{publish: "
                + "&{deleteSubscription: &{closeSession: end}}}}, "
                + "SUB_FAIL: &{closeSession: end}}}}";
        return new IndustrialProtocol(
                "OPCUA_Subscription",
                st,
                "OPCUA",
                "OPC-UA subscription lifecycle: create subscription, add "
                        + "monitored item, receive publish notifications, delete "
                        + "subscription, close session.",
                List.of("FR2_UseControl",
                        "FR3_SystemIntegrity",
                        "FR6_TimelyResponseToEvents"));
    }

    public static IndustrialProtocol opcuaSecureChannelRenew() {
        String st = "&{openSecureChannel: +{OPN_OK: "
                + "&{useChannel: &{renewSecureChannel: +{RENEW_OK: "
                + "&{useChannel2: &{closeSecureChannel: end}}, "
                + "RENEW_FAIL: &{closeSecureChannel: end}}}}, "
                + "OPN_FAIL: end}}";
        return new IndustrialProtocol(
                "OPCUA_RenewSecureChannel",
                st,
                "OPCUA",
                "OPC-UA SecureChannel renewal: open, use, renew, use, close. "
                        + "Models certificate/nonce re-keying for long-lived sessions.",
                List.of("FR1_IdentificationAndAuthenticationControl",
                        "FR4_DataConfidentiality"));
    }

    // -----------------------------------------------------------------------
    // MQTT protocols
    // -----------------------------------------------------------------------

    public static IndustrialProtocol mqttSparkplugEdge() {
        String st = "&{connect: +{CONNACK_OK: "
                + "&{nbirth: &{dbirth: "
                + "+{ddata: &{ndeath: end}, "
                + "ncmd: &{ndeath: end}}}}, "
                + "CONNACK_FAIL: end}}";
        return new IndustrialProtocol(
                "MQTT_SparkplugB_Edge",
                st,
                "MQTT",
                "Sparkplug B edge node lifecycle: CONNECT, NBIRTH, DBIRTH, "
                        + "DDATA or NCMD, NDEATH (last will).",
                List.of("FR1_IdentificationAndAuthenticationControl",
                        "FR3_SystemIntegrity",
                        "FR6_TimelyResponseToEvents"));
    }

    public static IndustrialProtocol mqttTlsSecured() {
        String st = "&{tlsHandshake: +{TLS_OK: "
                + "&{connect: +{CONNACK_OK: "
                + "&{subscribe: &{suback: "
                + "&{publish: &{puback: "
                + "&{disconnect: &{tlsClose: end}}}}}}, "
                + "CONNACK_FAIL: &{tlsClose: end}}}, "
                + "TLS_FAIL: end}}";
        return new IndustrialProtocol(
                "MQTT_TLS_Mutual",
                st,
                "MQTT",
                "MQTT over TLS with mutual authentication: TLS handshake, "
                        + "CONNECT with client cert, SUBSCRIBE, PUBLISH with QoS 1, "
                        + "DISCONNECT, TLS close.",
                List.of("FR1_IdentificationAndAuthenticationControl",
                        "FR3_SystemIntegrity",
                        "FR4_DataConfidentiality",
                        "FR5_RestrictedDataFlow"));
    }

    /** Canonical benchmark set for Step 79. */
    public static List<IndustrialProtocol> allIndustrialProtocols() {
        return List.of(
                opcuaClientSession(),
                opcuaSubscription(),
                opcuaSecureChannelRenew(),
                mqttSparkplugEdge(),
                mqttTlsSecured());
    }

    // -----------------------------------------------------------------------
    // phi / psi morphisms
    // -----------------------------------------------------------------------

    /**
     * Keywords in transition labels that map to IEC 62443 FRs.
     * Ordering matters: first matching keyword wins (insertion order).
     */
    private static final Map<String, String> LABEL_TO_FR;
    static {
        LABEL_TO_FR = new LinkedHashMap<>();
        LABEL_TO_FR.put("tls", "FR1_IdentificationAndAuthenticationControl");
        LABEL_TO_FR.put("open", "FR1_IdentificationAndAuthenticationControl");
        LABEL_TO_FR.put("connect", "FR1_IdentificationAndAuthenticationControl");
        LABEL_TO_FR.put("hello", "FR1_IdentificationAndAuthenticationControl");
        LABEL_TO_FR.put("activate", "FR1_IdentificationAndAuthenticationControl");
        LABEL_TO_FR.put("auth", "FR1_IdentificationAndAuthenticationControl");
        LABEL_TO_FR.put("createsession", "FR1_IdentificationAndAuthenticationControl");
        LABEL_TO_FR.put("session", "FR2_UseControl");
        LABEL_TO_FR.put("read", "FR2_UseControl");
        LABEL_TO_FR.put("write", "FR3_SystemIntegrity");
        LABEL_TO_FR.put("browse", "FR2_UseControl");
        LABEL_TO_FR.put("publish", "FR3_SystemIntegrity");
        LABEL_TO_FR.put("subscribe", "FR5_RestrictedDataFlow");
        LABEL_TO_FR.put("ddata", "FR3_SystemIntegrity");
        LABEL_TO_FR.put("nbirth", "FR6_TimelyResponseToEvents");
        LABEL_TO_FR.put("dbirth", "FR6_TimelyResponseToEvents");
        LABEL_TO_FR.put("ndeath", "FR6_TimelyResponseToEvents");
        LABEL_TO_FR.put("ncmd", "FR2_UseControl");
        LABEL_TO_FR.put("renew", "FR4_DataConfidentiality");
        LABEL_TO_FR.put("channel", "FR4_DataConfidentiality");
        LABEL_TO_FR.put("monitored", "FR6_TimelyResponseToEvents");
        LABEL_TO_FR.put("suback", "FR5_RestrictedDataFlow");
        LABEL_TO_FR.put("puback", "FR3_SystemIntegrity");
        LABEL_TO_FR.put("close", "FR7_ResourceAvailability");
        LABEL_TO_FR.put("disconnect", "FR7_ResourceAvailability");
        LABEL_TO_FR.put("delete", "FR7_ResourceAvailability");
    }

    static String labelToFr(String label) {
        String lab = label.toLowerCase(Locale.ROOT);
        for (var e : LABEL_TO_FR.entrySet()) {
            if (lab.contains(e.getKey())) return e.getValue();
        }
        return "FR3_SystemIntegrity";
    }

    static String labelSeverity(String label) {
        String lab = label.toLowerCase(Locale.ROOT);
        if (lab.contains("fail") || lab.contains("reject") || lab.contains("ndeath")) {
            return "critical";
        }
        if (lab.contains("renew") || lab.contains("activate") || lab.contains("open")) {
            return "warn";
        }
        return "info";
    }

    static String stateAction(int stateId, String label) {
        String lab = label.toLowerCase(Locale.ROOT);
        if (lab.contains("fail") || lab.contains("reject")) {
            return "alert_and_abort(state=" + stateId + ", reason=" + label + ")";
        }
        if (lab.contains("close") || lab.contains("disconnect") || lab.contains("death")) {
            return "record_session_end(state=" + stateId + ")";
        }
        if (lab.contains("open") || lab.contains("connect") || lab.contains("tls")) {
            return "log_auth_event(state=" + stateId + ", event=" + label + ")";
        }
        return "update_monitor(state=" + stateId + ", transition=" + label + ")";
    }

    /**
     * phi : L(S) -> SafetyAndSecurityChecks.
     * Emits one entry check at the top, plus one check per transition (keyed on target).
     */
    public static List<SafetyCheck> phiLatticeToChecks(StateSpace ss) {
        List<SafetyCheck> checks = new ArrayList<>();
        checks.add(new SafetyCheck(
                ss.top(),
                "FR1_IdentificationAndAuthenticationControl",
                "log_session_start(state=" + ss.top() + ")",
                "info"));
        for (var t : ss.transitions()) {
            checks.add(new SafetyCheck(
                    t.target(),
                    labelToFr(t.label()),
                    stateAction(t.target(), t.label()),
                    labelSeverity(t.label())));
        }
        return checks;
    }

    /**
     * psi : SafetyAndSecurityChecks -> L(S).
     * Recovers reachable-state subset covered by a bag of checks.
     */
    public static List<Integer> psiChecksToLattice(List<SafetyCheck> checks, StateSpace ss) {
        Set<Integer> reachable = reachableStates(ss);
        Set<Integer> covered = new TreeSet<>();
        for (SafetyCheck c : checks) {
            if (reachable.contains(c.stateId())) covered.add(c.stateId());
        }
        return new ArrayList<>(covered);
    }

    private static Set<Integer> reachableStates(StateSpace ss) {
        return ss.reachableFrom(ss.top());
    }

    public static boolean isPhiTotal(StateSpace ss, List<SafetyCheck> checks) {
        Set<Integer> covered = new java.util.HashSet<>();
        for (SafetyCheck c : checks) covered.add(c.stateId());
        return covered.containsAll(reachableStates(ss));
    }

    public static boolean isPsiTotal(StateSpace ss, List<SafetyCheck> checks) {
        Set<Integer> reachable = reachableStates(ss);
        for (SafetyCheck c : checks) {
            if (!reachable.contains(c.stateId())) return false;
        }
        return true;
    }

    public static boolean isGaloisPair(StateSpace ss) {
        List<SafetyCheck> phi = phiLatticeToChecks(ss);
        Set<Integer> recovered = new java.util.HashSet<>(psiChecksToLattice(phi, ss));
        return recovered.equals(reachableStates(ss));
    }

    // -----------------------------------------------------------------------
    // Verification driver
    // -----------------------------------------------------------------------

    public static IndustrialAnalysisResult verifyIndustrialProtocol(IndustrialProtocol proto) {
        SessionType ast = Parser.parse(proto.sessionTypeString());
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lat = LatticeChecker.checkLattice(ss);
        DistributivityResult dist = DistributivityChecker.checkDistributive(ss);
        List<SafetyCheck> checks = phiLatticeToChecks(ss);
        return new IndustrialAnalysisResult(
                proto,
                ast,
                ss,
                lat,
                dist,
                ss.states().size(),
                ss.transitions().size(),
                checks,
                isPhiTotal(ss, checks),
                isPsiTotal(ss, checks),
                isGaloisPair(ss),
                lat.isLattice());
    }

    public static String formatIndustrialReport(IndustrialAnalysisResult res) {
        StringBuilder sb = new StringBuilder();
        sb.append("Industrial protocol: ").append(res.protocol().name())
                .append(" (").append(res.protocol().family()).append(")\n");
        sb.append("  states: ").append(res.numStates())
                .append(", transitions: ").append(res.numTransitions()).append('\n');
        sb.append("  lattice: ").append(res.isWellFormed())
                .append(", distributive: ").append(res.distributivity().isDistributive()).append('\n');
        sb.append("  phi total: ").append(res.phiTotal())
                .append(", psi total: ").append(res.psiTotal())
                .append(", galois: ").append(res.galois()).append('\n');
        sb.append("  safety checks emitted: ").append(res.safetyChecks().size()).append('\n');
        sb.append("  IEC 62443 FR coverage:\n");
        Set<String> frs = new TreeSet<>();
        for (SafetyCheck c : res.safetyChecks()) frs.add(c.fr());
        for (String fr : frs) {
            long n = res.safetyChecks().stream().filter(c -> c.fr().equals(fr)).count();
            sb.append("    ").append(fr).append(": ").append(n).append('\n');
        }
        return sb.toString();
    }
}
