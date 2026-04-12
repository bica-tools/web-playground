package com.bica.reborn.realizability;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.reticular.ReticularChecker;
import com.bica.reborn.reticular.ReticularFormResult;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;

/**
 * Realizability checker (Step 156 port).
 *
 * <p>A finite bounded lattice L is realizable (L is isomorphic to L(S) for some session
 * type S) if and only if L has reticular form.
 */
public final class RealizabilityChecker {

    private RealizabilityChecker() {}

    /**
     * A specific reason why a state space is not realizable.
     */
    public record Obstruction(String kind, List<Integer> states, String detail) {
        public Obstruction {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(states);
            Objects.requireNonNull(detail);
            states = List.copyOf(states);
        }
    }

    /**
     * Necessary conditions for realizability.
     */
    public record Conditions(
            boolean isLattice,
            boolean isBounded,
            boolean isDeterministic,
            boolean allReachable,
            boolean allReachBottom,
            boolean hasReticularForm) {}

    // =========================================================================
    // Determinism check
    // =========================================================================

    /**
     * Check that no (state, label) pair maps to two different targets.
     */
    public static List<Obstruction> checkDeterminism(StateSpace ss) {
        List<Obstruction> obs = new ArrayList<>();
        Map<String, Set<Integer>> labelMap = new HashMap<>();
        for (var t : ss.transitions()) {
            String key = t.source() + ":" + t.label();
            labelMap.computeIfAbsent(key, k -> new HashSet<>()).add(t.target());
        }
        for (var entry : labelMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                String[] parts = entry.getKey().split(":");
                int src = Integer.parseInt(parts[0]);
                List<Integer> states = new ArrayList<>();
                states.add(src);
                states.addAll(entry.getValue());
                obs.add(new Obstruction("non_deterministic", states,
                        "State " + src + " has label '" + parts[1] + "' leading to " + entry.getValue()));
            }
        }
        return obs;
    }

    // =========================================================================
    // Realizability conditions
    // =========================================================================

    /**
     * Check all necessary conditions.
     */
    public static Conditions checkConditions(StateSpace ss) {
        Set<Integer> reachable = ss.reachableFrom(ss.top());
        boolean allReachable = reachable.containsAll(ss.states());

        // All states reach bottom (reverse BFS)
        Map<Integer, Set<Integer>> revAdj = new HashMap<>();
        for (int s : ss.states()) revAdj.put(s, new HashSet<>());
        for (var t : ss.transitions()) {
            revAdj.computeIfAbsent(t.target(), k -> new HashSet<>()).add(t.source());
        }
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(ss.bottom());
        while (!stack.isEmpty()) {
            int s = stack.pop();
            if (visited.add(s)) {
                for (int pred : revAdj.getOrDefault(s, Set.of())) stack.push(pred);
            }
        }
        boolean allReachBottom = visited.containsAll(ss.states());

        boolean isDeterministic = checkDeterminism(ss).isEmpty();
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        ReticularFormResult rr = ReticularChecker.checkReticularForm(ss);

        return new Conditions(
                lr.isLattice(),
                lr.hasTop() && lr.hasBottom(),
                isDeterministic,
                allReachable,
                allReachBottom,
                rr.isReticulate());
    }

    // =========================================================================
    // Full realizability check
    // =========================================================================

    /**
     * Check if a state space is realizable as a session type.
     */
    public static boolean isRealizable(StateSpace ss) {
        return checkRealizability(ss).isRealizable();
    }

    /**
     * Full realizability pipeline.
     */
    public static RealizabilityResult checkRealizability(StateSpace ss) {
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        ReticularFormResult rr = ReticularChecker.checkReticularForm(ss);
        Conditions conds = checkConditions(ss);

        boolean realizable = conds.isLattice() && conds.isBounded() && conds.hasReticularForm();

        String reconstructedType = null;
        if (realizable) {
            try {
                var ast = ReticularChecker.reconstruct(ss);
                reconstructedType = PrettyPrinter.pretty(ast);
            } catch (Exception e) {
                realizable = false;
            }
        }

        List<Obstruction> obs = realizable ? List.of() : findObstructions(ss);

        return new RealizabilityResult(realizable, obs, conds, reconstructedType, lr, rr);
    }

    /**
     * Synthesize a session type from a state space (if realizable).
     *
     * @throws IllegalArgumentException if the state space is not realizable.
     */
    public static com.bica.reborn.ast.SessionType synthesize(StateSpace ss) {
        if (!isRealizable(ss)) {
            throw new IllegalArgumentException("State space is not realizable");
        }
        return ReticularChecker.reconstruct(ss);
    }

    // =========================================================================
    // Obstruction collection
    // =========================================================================

    private static List<Obstruction> findObstructions(StateSpace ss) {
        List<Obstruction> obs = new ArrayList<>();

        // Determinism
        obs.addAll(checkDeterminism(ss));

        // Reachability
        Set<Integer> reachable = ss.reachableFrom(ss.top());
        Set<Integer> unreachable = new HashSet<>(ss.states());
        unreachable.removeAll(reachable);
        if (!unreachable.isEmpty()) {
            obs.add(new Obstruction("unreachable_states",
                    new ArrayList<>(unreachable),
                    "States " + unreachable + " not reachable from top"));
        }

        // Lattice
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) {
            obs.add(new Obstruction("not_lattice", List.of(),
                    "State space is not a lattice"));
        }

        // Bottom transitions
        List<String> bottomLabels = new ArrayList<>();
        for (var t : ss.transitions()) {
            if (t.source() == ss.bottom()) bottomLabels.add(t.label());
        }
        if (!bottomLabels.isEmpty()) {
            obs.add(new Obstruction("bottom_has_transitions", List.of(ss.bottom()),
                    "Bottom has transitions: " + bottomLabels));
        }

        return obs;
    }
}
