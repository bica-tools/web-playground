package com.bica.reborn.fkg;

import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Correlation inequalities on session type lattices (Step 30z).
 *
 * <p>Four correlation inequalities, in increasing generality:
 * <ol>
 *   <li><b>Harris (1960)</b>: For a finite distributive lattice with uniform measure,
 *       monotone increasing functions f, g satisfy E[fg] &ge; E[f]&middot;E[g].</li>
 *   <li><b>Holley (1974)</b>: If &mu;&#x2081; &le;_FKG &mu;&#x2082; (stochastic domination via lattice
 *       condition), then E&#x2081;[f] &le; E&#x2082;[f] for monotone increasing f.</li>
 *   <li><b>FKG (1971)</b>: Generalizes Harris to log-supermodular measures &mu; on
 *       distributive lattices.</li>
 *   <li><b>Ahlswede-Daykin (1978)</b>: The four-functions theorem &mdash; master inequality
 *       from which all above follow.</li>
 * </ol>
 */
public final class FkgChecker {

    private static final double TOLERANCE = 1e-10;

    private FkgChecker() {}

    // -----------------------------------------------------------------------
    // Result types
    // -----------------------------------------------------------------------

    /** Result of checking a correlation inequality. */
    public record CorrelationResult(
            String inequality,
            boolean holds,
            double lhs,
            double rhs,
            double gap,
            int numStates) {}

    /** Result of Holley's stochastic domination check. */
    public record StochasticDominance(
            boolean dominates,
            boolean holleyCondition,
            double expectationGap,
            int[] failingPair) {}

    /** Result of Ahlswede-Daykin four-functions theorem check. */
    public record FourFunctionsResult(
            boolean holds,
            double sumAlpha,
            double sumBeta,
            double sumGamma,
            double sumDelta,
            double lhs,
            double rhs,
            boolean pointwiseHolds,
            int[] failingPair) {}

    /** Full correlation analysis across all four inequalities. */
    public record CorrelationAnalysis(
            CorrelationResult harris,
            CorrelationResult fkg,
            StochasticDominance holley,
            FourFunctionsResult ahlswedeDaykin,
            boolean isDistributive,
            int numStates,
            double avgFkgGap) {}

    // -----------------------------------------------------------------------
    // Internal: lattice operations
    // -----------------------------------------------------------------------

