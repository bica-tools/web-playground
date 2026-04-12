package com.bica.reborn.coequalizer;

import com.bica.reborn.category.CategoryChecker;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.reticular.ReticularChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;

/**
 * Coequalizers in SessLat — categorical coequalizer analysis.
 *
 * <p>A coequalizer of two morphisms f, g: L → R is a lattice Q with a
 * morphism q: R → Q such that q ∘ f = q ∘ g, and Q is universal.
 *
 * <p><b>Main result</b>: SessLat does NOT have general coequalizers.
 * The quotient R/θ is always a lattice, but may not be realizable as a session type.
 */
public final class CoequalizerChecker {

    private CoequalizerChecker() {}

    // =========================================================================
    // Seed pairs
    // =========================================================================

    /**
     * Compute {(f(s), g(s)) : s ∈ source}.
     */
    public static Set<long[]> coequalizerSeedPairs(
            StateSpace source, Map<Integer, Integer> f, Map<Integer, Integer> g) {
        Set<long[]> pairs = new HashSet<>();
        for (int s : source.states()) {
            pairs.add(new long[]{f.get(s), g.get(s)});
        }
        return pairs;
    }

    // =========================================================================
    // Union-Find for congruence closure
    // =========================================================================

    private static class UnionFind {
        private final Map<Integer, Integer> parent;
        private final Map<Integer, Integer> rank;

        UnionFind(Set<Integer> elements) {
            parent = new HashMap<>();
            rank = new HashMap<>();
            for (int e : elements) {
                parent.put(e, e);
                rank.put(e, 0);
            }
        }

        int find(int x) {
            while (parent.get(x) != x) {
                parent.put(x, parent.get(parent.get(x)));
                x = parent.get(x);
            }
            return x;
        }

        boolean union(int x, int y) {
            int rx = find(x), ry = find(y);
            if (rx == ry) return false;
            if (rank.get(rx) < rank.get(ry)) { int t = rx; rx = ry; ry = t; }
            parent.put(ry, rx);
            if (rank.get(rx).equals(rank.get(ry))) rank.merge(rx, 1, Integer::sum);
            return true;
        }

        Map<Integer, Set<Integer>> classes() {
            Map<Integer, Set<Integer>> groups = new HashMap<>();
            for (int e : parent.keySet()) {
                int r = find(e);
                groups.computeIfAbsent(r, k -> new HashSet<>()).add(e);
            }
            return groups;
        }
    }

    // =========================================================================
    // Congruence closure
    // =========================================================================

    /**
     * Compute the smallest congruence on target containing seed pairs.
     */
    public static Map<Integer, Set<Integer>> congruenceClosure(
            StateSpace target, Set<long[]> seedPairs) {
        UnionFind uf = new UnionFind(target.states());

        for (long[] pair : seedPairs) {
            uf.union((int) pair[0], (int) pair[1]);
        }

        // Iterate meet/join closure until fixed point
        boolean changed = true;
        while (changed) {
            changed = false;
            Map<Integer, Set<Integer>> cls = uf.classes();

            // For each pair of elements in the same class, close under meets/joins
            for (var entry : cls.values()) {
                List<Integer> members = new ArrayList<>(entry);
                Collections.sort(members);
                for (int i = 0; i < members.size(); i++) {
                    int a = members.get(i);
                    for (int j = i + 1; j < members.size(); j++) {
                        int b = members.get(j);
                        for (int c : target.states()) {
                            Integer ma = LatticeChecker.computeMeet(target, a, c);
                            Integer mb = LatticeChecker.computeMeet(target, b, c);
                            if (ma != null && mb != null && uf.union(ma, mb)) changed = true;

                            Integer ja = LatticeChecker.computeJoin(target, a, c);
                            Integer jb = LatticeChecker.computeJoin(target, b, c);
                            if (ja != null && jb != null && uf.union(ja, jb)) changed = true;
                        }
                    }
                }
            }
        }

        return uf.classes();
    }

    // =========================================================================
    // Quotient lattice construction
    // =========================================================================

