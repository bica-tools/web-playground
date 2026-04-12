package com.bica.reborn.config_domains;

import com.bica.reborn.eventstructures.EventStructureChecker;
import com.bica.reborn.eventstructures.EventStructureChecker.ConfigDomain;
import com.bica.reborn.eventstructures.EventStructureChecker.Configuration;
import com.bica.reborn.eventstructures.EventStructureChecker.EventStructureResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Configuration domains for session type event structures (Step 17).
 *
 * <p>A <b>configuration domain</b> D(ES(S)) is the poset of configurations of a
 * prime event structure, ordered by subset inclusion.  For session types,
 * the configuration domain is order-isomorphic to the state space L(S).
 *
 * <p>Key domain-theoretic properties:
 * <ul>
 *   <li><b>Scott domain</b>: algebraic + bounded-complete dcpo</li>
 *   <li><b>Coherence</b>: pairwise-consistent sets have an upper bound</li>
 *   <li><b>Algebraicity</b>: every element is a directed join of compact elements</li>
 *   <li><b>Bounded completeness</b>: every bounded subset has a supremum</li>
 *   <li><b>dI-domain</b>: Scott domain + meets distribute over directed joins</li>
 * </ul>
 *
 * <p>Since all session type event structures are finite, the configuration
 * domain is a finite poset. In a finite poset every element is compact,
 * directed completeness is automatic, and bounded completeness reduces to
 * checking that every pair with an upper bound has a join.
 */
public final class ConfigDomainsChecker {

    private ConfigDomainsChecker() {}

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * Result of Scott domain property checking.
     */
    public record ScottDomainResult(
            boolean isScottDomain,
            boolean isAlgebraic,
            boolean isBoundedComplete,
            boolean isDcpo,
            boolean isCoherent,
            boolean isDistributive,
            boolean isDiDomain,
            int numCompact,
            int numConfigs,
            int numConsistentPairs,
            int numScottOpen) {}

    /**
     * Complete configuration domain analysis.
     */
    public record ConfigDomainAnalysis(
            ConfigDomain domain,
            EventStructureResult es,
            ScottDomainResult scottResult,
            int numConfigs,
            int numCoveringChains,
            int numJoinIrreducibles,
            int maxChainLength,
            int width,
            boolean isLattice) {}

    // -----------------------------------------------------------------------
    // Internal: ordering set wrapper (int[] has no structural equality)
    // -----------------------------------------------------------------------

    /**
     * Build a set of (i,j) pairs from the ConfigDomain ordering,
     * which uses int[] (reference equality) and thus cannot be used
     * directly for contains() checks.
     */
    private static Set<Long> buildOrderingSet(ConfigDomain domain) {
        var set = new HashSet<Long>();
        for (var pair : domain.ordering()) {
            set.add(packPair(pair[0], pair[1]));
        }
        return set;
    }

    private static long packPair(int i, int j) {
        return ((long) i << 32) | (j & 0xFFFFFFFFL);
    }

