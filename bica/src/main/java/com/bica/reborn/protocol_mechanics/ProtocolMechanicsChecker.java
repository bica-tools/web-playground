package com.bica.reborn.protocol_mechanics;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Protocol mechanics: the physics of session types (Step 60p).
 *
 * <p>The fundamental correspondence between session type structure and
 * classical mechanics:
 * <ul>
 *   <li>Position &harr; State (where you are)</li>
 *   <li>Velocity &harr; Transition (how you move)</li>
 *   <li>Acceleration &harr; Branch point (what changes the movement)</li>
 *   <li>Jerk &harr; Nested branch (what changes the acceleration)</li>
 *   <li>k-th derivative &harr; k-nested branch (k-deep tree structure)</li>
 *   <li>Dimension &harr; Parallel (&parallel;) (independent degrees of freedom)</li>
 *   <li>Period &harr; Recursion (&mu;X) (oscillation / periodic orbit)</li>
 * </ul>
 *
 * <p>Faithful Java port of {@code reticulate.protocol_mechanics}.
 */
public final class ProtocolMechanicsChecker {

    private ProtocolMechanicsChecker() {}

    // =========================================================================
    // Result types
    // =========================================================================

    /** Complete protocol mechanics analysis result. */
    public record MechanicsResult(
            int numStates,
            int numTransitions,
            int numBranchPoints,
            int maxBranchOrder,
            int parallelDimensions,
            boolean hasRecursion,
            double smoothness,
            int energy,
            Map<Integer, List<String>> velocityField,
            Map<Integer, Integer> accelerationField) {}

    // =========================================================================
    // Discrete calculus on lattices
    // =========================================================================

    /**
     * Discrete derivative: &Delta;f(x) = &Sigma;_{y: x covers y} (f(x) - f(y)).
     *
     * <p>Discrete analogue of df/dt: how much does f change
     * when descending one step in the lattice.
     */
    public static Map<Integer, Double> discreteDerivative(StateSpace ss, Map<Integer, Double> f) {
        List<Zeta.Pair> covers = Zeta.coveringRelation(ss);
        Map<Integer, List<Integer>> adjDown = new HashMap<>();
        for (int s : ss.states()) adjDown.put(s, new ArrayList<>());
        for (Zeta.Pair c : covers) adjDown.get(c.x()).add(c.y());

        Map<Integer, Double> df = new HashMap<>();
        for (int x : ss.states()) {
            List<Integer> children = adjDown.get(x);
            if (children.isEmpty()) {
                df.put(x, 0.0);
            } else {
                double sum = 0.0;
                for (int y : children) {
                    sum += f.getOrDefault(x, 0.0) - f.getOrDefault(y, 0.0);
                }
                df.put(x, sum);
            }
        }
        return df;
    }

    /**
     * Discrete second derivative: &Delta;&sup2;f = &Delta;(&Delta;f).
     *
     * <p>Measures the "acceleration" &mdash; how rapidly the rate of change itself changes.
     */
    public static Map<Integer, Double> discreteSecondDerivative(StateSpace ss, Map<Integer, Double> f) {
        Map<Integer, Double> df = discreteDerivative(ss, f);
        return discreteDerivative(ss, df);
    }

    /**
     * Discrete integral: F(x) = &Sigma;_{y &le; x} f(y).
     *
     * <p>Summation over the downset of x.
     */
    public static Map<Integer, Double> discreteIntegral(StateSpace ss, Map<Integer, Double> f) {
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        Map<Integer, Double> F = new HashMap<>();
        for (int x : ss.states()) {
            double sum = 0.0;
            for (int y : reach.get(x)) {
                sum += f.getOrDefault(y, 0.0);
            }
            F.put(x, sum);
        }
        return F;
    }

    /**
     * M&ouml;bius inversion: recover f from F = &Sigma;_{y&le;x} f(y).
     *
     * <p>f(x) = &Sigma;_{y &le; x} &mu;(x, y) &middot; F(y).
     */
    public static Map<Integer, Double> mobiusInversion(StateSpace ss, Map<Integer, Double> F) {
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);

