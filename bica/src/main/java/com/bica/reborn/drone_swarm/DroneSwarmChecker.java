package com.bica.reborn.drone_swarm;

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
 * Drone Swarm Coordination protocol verification (Step 805).
 *
 * <p>Java port of {@code reticulate/reticulate/drone_swarm.py}. Models
 * multi-drone (UAV) coordination protocols as multiparty session types
 * built with the parallel constructor; right-of-way / mission state is
 * captured as a section-retraction (phi, psi) onto a 7-element
 * SwarmSupervisor lattice.
 */
public final class DroneSwarmChecker {

    private DroneSwarmChecker() {}

    // -----------------------------------------------------------------------
    // Domain data types
    // -----------------------------------------------------------------------

    /** Swarm Assurance Levels (analogous to ISO 21384-3 categories). */
    public static final List<String> SAL_LEVELS = List.of("SAL1", "SAL2", "SAL3", "SAL4");

    /** Swarm supervisor verdicts (the small lattice). */
    public static final List<String> SWARM_VERDICTS = List.of(
            "IDLE", "ELECTING", "FORMING", "CRUISING",
            "REPLANNING", "FAILOVER", "ABORT");

    private static final Set<String> VALID_SCENARIOS = Set.of(
            "election", "formation", "task_alloc", "collision", "failover");

