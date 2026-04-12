package com.bica.reborn.congruence;

import java.util.*;

/**
 * A congruence on a lattice: an equivalence relation compatible with meet and join.
 *
 * <p>If {@code x ≡ y (mod θ)} then {@code x∧z ≡ y∧z} and {@code x∨z ≡ y∨z}
 * for all z in L.
 *
 * @param classes    Equivalence classes (each a set of state IDs).
 * @param numClasses Number of equivalence classes.
 * @param generators Pairs (a,b) that generate this congruence (may be empty).
 */
public record Congruence(
        List<Set<Integer>> classes,
        int numClasses,
        List<int[]> generators) {

    public Congruence {
        Objects.requireNonNull(classes, "classes must not be null");
        Objects.requireNonNull(generators, "generators must not be null");
        classes = classes.stream()
                .map(Set::copyOf)
                .toList();
        generators = List.copyOf(generators);
    }

    /** True iff this is the identity congruence (all singletons). */
    public boolean isTrivialBottom() {
        return classes.stream().allMatch(c -> c.size() == 1);
    }

    /** True iff this is the total congruence (one big class). */
    public boolean isTrivialTop() {
        return numClasses == 1;
    }

    /** Return the equivalence class containing the given state. */
    public Set<Integer> classOf(int state) {
        for (Set<Integer> c : classes) {
            if (c.contains(state)) return c;
        }
        return Set.of(state);
    }
}
