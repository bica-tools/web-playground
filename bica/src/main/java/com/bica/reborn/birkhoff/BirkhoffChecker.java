package com.bica.reborn.birkhoff;

import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.IrreduciblesChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;

/**
 * Birkhoff's Representation Theorem for session type lattices.
 *
 * <p>Birkhoff's fundamental theorem (1937) states that every finite distributive
 * lattice L is isomorphic to the lattice of downsets (order ideals) of the
 * poset J(L) of its join-irreducible elements: {@code L ≅ O(J(L))}.
 *
 * <p>This class implements:
 * <ul>
 *   <li>Extraction of the join-irreducible poset J(L) from a session type lattice</li>
 *   <li>Construction of the downset lattice O(P) from an arbitrary finite poset</li>
 *   <li>Verification that L ≅ O(J(L)) (the Birkhoff representation)</li>
 *   <li>Inverse: reconstruct a lattice (as a StateSpace) from a Birkhoff poset</li>
 * </ul>
 */
public final class BirkhoffChecker {

    private BirkhoffChecker() {}

    // =========================================================================
    // High-level API
    // =========================================================================

    /**
     * Compute the Birkhoff representation for a session type lattice.
     *
     * <p>For a distributive lattice L, verifies {@code L ≅ O(J(L))} where J(L) is the
     * poset of join-irreducible elements and O(P) is the downset lattice.
     */
    public static BirkhoffResult birkhoffRepresentation(StateSpace ss) {
        DistributivityResult dr = DistributivityChecker.checkDistributive(ss);

        if (!dr.isLattice()) {
            return new BirkhoffResult(
                    false, false, false,
                    Set.of(), Map.of(), 0,
                    ss.states().size(), 1.0, false, Map.of());
        }

        var q = DistributivityChecker.QuotientPoset.build(ss);
        int latticeSize = q.nodes().size();

        // Find join-irreducibles on the quotient
        Set<Integer> jis = findQuotientJoinIrreducibles(q);

        // Map JI quotient node IDs to representative state IDs
        Set<Integer> jisRep = new HashSet<>();
        for (int j : jis) {
            jisRep.add(q.rep().get(j));
        }

        // Build JI poset (Hasse diagram restricted to J(L))
        Map<Integer, Set<Integer>> poset = buildJIPoset(jis, q);

        // Map poset to representative state IDs
        Map<Integer, Set<Integer>> posetRep = new HashMap<>();
        for (var entry : poset.entrySet()) {
            Set<Integer> targets = new HashSet<>();
            for (int k : entry.getValue()) {
                targets.add(q.rep().get(k));
            }
            posetRep.put(q.rep().get(entry.getKey()), targets);
        }

        if (dr.isDistributive()) {
            // Build downset lattice and verify isomorphism
            Set<Set<Integer>> downsets = enumerateDownsets(jis, q);
            Map<Integer, Set<Integer>> isoMap = birkhoffMap(q, jis);

            // Map iso to representative state IDs
            Map<Integer, Set<Integer>> isoRep = new HashMap<>();
            for (var entry : isoMap.entrySet()) {
                Set<Integer> mapped = new HashSet<>();
                for (int j : entry.getValue()) {
                    mapped.add(q.rep().get(j));
                }
                isoRep.put(q.rep().get(entry.getKey()), mapped);
            }

            boolean verified = verifyIsomorphism(q, isoMap, downsets);

            double compression = latticeSize > 0
                    ? (double) jis.size() / latticeSize : 1.0;

            return new BirkhoffResult(
                    true, true, true,
                    jisRep, posetRep, downsets.size(),
                    latticeSize, compression, verified, isoRep);
        } else {
            double compression = latticeSize > 0
                    ? (double) jis.size() / latticeSize : 1.0;

            return new BirkhoffResult(
                    true, false, false,
                    jisRep, posetRep, 0,
                    latticeSize, compression, false, Map.of());
        }
    }

    /**
     * Check if a session type lattice has a Birkhoff representation.
     * Equivalent to checking distributivity.
     */
    public static boolean isRepresentable(StateSpace ss) {
        return DistributivityChecker.checkDistributive(ss).isDistributive();
    }

