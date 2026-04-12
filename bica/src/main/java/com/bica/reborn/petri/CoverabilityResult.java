package com.bica.reborn.petri;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Result of coverability analysis (Karp-Miller tree).
 *
 * @param numNodes             Total number of nodes in the coverability tree.
 * @param numDistinctMarkings  Number of distinct omega-markings.
 * @param isBounded            True if all places are bounded (no omega).
 * @param unboundedPlaces      Set of place IDs that are unbounded.
 * @param isOneSafe            True if every place has at most 1 token.
 */
public record CoverabilityResult(
        int numNodes,
        int numDistinctMarkings,
        boolean isBounded,
        Set<Integer> unboundedPlaces,
        boolean isOneSafe) {

    public CoverabilityResult {
        Objects.requireNonNull(unboundedPlaces);
        unboundedPlaces = Set.copyOf(unboundedPlaces);
    }
}
