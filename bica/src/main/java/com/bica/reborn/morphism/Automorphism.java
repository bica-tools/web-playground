package com.bica.reborn.morphism;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Automorphism group analysis for session type lattices.
 *
 * <p>Step 363e: Computes the automorphism group {@code Aut(L)} of a session
 * type lattice — the group of all order-preserving bijections {@code L → L}
 * that preserve meet and join.
 *
 * <p>Operational interpretation: automorphisms represent protocol symmetries —
 * permutations of states that preserve all structural relationships. A large
 * automorphism group means the protocol has redundant structure that could be
 * factored out.
 *
 * <p>Ported from {@code reticulate/reticulate/automorphism.py}.
 */
public final class Automorphism {

    /** Maximum lattice size for brute-force enumeration. */
    public static final int MAX_BRUTE_FORCE = 10;

    private Automorphism() {}

    // -- Result records --------------------------------------------------------

    /** A single lattice automorphism as a state permutation. */
    public record LatticeAutomorphism(
            Map<Integer, Integer> mapping,
            List<Integer> fixedPoints,
            boolean isIdentity,
            int order) {

        public LatticeAutomorphism {
            mapping = Map.copyOf(mapping);
            fixedPoints = List.copyOf(fixedPoints);
        }

        /** Apply this automorphism to a state. O(1) via map lookup. */
        public int apply(int state) {
            Integer v = mapping.get(state);
            return v == null ? state : v;
        }
    }

    /** The automorphism group {@code Aut(L)} of a lattice. */
    public record AutomorphismGroup(
            List<LatticeAutomorphism> automorphisms,
            int order,
            boolean isTrivial,
            boolean isComplete,
            List<LatticeAutomorphism> generators,
            int numFixedByAll,
            List<List<Integer>> orbitPartition) {

        public AutomorphismGroup {
            automorphisms = List.copyOf(automorphisms);
            generators = List.copyOf(generators);
            List<List<Integer>> ops = new ArrayList<>(orbitPartition.size());
            for (var orb : orbitPartition) ops.add(List.copyOf(orb));
            orbitPartition = List.copyOf(ops);
        }
    }

    /** Bundled return for {@link #findAutomorphisms}. */
    public record FindResult(List<LatticeAutomorphism> automorphisms, boolean isComplete) {
        public FindResult {
            automorphisms = List.copyOf(automorphisms);
        }
    }

    // -- Internal: SCC-quotient reachability ----------------------------------

    private static final class Reach {
        final List<Integer> nodes;
        final int topRep;
        final int bottomRep;
        final Map<Integer, Set<Integer>> reach;

        Reach(List<Integer> nodes, int topRep, int bottomRep,
              Map<Integer, Set<Integer>> reach) {
            this.nodes = nodes;
            this.topRep = topRep;
            this.bottomRep = bottomRep;
            this.reach = reach;
        }
    }

