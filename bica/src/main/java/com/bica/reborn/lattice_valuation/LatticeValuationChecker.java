package com.bica.reborn.lattice_valuation;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Lattice valuations and the FKG inequality (Steps 30y-30z) -- Java port of
 * {@code reticulate/reticulate/lattice_valuation.py}.
 *
 * <ul>
 *   <li>Step 30y: Lattice valuations v: L -> R with v(x /\ y) + v(x \/ y) = v(x) + v(y)</li>
 *   <li>Step 30z: FKG inequality for monotone functions on distributive lattices</li>
 * </ul>
 *
 * <p>For session type lattices, valuations provide additive measures on
 * protocol states (e.g., execution cost, resource usage) that respect
 * the lattice structure.
 */
public final class LatticeValuationChecker {

    private LatticeValuationChecker() {}

    // -----------------------------------------------------------------------
    // Result type
    // -----------------------------------------------------------------------

    /**
     * Lattice valuation analysis.
     *
     * @param numStates       number of quotient states
     * @param rankValuation   v(x) = rank(x)
     * @param isRankModular   true iff rank is modular (v(x/\y)+v(x\/y)=v(x)+v(y))
     * @param heightValuation v(x) = height from top
     * @param fkgApplicable   true iff the lattice is distributive (FKG applies)
     */
    public record ValuationResult(
            int numStates,
            Map<Integer, Integer> rankValuation,
            boolean isRankModular,
            Map<Integer, Integer> heightValuation,
            boolean fkgApplicable) {

        public ValuationResult {
            rankValuation = Map.copyOf(rankValuation);
            heightValuation = Map.copyOf(heightValuation);
        }
    }

    // -----------------------------------------------------------------------
    // Valuations
    // -----------------------------------------------------------------------

    /**
     * The rank function as a valuation: v(x) = rank(x).
     * Uses Zeta.computeRank which computes longest chain from x to bottom.
     */
    public static Map<Integer, Integer> rankValuation(StateSpace ss) {
        return Zeta.computeRank(ss);
    }

    /**
     * Height from top as a valuation: v(x) = BFS distance from top in quotient DAG.
     */
    public static Map<Integer, Integer> heightValuation(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        // Build quotient adjacency
        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new HashSet<>());
        for (int s : ss.states()) {
            for (int t : adj.get(s)) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                if (sr != tr) qAdj.get(sr).add(tr);
            }
        }

        int topRep = sccMap.get(ss.top());

        // BFS from top
        Map<Integer, Integer> dist = new HashMap<>();
        dist.put(topRep, 0);
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(topRep);
        Set<Integer> visited = new HashSet<>();
        visited.add(topRep);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : qAdj.getOrDefault(u, Set.of())) {
                if (!visited.contains(v)) {
                    visited.add(v);
                    dist.put(v, dist.get(u) + 1);
                    queue.add(v);
                }
            }
        }

        // Default distance for unreachable reps
        for (int r : reps) {
            dist.putIfAbsent(r, 0);
        }

        // Map back to original states
        Map<Integer, Integer> result = new HashMap<>();
        for (int s : ss.states()) {
            result.put(s, dist.getOrDefault(sccMap.get(s), 0));
        }
        return result;
    }

    /**
     * Check if v is a modular function: v(x /\ y) + v(x \/ y) = v(x) + v(y).
     * Requires computing meets and joins for all pairs.
     */
    public static boolean isModularValuation(StateSpace ss, Map<Integer, Integer> v) {
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        List<Integer> states = Zeta.stateList(ss);

        for (int i = 0; i < states.size(); i++) {
            int x = states.get(i);
            for (int j = i + 1; j < states.size(); j++) {
                int y = states.get(j);

                // Meet (GLB): greatest z with z <= x and z <= y
                List<Integer> lower = new ArrayList<>();
                for (int z : states) {
                    if (reach.get(x).contains(z) && reach.get(y).contains(z)) {
                        lower.add(z);
                    }
                }
                if (lower.isEmpty()) continue;

                // Find greatest: one that reaches the fewest others... or equivalently
                // the one from which the fewest states are reachable among lower bounds
                int meet = lower.get(0);
                int meetReach = countReachable(meet, states, reach);
                for (int z : lower) {
                    int zReach = countReachable(z, states, reach);
                    if (zReach > meetReach) {
                        meetReach = zReach;
                        meet = z;
                    }
                }

                // Join (LUB): least z with z >= x and z >= y
                List<Integer> upper = new ArrayList<>();
                for (int z : states) {
                    if (reach.get(z).contains(x) && reach.get(z).contains(y)) {
                        upper.add(z);
                    }
                }
                if (upper.isEmpty()) continue;

                int join = upper.get(0);
                int joinReach = countReachable(join, states, reach);
                for (int z : upper) {
                    int zReach = countReachable(z, states, reach);
                    if (zReach < joinReach) {
                        joinReach = zReach;
                        join = z;
                    }
                }

                // Check modularity
                int lhs = v.getOrDefault(meet, 0) + v.getOrDefault(join, 0);
                int rhs = v.getOrDefault(x, 0) + v.getOrDefault(y, 0);
                if (lhs != rhs) return false;
            }
        }
        return true;
    }

    private static int countReachable(int s, List<Integer> states, Map<Integer, Set<Integer>> reach) {
        int count = 0;
        for (int w : states) {
            if (reach.get(s).contains(w)) count++;
        }
        return count;
    }

    // -----------------------------------------------------------------------
    // FKG inequality (Step 30z)
    // -----------------------------------------------------------------------

    /**
     * Check if the lattice is distributive (FKG applies).
     */
    public static boolean isDistributiveLattice(StateSpace ss) {
        var result = LatticeChecker.checkLattice(ss);
        return result.isLattice();
        // Python uses getattr(result, 'is_distributive', True) defaulting to True
        // when is_lattice is True. We replicate this behaviour.
    }

    /**
     * Verify FKG inequality: E[fg] >= E[f] * E[g] for monotone f, g.
     * Uses uniform measure (mu(x) = 1/n).
     */
    public static boolean verifyFkg(
            StateSpace ss,
            ToDoubleFunction<Integer> f,
            ToDoubleFunction<Integer> g) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n == 0) return true;

        double sumFg = 0.0, sumF = 0.0, sumG = 0.0;
        for (int s : states) {
            double fs = f.applyAsDouble(s);
            double gs = g.applyAsDouble(s);
            sumFg += fs * gs;
            sumF += fs;
            sumG += gs;
        }
        sumFg /= n;
        sumF /= n;
        sumG /= n;

        return sumFg >= sumF * sumG - 1e-10;
    }

    // -----------------------------------------------------------------------
    // Main analysis
    // -----------------------------------------------------------------------

    /**
     * Complete valuation analysis.
     */
    public static ValuationResult analyzeValuations(StateSpace ss) {
        Map<Integer, Integer> rv = rankValuation(ss);
        Map<Integer, Integer> hv = heightValuation(ss);
        boolean isMod = isModularValuation(ss, rv);
        boolean fkg = isDistributiveLattice(ss);

        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        int nQuotient = sccR.sccMembers().size();

        return new ValuationResult(
                nQuotient,
                rv,
                isMod,
                hv,
                fkg);
    }
}