    /**
     * Construct the downset lattice O(P) from a finite poset P.
     *
     * @param poset Hasse diagram: node → set of nodes it covers (node > covered_node).
     * @return a StateSpace whose reachability lattice is O(P).
     */
    public static StateSpace downsetLattice(Map<Integer, Set<Integer>> poset) {
        List<Integer> elements = new ArrayList<>(poset.keySet());
        Collections.sort(elements);

        if (elements.isEmpty()) {
            return new StateSpace(
                    Set.of(0), List.of(), 0, 0,
                    Map.of(0, "end"));
        }

        // Transitive closure: orderBelow[x] = all y with y <= x (including x)
        Map<Integer, Set<Integer>> orderBelow = new HashMap<>();
        for (int x : elements) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(x);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (visited.add(u)) {
                    for (int v : poset.getOrDefault(u, Set.of())) {
                        stack.push(v);
                    }
                }
            }
            orderBelow.put(x, visited);
        }

        // Enumerate all downsets
        int n = elements.size();
        List<Set<Integer>> allDownsets = new ArrayList<>();
        for (int mask = 0; mask < (1 << n); mask++) {
            Set<Integer> subset = new HashSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) subset.add(elements.get(i));
            }
            boolean isDownset = true;
            for (int x : subset) {
                if (!subset.containsAll(orderBelow.get(x))) {
                    isDownset = false;
                    break;
                }
            }
            if (isDownset) allDownsets.add(subset);
        }

        // Sort downsets deterministically
        allDownsets.sort((a, b) -> {
            int cmp = Integer.compare(b.size(), a.size());
            if (cmp != 0) return cmp;
            return a.toString().compareTo(b.toString());
        });

        // Map downsets to state IDs
        Map<Set<Integer>, Integer> dsToId = new HashMap<>();
        Map<Integer, String> labels = new HashMap<>();
        for (int i = 0; i < allDownsets.size(); i++) {
            Set<Integer> d = allDownsets.get(i);
            dsToId.put(d, i);
            if (d.isEmpty()) {
                labels.put(i, "end");
            } else {
                List<Integer> sorted = new ArrayList<>(d);
                Collections.sort(sorted);
                labels.put(i, "{" + String.join(",",
                        sorted.stream().map(String::valueOf).toList()) + "}");
            }
        }

        Set<Integer> top = new HashSet<>(elements);
        Set<Integer> bottom = Set.of();

        // Build transitions: d2 covers d1 iff d1 ⊂ d2 and |d2 - d1| == 1
        List<Transition> transitions = new ArrayList<>();
        for (Set<Integer> d2 : allDownsets) {
            for (Set<Integer> d1 : allDownsets) {
                if (d1.size() + 1 == d2.size() && d2.containsAll(d1)) {
                    Set<Integer> diff = new HashSet<>(d2);
                    diff.removeAll(d1);
                    if (diff.size() == 1) {
                        int removed = diff.iterator().next();
                        String label = "t" + removed;
                        transitions.add(new Transition(
                                dsToId.get(d2), label, dsToId.get(d1)));
                    }
                }
            }
        }

        return new StateSpace(
                new HashSet<>(dsToId.values()),
                transitions,
                dsToId.get(top),
                dsToId.get(bottom),
                labels);
    }

    /**
     * Reconstruct a session type state space from a Birkhoff poset.
     * Given a poset P (Hasse diagram), builds a state space whose reachability
     * lattice is isomorphic to O(P).
     */
    public static StateSpace reconstructFromPoset(Map<Integer, Set<Integer>> poset) {
        return downsetLattice(poset);
    }

    // =========================================================================
    // Internal: join-irreducibles on quotient
    // =========================================================================

    private static Set<Integer> findQuotientJoinIrreducibles(
            DistributivityChecker.QuotientPoset q) {
        // Compute Hasse edges on the quotient
        List<int[]> hasse = quotientHasseEdges(q);

        // Count lower covers for each node
        Map<Integer, Integer> lowerCoverCount = new HashMap<>();
        for (int n : q.nodes()) lowerCoverCount.put(n, 0);
        for (int[] edge : hasse) {
            lowerCoverCount.merge(edge[0], 1, Integer::sum);
        }

        Set<Integer> jis = new HashSet<>();
        for (int n : q.nodes()) {
            if (n != q.bottom() && lowerCoverCount.getOrDefault(n, 0) == 1) {
                jis.add(n);
            }
        }
        return jis;
    }

    private static List<int[]> quotientHasseEdges(
            DistributivityChecker.QuotientPoset q) {
        List<int[]> edges = new ArrayList<>();
        for (int a : q.nodes()) {
            Set<Integer> direct = q.fwdAdj().get(a);
            for (int b : direct) {
                boolean isCover = true;
                for (int c : direct) {
                    if (c == b) continue;
                    if (q.fwdReach().get(c).contains(b)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) {
                    edges.add(new int[]{a, b});
                }
            }
        }
        return edges;
    }

    // =========================================================================
    // Internal: JI poset construction
    // =========================================================================

    private static Map<Integer, Set<Integer>> buildJIPoset(
            Set<Integer> jis, DistributivityChecker.QuotientPoset q) {
        List<Integer> jiList = new ArrayList<>(jis);
        Collections.sort(jiList);

        // Full order restricted to J(L)
        Map<Integer, Set<Integer>> jiReach = new HashMap<>();
        for (int j : jiList) {
            Set<Integer> below = new HashSet<>();
            for (int k : jis) {
                if (k != j && q.fwdReach().get(j).contains(k)) {
                    below.add(k);
                }
            }
            jiReach.put(j, below);
        }

        // Remove transitive edges (Hasse diagram)
        Map<Integer, Set<Integer>> hasse = new HashMap<>();
        for (int j : jiList) hasse.put(j, new HashSet<>());
        for (int j : jiList) {
            for (int k : jiReach.get(j)) {
                boolean isCover = true;
                for (int m : jiReach.get(j)) {
                    if (m != k && q.fwdReach().getOrDefault(m, Set.of()).contains(k)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) {
                    hasse.get(j).add(k);
                }
            }
        }
        return hasse;
    }

    // =========================================================================
    // Internal: downset enumeration and Birkhoff map
    // =========================================================================

    private static Set<Set<Integer>> enumerateDownsets(
            Set<Integer> jis, DistributivityChecker.QuotientPoset q) {
        List<Integer> jiList = new ArrayList<>(jis);
        Collections.sort(jiList);
        int n = jiList.size();

        // Build order on J(L) from quotient reachability
        Map<Integer, Set<Integer>> orderBelow = new HashMap<>();
        for (int j : jiList) {
            Set<Integer> below = new HashSet<>();
            below.add(j);
            for (int k : jis) {
                if (k != j && q.fwdReach().get(j).contains(k)) {
                    below.add(k);
                }
            }
            orderBelow.put(j, below);
        }

        Set<Set<Integer>> downsets = new HashSet<>();
        for (int mask = 0; mask < (1 << n); mask++) {
            Set<Integer> subset = new HashSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) subset.add(jiList.get(i));
            }
            boolean isDownset = true;
            for (int x : subset) {
                if (!subset.containsAll(orderBelow.get(x))) {
                    isDownset = false;
                    break;
                }
            }
            if (isDownset) downsets.add(subset);
        }
        return downsets;
    }

    private static Map<Integer, Set<Integer>> birkhoffMap(
            DistributivityChecker.QuotientPoset q, Set<Integer> jis) {
        Map<Integer, Set<Integer>> iso = new HashMap<>();
        for (int x : q.nodes()) {
            Set<Integer> below = new HashSet<>();
            for (int j : jis) {
                if (q.fwdReach().get(x).contains(j)) {
                    below.add(j);
                }
            }
            iso.put(x, below);
        }
        return iso;
    }

    private static boolean verifyIsomorphism(
            DistributivityChecker.QuotientPoset q,
            Map<Integer, Set<Integer>> iso,
            Set<Set<Integer>> downsets) {
        // Check injectivity
        Set<Set<Integer>> images = new HashSet<>(iso.values());
        if (images.size() != iso.size()) return false;

        // Check surjectivity
        if (!images.equals(downsets)) return false;

        // Check order preservation/reflection
        List<Integer> nodesList = new ArrayList<>(q.nodes());
        Collections.sort(nodesList);
        for (int x : nodesList) {
            for (int y : nodesList) {
                boolean xGeY = q.fwdReach().get(x).contains(y);
                boolean phiXGePhiY = iso.get(x).containsAll(iso.get(y));
                if (xGeY != phiXGePhiY) return false;
            }
        }
        return true;
    }
}
