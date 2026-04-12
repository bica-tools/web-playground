package com.bica.reborn.coproduct;

import com.bica.reborn.coproduct.CoproductResult.CoproductCandidate;
import com.bica.reborn.coproduct.CoproductResult.InjectionMap;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.reticular.ReticularChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;

/**
 * Coproducts in SessLat — categorical coproduct analysis.
 *
 * <p>Investigates whether the category SessLat has coproducts.  A coproduct of
 * L₁ and L₂ would be a lattice C with injections ι₁: L₁→C, ι₂: L₂→C
 * satisfying the universal property.
 *
 * <p><b>Main result</b>: SessLat does NOT have general coproducts.
 * The realizability constraint prevents arbitrary lattice coproducts.
 *
 * <p>Three candidate constructions are tried:
 * <ol>
 *   <li>Coalesced sum — identify bottoms, add fresh top</li>
 *   <li>Separated sum — fresh top and fresh bottom, factors side by side</li>
 *   <li>Linear sum — place L₁ above L₂ (ordinal sum)</li>
 * </ol>
 */
public final class CoproductChecker {

    private CoproductChecker() {}

    // =========================================================================
    // Construction functions
    // =========================================================================

    /**
     * Coalesced sum: identify bottoms, add fresh top.
     * Result has |L₁| + |L₂| - 1 + 1 states.
     */
    public static StateSpace coalescedSum(StateSpace left, StateSpace right) {
        int nextId = 0;
        Map<Integer, Integer> leftRemap = new TreeMap<>();
        Map<Integer, String> labels = new HashMap<>();

        for (int s : new TreeSet<>(left.states())) {
            leftRemap.put(s, nextId);
            labels.put(nextId, "L1:" + left.labels().getOrDefault(s, String.valueOf(s)));
            nextId++;
        }

        Map<Integer, Integer> rightRemap = new TreeMap<>();
        for (int s : new TreeSet<>(right.states())) {
            if (s == right.bottom()) {
                rightRemap.put(s, leftRemap.get(left.bottom()));
            } else {
                rightRemap.put(s, nextId);
                labels.put(nextId, "L2:" + right.labels().getOrDefault(s, String.valueOf(s)));
                nextId++;
            }
        }

        int freshTop = nextId;
        labels.put(freshTop, "T_coprod");

        Set<Integer> states = new HashSet<>();
        states.addAll(leftRemap.values());
        states.addAll(rightRemap.values());
        states.add(freshTop);

        List<Transition> transitions = new ArrayList<>();
        for (var t : left.transitions()) {
            transitions.add(new Transition(leftRemap.get(t.source()), t.label(),
                    leftRemap.get(t.target()), t.kind()));
        }
        for (var t : right.transitions()) {
            transitions.add(new Transition(rightRemap.get(t.source()), t.label(),
                    rightRemap.get(t.target()), t.kind()));
        }
        transitions.add(new Transition(freshTop, "i1", leftRemap.get(left.top())));
        transitions.add(new Transition(freshTop, "i2", rightRemap.get(right.top())));

        return new StateSpace(states, transitions, freshTop,
                leftRemap.get(left.bottom()), labels);
    }

