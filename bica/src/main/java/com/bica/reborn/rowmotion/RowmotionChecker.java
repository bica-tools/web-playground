package com.bica.reborn.rowmotion;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Rowmotion and toggling for session type lattices (port of Python {@code reticulate.rowmotion}).
 *
 * <p>Rowmotion is a fundamental dynamical operator on finite posets that acts
 * on the set of order ideals (downsets).  Given a finite poset P:
 *
 * <ul>
 *   <li>An <b>order ideal</b> (downset) I is a subset I of P such that if
 *       x in I and y &le; x then y in I.</li>
 *   <li>An <b>antichain</b> A is a subset where no two elements are comparable.</li>
 *   <li><b>Rowmotion</b> on order ideal I: Row(I) = downset(min(P \ I)).</li>
 *   <li><b>Toggle</b> at element x: add x to I if feasible, remove if feasible,
 *       or leave unchanged.</li>
 * </ul>
 *
 * <p>All methods are static and operate on the SCC quotient of the state space.
 */
public final class RowmotionChecker {

    private RowmotionChecker() {}

    // =======================================================================
    // Result types
    // =======================================================================

    /** Complete rowmotion analysis result. */
    public record RowmotionResult(
            List<Set<Integer>> orderIdeals,
            List<Set<Integer>> antichains,
            int rowmotionOrder,
            List<Integer> orbitSizes,
            int numOrbits,
            boolean isHomomesicCardinality,
            Double homomesyAverage,
            List<Integer> toggleSequence) {}

    // =======================================================================
    // Internal: quotient poset
    // =======================================================================

    /**
     * Quotient poset data: SCC representatives, below/above relations.
     */
    private record QuotientPoset(
            List<Integer> reps,
            Map<Integer, Integer> sccMap,
            Map<Integer, Set<Integer>> sccMembers,
            Map<Integer, Set<Integer>> below,
            Map<Integer, Set<Integer>> above) {}

    private static QuotientPoset quotientPoset(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);

