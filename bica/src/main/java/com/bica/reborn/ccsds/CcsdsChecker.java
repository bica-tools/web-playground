package com.bica.reborn.ccsds;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * CCSDS deep-space telecommand/telemetry as session-type lattices (Step 802).
 *
 * <p>Java port of {@code reticulate/reticulate/ccsds.py}. Models eight canonical
 * CCSDS protocols (TC SDLP, COP-1 FOP/FARM, TM SDLP, AOS M_PDU/B_PDU parallel,
 * CFDP class 1/2, RF acquisition, blackout autonomous window) as session types
 * and provides bidirectional supervisor morphisms phi : L(S) -> LinkSupervisor
 * and psi : LinkSupervisor -> L(S).
 */
public final class CcsdsChecker {

    private CcsdsChecker() {}

    public static final List<String> LINK_MODES = List.of(
            "IDLE", "ACQUIRING", "LOCKED", "COMMANDING", "RETRANSMIT",
            "DOWNLINKING", "BLACKOUT", "SAFE_HOLD", "FILE_TRANSFER", "TERMINATED");

    public static final List<String> CCSDS_ROLES = List.of(
            "ground_mcc", "spacecraft", "dsn_station", "cfdp_entity");

    private static final Set<String> VALID_LAYERS = Set.of(
            "TC", "TM", "AOS", "CFDP", "COP1", "RF");

