package com.bica.reborn.lattice;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Lattice valuations and the FKG inequality for session type lattices.
 *
 * <p>Step 30y: A <b>valuation</b> on a lattice {@code L} is a function
 * {@code v: L → ℝ} satisfying
 * {@code v(x ∧ y) + v(x ∨ y) = v(x) + v(y)} for all {@code x, y ∈ L}.
 *
 * <p>Key results:
 * <ul>
 *   <li>The rank function is a valuation on modular lattices (Birkhoff 1935).</li>
 *   <li>Every valuation on a distributive lattice is determined by its values
 *       on join-irreducible elements.</li>
 *   <li>The FKG inequality holds on distributive lattices: monotone increasing
 *       functions are positively correlated under log-supermodular measures.</li>
 * </ul>
 *
 * <p>Ported from {@code reticulate/reticulate/valuation.py}.
 */
public final class Valuations {

    private static final double DEFAULT_TOLERANCE = 1e-10;

    private Valuations() {}

    // -- Result records --------------------------------------------------------

    /**
     * Result of checking the valuation identity for a function.
     *
     * @param isValuation     true iff {@code v(x∧y)+v(x∨y)=v(x)+v(y)} for all pairs
     * @param maxDefect       maximum absolute defect over all incomparable pairs
     * @param numPairsChecked number of incomparable pairs checked
     * @param failingPair     first failing pair {@code (x, y, defect)}, or {@code null}
     */
    public record ValuationResult(
            boolean isValuation,
            double maxDefect,
            int numPairsChecked,
            FailingPair failingPair) {}

    /** A failing pair reported by {@link #checkValuation(StateSpace, ToDoubleFunction)}. */
    public record FailingPair(int a, int b, double defect) {}

    /**
     * Result of checking the FKG inequality.
     *
     * @param holds             true iff {@code E[fg] ≥ E[f]·E[g]}
     * @param eFg               {@code E[fg]}
     * @param eF                {@code E[f]}
     * @param eG                {@code E[g]}
     * @param correlation       {@code E[fg] − E[f]·E[g]}
     * @param isLogSupermodular whether the measure is log-supermodular
     */
    public record FKGResult(
            boolean holds,
            double eFg,
            double eF,
            double eG,
            double correlation,
            boolean isLogSupermodular) {}

    /** Complete valuation analysis of a state space. */
    public record ValuationAnalysis(
            boolean rankIsValuation,
            boolean heightIsValuation,
            double rankDefect,
            double heightDefect,
            Map<Integer, Integer> rank,
            Map<Integer, Integer> height,
            boolean isGraded,
            boolean isModular,
            int numStates,
            int valuationDimensionLower) {

        public ValuationAnalysis {
            rank = Map.copyOf(rank);
            height = Map.copyOf(height);
        }
    }

    // -- Internal helpers -------------------------------------------------------

    private record Adj(Map<Integer, Set<Integer>> fwd, Map<Integer, Set<Integer>> rev) {}

    private static Adj buildAdj(StateSpace ss) {
        Map<Integer, Set<Integer>> fwd = new HashMap<>();
        Map<Integer, Set<Integer>> rev = new HashMap<>();
        for (int s : ss.states()) {
            fwd.put(s, new HashSet<>());
            rev.put(s, new HashSet<>());
        }
        for (Transition t : ss.transitions()) {
            fwd.get(t.source()).add(t.target());
            rev.get(t.target()).add(t.source());
        }
        return new Adj(fwd, rev);
    }