        // Quotient adjacency
        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new HashSet<>());
        for (int s : ss.states()) {
            for (int t : adj.get(s)) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                if (sr != tr) qAdj.get(sr).add(tr);
            }
        }

        // Transitive closure (below = reachable = lower in poset)
        Map<Integer, Set<Integer>> below = new HashMap<>();
        for (int r : reps) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(r);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (!visited.add(u)) continue;
                for (int v : qAdj.getOrDefault(u, Set.of())) {
                    stack.push(v);
                }
            }
            // strictly below: reachable minus self
            visited.remove(r);
            below.put(r, visited);
        }

        // above: inverse of below
        Map<Integer, Set<Integer>> above = new HashMap<>();
        for (int r : reps) above.put(r, new HashSet<>());
        for (int r : reps) {
            for (int b : below.get(r)) {
                above.get(b).add(r);
            }
        }

        return new QuotientPoset(reps, sccMap, sccMembers, below, above);
    }

    // =======================================================================
    // Core poset operations
    // =======================================================================

    /**
     * Build order relation: rep -&gt; set of reps at or below it (reflexive closure).
     */
    public static Map<Integer, Set<Integer>> computeOrderRelation(StateSpace ss) {
        QuotientPoset q = quotientPoset(ss);
        Map<Integer, Set<Integer>> order = new HashMap<>();
        for (int r : q.reps()) {
            Set<Integer> s = new HashSet<>(q.below().get(r));
            s.add(r);
            order.put(r, s);
        }
        return order;
    }

    /**
     * Covering relation: rep -&gt; set of reps it directly covers.
     * x covers y iff x &gt; y and no z with x &gt; z &gt; y.
     */
    public static Map<Integer, Set<Integer>> computeCovers(StateSpace ss) {
        QuotientPoset q = quotientPoset(ss);
        Map<Integer, Set<Integer>> covers = new HashMap<>();
        for (int x : q.reps()) {
            Set<Integer> covX = new HashSet<>();
            for (int y : q.below().get(x)) {
                boolean isCover = true;
                for (int z : q.below().get(x)) {
                    if (z != y && q.below().get(z).contains(y)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) covX.add(y);
            }
            covers.put(x, covX);
        }
        return covers;
    }

    /**
     * Enumerate all order ideals (downsets) of the quotient poset.
     */
    public static List<Set<Integer>> orderIdeals(StateSpace ss) {
        QuotientPoset q = quotientPoset(ss);

        Set<Set<Integer>> seen = new HashSet<>();
        Deque<Set<Integer>> queue = new ArrayDeque<>();
        Set<Integer> empty = Set.of();
        queue.add(empty);
        seen.add(empty);
        List<Set<Integer>> ideals = new ArrayList<>();

        while (!queue.isEmpty()) {
            Set<Integer> current = queue.poll();
            ideals.add(current);
            for (int r : q.reps()) {
                if (current.contains(r)) continue;
                if (current.containsAll(q.below().get(r))) {
                    Set<Integer> newIdeal = new HashSet<>(current);
                    newIdeal.add(r);
                    Set<Integer> frozen = Collections.unmodifiableSet(newIdeal);
                    if (seen.add(frozen)) {
                        queue.add(frozen);
                    }
                }
            }
        }
        return ideals;
    }

    /**
     * Enumerate all antichains via the bijection: antichain = max elements of ideal.
     */
    public static List<Set<Integer>> antichains(StateSpace ss) {
        QuotientPoset q = quotientPoset(ss);
        List<Set<Integer>> ideals = orderIdeals(ss);
        List<Set<Integer>> result = new ArrayList<>();
        for (Set<Integer> ideal : ideals) {
            result.add(idealToAntichainImpl(ideal, q.below()));
        }
        return result;
    }

    /**
     * Convert antichain to order ideal (downward closure).
     */
    public static Set<Integer> antichainToIdeal(StateSpace ss, Set<Integer> ac) {
        QuotientPoset q = quotientPoset(ss);
        Set<Integer> ideal = new HashSet<>();
        for (int a : ac) {
            ideal.add(a);
            ideal.addAll(q.below().get(a));
        }
        return Collections.unmodifiableSet(ideal);
    }

    /**
     * Convert order ideal to antichain (maximal elements).
     */
    public static Set<Integer> idealToAntichain(StateSpace ss, Set<Integer> ideal) {
        QuotientPoset q = quotientPoset(ss);
        return idealToAntichainImpl(ideal, q.below());
    }

    private static Set<Integer> idealToAntichainImpl(
            Set<Integer> ideal, Map<Integer, Set<Integer>> below) {
        if (ideal.isEmpty()) return Set.of();
        Set<Integer> maximal = new HashSet<>();
        for (int x : ideal) {
            boolean isMax = true;
            for (int y : ideal) {
                if (y != x && below.get(y) != null && below.get(y).contains(x)) {
                    isMax = false;
                    break;
                }
            }
            if (isMax) maximal.add(x);
        }
        return Collections.unmodifiableSet(maximal);
    }

    // =======================================================================
    // Toggle operations
    // =======================================================================

    /**
     * Toggle element x in/out of an order ideal.
     */
    public static Set<Integer> toggle(StateSpace ss, Set<Integer> ideal, int x) {
        QuotientPoset q = quotientPoset(ss);

        if (!ideal.contains(x)) {
            // Try to add: below[x] must be subset of ideal
            if (ideal.containsAll(q.below().get(x))) {
                Set<Integer> result = new HashSet<>(ideal);
                result.add(x);
                return Collections.unmodifiableSet(result);
            }
            return ideal;
        } else {
            // Try to remove: I \ {x} must still be a downset
            Set<Integer> candidate = new HashSet<>(ideal);
            candidate.remove(x);
            for (int y : candidate) {
                if (!candidate.containsAll(q.below().get(y))) {
                    return ideal; // cannot remove
                }
            }
            return Collections.unmodifiableSet(candidate);
        }
    }

    /**
     * Linear extension from top to bottom for toggle decomposition.
     */
    public static List<Integer> toggleSequence(StateSpace ss) {
        QuotientPoset q = quotientPoset(ss);
        Map<Integer, Integer> rank = Zeta.computeRank(ss);

        Map<Integer, Integer> repRank = new HashMap<>();
        for (int r : q.reps()) {
            repRank.put(r, rank.getOrDefault(r, 0));
        }

        List<Integer> result = new ArrayList<>(q.reps());
        result.sort((a, b) -> Integer.compare(repRank.getOrDefault(b, 0),
                repRank.getOrDefault(a, 0)));
        return result;
    }

    // =======================================================================
    // Rowmotion
    // =======================================================================

    /**
     * Apply rowmotion to an order ideal: Row(I) = downset(min(P \ I)).
     */
    public static Set<Integer> rowmotionIdeal(StateSpace ss, Set<Integer> ideal) {
        QuotientPoset q = quotientPoset(ss);
        Set<Integer> repsSet = new HashSet<>(q.reps());
        Set<Integer> complement = new HashSet<>(repsSet);
        complement.removeAll(ideal);

        if (complement.isEmpty()) {
            return Set.of();
        }

        // Find minimal elements of complement
        Set<Integer> minimal = new HashSet<>();
        for (int x : complement) {
            boolean isMin = true;
            for (int y : complement) {
                if (y != x && q.below().get(x).contains(y)) {
                    isMin = false;
                    break;
                }
            }
            if (isMin) minimal.add(x);
        }

        // Downward closure of minimal elements
        Set<Integer> result = new HashSet<>();
        for (int m : minimal) {
            result.add(m);
            result.addAll(q.below().get(m));
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Apply rowmotion to an antichain.
     */
    public static Set<Integer> rowmotionAntichain(StateSpace ss, Set<Integer> ac) {
        Set<Integer> ideal = antichainToIdeal(ss, ac);
        Set<Integer> newIdeal = rowmotionIdeal(ss, ideal);
        return idealToAntichain(ss, newIdeal);
    }

    /**
     * Apply rowmotion as composition of toggles.
     */
    public static Set<Integer> rowmotionViaToggles(StateSpace ss, Set<Integer> ideal) {
        List<Integer> seq = toggleSequence(ss);
        Set<Integer> current = ideal;
        for (int x : seq) {
            current = toggle(ss, current, x);
        }
        return current;
    }

    // =======================================================================
    // Orbit analysis
    // =======================================================================

    /**
     * Compute the orbit of an order ideal under rowmotion.
     */
    public static List<Set<Integer>> rowmotionOrbit(StateSpace ss, Set<Integer> ideal) {
        List<Set<Integer>> orbit = new ArrayList<>();
        orbit.add(ideal);
        Set<Integer> current = rowmotionIdeal(ss, ideal);
        while (!current.equals(ideal)) {
            orbit.add(current);
            current = rowmotionIdeal(ss, current);
        }
        return orbit;
    }

    /**
     * Partition all order ideals into rowmotion orbits.
     */
    public static List<List<Set<Integer>>> allOrbits(StateSpace ss) {
        List<Set<Integer>> ideals = orderIdeals(ss);
        Set<Set<Integer>> seen = new HashSet<>();
        List<List<Set<Integer>>> orbits = new ArrayList<>();

        for (Set<Integer> ideal : ideals) {
            if (!seen.contains(ideal)) {
                List<Set<Integer>> orbit = rowmotionOrbit(ss, ideal);
                seen.addAll(orbit);
                orbits.add(orbit);
            }
        }
        return orbits;
    }

    /**
     * Compute the order of rowmotion (LCM of all orbit sizes).
     */
    public static int rowmotionOrder(StateSpace ss) {
        List<List<Set<Integer>>> orbits = allOrbits(ss);
        if (orbits.isEmpty()) return 1;
        long order = 1;
        for (List<Set<Integer>> orbit : orbits) {
            int size = orbit.size();
            order = lcm(order, size);
        }
        return (int) order;
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    private static long lcm(long a, long b) {
        return a / gcd(a, b) * b;
    }

    // =======================================================================
    // Homomesy
    // =======================================================================

    /**
     * Check if cardinality |I| is homomesic across orbits.
     *
     * @return [isHomomesic, average] where average is null if not homomesic
     */
    public static HomomesyResult checkHomomesy(StateSpace ss, List<List<Set<Integer>>> orbits) {
        if (orbits.isEmpty()) {
            return new HomomesyResult(true, null);
        }

        // Use exact rational comparison: total_i / size_i = total_j / size_j
        long t0 = 0;
        int s0 = orbits.get(0).size();
        for (Set<Integer> ideal : orbits.get(0)) t0 += ideal.size();

        boolean isHomo = true;
        for (List<Set<Integer>> orbit : orbits) {
            long ti = 0;
            int si = orbit.size();
            for (Set<Integer> ideal : orbit) ti += ideal.size();
            if (t0 * si != ti * s0) {
                isHomo = false;
                break;
            }
        }

        if (isHomo) {
            double avg = (double) t0 / s0;
            return new HomomesyResult(true, avg);
        }
        return new HomomesyResult(false, null);
    }

    /** Homomesy check result. */
    public record HomomesyResult(boolean isHomomesic, Double average) {}

    // =======================================================================
    // Full analysis
    // =======================================================================

    /**
     * Full rowmotion analysis: enumerate ideals, compute orbits, check homomesy.
     */
    public static RowmotionResult analyzeRowmotion(StateSpace ss) {
        List<Set<Integer>> ideals = orderIdeals(ss);
        List<Set<Integer>> acs = antichains(ss);
        List<Integer> seq = toggleSequence(ss);
        List<List<Set<Integer>>> orbits = allOrbits(ss);

        List<Integer> orbitSizes = new ArrayList<>();
        for (List<Set<Integer>> orbit : orbits) {
            orbitSizes.add(orbit.size());
        }
        Collections.sort(orbitSizes);

        int numOrbits = orbits.size();
        long order = 1;
        for (int size : orbitSizes) {
            order = lcm(order, size);
        }

        HomomesyResult hom = checkHomomesy(ss, orbits);

        return new RowmotionResult(
                ideals, acs, (int) order, orbitSizes, numOrbits,
                hom.isHomomesic(), hom.average(), seq);
    }
}