    /** A CCSDS deep-space protocol modelled as a session type. */
    public record CcsdsProtocol(
            String name,
            String layer,
            List<String> roles,
            String sessionTypeString,
            String specReference,
            String description) {

        public CcsdsProtocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(layer);
            Objects.requireNonNull(roles);
            Objects.requireNonNull(sessionTypeString);
            Objects.requireNonNull(specReference);
            Objects.requireNonNull(description);
            if (!VALID_LAYERS.contains(layer)) {
                throw new IllegalArgumentException(
                        "Invalid layer " + layer + "; expected one of " + VALID_LAYERS);
            }
            roles = List.copyOf(roles);
        }
    }

    /** A deep-space link supervisor mode (codomain of phi). */
    public record LinkMode(String kind, String rationale) {
        public LinkMode {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(rationale);
        }
    }

    /** Complete analysis result for a CCSDS protocol. */
    public record CcsdsAnalysisResult(
            CcsdsProtocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            DistributivityResult distributivity,
            int numStates,
            int numTransitions,
            boolean isWellFormed,
            Map<Integer, LinkMode> linkModes,
            List<Integer> deadlockStates) {

        public CcsdsAnalysisResult {
            Objects.requireNonNull(protocol);
            Objects.requireNonNull(ast);
            Objects.requireNonNull(stateSpace);
            Objects.requireNonNull(latticeResult);
            Objects.requireNonNull(distributivity);
            Objects.requireNonNull(linkModes);
            Objects.requireNonNull(deadlockStates);
            linkModes = Map.copyOf(linkModes);
            deadlockStates = List.copyOf(deadlockStates);
        }
    }

    // -----------------------------------------------------------------------
    // Protocol definitions
    // -----------------------------------------------------------------------

    public static CcsdsProtocol tcCop1FopFarm() {
        return new CcsdsProtocol(
                "TcCop1FopFarm",
                "COP1",
                List.of("ground_mcc", "spacecraft"),
                "&{sendBdFrame: &{awaitClcw: "
                        + "+{ACCEPTED: &{advanceWindow: end}, "
                        + "REJECTED: &{retransmit: +{ACCEPTED: &{advanceWindow: end}, "
                        + "LOCKOUT: &{sendBcUnlock: &{awaitClcw: "
                        + "+{UNLOCKED: &{advanceWindow: end}, "
                        + "TIMEOUT: &{abortFop: end}}}}}}, "
                        + "TIMEOUT: &{abortFop: end}}}}",
                "CCSDS 232.0-B-3 Annex C (COP-1, FOP-1/FARM-1)",
                "COP-1 FOP/FARM retransmission: send BD, await CLCW, retransmit, BC unlock, abort.");
    }

    public static CcsdsProtocol tcSpaceDataLink() {
        return new CcsdsProtocol(
                "TcSpaceDataLink",
                "TC",
                List.of("ground_mcc", "spacecraft"),
                "&{formTcFrame: &{bchEncode: &{modulateUplink: "
                        + "+{CARRIER_LOCKED: &{transmit: &{awaitClcw: "
                        + "+{ACK: &{frameAccepted: end}, "
                        + "NAK: &{frameRejected: end}}}}, "
                        + "NO_LOCK: &{linkLossAbort: end}}}}}",
                "CCSDS 232.0-B-3 / 232.1-B-2",
                "TC SDLP uplink: form, BCH encode, modulate, await CLCW.");
    }

    public static CcsdsProtocol tmDownlinkVirtualChannel() {
        return new CcsdsProtocol(
                "TmDownlinkVirtualChannel",
                "TM",
                List.of("spacecraft", "dsn_station", "ground_mcc"),
                "&{fillTmFrame: &{rsEncode: &{attachSyncMarker: "
                        + "&{modulateDownlink: +{ACQUIRED: "
                        + "&{demodulate: &{rsDecode: +{CRC_OK: "
                        + "&{extractVcid: &{deliverPayload: end}}, "
                        + "CRC_BAD: &{frameDiscard: end}}}}, "
                        + "NO_SIGNAL: &{linkLossAbort: end}}}}}}",
                "CCSDS 132.0-B-2 / 131.0-B-3",
                "TM downlink: fill, RS encode, sync marker, modulate, RS decode.");
    }

    public static CcsdsProtocol aosMpduBpduParallel() {
        return new CcsdsProtocol(
                "AosMpduBpduParallel",
                "AOS",
                List.of("spacecraft", "dsn_station"),
                "(&{mpduPacketize: &{mpduInsertHeader: "
                        + "&{mpduDownlink: end}}} "
                        + "|| &{bpduChunk: &{bpduInsertHeader: "
                        + "&{bpduDownlink: end}}})",
                "CCSDS 732.0-B-3 (AOS SDLP)",
                "AOS parallel: M_PDU and B_PDU; product 4*4 = 16 states.");
    }

    public static CcsdsProtocol cfdpClass2Put() {
        return new CcsdsProtocol(
                "CfdpClass2Put",
                "CFDP",
                List.of("ground_mcc", "cfdp_entity", "spacecraft"),
                "&{sendMetadata: &{sendFileData: &{sendEof: &{awaitFinished: "
                        + "+{COMPLETE: &{closeTransaction: end}, "
                        + "NAK_LIST: &{retransmitData: &{awaitFinished: "
                        + "+{COMPLETE: &{closeTransaction: end}, "
                        + "TIMEOUT: &{abortCfdp: end}}}}}}}}}",
                "CCSDS 727.0-B-5 (CFDP Class 2)",
                "CFDP class 2 acknowledged put with retransmission.");
    }

    public static CcsdsProtocol cfdpClass1Put() {
        return new CcsdsProtocol(
                "CfdpClass1Put",
                "CFDP",
                List.of("ground_mcc", "cfdp_entity"),
                "&{sendMetadata: &{sendFileData: "
                        + "&{sendEof: &{closeTransaction: end}}}}",
                "CCSDS 727.0-B-5 (CFDP Class 1)",
                "CFDP class 1 unacknowledged put.");
    }

    public static CcsdsProtocol blackoutAutonomousWindow() {
        return new CcsdsProtocol(
                "BlackoutAutonomousWindow",
                "RF",
                List.of("spacecraft"),
                "&{enterBlackout: &{runHousekeeping: "
                        + "+{NOMINAL: &{collectScience: &{awaitNextPass: "
                        + "+{REACQUIRED: &{resyncCop1: end}, "
                        + "MISSED: &{enterSafeHold: end}}}}, "
                        + "ANOMALY: &{enterSafeHold: end}}}}",
                "Mars-class operations: solar-conjunction blackout",
                "Autonomous blackout window with safe-hold escapes.");
    }

    public static CcsdsProtocol rfAcquireLock() {
        return new CcsdsProtocol(
                "RfAcquireLock",
                "RF",
                List.of("dsn_station", "spacecraft"),
                "&{searchCarrier: +{CARRIER_LOCKED: "
                        + "&{symbolSync: +{SYMBOL_LOCKED: "
                        + "&{frameSync: +{FRAME_LOCKED: &{linkUp: end}, "
                        + "NO_FRAME_SYNC: &{acquireFail: end}}}, "
                        + "NO_SYMBOL_SYNC: &{acquireFail: end}}}, "
                        + "NO_CARRIER: &{acquireFail: end}}}",
                "CCSDS 131.0-B-3 / 232.1-B-2 (acquisition)",
                "RF acquisition: carrier, symbol, frame sync.");
    }

    public static List<CcsdsProtocol> allTcProtocols() {
        return List.of(tcCop1FopFarm(), tcSpaceDataLink());
    }

    public static List<CcsdsProtocol> allTmProtocols() {
        return List.of(tmDownlinkVirtualChannel());
    }

    public static List<CcsdsProtocol> allAosProtocols() {
        return List.of(aosMpduBpduParallel());
    }

    public static List<CcsdsProtocol> allCfdpProtocols() {
        return List.of(cfdpClass1Put(), cfdpClass2Put());
    }

    public static List<CcsdsProtocol> allRfProtocols() {
        return List.of(rfAcquireLock(), blackoutAutonomousWindow());
    }

    public static List<CcsdsProtocol> allCcsdsProtocols() {
        List<CcsdsProtocol> all = new ArrayList<>();
        all.addAll(allTcProtocols());
        all.addAll(allTmProtocols());
        all.addAll(allAosProtocols());
        all.addAll(allCfdpProtocols());
        all.addAll(allRfProtocols());
        return List.copyOf(all);
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphisms phi / psi
    // -----------------------------------------------------------------------

    private record KeywordRule(List<String> keywords, String kind) {}

    private static final List<KeywordRule> MODE_KEYWORDS = List.of(
            new KeywordRule(
                    List.of("abortFop", "abortCfdp", "linkLossAbort", "acquireFail",
                            "frameDiscard", "frameRejected"),
                    "SAFE_HOLD"),
            new KeywordRule(
                    List.of("retransmit", "sendBcUnlock"),
                    "RETRANSMIT"),
            new KeywordRule(
                    List.of("enterBlackout", "runHousekeeping", "collectScience",
                            "awaitNextPass", "enterSafeHold"),
                    "BLACKOUT"),
            new KeywordRule(
                    List.of("sendMetadata", "sendFileData", "sendEof", "awaitFinished",
                            "closeTransaction", "retransmitData"),
                    "FILE_TRANSFER"),
            new KeywordRule(
                    List.of("rsEncode", "rsDecode", "attachSyncMarker", "modulateDownlink",
                            "demodulate", "extractVcid", "deliverPayload",
                            "fillTmFrame", "mpduPacketize", "mpduInsertHeader",
                            "mpduDownlink", "bpduChunk", "bpduInsertHeader", "bpduDownlink"),
                    "DOWNLINKING"),
            new KeywordRule(
                    List.of("sendBdFrame", "awaitClcw", "advanceWindow",
                            "formTcFrame", "bchEncode", "modulateUplink", "transmit",
                            "frameAccepted"),
                    "COMMANDING"),
            new KeywordRule(
                    List.of("searchCarrier", "symbolSync", "frameSync", "linkUp"),
                    "ACQUIRING"),
            new KeywordRule(
                    List.of("resyncCop1"),
                    "LOCKED"));

    /** phi : L(S) -> LinkSupervisor (state-indexed). */
    public static LinkMode phiLinkMode(
            CcsdsProtocol protocol, StateSpace ss, int state) {
        if (state == ss.bottom()) {
            return new LinkMode(
                    "TERMINATED",
                    "State " + state + " is the lattice bottom; session terminated.");
        }
        if (state == ss.top()) {
            return new LinkMode(
                    "IDLE",
                    "State " + state + " is the lattice top; no command issued yet.");
        }

        List<String> incident = new ArrayList<>();
        for (var t : ss.transitions()) {
            if (t.source() == state || t.target() == state) {
                incident.add(t.label());
            }
        }

        for (KeywordRule rule : MODE_KEYWORDS) {
            for (String kw : rule.keywords()) {
                for (String lbl : incident) {
                    if (lbl.contains(kw)) {
                        return new LinkMode(
                                rule.kind(),
                                "State " + state + " mapped to " + rule.kind()
                                        + " via label containing '" + kw + "'.");
                    }
                }
            }
        }

        return new LinkMode(
                "LOCKED",
                "State " + state + " has no distinguishing label; default LOCKED.");
    }

    public static Map<Integer, LinkMode> phiAllStates(
            CcsdsProtocol protocol, StateSpace ss) {
        Map<Integer, LinkMode> result = new TreeMap<>();
        for (int s : ss.states()) {
            result.put(s, phiLinkMode(protocol, ss, s));
        }
        return result;
    }

    /** psi : LinkSupervisor -> L(S). */
    public static Set<Integer> psiModeToStates(
            CcsdsProtocol protocol, StateSpace ss, String modeKind) {
        Map<Integer, LinkMode> modes = phiAllStates(protocol, ss);
        Set<Integer> result = new HashSet<>();
        for (var entry : modes.entrySet()) {
            if (entry.getValue().kind().equals(modeKind)) {
                result.add(entry.getKey());
            }
        }
        return Set.copyOf(result);
    }

    public static String classifyMorphismPair(CcsdsProtocol protocol, StateSpace ss) {
        Map<Integer, LinkMode> modes = phiAllStates(protocol, ss);
        Set<String> kinds = new HashSet<>();
        for (LinkMode m : modes.values()) {
            kinds.add(m.kind());
        }
        if (kinds.size() == ss.states().size()) {
            return "isomorphism";
        }
        if (kinds.size() < ss.states().size()) {
            for (String kind : kinds) {
                Set<Integer> states = psiModeToStates(protocol, ss, kind);
                for (int s : states) {
                    if (!phiLinkMode(protocol, ss, s).kind().equals(kind)) {
                        return "galois";
                    }
                }
            }
            return "section-retraction";
        }
        return "projection";
    }

    // -----------------------------------------------------------------------
    // Deadlock detection
    // -----------------------------------------------------------------------

    public static List<Integer> detectDeadlocks(StateSpace ss) {
        Set<Integer> hasOutgoing = new HashSet<>();
        for (var t : ss.transitions()) {
            hasOutgoing.add(t.source());
        }
        TreeSet<Integer> result = new TreeSet<>();
        for (int s : ss.states()) {
            if (!hasOutgoing.contains(s) && s != ss.bottom()) {
                result.add(s);
            }
        }
        return List.copyOf(result);
    }

    // -----------------------------------------------------------------------
    // Core verification pipeline
    // -----------------------------------------------------------------------

    public static SessionType ccsdsToSessionType(CcsdsProtocol protocol) {
        return Parser.parse(protocol.sessionTypeString());
    }

    public static CcsdsAnalysisResult verifyCcsdsProtocol(CcsdsProtocol protocol) {
        SessionType ast = ccsdsToSessionType(protocol);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        DistributivityResult dist = DistributivityChecker.checkDistributive(ss);
        Map<Integer, LinkMode> modes = phiAllStates(protocol, ss);
        List<Integer> deadlocks = detectDeadlocks(ss);
        return new CcsdsAnalysisResult(
                protocol, ast, ss, lr, dist,
                ss.states().size(),
                ss.transitions().size(),
                lr.isLattice(),
                modes,
                deadlocks);
    }

    public static List<CcsdsAnalysisResult> analyzeAllCcsds() {
        List<CcsdsAnalysisResult> out = new ArrayList<>();
        for (CcsdsProtocol p : allCcsdsProtocols()) {
            out.add(verifyCcsdsProtocol(p));
        }
        return List.copyOf(out);
    }
}