    /** A drone-swarm coordination protocol. */
    public record SwarmProtocol(
            String name,
            String scenario,
            String sal,
            List<String> roles,
            String sessionTypeString,
            String specReference,
            String description) {

        public SwarmProtocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(scenario);
            Objects.requireNonNull(sal);
            Objects.requireNonNull(roles);
            Objects.requireNonNull(sessionTypeString);
            Objects.requireNonNull(specReference);
            Objects.requireNonNull(description);
            if (!SAL_LEVELS.contains(sal)) {
                throw new IllegalArgumentException(
                        "Invalid SAL level " + sal + "; expected one of " + SAL_LEVELS);
            }
            if (!VALID_SCENARIOS.contains(scenario)) {
                throw new IllegalArgumentException(
                        "Invalid scenario " + scenario
                                + "; expected election, formation, task_alloc, collision, or failover.");
            }
            roles = List.copyOf(roles);
        }
    }

    /** A swarm verdict assigned to a joint protocol state. */
    public record SwarmVerdict(String verdict, String sal, String rationale) {
        public SwarmVerdict {
            Objects.requireNonNull(verdict);
            Objects.requireNonNull(sal);
            Objects.requireNonNull(rationale);
            if (!SWARM_VERDICTS.contains(verdict)) {
                throw new IllegalArgumentException(
                        "Invalid verdict " + verdict + "; expected one of " + SWARM_VERDICTS);
            }
        }
    }

    /** Complete analysis result for a drone-swarm protocol. */
    public record SwarmAnalysisResult(
            SwarmProtocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            DistributivityResult distributivity,
            int numStates,
            int numTransitions,
            boolean isWellFormed,
            Map<Integer, SwarmVerdict> swarmVerdicts,
            boolean noCollision,
            boolean consensusProgress,
            boolean livenessHolds,
            boolean faultTolerant) {

        public SwarmAnalysisResult {
            Objects.requireNonNull(protocol);
            Objects.requireNonNull(ast);
            Objects.requireNonNull(stateSpace);
            Objects.requireNonNull(latticeResult);
            Objects.requireNonNull(distributivity);
            Objects.requireNonNull(swarmVerdicts);
            swarmVerdicts = Map.copyOf(swarmVerdicts);
        }
    }

    // -----------------------------------------------------------------------
    // Drone-swarm protocol definitions
    // -----------------------------------------------------------------------

    public static SwarmProtocol leaderElection() {
        return new SwarmProtocol(
                "LeaderElection",
                "election",
                "SAL2",
                List.of("drone1", "drone2"),
                "( &{boot: &{candidacy: +{wonElection: &{leaderAck: end}, "
                        + "lostElection: &{followerAck: end}}}} "
                        + "|| "
                        + "&{boot: &{candidacy: +{wonElection: &{leaderAck: end}, "
                        + "lostElection: &{followerAck: end}}}} )",
                "MAVLink HEARTBEAT, Garcia-Molina Bully (1982)",
                "Bully leader election among two parallel drones.");
    }

    public static SwarmProtocol formationFlying() {
        return new SwarmProtocol(
                "FormationFlying",
                "formation",
                "SAL3",
                List.of("leader", "wingman"),
                "( &{takeoff: &{publishWaypoint: +{cruise: &{land: end}, "
                        + "abortMission: &{land: end}}}} "
                        + "|| "
                        + "&{takeoff: &{stationKeep: +{holdStation: &{land: end}, "
                        + "peelOff: &{abortStation: &{land: end}}}}} )",
                "MAVLink MISSION_ITEM, ASTM F3411-22a",
                "V-formation with leader publishing waypoints and wingman holding station.");
    }

    public static SwarmProtocol taskAllocation() {
        return new SwarmProtocol(
                "TaskAllocation",
                "task_alloc",
                "SAL2",
                List.of("drone1", "drone2"),
                "( &{auctionOpen: &{submitBid: +{wonTask: &{executeTask: end}, "
                        + "lostTask: &{idleSlot: end}}}} "
                        + "|| "
                        + "&{auctionOpen: &{submitBid: +{wonTask: &{executeTask: end}, "
                        + "lostTask: &{idleSlot: end}}}} )",
                "Smith Contract Net (1980), MRTA taxonomy",
                "Two drones bid on a task in an auction; exactly one wins.");
    }

    public static SwarmProtocol collisionAvoidance() {
        return new SwarmProtocol(
                "CollisionAvoidance",
                "collision",
                "SAL4",
                List.of("drone1", "drone2"),
                "( &{detectPeer: &{negotiateGap: +{gapAccepted: "
                        + "&{maintainHeading: end}, abortCollision: &{emergencyLand: end}}}} "
                        + "|| "
                        + "&{detectPeer: &{negotiateGap: +{gapAccepted: "
                        + "&{maintainHeading: end}, abortCollision: &{emergencyLand: end}}}} )",
                "ASTM F3442 / RTCA DO-365 DAA",
                "Two drones detect each other, negotiate a gap, or abort.");
    }

    public static SwarmProtocol leaderFailover() {
        return new SwarmProtocol(
                "LeaderFailover",
                "failover",
                "SAL3",
                List.of("oldLeader", "successor"),
                "( &{heartbeatLoss: &{candidacy: +{wonElection: "
                        + "&{leaderAck: end}, lostElection: &{followerAck: end}}}} "
                        + "|| "
                        + "&{heartbeatLoss: &{candidacy: +{wonElection: "
                        + "&{leaderAck: end}, lostElection: &{followerAck: end}}}} )",
                "Raft (Ongaro 2014), MAVLink HEARTBEAT timeout",
                "After heartbeat loss, surviving drones run a fresh election.");
    }

    public static List<SwarmProtocol> allSwarmProtocols() {
        return List.of(
                leaderElection(),
                formationFlying(),
                taskAllocation(),
                collisionAvoidance(),
                leaderFailover());
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphisms phi and psi
    // -----------------------------------------------------------------------

    private record KeywordRule(List<String> keywords, String verdict) {}

    private static final List<KeywordRule> VERDICT_KEYWORDS = List.of(
            new KeywordRule(List.of("abortCollision", "emergencyLand", "abortMission",
                    "abortStation"), "ABORT"),
            new KeywordRule(List.of("heartbeatLoss"), "FAILOVER"),
            new KeywordRule(List.of("publishWaypoint", "stationKeep", "negotiateGap"),
                    "REPLANNING"),
            new KeywordRule(List.of("cruise", "holdStation", "maintainHeading", "executeTask",
                    "leaderAck", "followerAck"), "CRUISING"),
            new KeywordRule(List.of("takeoff", "peelOff"), "FORMING"),
            new KeywordRule(List.of("candidacy", "wonElection", "lostElection",
                    "auctionOpen", "submitBid", "wonTask", "lostTask",
                    "detectPeer", "boot"), "ELECTING"));

    /** phi : L(S) -> SwarmSupervisor (state-indexed). */
    public static SwarmVerdict phiSwarmVerdict(SwarmProtocol protocol, StateSpace ss, int state) {
        if (state == ss.bottom()) {
            return new SwarmVerdict("IDLE", protocol.sal(),
                    "State " + state + " is the lattice bottom; swarm grounded.");
        }
        if (state == ss.top()) {
            return new SwarmVerdict("ELECTING", protocol.sal(),
                    "State " + state + " is the lattice top; swarm bring-up.");
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
                        return new SwarmVerdict(rule.verdict(), protocol.sal(),
                                "State " + state + " witnesses verdict " + rule.verdict()
                                        + " via label containing '" + kw + "'.");
                    }
                }
            }
        }

        return new SwarmVerdict("ELECTING", protocol.sal(),
                "State " + state + " matches no specific verdict keyword; default ELECTING.");
    }

    public static Map<Integer, SwarmVerdict> phiAllStates(SwarmProtocol protocol, StateSpace ss) {
        Map<Integer, SwarmVerdict> result = new TreeMap<>();
        for (int s : ss.states()) {
            result.put(s, phiSwarmVerdict(protocol, ss, s));
        }
        return result;
    }

    /** psi : SwarmSupervisor -> L(S). */
    public static Set<Integer> psiVerdictToStates(
            SwarmProtocol protocol, StateSpace ss, String verdict) {
        if (!SWARM_VERDICTS.contains(verdict)) {
            throw new IllegalArgumentException(
                    "Invalid verdict " + verdict + "; expected one of " + SWARM_VERDICTS);
        }
        Map<Integer, SwarmVerdict> verdicts = phiAllStates(protocol, ss);
        Set<Integer> result = new HashSet<>();
        for (var e : verdicts.entrySet()) {
            if (e.getValue().verdict().equals(verdict)) {
                result.add(e.getKey());
            }
        }
        return Set.copyOf(result);
    }

    public static String classifyMorphismPair(SwarmProtocol protocol, StateSpace ss) {
        Map<Integer, SwarmVerdict> verdicts = phiAllStates(protocol, ss);
        Set<String> kinds = new HashSet<>();
        for (SwarmVerdict v : verdicts.values()) {
            kinds.add(v.verdict());
        }
        if (kinds.size() == ss.states().size()) {
            return "isomorphism";
        }
        if (kinds.size() < ss.states().size()) {
            for (String kind : kinds) {
                Set<Integer> states = psiVerdictToStates(protocol, ss, kind);
                for (int s : states) {
                    if (!phiSwarmVerdict(protocol, ss, s).verdict().equals(kind)) {
                        return "galois";
                    }
                }
            }
            return "section-retraction";
        }
        return "projection";
    }

    // -----------------------------------------------------------------------
    // Safety / liveness / fault tolerance
    // -----------------------------------------------------------------------

    public static boolean checkNoCollision(SwarmProtocol protocol, StateSpace ss) {
        Map<Integer, SwarmVerdict> verdicts = phiAllStates(protocol, ss);
        boolean hasAbort = verdicts.values().stream()
                .anyMatch(v -> v.verdict().equals("ABORT"));
        if (protocol.scenario().equals("collision")) {
            return true;
        }
        return !hasAbort;
    }

    public static boolean checkConsensusProgress(SwarmProtocol protocol, StateSpace ss) {
        Map<Integer, SwarmVerdict> verdicts = phiAllStates(protocol, ss);
        Set<String> kinds = new HashSet<>();
        for (SwarmVerdict v : verdicts.values()) kinds.add(v.verdict());
        if (!kinds.contains("REPLANNING")) return true;
        return kinds.contains("CRUISING") || kinds.contains("ABORT");
    }

    public static boolean checkLiveness(SwarmProtocol protocol, StateSpace ss) {
        return ss.states().contains(ss.bottom()) && ss.states().contains(ss.top());
    }

    public static boolean checkFaultTolerance(SwarmProtocol protocol, StateSpace ss) {
        Map<Integer, SwarmVerdict> verdicts = phiAllStates(protocol, ss);
        Set<String> kinds = new HashSet<>();
        for (SwarmVerdict v : verdicts.values()) kinds.add(v.verdict());
        if (!kinds.contains("FAILOVER")) return true;
        return kinds.contains("CRUISING") || kinds.contains("FORMING") || kinds.contains("ELECTING");
    }

    // -----------------------------------------------------------------------
    // Core verification pipeline
    // -----------------------------------------------------------------------

    public static SessionType swarmToSessionType(SwarmProtocol protocol) {
        return Parser.parse(protocol.sessionTypeString());
    }

    public static SwarmAnalysisResult verifySwarmProtocol(SwarmProtocol protocol) {
        SessionType ast = swarmToSessionType(protocol);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        DistributivityResult dist = DistributivityChecker.checkDistributive(ss);
        Map<Integer, SwarmVerdict> verdicts = phiAllStates(protocol, ss);

        return new SwarmAnalysisResult(
                protocol, ast, ss, lr, dist,
                ss.states().size(),
                ss.transitions().size(),
                lr.isLattice(),
                verdicts,
                checkNoCollision(protocol, ss),
                checkConsensusProgress(protocol, ss),
                checkLiveness(protocol, ss),
                checkFaultTolerance(protocol, ss));
    }

    public static List<SwarmAnalysisResult> analyzeAllSwarms() {
        List<SwarmAnalysisResult> out = new ArrayList<>();
        for (SwarmProtocol p : allSwarmProtocols()) {
            out.add(verifySwarmProtocol(p));
        }
        return List.copyOf(out);
    }

    // -----------------------------------------------------------------------
    // Report formatting
    // -----------------------------------------------------------------------

    public static String formatSwarmReport(SwarmAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        SwarmProtocol proto = result.protocol();
        String bar = "=".repeat(72);
        sb.append(bar).append('\n');
        sb.append("  DRONE SWARM PROTOCOL REPORT: ").append(proto.name()).append('\n');
        sb.append(bar).append('\n').append('\n');
        sb.append("  ").append(proto.description()).append('\n').append('\n');
        sb.append("--- Protocol Details ---\n");
        sb.append("  Scenario: ").append(proto.scenario()).append('\n');
        sb.append("  SAL:      ").append(proto.sal()).append('\n');
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
        sb.append("--- Swarm Verdicts (phi) ---\n");
        Map<String, Integer> counts = new TreeMap<>();
        for (SwarmVerdict v : result.swarmVerdicts().values()) {
            counts.merge(v.verdict(), 1, Integer::sum);
        }
        for (var e : counts.entrySet()) {
            sb.append(String.format("  %-12s: %d state(s)%n", e.getKey(), e.getValue()));
        }
        sb.append('\n');
        sb.append("--- Safety / Liveness ---\n");
        sb.append("  No collision:       ").append(result.noCollision()).append('\n');
        sb.append("  Consensus progress: ").append(result.consensusProgress()).append('\n');
        sb.append("  Liveness:           ").append(result.livenessHolds()).append('\n');
        sb.append("  Fault tolerant:     ").append(result.faultTolerant()).append('\n').append('\n');
        return sb.toString();
    }
}