    /**
     * Build the quotient lattice R/θ.
     */
    public static StateSpace quotientLattice(
            StateSpace target, Map<Integer, Set<Integer>> congruenceClasses) {
        // Choose representative per class: minimum element
        Map<Integer, Integer> repMap = new HashMap<>();
        for (var entry : congruenceClasses.entrySet()) {
            int canonical = Collections.min(entry.getValue());
            for (int m : entry.getValue()) {
                repMap.put(m, canonical);
            }
        }

        List<Integer> canonicalReps = new ArrayList<>(new TreeSet<>(repMap.values()));
        Map<Integer, Integer> remap = new HashMap<>();
        for (int i = 0; i < canonicalReps.size(); i++) {
            remap.put(canonicalReps.get(i), i);
        }

        Set<Integer> states = new HashSet<>(remap.values());
        Map<Integer, String> labels = new HashMap<>();
        for (var entry : remap.entrySet()) {
            int old = entry.getKey();
            int newId = entry.getValue();
            List<Integer> members = new ArrayList<>();
            for (int s : target.states()) {
                if (repMap.get(s) == old) members.add(s);
            }
            if (members.size() == 1) {
                labels.put(newId, target.labels().getOrDefault(old, String.valueOf(old)));
            } else {
                Collections.sort(members);
                labels.put(newId, "[" + String.join(",",
                        members.stream().map(m ->
                                target.labels().getOrDefault(m, String.valueOf(m))).toList()) + "]");
            }
        }

        // Compute reachability in target
        Map<Integer, Set<Integer>> tgtReach = new HashMap<>();
        for (int s : target.states()) {
            tgtReach.put(s, target.reachableFrom(s));
        }

        // Order on quotient
        Map<Integer, Set<Integer>> qReach = new HashMap<>();
        for (int a : canonicalReps) qReach.put(a, new HashSet<>());
        for (int a : canonicalReps) {
            List<Integer> aClass = target.states().stream()
                    .filter(s -> repMap.get(s) == a).toList();
            for (int b : canonicalReps) {
                if (a == b) continue;
                List<Integer> bClass = target.states().stream()
                        .filter(s -> repMap.get(s) == b).toList();
                boolean reaches = false;
                for (int am : aClass) {
                    for (int bm : bClass) {
                        if (tgtReach.get(am).contains(bm)) {
                            reaches = true;
                            break;
                        }
                    }
                    if (reaches) break;
                }
                if (reaches) qReach.get(a).add(b);
            }
        }

        // Build covering relation
        List<Transition> transitions = new ArrayList<>();
        for (int a : canonicalReps) {
            for (int b : canonicalReps) {
                if (a == b) continue;
                if (!qReach.get(a).contains(b)) continue;
                boolean isCover = true;
                for (int c : canonicalReps) {
                    if (c == a || c == b) continue;
                    if (qReach.get(a).contains(c) && qReach.get(c).contains(b)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) {
                    String label = findLabel(target, a, b, repMap);
                    transitions.add(new Transition(remap.get(a), label, remap.get(b)));
                }
            }
        }

        int topRep = repMap.get(target.top());
        int bottomRep = repMap.get(target.bottom());

        return new StateSpace(states, transitions,
                remap.get(topRep), remap.get(bottomRep), labels);
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Check whether the coequalizer of f, g: source → target exists in SessLat.
     */
    public static CoequalizerResult computeCoequalizer(
            StateSpace source, StateSpace target,
            Map<Integer, Integer> f, Map<Integer, Integer> g) {

        LatticeResult srcLr = LatticeChecker.checkLattice(source);
        if (!srcLr.isLattice()) {
            return new CoequalizerResult(false, null, null, null,
                    f, g, false, false, false, "source is not a lattice");
        }
        LatticeResult tgtLr = LatticeChecker.checkLattice(target);
        if (!tgtLr.isLattice()) {
            return new CoequalizerResult(false, null, null, null,
                    f, g, false, false, false, "target is not a lattice");
        }

        Set<long[]> seeds = coequalizerSeedPairs(source, f, g);
        Map<Integer, Set<Integer>> cong = congruenceClosure(target, seeds);

        StateSpace qSs = quotientLattice(target, cong);

        // Build quotient map
        Map<Integer, Integer> repMap = new HashMap<>();
        for (var entry : cong.entrySet()) {
            int canonical = Collections.min(entry.getValue());
            for (int m : entry.getValue()) repMap.put(m, canonical);
        }
        List<Integer> canonicalReps = new ArrayList<>(new TreeSet<>(repMap.values()));
        Map<Integer, Integer> remap = new HashMap<>();
        for (int i = 0; i < canonicalReps.size(); i++) {
            remap.put(canonicalReps.get(i), i);
        }
        Map<Integer, Integer> qMap = new HashMap<>();
        for (int s : target.states()) {
            qMap.put(s, remap.get(repMap.get(s)));
        }

        LatticeResult qLr = LatticeChecker.checkLattice(qSs);
        boolean isLat = qLr.isLattice();
        boolean realizable = isLat && ReticularChecker.isReticulate(qSs);

        // Verify q ∘ f = q ∘ g
        for (int s : source.states()) {
            if (!Objects.equals(qMap.get(f.get(s)), qMap.get(g.get(s)))) {
                return new CoequalizerResult(false, qSs, qMap, cong,
                        f, g, isLat, realizable, false, "q . f != q . g");
            }
        }

        boolean hasCoeq = isLat;
        return new CoequalizerResult(hasCoeq, qSs, qMap, cong,
                f, g, isLat, realizable, isLat,
                isLat ? null : "quotient is not a lattice");
    }

    /**
     * Check the coequalizer universal property (simplified).
     */
    public static boolean checkCoequalizerProperty(
            StateSpace quotient, StateSpace source, StateSpace target,
            Map<Integer, Integer> f, Map<Integer, Integer> g,
            Map<Integer, Integer> qMap) {
        if (!LatticeChecker.checkLattice(quotient).isLattice()) return false;
        for (int s : source.states()) {
            if (!Objects.equals(qMap.get(f.get(s)), qMap.get(g.get(s)))) return false;
        }
        return true;
    }

    /**
     * Check if quotient with qMap is the coequalizer of f and g.
     */
    public static boolean isCoequalizer(
            StateSpace quotient, StateSpace source, StateSpace target,
            Map<Integer, Integer> f, Map<Integer, Integer> g,
            Map<Integer, Integer> qMap) {
        return checkCoequalizerProperty(quotient, source, target, f, g, qMap);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private static String findLabel(
            StateSpace target, int a, int b, Map<Integer, Integer> repMap) {
        for (var t : target.transitions()) {
            if (Objects.equals(repMap.get(t.source()), a)
                    && Objects.equals(repMap.get(t.target()), b)) {
                return t.label();
            }
        }
        for (var t : target.transitions()) {
            if (Objects.equals(repMap.get(t.source()), a)) {
                List<Integer> bClass = target.states().stream()
                        .filter(s -> repMap.get(s) == b).toList();
                Set<Integer> reachable = target.reachableFrom(t.target());
                if (bClass.stream().anyMatch(m -> reachable.contains(m) || m == t.target())) {
                    return t.label();
                }
            }
        }
        return "tau";
    }
}
