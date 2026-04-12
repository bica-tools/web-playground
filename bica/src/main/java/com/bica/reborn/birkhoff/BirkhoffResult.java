package com.bica.reborn.birkhoff;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Result of Birkhoff representation analysis for a session type lattice.
 *
 * <p>Birkhoff's fundamental theorem (1937) states that every finite distributive
 * lattice L is isomorphic to the lattice of downsets (order ideals) of the
 * poset J(L) of its join-irreducible elements: {@code L ≅ O(J(L))}.
 *
 * @param isLattice              True iff the state space forms a lattice.
 * @param isDistributive         True iff the lattice is distributive.
 * @param isRepresentable        True iff the Birkhoff representation holds (= distributive).
 * @param joinIrreducibles       Set of join-irreducible representative state IDs.
 * @param jiPoset                Adjacency map of the join-irreducible poset (Hasse diagram).
 * @param downsetLatticeSize     Number of downsets of J(L).
 * @param latticeSize            Number of elements in the lattice (quotient nodes).
 * @param compressionRatio       |J(L)| / |L| — smaller means better compression.
 * @param isomorphismVerified    True iff L ≅ O(J(L)) was verified element-by-element.
 * @param isoMap                 Mapping from quotient node → downset of join-irreducibles.
 */
public record BirkhoffResult(
        boolean isLattice,
        boolean isDistributive,
        boolean isRepresentable,
        Set<Integer> joinIrreducibles,
        Map<Integer, Set<Integer>> jiPoset,
        int downsetLatticeSize,
        int latticeSize,
        double compressionRatio,
        boolean isomorphismVerified,
        Map<Integer, Set<Integer>> isoMap) {

    public BirkhoffResult {
        Objects.requireNonNull(joinIrreducibles, "joinIrreducibles must not be null");
        Objects.requireNonNull(jiPoset, "jiPoset must not be null");
        Objects.requireNonNull(isoMap, "isoMap must not be null");
        joinIrreducibles = Set.copyOf(joinIrreducibles);
        isoMap = Map.copyOf(isoMap);
    }
}