        Map<Integer, Double> f = new HashMap<>();
        for (int x : ss.states()) {
            double val = 0.0;
            for (int y : ss.states()) {
                if (reach.get(x).contains(y)) {
                    val += mu.getOrDefault(new Zeta.Pair(x, y), 0) * F.getOrDefault(y, 0.0);
                }
            }
            f.put(x, val);
        }
        return f;
    }

    // =========================================================================
    // Velocity field (transitions as velocity vectors)
    // =========================================================================

    /**
     * For each state, the set of available transitions (velocity vectors).
     */
    public static Map<Integer, List<String>> velocityField(StateSpace ss) {
        Map<Integer, List<String>> vf = new HashMap<>();
        for (int s : ss.states()) vf.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            List<String> labels = vf.get(t.source());
            if (!labels.contains(t.label())) {
                labels.add(t.label());
            }
        }
        return vf;
    }

    /**
     * The "speed" at each state = number of available transitions.
     */
    public static Map<Integer, Integer> velocityMagnitude(StateSpace ss) {
        Map<Integer, List<String>> vf = velocityField(ss);
        Map<Integer, Integer> vm = new HashMap<>();
        for (var entry : vf.entrySet()) {
            vm.put(entry.getKey(), entry.getValue().size());
        }
        return vm;
    }

    // =========================================================================
    // Acceleration field (branch points)
    // =========================================================================

    /**
     * For each state, the branch degree (acceleration magnitude).
     * acceleration = 0 for sequential states, &gt;0 for branch points.
     */
    public static Map<Integer, Integer> accelerationField(StateSpace ss) {
        Map<Integer, Integer> vm = velocityMagnitude(ss);
        Map<Integer, Integer> af = new HashMap<>();
        for (var entry : vm.entrySet()) {
            af.put(entry.getKey(), Math.max(0, entry.getValue() - 1));
        }
        return af;
    }

    /**
     * States where acceleration is non-zero (trajectory changes direction).
     */
    public static List<Integer> branchPoints(StateSpace ss) {
        Map<Integer, Integer> af = accelerationField(ss);
        List<Integer> bp = new ArrayList<>();
        for (var entry : af.entrySet()) {
            if (entry.getValue() > 0) bp.add(entry.getKey());
        }
        Collections.sort(bp);
        return bp;
    }

    /**
     * Maximum nesting depth of branch points.
     *
     * <p>Corresponds to the highest-order derivative that is non-zero.
     */
    public static int maxBranchOrder(StateSpace ss) {
        // Count outgoing transitions per state
        Map<Integer, Integer> outCount = new HashMap<>();
        Map<Integer, Set<Integer>> outTargets = new HashMap<>();
        for (int s : ss.states()) {
            outCount.put(s, 0);
            outTargets.put(s, new HashSet<>());
        }
        for (var t : ss.transitions()) {
            outCount.merge(t.source(), 1, Integer::sum);
            outTargets.get(t.source()).add(t.target());
        }

        int[] maxOrder = {0};
        Set<Integer> visited = new HashSet<>();
        dfsOrder(ss.top(), 0, visited, outCount, outTargets, maxOrder);
        return maxOrder[0];
    }

    private static void dfsOrder(int s, int currentOrder, Set<Integer> visited,
                                  Map<Integer, Integer> outCount,
                                  Map<Integer, Set<Integer>> outTargets,
                                  int[] maxOrder) {
        if (!visited.add(s)) return;
        boolean isBranch = outCount.get(s) > 1;
        int newOrder = currentOrder + (isBranch ? 1 : 0);
        maxOrder[0] = Math.max(maxOrder[0], newOrder);
        for (int c : outTargets.get(s)) {
            dfsOrder(c, newOrder, visited, outCount, outTargets, maxOrder);
        }
    }

    // =========================================================================
    // Parallel dimensions
    // =========================================================================

    /**
     * Number of independent parallel dimensions (approximated by width).
     */
    public static int parallelDimensions(StateSpace ss) {
        int w = Zeta.computeWidth(ss);
        return Math.max(1, w);
    }

    // =========================================================================
    // Recursion period
    // =========================================================================

    /**
     * Check if the protocol has recursion (periodic orbits).
     */
    public static boolean hasPeriodicOrbit(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        for (var members : sccR.sccMembers().values()) {
            if (members.size() > 1) return true;
        }
        return false;
    }

    // =========================================================================
    // Smoothness
    // =========================================================================

    /**
     * Smoothness measure: mean |&Delta;&sup2;f| for the rank function.
     *
     * <p>Low smoothness = high acceleration = sharp turns in the protocol.
     * High smoothness = low acceleration = gentle trajectory.
     */
    public static double smoothness(StateSpace ss) {
        Map<Integer, Integer> rank = Zeta.computeRank(ss);
        Map<Integer, Double> f = new HashMap<>();
        for (var entry : rank.entrySet()) f.put(entry.getKey(), entry.getValue().doubleValue());

        Map<Integer, Double> d2f = discreteSecondDerivative(ss, f);
        if (d2f.isEmpty()) return 1.0;

        double sumAbs = 0.0;
        for (double v : d2f.values()) sumAbs += Math.abs(v);
        double meanAbs = sumAbs / d2f.size();
        return 1.0 / (1.0 + meanAbs);
    }

    // =========================================================================
    // Energy (Euler characteristic)
    // =========================================================================

    /**
     * Protocol energy = Euler characteristic of the order complex.
     *
     * <p>Computed inline: enumerate chains in the proper part (excluding top and bottom),
     * build f-vector, then &chi; = &Sigma;_{k&ge;0} (-1)^k f_k.
     */
    public static int protocolEnergy(StateSpace ss) {
        // Enumerate chains in proper part (excluding top and bottom)
        List<Integer> proper = new ArrayList<>();
        for (int s : ss.states()) {
            if (s != ss.top() && s != ss.bottom()) proper.add(s);
        }
        Collections.sort(proper);

        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();

        // Enumerate all chains (subsets where all pairs are comparable)
        // For small state spaces this is feasible
        int n = proper.size();
        if (n == 0) {
            // Only top and bottom → f-vector = [1, 0], chi = 0
            return ss.states().size() == 1 ? 1 : 0;
        }

        // f-vector: fv[k] = number of chains of length k (0-indexed, k=0 is empty chain)
        int[] fv = new int[n + 1];
        fv[0] = 1; // empty face

        // Enumerate via DFS on subsets up to reasonable size
        if (n <= 20) {
            for (int mask = 1; mask < (1 << n); mask++) {
                List<Integer> subset = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    if ((mask & (1 << i)) != 0) subset.add(proper.get(i));
                }
                // Check if subset forms a chain (all pairs comparable)
                boolean isChain = true;
                for (int i = 0; i < subset.size() && isChain; i++) {
                    for (int j = i + 1; j < subset.size() && isChain; j++) {
                        int a = subset.get(i), b = subset.get(j);
                        if (!reach.get(a).contains(b) && !reach.get(b).contains(a)) {
                            isChain = false;
                        }
                        if (isChain && sccMap.get(a).intValue() == sccMap.get(b).intValue() && a != b) {
                            isChain = false;
                        }
                    }
                }
                if (isChain) {
                    fv[subset.size()]++;
                }
            }
        } else {
            // For large state spaces, approximate: chi = V - E where V = |proper|, E = |covers in proper|
            List<Zeta.Pair> covers = Zeta.coveringRelation(ss);
            int edges = 0;
            Set<Integer> properSet = new HashSet<>(proper);
            for (Zeta.Pair c : covers) {
                if (properSet.contains(c.x()) && properSet.contains(c.y())) edges++;
            }
            return proper.size() - edges;
        }

        // chi = sum_{k>=0} (-1)^k * fv[k+1]  (fv[k+1] = f_k where f_0 = vertices)
        int chi = 0;
        for (int k = 1; k <= n; k++) {
            chi += ((k - 1) % 2 == 0 ? 1 : -1) * fv[k];
        }
        return chi;
    }

    // =========================================================================
    // Main analysis
    // =========================================================================

    /** Complete protocol mechanics analysis. */
    public static MechanicsResult analyzeMechanics(StateSpace ss) {
        Map<Integer, List<String>> vf = velocityField(ss);
        Map<Integer, Integer> af = accelerationField(ss);
        List<Integer> bp = branchPoints(ss);
        int mbo = maxBranchOrder(ss);
        int pd = parallelDimensions(ss);
        boolean rec = hasPeriodicOrbit(ss);
        double smooth = smoothness(ss);
        int energy = protocolEnergy(ss);

        return new MechanicsResult(
                ss.states().size(),
                ss.transitions().size(),
                bp.size(),
                mbo,
                pd,
                rec,
                smooth,
                energy,
                Map.copyOf(vf),
                Map.copyOf(af));
    }
}
