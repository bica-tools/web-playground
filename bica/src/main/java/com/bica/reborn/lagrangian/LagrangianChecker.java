package com.bica.reborn.lagrangian;

import com.bica.reborn.protocol_engineering.LagrangianUtils;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Full Lagrangian mechanics analysis for session types (Step 60q).
 *
 * <p>Complete mechanical formulation of session type dynamics:
 * <pre>
 *   L(x) = T(x) - V(x)
 * </pre>
 * where T is kinetic energy (transition availability) and V is potential
 * energy (distance to termination).
 *
 * <p>Delegates low-level computations to {@link LagrangianUtils} where
 * appropriate, and adds full analysis, conservation laws, and max-action
 * path computation.
 *
 * <p>Faithful Java port of {@code reticulate.lagrangian}.
 */
public final class LagrangianChecker {

    private LagrangianChecker() {}

    // =========================================================================
    // Result types
    // =========================================================================

    /**
     * Mechanics of a single recursive cycle (SCC).
     *
     * <p>Mirrors Python {@code CircularMechanics} frozen dataclass.
     */
    public record CircularMechanics(
            int sccId,
            Set<Integer> states,
            int period,
            double angularVelocity,
            int inertia,
            double centripetalAccel,
            int internalBranches,
            int tangentialAccel,
            int escapeTransitions) {}

    /**
     * Complete Lagrangian mechanics analysis result.
     *
     * <p>Mirrors Python {@code LagrangianResult} frozen dataclass.
     */
    public record LagrangianResult(
            int numStates,
            Map<Integer, Double> kineticEnergy,
            Map<Integer, Double> potentialEnergy,
            Map<Integer, Double> lagrangian,
            double totalEnergy,
            List<Integer> leastActionPath,
            double leastActionValue,
            List<Integer> maxActionPath,
            double maxActionValue,
            List<CircularMechanics> circularMechanics,
            Map<Integer, Double> gravitationalField,
            Map<Integer, Integer> momentum) {

        public LagrangianResult {
            kineticEnergy = Map.copyOf(kineticEnergy);
            potentialEnergy = Map.copyOf(potentialEnergy);
            lagrangian = Map.copyOf(lagrangian);
            leastActionPath = List.copyOf(leastActionPath);
            maxActionPath = List.copyOf(maxActionPath);
            circularMechanics = List.copyOf(circularMechanics);
            gravitationalField = Map.copyOf(gravitationalField);
            momentum = Map.copyOf(momentum);
        }
    }

    // =========================================================================
    // Delegating energy functions
    // =========================================================================

    /** Kinetic energy T(x) = |outgoing transitions|^2 / 2. */
    public static Map<Integer, Double> kineticEnergy(StateSpace ss) {
        return LagrangianUtils.kineticEnergy(ss);
    }

    /** Potential energy V(x) = rank(x) = distance to bottom. */
    public static Map<Integer, Double> potentialEnergy(StateSpace ss) {
        return LagrangianUtils.potentialEnergy(ss);
    }

    /** Lagrangian L(x) = T(x) - V(x) at each state. */
    public static Map<Integer, Double> lagrangianField(StateSpace ss) {
        return LagrangianUtils.lagrangianField(ss);
    }

    /** Hamiltonian H(x) = T(x) + V(x) (total energy at each state). */
    public static Map<Integer, Double> hamiltonianField(StateSpace ss) {
        return LagrangianUtils.hamiltonianField(ss);
    }

    /** Total system energy: sum of H(x) over all states. */
    public static double totalEnergy(StateSpace ss) {
        return LagrangianUtils.totalEnergy(ss);
    }

    /** Gravitational pull toward end state. g(x) = 1/rank(x). */
    public static Map<Integer, Double> gravitationalField(StateSpace ss) {
        return LagrangianUtils.gravitationalField(ss);
    }

    /** Momentum p(x) = number of outgoing transitions. */
    public static Map<Integer, Integer> momentum(StateSpace ss) {
        return LagrangianUtils.momentum(ss);
    }

    // =========================================================================
    // Action and path functions
    // =========================================================================

    /** Compute the action S = sum of L(x) along a path. */
    public static double pathAction(StateSpace ss, List<Integer> path) {
        return LagrangianUtils.pathAction(ss, path);
    }

    /** Find the path from top to bottom that minimizes the action. */
    public static Map.Entry<List<Integer>, Double> leastActionPath(StateSpace ss) {
        return LagrangianUtils.leastActionPath(ss);
    }

