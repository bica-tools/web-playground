package com.bica.reborn.protocol_engineering;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Lagrangian mechanics utilities for session types (Step 60q).
 *
 * <p>Complete mechanical formulation: L(x) = T(x) - V(x) where
 * T is kinetic energy (transition availability) and V is potential
 * energy (distance to termination).
 *
 * <p>Faithful Java port of {@code reticulate.lagrangian}.
 */
public final class LagrangianUtils {

    private LagrangianUtils() {}

    /** Mechanics of a single recursive cycle (SCC). */
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

    // =========================================================================
    // Energy functions
    // =========================================================================

    /** Kinetic energy T(x) = |outgoing transitions|^2 / 2. */
    public static Map<Integer, Double> kineticEnergy(StateSpace ss) {
        Map<Integer, Integer> outCount = new HashMap<>();
        for (int s : ss.states()) outCount.put(s, 0);
        for (var t : ss.transitions()) outCount.merge(t.source(), 1, Integer::sum);
        Map<Integer, Double> T = new HashMap<>();
        for (var e : outCount.entrySet()) {
            int c = e.getValue();
            T.put(e.getKey(), (c * c) / 2.0);
        }
        return T;
    }

    /** Potential energy V(x) = rank(x) = distance to bottom. */
    public static Map<Integer, Double> potentialEnergy(StateSpace ss) {
        Map<Integer, Integer> rank = Zeta.computeRank(ss);
        Map<Integer, Double> V = new HashMap<>();
        for (var e : rank.entrySet()) V.put(e.getKey(), e.getValue().doubleValue());
        return V;
    }

    /** Lagrangian L(x) = T(x) - V(x) at each state. */
    public static Map<Integer, Double> lagrangianField(StateSpace ss) {
        Map<Integer, Double> T = kineticEnergy(ss);
        Map<Integer, Double> V = potentialEnergy(ss);
        Map<Integer, Double> L = new HashMap<>();
        for (int s : ss.states()) L.put(s, T.get(s) - V.get(s));
        return L;
    }

    /** Hamiltonian H(x) = T(x) + V(x) (total energy at each state). */
    public static Map<Integer, Double> hamiltonianField(StateSpace ss) {
        Map<Integer, Double> T = kineticEnergy(ss);
        Map<Integer, Double> V = potentialEnergy(ss);
        Map<Integer, Double> H = new HashMap<>();
        for (int s : ss.states()) H.put(s, T.get(s) + V.get(s));
        return H;
    }

    /** Total system energy: sum of H(x) over all states. */
    public static double totalEnergy(StateSpace ss) {
        Map<Integer, Double> H = hamiltonianField(ss);
        return H.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    // =========================================================================
    // Gravitational field
    // =========================================================================

    /** Gravitational pull toward end state. g(x) = 1/rank(x). */
    public static Map<Integer, Double> gravitationalField(StateSpace ss) {
        Map<Integer, Integer> rank = Zeta.computeRank(ss);
        Map<Integer, Double> g = new HashMap<>();
        for (var e : rank.entrySet()) {
            g.put(e.getKey(), e.getValue() == 0 ? 100.0 : 1.0 / e.getValue());
        }
        return g;
    }

    // =========================================================================
    // Momentum
    // =========================================================================

    /** Momentum p(x) = number of outgoing transitions. */
    public static Map<Integer, Integer> momentum(StateSpace ss) {
        Map<Integer, Integer> p = new HashMap<>();
        for (int s : ss.states()) p.put(s, 0);
        for (var t : ss.transitions()) p.merge(t.source(), 1, Integer::sum);
        return p;
    }

    // =========================================================================
    // Action and least-action paths
    // =========================================================================

    /** Compute the action S = sum of L(x) along a path. */
    public static double pathAction(StateSpace ss, List<Integer> path) {
        Map<Integer, Double> L = lagrangianField(ss);
        return path.stream().mapToDouble(s -> L.getOrDefault(s, 0.0)).sum();
    }

    /** Enumerate all directed paths from top to bottom (bounded). */
    static List<List<Integer>> enumerateTopToBottomPaths(StateSpace ss, int maxPaths) {
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

    /** Find the path from top to bottom that minimizes the action. */
    public static Map.Entry<List<Integer>, Double> leastActionPath(StateSpace ss) {
        List<List<Integer>> paths = enumerateTopToBottomPaths(ss, 1000);
        if (paths.isEmpty()) return Map.entry(List.of(ss.top()), 0.0);

        Map<Integer, Double> L = lagrangianField(ss);
        List<Integer> bestPath = paths.get(0);
        double bestAction = pathAction(ss, bestPath);

        for (int i = 1; i < paths.size(); i++) {
            double a = pathAction(ss, paths.get(i));
            if (a < bestAction) {
                bestAction = a;
                bestPath = paths.get(i);
            }
        }
        return Map.entry(List.copyOf(bestPath), bestAction);
    }

    // =========================================================================
    // Circular mechanics (recursive cycles)
    // =========================================================================

    /** Analyze the mechanics of each recursive cycle (SCC with size > 1). */
    public static List<CircularMechanics> analyzeCircular(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);
        List<CircularMechanics> results = new ArrayList<>();

        for (var entry : sccR.sccMembers().entrySet()) {
            int rep = entry.getKey();
            Set<Integer> members = entry.getValue();
            if (members.size() <= 1) continue;

            int internalEdges = 0;
            int internalBranches = 0;
            int escapeCount = 0;

            for (int s : members) {
                int outInScc = 0, outOutside = 0;
                for (int t : adj.getOrDefault(s, List.of())) {
                    if (sccR.sccMap().get(t).intValue() == rep) outInScc++;
                    else outOutside++;
                }
                internalEdges += outInScc;
                escapeCount += outOutside;
                if (outInScc > 1) internalBranches++;
            }

            int period = Math.max(1, internalEdges);
            int inertia = members.size();
            double angularVel = 2 * Math.PI / period;
            double centripetal = (double)(period * period) / inertia;

            results.add(new CircularMechanics(
                    rep, Set.copyOf(members), period, angularVel, inertia,
                    centripetal, internalBranches, internalBranches, escapeCount));
        }
        return results;
    }
}