    private static Reach buildReach(StateSpace ss, LatticeResult lr) {
        Map<Integer, Integer> sccMap = lr.sccMap();
        List<Integer> reps;
        if (sccMap != null && !sccMap.isEmpty()) {
            reps = new ArrayList<>(new TreeSet<>(sccMap.values()));
        } else {
            reps = new ArrayList<>(new TreeSet<>(ss.states()));
        }
        if (reps.isEmpty()) {
            reps = new ArrayList<>(new TreeSet<>(ss.states()));
        }

        int topRep = sccMap == null ? ss.top() : sccMap.getOrDefault(ss.top(), ss.top());
        int bottomRep = sccMap == null ? ss.bottom() : sccMap.getOrDefault(ss.bottom(), ss.bottom());

        Map<Integer, Set<Integer>> fwd = new HashMap<>();
        for (int r : reps) fwd.put(r, new HashSet<>());

        for (var t : ss.transitions()) {
            int srcR = sccMap == null ? t.source() : sccMap.getOrDefault(t.source(), t.source());
            int tgtR = sccMap == null ? t.target() : sccMap.getOrDefault(t.target(), t.target());
            if (srcR != tgtR) {
                fwd.computeIfAbsent(srcR, k -> new HashSet<>()).add(tgtR);
            }
        }

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : reps) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int cur = stack.pop();
                if (!visited.add(cur)) continue;
                for (int t : fwd.getOrDefault(cur, Set.of())) {
                    if (!visited.contains(t)) stack.push(t);
                }
            }
            reach.put(s, visited);
        }
        return new Reach(reps, topRep, bottomRep, reach);
    }

    // -- Internal: meet table --------------------------------------------------

    private static Map<Long, Integer> precomputeMeetTable(
            Map<Integer, Set<Integer>> reach, List<Integer> nodes) {
        // value = meet rep, or Integer.MIN_VALUE for "null"
        Map<Long, Integer> table = new HashMap<>();
        for (int a : nodes) {
            for (int b : nodes) {
                long key = packKey(a, b);
                if (a == b) {
                    table.put(key, a);
                    continue;
                }
                List<Integer> lower = new ArrayList<>();
                Set<Integer> ra = reach.get(a);
                Set<Integer> rb = reach.get(b);
                for (int n : nodes) {
                    if (ra.contains(n) && rb.contains(n)) lower.add(n);
                }
                Integer meet = null;
                for (int c : lower) {
                    Set<Integer> rc = reach.get(c);
                    boolean ok = true;
                    for (int lb : lower) {
                        if (!(rc.contains(lb) || lb == c)) { ok = false; break; }
                    }
                    if (ok) { meet = c; break; }
                }
                table.put(key, meet == null ? Integer.MIN_VALUE : meet);
            }
        }
        return table;
    }

    private static long packKey(int a, int b) {
        return (((long) a) << 32) ^ (b & 0xffffffffL);
    }

    private static Integer meetLookup(Map<Long, Integer> table, int a, int b) {
        Integer v = table.get(packKey(a, b));
        if (v == null || v == Integer.MIN_VALUE) return null;
        return v;
    }

    // -- Internal: automorphism check -----------------------------------------

    private static boolean isAutomorphism(
            Map<Integer, Integer> perm,
            List<Integer> nodes,
            Map<Integer, Set<Integer>> reach,
            Map<Long, Integer> meetTable) {
        // Order preservation: b in reach[a]  =>  perm[b] in reach[perm[a]]
        for (int a : nodes) {
            Set<Integer> ra = reach.get(a);
            Set<Integer> rPa = reach.get(perm.get(a));
            for (int b : nodes) {
                if (ra.contains(b)) {
                    if (!rPa.contains(perm.get(b))) return false;
                }
            }
        }
        // Meet preservation
        for (int a : nodes) {
            for (int b : nodes) {
                Integer m = meetLookup(meetTable, a, b);
                Integer pm = meetLookup(meetTable, perm.get(a), perm.get(b));
                if (m != null && pm != null) {
                    if (!perm.get(m).equals(pm)) return false;
                }
            }
        }
        return true;
    }

    private static int computeOrder(Map<Integer, Integer> perm, List<Integer> nodes) {
        Map<Integer, Integer> current = new HashMap<>(perm);
        for (int k = 1; k <= nodes.size(); k++) {
            boolean isId = true;
            for (int n : nodes) {
                if (current.get(n).intValue() != n) { isId = false; break; }
            }
            if (isId) return k;
            Map<Integer, Integer> next = new HashMap<>();
            for (int n : nodes) next.put(n, perm.get(current.get(n)));
            current = next;
        }
        return nodes.size();
    }

    // -- Public API -----------------------------------------------------------

    /** Find all automorphisms of the lattice. */
    public static FindResult findAutomorphisms(StateSpace ss) {
        return findAutomorphisms(ss, null);
    }

    public static FindResult findAutomorphisms(StateSpace ss, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return new FindResult(List.of(), true);

        Reach r = buildReach(ss, lr);
        int n = r.nodes.size();
        if (n == 0) return new FindResult(List.of(), true);

        List<LatticeAutomorphism> auts = new ArrayList<>();

        if (n <= MAX_BRUTE_FORCE) {
            Map<Long, Integer> meetTable = precomputeMeetTable(r.reach, r.nodes);

            // Permute only inner nodes (top and bottom are fixed)
            List<Integer> inner = new ArrayList<>();
            for (int s : r.nodes) {
                if (s != r.topRep && s != r.bottomRep) inner.add(s);
            }

            for (List<Integer> perm : permutations(inner)) {
                Map<Integer, Integer> permDict = new HashMap<>();
                permDict.put(r.topRep, r.topRep);
                permDict.put(r.bottomRep, r.bottomRep);
                for (int i = 0; i < inner.size(); i++) {
                    permDict.put(inner.get(i), perm.get(i));
                }
                if (isAutomorphism(permDict, r.nodes, r.reach, meetTable)) {
                    List<Integer> fixed = new ArrayList<>();
                    boolean isId = true;
                    for (int s : r.nodes) {
                        if (permDict.get(s).intValue() == s) fixed.add(s);
                        else isId = false;
                    }
                    int order = isId ? 1 : computeOrder(permDict, r.nodes);
                    auts.add(new LatticeAutomorphism(permDict, fixed, isId, order));
                }
            }
            return new FindResult(auts, true);
        } else {
            // Too large: only return identity, mark incomplete
            Map<Integer, Integer> id = new HashMap<>();
            for (int s : r.nodes) id.put(s, s);
            auts.add(new LatticeAutomorphism(id, new ArrayList<>(r.nodes), true, 1));
            return new FindResult(auts, false);
        }
    }

    /** Compute the full automorphism group {@code Aut(L)}. */
    public static AutomorphismGroup automorphismGroup(StateSpace ss) {
        return automorphismGroup(ss, null);
    }

    public static AutomorphismGroup automorphismGroup(StateSpace ss, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) {
            return new AutomorphismGroup(
                    List.of(), 0, true, true, List.of(), 0, List.of());
        }

        FindResult fr = findAutomorphisms(ss, lr);
        List<LatticeAutomorphism> auts = fr.automorphisms();
        Reach r = buildReach(ss, lr);

        List<List<Integer>> orbits = computeOrbits(auts, r.nodes);
        List<LatticeAutomorphism> generators = findGenerators(auts);

        int fixedByAll = 0;
        for (int s : r.nodes) {
            boolean all = true;
            for (var a : auts) {
                if (a.apply(s) != s) { all = false; break; }
            }
            if (all) fixedByAll++;
        }

        return new AutomorphismGroup(
                auts,
                auts.size(),
                auts.size() <= 1,
                fr.isComplete(),
                generators,
                fixedByAll,
                orbits);
    }

    // -- Internal: orbits & generators ----------------------------------------

    private static List<List<Integer>> computeOrbits(
            List<LatticeAutomorphism> auts, List<Integer> nodes) {
        Set<Integer> visited = new HashSet<>();
        List<List<Integer>> orbits = new ArrayList<>();
        for (int s : nodes) {
            if (visited.contains(s)) continue;
            Set<Integer> orbit = new HashSet<>();
            orbit.add(s);
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(s);
            while (!queue.isEmpty()) {
                int current = queue.poll();
                for (var a : auts) {
                    int img = a.apply(current);
                    if (orbit.add(img)) queue.add(img);
                }
            }
            List<Integer> sorted = new ArrayList<>(orbit);
            Collections.sort(sorted);
            orbits.add(sorted);
            visited.addAll(orbit);
        }
        return orbits;
    }

    private static List<LatticeAutomorphism> findGenerators(List<LatticeAutomorphism> auts) {
        if (auts.size() <= 1) return List.of();
        List<LatticeAutomorphism> nonId = new ArrayList<>();
        LatticeAutomorphism idAut = null;
        for (var a : auts) {
            if (a.isIdentity()) idAut = a;
            else nonId.add(a);
        }
        if (nonId.isEmpty() || idAut == null) return List.of();

        Set<String> allMappings = new HashSet<>();
        for (var a : auts) allMappings.add(mappingKey(a.mapping()));

        List<LatticeAutomorphism> generators = new ArrayList<>();
        Set<String> generated = new HashSet<>();
        generated.add(mappingKey(idAut.mapping()));

        Set<Integer> domain = idAut.mapping().keySet();

        for (var a : nonId) {
            String aKey = mappingKey(a.mapping());
            if (generated.contains(aKey)) continue;
            generators.add(a);

            Set<String> newElements = new HashSet<>();
            newElements.add(aKey);
            // Need a quick lookup from key -> mapping for compositions
            Map<String, Map<Integer, Integer>> keyToMap = new HashMap<>();
            for (String k : generated) keyToMap.put(k, parseKey(k));
            keyToMap.put(aKey, a.mapping());

            while (!newElements.isEmpty()) {
                Set<String> nextNew = new HashSet<>();
                for (String newKey : newElements) {
                    Map<Integer, Integer> newMap = keyToMap.get(newKey);
                    for (String existingKey : new ArrayList<>(generated)) {
                        Map<Integer, Integer> existingMap = keyToMap.get(existingKey);
                        Map<Integer, Integer> comp1 = new HashMap<>();
                        Map<Integer, Integer> comp2 = new HashMap<>();
                        for (int s : domain) {
                            int e1 = existingMap.getOrDefault(s, s);
                            comp1.put(s, newMap.getOrDefault(e1, e1));
                            int n1 = newMap.getOrDefault(s, s);
                            comp2.put(s, existingMap.getOrDefault(n1, n1));
                        }
                        String k1 = mappingKey(comp1);
                        String k2 = mappingKey(comp2);
                        if (!generated.contains(k1)) {
                            nextNew.add(k1);
                            keyToMap.put(k1, comp1);
                        }
                        if (!generated.contains(k2)) {
                            nextNew.add(k2);
                            keyToMap.put(k2, comp2);
                        }
                    }
                }
                generated.addAll(newElements);
                newElements = nextNew;
            }

            if (generated.equals(allMappings)) break;
        }

        return generators;
    }

    private static String mappingKey(Map<Integer, Integer> m) {
        List<Integer> keys = new ArrayList<>(m.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (int k : keys) sb.append(k).append('>').append(m.get(k)).append(';');
        return sb.toString();
    }

    private static Map<Integer, Integer> parseKey(String key) {
        Map<Integer, Integer> result = new HashMap<>();
        if (key.isEmpty()) return result;
        for (String part : key.split(";")) {
            if (part.isEmpty()) continue;
            int idx = part.indexOf('>');
            int k = Integer.parseInt(part.substring(0, idx));
            int v = Integer.parseInt(part.substring(idx + 1));
            result.put(k, v);
        }
        return result;
    }

    // -- Internal: permutations -----------------------------------------------

    private static List<List<Integer>> permutations(List<Integer> items) {
        List<List<Integer>> result = new ArrayList<>();
        permute(new ArrayList<>(items), 0, result);
        return result;
    }

    private static void permute(List<Integer> arr, int k, List<List<Integer>> out) {
        if (k == arr.size() - 1 || arr.isEmpty()) {
            out.add(new ArrayList<>(arr));
            return;
        }
        for (int i = k; i < arr.size(); i++) {
            Collections.swap(arr, i, k);
            permute(arr, k + 1, out);
            Collections.swap(arr, i, k);
        }
    }
}