    /** Find the path from top to bottom that maximizes the action. */
    public static Map.Entry<List<Integer>, Double> maxActionPath(StateSpace ss) {
        List<List<Integer>> paths = enumerateTopToBottomPaths(ss, 1000);
        if (paths.isEmpty()) {
            return Map.entry(List.of(ss.top()), 0.0);
        }

        List<Integer> bestPath = paths.get(0);
        double bestAction = pathAction(ss, bestPath);

        for (int i = 1; i < paths.size(); i++) {
            double a = pathAction(ss, paths.get(i));
            if (a > bestAction) {
                bestAction = a;
                bestPath = paths.get(i);
            }
        }
        return Map.entry(List.copyOf(bestPath), bestAction);
    }

    // -------------------------------------------------------------------------
    // Path enumeration (local copy — LagrangianUtils version is package-private)
    // -------------------------------------------------------------------------

    /** Enumerate all directed paths from top to bottom (bounded). */
    private static List<List<Integer>> enumerateTopToBottomPaths(StateSpace ss, int maxPaths) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            List<Integer> out = adj.get(t.source());
            if (!out.contains(t.target())) out.add(t.target());
        }
        List<List<Integer>> paths = new ArrayList<>();
        Deque<Integer> path = new ArrayDeque<>();
        path.add(ss.top());
        Set<Integer> visited = new HashSet<>();
        dfsPath(ss.top(), ss.bottom(), adj, path, visited, paths, maxPaths);
        return paths;
    }

    private static void dfsPath(int current, int bottom,
                                 Map<Integer, List<Integer>> adj,
                                 Deque<Integer> path, Set<Integer> visited,
                                 List<List<Integer>> paths, int maxPaths) {
        if (paths.size() >= maxPaths) return;
        if (current == bottom) {
            paths.add(new ArrayList<>(path));
            return;
        }
        if (!visited.add(current)) return;
        for (int nxt : adj.getOrDefault(current, List.of())) {
            path.addLast(nxt);
            dfsPath(nxt, bottom, adj, path, visited, paths, maxPaths);
            path.removeLast();
        }
        visited.remove(current);
    }

    // =========================================================================
    // Circular mechanics
    // =========================================================================

    /** Analyze the mechanics of each recursive cycle (SCC with size > 1). */
    public static List<CircularMechanics> analyzeCircular(StateSpace ss) {
        // Delegate to LagrangianUtils and convert record type
        List<LagrangianUtils.CircularMechanics> utilResults = LagrangianUtils.analyzeCircular(ss);
        List<CircularMechanics> results = new ArrayList<>();
        for (var ur : utilResults) {
            results.add(new CircularMechanics(
                    ur.sccId(),
                    ur.states(),
                    ur.period(),
                    ur.angularVelocity(),
                    ur.inertia(),
                    ur.centripetalAccel(),
                    ur.internalBranches(),
                    ur.tangentialAccel(),
                    ur.escapeTransitions()));
        }
        return results;
    }

    // =========================================================================
    // Conservation laws
    // =========================================================================

    /**
     * Check energy conservation along a path.
     *
     * <p>Returns the Hamiltonian H(x) at each step. In a deterministic
     * (non-branching) path, H should be approximately constant.
     */
    public static List<Double> checkEnergyConservation(StateSpace ss, List<Integer> path) {
        Map<Integer, Double> H = hamiltonianField(ss);
        List<Double> energies = new ArrayList<>();
        for (int s : path) {
            energies.add(H.getOrDefault(s, 0.0));
        }
        return energies;
    }

    /**
     * Check if energy is approximately conserved along a path.
     *
     * @param tolerance maximum allowed difference between max and min energy
     */
    public static boolean isEnergyConserved(StateSpace ss, List<Integer> path, double tolerance) {
        List<Double> energies = checkEnergyConservation(ss, path);
        if (energies.size() < 2) return true;
        double maxE = energies.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double minE = energies.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        return (maxE - minE) <= tolerance;
    }

    /** Overload with default tolerance = 0.5. */
    public static boolean isEnergyConserved(StateSpace ss, List<Integer> path) {
        return isEnergyConserved(ss, path, 0.5);
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /** Complete Lagrangian mechanics analysis. */
    public static LagrangianResult analyzeLagrangian(StateSpace ss) {
        Map<Integer, Double> T = kineticEnergy(ss);
        Map<Integer, Double> V = potentialEnergy(ss);
        Map<Integer, Double> L = lagrangianField(ss);
        double E = totalEnergy(ss);
        var lap = leastActionPath(ss);
        var map = maxActionPath(ss);
        List<CircularMechanics> circ = analyzeCircular(ss);
        Map<Integer, Double> gf = gravitationalField(ss);
        Map<Integer, Integer> p = momentum(ss);

        return new LagrangianResult(
                ss.states().size(),
                T, V, L, E,
                lap.getKey(), lap.getValue(),
                map.getKey(), map.getValue(),
                circ, gf, p);
    }
}
