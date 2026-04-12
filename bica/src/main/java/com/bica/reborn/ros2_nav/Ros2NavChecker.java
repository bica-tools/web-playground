package com.bica.reborn.ros2_nav;

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
 * ROS 2 Nav2 stack as session-type lattices (Step 801).
 *
 * <p>Java port of {@code reticulate/reticulate/ros2_nav.py}. Models the eight
 * canonical Nav2 protocols (NavigateToPose, ComputePathToPose, FollowPath,
 * RecoverySpin, BtNavigatorFallback, SensorPipelineParallel, Tf2TransformChain,
 * AmclLocalization) as session types and provides bidirectional supervisor
 * morphisms phi : L(S) -> NavSupervisor and psi : NavSupervisor -> L(S).
 */
public final class Ros2NavChecker {

    private Ros2NavChecker() {}

    public static final List<String> SUPERVISOR_MODES = List.of(
            "IDLE", "LOCALIZED", "NAVIGATING", "REPLANNING",
            "RECOVERING", "SENSOR_FAULT", "EMERGENCY_STOP", "GOAL_REACHED");

    public static final List<String> NAV2_NODES = List.of(
            "nav2_planner", "nav2_controller", "nav2_bt_navigator",
            "nav2_recoveries", "costmap_2d", "amcl");

    private static final Set<String> VALID_LAYERS = Set.of(
            "ACTION", "BT", "SENSOR", "TF", "AMCL");

