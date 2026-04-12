package com.bica.reborn.confluence;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Confluence and Church-Rosser analysis for session-type state spaces (Step 80m).
 *
 * <p>The central theorem is: for a finite, well-formed session-type state space L(S),
 * the following are equivalent:
 * <ol>
 *   <li>L(S) is a meet-semilattice (every pair has a greatest lower bound).</li>
 *   <li>L(S) is globally confluent / Church-Rosser.</li>
 *   <li>L(S) is locally confluent.</li>
 * </ol>
 *
 * <p>Ordering convention: {@code s1 >= s2} iff there is a directed path from s1 to s2.
 * The meet (greatest lower bound) is the closing element of a divergence.
 */
public final class ConfluenceChecker {

    private ConfluenceChecker() {}

    // =========================================================================
    // Result types
    // =========================================================================

    /**
     * Outcome of a confluence analysis.
     *
     * @param isLocallyConfluent  every critical pair (single-step divergence) closes
     * @param isGloballyConfluent every pair of co-reachable states has a meet
     * @param criticalPairCount   number of distinct critical pairs
     * @param missingMeets        pairs (x, y) with no common lower bound
     * @param unclosedCriticalPairs critical pairs whose successors share no common lower bound
     */
    public record ConfluenceResult(
            boolean isLocallyConfluent,
            boolean isGloballyConfluent,
            int criticalPairCount,
            List<int[]> missingMeets,
            List<int[]> unclosedCriticalPairs) {

        public ConfluenceResult {
            missingMeets = List.copyOf(missingMeets);
            unclosedCriticalPairs = List.copyOf(unclosedCriticalPairs);
        }
    }

    /**
     * A critical pair: two distinct successors from the same origin.
     */
    public record CriticalPair(int origin, int branch1, int branch2) {}

    // =========================================================================
    // Forward reachability
    // =========================================================================

