package com.bica.reborn.v2x_intersection;

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

/**
 * V2X Intersection Right-of-Way protocol verification (Step 804).
 *
 * <p>Java port of {@code reticulate/reticulate/v2x_intersection.py}. Models
 * vehicle-to-everything intersection coordination using SAE J2735 BSM and
 * SPaT/MAP messages as multiparty session types, with right-of-way captured
 * as a section-retraction (phi, psi) onto a five-element supervisor lattice.
 */
public final class V2xIntersectionChecker {

    private V2xIntersectionChecker() {}

    // -----------------------------------------------------------------------
    // Domain data types
    // -----------------------------------------------------------------------

    /** ISO/SAE 21434 Cybersecurity Assurance Levels. */
    public static final List<String> CAL_LEVELS = List.of("CAL1", "CAL2", "CAL3", "CAL4");

    /** Right-of-way verdicts (the supervisor lattice). */
    public static final List<String> ROW_VERDICTS = List.of(
            "CLEAR", "EGO_GO", "OTHER_GO", "WAIT", "EMERGENCY_BRAKE");

    private static final Set<String> VALID_SCENARIOS = Set.of("stop_sign", "signalized", "left_turn");

    /** A V2X intersection protocol modelled as a session type. */
    public record V2xProtocol(
            String name,
            String scenario,
            String cal,
            List<String> roles,
            String sessionTypeString,
            String specReference,
            String description) {

        public V2xProtocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(scenario);
            Objects.requireNonNull(cal);
            Objects.requireNonNull(roles);
            Objects.requireNonNull(sessionTypeString);
            Objects.requireNonNull(specReference);
            Objects.requireNonNull(description);
            if (!CAL_LEVELS.contains(cal)) {
                throw new IllegalArgumentException(
                        "Invalid CAL level " + cal + "; expected one of " + CAL_LEVELS);
            }
            if (!VALID_SCENARIOS.contains(scenario)) {
                throw new IllegalArgumentException(
                        "Invalid scenario " + scenario
                                + "; expected stop_sign, signalized, or left_turn.");
            }
            roles = List.copyOf(roles);
        }
    }

    /** A right-of-way verdict assigned to a joint protocol state. */
    public record RowVerdict(String verdict, String cal, String rationale) {
        public RowVerdict {
            Objects.requireNonNull(verdict);
            Objects.requireNonNull(cal);
            Objects.requireNonNull(rationale);
            if (!ROW_VERDICTS.contains(verdict)) {
                throw new IllegalArgumentException(
                        "Invalid verdict " + verdict + "; expected one of " + ROW_VERDICTS);
            }
        }
    }

    /** Complete analysis result for a V2X intersection protocol. */
    public record V2xAnalysisResult(
            V2xProtocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            DistributivityResult distributivity,
            int numStates,
            int numTransitions,
            boolean isWellFormed,
            Map<Integer, RowVerdict> rowVerdicts,
            boolean collisionFree,
            boolean fairnessHolds,
            boolean livenessHolds) {

        public V2xAnalysisResult {
            Objects.requireNonNull(protocol);
            Objects.requireNonNull(ast);
            Objects.requireNonNull(stateSpace);
            Objects.requireNonNull(latticeResult);
            Objects.requireNonNull(distributivity);
            Objects.requireNonNull(rowVerdicts);
            rowVerdicts = Map.copyOf(rowVerdicts);
        }
    }

    // -----------------------------------------------------------------------
    // V2X protocol definitions
    // -----------------------------------------------------------------------

    public static V2xProtocol fourWayStop() {
        return new V2xProtocol(
                "FourWayStop",
                "stop_sign",
                "CAL2",
                List.of("ego", "other"),
                "( &{approach: &{bsmTx: &{stopLine: +{firstArrival: "
                        + "&{proceed: end}, yieldTurn: &{waitTurn: &{proceed: end}}}}}} "
                        + "|| "
                        + "&{approach: &{bsmTx: &{stopLine: +{firstArrival: "
                        + "&{proceed: end}, yieldTurn: &{waitTurn: &{proceed: end}}}}}} )",
                "SAE J2735_202211 BSM, MUTCD 2B.05",
                "Four-way stop intersection with two vehicles. Each vehicle "
                        + "approaches, broadcasts a BSM, stops at the line, and either "
                        + "proceeds first or yields. FIFO right-of-way arbitration.");
    }

    public static V2xProtocol signalizedIntersection() {
        return new V2xProtocol(
                "SignalizedIntersection",
                "signalized",
                "CAL3",
                List.of("ego", "rsu", "signal"),
                "( &{approach: &{bsmTx: &{spatRx: +{GREEN: &{proceed: end}, "
                        + "YELLOW: &{decelerate: &{stopAtLine: end}}, "
                        + "RED: &{stopAtLine: &{waitGreen: &{proceed: end}}}}}}} "
                        + "|| "
                        + "&{phaseRed: &{phaseGreen: &{phaseYellow: &{phaseRed2: end}}}} )",
                "SAE J2735_202211 SPaT/MAP, ISO 19091:2017",
                "Signalized intersection with RSU broadcasting SPaT/MAP. Ego "
                        + "vehicle reads phase, proceeds on green, decelerates on yellow, "
                        + "stops on red. Parallel signal phase rotation.");
    }

    public static V2xProtocol unprotectedLeftTurn() {
        return new V2xProtocol(
                "UnprotectedLeftTurn",
                "left_turn",
                "CAL3",
                List.of("ego", "oncoming"),
                "( &{approach: &{bsmTx: &{enterIntersection: "
                        + "+{gapDetected: &{commitTurn: &{clearIntersection: end}}, "
                        + "noGap: &{yieldOncoming: &{waitGap: &{commitTurn: "
                        + "&{clearIntersection: end}}}}}}}} "
                        + "|| "
                        + "&{oncomingApproach: &{oncomingPass: end}} )",
                "SAE J3161/1, NHTSA LTAP/OD CIB",
                "Unprotected left turn against oncoming traffic. Ego yields "
                        + "until a safe gap, then commits. Concurrent oncoming vehicle "
                        + "session models the gap closure.");
    }

    public static V2xProtocol emergencyVehiclePreemption() {
        return new V2xProtocol(
                "EmergencyPreemption",
                "signalized",
                "CAL4",
                List.of("ego", "emergency", "rsu"),
                "( &{approach: &{sirenDetected: &{pullOver: &{stop: "
                        + "&{waitClear: &{resume: end}}}}}} "
                        + "|| "
                        + "&{preemptRequest: &{rsuGrant: &{greenWave: "
                        + "&{passIntersection: end}}}} )",
                "SAE J2735 EVP, NTCIP 1211",
                "Emergency vehicle preemption: emergency requests green wave "
                        + "from RSU; ego vehicle pulls over and waits.");
    }

    public static V2xProtocol pedestrianCrossing() {
        return new V2xProtocol(
                "PedestrianCrossing",
                "signalized",
                "CAL3",
                List.of("ego", "pedestrian"),
                "( &{approach: &{psmRx: +{pedDetected: "
                        + "&{decelerate: &{stopAtCrosswalk: &{waitClear: &{proceed: end}}}}, "
                        + "noPed: &{proceed: end}}}} "
                        + "|| "
                        + "&{stepOffCurb: &{cross: &{reachOtherSide: end}}} )",
                "SAE J2945/9 (PSM), SAE J3186",
                "Pedestrian-aware crossing using SAE J2945/9 PSM. Ego detects "
                        + "pedestrian via PSM and yields at crosswalk.");
    }

    public static List<V2xProtocol> allV2xProtocols() {
        return List.of(
                fourWayStop(),
                signalizedIntersection(),
                unprotectedLeftTurn(),
                emergencyVehiclePreemption(),
                pedestrianCrossing());
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphisms phi and psi
    // -----------------------------------------------------------------------

    private record KeywordRule(List<String> keywords, String verdict) {}

    private static final List<KeywordRule> VERDICT_KEYWORDS = List.of(
            new KeywordRule(List.of("emergency", "siren", "pullOver"), "EMERGENCY_BRAKE"),
            new KeywordRule(List.of("waitTurn", "waitGreen", "yieldTurn", "yieldOncoming",
                    "waitGap", "waitClear", "stopAtLine", "stopAtCrosswalk",
                    "stop", "decelerate", "phaseRed", "RED", "noGap"), "WAIT"),
            new KeywordRule(List.of("firstArrival", "GREEN", "gapDetected", "commitTurn",
                    "proceed", "rsuGrant", "greenWave"), "EGO_GO"),
            new KeywordRule(List.of("oncomingPass", "oncomingApproach", "phaseGreen",
                    "passIntersection", "cross"), "OTHER_GO"));

    /** phi : L(S) -> ROWSupervisor (state-indexed). */
    public static RowVerdict phiRowVerdict(V2xProtocol protocol, StateSpace ss, int state) {
        if (state == ss.bottom()) {
            return new RowVerdict("CLEAR", protocol.cal(),
                    "State " + state + " is the lattice bottom; intersection cleared.");
        }
        if (state == ss.top()) {
            return new RowVerdict("WAIT", protocol.cal(),
                    "State " + state + " is the lattice top; supervisor instructs WAIT.");
        }

        List<String> incident = new ArrayList<>();
        for (var t : ss.transitions()) {
            if (t.source() == state || t.target() == state) {
                incident.add(t.label());
            }
        }

        for (KeywordRule rule : VERDICT_KEYWORDS) {
            for (String kw : rule.keywords()) {
                for (String lbl : incident) {
                    if (lbl.contains(kw)) {
                        return new RowVerdict(rule.verdict(), protocol.cal(),
                                "State " + state + " witnesses verdict " + rule.verdict()
                                        + " via label containing '" + kw + "'.");
                    }
                }
            }
        }

        return new RowVerdict("WAIT", protocol.cal(),
                "State " + state + " matches no specific verdict keyword; default WAIT.");
    }

    public static Map<Integer, RowVerdict> phiAllStates(V2xProtocol protocol, StateSpace ss) {
        Map<Integer, RowVerdict> result = new TreeMap<>();
        for (int s : ss.states()) {
            result.put(s, phiRowVerdict(protocol, ss, s));
        }
        return result;
    }

    /** psi : ROWSupervisor -> L(S). */
    public static Set<Integer> psiVerdictToStates(
            V2xProtocol protocol, StateSpace ss, String verdict) {
        if (!ROW_VERDICTS.contains(verdict)) {
            throw new IllegalArgumentException(
                    "Invalid verdict " + verdict + "; expected one of " + ROW_VERDICTS);
        }
        Map<Integer, RowVerdict> verdicts = phiAllStates(protocol, ss);
        Set<Integer> result = new HashSet<>();
        for (var e : verdicts.entrySet()) {
            if (e.getValue().verdict().equals(verdict)) {
                result.add(e.getKey());
            }
        }
        return Set.copyOf(result);
    }

    /** Classify the pair (phi, psi). */
    public static String classifyMorphismPair(V2xProtocol protocol, StateSpace ss) {
        Map<Integer, RowVerdict> verdicts = phiAllStates(protocol, ss);
        Set<String> kinds = new HashSet<>();
        for (RowVerdict v : verdicts.values()) {
            kinds.add(v.verdict());
        }
        if (kinds.size() == ss.states().size()) {
            return "isomorphism";
        }
        if (kinds.size() < ss.states().size()) {
            for (String kind : kinds) {
                Set<Integer> states = psiVerdictToStates(protocol, ss, kind);
                for (int s : states) {
                    if (!phiRowVerdict(protocol, ss, s).verdict().equals(kind)) {
                        return "galois";
                    }
                }
            }
            return "section-retraction";
        }
        return "projection";
    }

    // -----------------------------------------------------------------------
    // Safety / fairness / liveness
    // -----------------------------------------------------------------------

    public static boolean checkCollisionFree(V2xProtocol protocol, StateSpace ss) {
        Map<Integer, RowVerdict> verdicts = phiAllStates(protocol, ss);
        boolean hasEmergency = verdicts.values().stream()
                .anyMatch(v -> v.verdict().equals("EMERGENCY_BRAKE"));
        if (protocol.scenario().equals("signalized") && protocol.roles().contains("emergency")) {
            return true;
        }
        return !hasEmergency;
    }

    public static boolean checkFairness(V2xProtocol protocol, StateSpace ss) {
        if (protocol.roles().size() <= 1) return true;
        Map<Integer, RowVerdict> verdicts = phiAllStates(protocol, ss);
        Set<String> kinds = new HashSet<>();
        for (RowVerdict v : verdicts.values()) kinds.add(v.verdict());
        if (protocol.scenario().equals("signalized") || protocol.scenario().equals("left_turn")) {
            return kinds.contains("EGO_GO");
        }
        return kinds.contains("EGO_GO");
    }

    public static boolean checkLiveness(V2xProtocol protocol, StateSpace ss) {
        return ss.states().contains(ss.bottom()) && ss.states().contains(ss.top());
    }

    // -----------------------------------------------------------------------
    // Core verification pipeline
    // -----------------------------------------------------------------------

    public static SessionType v2xToSessionType(V2xProtocol protocol) {
        return Parser.parse(protocol.sessionTypeString());
    }

    public static V2xAnalysisResult verifyV2xProtocol(V2xProtocol protocol) {
        SessionType ast = v2xToSessionType(protocol);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        DistributivityResult dist = DistributivityChecker.checkDistributive(ss);
        Map<Integer, RowVerdict> verdicts = phiAllStates(protocol, ss);

        return new V2xAnalysisResult(
                protocol, ast, ss, lr, dist,
                ss.states().size(),
                ss.transitions().size(),
                lr.isLattice(),
                verdicts,
                checkCollisionFree(protocol, ss),
                checkFairness(protocol, ss),
                checkLiveness(protocol, ss));
    }

    public static List<V2xAnalysisResult> analyzeAllV2x() {
        List<V2xAnalysisResult> out = new ArrayList<>();
        for (V2xProtocol p : allV2xProtocols()) {
            out.add(verifyV2xProtocol(p));
        }
        return List.copyOf(out);
    }

    // -----------------------------------------------------------------------
    // Report formatting
    // -----------------------------------------------------------------------

    public static String formatV2xReport(V2xAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        V2xProtocol proto = result.protocol();
        String bar = "=".repeat(72);
        sb.append(bar).append('\n');
        sb.append("  V2X INTERSECTION PROTOCOL REPORT: ").append(proto.name()).append('\n');
        sb.append(bar).append('\n').append('\n');
        sb.append("  ").append(proto.description()).append('\n').append('\n');
        sb.append("--- Protocol Details ---\n");
        sb.append("  Scenario: ").append(proto.scenario()).append('\n');
        sb.append("  CAL:      ").append(proto.cal()).append('\n');
        sb.append("  Spec:     ").append(proto.specReference()).append('\n');
        for (String r : proto.roles()) {
            sb.append("  Role:     ").append(r).append('\n');
        }
        sb.append('\n');
        sb.append("--- State Space ---\n");
        sb.append("  States:      ").append(result.numStates()).append('\n');
        sb.append("  Transitions: ").append(result.numTransitions()).append('\n').append('\n');
        sb.append("--- Lattice Analysis ---\n");
        sb.append("  Is lattice:   ").append(result.latticeResult().isLattice()).append('\n');
        sb.append("  Distributive: ").append(result.distributivity().isDistributive()).append('\n').append('\n');
        sb.append("--- ROW Verdicts (phi) ---\n");
        Map<String, Integer> counts = new TreeMap<>();
        for (RowVerdict v : result.rowVerdicts().values()) {
            counts.merge(v.verdict(), 1, Integer::sum);
        }
        for (var e : counts.entrySet()) {
            sb.append(String.format("  %-18s: %d state(s)%n", e.getKey(), e.getValue()));
        }
        sb.append('\n');
        sb.append("--- Safety Properties ---\n");
        sb.append("  Collision free: ").append(result.collisionFree()).append('\n');
        sb.append("  Fairness:       ").append(result.fairnessHolds()).append('\n');
        sb.append("  Liveness:       ").append(result.livenessHolds()).append('\n').append('\n');
        return sb.toString();
    }
}