    /** A ROS 2 Nav2 protocol modelled as a session type. */
    public record Ros2Protocol(
            String name,
            String layer,
            List<String> nodes,
            String sessionTypeString,
            String specReference,
            String description) {

        public Ros2Protocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(layer);
            Objects.requireNonNull(nodes);
            Objects.requireNonNull(sessionTypeString);
            Objects.requireNonNull(specReference);
            Objects.requireNonNull(description);
            if (!VALID_LAYERS.contains(layer)) {
                throw new IllegalArgumentException(
                        "Invalid layer " + layer + "; expected one of " + VALID_LAYERS);
            }
            nodes = List.copyOf(nodes);
        }
    }

    /** A Nav2 supervisor mode (codomain of phi). */
    public record SupervisorMode(String kind, String rationale) {
        public SupervisorMode {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(rationale);
        }
    }

    /** Complete analysis result for a ROS 2 Nav2 protocol. */
    public record Ros2AnalysisResult(
            Ros2Protocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            DistributivityResult distributivity,
            int numStates,
            int numTransitions,
            boolean isWellFormed,
            Map<Integer, SupervisorMode> supervisorModes,
            List<Integer> deadlockStates) {

        public Ros2AnalysisResult {
            Objects.requireNonNull(protocol);
            Objects.requireNonNull(ast);
            Objects.requireNonNull(stateSpace);
            Objects.requireNonNull(latticeResult);
            Objects.requireNonNull(distributivity);
            Objects.requireNonNull(supervisorModes);
            Objects.requireNonNull(deadlockStates);
            supervisorModes = Map.copyOf(supervisorModes);
            deadlockStates = List.copyOf(deadlockStates);
        }
    }

    // -----------------------------------------------------------------------
    // Protocol definitions
    // -----------------------------------------------------------------------

    public static Ros2Protocol navigateToPose() {
        return new Ros2Protocol(
                "NavigateToPose",
                "ACTION",
                List.of("nav2_bt_navigator", "nav2_planner", "nav2_controller"),
                "&{sendGoal: +{ACCEPTED: "
                        + "&{computePath: +{PATH_OK: "
                        + "&{followPath: +{REACHED: &{succeed: end}, "
                        + "BLOCKED: &{recover: +{CLEARED: &{succeed: end}, "
                        + "ABORT: &{abort: end}}}}}, "
                        + "NO_PATH: &{abort: end}}}, "
                        + "REJECTED: &{abort: end}}}",
                "Nav2 Humble: NavigateToPose action",
                "Top-level NavigateToPose: validate goal, compute path, follow, recover, abort.");
    }

    public static Ros2Protocol computePathToPose() {
        return new Ros2Protocol(
                "ComputePathToPose",
                "ACTION",
                List.of("nav2_planner", "costmap_2d"),
                "&{requestPlan: +{COSTMAP_OK: "
                        + "&{queryCostmap: &{runPlanner: "
                        + "+{PATH_FOUND: &{returnPath: end}, "
                        + "NO_PATH: &{planFailure: end}}}}, "
                        + "COSTMAP_STALE: &{planFailure: end}}}",
                "Nav2 Humble: ComputePathToPose action",
                "Planner action: query costmap, run A*/Theta*, return path or failure.");
    }

    public static Ros2Protocol followPath() {
        return new Ros2Protocol(
                "FollowPath",
                "ACTION",
                List.of("nav2_controller", "costmap_2d"),
                "&{loadPath: &{computeVelocity: "
                        + "+{COMMAND_OK: &{publishCmdVel: "
                        + "+{GOAL_REACHED: &{succeed: end}, "
                        + "OBSTACLE: &{stop: &{controllerFailure: end}}}}, "
                        + "INFEASIBLE: &{controllerFailure: end}}}}",
                "Nav2 Humble: FollowPath action (DWB/MPPI)",
                "Controller action: load path, compute velocity, publish cmd_vel.");
    }

    public static Ros2Protocol sensorPipelineParallel() {
        return new Ros2Protocol(
                "SensorPipelineParallel",
                "SENSOR",
                List.of("costmap_2d"),
                "((&{lidarRead: &{lidarFilter: &{lidarPublish: end}}} "
                        + "|| &{odomRead: &{odomFilter: &{odomPublish: end}}}) "
                        + "|| &{imuRead: &{imuFilter: &{imuPublish: end}}})",
                "Nav2 Humble: costmap_2d sensor sources",
                "Three sensor pipelines run in parallel; product lattice 4*4*4 = 64 states.");
    }

    public static Ros2Protocol btNavigatorFallback() {
        return new Ros2Protocol(
                "BtNavigatorFallback",
                "BT",
                List.of("nav2_bt_navigator", "nav2_recoveries"),
                "&{tryPrimary: +{OK: &{succeed: end}, "
                        + "FAIL: &{clearLocal: +{OK: &{retry: &{succeed: end}}, "
                        + "FAIL: &{clearGlobal: +{OK: &{retry: &{succeed: end}}, "
                        + "FAIL: &{spinRecovery: +{OK: &{retry: &{succeed: end}}, "
                        + "FAIL: &{abort: end}}}}}}}}}",
                "Nav2 Humble: behavior_tree_xml RecoveryNode",
                "BT recovery escalation: primary -> clearLocal -> clearGlobal -> spin -> abort.");
    }

    public static Ros2Protocol amclLocalization() {
        return new Ros2Protocol(
                "AmclLocalization",
                "AMCL",
                List.of("amcl"),
                "&{initPose: &{laserUpdate: "
                        + "+{CONVERGED: &{publishTransform: end}, "
                        + "DIVERGED: &{relocalize: "
                        + "+{CONVERGED: &{publishTransform: end}, "
                        + "FAILED: &{localizationFailure: end}}}}}}",
                "Nav2 Humble: amcl node (KLD-sampling)",
                "AMCL: init, fuse laser, converge or relocalize, report failure.");
    }

    public static Ros2Protocol tf2TransformChain() {
        return new Ros2Protocol(
                "Tf2TransformChain",
                "TF",
                List.of("nav2_bt_navigator"),
                "&{lookupMapOdom: +{OK: "
                        + "&{lookupOdomBase: +{OK: &{composeTransform: end}, "
                        + "TIMEOUT: &{tfFailure: end}}}, "
                        + "TIMEOUT: &{tfFailure: end}}}",
                "ROS 2 Humble: tf2 lookupTransform",
                "tf2 chain: lookup map->odom and odom->base_link, compose or fail.");
    }

    public static Ros2Protocol recoverySpin() {
        return new Ros2Protocol(
                "RecoverySpin",
                "ACTION",
                List.of("nav2_recoveries"),
                "&{startSpin: &{rotate: "
                        + "+{COMPLETE: &{succeed: end}, "
                        + "BLOCKED: &{abort: end}}}}",
                "Nav2 Humble: Spin recovery",
                "Spin recovery: rotate in place, succeed or abort if blocked.");
    }

    public static List<Ros2Protocol> allActionProtocols() {
        return List.of(
                navigateToPose(),
                computePathToPose(),
                followPath(),
                recoverySpin());
    }

    public static List<Ros2Protocol> allBtProtocols() {
        return List.of(btNavigatorFallback());
    }

    public static List<Ros2Protocol> allSensorProtocols() {
        return List.of(sensorPipelineParallel());
    }

    public static List<Ros2Protocol> allTfProtocols() {
        return List.of(tf2TransformChain());
    }

    public static List<Ros2Protocol> allAmclProtocols() {
        return List.of(amclLocalization());
    }

    public static List<Ros2Protocol> allRos2Protocols() {
        List<Ros2Protocol> all = new ArrayList<>();
        all.addAll(allActionProtocols());
        all.addAll(allBtProtocols());
        all.addAll(allSensorProtocols());
        all.addAll(allTfProtocols());
        all.addAll(allAmclProtocols());
        return List.copyOf(all);
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphisms phi / psi
    // -----------------------------------------------------------------------

    private record KeywordRule(List<String> keywords, String kind) {}

    private static final List<KeywordRule> MODE_KEYWORDS = List.of(
            new KeywordRule(
                    List.of("abort", "controllerFailure", "planFailure",
                            "localizationFailure", "tfFailure"),
                    "EMERGENCY_STOP"),
            new KeywordRule(
                    List.of("recover", "spinRecovery", "clearLocal", "clearGlobal",
                            "relocalize", "retry"),
                    "RECOVERING"),
            new KeywordRule(
                    List.of("succeed", "GOAL_REACHED", "publishTransform"),
                    "GOAL_REACHED"),
            new KeywordRule(
                    List.of("computePath", "queryCostmap", "runPlanner", "requestPlan"),
                    "REPLANNING"),
            new KeywordRule(
                    List.of("followPath", "publishCmdVel", "computeVelocity",
                            "loadPath", "rotate", "startSpin"),
                    "NAVIGATING"),
            new KeywordRule(
                    List.of("initPose", "laserUpdate", "lookupMapOdom",
                            "lookupOdomBase", "composeTransform"),
                    "LOCALIZED"),
            new KeywordRule(
                    List.of("lidar", "odom", "imu"),
                    "SENSOR_FAULT"));

    /** phi : L(S) -> NavSupervisor (state-indexed). */
    public static SupervisorMode phiSupervisorMode(
            Ros2Protocol protocol, StateSpace ss, int state) {
        if (state == ss.bottom()) {
            return new SupervisorMode(
                    "GOAL_REACHED",
                    "State " + state + " is the lattice bottom; goal terminated.");
        }
        if (state == ss.top()) {
            return new SupervisorMode(
                    "IDLE",
                    "State " + state + " is the lattice top; no work issued yet.");
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
                        return new SupervisorMode(
                                rule.kind(),
                                "State " + state + " mapped to " + rule.kind()
                                        + " via label containing '" + kw + "'.");
                    }
                }
            }
        }

        return new SupervisorMode(
                "NAVIGATING",
                "State " + state + " has no distinguishing label; default NAVIGATING.");
    }

    public static Map<Integer, SupervisorMode> phiAllStates(
            Ros2Protocol protocol, StateSpace ss) {
        Map<Integer, SupervisorMode> result = new TreeMap<>();
        for (int s : ss.states()) {
            result.put(s, phiSupervisorMode(protocol, ss, s));
        }
        return result;
    }

    /** psi : NavSupervisor -> L(S). */
    public static Set<Integer> psiModeToStates(
            Ros2Protocol protocol, StateSpace ss, String modeKind) {
        Map<Integer, SupervisorMode> modes = phiAllStates(protocol, ss);
        Set<Integer> result = new HashSet<>();
        for (var entry : modes.entrySet()) {
            if (entry.getValue().kind().equals(modeKind)) {
                result.add(entry.getKey());
            }
        }
        return Set.copyOf(result);
    }

    public static String classifyMorphismPair(Ros2Protocol protocol, StateSpace ss) {
        Map<Integer, SupervisorMode> modes = phiAllStates(protocol, ss);
        Set<String> kinds = new HashSet<>();
        for (SupervisorMode m : modes.values()) {
            kinds.add(m.kind());
        }
        if (kinds.size() == ss.states().size()) {
            return "isomorphism";
        }
        if (kinds.size() < ss.states().size()) {
            for (String kind : kinds) {
                Set<Integer> states = psiModeToStates(protocol, ss, kind);
                for (int s : states) {
                    if (!phiSupervisorMode(protocol, ss, s).kind().equals(kind)) {
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

    public static SessionType ros2ToSessionType(Ros2Protocol protocol) {
        return Parser.parse(protocol.sessionTypeString());
    }

    public static Ros2AnalysisResult verifyRos2Protocol(Ros2Protocol protocol) {
        SessionType ast = ros2ToSessionType(protocol);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        DistributivityResult dist = DistributivityChecker.checkDistributive(ss);
        Map<Integer, SupervisorMode> modes = phiAllStates(protocol, ss);
        List<Integer> deadlocks = detectDeadlocks(ss);
        return new Ros2AnalysisResult(
                protocol, ast, ss, lr, dist,
                ss.states().size(),
                ss.transitions().size(),
                lr.isLattice(),
                modes,
                deadlocks);
    }

    public static List<Ros2AnalysisResult> analyzeAllRos2() {
        List<Ros2AnalysisResult> out = new ArrayList<>();
        for (Ros2Protocol p : allRos2Protocols()) {
            out.add(verifyRos2Protocol(p));
        }
        return List.copyOf(out);
    }

    // -----------------------------------------------------------------------
    // Report formatting
    // -----------------------------------------------------------------------

    public static String formatRos2Report(Ros2AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        Ros2Protocol proto = result.protocol();
        String bar = "=".repeat(72);
        sb.append(bar).append('\n');
        sb.append("  ROS 2 NAV2 PROTOCOL REPORT: ").append(proto.name()).append('\n');
        sb.append(bar).append('\n').append('\n');
        sb.append("  ").append(proto.description()).append('\n').append('\n');
        sb.append("--- Protocol Details ---\n");
        sb.append("  Layer: ").append(proto.layer()).append('\n');
        sb.append("  Spec:  ").append(proto.specReference()).append('\n');
        for (String n : proto.nodes()) {
            sb.append("  Node:  ").append(n).append('\n');
        }
        sb.append('\n');
        sb.append("--- State Space ---\n");
        sb.append("  States:      ").append(result.numStates()).append('\n');
        sb.append("  Transitions: ").append(result.numTransitions()).append('\n');
        sb.append('\n');
        sb.append("--- Lattice Analysis ---\n");
        sb.append("  Is lattice:   ").append(result.latticeResult().isLattice()).append('\n');
        sb.append("  Distributive: ").append(result.distributivity().isDistributive()).append('\n');
        sb.append('\n');
        sb.append("--- Supervisor Modes (phi) ---\n");
        Map<String, Integer> counts = new TreeMap<>();
        for (SupervisorMode m : result.supervisorModes().values()) {
            counts.merge(m.kind(), 1, Integer::sum);
        }
        for (var e : counts.entrySet()) {
            sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        }
        sb.append('\n');
        sb.append("--- Deadlock Detection ---\n");
        if (result.deadlockStates().isEmpty()) {
            sb.append("  OK: no deadlocks detected.\n");
        } else {
            sb.append("  WARN: deadlocked states ").append(result.deadlockStates()).append('\n');
        }
        sb.append('\n');
        sb.append("--- Verdict ---\n");
        if (result.isWellFormed() && result.deadlockStates().isEmpty()) {
            sb.append("  PASS: lattice well-formed and deadlock-free.\n");
        } else {
            sb.append("  FAIL: lattice or deadlock check failed.\n");
        }
        return sb.toString();
    }
}