    /** Create a set of integers (handles duplicates unlike Set.of). */
    private static Set<Integer> setOf(int... values) {
        var s = new HashSet<Integer>();
        for (int v : values) s.add(v);
        return s;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static Set<Integer> upperBounds(ConfigDomain domain, Set<Long> ordering, Set<Integer> indices) {
        int n = domain.numConfigs();
        if (indices.isEmpty()) {
            var all = new HashSet<Integer>();
            for (int i = 0; i < n; i++) all.add(i);
            return all;
        }
        Set<Integer> result = null;
        for (int idx : indices) {
            var above = new HashSet<Integer>();
            for (int j = 0; j < n; j++) {
                if (ordering.contains(packPair(idx, j))) {
                    above.add(j);
                }
            }
            if (result == null) {
                result = above;
            } else {
                result.retainAll(above);
            }
        }
        return result != null ? result : Set.of();
    }

    private static Set<Integer> lowerBounds(ConfigDomain domain, Set<Long> ordering, Set<Integer> indices) {
        int n = domain.numConfigs();
        if (indices.isEmpty()) {
            var all = new HashSet<Integer>();
            for (int i = 0; i < n; i++) all.add(i);
            return all;
        }
        Set<Integer> result = null;
        for (int idx : indices) {
            var below = new HashSet<Integer>();
            for (int i = 0; i < n; i++) {
                if (ordering.contains(packPair(i, idx))) {
                    below.add(i);
                }
            }
            if (result == null) {
                result = below;
            } else {
                result.retainAll(below);
            }
        }
        return result != null ? result : Set.of();
    }

    private static OptionalInt sup(ConfigDomain domain, Set<Long> ordering, Set<Integer> indices) {
        var ubs = upperBounds(domain, ordering, indices);
        if (ubs.isEmpty()) return OptionalInt.empty();
        for (int candidate : ubs) {
            boolean isLeast = true;
            for (int other : ubs) {
                if (candidate != other && !ordering.contains(packPair(candidate, other))) {
                    isLeast = false;
                    break;
                }
            }
            if (isLeast) return OptionalInt.of(candidate);
        }
        return OptionalInt.empty();
    }

    private static OptionalInt inf(ConfigDomain domain, Set<Long> ordering, Set<Integer> indices) {
        var lbs = lowerBounds(domain, ordering, indices);
        if (lbs.isEmpty()) return OptionalInt.empty();
        for (int candidate : lbs) {
            boolean isGreatest = true;
            for (int other : lbs) {
                if (candidate != other && !ordering.contains(packPair(other, candidate))) {
                    isGreatest = false;
                    break;
                }
            }
            if (isGreatest) return OptionalInt.of(candidate);
        }
        return OptionalInt.empty();
    }

    // -----------------------------------------------------------------------
    // Public API: Build configuration domain
    // -----------------------------------------------------------------------

    /**
     * Build the configuration domain D(ES(S)) from a state space.
     */
    public static ConfigDomain buildConfigDomain(StateSpace ss) {
        var es = EventStructureChecker.buildEventStructure(ss);
        return EventStructureChecker.configDomain(es);
    }

    // -----------------------------------------------------------------------
    // Public API: Compact elements
    // -----------------------------------------------------------------------

    /**
     * Identify compact elements. In a finite poset, every element is compact.
     */
    public static List<Integer> compactElements(ConfigDomain domain) {
        var result = new ArrayList<Integer>();
        for (int i = 0; i < domain.numConfigs(); i++) result.add(i);
        return result;
    }

    // -----------------------------------------------------------------------
    // Public API: Consistent pairs
    // -----------------------------------------------------------------------

    /**
     * Find all consistent configuration pairs (those with an upper bound).
     */
    public static List<int[]> consistentPairs(ConfigDomain domain) {
        var ordering = buildOrderingSet(domain);
        int n = domain.numConfigs();
        var result = new ArrayList<int[]>();
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                var ubs = upperBounds(domain, ordering, setOf(i, j));
                if (!ubs.isEmpty()) {
                    result.add(new int[]{i, j});
                }
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Public API: Coherence
    // -----------------------------------------------------------------------

    /**
     * Check coherence: every pairwise-consistent subset has an upper bound.
     */
    public static boolean checkCoherence(ConfigDomain domain) {
        var ordering = buildOrderingSet(domain);
        int n = domain.numConfigs();
        if (n <= 1) return true;

        // Build pairwise consistency
        var consistent = new HashSet<Long>();
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (!upperBounds(domain, ordering, setOf(i, j)).isEmpty()) {
                    consistent.add(packPair(i, j));
                    consistent.add(packPair(j, i));
                }
            }
        }

        // Check triples (sufficient for most cases; full check for small domains)
        if (n <= 12) {
            // Check all subsets of size 3+
            for (int size = 3; size <= n; size++) {
                for (var subset : subsetsOfSize(n, size)) {
                    boolean pwConsistent = true;
                    outer:
                    for (int a : subset) {
                        for (int b : subset) {
                            if (a < b && !consistent.contains(packPair(a, b))) {
                                pwConsistent = false;
                                break outer;
                            }
                        }
                    }
                    if (pwConsistent) {
                        if (upperBounds(domain, ordering, subset).isEmpty()) {
                            return false;
                        }
                    }
                }
            }
        } else {
            // For larger domains, check triples only
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (!consistent.contains(packPair(i, j))) continue;
                    for (int k = j + 1; k < n; k++) {
                        if (consistent.contains(packPair(i, k))
                                && consistent.contains(packPair(j, k))) {
                            if (upperBounds(domain, ordering, setOf(i, j, k)).isEmpty()) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private static List<Set<Integer>> subsetsOfSize(int n, int size) {
        var result = new ArrayList<Set<Integer>>();
        generateSubsets(n, size, 0, new ArrayList<>(), result);
        return result;
    }

    private static void generateSubsets(int n, int size, int start,
                                         List<Integer> current, List<Set<Integer>> result) {
        if (current.size() == size) {
            result.add(new HashSet<>(current));
            return;
        }
        int remaining = size - current.size();
        for (int i = start; i <= n - remaining; i++) {
            current.add(i);
            generateSubsets(n, size, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    // -----------------------------------------------------------------------
    // Public API: Algebraicity
    // -----------------------------------------------------------------------

    /**
     * Check algebraicity. Trivially true for finite posets.
     */
    public static boolean checkAlgebraicity(ConfigDomain domain) {
        return true;
    }

    // -----------------------------------------------------------------------
    // Public API: Bounded completeness
    // -----------------------------------------------------------------------

    /**
     * Check bounded completeness: every pair with an upper bound has a sup.
     */
    public static boolean checkBoundedCompleteness(ConfigDomain domain) {
        var ordering = buildOrderingSet(domain);
        int n = domain.numConfigs();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                var ubs = upperBounds(domain, ordering, setOf(i, j));
                if (!ubs.isEmpty()) {
                    if (sup(domain, ordering, setOf(i, j)).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Public API: Scott domain check
    // -----------------------------------------------------------------------

    /**
     * Verify all Scott domain properties.
     */
    public static ScottDomainResult checkScottDomain(ConfigDomain domain) {
        boolean isAlgebraic = checkAlgebraicity(domain);
        boolean isBoundedComplete = checkBoundedCompleteness(domain);
        boolean isDcpo = true; // automatic for finite posets
        boolean isCoherent = checkCoherence(domain);
        boolean isDistributive = checkDistributivity(domain);

        boolean isScott = isDcpo && isAlgebraic && isBoundedComplete;
        boolean isDi = isScott && isDistributive;

        var compacts = compactElements(domain);
        var pairs = consistentPairs(domain);
        var scottOpens = scottOpenSets(domain);

        return new ScottDomainResult(
                isScott, isAlgebraic, isBoundedComplete, isDcpo,
                isCoherent, isDistributive, isDi,
                compacts.size(), domain.numConfigs(),
                pairs.size(), scottOpens.size());
    }

    // -----------------------------------------------------------------------
    // Public API: Scott topology
    // -----------------------------------------------------------------------

    /**
     * Enumerate Scott-open sets (upward-closed sets in finite posets).
     */
    public static List<Set<Integer>> scottOpenSets(ConfigDomain domain) {
        var ordering = buildOrderingSet(domain);
        int n = domain.numConfigs();

        // Build "above" lookup
        var above = new HashMap<Integer, Set<Integer>>();
        for (int i = 0; i < n; i++) {
            var set = new HashSet<Integer>();
            for (int j = 0; j < n; j++) {
                if (ordering.contains(packPair(i, j))) {
                    set.add(j);
                }
            }
            above.put(i, set);
        }

        var result = new ArrayList<Set<Integer>>();
        for (long mask = 0; mask < (1L << n); mask++) {
            var subset = new HashSet<Integer>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1L << i)) != 0) subset.add(i);
            }
            boolean isUpper = true;
            for (int i : subset) {
                if (!subset.containsAll(above.get(i))) {
                    isUpper = false;
                    break;
                }
            }
            if (isUpper) result.add(subset);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Public API: Distributivity
    // -----------------------------------------------------------------------

    /**
     * Check if meets distribute over directed joins.
     * For finite posets: a meet (b join c) = (a meet b) join (a meet c).
     */
    public static boolean checkDistributivity(ConfigDomain domain) {
        var ordering = buildOrderingSet(domain);
        int n = domain.numConfigs();
        if (n <= 2) return true;

        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                for (int c = 0; c < n; c++) {
                    var bcJoin = sup(domain, ordering, setOf(b, c));
                    if (bcJoin.isEmpty()) continue;

                    var lhs = inf(domain, ordering, setOf(a, bcJoin.getAsInt()));
                    if (lhs.isEmpty()) continue;

                    var abMeet = inf(domain, ordering, setOf(a, b));
                    if (abMeet.isEmpty()) continue;

                    var acMeet = inf(domain, ordering, setOf(a, c));
                    if (acMeet.isEmpty()) continue;

                    var rhs = sup(domain, ordering, setOf(abMeet.getAsInt(), acMeet.getAsInt()));
                    if (rhs.isEmpty()) continue;

                    if (lhs.getAsInt() != rhs.getAsInt()) return false;
                }
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Public API: Covering chains
    // -----------------------------------------------------------------------

    /**
     * Enumerate all maximal covering chains (bottom to maximal elements).
     */
    public static List<List<Integer>> coveringChains(ConfigDomain domain) {
        var ordering = buildOrderingSet(domain);
        int n = domain.numConfigs();
        if (n == 0) return List.of();

        var configs = domain.configs();

        // Build covers: i covers j iff exactly one event difference
        var covers = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < n; i++) covers.put(i, new ArrayList<>());

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                if (!ordering.contains(packPair(i, j))) continue;
                var ciEvents = configs.get(i).events();
                var cjEvents = configs.get(j).events();
                if (cjEvents.size() == ciEvents.size() + 1
                        && cjEvents.containsAll(ciEvents)) {
                    covers.get(i).add(j);
                }
            }
        }

        // Find bottom (empty configuration)
        int bottomIdx = -1;
        for (int i = 0; i < n; i++) {
            if (configs.get(i).events().isEmpty()) {
                bottomIdx = i;
                break;
            }
        }
        if (bottomIdx < 0) return List.of();

        // Find maximal elements (no covers above)
        var maximal = new HashSet<Integer>();
        for (int i = 0; i < n; i++) {
            if (covers.get(i).isEmpty()) maximal.add(i);
        }

        // DFS from bottom to maximal
        var result = new ArrayList<List<Integer>>();
        var path = new ArrayList<Integer>();
        path.add(bottomIdx);
        dfsChains(bottomIdx, path, maximal, covers, result);
        return result;
    }

    private static void dfsChains(int current, List<Integer> path,
                                   Set<Integer> maximal,
                                   Map<Integer, List<Integer>> covers,
                                   List<List<Integer>> result) {
        if (maximal.contains(current)) {
            result.add(new ArrayList<>(path));
            return;
        }
        for (int next : covers.get(current)) {
            path.add(next);
            dfsChains(next, path, maximal, covers, result);
            path.remove(path.size() - 1);
        }
    }

    // -----------------------------------------------------------------------
    // Public API: Join-irreducible elements
    // -----------------------------------------------------------------------

    /**
     * Find join-irreducible elements (non-bottom elements with exactly one lower cover).
     */
    public static List<Integer> joinIrreducibles(ConfigDomain domain) {
        var ordering = buildOrderingSet(domain);
        int n = domain.numConfigs();
        if (n == 0) return List.of();

        var configs = domain.configs();

        // Find bottom
        int bottomIdx = -1;
        for (int i = 0; i < n; i++) {
            if (configs.get(i).events().isEmpty()) {
                bottomIdx = i;
                break;
            }
        }

        var result = new ArrayList<Integer>();
        for (int j = 0; j < n; j++) {
            if (j == bottomIdx) continue;
            int lowerCovers = 0;
            for (int i = 0; i < n; i++) {
                if (i == j) continue;
                if (!ordering.contains(packPair(i, j))) continue;
                var ciEvents = configs.get(i).events();
                var cjEvents = configs.get(j).events();
                if (cjEvents.size() == ciEvents.size() + 1
                        && cjEvents.containsAll(ciEvents)) {
                    lowerCovers++;
                }
            }
            if (lowerCovers == 1) result.add(j);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Public API: Lattice check
    // -----------------------------------------------------------------------

    /**
     * Check if the configuration domain is a lattice.
     */
    public static boolean isLattice(ConfigDomain domain) {
        var ordering = buildOrderingSet(domain);
        int n = domain.numConfigs();
        if (n <= 1) return true;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (sup(domain, ordering, setOf(i, j)).isEmpty()) return false;
                if (inf(domain, ordering, setOf(i, j)).isEmpty()) return false;
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Public API: Width (max antichain)
    // -----------------------------------------------------------------------

    /**
     * Compute the width (maximum antichain size) of the domain.
     */
    public static int width(ConfigDomain domain) {
        var ordering = buildOrderingSet(domain);
        int n = domain.numConfigs();
        if (n == 0) return 0;

        // Build comparability
        var comparable = new HashSet<Long>();
        for (var pair : domain.ordering()) {
            int i = pair[0], j = pair[1];
            if (i != j) {
                comparable.add(packPair(i, j));
                comparable.add(packPair(j, i));
            }
        }

        int best = 1;
        if (n <= 16) {
            for (long mask = 1; mask < (1L << n); mask++) {
                var subset = new ArrayList<Integer>();
                for (int i = 0; i < n; i++) {
                    if ((mask & (1L << i)) != 0) subset.add(i);
                }
                int sz = subset.size();
                if (sz <= best) continue;
                boolean isAntichain = true;
                outer:
                for (int a = 0; a < sz; a++) {
                    for (int b = a + 1; b < sz; b++) {
                        if (comparable.contains(packPair(subset.get(a), subset.get(b)))) {
                            isAntichain = false;
                            break outer;
                        }
                    }
                }
                if (isAntichain) best = sz;
            }
        } else {
            // Greedy approximation
            var antichain = new ArrayList<Integer>();
            for (int i = 0; i < n; i++) {
                boolean ok = true;
                for (int j : antichain) {
                    if (comparable.contains(packPair(i, j))) {
                        ok = false;
                        break;
                    }
                }
                if (ok) antichain.add(i);
            }
            best = Math.max(best, antichain.size());
        }
        return best;
    }

    // -----------------------------------------------------------------------
    // Public API: Full analysis
    // -----------------------------------------------------------------------

    /**
     * Full configuration domain analysis of a state space.
     */
    public static ConfigDomainAnalysis analyzeConfigDomain(StateSpace ss) {
        var es = EventStructureChecker.buildEventStructure(ss);
        var dom = EventStructureChecker.configDomain(es);

        var scottResult = checkScottDomain(dom);
        var chains = coveringChains(dom);
        var ji = joinIrreducibles(dom);
        boolean isLat = isLattice(dom);
        int w = width(dom);

        int maxChainLen = chains.stream().mapToInt(List::size).max().orElse(0);

        return new ConfigDomainAnalysis(
                dom, es, scottResult,
                dom.numConfigs(), chains.size(), ji.size(),
                maxChainLen, w, isLat);
    }
}