    /**
     * Separated sum: L₁ and L₂ side by side, plus fresh top and bottom.
     * Result has |L₁| + |L₂| + 2 states.
     */
    public static StateSpace separatedSum(StateSpace left, StateSpace right) {
        int nextId = 0;
        Map<Integer, Integer> leftRemap = new TreeMap<>();
        Map<Integer, String> labels = new HashMap<>();

        for (int s : new TreeSet<>(left.states())) {
            leftRemap.put(s, nextId);
            labels.put(nextId, "L1:" + left.labels().getOrDefault(s, String.valueOf(s)));
            nextId++;
        }

        Map<Integer, Integer> rightRemap = new TreeMap<>();
        for (int s : new TreeSet<>(right.states())) {
            rightRemap.put(s, nextId);
            labels.put(nextId, "L2:" + right.labels().getOrDefault(s, String.valueOf(s)));
            nextId++;
        }

        int freshTop = nextId++;
        labels.put(freshTop, "T_coprod");
        int freshBottom = nextId;
        labels.put(freshBottom, "B_coprod");

        Set<Integer> states = new HashSet<>();
        states.addAll(leftRemap.values());
        states.addAll(rightRemap.values());
        states.add(freshTop);
        states.add(freshBottom);

        List<Transition> transitions = new ArrayList<>();
        for (var t : left.transitions()) {
            transitions.add(new Transition(leftRemap.get(t.source()), t.label(),
                    leftRemap.get(t.target()), t.kind()));
        }
        for (var t : right.transitions()) {
            transitions.add(new Transition(rightRemap.get(t.source()), t.label(),
                    rightRemap.get(t.target()), t.kind()));
        }
        transitions.add(new Transition(freshTop, "i1", leftRemap.get(left.top())));
        transitions.add(new Transition(freshTop, "i2", rightRemap.get(right.top())));
        transitions.add(new Transition(leftRemap.get(left.bottom()), "b1", freshBottom));
        transitions.add(new Transition(rightRemap.get(right.bottom()), "b2", freshBottom));

        return new StateSpace(states, transitions, freshTop, freshBottom, labels);
    }

    /**
     * Linear (ordinal) sum: L₁ stacked above L₂.
     * Identify L₁.bottom with L₂.top. NOT symmetric.
     * Result has |L₁| + |L₂| - 1 states.
     */
    public static StateSpace linearSum(StateSpace left, StateSpace right) {
        int nextId = 0;
        Map<Integer, Integer> leftRemap = new TreeMap<>();
        Map<Integer, String> labels = new HashMap<>();

        for (int s : new TreeSet<>(left.states())) {
            leftRemap.put(s, nextId);
            labels.put(nextId, "L1:" + left.labels().getOrDefault(s, String.valueOf(s)));
            nextId++;
        }

        Map<Integer, Integer> rightRemap = new TreeMap<>();
        for (int s : new TreeSet<>(right.states())) {
            if (s == right.top()) {
                rightRemap.put(s, leftRemap.get(left.bottom()));
                labels.put(leftRemap.get(left.bottom()), "L1B=L2T");
            } else {
                rightRemap.put(s, nextId);
                labels.put(nextId, "L2:" + right.labels().getOrDefault(s, String.valueOf(s)));
                nextId++;
            }
        }

        Set<Integer> states = new HashSet<>();
        states.addAll(leftRemap.values());
        states.addAll(rightRemap.values());

        List<Transition> transitions = new ArrayList<>();
        for (var t : left.transitions()) {
            transitions.add(new Transition(leftRemap.get(t.source()), t.label(),
                    leftRemap.get(t.target()), t.kind()));
        }
        for (var t : right.transitions()) {
            transitions.add(new Transition(rightRemap.get(t.source()), t.label(),
                    rightRemap.get(t.target()), t.kind()));
        }

        return new StateSpace(states, transitions,
                leftRemap.get(left.top()), rightRemap.get(right.bottom()), labels);
    }

    // =========================================================================
    // High-level API
    // =========================================================================

    /**
     * Check whether L₁ + L₂ exists as a coproduct in SessLat.
     *
     * <p>Tries all three candidate constructions, checks lattice property,
     * realizability, and injections.
     */
    public static CoproductResult checkCoproduct(StateSpace left, StateSpace right) {
        List<CoproductCandidate> candidates = new ArrayList<>();

        record BuildEntry(String name,
                          java.util.function.BiFunction<StateSpace, StateSpace, StateSpace> fn) {}

        var constructions = List.of(
                new BuildEntry("coalesced", CoproductChecker::coalescdSum),
                new BuildEntry("separated", CoproductChecker::separatedSum),
                new BuildEntry("linear", CoproductChecker::linearSum));

        for (var entry : constructions) {
            StateSpace candSs = entry.fn.apply(left, right);
            boolean isLat = LatticeChecker.checkLattice(candSs).isLattice();
            boolean realizable = ReticularChecker.isReticulate(candSs);

            Map<Integer, Integer> leftRemap = computeRemap(left, right, entry.name, true);
            Map<Integer, Integer> rightRemap = computeRemap(left, right, entry.name, false);

            List<InjectionMap> injections = buildInjections(
                    candSs, left, right, leftRemap, rightRemap);

            candidates.add(new CoproductCandidate(
                    candSs, injections, entry.name, isLat, realizable));
        }

        // Find best candidate (lattice + realizable)
        CoproductCandidate best = null;
        for (var cand : candidates) {
            if (cand.isLattice()) {
                best = cand;
                break;
            }
        }

        if (best != null) {
            return new CoproductResult(true, best, candidates, null);
        }

        // Build counterexample
        List<String> reasons = new ArrayList<>();
        for (var cand : candidates) {
            if (!cand.isLattice()) {
                reasons.add(cand.construction() + ": not a lattice");
            } else {
                reasons.add(cand.construction() + ": universal property fails");
            }
        }

        return new CoproductResult(false, null, candidates,
                String.join("; ", reasons));
    }