    /** Forward reachability (inclusive) for all states. */
    static Map<Integer, Set<Integer>> reachability(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : ss.transitions()) adj.get(t.source()).add(t.target());

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int v = stack.pop();
                if (visited.add(v)) {
                    for (int next : adj.getOrDefault(v, Set.of())) {
                        stack.push(next);
                    }
                }
            }
            reach.put(s, visited);
        }
        return reach;
    }

    /** Minimum distance from bottom (BFS over reverse edges). */
    static Map<Integer, Integer> rank(StateSpace ss) {
        Map<Integer, Set<Integer>> rev = new HashMap<>();
        for (int s : ss.states()) rev.put(s, new HashSet<>());
        for (var t : ss.transitions()) rev.computeIfAbsent(t.target(), k -> new HashSet<>()).add(t.source());

        Map<Integer, Integer> dist = new HashMap<>();
        if (ss.states().contains(ss.bottom())) {
            dist.put(ss.bottom(), 0);
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(ss.bottom());
            while (!queue.isEmpty()) {
                int s = queue.poll();
                for (int pred : rev.getOrDefault(s, Set.of())) {
                    if (!dist.containsKey(pred)) {
                        dist.put(pred, dist.get(s) + 1);
                        queue.add(pred);
                    }
                }
            }
        }
        for (int s : ss.states()) dist.putIfAbsent(s, 0);
        return dist;
    }

    /** Uniform probability measure. */
    static Map<Integer, Double> uniformMeasure(StateSpace ss) {
        int n = ss.states().size();
        if (n == 0) return Map.of();
        Map<Integer, Double> mu = new HashMap<>();
        double val = 1.0 / n;
        for (int s : ss.states()) mu.put(s, val);
        return mu;
    }

    /** Exponential-of-rank measure: mu(s) proportional to 2^rank(s). */
    static Map<Integer, Double> rankExponentialMeasure(StateSpace ss) {
        Map<Integer, Integer> r = rank(ss);
        Map<Integer, Double> raw = new HashMap<>();
        double total = 0;
        for (int s : ss.states()) {
            double v = Math.pow(2.0, r.get(s));
            raw.put(s, v);
            total += v;
        }
        if (total <= 0) return raw;
        Map<Integer, Double> result = new HashMap<>();
        for (var e : raw.entrySet()) result.put(e.getKey(), e.getValue() / total);
        return result;
    }

    // -----------------------------------------------------------------------
    // Public API: Monotonicity
    // -----------------------------------------------------------------------

    /** Check if f is monotone increasing: x >= y implies f(x) >= f(y). */
    public static boolean isMonotoneIncreasing(StateSpace ss, Map<Integer, Double> f) {
        Map<Integer, Set<Integer>> reach = reachability(ss);
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (y != x && reach.get(x).contains(y)) { // x >= y
                    if (f.getOrDefault(x, 0.0) < f.getOrDefault(y, 0.0) - TOLERANCE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Check if f is monotone decreasing: x >= y implies f(x) <= f(y). */
    public static boolean isMonotoneDecreasing(StateSpace ss, Map<Integer, Double> f) {
        Map<Integer, Double> negF = new HashMap<>();
        for (var e : f.entrySet()) negF.put(e.getKey(), -e.getValue());
        return isMonotoneIncreasing(ss, negF);
    }

    /**
     * Generate a random monotone increasing function on the lattice.
     * Uses rank function plus random noise that preserves monotonicity.
     */
    public static Map<Integer, Double> generateRandomMonotone(StateSpace ss, long seed) {
        Random rng = new Random(seed);
        Map<Integer, Integer> r = rank(ss);
        int maxRank = 0;
        for (int v : r.values()) if (v > maxRank) maxRank = v;

        Map<Integer, Double> f = new HashMap<>();
        for (int s : ss.states()) {
            double base = maxRank > 0 ? (double) r.get(s) / (maxRank + 1) : 0.0;
            double noise = rng.nextDouble() * 0.1 / (maxRank + 1);
            f.put(s, base + noise);
        }
        return f;
    }

    // -----------------------------------------------------------------------
    // Public API: Harris inequality
    // -----------------------------------------------------------------------

    /** Check Harris inequality: E[fg] >= E[f]*E[g] under uniform measure. */
    public static CorrelationResult checkHarris(
            StateSpace ss, Map<Integer, Double> f, Map<Integer, Double> g) {
        Map<Integer, Double> mu = uniformMeasure(ss);

        double eF = 0, eG = 0, eFG = 0;
        for (int s : ss.states()) {
            double fv = f.getOrDefault(s, 0.0);
            double gv = g.getOrDefault(s, 0.0);
            double mv = mu.getOrDefault(s, 0.0);
            eF += fv * mv;
            eG += gv * mv;
            eFG += fv * gv * mv;
        }

        double gap = eFG - eF * eG;
        return new CorrelationResult("harris", gap >= -TOLERANCE, eFG, eF * eG, gap, ss.states().size());
    }

    // -----------------------------------------------------------------------
    // Public API: FKG inequality
    // -----------------------------------------------------------------------

    /** Check if mu is log-supermodular: mu(x^y)*mu(x v y) >= mu(x)*mu(y). */
    public static boolean isLogSupermodular(StateSpace ss, Map<Integer, Double> mu) {
        Map<Integer, Set<Integer>> reach = reachability(ss);
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);

        for (int i = 0; i < states.size(); i++) {
            int a = states.get(i);
            for (int j = i + 1; j < states.size(); j++) {
                int b = states.get(j);
                // Only check incomparable pairs
                if (reach.get(a).contains(b) || reach.get(b).contains(a)) continue;

                Integer m = LatticeChecker.computeMeet(ss, a, b);
                Integer jn = LatticeChecker.computeJoin(ss, a, b);
                if (m == null || jn == null) continue;

                double lhs = mu.getOrDefault(m, 0.0) * mu.getOrDefault(jn, 0.0);
                double rhs = mu.getOrDefault(a, 0.0) * mu.getOrDefault(b, 0.0);
                if (lhs < rhs - TOLERANCE) return false;
            }
        }
        return true;
    }

    /** Check FKG inequality: E_mu[fg] >= E_mu[f]*E_mu[g]. */
    public static CorrelationResult checkFkg(
            StateSpace ss, Map<Integer, Double> f, Map<Integer, Double> g,
            Map<Integer, Double> mu) {
        double total = 0;
        for (int s : ss.states()) total += mu.getOrDefault(s, 0.0);
        if (total <= 0) return new CorrelationResult("fkg", true, 0, 0, 0, ss.states().size());

        double eF = 0, eG = 0, eFG = 0;
        for (int s : ss.states()) {
            double fv = f.getOrDefault(s, 0.0);
            double gv = g.getOrDefault(s, 0.0);
            double norm = mu.getOrDefault(s, 0.0) / total;
            eF += fv * norm;
            eG += gv * norm;
            eFG += fv * gv * norm;
        }

        double gap = eFG - eF * eG;
        return new CorrelationResult("fkg", gap >= -TOLERANCE, eFG, eF * eG, gap, ss.states().size());
    }

    /** Compute the FKG gap: E[fg] - E[f]*E[g]. */
    public static double fkgGap(
            StateSpace ss, Map<Integer, Double> f, Map<Integer, Double> g,
            Map<Integer, Double> mu) {
        return checkFkg(ss, f, g, mu).gap();
    }

    // -----------------------------------------------------------------------
    // Public API: Holley inequality
    // -----------------------------------------------------------------------

    /** Check Holley's stochastic domination inequality. */
    public static StochasticDominance checkHolley(
            StateSpace ss, Map<Integer, Double> mu1, Map<Integer, Double> mu2,
            Map<Integer, Double> f) {
        Map<Integer, Set<Integer>> reach = reachability(ss);
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);

        boolean holleyOk = true;
        int[] failing = null;

        for (int i = 0; i < states.size(); i++) {
            int a = states.get(i);
            for (int j = i + 1; j < states.size(); j++) {
                int b = states.get(j);
                if (reach.get(a).contains(b) || reach.get(b).contains(a)) continue;

                Integer m = LatticeChecker.computeMeet(ss, a, b);
                Integer jn = LatticeChecker.computeJoin(ss, a, b);
                if (m == null || jn == null) continue;

                double lhs = mu1.getOrDefault(m, 0.0) * mu2.getOrDefault(jn, 0.0);
                double rhs = mu1.getOrDefault(jn, 0.0) * mu2.getOrDefault(m, 0.0);
                if (lhs < rhs - TOLERANCE) {
                    holleyOk = false;
                    if (failing == null) failing = new int[]{a, b};
                }
            }
        }

        // Compute expectations
        double total1 = 0, total2 = 0;
        for (int s : ss.states()) {
            total1 += mu1.getOrDefault(s, 0.0);
            total2 += mu2.getOrDefault(s, 0.0);
        }

        double expGap = 0.0;
        if (total1 > 0 && total2 > 0) {
            double e1 = 0, e2 = 0;
            for (int s : ss.states()) {
                double fv = f.getOrDefault(s, 0.0);
                e1 += fv * mu1.getOrDefault(s, 0.0) / total1;
                e2 += fv * mu2.getOrDefault(s, 0.0) / total2;
            }
            expGap = e2 - e1;
        }

        boolean dominates = holleyOk && expGap >= -TOLERANCE;
        return new StochasticDominance(dominates, holleyOk, expGap, failing);
    }

    // -----------------------------------------------------------------------
    // Public API: Ahlswede-Daykin four-functions theorem
    // -----------------------------------------------------------------------

    /**
     * Check the Ahlswede-Daykin four-functions theorem.
     * If alpha(x)*beta(y) <= gamma(x v y)*delta(x ^ y) for all x,y,
     * then (Sum alpha)*(Sum beta) <= (Sum gamma)*(Sum delta).
     */
    public static FourFunctionsResult checkAhlswedeDaykin(
            StateSpace ss,
            Map<Integer, Double> alpha, Map<Integer, Double> beta,
            Map<Integer, Double> gamma, Map<Integer, Double> delta) {
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);

        boolean pwOk = true;
        int[] failing = null;

        for (int a : states) {
            for (int b : states) {
                Integer m = LatticeChecker.computeMeet(ss, a, b);
                Integer jn = LatticeChecker.computeJoin(ss, a, b);
                if (m == null || jn == null) continue;

                double lhsPw = alpha.getOrDefault(a, 0.0) * beta.getOrDefault(b, 0.0);
                double rhsPw = gamma.getOrDefault(jn, 0.0) * delta.getOrDefault(m, 0.0);
                if (lhsPw > rhsPw + TOLERANCE) {
                    pwOk = false;
                    if (failing == null) failing = new int[]{a, b};
                }
            }
        }

        double sAlpha = 0, sBeta = 0, sGamma = 0, sDelta = 0;
        for (int s : states) {
            sAlpha += alpha.getOrDefault(s, 0.0);
            sBeta += beta.getOrDefault(s, 0.0);
            sGamma += gamma.getOrDefault(s, 0.0);
            sDelta += delta.getOrDefault(s, 0.0);
        }

        double lhs = sAlpha * sBeta;
        double rhs = sGamma * sDelta;

        return new FourFunctionsResult(
                lhs <= rhs + TOLERANCE,
                sAlpha, sBeta, sGamma, sDelta,
                lhs, rhs, pwOk, failing);
    }

    // -----------------------------------------------------------------------
    // Public API: Correlation profile and analysis
    // -----------------------------------------------------------------------

    /** Test all four inequalities on random monotone function pairs. */
    public static Map<String, Object> correlationProfile(StateSpace ss, int nTrials, long seed) {
        List<Double> harrisGaps = new ArrayList<>();
        List<Double> fkgGaps = new ArrayList<>();

        Map<Integer, Double> muRank = rankExponentialMeasure(ss);

        for (int i = 0; i < nTrials; i++) {
            Map<Integer, Double> f = generateRandomMonotone(ss, seed + i);
            Map<Integer, Double> g = generateRandomMonotone(ss, seed + i + 1000);

            CorrelationResult h = checkHarris(ss, f, g);
            harrisGaps.add(h.gap());

            CorrelationResult fk = checkFkg(ss, f, g, muRank);
            fkgGaps.add(fk.gap());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("harris_avg_gap", harrisGaps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        result.put("harris_min_gap", harrisGaps.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
        result.put("harris_all_hold", harrisGaps.stream().allMatch(g -> g >= -TOLERANCE));
        result.put("fkg_avg_gap", fkgGaps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        result.put("fkg_min_gap", fkgGaps.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
        result.put("fkg_all_hold", fkgGaps.stream().allMatch(g -> g >= -TOLERANCE));
        result.put("n_trials", nTrials);
        return result;
    }

    /** Full correlation analysis across all four inequalities. */
    public static CorrelationAnalysis analyzeCorrelation(StateSpace ss, long seed) {
        Map<Integer, Double> f = generateRandomMonotone(ss, seed);
        Map<Integer, Double> g = generateRandomMonotone(ss, seed + 1);

        Map<Integer, Double> muUniform = uniformMeasure(ss);
        Map<Integer, Double> muRank = rankExponentialMeasure(ss);

        CorrelationResult harris = checkHarris(ss, f, g);
        CorrelationResult fkg = checkFkg(ss, f, g, muRank);
        StochasticDominance holley = checkHolley(ss, muUniform, muRank, f);

        // For AD, use alpha=mu*f, beta=mu*g, gamma=mu*f*g, delta=mu
        Map<Integer, Double> alpha = new HashMap<>();
        Map<Integer, Double> beta = new HashMap<>();
        Map<Integer, Double> gamma = new HashMap<>();
        Map<Integer, Double> delta = new HashMap<>(muRank);
        for (int s : ss.states()) {
            double mv = muRank.getOrDefault(s, 0.0);
            double fv = f.getOrDefault(s, 0.0);
            double gv = g.getOrDefault(s, 0.0);
            alpha.put(s, mv * fv);
            beta.put(s, mv * gv);
            gamma.put(s, mv * fv * gv);
        }
        FourFunctionsResult ad = checkAhlswedeDaykin(ss, alpha, beta, gamma, delta);

        DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
        boolean isDist = dr.isLattice() && dr.isDistributive();

        Map<String, Object> profile = correlationProfile(ss, 10, seed);
        double avgFkgGap = (Double) profile.get("fkg_avg_gap");

        return new CorrelationAnalysis(harris, fkg, holley, ad, isDist, ss.states().size(), avgFkgGap);
    }
}