    private static Map<Integer, Set<Integer>> reachability(StateSpace ss) {
        Adj adj = buildAdj(ss);
        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int v = stack.pop();
                if (!visited.add(v)) continue;
                for (int t : adj.fwd().getOrDefault(v, Set.of())) {
                    stack.push(t);
                }
            }
            reach.put(s, visited);
        }
        return reach;
    }

    // -- Rank and height functions ---------------------------------------------

    /**
     * Compute the rank function: minimum distance from bottom for each state.
     *
     * <p>{@code rank(bottom) = 0}; {@code rank(x)} is the minimum number of
     * reverse-edge steps from {@code x} to {@code bottom}. States unreachable
     * from bottom (in the reverse graph) receive rank {@code -1}.
     */
    public static Map<Integer, Integer> rankFunction(StateSpace ss) {
        Adj adj = buildAdj(ss);
        Map<Integer, Integer> dist = new HashMap<>();
        dist.put(ss.bottom(), 0);
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(ss.bottom());
        while (!queue.isEmpty()) {
            int s = queue.poll();
            for (int pred : adj.rev().getOrDefault(s, Set.of())) {
                if (!dist.containsKey(pred)) {
                    dist.put(pred, dist.get(s) + 1);
                    queue.add(pred);
                }
            }
        }
        for (int s : ss.states()) {
            dist.putIfAbsent(s, -1);
        }
        return dist;
    }

    /**
     * Compute the height function: maximum distance from bottom for each state.
     *
     * <p>{@code height(bottom) = 0}; {@code height(x)} is the longest simple
     * forward path from {@code x} to {@code bottom}. Returns {@code -1} for
     * states with no forward path to bottom.
     */
    public static Map<Integer, Integer> heightFunction(StateSpace ss) {
        Adj adj = buildAdj(ss);
        Map<Integer, Integer> memo = new HashMap<>();
        Map<Integer, Integer> result = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> seed = new HashSet<>();
            seed.add(s);
            result.put(s, heightDfs(s, seed, ss, adj, memo));
        }
        return result;
    }

    private static int heightDfs(
            int s, Set<Integer> visited, StateSpace ss, Adj adj, Map<Integer, Integer> memo) {
        if (s == ss.bottom()) return 0;
        Integer cached = memo.get(s);
        if (cached != null) return cached;
        int best = -1;
        for (int t : adj.fwd().getOrDefault(s, Set.of())) {
            if (!visited.contains(t)) {
                Set<Integer> next = new HashSet<>(visited);
                next.add(t);
                int h = heightDfs(t, next, ss, adj, memo);
                if (h >= 0) best = Math.max(best, 1 + h);
            }
        }
        // Python memoises only when s is not in the recursion's visited set; at
        // the top-level call, s is seeded in visited, so memoisation is skipped
        // exactly at the entry frame. Inner calls add s to visited before
        // recursing, so they too skip memoising the current node. In effect the
        // Python memo is never populated; we replicate that behaviour faithfully.
        return best;
    }

    // -- Valuation checking -----------------------------------------------------

    /** Check if a function {@code v} satisfies the valuation identity. */
    public static ValuationResult checkValuation(StateSpace ss, ToDoubleFunction<Integer> v) {
        return checkValuation(ss, v, DEFAULT_TOLERANCE);
    }

    public static ValuationResult checkValuation(
            StateSpace ss, ToDoubleFunction<Integer> v, double tolerance) {
        Map<Integer, Set<Integer>> reach = reachability(ss);
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);

        double maxDefect = 0.0;
        int numChecked = 0;
        FailingPair failing = null;

        for (int i = 0; i < states.size(); i++) {
            int a = states.get(i);
            for (int j = i + 1; j < states.size(); j++) {
                int b = states.get(j);
                boolean aReachesB = reach.getOrDefault(a, Set.of()).contains(b);
                boolean bReachesA = reach.getOrDefault(b, Set.of()).contains(a);
                if (aReachesB || bReachesA) continue;

                Integer m = LatticeChecker.computeMeet(ss, a, b);
                Integer jj = LatticeChecker.computeJoin(ss, a, b);
                if (m == null || jj == null) continue;

                numChecked++;
                double va = v.applyAsDouble(a);
                double vb = v.applyAsDouble(b);
                double vm = v.applyAsDouble(m);
                double vj = v.applyAsDouble(jj);
                double defect = Math.abs((vm + vj) - (va + vb));
                if (defect > maxDefect) maxDefect = defect;
                if (defect > tolerance && failing == null) {
                    failing = new FailingPair(a, b, defect);
                }
            }
        }

        return new ValuationResult(maxDefect <= tolerance, maxDefect, numChecked, failing);
    }

    /** Check if the rank function is a valuation. */
    public static boolean isRankValuation(StateSpace ss) {
        Map<Integer, Integer> r = rankFunction(ss);
        return checkValuation(ss, s -> (double) r.get(s)).isValuation();
    }

    /** Maximum defect of {@code v} from being a valuation. */
    public static double valuationDefect(StateSpace ss, ToDoubleFunction<Integer> v) {
        return checkValuation(ss, v).maxDefect();
    }

    // -- FKG inequality ---------------------------------------------------------

    /** Check if a measure {@code mu} is log-supermodular. */
    public static boolean isLogSupermodular(StateSpace ss, ToDoubleFunction<Integer> mu) {
        return isLogSupermodular(ss, mu, DEFAULT_TOLERANCE);
    }

    public static boolean isLogSupermodular(
            StateSpace ss, ToDoubleFunction<Integer> mu, double tolerance) {
        Map<Integer, Set<Integer>> reach = reachability(ss);
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);

        for (int i = 0; i < states.size(); i++) {
            int a = states.get(i);
            for (int j = i + 1; j < states.size(); j++) {
                int b = states.get(j);
                boolean aReachesB = reach.getOrDefault(a, Set.of()).contains(b);
                boolean bReachesA = reach.getOrDefault(b, Set.of()).contains(a);
                if (aReachesB || bReachesA) continue;

                Integer m = LatticeChecker.computeMeet(ss, a, b);
                Integer jj = LatticeChecker.computeJoin(ss, a, b);
                if (m == null || jj == null) continue;

                double muM = mu.applyAsDouble(m);
                double muJ = mu.applyAsDouble(jj);
                double muA = mu.applyAsDouble(a);
                double muB = mu.applyAsDouble(b);
                if (muM * muJ < muA * muB - tolerance) return false;
            }
        }
        return true;
    }

    /**
     * Check the FKG inequality {@code E[fg] ≥ E[f]·E[g]} against measure {@code mu}.
     *
     * <p>The measure is normalised internally. If the total mass is non-positive,
     * a trivial "holds" result with all expectations zero is returned.
     */
    public static FKGResult checkFkg(
            StateSpace ss,
            ToDoubleFunction<Integer> f,
            ToDoubleFunction<Integer> g,
            ToDoubleFunction<Integer> mu) {
        double totalMu = 0.0;
        for (int s : ss.states()) totalMu += mu.applyAsDouble(s);
        if (totalMu <= 0) {
            return new FKGResult(true, 0, 0, 0, 0, false);
        }

        double eF = 0.0, eG = 0.0, eFg = 0.0;
        for (int s : ss.states()) {
            double ms = mu.applyAsDouble(s) / totalMu;
            double fs = f.applyAsDouble(s);
            double gs = g.applyAsDouble(s);
            eF += fs * ms;
            eG += gs * ms;
            eFg += fs * gs * ms;
        }
        double correlation = eFg - eF * eG;
        boolean logSup = isLogSupermodular(ss, mu);
        return new FKGResult(correlation >= -1e-10, eFg, eF, eG, correlation, logSup);
    }

    /**
     * Pearson correlation of {@code rank} and {@code height} under the uniform
     * measure. Returns 1.0 if either function has (nearly) zero variance.
     */
    public static double monotoneCorrelation(StateSpace ss) {
        Map<Integer, Integer> r = rankFunction(ss);
        Map<Integer, Integer> h = heightFunction(ss);
        int n = ss.states().size();
        if (n == 0) return 0.0;

        List<Integer> states = new ArrayList<>(ss.states());
        double meanR = 0.0, meanH = 0.0;
        for (int s : states) {
            meanR += r.get(s);
            meanH += h.get(s);
        }
        meanR /= n;
        meanH /= n;

        double cov = 0.0, varR = 0.0, varH = 0.0;
        for (int s : states) {
            double dr = r.get(s) - meanR;
            double dh = h.get(s) - meanH;
            cov += dr * dh;
            varR += dr * dr;
            varH += dh * dh;
        }
        cov /= n;
        varR /= n;
        varH /= n;

        if (varR < 1e-14 || varH < 1e-14) return 1.0;
        return cov / Math.sqrt(varR * varH);
    }

    // -- Full analysis ----------------------------------------------------------

    /** Full valuation analysis of a state space. */
    public static ValuationAnalysis analyzeValuations(StateSpace ss) {
        Map<Integer, Integer> r = rankFunction(ss);
        Map<Integer, Integer> h = heightFunction(ss);

        ValuationResult rankResult = checkValuation(ss, s -> (double) r.get(s));
        ValuationResult heightResult = checkValuation(ss, s -> (double) h.get(s));

        boolean isGraded = true;
        for (int s : ss.states()) {
            int rs = r.get(s);
            int hs = h.get(s);
            if (rs >= 0 && hs >= 0 && rs != hs) {
                isGraded = false;
                break;
            }
        }

        DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
        boolean isMod = dr.isLattice() && dr.isModular();
        boolean isDist = dr.isLattice() && dr.isDistributive();

        int dimLower = 1;
        if (isDist) {
            // Count join-irreducibles: non-bottom states with exactly one lower cover
            // (one predecessor in the reverse graph, excluding self-loops).
            Map<Integer, Set<Integer>> rev = new HashMap<>();
            for (Transition t : ss.transitions()) {
                rev.computeIfAbsent(t.target(), k -> new HashSet<>()).add(t.source());
            }
            int jIrr = 0;
            for (int s : ss.states()) {
                if (s == ss.bottom()) continue;
                Set<Integer> upperCovers = new HashSet<>();
                for (int u : rev.getOrDefault(s, Set.of())) {
                    if (u != s) upperCovers.add(u);
                }
                if (upperCovers.size() == 1) jIrr++;
            }
            dimLower = Math.max(dimLower, jIrr);
        }

        return new ValuationAnalysis(
                rankResult.isValuation(),
                heightResult.isValuation(),
                rankResult.maxDefect(),
                heightResult.maxDefect(),
                r,
                h,
                isGraded,
                isMod,
                ss.states().size(),
                dimLower);
    }
}