    /**
     * Build the injections for a candidate coproduct.
     */
    public static List<InjectionMap> injections(
            StateSpace candidate, StateSpace left, StateSpace right,
            Map<Integer, Integer> leftRemap, Map<Integer, Integer> rightRemap) {
        return buildInjections(candidate, left, right, leftRemap, rightRemap);
    }

    /**
     * Build a coproduct candidate using the specified construction.
     */
    public static StateSpace buildCoproduct(
            StateSpace left, StateSpace right, String construction) {
        return switch (construction) {
            case "coalesced" -> coalescdSum(left, right);
            case "separated" -> separatedSum(left, right);
            case "linear" -> linearSum(left, right);
            default -> throw new IllegalArgumentException("Unknown construction: " + construction);
        };
    }

    /**
     * Check the coproduct universal property (simplified: checks lattice + injection properties).
     */
    public static boolean checkCoproductUniversalProperty(
            StateSpace candidate, StateSpace left, StateSpace right,
            Map<Integer, Integer> leftRemap, Map<Integer, Integer> rightRemap) {
        if (!LatticeChecker.checkLattice(candidate).isLattice()) return false;
        List<InjectionMap> injs = buildInjections(candidate, left, right, leftRemap, rightRemap);
        return injs.stream().allMatch(i -> i.isInjective() && i.isOrderPreserving());
    }

    // =========================================================================
    // Internal helpers — wrapper to handle the naming mismatch
    // =========================================================================

    private static StateSpace coalescdSum(StateSpace left, StateSpace right) {
        return coalescedSum(left, right);
    }

    private static Map<Integer, Integer> computeRemap(
            StateSpace left, StateSpace right, String construction, boolean isLeft) {
        int nextId = 0;
        Map<Integer, Integer> leftRemap = new TreeMap<>();
        for (int s : new TreeSet<>(left.states())) {
            leftRemap.put(s, nextId++);
        }

        if (isLeft) return leftRemap;

        Map<Integer, Integer> rightRemap = new TreeMap<>();
        switch (construction) {
            case "coalesced":
                for (int s : new TreeSet<>(right.states())) {
                    if (s == right.bottom()) {
                        rightRemap.put(s, leftRemap.get(left.bottom()));
                    } else {
                        rightRemap.put(s, nextId++);
                    }
                }
                break;
            case "separated":
                for (int s : new TreeSet<>(right.states())) {
                    rightRemap.put(s, nextId++);
                }
                break;
            case "linear":
                for (int s : new TreeSet<>(right.states())) {
                    if (s == right.top()) {
                        rightRemap.put(s, leftRemap.get(left.bottom()));
                    } else {
                        rightRemap.put(s, nextId++);
                    }
                }
                break;
        }
        return rightRemap;
    }

    private static List<InjectionMap> buildInjections(
            StateSpace candidate, StateSpace left, StateSpace right,
            Map<Integer, Integer> leftRemap, Map<Integer, Integer> rightRemap) {
        List<InjectionMap> result = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            StateSpace factor = (i == 0) ? left : right;
            Map<Integer, Integer> remap = (i == 0) ? leftRemap : rightRemap;

            boolean injective = new HashSet<>(remap.values()).size() == remap.size();
            boolean orderPres = MorphismChecker.isOrderPreserving(factor, candidate, remap);

            result.add(new InjectionMap(i, remap, injective, orderPres));
        }
        return result;
    }
}
