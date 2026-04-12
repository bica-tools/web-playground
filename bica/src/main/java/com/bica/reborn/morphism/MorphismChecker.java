package com.bica.reborn.morphism;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Morphism hierarchy checker for session type state spaces.
 *
 * <p>Classifies structure-preserving maps between state spaces (labeled
 * transition systems), ordered by strength:
 * <pre>
 *     isomorphism &sub; embedding &sub; projection &sub; homomorphism
 * </pre>
 *
 * <p>Ordering convention: {@code s1 &ge; s2} iff there is a directed path
 * from s1 to s2 (s2 is reachable from s1).
 *
 * <p>Also provides Galois connection checking.
 */
public final class MorphismChecker {

    private MorphismChecker() {}

    // =========================================================================
    // Public API: order properties
    // =========================================================================

    /**
     * Check whether {@code mapping} preserves order.
     *
     * <p>For all s1, s2 in source: if s2 &isin; reach(s1),
     * then mapping[s2] &isin; reach(mapping[s1]) in target.
     */
    public static boolean isOrderPreserving(
            StateSpace source, StateSpace target, Map<Integer, Integer> mapping) {
        var srcReach = reachability(source);
        var tgtReach = reachability(target);

        for (int s1 : source.states()) {
            for (int s2 : srcReach.get(s1)) {
                if (!tgtReach.get(mapping.get(s1)).contains(mapping.get(s2))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check whether {@code mapping} reflects order.
     *
     * <p>For all s1, s2 in source: if mapping[s2] &isin; reach(mapping[s1]),
     * then s2 &isin; reach(s1).
     */
    public static boolean isOrderReflecting(
            StateSpace source, StateSpace target, Map<Integer, Integer> mapping) {
        var srcReach = reachability(source);
        var tgtReach = reachability(target);

        for (int s1 : source.states()) {
            for (int s2 : source.states()) {
                if (tgtReach.get(mapping.get(s1)).contains(mapping.get(s2))
                        && !srcReach.get(s1).contains(s2)) {
                    return false;
                }
            }
        }
        return true;
    }

    // =========================================================================
    // Public API: morphism classification
    // =========================================================================

    /**
     * Classify a mapping between state spaces.
     *
     * @throws IllegalArgumentException if the mapping is not order-preserving
     */
    public static Morphism classifyMorphism(
            StateSpace source, StateSpace target, Map<Integer, Integer> mapping) {
        if (!isOrderPreserving(source, target, mapping)) {
            throw new IllegalArgumentException(
                    "mapping is not order-preserving (not a homomorphism)");
        }

        boolean injective = new HashSet<>(mapping.values()).size() == mapping.size();
        boolean surjective = new HashSet<>(mapping.values()).containsAll(target.states());
        boolean reflecting = isOrderReflecting(source, target, mapping);

        MorphismKind kind;
        if (injective && surjective && reflecting) {
            kind = MorphismKind.ISOMORPHISM;
        } else if (injective && reflecting) {
            kind = MorphismKind.EMBEDDING;
        } else if (surjective) {
            kind = MorphismKind.PROJECTION;
        } else {
            kind = MorphismKind.HOMOMORPHISM;
        }

        return new Morphism(source, target, mapping, kind);
    }

    // =========================================================================
    // Public API: find isomorphism
    // =========================================================================

    /**
     * Search for an isomorphism between {@code ss1} and {@code ss2}.
     *
     * <p>Returns a {@link Morphism} with kind {@link MorphismKind#ISOMORPHISM}
     * if one exists, or {@code null} if the state spaces are not isomorphic.
     *
     * <p>Uses backtracking with pruning:
     * <ol>
     *   <li>Quick rejection on state/transition counts.</li>
     *   <li>Degree-based pruning (out-degree, in-degree, reachability-set size).</li>
     *   <li>Fix top&rarr;top, bottom&rarr;bottom.</li>
     *   <li>Backtrack over remaining states.</li>
     * </ol>
     */
    public static Morphism findIsomorphism(StateSpace ss1, StateSpace ss2) {
        // Quick rejection
        if (ss1.states().size() != ss2.states().size()) return null;
        if (ss1.transitions().size() != ss2.transitions().size()) return null;

        // Compute signatures for pruning
        var sig1 = stateSignatures(ss1);
        var sig2 = stateSignatures(ss2);

        // Check that signature multisets match
        var sigs1Sorted = ss1.states().stream().map(sig1::get).sorted().toList();
        var sigs2Sorted = ss2.states().stream().map(sig2::get).sorted().toList();
        if (!sigs1Sorted.equals(sigs2Sorted)) return null;

        // Group target states by signature
        Map<Signature, List<Integer>> sigToS2 = new HashMap<>();
        for (int s : ss2.states()) {
            sigToS2.computeIfAbsent(sig2.get(s), k -> new ArrayList<>()).add(s);
        }

        var reach1 = reachability(ss1);
        var reach2 = reachability(ss2);

        // Fixed mappings: top->top, bottom->bottom
        Map<Integer, Integer> mapping = new HashMap<>();
        Set<Integer> used = new HashSet<>();

        mapping.put(ss1.top(), ss2.top());
        used.add(ss2.top());

        if (ss1.bottom() != ss1.top()) {
            if (used.contains(ss2.bottom())) return null;
            mapping.put(ss1.bottom(), ss2.bottom());
            used.add(ss2.bottom());
        }

        // Check signature compatibility for fixed states
        if (!sig1.get(ss1.top()).equals(sig2.get(ss2.top()))) return null;
        if (ss1.bottom() != ss1.top()
                && !sig1.get(ss1.bottom()).equals(sig2.get(ss2.bottom()))) {
            return null;
        }

        // Remaining states to assign
        List<Integer> remaining = ss1.states().stream()
                .filter(s -> !mapping.containsKey(s))
                .sorted()
                .toList();

        if (backtrackIso(0, remaining, mapping, used, sig1, sigToS2, reach1, reach2, ss1)) {
            return new Morphism(ss1, ss2, Map.copyOf(mapping), MorphismKind.ISOMORPHISM);
        }
        return null;
    }

    private static boolean backtrackIso(
            int idx,
            List<Integer> remaining,
            Map<Integer, Integer> mapping,
            Set<Integer> used,
            Map<Integer, Signature> sig1,
            Map<Signature, List<Integer>> sigToS2,
            Map<Integer, Set<Integer>> reach1,
            Map<Integer, Set<Integer>> reach2,
            StateSpace ss1) {

        if (idx == remaining.size()) {
            return checkPreserving(mapping, reach1, reach2, ss1)
                    && checkReflecting(mapping, reach1, reach2, ss1);
        }

        int s1 = remaining.get(idx);
        Signature sig = sig1.get(s1);
        List<Integer> candidates = sigToS2.getOrDefault(sig, List.of());

        for (int s2 : candidates) {
            if (used.contains(s2)) continue;
            if (compatible(s1, s2, mapping, reach1, reach2)) {
                mapping.put(s1, s2);
                used.add(s2);
                if (backtrackIso(idx + 1, remaining, mapping, used,
                        sig1, sigToS2, reach1, reach2, ss1)) {
                    return true;
                }
                mapping.remove(s1);
                used.remove(s2);
            }
        }
        return false;
    }

    // =========================================================================
    // Public API: find embedding
    // =========================================================================

    /**
     * Search for an order-embedding of {@code ss1} into {@code ss2}.
     *
     * <p>Returns a {@link Morphism} with kind {@link MorphismKind#EMBEDDING}
     * (or {@link MorphismKind#ISOMORPHISM} if surjective), or {@code null}
     * if no embedding exists.
     */
    public static Morphism findEmbedding(StateSpace ss1, StateSpace ss2) {
        if (ss1.states().size() > ss2.states().size()) return null;

        var reach1 = reachability(ss1);
        var reach2 = reachability(ss2);

        // Fixed: top->top, bottom->bottom
        Map<Integer, Integer> mapping = new HashMap<>();
        Set<Integer> used = new HashSet<>();

        mapping.put(ss1.top(), ss2.top());
        used.add(ss2.top());

        if (ss1.bottom() != ss1.top()) {
            if (used.contains(ss2.bottom())) return null;
            mapping.put(ss1.bottom(), ss2.bottom());
            used.add(ss2.bottom());
        }

        // Check top/bottom reachability compatibility
        if (reach1.get(ss1.top()).contains(ss1.bottom())
                && !reach2.get(ss2.top()).contains(ss2.bottom())) {
            return null;
        }

        List<Integer> remaining = ss1.states().stream()
                .filter(s -> !mapping.containsKey(s))
                .sorted()
                .toList();

        List<Integer> s2Candidates = ss2.states().stream().sorted().toList();

        if (backtrackEmbed(0, remaining, mapping, used,
                s2Candidates, reach1, reach2, ss1, ss2)) {
            boolean surjective = new HashSet<>(mapping.values()).containsAll(ss2.states());
            MorphismKind kind = surjective
                    ? MorphismKind.ISOMORPHISM : MorphismKind.EMBEDDING;
            return new Morphism(ss1, ss2, Map.copyOf(mapping), kind);
        }
        return null;
    }

    private static boolean backtrackEmbed(
            int idx,
            List<Integer> remaining,
            Map<Integer, Integer> mapping,
            Set<Integer> used,
            List<Integer> s2Candidates,
            Map<Integer, Set<Integer>> reach1,
            Map<Integer, Set<Integer>> reach2,
            StateSpace ss1,
            StateSpace ss2) {

        if (idx == remaining.size()) {
            return checkPreserving(mapping, reach1, reach2, ss1)
                    && checkReflecting(mapping, reach1, reach2, ss1);
        }

        int s1 = remaining.get(idx);
        for (int s2 : s2Candidates) {
            if (used.contains(s2)) continue;
            if (compatible(s1, s2, mapping, reach1, reach2)) {
                mapping.put(s1, s2);
                used.add(s2);
                if (backtrackEmbed(idx + 1, remaining, mapping, used,
                        s2Candidates, reach1, reach2, ss1, ss2)) {
                    return true;
                }
                mapping.remove(s1);
                used.remove(s2);
            }
        }
        return false;
    }

    // =========================================================================
    // Public API: Galois connection
    // =========================================================================

    /**
     * Check whether ({@code alpha}, {@code gamma}) forms a Galois connection.
     *
     * <p>Verifies: &alpha;(x) &le; y &hArr; x &le; &gamma;(y)
     * for all x in source, y in target.
     *
     * <p>In our ordering (&le; means "reachable from"):
     * s1 &le; s2 iff s1 &isin; reach(s2).
     */
    public static boolean isGaloisConnection(
            Map<Integer, Integer> alpha,
            Map<Integer, Integer> gamma,
            StateSpace source,
            StateSpace target) {
        var srcReach = reachability(source);
        var tgtReach = reachability(target);

        for (int x : source.states()) {
            for (int y : target.states()) {
                boolean alphaXLeqY = tgtReach.get(y).contains(alpha.get(x));
                boolean xLeqGammaY = srcReach.get(gamma.get(y)).contains(x);
                if (alphaXLeqY != xLeqGammaY) {
                    return false;
                }
            }
        }
        return true;
    }

    // =========================================================================
    // Internal: reachability computation
    // =========================================================================

    private static Map<Integer, Set<Integer>> reachability(StateSpace ss) {
        Map<Integer, Set<Integer>> result = new HashMap<>();
        for (int s : ss.states()) {
            result.put(s, ss.reachableFrom(s));
        }
        return result;
    }

    // =========================================================================
    // Internal: state signatures for pruning
    // =========================================================================

    /** Structural signature for pruning in isomorphism search. */
    private record Signature(int outDeg, int inDeg, int reachSize)
            implements Comparable<Signature> {

        @Override
        public int compareTo(Signature other) {
            int c = Integer.compare(outDeg, other.outDeg);
            if (c != 0) return c;
            c = Integer.compare(inDeg, other.inDeg);
            if (c != 0) return c;
            return Integer.compare(reachSize, other.reachSize);
        }
    }

    private static Map<Integer, Signature> stateSignatures(StateSpace ss) {
        Map<Integer, Integer> outDeg = new HashMap<>();
        Map<Integer, Integer> inDeg = new HashMap<>();
        for (int s : ss.states()) {
            outDeg.put(s, 0);
            inDeg.put(s, 0);
        }
        for (var t : ss.transitions()) {
            outDeg.merge(t.source(), 1, Integer::sum);
            inDeg.merge(t.target(), 1, Integer::sum);
        }

        var reach = reachability(ss);
        Map<Integer, Signature> sigs = new HashMap<>();
        for (int s : ss.states()) {
            sigs.put(s, new Signature(outDeg.get(s), inDeg.get(s), reach.get(s).size()));
        }
        return sigs;
    }

    // =========================================================================
    // Internal: backtracking helpers
    // =========================================================================

    /**
     * Check that assigning s1&rarr;s2 is compatible with the partial mapping.
     */
    private static boolean compatible(
            int s1, int s2,
            Map<Integer, Integer> partial,
            Map<Integer, Set<Integer>> reach1,
            Map<Integer, Set<Integer>> reach2) {

        for (var entry : partial.entrySet()) {
            int a = entry.getKey();
            int fa = entry.getValue();

            // Preserving: s1 >= a => s2 >= fa
            if (reach1.get(s1).contains(a) && !reach2.get(s2).contains(fa)) return false;
            // Preserving: a >= s1 => fa >= s2
            if (reach1.get(a).contains(s1) && !reach2.get(fa).contains(s2)) return false;
            // Reflecting: s2 >= fa => s1 >= a
            if (reach2.get(s2).contains(fa) && !reach1.get(s1).contains(a)) return false;
            // Reflecting: fa >= s2 => a >= s1
            if (reach2.get(fa).contains(s2) && !reach1.get(a).contains(s1)) return false;
        }
        return true;
    }

    private static boolean checkPreserving(
            Map<Integer, Integer> mapping,
            Map<Integer, Set<Integer>> reach1,
            Map<Integer, Set<Integer>> reach2,
            StateSpace ss1) {
        for (int s1 : ss1.states()) {
            for (int s2 : reach1.get(s1)) {
                if (!reach2.get(mapping.get(s1)).contains(mapping.get(s2))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean checkReflecting(
            Map<Integer, Integer> mapping,
            Map<Integer, Set<Integer>> reach1,
            Map<Integer, Set<Integer>> reach2,
            StateSpace ss1) {
        for (int s1 : ss1.states()) {
            for (int s2 : ss1.states()) {
                if (reach2.get(mapping.get(s1)).contains(mapping.get(s2))
                        && !reach1.get(s1).contains(s2)) {
                    return false;
                }
            }
        }
        return true;
    }
}