    /**
     * Compute forward reachability for every state via iterative fixpoint.
     * Handles cycles from recursive session types naturally.
     */
    static Map<Integer, Set<Integer>> forwardReach(StateSpace ss) {
        Map<Integer, Set<Integer>> succ = new HashMap<>();
        for (int s : ss.states()) {
            succ.put(s, new HashSet<>());
        }
        for (var t : ss.transitions()) {
            succ.get(t.source()).add(t.target());
        }

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            reach.put(s, new HashSet<>(Set.of(s)));
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int s : ss.states()) {
                for (int t : succ.get(s)) {
                    if (!reach.get(s).containsAll(reach.get(t))) {
                        reach.get(s).addAll(reach.get(t));
                        changed = true;
                    }
                }
            }
        }
        return reach;
    }

    // =========================================================================
    // Critical pairs
    // =========================================================================

    /**
     * Find every critical pair (origin, branch1, branch2) where branch1 and branch2
     * are distinct non-self-loop successors from origin.
     *
     * <p>Targets are sorted; each unordered pair appears exactly once.
     */
    public static List<CriticalPair> findCriticalPairs(StateSpace ss) {
        Map<Integer, Set<Integer>> out = new HashMap<>();
        for (int s : ss.states()) {
            out.put(s, new TreeSet<>());
        }
        for (var t : ss.transitions()) {
            if (t.source() != t.target()) {
                out.get(t.source()).add(t.target());
            }
        }

        List<CriticalPair> pairs = new ArrayList<>();
        List<Integer> sortedStates = new ArrayList<>(ss.states());
        Collections.sort(sortedStates);

        for (int origin : sortedStates) {
            List<Integer> targets = new ArrayList<>(out.get(origin));
            int n = targets.size();
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    pairs.add(new CriticalPair(origin, targets.get(i), targets.get(j)));
                }
            }
        }
        return pairs;
    }

    // =========================================================================
    // Confluence closure (meet computation)
    // =========================================================================

    /**
     * Return the closing element (meet) of x and y, or null if none exists.
     *
     * <p>The closing element is a state w such that x reaches w and y reaches w
     * and every other such w' satisfies w reaches w'.
     */
    public static Integer confluenceClosure(StateSpace ss, int x, int y) {
        if (!ss.states().contains(x) || !ss.states().contains(y)) {
            return null;
        }
        Map<Integer, Set<Integer>> reach = forwardReach(ss);
        return meetFromReach(reach, x, y);
    }

    /**
     * Greatest lower bound of x and y given precomputed forward reach.
     */
    static Integer meetFromReach(Map<Integer, Set<Integer>> reach, int x, int y) {
        if (x == y) return x;
        // SCC equivalence: same SCC iff each reaches the other
        if (reach.get(x).contains(y) && reach.get(y).contains(x)) {
            return x;
        }
        Set<Integer> common = new HashSet<>(reach.get(x));
        common.retainAll(reach.get(y));
        if (common.isEmpty()) return null;

        // Quotient common by SCC: keep one representative per SCC
        Map<Integer, Set<Integer>> sccOf = new HashMap<>();
        for (int c : common) {
            Set<Integer> scc = new HashSet<>();
            for (int d : common) {
                if (reach.get(c).contains(d) && reach.get(d).contains(c)) {
                    scc.add(d);
                }
            }
            sccOf.put(c, scc);
        }
        Set<Integer> reps = new HashSet<>();
        for (Set<Integer> scc : sccOf.values()) {
            reps.add(Collections.min(scc));
        }

        // The greatest lower bound is the unique rep whose forward-reach contains all of common
        List<Integer> candidates = new ArrayList<>();
        for (int c : reps) {
            if (reach.get(c).containsAll(common)) {
                candidates.add(c);
            }
        }
        if (candidates.size() == 1) return candidates.get(0);
        return null;
    }

    // =========================================================================
    // Local / global confluence
    // =========================================================================

    /**
     * True iff every critical pair has a common lower bound.
     */
    public static boolean isLocallyConfluent(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = forwardReach(ss);
        for (CriticalPair cp : findCriticalPairs(ss)) {
            Set<Integer> intersection = new HashSet<>(reach.get(cp.branch1()));
            intersection.retainAll(reach.get(cp.branch2()));
            if (intersection.isEmpty()) return false;
        }
        return true;
    }

    /**
     * True iff every pair of states reachable from a common ancestor has a meet.
     */
    public static boolean isGloballyConfluent(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = forwardReach(ss);
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);

        for (int i = 0; i < states.size(); i++) {
            int x = states.get(i);
            for (int j = i + 1; j < states.size(); j++) {
                int y = states.get(j);
                boolean commonAncExists = false;
                for (int a : states) {
                    if (reach.get(a).contains(x) && reach.get(a).contains(y)) {
                        commonAncExists = true;
                        break;
                    }
                }
                if (!commonAncExists) continue;
                if (meetFromReach(reach, x, y) == null) return false;
            }
        }
        return true;
    }

    // =========================================================================
    // Top-level analysis
    // =========================================================================

    /**
     * Full confluence analysis returning a frozen result record.
     */
    public static ConfluenceResult checkConfluence(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = forwardReach(ss);
        List<CriticalPair> crit = findCriticalPairs(ss);

        List<int[]> unclosed = new ArrayList<>();
        for (CriticalPair cp : crit) {
            Set<Integer> intersection = new HashSet<>(reach.get(cp.branch1()));
            intersection.retainAll(reach.get(cp.branch2()));
            if (intersection.isEmpty()) {
                unclosed.add(new int[]{cp.origin(), cp.branch1(), cp.branch2()});
            }
        }

        List<int[]> missing = new ArrayList<>();
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);
        for (int i = 0; i < states.size(); i++) {
            int x = states.get(i);
            for (int j = i + 1; j < states.size(); j++) {
                int y = states.get(j);
                boolean commonAnc = false;
                for (int a : states) {
                    if (reach.get(a).contains(x) && reach.get(a).contains(y)) {
                        commonAnc = true;
                        break;
                    }
                }
                if (!commonAnc) continue;
                if (meetFromReach(reach, x, y) == null) {
                    missing.add(new int[]{x, y});
                }
            }
        }

        return new ConfluenceResult(
                unclosed.isEmpty(),
                missing.isEmpty(),
                crit.size(),
                missing,
                unclosed);
    }
}
