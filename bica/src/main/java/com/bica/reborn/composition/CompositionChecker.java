package com.bica.reborn.composition;

import com.bica.reborn.ast.*;
import com.bica.reborn.globaltype.GlobalType;
import com.bica.reborn.globaltype.GlobalTypeChecker;
import com.bica.reborn.globaltype.GlobalTypeParser;
import com.bica.reborn.globaltype.Projection;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.morphism.Morphism;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.statespace.ProductStateSpace;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;

/**
 * Bottom-up multiparty composition via lattice products (Step 15 / Phase 18).
 *
 * <p>Two composition modes:
 * <ul>
 *   <li><b>Free product</b> ({@link #compose}): all interleavings (L₁ × L₂ × ... × Lₙ)</li>
 *   <li><b>Synchronized product</b> ({@link #synchronizedCompose}): CSP-style sync on shared labels</li>
 * </ul>
 *
 * <p>The hierarchy: L(G) ⊆ synchronized product ⊆ free product
 */
public final class CompositionChecker {

    private CompositionChecker() {}

    // =========================================================================
    // Free product composition
    // =========================================================================

    /**
     * Compose independent session types into a product lattice.
     *
     * <p>Each entry is a (name, session_type) pair. The function:
     * <ol>
     *   <li>Builds each state space and verifies it is a lattice.</li>
     *   <li>Builds the N-ary product via chained binary products.</li>
     *   <li>Checks pairwise compatibility.</li>
     * </ol>
     *
     * @throws IllegalArgumentException if empty or any participant is not a lattice
     */
    public static CompositionResult compose(List<Map.Entry<String, SessionType>> participants) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("need at least one participant");
        }

        var participantMap = new LinkedHashMap<String, SessionType>();
        var stateSpaceMap = new LinkedHashMap<String, StateSpace>();

        for (var entry : participants) {
            participantMap.put(entry.getKey(), entry.getValue());
            StateSpace ss = StateSpaceBuilder.build(entry.getValue());
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            if (!lr.isLattice()) {
                throw new IllegalArgumentException(
                        "participant '" + entry.getKey() + "' state space is not a lattice");
            }
            stateSpaceMap.put(entry.getKey(), ss);
        }

        // N-ary product
        List<StateSpace> ssList = participants.stream()
                .map(e -> stateSpaceMap.get(e.getKey()))
                .toList();
        StateSpace product = productNary(ssList);

        LatticeResult productLattice = LatticeChecker.checkLattice(product);

        // Pairwise compatibility
        var compat = new LinkedHashMap<ParticipantPair, Boolean>();
        List<String> names = participants.stream().map(Map.Entry::getKey).toList();
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                compat.put(
                        new ParticipantPair(names.get(i), names.get(j)),
                        checkCompatibility(
                                participantMap.get(names.get(i)),
                                participantMap.get(names.get(j))));
            }
        }

        return new CompositionResult(
                participantMap, stateSpaceMap, product,
                productLattice.isLattice(), compat);
    }

    // =========================================================================
    // Compatibility checking
    // =========================================================================

    /**
     * Check if two session types can interact as object/client pair.
     *
     * <p>Incompatible when:
     * <ul>
     *   <li>Both select the same label without either offering it</li>
     *   <li>Both offer and both select the same label (conflict)</li>
     * </ul>
     */
    public static boolean checkCompatibility(SessionType s1, SessionType s2) {
        var iface1 = extractInterface(s1);
        var iface2 = extractInterface(s2);

        Set<String> offered1 = iface1.get("offered");
        Set<String> selected1 = iface1.get("selected");
        Set<String> offered2 = iface2.get("offered");
        Set<String> selected2 = iface2.get("selected");

        // Both selecting without either offering → incompatible
        var bothSelect = new HashSet<>(selected1);
        bothSelect.retainAll(selected2);
        var bothOffered = new HashSet<>(offered1);
        bothOffered.addAll(offered2);
        var bothSelectNotOffered = new HashSet<>(bothSelect);
        bothSelectNotOffered.removeAll(bothOffered);
        if (!bothSelectNotOffered.isEmpty()) {
            return false;
        }

        // Label offered by both AND selected by both → conflict
        var offeredBoth = new HashSet<>(offered1);
        offeredBoth.retainAll(offered2);
        var conflict = new HashSet<>(offeredBoth);
        conflict.retainAll(bothSelect);
        if (!conflict.isEmpty()) {
            return false;
        }

        return true;
    }

    // =========================================================================
    // N-ary product
    // =========================================================================

    /**
     * N-ary product via left-fold of binary products.
     *
     * @throws IllegalArgumentException if list is empty
     */
    public static StateSpace productNary(List<StateSpace> stateSpaces) {
        if (stateSpaces.isEmpty()) {
            throw new IllegalArgumentException("need at least one state space for product");
        }
        StateSpace result = stateSpaces.getFirst();
        for (int i = 1; i < stateSpaces.size(); i++) {
            result = ProductStateSpace.product(result, stateSpaces.get(i));
        }
        return result;
    }

    // =========================================================================
    // Synchronized product
    // =========================================================================

    /**
     * Build the synchronized product of two state spaces.
     *
     * <p>CSP-style parallel composition:
     * <ul>
     *   <li>Private label: advance one component only</li>
     *   <li>Shared label: both components must move simultaneously</li>
     * </ul>
     *
     * <p>BFS from (top, top) — only reachable states are included.
     */
    public static StateSpace synchronizedProduct(StateSpace left, StateSpace right) {
        Set<String> shared = sharedLabels(left, right);

        // Pre-compute adjacency lists
        Map<Integer, List<Transition>> leftAdj = buildAdjacency(left);
        Map<Integer, List<Transition>> rightAdj = buildAdjacency(right);

        // BFS state mapping
        Map<Long, Integer> pairToId = new HashMap<>();
        Map<Integer, String> idLabels = new HashMap<>();
        List<Transition> transitions = new ArrayList<>();
        int[] nextId = {0};

        // Helper to get or create state ID for a pair
        class IdHelper {
            int getId(int s1, int s2) {
                long key = packPair(s1, s2);
                Integer existing = pairToId.get(key);
                if (existing != null) return existing;
                int sid = nextId[0]++;
                pairToId.put(key, sid);
                String l1 = left.labels().getOrDefault(s1, String.valueOf(s1));
                String l2 = right.labels().getOrDefault(s2, String.valueOf(s2));
                idLabels.put(sid, "(" + l1 + ", " + l2 + ")");
                return sid;
            }
        }
        var helper = new IdHelper();

        // BFS from (top, top)
        int startS1 = left.top(), startS2 = right.top();
        helper.getId(startS1, startS2);
        Deque<long[]> queue = new ArrayDeque<>();
        queue.add(new long[]{startS1, startS2});
        Set<Long> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            long[] pair = queue.poll();
            int s1 = (int) pair[0], s2 = (int) pair[1];
            long key = packPair(s1, s2);
            if (!visited.add(key)) continue;
            int src = pairToId.get(key);

            // Private left transitions (label not shared)
            for (var lt : leftAdj.getOrDefault(s1, List.of())) {
                if (!shared.contains(lt.label())) {
                    int tgt = helper.getId(lt.target(), s2);
                    transitions.add(new Transition(src, lt.label(), tgt, lt.kind()));
                    long tgtKey = packPair(lt.target(), s2);
                    if (!visited.contains(tgtKey)) {
                        queue.add(new long[]{lt.target(), s2});
                    }
                }
            }

            // Private right transitions (label not shared)
            for (var rt : rightAdj.getOrDefault(s2, List.of())) {
                if (!shared.contains(rt.label())) {
                    int tgt = helper.getId(s1, rt.target());
                    transitions.add(new Transition(src, rt.label(), tgt, rt.kind()));
                    long tgtKey = packPair(s1, rt.target());
                    if (!visited.contains(tgtKey)) {
                        queue.add(new long[]{s1, rt.target()});
                    }
                }
            }

            // Synchronized transitions (shared labels — both must enable)
            for (var lt : leftAdj.getOrDefault(s1, List.of())) {
                if (shared.contains(lt.label())) {
                    for (var rt : rightAdj.getOrDefault(s2, List.of())) {
                        if (rt.label().equals(lt.label())) {
                            int tgt = helper.getId(lt.target(), rt.target());
                            // Selection if either side is selecting
                            TransitionKind kind = (lt.kind() == TransitionKind.SELECTION
                                    || rt.kind() == TransitionKind.SELECTION)
                                    ? TransitionKind.SELECTION : TransitionKind.METHOD;
                            transitions.add(new Transition(src, lt.label(), tgt, kind));
                            long tgtKey = packPair(lt.target(), rt.target());
                            if (!visited.contains(tgtKey)) {
                                queue.add(new long[]{lt.target(), rt.target()});
                            }
                        }
                    }
                }
            }
        }

        int top = pairToId.get(packPair(left.top(), right.top()));
        long bottomKey = packPair(left.bottom(), right.bottom());
        int bottom;
        if (pairToId.containsKey(bottomKey)) {
            bottom = pairToId.get(bottomKey);
        } else {
            // Bottom unreachable — synchronization deadlock; allocate it anyway
            bottom = helper.getId(left.bottom(), right.bottom());
        }

        return new StateSpace(
                new HashSet<>(pairToId.values()),
                transitions, top, bottom, idLabels);
    }

    /**
     * Compose session types with synchronization on shared labels.
     *
     * @throws IllegalArgumentException if fewer than 2 participants or non-lattice state spaces
     */
    public static SynchronizedResult synchronizedCompose(
            List<Map.Entry<String, SessionType>> participants) {
        if (participants.size() < 2) {
            throw new IllegalArgumentException(
                    "need at least two participants for synchronized composition");
        }

        var participantMap = new LinkedHashMap<String, SessionType>();
        var stateSpaceMap = new LinkedHashMap<String, StateSpace>();

        for (var entry : participants) {
            participantMap.put(entry.getKey(), entry.getValue());
            StateSpace ss = StateSpaceBuilder.build(entry.getValue());
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            if (!lr.isLattice()) {
                throw new IllegalArgumentException(
                        "participant '" + entry.getKey() + "' state space is not a lattice");
            }
            stateSpaceMap.put(entry.getKey(), ss);
        }

        List<String> names = participants.stream().map(Map.Entry::getKey).toList();

        // Pairwise shared labels and compatibility
        var compat = new LinkedHashMap<ParticipantPair, Boolean>();
        var sharedMap = new LinkedHashMap<ParticipantPair, Set<String>>();
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                var pair = new ParticipantPair(names.get(i), names.get(j));
                compat.put(pair, checkCompatibility(
                        participantMap.get(names.get(i)),
                        participantMap.get(names.get(j))));
                sharedMap.put(pair, sharedLabels(
                        stateSpaceMap.get(names.get(i)),
                        stateSpaceMap.get(names.get(j))));
            }
        }

        // Build synchronized product via pairwise then chain
        List<StateSpace> ssList = names.stream()
                .map(stateSpaceMap::get)
                .toList();
        StateSpace synced = synchronizedProductNary(ssList);

        // Also build free product for comparison
        StateSpace free = productNary(ssList);

        LatticeResult syncedLattice = LatticeChecker.checkLattice(synced);

        double ratio = free.states().isEmpty() ? 1.0
                : (double) synced.states().size() / free.states().size();

        return new SynchronizedResult(
                participantMap, stateSpaceMap, synced, free,
                syncedLattice.isLattice(), compat, sharedMap, ratio);
    }

    // =========================================================================
    // Compare with global type
    // =========================================================================

    /**
     * Compare bottom-up composition with top-down projection.
     *
     * <ol>
     *   <li>Parse global type, build its state space (top-down).</li>
     *   <li>Project onto all roles, build product of projections.</li>
     *   <li>Check: L(G) embeds into product?</li>
     *   <li>Check: participant types ≅ projected types?</li>
     * </ol>
     */
    public static ComparisonResult compareWithGlobal(
            Map<String, SessionType> participants, String globalTypeString) {
        // Top-down: parse and build global state space
        GlobalType g = GlobalTypeParser.parse(globalTypeString);
        StateSpace globalSs = GlobalTypeChecker.buildStateSpace(g);
        LatticeResult globalLattice = LatticeChecker.checkLattice(globalSs);

        // Project onto all roles
        Map<String, SessionType> projections = Projection.projectAll(g);

        // Build state spaces for projections
        List<StateSpace> projectionSsList = new ArrayList<>();
        Map<String, StateSpace> projectionSsMap = new TreeMap<>();
        for (var entry : new TreeMap<>(projections).entrySet()) {
            StateSpace ss = StateSpaceBuilder.build(entry.getValue());
            projectionSsMap.put(entry.getKey(), ss);
            projectionSsList.add(ss);
        }

        // Bottom-up: N-ary product of projected local types
        StateSpace product = productNary(projectionSsList);
        LatticeResult productLattice = LatticeChecker.checkLattice(product);

        // Check embedding: L(G) embeds into product of projections
        Morphism embedding = MorphismChecker.findEmbedding(globalSs, product);
        boolean embeddingExists = embedding != null;

        // Check role type matches: participant types ≅ projected types
        Map<String, Boolean> roleMatches = new TreeMap<>();
        for (var entry : projections.entrySet()) {
            String role = entry.getKey();
            if (participants.containsKey(role)) {
                StateSpace participantSs = StateSpaceBuilder.build(participants.get(role));
                StateSpace projectedSs = projectionSsMap.get(role);
                Morphism iso = MorphismChecker.findIsomorphism(participantSs, projectedSs);
                roleMatches.put(role, iso != null);
            } else {
                roleMatches.put(role, false);
            }
        }

        double ratio = globalSs.states().isEmpty() ? 0.0
                : (double) product.states().size() / globalSs.states().size();

        return new ComparisonResult(
                globalTypeString,
                globalSs.states().size(),
                product.states().size(),
                globalLattice.isLattice(),
                productLattice.isLattice(),
                embeddingExists,
                ratio,
                roleMatches);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /** Find transition labels that appear in both state spaces. */
    static Set<String> sharedLabels(StateSpace left, StateSpace right) {
        Set<String> leftLabels = new HashSet<>();
        for (var t : left.transitions()) leftLabels.add(t.label());
        Set<String> rightLabels = new HashSet<>();
        for (var t : right.transitions()) rightLabels.add(t.label());
        leftLabels.retainAll(rightLabels);
        return leftLabels;
    }

    /** Extract offered (Branch) and selected (Select) method labels from a type. */
    static Map<String, Set<String>> extractInterface(SessionType s) {
        Set<String> offered = new HashSet<>();
        Set<String> selected = new HashSet<>();
        collectLabels(s, offered, selected, new HashSet<>());
        return Map.of("offered", offered, "selected", selected);
    }

    /** Recursively collect Branch/Select labels from the AST. */
    private static void collectLabels(
            SessionType s, Set<String> offered, Set<String> selected, Set<String> visited) {
        switch (s) {
            case Branch b -> {
                for (var c : b.choices()) {
                    offered.add(c.label());
                    collectLabels(c.body(), offered, selected, visited);
                }
            }
            case Select sel -> {
                for (var c : sel.choices()) {
                    selected.add(c.label());
                    collectLabels(c.body(), offered, selected, visited);
                }
            }
            case Parallel p -> {
                collectLabels(p.left(), offered, selected, visited);
                collectLabels(p.right(), offered, selected, visited);
            }
            case Sequence seq -> {
                collectLabels(seq.left(), offered, selected, visited);
                collectLabels(seq.right(), offered, selected, visited);
            }
            case Rec r -> {
                if (!visited.contains(r.var())) {
                    var newVisited = new HashSet<>(visited);
                    newVisited.add(r.var());
                    collectLabels(r.body(), offered, selected, newVisited);
                }
            }
            case Chain ch -> {
                // Chain is a labelled one-step method invocation — offered by the receiver
                offered.add(ch.method());
                collectLabels(ch.cont(), offered, selected, visited);
            }
            case End e -> {}
            case Var v -> {}
        }
    }

    /** N-ary synchronized product via left-fold. */
    private static StateSpace synchronizedProductNary(List<StateSpace> stateSpaces) {
        if (stateSpaces.isEmpty()) {
            throw new IllegalArgumentException("need at least one state space");
        }
        StateSpace result = stateSpaces.getFirst();
        for (int i = 1; i < stateSpaces.size(); i++) {
            result = synchronizedProduct(result, stateSpaces.get(i));
        }
        return result;
    }

    private static Map<Integer, List<Transition>> buildAdjacency(StateSpace ss) {
        Map<Integer, List<Transition>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t);
        }
        return adj;
    }

    private static long packPair(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }
}
