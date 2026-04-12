package com.bica.reborn.stage_blocking;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.globaltype.GEnd;
import com.bica.reborn.globaltype.GMessage;
import com.bica.reborn.globaltype.GlobalType;
import com.bica.reborn.globaltype.GlobalTypeChecker;
import com.bica.reborn.globaltype.Projection;
import com.bica.reborn.globaltype.ProjectionError;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Theatrical stage blocking as multiparty session types (Step 58) --
 * Java port of {@code reticulate/reticulate/stage_blocking.py}.
 *
 * <p>A scene is encoded as a sequential {@link GlobalType}; roles are
 * the actors plus an implicit {@link #STAGE_MANAGER}; beats are unary
 * role-to-role messages carrying cues or position reports. The global
 * state space is checked for lattice structure, projected onto each
 * role, and a bidirectional morphism pair (phi, psi) maps lattice
 * states to blocking snapshots (who is where, which cues have fired).
 */
public final class StageBlockingChecker {

    public static final String STAGE_MANAGER = "SM";

    private static final Set<String> ALLOWED_POSITIONS = Set.of(
            "DSL", "DSC", "DSR", "USL", "USC", "USR", "CS", "OFF");

    private StageBlockingChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /** Kinds of cues issued by the stage manager. */
    public enum CueKind {
        ENTRANCE,
        LIGHT,
        SOUND,
        LINE,
        EXIT
    }

    /** A blocking position on stage. */
    public record Position(String code) {
        public Position {
            Objects.requireNonNull(code, "code must not be null");
            if (!ALLOWED_POSITIONS.contains(code)) {
                throw new IllegalArgumentException("unknown stage position: " + code);
            }
        }
    }

    /**
     * A single beat: either a cue from SM to an actor, or a position
     * report from an actor back to SM.
     */
    public record Beat(
            String sender,
            String receiver,
            String label,
            CueKind cueKind,
            Position position) {

        public Beat {
            Objects.requireNonNull(sender, "sender must not be null");
            Objects.requireNonNull(receiver, "receiver must not be null");
            Objects.requireNonNull(label, "label must not be null");
        }

        public Beat(String sender, String receiver, String label) {
            this(sender, receiver, label, null, null);
        }

        public Beat(String sender, String receiver, String label, CueKind cueKind) {
            this(sender, receiver, label, cueKind, null);
        }
    }

    /** A theatrical scene as a totally ordered list of beats. */
    public record Scene(String title, List<String> actors, List<Beat> beats) {

        public Scene {
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(actors, "actors must not be null");
            Objects.requireNonNull(beats, "beats must not be null");
            if (actors.isEmpty()) {
                throw new IllegalArgumentException("scene must have at least one actor");
            }
            if (actors.contains(STAGE_MANAGER)) {
                throw new IllegalArgumentException(
                        "actor name '" + STAGE_MANAGER + "' is reserved");
            }
            Set<String> allRoles = new HashSet<>(actors);
            allRoles.add(STAGE_MANAGER);
            for (Beat b : beats) {
                if (!allRoles.contains(b.sender())) {
                    throw new IllegalArgumentException(
                            "unknown sender in beat: " + b.sender());
                }
                if (!allRoles.contains(b.receiver())) {
                    throw new IllegalArgumentException(
                            "unknown receiver in beat: " + b.receiver());
                }
                if (b.sender().equals(b.receiver())) {
                    throw new IllegalArgumentException(
                            "self-loop beat forbidden: " + b.sender());
                }
            }
            actors = List.copyOf(actors);
            beats = List.copyOf(beats);
        }
    }

    /** Well-formedness report for a scene. */
    public record SceneWellFormedness(
            boolean isWellFormed,
            boolean globalIsLattice,
            List<String> roles,
            Map<String, String> localTypes,
            int globalSize,
            List<String> errors) {

        public SceneWellFormedness {
            roles = List.copyOf(roles);
            localTypes = Map.copyOf(localTypes);
            errors = List.copyOf(errors);
        }
    }

    /**
     * Snapshot of the stage at a single lattice state.
     * {@code positions} is a sorted list of (actor, position-code)
     * pairs; {@code firedCues} is the ordered list of cue labels.
     */
    public record BlockingSnapshot(
            List<Map.Entry<String, String>> positions,
            List<String> firedCues) {

        public BlockingSnapshot {
            positions = List.copyOf(positions);
            firedCues = List.copyOf(firedCues);
        }

        public List<String> actorsOnStage() {
            List<String> out = new ArrayList<>();
            for (Map.Entry<String, String> p : positions) {
                if (!p.getValue().equals("OFF")) {
                    out.add(p.getKey());
                }
            }
            return out;
        }
    }

    /** Full blocking plan: lattice-state-id -> snapshot. */
    public record BlockingPlan(Map<Integer, BlockingSnapshot> snapshots, Scene scene) {
        public BlockingPlan {
            snapshots = Map.copyOf(snapshots);
        }
    }

    /** Bidirectional morphism between the scene lattice and blocking plans. */
    public record BlockingMorphism(
            Map<Integer, BlockingSnapshot> phi,
            Map<BlockingSnapshot, Integer> psi,
            StateSpace stateSpace,
            BlockingPlan plan) {

        public BlockingMorphism {
            phi = Map.copyOf(phi);
            psi = Map.copyOf(psi);
        }
    }

    // -----------------------------------------------------------------------
    // Encoding
    // -----------------------------------------------------------------------

    /** Build a {@link GlobalType} from a {@link Scene}. */
    public static GlobalType encodeScene(Scene scene) {
        if (scene.beats().isEmpty()) {
            return new GEnd();
        }
        GlobalType g = new GEnd();
        List<Beat> beats = scene.beats();
        for (int i = beats.size() - 1; i >= 0; i--) {
            Beat b = beats.get(i);
            g = new GMessage(
                    b.sender(),
                    b.receiver(),
                    List.of(new GMessage.Choice(b.label(), g)));
        }
        return g;
    }

    // -----------------------------------------------------------------------
    // Well-formedness
    // -----------------------------------------------------------------------

    /** Project the scene onto every role and check the lattice property. */
    public static SceneWellFormedness checkScene(Scene scene) {
        GlobalType g = encodeScene(scene);
        List<String> errors = new ArrayList<>();
        Map<String, String> locals = new LinkedHashMap<>();

        Set<String> all = new LinkedHashSet<>(scene.actors());
        all.add(STAGE_MANAGER);
        List<String> allRoles = new ArrayList<>(all);
        Collections.sort(allRoles);

        for (String r : allRoles) {
            try {
                SessionType lt = Projection.project(g, r);
                locals.put(r, PrettyPrinter.pretty(lt));
            } catch (ProjectionError exc) {
                errors.add("projection onto " + r + " failed: " + exc.getMessage());
            }
        }

        StateSpace gss = null;
        int size = 0;
        try {
            gss = GlobalTypeChecker.buildStateSpace(g);
            size = gss.states().size();
        } catch (RuntimeException exc) {
            errors.add("state-space build failed: " + exc.getMessage());
        }

        boolean isWf = errors.isEmpty();
        boolean isLat = false;
        if (isWf && gss != null) {
            LatticeResult lr = LatticeChecker.checkLattice(gss);
            isLat = lr.isLattice();
        }

        return new SceneWellFormedness(isWf, isLat, allRoles, locals, size, errors);
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphism
    // -----------------------------------------------------------------------

    private static Map<Integer, Integer> bfsOrder(StateSpace ss) {
        Map<Integer, Integer> order = new LinkedHashMap<>();
        Set<Integer> seen = new HashSet<>();
        List<Integer> frontier = new ArrayList<>();
        frontier.add(ss.top());
        seen.add(ss.top());
        int idx = 0;
        while (!frontier.isEmpty()) {
            List<Integer> next = new ArrayList<>();
            for (int s : frontier) {
                order.put(s, idx++);
                for (Transition t : ss.transitions()) {
                    if (t.source() == s && seen.add(t.target())) {
                        next.add(t.target());
                    }
                }
            }
            frontier = next;
        }
        for (int s : ss.states()) {
            if (!order.containsKey(s)) {
                order.put(s, idx++);
            }
        }
        return order;
    }

    private static BlockingSnapshot snapshotOf(
            TreeMap<String, String> positions, List<String> fired) {
        List<Map.Entry<String, String>> pos = new ArrayList<>();
        for (Map.Entry<String, String> e : positions.entrySet()) {
            pos.add(Map.entry(e.getKey(), e.getValue()));
        }
        return new BlockingSnapshot(pos, new ArrayList<>(fired));
    }

    /** Construct phi and psi for a scene. */
    public static BlockingMorphism buildMorphism(Scene scene) {
        GlobalType g = encodeScene(scene);
        StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
        Map<Integer, Integer> order = bfsOrder(ss);

        TreeMap<String, String> positions = new TreeMap<>();
        for (String a : scene.actors()) {
            positions.put(a, "OFF");
        }

        List<BlockingSnapshot> snapshotsByIndex = new ArrayList<>();
        List<String> fired = new ArrayList<>();
        snapshotsByIndex.add(snapshotOf(positions, fired));

        for (Beat b : scene.beats()) {
            if (b.position() != null && positions.containsKey(b.sender())) {
                positions.put(b.sender(), b.position().code());
            }
            if (b.position() != null && positions.containsKey(b.receiver())) {
                positions.put(b.receiver(), b.position().code());
            }
            if (b.cueKind() == CueKind.EXIT && positions.containsKey(b.receiver())) {
                positions.put(b.receiver(), "OFF");
            }
            fired.add(b.label());
            snapshotsByIndex.add(snapshotOf(positions, fired));
        }

        Map<Integer, BlockingSnapshot> phi = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : order.entrySet()) {
            int k = e.getValue();
            int idx = Math.min(k, snapshotsByIndex.size() - 1);
            phi.put(e.getKey(), snapshotsByIndex.get(idx));
        }

        List<Map.Entry<Integer, Integer>> ordered = new ArrayList<>(order.entrySet());
        ordered.sort(Map.Entry.comparingByValue());
        Map<BlockingSnapshot, Integer> psi = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : ordered) {
            BlockingSnapshot sn = phi.get(e.getKey());
            psi.putIfAbsent(sn, e.getKey());
        }

        BlockingPlan plan = new BlockingPlan(phi, scene);
        return new BlockingMorphism(phi, psi, ss, plan);
    }

    /** Verify phi(psi(sn)) == sn for all snapshots in the image of psi. */
    public static boolean roundTripBlocking(BlockingMorphism m) {
        for (Map.Entry<BlockingSnapshot, Integer> e : m.psi().entrySet()) {
            if (!m.phi().get(e.getValue()).equals(e.getKey())) {
                return false;
            }
        }
        return true;
    }

    /** phi lookup. */
    public static BlockingSnapshot classifyState(BlockingMorphism m, int state) {
        BlockingSnapshot sn = m.phi().get(state);
        if (sn == null) {
            throw new IllegalArgumentException("unknown state: " + state);
        }
        return sn;
    }

    // -----------------------------------------------------------------------
    // Safety / cue verification
    // -----------------------------------------------------------------------

    /** Detect blocking collisions (two actors scripted into same position). */
    public static List<String> detectCollisions(Scene scene) {
        Map<String, String> positions = new LinkedHashMap<>();
        for (String a : scene.actors()) {
            positions.put(a, "OFF");
        }
        List<String> errors = new ArrayList<>();
        List<Beat> beats = scene.beats();
        for (int i = 0; i < beats.size(); i++) {
            Beat b = beats.get(i);
            if (b.position() != null && positions.containsKey(b.receiver())) {
                String target = b.position().code();
                if (!target.equals("OFF")) {
                    for (Map.Entry<String, String> other : positions.entrySet()) {
                        if (!other.getKey().equals(b.receiver())
                                && other.getValue().equals(target)) {
                            errors.add(
                                    "beat " + i + " (" + b.label() + "): "
                                            + b.receiver() + " moving to " + target
                                            + " but " + other.getKey() + " is already there");
                        }
                    }
                }
                positions.put(b.receiver(), target);
            }
            if (b.cueKind() == CueKind.EXIT && positions.containsKey(b.receiver())) {
                positions.put(b.receiver(), "OFF");
            }
        }
        return errors;
    }

    /** Result of comparing a scripted cue list against an expected cue sheet. */
    public record CueSheetDiff(List<String> missing, List<String> extra) {
        public CueSheetDiff {
            missing = List.copyOf(missing);
            extra = List.copyOf(extra);
        }
    }

    /** Compare scripted cues to an expected cue sheet. */
    public static CueSheetDiff verifyCueSheet(Scene scene, List<String> expectedCues) {
        List<String> scripted = new ArrayList<>();
        for (Beat b : scene.beats()) {
            if (b.sender().equals(STAGE_MANAGER)) {
                scripted.add(b.label());
            }
        }
        List<String> missing = new ArrayList<>();
        for (String c : expectedCues) {
            if (!scripted.contains(c)) missing.add(c);
        }
        List<String> extra = new ArrayList<>();
        for (String c : scripted) {
            if (!expectedCues.contains(c)) extra.add(c);
        }
        return new CueSheetDiff(missing, extra);
    }

    // -----------------------------------------------------------------------
    // Running example: Macbeth I.i
    // -----------------------------------------------------------------------

    /** Opening scene of Macbeth: three witches on a heath. */
    public static Scene macbethWitchesScene() {
        List<String> actors = List.of("W1", "W2", "W3");
        List<Beat> beats = List.of(
                new Beat(STAGE_MANAGER, "W1", "thunder", CueKind.SOUND),
                new Beat(STAGE_MANAGER, "W1", "enter_DSL", CueKind.ENTRANCE, new Position("DSL")),
                new Beat(STAGE_MANAGER, "W2", "enter_DSC", CueKind.ENTRANCE, new Position("DSC")),
                new Beat(STAGE_MANAGER, "W3", "enter_DSR", CueKind.ENTRANCE, new Position("DSR")),
                new Beat("W1", STAGE_MANAGER, "when_shall_we_three", CueKind.LINE),
                new Beat("W2", STAGE_MANAGER, "when_hurlyburly", CueKind.LINE),
                new Beat("W3", STAGE_MANAGER, "ere_set_of_sun", CueKind.LINE),
                new Beat(STAGE_MANAGER, "W1", "exit_W1", CueKind.EXIT),
                new Beat(STAGE_MANAGER, "W2", "exit_W2", CueKind.EXIT),
                new Beat(STAGE_MANAGER, "W3", "fair_is_foul", CueKind.EXIT));
        return new Scene("Macbeth I.i", actors, beats);
    }
}
